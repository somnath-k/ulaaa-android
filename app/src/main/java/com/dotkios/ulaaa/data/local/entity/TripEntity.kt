package com.dotkios.ulaaa.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val title: String,
    val destination: String,
    val startMillis: Long,
    val endMillis: Long,
    val squadSize: Int,
    val colorArgb: Long,
    val createdAt: Long,
)
