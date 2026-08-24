package com.tomdunkley.dailypuzzles.ui.screens.numbers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.audio.SoundFeedback
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.network.dto.ChallengePlayRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.ChallengePlayResultDto
import com.tomdunkley.dailypuzzles.data.network.dto.NumbersStepDto
import com.tomdunkley.dailypuzzles.data.numbers.NumbersTile
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedHighScoreStore
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedPuzzleGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val UNLIMITED_DURATION_SECONDS = 90
private val BIG_NUMBERS = setOf(25, 50, 75, 100)
private const val SOLUTION_CAP = 200

private fun unlimitedLegalResult(a: Int, op: String, b: Int): Int? = when (op) {
    "+" -> a + b
    "*" -> a * b
    "-" -> if (a > b) a - b else null
    "/" -> if (b != 0 && a % b == 0) a / b else null
    else -> null
}

private fun unlimitedIllegalCombineMessage(a: Int, op: String, b: Int): String = when (op) {
    "-" -> "$a needs to be bigger than $b"
    "/" -> "$b doesn't go into $a"
    else -> "That doesn't work"
}

class NumbersUnlimitedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<NumbersUiState>(NumbersUiState.Loading)
    val uiState: StateFlow<NumbersUiState> = _uiState.asStateFlow()

    private val _seed = MutableStateFlow("")
    val seed: StateFlow<String> = _seed.asStateFlow()

    /** True while the seed intro screen should be shown (before the game starts). */
    private val _showIntro = MutableStateFlow(false)
    val showIntro: StateFlow<Boolean> = _showIntro.asStateFlow()

    private val _isNewBestScore = MutableStateFlow(false)
    val isNewBestScore: StateFlow<Boolean> = _isNewBestScore.asStateFlow()

    private val _challengePlayResult = MutableStateFlow<ChallengePlayResultDto?>(null)
    val challengePlayResult: StateFlow<ChallengePlayResultDto?> = _challengePlayResult.asStateFlow()

    private var challengeId: String? = null
    private var isChallengeModeSetUp = false

    private var timerJob: Job? = null
    private var errorMessageJob: Job? = null
    private var nextTileId = 0
    private var originalNumbers = emptyList<Int>()
    private var pendingTarget = 0
    private var pendingSolution: List<NumbersStepDto> = emptyList()

    fun setupChallengeMode(id: String, numbers: List<Int>, target: Int) {
        isChallengeModeSetUp = true
        challengeId = id
        pendingTarget = target
        originalNumbers = numbers
        pendingSolution = emptyList()
        _showIntro.value = false
        _uiState.value = NumbersUiState.Loading
    }

    fun loadPuzzle() {
        if (_seed.value.isNotEmpty() || isChallengeModeSetUp) return
        prepareNewGame()
    }

    fun newGame() {
        timerJob?.cancel()
        prepareNewGame()
    }

    fun setSeed(newCode: String) {
        _seed.value = newCode
        val (target, numbers, solution) = generateSolvablePuzzle(UnlimitedPuzzleGenerator.seedCodeToLong(newCode))
        pendingTarget = target
        originalNumbers = numbers
        pendingSolution = solution
    }

    private fun prepareNewGame() {
        val newCode = UnlimitedPuzzleGenerator.generateSeedCode()
        _seed.value = newCode
        val (target, numbers, solution) = generateSolvablePuzzle(UnlimitedPuzzleGenerator.seedCodeToLong(newCode))
        pendingTarget = target
        originalNumbers = numbers
        pendingSolution = solution
        _showIntro.value = true
        _uiState.value = NumbersUiState.Loading
    }

    /** Generates a puzzle, looping with seed offsets until solvable (almost always first try). */
    private fun generateSolvablePuzzle(baseSeed: Long): Triple<Int, List<Int>, List<NumbersStepDto>> {
        repeat(20) { attempt ->
            val (target, numbers) = UnlimitedPuzzleGenerator.generateNumbersPuzzle(baseSeed + attempt * 1_000_003L)
            val solution = solveNumbers(target, numbers)
            if (solution != null) return Triple(target, numbers, solution)
        }
        val (target, numbers) = UnlimitedPuzzleGenerator.generateNumbersPuzzle(baseSeed)
        return Triple(target, numbers, emptyList())
    }

    /** Collects up to SOLUTION_CAP solutions then returns the most human-like one, or
     *  null if the target is unreachable. Uses memoization on sorted tile multisets to
     *  avoid re-exploring equivalent states.
     */
    private fun solveNumbers(target: Int, numbers: List<Int>): List<NumbersStepDto>? {
        if (target in numbers) return emptyList()
        if (numbers.size < 2) return null

        data class Tile(val value: Int, val steps: List<NumbersStepDto>)

        val solutions = mutableListOf<List<NumbersStepDto>>()
        val seenStates = mutableSetOf<List<Int>>()

        fun visit(tiles: List<Tile>) {
            if (solutions.size >= SOLUTION_CAP) return
            val state = tiles.map { it.value }.sorted()
            if (!seenStates.add(state)) return
            for (tile in tiles) {
                if (tile.value == target && tile.steps.isNotEmpty()) {
                    solutions.add(tile.steps)
                    if (solutions.size >= SOLUTION_CAP) return
                }
            }
            if (tiles.size < 2) return
            for (i in tiles.indices) {
                for (j in i + 1 until tiles.size) {
                    if (solutions.size >= SOLUTION_CAP) return
                    val a = tiles[i]; val b = tiles[j]
                    val rest = tiles.filterIndexed { k, _ -> k != i && k != j }
                    val combined = a.steps + b.steps
                    fun tryOp(left: Int, op: String, right: Int, result: Int) {
                        if (solutions.size < SOLUTION_CAP)
                            visit(rest + Tile(result, combined + NumbersStepDto(left, op, right, result)))
                    }
                    tryOp(a.value, "+", b.value, a.value + b.value)
                    if (a.value > b.value) tryOp(a.value, "-", b.value, a.value - b.value)
                    else if (b.value > a.value) tryOp(b.value, "-", a.value, b.value - a.value)
                    if (a.value > 1 && b.value > 1) tryOp(a.value, "*", b.value, a.value * b.value)
                    if (b.value > 1 && a.value % b.value == 0) tryOp(a.value, "/", b.value, a.value / b.value)
                    if (a.value > 1 && b.value % a.value == 0 && a.value != b.value)
                        tryOp(b.value, "/", a.value, b.value / a.value)
                }
            }
        }

        visit(numbers.map { Tile(it, emptyList()) })
        if (solutions.isEmpty()) return null

        return solutions.minWith(compareBy(
            { it.size },
            { steps -> -steps.mapIndexed { i, s -> if (s.a in BIG_NUMBERS || s.b in BIG_NUMBERS) steps.size - i else 0 }.sum() },
            { steps -> steps.maxOfOrNull { it.result } ?: 0 },
            { steps -> -steps.sumOf { s -> when { s.result % 100 == 0 -> 4; s.result % 50 == 0 -> 3; s.result % 25 == 0 -> 2; s.result % 10 == 0 -> 1; s.result % 5 == 0 -> 0; else -> -1 } } },
        ))
    }

    fun startGame() {
        _showIntro.value = false
        nextTileId = 0
        val tiles = originalNumbers.map { value -> NumbersTile(id = nextTileId++, value = value, steps = emptyList()) }
        val initialBest = tiles.minByOrNull { kotlin.math.abs(pendingTarget - it.value) }
        _uiState.value = NumbersUiState.Playing(
            target = pendingTarget,
            tiles = tiles,
            solution = pendingSolution,
            bestValue = initialBest?.value ?: 0,
            bestSteps = emptyList(),
            secondsRemaining = UNLIMITED_DURATION_SECONDS,
        )
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
                    submitResult()
                    return@launch
                }
                _uiState.value = current.copy(secondsRemaining = current.secondsRemaining - 1)
            }
        }
    }

    fun persistProgress() {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        timerJob?.cancel()
        _uiState.value = state.copy(isPaused = true, selectedTileId = null, pendingOp = null, errorMessage = null)
    }

    fun resume() {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        if (!state.isPaused) return
        _uiState.value = state.copy(isPaused = false)
        startTimer()
    }

    fun finishEarly() {
        timerJob?.cancel()
        submitResult()
    }

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
            SoundFeedback.tap()
            _uiState.value = state.copy(selectedTileId = null, pendingOp = null)
            return
        }
        val a = state.tiles.firstOrNull { it.id == firstId } ?: return
        val b = state.tiles.firstOrNull { it.id == tileId } ?: return
        val result = unlimitedLegalResult(a.value, op, b.value)
        if (result == null) {
            SoundFeedback.incorrect()
            showErrorMessage(unlimitedIllegalCombineMessage(a.value, op, b.value))
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

        if (resultTile.value == state.target) {
            SoundFeedback.correct()
            timerJob?.cancel()
            submitResult()
        } else {
            SoundFeedback.tap()
        }
    }

    fun undo(tileId: Int) {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        val consumed = state.history[tileId] ?: return
        SoundFeedback.tap()
        _uiState.value = state.copy(
            tiles = state.tiles.filterNot { it.id == tileId } + consumed,
            history = state.history - tileId,
        )
    }

    fun reset() {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        val tiles = originalNumbers.map { value -> NumbersTile(id = nextTileId++, value = value, steps = emptyList()) }
        SoundFeedback.tap()
        _uiState.value = state.copy(
            tiles = tiles,
            selectedTileId = null,
            pendingOp = null,
            errorMessage = null,
            history = emptyMap(),
        )
    }

    private fun submitResult() {
        val state = _uiState.value as? NumbersUiState.Playing ?: return
        val elapsed = UNLIMITED_DURATION_SECONDS - state.secondsRemaining
        val cid = challengeId
        if (cid != null) {
            _uiState.value = NumbersUiState.Results(
                target = state.target,
                numbers = originalNumbers,
                resultValue = state.bestValue,
                steps = state.bestSteps,
                solution = pendingSolution,
                distance = state.bestDistance,
                durationSeconds = elapsed,
                rankToday = 0,
                currentStreak = 0,
                puzzleId = cid,
                date = "",
            )
            viewModelScope.launch {
                runCatching {
                    AuthRepository.apiServiceForCurrentSession().submitChallengePlay(
                        cid,
                        ChallengePlayRequestDto(
                            durationSeconds = elapsed,
                            resultValue = state.bestValue,
                            steps = state.bestSteps.map { s ->
                                com.tomdunkley.dailypuzzles.data.network.dto.NumbersStepDto(
                                    a = s.a, op = s.op, b = s.b, result = s.result,
                                )
                            },
                        ),
                    )
                }.onSuccess { result ->
                    _challengePlayResult.value = result
                }
            }
        } else {
            val prevDist = UnlimitedHighScoreStore.numbersBestDistance
            val prevTime = UnlimitedHighScoreStore.numbersBestTimeSeconds
            UnlimitedHighScoreStore.updateNumbersDistance(state.bestDistance, elapsed, _seed.value, state.bestValue, state.bestSteps)
            _isNewBestScore.value = prevDist == -1 || state.bestDistance < prevDist ||
                (state.bestDistance == prevDist && elapsed < prevTime)
            _uiState.value = NumbersUiState.Results(
                target = state.target,
                numbers = originalNumbers,
                resultValue = state.bestValue,
                steps = state.bestSteps,
                solution = pendingSolution,
                distance = state.bestDistance,
                durationSeconds = elapsed,
                rankToday = 0,
                currentStreak = 0,
                puzzleId = "numbers_practice_${_seed.value}",
                date = "",
            )
        }
    }

    val practiceBestDistance: Int get() = UnlimitedHighScoreStore.numbersBestDistance
    val practiceBestTimeSeconds: Int get() = UnlimitedHighScoreStore.numbersBestTimeSeconds
    val practiceBestSeed: String get() = UnlimitedHighScoreStore.numbersBestSeed
    val practiceBestResultValue: Int get() = UnlimitedHighScoreStore.numbersBestResultValue
    val practiceBestSteps: List<NumbersStepDto> get() = UnlimitedHighScoreStore.numbersBestSteps

    fun detailForSeed(seed: String): Triple<Int, List<Int>, List<NumbersStepDto>> {
        val (target, numbers) = UnlimitedPuzzleGenerator.generateNumbersPuzzle(UnlimitedPuzzleGenerator.seedCodeToLong(seed))
        return Triple(target, numbers, solveNumbers(target, numbers) ?: emptyList())
    }

    override fun onCleared() {
        timerJob?.cancel()
        errorMessageJob?.cancel()
    }
}
