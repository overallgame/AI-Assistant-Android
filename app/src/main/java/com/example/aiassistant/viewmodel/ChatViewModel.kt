package com.example.aiassistant.viewmodel

import androidx.lifecycle.ViewModel
import com.example.aiassistant.data.model.ChatMessage
import com.example.aiassistant.data.model.ChatFileType
import com.example.aiassistant.data.model.ChatMessagePart
import com.example.aiassistant.data.model.ChatRole
import com.example.aiassistant.data.model.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun newChat() {
        _uiState.update { it.copy(messages = emptyList(), inputText = "", errorMessage = null) }
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
        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            newMessages.add(
                ChatMessage(
                    role = ChatRole.User,
                    parts = listOf(
                        ChatMessagePart.Image(
                            contentUri = contentUri,
                            mimeType = mimeType,
                            widthPx = widthPx,
                            heightPx = heightPx,
                        ),
                    ),
                ),
            )
            state.copy(messages = newMessages)
        }
    }

    fun sendFile(
        contentUri: String,
        fileName: String,
        mimeType: String? = null,
        sizeBytes: Long? = null,
    ) {
        _uiState.update { state ->
            val newMessages = state.messages.toMutableList()
            newMessages.add(
                ChatMessage(
                    role = ChatRole.User,
                    parts = listOf(
                        ChatMessagePart.File(
                            contentUri = contentUri,
                            fileName = fileName,
                            fileType = guessFileType(fileName = fileName, mimeType = mimeType),
                            mimeType = mimeType,
                            sizeBytes = sizeBytes,
                        ),
                    ),
                ),
            )
            state.copy(messages = newMessages)
        }
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
}
