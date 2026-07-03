package com.tomdunkley.dailypuzzles.ui.screens.scoredetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.data.network.dto.ScoreDetailDto
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.BoggleBoardView
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.numbersOpSymbol
import com.tomdunkley.dailypuzzles.ui.share.buildBoggleShareText
import com.tomdunkley.dailypuzzles.ui.share.buildNumbersShareText
import com.tomdunkley.dailypuzzles.ui.share.shareText

@Composable
fun ScoreDetailScreen(
    puzzleId: String,
    userId: String,
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onSignInClick: () -> Unit,
    viewModel: ScoreDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(puzzleId, userId) { viewModel.load(puzzleId, userId) }
    val title = (uiState as? ScoreDetailUiState.Loaded)?.detail?.puzzleId?.substringAfter("_") ?: ""

    Scaffold(
        topBar = {
            SectionTopBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        when (val state = uiState) {
            is ScoreDetailUiState.Loading -> CenteredContent(Modifier.padding(padding)) {
                CircularProgressIndicator()
            }
            is ScoreDetailUiState.Error -> CenteredContent(Modifier.padding(padding)) {
                Text(state.message, style = MaterialTheme.typography.bodyLarge)
            }
            is ScoreDetailUiState.Loaded -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Weighted so it grows to fill the available space, pushing the SHARE /
                // SIGN IN TO SAVE buttons below down to the bottom of the screen instead
                // of leaving a gap when the content is shorter than the screen. Scrolls as
                // one unit (board/target included) rather than only the words/steps below
                // scrolling within their own confined box.
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (isSignedIn) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarIcon(state.detail.avatarId, state.detail.avatarColorId, avatarIconColor = state.detail.avatarIconColor, size = 40.dp)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(state.detail.displayName, style = MaterialTheme.typography.titleLarge)
                                // rank_today is 0 if this player has opted out of the global
                                // leaderboard -- no rank to show in that case.
                                if (state.detail.rankToday > 0) {
                                    Text(
                                        "Global rank #${state.detail.rankToday} today",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    if (state.detail.game == "numbers") {
                        NumbersDetailContent(state.detail)
                    } else {
                        BoggleDetailContent(state.detail)
                    }
                }

                if (state.isOwnScore) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                        ),
                        onClick = {
                            val date = state.detail.puzzleId.substringAfter("_")
                            val text = if (state.detail.game == "numbers") {
                                buildNumbersShareText(
                                    date = date,
                                    target = state.detail.target ?: 0,
                                    resultValue = state.detail.resultValue ?: 0,
                                    distance = state.detail.distance ?: 0,
                                    durationSeconds = state.detail.durationSeconds ?: 0,
                                    rankToday = if (isSignedIn && state.detail.rankToday > 0) state.detail.rankToday else null,
                                )
                            } else {
                                buildBoggleShareText(
                                    date = date,
                                    score = state.detail.score ?: 0,
                                    wordCount = state.detail.validWords?.size ?: 0,
                                    rankToday = if (isSignedIn && state.detail.rankToday > 0) state.detail.rankToday else null,
                                )
                            }
                            shareText(context, text)
                        },
                    ) {
                        Text("SHARE")
                    }
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
    }
}

@Composable
private fun ColumnScope.BoggleDetailContent(detail: ScoreDetailDto) {
    Text("Score: ${detail.score ?: 0}", style = MaterialTheme.typography.titleLarge)

    if (detail.locked) {
        LockedNotice("Complete today's Words puzzle to see the board and word list.")
        return
    }

    BoggleBoardView(board = detail.board ?: emptyList())

    val words = detail.validWords ?: emptyList()
    if (words.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            words.forEach { word ->
                Text(
                    text = "$word (${word.length})",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    } else {
        Text(
            "No words found.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColumnScope.NumbersDetailContent(detail: ScoreDetailDto) {
    val distance = detail.distance ?: 0
    Text(
        text = if (distance == 0) {
            "Got it in ${detail.durationSeconds ?: 0}s!"
        } else {
            "Reached ${detail.resultValue ?: 0} ($distance away)"
        },
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )

    if (detail.locked) {
        LockedNotice("Complete today's Numbers puzzle to see the target, numbers, and method.")
        return
    }

    Text("Target: ${detail.target ?: 0}", style = MaterialTheme.typography.titleLarge)
    Text(
        (detail.numbers ?: emptyList()).joinToString(", "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val steps = detail.steps ?: emptyList()
    if (steps.isNotEmpty()) {
        Text(
            "How they got there:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            steps.forEach { step ->
                Text(
                    "${step.a} ${numbersOpSymbol(step.op)} ${step.b} = ${step.result}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun LockedNotice(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CenteredContent(modifier: Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        content = content,
    )
}
