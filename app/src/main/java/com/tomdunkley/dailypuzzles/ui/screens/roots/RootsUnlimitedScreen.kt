package com.tomdunkley.dailypuzzles.ui.screens.roots

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.ui.components.BestScorePill
import com.tomdunkley.dailypuzzles.ui.components.RootsAccentColor
import com.tomdunkley.dailypuzzles.ui.components.RootsSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar

@Composable
fun RootsUnlimitedScreen(
    onBack: () -> Unit,
    onShowBottomBarChange: (Boolean) -> Unit,
    viewModel: RootsUnlimitedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.persistProgress()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(Unit) { viewModel.loadPuzzle() }

    val showBottomBar = uiState is RootsUnlimitedUiState.Results
    LaunchedEffect(showBottomBar) { onShowBottomBarChange(showBottomBar) }

    when (val state = uiState) {
        is RootsUnlimitedUiState.Start -> StartContent(
            state = state,
            onSeedChange = { viewModel.setSeed(it) },
            onStart = { viewModel.startGame() },
            onBack = onBack,
        )
        is RootsUnlimitedUiState.Playing -> UnlimitedPlayingContent(
            state = state,
            canUndo = canUndo,
            onPause = { viewModel.persistProgress() },
            onResume = { viewModel.resume() },
            onUndo = { viewModel.undo() },
            onClearPath = { viewModel.clearPath() },
            onDragStart = { viewModel.onDragStart(it) },
            onCellDrag = { viewModel.onCellDrag(it) },
            onTapCell = { viewModel.onTapCell(it) },
            onBack = onBack,
        )
        is RootsUnlimitedUiState.Results -> UnlimitedResultsContent(
            state = state,
            onNewGame = { viewModel.newGame() },
            onBack = onBack,
        )
    }
}

@Composable
private fun StartContent(
    state: RootsUnlimitedUiState.Start,
    onSeedChange: (String) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember(state.seed) { mutableStateOf(state.seed) }
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        topBar = {
            SectionTopBar(
                title = "Routes: Unlimited",
                onBack = onBack,
                backgroundColor = RootsAccentColor,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                if (isEditing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = editValue,
                            onValueChange = { editValue = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8) },
                            label = { Text("Seed") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                onSeedChange(editValue)
                                isEditing = false
                            }),
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                        )
                        IconButton(onClick = {
                            onSeedChange(editValue)
                            isEditing = false
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = "Confirm seed")
                        }
                    }
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Seed: ",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(state.seed, style = MaterialTheme.typography.headlineMedium)
                        IconButton(onClick = { editValue = state.seed; isEditing = true }) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "Edit seed",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("START")
                }

                if (state.bestTimeSeconds >= 0) {
                    BestScorePill(
                        text = "Best: ${formatTime(state.bestTimeSeconds)}",
                        iconTint = RootsSolidColor,
                        icon = Icons.Filled.EmojiEvents,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnlimitedPlayingContent(
    state: RootsUnlimitedUiState.Playing,
    canUndo: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onUndo: () -> Unit,
    onClearPath: () -> Unit,
    onDragStart: (com.tomdunkley.dailypuzzles.data.roots.RootsCell) -> Unit,
    onCellDrag: (com.tomdunkley.dailypuzzles.data.roots.RootsCell) -> Unit,
    onTapCell: (com.tomdunkley.dailypuzzles.data.roots.RootsCell) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            SectionTopBar(
                title = "Routes: Unlimited",
                subtitle = formatTime(state.elapsedSeconds),
                onBack = onBack,
                backgroundColor = RootsAccentColor,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                RootsGridCanvas(
                    n = state.gridSize,
                    startCell = state.startCell,
                    endCell = state.endCell,
                    rowClues = state.rowClues,
                    colClues = state.colClues,
                    path = state.currentPath,
                    crossMarkers = state.crossMarkers,
                    tickMarkers = state.tickMarkers,
                    interactive = !state.isPaused,
                    onDragStart = onDragStart,
                    onCellDrag = onCellDrag,
                    onTapCell = onTapCell,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onUndo,
                        enabled = canUndo,
                        border = BorderStroke(1.dp, if (canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f),
                    ) { Text("UNDO") }
                    OutlinedButton(
                        onClick = onClearPath,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f),
                    ) { Text("CLEAR") }
                }
                Spacer(Modifier.weight(1f))
            }

            if (state.isPaused) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text("Paused", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Time: ${formatTime(state.elapsedSeconds)}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(
                                onClick = onResume,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RootsSolidColor,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Text("RESUME", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlimitedResultsContent(
    state: RootsUnlimitedUiState.Results,
    onNewGame: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            SectionTopBar(
                title = "Routes: Unlimited",
                onBack = onBack,
                backgroundColor = RootsAccentColor,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        ) {
            if (state.isNewBest) {
                BestScorePill(
                    text = "New best!",
                    iconTint = RootsSolidColor,
                    icon = Icons.Filled.EmojiEvents,
                )
            }
            Text(
                "Solved in ${formatTime(state.durationSeconds)}!",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                "Puzzle: ${state.seed}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            RootsGridCanvas(
                n = state.gridSize,
                startCell = state.startCell,
                endCell = state.endCell,
                rowClues = state.rowClues,
                colClues = state.colClues,
                path = state.solution,
                crossMarkers = emptySet(),
                tickMarkers = emptySet(),
                interactive = false,
                onDragStart = {},
                onCellDrag = {},
                onTapCell = {},
                modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f),
            )
            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("NEW GAME") }
        }
    }
}
