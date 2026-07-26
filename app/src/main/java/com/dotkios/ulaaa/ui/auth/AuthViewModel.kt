package com.dotkios.ulaaa.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.repository.AuthRepository
import com.dotkios.ulaaa.data.repository.PhoneVerification
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

    fun onPhoneChange(value: String) =
        _uiState.update { it.copy(phone = value.filter { c -> c.isDigit() || c == '+' }, error = null) }

    fun onOtpChange(value: String) =
        _uiState.update { it.copy(otp = value.filter { c -> c.isDigit() }.take(6), error = null) }

    fun useMethod(method: AuthMethod) = _uiState.update {
        it.copy(method = method, error = null, verificationId = null, otp = "")
    }

    /** Back out of code entry to edit the phone number. */
    fun editPhone() = _uiState.update { it.copy(verificationId = null, otp = "", error = null) }

    fun sendOtp(activity: Activity) {
        val phone = _uiState.value.phone
        val e164 = toE164(phone) ?: run {
            _uiState.update { it.copy(error = "Enter a valid mobile number") }
            return
        }
        _uiState.update { it.copy(otpSending = true, error = null) }
        viewModelScope.launch {
            authRepository.startPhoneVerification(activity, e164)
                .onSuccess { result ->
                    when (result) {
                        is PhoneVerification.CodeSent ->
                            _uiState.update { it.copy(otpSending = false, verificationId = result.verificationId) }
                        PhoneVerification.AutoSignedIn ->
                            _uiState.update { it.copy(otpSending = false, isAuthenticated = true) }
                    }
                }
                .onFailure { e -> _uiState.update { it.copy(otpSending = false, error = e.friendly()) } }
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        val verificationId = state.verificationId ?: return
        if (state.otp.length != 6) {
            _uiState.update { it.copy(error = "Enter the 6-digit code") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.verifyOtp(verificationId, state.otp)
                .onSuccess { _uiState.update { s -> s.copy(isLoading = false, isAuthenticated = true) } }
                .onFailure { e -> _uiState.update { s -> s.copy(isLoading = false, error = e.friendly()) } }
        }
    }

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

    fun signInWithGoogle(idToken: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken)
                .onSuccess { _uiState.update { s -> s.copy(isLoading = false, isAuthenticated = true) } }
                .onFailure { e -> _uiState.update { s -> s.copy(isLoading = false, error = e.friendly()) } }
        }
    }

    fun onGoogleError(message: String) {
        _uiState.update { it.copy(isLoading = false, error = message) }
    }

    private fun validate(email: String, password: String): String? = when {
        email.isBlank() -> "Enter your email"
        !EMAIL_REGEX.matches(email.trim()) -> "Enter a valid email"
        password.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }

    private fun Throwable.friendly(): String =
        message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Try again."

    /** Builds an E.164 number, defaulting to India (+91) when no country code is given. */
    private fun toE164(raw: String): String? {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("+") -> trimmed.takeIf { it.length in 8..16 }
            trimmed.length == 10 -> "+91$trimmed"
            else -> null
        }
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
