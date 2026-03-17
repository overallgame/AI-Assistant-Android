package com.example.aiassistant.data.websocket

import android.util.Log
import com.example.aiassistant.config.AppConfig
import com.example.aiassistant.data.model.WebSocketConnectionState
import com.example.aiassistant.data.model.XinghuoAuth
import com.example.aiassistant.data.model.XinghuoChat
import com.example.aiassistant.data.model.XinghuoErrorCode
import com.example.aiassistant.data.model.XinghuoHeader
import com.example.aiassistant.data.model.XinghuoMessage
import com.example.aiassistant.data.model.XinghuoParameter
import com.example.aiassistant.data.model.XinghuoPayload
import com.example.aiassistant.data.model.XinghuoRequest
import com.example.aiassistant.data.model.XinghuoResponse
import com.example.aiassistant.data.model.XinghuoText
import com.example.aiassistant.data.model.XinghuoThinking
import com.example.aiassistant.data.model.XinghuoTool
import com.example.aiassistant.data.model.XinghuoUsageText
import com.example.aiassistant.data.model.XinghuoWebSearch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WebSocket 事件
 */
sealed class WebSocketEvent {
    data class Error(val message: String) : WebSocketEvent()
    data object Connected : WebSocketEvent()
    data object Disconnected : WebSocketEvent()
    data object Reconnecting : WebSocketEvent()

    // 星火大模型相关事件
    data class XinghuoReasoningChunkReceived(
        val messageId: String,
        val reasoning: String
    ) : WebSocketEvent()

    data class XinghuoContentChunkReceived(
        val messageId: String,
        val content: String
    ) : WebSocketEvent()

    data class XinghuoDoneReceived(
        val messageId: String,
        val fullContent: String,
        val fullReasoning: String?,
        val usage: XinghuoUsageText?
    ) : WebSocketEvent()

    data class XinghuoError(
        val code: Int,
        val message: String,
        val sid: String?
    ) : WebSocketEvent()
}

/**
 * 星火大模型 WebSocket 短连接管理器
 *
 * 核心特性：
 * 1. 流式响应 - 支持实时流式输出
 * 2. 多轮对话 - 支持上下文对话
 */
