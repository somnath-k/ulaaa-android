package com.dotkios.ulaaa.data.model

enum class ChatRole { USER, ASSISTANT }

data class ChatMessage(
    val role: ChatRole,
    val text: String,
)
