package com.example.aiassistant.data.websocket

import android.util.Log
import com.example.aiassistant.config.AppConfig
import com.example.aiassistant.data.model.ChatMessage
import com.example.aiassistant.data.model.ChatMessagePart
import com.example.aiassistant.data.model.ChatRole
import com.example.aiassistant.data.model.WebSocketConnectionState
import com.example.aiassistant.data.model.XinghuoAuth
import com.example.aiassistant.data.model.XinghuoAnswerText
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

sealed class WebSocketEvent {
    data class MessageReceived(val message: ChatMessage) : WebSocketEvent()
    data class ChunkReceived(val messageId: String, val content: String) : WebSocketEvent()
    data class DoneReceived(val messageId: String, val fullContent: String) : WebSocketEvent()
    data class Error(val message: String) : WebSocketEvent()
    data object Connected : WebSocketEvent()
    data object Disconnected : WebSocketEvent()

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

@Singleton
class ChatWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val appConfig: AppConfig,
) {
    private var webSocket: WebSocket? = null
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.Disconnected)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<WebSocketEvent>()
    val events: SharedFlow<WebSocketEvent> = _events.asSharedFlow()

    // 星火流式输出缓冲区
    private var currentMessageId: String? = null
    private val contentBuffer = StringBuilder()
    private val reasoningBuffer = StringBuilder()

    // 防止 onMessage 多线程并发导致乱序/丢字
    private val responseMutex = Mutex()

    // 缓冲相关 - 用于批量发送更新
    // 第一段立即显示（0ms），后续段缓冲 10ms 再显示（减少频繁更新）
    private var pendingContentJob: Job? = null
    private var pendingReasoningJob: Job? = null
    private var pendingContent = ""
    private var pendingReasoning = ""
    private var isFirstContentChunk = true  // 标记是否是第一个内容块
    private var isFirstReasoningChunk = true  // 标记是否是第一个推理块

    private val FIRST_CHUNK_DELAY_MS = 0L  // 第一段立即显示
    private val BATCH_DELAY_MS = 10L  // 后续段缓冲 10ms

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val RECONNECT_DELAY_MS = 3000L

        // JSON 配置
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    private var reconnectAttempts = 0

    /**
     * 连接到星火大模型WebSocket
     */
    fun connect() {
        if (_connectionState.value == WebSocketConnectionState.Connected ||
            _connectionState.value == WebSocketConnectionState.Connecting) {
            return
        }

        // 检查星火配置
        val appId = appConfig.appId
        val apiKey = appConfig.apiKey
        val apiSecret = appConfig.apiSecret

        if (appId.isBlank() || apiKey.isBlank() || apiSecret.isBlank()) {
            scope.launch {
                _events.emit(WebSocketEvent.Error("星火配置不完整，请检查 appId、apiKey、apiSecret"))
            }
            return
        }

        _connectionState.value = WebSocketConnectionState.Connecting

        // 生成鉴权URL
        val domain = appConfig.xinghuoDomain
        val hostUrl = XinghuoAuth.getXinghuoApiUrl(domain)
        val authUrl = XinghuoAuth.getAuthUrl(hostUrl, apiKey, apiSecret)

        val request = Request.Builder()
            .url(authUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    fun disconnect() {
        reconnectAttempts = MAX_RECONNECT_ATTEMPTS
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = WebSocketConnectionState.Disconnected
    }

    /**
     * 发送消息到星火大模型
     * @param content 用户输入的内容
     * @param messageId 消息ID
     * @param conversationHistory 对话历史，用于多轮对话
     */
    fun sendMessage(
        content: String,
        messageId: String,
        conversationHistory: List<Pair<String, String>> = emptyList(), // role -> content
    ) {
        if (_connectionState.value != WebSocketConnectionState.Connected) {
            scope.launch {
                _events.emit(WebSocketEvent.Error("未连接到服务器"))
            }
            return
        }

        currentMessageId = messageId
        contentBuffer.clear()
        reasoningBuffer.clear()
        pendingContent = ""
        pendingReasoning = ""
        pendingContentJob?.cancel()
        pendingReasoningJob?.cancel()
        // 重置第一段标记
        isFirstContentChunk = true
        isFirstReasoningChunk = true

        // 构建请求
        val request = buildXinghuoRequest(content, conversationHistory)
        val requestJson = json.encodeToString(XinghuoRequest.serializer(), request)

        // 调试模式下打印发送的消息
        if (appConfig.debugLogging) {
            scope.launch(Dispatchers.IO) {
                Log.d("ChatWebSocket", "=== 发送消息 ===")
                Log.d("ChatWebSocket", requestJson)
            }
        }

        webSocket?.send(requestJson)
    }

    /**
     * 构建星火请求
     */
    private fun buildXinghuoRequest(
        userContent: String,
        history: List<Pair<String, String>>
    ): XinghuoRequest {
        // 构建消息文本
        val textList = mutableListOf<XinghuoText>()

        // 添加历史消息
        history.forEach { (role, content) ->
            textList.add(XinghuoText(role = role, content = content))
        }

        // 添加当前用户消息
        textList.add(XinghuoText(role = "user", content = userContent))

        // 构建请求
        return XinghuoRequest(
            header = XinghuoHeader(
                appId = appConfig.appId,
                uid = XinghuoAuth.generateUid()
            ),
            parameter = XinghuoParameter(
                chat = XinghuoChat(
                    domain = appConfig.xinghuoDomain,
                    maxTokens = 4096,
                    temperature = 1.0f,
                    topK = 5,
                    presencePenalty = 0.0f,
                    frequencyPenalty = 0.0f,
                    thinking = XinghuoThinking(type = appConfig.thinkingType),
                    tools = buildTools()
                )
            ),
            payload = XinghuoPayload(
                message = XinghuoMessage(text = textList)
            )
        )
    }

    /**
     * 构建工具列表
     */
    private fun buildTools(): List<XinghuoTool>? {
        return if (appConfig.searchEnabled) {
            listOf(
                XinghuoTool(
                    type = "web_search",
                    webSearch = XinghuoWebSearch(
                        enable = true,
                        searchMode = appConfig.searchMode
                    )
                )
            )
        } else {
            null
        }
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
            // 调试模式下打印接收的消息
            if (appConfig.debugLogging) {
                val textCopy = text
                scope.launch(Dispatchers.IO) {
                    Log.d("ChatWebSocket", "=== 接收消息 ===")
                    Log.d("ChatWebSocket", textCopy)
                }
            }
            // 串行处理，避免多线程并发导致乱序/丢字
            scope.launch {
                responseMutex.withLock {
                    handleXinghuoResponse(text)
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            // 不主动关闭，等待服务器关闭连接
            // 让 WebSocket 保持打开状态直到服务器断开
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _connectionState.value = WebSocketConnectionState.Disconnected
            scope.launch {
                _events.emit(WebSocketEvent.Disconnected)
            }
            // 星火不需要自动重连，每次请求都是独立的
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connectionState.value = WebSocketConnectionState.Disconnected
            scope.launch {
                _events.emit(WebSocketEvent.Error(t.message ?: "连接失败"))
            }
        }
    }

    /**
     * 处理星火响应 - 支持流式输出（带缓冲优化）
     */
    private fun handleXinghuoResponse(text: String) {
        try {
            val response = json.decodeFromString(XinghuoResponse.serializer(), text)
            val header = response.header
            val payload = response.payload

            // 错误处理
            if (header.code != XinghuoErrorCode.SUCCESS) {
                val errorMsg = XinghuoErrorCode.getErrorMessage(header.code)
                scope.launch {
                    _events.emit(WebSocketEvent.XinghuoError(
                        code = header.code,
                        message = errorMsg,
                        sid = header.sid
                    ))
                }
                return
            }

            // 获取内容（星火 API：每次返回的是本段的增量内容，需客户端拼接展示）
            val answerText: XinghuoAnswerText? = payload.choices.text.firstOrNull()
            val currentContent = answerText?.content ?: ""
            val currentReasoning = answerText?.reasoningContent ?: ""

            // 推理内容：本包即为增量，直接追加
            if (currentReasoning.isNotEmpty()) {
                reasoningBuffer.append(currentReasoning)
                pendingReasoning += currentReasoning
                scheduleReasoningFlush()
            }

            // 最终回复内容：本包即为增量，直接追加
            if (currentContent.isNotEmpty()) {
                contentBuffer.append(currentContent)
                pendingContent += currentContent
                scheduleContentFlush()
            }

            // status = 2 表示结束，刷新所有剩余内容
            if (header.status == 2) {
                // 先刷新剩余的缓冲内容
                flushPendingContent()
                flushPendingReasoning()

                val fullContent = contentBuffer.toString()
                val fullReasoning = reasoningBuffer.toString().ifEmpty { null }
                val usage = payload.usage?.text

                currentMessageId?.let { msgId ->
                    scope.launch {
                        _events.emit(WebSocketEvent.XinghuoDoneReceived(
                            messageId = msgId,
                            fullContent = fullContent,
                            fullReasoning = fullReasoning,
                            usage = usage
                        ))
                        // 同时发送通用事件，保持兼容性
                        _events.emit(WebSocketEvent.DoneReceived(
                            messageId = msgId,
                            fullContent = fullContent
                        ))
                        _events.emit(WebSocketEvent.MessageReceived(
                            ChatMessage(
                                id = msgId,
                                role = ChatRole.Assistant,
                                parts = listOf(ChatMessagePart.Text(fullContent))
                            )
                        ))

                        // 延迟断开连接，给 UI 更新时间
                        delay(500)
                        webSocket?.close(1000, "Response complete")
                    }
                }
                // 清空缓冲区
                contentBuffer.clear()
                reasoningBuffer.clear()
                pendingContent = ""
                pendingReasoning = ""
                pendingContentJob?.cancel()
                pendingReasoningJob?.cancel()
                // 重置标记
                isFirstContentChunk = true
                isFirstReasoningChunk = true
            }
        } catch (e: Exception) {
            scope.launch {
                _events.emit(WebSocketEvent.Error("解析响应失败: ${e.message}"))
            }
        }
    }

    /**
     * 调度内容刷新 - 第一段立即显示，后续段缓冲后显示
     */
    private fun scheduleContentFlush() {
        val delayMs = if (isFirstContentChunk) {
            isFirstContentChunk = false
            FIRST_CHUNK_DELAY_MS
        } else {
            BATCH_DELAY_MS
        }
        pendingContentJob?.cancel()
        pendingContentJob = scope.launch {
            delay(delayMs)
            flushPendingContent()
        }
    }

    /**
     * 调度推理刷新 - 第一段立即显示，后续段缓冲后显示
     */
    private fun scheduleReasoningFlush() {
        val delayMs = if (isFirstReasoningChunk) {
            isFirstReasoningChunk = false
            FIRST_CHUNK_DELAY_MS
        } else {
            BATCH_DELAY_MS
        }
        pendingReasoningJob?.cancel()
        pendingReasoningJob = scope.launch {
            delay(delayMs)
            flushPendingReasoning()
        }
    }

    /**
     * 刷新待发送的内容到UI
     */
    private fun flushPendingContent() {
        if (pendingContent.isNotEmpty() && currentMessageId != null) {
            val contentToSend = pendingContent
            pendingContent = ""
            scope.launch {
                _events.emit(WebSocketEvent.XinghuoContentChunkReceived(
                    messageId = currentMessageId!!,
                    content = contentToSend
                ))
            }
        }
    }

    /**
     * 刷新待发送的推理到UI
     */
    private fun flushPendingReasoning() {
        if (pendingReasoning.isNotEmpty() && currentMessageId != null) {
            val reasoningToSend = pendingReasoning
            pendingReasoning = ""
            scope.launch {
                _events.emit(WebSocketEvent.XinghuoReasoningChunkReceived(
                    messageId = currentMessageId!!,
                    reasoning = reasoningToSend
                ))
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
