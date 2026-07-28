package com.tomdunkley.dailypuzzles.ui.screens.lines

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.data.lines.LinesCell
import com.tomdunkley.dailypuzzles.ui.components.BestScorePill
import com.tomdunkley.dailypuzzles.ui.components.LinesAccentColor
import com.tomdunkley.dailypuzzles.ui.components.LinesSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.SignInPrompt
import com.tomdunkley.dailypuzzles.ui.components.TrophyUnlockedBanner

@Composable
fun LinesScreen(
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onSignInClick: () -> Unit,
    onShowBottomBarChange: (Boolean) -> Unit,
    viewModel: LinesViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val newlyUnlockedTrophies by viewModel.newlyUnlockedTrophies.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.persistProgress()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(Unit) { viewModel.loadTodayPuzzle() }

    val showBottomBar = uiState is LinesUiState.Results
    LaunchedEffect(showBottomBar) { onShowBottomBarChange(showBottomBar) }

    Box(Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is LinesUiState.Loading -> LinesLoadingContent()
            is LinesUiState.Error -> LinesErrorContent(state.message, onBack)
            is LinesUiState.Playing -> PlayingContent(
                state = state,
                onPause = { viewModel.persistProgress() },
                onResume = { viewModel.resume() },
                onClearPath = { viewModel.clearPath() },
                onDragStart = { viewModel.onDragStart(it) },
                onCellDrag = { viewModel.onCellDrag(it) },
                onTapCell = { viewModel.onTapCell(it) },
            )
            is LinesUiState.Submitting -> LinesLoadingContent()
            is LinesUiState.Results -> ResultsContent(
                state = state,
                isSignedIn = isSignedIn,
                onBack = onBack,
                onSignInClick = onSignInClick,
            )
        }
        TrophyUnlockedBanner(
            trophies = newlyUnlockedTrophies,
            onDismiss = viewModel::dismissTrophyNotification,
        )
    }
}

@Composable
fun LinesLoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = LinesSolidColor)
    }
}

@Composable
fun LinesErrorContent(message: String, onBack: () -> Unit) {
    Scaffold(
        topBar = { SectionTopBar(title = "Lines", onBack = onBack, backgroundColor = LinesAccentColor) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                OutlinedButton(
                    onClick = onBack,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                ) { Text("BACK") }
            }
        }
    }
}

