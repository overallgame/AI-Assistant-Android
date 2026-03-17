package com.example.aiassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiassistant.data.model.AttachmentTransferStatus
import com.example.aiassistant.data.model.ChatFileType
import com.example.aiassistant.data.model.ChatMessage
import com.example.aiassistant.data.model.ChatMessagePart
import com.example.aiassistant.data.model.ChatRole
import com.example.aiassistant.data.model.ChatUiState
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
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

/**
 * 待处理的流式更新项
 */
private data class PendingStreamUpdate(
    val messageId: String,
    val content: String = "",
    val reasoning: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val webSocketRepository: ChatWebSocketRepository,
    private val speakManager: SpeakManager,
) : ViewModel(), SpeakCallback {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<WebSocketConnectionState>
        get() = webSocketRepository.connectionState

    // 录音状态
    val isRecording: StateFlow<Boolean>
        get() = speakManager.isRecordingStateFlow()

    private val attachmentJobs = mutableMapOf<String, Job>()

    // 流式更新缓冲（用于批量合并更新）
    private val pendingStreamUpdates = mutableMapOf<String, PendingStreamUpdate>()
    private var streamFlushJob: Job? = null

    // 消息索引缓存（避免重复遍历）
    private var messageIndexCache: Map<String, Int> = emptyMap()

    init {
        observeWebSocketEvents()

        // 初始化语音管理器，设置TTS为自动切换模式
        speakManager.setTtsSwitchStrategy(SwitchStrategy.AUTO)
        // 设置语音识别回调
        speakManager.setCallback(this)
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            try {
                webSocketRepository.events.collect { event ->
                    when (event) {
                        is WebSocketEvent.Error -> {
                            _uiState.update {
                                it.copy(
                                    errorMessage = event.message,
                                    isSending = false
                                )
                            }
                        }

                        is WebSocketEvent.Connected -> {
                            _uiState.update { it.copy(errorMessage = null) }
                        }

                        is WebSocketEvent.Disconnected -> {
                            _uiState.update { it.copy(isSending = false) }
                        }
                        is WebSocketEvent.Reconnecting -> {
                            _uiState.update { it.copy(errorMessage = "正在重新连接...") }
                        }
                        // 星火大模型事件 - 合并处理
                        is WebSocketEvent.XinghuoReasoningChunkReceived -> {
                            bufferStreamUpdate(event.messageId, reasoning = event.reasoning)
                        }

                        is WebSocketEvent.XinghuoContentChunkReceived -> {
                            bufferStreamUpdate(event.messageId, content = event.content)
                        }

                        is WebSocketEvent.XinghuoDoneReceived -> {
                            // 立即刷新缓冲区
                            flushStreamUpdates()
                            finishStreamingMessage(event.messageId)
                        }

                        is WebSocketEvent.XinghuoError -> {
                            _uiState.update {
                                it.copy(
                                    errorMessage = "星火错误[${event.code}]: ${event.message}",
                                    isSending = false
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "事件监听错误: ${e.message}", isSending = false)
                }
            }
        }
    }

    /**
     * 缓冲流式更新，批量合并多个更新
     */
    private fun bufferStreamUpdate(
        messageId: String,
        content: String = "",
        reasoning: String = ""
    ) {
        val current = pendingStreamUpdates[messageId] ?: PendingStreamUpdate(messageId)
        pendingStreamUpdates[messageId] = current.copy(
            content = current.content + content,
            reasoning = current.reasoning + reasoning
        )

        // 调度批量刷新（防抖）
        if (streamFlushJob?.isActive != true) {
            streamFlushJob = viewModelScope.launch {
                delay(50) // 50ms 防抖
                flushStreamUpdates()
            }
        }
    }

    /**
     * 批量刷新流式更新到 UI
     */
    private fun flushStreamUpdates() {
        if (pendingStreamUpdates.isEmpty()) return

        val updates = pendingStreamUpdates.toMap()
        pendingStreamUpdates.clear()

        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            var messagesChanged = false

            for ((messageId, update) in updates) {
                val idx = newMessages.indexOfFirst { it.id == messageId }

                if (idx >= 0) {
                    // 更新现有消息
                    val existingMsg = newMessages[idx]
                    val newParts = existingMsg.parts.map { part ->
                        if (part is ChatMessagePart.Text) {
                            part.copy(
                                text = part.text + update.content,
                                reasoning = part.reasoning + update.reasoning
                            )
                        } else {
                            part
                        }
                    }
                    newMessages[idx] = existingMsg.copy(parts = newParts, isStreaming = true)
                    messagesChanged = true
                } else {
                    // 新建消息（首次接收）
                    val newMessage = ChatMessage(
                        id = messageId,
                        role = ChatRole.Assistant,
                        parts = listOf(ChatMessagePart.Text(update.content, update.reasoning)),
                        isStreaming = true,
                    )
                    newMessages.add(newMessage)
                    messagesChanged = true
                }
            }

            if (messagesChanged) {
                // 更新索引缓存
                messageIndexCache = newMessages.mapIndexed { index, msg -> msg.id to index }.toMap()
            }

            state.copy(messages = newMessages, isSending = false)
        }
    }

    private fun disconnect() {
        webSocketRepository.disconnect()
    }

    fun newChat() {
        _uiState.update {
            it.copy(
                messages = emptyList(),
                inputText = "",
                systemPrompt = "",
                errorMessage = null
            )
        }
        attachmentJobs.values.forEach { it.cancel() }
        attachmentJobs.clear()
        pendingStreamUpdates.clear()
        messageIndexCache = emptyMap()
    }

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun toggleThinking() {
        _uiState.update { it.copy(mode = it.mode.copy(thinkingEnabled = !it.mode.thinkingEnabled)) }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(mode = it.mode.copy(searchEnabled = !it.mode.searchEnabled)) }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        val content = currentState.inputText.trim()

        if (content.isBlank() && currentState.messages.isEmpty()) {
            return
        }

        val userMessageId = UUID.randomUUID().toString()
        val userMessage = ChatMessage(
            id = userMessageId,
            role = ChatRole.User,
            parts = if (content.isNotBlank()) listOf(ChatMessagePart.Text(content)) else emptyList(),
        )

        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            newMessages.add(userMessage)
            // 更新索引缓存
            messageIndexCache = newMessages.mapIndexed { index, msg -> msg.id to index }.toMap()
            state.copy(
                messages = newMessages,
                inputText = "",
                isSending = true,
            )
        }

        val messageId = UUID.randomUUID().toString()

        // 构建对话历史（用于多轮对话）
        val history = mutableListOf<Pair<String, String>>()

        // 添加 system 消息（如果存在）
        val systemPrompt = currentState.systemPrompt
        if (systemPrompt.isNotBlank()) {
            history.add("system" to systemPrompt)
        }

        // 添加对话历史
        currentState.messages.forEach { msg ->
            val textParts = msg.parts.filterIsInstance<ChatMessagePart.Text>()
            if (textParts.isEmpty()) return@forEach

            val msgContent = textParts.joinToString("") { it.text }
            val role = when (msg.role) {
                ChatRole.System -> "system"
                ChatRole.User -> "user"
                ChatRole.Assistant -> "assistant"
            }
            history.add(role to msgContent)
        }

        webSocketRepository.sendMessage(
            content = content,
            messageId = messageId,
            conversationHistory = history,
            thinkingEnabled = currentState.mode.thinkingEnabled,
            searchEnabled = currentState.mode.searchEnabled,
        )
    }

    fun sendImage(
        contentUri: String,
        mimeType: String? = null,
        widthPx: Int? = null,
        heightPx: Int? = null,
    ) {
        val message = ChatMessage(
            role = ChatRole.User,
            parts = listOf(
                ChatMessagePart.Image(
                    contentUri = contentUri,
                    mimeType = mimeType,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    transferStatus = AttachmentTransferStatus.Uploading,
                    progress = 0f,
                ),
            ),
        )

        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            newMessages.add(message)
            messageIndexCache = newMessages.mapIndexed { index, msg -> msg.id to index }.toMap()
            state.copy(messages = newMessages)
        }

        startSimulateAttachmentFlow(messageId = message.id, contentUri = contentUri)
    }

    fun sendFile(
        contentUri: String,
        fileName: String,
        mimeType: String? = null,
        sizeBytes: Long? = null,
    ) {
        val message = ChatMessage(
            role = ChatRole.User,
            parts = listOf(
                ChatMessagePart.File(
                    contentUri = contentUri,
                    fileName = fileName,
                    fileType = guessFileType(fileName = fileName, mimeType = mimeType),
                    mimeType = mimeType,
                    sizeBytes = sizeBytes,
                    transferStatus = AttachmentTransferStatus.Uploading,
                    progress = 0f,
                ),
            ),
        )

        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            newMessages.add(message)
            messageIndexCache = newMessages.mapIndexed { index, msg -> msg.id to index }.toMap()
            state.copy(messages = newMessages)
        }

        startSimulateAttachmentFlow(messageId = message.id, contentUri = contentUri)
    }

    fun retryAttachment(messageId: String) {
        val msg = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val targetUri = when (val p = msg.parts.firstOrNull()) {
            is ChatMessagePart.Image -> p.contentUri
            is ChatMessagePart.File -> p.contentUri
            else -> return
        }

        updateAttachmentPart(
            messageId = messageId,
            contentUri = targetUri,
            status = AttachmentTransferStatus.Uploading,
            progress = 0f,
        )

        startSimulateAttachmentFlow(messageId = messageId, contentUri = targetUri)
    }

    private fun guessFileType(fileName: String, mimeType: String?): ChatFileType {
        val name = fileName.lowercase()
        return when {
            mimeType == "application/pdf" || name.endsWith(".pdf") -> ChatFileType.Pdf
            mimeType == "text/csv" || name.endsWith(".csv") -> ChatFileType.Csv
            mimeType == "application/msword" ||
                    mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                    name.endsWith(".doc") ||
                    name.endsWith(".docx") -> ChatFileType.Word

            mimeType?.startsWith("image/") == true -> ChatFileType.Image
            else -> ChatFileType.Other
        }
    }

    // ==================== 语音识别相关 ====================

    fun startVoiceRecognition(): Boolean {
        return speakManager.startRecordingAndRecognition()
    }

    fun stopVoiceRecognition() {
        speakManager.stopRecordingAndRecognition()
    }

    override fun onAsrResult(text: String, isFinal: Boolean) {
        _uiState.update { it.copy(inputText = text) }
    }

    override fun onAsrError(errorCode: Int, errorMessage: String) {
        _uiState.update { it.copy(errorMessage = "语音识别错误: $errorMessage") }
    }

    override fun onRecordingStarted() {}
    override fun onRecordingStopped() {}
    override fun onTtsStart() {}
    override fun onTtsData(audioData: ByteArray) {}
    override fun onTtsComplete() {}
    override fun onTtsError(errorCode: Int, errorMessage: String) {}

    // 流式输出完成
    private fun finishStreamingMessage(messageId: String) {
        _uiState.update { state ->
            val idx = state.messages.indexOfFirst { it.id == messageId }
            if (idx < 0) return@update state

            val newMessages = state.messages.toMutableList()
            val existingMsg = newMessages[idx]
            newMessages[idx] = existingMsg.copy(isStreaming = false)

            state.copy(
                messages = newMessages,
                isSending = false,
            )
        }
    }

    private fun startSimulateAttachmentFlow(messageId: String, contentUri: String) {
        attachmentJobs.remove(messageId)?.cancel()

        attachmentJobs[messageId] = viewModelScope.launch {
            var p = 0f
            while (p < 1f) {
                delay(180)
                p = (p + 0.12f).coerceAtMost(1f)
                updateAttachmentPart(
                    messageId = messageId,
                    contentUri = contentUri,
                    status = AttachmentTransferStatus.Uploading,
                    progress = p,
                )
            }

            updateAttachmentPart(
                messageId = messageId,
                contentUri = contentUri,
                status = AttachmentTransferStatus.Processing,
                progress = null,
            )

            delay(650)

            val ok = Random.nextFloat() >= 0.15f
            updateAttachmentPart(
                messageId = messageId,
                contentUri = contentUri,
                status = if (ok) AttachmentTransferStatus.Done else AttachmentTransferStatus.Failed,
                progress = null,
            )
        }
    }

    private fun updateAttachmentPart(
        messageId: String,
        contentUri: String,
        status: AttachmentTransferStatus,
        progress: Float?,
    ) {
        _uiState.update { state ->
            val idx = state.messages.indexOfFirst { it.id == messageId }
            if (idx < 0) return@update state

            val msg = state.messages[idx]
            val newParts = msg.parts.map { part ->
                when (part) {
                    is ChatMessagePart.Image ->
                        if (part.contentUri == contentUri) {
                            part.copy(transferStatus = status, progress = progress)
                        } else {
                            part
                        }

                    is ChatMessagePart.File ->
                        if (part.contentUri == contentUri) {
                            part.copy(transferStatus = status, progress = progress)
                        } else {
                            part
                        }

                    else -> part
                }
            }

            val newMessages = state.messages.toMutableList()
            newMessages[idx] = msg.copy(parts = newParts)
            state.copy(messages = newMessages)
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
