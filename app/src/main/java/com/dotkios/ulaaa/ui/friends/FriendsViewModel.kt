package com.dotkios.ulaaa.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.Friend
import com.dotkios.ulaaa.data.model.FriendRequest
import com.dotkios.ulaaa.data.model.UserProfile
import com.dotkios.ulaaa.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val result: UserProfile? = null,
    val message: String? = null,
    val isSearching: Boolean = false,
)

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
) : ViewModel() {

    val friends: StateFlow<List<Friend>> = friendRepository.friends().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    val requests: StateFlow<List<FriendRequest>> = friendRepository.incomingRequests().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    private val _search = MutableStateFlow(SearchState())
    val search: StateFlow<SearchState> = _search.asStateFlow()

    fun search(email: String) {
        if (email.isBlank()) return
        _search.update { it.copy(isSearching = true, message = null, result = null) }
        viewModelScope.launch {
            friendRepository.searchByEmail(email)
                .onSuccess { profile ->
                    _search.update {
                        it.copy(
                            isSearching = false,
                            result = profile,
                            message = if (profile == null) "No Ulaaa user with that email." else null,
                        )
                    }
                }
                .onFailure { e ->
                    _search.update { it.copy(isSearching = false, message = e.message ?: "Search failed") }
                }
        }
    }

    fun sendRequest(profile: UserProfile) {
        viewModelScope.launch {
            friendRepository.sendRequest(profile)
                .onSuccess {
                    _search.update { SearchState(message = "Request sent to ${profile.name}.") }
                }
                .onFailure { e ->
                    _search.update { it.copy(message = e.message ?: "Could not send request") }
                }
        }
    }

    fun accept(request: FriendRequest) {
        viewModelScope.launch { friendRepository.acceptRequest(request) }
    }

    fun decline(fromUid: String) {
        viewModelScope.launch { friendRepository.declineRequest(fromUid) }
    }

    fun remove(uid: String) {
        viewModelScope.launch { friendRepository.removeFriend(uid) }
    }
}
