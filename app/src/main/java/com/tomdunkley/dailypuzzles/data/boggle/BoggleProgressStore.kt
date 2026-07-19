package com.tomdunkley.dailypuzzles.data.boggle

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BoggleProgress(
    val puzzleId: String,
    val secondsRemaining: Int,
    val foundWords: List<String>,
    val isPaused: Boolean,
)

@Serializable
data class BoggleResult(
    val puzzleId: String,
    val date: String,
    val score: Int,
    val validWords: List<String>,
    val rankToday: Int,
    val currentStreak: Int,
    val dailyBestScore: Int? = null,
    val dailyBestWordCount: Int? = null,
    val isNewDailyBest: Boolean = false,
)

/** Plain (unencrypted) on-device cache of in-progress Boggle state -- nothing sensitive
 * here, unlike AuthTokenStore, so a normal SharedPreferences file is fine. Keyed by
 * puzzle id so a new day's puzzle never resumes stale progress. Also caches the most
 * recent finished result, so a guest can still see today's score with no network --
 * see BoggleViewModel.loadTodayPuzzle's offline fallback.
 */
object BoggleProgressStore {

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("boggle_progress", Context.MODE_PRIVATE)
    }

    fun load(puzzleId: String): BoggleProgress? {
        val json = prefs.getString(KEY_PROGRESS, null) ?: return null
        val progress = runCatching { Json.decodeFromString<BoggleProgress>(json) }.getOrNull() ?: return null
        return progress.takeIf { it.puzzleId == puzzleId }
    }

    fun save(progress: BoggleProgress) {
        prefs.edit().putString(KEY_PROGRESS, Json.encodeToString(progress)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_PROGRESS).apply()
    }

    fun loadResult(puzzleId: String): BoggleResult? {
        val json = prefs.getString(KEY_RESULT, null) ?: return null
        val result = runCatching { Json.decodeFromString<BoggleResult>(json) }.getOrNull() ?: return null
        return result.takeIf { it.puzzleId == puzzleId }
    }

    fun saveResult(result: BoggleResult) {
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
