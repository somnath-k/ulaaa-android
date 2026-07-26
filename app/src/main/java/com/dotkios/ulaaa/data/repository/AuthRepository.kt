package com.dotkios.ulaaa.data.repository

import android.app.Activity
import com.dotkios.ulaaa.data.model.UserProfile
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Outcome of starting phone verification. */
sealed interface PhoneVerification {
    /** SMS sent; prompt the user for the code with this id. */
    data class CodeSent(val verificationId: String) : PhoneVerification
    /** Instant/auto verification succeeded — the user is already signed in. */
    data object AutoSignedIn : PhoneVerification
}

interface AuthRepository {
    val currentUser: FirebaseUser?
    fun authState(): Flow<FirebaseUser?>
    suspend fun signIn(email: String, password: String): Result<FirebaseUser>
    suspend fun signUp(name: String, email: String, password: String): Result<FirebaseUser>
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser>
    /** Sends an OTP to [phoneE164]; [activity] is needed for reCAPTCHA/Play Integrity. */
    suspend fun startPhoneVerification(activity: Activity, phoneE164: String): Result<PhoneVerification>
    suspend fun verifyOtp(verificationId: String, code: String): Result<FirebaseUser>
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

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: error("Google sign-in returned no user")
        // Best-effort profile write so the user is searchable/friendable.
        userRepository.createProfile(
            UserProfile(
                uid = user.uid,
                name = user.displayName.orEmpty(),
                email = user.email.orEmpty(),
                createdAt = System.currentTimeMillis(),
            ),
        )
        user
    }

    override suspend fun startPhoneVerification(
        activity: Activity,
        phoneE164: String,
    ): Result<PhoneVerification> = runCatching {
        val outcome = suspendCancellableCoroutine { cont ->
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    if (cont.isActive) cont.resume(PhoneOutcome.Auto(credential))
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    if (cont.isActive) cont.resume(PhoneOutcome.Sent(id))
                }
            }
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneE164)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
        when (outcome) {
            is PhoneOutcome.Auto -> {
                signInWithPhoneCredential(outcome.credential)
                PhoneVerification.AutoSignedIn
            }
            is PhoneOutcome.Sent -> PhoneVerification.CodeSent(outcome.verificationId)
        }
    }

    override suspend fun verifyOtp(verificationId: String, code: String): Result<FirebaseUser> = runCatching {
        signInWithPhoneCredential(PhoneAuthProvider.getCredential(verificationId, code))
    }

    /** Signs in with a phone credential and provisions the profile without clobbering existing data. */
    private suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): FirebaseUser {
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: error("Phone sign-in returned no user")
        val phone = user.phoneNumber.orEmpty().filter { it.isDigit() }.takeLast(10)
        val existing = userRepository.getProfile(user.uid).getOrNull()
        if (existing == null) {
            userRepository.createProfile(
                UserProfile(
                    uid = user.uid,
                    name = user.displayName.orEmpty(),
                    email = user.email.orEmpty(),
                    phone = phone,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        } else if (phone.isNotBlank() && existing.phone != phone) {
            userRepository.setPhone(user.uid, phone)
        }
        return user
    }

    override fun signOut() = auth.signOut()

    private sealed interface PhoneOutcome {
        data class Auto(val credential: PhoneAuthCredential) : PhoneOutcome
        data class Sent(val verificationId: String) : PhoneOutcome
    }
}
