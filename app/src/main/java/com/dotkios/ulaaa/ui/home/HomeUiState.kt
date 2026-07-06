package com.dotkios.ulaaa.ui.home

import com.dotkios.ulaaa.data.model.Category
import com.dotkios.ulaaa.data.model.CuratedItinerary
import com.dotkios.ulaaa.data.model.Landmark
import com.dotkios.ulaaa.data.model.Trip

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "Explorer",
    val trips: List<Trip> = emptyList(),
    val curated: List<CuratedItinerary> = emptyList(),
    val curatedLoading: Boolean = true,
    val nearbyLandmarks: List<Landmark> = emptyList(),
    val nearbyLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
)
