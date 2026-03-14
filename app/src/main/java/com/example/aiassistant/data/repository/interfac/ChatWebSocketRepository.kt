package com.example.aiassistant.data.repository.interfac

import com.example.aiassistant.data.model.ChatMessagePart
import com.example.aiassistant.data.model.WebSocketConnectionState
import com.example.aiassistant.data.websocket.WebSocketEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ChatWebSocketRepository {
    val connectionState: StateFlow<WebSocketConnectionState>
    val events: SharedFlow<WebSocketEvent>

    fun connect()
    fun disconnect()
    fun sendMessage(
        content: String,
        messageId: String,
        thinkingEnabled: Boolean = false,
        searchEnabled: Boolean = false,
        attachments: List<ChatMessagePart> = emptyList(),
        conversationHistory: List<Pair<String, String>> = emptyList(),
    )

    fun setServerUrl(url: String)
}
