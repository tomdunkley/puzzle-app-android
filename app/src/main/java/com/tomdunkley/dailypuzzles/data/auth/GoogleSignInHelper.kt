package com.tomdunkley.dailypuzzles.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.tomdunkley.dailypuzzles.BuildConfig

/** Wraps the Credential Manager "Sign in with Google" flow. Requires
 * BuildConfig.GOOGLE_WEB_CLIENT_ID to be a real OAuth web client ID from Google
 * Cloud Console -- callers should check [isConfigured] before invoking this so
 * the button can be disabled with an honest message until that's set up.
 */
object GoogleSignInHelper {

    val isConfigured: Boolean
        get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    suspend fun requestIdToken(context: Context): String {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val result = CredentialManager.create(context).getCredential(context, request)
        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
        return credential.idToken
    }
}
