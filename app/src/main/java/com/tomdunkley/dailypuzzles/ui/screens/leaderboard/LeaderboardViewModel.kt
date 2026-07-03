package com.tomdunkley.dailypuzzles.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.network.ApiClient
import com.tomdunkley.dailypuzzles.data.network.dto.GameSummaryDto
import com.tomdunkley.dailypuzzles.data.network.dto.LeaderboardEntryDto
import com.tomdunkley.dailypuzzles.data.network.handleIfVerificationRequired
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LeaderboardScope { FRIENDS, GLOBAL }

sealed interface LeaderboardUiState {
    data object Loading : LeaderboardUiState
    data class Error(val message: String) : LeaderboardUiState
    data class Loaded(
        val entries: List<LeaderboardEntryDto>,
        val selfUserId: String,
        val puzzleId: String,
        val games: List<GameSummaryDto>,
        val selectedGameIndex: Int,
        val scope: LeaderboardScope,
        val hasFriends: Boolean,
    ) : LeaderboardUiState
}

class LeaderboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var games: List<GameSummaryDto> = emptyList()
    private var selectedGameIndex: Int = 0
    private var scope: LeaderboardScope = LeaderboardScope.FRIENDS

    fun load() {
        viewModelScope.launch {
            _uiState.value = LeaderboardUiState.Loading
            runCatching {
                if (games.isEmpty()) {
                    games = ApiClient.authenticatedService.getGames()
                }
                val game = games.getOrNull(selectedGameIndex)?.game ?: "boggle"
                val me = ApiClient.authenticatedService.getMyProfile()
                val puzzle = ApiClient.authenticatedService.getTodayPuzzle(game)
                val leaderboard = when (scope) {
                    LeaderboardScope.FRIENDS -> ApiClient.authenticatedService.getLeaderboard(puzzle.puzzleId)
                    LeaderboardScope.GLOBAL -> ApiClient.authenticatedService.getGlobalLeaderboard(puzzle.puzzleId)
                }
                val hasFriends = ApiClient.authenticatedService.getFriends().isNotEmpty()
                Triple(me.userId, puzzle.puzzleId, leaderboard.entries) to hasFriends
            }.onSuccess { (triple, hasFriends) ->
                val (selfUserId, puzzleId, entries) = triple
                _uiState.value = LeaderboardUiState.Loaded(
                    entries = entries,
                    selfUserId = selfUserId,
                    puzzleId = puzzleId,
                    games = games,
                    selectedGameIndex = selectedGameIndex,
                    scope = scope,
                    hasFriends = hasFriends,
                )
            }.onFailure {
                if (!handleIfVerificationRequired(it)) {
                    _uiState.value = LeaderboardUiState.Error(it.toUserMessage("Couldn't load the leaderboard"))
                }
            }
        }
    }

    fun selectPreviousGame() {
        if (selectedGameIndex <= 0) return
        selectedGameIndex--
        load()
    }

    fun selectNextGame() {
        if (selectedGameIndex >= games.lastIndex) return
        selectedGameIndex++
        load()
    }

    fun selectScope(newScope: LeaderboardScope) {
        if (scope == newScope) return
        scope = newScope
        load()
    }
}
