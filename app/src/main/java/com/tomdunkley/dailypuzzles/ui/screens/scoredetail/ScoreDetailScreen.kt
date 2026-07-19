package com.tomdunkley.dailypuzzles.ui.screens.scoredetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import com.tomdunkley.dailypuzzles.data.network.dto.AllWordDto
import com.tomdunkley.dailypuzzles.util.formatDisplayDate
import com.tomdunkley.dailypuzzles.data.network.dto.ScoreDetailDto
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.BoggleBoardView
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor
import com.tomdunkley.dailypuzzles.ui.screens.boggle.scoreForWord
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
    onViewProfile: ((String) -> Unit)? = null,
    viewModel: ScoreDetailViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val allWordsState by viewModel.allWordsState.collectAsState()
    val context = LocalContext.current
    var showAllWordsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(puzzleId, userId) { viewModel.load(puzzleId, userId) }
    val loadedDetail = (uiState as? ScoreDetailUiState.Loaded)?.detail
    val gameLabel = when (loadedDetail?.game) { "numbers" -> "Numbers" else -> "Words" }
    val dateOrSeed = loadedDetail?.puzzleId?.substringAfter("_")?.let { iso ->
        runCatching { formatDisplayDate(LocalDate.parse(iso)) }.getOrDefault(iso)
    } ?: ""
    val title = if (loadedDetail != null) "$gameLabel: $dateOrSeed" else ""
    val topBarColor = when (loadedDetail?.game) {
        "numbers" -> NumbersSolidColor.copy(alpha = 0.15f)
        else -> WordsSolidColor.copy(alpha = 0.15f)
    }

    Scaffold(
        topBar = {
            SectionTopBar(
                title = title,
                backgroundColor = topBarColor,
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
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val isNumbers = state.detail.game == "numbers"
                Column(
                    modifier = if (isNumbers) {
                        Modifier.weight(1f).padding(vertical = 12.dp)
                    } else {
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 12.dp)
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = if (isNumbers) {
                        Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
                    } else {
                        Arrangement.spacedBy(12.dp)
                    },
                ) {
                    if (isSignedIn && isNumbers) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (onViewProfile != null && !state.isOwnScore)
                                        Modifier.clickable { onViewProfile(userId) }
                                    else Modifier,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AvatarIcon(state.detail.avatarId, state.detail.avatarColorId, avatarIconColor = state.detail.avatarIconColor, size = 40.dp)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(state.detail.displayName, style = MaterialTheme.typography.titleLarge)
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

                    if (isNumbers) {
                        NumbersDetailContent(state.detail)
                    } else {
                        BoggleDetailContent(
                            state.detail,
                            isSignedIn,
                            onViewProfile = if (!state.isOwnScore) onViewProfile else null,
                        )
                    }
                }

                if (state.detail.game == "boggle" && !state.detail.locked) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                        onClick = {
                            viewModel.loadAllWords(state.detail.puzzleId)
                            showAllWordsDialog = true
                        },
                    ) {
                        Text("VIEW ALL POSSIBLE WORDS")
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
                Spacer(modifier = Modifier.height(4.dp))

                if (showAllWordsDialog) {
                    AllWordsDialog(
                        state = allWordsState,
                        onClose = { showAllWordsDialog = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun AllWordsDialog(state: AllWordsState, onClose: () -> Unit) {
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
                    AllWordsState.Idle, AllWordsState.Loading -> Box(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    is AllWordsState.Error -> Text(
                        state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    is AllWordsState.Loaded -> FlowRow(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.words.forEach { entry ->
                            Text(
                                text = "${entry.word} (${entry.score})",
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
                ) {
                    Text("CLOSE")
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.BoggleDetailContent(
    detail: ScoreDetailDto,
    isSignedIn: Boolean,
    onViewProfile: ((String) -> Unit)? = null,
) {
    if (isSignedIn) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier
                    .then(if (onViewProfile != null) Modifier.clickable { onViewProfile(detail.userId) } else Modifier)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarIcon(detail.avatarId, detail.avatarColorId, avatarIconColor = detail.avatarIconColor, size = 40.dp)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(detail.displayName, style = MaterialTheme.typography.titleLarge)
                    if (detail.rankToday > 0) {
                        Text(
                            "Global rank #${detail.rankToday} today",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Score: ${detail.score ?: 0}", style = MaterialTheme.typography.titleLarge)
                if (!detail.locked) {
                    val wordCount = detail.validWords?.size ?: 0
                    Text(
                        "$wordCount word${if (wordCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    } else {
        Text("Score: ${detail.score ?: 0}", style = MaterialTheme.typography.titleLarge)
        if (!detail.locked) {
            val wordCount = detail.validWords?.size ?: 0
            Text(
                "$wordCount word${if (wordCount == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (detail.locked) {
        LockedNotice("Complete today's Words puzzle to see the board and word list.")
        return
    }

    BoggleBoardView(board = detail.board ?: emptyList())

    val words = (detail.validWords ?: emptyList()).sortedByDescending { it.length }
    if (words.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            words.forEach { word ->
                Text(
                    text = "$word (${scoreForWord(word)})",
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
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
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