@Composable
private fun PlayingContent(
    state: LinesUiState.Playing,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onClearPath: () -> Unit,
    onDragStart: (LinesCell) -> Unit,
    onCellDrag: (LinesCell) -> Unit,
    onTapCell: (LinesCell) -> Unit,
) {
    Scaffold(
        topBar = {
            SectionTopBar(
                title = "Lines",
                subtitle = formatTime(state.elapsedSeconds),
                onBack = if (!state.isPaused) onPause else null,
                backgroundColor = LinesAccentColor,
                actions = {
                    if (!state.isPaused) {
                        IconButton(onClick = onPause) {
                            Icon(Icons.Filled.Pause, contentDescription = "Pause")
                        }
                    }
                },
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
private fun ResultsContent(
    state: LinesUiState.Results,
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onSignInClick: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            SectionTopBar(title = "Lines", onBack = onBack, backgroundColor = LinesAccentColor)
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
            if (isSignedIn) {
                if (state.rankToday > 0) {
                    Text(
                        "🌍 Global rank today: #${state.rankToday}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.currentStreak > 0) {
                        BestScorePill(
                            text = "${state.currentStreak} day streak",
                            iconTint = LinesSolidColor,
                            icon = Icons.Filled.LocalFireDepartment,
                        )
                    }
                    state.dailyBestDurationSeconds?.let { best ->
                        BestScorePill(
                            text = "Best today: ${formatTime(best)}",
                            iconTint = LinesSolidColor,
                            icon = Icons.Filled.EmojiEvents,
                        )
                    }
                }
                if (state.isNewDailyBest) {
                    Text(
                        "New personal best! 🎉",
                        style = MaterialTheme.typography.titleSmall,
                        color = LinesSolidColor,
                    )
                }
            }

            Text(
                "Solved in ${formatTime(state.durationSeconds)}!",
                style = MaterialTheme.typography.headlineMedium,
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

            if (!isSignedIn) {
                SignInPrompt(
                    message = "Sign in to compare your time with friends.",
                    onGoToSettings = onSignInClick,
                )
            }

            Button(
                onClick = {
                    val text = buildLinesShareText(
                        date = state.date,
                        durationSeconds = state.durationSeconds,
                        rankToday = if (isSignedIn && state.rankToday > 0) state.rankToday else null,
                        solution = state.solution,
                        gridSize = state.gridSize,
                    )
                    shareText(context, text)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = LinesSolidColor,
                    contentColor = Color.White,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("SHARE") }

            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("DONE") }
        }
    }
}

@Composable
fun LinesGridCanvas(
    n: Int,
    startCell: LinesCell,
    endCell: LinesCell,
    rowClues: List<Int>,
    colClues: List<Int>,
    path: List<LinesCell>,
    crossMarkers: Set<LinesCell>,
    interactive: Boolean,
    onDragStart: (LinesCell) -> Unit,
    onCellDrag: (LinesCell) -> Unit,
    onTapCell: (LinesCell) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val gridLineColor = MaterialTheme.colorScheme.outline
    val clueDefaultColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val pathColor = LinesSolidColor
    val pathBgColor = LinesAccentColor
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier) {
        val totalPx = with(density) { maxWidth.toPx() }
        val cluePx = with(density) { 36.dp.toPx() }
        val cellPx = (totalPx - cluePx) / n

        val gestureModifier = if (interactive) {
            Modifier.pointerInput(n) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startDragCell = posToCell(down.position, cluePx, cellPx, n)
                        ?: return@awaitEachGesture
                    onDragStart(startDragCell)
                    var lastCell = startDragCell
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed }) break
                        val pos = event.changes.first().position
                        val c = posToCell(pos, cluePx, cellPx, n)
                        if (c != null && c != lastCell) {
                            onCellDrag(c)
                            lastCell = c
                            moved = true
                            event.changes.forEach { it.consume() }
                        }
                    }
                    if (!moved) onTapCell(startDragCell)
                }
            }
        } else Modifier

        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize().then(gestureModifier),
        ) {
            val textSizeSp = (cellPx * 0.45f).toSp()

            // Cell backgrounds
            for (r in 0 until n) {
                for (c in 0 until n) {
                    val x = cluePx + c * cellPx
                    val y = cluePx + r * cellPx
                    drawRect(
                        color = surfaceColor,
                        topLeft = Offset(x, y),
                        size = Size(cellPx, cellPx),
                    )
                    if (path.contains(LinesCell(r, c))) {
                        drawRect(
                            color = pathBgColor,
                            topLeft = Offset(x, y),
                            size = Size(cellPx, cellPx),
                        )
                    }
                }
            }

            // Grid border
            drawRect(
                color = gridLineColor,
                topLeft = Offset(cluePx, cluePx),
                size = Size(n * cellPx, n * cellPx),
                style = Stroke(width = 2.dp.toPx()),
            )
            // Inner grid lines
            for (i in 1 until n) {
                drawLine(
                    color = gridLineColor,
                    start = Offset(cluePx + i * cellPx, cluePx),
                    end = Offset(cluePx + i * cellPx, cluePx + n * cellPx),
                    strokeWidth = 0.5.dp.toPx(),
                )
                drawLine(
                    color = gridLineColor,
                    start = Offset(cluePx, cluePx + i * cellPx),
                    end = Offset(cluePx + n * cellPx, cluePx + i * cellPx),
                    strokeWidth = 0.5.dp.toPx(),
                )
            }

            // Path connections
            val lineStroke = cellPx * 0.18f
            for (i in 0 until path.size - 1) {
                val a = path[i]
                val b = path[i + 1]
                drawLine(
                    color = pathColor,
                    start = Offset(cluePx + a.col * cellPx + cellPx / 2, cluePx + a.row * cellPx + cellPx / 2),
                    end = Offset(cluePx + b.col * cellPx + cellPx / 2, cluePx + b.row * cellPx + cellPx / 2),
                    strokeWidth = lineStroke,
                    cap = StrokeCap.Round,
                )
            }

            // Path dots (small circles at intermediate path cells)
            val dotRadius = cellPx * 0.12f
            for (cell in path) {
                if (cell == startCell || cell == endCell) continue
                val cx = cluePx + cell.col * cellPx + cellPx / 2
                val cy = cluePx + cell.row * cellPx + cellPx / 2
                drawCircle(color = pathColor, radius = dotRadius, center = Offset(cx, cy))
            }

            // Endpoint circles (larger, with white center dot)
            val endRadius = cellPx * 0.28f
            for (ep in listOf(startCell, endCell)) {
                val cx = cluePx + ep.col * cellPx + cellPx / 2
                val cy = cluePx + ep.row * cellPx + cellPx / 2
                drawCircle(color = pathColor, radius = endRadius, center = Offset(cx, cy))
                drawCircle(color = Color.White, radius = endRadius * 0.45f, center = Offset(cx, cy))
            }

            // Cross markers (X shape)
            val crossMargin = cellPx * 0.22f
            val crossStroke = 1.5.dp.toPx()
            for (cell in crossMarkers) {
                val x = cluePx + cell.col * cellPx
                val y = cluePx + cell.row * cellPx
                drawLine(
                    color = gridLineColor.copy(alpha = 0.45f),
                    start = Offset(x + crossMargin, y + crossMargin),
                    end = Offset(x + cellPx - crossMargin, y + cellPx - crossMargin),
                    strokeWidth = crossStroke,
                )
                drawLine(
                    color = gridLineColor.copy(alpha = 0.45f),
                    start = Offset(x + cellPx - crossMargin, y + crossMargin),
                    end = Offset(x + crossMargin, y + cellPx - crossMargin),
                    strokeWidth = crossStroke,
                )
            }

            // Clue numbers using Compose text measurement
            val baseStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = textSizeSp)
            for (col in 0 until n) {
                val satisfied = path.count { it.col == col } == colClues[col]
                val measured = textMeasurer.measure(
                    text = colClues[col].toString(),
                    style = baseStyle.copy(color = if (satisfied) pathColor else clueDefaultColor),
                )
                drawText(
                    measured,
                    topLeft = Offset(
                        cluePx + col * cellPx + cellPx / 2 - measured.size.width / 2,
                        cluePx / 2 - measured.size.height / 2,
                    ),
                )
            }
            for (row in 0 until n) {
                val satisfied = path.count { it.row == row } == rowClues[row]
                val measured = textMeasurer.measure(
                    text = rowClues[row].toString(),
                    style = baseStyle.copy(color = if (satisfied) pathColor else clueDefaultColor),
                )
                drawText(
                    measured,
                    topLeft = Offset(
                        cluePx / 2 - measured.size.width / 2,
                        cluePx + row * cellPx + cellPx / 2 - measured.size.height / 2,
                    ),
                )
            }
        }
    }
}

fun posToCell(pos: Offset, cluePx: Float, cellPx: Float, n: Int): LinesCell? {
    val col = ((pos.x - cluePx) / cellPx).toInt()
    val row = ((pos.y - cluePx) / cellPx).toInt()
    if (row !in 0 until n || col !in 0 until n) return null
    return LinesCell(row, col)
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

private fun buildLinesShareText(
    date: String,
    durationSeconds: Int,
    rankToday: Int?,
    solution: List<LinesCell>,
    gridSize: Int,
): String {
    val rankClause = if (rankToday != null) " — Global rank #$rankToday today" else ""
    val grid = buildString {
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                append(if (solution.contains(LinesCell(r, c))) "🟩" else "⬜")
            }
            if (r < gridSize - 1) append("\n")
        }
    }
    return "td Puzzles — Lines — $date\nSolved in ${formatTime(durationSeconds)}$rankClause\n\n$grid"
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
