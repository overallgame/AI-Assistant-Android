package com.example.aiassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiassistant.data.model.AppearancePreference
import com.example.aiassistant.data.model.Avatar
import com.example.aiassistant.data.model.UserState
import com.example.aiassistant.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    val userState: StateFlow<UserState> = userRepository.userState

    fun setAppearance(appearance: AppearancePreference) {
        viewModelScope.launch {
            userRepository.updateAppearance(appearance)
        }
    }

    fun setFontFollowSystem(followSystem: Boolean) {
        viewModelScope.launch {
            val current = userRepository.userState.value.preferences.fontSize
            userRepository.updateFontSize(current.copy(followSystem = followSystem))
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            val current = userRepository.userState.value.preferences.fontSize
            userRepository.updateFontSize(current.copy(scale = scale))
        }
    }

    fun setAvatar(avatar: Avatar) {
        viewModelScope.launch {
            userRepository.updateAvatar(avatar)
        }
    }

    fun setDisplayName(displayName: String?) {
        viewModelScope.launch {
            userRepository.updateDisplayName(displayName)
        }
    }
}
