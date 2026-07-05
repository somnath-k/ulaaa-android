package com.dotkios.ulaaa.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateTripViewModel @Inject constructor(
    private val tripRepository: TripRepository,
) : ViewModel() {

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun save(
        title: String,
        destination: String,
        startMillis: Long,
        endMillis: Long,
    ) {
        if (title.isBlank() || destination.isBlank()) return
        viewModelScope.launch {
            tripRepository.addTrip(title, destination, startMillis, endMillis)
            _saved.update { true }
        }
    }
}
