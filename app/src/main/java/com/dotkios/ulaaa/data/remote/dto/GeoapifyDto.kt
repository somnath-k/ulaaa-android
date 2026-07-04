package com.dotkios.ulaaa.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeoapifyResponse(
    val features: List<GeoapifyFeature> = emptyList(),
)

@Serializable
data class GeoapifyFeature(
    val properties: GeoapifyProperties,
)

@Serializable
data class GeoapifyProperties(
    val name: String? = null,
    val categories: List<String> = emptyList(),
    val distance: Int? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("place_id") val placeId: String? = null,
)
