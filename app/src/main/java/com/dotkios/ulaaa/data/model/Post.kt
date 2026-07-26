package com.dotkios.ulaaa.data.model

/** A user's photo post. No-arg defaults required for Firestore deserialization. */
data class Post(
    val id: String = "",
    val uid: String = "",
    // Denormalized author info so the feed renders without extra lookups.
    val authorName: String = "",
    val authorPhotoUrl: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val likes: List<String> = emptyList(),
    val createdAt: Long = 0L,
)
