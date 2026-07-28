package com.tomdunkley.dailypuzzles.ui.screens.roots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.audio.SoundFeedback
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.roots.RootsCell
import com.tomdunkley.dailypuzzles.data.roots.RootsProgress
import com.tomdunkley.dailypuzzles.data.roots.RootsProgressStore
import com.tomdunkley.dailypuzzles.data.roots.RootsResult
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

sealed interface RootsUiState {
    data object Loading : RootsUiState
    data class Error(val message: String) : RootsUiState
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
    ) : RootsUiState
    data object Submitting : RootsUiState
    data class Results(
        val gridSize: Int,
        val startCell: RootsCell,
        val endCell: RootsCell,
        val solution: List<RootsCell>,
        val rowClues: List<Int>,
        val colClues: List<Int>,
        val durationSeconds: Int,
        val rankToday: Int,
        val currentStreak: Int,
        val puzzleId: String,
        val date: String,
        val personalBestSeconds: Int?,
    ) : RootsUiState
}

class RootsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<RootsUiState>(RootsUiState.Loading)
    val uiState: StateFlow<RootsUiState> = _uiState.asStateFlow()

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
            _uiState.value = RootsUiState.Loading
            runCatching {
                AuthRepository.apiServiceForCurrentSession().getTodayPuzzle("routes")
            }
                .onSuccess { dto ->
                    puzzleId = dto.puzzleId
                    puzzleDate = dto.date
                    val gridSize = gridSizeForDate(dto.date)
                    val seed = seedFromPuzzleId(dto.puzzleId)

                    if (dto.alreadyPlayed) {
                        val saved = RootsProgressStore.loadResult(dto.puzzleId)
                        val result = RootsResult(
                            puzzleId = dto.puzzleId,
                            date = dto.date,
                            gridSize = gridSize,
                            durationSeconds = saved?.durationSeconds ?: 0,
                            rankToday = saved?.rankToday ?: 0,
                            currentStreak = dto.streak?.current ?: 0,
                        )
                        RootsProgressStore.saveResult(result)
                        val personalBest = RootsProgressStore.personalBestSeconds()
                        _uiState.value = result.toUiState(seed, personalBest.takeIf { it >= 0 })
                    } else {
                        val puzzle = RootsPuzzleGenerator.generate(seed, gridSize)
                        val saved = RootsProgressStore.load(dto.puzzleId)
                        if (saved != null && saved.gridSize == gridSize) {
                            _uiState.value = RootsUiState.Playing(
                                gridSize = puzzle.gridSize,
                                startCell = puzzle.startCell,
                                endCell = puzzle.endCell,
                                solution = puzzle.solution,
                                rowClues = puzzle.rowClues,
                                colClues = puzzle.colClues,
                                currentPath = saved.currentPath,
                                crossMarkers = saved.crossMarkers.toSet(),
                                tickMarkers = saved.tickMarkers.toSet(),
                                elapsedSeconds = saved.elapsedSeconds,
                                isPaused = saved.isPaused,
                            )
                            if (!saved.isPaused) startTimer()
                        } else {
                            _uiState.value = RootsUiState.Playing(
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
                    }
                }
                .onFailure {
                    if (handleIfVerificationRequired(it)) return@onFailure
                    val cached = if (it.isOffline()) {
                        RootsProgressStore.loadResult("routes_${todayUtcIso()}")
                    } else null
                    if (cached != null) {
                        val personalBest = RootsProgressStore.personalBestSeconds()
                        _uiState.value = cached.toUiState(seedFromPuzzleId(cached.puzzleId), personalBest.takeIf { it >= 0 })
                    } else {
                        _uiState.value = RootsUiState.Error(it.toUserMessage("Couldn't load today's puzzle"))
                    }
                }
        }
    }

    fun persistProgress() {
        val state = _uiState.value as? RootsUiState.Playing ?: return
        timerJob?.cancel()
        val paused = state.copy(isPaused = true)
        _uiState.value = paused
        RootsProgressStore.save(
            RootsProgress(
                puzzleId = puzzleId,
                gridSize = paused.gridSize,
                startCell = paused.startCell,
                endCell = paused.endCell,
                solution = paused.solution,
                rowClues = paused.rowClues,
                colClues = paused.colClues,
                currentPath = paused.currentPath,
                crossMarkers = paused.crossMarkers.toList(),
                tickMarkers = paused.tickMarkers.toList(),
                elapsedSeconds = paused.elapsedSeconds,
                isPaused = true,
            ),
        )
    }

    fun resume() {
        val state = _uiState.value as? RootsUiState.Playing ?: return
        if (!state.isPaused) return
        _uiState.value = state.copy(isPaused = false)
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value as? RootsUiState.Playing ?: return@launch
                _uiState.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
            }
        }
    }

    private enum class DragMode { PATH, CROSS_MARKER, NONE }
    private var dragMode = DragMode.NONE

    fun onDragStart(cell: RootsCell) {
        val state = _uiState.value as? RootsUiState.Playing ?: return
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
        dragMode = DragMode.CROSS_MARKER
        _uiState.value = state.copy(
            crossMarkers = state.crossMarkers + cell,
            tickMarkers = state.tickMarkers - cell,
        )
    }

    fun onCellDrag(cell: RootsCell) {
        val state = _uiState.value as? RootsUiState.Playing ?: return
        when (dragMode) {
            DragMode.CROSS_MARKER -> {
                if (cell == state.startCell || cell == state.endCell) return
                if (state.currentPath.contains(cell)) return
                _uiState.value = state.copy(
                    crossMarkers = state.crossMarkers + cell,
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
                    submit(updatedState)
                }
            }
        }
    }

    fun onTapCell(cell: RootsCell) {
        val state = _uiState.value as? RootsUiState.Playing ?: return
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
        val state = _uiState.value as? RootsUiState.Playing ?: return
        _uiState.value = state.copy(currentPath = emptyList())
    }

    private fun submit(state: RootsUiState.Playing) {
        _uiState.value = RootsUiState.Submitting
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
                    RootsProgressStore.clear()
                    TrophySeenStore.addUnseen(result.newlyUnlocked)
                    if (result.newlyUnlocked.isNotEmpty()) {
                        val titles = result.newlyUnlocked.mapNotNull { TROPHY_TITLES[it] }
                        if (titles.isNotEmpty()) {
                            _newlyUnlockedTrophies.value = _newlyUnlockedTrophies.value + titles
                        }
                        claimAchievements(result.newlyUnlocked)
                    }
                    RootsProgressStore.updatePersonalBest(state.elapsedSeconds)
                    val personalBest = RootsProgressStore.personalBestSeconds()
                    val rootsResult = RootsResult(
                        puzzleId = puzzleId,
                        date = puzzleDate,
                        gridSize = state.gridSize,
                        durationSeconds = state.elapsedSeconds,
                        rankToday = result.rankToday,
                        currentStreak = result.currentStreak,
                    )
                    RootsProgressStore.saveResult(rootsResult)
                    val seed = seedFromPuzzleId(puzzleId)
                    _uiState.value = rootsResult.toUiState(seed, personalBest.takeIf { it >= 0 })
                }
                .onFailure {
                    if (!handleIfVerificationRequired(it)) {
                        _uiState.value = RootsUiState.Error(it.toUserMessage("Couldn't submit your score"))
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

    override fun onCleared() {
        timerJob?.cancel()
    }

    companion object {
        private fun isAdjacent(a: RootsCell, b: RootsCell) =
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

private fun RootsResult.toUiState(seed: Long, personalBestSeconds: Int?): RootsUiState.Results {
    val puzzle = RootsPuzzleGenerator.generate(seed, gridSize)
    return RootsUiState.Results(
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
        personalBestSeconds = personalBestSeconds,
    )
}
