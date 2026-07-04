package com.dotkios.ulaaa.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null,
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null,
)

@Serializable
data class GeminiPart(
    val text: String,
)

@Serializable
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Double? = null,
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
)

/** Shape Gemini is asked to emit for each curated itinerary suggestion. */
@Serializable
data class ItinerarySuggestion(
    val title: String,
    val subtitle: String,
    val stopCount: Int,
)

/** One day-stop in a generated trip itinerary. */
@Serializable
data class ItineraryStopSuggestion(
    val day: Int,
    val title: String,
    val detail: String,
)
