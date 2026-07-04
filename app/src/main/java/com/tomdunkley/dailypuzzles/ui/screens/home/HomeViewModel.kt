package com.tomdunkley.dailypuzzles.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.boggle.BoggleProgressStore
import com.tomdunkley.dailypuzzles.data.network.todayUtcIso
import com.tomdunkley.dailypuzzles.data.numbers.NumbersProgressStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PuzzleStatus { NONE, IN_PROGRESS, COMPLETED }

class HomeViewModel : ViewModel() {

    private val _puzzleStatuses = MutableStateFlow<Map<String, PuzzleStatus>>(emptyMap())
    val puzzleStatuses: StateFlow<Map<String, PuzzleStatus>> = _puzzleStatuses.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Emits cached statuses immediately so the home cards show the last-known state
     * without any loading spinners, then fetches from the network and updates.
     * Pull-to-refresh also calls this; the [isRefreshing] indicator covers the update.
     */
    fun refresh() {
        val todayId = todayUtcIso()
        _puzzleStatuses.value = availablePuzzles.associate { puzzle ->
            val todayPuzzleId = "${puzzle.id}_$todayId"
            puzzle.id to when {
                hasCachedResult(puzzle.id, todayPuzzleId) -> PuzzleStatus.COMPLETED
                hasSavedProgress(puzzle.id, todayPuzzleId) -> PuzzleStatus.IN_PROGRESS
                else -> PuzzleStatus.NONE
            }
        }
        viewModelScope.launch {
            _isRefreshing.value = true
            val service = AuthRepository.apiServiceForExistingSession()
            val statuses = availablePuzzles.associate { puzzle ->
                val todayPuzzleId = "${puzzle.id}_$todayId"
                val alreadyPlayed = service?.let {
                    runCatching { it.getTodayPuzzle(puzzle.id) }.getOrNull()?.alreadyPlayed
                } == true
                val status = when {
                    alreadyPlayed || hasCachedResult(puzzle.id, todayPuzzleId) -> PuzzleStatus.COMPLETED
                    hasSavedProgress(puzzle.id, todayPuzzleId) -> PuzzleStatus.IN_PROGRESS
                    else -> PuzzleStatus.NONE
                }
                puzzle.id to status
            }
            _puzzleStatuses.value = statuses
            _isRefreshing.value = false
        }
    }

    private fun hasSavedProgress(gameId: String, puzzleId: String): Boolean = when (gameId) {
        "boggle" -> BoggleProgressStore.load(puzzleId) != null
        "numbers" -> NumbersProgressStore.load(puzzleId) != null
        else -> false
    }

    private fun hasCachedResult(gameId: String, puzzleId: String): Boolean = when (gameId) {
        "boggle" -> BoggleProgressStore.loadResult(puzzleId) != null
        "numbers" -> NumbersProgressStore.loadResult(puzzleId) != null
        else -> false
    }
}
