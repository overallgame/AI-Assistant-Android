package com.example.aiassistant.data.repository

import com.example.aiassistant.data.model.AppearancePreference
import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.FontSizePreference
import com.example.aiassistant.data.model.LanguagePreference
import com.example.aiassistant.data.model.User
import com.example.aiassistant.data.model.UserState
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val userState: StateFlow<UserState>

    suspend fun loginWithPhone(phoneE164: String): User

    suspend fun logout()

    suspend fun updateAvatar(avatar: Avatar)

    suspend fun updateDisplayName(displayName: String?)

    suspend fun updateAppearance(appearance: AppearancePreference)

    suspend fun updateFontSize(fontSize: FontSizePreference)

    suspend fun updateLanguage(language: LanguagePreference)
}
