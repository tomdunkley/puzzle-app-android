package com.tomdunkley.dailypuzzles.ui.screens.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.network.ApiClient
import com.tomdunkley.dailypuzzles.data.network.dto.GameSummaryDto
import com.tomdunkley.dailypuzzles.data.network.dto.LeaderboardEntryDto
import com.tomdunkley.dailypuzzles.data.network.handleIfVerificationRequired
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.tomdunkley.dailypuzzles.util.formatDisplayDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal const val MAX_DATE_OFFSET = 365
private val SUPPORTED_GAMES = setOf("boggle", "numbers", "routes")

enum class LeaderboardScope { FRIENDS, GLOBAL }

/** Reflects the user's current selections immediately on press — before the API responds. */
data class LeaderboardControlsState(
    val games: List<GameSummaryDto>,
    val selectedGameIndex: Int,
    val scope: LeaderboardScope,
    val dateLabel: String,
    val dateOffset: Int,
    val todayDate: LocalDate,
    val hasFriends: Boolean,
)

sealed interface LeaderboardUiState {
    data object Loading : LeaderboardUiState
    data class Error(val message: String) : LeaderboardUiState
    data class Loaded(
        val entries: List<LeaderboardEntryDto>,
        val selfUserId: String,
        val puzzleId: String,
        val scope: LeaderboardScope,
        val hasFriends: Boolean,
    ) : LeaderboardUiState
}

private fun dateLabel(offset: Int, todayDate: LocalDate): String =
    formatDisplayDate(todayDate.minusDays(offset.toLong()))

class LeaderboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private val _controlsState = MutableStateFlow<LeaderboardControlsState?>(null)
    val controlsState: StateFlow<LeaderboardControlsState?> = _controlsState.asStateFlow()

    private var games: List<GameSummaryDto> = emptyList()
    private var selectedGameIndex: Int = 0
    private var scope: LeaderboardScope = LeaderboardScope.FRIENDS
    private var dateOffset: Int = 0
    private var serverTodayDate: LocalDate? = null
    private var cachedHasFriends: Boolean = false
    private var loadJob: Job? = null

    private fun updateControls() {
        val today = serverTodayDate ?: return
        if (games.isEmpty()) return
        _controlsState.value = LeaderboardControlsState(
            games = games,
            selectedGameIndex = selectedGameIndex,
            scope = scope,
            dateLabel = dateLabel(dateOffset, today),
            dateOffset = dateOffset,
            todayDate = today,
            hasFriends = cachedHasFriends,
        )
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = LeaderboardUiState.Loading
            runCatching {
                if (games.isEmpty()) {
                    games = ApiClient.authenticatedService.getGames()
                        .filter { it.game in SUPPORTED_GAMES }
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
                Triple(me.userId, puzzleId, leaderboard.entries) to hasFriends
            }.onSuccess { (triple, hasFriends) ->
                val (selfUserId, puzzleId, entries) = triple
                cachedHasFriends = hasFriends
                updateControls()
                _uiState.value = LeaderboardUiState.Loaded(
                    entries = entries,
                    selfUserId = selfUserId,
                    puzzleId = puzzleId,
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

    fun selectGame(index: Int) {
        if (index == selectedGameIndex) return
        selectedGameIndex = index.coerceIn(0, games.lastIndex)
        updateControls()
        load()
    }

    fun selectPreviousGame() {
        if (selectedGameIndex <= 0) return
        selectedGameIndex--
        updateControls()
        load()
    }

    fun selectNextGame() {
        if (selectedGameIndex >= games.lastIndex) return
        selectedGameIndex++
        updateControls()
        load()
    }

    fun selectScope(newScope: LeaderboardScope) {
        if (scope == newScope) return
        scope = newScope
        updateControls()
        load()
    }

    fun selectOlderDate() {
        if (dateOffset >= MAX_DATE_OFFSET) return
        dateOffset++
        updateControls()
        load()
    }

    fun selectNewerDate() {
        if (dateOffset <= 0) return
        dateOffset--
        updateControls()
        load()
    }

    fun selectDate(date: LocalDate) {
        val today = serverTodayDate ?: return
        val offset = java.time.temporal.ChronoUnit.DAYS.between(date, today).toInt()
        val clamped = offset.coerceIn(0, MAX_DATE_OFFSET)
        if (clamped == dateOffset) return
        dateOffset = clamped
        updateControls()
        load()
    }
}
