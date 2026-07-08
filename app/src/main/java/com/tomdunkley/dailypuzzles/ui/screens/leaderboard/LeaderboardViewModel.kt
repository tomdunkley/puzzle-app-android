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
import com.tomdunkley.dailypuzzles.util.formatDisplayDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
private const val MAX_DATE_OFFSET = 7

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
        val dateLabel: String,
        val dateOffset: Int,
    ) : LeaderboardUiState
}

private fun dateLabel(offset: Int, todayDate: LocalDate): String =
    formatDisplayDate(todayDate.minusDays(offset.toLong()))

class LeaderboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private var games: List<GameSummaryDto> = emptyList()
    private var selectedGameIndex: Int = 0
    private var scope: LeaderboardScope = LeaderboardScope.FRIENDS
    private var dateOffset: Int = 0
    private var serverTodayDate: LocalDate? = null

    fun load() {
        viewModelScope.launch {
            _uiState.value = LeaderboardUiState.Loading
            runCatching {
                if (games.isEmpty()) {
                    games = ApiClient.authenticatedService.getGames()
                }
                val game = games.getOrNull(selectedGameIndex)?.game ?: "boggle"
                val me = ApiClient.authenticatedService.getMyProfile()

                val todayDate = serverTodayDate ?: run {
                    val todayPuzzle = ApiClient.authenticatedService.getTodayPuzzle(game)
                    val parsed = LocalDate.parse(todayPuzzle.puzzleId.substringAfter("_"))
                    serverTodayDate = parsed
                    parsed
                }

                val puzzleDate = todayDate.minusDays(dateOffset.toLong())
                val puzzleId = "${game}_${puzzleDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}"

                val leaderboard = when (scope) {
                    LeaderboardScope.FRIENDS -> ApiClient.authenticatedService.getLeaderboard(puzzleId)
                    LeaderboardScope.GLOBAL -> ApiClient.authenticatedService.getGlobalLeaderboard(puzzleId)
                }
                val hasFriends = ApiClient.authenticatedService.getFriends().isNotEmpty()
                Triple(me.userId, puzzleId, leaderboard.entries) to Pair(hasFriends, todayDate)
            }.onSuccess { (triple, extra) ->
                val (selfUserId, puzzleId, entries) = triple
                val (hasFriends, todayDate) = extra
                _uiState.value = LeaderboardUiState.Loaded(
                    entries = entries,
                    selfUserId = selfUserId,
                    puzzleId = puzzleId,
                    games = games,
                    selectedGameIndex = selectedGameIndex,
                    scope = scope,
                    hasFriends = hasFriends,
                    dateLabel = dateLabel(dateOffset, todayDate),
                    dateOffset = dateOffset,
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

    fun selectOlderDate() {
        if (dateOffset >= MAX_DATE_OFFSET) return
        dateOffset++
        load()
    }

    fun selectNewerDate() {
        if (dateOffset <= 0) return
        dateOffset--
        load()
    }
}
