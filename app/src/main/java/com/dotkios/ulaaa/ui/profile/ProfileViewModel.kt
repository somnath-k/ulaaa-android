package com.dotkios.ulaaa.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.Post
import com.dotkios.ulaaa.data.repository.AuthRepository
import com.dotkios.ulaaa.data.repository.MediaRepository
import com.dotkios.ulaaa.data.repository.PostRepository
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
    val photoUrl: String = "",
    val posts: List<Post> = emptyList(),
    val uploadingPhoto: Boolean = false,
    val posting: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val mediaRepository: MediaRepository,
    private val postRepository: PostRepository,
) : ViewModel() {

    private val uid: String? = authRepository.currentUser?.uid

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            name = authRepository.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Explorer",
            email = authRepository.currentUser?.email.orEmpty(),
            photoUrl = authRepository.currentUser?.photoUrl?.toString().orEmpty(),
        ),
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
        observePosts()
    }

    private fun loadProfile() {
        val id = uid ?: return
        viewModelScope.launch {
            userRepository.getProfile(id).onSuccess { profile ->
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            phone = profile.phone,
                            photoUrl = profile.photoUrl.ifBlank { it.photoUrl },
                        )
                    }
                }
            }
        }
    }

    private fun observePosts() {
        val id = uid ?: return
        viewModelScope.launch {
            postRepository.userPosts(id).collect { posts ->
                _uiState.update { it.copy(posts = posts) }
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

    fun changePhoto(image: Uri) {
        val id = uid ?: return
        _uiState.update { it.copy(uploadingPhoto = true, error = null) }
        viewModelScope.launch {
            mediaRepository.uploadProfilePhoto(id, image)
                .onSuccess { url ->
                    userRepository.setPhotoUrl(id, url)
                    _uiState.update { it.copy(uploadingPhoto = false, photoUrl = url) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(uploadingPhoto = false, error = e.uploadError()) }
                }
        }
    }

    fun addPost(image: Uri, caption: String) {
        val id = uid ?: return
        _uiState.update { it.copy(posting = true, error = null) }
        viewModelScope.launch {
            val postId = postRepository.newPostId()
            mediaRepository.uploadPostImage(id, postId, image)
                .onSuccess { url ->
                    postRepository.addPost(id, url, caption, postId)
                    _uiState.update { it.copy(posting = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(posting = false, error = e.uploadError()) }
                }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch { postRepository.deletePost(post.id) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun signOut() = authRepository.signOut()

    private fun Throwable.uploadError(): String =
        message?.takeIf { it.isNotBlank() } ?: "Upload failed. Check your connection and try again."
}
