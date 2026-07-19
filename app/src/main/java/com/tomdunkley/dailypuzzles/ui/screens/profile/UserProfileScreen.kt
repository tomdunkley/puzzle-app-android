package com.tomdunkley.dailypuzzles.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import androidx.compose.foundation.BorderStroke

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
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val state = uiState) {
                is UserProfileUiState.Loading -> CircularProgressIndicator()
                is UserProfileUiState.Error -> Text(
                    state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
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
        Text(
            "${profile.trophyCount} / ${profile.totalTrophies}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        HorizontalDivider()

        Text("TODAY'S RESULTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(0.dp))
        TodayScoreRow("Words", profile.todayBoggle, profile.boggleDailyBest, userId, onViewScore)
        Spacer(Modifier.height(4.dp))
        TodayScoreRow("Numbers", profile.todayNumbers, profile.numbersDailyBest, userId, onViewScore)
    }
}

@Composable
private fun TodayScoreRow(
    gameName: String,
    today: TodayGameScoreDto?,
    best: DailyBestScoreDto?,
    userId: String,
    onViewScore: (puzzleId: String, userId: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(gameName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (today == null) {
            Text(
                "Not played today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val todayText = if (today.game == "numbers") {
                if ((today.distance ?: 1) == 0) "Solved!" else "${today.resultValue} (${today.distance} away)"
            } else {
                "${today.score} pts"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Today: $todayText", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { onViewScore(best?.puzzleId ?: return@TextButton, userId) }) {
                    Text("VIEW")
                }
            }
        }
        if (best != null) {
            val bestText = if (best.game == "numbers") {
                if ((best.distance ?: 1) == 0) "Best: Exact!" else "Best: ${best.resultValue} (${best.distance} away)"
            } else {
                "Best: ${best.score} pts"
            }
            Text(bestText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
