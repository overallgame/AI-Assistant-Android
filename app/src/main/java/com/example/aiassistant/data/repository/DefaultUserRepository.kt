package com.example.aiassistant.data.repository

import com.example.aiassistant.data.model.AuthMode
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
import java.util.UUID

class DefaultUserRepository(
    private val local: LocalUserDataSource,
    private val remote: RemoteUserDataSource,
    private val authMode: AuthMode = AuthMode.LOCAL_THEN_REMOTE,
) : UserRepository {

    override val userState: StateFlow<UserState> = local.userState

    override suspend fun loginWithPhone(phoneE164: String): User {
        return when (authMode) {
            AuthMode.LOCAL_ONLY -> {
                loginLocally(phoneE164)
            }

            AuthMode.LOCAL_THEN_REMOTE -> {
                tryLoginLocalThenRemote(phoneE164)
            }

            AuthMode.REMOTE_ONLY -> {
                loginRemotely(phoneE164)
            }
        }
    }

    private suspend fun loginLocally(phoneE164: String): User {
        val currentState = local.userState.value

        val existingUser = currentState.user
        val existingPhone = existingUser?.phoneE164

        val user = if (existingPhone != null && existingPhone == phoneE164) {
            existingUser
        } else {
            createLocalUser(phoneE164)
        }

        local.setUser(user)
        local.setSessionLoggedIn(user.id)
        return user
    }

    private suspend fun tryLoginLocalThenRemote(phoneE164: String): User {
        // 先尝试本地
        val localUser = local.userState.value.user
        if (localUser?.phoneE164 == phoneE164) {
            local.setSessionLoggedIn(localUser.id)
            return localUser
        }

        // 本地没有，尝试远程
        return try {
            loginRemotely(phoneE164)
        } catch (e: Exception) {
            // 远程失败，回退到本地创建
            loginLocally(phoneE164)
        }
    }

    private suspend fun loginRemotely(phoneE164: String): User {
        val remoteUser = remote.loginWithPhone(phoneE164)
        local.setUser(remoteUser)
        local.setSessionLoggedIn(remoteUser.id)
        return remoteUser
    }

    private fun createLocalUser(phoneE164: String): User {
        val phoneDigits = phoneE164.filter { it.isDigit() }
        val maskedPhone = if (phoneDigits.length >= 7) {
            "${phoneDigits.substring(0, 3)}****${phoneDigits.substring(7)}"
        } else {
            phoneE164
        }

        return User(
            id = UUID.randomUUID().toString(),
            phoneE164 = phoneE164,
            phoneMasked = maskedPhone,
            displayName = "用户${phoneDigits.takeLast(4)}",
        )
    }

    override suspend fun logout() {
        local.setSessionLoggedOut()
        local.setUser(null)
    }

    override suspend fun updateAvatar(avatar: Avatar) {
        val currentUserId = local.userState.value.session.currentUserId ?: return

        when (authMode) {
            AuthMode.LOCAL_ONLY -> {
                local.updateAvatar(avatar)
            }

            AuthMode.LOCAL_THEN_REMOTE -> {
                try {
                    val updatedUser = remote.updateAvatar(currentUserId, avatar)
                    local.setUser(updatedUser)
                } catch (e: Exception) {
                    local.updateAvatar(avatar)
                }
            }

            AuthMode.REMOTE_ONLY -> {
                val updatedUser = remote.updateAvatar(currentUserId, avatar)
                local.setUser(updatedUser)
            }
        }
    }

    override suspend fun updateDisplayName(displayName: String?) {
        val currentUserId = local.userState.value.session.currentUserId ?: return

        when (authMode) {
            AuthMode.LOCAL_ONLY -> {
                local.updateDisplayName(displayName)
            }

            AuthMode.LOCAL_THEN_REMOTE -> {
                try {
                    val updatedUser = remote.updateDisplayName(currentUserId, displayName)
                    local.setUser(updatedUser)
                } catch (e: Exception) {
                    local.updateDisplayName(displayName)
                }
            }

            AuthMode.REMOTE_ONLY -> {
                val updatedUser = remote.updateDisplayName(currentUserId, displayName)
                local.setUser(updatedUser)
            }
        }
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

    private suspend fun updatePreferences(
        transform: (UserPreferences) -> UserPreferences,
    ) {
        val state = local.userState.value
        val newPrefs = transform(state.preferences)

        val currentUserId = state.session.currentUserId
        if (currentUserId == null) {
            local.updatePreferences(newPrefs)
            return
        }

        when (authMode) {
            AuthMode.LOCAL_ONLY -> {
                local.updatePreferences(newPrefs)
            }

            AuthMode.LOCAL_THEN_REMOTE -> {
                try {
                    val remotePrefs = remote.updatePreferences(currentUserId, newPrefs)
                    local.updatePreferences(remotePrefs)
                } catch (e: Exception) {
                    local.updatePreferences(newPrefs)
                }
            }

            AuthMode.REMOTE_ONLY -> {
                val remotePrefs = remote.updatePreferences(currentUserId, newPrefs)
                local.updatePreferences(remotePrefs)
            }
        }
    }
}
