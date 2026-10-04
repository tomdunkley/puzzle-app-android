package com.tomdunkley.dailypuzzles.ui.screens.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.network.dto.ScoreDetailDto
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ChallengeResultUiState {
    data object Idle : ChallengeResultUiState
    data object Loading : ChallengeResultUiState
    data class Loaded(
        val myResult: ScoreDetailDto,
        val opponentResult: ScoreDetailDto,
        val outcome: String,
    ) : ChallengeResultUiState
    data class Error(val message: String) : ChallengeResultUiState
}

class ChallengeWaitingViewModel : ViewModel() {

    private val _resultState = MutableStateFlow<ChallengeResultUiState>(ChallengeResultUiState.Idle)
    val resultState: StateFlow<ChallengeResultUiState> = _resultState.asStateFlow()

    fun loadResult(challengeId: String, myUserId: String) {
        if (_resultState.value !is ChallengeResultUiState.Idle) return
        viewModelScope.launch {
            _resultState.value = ChallengeResultUiState.Loading
            runCatching {
                val service = AuthRepository.apiServiceForCurrentSession()
                val myResult = service.getChallengeResult(challengeId, myUserId)
                val opponentUserId = myResult.opponentUserId
                    ?: error("Challenge result missing opponent_user_id")
                val opponentResult = async { service.getChallengeResult(challengeId, opponentUserId) }
                Pair(myResult, opponentResult.await())
            }.onSuccess { (myResult, opponentResult) ->
                val outcome = computeOutcome(myResult, opponentResult)
                _resultState.value = ChallengeResultUiState.Loaded(myResult, opponentResult, outcome)
            }.onFailure {
                _resultState.value = ChallengeResultUiState.Error(it.toUserMessage("Couldn't load results"))
            }
        }
    }

    private fun computeOutcome(myResult: ScoreDetailDto, opponentResult: ScoreDetailDto): String {
        val game = myResult.game
        val myKey = rankingKey(game, myResult)
        val oppKey = rankingKey(game, opponentResult)
        val cmp = compareValuesBy(myKey, oppKey, { it.first }, { it.second })
        return when {
            cmp > 0 -> "win"
            cmp < 0 -> "loss"
            else -> "draw"
        }
    }

    // Returns (primary, secondary) where higher is better.
    private fun rankingKey(game: String, result: ScoreDetailDto): Pair<Int, Int> {
        return when (game) {
            "numbers" -> {
                val distance = result.distance ?: Int.MAX_VALUE
                val duration = result.durationSeconds ?: Int.MAX_VALUE
                if (distance == 0) Pair(0, -duration) else Pair(-distance, 0)
            }
            "routes" -> Pair(-(result.durationSeconds ?: Int.MAX_VALUE), 0)
            else -> Pair(result.score ?: 0, 0)
        }
    }
}
