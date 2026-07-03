package com.tomdunkley.dailypuzzles.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.tomdunkley.dailypuzzles.data.network.ApiClient

/** A guest's own access/refresh tokens, kept entirely separate from AuthTokenStore and
 * AuthRepository.state -- playing as a guest must never make Friends, Leaderboard, or
 * Settings look signed-in. Only Boggle (and viewing your own board afterwards) reads
 * this, via AuthRepository.apiServiceForCurrentSession().
 */
object GuestSession {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("guest_session", Context.MODE_PRIVATE)
    }

    val accessToken: String? get() = prefs.getString(KEY_ACCESS, null)
    val refreshToken: String? get() = prefs.getString(KEY_REFRESH, null)

    fun save(accessToken: String, refreshToken: String) {
        prefs.edit().putString(KEY_ACCESS, accessToken).putString(KEY_REFRESH, refreshToken).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    /** Returns the current guest's access token, signing up a brand-new guest account
     * first if this is the first time playing as a guest on this device. */
    suspend fun ensureToken(): String {
        accessToken?.let { return it }
        val tokens = ApiClient.plainService.guestSignIn()
        save(tokens.accessToken, tokens.refreshToken)
        return tokens.accessToken
    }

    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
}
