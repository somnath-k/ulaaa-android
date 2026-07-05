package com.dotkios.ulaaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PexelsResponse(
    val photos: List<PexelsPhoto> = emptyList(),
)

@Serializable
data class PexelsPhoto(
    val src: PexelsSrc? = null,
)

@Serializable
data class PexelsSrc(
    val large: String? = null,
    val medium: String? = null,
)
