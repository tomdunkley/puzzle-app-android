package com.tomdunkley.dailypuzzles.ui.screens.boggle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.audio.SoundFeedback
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.boggle.BoggleDictionary
import com.tomdunkley.dailypuzzles.data.boggle.BoggleProgress
import com.tomdunkley.dailypuzzles.data.boggle.BoggleProgressStore
import com.tomdunkley.dailypuzzles.data.boggle.BoggleResult
import com.tomdunkley.dailypuzzles.data.trophies.TROPHY_TITLES
import com.tomdunkley.dailypuzzles.data.trophies.TrophySeenStore
import com.tomdunkley.dailypuzzles.data.network.dto.PuzzleDto
import com.tomdunkley.dailypuzzles.data.network.dto.ScoreSubmissionDto
import com.tomdunkley.dailypuzzles.data.network.handleIfVerificationRequired
import com.tomdunkley.dailypuzzles.data.network.isOffline
import com.tomdunkley.dailypuzzles.data.network.todayUtcIso
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val BOARD_SIZE = 5

enum class WordFeedbackType { CORRECT, INCORRECT, DUPLICATE, TOO_SHORT }

data class WordFeedback(val word: String, val type: WordFeedbackType)

sealed interface BoggleUiState {
    data object Loading : BoggleUiState
    data class Error(val message: String) : BoggleUiState
    data class Playing(
        val board: List<String>,
        val selectedPath: List<Int> = emptyList(),
        val foundWords: List<String> = emptyList(),
        val secondsRemaining: Int,
        val isPaused: Boolean = false,
        val wordFeedback: WordFeedback? = null,
    ) : BoggleUiState {
        val currentWord: String get() = selectedPath.joinToString("") { board[it] }
        val liveScore: Int get() = estimatedScoreForWords(foundWords)
    }
    data object Submitting : BoggleUiState
    data class Results(
        val score: Int,
        val validWords: List<String>,
        val rankToday: Int,
        val currentStreak: Int,
        val puzzleId: String,
        val date: String,
    ) : BoggleUiState
}

/** True if `to` is one of the 8 neighbors of `from` on the BOARD_SIZE x BOARD_SIZE grid. */
fun areAdjacent(from: Int, to: Int): Boolean {
    val fromRow = from / BOARD_SIZE
    val fromCol = from % BOARD_SIZE
    val toRow = to / BOARD_SIZE
    val toCol = to % BOARD_SIZE
    return kotlin.math.abs(fromRow - toRow) <= 1 && kotlin.math.abs(fromCol - toCol) <= 1 && from != to
}

class BoggleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<BoggleUiState>(BoggleUiState.Loading)
    val uiState: StateFlow<BoggleUiState> = _uiState.asStateFlow()

    private val _newlyUnlockedTrophies = MutableStateFlow<List<String>>(emptyList())
    val newlyUnlockedTrophies: StateFlow<List<String>> = _newlyUnlockedTrophies.asStateFlow()

    fun dismissTrophyNotification() {
        _newlyUnlockedTrophies.value = _newlyUnlockedTrophies.value.drop(1)
    }

    private val shownInGameTrophyIds = mutableSetOf<String>()

    private fun showTrophyNotification(trophyIds: List<String>) {
        val titles = trophyIds.mapNotNull { TROPHY_TITLES[it] }
        if (titles.isNotEmpty()) _newlyUnlockedTrophies.value = _newlyUnlockedTrophies.value + titles
    }

    private lateinit var puzzle: PuzzleDto
    private var timerJob: kotlinx.coroutines.Job? = null

    fun loadTodayPuzzle() {
        viewModelScope.launch {
            _uiState.value = BoggleUiState.Loading
            runCatching {
                val service = AuthRepository.apiServiceForCurrentSession()
                service to service.getTodayPuzzle("boggle")
            }
                .onSuccess { (service, dto) ->
                    puzzle = dto
                    if (dto.alreadyPlayed) {
                        // Reuses the exact same Results state/UI a just-finished game lands on --
                        // "already played today" and "you just finished" are the same screen.
                        val detail = runCatching {
                            val userId = service.getMyProfile().userId
                            service.getScoreDetail(dto.puzzleId, userId)
                        }.getOrNull()
                        val result = BoggleResult(
                            puzzleId = dto.puzzleId,
                            date = dto.date,
                            score = dto.yourScore ?: 0,
                            validWords = detail?.validWords ?: emptyList(),
                            rankToday = detail?.rankToday ?: 0,
                            currentStreak = dto.streak?.current ?: 0,
                        )
                        BoggleProgressStore.saveResult(result)
                        _uiState.value = result.toUiState()
                    } else {
                        val saved = BoggleProgressStore.load(dto.puzzleId)
                        _uiState.value = BoggleUiState.Playing(
                            board = dto.board ?: emptyList(),
                            foundWords = saved?.foundWords ?: emptyList(),
                            secondsRemaining = saved?.secondsRemaining ?: dto.durationSeconds,
                            isPaused = saved?.isPaused ?: false,
                        )
                        // Progress is only ever saved while paused (see persistProgress), so a
                        // resumed-but-not-paused state never happens -- this check just guards
                        // the same brand-new-puzzle case as `saved == null`.
                        if (saved?.isPaused != true) {
                            startTimer()
                        }
                    }
                }
                .onFailure {
                    if (handleIfVerificationRequired(it)) return@onFailure
                    // No network at all: today's puzzle id is deterministic from the date, so a
                    // previously cached result for it can still be shown without the server.
                    val cached = if (it.isOffline()) {
                        BoggleProgressStore.loadResult("boggle_${todayUtcIso()}")
                    } else {
                        null
                    }
                    _uiState.value = cached?.toUiState()
                        ?: BoggleUiState.Error(it.toUserMessage("Couldn't load today's puzzle"))
                }
        }
    }

    /** Cancels the timer and saves progress as paused -- called both from the explicit
     * pause button and from the screen's ON_STOP lifecycle hook (leaving/backgrounding
     * the game always pauses it, per the product requirement).
     */
    fun persistProgress() {
        val state = _uiState.value as? BoggleUiState.Playing ?: return
        timerJob?.cancel()
        _uiState.value = state.copy(isPaused = true)
        BoggleProgressStore.save(
            BoggleProgress(
                puzzleId = puzzle.puzzleId,
                secondsRemaining = state.secondsRemaining,
                foundWords = state.foundWords,
                isPaused = true,
            ),
        )
    }

    fun resume() {
        val state = _uiState.value as? BoggleUiState.Playing ?: return
        if (!state.isPaused) return
        _uiState.value = state.copy(isPaused = false)
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value as? BoggleUiState.Playing ?: return@launch
                if (current.secondsRemaining <= 1) {
                    _uiState.value = current.copy(secondsRemaining = 0)
                    submit()
                    return@launch
                }
                _uiState.value = current.copy(secondsRemaining = current.secondsRemaining - 1)
            }
        }
    }

    /** Called as the player drags across the grid: extends the path if `index` is
     * adjacent to the last selected cell and not already part of it. Dragging back
     * over an already-selected cell (other than the current last one) backtracks --
     * truncates the path to end at that cell, letting the player "unselect" a wrong
     * turn without lifting their finger.
     */
    fun onCellDragged(index: Int) {
        val state = _uiState.value as? BoggleUiState.Playing ?: return
        val path = state.selectedPath
        val existingPosition = path.indexOf(index)
        if (existingPosition != -1) {
            if (existingPosition == path.lastIndex) return
            _uiState.value = state.copy(selectedPath = path.subList(0, existingPosition + 1).toList())
            return
        }
        val last = path.lastOrNull()
        if (last != null && !areAdjacent(last, index)) return
        SoundFeedback.tap()
        _uiState.value = state.copy(selectedPath = path + index)
    }

    private var feedbackJob: Job? = null

    /** Called when the player lifts their finger: commits the current path as a found
     * word once it clears the same two gates the server would apply -- long enough, not
     * already found, and an actual dictionary word -- using the on-device word list so a
     * non-word never visibly appears in the found-words list in the first place. The
     * server still independently re-validates everything when the game ends. Also drives
     * the brief pulse/shake feedback shown on the current-word line.
     */
    fun onDragEnded() {
        val state = _uiState.value as? BoggleUiState.Playing ?: return
        val word = state.currentWord
        _uiState.value = state.copy(selectedPath = emptyList())
        if (word.isEmpty()) return
        if (word.length < 4) {
            showFeedback(word, WordFeedbackType.TOO_SHORT)
            return
        }
        if (word in state.foundWords) {
            showFeedback(word, WordFeedbackType.DUPLICATE)
            return
        }
        viewModelScope.launch {
            val isValid = BoggleDictionary.contains(word)
            val current = _uiState.value as? BoggleUiState.Playing ?: return@launch
            if (isValid) {
                if (word !in current.foundWords) {
                    _uiState.value = current.copy(foundWords = current.foundWords + word)
                    checkInGameWordAchievements(word)
                } else {
                    _uiState.value = current
                }
                showFeedback(word, WordFeedbackType.CORRECT)
            } else {
                showFeedback(word, WordFeedbackType.INCORRECT)
            }
        }
    }

    private fun checkInGameWordAchievements(word: String) {
        val w = word.lowercase()
        val toShow = mutableListOf<String>()
        if (w == "words" && shownInGameTrophyIds.add("meta")) toShow.add("meta")
        if ("qu" in w && shownInGameTrophyIds.add("queen")) toShow.add("queen")
        if (w.length >= 8 && shownInGameTrophyIds.add("hippopotomonstrosesquippedaliophobia")) toShow.add("hippopotomonstrosesquippedaliophobia")
        showTrophyNotification(toShow)
    }

    private fun showFeedback(word: String, type: WordFeedbackType) {
        when (type) {
            WordFeedbackType.CORRECT -> SoundFeedback.correct()
            WordFeedbackType.INCORRECT, WordFeedbackType.TOO_SHORT -> SoundFeedback.incorrect()
            WordFeedbackType.DUPLICATE -> SoundFeedback.duplicate()
        }
        val state = _uiState.value as? BoggleUiState.Playing ?: return
        _uiState.value = state.copy(wordFeedback = WordFeedback(word, type))
        feedbackJob?.cancel()
        feedbackJob = viewModelScope.launch {
            // Correct: linger 900ms. Incorrect/duplicate: 1000ms so there's time for the
            // fade-out animation in the UI to complete before wordFeedback becomes null.
            delay(if (type == WordFeedbackType.CORRECT) 900 else 1000)
            val current = _uiState.value as? BoggleUiState.Playing ?: return@launch
            if (current.wordFeedback?.word == word) {
                _uiState.value = current.copy(wordFeedback = null)
            }
        }
    }

    /** Resolved lazily, on demand (e.g. tapping "view word list"), rather than cached --
     * mirrors how other screens (Leaderboard) re-fetch the profile each time it's needed.
     */
    suspend fun ownUserId(): String? =
        runCatching { AuthRepository.apiServiceForCurrentSession().getMyProfile().userId }.getOrNull()

    fun finishEarly() {
        timerJob?.cancel()
        submit()
    }

    private fun submit() {
        val state = _uiState.value as? BoggleUiState.Playing ?: return
        _uiState.value = BoggleUiState.Submitting
        viewModelScope.launch {
            val elapsed = puzzle.durationSeconds - state.secondsRemaining
            runCatching {
                AuthRepository.apiServiceForCurrentSession().submitScore(
                    ScoreSubmissionDto(
                        puzzleId = puzzle.puzzleId,
                        words = state.foundWords,
                        durationSeconds = elapsed,
                    ),
                )
            }
                .onSuccess { result ->
                    BoggleProgressStore.clear()
                    TrophySeenStore.addUnseen(result.newlyUnlocked)
                    showTrophyNotification(result.newlyUnlocked.filter { it !in shownInGameTrophyIds })
                    val completed = BoggleResult(
                        puzzleId = puzzle.puzzleId,
                        date = puzzle.date,
                        score = result.score ?: 0,
                        validWords = result.validWords ?: emptyList(),
                        rankToday = result.rankToday,
                        currentStreak = result.currentStreak,
                    )
                    BoggleProgressStore.saveResult(completed)
                    _uiState.value = completed.toUiState()
                }
                .onFailure {
                    if (!handleIfVerificationRequired(it)) {
                        _uiState.value = BoggleUiState.Error(it.toUserMessage("Couldn't submit your score"))
                    }
                }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        feedbackJob?.cancel()
    }
}

private fun BoggleResult.toUiState(): BoggleUiState.Results = BoggleUiState.Results(
    score = score,
    validWords = validWords,
    rankToday = rankToday,
    currentStreak = currentStreak,
    puzzleId = puzzleId,
    date = date,
)
