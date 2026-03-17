package com.example.aiassistant.speak

import android.util.Log
import com.iflytek.sparkchain.core.asr.ASR
import com.iflytek.sparkchain.core.asr.AsrCallbacks

/**
 * 语音识别（ASR）管理器
 */
class AsrManager {

    companion object {
        private const val TAG = "AsrManager"
    }

    private var asr: ASR? = null
    private var callback: SpeakCallback? = null
    private var isRecognizing = false

    // 识别参数
    private var language = "zh_cn"      // 语种：zh_cn中文，en_us英文
    private var domain = "iat"          // 领域：iat日常用语
    private var accent = "mandarin"     // 方言：mandarin普通话
    private var enableVadInfo = true    // 启用VAD信息
    private var enableDwa = "wpgs"      // 动态修正：wpgs

    /**
     * 初始化ASR
     */
    fun init() {
        if (asr == null) {
            asr = ASR()
            asr?.registerCallbacks(asrCallbacks)
            Log.d(TAG, "ASR初始化完成")
        }
    }

    /**
     * 设置回调
     */
    fun setCallback(callback: SpeakCallback) {
        this.callback = callback
    }

    /**
     * 设置识别语言
     * @param lang 语种：zh_cn, en_us
     */
    fun setLanguage(lang: String) {
        language = lang
    }

    /**
     * 开始语音识别（使用麦克风实时录音）
     */
    fun startRecognition(): Boolean {
        if (isRecognizing) {
            Log.w(TAG, "正在识别中，请勿重复开启")
            return false
        }

        Log.d(TAG, "========== ASR开始识别 ==========")
        Log.d(TAG, "语种: $language, 领域: $domain, 方言: $accent")
        Log.d(TAG, "VAD启用: $enableVadInfo, DWA启用: $enableDwa")

        init()

        // 配置识别参数
        asr?.apply {
            language(language)          // 语种
            domain(domain)              // 领域
            accent(accent)              // 方言
            vinfo(enableVadInfo)        // VAD信息

            // 动态修正仅对中文有效
            if (language == "zh_cn") {
                dwa(enableDwa)
            }
        }

        // 启动识别
        Log.d(TAG, "调用asr.start()启动识别")
        val ret = asr?.start(System.currentTimeMillis().toString()) ?: -1

        if (ret == 0) {
            isRecognizing = true
            Log.d(TAG, "========== ASR识别启动成功 ==========")
            return true
        } else {
            Log.e(TAG, "========== ASR识别启动失败 ==========")
            Log.e(TAG, "错误码: $ret")
            callback?.onAsrError(ret, "识别启动失败，错误码: $ret")
            return false
        }
    }

    /**
     * 写入音频数据（从录音管理器传入）
     */
    fun writeAudioData(data: ByteArray): Int {
        if (!isRecognizing) {
            return -1
        }
        return asr?.write(data) ?: -1
    }

    /**
     * 停止识别
     * @param immediately 是否立即停止，false表示等待云端返回最后一帧
     */
    fun stopRecognition(immediately: Boolean = false) {
        if (!isRecognizing) {
            return
        }

        asr?.stop(immediately)
        isRecognizing = false
        callback?.onRecordingStopped()
        Log.d(TAG, "停止识别")
    }

    /**
     * 释放资源
     */
    fun release() {
        stopRecognition()
        asr = null
        callback = null
        Log.d(TAG, "ASR资源已释放")
    }

    /**
     * ASR回调实现
     */
    private val asrCallbacks = object : AsrCallbacks {
        override fun onResult(asrResult: ASR.ASRResult, obj: Any?) {
            val begin = asrResult.begin          // 起始帧
            val end = asrResult.end            // 结束帧
            val status = asrResult.status      // 0:首结果 1:中间结果 2:最终结果
            val bestText = asrResult.bestMatchText ?: ""  // 最佳匹配文本
            val sid = asrResult.sid            // 会话ID

            Log.d(TAG, "========== ASR识别结果 ==========")
            Log.d(TAG, "状态: $status (0=首结果, 1=中间结果, 2=最终结果)")
            Log.d(TAG, "识别文本: [$bestText]")
            Log.d(TAG, "会话ID: $sid")
            Log.d(TAG, "起始帧: $begin, 结束帧: $end")

            // 处理VAD信息
            val vads = asrResult.vads
            if (vads.isNotEmpty()) {
                Log.d(TAG, "VAD信息:")
                for (vad in vads) {
                    Log.d(TAG, "  - begin=${vad.begin}, end=${vad.end}")
                }
            }

            // 处理分词结果
            val transcriptions = asrResult.transcriptions
            if (transcriptions.isNotEmpty()) {
                Log.d(TAG, "分词结果:")
                for (transcription in transcriptions) {
                    val segments = transcription.segments
                    for (segment in segments) {
                        Log.d(TAG, "  - ${segment.text}")
                    }
                }
            }

            // 根据状态回调
            when (status) {
                0 -> {
                    // 识别首结果
                    Log.d(TAG, "收到首结果，触发onAsrResult回调(中间结果)")
                    callback?.onAsrResult(bestText, false)
                }

                1 -> {
                    // 识别中间结果
                    Log.d(TAG, "收到中间结果，触发onAsrResult回调(中间结果)")
                    callback?.onAsrResult(bestText, false)
                }

                2 -> {
                    // 识别最终结果
                    Log.d(TAG, "========== 收到最终结果 ==========")
                    Log.d(TAG, "触发onAsrResult回调(最终结果)")
                    callback?.onAsrResult(bestText, true)
                    isRecognizing = false
                    callback?.onRecordingStopped()
                }
            }
        }

        override fun onError(asrError: ASR.ASRError, obj: Any?) {
            val code = asrError.code
            val msg = asrError.errMsg
            val sid = asrError.sid

            Log.e(TAG, "========== ASR识别错误 ==========")
            Log.e(TAG, "错误码: $code")
            Log.e(TAG, "错误信息: $msg")
            Log.e(TAG, "会话ID: $sid")

            isRecognizing = false
            callback?.onAsrError(code, msg)
            callback?.onRecordingStopped()
        }

        override fun onBeginOfSpeech() {
            Log.d(TAG, "========== 检测到开始说话 ==========")
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "========== 检测到说话结束 ==========")
        }
    }
}
