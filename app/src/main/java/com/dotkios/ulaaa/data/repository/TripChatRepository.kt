package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.data.model.TripChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface TripChatRepository {
    val currentUid: String?
    fun messages(tripId: String): Flow<List<TripChatMessage>>
    suspend fun send(tripId: String, text: String): Result<Unit>
}

@Singleton
class TripChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : TripChatRepository {

    override val currentUid: String? get() = auth.currentUser?.uid

    private fun messagesRef(tripId: String) =
        firestore.collection("trip_chats").document(tripId).collection("messages")

    override fun messages(tripId: String): Flow<List<TripChatMessage>> = callbackFlow {
        val registration = messagesRef(tripId)
            .orderBy("at", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    TripChatMessage(
                        id = doc.id,
                        senderUid = doc.getString("senderUid").orEmpty(),
                        senderName = doc.getString("senderName").orEmpty(),
                        text = doc.getString("text").orEmpty(),
                        at = doc.getLong("at") ?: 0L,
                    )
                }.orEmpty()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun send(tripId: String, text: String): Result<Unit> = runCatching {
        val user = auth.currentUser ?: error("Not signed in")
        if (text.isBlank()) return@runCatching
        messagesRef(tripId).add(
            mapOf(
                "senderUid" to user.uid,
                "senderName" to (user.displayName ?: "Someone"),
                "text" to text.trim(),
                "at" to System.currentTimeMillis(),
            ),
        ).await()
    }
}
