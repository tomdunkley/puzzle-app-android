package com.tomdunkley.dailypuzzles.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateChallengeRequestDto(
    @SerialName("friend_id") val friendId: String,
    val game: String,
    val seed: String,
    @SerialName("puzzle_data") val puzzleData: ChallengePuzzleDataDto,
)

@Serializable
data class ChallengePuzzleDataDto(
    val board: List<String>? = null,       // boggle
    val numbers: List<Int>? = null,        // numbers
    val target: Int? = null,               // numbers
    @SerialName("grid_size") val gridSize: Int? = null,  // routes
    val seed: String? = null,              // routes (injected by server into summary response)
)

@Serializable
data class ChallengeCreateResponseDto(
    @SerialName("challenge_id") val challengeId: String,
)

@Serializable
data class ChallengePlayRequestDto(
    @SerialName("duration_seconds") val durationSeconds: Int,
    val words: List<String> = emptyList(),
    @SerialName("result_value") val resultValue: Int? = null,
    val steps: List<NumbersStepDto> = emptyList(),
)

@Serializable
data class ChallengePlayResultDto(
    @SerialName("challenge_id") val challengeId: String,
    val status: String,
    @SerialName("both_played") val bothPlayed: Boolean,
)

@Serializable
data class LastChallengeResultDto(
    val outcome: String,
    @SerialName("my_summary") val mySummary: String,
    @SerialName("their_summary") val theirSummary: String,
)

@Serializable
data class ChallengeSummaryGameDto(
    val game: String,
    @SerialName("challenge_id") val challengeId: String? = null,
    // "open" = my turn, "waiting" = I played, waiting for them, "completed" = both played
    val status: String? = null,
    @SerialName("puzzle_data") val puzzleData: ChallengePuzzleDataDto? = null,
    @SerialName("record_wins") val recordWins: Int = 0,
    @SerialName("record_losses") val recordLosses: Int = 0,
    @SerialName("record_draws") val recordDraws: Int = 0,
    @SerialName("last_challenge_id") val lastChallengeId: String? = null,
    @SerialName("last_result") val lastResult: LastChallengeResultDto? = null,
    @SerialName("expires_at_epoch") val expiresAtEpoch: Long? = null,
)

@Serializable
data class FriendChallengeSummaryResponseDto(
    val games: List<ChallengeSummaryGameDto>,
)

@Serializable
data class PendingChallengesResponseDto(
    val count: Int,
    @SerialName("by_friend") val byFriend: Map<String, Int>,
)
