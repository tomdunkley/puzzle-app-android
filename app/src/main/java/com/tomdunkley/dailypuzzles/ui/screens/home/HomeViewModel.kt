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

enum class PuzzleStatus { LOADING, NONE, IN_PROGRESS, COMPLETED }

class HomeViewModel : ViewModel() {

    private val _puzzleStatuses = MutableStateFlow<Map<String, PuzzleStatus>>(emptyMap())
    val puzzleStatuses: StateFlow<Map<String, PuzzleStatus>> = _puzzleStatuses.asStateFlow()

    /** Badges each puzzle as completed, in-progress (paused/unfinished), or untouched
     * today. Completed/in-progress are also checked against the on-device caches so
     * the badges stay correct even with no network -- only a never-played-as-guest
     * signed-out visitor with no local state sees nothing badged.
     */
    fun refresh() {
        // Show LOADING on every card while the network call is in flight.
        _puzzleStatuses.value = availablePuzzles.associate { it.id to PuzzleStatus.LOADING }
        viewModelScope.launch {
            val service = AuthRepository.apiServiceForExistingSession()
            val statuses = availablePuzzles.associate { puzzle ->
                val todayPuzzleId = "${puzzle.id}_${todayUtcIso()}"
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
