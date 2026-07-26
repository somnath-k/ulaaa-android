package com.dotkios.ulaaa.data.model

/** Firestore-backed user profile. No-arg defaults required for deserialization. */
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    /** Normalized phone (digits only, last 10) — used to match contacts to users. */
    val phone: String = "",
    /** Firebase Storage download URL for the profile picture (empty if none). */
    val photoUrl: String = "",
    val createdAt: Long = 0L,
)
