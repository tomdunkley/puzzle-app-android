package com.tomdunkley.dailypuzzles.ui.screens.numbers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.data.network.dto.NumbersStepDto
import com.tomdunkley.dailypuzzles.data.numbers.NumbersTile
import com.tomdunkley.dailypuzzles.ui.components.FeedbackPill
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.TrophyUnlockedBanner
import com.tomdunkley.dailypuzzles.ui.components.numbersOpSymbol
import com.tomdunkley.dailypuzzles.ui.share.buildNumbersShareText
import com.tomdunkley.dailypuzzles.ui.share.shareText

private val CORRECT_GREEN = Color(0xFF2E7D32)

@Composable
fun NumbersScreen(
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onSignInClick: () -> Unit,
    onShowBottomBarChange: (Boolean) -> Unit,
    viewModel: NumbersViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val newlyUnlockedTrophies by viewModel.newlyUnlockedTrophies.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadTodayPuzzle() }
    LaunchedEffect(uiState) { onShowBottomBarChange(uiState is NumbersUiState.Results) }

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
                title = "Numbers",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                backgroundColor = NumbersSolidColor.copy(alpha = 0.15f),
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is NumbersUiState.Loading -> CenteredMessage { CircularProgressIndicator() }
                is NumbersUiState.Error -> CenteredMessage {
                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                }
                is NumbersUiState.Playing -> PlayingContent(state, viewModel)
                is NumbersUiState.Submitting -> CenteredMessage {
                    CircularProgressIndicator()
                    Text("Submitting your score...", style = MaterialTheme.typography.bodyMedium)
                }
                is NumbersUiState.Results -> ResultsContent(
                    state = state,
                    isSignedIn = isSignedIn,
                    onShare = {
                        shareText(
                            context,
                            buildNumbersShareText(
                                date = state.date,
                                target = state.target,
                                resultValue = state.resultValue,
                                distance = state.distance,
                                durationSeconds = state.durationSeconds,
                                rankToday = if (isSignedIn && state.rankToday > 0) state.rankToday else null,
                            ),
                        )
                    },
                    onSignInClick = onSignInClick,
                )
            }

            val errorMessage = (uiState as? NumbersUiState.Playing)?.errorMessage
            if (errorMessage != null) {
                FeedbackPill(
                    message = errorMessage,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                )
            }
            TrophyUnlockedBanner(
                trophies = newlyUnlockedTrophies,
                onDismiss = viewModel::dismissTrophyNotification,
            )
        }
    }
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
private fun PlayingContent(state: NumbersUiState.Playing, viewModel: NumbersViewModel) {
    if (state.isPaused) {
        PausedContent(state, viewModel)
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerText(state.secondsRemaining)
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

        // Calculator body: numbers left, operators right, all inside a bordered panel.
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
                                NumberTile(
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
                                OperatorButton(
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
private fun PausedContent(state: NumbersUiState.Playing, viewModel: NumbersViewModel) {
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
private fun NumberTile(
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
        // Main selectable area — fills the square
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
        // Undo circle — overlaid in top-right corner, touch area matches visible size
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
private fun OperatorButton(
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
private fun SolutionDialog(target: Int, solution: List<NumbersStepDto>, onClose: () -> Unit) {
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
                    Text(
                        "$target was one of the starting numbers.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
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

@Composable
private fun ResultsContent(
    state: NumbersUiState.Results,
    isSignedIn: Boolean,
    onShare: () -> Unit,
    onSignInClick: () -> Unit,
) {
    var showSolution by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("Target: ${state.target}", style = MaterialTheme.typography.titleLarge)
        Text(
            state.numbers.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        if (isSignedIn) {
            if (state.rankToday > 0) {
                Text("Global rank today: #${state.rankToday}", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "${state.currentStreak} day streak",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            onClick = onShare,
        ) {
            Text("SHARE")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            onClick = { showSolution = true },
        ) {
            Text("VIEW SOLUTION")
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

    if (showSolution) {
        SolutionDialog(target = state.target, solution = state.solution, onClose = { showSolution = false })
    }
}
