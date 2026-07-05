package com.dotkios.ulaaa.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.dotkios.ulaaa.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Outcome of a Google sign-in attempt. */
sealed interface GoogleSignInResult {
    data class Token(val idToken: String) : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
    data object NotConfigured : GoogleSignInResult
}

/**
 * Launches the Credential Manager Google chooser and returns a Google ID token to exchange
 * for a Firebase credential. Requires GOOGLE_WEB_CLIENT_ID (Firebase web client id) to be set.
 */
suspend fun requestGoogleIdToken(context: Context): GoogleSignInResult {
    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    if (webClientId.isBlank()) return GoogleSignInResult.NotConfigured

    val googleIdOption = GetGoogleIdOption.Builder()
        .setServerClientId(webClientId)
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(false)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    return try {
        val response = CredentialManager.create(context).getCredential(context, request)
        val credential = response.credential
        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
            GoogleSignInResult.Token(token)
        } else {
            GoogleSignInResult.Error("Unexpected credential type")
        }
    } catch (e: Exception) {
        GoogleSignInResult.Error(e.message ?: "Google sign-in cancelled")
    }
}
