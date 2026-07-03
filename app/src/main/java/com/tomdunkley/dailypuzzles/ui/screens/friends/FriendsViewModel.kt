package com.tomdunkley.dailypuzzles.ui.screens.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.network.ApiClient
import com.tomdunkley.dailypuzzles.data.network.dto.FriendRequestSummaryDto
import com.tomdunkley.dailypuzzles.data.network.dto.FriendSummaryDto
import com.tomdunkley.dailypuzzles.data.network.dto.SendFriendRequestDto
import com.tomdunkley.dailypuzzles.data.network.dto.UserSearchResultDto
import com.tomdunkley.dailypuzzles.data.network.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FriendsUiState {
    data object Loading : FriendsUiState
    data class Error(val message: String) : FriendsUiState
    data class Loaded(
        val friends: List<FriendSummaryDto> = emptyList(),
        val incomingRequests: List<FriendRequestSummaryDto> = emptyList(),
        val outgoingRequests: List<FriendRequestSummaryDto> = emptyList(),
        val searchQuery: String = "",
        val searchResults: List<UserSearchResultDto> = emptyList(),
        val isSearching: Boolean = false,
    ) : FriendsUiState
}

class FriendsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<FriendsUiState>(FriendsUiState.Loading)
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun loadFriendsData() {
        viewModelScope.launch {
            _uiState.value = FriendsUiState.Loading
            runCatching {
                val friends = ApiClient.authenticatedService.getFriends()
                val incoming = ApiClient.authenticatedService.getIncomingRequests()
                val outgoing = ApiClient.authenticatedService.getOutgoingRequests()
                Triple(friends, incoming, outgoing)
            }.onSuccess { (friends, incoming, outgoing) ->
                _uiState.value = FriendsUiState.Loaded(
                    friends = friends,
                    incomingRequests = incoming,
                    outgoingRequests = outgoing,
                )
            }.onFailure {
                _uiState.value = FriendsUiState.Error(it.toUserMessage("Couldn't load your friends"))
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val state = _uiState.value as? FriendsUiState.Loaded ?: return
        _uiState.value = state.copy(searchQuery = query)

        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = state.copy(searchQuery = query, searchResults = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            val loading = _uiState.value as? FriendsUiState.Loaded ?: return@launch
            _uiState.value = loading.copy(isSearching = true)
            runCatching { ApiClient.authenticatedService.searchUsers(query) }
                .onSuccess { results ->
                    val current = _uiState.value as? FriendsUiState.Loaded ?: return@onSuccess
                    _uiState.value = current.copy(searchResults = results, isSearching = false)
                }
                .onFailure {
                    val current = _uiState.value as? FriendsUiState.Loaded ?: return@onFailure
                    _uiState.value = current.copy(isSearching = false)
                }
        }
    }

    fun sendRequest(userId: String) {
        viewModelScope.launch {
            runCatching { ApiClient.authenticatedService.sendFriendRequest(SendFriendRequestDto(userId)) }
                .onSuccess { refreshAfterMutation() }
        }
    }

    fun acceptRequest(userId: String) {
        viewModelScope.launch {
            runCatching { ApiClient.authenticatedService.acceptFriendRequest(userId) }
                .onSuccess { refreshAfterMutation() }
        }
    }

    fun declineRequest(userId: String) {
        viewModelScope.launch {
            runCatching { ApiClient.authenticatedService.declineFriendRequest(userId) }
                .onSuccess { refreshAfterMutation() }
        }
    }

    fun unfriend(userId: String) {
        // Removed optimistically -- waiting on the round-trip-and-refetch left the row
        // visible until the tab was left and revisited, even though the backend had
        // already applied it.
        val state = _uiState.value as? FriendsUiState.Loaded ?: return
        _uiState.value = state.copy(friends = state.friends.filterNot { it.userId == userId })
        viewModelScope.launch {
            runCatching { ApiClient.authenticatedService.unfriend(userId) }
                .onSuccess { refreshAfterMutation() }
                .onFailure { loadFriendsData() }
        }
    }

    /** Re-fetches friends/requests, keeping the current search query/results as-is so a
     * search-result action (e.g. tapping "Add") doesn't blow away what's on screen.
     */
    private fun refreshAfterMutation() {
        val previous = _uiState.value as? FriendsUiState.Loaded
        viewModelScope.launch {
            runCatching {
                val friends = ApiClient.authenticatedService.getFriends()
                val incoming = ApiClient.authenticatedService.getIncomingRequests()
                val outgoing = ApiClient.authenticatedService.getOutgoingRequests()
                Triple(friends, incoming, outgoing)
            }.onSuccess { (friends, incoming, outgoing) ->
                _uiState.value = FriendsUiState.Loaded(
                    friends = friends,
                    incomingRequests = incoming,
                    outgoingRequests = outgoing,
                    searchQuery = previous?.searchQuery ?: "",
                    searchResults = previous?.searchResults ?: emptyList(),
                )
                // Re-run the search so status chips (e.g. "Requested" -> "Friends") update too.
                previous?.searchQuery?.takeIf { it.isNotBlank() }?.let { onSearchQueryChanged(it) }
            }
        }
    }
}
