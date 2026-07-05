package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.data.model.FriendLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface FriendLocationRepository {
    suspend fun publishMyLocation(lat: Double, lon: Double): Result<Unit>
    fun observeFriends(friendUids: List<String>): Flow<List<FriendLocation>>
}

@Singleton
class FriendLocationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : FriendLocationRepository {

    private val locations get() = firestore.collection("friend_locations")

    override suspend fun publishMyLocation(lat: Double, lon: Double): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching
        locations.document(user.uid).set(
            mapOf(
                "uid" to user.uid,
                "name" to (user.displayName ?: "Friend"),
                "lat" to lat,
                "lon" to lon,
                "at" to System.currentTimeMillis(),
            ),
        ).await()
    }

    override fun observeFriends(friendUids: List<String>): Flow<List<FriendLocation>> {
        // whereIn caps at 10 — realtime for up to 10 friends (fine for now).
        val uids = friendUids.take(10)
        if (uids.isEmpty()) return flowOf(emptyList())
        return callbackFlow {
            val registration = locations.whereIn("uid", uids).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    val lat = doc.getDouble("lat") ?: return@mapNotNull null
                    val lon = doc.getDouble("lon") ?: return@mapNotNull null
                    FriendLocation(
                        uid = doc.getString("uid").orEmpty(),
                        name = doc.getString("name").orEmpty(),
                        lat = lat,
                        lon = lon,
                    )
                }.orEmpty()
                trySend(list)
            }
            awaitClose { registration.remove() }
        }
    }
}
