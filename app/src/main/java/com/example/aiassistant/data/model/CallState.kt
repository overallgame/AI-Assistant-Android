package com.example.aiassistant.data.model

import android.annotation.SuppressLint

/**
 * 对话消息记录
 */
data class ChatMessageRecord(
    val role: String,      // "user" 或 "assistant"
    val content: String    // 消息内容
)

/**
 * 通话阶段
 */
enum class CallPhase {
    Idle,           // 初始状态，未开始通话
    Connecting,     // 连接中
    Listening,      // 正在听用户说话
    Thinking,       // AI思考中（等待回复）
    Speaking,       // AI正在说话
    Ended           // 通话结束
}

/**
 * 通话状态数据模型
 */
data class CallState(
    val phase: CallPhase = CallPhase.Idle,
    val callDurationMs: Long = 0L,
    val errorMessage: String? = null,
    val lastUserSpeech: String = "",      // 用户最后说的内容
    val lastAiReply: String = "",         // AI最后的回复
    val isMuted: Boolean = false,        // 是否静音
    val isConnecting: Boolean = false,    // 是否正在连接
    val conversationHistory: List<ChatMessageRecord> = emptyList(), // 对话历史
) {
    /**
     * 是否可以开始通话
     */
    val canStartCall: Boolean
        get() = phase == CallPhase.Idle

    /**
     * 是否正在通话中
     */
    val isInCall: Boolean
        get() = phase != CallPhase.Idle && phase != CallPhase.Ended

    /**
     * 获取通话状态描述
     */
    val statusDescription: String
        get() = when (phase) {
            CallPhase.Idle -> "点击开始通话"
            CallPhase.Connecting -> "正在连接..."
            CallPhase.Listening -> "正在倾听..."
            CallPhase.Thinking -> "AI思考中..."
            CallPhase.Speaking -> "AI正在回复..."
            CallPhase.Ended -> "通话已结束"
        }

    /**
     * 格式化通话时长
     */
    val formattedDuration: String
        @SuppressLint("DefaultLocale")
        get() {
            val totalSeconds = callDurationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
}
