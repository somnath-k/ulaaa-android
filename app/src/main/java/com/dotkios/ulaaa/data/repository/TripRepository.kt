package com.dotkios.ulaaa.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dotkios.ulaaa.data.model.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface TripRepository {
    /** Trips the current user is a member of (owner or added to the squad). */
    val trips: Flow<List<Trip>>
    fun trip(id: String): Flow<Trip?>
    suspend fun addTrip(title: String, destination: String, startMillis: Long, endMillis: Long)
    suspend fun deleteTrip(id: String)
}

@Singleton
class TripRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val placeImageRepository: PlaceImageRepository,
) : TripRepository {

    private val tripsCol get() = firestore.collection("trips")

    override val trips: Flow<List<Trip>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val registration = tripsCol.whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents
                    ?.mapNotNull { it.toTrip() }
                    ?.sortedBy { it.title }
                    .orEmpty()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }.map { list -> list.map { it.withImage() } }

    override fun trip(id: String): Flow<Trip?> {
        if (id.isBlank()) return flowOf(null)
        return callbackFlow {
            val registration = tripsCol.document(id).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.toTrip())
            }
            awaitClose { registration.remove() }
        }.map { it?.withImage() }
    }

    /** Resolve a real destination photo (Wikipedia → Pexels), cached; gradient if none. */
    private suspend fun Trip.withImage(): Trip =
        copy(imageUrl = placeImageRepository.resolve(destination, destination).url)

    override suspend fun addTrip(title: String, destination: String, startMillis: Long, endMillis: Long) {
        val user = auth.currentUser ?: return
        val name = user.displayName?.takeIf { it.isNotBlank() } ?: "Me"
        val doc = tripsCol.document()
        doc.set(
            mapOf(
                "ownerId" to user.uid,
                "title" to title.trim(),
                "destination" to destination.trim(),
                "startMillis" to startMillis,
                "endMillis" to endMillis,
                "colorArgb" to PALETTE.random().toArgb().toLong(),
                "createdAt" to System.currentTimeMillis(),
                "members" to listOf(user.uid),
                "memberNames" to mapOf(user.uid to name),
            ),
        ).await()
    }

    override suspend fun deleteTrip(id: String) {
        tripsCol.document(id).delete().await()
    }

    private fun DocumentSnapshot.toTrip(): Trip? {
        val title = getString("title") ?: return null
        val start = getLong("startMillis") ?: 0L
        val end = getLong("endMillis") ?: 0L
        val memberCount = (get("members") as? List<*>)?.size ?: 1
        return Trip(
            id = id,
            title = title,
            destination = getString("destination").orEmpty(),
            dateRange = formatRange(start, end),
            squadSize = memberCount,
            days = durationDays(start, end),
            accent = Color((getLong("colorArgb") ?: 0xFF0E7C7B).toInt()),
        )
    }

    private fun formatRange(startMillis: Long, endMillis: Long): String {
        if (startMillis <= 0L) return "Dates TBD"
        val start = DAY_FMT.format(Date(startMillis))
        val end = DAY_FMT.format(Date(endMillis.coerceAtLeast(startMillis)))
        return if (start == end) start else "$start – $end"
    }

    private fun durationDays(startMillis: Long, endMillis: Long): Int {
        if (startMillis <= 0L || endMillis < startMillis) return 3
        return ((endMillis - startMillis) / 86_400_000L).toInt() + 1
    }

    private companion object {
        val DAY_FMT = SimpleDateFormat("MMM d", Locale.getDefault())
        val PALETTE = listOf(
            Color(0xFF0E7C7B),
            Color(0xFFFF6B57),
            Color(0xFFF4A259),
            Color(0xFF5A6FEA),
        )
    }
}
