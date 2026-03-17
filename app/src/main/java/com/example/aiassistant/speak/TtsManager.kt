package com.example.aiassistant.speak

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.iflytek.sparkchain.core.tts.OnlineTTS
import com.iflytek.sparkchain.core.tts.TTS
import com.iflytek.sparkchain.core.tts.TTSCallbacks
import java.util.Locale
import java.util.UUID

/**
 * 混合TTS管理器
 * 支持在线（讯飞SDK）和离线（系统TTS）自动切换
 */
class TtsManager(private val context: Context) {

    companion object {
        private const val TAG = "TtsManager"

        // 音频参数
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    // TTS模式配置
    private var currentMode: TtsMode = TtsMode.ONLINE
    private var switchStrategy: SwitchStrategy = SwitchStrategy.ONLINE_FIRST

    // 在线TTS
    private var onlineTTS: OnlineTTS? = null

    // 离线TTS（系统TTS）
    private var systemTTS: TextToSpeech? = null
    private var isSystemTTSReady = false
    private var systemTTSLanguage = Locale.CHINESE

    // 回调
    private var callback: SpeakCallback? = null

    // 状态
    private var isSpeaking = false
    private var isPaused = false

    // 在线TTS参数
    private var vcn = "xiaoyan"
    private var speed = 50
    private var pitch = 50
    private var volume = 100

    // 离线TTS参数
    private var offlineSpeed = 1.0f
    private var offlinePitch = 1.0f

    // 播放相关（在线TTS用）
    private var audioTrack: AudioTrack? = null
    private var isPlayingAudio = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // 待播报的文本队列（用于切换后继续播放）
    private var pendingText: String = ""

    init {
        initSystemTTS()
    }

    /**
     * 初始化系统TTS
     */
    private fun initSystemTTS() {
        systemTTS = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // 设置中文语言
                val result = systemTTS?.setLanguage(systemTTSLanguage)
                isSystemTTSReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED

                if (isSystemTTSReady) {
                    Log.d(TAG, "系统TTS初始化成功")
                    setupSystemTTSListener()
                } else {
                    Log.w(TAG, "系统TTS语言不支持，需要下载离线语言包")
                }
            } else {
                Log.e(TAG, "系统TTS初始化失败: $status")
            }
        }
    }

    /**
     * 设置系统TTS监听器
     */
    private fun setupSystemTTSListener() {
        systemTTS?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                mainHandler.post {
                    callback?.onTtsStart()
                }
                Log.d(TAG, "系统TTS开始播放")
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                isPaused = false
                mainHandler.post {
                    callback?.onTtsComplete()
                }
                Log.d(TAG, "系统TTS播放完成")
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                mainHandler.post {
                    callback?.onTtsError(-1, "系统TTS播放错误")
                }
                Log.e(TAG, "系统TTS播放错误")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeaking = false
                mainHandler.post {
                    callback?.onTtsError(errorCode, "系统TTS播放错误")
                }
                Log.e(TAG, "系统TTS播放错误: $errorCode")
            }
        })
    }

    /**
     * 设置回调
     */
    fun setCallback(callback: SpeakCallback) {
        this.callback = callback
    }

    /**
     * 设置TTS模式
     */
    fun setTtsMode(mode: TtsMode) {
        this.currentMode = mode
        Log.d(TAG, "设置TTS模式: $mode")
    }

    /**
     * 设置切换策略
     */
    fun setSwitchStrategy(strategy: SwitchStrategy) {
        this.switchStrategy = strategy
        Log.d(TAG, "设置切换策略: $strategy")
    }

    /**
     * 设置发音人（在线TTS）
     */
    fun setVoice(voiceName: String) {
        vcn = voiceName
    }

    /**
     * 设置语速
     * @param speed 语速 0-100
     */
    fun setSpeed(speed: Int) {
        this.speed = speed.coerceIn(0, 100)
        // 转换到系统TTS的语速范围 (0.5 - 2.0)
        this.offlineSpeed = 0.5f + (this.speed / 100f) * 1.5f
    }

    /**
     * 设置语调
     * @param pitch 语调 0-100
     */
    fun setPitch(pitch: Int) {
        this.pitch = pitch.coerceIn(0, 100)
        // 转换到系统TTS的语调范围 (0.5 - 2.0)
        this.offlinePitch = 0.5f + (this.pitch / 100f) * 1.5f
    }

    /**
     * 设置音量
     */
    fun setVolume(volume: Int) {
        this.volume = volume.coerceIn(0, 100)
    }

    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * 检查系统TTS是否可用
     */
    fun isOfflineTTSAvailable(): Boolean {
        return isSystemTTSReady
    }

    /**
     * 合成并播放文本
     * 根据策略自动选择在线或离线
     */
    fun speak(text: String) {
        if (text.isBlank()) {
            Log.w(TAG, "合成文本为空")
            return
        }

        pendingText = text

        // 根据策略选择TTS模式
        val selectedMode = selectTtsMode()
        Log.d(TAG, "选择TTS模式: $selectedMode")

        when (selectedMode) {
            TtsMode.ONLINE -> speakOnline(text)
            TtsMode.OFFLINE -> speakOffline(text)
        }
    }

    /**
     * 根据策略选择TTS模式
     */
    private fun selectTtsMode(): TtsMode {
        return when (switchStrategy) {
            SwitchStrategy.ONLINE_FIRST -> {
                if (isNetworkAvailable()) TtsMode.ONLINE else TtsMode.OFFLINE
            }

            SwitchStrategy.OFFLINE_FIRST -> {
                if (isSystemTTSReady) TtsMode.OFFLINE else TtsMode.ONLINE
            }

            SwitchStrategy.AUTO -> {
                if (isNetworkAvailable()) TtsMode.ONLINE else TtsMode.OFFLINE
            }
        }
    }

    /**
     * 在线TTS播报
     */
    private fun speakOnline(text: String) {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "网络不可用，切换到离线TTS")
            speakOffline(text)
            return
        }

        try {
            if (isSpeaking) {
                stop()
            }

            // 初始化AudioTrack
            initAudioTrack()

            // 配置在线TTS
            onlineTTS = OnlineTTS(vcn).apply {
                speed(this@TtsManager.speed)
                pitch(this@TtsManager.pitch)
                volume(this@TtsManager.volume)
                registerCallbacks(ttsCallbacks)
            }

            // 开始合成
            val ret = onlineTTS?.aRun(text) ?: -1

            if (ret == 0) {
                isSpeaking = true
                currentMode = TtsMode.ONLINE
                mainHandler.post {
                    callback?.onTtsStart()
                }
                Log.d(TAG, "开始在线TTS合成: $text")
            } else {
                Log.e(TAG, "在线TTS启动失败，错误码: $ret，切换到离线TTS")
                speakOffline(text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "在线TTS异常: ${e.message}，切换到离线TTS")
            speakOffline(text)
        }
    }

    /**
     * 离线TTS播报
     */
    private fun speakOffline(text: String) {
        if (!isSystemTTSReady) {
            Log.e(TAG, "系统TTS不可用")
            mainHandler.post {
                callback?.onTtsError(-1, "离线TTS不可用")
            }
            return
        }

        try {
            if (isSpeaking) {
                systemTTS?.stop()
            }

            // 设置参数
            systemTTS?.setSpeechRate(offlineSpeed)
            systemTTS?.setPitch(offlinePitch)

            // 设置音量 (0.0-1.0)
            val volumeFloat = this.volume / 100f

            // 创建唯一ID
            val utteranceId = UUID.randomUUID().toString()

            // 播放
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeFloat)
            }

            isSpeaking = true
            currentMode = TtsMode.OFFLINE

            systemTTS?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)

            Log.d(TAG, "开始离线TTS合成: $text")
        } catch (e: Exception) {
            Log.e(TAG, "离线TTS异常: ${e.message}")
            mainHandler.post {
                callback?.onTtsError(-1, "离线TTS播放失败: ${e.message}")
            }
        }
    }

    /**
     * 停止播放
     */
    fun stop() {
        try {
            isSpeaking = false
            isPaused = false

            when (currentMode) {
                TtsMode.ONLINE -> {
                    onlineTTS?.stop()
                    onlineTTS = null
                    stopAudioPlayback()
                }

                TtsMode.OFFLINE -> {
                    systemTTS?.stop()
                }
            }

            Log.d(TAG, "停止TTS")
        } catch (e: Exception) {
            Log.e(TAG, "停止TTS异常: ${e.message}")
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        when (currentMode) {
            TtsMode.ONLINE -> {
                try {
                    isPaused = true
                    isPlayingAudio = false
                    audioTrack?.pause()
                    Log.d(TAG, "暂停在线TTS")
                } catch (e: Exception) {
                    Log.e(TAG, "暂停在线TTS异常: ${e.message}")
                }
            }

            TtsMode.OFFLINE -> {
                systemTTS?.stop()
                isPaused = true
                isSpeaking = false
                Log.d(TAG, "暂停离线TTS")
            }
        }
    }

    /**
     * 恢复播放
     */
    fun resume() {
        if (!isPaused || pendingText.isBlank()) return

        when (currentMode) {
            TtsMode.ONLINE -> {
                try {
                    isPaused = false
                    isPlayingAudio = true
                    audioTrack?.play()
                    Log.d(TAG, "恢复在线TTS")
                } catch (e: Exception) {
                    Log.e(TAG, "恢复在线TTS异常: ${e.message}")
                }
            }

            TtsMode.OFFLINE -> {
                speak(pendingText)
            }
        }
    }

    /**
     * 是否正在播放
     */
    fun isSpeaking(): Boolean = isSpeaking

    /**
     * 获取当前TTS模式
     */
    fun getCurrentMode(): TtsMode = currentMode

    /**
     * 释放资源
     */
    fun release() {
        stop()

        // 释放在线TTS
        onlineTTS?.stop()
        onlineTTS = null

        // 释放系统TTS
        systemTTS?.stop()
        systemTTS?.shutdown()
        systemTTS = null

        // 释放AudioTrack
        audioTrack?.release()
        audioTrack = null

        callback = null
        Log.d(TAG, "TTS资源已释放")
    }

    /**
     * 初始化AudioTrack
     */
    private fun initAudioTrack() {
        try {
            val minBufferSize =
                AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = minBufferSize * 2

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            isPlayingAudio = true
            audioTrack?.play()
            Log.d(TAG, "AudioTrack初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack初始化失败: ${e.message}")
            mainHandler.post {
                callback?.onTtsError(-1, "音频播放初始化失败")
            }
        }
    }

    /**
     * 停止音频播放
     */
    private fun stopAudioPlayback() {
        try {
            isPlayingAudio = false
            audioTrack?.stop()
            audioTrack?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "停止音频播放异常: ${e.message}")
        }
    }

    /**
     * 在线TTS回调实现
     */
    private val ttsCallbacks = object : TTSCallbacks {
        override fun onResult(ttsResult: TTS.TTSResult, obj: Any?) {
            val audioData = ttsResult.data
            val len = ttsResult.len
            val status = ttsResult.status

            Log.d(TAG, "在线TTS结果: status=$status, len=$len")

            if (audioData != null && audioData.isNotEmpty()) {
                try {
                    if (isPlayingAudio && audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                        audioTrack?.write(audioData, 0, audioData.size)
                    }
                    mainHandler.post {
                        callback?.onTtsData(audioData)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "在线TTS音频播放异常: ${e.message}")
                }
            }

            if (status == 2) {
                isSpeaking = false
                mainHandler.post {
                    callback?.onTtsComplete()
                }
                Log.d(TAG, "在线TTS合成完成")
            }
        }

        override fun onError(ttsError: TTS.TTSError, obj: Any?) {
            val code = ttsError.code
            val msg = ttsError.errMsg

            Log.e(TAG, "在线TTS错误: code=$code, msg=$msg")

            // 尝试切换到离线TTS
            if (pendingText.isNotBlank() && isSystemTTSReady) {
                Log.d(TAG, "在线TTS失败，尝试切换到离线TTS")
                val textToSpeak = pendingText
                pendingText = ""  // 清理待播放文本
                speakOffline(textToSpeak)
            } else {
                isSpeaking = false
                mainHandler.post {
                    callback?.onTtsError(code, msg)
                }
            }
        }
    }
}
