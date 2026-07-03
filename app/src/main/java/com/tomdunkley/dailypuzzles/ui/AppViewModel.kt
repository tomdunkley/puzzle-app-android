package com.tomdunkley.dailypuzzles.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomdunkley.dailypuzzles.data.network.ApiClient
import com.tomdunkley.dailypuzzles.data.trophies.TrophySeenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {

    private val _hasPendingFriendRequests = MutableStateFlow(false)
    val hasPendingFriendRequests: StateFlow<Boolean> = _hasPendingFriendRequests.asStateFlow()

    val newTrophyCount: StateFlow<Int> = TrophySeenStore.newTrophyCount

    fun refreshFriendRequestBadge() {
        viewModelScope.launch {
            runCatching { ApiClient.authenticatedService.getIncomingRequests() }
                .onSuccess { _hasPendingFriendRequests.value = it.isNotEmpty() }
                .onFailure { _hasPendingFriendRequests.value = false }
        }
    }
}
