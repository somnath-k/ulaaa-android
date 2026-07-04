package com.dotkios.ulaaa.data.repository

import com.dotkios.ulaaa.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val currentUser: FirebaseUser?
    fun authState(): Flow<FirebaseUser?>
    suspend fun signIn(email: String, password: String): Result<FirebaseUser>
    suspend fun signUp(name: String, email: String, password: String): Result<FirebaseUser>
    fun signOut()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
) : AuthRepository {

    override val currentUser: FirebaseUser? get() = auth.currentUser

    override fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        result.user ?: error("Sign-in returned no user")
    }

    override suspend fun signUp(name: String, email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("Sign-up returned no user")
        user.updateProfile(userProfileChangeRequest { displayName = name.trim() }).await()
        // Best-effort profile write; auth still succeeds if Firestore hiccups.
        userRepository.createProfile(
            UserProfile(
                uid = user.uid,
                name = name.trim(),
                email = email.trim(),
                createdAt = System.currentTimeMillis(),
            ),
        )
        user
    }

    override fun signOut() = auth.signOut()
}
