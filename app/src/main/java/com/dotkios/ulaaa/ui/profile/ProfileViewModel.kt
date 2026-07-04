package com.dotkios.ulaaa.ui.profile

import androidx.lifecycle.ViewModel
import com.dotkios.ulaaa.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: ProfileUiState = authRepository.currentUser.let { user ->
        ProfileUiState(
            name = user?.displayName?.takeIf { it.isNotBlank() } ?: "Explorer",
            email = user?.email.orEmpty(),
        )
    }

    fun signOut() = authRepository.signOut()
}
