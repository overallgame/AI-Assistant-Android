package com.example.aiassistant.data.repository

import com.example.aiassistant.data.model.AppearancePreference
import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.FontSizePreference
import com.example.aiassistant.data.model.LanguagePreference
import com.example.aiassistant.data.model.User
import com.example.aiassistant.data.model.UserPreferences
import com.example.aiassistant.data.model.UserState
import com.example.aiassistant.data.repository.interfac.UserRepository
import com.example.aiassistant.data.source.interfac.LocalUserDataSource
import com.example.aiassistant.data.source.interfac.RemoteUserDataSource
import kotlinx.coroutines.flow.StateFlow

class DefaultUserRepository(
    private val local: LocalUserDataSource,
    private val remote: RemoteUserDataSource,
) : UserRepository {

    override val userState: StateFlow<UserState> = local.userState

    override suspend fun loginWithPhone(phoneE164: String): User {
        val user = remote.loginWithPhone(phoneE164)
        local.setUser(user)
        local.setSessionLoggedIn(user.id)
        return user
    }

    override suspend fun logout() {
        local.setSessionLoggedOut()
        local.setUser(null)
    }

    override suspend fun updateAvatar(avatar: Avatar) {
        val currentUserId = local.userState.value.session.currentUserId ?: return
        val updatedUser = remote.updateAvatar(currentUserId, avatar)
        local.setUser(updatedUser)
    }

    override suspend fun updateDisplayName(displayName: String?) {
        val currentUserId = local.userState.value.session.currentUserId ?: return
        val updatedUser = remote.updateDisplayName(currentUserId, displayName)
        local.setUser(updatedUser)
    }

    override suspend fun updateAppearance(appearance: AppearancePreference) {
        updatePreferences { prefs -> prefs.copy(appearance = appearance) }
    }

    override suspend fun updateFontSize(fontSize: FontSizePreference) {
        updatePreferences { prefs -> prefs.copy(fontSize = fontSize) }
    }

    override suspend fun updateLanguage(language: LanguagePreference) {
        updatePreferences { prefs -> prefs.copy(language = language) }
    }

    private suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        val state = local.userState.value
        val newPrefs = transform(state.preferences)

        val userId = state.session.currentUserId
        if (userId != null) {
            try {
                val remotePrefs = remote.updatePreferences(userId, newPrefs)
                local.updatePreferences(remotePrefs)
            } catch (e: Exception) {
                local.updatePreferences(newPrefs)
            }
        } else {
            local.updatePreferences(newPrefs)
        }
    }
}
