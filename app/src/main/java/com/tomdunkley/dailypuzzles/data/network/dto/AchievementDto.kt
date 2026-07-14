package com.tomdunkley.dailypuzzles.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AchievementItemDto(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    @SerialName("unlocked_at") val unlockedAt: String? = null,
    @SerialName("unlocks_avatar_id") val unlocksAvatarId: String? = null,
    @SerialName("unlocks_color_id") val unlocksColorId: String? = null,
    @SerialName("unlocks_icon_color_id") val unlocksIconColorId: String? = null,
)

@Serializable
data class ClaimAchievementRequest(@SerialName("achievement_id") val achievementId: String)

@Serializable
data class ClaimAchievementResponse(@SerialName("newly_unlocked") val newlyUnlocked: List<String> = emptyList())

@Serializable
data class AchievementSummaryDto(
    val achievements: List<AchievementItemDto>,
    @SerialName("unlocked_ids") val unlockedIds: List<String>,
)
