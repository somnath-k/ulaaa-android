package com.dotkios.ulaaa.ui.trip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.ChecklistItem
import com.dotkios.ulaaa.data.model.Expense
import com.dotkios.ulaaa.data.model.Friend
import com.dotkios.ulaaa.data.model.SplitResult
import com.dotkios.ulaaa.data.model.Trip
import com.dotkios.ulaaa.data.model.TripMember
import com.dotkios.ulaaa.data.repository.FriendRepository
import com.dotkios.ulaaa.data.repository.TripDetailRepository
import com.dotkios.ulaaa.data.repository.TripRepository
import com.dotkios.ulaaa.domain.SplitCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripRepository: TripRepository,
    private val detailRepository: TripDetailRepository,
    friendRepository: FriendRepository,
) : ViewModel() {

    private val tripId: String = checkNotNull(savedStateHandle["tripId"])

    val uiState: StateFlow<TripDetailUiState> = combine(
        tripRepository.trip(tripId),
        detailRepository.members(tripId),
        detailRepository.checklist(tripId),
        detailRepository.expenses(tripId),
        friendRepository.friends(),
    ) { trip, members, checklist, expenses, friends ->
        val memberUids = members.map { it.uid }.toSet()
        TripDetailUiState(
            trip = trip,
            members = members,
            checklist = checklist,
            expenses = expenses,
            split = SplitCalculator.calculate(members.map { it.name }, expenses),
            addableFriends = friends.filter { it.uid !in memberUids },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TripDetailUiState(),
    )

    fun addMember(friend: Friend) = launch { detailRepository.addMember(tripId, friend.uid, friend.name) }
    fun deleteMember(id: String) = launch { detailRepository.deleteMember(id) }

    fun addChecklistItem(text: String) = launch { detailRepository.addChecklistItem(tripId, text) }
    fun toggleChecklist(id: String, done: Boolean) = launch { detailRepository.setChecklistDone(id, done) }
    fun deleteChecklistItem(id: String) = launch { detailRepository.deleteChecklistItem(id) }

    fun addExpense(title: String, amount: Double, paidBy: String) =
        launch { detailRepository.addExpense(tripId, title, amount, paidBy) }
    fun deleteExpense(id: String) = launch { detailRepository.deleteExpense(id) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
