package com.tomdunkley.dailypuzzles.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.auth.AuthRepository
import com.tomdunkley.dailypuzzles.data.network.ApiClient
import com.tomdunkley.dailypuzzles.data.network.dto.PublicUserProfileDto
import com.tomdunkley.dailypuzzles.data.network.dto.SendFriendRequestDto
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UserProfileUiState {
    data object Loading : UserProfileUiState
    data class Loaded(val profile: PublicUserProfileDto) : UserProfileUiState
    data class Error(val message: String) : UserProfileUiState
}

class UserProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private var userId: String = ""

    fun load(userId: String) {
        this.userId = userId
        viewModelScope.launch {
            _uiState.value = UserProfileUiState.Loading
            val service = AuthRepository.apiServiceForExistingSession()
            if (service == null) {
                _uiState.value = UserProfileUiState.Error("Sign in to view player profiles")
                return@launch
            }
            runCatching { service.getPublicProfile(userId) }
                .onSuccess { _uiState.value = UserProfileUiState.Loaded(it) }
                .onFailure { _uiState.value = UserProfileUiState.Error(it.toUserMessage("Couldn't load profile")) }
        }
    }

    fun addFriend() {
        viewModelScope.launch {
            runCatching {
                ApiClient.authenticatedService
                    .sendFriendRequest(SendFriendRequestDto(toUserId = userId))
            }.onSuccess { load(userId) }
        }
    }

    fun removeFriend() {
        viewModelScope.launch {
            runCatching {
                ApiClient.authenticatedService.unfriend(userId)
            }.onSuccess { load(userId) }
        }
    }
}
