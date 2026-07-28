package com.tomdunkley.dailypuzzles.data.lines

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LinesCell(val row: Int, val col: Int)

@Serializable
data class LinesProgress(
    val puzzleId: String,
    val gridSize: Int,
    val startCell: LinesCell,
    val endCell: LinesCell,
    val solution: List<LinesCell>,
    val rowClues: List<Int>,
    val colClues: List<Int>,
    val currentPath: List<LinesCell>,
    val crossMarkers: List<LinesCell>,
    val elapsedSeconds: Int,
    val isPaused: Boolean,
)

@Serializable
data class LinesResult(
    val puzzleId: String,
    val date: String,
    val gridSize: Int,
    val durationSeconds: Int,
    val rankToday: Int,
    val currentStreak: Int,
    val dailyBestDurationSeconds: Int? = null,
    val isNewDailyBest: Boolean = false,
)

object LinesProgressStore {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("lines_progress", Context.MODE_PRIVATE)
    }

    fun load(puzzleId: String): LinesProgress? {
        val json = prefs.getString(KEY_PROGRESS, null) ?: return null
        val progress = runCatching { Json.decodeFromString<LinesProgress>(json) }.getOrNull() ?: return null
        return progress.takeIf { it.puzzleId == puzzleId }
    }

    fun save(progress: LinesProgress) {
        prefs.edit().putString(KEY_PROGRESS, Json.encodeToString(progress)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_PROGRESS).apply()
    }

    fun loadResult(puzzleId: String): LinesResult? {
        val json = prefs.getString(KEY_RESULT, null) ?: return null
        val result = runCatching { Json.decodeFromString<LinesResult>(json) }.getOrNull() ?: return null
        return result.takeIf { it.puzzleId == puzzleId }
    }

    fun saveResult(result: LinesResult) {
        prefs.edit().putString(KEY_RESULT, Json.encodeToString(result)).apply()
    }

    fun clearAll() {
        prefs.edit().remove(KEY_PROGRESS).remove(KEY_RESULT).apply()
    }

    private const val KEY_PROGRESS = "progress"
    private const val KEY_RESULT = "last_result"
}
