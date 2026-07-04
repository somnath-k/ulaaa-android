package com.dotkios.ulaaa.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "itinerary_stops",
    indices = [Index("tripId")],
)
data class ItineraryStopEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val day: Int,
    val title: String,
    val detail: String,
    val orderIndex: Int,
)
