package com.example.aiassistant.data.mock

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
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockServerManager @Inject constructor() {

    private var httpServer: MockWebServer? = null
    private var wsServer: MockWebServer? = null

    private var mockApiHandler: MockApiHandler? = null
    private var mockWsHandler: MockWsHandler? = null

    private val _httpServerUrl = MutableStateFlow<String?>(null)
    val httpServerUrl: StateFlow<String?> = _httpServerUrl.asStateFlow()

    private val _wsServerUrl = MutableStateFlow<String?>(null)
    val wsServerUrl: StateFlow<String?> = _wsServerUrl.asStateFlow()

    private var currentWebSocket: WebSocket? = null
    private val _wsEvents = MutableSharedFlow<WsMockEvent>()
    val wsEvents: SharedFlow<WsMockEvent> = _wsEvents.asSharedFlow()

    private val _isStarted = MutableStateFlow(false)
    val isStarted: StateFlow<Boolean> = _isStarted.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun startHttpServer(mockApiHandler: MockApiHandler) {
        if (httpServer != null) return
        httpServer = MockWebServer().apply {
            dispatcher = createHttpDispatcher(mockApiHandler)
            start(0)
        }
        _httpServerUrl.value = "http://localhost:${httpServer!!.port}"
    }

    private fun startWsServer(mockWsHandler: MockWsHandler) {
        if (wsServer != null) return
        wsServer = MockWebServer().apply {
            dispatcher = createWsDispatcher(mockWsHandler)
            start(0)
        }
        _wsServerUrl.value = "ws://localhost:${wsServer!!.port}"
    }

    fun startAll(mockApiHandler: MockApiHandler, mockWsHandler: MockWsHandler) {
        scope.launch {
            startHttpServer(mockApiHandler)
            startWsServer(mockWsHandler)
            _isStarted.value = true
        }
    }

    fun ensureStarted(mockApiHandler: MockApiHandler, mockWsHandler: MockWsHandler) {
        if (_isStarted.value) return
        // 同步启动服务器并等待完成
        startHttpServer(mockApiHandler)
        startWsServer(mockWsHandler)
        _isStarted.value = true
    }

    fun stopAll() {
        httpServer?.shutdown()
        httpServer = null
        _httpServerUrl.value = null

        wsServer?.shutdown()
        wsServer = null
        _wsServerUrl.value = null

        currentWebSocket = null
        _isStarted.value = false
    }

    fun getHttpServerUrl(): String? = _httpServerUrl.value
    fun getWsServerUrl(): String? = _wsServerUrl.value

    private fun createHttpDispatcher(handler: MockApiHandler) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return try {
                val path = request.path ?: "/"
                val method = request.method ?: "GET"
                val body = request.body.readUtf8()
                val response = when {
                    path.startsWith("/api/user/login") && method == "POST" ->
                        handler.handleLogin(body)
                    path.startsWith("/api/conversations") && method == "GET" ->
                        handler.handleGetConversations()
                    path.matches(Regex("/api/user/[^/]+/avatar")) && method == "PUT" -> {
                        val userId = path.removePrefix("/api/user/").removeSuffix("/avatar")
                        handler.handleUpdateUser(userId, body)
                    }
                    path.matches(Regex("/api/user/[^/]+/display-name")) && method == "PUT" -> {
                        val userId = path.removePrefix("/api/user/").removeSuffix("/display-name")
                        handler.handleUpdateUser(userId, body)
                    }
                    path.matches(Regex("/api/user/[^/]+/preferences")) && method == "PUT" -> {
                        val userId = path.removePrefix("/api/user/").removeSuffix("/preferences")
                        handler.handleUpdateUser(userId, body)
                    }
                    path.matches(Regex("/api/user/[^/]+")) && method == "GET" -> {
                        val userId = path.removePrefix("/api/user/")
                        handler.handleGetUser(userId)
                    }
                    else -> HttpMockResponse(404, """{"error": "Not found: $path"}""")
                }
                MockResponse()
                    .setResponseCode(response.statusCode)
                    .setBody(response.body)
                    .setHeader("Content-Type", "application/json")
            } catch (e: Exception) {
                MockResponse()
                    .setResponseCode(500)
                    .setBody("""{"error": "${e.message}"}""")
            }
        }
    }

    private fun createWsDispatcher(handler: MockWsHandler) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            return MockResponse()
                .setResponseCode(101)
                .setHeader("Upgrade", "websocket")
                .setHeader("Connection", "Upgrade")
                .withWebSocketUpgrade(createWsListener(handler))
        }
    }

    private fun createWsListener(handler: MockWsHandler) = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            currentWebSocket = webSocket
            scope.launch { _wsEvents.emit(WsMockEvent.Connected) }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch {
                handler.handleMessage(text)?.let { responses ->
                    responses.forEach { msg ->
                        webSocket.send(msg)
                        delay(50)
                    }
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            currentWebSocket = null
            scope.launch { _wsEvents.emit(WsMockEvent.Disconnected) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            currentWebSocket = null
            scope.launch { _wsEvents.emit(WsMockEvent.Error(t.message ?: "Unknown error")) }
        }
    }
}

data class HttpMockResponse(
    val statusCode: Int,
    val body: String,
)

sealed class WsMockEvent {
    data object Connected : WsMockEvent()
    data object Disconnected : WsMockEvent()
    data class Error(val message: String) : WsMockEvent()
}

interface MockApiHandler {
    fun handleLogin(body: String): HttpMockResponse
    fun handleGetUser(userId: String): HttpMockResponse
    fun handleUpdateUser(userId: String, body: String): HttpMockResponse
    fun handleGetConversations(): HttpMockResponse
}

interface MockWsHandler {
    suspend fun handleMessage(message: String): List<String>?
}
