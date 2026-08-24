package com.tomdunkley.dailypuzzles.data.challenges

object ChallengeGameStore {
    sealed class PuzzleData {
        data class Boggle(val board: List<String>) : PuzzleData()
        data class Numbers(val numbers: List<Int>, val target: Int) : PuzzleData()
        data class Routes(val seed: String, val gridSize: Int) : PuzzleData()
    }

    var pendingChallengeId: String? = null
    var pendingGame: String? = null
    var pendingPuzzleData: PuzzleData? = null
    // These survive clear() so they're accessible after the game screen clears puzzle data.
    var pendingOpponentName: String? = null
    var pendingMyUserId: String? = null
    var pendingSeed: String? = null
    var pendingOpponentAvatarId: String? = null
    var pendingOpponentAvatarColorId: String? = null
    var pendingOpponentAvatarIconColor: String? = null

    fun clear() {
        pendingChallengeId = null
        pendingGame = null
        pendingPuzzleData = null
    }
}
