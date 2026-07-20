package com.tomdunkley.dailypuzzles.data.auth

import com.tomdunkley.dailypuzzles.data.boggle.BoggleProgressStore
import com.tomdunkley.dailypuzzles.data.developer.DeveloperStore
import com.tomdunkley.dailypuzzles.data.network.ApiClient
import com.tomdunkley.dailypuzzles.data.network.ApiService
import com.tomdunkley.dailypuzzles.data.numbers.NumbersProgressStore
import com.tomdunkley.dailypuzzles.data.network.dto.ChangePasswordRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.ForgotPasswordRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.GoogleSignInRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.LoginRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.RegisterRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.ResetPasswordRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.SetPasswordRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.TokenPairDto
import com.tomdunkley.dailypuzzles.data.network.dto.VerifyEmailRequestDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** App-wide sign-in state, backed by encrypted on-device token storage. A singleton
 * (like ApiClient) rather than a ViewModel, since auth state needs to be visible
 * from multiple independent screens (Home's play-gate, Friends' account section).
 */
object AuthRepository {

    private lateinit var tokenStore: AuthTokenStore
    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun init(tokenStore: AuthTokenStore) {
        this.tokenStore = tokenStore
    }

    /** Call once at app start: trusts the locally cached display name if a refresh
     * token is present, rather than making a network call before the UI can render.
     * Any expired/invalid token surfaces naturally the first time a real API call
     * 401s and ApiClient's authenticator can't refresh it.
     */
    fun restoreSession() {
        val displayName = tokenStore.displayName
        _state.value = when {
            tokenStore.refreshToken == null || displayName == null -> AuthState.SignedOut
            tokenStore.emailVerified -> AuthState.SignedIn(displayName)
            else -> AuthState.SignedInUnverified(displayName)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val tokens = ApiClient.plainService.signInWithGoogle(GoogleSignInRequestDto(idToken, GuestSession.accessToken))
        onSignedIn(tokens)
        GuestSession.clear()
    }

    suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        val tokens = ApiClient.plainService.register(
            RegisterRequestDto(email, password, GuestSession.accessToken),
        )
        onSignedIn(tokens)
        GuestSession.clear()
    }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val tokens = ApiClient.plainService.login(LoginRequestDto(email, password, GuestSession.accessToken))
        onSignedIn(tokens)
        GuestSession.clear()
    }

    suspend fun verifyEmail(code: String): Result<Unit> = runCatching {
        ApiClient.authenticatedService.verifyEmail(VerifyEmailRequestDto(code))
        val displayName = tokenStore.displayName ?: ""
        tokenStore.emailVerified = true
        _state.value = AuthState.SignedIn(displayName)
    }

    suspend fun resendVerification(): Result<Unit> = runCatching {
        ApiClient.authenticatedService.resendVerification()
    }

    suspend fun forgotPassword(email: String): Result<Unit> = runCatching {
        ApiClient.plainService.forgotPassword(ForgotPasswordRequestDto(email))
    }

    suspend fun resendPasswordReset(email: String): Result<Unit> = runCatching {
        ApiClient.plainService.resendPasswordReset(ForgotPasswordRequestDto(email))
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<Unit> = runCatching {
        ApiClient.plainService.resetPassword(ResetPasswordRequestDto(email, code, newPassword))
    }

    suspend fun setPassword(newPassword: String): Result<Unit> = runCatching {
        ApiClient.authenticatedService.setPassword(SetPasswordRequestDto(newPassword))
    }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = runCatching {
        ApiClient.authenticatedService.changePassword(ChangePasswordRequestDto(currentPassword, newPassword))
    }

    suspend fun linkGoogleAccount(idToken: String): Result<Unit> = runCatching {
        ApiClient.authenticatedService.linkGoogleAccount(GoogleSignInRequestDto(idToken))
    }

    /** Called when a gameplay endpoint 403s with "please verify your email" -- the
     * cached verified flag was stale (e.g. set on another device), so correct it here
     * rather than waiting for the next full sign-in.
     */
    fun markUnverified() {
        val displayName = tokenStore.displayName ?: return
        tokenStore.emailVerified = false
        _state.value = AuthState.SignedInUnverified(displayName)
    }

    /** The Retrofit service to use for the current effective session: a real signed-in
     * account if there is one, otherwise the guest session (creating one on first use).
     * Used only by Boggle and viewing your own board -- Friends, Leaderboard, and
     * Settings always use authenticatedService directly and show a sign-in prompt
     * instead when signed out, per design.
     */
    suspend fun apiServiceForCurrentSession(): ApiService {
        val state = _state.value
        if (state is AuthState.SignedIn || state is AuthState.SignedInUnverified) {
            return ApiClient.authenticatedService
        }
        GuestSession.ensureToken()
        return ApiClient.guestService
    }

    /** Like apiServiceForCurrentSession(), but never creates a guest session just to
     * check something -- returns null if nobody is signed in and no guest session
     * already exists. Used by Home's "already completed today" check, which should
     * be a passive read, not something that mints a brand-new guest account on its own.
     */
    fun apiServiceForExistingSession(): ApiService? {
        val state = _state.value
        return when {
            state is AuthState.SignedIn || state is AuthState.SignedInUnverified -> ApiClient.authenticatedService
            GuestSession.accessToken != null -> ApiClient.guestService
            else -> null
        }
    }

    fun signOut() {
        tokenStore.clear()
        // These on-device caches are keyed by puzzle id alone, not by account -- without
        // this, whoever signs in next would inherit this account's "already played"
        // badges and cached results until the next day's puzzle.
        BoggleProgressStore.clearAll()
        NumbersProgressStore.clearAll()
        DeveloperStore.clear()
        _state.value = AuthState.SignedOut
    }

    /** Keeps the cached display name (shown elsewhere, e.g. Friends' "Signed in as X")
     * in sync after a successful Settings update, without a full re-login.
     */
    fun updateDisplayName(displayName: String) {
        if (_state.value !is AuthState.SignedIn) return
        tokenStore.displayName = displayName
        _state.value = AuthState.SignedIn(displayName)
    }

    private suspend fun onSignedIn(tokens: TokenPairDto) {
        tokenStore.saveTokens(tokens.accessToken, tokens.refreshToken)
        val profile = ApiClient.authenticatedService.getMyProfile()
        tokenStore.displayName = profile.displayName
        tokenStore.emailVerified = profile.emailVerified
        _state.value = if (profile.emailVerified) {
            AuthState.SignedIn(profile.displayName)
        } else {
            AuthState.SignedInUnverified(profile.displayName)
        }
    }
}
