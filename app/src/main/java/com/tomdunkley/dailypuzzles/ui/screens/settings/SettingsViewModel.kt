package com.tomdunkley.dailypuzzles.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.boggle.BoggleProgressStore
import com.tomdunkley.dailypuzzles.data.developer.DeveloperStore
import com.tomdunkley.dailypuzzles.data.lines.LinesProgressStore
import com.tomdunkley.dailypuzzles.data.network.ApiClient
import com.tomdunkley.dailypuzzles.data.unlimited.UnlimitedHighScoreStore
import com.tomdunkley.dailypuzzles.data.network.dto.AchievementSummaryDto
import com.tomdunkley.dailypuzzles.data.network.dto.UpdateProfileRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.UserProfileDto
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import com.tomdunkley.dailypuzzles.data.numbers.NumbersProgressStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Error(val message: String) : SettingsUiState
    data class Loaded(
        val profile: UserProfileDto,
        val unlockedAchievementAvatarIds: Set<String> = emptySet(),
        val unlockedAchievementColorIds: Set<String> = emptySet(),
        val unlockedAchievementIconColorIds: Set<String> = emptySet(),
        val unlockedAchievementCount: Int = 0,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
    ) : SettingsUiState
}

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            val profileResult = runCatching { ApiClient.authenticatedService.getMyProfile() }
            val profile = profileResult.getOrElse {
                _uiState.value = SettingsUiState.Error(it.toUserMessage("Couldn't load your profile"))
                return@launch
            }
            val achievementItems = runCatching { ApiClient.authenticatedService.getMyAchievements() }
                .getOrNull()?.achievements ?: emptyList()
            val unlockedAvatarIds = achievementItems
                .filter { it.unlocked }.mapNotNull { it.unlocksAvatarId }.toSet()
            val unlockedColorIds = achievementItems
                .filter { it.unlocked }.mapNotNull { it.unlocksColorId }.toSet()
            val unlockedIconColorIds = achievementItems
                .filter { it.unlocked }.mapNotNull { it.unlocksIconColorId }.toSet()
            DeveloperStore.isDeveloper = profile.isDeveloper
            _uiState.value = SettingsUiState.Loaded(
                profile = profile,
                unlockedAchievementAvatarIds = unlockedAvatarIds,
                unlockedAchievementColorIds = unlockedColorIds,
                unlockedAchievementIconColorIds = unlockedIconColorIds,
                unlockedAchievementCount = achievementItems.count { it.unlocked },
            )
        }
    }

    fun updateDisplayName(newName: String) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                ApiClient.authenticatedService.updateMyProfile(UpdateProfileRequestDto(displayName = newName))
            }
                .onSuccess { profile ->
                    AuthRepository.updateDisplayName(profile.displayName)
                    _uiState.value = state.copy(profile = profile, isSaving = false)
                }
                .onFailure {
                    _uiState.value = state.copy(
                        isSaving = false,
                        errorMessage = it.toUserMessage("Couldn't update display name"),
                    )
                }
        }
    }

    fun updateAvatar(avatarId: String) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                ApiClient.authenticatedService.updateMyProfile(UpdateProfileRequestDto(avatarId = avatarId))
            }
                .onSuccess { profile -> _uiState.value = state.copy(profile = profile, isSaving = false) }
                .onFailure {
                    _uiState.value = state.copy(
                        isSaving = false,
                        errorMessage = it.toUserMessage("Couldn't update avatar"),
                    )
                }
        }
    }

    fun updateAvatarIconColor(iconColor: String) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                ApiClient.authenticatedService.updateMyProfile(UpdateProfileRequestDto(avatarIconColor = iconColor))
            }
                .onSuccess { profile -> _uiState.value = state.copy(profile = profile, isSaving = false) }
                .onFailure {
                    _uiState.value = state.copy(
                        isSaving = false,
                        errorMessage = it.toUserMessage("Couldn't update icon colour"),
                    )
                }
        }
    }

    fun updateAvatarColor(avatarColorId: String) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                ApiClient.authenticatedService.updateMyProfile(UpdateProfileRequestDto(avatarColorId = avatarColorId))
            }
                .onSuccess { profile -> _uiState.value = state.copy(profile = profile, isSaving = false) }
                .onFailure {
                    _uiState.value = state.copy(
                        isSaving = false,
                        errorMessage = it.toUserMessage("Couldn't update avatar color"),
                    )
                }
        }
    }

    fun updateVisibleOnGlobalLeaderboard(visible: Boolean) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching {
                ApiClient.authenticatedService.updateMyProfile(
                    UpdateProfileRequestDto(visibleOnGlobalLeaderboard = visible),
                )
            }
                .onSuccess { profile -> _uiState.value = state.copy(profile = profile, isSaving = false) }
                .onFailure {
                    _uiState.value = state.copy(
                        isSaving = false,
                        errorMessage = it.toUserMessage("Couldn't update leaderboard visibility"),
                    )
                }
        }
    }

    fun unlockAllAchievements() {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { ApiClient.authenticatedService.unlockAllAchievements() }
                .onSuccess { load() }
                .onFailure {
                    _uiState.value = state.copy(
                        isSaving = false,
                        errorMessage = it.toUserMessage("Couldn't unlock trophies"),
                    )
                }
        }
    }

    fun resetAchievements() {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { ApiClient.authenticatedService.resetAchievements() }
                .onSuccess { load() }
                .onFailure {
                    _uiState.value = state.copy(
                        isSaving = false,
                        errorMessage = it.toUserMessage("Couldn't reset trophies"),
                    )
                }
        }
    }

    fun resetPracticeHighScores() {
        UnlimitedHighScoreStore.reset()
    }

    fun resetDevProgress() {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { ApiClient.authenticatedService.resetDevProgress() }
                .onSuccess {
                    // The server-side score rows are gone, but these on-device caches would
                    // otherwise keep showing today's puzzles as already played/in-progress.
                    BoggleProgressStore.clearAll()
                    NumbersProgressStore.clearAll()
                    LinesProgressStore.clearAll()
                    _uiState.value = state.copy(isSaving = false)
                }
                .onFailure {
                    _uiState.value = state.copy(
                        isSaving = false,
                        errorMessage = it.toUserMessage("Couldn't reset progress"),
                    )
                }
        }
    }

    fun setPassword(newPassword: String) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            AuthRepository.setPassword(newPassword)
                .onSuccess { load() }
                .onFailure {
                    _uiState.value = state.copy(
                        isSaving = false,
                        errorMessage = it.toUserMessage("Couldn't set up a password"),
                    )
                }
        }
    }

    fun deleteAccount() {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { ApiClient.authenticatedService.deleteAccount() }
                .onSuccess { AuthRepository.signOut() }
                .onFailure {
                    _uiState.value = state.copy(
                        isSaving = false,
                        errorMessage = it.toUserMessage("Couldn't delete account"),
                    )
                }
        }
    }

    fun reportError(message: String) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(errorMessage = message)
    }

    fun linkGoogleAccount(idToken: String) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            AuthRepository.linkGoogleAccount(idToken)
                .onSuccess { load() }
                .onFailure {
                    _uiState.value = state.copy(
                        isSaving = false,
                        errorMessage = it.toUserMessage("Couldn't link that Google account"),
                    )
                }
        }
    }
}
