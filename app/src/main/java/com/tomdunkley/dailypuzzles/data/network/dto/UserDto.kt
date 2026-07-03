package com.tomdunkley.dailypuzzles.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreakInfoDto(
    val current: Int,
    val longest: Int,
    @SerialName("last_played_date") val lastPlayedDate: String? = null,
)

@Serializable
data class UserProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val email: String? = null,
    @SerialName("avatar_id") val avatarId: String? = null,
    @SerialName("avatar_color_id") val avatarColorId: String? = null,
    @SerialName("avatar_icon_color") val avatarIconColor: String? = null,
    val streaks: Map<String, StreakInfoDto> = emptyMap(),
    @SerialName("email_verified") val emailVerified: Boolean = false,
    @SerialName("has_password") val hasPassword: Boolean = false,
    @SerialName("has_google") val hasGoogle: Boolean = false,
    @SerialName("visible_on_global_leaderboard") val visibleOnGlobalLeaderboard: Boolean = true,
    @SerialName("is_developer") val isDeveloper: Boolean = false,
)

@Serializable
data class UpdateProfileRequestDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_id") val avatarId: String? = null,
    @SerialName("avatar_color_id") val avatarColorId: String? = null,
    @SerialName("avatar_icon_color") val avatarIconColor: String? = null,
    @SerialName("visible_on_global_leaderboard") val visibleOnGlobalLeaderboard: Boolean? = null,
)
