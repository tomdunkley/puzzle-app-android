package com.tomdunkley.dailypuzzles.ui.screens.challenge

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomdunkley.dailypuzzles.data.challenges.ChallengeGameStore
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.RootsSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor

@Composable
fun ChallengeStartScreen(
    game: String,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val gameName = when (game) {
        "boggle" -> "Words"
        "numbers" -> "Numbers"
        else -> "Routes"
    }
    val gameIcon = when (game) {
        "boggle" -> Icons.Filled.GridOn
        "numbers" -> Icons.Filled.Calculate
        else -> Icons.Filled.Route
    }
    val gameColor = when (game) {
        "boggle" -> WordsSolidColor
        "numbers" -> NumbersSolidColor
        else -> RootsSolidColor
    }

    val opponentName = ChallengeGameStore.pendingOpponentName ?: "Opponent"
    val seed = ChallengeGameStore.pendingSeed

    Scaffold(
        topBar = { SectionTopBar(title = "Challenge", onBack = onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            // Game icon + name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    gameIcon,
                    contentDescription = null,
                    tint = gameColor,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    gameName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = gameColor,
                )
            }

            // Opponent info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarIcon(
                    avatarId = ChallengeGameStore.pendingOpponentAvatarId,
                    avatarColorId = ChallengeGameStore.pendingOpponentAvatarColorId,
                    avatarIconColor = ChallengeGameStore.pendingOpponentAvatarIconColor,
                    size = 72.dp,
                )
                Text(
                    "vs $opponentName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }

            // Seed
            if (seed != null) {
                Text(
                    seed,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Text("START", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
