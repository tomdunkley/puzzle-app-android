package com.tomdunkley.dailypuzzles.ui.screens.numbers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.data.network.dto.NumbersStepDto
import com.tomdunkley.dailypuzzles.data.numbers.NumbersTile
import com.tomdunkley.dailypuzzles.ui.components.BestScorePill
import com.tomdunkley.dailypuzzles.ui.components.FeedbackPill
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.numbersOpSymbol

private val CORRECT_GREEN = Color(0xFF2E7D32)

@Composable
fun NumbersUnlimitedScreen(
    onBack: () -> Unit,
    onShowBottomBarChange: (Boolean) -> Unit,
    viewModel: NumbersUnlimitedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val seed by viewModel.seed.collectAsState()
    val showIntro by viewModel.showIntro.collectAsState()
    val isNewBestScore by viewModel.isNewBestScore.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var showHighScoreDetail by remember { mutableStateOf(false) }

    LaunchedEffect(showIntro) { showHighScoreDetail = false }

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
        showHighScoreDetail -> viewModel.practiceBestSeed.takeIf { it.isNotEmpty() }
        !showIntro && (uiState is NumbersUiState.Playing || uiState is NumbersUiState.Results) && seed.isNotEmpty() -> seed
        else -> null
    }

    Scaffold(
        topBar = {
            SectionTopBar(
                title = "Numbers: Unlimited",
                subtitle = subtitle,
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            showHighScoreDetail -> showHighScoreDetail = false
                            else -> onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = NumbersSolidColor.copy(alpha = 0.15f),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                showHighScoreDetail -> {
                    val detail = remember(showHighScoreDetail) {
                        val s = viewModel.practiceBestSeed
                        if (s.isNotEmpty()) viewModel.detailForSeed(s) else null
                    }
                    NumbersHighScoreDetailContent(
                        detail = detail,
                        bestDistance = viewModel.practiceBestDistance,
                        bestTimeSeconds = viewModel.practiceBestTimeSeconds,
                        bestResultValue = viewModel.practiceBestResultValue,
                        bestSteps = viewModel.practiceBestSteps,
                    )
                }
                showIntro -> UnlimitedNumbersIntroContent(
                    seed = seed,
                    bestDistance = viewModel.practiceBestDistance,
                    bestTimeSeconds = viewModel.practiceBestTimeSeconds,
                    onStart = { viewModel.startGame() },
                    onSetSeed = { viewModel.setSeed(it) },
                    onViewHighScore = if (viewModel.practiceBestSeed.isNotEmpty()) {
                        { showHighScoreDetail = true }
                    } else null,
                )
                else -> {
                    when (val state = uiState) {
                        is NumbersUiState.Loading -> UnlimitedCenteredMessage { CircularProgressIndicator() }
                        is NumbersUiState.Error -> UnlimitedCenteredMessage {
                            Text(state.message, style = MaterialTheme.typography.bodyLarge)
                        }
                        is NumbersUiState.Playing -> UnlimitedPlayingContent(state, viewModel)
                        is NumbersUiState.Submitting -> UnlimitedCenteredMessage { CircularProgressIndicator() }
                        is NumbersUiState.Results -> UnlimitedResultsContent(
                            state = state,
                            bestDistance = viewModel.practiceBestDistance,
                            bestTimeSeconds = viewModel.practiceBestTimeSeconds,
                            isNewBest = isNewBestScore,
                            onNewGame = { viewModel.newGame() },
                        )
                    }

                    val errorMessage = (uiState as? NumbersUiState.Playing)?.errorMessage
                    if (errorMessage != null) {
                        FeedbackPill(
                            message = errorMessage,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumbersHighScoreDetailContent(
    detail: Triple<Int, List<Int>, List<NumbersStepDto>>?,
    bestDistance: Int,
    bestTimeSeconds: Int,
    bestResultValue: Int,
    bestSteps: List<NumbersStepDto>,
) {
    var showSolution by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        if (bestDistance == 0) {
            Text(
                "Got it in ${bestTimeSeconds}s!",
                style = MaterialTheme.typography.headlineMedium,
                color = CORRECT_GREEN,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Text(
                "$bestDistance away",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
        }
        if (detail != null) {
            Text(
                "Target: ${detail.first}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                detail.second.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (bestSteps.isNotEmpty()) {
            Text(
                "How you got there:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                bestSteps.forEach { step ->
                    Text(
                        "${step.a} ${numbersOpSymbol(step.op)} ${step.b} = ${step.result}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        if (bestDistance > 0 && detail != null && detail.third.isNotEmpty()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                onClick = { showSolution = true },
            ) {
                Text("VIEW SOLUTION")
            }
        }
    }

    if (showSolution && detail != null) {
        UnlimitedSolutionDialog(
            target = detail.first,
            solution = detail.third,
            onClose = { showSolution = false },
        )
    }
}

@Composable
private fun UnlimitedNumbersIntroContent(
    seed: String,
    bestDistance: Int,
    bestTimeSeconds: Int,
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
        if (bestDistance >= 0) {
            val highScoreText = when {
                bestDistance == 0 -> "Exact! in ${bestTimeSeconds}s"
                else -> "$bestDistance away"
            }
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
                        tint = NumbersSolidColor,
                    )
                    Text(
                        highScoreText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    if (showSeedDialog) {
        NumbersSeedInputDialog(
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
private fun NumbersSeedInputDialog(currentSeed: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
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

@Composable
private fun UnlimitedTimerText(secondsRemaining: Int) {
    val isRed = secondsRemaining <= 30
    val isPulsing = secondsRemaining <= 10
    val color = if (isRed) Color.Red else MaterialTheme.colorScheme.onSurface
    val scale = androidx.compose.runtime.remember { Animatable(1f) }
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
private fun UnlimitedPlayingContent(state: NumbersUiState.Playing, viewModel: NumbersUnlimitedViewModel) {
    if (state.isPaused) {
        UnlimitedPausedContent(state, viewModel)
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UnlimitedTimerText(state.secondsRemaining)
            IconButton(onClick = viewModel::persistProgress) {
                Icon(Icons.Filled.Pause, contentDescription = "Pause")
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("TARGET", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(state.target.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            if (state.bestDistance == 0) {
                Text(
                    "You've got it: ${state.bestValue}",
                    style = MaterialTheme.typography.titleMedium,
                    color = CORRECT_GREEN,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Text(
                    "Closest so far: ${state.bestValue} (${state.bestDistance} away)",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.tiles.chunked(2).forEach { rowTiles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTiles.forEach { tile ->
                                UnlimitedNumberTile(
                                    tile = tile,
                                    selected = tile.id == state.selectedTileId,
                                    canUndo = tile.id in state.history,
                                    onClick = { viewModel.selectTile(tile.id) },
                                    onUndo = { viewModel.undo(tile.id) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowTiles.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("+" to "-", "*" to "/").forEach { (opA, opB) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(opA, opB).forEach { op ->
                                UnlimitedOperatorButton(
                                    op = op,
                                    selected = op == state.pendingOp,
                                    enabled = state.selectedTileId != null,
                                    onClick = { viewModel.selectOperator(op) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                        onClick = { viewModel.reset() },
                    ) {
                        Text("RESET")
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlimitedPausedContent(state: NumbersUiState.Playing, viewModel: NumbersUnlimitedViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("PAUSED", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = if (state.bestDistance == 0) {
                "You've got it: ${state.bestValue}"
            } else {
                "Closest so far: ${state.bestValue} (${state.bestDistance} away)"
            },
            style = MaterialTheme.typography.titleMedium,
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
private fun UnlimitedNumberTile(
    tile: NumbersTile,
    selected: Boolean,
    canUndo: Boolean,
    onClick: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier.aspectRatio(1f)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tile.value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        }
        if (canUndo) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    .clickable(onClick = onUndo),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun UnlimitedOperatorButton(
    op: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(shape)
            .background(backgroundColor.copy(alpha = if (enabled) 1f else 0.4f), shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = numbersOpSymbol(op),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = textColor.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

@Composable
private fun UnlimitedResultsContent(
    state: NumbersUiState.Results,
    bestDistance: Int,
    bestTimeSeconds: Int,
    isNewBest: Boolean,
    onNewGame: () -> Unit,
) {
    var showSolution by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        if (bestDistance >= 0) {
            val bestText = when {
                isNewBest -> "New best!"
                bestDistance == 0 -> "Exact (${bestTimeSeconds}s)"
                else -> "$bestDistance away"
            }
            BestScorePill(text = bestText, iconTint = NumbersSolidColor)
        }
        if (state.distance == 0) {
            Text(
                "You got it in ${state.durationSeconds}s!",
                style = MaterialTheme.typography.headlineMedium,
                color = CORRECT_GREEN,
                fontWeight = FontWeight.Bold,
            )
        } else {
            Text(
                "You reached ${state.resultValue} (${state.distance} away)",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text("Target: ${state.target}", style = MaterialTheme.typography.titleLarge)
        Text(
            state.numbers.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.steps.isNotEmpty()) {
            Text(
                "How you got there:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.steps.forEach { step ->
                    Text(
                        "${step.a} ${numbersOpSymbol(step.op)} ${step.b} = ${step.result}",
                        style = MaterialTheme.typography.bodyLarge,
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
        if (state.solution.isNotEmpty()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                onClick = { showSolution = true },
            ) {
                Text("VIEW SOLUTION")
            }
        }
    }

    if (showSolution) {
        UnlimitedSolutionDialog(
            target = state.target,
            solution = state.solution,
            onClose = { showSolution = false },
        )
    }
}

@Composable
private fun UnlimitedSolutionDialog(
    target: Int,
    solution: List<NumbersStepDto>,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("ONE WAY TO $target", style = MaterialTheme.typography.titleLarge)
                if (solution.isEmpty()) {
                    Text("$target was one of the starting numbers.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    solution.forEach { step ->
                        Text(
                            "${step.a} ${numbersOpSymbol(step.op)} ${step.b} = ${step.result}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                    onClick = onClose,
                ) {
                    Text("CLOSE")
                }
            }
        }
    }
}
