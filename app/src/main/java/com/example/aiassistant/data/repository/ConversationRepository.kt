package com.example.aiassistant.data.repository

import com.example.aiassistant.data.model.ConversationGroup
import com.example.aiassistant.data.source.RemoteConversationDataSource

interface ConversationRepository {
    suspend fun fetchConversationGroups(forceRefresh: Boolean = false): List<ConversationGroup>
}
