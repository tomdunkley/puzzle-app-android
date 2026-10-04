package com.tomdunkley.dailypuzzles.ui.screens.challenges

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.derivedStateOf
import com.tomdunkley.dailypuzzles.data.challenges.CompletedChallengesStore
import com.tomdunkley.dailypuzzles.data.challenges.InProgressChallengeStore
import com.tomdunkley.dailypuzzles.data.network.dto.ChallengeSummaryGameDto
import com.tomdunkley.dailypuzzles.data.network.dto.FriendSummaryDto
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.RootsSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor

private fun gameColor(game: String) = when (game) {
    "boggle" -> WordsSolidColor
    "numbers" -> NumbersSolidColor
    else -> RootsSolidColor
}

private fun gameIcon(game: String) = when (game) {
    "boggle" -> Icons.Filled.GridOn
    "numbers" -> Icons.Filled.Calculate
    else -> Icons.Filled.Route
}

private fun gameName(game: String) = when (game) {
    "boggle" -> "Words"
    "numbers" -> "Numbers"
    else -> "Routes"
}

@Composable
fun ChallengesScreen(
    onBack: () -> Unit,
    onGoToChallenge: (friendId: String) -> Unit,
    onViewScore: (challengeId: String, userId: String) -> Unit = { _, _ -> },
    onStartGame: (game: String) -> Unit = {},
    viewModel: ChallengesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val navEvent by viewModel.navEvent.collectAsState()
    var showFriendPicker by remember { mutableStateOf(false) }
    var myTurnExpanded by remember { mutableStateOf(true) }
    var waitingExpanded by remember { mutableStateOf(false) }
    var previousExpanded by remember { mutableStateOf(false) }
    val expandedCards = remember { mutableStateMapOf<String, Boolean>() }

    // Result queue: unseen completed challenges to show one by one
    var resultQueueIndex by remember { mutableStateOf(0) }
    val resultQueue by remember {
        derivedStateOf {
            val loaded = uiState as? ChallengesUiState.Loaded ?: return@derivedStateOf emptyList()
            loaded.challengeData.flatMap { fcd ->
                fcd.games.mapNotNull { game ->
                    val id = game.lastChallengeId ?: return@mapNotNull null
                    if (CompletedChallengesStore.isUnseen(id)) Pair(fcd, game) else null
                }
            }
        }
    }
    val currentResultItem = if (resultQueueIndex < resultQueue.size) resultQueue[resultQueueIndex] else null
    // Mark as seen when a result card is displayed
    currentResultItem?.second?.lastChallengeId?.let { id ->
        LaunchedEffect(id) { CompletedChallengesStore.markSeen(id) }
    }

    val wrappedOnViewScore: (String, String) -> Unit = { challengeId, userId ->
        CompletedChallengesStore.markSeen(challengeId)
        onViewScore(challengeId, userId)
    }

    val inProgressId = remember { InProgressChallengeStore.getInProgressChallengeId() }

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(navEvent) {
        val event = navEvent as? ChallengesNavEvent.StartGame ?: return@LaunchedEffect
        viewModel.clearNavEvent()
        onStartGame(event.game)
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = { SectionTopBar(title = "Challenges", onBack = onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when (val state = uiState) {
            is ChallengesUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is ChallengesUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text(state.message, style = MaterialTheme.typography.bodyLarge) }

            is ChallengesUiState.Loaded -> {
                val allPairs: List<Pair<FriendChallengeData, ChallengeSummaryGameDto>> =
                    state.challengeData.flatMap { fcd -> fcd.games.map { fcd to it } }

                fun List<Pair<FriendChallengeData, ChallengeSummaryGameDto>>.byRecent() =
                    sortedByDescending { (_, g) -> g.updatedAtEpoch ?: g.expiresAtEpoch ?: 0L }

                val myTurn = allPairs.filter { (_, g) -> g.status == "open" }.byRecent()
                val waiting = allPairs.filter { (_, g) -> g.status == "waiting" }.byRecent()
                val previous = allPairs.filter { (_, g) ->
                    g.status == null && g.lastChallengeId != null
                }.byRecent()

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        Button(
                            onClick = { showFriendPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) { Text("CREATE CHALLENGE") }
                    }

                    if (myTurn.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            CollapsibleSectionHeader("Your Turn", myTurnExpanded) { myTurnExpanded = !myTurnExpanded }
                        }
                        item {
                            AnimatedVisibility(
                                visible = myTurnExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    myTurn.forEach { (fcd, game) ->
                                        val cardKey = "open_${fcd.friend.userId}_${game.game}"
                                        val isResumable = game.challengeId != null && game.challengeId == inProgressId
                                        FriendGameCard(
                                            friend = fcd.friend,
                                            game = game,
                                            myUserId = state.myUserId,
                                            isExpanded = expandedCards[cardKey] == true,
                                            onToggle = { expandedCards[cardKey] = expandedCards[cardKey] != true },
                                            actionLabel = if (isResumable) "RESUME" else null,
                                            onAction = {
                                                if (isResumable) {
                                                    viewModel.resumeChallenge(game.game)
                                                } else {
                                                    val pd = game.puzzleData
                                                    val id = game.challengeId
                                                    if (pd != null && id != null) {
                                                        viewModel.playExistingChallenge(game.game, id, pd, fcd.friend)
                                                    } else {
                                                        onGoToChallenge(fcd.friend.userId)
                                                    }
                                                }
                                            },
                                            onViewScore = wrappedOnViewScore,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (waiting.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            CollapsibleSectionHeader("Waiting", waitingExpanded) { waitingExpanded = !waitingExpanded }
                        }
                        item {
                            AnimatedVisibility(
                                visible = waitingExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    waiting.forEach { (fcd, game) ->
                                        val cardKey = "waiting_${fcd.friend.userId}_${game.game}"
                                        FriendGameCard(
                                            friend = fcd.friend,
                                            game = game,
                                            myUserId = state.myUserId,
                                            isExpanded = expandedCards[cardKey] == true,
                                            onToggle = { expandedCards[cardKey] = expandedCards[cardKey] != true },
                                            onAction = { onGoToChallenge(fcd.friend.userId) },
                                            onViewScore = wrappedOnViewScore,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (myTurn.isEmpty() && waiting.isEmpty() && previous.isEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No active challenges.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if (previous.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            CollapsibleSectionHeader("Previous", previousExpanded) { previousExpanded = !previousExpanded }
                        }
                        item {
                            AnimatedVisibility(
                                visible = previousExpanded,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    previous.forEach { (fcd, game) ->
                                        val cardKey = "prev_${fcd.friend.userId}_${game.game}"
                                        FriendGameCard(
                                            friend = fcd.friend,
                                            game = game,
                                            myUserId = state.myUserId,
                                            isExpanded = expandedCards[cardKey] == true,
                                            onToggle = { expandedCards[cardKey] = expandedCards[cardKey] != true },
                                            onAction = { viewModel.createRematch(fcd.friend, game.game) },
                                            onViewScore = wrappedOnViewScore,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showFriendPicker) {
                    FriendPickerDialog(
                        friends = state.friends,
                        onDismiss = { showFriendPicker = false },
                        onSelectFriend = { friendId ->
                            showFriendPicker = false
                            onGoToChallenge(friendId)
                        },
                    )
                }

            }
        }
    }

    // Full-screen result overlay — shown on top of the Scaffold
    val loadedState = uiState as? ChallengesUiState.Loaded
    AnimatedVisibility(
        visible = currentResultItem != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        if (currentResultItem != null && loadedState != null) {
            val (fcd, game) = currentResultItem
            ResultQueueScreen(
                friendName = fcd.friend.displayName,
                game = game,
                myUserId = loadedState.myUserId,
                friendUserId = fcd.friend.userId,
                onViewMyResult = { wrappedOnViewScore(game.lastChallengeId!!, loadedState.myUserId) },
                onViewTheirResult = { wrappedOnViewScore(game.lastChallengeId!!, fcd.friend.userId) },
                onContinue = { resultQueueIndex++ },
            )
        }
    }
    } // end outer Box
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

@Composable
private fun CollapsibleSectionHeader(text: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onToggle,
            )
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ColumnScope.PreviousChallengeContent(
    game: ChallengeSummaryGameDto,
    myUserId: String,
    friendUserId: String,
    onViewScore: (challengeId: String, userId: String) -> Unit,
) {
    val lastResult = game.lastResult
    val outcomeText = when (lastResult?.outcome) {
        "win" -> "You won the last challenge"
        "loss" -> "You lost the last challenge"
        "draw" -> "Last challenge was a draw"
        else -> "Last challenge"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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
            Text(
                "${game.recordWins}W  ${game.recordDraws}D  ${game.recordLosses}L",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OutlinedButton(onClick = { onViewScore(game.lastChallengeId!!, myUserId) }) { Text("YOUR RESULT") }
            OutlinedButton(onClick = { onViewScore(game.lastChallengeId!!, friendUserId) }) { Text("THEIR RESULT") }
        }
    }
}

private val BlackButtonColors @Composable get() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.onSurface,
    contentColor = MaterialTheme.colorScheme.surface,
)

@Composable
private fun FriendGameCard(
    friend: FriendSummaryDto,
    game: ChallengeSummaryGameDto,
    myUserId: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAction: () -> Unit,
    onViewScore: (challengeId: String, userId: String) -> Unit,
    actionLabel: String? = null,
) {
    val color = gameColor(game.game)
    val lastResult = game.lastResult

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f))
            .padding(12.dp),
    ) {
        // Header row: chevron+avatar+name (left), game icon+name (right) — clickable to expand/collapse
        Row(
            modifier = Modifier.fillMaxWidth().clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onToggle,
            ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
                AvatarIcon(
                    friend.avatarId, friend.avatarColorId,
                    avatarIconColor = friend.avatarIconColor, size = 32.dp,
                )
                Text(
                    friend.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(gameIcon(game.game), contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Text(
                    gameName(game.game),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            when (game.status) {
            "waiting" -> {
                // Active challenge info — exactly as on the friend challenge page
                Text(
                    "Waiting for ${friend.displayName}...",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (game.expiresAtEpoch != null) {
                    val nowEpoch = System.currentTimeMillis() / 1000
                    val daysLeft = ((game.expiresAtEpoch - nowEpoch) / (24 * 3600)).coerceAtLeast(0)
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
                if (game.challengeId != null) {
                    Button(
                        onClick = { onViewScore(game.challengeId, myUserId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = BlackButtonColors,
                    ) { Text("VIEW YOUR RESULT") }
                }
                // Previous challenge below a divider
                if (game.lastChallengeId != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    PreviousChallengeContent(game, myUserId, friend.userId, onViewScore)
                }
            }
            "open" -> {
                val hasData = game.puzzleData != null
                Text(
                    if (hasData) "${friend.displayName} has challenged you" else "There is an open challenge",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (game.expiresAtEpoch != null) {
                    val nowEpoch = System.currentTimeMillis() / 1000
                    val daysLeft = ((game.expiresAtEpoch - nowEpoch) / (24 * 3600)).coerceAtLeast(0)
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
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth(),
                    colors = BlackButtonColors,
                ) { Text(actionLabel ?: if (hasData) "RESPOND TO CHALLENGE" else "PLAY") }
                if (game.lastChallengeId != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                    PreviousChallengeContent(game, myUserId, friend.userId, onViewScore)
                }
            }
            else -> {
                // null = previous challenge
                val statusText = when (lastResult?.outcome) {
                    "win" -> "You won the last challenge"
                    "loss" -> "You lost the last challenge"
                    "draw" -> "Last challenge was a draw"
                    else -> if (game.lastChallengeId != null) "Last challenge" else null
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (statusText != null) {
                            Text(
                                statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (lastResult != null) {
                            Text(
                                "${lastResult.mySummary} vs ${lastResult.theirSummary}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Text(
                            "${game.recordWins}W  ${game.recordDraws}D  ${game.recordLosses}L",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (game.lastChallengeId != null) {
                            OutlinedButton(onClick = { onViewScore(game.lastChallengeId, myUserId) }) { Text("YOUR RESULT") }
                            OutlinedButton(onClick = { onViewScore(game.lastChallengeId, friend.userId) }) { Text("THEIR RESULT") }
                        }
                    }
                }
            }
        }

        // REMATCH spans the full width at the bottom
        if (game.status == null) {
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                colors = BlackButtonColors,
            ) { Text("REMATCH") }
        }
            } // end when (closes inner Column)
        } // end AnimatedVisibility (closes outer Column)
    }
}

@Composable
private fun ResultQueueScreen(
    friendName: String,
    game: ChallengeSummaryGameDto,
    myUserId: String,
    friendUserId: String,
    onViewMyResult: () -> Unit,
    onViewTheirResult: () -> Unit,
    onContinue: () -> Unit,
) {
    val lastResult = game.lastResult
    val outcomeText = when (lastResult?.outcome) {
        "win" -> "You won against $friendName"
        "loss" -> "You lost to $friendName"
        "draw" -> "Draw with $friendName"
        else -> "Challenge complete vs $friendName"
    }
    Scaffold(
        topBar = { SectionTopBar(title = gameName(game.game), onBack = onContinue) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                text = outcomeText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (lastResult != null) {
                Text(
                    text = "${lastResult.mySummary}  vs  ${lastResult.theirSummary}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.weight(1f))
            if (game.lastChallengeId != null) {
                Button(
                    onClick = onViewMyResult,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                ) { Text("VIEW YOUR RESULT") }
                OutlinedButton(
                    onClick = onViewTheirResult,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                ) { Text("VIEW THEIR RESULT") }
            }
            OutlinedButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            ) { Text("CONTINUE") }
        }
    }
}

@Composable
private fun FriendPickerDialog(
    friends: List<FriendSummaryDto>,
    onDismiss: () -> Unit,
    onSelectFriend: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Challenge a friend", style = MaterialTheme.typography.titleLarge)
                if (friends.isEmpty()) {
                    Text(
                        "No friends yet. Add some from your profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        friends.forEachIndexed { index, friend ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AvatarIcon(
                                    friend.avatarId, friend.avatarColorId,
                                    avatarIconColor = friend.avatarIconColor, size = 36.dp,
                                )
                                Text(
                                    friend.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Button(
                                    onClick = { onSelectFriend(friend.userId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onSurface,
                                        contentColor = MaterialTheme.colorScheme.surface,
                                    ),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                ) { Text("CHALLENGE") }
                            }
                        }
                    }
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                    onClick = onDismiss,
                ) { Text("CANCEL") }
            }
        }
    }
}
