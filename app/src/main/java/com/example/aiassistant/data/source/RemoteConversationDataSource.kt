package com.example.aiassistant.data.source

import com.example.aiassistant.data.model.ConversationGroup

interface RemoteConversationDataSource {
    suspend fun fetchConversationGroups(): List<ConversationGroup>
}
