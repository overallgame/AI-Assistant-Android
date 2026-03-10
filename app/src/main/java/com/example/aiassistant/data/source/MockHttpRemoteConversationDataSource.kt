package com.example.aiassistant.data.source

import com.example.aiassistant.data.mock.DefaultMockApiHandler
import com.example.aiassistant.data.mock.DefaultMockWsHandler
import com.example.aiassistant.data.mock.MockServerManager
import com.example.aiassistant.data.model.ConversationGroup
import com.example.aiassistant.data.model.ConversationSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

class MockHttpRemoteConversationDataSource(
    private val mockServerManager: MockServerManager,
    private val mockApiHandler: DefaultMockApiHandler,
    private val mockWsHandler: DefaultMockWsHandler,
    private val okHttpClient: OkHttpClient,
) : RemoteConversationDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    private val baseUrl: String
        get() = mockServerManager.getHttpServerUrl() ?: error("Mock HTTP server not started")

    private fun ensureStarted() {
        mockServerManager.ensureStarted(mockApiHandler, mockWsHandler)
    }

    override suspend fun fetchConversationGroups(): List<ConversationGroup> = withContext(Dispatchers.IO) {
        ensureStarted()
        val request = Request.Builder()
            .url("$baseUrl/api/conversations")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Fetch conversations failed: ${response.code}")
            val responseBody = response.body?.string() ?: error("Empty response body")
            parseConversationGroups(responseBody)
        }
    }

    private fun parseConversationGroups(responseBody: String): List<ConversationGroup> {
        val array = json.parseToJsonElement(responseBody).jsonArray
        return array.map { groupElement ->
            val group = groupElement.jsonObject
            val title = group["title"]?.jsonPrimitive?.content ?: ""
            val items = group["items"]?.jsonArray?.map { itemElement ->
                val item = itemElement.jsonObject
                ConversationSummary(
                    id = item["id"]?.jsonPrimitive?.content ?: "",
                    title = item["title"]?.jsonPrimitive?.content ?: "",
                    updatedAtEpochMs = item["updatedAtEpochMs"]?.jsonPrimitive?.content?.toLongOrNull()
                        ?: System.currentTimeMillis(),
                )
            } ?: emptyList()
            ConversationGroup(title = title, items = items)
        }
    }
}
