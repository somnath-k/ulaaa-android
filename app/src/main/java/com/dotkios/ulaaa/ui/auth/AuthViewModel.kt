package com.dotkios.ulaaa.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun login() {
        val state = _uiState.value
        val validation = validate(state.email, state.password)
        if (validation != null) {
            _uiState.update { it.copy(error = validation) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.signIn(state.email, state.password)
                .onSuccess { _uiState.update { s -> s.copy(isLoading = false, isAuthenticated = true) } }
                .onFailure { e -> _uiState.update { s -> s.copy(isLoading = false, error = e.friendly()) } }
        }
    }

    fun signup() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Enter your name") }
            return
        }
        val validation = validate(state.email, state.password)
        if (validation != null) {
            _uiState.update { it.copy(error = validation) }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.signUp(state.name, state.email, state.password)
                .onSuccess { _uiState.update { s -> s.copy(isLoading = false, isAuthenticated = true) } }
                .onFailure { e -> _uiState.update { s -> s.copy(isLoading = false, error = e.friendly()) } }
        }
    }

    private fun validate(email: String, password: String): String? = when {
        email.isBlank() -> "Enter your email"
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email"
        password.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }

    private fun Throwable.friendly(): String =
        message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Try again."
}
