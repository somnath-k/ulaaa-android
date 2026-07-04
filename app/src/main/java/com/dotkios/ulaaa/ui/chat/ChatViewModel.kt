package com.dotkios.ulaaa.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dotkios.ulaaa.data.model.ChatMessage
import com.dotkios.ulaaa.data.model.ChatRole
import com.dotkios.ulaaa.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(ChatRole.ASSISTANT, "Hi, I'm Dot 👋 Ask me anything about your next trip."),
    ),
    val isSending: Boolean = false,
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun send(text: String) {
        val question = text.trim()
        if (question.isBlank() || _uiState.value.isSending) return

        val history = _uiState.value.messages
        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(ChatRole.USER, question),
                isSending = true,
            )
        }

        viewModelScope.launch {
            val reply = chatRepository.ask(history, question)
                .getOrElse { e -> "Dot hit a snag: ${e.message ?: "unknown error"}. Try again." }
            _uiState.update {
                it.copy(
                    messages = it.messages + ChatMessage(ChatRole.ASSISTANT, reply),
                    isSending = false,
                )
            }
        }
    }
}
