package com.tomdunkley.dailypuzzles.ui.screens.challenges

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.challenges.ChallengeGameStore
import com.tomdunkley.dailypuzzles.data.challenges.CompletedChallengesStore
import com.tomdunkley.dailypuzzles.data.challenges.InProgressChallengeStore
import com.tomdunkley.dailypuzzles.data.challenges.PendingChallengesStore
import com.tomdunkley.dailypuzzles.data.network.dto.ChallengePuzzleDataDto
import com.tomdunkley.dailypuzzles.data.network.dto.ChallengeSummaryGameDto
import com.tomdunkley.dailypuzzles.data.network.dto.CreateChallengeRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.FriendSummaryDto
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedPuzzleGenerator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendChallengeData(
    val friend: FriendSummaryDto,
    val games: List<ChallengeSummaryGameDto>,
)

sealed interface ChallengesNavEvent {
    data class StartGame(val game: String) : ChallengesNavEvent
}

sealed interface ChallengesUiState {
    data object Loading : ChallengesUiState
    data class Error(val message: String) : ChallengesUiState
    data class Loaded(
        val myUserId: String,
        val myDisplayName: String,
        val myAvatarId: String?,
        val myAvatarColorId: String?,
        val myAvatarIconColor: String?,
        val friends: List<FriendSummaryDto>,
        val challengeData: List<FriendChallengeData>,
    ) : ChallengesUiState
}

class ChallengesViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChallengesUiState>(ChallengesUiState.Loading)
    val uiState: StateFlow<ChallengesUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableStateFlow<ChallengesNavEvent?>(null)
    val navEvent: StateFlow<ChallengesNavEvent?> = _navEvent.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val myUserId: String get() = (_uiState.value as? ChallengesUiState.Loaded)?.myUserId ?: ""

    var myTurnExpanded by mutableStateOf(true)
    var waitingExpanded by mutableStateOf(false)
    var previousExpanded by mutableStateOf(false)
    val expandedCards = mutableStateMapOf<String, Boolean>()

    fun clearNavEvent() { _navEvent.value = null }

    fun playExistingChallenge(game: String, challengeId: String, puzzleData: ChallengePuzzleDataDto, friend: FriendSummaryDto) {
        when (game) {
            "boggle" -> ChallengeGameStore.pendingPuzzleData =
                ChallengeGameStore.PuzzleData.Boggle(puzzleData.board ?: emptyList())
            "numbers" -> ChallengeGameStore.pendingPuzzleData =
                ChallengeGameStore.PuzzleData.Numbers(puzzleData.numbers ?: emptyList(), puzzleData.target ?: 0)
            else -> ChallengeGameStore.pendingPuzzleData =
                ChallengeGameStore.PuzzleData.Routes(puzzleData.seed ?: "", puzzleData.gridSize ?: 5)
        }
        ChallengeGameStore.pendingChallengeId = challengeId
        ChallengeGameStore.pendingGame = game
        ChallengeGameStore.pendingMyUserId = myUserId
        ChallengeGameStore.pendingSeed = puzzleData.seed
        ChallengeGameStore.pendingOpponentName = friend.displayName
        ChallengeGameStore.pendingOpponentAvatarId = friend.avatarId
        ChallengeGameStore.pendingOpponentAvatarColorId = friend.avatarColorId
        ChallengeGameStore.pendingOpponentAvatarIconColor = friend.avatarIconColor
        _navEvent.value = ChallengesNavEvent.StartGame(game)
    }

    fun resumeChallenge(game: String) {
        InProgressChallengeStore.loadIntoChallengeGameStore()
        _navEvent.value = ChallengesNavEvent.StartGame(game)
    }

    fun createRematch(friend: FriendSummaryDto, game: String) {
        if (_isCreating.value) return
        viewModelScope.launch {
            _isCreating.value = true
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
                    else -> {
                        val gridSize = listOf(4, 5, 6).random()
                        ChallengeGameStore.pendingPuzzleData = ChallengeGameStore.PuzzleData.Routes(seed, gridSize)
                        ChallengePuzzleDataDto(gridSize = gridSize)
                    }
                }
                ChallengeGameStore.pendingSeed = seed
                val response = service.createChallenge(
                    CreateChallengeRequestDto(friendId = friend.userId, game = game, seed = seed, puzzleData = puzzleData)
                )
                response.challengeId
            }.onSuccess { challengeId ->
                ChallengeGameStore.pendingChallengeId = challengeId
                ChallengeGameStore.pendingGame = game
                ChallengeGameStore.pendingMyUserId = myUserId
                ChallengeGameStore.pendingOpponentName = friend.displayName
                ChallengeGameStore.pendingOpponentAvatarId = friend.avatarId
                ChallengeGameStore.pendingOpponentAvatarColorId = friend.avatarColorId
                ChallengeGameStore.pendingOpponentAvatarIconColor = friend.avatarIconColor
                _navEvent.value = ChallengesNavEvent.StartGame(game)
            }.onFailure {
                ChallengeGameStore.clear()
            }
            _isCreating.value = false
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = ChallengesUiState.Loading
            runCatching {
                val service = AuthRepository.apiServiceForCurrentSession()
                val meDeferred = async { service.getMyProfile() }
                val friends = async { service.getFriends() }
                val meProfile = meDeferred.await()
                val friendList = friends.await()
                val challengeData = friendList.map { friend ->
                    async {
                        val games = runCatching {
                            service.getFriendChallengeSummary(friend.userId).games
                        }.getOrDefault(emptyList())
                        FriendChallengeData(friend, games)
                    }
                }.awaitAll()
                Pair(meProfile, Pair(friendList, challengeData))
            }.onSuccess { (meProfile, rest) ->
                val (friendList, challengeData) = rest
                val completedIds = challengeData.flatMap { fcd ->
                    fcd.games.mapNotNull { it.lastChallengeId }
                }
                CompletedChallengesStore.updateFromLoad(completedIds)
                val pendingCount = challengeData.sumOf { fcd -> fcd.games.count { it.status == "open" } }
                val pendingByFriend = challengeData.associate { fcd ->
                    fcd.friend.userId to fcd.games.count { it.status == "open" }
                }.filterValues { it > 0 }
                PendingChallengesStore.update(pendingCount, pendingByFriend)
                _uiState.value = ChallengesUiState.Loaded(
                    myUserId = meProfile.userId,
                    myDisplayName = meProfile.displayName,
                    myAvatarId = meProfile.avatarId,
                    myAvatarColorId = meProfile.avatarColorId,
                    myAvatarIconColor = meProfile.avatarIconColor,
                    friends = friendList,
                    challengeData = challengeData,
                )
            }.onFailure {
                _uiState.value = ChallengesUiState.Error(it.toUserMessage("Couldn't load challenges"))
            }
        }
    }
}
