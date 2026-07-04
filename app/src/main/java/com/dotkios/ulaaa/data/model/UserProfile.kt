package com.dotkios.ulaaa.data.model

/** Firestore-backed user profile. No-arg defaults required for deserialization. */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val createdAt: Long = 0L,
)
