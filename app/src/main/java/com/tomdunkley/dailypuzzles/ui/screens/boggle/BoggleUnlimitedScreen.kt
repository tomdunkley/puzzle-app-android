package com.tomdunkley.dailypuzzles.ui.screens.boggle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.ui.components.BestScorePill
import com.tomdunkley.dailypuzzles.ui.components.BoggleBoardView
import com.tomdunkley.dailypuzzles.ui.components.FeedbackPill
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor
import com.tomdunkley.dailypuzzles.ui.components.displayLetter

@Composable
fun BoggleUnlimitedScreen(
    onBack: () -> Unit,
    onShowBottomBarChange: (Boolean) -> Unit,
    viewModel: BoggleUnlimitedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val seed by viewModel.seed.collectAsState()
    val showIntro by viewModel.showIntro.collectAsState()
    val boardForResults by viewModel.boardForResults.collectAsState()
    val isNewBestScore by viewModel.isNewBestScore.collectAsState()
    val allWordsState by viewModel.allWordsState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var showBoardFullScreen by remember { mutableStateOf(false) }
    var showHighScoreDetail by remember { mutableStateOf(false) }

    LaunchedEffect(showIntro) {
        showBoardFullScreen = false
        showHighScoreDetail = false
    }

    LaunchedEffect(Unit) { viewModel.loadPuzzle() }
    LaunchedEffect(Unit) { onShowBottomBarChange(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.persistProgress()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val subtitle = when {
        showHighScoreDetail -> viewModel.practiceHighScoreSeed.takeIf { it.isNotEmpty() }
        !showIntro && (uiState is BoggleUiState.Playing || uiState is BoggleUiState.Results || showBoardFullScreen) && seed.isNotEmpty() -> seed
        else -> null
    }

    Scaffold(
        topBar = {
            SectionTopBar(
                title = "Words: Unlimited",
                subtitle = subtitle,
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            showBoardFullScreen -> showBoardFullScreen = false
                            showHighScoreDetail -> showHighScoreDetail = false
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = WordsSolidColor.copy(alpha = 0.15f),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                showHighScoreDetail -> BoggleHighScoreDetailContent(
                    score = viewModel.practiceHighScore,
                    board = remember(showHighScoreDetail) { viewModel.highScoreBoard() },
                    words = remember(showHighScoreDetail) { viewModel.highScoreWords() },
                    allWordsState = allWordsState,
                    onLoadAllWords = viewModel::computeAllWords,
                )
                showBoardFullScreen -> {
                    val resultsState = uiState as? BoggleUiState.Results
                    UnlimitedBoardScreen(
                        board = boardForResults,
                        score = resultsState?.score ?: 0,
                        validWords = resultsState?.validWords ?: emptyList(),
                        allWordsState = allWordsState,
                        onLoadAllWords = viewModel::computeAllWords,
                    )
                }
                showIntro -> UnlimitedIntroContent(
                    seed = seed,
                    highScore = viewModel.practiceHighScore,
                    onStart = { viewModel.startGame() },
                    onSetSeed = { viewModel.setSeed(it) },
                    onViewHighScore = if (viewModel.practiceHighScoreSeed.isNotEmpty()) {
                        { showHighScoreDetail = true }
                    } else null,
                )
                else -> when (val state = uiState) {
                    is BoggleUiState.Loading -> UnlimitedCenteredMessage { CircularProgressIndicator() }
                    is BoggleUiState.Error -> UnlimitedCenteredMessage {
                        Text(state.message, style = MaterialTheme.typography.bodyLarge)
                    }
                    is BoggleUiState.Playing -> UnlimitedPlayingContent(state, viewModel)
                    is BoggleUiState.Submitting -> UnlimitedCenteredMessage { CircularProgressIndicator() }
                    is BoggleUiState.Results -> UnlimitedResultsContent(
                        state = state,
                        highScore = viewModel.practiceHighScore,
                        highScoreWordCount = viewModel.practiceHighScoreWordCount,
                        isNewBest = isNewBestScore,
                        onNewGame = { viewModel.newGame() },
                        onViewBoard = { showBoardFullScreen = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoggleHighScoreDetailContent(
    score: Int,
    board: List<String>,
    words: List<String>,
    allWordsState: BoggleUnlimitedViewModel.AllWordsState,
    onLoadAllWords: (List<String>) -> Unit,
) {
    var showAllWordsDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Score: $score", style = MaterialTheme.typography.titleLarge)
            if (board.isNotEmpty()) BoggleBoardView(board = board)
            if (words.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    words.sortedWith(compareByDescending<String> { it.length }.thenBy { it }).forEach { word ->
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
        }
        if (board.isNotEmpty()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                onClick = { onLoadAllWords(board); showAllWordsDialog = true },
            ) { Text("VIEW ALL POSSIBLE WORDS") }
        }
    }
    if (showAllWordsDialog) {
        AllWordsDialog(state = allWordsState, onClose = { showAllWordsDialog = false })
    }
}

@Composable
private fun UnlimitedBoardScreen(
    board: List<String>,
    score: Int,
    validWords: List<String>,
    allWordsState: BoggleUnlimitedViewModel.AllWordsState,
    onLoadAllWords: (List<String>) -> Unit,
) {
    var showAllWordsDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Score: $score", style = MaterialTheme.typography.titleLarge)
            BoggleBoardView(board = board)
            if (validWords.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    validWords.sortedWith(compareByDescending<String> { it.length }.thenBy { it }).forEach { word ->
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
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            onClick = { onLoadAllWords(board); showAllWordsDialog = true },
        ) { Text("VIEW ALL POSSIBLE WORDS") }
    }
    if (showAllWordsDialog) {
        AllWordsDialog(state = allWordsState, onClose = { showAllWordsDialog = false })
    }
}

@Composable
private fun UnlimitedIntroContent(
    seed: String,
    highScore: Int,
    onStart: () -> Unit,
    onSetSeed: (String) -> Unit,
    onViewHighScore: (() -> Unit)?,
) {
    var showSeedDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Seed: ",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = seed,
                style = MaterialTheme.typography.headlineMedium,
            )
            IconButton(onClick = { showSeedDialog = true }) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit seed",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
            onClick = onStart,
        ) {
            Text("START")
        }
        if (highScore >= 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(if (onViewHighScore != null) Modifier.clickable(onClick = onViewHighScore) else Modifier)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = WordsSolidColor,
                    )
                    Text(
                        "$highScore pts",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    if (showSeedDialog) {
        SeedInputDialog(
            currentSeed = seed,
            onConfirm = { newSeed ->
                onSetSeed(newSeed)
                showSeedDialog = false
            },
            onDismiss = { showSeedDialog = false },
        )
    }
}

@Composable
private fun SeedInputDialog(currentSeed: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(currentSeed) }
    val isValid = text.length == 5 && text.all { it.isLetterOrDigit() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Enter seed", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = text,
                    onValueChange = { new ->
                        text = new.uppercase().filter { c -> c.isLetterOrDigit() }.take(5)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters,
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                        onClick = onDismiss,
                    ) {
                        Text("CANCEL")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                        ),
                        onClick = { onConfirm(text) },
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlimitedCenteredMessage(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
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
private fun UnlimitedPlayingContent(state: BoggleUiState.Playing, viewModel: BoggleUnlimitedViewModel) {
    if (state.isPaused) {
        UnlimitedPausedContent(state, viewModel)
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val reserved = HEADER_HEIGHT + CURRENT_WORD_HEIGHT + MIN_WORDS_ROW_HEIGHT + PILL_SLOT_HEIGHT + PLAYING_GAPS + 32.dp
        val boardSize = minOf(maxWidth - 32.dp, maxHeight - reserved).coerceAtLeast(160.dp)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UnlimitedTimerText(state.secondsRemaining)
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

            UnlimitedCurrentWordText(state)

            UnlimitedBoggleGrid(
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

            val pillMessage = state.wordFeedback?.type?.let { type ->
                when (type) {
                    WordFeedbackType.CORRECT -> null
                    WordFeedbackType.INCORRECT -> "Invalid word"
                    WordFeedbackType.DUPLICATE -> "Already guessed"
                    WordFeedbackType.TOO_SHORT -> "Not long enough"
                }
            }
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
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun UnlimitedPausedContent(state: BoggleUiState.Playing, viewModel: BoggleUnlimitedViewModel) {
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
private fun UnlimitedTimerText(secondsRemaining: Int) {
    val isRed = secondsRemaining <= 30
    val isPulsing = secondsRemaining <= 10
    val color = if (isRed) androidx.compose.ui.graphics.Color.Red else MaterialTheme.colorScheme.onSurface
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
private fun UnlimitedCurrentWordText(state: BoggleUiState.Playing) {
    val feedback = state.wordFeedback
    val displayWord = feedback?.word ?: state.currentWord
    val borderColor = when (feedback?.type) {
        WordFeedbackType.CORRECT -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
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
private fun UnlimitedBoggleGrid(
    board: List<String>,
    selectedPath: List<Int>,
    viewModel: BoggleUnlimitedViewModel,
    modifier: Modifier = Modifier,
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
                .pointerInput(Unit) {
                    var lastIndex: Int? = null
                    while (true) {
                        awaitPointerEventScope {
                            val down = awaitPointerEvent()
                            val position = down.changes.firstOrNull()?.position ?: return@awaitPointerEventScope
                            val initialIndex = rawIndexAt(position)
                            lastIndex = initialIndex
                            viewModel.onCellDragged(initialIndex)

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
                        UnlimitedBoggleTile(
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
private fun UnlimitedBoggleTile(letter: String, selected: Boolean, modifier: Modifier = Modifier) {
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
private fun UnlimitedResultsContent(
    state: BoggleUiState.Results,
    highScore: Int,
    highScoreWordCount: Int,
    isNewBest: Boolean,
    onNewGame: () -> Unit,
    onViewBoard: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        if (highScore >= 0) {
            BestScorePill(
                text = if (isNewBest) "New best!" else "$highScore pts (words: $highScoreWordCount)",
                iconTint = WordsSolidColor,
            )
        }
        Text("Score: ${state.score}", style = MaterialTheme.typography.headlineMedium)
        if (state.validWords.isNotEmpty()) {
            Text(
                "${state.validWords.size} words found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            onClick = onNewGame,
        ) {
            Text("NEW GAME")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            onClick = onViewBoard,
        ) {
            Text("VIEW BOARD")
        }
    }
}

@Composable
private fun AllWordsDialog(
    state: BoggleUnlimitedViewModel.AllWordsState,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("All possible words", style = MaterialTheme.typography.titleLarge)
                when (state) {
                    BoggleUnlimitedViewModel.AllWordsState.Idle,
                    BoggleUnlimitedViewModel.AllWordsState.Computing -> Box(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                    is BoggleUnlimitedViewModel.AllWordsState.Done -> FlowRow(
                        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.words.forEach { word ->
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
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                    onClick = onClose,
                ) { Text("CLOSE") }
            }
        }
    }
}
