package com.dotkios.ulaaa.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.FriendLocation
import com.dotkios.ulaaa.data.model.GeoPoint
import com.dotkios.ulaaa.data.model.Landmark
import com.dotkios.ulaaa.data.repository.FriendLocationRepository
import com.dotkios.ulaaa.data.repository.FriendRepository
import com.dotkios.ulaaa.data.repository.LocationRepository
import com.dotkios.ulaaa.data.repository.PlacesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val isLoading: Boolean = false,
    val location: GeoPoint? = null,
    val landmarks: List<Landmark> = emptyList(),
    val friends: List<FriendLocation> = emptyList(),
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val placesRepository: PlacesRepository,
    private val friendRepository: FriendRepository,
    private val friendLocationRepository: FriendLocationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        // Live friend locations: for the current friend list, watch their shared positions.
        viewModelScope.launch {
            friendRepository.friends()
                .flatMapLatest { friends -> friendLocationRepository.observeFriends(friends.map { it.uid }) }
                .collect { locations -> _uiState.update { it.copy(friends = locations) } }
        }
    }

    /** Call once the location permission is granted. */
    fun loadNearby() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            locationRepository.currentLocation()
                .onSuccess { point ->
                    if (point == null) {
                        _uiState.update { it.copy(isLoading = false, error = "Couldn't read your location. Enable GPS and retry.") }
                        return@onSuccess
                    }
                    _uiState.update { it.copy(location = point) }
                    // Share own location so friends can see us on their map.
                    friendLocationRepository.publishMyLocation(point.lat, point.lon)
                    placesRepository.nearbyLandmarks(point.lat, point.lon)
                        .onSuccess { list -> _uiState.update { it.copy(isLoading = false, landmarks = list) } }
                        .onFailure { e -> _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to load places") } }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Location error") }
                }
        }
    }
}
