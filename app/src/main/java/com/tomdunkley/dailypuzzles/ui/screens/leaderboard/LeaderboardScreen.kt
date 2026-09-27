package com.tomdunkley.dailypuzzles.ui.screens.leaderboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.auth.AuthState
import com.tomdunkley.dailypuzzles.data.network.dto.LeaderboardEntryDto
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.NumbersIconColor
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.RootsIconColor
import com.tomdunkley.dailypuzzles.ui.components.RootsSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.SignInPrompt
import com.tomdunkley.dailypuzzles.ui.components.WordsIconColor
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor
import java.time.LocalDate
import java.time.ZoneOffset

private fun gameSolidColor(gameId: String): Color = when (gameId) {
    "boggle" -> WordsSolidColor
    "numbers" -> NumbersSolidColor
    "routes" -> RootsSolidColor
    else -> WordsSolidColor
}

private fun gameCircleColor(gameId: String): Color = when (gameId) {
    "boggle" -> WordsIconColor
    "numbers" -> NumbersIconColor
    "routes" -> RootsIconColor
    else -> WordsIconColor
}

private fun gameIcon(gameId: String): ImageVector = when (gameId) {
    "boggle" -> Icons.Filled.GridOn
    "numbers" -> Icons.Filled.Calculate
    else -> Icons.Filled.Route
}

private fun gameTitle(gameId: String): String = when (gameId) {
    "boggle" -> "Words"
    "numbers" -> "Numbers"
    "routes" -> "Routes"
    else -> gameId
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
            val iconColor = gameCircleColor(selectedGame?.game ?: "boggle")
            Column(modifier = Modifier.fillMaxSize()) {
                GameCarousel(
                    games = state.games.map { it.game },
                    selectedGameIndex = state.selectedGameIndex,
                    onSelectGame = viewModel::selectGame,
                )
                DateSwitcher(
                    dateLabel = state.dateLabel,
                    canGoPrevious = state.dateOffset < MAX_DATE_OFFSET,
                    canGoNext = state.dateOffset > 0,
                    onPrevious = viewModel::selectOlderDate,
                    onNext = viewModel::selectNewerDate,
                    onPickDate = viewModel::selectDate,
                    todayDate = state.todayDate,
                    currentDateOffset = state.dateOffset,
                )
                ScopeSwitcher(scope = state.scope, onScopeChange = viewModel::selectScope)
                if (state.scope == LeaderboardScope.FRIENDS && !state.hasFriends) {
                    CenteredContent {
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
private fun GameCarousel(
    games: List<String>,
    selectedGameIndex: Int,
    onSelectGame: (Int) -> Unit,
) {
    if (games.isEmpty()) return

    val circleSize = 72.dp
    val iconSize = 40.dp
    val outlineColor = Color(0xFF9E9E9E) // grey 500

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
    ) {
        if (games.size <= 5) {
            // Static centred row — no carousel needed
            games.forEachIndexed { index, gameId ->
                val isSelected = index == selectedGameIndex
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.82f,
                    animationSpec = tween(200),
                    label = "gameScale$index",
                )
                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.5f,
                    animationSpec = tween(200),
                    label = "gameAlpha$index",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelectGame(index) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(circleSize)
                            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                            .background(gameCircleColor(gameId), shape = CircleShape)
                            .border(1.dp, outlineColor, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = gameIcon(gameId),
                            contentDescription = gameId,
                            tint = gameSolidColor(gameId),
                            modifier = Modifier.size(iconSize),
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isSelected) gameTitle(gameId) else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        } else {
            // Pager carousel for large game lists
            val pagerState = rememberPagerState(
                initialPage = selectedGameIndex,
                pageCount = { games.size },
            )
            LaunchedEffect(selectedGameIndex) {
                if (pagerState.currentPage != selectedGameIndex) {
                    pagerState.animateScrollToPage(selectedGameIndex)
                }
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val sidePadding = (maxWidth - circleSize) / 2
                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = sidePadding),
                    pageSpacing = 8.dp,
                    pageSize = PageSize.Fixed(circleSize),
                    modifier = Modifier.fillMaxWidth(),
                ) { page ->
                    val gameId = games[page]
                    val isSelected = page == selectedGameIndex
                    val isCentered = page == pagerState.currentPage
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.82f,
                        animationSpec = tween(200),
                        label = "gameScale$page",
                    )
                    val alpha by animateFloatAsState(
                        targetValue = if (isCentered) 1f else 0.35f,
                        animationSpec = tween(200),
                        label = "gameAlpha$page",
                    )
                    Box(
                        modifier = Modifier
                            .size(circleSize)
                            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                            .background(gameCircleColor(gameId), shape = CircleShape)
                            .border(1.dp, outlineColor, shape = CircleShape)
                            .clickable { onSelectGame(page) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = gameIcon(gameId),
                            contentDescription = gameId,
                            tint = gameSolidColor(gameId),
                            modifier = Modifier.size(iconSize),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSwitcher(
    dateLabel: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPickDate: (LocalDate) -> Unit,
    todayDate: LocalDate,
    currentDateOffset: Int,
) {
    var showPicker by remember { mutableStateOf(false) }

    val currentDate = todayDate.minusDays(currentDateOffset.toLong())
    val currentEpochMillis = currentDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val minEpochMillis = todayDate.minusDays(MAX_DATE_OFFSET.toLong()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    val maxEpochMillis = todayDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 4.dp),
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
            modifier = Modifier.clickable { showPicker = true },
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Newer day",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (canGoNext) 1f else 0.3f),
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentEpochMillis,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis in minEpochMillis..maxEpochMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showPicker = false
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val picked = LocalDate.ofEpochDay(millis / 86_400_000)
                        onPickDate(picked)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
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
                Text("${if (entry.isTied) "=" else "#"}${entry.rank}", style = MaterialTheme.typography.titleMedium)
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
    } else if (entry.game == "routes") {
        val secs = entry.durationSeconds ?: 0
        val m = secs / 60
        val s = secs % 60
        val timeStr = if (m > 0) "${m}m ${s.toString().padStart(2, '0')}s" else "${s}s"
        Text(timeStr, style = MaterialTheme.typography.titleMedium)
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
