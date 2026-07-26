package com.dotkios.ulaaa.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.Trip
import com.dotkios.ulaaa.data.model.TripInvite
import com.dotkios.ulaaa.data.repository.TripDetailRepository
import com.dotkios.ulaaa.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TripsViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    private val detailRepository: TripDetailRepository,
) : ViewModel() {

    val trips: StateFlow<List<Trip>> = tripRepository.trips.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val invites: StateFlow<List<TripInvite>> = detailRepository.pendingInvites().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun accept(invite: TripInvite) {
        viewModelScope.launch { detailRepository.acceptInvite(invite.tripId) }
    }

    fun decline(invite: TripInvite) {
        viewModelScope.launch { detailRepository.declineInvite(invite.tripId) }
    }

    fun delete(id: String) {
        viewModelScope.launch { tripRepository.deleteTrip(id) }
    }
}
