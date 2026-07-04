package com.dotkios.ulaaa.ui.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.Category
import com.dotkios.ulaaa.data.model.CuratedItinerary
import com.dotkios.ulaaa.data.model.Landmark
import com.dotkios.ulaaa.data.model.Trip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    private fun loadHome() {
        viewModelScope.launch {
            // Simulates a repository fetch. Swap for real Repository call in Sprint 3+.
            delay(600)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    userName = "Sachin",
                    trips = MockData.trips,
                    curated = MockData.curated,
                    nearbyLandmarks = MockData.landmarks,
                    categories = MockData.categories,
                )
            }
        }
    }
}

/** Temporary in-memory content until real APIs (Ola Maps, Geoapify) are wired in. */
private object MockData {
    private val teal = Color(0xFF0E7C7B)
    private val coral = Color(0xFFFF6B57)
    private val sand = Color(0xFFF4A259)
    private val indigo = Color(0xFF5A6FEA)

    val trips = listOf(
        Trip("t1", "Backwaters & Beaches", "Kerala, India", "Aug 12 – 18", 4, teal),
        Trip("t2", "Himalayan Escape", "Manali, India", "Sep 3 – 9", 3, indigo),
    )

    val curated = listOf(
        CuratedItinerary("c1", "48h in Pondicherry", "French quarter + cafés", 6, coral),
        CuratedItinerary("c2", "Coorg Coffee Trail", "Estates & waterfalls", 5, teal),
        CuratedItinerary("c3", "Rajasthan in 5 Days", "Forts & desert nights", 9, sand),
    )

    val landmarks = listOf(
        Landmark("l1", "Marina Beach", "Beach", 2.4, 4.5, teal),
        Landmark("l2", "Kapaleeshwarar", "Temple", 5.1, 4.7, sand),
        Landmark("l3", "Elliot's Beach", "Beach", 6.8, 4.3, indigo),
        Landmark("l4", "Guindy Park", "Nature", 8.0, 4.2, coral),
    )

    val categories = listOf(
        Category("cat1", "Beaches", "🏖️"),
        Category("cat2", "Mountains", "⛰️"),
        Category("cat3", "Heritage", "🏛️"),
        Category("cat4", "Food", "🍜"),
        Category("cat5", "Nature", "🌿"),
    )
}
