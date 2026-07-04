package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.BuildConfig
import com.dotkios.ulaaa.data.local.dao.ItineraryDao
import com.dotkios.ulaaa.data.local.entity.ItineraryStopEntity
import com.dotkios.ulaaa.data.model.ItineraryStop
import com.dotkios.ulaaa.data.remote.GeminiApi
import com.dotkios.ulaaa.data.remote.dto.GeminiContent
import com.dotkios.ulaaa.data.remote.dto.GeminiGenerationConfig
import com.dotkios.ulaaa.data.remote.dto.GeminiPart
import com.dotkios.ulaaa.data.remote.dto.GeminiRequest
import com.dotkios.ulaaa.data.remote.dto.ItineraryStopSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface ItineraryRepository {
    fun observe(tripId: String): Flow<List<ItineraryStop>>
    /** Ask Gemini for a day-by-day plan and replace the trip's stored itinerary. */
    suspend fun generate(tripId: String, destination: String, days: Int): Result<Unit>
    suspend fun clear(tripId: String): Result<Unit>
}

@Singleton
class ItineraryRepositoryImpl @Inject constructor(
    private val dao: ItineraryDao,
    private val api: GeminiApi,
    private val json: Json,
) : ItineraryRepository {

    override fun observe(tripId: String): Flow<List<ItineraryStop>> =
        dao.observeByTrip(tripId).map { list ->
            list.map { ItineraryStop(it.id, it.day, it.title, it.detail) }
        }

    override suspend fun generate(tripId: String, destination: String, days: Int): Result<Unit> = runCatching {
        val dayCount = days.coerceIn(1, 10)
        val prompt = buildString {
            append("Build a $dayCount-day travel itinerary for $destination. ")
            append("Give 2-3 stops per day. Respond ONLY with a JSON array where each element has: ")
            append("\"day\" (integer starting at 1), \"title\" (a place or activity, max 5 words), ")
            append("\"detail\" (one short sentence).")
        }
        val response = api.generate(
            model = GeminiApi.MODEL,
            apiKey = BuildConfig.GEMINI_API_KEY,
            body = GeminiRequest(
                contents = listOf(GeminiContent(listOf(GeminiPart(prompt)))),
                generationConfig = GeminiGenerationConfig(responseMimeType = "application/json", temperature = 0.8),
            ),
        )
        val text = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: error("Gemini returned no itinerary")
        val stops = json.decodeFromString<List<ItineraryStopSuggestion>>(text)

        val entities = stops.mapIndexed { index, s ->
            ItineraryStopEntity(
                id = UUID.randomUUID().toString(),
                tripId = tripId,
                day = s.day,
                title = s.title,
                detail = s.detail,
                orderIndex = index,
            )
        }
        dao.deleteByTrip(tripId)
        dao.insertAll(entities)
    }

    override suspend fun clear(tripId: String): Result<Unit> = runCatching {
        dao.deleteByTrip(tripId)
    }
}
