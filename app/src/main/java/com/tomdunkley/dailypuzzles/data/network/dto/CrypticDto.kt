package com.tomdunkley.dailypuzzles.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmitCrypticClueRequestDto(
    val solution: String,
    val clue: String,
)

@Serializable
data class SuggestCrypticWordsResponseDto(
    val words: List<String>,
    @SerialName("grid_size") val gridSize: Int = 0,
)
