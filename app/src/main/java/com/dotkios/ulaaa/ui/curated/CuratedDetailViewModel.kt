package com.dotkios.ulaaa.ui.curated

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.ItineraryStop
import com.dotkios.ulaaa.data.repository.ItineraryRepository
import com.dotkios.ulaaa.data.repository.RecommendationRepository
import com.dotkios.ulaaa.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CuratedDetailUiState(
    val title: String = "",
    val destination: String = "",
    val days: Int = 3,
    val stops: List<ItineraryStop> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val savedTripId: String? = null,
)

@HiltViewModel
class CuratedDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recommendationRepository: RecommendationRepository,
    private val tripRepository: TripRepository,
    private val itineraryRepository: ItineraryRepository,
) : ViewModel() {

    private val title: String = savedStateHandle["title"] ?: "Trip"
    private val destination: String = savedStateHandle["destination"] ?: title
    private val days: Int = savedStateHandle.get<String>("days")?.toIntOrNull() ?: 3

    private val _uiState = MutableStateFlow(
        CuratedDetailUiState(title = title, destination = destination, days = days),
    )
    val uiState: StateFlow<CuratedDetailUiState> = _uiState.asStateFlow()

    init {
        generate()
    }

    private fun generate() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            recommendationRepository.itineraryFor(destination, days)
                .onSuccess { stops -> _uiState.update { it.copy(isLoading = false, stops = stops) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Couldn't build itinerary") } }
        }
    }

    /** Create a trip from this curated idea and copy the itinerary into it. */
    fun saveAsTrip() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val tripId = tripRepository.addTrip(title, destination, 0L, 0L)
            if (tripId.isBlank()) {
                _uiState.update { it.copy(isSaving = false, error = "Couldn't create trip") }
                return@launch
            }
            _uiState.value.stops.forEach { stop ->
                itineraryRepository.addStop(tripId, stop.day, stop.title, stop.detail, stop.cost)
            }
            _uiState.update { it.copy(isSaving = false, savedTripId = tripId) }
        }
    }
}
