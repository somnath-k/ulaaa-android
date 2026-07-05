package com.dotkios.ulaaa.ui.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.ChecklistItem
import com.dotkios.ulaaa.data.model.Expense
import com.dotkios.ulaaa.data.model.Friend
import com.dotkios.ulaaa.data.model.ItineraryStop
import com.dotkios.ulaaa.data.model.SplitResult
import com.dotkios.ulaaa.data.model.Trip
import com.dotkios.ulaaa.data.model.TripMember
import com.dotkios.ulaaa.data.repository.FriendRepository
import com.dotkios.ulaaa.data.repository.ItineraryRepository
import com.dotkios.ulaaa.data.repository.TripDetailRepository
import com.dotkios.ulaaa.data.repository.TripRepository
import com.dotkios.ulaaa.domain.SplitCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TripDetailUiState(
    val trip: Trip? = null,
    val members: List<TripMember> = emptyList(),
    val checklist: List<ChecklistItem> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val split: SplitResult = SplitResult(0.0, 0.0, emptyMap(), emptyList()),
    /** Friends not already in this trip's squad — the pool the picker adds from. */
    val addableFriends: List<Friend> = emptyList(),
    val itinerary: List<ItineraryStop> = emptyList(),
    val isGeneratingItinerary: Boolean = false,
    val itineraryError: String? = null,
)

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val detailRepository: TripDetailRepository,
    private val itineraryRepository: ItineraryRepository,
    friendRepository: FriendRepository,
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])

    private data class Content(
        val checklist: List<ChecklistItem>,
        val expenses: List<Expense>,
        val itinerary: List<ItineraryStop>,
    )

    // Transient generation status (not persisted).
    private val itineraryStatus = MutableStateFlow(GenStatus())
    private data class GenStatus(val generating: Boolean = false, val error: String? = null)

    private val contentFlow = combine(
        detailRepository.checklist(tripId),
        detailRepository.expenses(tripId),
        itineraryRepository.observe(tripId),
    ) { checklist, expenses, itinerary -> Content(checklist, expenses, itinerary) }

    val uiState: StateFlow<TripDetailUiState> = combine(
        tripRepository.trip(tripId),
        detailRepository.members(tripId),
        friendRepository.friends(),
        contentFlow,
        itineraryStatus,
    ) { trip, members, friends, content, status ->
        val memberUids = members.map { it.uid }.toSet()
        TripDetailUiState(
            trip = trip,
            members = members,
            checklist = content.checklist,
            expenses = content.expenses,
            split = SplitCalculator.calculate(members.map { it.name }, content.expenses),
            addableFriends = friends.filter { it.uid !in memberUids },
            itinerary = content.itinerary,
            isGeneratingItinerary = status.generating,
            itineraryError = status.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TripDetailUiState(),
    )

    fun addMember(friend: Friend) = launch { detailRepository.addMember(tripId, friend.uid, friend.name) }
    fun deleteMember(id: String) = launch { detailRepository.deleteMember(tripId, id) }

    fun addChecklistItem(text: String) = launch { detailRepository.addChecklistItem(tripId, text) }
    fun toggleChecklist(id: String, done: Boolean) = launch { detailRepository.setChecklistDone(tripId, id, done) }
    fun deleteChecklistItem(id: String) = launch { detailRepository.deleteChecklistItem(tripId, id) }

    fun addExpense(title: String, amount: Double, paidBy: String) =
        launch { detailRepository.addExpense(tripId, title, amount, paidBy) }
    fun deleteExpense(id: String) = launch { detailRepository.deleteExpense(tripId, id) }

    fun generateItinerary() {
        val trip = uiState.value.trip ?: return
        if (itineraryStatus.value.generating) return
        itineraryStatus.value = GenStatus(generating = true)
        viewModelScope.launch {
            itineraryRepository.generate(tripId, trip.destination, trip.days)
                .onSuccess { itineraryStatus.value = GenStatus() }
                .onFailure { e -> itineraryStatus.value = GenStatus(error = e.message ?: "Couldn't generate itinerary") }
        }
    }

    fun addItineraryStop(day: Int, title: String, detail: String, cost: Int) =
        launch { itineraryRepository.addStop(tripId, day, title, detail, cost) }

    fun deleteItineraryStop(id: String) = launch { itineraryRepository.deleteStop(tripId, id) }

    fun clearItinerary() = launch { itineraryRepository.clear(tripId) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
