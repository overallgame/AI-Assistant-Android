package com.example.aiassistant.data.source

import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.User
import com.example.aiassistant.data.model.UserPreferences
import com.example.aiassistant.data.model.UserState
import kotlinx.coroutines.flow.StateFlow

interface LocalUserDataSource {
    val userState: StateFlow<UserState>

    suspend fun setSessionLoggedIn(userId: String)

    suspend fun setSessionLoggedOut()

    suspend fun setUser(user: User?)

    suspend fun updateAvatar(avatar: Avatar)

    suspend fun updateDisplayName(displayName: String?)

    suspend fun updatePreferences(preferences: UserPreferences)
}
