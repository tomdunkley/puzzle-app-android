package com.tomdunkley.dailypuzzles.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.data.network.dto.DailyBestScoreDto
import com.tomdunkley.dailypuzzles.data.network.dto.PublicUserProfileDto
import com.tomdunkley.dailypuzzles.data.network.dto.TodayGameScoreDto
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.RootsSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor

@Composable
fun UserProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onViewScore: (puzzleId: String, userId: String) -> Unit = { _, _ -> },
    viewModel: UserProfileViewModel = viewModel(),
) {
    LaunchedEffect(userId) { viewModel.load(userId) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { SectionTopBar(title = "Profile", onBack = onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val state = uiState) {
                is UserProfileUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.padding(24.dp),
                )
                is UserProfileUiState.Error -> Text(
                    state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )
                is UserProfileUiState.Loaded -> ProfileContent(
                    profile = state.profile,
                    userId = userId,
                    onAddFriend = viewModel::addFriend,
                    onRemoveFriend = viewModel::removeFriend,
                    onViewScore = onViewScore,
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: PublicUserProfileDto,
    userId: String,
    onAddFriend: () -> Unit,
    onRemoveFriend: () -> Unit,
    onViewScore: (puzzleId: String, userId: String) -> Unit,
) {
    var showUnfriendDialog by remember { mutableStateOf(false) }

    if (showUnfriendDialog) {
        AlertDialog(
            onDismissRequest = { showUnfriendDialog = false },
            title = { Text("Remove ${profile.displayName}?") },
            text = { Text("You'll need to send a new friend request to add them again.") },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveFriend()
                    showUnfriendDialog = false
                }) { Text("REMOVE") }
            },
            dismissButton = {
                TextButton(onClick = { showUnfriendDialog = false }) { Text("CANCEL") }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AvatarIcon(
                profile.avatarId,
                profile.avatarColorId,
                avatarIconColor = profile.avatarIconColor,
                size = 56.dp,
            )
            Text(profile.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        when (profile.friendshipStatus) {
            "self" -> Unit
            "friends" -> OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                onClick = { showUnfriendDialog = true },
            ) {
                Text("REMOVE FRIEND")
            }
            "request_sent" -> OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                onClick = {},
                enabled = false,
            ) {
                Text("REQUEST SENT")
            }
            "request_received" -> Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
                onClick = onAddFriend,
            ) {
                Text("ACCEPT REQUEST")
            }
            else -> Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
                onClick = onAddFriend,
            ) {
                Text("ADD FRIEND")
            }
        }

        HorizontalDivider()

        Text("TROPHIES", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(
                "${profile.trophyCount} / ${profile.totalTrophies}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        HorizontalDivider()

        Text("TODAY'S RESULTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(0.dp))
        TodayScoreRow("boggle", "Words", profile.todayBoggle, profile.boggleDailyBest, userId, onViewScore)
        Spacer(Modifier.height(4.dp))
        TodayScoreRow("numbers", "Numbers", profile.todayNumbers, profile.numbersDailyBest, userId, onViewScore)
        Spacer(Modifier.height(4.dp))
        TodayScoreRow("routes", "Routes", profile.todayRoutes, profile.routesDailyBest, userId, onViewScore)
    }
}

@Composable
private fun TodayScoreRow(
    gameId: String,
    gameName: String,
    today: TodayGameScoreDto?,
    best: DailyBestScoreDto?,
    userId: String,
    onViewScore: (puzzleId: String, userId: String) -> Unit,
) {
    val solidColor = when (gameId) {
        "boggle" -> WordsSolidColor
        "numbers" -> NumbersSolidColor
        else -> RootsSolidColor
    }
    val gameIcon = when (gameId) {
        "boggle" -> Icons.Filled.GridOn
        "numbers" -> Icons.Filled.Calculate
        else -> Icons.Filled.Route
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(solidColor.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(gameIcon, contentDescription = null, tint = solidColor, modifier = Modifier.size(18.dp))
            Text(gameName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = solidColor)
        }
        if (today == null) {
            Text(
                "Not played today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "Today: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    when (gameId) {
                        "numbers" -> {
                            val distance = today.distance ?: 0
                            if (distance == 0) {
                                Text("Got it", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    " (${today.durationSeconds ?: 0}s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text("${today.resultValue ?: 0}", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    " ($distance away)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        "routes" -> {
                            val secs = today.durationSeconds ?: 0
                            val m = secs / 60
                            val s = secs % 60
                            Text(
                                if (m > 0) "${m}m ${s}s" else "${s}s",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        else -> {
                            Text("${today.score ?: 0}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                " (words: ${today.wordCount ?: 0})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                OutlinedButton(onClick = { onViewScore(today.puzzleId ?: return@OutlinedButton, userId) }) {
                    Text("VIEW")
                }
            }
        }
        if (best != null) {
            val bestText = when (gameId) {
                "numbers" -> if ((best.distance ?: 1) == 0) "Best: Exact!" else "Best: ${best.resultValue} (${best.distance} away)"
                "routes" -> {
                    val secs = best.durationSeconds ?: 0
                    val m = secs / 60
                    val s = secs % 60
                    "Best: ${if (m > 0) "${m}m ${s}s" else "${s}s"}"
                }
                else -> "Best: ${best.score} pts (words: ${best.wordCount ?: 0})"
            }
            Text(bestText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
