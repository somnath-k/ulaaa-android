package com.dotkios.ulaaa.ui.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.Category
import com.dotkios.ulaaa.data.model.CuratedItinerary
import com.dotkios.ulaaa.data.model.Landmark
import com.dotkios.ulaaa.data.repository.AuthRepository
import com.dotkios.ulaaa.data.repository.RecommendationRepository
import com.dotkios.ulaaa.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    tripRepository: TripRepository,
    authRepository: AuthRepository,
    private val recommendationRepository: RecommendationRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    // Seeded with mock data so the rail is never empty; Gemini overwrites it on success.
    private val curated = MutableStateFlow(MockData.curated)

    private val userName: String =
        authRepository.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Explorer"

    val uiState: StateFlow<HomeUiState> =
        combine(query, tripRepository.trips, curated) { q, trips, curatedList ->
            HomeUiState(
                isLoading = false,
                userName = userName,
                query = q,
                trips = trips,
                curated = curatedList,
                nearbyLandmarks = MockData.landmarks,
                categories = MockData.categories,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true, userName = userName),
        )

    init {
        loadRecommendations()
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            recommendationRepository.curatedItineraries("beaches, heritage and mountains")
                .onSuccess { list -> if (list.isNotEmpty()) curated.value = list }
            // On failure we keep the seeded mock list — no user-facing error for a soft feature.
        }
    }
}

/** Curated/landmark/category rails stay mocked until their APIs are wired (Gemini, Geoapify feed). */
private object MockData {
    private val teal = Color(0xFF0E7C7B)
    private val coral = Color(0xFFFF6B57)
    private val sand = Color(0xFFF4A259)
    private val indigo = Color(0xFF5A6FEA)

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
