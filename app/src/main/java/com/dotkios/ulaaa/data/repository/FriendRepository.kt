package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.data.model.Friend
import com.dotkios.ulaaa.data.model.FriendRequest
import com.dotkios.ulaaa.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface FriendRepository {
    fun friends(): Flow<List<Friend>>
    fun incomingRequests(): Flow<List<FriendRequest>>
    suspend fun searchByEmail(email: String): Result<UserProfile?>
    suspend fun findByPhone(normalizedPhone: String): Result<UserProfile?>
    /** Maps normalized phone -> Ulaaa user, for the phones that belong to a registered user. */
    suspend fun findUsersByPhones(normalizedPhones: List<String>): Result<Map<String, UserProfile>>
    suspend fun sendRequest(to: UserProfile): Result<Unit>
    suspend fun acceptRequest(request: FriendRequest): Result<Unit>
    suspend fun declineRequest(fromUid: String): Result<Unit>
    suspend fun removeFriend(uid: String): Result<Unit>
}

@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : FriendRepository {

    private val users get() = firestore.collection("users")
    private fun me() = auth.currentUser

    override fun friends(): Flow<List<Friend>> {
        val uid = me()?.uid ?: return flowOf(emptyList())
        return users.document(uid).collection("friends").observe { doc ->
            val friendUid = doc.getString("uid") ?: return@observe null
            Friend(uid = friendUid, name = doc.getString("name").orEmpty())
        }
    }

    override fun incomingRequests(): Flow<List<FriendRequest>> {
        val uid = me()?.uid ?: return flowOf(emptyList())
        return users.document(uid).collection("requests").observe { doc ->
            val fromUid = doc.getString("fromUid") ?: return@observe null
            FriendRequest(
                fromUid = fromUid,
                name = doc.getString("name").orEmpty(),
                email = doc.getString("email").orEmpty(),
            )
        }
    }

    /** Realtime Firestore collection as a Flow, mapping each doc (null docs skipped). */
    private fun <T> CollectionReference.observe(map: (DocumentSnapshot) -> T?): Flow<List<T>> =
        callbackFlow {
            val registration = addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents?.mapNotNull(map).orEmpty()
                trySend(items)
            }
            awaitClose { registration.remove() }
        }

    override suspend fun searchByEmail(email: String): Result<UserProfile?> = runCatching {
        val snapshot = users.whereEqualTo("email", email.trim()).limit(1).get().await()
        val profile = snapshot.documents.firstOrNull()?.toObject(UserProfile::class.java)
        // Don't return yourself as a searchable friend.
        profile?.takeIf { it.uid != me()?.uid }
    }

    override suspend fun findByPhone(normalizedPhone: String): Result<UserProfile?> = runCatching {
        if (normalizedPhone.isBlank()) return@runCatching null
        val snapshot = users.whereEqualTo("phone", normalizedPhone).limit(1).get().await()
        val profile = snapshot.documents.firstOrNull()?.toObject(UserProfile::class.java)
        profile?.takeIf { it.uid != me()?.uid }
    }

    override suspend fun findUsersByPhones(normalizedPhones: List<String>): Result<Map<String, UserProfile>> = runCatching {
        val myUid = me()?.uid
        val result = HashMap<String, UserProfile>()
        normalizedPhones.filter { it.isNotBlank() }.distinct().chunked(10).forEach { chunk ->
            // Firestore whereIn accepts up to 10 values per query.
            val snapshot = users.whereIn("phone", chunk).get().await()
            snapshot.documents.forEach { doc ->
                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null && profile.phone.isNotBlank() && profile.uid != myUid) {
                    result[profile.phone] = profile
                }
            }
        }
        result
    }

    override suspend fun sendRequest(to: UserProfile): Result<Unit> = runCatching {
        val current = me() ?: error("Not signed in")
        users.document(to.uid).collection("requests").document(current.uid)
            .set(
                mapOf(
                    "fromUid" to current.uid,
                    "name" to (current.displayName ?: ""),
                    "email" to (current.email ?: ""),
                    "at" to System.currentTimeMillis(),
                ),
            ).await()
    }

    override suspend fun acceptRequest(request: FriendRequest): Result<Unit> = runCatching {
        val current = me() ?: error("Not signed in")
        val batch = firestore.batch()
        val now = System.currentTimeMillis()

        val myFriend = users.document(current.uid).collection("friends").document(request.fromUid)
        batch.set(myFriend, mapOf("uid" to request.fromUid, "name" to request.name, "since" to now))

        val theirFriend = users.document(request.fromUid).collection("friends").document(current.uid)
        batch.set(theirFriend, mapOf("uid" to current.uid, "name" to (current.displayName ?: ""), "since" to now))

        val requestDoc = users.document(current.uid).collection("requests").document(request.fromUid)
        batch.delete(requestDoc)

        batch.commit().await()
    }

    override suspend fun declineRequest(fromUid: String): Result<Unit> = runCatching {
        val current = me() ?: error("Not signed in")
        users.document(current.uid).collection("requests").document(fromUid).delete().await()
    }

    override suspend fun removeFriend(uid: String): Result<Unit> = runCatching {
        val current = me() ?: error("Not signed in")
        val batch = firestore.batch()
        batch.delete(users.document(current.uid).collection("friends").document(uid))
        batch.delete(users.document(uid).collection("friends").document(current.uid))
        batch.commit().await()
    }
}
