package com.tomdunkley.dailypuzzles.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.ui.components.RootsAccentColor
import com.tomdunkley.dailypuzzles.ui.components.RootsSolidColor
import com.tomdunkley.dailypuzzles.ui.components.NumbersAccentColor
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.WordsAccentColor
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor

data class Puzzle(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val solidColor: Color,
    val accentColor: Color,
)

val availablePuzzles = listOf(
    Puzzle(
        id = "boggle",
        title = "Words",
        description = "Find as many words as you can before time runs out.",
        icon = Icons.Filled.GridOn,
        solidColor = WordsSolidColor,
        accentColor = WordsAccentColor,
    ),
    Puzzle(
        id = "numbers",
        title = "Numbers",
        description = "Combine six numbers to get as close to the target as you can.",
        icon = Icons.Filled.Calculate,
        solidColor = NumbersSolidColor,
        accentColor = NumbersAccentColor,
    ),
    Puzzle(
        id = "routes",
        title = "Routes",
        description = "Use the number of cells in each row and column to find the path.",
        icon = Icons.Filled.Route,
        solidColor = RootsSolidColor,
        accentColor = RootsAccentColor,
    ),
)

private var hasShownSignInPromptThisLaunch = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isSignedIn: Boolean,
    onPuzzleClick: (String) -> Unit,
    onUnlimitedPuzzleClick: (String) -> Unit,
    onSignInClick: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    var showSignInPrompt by remember {
        val shouldShow = !isSignedIn && !hasShownSignInPromptThisLaunch
        if (shouldShow) hasShownSignInPromptThisLaunch = true
        mutableStateOf(shouldShow)
    }
    val puzzleStatuses by viewModel.puzzleStatuses.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isDeveloper by viewModel.isDeveloper.collectAsState()
    val bogglePracticeHighScore by viewModel.bogglePracticeHighScore.collectAsState()
    val numbersPracticeBestDistance by viewModel.numbersPracticeBestDistance.collectAsState()
    val numbersPracticeBestTimeSeconds by viewModel.numbersPracticeBestTimeSeconds.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = { SectionTopBar(title = "Puzzles") },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh(showSpinner = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                availablePuzzles.forEach { puzzle ->
                    item(key = puzzle.id) {
                        PuzzleCard(
                            puzzle = puzzle,
                            status = puzzleStatuses[puzzle.id] ?: PuzzleStatus.NONE,
                            onClick = { onPuzzleClick(puzzle.id) },
                        )
                    }
                    if (isDeveloper) {
                        item(key = "${puzzle.id}_unlimited") {
                            val practiceHighScore = when (puzzle.id) {
                                "boggle" -> if (bogglePracticeHighScore >= 0) "$bogglePracticeHighScore pts" else ""
                                "numbers" -> when {
                                    numbersPracticeBestDistance < 0 -> ""
                                    numbersPracticeBestDistance == 0 -> "Exact! in ${numbersPracticeBestTimeSeconds}s"
                                    else -> "$numbersPracticeBestDistance away"
                                }
                                else -> ""
                            }
                            PracticeCard(
                                puzzle = puzzle,
                                onClick = { onUnlimitedPuzzleClick(puzzle.id) },
                                highScore = practiceHighScore,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSignInPrompt) {
        SignInPromptDialog(
            onClose = { showSignInPrompt = false },
            onSignInClick = {
                showSignInPrompt = false
                onSignInClick()
            },
        )
    }
}

@Composable
private fun SignInPromptDialog(onClose: () -> Unit, onSignInClick: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "Sign in to play and compare scores with friends.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                        ),
                        onClick = onSignInClick,
                    ) {
                        Text("SIGN IN")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                        onClick = onClose,
                    ) {
                        Text("CONTINUE AS GUEST")
                    }
                }
            }
        }
    }
}

private val PAUSE_YELLOW = Color(0xFFFFCC00)

@Composable
private fun PracticeCard(puzzle: Puzzle, onClick: () -> Unit, highScore: String) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(containerColor = puzzle.solidColor.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AllInclusive,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = puzzle.solidColor,
                )
            }
            Column {
                Text(text = "${puzzle.title}: Unlimited", style = MaterialTheme.typography.headlineSmall)
                if (highScore.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = puzzle.solidColor,
                        )
                        Text(
                            text = highScore,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleCard(puzzle: Puzzle, status: PuzzleStatus, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(containerColor = puzzle.solidColor.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = puzzle.icon,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = puzzle.solidColor,
                )
                when (status) {
                    PuzzleStatus.COMPLETED -> Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .background(color = Color(0xFF2E7D32), shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Completed today",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    PuzzleStatus.IN_PROGRESS -> Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .background(color = PAUSE_YELLOW, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = "Started, not finished",
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    PuzzleStatus.NONE -> Unit
                }
            }
            Column {
                Text(text = puzzle.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = puzzle.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
