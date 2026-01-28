package com.example.aiassistant.data.source

import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.User
import com.example.aiassistant.data.model.UserPreferences

interface RemoteUserDataSource {
    suspend fun loginWithPhone(phoneE164: String): User

    suspend fun fetchUser(userId: String): User

    suspend fun updateAvatar(userId: String, avatar: Avatar): User

    suspend fun updateDisplayName(userId: String, displayName: String?): User

    suspend fun updatePreferences(userId: String, preferences: UserPreferences): UserPreferences
}
