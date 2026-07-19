package com.tomdunkley.dailypuzzles.data.numbers

import android.content.Context
import android.content.SharedPreferences
import com.tomdunkley.dailypuzzles.data.network.dto.NumbersStepDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class NumbersTile(
    val id: Int,
    val value: Int,
    val steps: List<NumbersStepDto>,
)

@Serializable
data class NumbersProgress(
    val puzzleId: String,
    val secondsRemaining: Int,
    val tiles: List<NumbersTile>,
    val history: Map<Int, List<NumbersTile>>,
    val nextTileId: Int,
    val bestValue: Int,
    val bestSteps: List<NumbersStepDto>,
    val isPaused: Boolean,
)

@Serializable
data class NumbersResult(
    val puzzleId: String,
    val date: String,
    val target: Int,
    val numbers: List<Int>,
    val resultValue: Int,
    val steps: List<NumbersStepDto>,
    val distance: Int,
    val durationSeconds: Int,
    val rankToday: Int,
    val currentStreak: Int,
    val solution: List<NumbersStepDto> = emptyList(),
    val dailyBestDistance: Int? = null,
    val dailyBestResultValue: Int? = null,
)

/** Plain (unencrypted) on-device cache of in-progress Numbers state -- mirrors
 * BoggleProgressStore exactly, keyed by puzzle id so a new day's puzzle never resumes
 * stale progress. Also caches the most recent finished result, so a guest can still
 * see today's score with no network -- see NumbersViewModel.loadTodayPuzzle's offline
 * fallback.
 */
object NumbersProgressStore {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("numbers_progress", Context.MODE_PRIVATE)
    }

    fun load(puzzleId: String): NumbersProgress? {
        val json = prefs.getString(KEY_PROGRESS, null) ?: return null
        val progress = runCatching { Json.decodeFromString<NumbersProgress>(json) }.getOrNull() ?: return null
        return progress.takeIf { it.puzzleId == puzzleId }
    }

    fun save(progress: NumbersProgress) {
        prefs.edit().putString(KEY_PROGRESS, Json.encodeToString(progress)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_PROGRESS).apply()
    }

    fun loadResult(puzzleId: String): NumbersResult? {
        val json = prefs.getString(KEY_RESULT, null) ?: return null
        val result = runCatching { Json.decodeFromString<NumbersResult>(json) }.getOrNull() ?: return null
        return result.takeIf { it.puzzleId == puzzleId }
    }

    fun saveResult(result: NumbersResult) {
        prefs.edit().putString(KEY_RESULT, Json.encodeToString(result)).apply()
    }

    /** Wipes both the in-progress and cached-result slots -- called on sign-out so a
     * different account signing in next never inherits the previous account's "already
     * played" badge (these caches are keyed by puzzle id alone, not by account).
     */
    fun clearAll() {
        prefs.edit().remove(KEY_PROGRESS).remove(KEY_RESULT).apply()
    }

    private const val KEY_PROGRESS = "progress"
    private const val KEY_RESULT = "last_result"
}
