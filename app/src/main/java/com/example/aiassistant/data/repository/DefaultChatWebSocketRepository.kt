package com.example.aiassistant.data.repository

import com.example.aiassistant.data.model.ChatMessagePart
import com.example.aiassistant.data.model.WebSocketConnectionState
import com.example.aiassistant.data.repository.interfac.ChatWebSocketRepository
import com.example.aiassistant.data.websocket.ChatWebSocketManager
import com.example.aiassistant.data.websocket.WebSocketEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultChatWebSocketRepository @Inject constructor(
    private val webSocketManager: ChatWebSocketManager,
) : ChatWebSocketRepository {

    override val connectionState: StateFlow<WebSocketConnectionState>
        get() = webSocketManager.connectionState

    override val events: SharedFlow<WebSocketEvent>
        get() = webSocketManager.events

    override fun connect() {}

    override fun disconnect() {
        webSocketManager.disconnect()
    }

    override fun sendMessage(
        content: String,
        messageId: String,
        thinkingEnabled: Boolean,
        searchEnabled: Boolean,
        attachments: List<ChatMessagePart>,
        conversationHistory: List<Pair<String, String>>,
    ) {
        webSocketManager.sendMessage(
            content = content,
            messageId = messageId,
            thinkingEnabled = thinkingEnabled,
            searchEnabled = searchEnabled,
            conversationHistory = conversationHistory,
        )
    }

    override fun setServerUrl(url: String) {
        // 星火模式不需要手动设置URL，由鉴权自动生成
    }
}
