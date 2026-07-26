package com.dotkios.ulaaa.data.model

/** A user's photo post. No-arg defaults required for Firestore deserialization. */
data class Post(
    val id: String = "",
    val uid: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val createdAt: Long = 0L,
)
