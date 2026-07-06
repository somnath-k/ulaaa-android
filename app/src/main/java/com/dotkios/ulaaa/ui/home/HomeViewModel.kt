package com.dotkios.ulaaa.ui.home

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.Category
import com.dotkios.ulaaa.data.model.CuratedItinerary
import com.dotkios.ulaaa.data.model.Landmark
import com.dotkios.ulaaa.data.repository.AuthRepository
import com.dotkios.ulaaa.data.repository.LocationRepository
import com.dotkios.ulaaa.data.repository.PlaceImageRepository
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
    private val placeImageRepository: PlaceImageRepository,
    private val locationRepository: LocationRepository,
    private val recommendationRepository: RecommendationRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    // Static curated rail — no per-open Gemini call (that drained the tiny free quota).
    private val curated = MutableStateFlow(MockData.curated)

    // Landmarks: no mock — empty + loading until Gemini + photos resolve.
    private val landmarks = MutableStateFlow<List<Landmark>>(emptyList())
    private val landmarksLoading = MutableStateFlow(true)

    private val userName: String =
        authRepository.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Explorer"

    val uiState: StateFlow<HomeUiState> =
        combine(query, tripRepository.trips, curated, landmarks, landmarksLoading) { q, trips, curatedList, landmarkList, loading ->
            HomeUiState(
                isLoading = false,
                userName = userName,
                query = q,
                trips = trips,
                curated = curatedList,
                nearbyLandmarks = landmarkList,
                nearbyLoading = loading,
                categories = MockData.categories,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(isLoading = true, userName = userName),
        )

    init {
        loadNearbyLandmarks()
    }

    private fun loadNearbyLandmarks() {
        viewModelScope.launch {
            landmarksLoading.value = true
            val place = locationRepository.currentCity() ?: "India"
            val list = recommendationRepository.nearbyLandmarks(place).getOrNull().orEmpty()
            // Resolve real photos before publishing so cards appear with images, not blank.
            landmarks.value = list.map { landmark ->
                landmark.copy(imageUrl = placeImageRepository.resolve(landmark.name, landmark.category).url)
            }
            landmarksLoading.value = false
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }
}

/** Curated/landmark/category rails stay mocked until their APIs are wired (Gemini, Geoapify feed). */
private object MockData {
    private val teal = Color(0xFF0E7C7B)
    private val coral = Color(0xFFFF6B57)
    private val sand = Color(0xFFF4A259)

    val curated = listOf(
        CuratedItinerary("c1", "48h in Pondicherry", "French quarter + cafés", 6, coral),
        CuratedItinerary("c2", "Coorg Coffee Trail", "Estates & waterfalls", 5, teal),
        CuratedItinerary("c3", "Rajasthan in 5 Days", "Forts & desert nights", 9, sand),
    )

    val categories = listOf(
        Category("cat1", "Beaches", "🏖️"),
        Category("cat2", "Mountains", "⛰️"),
        Category("cat3", "Heritage", "🏛️"),
        Category("cat4", "Food", "🍜"),
        Category("cat5", "Nature", "🌿"),
    )
}
