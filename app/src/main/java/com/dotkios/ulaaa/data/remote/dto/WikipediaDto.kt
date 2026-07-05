package com.dotkios.ulaaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WikiSummary(
    val title: String? = null,
    val extract: String? = null,
    val thumbnail: WikiImage? = null,
    val originalimage: WikiImage? = null,
)

@Serializable
data class WikiImage(
    val source: String? = null,
)
