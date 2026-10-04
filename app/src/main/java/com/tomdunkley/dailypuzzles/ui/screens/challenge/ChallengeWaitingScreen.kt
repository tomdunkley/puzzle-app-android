package com.tomdunkley.dailypuzzles.ui.screens.challenge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.tomdunkley.dailypuzzles.data.network.dto.ScoreDetailDto
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar

@Composable
fun ChallengeWaitingScreen(
    challengeId: String,
    opponentName: String,
    bothPlayed: Boolean,
    myUserId: String,
    onBack: () -> Unit,
    onViewMyResult: (challengeId: String, userId: String) -> Unit,
    viewModel: ChallengeWaitingViewModel = viewModel(),
) {
    val resultState by viewModel.resultState.collectAsState()

    LaunchedEffect(challengeId, myUserId, bothPlayed) {
        if (bothPlayed) viewModel.loadResult(challengeId, myUserId)
    }

    Scaffold(
        topBar = { SectionTopBar(title = "Challenge", onBack = onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (!bothPlayed) {
                WaitingContent(opponentName, challengeId, myUserId, onBack, onViewMyResult)
            } else {
                when (val state = resultState) {
                    is ChallengeResultUiState.Idle, is ChallengeResultUiState.Loading ->
                        CircularProgressIndicator()
                    is ChallengeResultUiState.Error -> {
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                            Text("BACK TO CHALLENGES")
                        }
                    }
                    is ChallengeResultUiState.Loaded -> ResultContent(
                        result = state,
                        challengeId = challengeId,
                        onViewResult = onViewMyResult,
                        onContinue = onBack,
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingContent(
    opponentName: String,
    challengeId: String,
    myUserId: String,
    onBack: () -> Unit,
    onViewMyResult: (String, String) -> Unit,
) {
    Text(
        text = "Waiting for $opponentName...",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "You've submitted your result. We'll let you know when $opponentName plays.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))
    Button(
        onClick = { onViewMyResult(challengeId, myUserId) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
        ),
    ) { Text("VIEW YOUR RESULT") }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("BACK TO CHALLENGES") }
}

@Composable
private fun ResultContent(
    result: ChallengeResultUiState.Loaded,
    challengeId: String,
    onViewResult: (challengeId: String, userId: String) -> Unit,
    onContinue: () -> Unit,
) {
    val outcomeText = when (result.outcome) {
        "win" -> "You won!"
        "loss" -> "You lost"
        else -> "It's a draw"
    }
    Text(
        text = outcomeText,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    ScoreSummaryRow(result.myResult, result.opponentResult)
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = { onViewResult(challengeId, result.myResult.userId) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
        ),
    ) { Text("VIEW YOUR BOARD") }
    Spacer(Modifier.height(8.dp))
    if (result.opponentResult.opponentUserId != null || result.myResult.opponentUserId != null) {
        val opponentId = result.myResult.opponentUserId ?: ""
        OutlinedButton(
            onClick = { onViewResult(challengeId, opponentId) },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        ) { Text("VIEW THEIR BOARD") }
        Spacer(Modifier.height(8.dp))
    }
    OutlinedButton(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
    ) { Text("BACK TO CHALLENGES") }
}

@Composable
private fun ScoreSummaryRow(myResult: ScoreDetailDto, opponentResult: ScoreDetailDto) {
    val mySummary = formatSummary(myResult)
    val theirSummary = formatSummary(opponentResult)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AvatarIcon(
                myResult.avatarId, myResult.avatarColorId,
                avatarIconColor = myResult.avatarIconColor, size = 40.dp,
            )
            Text(myResult.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(mySummary, style = MaterialTheme.typography.titleMedium)
        }
        Text("vs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AvatarIcon(
                opponentResult.avatarId, opponentResult.avatarColorId,
                avatarIconColor = opponentResult.avatarIconColor, size = 40.dp,
            )
            Text(opponentResult.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(theirSummary, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun formatSummary(result: ScoreDetailDto): String = when (result.game) {
    "boggle" -> "${result.score ?: 0} pts"
    "numbers" -> {
        val d = result.distance ?: 0
        val secs = result.durationSeconds ?: 0
        if (d == 0) "Exact (${secs}s)" else "$d away"
    }
    else -> {
        val secs = result.durationSeconds ?: 0
        val m = secs / 60; val s = secs % 60
        if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
