package com.tomdunkley.dailypuzzles.ui.screens.lines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.audio.SoundFeedback
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.lines.LinesCell
import com.tomdunkley.dailypuzzles.data.lines.LinesProgress
import com.tomdunkley.dailypuzzles.data.lines.LinesProgressStore
import com.tomdunkley.dailypuzzles.data.lines.LinesResult
import com.tomdunkley.dailypuzzles.data.network.dto.ClaimAchievementRequest
import com.tomdunkley.dailypuzzles.data.network.dto.ScoreSubmissionDto
import com.tomdunkley.dailypuzzles.data.network.handleIfVerificationRequired
import com.tomdunkley.dailypuzzles.data.network.isOffline
import com.tomdunkley.dailypuzzles.data.network.todayUtcIso
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import com.tomdunkley.dailypuzzles.data.trophies.TROPHY_TITLES
import com.tomdunkley.dailypuzzles.data.trophies.TrophySeenStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

sealed interface LinesUiState {
    data object Loading : LinesUiState
    data class Error(val message: String) : LinesUiState
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
    ) : LinesUiState
    data object Submitting : LinesUiState
    data class Results(
        val gridSize: Int,
        val startCell: LinesCell,
        val endCell: LinesCell,
        val solution: List<LinesCell>,
        val rowClues: List<Int>,
        val colClues: List<Int>,
        val durationSeconds: Int,
        val rankToday: Int,
        val currentStreak: Int,
        val puzzleId: String,
        val date: String,
        val dailyBestDurationSeconds: Int? = null,
        val isNewDailyBest: Boolean = false,
    ) : LinesUiState
}

class LinesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LinesUiState>(LinesUiState.Loading)
    val uiState: StateFlow<LinesUiState> = _uiState.asStateFlow()

    private val _newlyUnlockedTrophies = MutableStateFlow<List<String>>(emptyList())
    val newlyUnlockedTrophies: StateFlow<List<String>> = _newlyUnlockedTrophies.asStateFlow()

    fun dismissTrophyNotification() {
        _newlyUnlockedTrophies.value = _newlyUnlockedTrophies.value.drop(1)
    }

    private var puzzleId = ""
    private var puzzleDate = ""
    private var timerJob: Job? = null

    fun loadTodayPuzzle() {
        viewModelScope.launch {
            _uiState.value = LinesUiState.Loading
            runCatching {
                AuthRepository.apiServiceForCurrentSession().getTodayPuzzle("lines")
            }
                .onSuccess { dto ->
                    puzzleId = dto.puzzleId
                    puzzleDate = dto.date
                    val gridSize = gridSizeForDate(dto.date)
                    val seed = seedFromPuzzleId(dto.puzzleId)

                    if (dto.alreadyPlayed) {
                        val saved = LinesProgressStore.loadResult(dto.puzzleId)
                        val result = LinesResult(
                            puzzleId = dto.puzzleId,
                            date = dto.date,
                            gridSize = gridSize,
                            durationSeconds = saved?.durationSeconds ?: 0,
                            rankToday = saved?.rankToday ?: 0,
                            currentStreak = dto.streak?.current ?: 0,
                            dailyBestDurationSeconds = saved?.dailyBestDurationSeconds,
                            isNewDailyBest = saved?.isNewDailyBest ?: false,
                        )
                        LinesProgressStore.saveResult(result)
                        _uiState.value = result.toUiState(seed)
                    } else {
                        val puzzle = LinesPuzzleGenerator.generate(seed, gridSize)
                        val saved = LinesProgressStore.load(dto.puzzleId)
                        if (saved != null && saved.gridSize == gridSize) {
                            _uiState.value = LinesUiState.Playing(
                                gridSize = puzzle.gridSize,
                                startCell = puzzle.startCell,
                                endCell = puzzle.endCell,
                                solution = puzzle.solution,
                                rowClues = puzzle.rowClues,
                                colClues = puzzle.colClues,
                                currentPath = saved.currentPath,
                                crossMarkers = saved.crossMarkers.toSet(),
                                elapsedSeconds = saved.elapsedSeconds,
                                isPaused = saved.isPaused,
                            )
                            if (!saved.isPaused) startTimer()
                        } else {
                            _uiState.value = LinesUiState.Playing(
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
                    }
                }
                .onFailure {
                    if (handleIfVerificationRequired(it)) return@onFailure
                    val cached = if (it.isOffline()) {
                        LinesProgressStore.loadResult("lines_${todayUtcIso()}")
                    } else null
                    if (cached != null) {
                        _uiState.value = cached.toUiState(seedFromPuzzleId(cached.puzzleId))
                    } else {
                        _uiState.value = LinesUiState.Error(it.toUserMessage("Couldn't load today's puzzle"))
                    }
                }
        }
    }

    fun persistProgress() {
        val state = _uiState.value as? LinesUiState.Playing ?: return
        timerJob?.cancel()
        val paused = state.copy(isPaused = true)
        _uiState.value = paused
        LinesProgressStore.save(
            LinesProgress(
                puzzleId = puzzleId,
                gridSize = paused.gridSize,
                startCell = paused.startCell,
                endCell = paused.endCell,
                solution = paused.solution,
                rowClues = paused.rowClues,
                colClues = paused.colClues,
                currentPath = paused.currentPath,
                crossMarkers = paused.crossMarkers.toList(),
                elapsedSeconds = paused.elapsedSeconds,
                isPaused = true,
            ),
        )
    }

    fun resume() {
        val state = _uiState.value as? LinesUiState.Playing ?: return
        if (!state.isPaused) return
        _uiState.value = state.copy(isPaused = false)
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value as? LinesUiState.Playing ?: return@launch
                _uiState.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
            }
        }
    }

    fun onCellDrag(cell: LinesCell) {
        val state = _uiState.value as? LinesUiState.Playing ?: return
        val path = state.currentPath

        // Start drawing: can initiate from either endpoint when path is empty
        if (path.isEmpty()) {
            if (cell == state.startCell || cell == state.endCell) {
                _uiState.value = state.copy(currentPath = listOf(cell))
            }
            return
        }

        val pathHead = path.last()
        val pathTail = path.first()

        // Check if this extends the head
        if (cell == pathHead) return
        val existingIdx = path.indexOf(cell)
        if (existingIdx >= 0) {
            // Trim path back to this cell
            _uiState.value = state.copy(currentPath = path.subList(0, existingIdx + 1))
            return
        }

        // Must be adjacent to head to extend
        if (!isAdjacent(cell, pathHead)) {
            // Try from the tail instead (drawing from the other end)
            if (path.size == 1 && (pathTail == state.startCell || pathTail == state.endCell)) {
                if (isAdjacent(cell, pathTail)) {
                    _uiState.value = state.copy(currentPath = listOf(cell, pathTail))
                }
            }
            return
        }

        val newPath = path + cell
        val updatedState = state.copy(
            currentPath = newPath,
            crossMarkers = state.crossMarkers - cell,
        )
        _uiState.value = updatedState

        if (LinesPuzzleGenerator.checkSolved(newPath, state.rowClues, state.colClues, state.startCell, state.endCell)) {
            SoundFeedback.correct()
            timerJob?.cancel()
            submit(updatedState)
        }
    }

    fun onDragStart(cell: LinesCell) {
        val state = _uiState.value as? LinesUiState.Playing ?: return
        val path = state.currentPath

        if (path.isEmpty()) {
            if (cell == state.startCell || cell == state.endCell) {
                _uiState.value = state.copy(currentPath = listOf(cell))
            }
            return
        }

        // If dragging from an endpoint that IS in the path, trim to just that endpoint
        if (cell == state.startCell || cell == state.endCell) {
            val idx = path.indexOf(cell)
            if (idx >= 0) {
                _uiState.value = state.copy(currentPath = path.subList(0, idx + 1))
                return
            }
        }

        // If dragging from the current head or tail, keep path as-is (will extend via onCellDrag)
        if (cell == path.last() || cell == path.first()) return

        // Tapping the head/tail's adjacent position already handled in onCellDrag
    }

    fun onTapCell(cell: LinesCell) {
        val state = _uiState.value as? LinesUiState.Playing ?: return
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
        val state = _uiState.value as? LinesUiState.Playing ?: return
        _uiState.value = state.copy(currentPath = emptyList())
    }

    private fun submit(state: LinesUiState.Playing) {
        _uiState.value = LinesUiState.Submitting
        viewModelScope.launch {
            runCatching {
                AuthRepository.apiServiceForCurrentSession().submitScore(
                    ScoreSubmissionDto(
                        puzzleId = puzzleId,
                        durationSeconds = state.elapsedSeconds,
                        resultValue = state.elapsedSeconds,
                    ),
                )
            }
                .onSuccess { result ->
                    LinesProgressStore.clear()
                    TrophySeenStore.addUnseen(result.newlyUnlocked)
                    if (result.newlyUnlocked.isNotEmpty()) {
                        val titles = result.newlyUnlocked.mapNotNull { TROPHY_TITLES[it] }
                        if (titles.isNotEmpty()) {
                            _newlyUnlockedTrophies.value = _newlyUnlockedTrophies.value + titles
                        }
                        claimAchievements(result.newlyUnlocked)
                    }
                    val linesResult = LinesResult(
                        puzzleId = puzzleId,
                        date = puzzleDate,
                        gridSize = state.gridSize,
                        durationSeconds = state.elapsedSeconds,
                        rankToday = result.rankToday,
                        currentStreak = result.currentStreak,
                        dailyBestDurationSeconds = result.dailyBestDurationSeconds,
                        isNewDailyBest = result.isNewDailyBest,
                    )
                    LinesProgressStore.saveResult(linesResult)
                    val seed = seedFromPuzzleId(puzzleId)
                    _uiState.value = linesResult.toUiState(seed)
                }
                .onFailure {
                    if (!handleIfVerificationRequired(it)) {
                        _uiState.value = LinesUiState.Error(it.toUserMessage("Couldn't submit your score"))
                    }
                }
        }
    }

    private fun claimAchievements(ids: List<String>) {
        ids.forEach { id ->
            viewModelScope.launch {
                runCatching {
                    AuthRepository.apiServiceForCurrentSession().claimAchievement(ClaimAchievementRequest(id))
                }.onSuccess { result ->
                    if (result.newlyUnlocked.isNotEmpty()) {
                        val titles = result.newlyUnlocked.mapNotNull { TROPHY_TITLES[it] }
                        if (titles.isNotEmpty()) {
                            _newlyUnlockedTrophies.value = _newlyUnlockedTrophies.value + titles
                        }
                        TrophySeenStore.addUnseen(result.newlyUnlocked)
                    }
                }
            }
        }
    }

    suspend fun ownUserId(): String? =
        runCatching { AuthRepository.apiServiceForCurrentSession().getMyProfile().userId }.getOrNull()

    override fun onCleared() {
        timerJob?.cancel()
    }

    companion object {
        private fun isAdjacent(a: LinesCell, b: LinesCell) =
            (kotlin.math.abs(a.row - b.row) + kotlin.math.abs(a.col - b.col)) == 1

        fun gridSizeForDate(dateStr: String): Int {
            return try {
                val date = LocalDate.parse(dateStr)
                when (date.dayOfWeek) {
                    DayOfWeek.MONDAY -> 4
                    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> 6
                    else -> 5
                }
            } catch (e: Exception) {
                5
            }
        }

        fun seedFromPuzzleId(puzzleId: String): Long =
            puzzleId.fold(0L) { acc, c -> acc * 31 + c.code }
    }
}

private fun LinesResult.toUiState(seed: Long): LinesUiState.Results {
    val puzzle = LinesPuzzleGenerator.generate(seed, gridSize)
    return LinesUiState.Results(
        gridSize = gridSize,
        startCell = puzzle.startCell,
        endCell = puzzle.endCell,
        solution = puzzle.solution,
        rowClues = puzzle.rowClues,
        colClues = puzzle.colClues,
        durationSeconds = durationSeconds,
        rankToday = rankToday,
        currentStreak = currentStreak,
        puzzleId = puzzleId,
        date = date,
        dailyBestDurationSeconds = dailyBestDurationSeconds,
        isNewDailyBest = isNewDailyBest,
    )
}
