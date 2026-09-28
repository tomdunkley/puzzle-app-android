package com.tomdunkley.dailypuzzles.data.challenges

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CompletedChallengesStore {

    private const val PREFS_NAME = "seen_challenges"
    private const val KEY_SEEN_IDS = "seen_ids"

    private lateinit var prefs: SharedPreferences
    private var seenIds: MutableSet<String> = mutableSetOf()

    private val _unseenCount = MutableStateFlow(0)
    val unseenCount: StateFlow<Int> = _unseenCount.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        seenIds = prefs.getStringSet(KEY_SEEN_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
    }

    // Called after ChallengesViewModel loads — all lastChallengeId values currently on the server
    fun updateFromLoad(allCompletedIds: List<String>) {
        _unseenCount.value = allCompletedIds.count { !seenIds.contains(it) }
    }

    fun markSeen(challengeId: String) {
        if (seenIds.add(challengeId)) {
            prefs.edit().putStringSet(KEY_SEEN_IDS, seenIds.toSet()).apply()
            _unseenCount.value = (_unseenCount.value - 1).coerceAtLeast(0)
        }
    }
}
