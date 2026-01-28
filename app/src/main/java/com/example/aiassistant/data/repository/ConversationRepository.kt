package com.example.aiassistant.data.repository

import com.example.aiassistant.data.model.ConversationGroup

interface ConversationRepository {
    suspend fun fetchConversationGroups(): List<ConversationGroup>
}
