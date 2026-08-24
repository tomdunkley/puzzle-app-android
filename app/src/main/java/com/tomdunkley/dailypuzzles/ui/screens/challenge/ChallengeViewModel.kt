package com.tomdunkley.dailypuzzles.ui.screens.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.challenges.ChallengeGameStore
import com.tomdunkley.dailypuzzles.data.network.dto.ChallengePuzzleDataDto
import com.tomdunkley.dailypuzzles.data.network.dto.ChallengeSummaryGameDto
import com.tomdunkley.dailypuzzles.data.network.dto.CreateChallengeRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.PublicUserProfileDto
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedPuzzleGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ChallengeUiState {
    data object Loading : ChallengeUiState
    data class Error(val message: String) : ChallengeUiState
    data class Loaded(
        val myUserId: String,
        val friendProfile: PublicUserProfileDto,
        val games: List<ChallengeSummaryGameDto>,
    ) : ChallengeUiState
}

sealed interface ChallengeNavEvent {
    data class StartGame(val game: String, val challengeId: String) : ChallengeNavEvent
}

class ChallengeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChallengeUiState>(ChallengeUiState.Loading)
    val uiState: StateFlow<ChallengeUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableStateFlow<ChallengeNavEvent?>(null)
    val navEvent: StateFlow<ChallengeNavEvent?> = _navEvent.asStateFlow()

    private val _isCreatingChallenge = MutableStateFlow(false)
    val isCreatingChallenge: StateFlow<Boolean> = _isCreatingChallenge.asStateFlow()

    private var myUserId: String = ""

    fun load(friendId: String) {
        viewModelScope.launch {
            _uiState.value = ChallengeUiState.Loading
            runCatching {
                val service = AuthRepository.apiServiceForCurrentSession()
                val me = service.getMyProfile()
                val profile = service.getPublicProfile(friendId)
                val summary = service.getFriendChallengeSummary(friendId)
                Triple(me, profile, summary.games)
            }.onSuccess { (me, profile, games) ->
                myUserId = me.userId
                _uiState.value = ChallengeUiState.Loaded(me.userId, profile, games)
            }.onFailure {
                _uiState.value = ChallengeUiState.Error(it.toUserMessage("Couldn't load challenges"))
            }
        }
    }

    fun createChallenge(friendId: String, game: String) {
        if (_isCreatingChallenge.value) return
        viewModelScope.launch {
            _isCreatingChallenge.value = true
            runCatching {
                val service = AuthRepository.apiServiceForCurrentSession()
                val seed = UnlimitedPuzzleGenerator.generateSeedCode()
                val seedLong = UnlimitedPuzzleGenerator.seedCodeToLong(seed)

                val puzzleData = when (game) {
                    "boggle" -> {
                        val board = UnlimitedPuzzleGenerator.generateBoggleBoard(seedLong)
                        ChallengeGameStore.pendingPuzzleData = ChallengeGameStore.PuzzleData.Boggle(board)
                        ChallengePuzzleDataDto(board = board)
                    }
                    "numbers" -> {
                        val (target, numbers) = UnlimitedPuzzleGenerator.generateNumbersPuzzle(seedLong)
                        ChallengeGameStore.pendingPuzzleData = ChallengeGameStore.PuzzleData.Numbers(numbers, target)
                        ChallengePuzzleDataDto(numbers = numbers, target = target)
                    }
                    else -> { // routes
                        val gridSize = listOf(4, 5, 6).random()
                        ChallengeGameStore.pendingPuzzleData = ChallengeGameStore.PuzzleData.Routes(seed, gridSize)
                        ChallengePuzzleDataDto(gridSize = gridSize)
                    }
                }

                ChallengeGameStore.pendingSeed = seed

                val response = service.createChallenge(
                    CreateChallengeRequestDto(
                        friendId = friendId,
                        game = game,
                        seed = seed,
                        puzzleData = puzzleData,
                    )
                )
                response.challengeId
            }.onSuccess { challengeId ->
                val profile = (_uiState.value as? ChallengeUiState.Loaded)?.friendProfile
                ChallengeGameStore.pendingChallengeId = challengeId
                ChallengeGameStore.pendingGame = game
                ChallengeGameStore.pendingMyUserId = myUserId
                ChallengeGameStore.pendingOpponentName = profile?.displayName
                ChallengeGameStore.pendingOpponentAvatarId = profile?.avatarId
                ChallengeGameStore.pendingOpponentAvatarColorId = profile?.avatarColorId
                ChallengeGameStore.pendingOpponentAvatarIconColor = profile?.avatarIconColor
                _navEvent.value = ChallengeNavEvent.StartGame(game, challengeId)
            }.onFailure {
                ChallengeGameStore.clear()
            }
            _isCreatingChallenge.value = false
        }
    }

    fun playExistingChallenge(game: String, challengeId: String, puzzleData: ChallengePuzzleDataDto) {
        when (game) {
            "boggle" -> ChallengeGameStore.pendingPuzzleData =
                ChallengeGameStore.PuzzleData.Boggle(puzzleData.board ?: emptyList())
            "numbers" -> ChallengeGameStore.pendingPuzzleData =
                ChallengeGameStore.PuzzleData.Numbers(puzzleData.numbers ?: emptyList(), puzzleData.target ?: 0)
            "routes" -> ChallengeGameStore.pendingPuzzleData =
                ChallengeGameStore.PuzzleData.Routes(puzzleData.seed ?: "", puzzleData.gridSize ?: 5)
        }
        val profile = (_uiState.value as? ChallengeUiState.Loaded)?.friendProfile
        ChallengeGameStore.pendingChallengeId = challengeId
        ChallengeGameStore.pendingGame = game
        ChallengeGameStore.pendingMyUserId = myUserId
        ChallengeGameStore.pendingSeed = puzzleData.seed
        ChallengeGameStore.pendingOpponentName = profile?.displayName
        ChallengeGameStore.pendingOpponentAvatarId = profile?.avatarId
        ChallengeGameStore.pendingOpponentAvatarColorId = profile?.avatarColorId
        ChallengeGameStore.pendingOpponentAvatarIconColor = profile?.avatarIconColor
        _navEvent.value = ChallengeNavEvent.StartGame(game, challengeId)
    }

    fun clearNavEvent() {
        _navEvent.value = null
    }
}
