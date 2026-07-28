package com.tomdunkley.dailypuzzles.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val FRIENDS = "friends"
    const val LEADERBOARD = "leaderboard"
    const val SETTINGS = "settings"
    const val ACHIEVEMENTS = "achievements"
    const val ACCOUNT_SETTINGS = "account_settings"
    const val BOGGLE = "boggle"
    const val BOGGLE_UNLIMITED = "boggle_unlimited"
    const val NUMBERS = "numbers"
    const val NUMBERS_UNLIMITED = "numbers_unlimited"
    const val ROOTS = "roots"
    const val ROOTS_UNLIMITED = "roots_unlimited"
    const val VERIFY_EMAIL = "verify_email"
    const val FORGOT_PASSWORD = "forgot_password"
    const val CHANGE_PASSWORD = "change_password"
    const val AVATAR_PICKER = "avatar_picker"
    const val SCORE_DETAIL = "score_detail/{puzzleId}/{userId}"
    const val USER_PROFILE = "user_profile/{userId}"

    fun scoreDetail(puzzleId: String, userId: String) = "score_detail/$puzzleId/$userId"
    fun userProfile(userId: String) = "user_profile/$userId"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home),
    BottomNavItem(Routes.ACHIEVEMENTS, "Trophies", Icons.Filled.EmojiEvents),
    BottomNavItem(Routes.LEADERBOARD, "Rankings", Icons.Filled.Leaderboard),
    BottomNavItem(Routes.SETTINGS, "Profile", Icons.Filled.Person),
)
