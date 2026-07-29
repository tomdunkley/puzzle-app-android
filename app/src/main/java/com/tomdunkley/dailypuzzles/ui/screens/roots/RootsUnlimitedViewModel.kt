package com.tomdunkley.dailypuzzles.ui.screens.roots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.audio.SoundFeedback
import com.tomdunkley.dailypuzzles.data.roots.RootsCell
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedHighScoreStore
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedPuzzleGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RootsUnlimitedUiState {
    data class Start(
        val seed: String,
        val gridSize: Int,
        val bestTimeSeconds: Int,
    ) : RootsUnlimitedUiState
    data class Playing(
        val gridSize: Int,
        val startCell: RootsCell,
        val endCell: RootsCell,
        val solution: List<RootsCell>,
        val rowClues: List<Int>,
        val colClues: List<Int>,
        val currentPath: List<RootsCell>,
        val crossMarkers: Set<RootsCell>,
        val tickMarkers: Set<RootsCell>,
        val elapsedSeconds: Int,
        val isPaused: Boolean = false,
    ) : RootsUnlimitedUiState
    data class Results(
        val gridSize: Int,
        val startCell: RootsCell,
        val endCell: RootsCell,
        val solution: List<RootsCell>,
        val rowClues: List<Int>,
        val colClues: List<Int>,
        val durationSeconds: Int,
        val seed: String,
        val isNewBest: Boolean,
    ) : RootsUnlimitedUiState
}

private data class UnlimitedHistoryEntry(
    val path: List<RootsCell>,
    val crossMarkers: Set<RootsCell>,
    val tickMarkers: Set<RootsCell>,
)

class RootsUnlimitedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<RootsUnlimitedUiState>(buildStartState(""))
    val uiState: StateFlow<RootsUnlimitedUiState> = _uiState.asStateFlow()

    private val _seed = MutableStateFlow("")
    val seed: StateFlow<String> = _seed.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val history = ArrayDeque<UnlimitedHistoryEntry>()

    private fun pushHistory(state: RootsUnlimitedUiState.Playing) {
        if (history.size >= 50) history.removeAt(0)
        history.addLast(UnlimitedHistoryEntry(state.currentPath, state.crossMarkers, state.tickMarkers))
        _canUndo.value = true
    }

    private fun clearHistory() {
        history.clear()
        _canUndo.value = false
    }

    fun undo() {
        val state = _uiState.value as? RootsUnlimitedUiState.Playing ?: return
        if (history.isEmpty()) return
        val entry = history.removeAt(history.size - 1)
        _uiState.value = state.copy(
            currentPath = entry.path,
            crossMarkers = entry.crossMarkers,
            tickMarkers = entry.tickMarkers,
        )
        _canUndo.value = history.isNotEmpty()
    }

    private var timerJob: Job? = null

    fun loadPuzzle() {
        if (_seed.value.isNotEmpty()) return
        newGame()
    }

    fun newGame() {
        timerJob?.cancel()
        val code = UnlimitedPuzzleGenerator.generateSeedCode()
        _seed.value = code
        _uiState.value = buildStartState(code)
    }

    fun setSeed(code: String) {
        val upper = code.uppercase().trim()
        _seed.value = upper
        _uiState.value = buildStartState(upper)
    }

    fun startGame() {
        val code = _seed.value
        timerJob?.cancel()
        clearHistory()
        val baseSeed = UnlimitedPuzzleGenerator.seedCodeToLong(code)
        val gridSize = (baseSeed % 3).toInt().let { rem ->
            when (if (rem < 0) rem + 3 else rem) {
                0 -> 4; 1 -> 5; else -> 6
            }
        }
        val puzzle = RootsPuzzleGenerator.generate(baseSeed, gridSize)
        _uiState.value = RootsUnlimitedUiState.Playing(
            gridSize = puzzle.gridSize,
            startCell = puzzle.startCell,
            endCell = puzzle.endCell,
            solution = puzzle.solution,
            rowClues = puzzle.rowClues,
            colClues = puzzle.colClues,
            currentPath = emptyList(),
            crossMarkers = emptySet(),
            tickMarkers = emptySet(),
            elapsedSeconds = 0,
        )
        startTimer()
    }

    private fun buildStartState(code: String): RootsUnlimitedUiState.Start {
        val baseSeed = if (code.isEmpty()) 0L else UnlimitedPuzzleGenerator.seedCodeToLong(code)
        val gridSize = (baseSeed % 3).toInt().let { rem ->
            when (if (rem < 0) rem + 3 else rem) {
                0 -> 4; 1 -> 5; else -> 6
            }
        }
        val best = if (code.isEmpty()) -1 else UnlimitedHighScoreStore.rootsBestTimeSeconds
        return RootsUnlimitedUiState.Start(seed = code, gridSize = gridSize, bestTimeSeconds = best)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value as? RootsUnlimitedUiState.Playing ?: return@launch
                if (!current.isPaused) {
                    _uiState.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
                }
            }
        }
    }

    fun persistProgress() {
        val state = _uiState.value as? RootsUnlimitedUiState.Playing ?: return
        _uiState.value = state.copy(isPaused = true)
    }

    fun resume() {
        val state = _uiState.value as? RootsUnlimitedUiState.Playing ?: return
        if (!state.isPaused) return
        _uiState.value = state.copy(isPaused = false)
    }

    private enum class DragMode { PATH, CROSS_MARKER, TICK_MARKER, CLEAR_MARKER, NONE }
    private var dragMode = DragMode.NONE

    fun onDragStart(cell: RootsCell) {
        val state = _uiState.value as? RootsUnlimitedUiState.Playing ?: return
        if (state.isPaused) return
        pushHistory(state)
        val path = state.currentPath
        val idx = path.indexOf(cell)
        if (idx >= 0) {
            _uiState.value = state.copy(currentPath = path.subList(0, idx + 1))
            dragMode = DragMode.PATH
            return
        }
        if (cell == state.startCell || cell == state.endCell) {
            _uiState.value = state.copy(currentPath = listOf(cell))
            dragMode = DragMode.PATH
            return
        }
        when {
            state.crossMarkers.contains(cell) -> {
                dragMode = DragMode.TICK_MARKER
                _uiState.value = state.copy(
                    crossMarkers = state.crossMarkers - cell,
                    tickMarkers = state.tickMarkers + cell,
                )
            }
            state.tickMarkers.contains(cell) -> {
                dragMode = DragMode.CLEAR_MARKER
                _uiState.value = state.copy(tickMarkers = state.tickMarkers - cell)
            }
            else -> {
                dragMode = DragMode.CROSS_MARKER
                _uiState.value = state.copy(
                    crossMarkers = state.crossMarkers + cell,
                    tickMarkers = state.tickMarkers - cell,
                )
            }
        }
    }

    fun onCellDrag(cell: RootsCell) {
        val state = _uiState.value as? RootsUnlimitedUiState.Playing ?: return
        if (state.isPaused) return
        when (dragMode) {
            DragMode.CROSS_MARKER -> {
                if (cell == state.startCell || cell == state.endCell) return
                if (state.currentPath.contains(cell)) return
                _uiState.value = state.copy(
                    crossMarkers = state.crossMarkers + cell,
                    tickMarkers = state.tickMarkers - cell,
                )
            }
            DragMode.TICK_MARKER -> {
                if (cell == state.startCell || cell == state.endCell) return
                if (state.currentPath.contains(cell)) return
                _uiState.value = state.copy(
                    crossMarkers = state.crossMarkers - cell,
                    tickMarkers = state.tickMarkers + cell,
                )
            }
            DragMode.CLEAR_MARKER -> {
                if (cell == state.startCell || cell == state.endCell) return
                if (state.currentPath.contains(cell)) return
                _uiState.value = state.copy(
                    crossMarkers = state.crossMarkers - cell,
                    tickMarkers = state.tickMarkers - cell,
                )
            }
            else -> {
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
                    tickMarkers = state.tickMarkers - cell,
                )
                _uiState.value = updatedState
                if (RootsPuzzleGenerator.checkSolved(newPath, state.rowClues, state.colClues, state.startCell, state.endCell)) {
                    SoundFeedback.correct()
                    timerJob?.cancel()
                    submitResult(updatedState)
                }
            }
        }
    }

    fun onTapCell(cell: RootsCell) {
        val state = _uiState.value as? RootsUnlimitedUiState.Playing ?: return
        if (state.isPaused) return
        pushHistory(state)
        if (cell == state.startCell) {
            _uiState.value = state.copy(currentPath = listOf(cell))
            return
        }
        if (cell == state.endCell) return

        val idx = state.currentPath.indexOf(cell)
        if (idx >= 0) {
            _uiState.value = state.copy(currentPath = state.currentPath.subList(0, idx + 1))
            return
        }

        val updatedCross: Set<RootsCell>
        val updatedTick: Set<RootsCell>
        when {
            state.crossMarkers.contains(cell) -> {
                updatedCross = state.crossMarkers - cell
                updatedTick = state.tickMarkers + cell
            }
            state.tickMarkers.contains(cell) -> {
                updatedCross = state.crossMarkers
                updatedTick = state.tickMarkers - cell
            }
            else -> {
                updatedCross = state.crossMarkers + cell
                updatedTick = state.tickMarkers
            }
        }
        _uiState.value = state.copy(crossMarkers = updatedCross, tickMarkers = updatedTick)
    }

    fun clearPath() {
        val state = _uiState.value as? RootsUnlimitedUiState.Playing ?: return
        pushHistory(state)
        _uiState.value = state.copy(currentPath = emptyList(), crossMarkers = emptySet(), tickMarkers = emptySet())
    }

    private fun submitResult(state: RootsUnlimitedUiState.Playing) {
        clearHistory()
        val elapsed = state.elapsedSeconds
        val prevBest = UnlimitedHighScoreStore.rootsBestTimeSeconds
        val isNew = prevBest < 0 || elapsed < prevBest
        UnlimitedHighScoreStore.updateRootsTime(elapsed, _seed.value)
        _uiState.value = RootsUnlimitedUiState.Results(
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
        private fun isAdjacent(a: RootsCell, b: RootsCell) =
            (kotlin.math.abs(a.row - b.row) + kotlin.math.abs(a.col - b.col)) == 1
    }
}
