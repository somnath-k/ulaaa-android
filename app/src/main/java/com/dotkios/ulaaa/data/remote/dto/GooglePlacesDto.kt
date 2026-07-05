package com.dotkios.ulaaa.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GooglePlacesResponse(
    val results: List<GooglePlaceResult> = emptyList(),
)

@Serializable
data class GooglePlaceResult(
    val photos: List<GooglePlacePhoto> = emptyList(),
)

@Serializable
data class GooglePlacePhoto(
    @SerialName("photo_reference") val photoReference: String? = null,
)
