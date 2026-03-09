package com.example.aiassistant.data.websocket

import com.example.aiassistant.data.model.ChatMessage
import com.example.aiassistant.data.model.ChatMessagePart
import com.example.aiassistant.data.model.ChatRole
import com.example.aiassistant.data.model.WebSocketAttachment
import com.example.aiassistant.data.model.WebSocketConnectionState
import com.example.aiassistant.data.model.WebSocketMessage
import com.example.aiassistant.data.model.WebSocketResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

sealed class WebSocketEvent {
    data class MessageReceived(val message: ChatMessage) : WebSocketEvent()
    data class ChunkReceived(val messageId: String, val content: String) : WebSocketEvent()
    data class DoneReceived(val messageId: String, val fullContent: String) : WebSocketEvent()
    data class Error(val message: String) : WebSocketEvent()
    data object Connected : WebSocketEvent()
    data object Disconnected : WebSocketEvent()
}

@Singleton
class ChatWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private var webSocket: WebSocket? = null
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.Disconnected)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<WebSocketEvent>()
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    private var serverUrl: String = "wss://api.example.com/chat"
    private var currentMessageId: String? = null
    private val responseBuffer = StringBuilder()

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_DELAY_MS = 3000L
    }

    private var reconnectAttempts = 0

    fun setServerUrl(url: String) {
        serverUrl = url
    }

    fun connect() {
        if (_connectionState.value == WebSocketConnectionState.Connected ||
            _connectionState.value == WebSocketConnectionState.Connecting) {
            return
        }

        _connectionState.value = WebSocketConnectionState.Connecting

        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    fun disconnect() {
        reconnectAttempts = MAX_RECONNECT_ATTEMPTS
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = WebSocketConnectionState.Disconnected
    }

    fun sendMessage(
        content: String,
        messageId: String,
        thinkingEnabled: Boolean = false,
        searchEnabled: Boolean = false,
        attachments: List<ChatMessagePart> = emptyList(),
    ) {
        if (_connectionState.value != WebSocketConnectionState.Connected) {
            scope.launch {
                _events.emit(WebSocketEvent.Error("未连接到服务器"))
            }
            return
        }

        currentMessageId = messageId
        responseBuffer.clear()

        val wsAttachments = attachments.mapNotNull { part ->
            when (part) {
                is ChatMessagePart.Image -> WebSocketAttachment(
                    type = "image",
                    contentUri = part.contentUri,
                    mimeType = part.mimeType,
                )
                is ChatMessagePart.File -> WebSocketAttachment(
                    type = "file",
                    contentUri = part.contentUri,
                    mimeType = part.mimeType,
                    fileName = part.fileName,
                )
                else -> null
            }
        }

        val message = WebSocketMessage(
            type = "message",
            content = content,
            messageId = messageId,
            thinkingEnabled = thinkingEnabled,
            searchEnabled = searchEnabled,
            attachments = wsAttachments.ifEmpty { null },
        )

        val json = Json.encodeToString(WebSocketMessage.serializer(), message)
        webSocket?.send(json)
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connectionState.value = WebSocketConnectionState.Connected
            reconnectAttempts = 0
            scope.launch {
                _events.emit(WebSocketEvent.Connected)
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleResponse(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.value = WebSocketConnectionState.Disconnected
            scope.launch {
                _events.emit(WebSocketEvent.Disconnected)
            }
            attemptReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connectionState.value = WebSocketConnectionState.Disconnected
            scope.launch {
                _events.emit(WebSocketEvent.Error(t.message ?: "连接失败"))
            }
            attemptReconnect()
        }
    }

    private fun handleResponse(text: String) {
        try {
            val response = Json.decodeFromString(WebSocketResponse.serializer(), text)

            when (response.type) {
                "content" -> {
                    responseBuffer.append(response.content ?: "")
                }
                "chunk" -> {
                    responseBuffer.append(response.content ?: "")
                    val messageId = response.messageId ?: currentMessageId ?: return
                    scope.launch {
                        _events.emit(WebSocketEvent.ChunkReceived(messageId, responseBuffer.toString()))
                    }
                }
                "done" -> {
                    if (responseBuffer.isNotEmpty()) {
                        val messageId = response.messageId ?: currentMessageId ?: return
                        val fullContent = responseBuffer.toString()

                        scope.launch {
                            _events.emit(
                                WebSocketEvent.DoneReceived(
                                    messageId = messageId,
                                    fullContent = fullContent,
                                ),
                            )
                            _events.emit(
                                WebSocketEvent.MessageReceived(
                                    ChatMessage(
                                        id = messageId,
                                        role = ChatRole.Assistant,
                                        parts = listOf(ChatMessagePart.Text(fullContent)),
                                    ),
                                ),
                            )
                        }
                        responseBuffer.clear()
                    }
                }
                "error" -> {
                    scope.launch {
                        _events.emit(WebSocketEvent.Error(response.error ?: "未知错误"))
                    }
                }
            }
        } catch (e: Exception) {
            scope.launch {
                _events.emit(WebSocketEvent.Error("解析响应失败: ${e.message}"))
            }
        }
    }

    private fun attemptReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            return
        }

        reconnectAttempts++
        _connectionState.value = WebSocketConnectionState.Reconnecting

        scope.launch {
            delay(RECONNECT_DELAY_MS * reconnectAttempts)
            if (_connectionState.value != WebSocketConnectionState.Connected) {
                connect()
            }
        }
    }
}
