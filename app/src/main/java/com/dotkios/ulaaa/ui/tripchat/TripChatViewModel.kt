package com.dotkios.ulaaa.ui.tripchat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.TripChatMessage
import com.dotkios.ulaaa.data.repository.TripChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TripChatRepository,
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])

    val currentUid: String? = repository.currentUid

    val messages: StateFlow<List<TripChatMessage>> = repository.messages(tripId).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { repository.send(tripId, text) }
    }
}
