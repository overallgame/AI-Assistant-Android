package com.example.aiassistant.data.model

import java.util.UUID

data class ConversationSummary(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)

data class ConversationGroup(
    val title: String,
    val items: List<ConversationSummary>,
)

data class DrawerUserInfo(
    val phoneMasked: String = "",
    val displayName: String? = null,
    val avatar: Avatar? = null,
)

data class ConversationDrawerUiState(
    val groups: List<ConversationGroup> = emptyList(),
    val selectedConversationId: String? = null,
    val userInfo: DrawerUserInfo = DrawerUserInfo(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
