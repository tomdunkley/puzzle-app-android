package com.tomdunkley.dailypuzzles.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.network.ApiClient
import com.tomdunkley.dailypuzzles.data.network.dto.AchievementItemDto
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import com.tomdunkley.dailypuzzles.data.trophies.TrophySeenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AchievementsUiState {
    data object Loading : AchievementsUiState
    data class Error(val message: String) : AchievementsUiState
    data class Loaded(
        val achievements: List<AchievementItemDto>,
        val newTrophyIds: Set<String> = emptySet(),
    ) : AchievementsUiState
}

class AchievementsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AchievementsUiState>(AchievementsUiState.Loading)
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = AchievementsUiState.Loading
            runCatching { ApiClient.authenticatedService.getMyAchievements() }
                .onSuccess { dto ->
                    val newIds = TrophySeenStore.currentUnseenIds()
                    _uiState.value = AchievementsUiState.Loaded(dto.achievements, newIds)
                }
                .onFailure { _uiState.value = AchievementsUiState.Error(it.toUserMessage("Couldn't load trophies")) }
        }
    }
}
