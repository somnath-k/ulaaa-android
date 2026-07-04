package com.dotkios.ulaaa.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.repository.AuthRepository
import com.dotkios.ulaaa.data.repository.UserRepository
import com.dotkios.ulaaa.util.normalizePhone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val uid: String? = authRepository.currentUser?.uid

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            name = authRepository.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Explorer",
            email = authRepository.currentUser?.email.orEmpty(),
        ),
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadPhone()
    }

    private fun loadPhone() {
        val id = uid ?: return
        viewModelScope.launch {
            userRepository.getProfile(id).onSuccess { profile ->
                if (profile != null) _uiState.update { it.copy(phone = profile.phone) }
            }
        }
    }

    fun setPhone(raw: String) {
        val id = uid ?: return
        val normalized = normalizePhone(raw)
        viewModelScope.launch {
            userRepository.setPhone(id, normalized).onSuccess {
                _uiState.update { it.copy(phone = normalized) }
            }
        }
    }

    fun signOut() = authRepository.signOut()
}
