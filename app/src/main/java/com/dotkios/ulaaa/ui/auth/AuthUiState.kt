package com.dotkios.ulaaa.ui.auth

/** Which login method the user is currently using on the login screen. */
enum class AuthMethod { PHONE, EMAIL }

data class AuthUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    // Phone OTP
    val method: AuthMethod = AuthMethod.PHONE,
    val phone: String = "",
    val otp: String = "",
    /** Non-null once an OTP has been sent — the screen then shows the code entry. */
    val verificationId: String? = null,
    val otpSending: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
)
