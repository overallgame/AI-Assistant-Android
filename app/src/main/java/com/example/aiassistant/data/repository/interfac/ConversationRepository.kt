package com.example.aiassistant.data.repository.interfac

import com.example.aiassistant.data.model.ConversationGroup

interface ConversationRepository {
    suspend fun fetchConversationGroups(forceRefresh: Boolean = false): List<ConversationGroup>
}
