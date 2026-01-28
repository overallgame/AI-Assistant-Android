package com.example.aiassistant.data.model

import java.util.UUID

enum class ChatRole {
    User,
    Assistant,
}

enum class MessageSendStatus {
    Sending,
    Sent,
    Failed,
}

enum class ChatFileType {
    Pdf,
    Word,
    Csv,
    Image,
    Other,
}

sealed interface ChatMessagePart {
    data class Text(
        val text: String,
    ) : ChatMessagePart

    data class Image(
        val contentUri: String,
        val mimeType: String? = null,
        val widthPx: Int? = null,
        val heightPx: Int? = null,
    ) : ChatMessagePart

    data class File(
        val contentUri: String,
        val fileName: String,
        val fileType: ChatFileType = ChatFileType.Other,
        val mimeType: String? = null,
        val sizeBytes: Long? = null,
    ) : ChatMessagePart
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val parts: List<ChatMessagePart>,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val status: MessageSendStatus = MessageSendStatus.Sent,
)

data class ChatMode(
    val thinkingEnabled: Boolean = true,
    val searchEnabled: Boolean = false,
)

data class ChatUiState(
    val title: String = "新对话",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val mode: ChatMode = ChatMode(),
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)
