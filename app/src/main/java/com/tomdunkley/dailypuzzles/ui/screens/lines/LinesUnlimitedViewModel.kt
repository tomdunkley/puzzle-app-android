package com.tomdunkley.dailypuzzles.ui.screens.lines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.audio.SoundFeedback
import com.tomdunkley.dailypuzzles.data.lines.LinesCell
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedHighScoreStore
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedPuzzleGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface LinesUnlimitedUiState {
    data object Loading : LinesUnlimitedUiState
    data class Playing(
        val gridSize: Int,
        val startCell: LinesCell,
        val endCell: LinesCell,
        val solution: List<LinesCell>,
        val rowClues: List<Int>,
        val colClues: List<Int>,
        val currentPath: List<LinesCell>,
        val crossMarkers: Set<LinesCell>,
        val elapsedSeconds: Int,
        val isPaused: Boolean = false,
    ) : LinesUnlimitedUiState
    data class Results(
        val gridSize: Int,
        val startCell: LinesCell,
        val endCell: LinesCell,
        val solution: List<LinesCell>,
        val rowClues: List<Int>,
        val colClues: List<Int>,
        val durationSeconds: Int,
        val seed: String,
        val isNewBest: Boolean,
    ) : LinesUnlimitedUiState
}

class LinesUnlimitedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LinesUnlimitedUiState>(LinesUnlimitedUiState.Loading)
    val uiState: StateFlow<LinesUnlimitedUiState> = _uiState.asStateFlow()

    private val _seed = MutableStateFlow("")
    val seed: StateFlow<String> = _seed.asStateFlow()

    private var timerJob: Job? = null
    private var currentPuzzle: LinesPuzzle? = null

    fun loadPuzzle() {
        if (_seed.value.isNotEmpty()) return
        newGame()
    }

    fun newGame() {
        timerJob?.cancel()
        val code = UnlimitedPuzzleGenerator.generateSeedCode()
        _seed.value = code
        startGame(code)
    }

    fun setSeed(code: String) {
        _seed.value = code
        startGame(code)
    }

    private fun startGame(code: String) {
        timerJob?.cancel()
        val baseSeed = UnlimitedPuzzleGenerator.seedCodeToLong(code)
        val gridSize = (baseSeed % 3).toInt().let { rem ->
            when (if (rem < 0) rem + 3 else rem) {
                0 -> 4; 1 -> 5; else -> 6
            }
        }
        val puzzle = LinesPuzzleGenerator.generate(baseSeed, gridSize)
        currentPuzzle = puzzle
        _uiState.value = LinesUnlimitedUiState.Playing(
            gridSize = puzzle.gridSize,
            startCell = puzzle.startCell,
            endCell = puzzle.endCell,
            solution = puzzle.solution,
            rowClues = puzzle.rowClues,
            colClues = puzzle.colClues,
            currentPath = emptyList(),
            crossMarkers = emptySet(),
            elapsedSeconds = 0,
        )
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value as? LinesUnlimitedUiState.Playing ?: return@launch
                if (!current.isPaused) {
                    _uiState.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
                }
            }
        }
    }

    fun persistProgress() {
        val state = _uiState.value as? LinesUnlimitedUiState.Playing ?: return
        _uiState.value = state.copy(isPaused = true)
    }

    fun resume() {
        val state = _uiState.value as? LinesUnlimitedUiState.Playing ?: return
        if (!state.isPaused) return
        _uiState.value = state.copy(isPaused = false)
    }

    fun onDragStart(cell: LinesCell) {
        val state = _uiState.value as? LinesUnlimitedUiState.Playing ?: return
        if (state.isPaused) return
        val path = state.currentPath
        if (path.isEmpty()) {
            if (cell == state.startCell || cell == state.endCell) {
                _uiState.value = state.copy(currentPath = listOf(cell))
            }
            return
        }
        if (cell == state.startCell || cell == state.endCell) {
            val idx = path.indexOf(cell)
            if (idx >= 0) {
                _uiState.value = state.copy(currentPath = path.subList(0, idx + 1))
            }
        }
    }

    fun onCellDrag(cell: LinesCell) {
        val state = _uiState.value as? LinesUnlimitedUiState.Playing ?: return
        if (state.isPaused) return
        val path = state.currentPath
        if (path.isEmpty()) {
            if (cell == state.startCell || cell == state.endCell) {
                _uiState.value = state.copy(currentPath = listOf(cell))
            }
            return
        }
        val pathHead = path.last()
        if (cell == pathHead) return
        val existingIdx = path.indexOf(cell)
        if (existingIdx >= 0) {
            _uiState.value = state.copy(currentPath = path.subList(0, existingIdx + 1))
            return
        }
        if (!isAdjacent(cell, pathHead)) return
        val newPath = path + cell
        val updatedState = state.copy(
            currentPath = newPath,
            crossMarkers = state.crossMarkers - cell,
        )
        _uiState.value = updatedState
        if (LinesPuzzleGenerator.checkSolved(newPath, state.rowClues, state.colClues, state.startCell, state.endCell)) {
            SoundFeedback.correct()
            timerJob?.cancel()
            submitResult(updatedState)
        }
    }

    fun onTapCell(cell: LinesCell) {
        val state = _uiState.value as? LinesUnlimitedUiState.Playing ?: return
        if (state.isPaused) return
        if (cell == state.startCell || cell == state.endCell) return
        if (state.currentPath.contains(cell)) return
        val updated = if (state.crossMarkers.contains(cell)) {
            state.crossMarkers - cell
        } else {
            state.crossMarkers + cell
        }
        _uiState.value = state.copy(crossMarkers = updated)
    }

    fun clearPath() {
        val state = _uiState.value as? LinesUnlimitedUiState.Playing ?: return
        _uiState.value = state.copy(currentPath = emptyList())
    }

    private fun submitResult(state: LinesUnlimitedUiState.Playing) {
        val elapsed = state.elapsedSeconds
        val prevBest = UnlimitedHighScoreStore.linesBestTimeSeconds(state.gridSize)
        val isNew = prevBest < 0 || elapsed < prevBest
        UnlimitedHighScoreStore.updateLinesTime(state.gridSize, elapsed, _seed.value)
        _uiState.value = LinesUnlimitedUiState.Results(
            gridSize = state.gridSize,
            startCell = state.startCell,
            endCell = state.endCell,
            solution = state.solution,
            rowClues = state.rowClues,
            colClues = state.colClues,
            durationSeconds = elapsed,
            seed = _seed.value,
            isNewBest = isNew,
        )
    }

    override fun onCleared() {
        timerJob?.cancel()
    }

    companion object {
        private fun isAdjacent(a: LinesCell, b: LinesCell) =
            (kotlin.math.abs(a.row - b.row) + kotlin.math.abs(a.col - b.col)) == 1
    }
}
