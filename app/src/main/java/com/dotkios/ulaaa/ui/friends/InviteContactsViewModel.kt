package com.dotkios.ulaaa.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.UserProfile
import com.dotkios.ulaaa.data.repository.ContactsRepository
import com.dotkios.ulaaa.data.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactInvite(
    val name: String,
    val phoneRaw: String,
    val phoneKey: String,
    val user: UserProfile?,
)

data class InviteContactsUiState(
    val isLoading: Boolean = false,
    val contacts: List<ContactInvite> = emptyList(),
    val requestedUids: Set<String> = emptySet(),
    val error: String? = null,
    val loaded: Boolean = false,
)

@HiltViewModel
class InviteContactsViewModel @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val friendRepository: FriendRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteContactsUiState())
    val uiState: StateFlow<InviteContactsUiState> = _uiState.asStateFlow()

    private val _smsInvite = MutableStateFlow<String?>(null)
    val smsInvite: StateFlow<String?> = _smsInvite.asStateFlow()

    /** Call once the READ_CONTACTS permission is granted. */
    fun load() {
        if (_uiState.value.isLoading || _uiState.value.loaded) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val contacts = contactsRepository.readContacts()
            val matches = friendRepository.findUsersByPhones(contacts.map { it.phoneKey })
                .getOrDefault(emptyMap())
            val list = contacts
                .map { ContactInvite(it.name, it.phoneRaw, it.phoneKey, matches[it.phoneKey]) }
                .sortedByDescending { it.user != null } // people already on Ulaaa first
            _uiState.update {
                it.copy(isLoading = false, contacts = list, loaded = true)
            }
        }
    }

    fun addFriend(user: UserProfile) {
        viewModelScope.launch {
            friendRepository.sendRequest(user).onSuccess {
                _uiState.update { s -> s.copy(requestedUids = s.requestedUids + user.uid) }
            }
        }
    }

    fun invite(phoneRaw: String) {
        _smsInvite.value = phoneRaw
    }

    fun consumeSmsInvite() {
        _smsInvite.value = null
    }
}
