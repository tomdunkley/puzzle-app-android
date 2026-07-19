package com.tomdunkley.dailypuzzles.ui.screens.boggle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.LocalFireDepartment
import com.tomdunkley.dailypuzzles.ui.components.BestScorePill
import com.tomdunkley.dailypuzzles.ui.components.FeedbackPill
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.TrophyUnlockedBanner
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor
import com.tomdunkley.dailypuzzles.ui.components.displayLetter
import com.tomdunkley.dailypuzzles.ui.share.buildBoggleShareText
import com.tomdunkley.dailypuzzles.ui.share.shareText
import kotlinx.coroutines.launch

private val CORRECT_GREEN = Color(0xFF2E7D32)

@Composable
fun BoggleScreen(
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onViewDetail: (puzzleId: String, userId: String) -> Unit,
    onSignInClick: () -> Unit,
    onShowBottomBarChange: (Boolean) -> Unit,
    viewModel: BoggleViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val newlyUnlockedTrophies by viewModel.newlyUnlockedTrophies.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadTodayPuzzle() }
    LaunchedEffect(Unit) { onShowBottomBarChange(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.persistProgress()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            SectionTopBar(
                title = "Words",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = WordsSolidColor.copy(alpha = 0.15f),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is BoggleUiState.Loading -> CenteredMessage { CircularProgressIndicator() }
                is BoggleUiState.Error -> CenteredMessage {
                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                }
                is BoggleUiState.Playing -> PlayingContent(state, viewModel)
                is BoggleUiState.Submitting -> CenteredMessage {
                    CircularProgressIndicator()
                    Text("Submitting your score...", style = MaterialTheme.typography.bodyMedium)
                }
                is BoggleUiState.Results -> ResultsContent(
                    state = state,
                    isSignedIn = isSignedIn,
                    onViewDetail = {
                        coroutineScope.launch {
                            viewModel.ownUserId()?.let { onViewDetail(state.puzzleId, it) }
                        }
                    },
                    onShare = {
                        shareText(
                            context,
                            buildBoggleShareText(
                                date = state.date,
                                score = state.score,
                                wordCount = state.validWords.size,
                                rankToday = if (isSignedIn && state.rankToday > 0) state.rankToday else null,
                            ),
                        )
                    },
                    onSignInClick = onSignInClick,
                )
            }
            TrophyUnlockedBanner(
                trophies = newlyUnlockedTrophies,
                onDismiss = viewModel::dismissTrophyNotification,
            )
        }
    }
}

/** Words that fail validation get a pill explaining why; a correct find is celebrated
 * by the word-pulse animation alone, with no popup needed.
 */
private fun WordFeedbackType.pillMessage(): String? = when (this) {
    WordFeedbackType.CORRECT -> null
    WordFeedbackType.INCORRECT -> "Invalid word"
    WordFeedbackType.DUPLICATE -> "Already guessed"
    WordFeedbackType.TOO_SHORT -> "Not long enough"
}

@Composable
private fun TimerText(secondsRemaining: Int) {
    val isRed = secondsRemaining <= 30
    val isPulsing = secondsRemaining <= 10
    val color = if (isRed) Color.Red else MaterialTheme.colorScheme.onSurface

    val scale = remember { Animatable(1f) }
    LaunchedEffect(secondsRemaining) {
        if (isPulsing && secondsRemaining > 0) {
            scale.animateTo(1.25f, tween(80))
            scale.animateTo(1f, tween(320))
        }
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    Text(
        text = "%d:%02d".format(minutes, seconds),
        style = MaterialTheme.typography.titleLarge,
        color = color,
        modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    )
}

@Composable
private fun CurrentWordText(state: BoggleUiState.Playing) {
    val feedback = state.wordFeedback
    val displayWord = feedback?.word ?: state.currentWord
    val borderColor = when (feedback?.type) {
        WordFeedbackType.CORRECT -> CORRECT_GREEN
        WordFeedbackType.INCORRECT, WordFeedbackType.TOO_SHORT -> MaterialTheme.colorScheme.error
        WordFeedbackType.DUPLICATE -> MaterialTheme.colorScheme.onSurfaceVariant
        null -> MaterialTheme.colorScheme.onSurface
    }
    val scale = remember { Animatable(1f) }
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(feedback) {
        when (feedback?.type) {
            WordFeedbackType.CORRECT -> {
                scale.snapTo(1f)
                scale.animateTo(1.15f, tween(120))
                scale.animateTo(1f, tween(180))
            }
            WordFeedbackType.INCORRECT, WordFeedbackType.DUPLICATE, WordFeedbackType.TOO_SHORT -> {
                shakeOffset.snapTo(0f)
                listOf(-10f, 10f, -8f, 8f, -4f, 4f, 0f).forEach { shakeOffset.animateTo(it, tween(35)) }
            }
            null -> {}
        }
    }
    Text(
        text = displayWord.ifEmpty { " " },
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = shakeOffset.value
            }
            .border(width = if (feedback != null) 2.dp else 1.dp, color = borderColor)
            .padding(6.dp),
    )
}

