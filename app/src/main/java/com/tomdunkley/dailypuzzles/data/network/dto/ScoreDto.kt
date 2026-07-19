package com.tomdunkley.dailypuzzles.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AllWordDto(
    val word: String,
    val score: Int,
)

@Serializable
data class AllWordsResponseDto(
    val words: List<AllWordDto>,
)

@Serializable
data class ScoreSubmissionDto(
    @SerialName("puzzle_id") val puzzleId: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    val words: List<String> = emptyList(),
    @SerialName("result_value") val resultValue: Int? = null,
    val steps: List<NumbersStepDto> = emptyList(),
    @SerialName("all_computed_values") val allComputedValues: List<Int> = emptyList(),
)

@Serializable
data class ScoreSubmissionResultDto(
    @SerialName("score_id") val scoreId: String,
    @SerialName("rank_today") val rankToday: Int,
    @SerialName("current_streak") val currentStreak: Int,
    val score: Int? = null,
    @SerialName("valid_words") val validWords: List<String>? = null,
    @SerialName("result_value") val resultValue: Int? = null,
    val distance: Int? = null,
    val steps: List<NumbersStepDto>? = null,
    @SerialName("newly_unlocked") val newlyUnlocked: List<String> = emptyList(),
    @SerialName("daily_best_score") val dailyBestScore: Int? = null,
    @SerialName("daily_best_word_count") val dailyBestWordCount: Int? = null,
    @SerialName("daily_best_distance") val dailyBestDistance: Int? = null,
    @SerialName("daily_best_result_value") val dailyBestResultValue: Int? = null,
    @SerialName("daily_best_duration_seconds") val dailyBestDurationSeconds: Int? = null,
)

@Serializable
data class LeaderboardEntryDto(
    val rank: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_id") val avatarId: String? = null,
    @SerialName("avatar_color_id") val avatarColorId: String? = null,
    @SerialName("avatar_icon_color") val avatarIconColor: String? = null,
    val game: String = "boggle",
    val score: Int? = null,
    @SerialName("word_count") val wordCount: Int? = null,
    @SerialName("result_value") val resultValue: Int? = null,
    val distance: Int? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
)

@Serializable
data class LeaderboardDto(
    @SerialName("puzzle_id") val puzzleId: String,
    val entries: List<LeaderboardEntryDto>,
)

@Serializable
data class ScoreDetailDto(
    @SerialName("puzzle_id") val puzzleId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_id") val avatarId: String? = null,
    @SerialName("avatar_color_id") val avatarColorId: String? = null,
    @SerialName("avatar_icon_color") val avatarIconColor: String? = null,
    val game: String = "boggle",
    @SerialName("rank_today") val rankToday: Int,
    // True if the requester hasn't completed this puzzle themselves yet -- board,
    // numbers, target, valid_words, and steps below will be null to avoid spoiling it.
    val locked: Boolean = false,
    // boggle
    val score: Int? = null,
    @SerialName("valid_words") val validWords: List<String>? = null,
    val board: List<String>? = null,
    // numbers
    val numbers: List<Int>? = null,
    val target: Int? = null,
    @SerialName("result_value") val resultValue: Int? = null,
    val distance: Int? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    val steps: List<NumbersStepDto>? = null,
)
