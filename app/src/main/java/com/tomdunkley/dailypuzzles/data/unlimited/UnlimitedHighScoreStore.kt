package com.tomdunkley.dailypuzzles.data.unlimited

import android.content.Context
import android.content.SharedPreferences
import com.tomdunkley.dailypuzzles.data.network.dto.NumbersStepDto

object UnlimitedHighScoreStore {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("unlimited_high_scores", Context.MODE_PRIVATE)
    }

    val boggleHighScore: Int get() = prefs.getInt("boggle_high_score", -1)
    val boggleHighScoreSeed: String get() = prefs.getString("boggle_high_score_seed", "") ?: ""
    val boggleHighScoreWords: List<String>
        get() = prefs.getString("boggle_high_score_words", "")
            ?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    val numbersBestDistance: Int get() = prefs.getInt("numbers_best_distance", -1)
    val numbersBestTimeSeconds: Int get() = prefs.getInt("numbers_best_time", -1)
    val numbersBestSeed: String get() = prefs.getString("numbers_best_seed", "") ?: ""
    val numbersBestResultValue: Int get() = prefs.getInt("numbers_best_result", -1)
    val numbersBestSteps: List<NumbersStepDto>
        get() {
            val encoded = prefs.getString("numbers_best_steps", "") ?: ""
            if (encoded.isEmpty()) return emptyList()
            return encoded.split("|").mapNotNull { part ->
                val p = part.split(":")
                if (p.size == 4) NumbersStepDto(p[0].toInt(), p[1], p[2].toInt(), p[3].toInt()) else null
            }
        }

    fun updateBoggleScore(score: Int, seed: String, words: List<String>) {
        if (boggleHighScore == -1 || score > boggleHighScore) {
            prefs.edit()
                .putInt("boggle_high_score", score)
                .putString("boggle_high_score_seed", seed)
                .putString("boggle_high_score_words", words.joinToString(","))
                .apply()
        }
    }

    val rootsBestTimeSeconds: Int get() = prefs.getInt("routes_best_time", -1)

    fun updateRootsTime(timeSeconds: Int, seed: String) {
        val current = rootsBestTimeSeconds
        if (current < 0 || timeSeconds < current) {
            prefs.edit()
                .putInt("routes_best_time", timeSeconds)
                .putString("routes_best_seed", seed)
                .apply()
        }
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    fun updateNumbersDistance(distance: Int, timeSeconds: Int, seed: String, resultValue: Int, steps: List<NumbersStepDto>) {
        val currentDist = numbersBestDistance
        val currentTime = numbersBestTimeSeconds
        val shouldUpdate = when {
            currentDist == -1 -> true
            distance < currentDist -> true
            distance == currentDist && timeSeconds < currentTime -> true
            else -> false
        }
        if (shouldUpdate) {
            prefs.edit()
                .putInt("numbers_best_distance", distance)
                .putInt("numbers_best_time", timeSeconds)
                .putString("numbers_best_seed", seed)
                .putInt("numbers_best_result", resultValue)
                .putString("numbers_best_steps", steps.joinToString("|") { "${it.a}:${it.op}:${it.b}:${it.result}" })
                .apply()
        }
    }
}
