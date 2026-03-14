package com.example.aiassistant.data.source.interfac

import com.example.aiassistant.data.model.ConversationGroup

interface RemoteConversationDataSource {
    suspend fun fetchConversationGroups(): List<ConversationGroup>
}
