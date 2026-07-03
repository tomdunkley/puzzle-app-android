package com.tomdunkley.dailypuzzles.ui.screens.scoredetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.network.dto.ScoreDetailDto
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScoreDetailUiState {
    data object Loading : ScoreDetailUiState
    data class Error(val message: String) : ScoreDetailUiState
    data class Loaded(val detail: ScoreDetailDto, val isOwnScore: Boolean) : ScoreDetailUiState
}

class ScoreDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ScoreDetailUiState>(ScoreDetailUiState.Loading)
    val uiState: StateFlow<ScoreDetailUiState> = _uiState.asStateFlow()

    fun load(puzzleId: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = ScoreDetailUiState.Loading
            runCatching {
                val service = AuthRepository.apiServiceForCurrentSession()
                val me = service.getMyProfile()
                val detail = service.getScoreDetail(puzzleId, userId)
                detail to (me.userId == userId)
            }.onSuccess { (detail, isOwnScore) ->
                _uiState.value = ScoreDetailUiState.Loaded(detail, isOwnScore)
            }.onFailure {
                _uiState.value = ScoreDetailUiState.Error(it.toUserMessage("Couldn't load that score"))
            }
        }
    }
}
