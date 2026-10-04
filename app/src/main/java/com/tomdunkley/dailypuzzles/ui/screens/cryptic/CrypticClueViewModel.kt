package com.tomdunkley.dailypuzzles.ui.screens.cryptic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.network.ApiClient
import com.tomdunkley.dailypuzzles.data.network.dto.SubmitCrypticClueRequestDto
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface CrypticClueUiState {
    object Idle : CrypticClueUiState
    object Submitting : CrypticClueUiState
    data class Submitted(val solution: String) : CrypticClueUiState
    data class Error(val message: String) : CrypticClueUiState
}

class CrypticClueViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<CrypticClueUiState>(CrypticClueUiState.Idle)
    val uiState: StateFlow<CrypticClueUiState> = _uiState

    private val _suggestedWords = MutableStateFlow<List<String>>(emptyList())
    val suggestedWords: StateFlow<List<String>> = _suggestedWords

    private val _isFetchingWords = MutableStateFlow(false)
    val isFetchingWords: StateFlow<Boolean> = _isFetchingWords

    private val _wordFetchError = MutableStateFlow<String?>(null)
    val wordFetchError: StateFlow<String?> = _wordFetchError

    fun submitClue(solution: String, clue: String) {
        if (solution.isBlank() || clue.isBlank()) return
        _uiState.value = CrypticClueUiState.Submitting
        viewModelScope.launch {
            runCatching {
                ApiClient.authenticatedService.submitCrypticClue(
                    SubmitCrypticClueRequestDto(solution.trim().uppercase(), clue.trim())
                )
            }.onSuccess {
                _uiState.value = CrypticClueUiState.Submitted(solution.trim().uppercase())
            }.onFailure {
                _uiState.value = CrypticClueUiState.Error(it.toUserMessage("Failed to submit clue"))
            }
        }
    }

    fun resetState() {
        _uiState.value = CrypticClueUiState.Idle
    }

    fun fetchWordSuggestions() {
        _isFetchingWords.value = true
        _wordFetchError.value = null
        viewModelScope.launch {
            runCatching {
                ApiClient.authenticatedService.suggestCrypticWords()
            }.onSuccess { response ->
                _suggestedWords.value = response.words
            }.onFailure {
                _wordFetchError.value = it.toUserMessage("Failed to fetch suggestions")
            }
            _isFetchingWords.value = false
        }
    }
}
