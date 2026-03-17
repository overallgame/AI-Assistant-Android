package com.example.aiassistant.speak

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 语音统一管理器 - 整合ASR和TTS功能
 */
class SpeakManager(context: Context) {

    companion object {
        private const val TAG = "SpeakManager"

        // 错误码
        const val PERMISSION_DENIED = -1001  // 权限被拒绝
    }

    private val asrManager: AsrManager = AsrManager()
    private val ttsManager: TtsManager = TtsManager(context)
    private val audioRecorderManager: AudioRecorderManager = AudioRecorderManager(context)

    private var callback: SpeakCallback? = null
    private var isRecording = false

    // 状态Flow
    private val _isSpeakingFlow = MutableStateFlow(false)
    private val _isRecordingFlow = MutableStateFlow(false)

    /**
     * 初始化所有语音模块
     */
    fun init() {
        asrManager.init()
        Log.d(TAG, "SpeakManager初始化完成")
    }

    /**
     * 设置回调
     */
    fun setCallback(callback: SpeakCallback) {
        this.callback = callback

        // 包装回调，同时更新Flow状态
        val wrappedCallback = object : SpeakCallback {
            override fun onAsrResult(text: String, isFinal: Boolean) {
                _isRecordingFlow.value = false
                callback.onAsrResult(text, isFinal)
            }

            override fun onAsrError(errorCode: Int, errorMessage: String) {
                _isRecordingFlow.value = false
                callback.onAsrError(errorCode, errorMessage)
            }

            override fun onTtsStart() {
                _isSpeakingFlow.value = true
                callback.onTtsStart()
            }

            override fun onTtsData(audioData: ByteArray) {
                callback.onTtsData(audioData)
            }

            override fun onTtsComplete() {
                _isSpeakingFlow.value = false
                callback.onTtsComplete()
            }

            override fun onTtsError(errorCode: Int, errorMessage: String) {
                _isSpeakingFlow.value = false
                callback.onTtsError(errorCode, errorMessage)
            }

            override fun onRecordingStarted() {
                _isRecordingFlow.value = true
                callback.onRecordingStarted()
            }

            override fun onRecordingStopped() {
                _isRecordingFlow.value = false
                callback.onRecordingStopped()
            }
        }

        asrManager.setCallback(wrappedCallback)
        ttsManager.setCallback(wrappedCallback)
    }

    /**
     * 开始语音识别（同时启动录音）
     * 需要RECORD_AUDIO权限
     * @return true表示成功开始
     */
    fun startRecordingAndRecognition(): Boolean {
        if (isRecording) {
            Log.w(TAG, "正在录音中")
            return false
        }

        // 检查录音权限
        if (!hasRecordPermission()) {
            Log.w(TAG, "没有录音权限")
            callback?.onAsrError(PERMISSION_DENIED, "没有录音权限")
            return false
        }

        // 启动ASR识别
        val asrStarted = asrManager.startRecognition()
        if (!asrStarted) {
            Log.e(TAG, "启动ASR失败")
            return false
        }

        // 启动录音
        audioRecorderManager.setAudioDataCallback { audioData ->
            asrManager.writeAudioData(audioData)
        }

        val recorderStarted = audioRecorderManager.startRecording()
        if (!recorderStarted) {
            Log.e(TAG, "启动录音失败")
            asrManager.stopRecognition(true)
            return false
        }

        isRecording = true
        callback?.onRecordingStarted()
        Log.d(TAG, "开始录音识别")
        return true
    }

    /**
     * 停止录音和识别
     */
    fun stopRecordingAndRecognition() {
        if (!isRecording) {
            return
        }

        audioRecorderManager.stopRecording()
        asrManager.stopRecognition(false)
        isRecording = false
        callback?.onRecordingStopped()
        Log.d(TAG, "停止录音识别")
    }

    /**
     * 开始语音合成并播放
     * @param text 要合成的文本
     */
    fun startSynthesis(text: String) {
        _isSpeakingFlow.value = true
        ttsManager.speak(text)
    }

    /**
     * 停止语音合成
     */
    fun stopSynthesis() {
        _isSpeakingFlow.value = false
        ttsManager.stop()
    }

    /**
     * 暂停语音合成
     */
    fun pauseSynthesis() {
        ttsManager.pause()
    }

    /**
     * 恢复语音合成
     */
    fun resumeSynthesis() {
        ttsManager.resume()
    }

    /**
     * 设置TTS发音人
     */
    fun setTtsVoice(voiceName: String) {
        ttsManager.setVoice(voiceName)
    }

    /**
     * 设置TTS语速
     */
    fun setTtsSpeed(speed: Int) {
        ttsManager.setSpeed(speed)
    }

    /**
     * 设置TTS语调
     */
    fun setTtsPitch(pitch: Int) {
        ttsManager.setPitch(pitch)
    }

    /**
     * 设置TTS音量
     */
    fun setTtsVolume(volume: Int) {
        ttsManager.setVolume(volume)
    }

    /**
     * 设置ASR语言
     */
    fun setAsrLanguage(language: String) {
        asrManager.setLanguage(language)
    }

    // ==================== TTS模式设置 ====================

    /**
     * 设置TTS模式（在线/离线）
     * @param mode TtsMode.ONLINE 或 TtsMode.OFFLINE
     */
    fun setTtsMode(mode: TtsMode) {
        ttsManager.setTtsMode(mode)
    }

    /**
     * 设置TTS切换策略
     * @param strategy SwitchStrategy 切换策略
     *   - ONLINE_FIRST: 优先在线，失败时切换离线
     *   - OFFLINE_FIRST: 优先离线，失败时切换在线
     *   - AUTO: 根据网络自动选择
     */
    fun setTtsSwitchStrategy(strategy: SwitchStrategy) {
        ttsManager.setSwitchStrategy(strategy)
    }

    /**
     * 检查离线TTS是否可用
     * 需要Android系统已下载离线语言包
     */
    fun isOfflineTTSAvailable(): Boolean {
        return ttsManager.isOfflineTTSAvailable()
    }

    /**
     * 获取当前TTS模式
     */
    fun getCurrentTtsMode(): TtsMode {
        return ttsManager.getCurrentMode()
    }

    /**
     * 检查录音权限
     */
    private fun hasRecordPermission(): Boolean {
        return audioRecorderManager.hasRecordPermission()
    }

    /**
     * 是否正在录音识别
     */
    fun isRecording(): Boolean = isRecording

    /**
     * 是否正在合成播放（作为StateFlow）
     */
    fun isSpeakingStateFlow(): StateFlow<Boolean> = _isSpeakingFlow.asStateFlow()

    /**
     * 是否正在录音识别（作为StateFlow）
     */
    fun isRecordingStateFlow(): StateFlow<Boolean> = _isRecordingFlow.asStateFlow()

    /**
     * 释放所有资源
     */
    fun release() {
        stopRecordingAndRecognition()
        stopSynthesis()
        audioRecorderManager.release()
        asrManager.release()
        ttsManager.release()
        callback = null
        Log.d(TAG, "SpeakManager资源已释放")
    }
}
