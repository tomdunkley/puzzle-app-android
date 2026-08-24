package com.tomdunkley.dailypuzzles.data.challenges

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PendingChallengesStore {
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _byFriend = MutableStateFlow<Map<String, Int>>(emptyMap())
    val byFriend: StateFlow<Map<String, Int>> = _byFriend.asStateFlow()

    fun update(count: Int, byFriend: Map<String, Int>) {
        _pendingCount.value = count
        _byFriend.value = byFriend
    }

    fun pendingCountForFriend(friendId: String): Int = _byFriend.value[friendId] ?: 0
}
