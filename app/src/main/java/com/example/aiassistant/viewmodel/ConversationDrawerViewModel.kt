package com.example.aiassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiassistant.data.model.ConversationDrawerUiState
import com.example.aiassistant.data.model.ConversationGroup
import com.example.aiassistant.data.model.DrawerUserInfo
import com.example.aiassistant.data.repository.interfac.ConversationRepository
import com.example.aiassistant.data.repository.interfac.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationDrawerViewModel @Inject constructor(
    userRepository: UserRepository,
    private val conversationRepository: ConversationRepository,
) : ViewModel() {

    private val selectedConversationId = MutableStateFlow<String?>(null)
    private val groups = MutableStateFlow<List<ConversationGroup>>(emptyList())
    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ConversationDrawerUiState> = combine(
        userRepository.userState,
        selectedConversationId,
        groups,
        isLoading,
        errorMessage,
    ) { userState, selectedId, groups, isLoading, errorMessage ->
        ConversationDrawerUiState(
            groups = groups,
            selectedConversationId = selectedId,
            userInfo = DrawerUserInfo(
                phoneMasked = userState.user?.phoneMasked.orEmpty(),
                displayName = userState.user?.displayName,
                avatar = userState.user?.avatar,
            ),
            isLoading = isLoading,
            errorMessage = errorMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConversationDrawerUiState())

    init {
        refresh(forceRefresh = false)
    }

    fun refresh(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                groups.value = conversationRepository.fetchConversationGroups(forceRefresh = forceRefresh)
            } catch (t: Throwable) {
                errorMessage.value = t.message ?: "加载失败"
                groups.value = emptyList()
            } finally {
                isLoading.value = false
            }
        }
    }

    fun selectConversation(conversationId: String) {
        selectedConversationId.value = conversationId
    }
}
