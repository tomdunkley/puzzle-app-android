package com.tomdunkley.dailypuzzles.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenPairDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class GoogleSignInRequestDto(
    @SerialName("id_token") val idToken: String,
    @SerialName("guest_access_token") val guestAccessToken: String? = null,
)

@Serializable
data class RefreshRequestDto(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    @SerialName("guest_access_token") val guestAccessToken: String? = null,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    @SerialName("guest_access_token") val guestAccessToken: String? = null,
)

@Serializable
data class VerifyEmailRequestDto(val code: String)

@Serializable
data class ForgotPasswordRequestDto(val email: String)

@Serializable
data class ResetPasswordRequestDto(
    val email: String,
    val code: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class SetPasswordRequestDto(@SerialName("new_password") val newPassword: String)

@Serializable
data class ChangePasswordRequestDto(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)
