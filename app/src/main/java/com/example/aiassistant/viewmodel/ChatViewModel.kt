package com.example.aiassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiassistant.config.AppConfig
import com.example.aiassistant.data.model.ChatMessage
import com.example.aiassistant.data.model.ChatMessagePart
import com.example.aiassistant.data.model.ChatRole
import com.example.aiassistant.data.model.ChatUiState
import com.example.aiassistant.data.model.ChatFileType
import com.example.aiassistant.data.model.AttachmentTransferStatus
import com.example.aiassistant.data.model.WebSocketConnectionState
import com.example.aiassistant.data.repository.ChatWebSocketRepository
import com.example.aiassistant.data.websocket.WebSocketEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val webSocketRepository: ChatWebSocketRepository,
    private val appConfig: AppConfig,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<WebSocketConnectionState>
        get() = webSocketRepository.connectionState

    private val attachmentJobs = mutableMapOf<String, Job>()

    init {
        webSocketRepository.setServerUrl(appConfig.webSocketUrl)
        observeWebSocketEvents()

        if (appConfig.autoConnect) {
            connect()
        }
    }

    private fun observeWebSocketEvents() {
        viewModelScope.launch {
            webSocketRepository.events.collect { event ->
                when (event) {
                    is WebSocketEvent.MessageReceived -> {
                        addAssistantMessage(event.message)
                    }
                    is WebSocketEvent.Error -> {
                        _uiState.update { it.copy(errorMessage = event.message, isSending = false) }
                    }
                    is WebSocketEvent.Connected -> {
                        _uiState.update { it.copy(errorMessage = null) }
                    }
                    is WebSocketEvent.Disconnected -> {}
                    is WebSocketEvent.ChunkReceived -> {}
                    is WebSocketEvent.DoneReceived -> {}
                }
            }
        }
    }

    fun connect() {
        webSocketRepository.connect()
    }

    fun disconnect() {
        webSocketRepository.disconnect()
    }

    fun setServerUrl(url: String) {
        appConfig.webSocketUrl = url
        webSocketRepository.setServerUrl(url)
    }

    fun newChat() {
        _uiState.update { it.copy(messages = emptyList(), inputText = "", errorMessage = null) }
        attachmentJobs.values.forEach { it.cancel() }
        attachmentJobs.clear()
    }

    fun setInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun toggleThinking() {
        _uiState.update { it.copy(mode = it.mode.copy(thinkingEnabled = !it.mode.thinkingEnabled)) }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(mode = it.mode.copy(searchEnabled = !it.mode.searchEnabled)) }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        val content = currentState.inputText.trim()
        
        if (content.isBlank() && currentState.messages.isEmpty()) {
            return
        }
        
        val userMessageId = UUID.randomUUID().toString()
        val userMessage = ChatMessage(
            id = userMessageId,
            role = ChatRole.User,
            parts = if (content.isNotBlank()) listOf(ChatMessagePart.Text(content)) else emptyList(),
        )
        
        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            newMessages.add(userMessage)
            state.copy(
                messages = newMessages,
                inputText = "",
                isSending = true,
            )
        }
        
        val messageId = UUID.randomUUID().toString()
        
        if (connectionState.value == WebSocketConnectionState.Connected) {
            webSocketRepository.sendMessage(
                content = content,
                messageId = messageId,
                thinkingEnabled = currentState.mode.thinkingEnabled,
                searchEnabled = currentState.mode.searchEnabled,
                attachments = emptyList(),
            )
        } else {
            viewModelScope.launch {
                webSocketRepository.connect()
                delay(2000)
                if (connectionState.value == WebSocketConnectionState.Connected) {
                    webSocketRepository.sendMessage(
                        content = content,
                        messageId = messageId,
                        thinkingEnabled = currentState.mode.thinkingEnabled,
                        searchEnabled = currentState.mode.searchEnabled,
                        attachments = emptyList(),
                    )
                } else {
                    _uiState.update { it.copy(errorMessage = "无法连接到服务器", isSending = false) }
                }
            }
        }
    }
    
    fun mockSendHello() {
        sendMessage()
    }

    fun sendImage(
        contentUri: String,
        mimeType: String? = null,
        widthPx: Int? = null,
        heightPx: Int? = null,
    ) {
        val message = ChatMessage(
            role = ChatRole.User,
            parts = listOf(
                ChatMessagePart.Image(
                    contentUri = contentUri,
                    mimeType = mimeType,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    transferStatus = AttachmentTransferStatus.Uploading,
                    progress = 0f,
                ),
            ),
        )

        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            newMessages.add(message)
            state.copy(messages = newMessages)
        }

        startSimulateAttachmentFlow(messageId = message.id, contentUri = contentUri)
    }

    fun sendFile(
        contentUri: String,
        fileName: String,
        mimeType: String? = null,
        sizeBytes: Long? = null,
    ) {
        val message = ChatMessage(
            role = ChatRole.User,
            parts = listOf(
                ChatMessagePart.File(
                    contentUri = contentUri,
                    fileName = fileName,
                    fileType = guessFileType(fileName = fileName, mimeType = mimeType),
                    mimeType = mimeType,
                    sizeBytes = sizeBytes,
                    transferStatus = AttachmentTransferStatus.Uploading,
                    progress = 0f,
                ),
            ),
        )

        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            newMessages.add(message)
            state.copy(messages = newMessages)
        }

        startSimulateAttachmentFlow(messageId = message.id, contentUri = contentUri)
    }

    fun retryAttachment(messageId: String) {
        val msg = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        val targetUri = when (val p = msg.parts.firstOrNull()) {
            is ChatMessagePart.Image -> p.contentUri
            is ChatMessagePart.File -> p.contentUri
            else -> return
        }

        updateAttachmentPart(
            messageId = messageId,
            contentUri = targetUri,
            status = AttachmentTransferStatus.Uploading,
            progress = 0f,
        )

        startSimulateAttachmentFlow(messageId = messageId, contentUri = targetUri)
    }

    private fun guessFileType(fileName: String, mimeType: String?): ChatFileType {
        val name = fileName.lowercase()
        return when {
            mimeType == "application/pdf" || name.endsWith(".pdf") -> ChatFileType.Pdf
            mimeType == "text/csv" || name.endsWith(".csv") -> ChatFileType.Csv
            mimeType == "application/msword" ||
                mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                name.endsWith(".doc") ||
                name.endsWith(".docx") -> ChatFileType.Word
            mimeType?.startsWith("image/") == true -> ChatFileType.Image
            else -> ChatFileType.Other
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    private fun addAssistantMessage(message: ChatMessage) {
        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            newMessages.add(message)
            state.copy(
                messages = newMessages,
                isSending = false,
            )
        }
    }

    private fun startSimulateAttachmentFlow(messageId: String, contentUri: String) {
        attachmentJobs.remove(messageId)?.cancel()

        attachmentJobs[messageId] = viewModelScope.launch {
            var p = 0f
            while (p < 1f) {
                delay(180)
                p = (p + 0.12f).coerceAtMost(1f)
                updateAttachmentPart(
                    messageId = messageId,
                    contentUri = contentUri,
                    status = AttachmentTransferStatus.Uploading,
                    progress = p,
                )
            }

            updateAttachmentPart(
                messageId = messageId,
                contentUri = contentUri,
                status = AttachmentTransferStatus.Processing,
                progress = null,
            )

            delay(650)

            val ok = Random.nextFloat() >= 0.15f
            updateAttachmentPart(
                messageId = messageId,
                contentUri = contentUri,
                status = if (ok) AttachmentTransferStatus.Done else AttachmentTransferStatus.Failed,
                progress = null,
            )
        }
    }

    private fun updateAttachmentPart(
        messageId: String,
        contentUri: String,
        status: AttachmentTransferStatus,
        progress: Float?,
    ) {
        _uiState.update { state ->
            val idx = state.messages.indexOfFirst { it.id == messageId }
            if (idx < 0) return@update state

            val msg = state.messages[idx]
            val newParts = msg.parts.map { part ->
                when (part) {
                    is ChatMessagePart.Image ->
                        if (part.contentUri == contentUri) {
                            part.copy(transferStatus = status, progress = progress)
                        } else {
                            part
                        }

                    is ChatMessagePart.File ->
                        if (part.contentUri == contentUri) {
                            part.copy(transferStatus = status, progress = progress)
                        } else {
                            part
                        }

                    else -> part
                }
            }

            val newMessages = state.messages.toMutableList()
            newMessages[idx] = msg.copy(parts = newParts)
            state.copy(messages = newMessages)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
