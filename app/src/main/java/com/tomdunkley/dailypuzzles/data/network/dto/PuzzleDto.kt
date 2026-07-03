package com.tomdunkley.dailypuzzles.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameSummaryDto(
    val game: String,
    val title: String,
    val description: String,
)

@Serializable
data class NumbersStepDto(
    val a: Int,
    val op: String,
    val b: Int,
    val result: Int,
)

@Serializable
data class PuzzleDto(
    @SerialName("puzzle_id") val puzzleId: String,
    val game: String,
    val date: String,
    @SerialName("duration_seconds") val durationSeconds: Int,
    @SerialName("already_played") val alreadyPlayed: Boolean,
    val streak: StreakInfoDto? = null,
    // boggle
    val board: List<String>? = null,
    @SerialName("your_score") val yourScore: Int? = null,
    // numbers
    val numbers: List<Int>? = null,
    val target: Int? = null,
    val solution: List<NumbersStepDto>? = null,
    @SerialName("your_result_value") val yourResultValue: Int? = null,
    @SerialName("your_distance") val yourDistance: Int? = null,
)
