package com.dotkios.ulaaa.data.repository

import androidx.compose.ui.graphics.Color
import com.dotkios.ulaaa.BuildConfig
import com.dotkios.ulaaa.data.model.CuratedItinerary
import com.dotkios.ulaaa.data.remote.GeminiApi
import com.dotkios.ulaaa.data.remote.dto.GeminiContent
import com.dotkios.ulaaa.data.remote.dto.GeminiGenerationConfig
import com.dotkios.ulaaa.data.remote.dto.GeminiPart
import com.dotkios.ulaaa.data.remote.dto.GeminiRequest
import com.dotkios.ulaaa.data.remote.dto.ItinerarySuggestion
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface RecommendationRepository {
    /** Gemini-generated curated itineraries for the given interest theme. */
    suspend fun curatedItineraries(interest: String): Result<List<CuratedItinerary>>
}

@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val api: GeminiApi,
    private val json: Json,
) : RecommendationRepository {

    override suspend fun curatedItineraries(interest: String): Result<List<CuratedItinerary>> = runCatching {
        val prompt = buildString {
            append("Suggest 6 short curated travel itineraries in India ")
            append("for a traveller interested in $interest. ")
            append("Respond ONLY with a JSON array. Each element must have exactly these fields: ")
            append("\"title\" (max 4 words), \"subtitle\" (max 6 words), \"stopCount\" (integer between 3 and 12).")
        }
        val response = api.generate(
            model = GeminiApi.MODEL,
            apiKey = BuildConfig.GEMINI_API_KEY,
            body = GeminiRequest(
                contents = listOf(GeminiContent(listOf(GeminiPart(prompt)))),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.9,
                ),
            ),
        )
        val text = response.candidates.firstOrNull()?.content?.parts?.firstNotNullOfOrNull { it.text }
            ?: error("Gemini returned no content")
        json.decodeFromString<List<ItinerarySuggestion>>(text)
            .mapIndexed { index, s ->
                CuratedItinerary(
                    id = "gemini-$index",
                    title = s.title,
                    subtitle = s.subtitle,
                    stopCount = s.stopCount,
                    accent = PALETTE[index % PALETTE.size],
                )
            }
    }

    private companion object {
        val PALETTE = listOf(
            Color(0xFFFF6B57),
            Color(0xFF0E7C7B),
            Color(0xFFF4A259),
            Color(0xFF5A6FEA),
        )
    }
}
