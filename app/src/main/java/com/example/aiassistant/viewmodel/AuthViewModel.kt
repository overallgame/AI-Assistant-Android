package com.example.aiassistant.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiassistant.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val codeSentToPhoneDigits: String? = null,
    val codeSentAtEpochMs: Long? = null,
    val codeSecondsRemaining: Int = 0,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val mockSmsCode = "123456"
    private val codeTtlMs = 5 * 60 * 1000L

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun sendCode(phone: String) {
        val normalized = phone.filter { it.isDigit() }
        if (normalized.length != 11) {
            _uiState.update { it.copy(errorMessage = "请输入正确的手机号") }
            return
        }

        if (_uiState.value.codeSecondsRemaining > 0) return

        clearMessages()
        val now = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                codeSentToPhoneDigits = normalized,
                codeSentAtEpochMs = now,
                infoMessage = "验证码已发送（测试码：$mockSmsCode）",
            )
        }
        startCountdown(seconds = 60)
    }

    fun loginWithCode(phone: String, code: String, onSuccess: () -> Unit) {
        val normalizedPhone = phone.filter { it.isDigit() }
        val normalizedCode = code.filter { it.isDigit() }

        if (normalizedPhone.length != 11) {
            _uiState.update { it.copy(errorMessage = "请输入正确的手机号") }
            return
        }
        if (normalizedCode.length != 6) {
            _uiState.update { it.copy(errorMessage = "请输入 6 位验证码") }
            return
        }

        val sentTo = _uiState.value.codeSentToPhoneDigits
        val sentAt = _uiState.value.codeSentAtEpochMs
        if (sentTo == null || sentAt == null || sentTo != normalizedPhone) {
            _uiState.update { it.copy(errorMessage = "请先发送验证码") }
            return
        }

        val now = System.currentTimeMillis()
        if (now - sentAt > codeTtlMs) {
            _uiState.update { it.copy(errorMessage = "验证码已过期，请重新获取") }
            return
        }

        if (normalizedCode != mockSmsCode) {
            _uiState.update { it.copy(errorMessage = "验证码错误") }
            return
        }

        clearMessages()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                userRepository.loginWithPhone("+86$normalizedPhone")
                onSuccess()
            } catch (t: Throwable) {
                _uiState.update { it.copy(errorMessage = "登录失败，请稍后重试") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loginWithPassword(phone: String, password: String, onSuccess: () -> Unit) {
        val normalizedPhone = phone.filter { it.isDigit() }
        if (normalizedPhone.length != 11) {
            _uiState.update { it.copy(errorMessage = "请输入正确的手机号") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "密码至少 6 位") }
            return
        }

        clearError()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                delay(150)
                userRepository.loginWithPhone("+86$normalizedPhone")
                onSuccess()
            } catch (t: Throwable) {
                _uiState.update { it.copy(errorMessage = "登录失败，请稍后重试") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        clearError()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                userRepository.logout()
                onSuccess()
            } catch (t: Throwable) {
                _uiState.update { it.copy(errorMessage = "退出失败，请稍后重试") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (s in seconds downTo 0) {
                _uiState.update { it.copy(codeSecondsRemaining = s) }
                delay(1000)
            }
        }
    }
}
