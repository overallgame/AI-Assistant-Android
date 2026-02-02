package com.example.aiassistant.viewmodel

import androidx.lifecycle.ViewModel
import com.example.aiassistant.data.model.ChatMessage
import com.example.aiassistant.data.model.ChatFileType
import com.example.aiassistant.data.model.ChatMessagePart
import com.example.aiassistant.data.model.ChatRole
import com.example.aiassistant.data.model.ChatUiState
import com.example.aiassistant.data.model.AttachmentTransferStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val attachmentJobs = mutableMapOf<String, Job>()

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

    fun mockSendHello() {
        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            newMessages.add(
                ChatMessage(
                    role = ChatRole.User,
                    parts = listOf(ChatMessagePart.Text("你好")),
                ),
            )
            newMessages.add(
                ChatMessage(
                    role = ChatRole.Assistant,
                    parts = listOf(ChatMessagePart.Text("你好")),
                ),
            )
            state.copy(messages = newMessages)
        }
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
}
