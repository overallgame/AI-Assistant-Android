package com.example.aiassistant.data.repository

import com.example.aiassistant.data.model.ConversationGroup
import com.example.aiassistant.data.source.RemoteConversationDataSource

class DefaultConversationRepository(
    private val remote: RemoteConversationDataSource,
) : ConversationRepository {

    override suspend fun fetchConversationGroups(): List<ConversationGroup> {
        return remote.fetchConversationGroups()
    }
}
