package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.BuildConfig
import com.dotkios.ulaaa.data.model.ItineraryStop
import com.dotkios.ulaaa.data.remote.GeminiApi
import com.dotkios.ulaaa.data.remote.dto.GeminiContent
import com.dotkios.ulaaa.data.remote.dto.GeminiGenerationConfig
import com.dotkios.ulaaa.data.remote.dto.GeminiPart
import com.dotkios.ulaaa.data.remote.dto.GeminiRequest
import com.dotkios.ulaaa.data.remote.dto.ItineraryStopSuggestion
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface ItineraryRepository {
    fun observe(tripId: String): Flow<List<ItineraryStop>>
    suspend fun generate(tripId: String, destination: String, days: Int): Result<Unit>
    suspend fun addStop(tripId: String, day: Int, title: String, detail: String, cost: Int): Result<Unit>
    suspend fun deleteStop(tripId: String, stopId: String): Result<Unit>
    suspend fun clear(tripId: String): Result<Unit>
}

@Singleton
class ItineraryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val api: GeminiApi,
    private val json: Json,
) : ItineraryRepository {

    private fun itineraryCol(tripId: String) =
        firestore.collection("trips").document(tripId).collection("itinerary")

    override fun observe(tripId: String): Flow<List<ItineraryStop>> = callbackFlow {
        val registration = itineraryCol(tripId)
            .orderBy("day", Query.Direction.ASCENDING)
            .orderBy("orderIndex", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val stops = snapshot?.documents?.map { doc ->
                    ItineraryStop(
                        id = doc.id,
                        day = (doc.getLong("day") ?: 1L).toInt(),
                        title = doc.getString("title").orEmpty(),
                        detail = doc.getString("detail").orEmpty(),
                        cost = (doc.getLong("cost") ?: 0L).toInt(),
                    )
                }.orEmpty()
                trySend(stops)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun generate(tripId: String, destination: String, days: Int): Result<Unit> = runCatching {
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
        val stops = json.decodeFromString<List<ItineraryStopSuggestion>>(text)

        clearInternal(tripId)
        val batch = firestore.batch()
        stops.forEachIndexed { index, s ->
            val doc = itineraryCol(tripId).document()
            batch.set(
                doc,
                mapOf(
                    "day" to s.day,
                    "title" to s.title,
                    "detail" to s.detail,
                    "cost" to s.cost,
                    "orderIndex" to index,
                ),
            )
        }
        batch.commit().await()
    }

    override suspend fun addStop(
        tripId: String,
        day: Int,
        title: String,
        detail: String,
        cost: Int,
    ): Result<Unit> = runCatching {
        if (title.isBlank()) return@runCatching
        itineraryCol(tripId).add(
            mapOf(
                "day" to day.coerceAtLeast(1),
                "title" to title.trim(),
                "detail" to detail.trim(),
                "cost" to cost.coerceAtLeast(0),
                // Large orderIndex so manual stops append after AI-generated ones within a day.
                "orderIndex" to ((System.currentTimeMillis() / 1000L) - 1_700_000_000L).toInt(),
            ),
        ).await()
    }

    override suspend fun deleteStop(tripId: String, stopId: String): Result<Unit> = runCatching {
        itineraryCol(tripId).document(stopId).delete().await()
    }

    override suspend fun clear(tripId: String): Result<Unit> = runCatching {
        clearInternal(tripId)
    }

    private suspend fun clearInternal(tripId: String) {
        val existing = itineraryCol(tripId).get().await()
        if (existing.isEmpty) return
        val batch = firestore.batch()
        existing.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }
}
