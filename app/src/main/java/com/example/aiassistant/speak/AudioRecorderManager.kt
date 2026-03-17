package com.example.aiassistant.speak

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * 录音管理器 - 用于获取麦克风音频数据
 */
class AudioRecorderManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioRecorderManager"

        // 讯飞SDK要求的音频参数
        const val SAMPLE_RATE = 16000        // 采样率：16kHz
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO    // 单声道
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT   // 16位PCM
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    // 音频数据回调
    private var audioDataCallback: ((ByteArray) -> Unit)? = null

    // 音量回调
    private var volumeCallback: ((Int) -> Unit)? = null

    /**
     * 检查录音权限
     */
    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取最小缓冲区大小
     */
    private fun getMinBufferSize(): Int {
        return AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    }

    /**
     * 注册音频数据回调
     */
    fun setAudioDataCallback(callback: (ByteArray) -> Unit) {
        audioDataCallback = callback
    }

    /**
     * 开始录音
     * @return true表示成功启动
     */
    fun startRecording(): Boolean {
        if (isRecording) {
            Log.w(TAG, "已经在录音中")
            return false
        }

        if (!hasRecordPermission()) {
            Log.e(TAG, "========== 没有录音权限 ==========")
            return false
        }

        Log.d(TAG, "========== AudioRecorder开始录音 ==========")
        Log.d(TAG, "采样率: $SAMPLE_RATE Hz, 声道: $CHANNEL_CONFIG, 编码: $AUDIO_FORMAT")

        try {
            val bufferSize = getMinBufferSize()
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "缓冲区大小无效: $bufferSize")
                return false
            }
            Log.d(TAG, "缓冲区大小: $bufferSize")

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 2
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "========== AudioRecord初始化失败 ==========")
                audioRecord?.release()
                audioRecord = null
                return false
            }
            Log.d(TAG, "AudioRecord初始化成功")

            audioRecord?.startRecording()
            isRecording = true
            Log.d(TAG, "========== 录音已启动 ==========")

            // 验证录音状态
            if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Log.e(TAG, "录音状态验证失败，录音可能未真正启动")
                isRecording = false
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                return false
            }
            Log.d(TAG, "录音状态验证通过")

            // 启动录音线程
            recordingThread = Thread {
                Log.d(TAG, "录音线程已启动")
                val buffer = ByteArray(bufferSize)
                var totalBytesRead = 0L
                while (isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        totalBytesRead += readSize
                        audioDataCallback?.invoke(buffer.copyOf(readSize))
                        val volume = calculateVolume(buffer, readSize)
                        volumeCallback?.invoke(volume)
                    }
                }
                Log.d(TAG, "录音线程结束，共读取 $totalBytesRead bytes")
            }
            recordingThread?.start()

            Log.d(TAG, "========== 开始录音成功 ==========")
            return true

        } catch (e: SecurityException) {
            Log.e(TAG, "录音权限异常: ${e.message}")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "录音启动异常: ${e.message}")
            return false
        }
    }

    /**
     * 停止录音
     */
    fun stopRecording() {
        if (!isRecording) {
            return
        }

        isRecording = false

        try {
            recordingThread?.join(1000)
        } catch (e: InterruptedException) {
            Log.e(TAG, "等待录音线程结束异常: ${e.message}")
        }

        recordingThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "停止录音异常: ${e.message}")
        }

        audioRecord = null
        Log.d(TAG, "停止录音")
    }

    /**
     * 计算音量（简单算法）
     */
    private fun calculateVolume(buffer: ByteArray, readSize: Int): Int {
        var sum = 0
        for (i in 0 until readSize step 2) {
            if (i + 1 < readSize) {
                val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                sum += kotlin.math.abs(sample.toShort().toInt())
            }
        }
        val avg = sum / (readSize / 2)
        return (avg / 327.68).toInt().coerceIn(0, 100)
    }

    /**
     * 释放资源
     */
    fun release() {
        stopRecording()
        audioDataCallback = null
        volumeCallback = null
    }
}
