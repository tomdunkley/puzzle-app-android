package com.tomdunkley.dailypuzzles.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSearchResultDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_id") val avatarId: String? = null,
    @SerialName("avatar_color_id") val avatarColorId: String? = null,
    @SerialName("avatar_icon_color") val avatarIconColor: String? = null,
    @SerialName("friendship_status") val friendshipStatus: String,
)

@Serializable
data class FriendRequestSummaryDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_id") val avatarId: String? = null,
    @SerialName("avatar_color_id") val avatarColorId: String? = null,
    @SerialName("avatar_icon_color") val avatarIconColor: String? = null,
)

@Serializable
data class FriendSummaryDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_id") val avatarId: String? = null,
    @SerialName("avatar_color_id") val avatarColorId: String? = null,
    @SerialName("avatar_icon_color") val avatarIconColor: String? = null,
)

@Serializable
data class SendFriendRequestDto(@SerialName("to_user_id") val toUserId: String)
