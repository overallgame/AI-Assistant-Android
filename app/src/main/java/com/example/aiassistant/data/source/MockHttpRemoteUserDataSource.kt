package com.example.aiassistant.data.source

import com.example.aiassistant.data.mock.DefaultMockApiHandler
import com.example.aiassistant.data.mock.DefaultMockWsHandler
import com.example.aiassistant.data.mock.MockServerManager
import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.AvatarType
import com.example.aiassistant.data.model.User
import com.example.aiassistant.data.model.UserPreferences
import com.example.aiassistant.data.source.interfac.RemoteUserDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MockHttpRemoteUserDataSource(
    private val mockServerManager: MockServerManager,
    private val mockApiHandler: DefaultMockApiHandler,
    private val mockWsHandler: DefaultMockWsHandler,
    private val okHttpClient: OkHttpClient,
) : RemoteUserDataSource {

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val baseUrl: String
        get() = mockServerManager.getHttpServerUrl() ?: error("Mock HTTP server not started")

    private fun ensureStarted() {
        mockServerManager.ensureStarted(mockApiHandler, mockWsHandler)
    }

    override suspend fun loginWithPhone(phoneE164: String): User = withContext(Dispatchers.IO) {
        ensureStarted()
        val body = """{"phoneE164": "$phoneE164"}""".toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseUrl/api/user/login")
            .post(body)
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Login failed: ${response.code}")
            val responseBody = response.body?.string() ?: error("Empty response body")
            parseUser(responseBody)
        }
    }

    override suspend fun fetchUser(userId: String): User = withContext(Dispatchers.IO) {
        ensureStarted()
        val request = Request.Builder()
            .url("$baseUrl/api/user/$userId")
            .get()
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Fetch user failed: ${response.code}")
            val responseBody = response.body?.string() ?: error("Empty response body")
            parseUser(responseBody)
        }
    }

    override suspend fun updateAvatar(userId: String, avatar: Avatar): User = withContext(Dispatchers.IO) {
        ensureStarted()
        val body = """{"avatarType": "${avatar.type.name}", "avatarValue": ${avatar.value?.let { "\"$it\"" } ?: "null"}}"""
            .toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseUrl/api/user/$userId/avatar")
            .put(body)
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Update avatar failed: ${response.code}")
            val responseBody = response.body?.string() ?: error("Empty response body")
            parseUser(responseBody)
        }
    }

    override suspend fun updateDisplayName(userId: String, displayName: String?): User = withContext(Dispatchers.IO) {
        ensureStarted()
        val nameJson = displayName?.let { "\"$it\"" } ?: "null"
        val body = """{"displayName": $nameJson}""".toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("$baseUrl/api/user/$userId/display-name")
            .put(body)
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Update display name failed: ${response.code}")
            val responseBody = response.body?.string() ?: error("Empty response body")
            parseUser(responseBody)
        }
    }

    override suspend fun updatePreferences(userId: String, preferences: UserPreferences): UserPreferences =
        withContext(Dispatchers.IO) {
            ensureStarted()
            val body = """{"preferences": true}""".toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url("$baseUrl/api/user/$userId/preferences")
                .put(body)
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Update preferences failed: ${response.code}")
                preferences
            }
        }

    private fun parseUser(responseBody: String): User {
        val obj = json.parseToJsonElement(responseBody) as? kotlinx.serialization.json.JsonObject
            ?: error("Invalid user response: not a JSON object")
        val id = obj["id"]?.asPrimitive?.content ?: error("Missing id field")
        val phoneE164 = obj["phoneE164"]?.asPrimitive?.content
        val phoneMasked = obj["phoneMasked"]?.asPrimitive?.content ?: ""
        val displayName = obj["displayName"]?.asPrimitive?.content
        val avatarTypeStr = obj["avatarType"]?.asPrimitive?.content ?: AvatarType.Default.name
        val avatarValue = obj["avatarValue"]?.asPrimitive?.content
        val avatarType = runCatching { AvatarType.valueOf(avatarTypeStr) }.getOrDefault(AvatarType.Default)
        return User(
            id = id,
            phoneE164 = phoneE164,
            phoneMasked = phoneMasked,
            displayName = displayName,
            avatar = Avatar(type = avatarType, value = avatarValue),
        )
    }

    private val JsonElement.asPrimitive: JsonPrimitive?
        get() = this as? JsonPrimitive
}