@Composable
private fun CenteredMessage(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        content = content,
    )
}

private val HEADER_HEIGHT = 48.dp
private val CURRENT_WORD_HEIGHT = 50.dp
private val MIN_WORDS_ROW_HEIGHT = 40.dp
private val PILL_SLOT_HEIGHT = 48.dp
private val PLAYING_GAPS = 12.dp * 4

@Composable
private fun PlayingContent(state: BoggleUiState.Playing, viewModel: BoggleViewModel) {
    if (state.isPaused) {
        PausedContent(state, viewModel)
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val reserved = HEADER_HEIGHT + CURRENT_WORD_HEIGHT + MIN_WORDS_ROW_HEIGHT + PILL_SLOT_HEIGHT + PLAYING_GAPS
        val boardSize = minOf(maxWidth, maxHeight - reserved).coerceAtLeast(160.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimerText(state.secondsRemaining)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Score: ${state.liveScore}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "${state.foundWords.size} words",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::persistProgress) {
                        Icon(Icons.Filled.Pause, contentDescription = "Pause")
                    }
                }
            }

            CurrentWordText(state)

            BoggleGrid(
                board = state.board,
                selectedPath = state.selectedPath,
                viewModel = viewModel,
                modifier = Modifier.size(boardSize).align(Alignment.CenterHorizontally),
            )

            FlowRow(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.foundWords.forEach { word ->
                    Text(
                        text = "$word (${scoreForWord(word)})",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            // Fixed-height slot for the feedback pill -- it fades in quickly and out slowly
            // so a wrong-word rejection lingers visibly rather than vanishing instantly.
            val pillMessage = state.wordFeedback?.type?.pillMessage()
            // Keep the last non-null message so it stays visible during the fade-out.
            var displayedPillMessage by remember { mutableStateOf("") }
            if (pillMessage != null) displayedPillMessage = pillMessage
            val pillAlpha by animateFloatAsState(
                targetValue = if (pillMessage != null) 1f else 0f,
                animationSpec = tween(if (pillMessage != null) 80 else 300),
                label = "pillAlpha",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PILL_SLOT_HEIGHT)
                    .graphicsLayer(alpha = pillAlpha),
                contentAlignment = Alignment.Center,
            ) {
                FeedbackPill(message = displayedPillMessage)
            }
        }
    }
}

@Composable
private fun PausedContent(state: BoggleUiState.Playing, viewModel: BoggleViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("PAUSED", style = MaterialTheme.typography.headlineMedium)
        Text("Score so far: ${state.liveScore}", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${state.foundWords.size} words found",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
            onClick = { viewModel.resume() },
        ) {
            Text("RESUME")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            onClick = { viewModel.finishEarly() },
        ) {
            Text("FINISH")
        }
    }
}

