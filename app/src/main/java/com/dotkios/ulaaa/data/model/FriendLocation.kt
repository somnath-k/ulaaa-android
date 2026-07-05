package com.dotkios.ulaaa.data.model

/** A friend's last-shared location, for the map. */
data class FriendLocation(
    val uid: String,
    val name: String,
    val lat: Double,
    val lon: Double,
)
