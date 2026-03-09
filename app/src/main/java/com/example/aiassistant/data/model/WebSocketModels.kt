package com.example.aiassistant.data.model

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketMessage(
    val type: String,
    val content: String? = null,
    val messageId: String? = null,
    val thinkingEnabled: Boolean = false,
    val searchEnabled: Boolean = false,
    val attachments: List<WebSocketAttachment>? = null,
)

@Serializable
data class WebSocketAttachment(
    val type: String,
    val contentUri: String,
    val mimeType: String? = null,
    val fileName: String? = null,
)

@Serializable
data class WebSocketResponse(
    val type: String,
    val content: String? = null,
    val messageId: String? = null,
    val error: String? = null,
    val thinking: String? = null,
    val done: Boolean = false,
)

enum class WebSocketConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
}
