package com.tomdunkley.dailypuzzles.data

import android.content.Context
import android.content.SharedPreferences

object TutorialStore {

    // TODO: set to false to restore "show once" behavior after testing
    private const val ALWAYS_SHOW = true

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("tutorials", Context.MODE_PRIVATE)
    }

    fun hasSeenTutorial(game: String): Boolean {
        if (ALWAYS_SHOW) return false
        return prefs.getBoolean("seen_$game", false)
    }

    fun markSeen(game: String) {
        prefs.edit().putBoolean("seen_$game", true).apply()
    }
}
