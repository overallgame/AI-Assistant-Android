package com.example.aiassistant.data.source

import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.AvatarType
import com.example.aiassistant.data.model.User
import com.example.aiassistant.data.model.UserPreferences
import kotlinx.coroutines.delay

class FakeRemoteUserDataSource : RemoteUserDataSource {

    private val users = mutableMapOf<String, User>()

    override suspend fun loginWithPhone(phoneE164: String): User {
        delay(200)
        val user = User(
            id = "user_001",
            phoneE164 = phoneE164,
            phoneMasked = maskPhone(phoneE164),
            displayName = null,
            avatar = Avatar(type = AvatarType.Default, value = "Blue"),
        )
        users[user.id] = user
        return user
    }

    override suspend fun fetchUser(userId: String): User {
        delay(150)
        return users[userId]
            ?: User(
                id = userId,
                phoneE164 = "+8613400000063",
                phoneMasked = "134******63",
                displayName = null,
                avatar = Avatar(type = AvatarType.Default, value = "Blue"),
            ).also { users[userId] = it }
    }

    override suspend fun updateAvatar(userId: String, avatar: Avatar): User {
        delay(150)
        val existing = fetchUser(userId)
        val updated = existing.copy(avatar = avatar)
        users[userId] = updated
        return updated
    }

    override suspend fun updateDisplayName(userId: String, displayName: String?): User {
        delay(150)
        val existing = fetchUser(userId)
        val updated = existing.copy(displayName = displayName)
        users[userId] = updated
        return updated
    }

    override suspend fun updatePreferences(userId: String, preferences: UserPreferences): UserPreferences {
        delay(120)
        return preferences
    }

    private fun maskPhone(phoneE164: String): String {
        val digits = phoneE164.filter { it.isDigit() }
        if (digits.length < 7) return phoneE164
        val start = digits.take(3)
        val end = digits.takeLast(2)
        return "$start******$end"
    }
}
