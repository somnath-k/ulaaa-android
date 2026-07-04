package com.dotkios.ulaaa.data.model

/** An accepted friend (another Ulaaa user). */
data class Friend(
    val uid: String,
    val name: String,
)

/** A pending incoming friend request. */
data class FriendRequest(
    val fromUid: String,
    val name: String,
    val email: String,
)
