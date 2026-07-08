package com.tomdunkley.dailypuzzles.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.auth.AuthState
import com.tomdunkley.dailypuzzles.data.network.dto.LeaderboardEntryDto
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.NumbersIconColor
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.SignInPrompt
import com.tomdunkley.dailypuzzles.ui.components.WordsIconColor
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor

private fun gameSolidColor(gameId: String): Color = when (gameId) {
    "boggle" -> WordsSolidColor
    "numbers" -> NumbersSolidColor
    else -> WordsSolidColor
}

private fun gameIconColor(gameId: String): Color = when (gameId) {
    "boggle" -> WordsIconColor
    "numbers" -> NumbersIconColor
    else -> WordsIconColor
}

@Composable
fun LeaderboardScreen(
    onAddFriendsClick: () -> Unit,
    onGoToSettings: () -> Unit,
    onViewScore: (puzzleId: String, userId: String) -> Unit,
    viewModel: LeaderboardViewModel = viewModel(),
) {
    val authState by AuthRepository.state.collectAsState()

    Scaffold(
        topBar = { SectionTopBar(title = "Rankings") },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (authState) {
                is AuthState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is AuthState.SignedIn -> SignedInLeaderboard(viewModel, onAddFriendsClick, onViewScore)
                else -> SignInPrompt("Sign in to see today's leaderboard.", onGoToSettings)
            }
        }
    }
}

@Composable
private fun SignedInLeaderboard(
    viewModel: LeaderboardViewModel,
    onAddFriendsClick: () -> Unit,
    onViewScore: (puzzleId: String, userId: String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    when (val state = uiState) {
        is LeaderboardUiState.Loading -> CenteredContent { CircularProgressIndicator() }
        is LeaderboardUiState.Error -> CenteredMessage(state.message)
        is LeaderboardUiState.Loaded -> {
            val selectedGame = state.games.getOrNull(state.selectedGameIndex)
            val solidColor = gameSolidColor(selectedGame?.game ?: "boggle")
            val iconColor = gameIconColor(selectedGame?.game ?: "boggle")
            Column(modifier = Modifier.fillMaxSize()) {
                DateSwitcher(
                    dateLabel = state.dateLabel,
                    canGoPrevious = state.dateOffset < 7,
                    canGoNext = state.dateOffset > 0,
                    onPrevious = viewModel::selectOlderDate,
                    onNext = viewModel::selectNewerDate,
                )
                GameSwitcher(
                    gameTitle = selectedGame?.title ?: "Words",
                    solidColor = solidColor,
                    canGoPrevious = state.selectedGameIndex > 0,
                    canGoNext = state.selectedGameIndex < state.games.lastIndex,
                    onPrevious = viewModel::selectPreviousGame,
                    onNext = viewModel::selectNextGame,
                )
                ScopeSwitcher(scope = state.scope, onScopeChange = viewModel::selectScope)
                if (state.scope == LeaderboardScope.FRIENDS && !state.hasFriends) {
                    CenteredContent {
                        val self = state.entries.firstOrNull()
                        if (self != null) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("You: ", style = MaterialTheme.typography.titleMedium)
                                ResultSummary(self)
                            }
                        } else {
                            Text(
                                text = "No scores yet today.",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Text(
                            text = "Add friends to compare scores.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onAddFriendsClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Text("ADD FRIENDS")
                        }
                    }
                } else if (state.entries.isEmpty()) {
                    CenteredContent {
                        Text(
                            text = "No scores yet today.",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.entries, key = { it.userId }) { entry ->
                            LeaderboardRow(
                                entry,
                                isSelf = entry.userId == state.selfUserId,
                                solidColor = solidColor,
                                iconColor = iconColor,
                                onClick = { onViewScore(state.puzzleId, entry.userId) },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntryDto, isSelf: Boolean, solidColor: Color, iconColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isSelf) iconColor.copy(alpha = 0.5f) else solidColor.copy(alpha = 0.08f))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                Text("#${entry.rank}", style = MaterialTheme.typography.titleMedium)
            }
            AvatarIcon(entry.avatarId, entry.avatarColorId, avatarIconColor = entry.avatarIconColor, size = 32.dp, modifier = Modifier.padding(start = 8.dp))
            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            ResultSummary(entry)
        }
    }
}

/** "320" + small grey "(5 away)", or "Got it" + small grey "(15s)" -- the headline number
 * always in the normal size, with the qualifying detail demoted to a smaller grey
 * parenthetical rather than competing with it for attention.
 */
@Composable
private fun ResultSummary(entry: LeaderboardEntryDto) {
    if (entry.game == "numbers") {
        val distance = entry.distance ?: 0
        if (distance == 0) {
            Text("Got it", style = MaterialTheme.typography.titleMedium)
            Text(
                " (${entry.durationSeconds ?: 0}s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text("${entry.resultValue ?: 0}", style = MaterialTheme.typography.titleMedium)
            Text(
                " ($distance away)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Text((entry.score ?: 0).toString(), style = MaterialTheme.typography.titleMedium)
        Text(
            " (words: ${entry.wordCount ?: 0})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DateSwitcher(
    dateLabel: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, top = 8.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = canGoPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Older day",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoPrevious) 1f else 0.3f),
            )
        }
        Text(
            dateLabel,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Newer day",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoNext) 1f else 0.3f),
            )
        }
    }
}

@Composable
private fun GameSwitcher(
    gameTitle: String,
    solidColor: Color,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious, enabled = canGoPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous game",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoPrevious) 1f else 0.3f),
            )
        }
        Text(gameTitle, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next game",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoNext) 1f else 0.3f),
            )
        }
    }
}

@Composable
private fun ScopeSwitcher(scope: LeaderboardScope, onScopeChange: (LeaderboardScope) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScopeOption("FRIENDS", selected = scope == LeaderboardScope.FRIENDS) {
            onScopeChange(LeaderboardScope.FRIENDS)
        }
        ScopeOption("GLOBAL", selected = scope == LeaderboardScope.GLOBAL) {
            onScopeChange(LeaderboardScope.GLOBAL)
        }
    }
}

@Composable
private fun RowScope.ScopeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CenteredContent(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        content = content,
    )
}

@Composable
private fun CenteredMessage(message: String) {
    CenteredContent {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}
