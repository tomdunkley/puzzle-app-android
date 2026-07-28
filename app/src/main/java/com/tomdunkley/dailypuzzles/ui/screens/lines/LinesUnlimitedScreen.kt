package com.tomdunkley.dailypuzzles.ui.screens.lines

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.ui.components.BestScorePill
import com.tomdunkley.dailypuzzles.ui.components.LinesAccentColor
import com.tomdunkley.dailypuzzles.ui.components.LinesSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar

@Composable
fun LinesUnlimitedScreen(
    onBack: () -> Unit,
    onShowBottomBarChange: (Boolean) -> Unit,
    viewModel: LinesUnlimitedViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val seed by viewModel.seed.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.persistProgress()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(Unit) { viewModel.loadPuzzle() }

    val showBottomBar = uiState is LinesUnlimitedUiState.Results
    LaunchedEffect(showBottomBar) { onShowBottomBarChange(showBottomBar) }

    when (val state = uiState) {
        is LinesUnlimitedUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LinesSolidColor)
        }
        is LinesUnlimitedUiState.Playing -> UnlimitedPlayingContent(
            state = state,
            seed = seed,
            onPause = { viewModel.persistProgress() },
            onResume = { viewModel.resume() },
            onClearPath = { viewModel.clearPath() },
            onDragStart = { viewModel.onDragStart(it) },
            onCellDrag = { viewModel.onCellDrag(it) },
            onTapCell = { viewModel.onTapCell(it) },
            onBack = onBack,
        )
        is LinesUnlimitedUiState.Results -> UnlimitedResultsContent(
            state = state,
            onNewGame = { viewModel.newGame() },
            onBack = onBack,
        )
    }
}

@Composable
private fun UnlimitedPlayingContent(
    state: LinesUnlimitedUiState.Playing,
    seed: String,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onClearPath: () -> Unit,
    onDragStart: (com.tomdunkley.dailypuzzles.data.lines.LinesCell) -> Unit,
    onCellDrag: (com.tomdunkley.dailypuzzles.data.lines.LinesCell) -> Unit,
    onTapCell: (com.tomdunkley.dailypuzzles.data.lines.LinesCell) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            SectionTopBar(
                title = "Lines: Unlimited",
                subtitle = "$seed — ${formatTime(state.elapsedSeconds)}",
                onBack = onBack,
                backgroundColor = LinesAccentColor,
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
                LinesGridCanvas(
                    n = state.gridSize,
                    startCell = state.startCell,
                    endCell = state.endCell,
                    rowClues = state.rowClues,
                    colClues = state.colClues,
                    path = state.currentPath,
                    crossMarkers = state.crossMarkers,
                    interactive = !state.isPaused,
                    onDragStart = onDragStart,
                    onCellDrag = onCellDrag,
                    onTapCell = onTapCell,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onClearPath,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("CLEAR PATH") }
                Spacer(Modifier.height(8.dp))
            }

            if (state.isPaused) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
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
                                    containerColor = LinesSolidColor,
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
    state: LinesUnlimitedUiState.Results,
    onNewGame: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            SectionTopBar(
                title = "Lines: Unlimited",
                onBack = onBack,
                backgroundColor = LinesAccentColor,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Solved in ${formatTime(state.durationSeconds)}!",
                style = MaterialTheme.typography.headlineMedium,
            )
            if (state.isNewBest) {
                BestScorePill(
                    text = "New best for ${state.gridSize}×${state.gridSize}! 🎉",
                    iconTint = LinesSolidColor,
                    icon = Icons.Filled.EmojiEvents,
                )
            }
            Text(
                "Puzzle: ${state.seed}  (${state.gridSize}×${state.gridSize})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            LinesGridCanvas(
                n = state.gridSize,
                startCell = state.startCell,
                endCell = state.endCell,
                rowClues = state.rowClues,
                colClues = state.colClues,
                path = state.solution,
                crossMarkers = emptySet(),
                interactive = false,
                onDragStart = {},
                onCellDrag = {},
                onTapCell = {},
                modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f),
            )
            Button(
                onClick = onNewGame,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LinesSolidColor,
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("NEW PUZZLE") }
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("DONE") }
        }
    }
}
