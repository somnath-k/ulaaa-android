package com.dotkios.ulaaa.data.repository

import androidx.compose.ui.graphics.Color
import com.dotkios.ulaaa.BuildConfig
import com.dotkios.ulaaa.data.model.CuratedItinerary
import com.dotkios.ulaaa.data.model.ItineraryStop
import com.dotkios.ulaaa.data.model.Landmark
import com.dotkios.ulaaa.data.model.TripAdvice
import com.dotkios.ulaaa.data.remote.GeminiApi
import com.dotkios.ulaaa.data.remote.dto.GeminiContent
import com.dotkios.ulaaa.data.remote.dto.GeminiGenerationConfig
import com.dotkios.ulaaa.data.remote.dto.GeminiPart
import com.dotkios.ulaaa.data.remote.dto.GeminiRequest
import com.dotkios.ulaaa.data.remote.dto.ItineraryStopSuggestion
import com.dotkios.ulaaa.data.remote.dto.ItinerarySuggestion
import com.dotkios.ulaaa.data.remote.dto.LandmarkSuggestion
import com.dotkios.ulaaa.data.remote.dto.TripAdviceDto
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface RecommendationRepository {
    /** Gemini-generated curated trip ideas, tailored to [place]. */
    suspend fun curatedItineraries(place: String): Result<List<CuratedItinerary>>

    /** Gemini-suggested landmarks in or near [place]. */
    suspend fun nearbyLandmarks(place: String): Result<List<Landmark>>

    /** Ready-made day-by-day itinerary for a destination (in-memory preview, not saved). */
    suspend fun itineraryFor(destination: String, days: Int): Result<List<ItineraryStop>>

    /** AI-suggested packing/checklist items for a trip. */
    suspend fun suggestChecklist(destination: String, days: Int): Result<List<String>>

    /** Weather outlook + travel concerns for [destination] around [dateRange]. */
    suspend fun tripAdvice(destination: String, dateRange: String): Result<TripAdvice>
}

