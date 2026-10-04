package com.tomdunkley.dailypuzzles.ui.screens.cryptic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CrypticClueScreen(
    onBack: () -> Unit,
    viewModel: CrypticClueViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val suggestedWords by viewModel.suggestedWords.collectAsState()
    val isFetchingWords by viewModel.isFetchingWords.collectAsState()
    val wordFetchError by viewModel.wordFetchError.collectAsState()

    var solution by remember { mutableStateOf("") }
    var clue by remember { mutableStateOf("") }

    val isSubmitting = uiState is CrypticClueUiState.Submitting

    Scaffold(
        topBar = { SectionTopBar(title = "Cryptic Clue", onBack = onBack) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = solution,
                onValueChange = { solution = it.uppercase() },
                label = { Text("Solution") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSubmitting,
            )

            OutlinedTextField(
                value = clue,
                onValueChange = { clue = it },
                label = { Text("Clue") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                enabled = !isSubmitting,
            )

            Button(
                onClick = {
                    viewModel.submitClue(solution, clue)
                },
                enabled = solution.isNotBlank() && clue.isNotBlank() && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        strokeWidth = 2.dp,
                    )
                }
                Text("SUBMIT")
            }

            when (val state = uiState) {
                is CrypticClueUiState.Submitted -> {
                    Text(
                        "Submitted: ${state.solution}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedButton(
                        onClick = {
                            solution = ""
                            clue = ""
                            viewModel.resetState()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                    ) { Text("ADD ANOTHER") }
                }
                is CrypticClueUiState.Error -> Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Unit
            }

            HorizontalDivider()

            Text(
                "Word suggestions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Suggests words that fit well in small dense crossword grids — good candidates for cryptic clue answers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = { viewModel.fetchWordSuggestions() },
                enabled = !isFetchingWords,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
            ) {
                if (isFetchingWords) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text("SUGGEST WORDS")
            }

            wordFetchError?.let { error ->
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            if (suggestedWords.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestedWords.forEach { word ->
                        OutlinedButton(
                            onClick = {
                                solution = word
                                viewModel.resetState()
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Text(word, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
