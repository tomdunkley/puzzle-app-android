package com.tomdunkley.dailypuzzles.data.roots

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RootsCell(val row: Int, val col: Int)

@Serializable
data class RootsProgress(
    val puzzleId: String,
    val gridSize: Int,
    val startCell: RootsCell,
    val endCell: RootsCell,
    val solution: List<RootsCell>,
    val rowClues: List<Int>,
    val colClues: List<Int>,
    val currentPath: List<RootsCell>,
    val crossMarkers: List<RootsCell>,
    val tickMarkers: List<RootsCell> = emptyList(),
    val elapsedSeconds: Int,
    val isPaused: Boolean,
)

@Serializable
data class RootsResult(
    val puzzleId: String,
    val date: String,
    val gridSize: Int,
    val durationSeconds: Int,
    val rankToday: Int,
    val currentStreak: Int,
)

object RootsProgressStore {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("roots_progress", Context.MODE_PRIVATE)
    }

    fun load(puzzleId: String): RootsProgress? {
        val json = prefs.getString(KEY_PROGRESS, null) ?: return null
        val progress = runCatching { Json.decodeFromString<RootsProgress>(json) }.getOrNull() ?: return null
        return progress.takeIf { it.puzzleId == puzzleId }
    }

    fun save(progress: RootsProgress) {
        prefs.edit().putString(KEY_PROGRESS, Json.encodeToString(progress)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_PROGRESS).apply()
    }

    fun loadResult(puzzleId: String): RootsResult? {
        val json = prefs.getString(KEY_RESULT, null) ?: return null
        val result = runCatching { Json.decodeFromString<RootsResult>(json) }.getOrNull() ?: return null
        return result.takeIf { it.puzzleId == puzzleId }
    }

    fun saveResult(result: RootsResult) {
        prefs.edit().putString(KEY_RESULT, Json.encodeToString(result)).apply()
    }

    fun personalBestSeconds(): Int = prefs.getInt(KEY_PERSONAL_BEST, -1)

    fun updatePersonalBest(seconds: Int) {
        val current = personalBestSeconds()
        if (current < 0 || seconds < current) {
            prefs.edit().putInt(KEY_PERSONAL_BEST, seconds).apply()
        }
    }

    fun clearAll() {
        prefs.edit().remove(KEY_PROGRESS).remove(KEY_RESULT).apply()
        // KEY_PERSONAL_BEST intentionally kept — personal best survives sign-out
    }

    private const val KEY_PROGRESS = "progress"
    private const val KEY_RESULT = "last_result"
    private const val KEY_PERSONAL_BEST = "personal_best_seconds"
}
