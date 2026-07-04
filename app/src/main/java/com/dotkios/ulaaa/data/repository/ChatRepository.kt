package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.BuildConfig
import com.dotkios.ulaaa.data.model.ChatMessage
import com.dotkios.ulaaa.data.model.ChatRole
import com.dotkios.ulaaa.data.remote.GeminiApi
import com.dotkios.ulaaa.data.remote.dto.GeminiContent
import com.dotkios.ulaaa.data.remote.dto.GeminiGenerationConfig
import com.dotkios.ulaaa.data.remote.dto.GeminiPart
import com.dotkios.ulaaa.data.remote.dto.GeminiRequest
import javax.inject.Inject
import javax.inject.Singleton

interface ChatRepository {
    /** Ask Dot a question, given the prior conversation. Returns the assistant reply text. */
    suspend fun ask(history: List<ChatMessage>, question: String): Result<String>
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: GeminiApi,
) : ChatRepository {

    override suspend fun ask(history: List<ChatMessage>, question: String): Result<String> = runCatching {
        val contents = buildList {
            history.forEach { msg ->
                add(
                    GeminiContent(
                        parts = listOf(GeminiPart(msg.text)),
                        role = if (msg.role == ChatRole.USER) "user" else "model",
                    ),
                )
            }
            add(GeminiContent(parts = listOf(GeminiPart(question)), role = "user"))
        }
        val response = api.generate(
            model = GeminiApi.MODEL,
            apiKey = BuildConfig.GEMINI_API_KEY,
            body = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiContent(listOf(GeminiPart(SYSTEM_PROMPT))),
                generationConfig = GeminiGenerationConfig(temperature = 0.8),
            ),
        )
        response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            ?: error("Dot didn't reply. Try again.")
    }

    private companion object {
        const val SYSTEM_PROMPT =
            "You are Dot, the friendly travel assistant inside the Ulaaa app. " +
                "Help users discover destinations, plan trips with friends, and get practical " +
                "travel tips (India and worldwide). Be warm, concise, and conversational — " +
                "short paragraphs, no long bullet lists. If asked something unrelated to travel, " +
                "gently steer back to trip planning."
    }
}
