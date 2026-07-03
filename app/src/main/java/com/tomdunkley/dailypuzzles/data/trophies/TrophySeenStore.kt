package com.tomdunkley.dailypuzzles.data.trophies

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TrophySeenStore {

    private const val PREFS_NAME = "trophy_unseen"
    private const val KEY_UNSEEN_IDS = "unseen_ids"

    private lateinit var prefs: SharedPreferences

    private val _unseenIds = MutableStateFlow<Set<String>>(emptySet())
    val unseenIds: StateFlow<Set<String>> = _unseenIds.asStateFlow()

    private val _newTrophyCount = MutableStateFlow(0)
    val newTrophyCount: StateFlow<Int> = _newTrophyCount.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(KEY_UNSEEN_IDS, emptySet()) ?: emptySet()
        _unseenIds.value = stored
        _newTrophyCount.value = stored.size
    }

    fun currentUnseenIds(): Set<String> = _unseenIds.value

    fun addUnseen(ids: List<String>) {
        if (ids.isEmpty()) return
        val newSet = _unseenIds.value + ids
        _unseenIds.value = newSet
        _newTrophyCount.value = newSet.size
        prefs.edit().putStringSet(KEY_UNSEEN_IDS, newSet).apply()
    }

    fun markAllSeen() {
        _unseenIds.value = emptySet()
        _newTrophyCount.value = 0
        prefs.edit().remove(KEY_UNSEEN_IDS).apply()
    }
}
