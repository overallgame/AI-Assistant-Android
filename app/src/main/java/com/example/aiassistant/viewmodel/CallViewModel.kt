package com.example.aiassistant.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiassistant.data.model.CallPhase
import com.example.aiassistant.data.model.CallState
import com.example.aiassistant.data.model.ChatMessageRecord
import com.example.aiassistant.data.model.WebSocketConnectionState
import com.example.aiassistant.data.repository.interfac.ChatWebSocketRepository
import com.example.aiassistant.data.websocket.WebSocketEvent
import com.example.aiassistant.speak.SpeakCallback
import com.example.aiassistant.speak.SpeakManager
import com.example.aiassistant.speak.SwitchStrategy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val webSocketRepository: ChatWebSocketRepository,
    private val speakManager: SpeakManager,
) : ViewModel(), SpeakCallback {

    companion object {
        private const val TAG = "CallViewModel"
    }

    private val _callState = MutableStateFlow(CallState())
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    // 通话计时器Job
    private var timerJob: Job? = null
    private var callStartTime: Long = 0L

    // 当前累积的AI回复
    private var currentAiReply = StringBuilder()

    // 是否正在等待AI响应
    private var isWaitingForAiResponse = false

    // 当前短连接的消息ID（用于关联响应）
    private var currentMessageId: String? = null

    init {
        // 初始化语音管理器
        speakManager.setTtsSwitchStrategy(SwitchStrategy.AUTO)
        speakManager.setCallback(this)
        speakManager.init()

        // 观察WebSocket连接状态
        observeConnectionState()

        // 观察WebSocket事件
        observeWebSocketEvents()
    }

    /**
     * 观察WebSocket连接状态
     * 短连接模式下：连接 -> 断开 是正常流程，不需要特殊处理
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            webSocketRepository.connectionState.collect { state ->
                when (state) {
                    WebSocketConnectionState.Connected -> {
                        Log.d(TAG, "WebSocket已连接")
                        // 连接成功后开始倾听
                        if (_callState.value.phase == CallPhase.Connecting) {
                            startListening()
                        }
                    }

                    WebSocketConnectionState.Connecting -> {
                        Log.d(TAG, "WebSocket连接中...")
                    }

                    WebSocketConnectionState.Disconnected -> {
                        Log.d(TAG, "WebSocket已断开（短连接模式，消息响应后正常断开）")
                        // 短连接模式下，断开是正常行为，不需要错误提示
                    }

                    WebSocketConnectionState.Reconnecting -> {
                        Log.d(TAG, "WebSocket重连中...")
                    }
                }
            }
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketRepository.events.collect { event ->
                when (event) {
                    is WebSocketEvent.XinghuoContentChunkReceived -> {
                        // 流式接收AI回复
                        onAiContentChunk(event.content)
                    }

                    is WebSocketEvent.XinghuoDoneReceived -> {
                        // AI回复完成，开始TTS播放
                        onAiReplyComplete()
                    }

                    is WebSocketEvent.XinghuoError -> {
                        // AI响应错误
                        onAiError(event.message)
                    }

                    is WebSocketEvent.Error -> {
                        // WebSocket错误
                        onAiError(event.message)
                    }

                    else -> {}
                }
            }
        }
    }

    /**
     * 开始通话
     * 短连接模式下：不需要预先建立连接，直接开始倾听
     * 连接会在用户说话后发送消息时自动建立
     */
    fun startCall() {
        if (!_callState.value.canStartCall) {
            Log.w(TAG, "当前状态不允许开始通话")
            return
        }

        Log.d(TAG, "开始通话")

        // 设置为连接中状态
        _callState.update {
            it.copy(
                phase = CallPhase.Listening,  // 直接进入倾听状态
                isConnecting = false,
                errorMessage = null
            )
        }

        // 短连接模式：不需要预先连接，直接开始倾听
        // WebSocket连接会在 sendToAI 时自动建立
        startListening()

        // 开始通话计时
        startTimer()
    }

    /**
     * 结束通话
     */
    fun endCall() {
        Log.d(TAG, "结束通话")

        // 停止所有语音活动
        stopListening()
        stopSpeaking()

        // 停止计时
        stopTimer()

        // 断开WebSocket
        webSocketRepository.disconnect()

        // 更新状态
        _callState.update {
            it.copy(
                phase = CallPhase.Ended,
                lastUserSpeech = "",
                lastAiReply = "",
                conversationHistory = emptyList() // 清空对话历史
            )
        }
    }

    /**
     * 切换静音状态
     */
    fun toggleMute() {
        _callState.update {
            it.copy(isMuted = !it.isMuted)
        }

        if (_callState.value.isMuted) {
            // 静音：停止TTS
            stopSpeaking()
        }
    }

    /**
     * 设置权限被拒绝状态
     */
    fun setPermissionDenied(denied: Boolean) {
        if (denied) {
            _callState.update {
                it.copy(
                    errorMessage = "需要麦克风权限",
                    phase = CallPhase.Ended
                )
            }
            endCall()
        }
    }

    /**
     * 打断AI说话
     * 当用户开始说话时调用此方法
     */
    fun interruptAi() {
        if (_callState.value.phase != CallPhase.Speaking) {
            return
        }

        Log.d(TAG, "用户打断AI说话")

        // 停止TTS播放
        stopSpeaking()

        // 清空累积的AI回复
        currentAiReply.clear()
        isWaitingForAiResponse = false

        // 重新开始听用户说话
        startListening()
    }

    /**
     * 开始倾听（启动ASR）
     */
    private fun startListening() {
        Log.d(TAG, "========== 开始倾听 ==========")
        Log.d(TAG, "当前通话状态: ${_callState.value.phase}")
        Log.d(TAG, "通话是否在进行: ${_callState.value.isInCall}")

        _callState.update {
            it.copy(
                phase = CallPhase.Listening,
                isConnecting = false,
                errorMessage = null
            )
        }

        // 开始语音识别
        Log.d(TAG, "调用speakManager.startRecordingAndRecognition()启动录音识别")
        speakManager.startRecordingAndRecognition()
    }

    /**
     * 停止倾听（停止ASR）
     */
    private fun stopListening() {
        Log.d(TAG, "========== 停止倾听 ==========")
        Log.d(TAG, "当前通话状态: ${_callState.value.phase}")
        Log.d(TAG, "调用speakManager.stopRecordingAndRecognition()停止录音识别")
        speakManager.stopRecordingAndRecognition()
    }

    /**
     * 停止TTS播放
     */
    private fun stopSpeaking() {
        Log.d(TAG, "停止TTS播放")
        speakManager.stopSynthesis()
    }

    /**
     * 开始通话计时
     */
    private fun startTimer() {
        callStartTime = System.currentTimeMillis()

        timerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - callStartTime
                _callState.update {
                    it.copy(callDurationMs = elapsed)
                }
                delay(1000)
            }
        }
    }

    /**
     * 停止通话计时
     */
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * 将用户语音发送给AI
     * 使用短连接：每次发送消息时建立连接，收到响应后自动断开
     */
    private fun sendToAI(text: String) {
        if (text.isBlank()) {
            // 空消息，重新开始倾听
            startListening()
            return
        }

        Log.d(TAG, "发送用户消息给AI: $text")

        // 保存用户说的内容
        _callState.update {
            it.copy(lastUserSpeech = text)
        }

        // 进入思考状态
        _callState.update {
            it.copy(phase = CallPhase.Thinking)
        }

        // 清空之前的回复
        currentAiReply.clear()
        isWaitingForAiResponse = true

        // 获取对话历史
        val history = _callState.value.conversationHistory.map { it.role to it.content }

        // 直接发送消息 - ChatWebSocketManager 会自动处理连接的建立和断开
        // 短连接模式：建立连接 -> 发送消息 -> 等待响应 -> 自动断开
        val messageId = java.util.UUID.randomUUID().toString()
        currentMessageId = messageId  // 记录当前消息ID
        webSocketRepository.sendMessage(
            content = text,
            messageId = messageId,
            thinkingEnabled = false,
            searchEnabled = false,
            attachments = emptyList(),
            conversationHistory = history,
        )
    }

    /**
     * AI内容片段回调（流式）
     */
    private fun onAiContentChunk(content: String) {
        currentAiReply.append(content)
    }

    /**
     * AI回复完成
     */
    private fun onAiReplyComplete() {
        val replyText = currentAiReply.toString()

        Log.d(TAG, "AI回复完成: $replyText")

        // 获取本轮的用户输入
        val userInput = _callState.value.lastUserSpeech

        // 保存AI回复和对话历史
        _callState.update { state ->
            val newHistory = state.conversationHistory.toMutableList()
            // 添加用户消息
            if (userInput.isNotBlank()) {
                newHistory.add(ChatMessageRecord(role = "user", content = userInput))
            }
            // 添加AI回复
            if (replyText.isNotBlank()) {
                newHistory.add(ChatMessageRecord(role = "assistant", content = replyText))
            }
            state.copy(
                lastAiReply = replyText,
                conversationHistory = newHistory
            )
        }

        Log.d(TAG, "对话历史已更新，当前共 ${_callState.value.conversationHistory.size} 条消息")

        isWaitingForAiResponse = false

        // 开始TTS播放
        if (replyText.isNotBlank() && !_callState.value.isMuted) {
            _callState.update {
                it.copy(phase = CallPhase.Speaking)
            }
            speakManager.startSynthesis(replyText)
        } else {
            // 静音或空消息，直接重新倾听（连接保持，无需重连）
            startListening()
        }
    }

    /**
     * AI响应错误
     */
    private fun onAiError(errorMessage: String) {
        Log.e(TAG, "AI响应错误: $errorMessage")

        isWaitingForAiResponse = false

        _callState.update {
            it.copy(
                errorMessage = errorMessage,
                phase = CallPhase.Listening
            )
        }

        // 重新开始倾听
        startListening()
    }

    // ==================== SpeakCallback 实现 ====================

    override fun onAsrResult(text: String, isFinal: Boolean) {
        Log.d(TAG, "========== ASR结果回调 ==========")
        Log.d(TAG, "识别文本: [$text]")
        Log.d(TAG, "是否最终结果: $isFinal")
        Log.d(TAG, "当前通话状态: ${_callState.value.phase}")
        Log.d(TAG, "当前通话时长: ${_callState.value.callDurationMs}ms")

        if (text.isBlank()) {
            Log.w(TAG, "识别文本为空，可能是用户没有说话或噪音太大")
            if (isFinal) {
                // 空结果，重新开始倾听
                startListening()
            }
            return
        }

        Log.d(TAG, "有效识别结果，准备发送给AI")

        if (isFinal) {
            // 识别完成，停止录音
            Log.d(TAG, "识别完成，停止录音")
            stopListening()

            // 发送给AI
            Log.d(TAG, "调用sendToAI发送识别结果")
            sendToAI(text)
        } else {
            // 中间结果，可以更新UI显示实时识别内容
            Log.d(TAG, "识别中...显示中间结果: $text")
            _callState.update {
                it.copy(lastUserSpeech = text)
            }
        }
    }

    override fun onAsrError(errorCode: Int, errorMessage: String) {
        Log.e(TAG, "========== ASR错误回调 ==========")
        Log.e(TAG, "错误码: $errorCode")
        Log.e(TAG, "错误信息: $errorMessage")
        Log.e(TAG, "当前通话状态: ${_callState.value.phase}")

        _callState.update {
            it.copy(errorMessage = "识别错误: $errorMessage")
        }

        // 出错后重新开始倾听
        Log.d(TAG, "500ms后尝试重新开始倾听")
        viewModelScope.launch {
            delay(500)
            if (_callState.value.phase == CallPhase.Listening) {
                Log.d(TAG, "重新启动倾听")
                startListening()
            }
        }
    }

    override fun onTtsStart() {
        Log.d(TAG, "TTS开始播放")
    }

    override fun onTtsData(audioData: ByteArray) {
        // 流式TTS数据回调
    }

    override fun onTtsComplete() {
        Log.d(TAG, "TTS播放完成")

        // TTS播放完成，连接保持，直接重新倾听
        startListening()
    }

    override fun onTtsError(errorCode: Int, errorMessage: String) {
        Log.e(TAG, "TTS错误: $errorCode, $errorMessage")

        _callState.update {
            it.copy(errorMessage = "语音播放错误: $errorMessage")
        }

        // 播放出错，重新开始倾听
        startListening()
    }

    override fun onRecordingStarted() {
        Log.d(TAG, "开始录音")
    }

    override fun onRecordingStopped() {
        Log.d(TAG, "停止录音")
    }

    override fun onCleared() {
        super.onCleared()
        // 清理资源
        stopTimer()
        speakManager.release()
    }
}