@Composable
private fun BoggleGrid(
    board: List<String>,
    selectedPath: List<Int>,
    viewModel: BoggleViewModel,
    modifier: Modifier = Modifier.fillMaxWidth().aspectRatio(1f),
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val cellSizePx = with(density) { maxWidth.toPx() } / BOARD_SIZE
        val deadZoneRadiusPx = cellSizePx * 0.4f

        fun rawIndexAt(offset: Offset): Int {
            val col = (offset.x / cellSizePx).toInt().coerceIn(0, BOARD_SIZE - 1)
            val row = (offset.y / cellSizePx).toInt().coerceIn(0, BOARD_SIZE - 1)
            return row * BOARD_SIZE + col
        }

        fun cellCenter(index: Int): Offset {
            val col = index % BOARD_SIZE
            val row = index / BOARD_SIZE
            return Offset((col + 0.5f) * cellSizePx, (row + 0.5f) * cellSizePx)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Custom pointer input: fire onCellDragged on the very first touch-down so
                // holding a single tile highlights it immediately (detectDragGestures only
                // fires after a slop distance, leaving a single stationary touch invisible).
                .pointerInput(Unit) {
                    var lastIndex: Int? = null
                    while (true) {
                        awaitPointerEventScope {
                            // Wait for the first finger to land.
                            val down = awaitPointerEvent()
                            val position = down.changes.firstOrNull()?.position ?: return@awaitPointerEventScope
                            val initialIndex = rawIndexAt(position)
                            lastIndex = initialIndex
                            viewModel.onCellDragged(initialIndex)

                            // Track subsequent moves until all pointers are lifted.
                            while (true) {
                                val event = awaitPointerEvent()
                                val anyPressed = event.changes.any { it.pressed }
                                val pos = event.changes.firstOrNull()?.position
                                if (pos != null) {
                                    val raw = rawIndexAt(pos)
                                    val current = lastIndex
                                    val target = if (current == null || raw == current) {
                                        raw
                                    } else {
                                        val center = cellCenter(raw)
                                        val dx = pos.x - center.x
                                        val dy = pos.y - center.y
                                        if (dx * dx + dy * dy <= deadZoneRadiusPx * deadZoneRadiusPx) raw else current
                                    }
                                    lastIndex = target
                                    viewModel.onCellDragged(target)
                                }
                                if (!anyPressed) {
                                    viewModel.onDragEnded()
                                    lastIndex = null
                                    break
                                }
                            }
                        }
                    }
                },
        ) {
            for (row in 0 until BOARD_SIZE) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until BOARD_SIZE) {
                        val index = row * BOARD_SIZE + col
                        BoggleTile(
                            letter = board[index],
                            selected = index in selectedPath,
                            modifier = Modifier.weight(1f).fillMaxHeight().padding(5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoggleTile(letter: String, selected: Boolean, modifier: Modifier = Modifier) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier.background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayLetter(letter),
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontSize = 32.sp),
            fontWeight = FontWeight.Bold,
            color = textColor,
        )
    }
}

@Composable
private fun ResultsContent(
    state: BoggleUiState.Results,
    isSignedIn: Boolean,
    onViewDetail: () -> Unit,
    onShare: () -> Unit,
    onSignInClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Score: ${state.score}", style = MaterialTheme.typography.headlineMedium)
        if (state.validWords.isNotEmpty()) {
            Text(
                "${state.validWords.size} words found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isSignedIn) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BestScorePill(
                    text = "${state.currentStreak} day streak",
                    iconTint = WordsSolidColor,
                    icon = Icons.Filled.LocalFireDepartment,
                )
                if (state.isNewDailyBest) {
                    BestScorePill(text = "New best!", iconTint = WordsSolidColor)
                } else if (state.dailyBestScore != null) {
                    val bestText = if (state.dailyBestWordCount != null) {
                        "${state.dailyBestScore} pts (words: ${state.dailyBestWordCount})"
                    } else {
                        "${state.dailyBestScore} pts"
                    }
                    BestScorePill(text = bestText, iconTint = WordsSolidColor)
                }
            }
            if (state.rankToday > 0) {
                Text("🌍 Global rank today: #${state.rankToday}", style = MaterialTheme.typography.titleMedium)
            }
        }
        if (state.validWords.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.validWords.sortedWith(compareByDescending<String> { it.length }.thenBy { it }).forEach { word ->
                    Text(
                        text = "$word (${scoreForWord(word)})",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
            onClick = onShare,
        ) {
            Text("SHARE")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            onClick = onViewDetail,
        ) {
            Text("VIEW BOARD")
        }
        if (!isSignedIn) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                onClick = onSignInClick,
            ) {
                Text("SIGN IN TO SAVE")
            }
        }
    }
}
