package com.tomdunkley.dailypuzzles.ui.screens.numbers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.audio.SoundFeedback
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.network.dto.NumbersStepDto
import com.tomdunkley.dailypuzzles.data.network.dto.PuzzleDto
import com.tomdunkley.dailypuzzles.data.network.dto.ScoreSubmissionDto
import com.tomdunkley.dailypuzzles.data.network.handleIfVerificationRequired
import com.tomdunkley.dailypuzzles.data.network.isOffline
import com.tomdunkley.dailypuzzles.data.network.todayUtcIso
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import com.tomdunkley.dailypuzzles.data.numbers.NumbersProgress
import com.tomdunkley.dailypuzzles.data.numbers.NumbersProgressStore
import com.tomdunkley.dailypuzzles.data.numbers.NumbersResult
import com.tomdunkley.dailypuzzles.data.numbers.NumbersTile
import com.tomdunkley.dailypuzzles.data.network.dto.ClaimAchievementRequest
import com.tomdunkley.dailypuzzles.data.trophies.TROPHY_TITLES
import com.tomdunkley.dailypuzzles.data.trophies.TrophySeenStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NumbersUiState {
    data object Loading : NumbersUiState
    data class Error(val message: String) : NumbersUiState
    data class Playing(
        val target: Int,
        val tiles: List<NumbersTile>,
        val solution: List<NumbersStepDto>,
        val selectedTileId: Int? = null,
        val pendingOp: String? = null,
        val errorMessage: String? = null,
        val history: Map<Int, List<NumbersTile>> = emptyMap(),
        val bestValue: Int,
        val bestSteps: List<NumbersStepDto>,
        val secondsRemaining: Int,
        val isPaused: Boolean = false,
        val allComputedValues: Set<Int> = emptySet(),
    ) : NumbersUiState {
        val bestDistance: Int get() = kotlin.math.abs(target - bestValue)
    }
    data object Submitting : NumbersUiState
    data class Results(
        val target: Int,
        val numbers: List<Int>,
        val resultValue: Int,
        val steps: List<NumbersStepDto>,
        val solution: List<NumbersStepDto>,
        val distance: Int,
        val durationSeconds: Int,
        val rankToday: Int,
        val currentStreak: Int,
        val puzzleId: String,
        val date: String,
    ) : NumbersUiState
}

/** Computes a + op + b under the same legality rules the server re-validates at
 * submission time -- null means blocked (decimal division, or a non-positive
 * subtraction), matching webapp/app/services/numbers_game.py's `_apply`.
 */
private fun legalResult(a: Int, op: String, b: Int): Int? = when (op) {
    "+" -> a + b
    "*" -> a * b
    "-" -> if (a > b) a - b else null
    "/" -> if (b != 0 && a % b == 0) a / b else null
    else -> null
}

/** Human-readable reason a combine was rejected, shown in a feedback pill. */
private fun illegalCombineMessage(a: Int, op: String, b: Int): String = when (op) {
    "-" -> "$a needs to be bigger than $b"
    "/" -> "$b doesn't go into $a"
    else -> "That doesn't work"
}

class NumbersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<NumbersUiState>(NumbersUiState.Loading)
    val uiState: StateFlow<NumbersUiState> = _uiState.asStateFlow()

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
    private var errorMessageJob: Job? = null
    private var nextTileId = 0

    fun loadTodayPuzzle() {
        viewModelScope.launch {
            _uiState.value = NumbersUiState.Loading
            runCatching {
                val service = AuthRepository.apiServiceForCurrentSession()
                service to service.getTodayPuzzle("numbers")
            }
                .onSuccess { (service, dto) ->
                    puzzle = dto
                    if (dto.alreadyPlayed) {
                        val detail = runCatching {
                            val userId = service.getMyProfile().userId
                            service.getScoreDetail(dto.puzzleId, userId)
                        }.getOrNull()
                        val result = NumbersResult(
                            puzzleId = dto.puzzleId,
                            date = dto.date,
                            target = dto.target ?: 0,
                            numbers = dto.numbers ?: emptyList(),
                            resultValue = detail?.resultValue ?: dto.yourResultValue ?: 0,
                            steps = detail?.steps ?: emptyList(),
                            solution = dto.solution ?: emptyList(),
                            distance = detail?.distance ?: dto.yourDistance ?: 0,
                            durationSeconds = detail?.durationSeconds ?: 0,
                            rankToday = detail?.rankToday ?: 0,
                            currentStreak = dto.streak?.current ?: 0,
                        )
                        NumbersProgressStore.saveResult(result)
                        _uiState.value = result.toUiState()
                    } else {
                        val target = dto.target ?: 0
                        val solution = dto.solution ?: emptyList()
                        val saved = NumbersProgressStore.load(dto.puzzleId)
                        if (saved != null) {
                            nextTileId = saved.nextTileId
                            _uiState.value = NumbersUiState.Playing(
                                target = target,
                                tiles = saved.tiles,
                                solution = solution,
                                history = saved.history,
                                bestValue = saved.bestValue,
                                bestSteps = saved.bestSteps,
                                secondsRemaining = saved.secondsRemaining,
                                isPaused = saved.isPaused,
                            )
                            if (!saved.isPaused) startTimer()
                        } else {
                            val tiles = (dto.numbers ?: emptyList()).map { value ->
                                NumbersTile(id = nextTileId++, value = value, steps = emptyList())
                            }
                            val initialBest = tiles.minByOrNull { kotlin.math.abs(target - it.value) }
                            _uiState.value = NumbersUiState.Playing(
                                target = target,
                                tiles = tiles,
                                solution = solution,
                                bestValue = initialBest?.value ?: 0,
                                bestSteps = emptyList(),
                                secondsRemaining = dto.durationSeconds,
                            )
                            startTimer()
                        }
                    }
                }
                .onFailure {
                    if (handleIfVerificationRequired(it)) return@onFailure
                    // No network at all: today's puzzle id is deterministic from the date, so a
                    // previously cached result for it can still be shown without the server.
                    val cached = if (it.isOffline()) {
                        NumbersProgressStore.loadResult("numbers_${todayUtcIso()}")
                    } else {
                        null
                    }
                    _uiState.value = cached?.toUiState()
                        ?: NumbersUiState.Error(it.toUserMessage("Couldn't load today's puzzle"))
                }
        }
    }

    fun persistProgress() {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        timerJob?.cancel()
        val cleared = state.copy(isPaused = true, selectedTileId = null, pendingOp = null, errorMessage = null)
        _uiState.value = cleared
        NumbersProgressStore.save(
            NumbersProgress(
                puzzleId = puzzle.puzzleId,
                secondsRemaining = cleared.secondsRemaining,
                tiles = cleared.tiles,
                history = cleared.history,
                nextTileId = nextTileId,
                bestValue = cleared.bestValue,
                bestSteps = cleared.bestSteps,
                isPaused = true,
            ),
        )
    }

    fun resume() {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        if (!state.isPaused) return
        _uiState.value = state.copy(isPaused = false)
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value as? NumbersUiState.Playing ?: return@launch
                if (current.secondsRemaining <= 1) {
                    _uiState.value = current.copy(secondsRemaining = 0)
                    submit()
                    return@launch
                }
                _uiState.value = current.copy(secondsRemaining = current.secondsRemaining - 1)
            }
        }
    }

    /** Taps a tile -- the first tap (with no operator chosen yet) selects/deselects it
     * as the left-hand operand; a second tap (once an operator is chosen) immediately
     * applies the combine if it's legal (e.g. a + b), or surfaces a feedback pill
     * explaining why not (e.g. a decimal division) and resets the selection so the
     * player can just start again.
     */
    fun selectTile(tileId: Int) {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        val op = state.pendingOp
        if (op == null) {
            SoundFeedback.tap()
            _uiState.value = state.copy(selectedTileId = if (state.selectedTileId == tileId) null else tileId)
            return
        }
        val firstId = state.selectedTileId ?: return
        if (tileId == firstId) {
            // Tapping the already-selected tile cancels the pending operation entirely.
            SoundFeedback.tap()
            _uiState.value = state.copy(selectedTileId = null, pendingOp = null)
            return
        }
        val a = state.tiles.firstOrNull { it.id == firstId } ?: return
        val b = state.tiles.firstOrNull { it.id == tileId } ?: return
        val result = legalResult(a.value, op, b.value)
        if (result == null) {
            SoundFeedback.incorrect()
            showErrorMessage(illegalCombineMessage(a.value, op, b.value))
            _uiState.value = state.copy(selectedTileId = null, pendingOp = null)
            return
        }
        applyCombine(state, a, b, op, result)
    }

    fun selectOperator(op: String) {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        if (state.selectedTileId == null) return
        SoundFeedback.tap()
        _uiState.value = state.copy(pendingOp = if (state.pendingOp == op) null else op)
    }

    private fun checkInGameNumberAchievements(result: Int) {
        val toShow = mutableListOf<String>()
        if (result == 69 && shownInGameTrophyIds.add("nice")) toShow.add("nice")
        if (result >= 100_000_000 && shownInGameTrophyIds.add("massive")) toShow.add("massive")
        if (toShow.isNotEmpty()) {
            showTrophyNotification(toShow)
            claimAchievements(toShow)
        }
    }

    private fun claimAchievements(ids: List<String>) {
        ids.forEach { id ->
            viewModelScope.launch {
                runCatching {
                    AuthRepository.apiServiceForCurrentSession().claimAchievement(ClaimAchievementRequest(id))
                }.onSuccess { result ->
                    TrophySeenStore.addUnseen(result.newlyUnlocked)
                }
            }
        }
    }

    private fun showErrorMessage(message: String) {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        _uiState.value = state.copy(errorMessage = message)
        errorMessageJob?.cancel()
        errorMessageJob = viewModelScope.launch {
            delay(2200)
            val current = _uiState.value as? NumbersUiState.Playing ?: return@launch
            if (current.errorMessage == message) {
                _uiState.value = current.copy(errorMessage = null)
            }
        }
    }

    /** Replaces the two operand tiles with a single result tile -- in the position `a`
     * (the first tile picked, before the operator) held, so combining doesn't shuffle
     * the board by sending every result to the end -- records the combine so it can be
     * individually undone later, and updates the best-distance-so-far if this result is
     * closer to the target -- that tracked best is never affected by a later undo.
     */
    private fun applyCombine(state: NumbersUiState.Playing, a: NumbersTile, b: NumbersTile, op: String, result: Int) {
        val step = NumbersStepDto(a = a.value, op = op, b = b.value, result = result)
        val resultTile = NumbersTile(id = nextTileId++, value = result, steps = a.steps + b.steps + step)
        val newTiles = state.tiles.filterNot { it.id == b.id }.map { if (it.id == a.id) resultTile else it }
        val newHistory = state.history + (resultTile.id to listOf(a, b))

        val improved = kotlin.math.abs(state.target - resultTile.value) < state.bestDistance
        _uiState.value = state.copy(
            tiles = newTiles,
            selectedTileId = null,
            pendingOp = null,
            history = newHistory,
            bestValue = if (improved) resultTile.value else state.bestValue,
            bestSteps = if (improved) resultTile.steps else state.bestSteps,
            allComputedValues = state.allComputedValues + result,
        )

        checkInGameNumberAchievements(result)

        // Hitting the target exactly ends the round immediately -- no reason to make the
        // player press FINISH once there's nothing left to improve on.
        if (resultTile.value == state.target) {
            SoundFeedback.correct()
            timerJob?.cancel()
            submit()
        } else {
            SoundFeedback.tap()
        }
    }

    /** Reverts exactly the combine that produced `tileId`: removes that result tile and
     * restores the two tiles it was built from. Only tiles with a history entry (i.e.
     * ones produced by a combine, not original numbers) can be undone.
     */
    fun undo(tileId: Int) {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        val consumed = state.history[tileId] ?: return
        SoundFeedback.tap()
        _uiState.value = state.copy(
            tiles = state.tiles.filterNot { it.id == tileId } + consumed,
            history = state.history - tileId,
        )
    }

    /** Restores the board to the six original starting numbers, discarding every combine
     * made so far -- but never touches the tracked best, same as undo.
     */
    fun reset() {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        val tiles = (puzzle.numbers ?: emptyList()).map { value ->
            NumbersTile(id = nextTileId++, value = value, steps = emptyList())
        }
        SoundFeedback.tap()
        _uiState.value = state.copy(
            tiles = tiles,
            selectedTileId = null,
            pendingOp = null,
            errorMessage = null,
            history = emptyMap(),
        )
    }

    fun finishEarly() {
        timerJob?.cancel()
        submit()
    }

    private fun submit() {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        _uiState.value = NumbersUiState.Submitting
        viewModelScope.launch {
            val elapsed = puzzle.durationSeconds - state.secondsRemaining
            runCatching {
                AuthRepository.apiServiceForCurrentSession().submitScore(
                    ScoreSubmissionDto(
                        puzzleId = puzzle.puzzleId,
                        durationSeconds = elapsed,
                        resultValue = state.bestValue,
                        steps = state.bestSteps,
                        allComputedValues = state.allComputedValues.toList(),
                    ),
                )
            }
                .onSuccess { result ->
                    NumbersProgressStore.clear()
                    TrophySeenStore.addUnseen(result.newlyUnlocked)
                    showTrophyNotification(result.newlyUnlocked.filter { it !in shownInGameTrophyIds })
                    val completed = NumbersResult(
                        puzzleId = puzzle.puzzleId,
                        date = puzzle.date,
                        target = state.target,
                        numbers = puzzle.numbers ?: emptyList(),
                        resultValue = result.resultValue ?: state.bestValue,
                        steps = result.steps ?: state.bestSteps,
                        solution = state.solution,
                        distance = result.distance ?: 0,
                        durationSeconds = elapsed,
                        rankToday = result.rankToday,
                        currentStreak = result.currentStreak,
                    )
                    NumbersProgressStore.saveResult(completed)
                    _uiState.value = completed.toUiState()
                }
                .onFailure {
                    if (!handleIfVerificationRequired(it)) {
                        _uiState.value = NumbersUiState.Error(it.toUserMessage("Couldn't submit your score"))
                    }
                }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        errorMessageJob?.cancel()
    }
}

private fun NumbersResult.toUiState(): NumbersUiState.Results = NumbersUiState.Results(
    target = target,
    numbers = numbers,
    resultValue = resultValue,
    steps = steps,
    solution = solution,
    distance = distance,
    durationSeconds = durationSeconds,
    rankToday = rankToday,
    currentStreak = currentStreak,
    puzzleId = puzzleId,
    date = date,
)
