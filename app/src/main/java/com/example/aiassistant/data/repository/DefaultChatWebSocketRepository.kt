package com.example.aiassistant.data.repository

import com.example.aiassistant.config.AppConfig
import com.example.aiassistant.data.mock.DefaultMockApiHandler
import com.example.aiassistant.data.mock.DefaultMockWsHandler
import com.example.aiassistant.data.mock.MockServerManager
import com.example.aiassistant.data.model.ChatMessagePart
import com.example.aiassistant.data.model.WebSocketConnectionState
import com.example.aiassistant.data.websocket.ChatWebSocketManager
import com.example.aiassistant.data.websocket.WebSocketEvent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultChatWebSocketRepository @Inject constructor(
    private val webSocketManager: ChatWebSocketManager,
    private val mockServerManager: MockServerManager,
    private val defaultMockApiHandler: DefaultMockApiHandler,
    private val defaultMockWsHandler: DefaultMockWsHandler,
    private val appConfig: AppConfig,
) : ChatWebSocketRepository {

    override val connectionState: StateFlow<WebSocketConnectionState>
        get() = webSocketManager.connectionState

    override val events: SharedFlow<WebSocketEvent>
        get() = webSocketManager.events

    override fun connect() {
        if (appConfig.useMockBackend) {
            // Ensure mock server is started before connecting
            mockServerManager.ensureStarted(defaultMockApiHandler, defaultMockWsHandler)
            mockServerManager.wsServerUrl.value?.let { url ->
                webSocketManager.setServerUrl(url)
            }
        }
        webSocketManager.connect()
    }

    override fun disconnect() {
        webSocketManager.disconnect()
    }

    override fun sendMessage(
        content: String,
        messageId: String,
        thinkingEnabled: Boolean,
        searchEnabled: Boolean,
        attachments: List<ChatMessagePart>,
    ) {
        webSocketManager.sendMessage(
            content = content,
            messageId = messageId,
            thinkingEnabled = thinkingEnabled,
            searchEnabled = searchEnabled,
            attachments = attachments,
        )
    }

    override fun setServerUrl(url: String) {
        webSocketManager.setServerUrl(url)
    }
}
