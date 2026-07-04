package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    suspend fun createProfile(profile: UserProfile): Result<Unit>
    suspend fun getProfile(uid: String): Result<UserProfile?>
    suspend fun setPhone(uid: String, phone: String): Result<Unit>
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserRepository {

    private val users get() = firestore.collection(COLLECTION)

    override suspend fun createProfile(profile: UserProfile): Result<Unit> = runCatching {
        users.document(profile.uid).set(profile).await()
    }

    override suspend fun getProfile(uid: String): Result<UserProfile?> = runCatching {
        users.document(uid).get().await().toObject(UserProfile::class.java)
    }

    override suspend fun setPhone(uid: String, phone: String): Result<Unit> = runCatching {
        users.document(uid).set(mapOf("phone" to phone), SetOptions.merge()).await()
    }

    private companion object {
        const val COLLECTION = "users"
    }
}
