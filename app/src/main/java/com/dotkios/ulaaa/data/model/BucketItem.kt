package com.dotkios.ulaaa.data.model

import androidx.compose.ui.graphics.Color

/** A saved bucket-list destination the user wants to visit. */
data class BucketItem(
    val id: String,
    val name: String,
    val location: String,
    val note: String,
    val accent: Color,
)