@Singleton
class RecommendationRepositoryImpl @Inject constructor(
    private val api: GeminiApi,
    private val json: Json,
) : RecommendationRepository {

    override suspend fun curatedItineraries(place: String): Result<List<CuratedItinerary>> = runCatching {
        val prompt = buildString {
            append("Suggest 6 curated trip ideas for a traveller based near $place. ")
            append("Give a MIX: at least 2 nearby weekend getaways, at least 2 domestic (same-country) ")
            append("trips, and 1-2 international destinations — not only international. ")
            append("Respond ONLY with a JSON array. Each element must have exactly: ")
            append("\"title\" (catchy, max 4 words), \"subtitle\" (max 6 words), ")
            append("\"destination\" (the city/place to visit), \"days\" (integer 2-7), ")
            append("\"stopCount\" (integer between 3 and 12).")
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
                    destination = s.destination.ifBlank { s.title },
                    days = s.days.coerceIn(1, 10),
                )
            }
    }

    override suspend fun itineraryFor(destination: String, days: Int): Result<List<ItineraryStop>> = runCatching {
        val dayCount = days.coerceIn(1, 10)
        val prompt = buildString {
            append("Build a $dayCount-day travel itinerary for $destination. ")
            append("Give 2-3 stops per day. Respond ONLY with a JSON array where each element has: ")
            append("\"day\" (integer starting at 1), \"title\" (a place or activity, max 5 words), ")
            append("\"detail\" (one short sentence), and ")
            append("\"cost\" (approximate per-person cost in Indian Rupees as an integer, 0 if free).")
        }
        val response = api.generate(
            model = GeminiApi.MODEL,
            apiKey = BuildConfig.GEMINI_API_KEY,
            body = GeminiRequest(
                contents = listOf(GeminiContent(listOf(GeminiPart(prompt)))),
                generationConfig = GeminiGenerationConfig(responseMimeType = "application/json", temperature = 0.8),
            ),
        )
        val text = response.candidates.firstOrNull()?.content?.parts?.firstNotNullOfOrNull { it.text }
            ?: error("Gemini returned no itinerary")
        json.decodeFromString<List<ItineraryStopSuggestion>>(text)
            .map { s ->
                ItineraryStop(
                    id = UUID.randomUUID().toString(),
                    day = s.day,
                    title = s.title,
                    detail = s.detail,
                    cost = s.cost,
                )
            }
    }

    override suspend fun suggestChecklist(destination: String, days: Int): Result<List<String>> = runCatching {
        val prompt = buildString {
            append("List 10 essential packing and to-do checklist items for a ")
            append("$days-day trip to $destination. Consider the typical weather and common ")
            append("activities there. Respond ONLY with a JSON array of short strings ")
            append("(each max 5 words, e.g. \"Sunscreen\", \"Power bank\").")
        }
        val text = generateText(prompt, temperature = 0.7)
        json.decodeFromString<List<String>>(text).map { it.trim() }.filter { it.isNotBlank() }.take(15)
    }

    override suspend fun tripAdvice(destination: String, dateRange: String): Result<TripAdvice> = runCatching {
        val prompt = buildString {
            append("A traveller is visiting $destination during: $dateRange. ")
            append("Give the typical weather to expect then, the best time of day for activities, ")
            append("and travel concerns or advisories to keep in mind. ")
            append("Respond ONLY as a JSON object with keys: ")
            append("\"weather\" (1-2 sentences), \"bestTime\" (short phrase), ")
            append("\"concerns\" (array of 3-5 short strings).")
        }
        val text = generateText(prompt, temperature = 0.6)
        val dto = json.decodeFromString<TripAdviceDto>(text)
        TripAdvice(weather = dto.weather, bestTime = dto.bestTime, concerns = dto.concerns)
    }

    private suspend fun generateText(prompt: String, temperature: Double): String {
        val response = api.generate(
            model = GeminiApi.MODEL,
            apiKey = BuildConfig.GEMINI_API_KEY,
            body = GeminiRequest(
                contents = listOf(GeminiContent(listOf(GeminiPart(prompt)))),
                generationConfig = GeminiGenerationConfig(responseMimeType = "application/json", temperature = temperature),
            ),
        )
        return response.candidates.firstOrNull()?.content?.parts?.firstNotNullOfOrNull { it.text }
            ?: error("Gemini returned no content")
    }

    override suspend fun nearbyLandmarks(place: String): Result<List<Landmark>> = runCatching {
        val prompt = buildString {
            append("The traveller is located at $place. ")
            append("List 8 real landmarks, attractions or getaway spots whose straight-line ")
            append("distance from that exact location is between 50 and 100 km — great for a day ")
            append("trip, NOT places inside the traveller's own city. ")
            append("Compute each distance from the given latitude/longitude and only include a ")
            append("place if its distance truly falls in the 50-100 km range. ")
            append("Respond ONLY with a JSON array. Each element must have exactly: ")
            append("\"name\" (the place name), \"category\" (one word like Beach, Temple, Park, Museum, ")
            append("Fort, Nature, Waterfall, Hill, Viewpoint), \"detail\" (one short sentence), ")
            append("\"distanceKm\" (the actual computed distance in km, an integer between 50 and 100).")
        }
        val response = api.generate(
            model = GeminiApi.MODEL,
            apiKey = BuildConfig.GEMINI_API_KEY,
            body = GeminiRequest(
                contents = listOf(GeminiContent(listOf(GeminiPart(prompt)))),
                generationConfig = GeminiGenerationConfig(responseMimeType = "application/json", temperature = 0.7),
            ),
        )
        val text = response.candidates.firstOrNull()?.content?.parts?.firstNotNullOfOrNull { it.text }
            ?: error("Gemini returned no landmarks")
        json.decodeFromString<List<LandmarkSuggestion>>(text)
            .filter { it.name.isNotBlank() && it.distanceKm in 40..120 }
            .mapIndexed { index, s ->
                Landmark(
                    id = "gemini-landmark-$index",
                    name = s.name,
                    category = s.category.ifBlank { "Place" },
                    distanceKm = s.distanceKm.toDouble(),
                    rating = 0.0,
                    accent = PALETTE[index % PALETTE.size],
                    description = s.detail,
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
