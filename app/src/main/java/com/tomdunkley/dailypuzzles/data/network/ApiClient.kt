package com.tomdunkley.dailypuzzles.data.network

import com.tomdunkley.dailypuzzles.BuildConfig
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.auth.AuthTokenStore
import com.tomdunkley.dailypuzzles.data.auth.GuestSession
import com.tomdunkley.dailypuzzles.data.network.dto.RefreshRequestDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.Retrofit
import retrofit2.create

/** Owns the two Retrofit clients the app needs: one plain (register/login/refresh,
 * which must never themselves trigger the auth-retry logic below), and one that
 * attaches the current access token to every request and transparently refreshes
 * it on a 401 before retrying once.
 */
object ApiClient {

    private lateinit var tokenStore: AuthTokenStore

    val plainService: ApiService by lazy { buildRetrofit(plainOkHttpClient()).create() }
    val authenticatedService: ApiService by lazy { buildRetrofit(authenticatedOkHttpClient()).create() }

    /** Attaches a guest's own access token (from GuestSession) instead of the main
     * tokenStore -- used by AuthRepository.apiServiceForCurrentSession() when nobody
     * is really signed in, so a guest can play Boggle without affecting AuthState.
     */
    val guestService: ApiService by lazy { buildRetrofit(guestOkHttpClient()).create() }

    fun init(tokenStore: AuthTokenStore) {
        this.tokenStore = tokenStore
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun buildRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    private fun plainOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .build()

    private fun authenticatedOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(bearerTokenInterceptor())
            .authenticator(refreshAuthenticator())
            .addInterceptor(loggingInterceptor())
            .build()

    private fun guestOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(guestBearerTokenInterceptor())
            .authenticator(guestRefreshAuthenticator())
            .addInterceptor(loggingInterceptor())
            .build()

    private fun loggingInterceptor() =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }

    private fun bearerTokenInterceptor() = Interceptor { chain ->
        val token = tokenStore.accessToken
        val request = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    /** On a 401, exchanges the refresh token for a new access token and retries
     * the original request once. If that also fails, gives up (caller sees the
     * 401 and the app's auth state should treat this as signed-out).
     */
    private fun refreshAuthenticator() = Authenticator { _, response ->
        if (response.request.header("X-Retry-After-Refresh") != null) {
            return@Authenticator null // already retried once -- don't loop forever
        }

        val refreshToken = tokenStore.refreshToken ?: return@Authenticator null

        val newTokens = runCatching {
            runBlocking { plainService.refresh(RefreshRequestDto(refreshToken)) }
        }.getOrNull() ?: run {
            // The refresh token itself is no longer valid -- there's no way back into a
            // signed-in state, so force AuthRepository's state to match reality instead
            // of leaving it stale (which would otherwise strand the UI on a screen that
            // looks signed-in but can never successfully call the API again).
            AuthRepository.signOut()
            return@Authenticator null
        }

        tokenStore.saveTokens(newTokens.accessToken, newTokens.refreshToken)

        response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .header("X-Retry-After-Refresh", "true")
            .build()
    }

    private fun guestBearerTokenInterceptor() = Interceptor { chain ->
        val token = GuestSession.accessToken
        val request = if (token != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    /** Mirrors refreshAuthenticator() above, but updates GuestSession instead of the
     * main tokenStore -- a guest session expiring just means the next call fails and
     * the caller (e.g. BoggleViewModel) surfaces that as a normal error, no sign-out.
     */
    private fun guestRefreshAuthenticator() = Authenticator { _, response ->
        if (response.request.header("X-Retry-After-Refresh") != null) {
            return@Authenticator null
        }

        val refreshToken = GuestSession.refreshToken ?: return@Authenticator null

        val newTokens = runCatching {
            runBlocking { plainService.refresh(RefreshRequestDto(refreshToken)) }
        }.getOrNull() ?: return@Authenticator null

        GuestSession.save(newTokens.accessToken, newTokens.refreshToken)

        response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .header("X-Retry-After-Refresh", "true")
            .build()
    }
}
