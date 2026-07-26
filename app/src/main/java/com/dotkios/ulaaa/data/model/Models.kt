package com.dotkios.ulaaa.data.model

import androidx.compose.ui.graphics.Color

/** A planned trip owned/joined by the user, taken with a Squad. */
data class Trip(
    val id: String,
    val title: String,
    val destination: String,
    val dateRange: String,
    val squadSize: Int,
    val days: Int,
    val accent: Color,
    val imageUrl: String? = null,
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
)

/** AI weather outlook + concerns for a trip's destination and dates. */
data class TripAdvice(
    val weather: String,
    val bestTime: String,
    val concerns: List<String>,
)

/** A discoverable place / point of interest. */
data class Landmark(
    val id: String,
    val name: String,
    val category: String,
    val distanceKm: Double,
    val rating: Double,
    val accent: Color,
    val imageUrl: String? = null,
    val description: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
)

/** A curated itinerary surfaced in the "Curated For You" rail. */
data class CuratedItinerary(
    val id: String,
    val title: String,
    val subtitle: String,
    val stopCount: Int,
    val accent: Color,
    val destination: String = "",
    val days: Int = 3,
    val imageUrl: String? = null,
)

/** A discovery category chip (Beaches, Mountains, etc.). */
data class Category(
    val id: String,
    val label: String,
    val emoji: String,
)
