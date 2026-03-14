package com.example.aiassistant.data.repository

import com.example.aiassistant.data.model.ConversationGroup
import com.example.aiassistant.data.repository.interfac.ConversationRepository
import com.example.aiassistant.data.source.interfac.RemoteConversationDataSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultConversationRepository(
    private val remote: RemoteConversationDataSource,
) : ConversationRepository {

    private val cacheMutex = Mutex()
    private var cachedGroups: List<ConversationGroup>? = null
    private var cacheTimestamp: Long = 0
    private val cacheValidityMs = 5 * 60 * 1000L

    override suspend fun fetchConversationGroups(forceRefresh: Boolean): List<ConversationGroup> {
        return cacheMutex.withLock {
            val now = System.currentTimeMillis()
            val isCacheValid = cachedGroups != null && (now - cacheTimestamp) < cacheValidityMs

            if (!forceRefresh && isCacheValid) {
                return@withLock cachedGroups!!
            }

            val groups = remote.fetchConversationGroups()
            cachedGroups = groups
            cacheTimestamp = now
            groups
        }
    }

    suspend fun clearCache() {
        cacheMutex.withLock {
            cachedGroups = null
            cacheTimestamp = 0
        }
    }
}
