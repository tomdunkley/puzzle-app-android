package com.tomdunkley.dailypuzzles.data.auth

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val displayName: String) : AuthState
    data class SignedInUnverified(val displayName: String) : AuthState
}
