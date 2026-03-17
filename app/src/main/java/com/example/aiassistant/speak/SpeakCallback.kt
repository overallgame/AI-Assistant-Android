package com.example.aiassistant.speak

/**
 * 语音识别和合成的统一回调接口
 */
interface SpeakCallback {

    /**
     * 语音识别结果回调
     * @param text 识别到的文本
     * @param isFinal 是否为最终结果（true表示识别完成）
     */
    fun onAsrResult(text: String, isFinal: Boolean)

    /**
     * 语音识别错误回调
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     */
    fun onAsrError(errorCode: Int, errorMessage: String)

    /**
     * 语音合成开始
     */
    fun onTtsStart()

    /**
     * 语音合成数据回调（用于流式播放）
     * @param audioData 音频数据
     */
    fun onTtsData(audioData: ByteArray)

    /**
     * 语音合成完成
     */
    fun onTtsComplete()

    /**
     * 语音合成错误回调
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     */
    fun onTtsError(errorCode: Int, errorMessage: String)

    /**
     * 开始录音回调（用于UI显示录音状态）
     */
    fun onRecordingStarted()

    /**
     * 录音停止回调
     */
    fun onRecordingStopped()
}
