package com.example.aiassistant.data.source

import com.example.aiassistant.data.local.UserStateDao
import com.example.aiassistant.data.local.toEntity
import com.example.aiassistant.data.local.toModel
import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.Session
import com.example.aiassistant.data.model.User
import com.example.aiassistant.data.model.UserPreferences
import com.example.aiassistant.data.model.UserState
import com.example.aiassistant.data.source.interfac.LocalUserDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class RoomLocalUserDataSource(
    private val dao: UserStateDao,
    scope: CoroutineScope,
) : LocalUserDataSource {

    override val userState: StateFlow<UserState> = dao
        .observeUserState()
        .map { entity -> entity?.toModel() ?: UserState() }
        .stateIn(scope, SharingStarted.Eagerly, UserState())

    override suspend fun setSessionLoggedIn(userId: String) {
        updateState { state ->
            state.copy(session = Session(isLoggedIn = true, currentUserId = userId))
        }
    }

    override suspend fun setSessionLoggedOut() {
        updateState { state ->
            state.copy(session = Session(isLoggedIn = false, currentUserId = null))
        }
    }

    override suspend fun setUser(user: User?) {
        updateState { state -> state.copy(user = user) }
    }

    override suspend fun updateAvatar(avatar: Avatar) {
        updateState { state ->
            val currentUser = state.user ?: return@updateState state
            state.copy(user = currentUser.copy(avatar = avatar))
        }
    }

    override suspend fun updateDisplayName(displayName: String?) {
        updateState { state ->
            val currentUser = state.user ?: return@updateState state
            state.copy(user = currentUser.copy(displayName = displayName))
        }
    }

    override suspend fun updatePreferences(preferences: UserPreferences) {
        updateState { state -> state.copy(preferences = preferences) }
    }

    private suspend fun updateState(transform: (UserState) -> UserState) {
        withContext(Dispatchers.IO) {
            dao.getUserStateInTransaction()?.let { currentEntity ->
                val currentState = currentEntity.toModel()
                val newState = transform(currentState)
                dao.upsert(newState.toEntity())
            } ?: run {
                val initialState = UserState()
                val newState = transform(initialState)
                dao.upsert(newState.toEntity())
            }
        }
    }
}
