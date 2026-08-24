package com.tomdunkley.dailypuzzles.ui.screens.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.data.challenges.PendingChallengesStore
import com.tomdunkley.dailypuzzles.data.network.dto.ChallengeSummaryGameDto
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.RootsSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor

@Composable
fun ChallengeScreen(
    friendId: String,
    onBack: () -> Unit,
    onStartGame: (game: String, challengeId: String) -> Unit,
    onViewResult: (challengeId: String, userId: String) -> Unit,
    viewModel: ChallengeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val navEvent by viewModel.navEvent.collectAsState()
    val isCreating by viewModel.isCreatingChallenge.collectAsState()
    val pendingByFriend by PendingChallengesStore.byFriend.collectAsState()

    LaunchedEffect(friendId) { viewModel.load(friendId) }

    LaunchedEffect(navEvent) {
        val event = navEvent ?: return@LaunchedEffect
        if (event is ChallengeNavEvent.StartGame) {
            viewModel.clearNavEvent()
            onStartGame(event.game, event.challengeId)
        }
    }

    Scaffold(
        topBar = { SectionTopBar(title = "Challenge", onBack = onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when (val state = uiState) {
            is ChallengeUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is ChallengeUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.message, style = MaterialTheme.typography.bodyLarge)
            }

            is ChallengeUiState.Loaded -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    AvatarIcon(
                        state.friendProfile.avatarId,
                        state.friendProfile.avatarColorId,
                        avatarIconColor = state.friendProfile.avatarIconColor,
                        size = 48.dp,
                    )
                    Text(
                        state.friendProfile.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                val pendingCount = pendingByFriend[friendId] ?: 0

                state.games.forEachIndexed { index, game ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    GameChallengeSection(
                        game = game,
                        friendId = friendId,
                        friendName = state.friendProfile.displayName,
                        isCreating = isCreating,
                        hasPendingChallenge = pendingCount > 0 && game.status == "open",
                        onChallenge = { viewModel.createChallenge(friendId, game.game) },
                        onPlay = {
                            val pd = game.puzzleData ?: return@GameChallengeSection
                            viewModel.playExistingChallenge(game.game, game.challengeId!!, pd)
                        },
                        onViewMyResult = { id -> onViewResult(id, state.myUserId) },
                        onViewTheirResult = { id -> onViewResult(id, friendId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GameChallengeSection(
    game: ChallengeSummaryGameDto,
    friendId: String,
    friendName: String,
    isCreating: Boolean,
    hasPendingChallenge: Boolean,
    onChallenge: () -> Unit,
    onPlay: () -> Unit,
    onViewMyResult: (challengeId: String) -> Unit,
    onViewTheirResult: (challengeId: String) -> Unit,
) {
    val gameName = when (game.game) {
        "boggle" -> "Words"
        "numbers" -> "Numbers"
        else -> "Routes"
    }
    val gameColor = when (game.game) {
        "boggle" -> WordsSolidColor
        "numbers" -> NumbersSolidColor
        else -> RootsSolidColor
    }
    val gameIcon = when (game.game) {
        "boggle" -> Icons.Filled.GridOn
        "numbers" -> Icons.Filled.Calculate
        else -> Icons.Filled.Route
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .background(gameColor.copy(alpha = 0.08f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: icon + game name + record
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(gameIcon, contentDescription = null, tint = gameColor, modifier = Modifier.size(18.dp))
                Text(gameName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = gameColor)
            }
            Text(
                "${game.recordWins}W  ${game.recordDraws}D  ${game.recordLosses}L",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Challenged / waiting state text
        when (game.status) {
            "open" -> Text(
                text = "$friendName has challenged you",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            "waiting" -> Text(
                text = "Waiting for $friendName...",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Expiry info for active challenge
        if (game.status != null && game.expiresAtEpoch != null) {
            val nowEpoch = System.currentTimeMillis() / 1000
            val secondsLeft = game.expiresAtEpoch - nowEpoch
            val daysLeft = (secondsLeft / (24 * 3600)).coerceAtLeast(0)
            val expiryText = when {
                daysLeft == 0L -> "Challenge expires today"
                daysLeft == 1L -> "Challenge expires in 1 day"
                else -> "Challenge expires in $daysLeft days"
            }
            Text(
                expiryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Action button
        when (game.status) {
            null -> Button(
                onClick = onChallenge,
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
            ) { Text("NEW CHALLENGE") }
            "open" -> BadgedBox(
                badge = { if (hasPendingChallenge) Badge(modifier = Modifier.size(10.dp)) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = onPlay,
                    enabled = !isCreating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                ) { Text("RESPOND TO CHALLENGE") }
            }
            "waiting" -> Button(
                onClick = { onViewMyResult(game.challengeId!!) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
            ) { Text("VIEW YOUR RESULT") }
        }

        // Last completed challenge, separated by a subtle divider
        if (game.lastChallengeId != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            val lastResult = game.lastResult
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val outcomeText = when (lastResult?.outcome) {
                        "win" -> "You won the last challenge"
                        "loss" -> "You lost the last challenge"
                        "draw" -> "Last challenge was a draw"
                        else -> "Last challenge"
                    }
                    Text(
                        outcomeText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (lastResult != null) {
                        Text(
                            "${lastResult.mySummary} vs ${lastResult.theirSummary}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedButton(
                        onClick = { onViewMyResult(game.lastChallengeId) },
                    ) { Text("YOUR RESULT") }
                    OutlinedButton(
                        onClick = { onViewTheirResult(game.lastChallengeId) },
                    ) { Text("THEIR RESULT") }
                }
            }
        }
    }
}
