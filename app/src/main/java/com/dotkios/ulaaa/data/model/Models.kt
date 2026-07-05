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
)

/** A curated itinerary surfaced in the "Curated For You" rail. */
data class CuratedItinerary(
    val id: String,
    val title: String,
    val subtitle: String,
    val stopCount: Int,
    val accent: Color,
)

/** A discovery category chip (Beaches, Mountains, etc.). */
data class Category(
    val id: String,
    val label: String,
    val emoji: String,
)
