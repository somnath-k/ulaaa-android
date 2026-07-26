package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.data.model.ChecklistItem
import com.dotkios.ulaaa.data.model.Expense
import com.dotkios.ulaaa.data.model.TripInvite
import com.dotkios.ulaaa.data.model.TripMember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface TripDetailRepository {
    fun checklist(tripId: String): Flow<List<ChecklistItem>>
    fun members(tripId: String): Flow<List<TripMember>>
    fun expenses(tripId: String): Flow<List<Expense>>

    suspend fun addChecklistItem(tripId: String, text: String)
    suspend fun setChecklistDone(tripId: String, itemId: String, done: Boolean)
    suspend fun deleteChecklistItem(tripId: String, itemId: String)

    suspend fun deleteMember(tripId: String, uid: String)

    // Invite-to-join: a friend becomes a member only after accepting.
    fun tripInvites(tripId: String): Flow<List<TripMember>>
    fun pendingInvites(): Flow<List<TripInvite>>
    suspend fun invite(tripId: String, tripTitle: String, destination: String, fromName: String, toUid: String, toName: String)
    suspend fun cancelInvite(tripId: String, uid: String)
    suspend fun acceptInvite(tripId: String)
    suspend fun declineInvite(tripId: String)

    suspend fun addExpense(tripId: String, title: String, amount: Double, paidBy: String)
    suspend fun deleteExpense(tripId: String, expenseId: String)
}

@Singleton
class TripDetailRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : TripDetailRepository {

    private fun tripDoc(tripId: String) = firestore.collection("trips").document(tripId)
    private fun checklistCol(tripId: String) = tripDoc(tripId).collection("checklist")
    private fun expensesCol(tripId: String) = tripDoc(tripId).collection("expenses")
    private fun tripInvitesCol(tripId: String) = tripDoc(tripId).collection("invites")
    private fun userInvitesCol(uid: String) =
        firestore.collection("users").document(uid).collection("tripInvites")

    override fun checklist(tripId: String): Flow<List<ChecklistItem>> =
        checklistCol(tripId).orderBy("createdAt", Query.Direction.ASCENDING).observe { doc ->
            ChecklistItem(
                id = doc.id,
                text = doc.getString("text").orEmpty(),
                isDone = doc.getBoolean("isDone") ?: false,
            )
        }

    override fun expenses(tripId: String): Flow<List<Expense>> =
        expensesCol(tripId).orderBy("createdAt", Query.Direction.DESCENDING).observe { doc ->
            Expense(
                id = doc.id,
                title = doc.getString("title").orEmpty(),
                amount = doc.getDouble("amount") ?: 0.0,
                paidBy = doc.getString("paidBy").orEmpty(),
            )
        }

    override fun members(tripId: String): Flow<List<TripMember>> = callbackFlow {
        val registration = tripDoc(tripId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            @Suppress("UNCHECKED_CAST")
            val names = (snapshot?.get("memberNames") as? Map<String, String>).orEmpty()
            trySend(names.map { (uid, name) -> TripMember(id = uid, uid = uid, name = name) })
        }
        awaitClose { registration.remove() }
    }

    override suspend fun addChecklistItem(tripId: String, text: String) {
        if (text.isBlank()) return
        checklistCol(tripId).add(
            mapOf(
                "text" to text.trim(),
                "isDone" to false,
                "createdAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    override suspend fun setChecklistDone(tripId: String, itemId: String, done: Boolean) {
        checklistCol(tripId).document(itemId).update("isDone", done).await()
    }

    override suspend fun deleteChecklistItem(tripId: String, itemId: String) {
        checklistCol(tripId).document(itemId).delete().await()
    }

    override fun tripInvites(tripId: String): Flow<List<TripMember>> =
        tripInvitesCol(tripId).observe { doc ->
            val uid = doc.getString("uid") ?: doc.id
            TripMember(id = uid, uid = uid, name = doc.getString("name").orEmpty())
        }

    override fun pendingInvites(): Flow<List<TripInvite>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return userInvitesCol(uid).observe { doc ->
            TripInvite(
                tripId = doc.getString("tripId") ?: doc.id,
                tripTitle = doc.getString("tripTitle").orEmpty(),
                destination = doc.getString("destination").orEmpty(),
                fromName = doc.getString("fromName").orEmpty(),
                fromUid = doc.getString("fromUid").orEmpty(),
                at = doc.getLong("at") ?: 0L,
            )
        }
    }

    override suspend fun invite(
        tripId: String,
        tripTitle: String,
        destination: String,
        fromName: String,
        toUid: String,
        toName: String,
    ) {
        if (toUid.isBlank()) return
        // Mirror on the trip (so the owner sees who's invited) and on the invitee (their inbox).
        tripInvitesCol(tripId).document(toUid).set(mapOf("uid" to toUid, "name" to toName)).await()
        userInvitesCol(toUid).document(tripId).set(
            mapOf(
                "tripId" to tripId,
                "tripTitle" to tripTitle,
                "destination" to destination,
                "fromName" to fromName,
                "fromUid" to (auth.currentUser?.uid ?: ""),
                "at" to System.currentTimeMillis(),
            ),
        ).await()
    }

    override suspend fun cancelInvite(tripId: String, uid: String) {
        tripInvitesCol(tripId).document(uid).delete().await()
        userInvitesCol(uid).document(tripId).delete().await()
    }

    override suspend fun acceptInvite(tripId: String) {
        val user = auth.currentUser ?: return
        val name = user.displayName?.takeIf { it.isNotBlank() } ?: "Me"
        tripDoc(tripId).update(
            mapOf(
                "members" to FieldValue.arrayUnion(user.uid),
                "memberNames.${user.uid}" to name,
            ),
        ).await()
        cancelInvite(tripId, user.uid)
    }

    override suspend fun declineInvite(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        cancelInvite(tripId, uid)
    }

    override suspend fun deleteMember(tripId: String, uid: String) {
        tripDoc(tripId).update(
            mapOf(
                "members" to FieldValue.arrayRemove(uid),
                "memberNames.$uid" to FieldValue.delete(),
            ),
        ).await()
    }

    override suspend fun addExpense(tripId: String, title: String, amount: Double, paidBy: String) {
        if (title.isBlank() || amount <= 0.0) return
        expensesCol(tripId).add(
            mapOf(
                "title" to title.trim(),
                "amount" to amount,
                "paidBy" to paidBy,
                "createdAt" to System.currentTimeMillis(),
            ),
        ).await()
    }

    override suspend fun deleteExpense(tripId: String, expenseId: String) {
        expensesCol(tripId).document(expenseId).delete().await()
    }

    /** Realtime query as a Flow, mapping each doc (nulls skipped). */
    private fun <T> Query.observe(map: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
        val registration = addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            trySend(snapshot?.documents?.mapNotNull(map).orEmpty())
        }
        awaitClose { registration.remove() }
    }
}
