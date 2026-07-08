package com.tomdunkley.dailypuzzles.ui.screens.boggle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.audio.SoundFeedback
import com.tomdunkley.dailypuzzles.data.boggle.BoggleDictionary
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedHighScoreStore
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedPuzzleGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val UNLIMITED_DURATION_SECONDS = 90

class BoggleUnlimitedViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<BoggleUiState>(BoggleUiState.Loading)
    val uiState: StateFlow<BoggleUiState> = _uiState.asStateFlow()

    private val _seed = MutableStateFlow("")
    val seed: StateFlow<String> = _seed.asStateFlow()

    /** True while the seed intro screen should be shown (before the game starts). */
    private val _showIntro = MutableStateFlow(false)
    val showIntro: StateFlow<Boolean> = _showIntro.asStateFlow()

    private val _isNewBestScore = MutableStateFlow(false)
    val isNewBestScore: StateFlow<Boolean> = _isNewBestScore.asStateFlow()

    /** Board from the most recently completed game, for the VIEW BOARD button. */
    private val _boardForResults = MutableStateFlow<List<String>>(emptyList())
    val boardForResults: StateFlow<List<String>> = _boardForResults.asStateFlow()

    private var currentBoard = emptyList<String>()
    private var timerJob: Job? = null

    fun loadPuzzle() {
        if (_seed.value.isNotEmpty()) return
        prepareNewGame()
    }

    fun newGame() {
        timerJob?.cancel()
        prepareNewGame()
    }

    fun setSeed(newCode: String) {
        _seed.value = newCode
        currentBoard = UnlimitedPuzzleGenerator.generateBoggleBoard(UnlimitedPuzzleGenerator.seedCodeToLong(newCode))
    }

    private fun prepareNewGame() {
        val newCode = UnlimitedPuzzleGenerator.generateSeedCode()
        _seed.value = newCode
        currentBoard = UnlimitedPuzzleGenerator.generateBoggleBoard(UnlimitedPuzzleGenerator.seedCodeToLong(newCode))
        _showIntro.value = true
        _uiState.value = BoggleUiState.Loading
    }

    fun startGame() {
        _showIntro.value = false
        _uiState.value = BoggleUiState.Playing(board = currentBoard, secondsRemaining = UNLIMITED_DURATION_SECONDS)
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _uiState.value as? BoggleUiState.Playing ?: return@launch
                if (current.isPaused) continue
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
        val state = _uiState.value as? BoggleUiState.Playing ?: return
        timerJob?.cancel()
        _uiState.value = state.copy(isPaused = true, selectedPath = emptyList(), wordFeedback = null)
    }

    fun resume() {
        val state = _uiState.value as? BoggleUiState.Playing ?: return
        if (!state.isPaused) return
        _uiState.value = state.copy(isPaused = false)
        startTimer()
    }

    fun finishEarly() {
        timerJob?.cancel()
        submitResult()
    }

    fun onCellDragged(index: Int) {
        val state = _uiState.value as? BoggleUiState.Playing ?: return
        if (state.isPaused) return
        val path = state.selectedPath
        when {
            path.isEmpty() -> _uiState.value = state.copy(selectedPath = listOf(index), wordFeedback = null)
            path.last() == index -> Unit
            index in path -> {
                val cutIdx = path.indexOf(index)
                _uiState.value = state.copy(selectedPath = path.subList(0, cutIdx + 1))
            }
            areAdjacent(path.last(), index) -> {
                _uiState.value = state.copy(selectedPath = path + index)
            }
        }
    }

    fun onDragEnded() {
        val state = _uiState.value as? BoggleUiState.Playing ?: return
        val path = state.selectedPath
        if (path.isEmpty()) return
        val word = path.joinToString("") { state.board[it] }
        _uiState.value = state.copy(selectedPath = emptyList())

        when {
            word.length < 4 -> {
                SoundFeedback.incorrect()
                showWordFeedback(word, WordFeedbackType.TOO_SHORT)
            }
            word in state.foundWords -> {
                SoundFeedback.incorrect()
                showWordFeedback(word, WordFeedbackType.DUPLICATE)
            }
            else -> {
                viewModelScope.launch {
                    val isValid = BoggleDictionary.contains(word)
                    val current = _uiState.value as? BoggleUiState.Playing ?: return@launch
                    if (isValid && word !in current.foundWords) {
                        SoundFeedback.correct()
                        _uiState.value = current.copy(
                            foundWords = current.foundWords + word,
                            wordFeedback = WordFeedback(word, WordFeedbackType.CORRECT),
                        )
                        delay(800)
                        val after = _uiState.value as? BoggleUiState.Playing ?: return@launch
                        if (after.wordFeedback?.word == word && after.wordFeedback.type == WordFeedbackType.CORRECT) {
                            _uiState.value = after.copy(wordFeedback = null)
                        }
                    } else if (!isValid) {
                        SoundFeedback.incorrect()
                        showWordFeedback(word, WordFeedbackType.INCORRECT)
                    }
                }
            }
        }
    }

    private fun showWordFeedback(word: String, type: WordFeedbackType) {
        val current = _uiState.value as? BoggleUiState.Playing ?: return
        _uiState.value = current.copy(wordFeedback = WordFeedback(word, type))
        viewModelScope.launch {
            delay(1000)
            val after = _uiState.value as? BoggleUiState.Playing ?: return@launch
            if (after.wordFeedback?.word == word) {
                _uiState.value = after.copy(wordFeedback = null)
            }
        }
    }

    private fun submitResult() {
        val state = _uiState.value as? BoggleUiState.Playing ?: return
        _boardForResults.value = currentBoard
        val score = estimatedScoreForWords(state.foundWords)
        val prevHighScore = UnlimitedHighScoreStore.boggleHighScore
        UnlimitedHighScoreStore.updateBoggleScore(score, _seed.value, state.foundWords)
        _isNewBestScore.value = prevHighScore == -1 || score > prevHighScore
        _uiState.value = BoggleUiState.Results(
            score = score,
            validWords = state.foundWords,
            rankToday = 0,
            currentStreak = 0,
            puzzleId = "boggle_practice_${_seed.value}",
            date = "",
        )
    }

    val practiceHighScore: Int get() = UnlimitedHighScoreStore.boggleHighScore
    val practiceHighScoreSeed: String get() = UnlimitedHighScoreStore.boggleHighScoreSeed

    fun highScoreBoard(): List<String> {
        val seed = UnlimitedHighScoreStore.boggleHighScoreSeed
        return if (seed.isNotEmpty()) {
            UnlimitedPuzzleGenerator.generateBoggleBoard(UnlimitedPuzzleGenerator.seedCodeToLong(seed))
        } else emptyList()
    }

    fun highScoreWords(): List<String> = UnlimitedHighScoreStore.boggleHighScoreWords

    override fun onCleared() {
        timerJob?.cancel()
    }
}
