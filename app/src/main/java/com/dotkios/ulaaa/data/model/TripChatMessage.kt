package com.dotkios.ulaaa.data.model

data class TripChatMessage(
    val id: String,
    val senderUid: String,
    val senderName: String,
    val text: String,
    val at: Long,
)
