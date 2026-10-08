package com.tomdunkley.dailypuzzles.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "home"
    const val CHALLENGES = "challenges"
    const val FRIENDS = "friends"
    const val LEADERBOARD = "leaderboard"
    const val SETTINGS = "settings"
    const val ACHIEVEMENTS = "achievements"
    const val ACCOUNT_SETTINGS = "account_settings"
    const val BOGGLE = "boggle"
    const val BOGGLE_UNLIMITED = "boggle_unlimited"
    const val NUMBERS = "numbers"
    const val NUMBERS_UNLIMITED = "numbers_unlimited"
    const val ROOTS = "routes"
    const val ROOTS_UNLIMITED = "routes_unlimited"
    const val VERIFY_EMAIL = "verify_email"
    const val FORGOT_PASSWORD = "forgot_password"
    const val CHANGE_PASSWORD = "change_password"
    const val AVATAR_PICKER = "avatar_picker"
    const val SCORE_DETAIL = "score_detail/{puzzleId}/{userId}?gameHint={gameHint}"
    const val USER_PROFILE = "user_profile/{userId}"
    const val CHALLENGE = "challenge/{friendId}"
    const val BOGGLE_CHALLENGE = "boggle_challenge/{challengeId}"
    const val NUMBERS_CHALLENGE = "numbers_challenge/{challengeId}"
    const val ROUTES_CHALLENGE = "routes_challenge/{challengeId}"
    const val CHALLENGE_WAITING = "challenge_waiting/{challengeId}/{opponentName}/{bothPlayed}/{myUserId}/{game}"
    const val CHALLENGE_START = "challenge_start/{game}"
    const val BOGGLE_TUTORIAL = "boggle_tutorial"
    const val NUMBERS_TUTORIAL = "numbers_tutorial"
    const val ROUTES_TUTORIAL = "routes_tutorial"

    fun scoreDetail(puzzleId: String, userId: String, gameHint: String = "") =
        if (gameHint.isEmpty()) "score_detail/$puzzleId/$userId"
        else "score_detail/$puzzleId/$userId?gameHint=$gameHint"
    fun userProfile(userId: String) = "user_profile/$userId"
    fun challenge(friendId: String) = "challenge/$friendId"
    fun challengeStart(game: String) = "challenge_start/$game"
    fun boggleChallenge(challengeId: String) = "boggle_challenge/$challengeId"
    fun numbersChallenge(challengeId: String) = "numbers_challenge/$challengeId"
    fun routesChallenge(challengeId: String) = "routes_challenge/$challengeId"
    fun challengeWaiting(challengeId: String, opponentName: String, bothPlayed: Boolean, myUserId: String, game: String) =
        "challenge_waiting/$challengeId/${opponentName.replace("/", "_")}/$bothPlayed/$myUserId/$game"
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
