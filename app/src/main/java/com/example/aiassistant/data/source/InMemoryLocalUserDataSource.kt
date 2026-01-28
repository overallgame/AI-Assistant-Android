package com.example.aiassistant.data.source

import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.Session
import com.example.aiassistant.data.model.User
import com.example.aiassistant.data.model.UserPreferences
import com.example.aiassistant.data.model.UserState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class InMemoryLocalUserDataSource(
    initialState: UserState = UserState(),
) : LocalUserDataSource {

    private val mutableUserState = MutableStateFlow(initialState)

    override val userState: StateFlow<UserState> = mutableUserState

    override suspend fun setSessionLoggedIn(userId: String) {
        mutableUserState.update { state ->
            state.copy(
                session = Session(
                    isLoggedIn = true,
                    currentUserId = userId,
                ),
            )
        }
    }

    override suspend fun setSessionLoggedOut() {
        mutableUserState.update { state ->
            state.copy(
                session = Session(
                    isLoggedIn = false,
                    currentUserId = null,
                ),
            )
        }
    }

    override suspend fun setUser(user: User?) {
        mutableUserState.update { state ->
            state.copy(user = user)
        }
    }

    override suspend fun updateAvatar(avatar: Avatar) {
        mutableUserState.update { state ->
            val currentUser = state.user ?: return@update state
            state.copy(user = currentUser.copy(avatar = avatar))
        }
    }

    override suspend fun updateDisplayName(displayName: String?) {
        mutableUserState.update { state ->
            val currentUser = state.user ?: return@update state
            state.copy(user = currentUser.copy(displayName = displayName))
        }
    }

    override suspend fun updatePreferences(preferences: UserPreferences) {
        mutableUserState.update { state ->
            state.copy(preferences = preferences)
        }
    }
}