@Singleton
class ChatWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val appConfig: AppConfig,
) {
    private var webSocket: WebSocket? = null

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.Disconnected)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<WebSocketEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    // 当前响应缓冲区
    private var currentMessageId: String? = null
    private val contentBuffer = StringBuilder()
    private val reasoningBuffer = StringBuilder()

    // 响应处理的互斥锁，防止并发乱序
    private val responseMutex = Mutex()

    // 待发送内容缓冲区
    private var pendingContent = ""
    private var pendingReasoning = ""
    private var pendingContentJob: Job? = null
    private var pendingReasoningJob: Job? = null

    companion object {
        private const val BATCH_DELAY_MS = 50L

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    // ==================== 连接管理 ====================

    /**
     * 建立短连接（仅用于发送单次消息）
     */
    private fun connectForMessage() {
        if (!checkConfig()) return

        _connectionState.value = WebSocketConnectionState.Connecting
        createConnection()
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = WebSocketConnectionState.Disconnected

        mainScope.launch {
            _events.emit(WebSocketEvent.Disconnected)
        }
    }

    /**
     * 断开连接
     */
    private fun internalDisconnect() {
        webSocket?.close(1000, "Done")
        webSocket = null
        _connectionState.value = WebSocketConnectionState.Disconnected
    }

    /**
     * 检查配置
     */
    private fun checkConfig(): Boolean {
        val appId = appConfig.appId
        val apiKey = appConfig.apiKey
        val apiSecret = appConfig.apiSecret

        if (appId.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            mainScope.launch {
                _events.emit(WebSocketEvent.Error("星火配置不完整，请检查 appId、apiKey、apiSecret"))
            }
            return false
        }
        return true
    }

    /**
     * 创建 WebSocket 连接
     */
    private fun createConnection() {
        val domain = appConfig.xinghuoDomain
        val hostUrl = XinghuoAuth.getXinghuoApiUrl(domain)
        val authUrl = XinghuoAuth.getAuthUrl(hostUrl, appConfig.apiKey, appConfig.apiSecret)

        val request = Request.Builder()
            .url(authUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    // ==================== 消息发送 ====================

    /**
     * 发送消息（短连接模式）
     * 流程：建立连接 -> 发送消息 -> 等待响应 -> 自动断开
     */
    fun sendMessage(
        content: String,
        messageId: String,
        thinkingEnabled: Boolean = false,
        searchEnabled: Boolean = false,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ) {
        ioScope.launch {
            if (content.isBlank()) {
                mainScope.launch {
                    _events.emit(WebSocketEvent.Error("消息内容不能为空"))
                }
                return@launch
            }

            // 使用传入的 conversationHistory 构建上下文
            val contextList = conversationHistory.map { (role, msgContent) ->
                XinghuoText(role = role, content = msgContent)
            }.toMutableList()

            // 添加当前用户消息
            contextList.add(XinghuoText(role = "user", content = content))

            // 建立短连接并发送消息
            _connectionState.value = WebSocketConnectionState.Connecting
            connectForMessage()

            // 等待连接后发送
            waitForConnected {
                doSendMessage(messageId, contextList, thinkingEnabled, searchEnabled)
            }
        }
    }

    /**
     * 执行实际发送
     */
    private fun doSendMessage(
        messageId: String,
        context: List<XinghuoText>,
        thinkingEnabled: Boolean = false,
        searchEnabled: Boolean = false
    ) {
        // 再次检查连接状态
        if (_connectionState.value != WebSocketConnectionState.Connected) {
            if (appConfig.debugLogging) {
                Log.w("ChatWebSocket", "连接未建立，发送失败")
            }
            mainScope.launch {
                _events.emit(WebSocketEvent.Error("连接未建立"))
            }
            return
        }

        // 初始化缓冲区
        currentMessageId = messageId
        contentBuffer.clear()
        reasoningBuffer.clear()
        pendingContent = ""
        pendingReasoning = ""
        pendingContentJob?.cancel()
        pendingReasoningJob?.cancel()

        // 构建请求
        val request = buildXinghuoRequest(context, thinkingEnabled, searchEnabled)
        val requestJson = json.encodeToString(XinghuoRequest.serializer(), request)

        if (appConfig.debugLogging) {
            Log.d("ChatWebSocket", "=== 发送消息 ===")
            Log.d("ChatWebSocket", requestJson)
        }

        // 发送
        val sent = webSocket?.send(requestJson) ?: false
        if (!sent) {
            mainScope.launch {
                _events.emit(WebSocketEvent.Error("消息发送失败"))
            }
        }
    }

    /**
     * 等待连接建立
     */
    private suspend fun waitForConnected(action: suspend () -> Unit) {
        var waited = 0
        while (_connectionState.value != WebSocketConnectionState.Connected && waited < 15000) {
            delay(100)
            waited += 100
        }
        if (_connectionState.value == WebSocketConnectionState.Connected) {
            action()
        } else {
            // 超时未连接，发送错误事件并重置状态
            mainScope.launch {
                _events.emit(WebSocketEvent.Error("连接超时，请检查网络后重试"))
            }
            _connectionState.value = WebSocketConnectionState.Disconnected
        }
    }

    /**
     * 构建星火请求
     */
    private fun buildXinghuoRequest(
        context: List<XinghuoText>,
        thinkingEnabled: Boolean = false,
        searchEnabled: Boolean = false
    ): XinghuoRequest {
        return XinghuoRequest(
            header = XinghuoHeader(
                appId = appConfig.appId,
                uid = UUID.randomUUID().toString().take(32)
            ),
            parameter = XinghuoParameter(
                chat = XinghuoChat(
                    domain = appConfig.xinghuoDomain,
                    maxTokens = 65535,
                    temperature = 1.2f,
                    topK = 6,
                    presencePenalty = 2.01f,
                    frequencyPenalty = 0.001f,
                    thinking = XinghuoThinking(
                        type = appConfig.thinkingType,
                        enable = thinkingEnabled
                    ),
                    tools = buildTools(searchEnabled),
                    chatId = UUID.randomUUID().toString()
                )
            ),
            payload = XinghuoPayload(
                message = XinghuoMessage(text = context)
            )
        )
    }

    /**
     * 构建工具列表
     */
    private fun buildTools(searchEnabled: Boolean = false): List<XinghuoTool>? {
        return if (searchEnabled) {
            listOf(
                XinghuoTool(
                    type = "web_search",
                    webSearch = XinghuoWebSearch(
                        enable = true,
                        searchMode = appConfig.searchMode
                    )
                )
            )
        } else null
    }

    // ==================== WebSocket 监听器 ====================

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _connectionState.value = WebSocketConnectionState.Connected

            // 通知连接成功
            mainScope.launch {
                _events.emit(WebSocketEvent.Connected)
            }

            if (appConfig.debugLogging) {
                Log.d("ChatWebSocket", "WebSocket 已连接")
            }
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (appConfig.debugLogging) {
                ioScope.launch {
                    Log.d("ChatWebSocket", "=== 接收消息 ===")
                    Log.d("ChatWebSocket", text)
                }
            }

            ioScope.launch {
                responseMutex.withLock {
                    handleResponse(text)
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            if (appConfig.debugLogging) {
                Log.d("ChatWebSocket", "WebSocket 正在关闭: $code $reason")
            }
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            handleDisconnected("WebSocket closed: $code $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            handleDisconnected("WebSocket error: ${t.message}")
        }
    }

    // ==================== 断开处理 ====================

    private fun handleDisconnected(reason: String) {
        _connectionState.value = WebSocketConnectionState.Disconnected
        webSocket = null

        mainScope.launch {
            _events.emit(WebSocketEvent.Disconnected)
        }

        if (appConfig.debugLogging) {
            Log.d("ChatWebSocket", "连接断开: $reason")
        }
    }

    // ==================== 响应处理 ====================

    private fun handleResponse(text: String) {
        try {
            val response = json.decodeFromString(XinghuoResponse.serializer(), text)
            val header = response.header
            val payload = response.payload

            // 错误处理
            if (header.code != XinghuoErrorCode.SUCCESS) {
                val errorMsg = XinghuoErrorCode.getErrorMessage(header.code)
                mainScope.launch {
                    _events.emit(
                        WebSocketEvent.XinghuoError(
                            code = header.code,
                            message = errorMsg,
                            sid = header.sid
                        )
                    )
                }
                return
            }

            val answerText = payload.choices.text.firstOrNull()
            val currentContent = answerText?.content ?: ""
            val currentReasoning = answerText?.reasoningContent ?: ""

            // 处理推理内容
            if (currentReasoning.isNotEmpty()) {
                reasoningBuffer.append(currentReasoning)
                pendingReasoning += currentReasoning
                scheduleReasoningFlush()
            }

            // 处理回复内容
            if (currentContent.isNotEmpty()) {
                contentBuffer.append(currentContent)
                pendingContent += currentContent
                scheduleContentFlush()
            }

            // status = 2 表示结束
            if (header.status == 2) {
                flushPendingContent()
                flushPendingReasoning()

                val fullContent = contentBuffer.toString()
                val fullReasoning = reasoningBuffer.toString().ifEmpty { null }
                val usage = payload.usage?.text

                // 发送完成事件
                currentMessageId?.let { msgId ->
                    mainScope.launch {
                        _events.emit(
                            WebSocketEvent.XinghuoDoneReceived(
                                messageId = msgId,
                                fullContent = fullContent,
                                fullReasoning = fullReasoning,
                                usage = usage
                            )
                        )
                    }
                }

                // 清理缓冲区
                contentBuffer.clear()
                reasoningBuffer.clear()
                pendingContent = ""
                pendingReasoning = ""
                pendingContentJob?.cancel()
                pendingReasoningJob?.cancel()

                // 短连接模式：收到响应后自动断开
                internalDisconnect()
            }
        } catch (e: Exception) {
            mainScope.launch {
                _events.emit(WebSocketEvent.Error("解析响应失败: ${e.message}"))
            }
        }
    }

    /**
     * 调度内容刷新
     */
    private fun scheduleContentFlush() {
        pendingContentJob?.cancel()
        pendingContentJob = mainScope.launch {
            delay(BATCH_DELAY_MS)
            flushPendingContent()
        }
    }

    /**
     * 调度推理刷新
     */
    private fun scheduleReasoningFlush() {
        pendingReasoningJob?.cancel()
        pendingReasoningJob = mainScope.launch {
            delay(BATCH_DELAY_MS)
            flushPendingReasoning()
        }
    }

    /**
     * 刷新待发送的内容到UI
     */
    private fun flushPendingContent() {
        val contentToSend = pendingContent.takeIf { it.isNotEmpty() } ?: return
        val msgId = currentMessageId ?: return

        pendingContent = ""

        mainScope.launch {
            _events.emit(
                WebSocketEvent.XinghuoContentChunkReceived(
                    messageId = msgId,
                    content = contentToSend
                )
            )
        }
    }

    /**
     * 刷新待发送的推理到UI
     */
    private fun flushPendingReasoning() {
        val reasoningToSend = pendingReasoning.takeIf { it.isNotEmpty() } ?: return
        val msgId = currentMessageId ?: return

        pendingReasoning = ""

        mainScope.launch {
            _events.emit(
                WebSocketEvent.XinghuoReasoningChunkReceived(
                    messageId = msgId,
                    reasoning = reasoningToSend
                )
            )
        }
    }
}