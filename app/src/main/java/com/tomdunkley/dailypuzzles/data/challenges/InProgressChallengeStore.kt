package com.tomdunkley.dailypuzzles.data.challenges

import android.content.Context
import android.content.SharedPreferences

object InProgressChallengeStore {

    private const val PREFS_NAME = "in_progress_challenge"
    private const val KEY_ID = "challenge_id"
    private const val KEY_GAME = "game"
    private const val KEY_BOARD = "board"           // comma-separated strings (boggle)
    private const val KEY_NUMBERS = "numbers"       // comma-separated ints
    private const val KEY_TARGET = "target"
    private const val KEY_SEED = "seed"
    private const val KEY_GRID_SIZE = "grid_size"
    private const val KEY_OPPONENT_NAME = "opponent_name"
    private const val KEY_OPPONENT_AVATAR_ID = "opponent_avatar_id"
    private const val KEY_OPPONENT_AVATAR_COLOR_ID = "opponent_avatar_color_id"
    private const val KEY_OPPONENT_AVATAR_ICON_COLOR = "opponent_avatar_icon_color"
    private const val KEY_MY_USER_ID = "my_user_id"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun save(
        challengeId: String,
        game: String,
        puzzleData: ChallengeGameStore.PuzzleData,
        seed: String?,
        opponentName: String?,
        opponentAvatarId: String?,
        opponentAvatarColorId: String?,
        opponentAvatarIconColor: String?,
        myUserId: String?,
    ) {
        runCatching {
            prefs.edit().apply {
                putString(KEY_ID, challengeId)
                putString(KEY_GAME, game)
                putString(KEY_SEED, seed)
                putString(KEY_OPPONENT_NAME, opponentName)
                putString(KEY_OPPONENT_AVATAR_ID, opponentAvatarId)
                putString(KEY_OPPONENT_AVATAR_COLOR_ID, opponentAvatarColorId)
                putString(KEY_OPPONENT_AVATAR_ICON_COLOR, opponentAvatarIconColor)
                putString(KEY_MY_USER_ID, myUserId)
                when (puzzleData) {
                    is ChallengeGameStore.PuzzleData.Boggle ->
                        putString(KEY_BOARD, puzzleData.board.joinToString(","))
                    is ChallengeGameStore.PuzzleData.Numbers -> {
                        putString(KEY_NUMBERS, puzzleData.numbers.joinToString(","))
                        putInt(KEY_TARGET, puzzleData.target)
                    }
                    is ChallengeGameStore.PuzzleData.Routes ->
                        putInt(KEY_GRID_SIZE, puzzleData.gridSize)
                }
            }.apply()
        }
    }

    fun getInProgressChallengeId(): String? = runCatching {
        if (!::prefs.isInitialized) null else prefs.getString(KEY_ID, null)
    }.getOrNull()

    fun loadIntoChallengeGameStore(): Boolean = runCatching {
        val id = prefs.getString(KEY_ID, null) ?: return false
        val game = prefs.getString(KEY_GAME, null) ?: return false
        val puzzleData: ChallengeGameStore.PuzzleData = when (game) {
            "boggle" -> {
                val board = prefs.getString(KEY_BOARD, null)
                    ?.split(",")?.filter { it.isNotEmpty() } ?: return false
                ChallengeGameStore.PuzzleData.Boggle(board)
            }
            "numbers" -> {
                val numbers = prefs.getString(KEY_NUMBERS, null)
                    ?.split(",")?.mapNotNull { it.toIntOrNull() } ?: return false
                val target = prefs.getInt(KEY_TARGET, 0)
                ChallengeGameStore.PuzzleData.Numbers(numbers, target)
            }
            else -> {
                val seed = prefs.getString(KEY_SEED, "") ?: ""
                val gridSize = prefs.getInt(KEY_GRID_SIZE, 5)
                ChallengeGameStore.PuzzleData.Routes(seed, gridSize)
            }
        }
        ChallengeGameStore.pendingChallengeId = id
        ChallengeGameStore.pendingGame = game
        ChallengeGameStore.pendingPuzzleData = puzzleData
        ChallengeGameStore.pendingSeed = prefs.getString(KEY_SEED, null)
        ChallengeGameStore.pendingOpponentName = prefs.getString(KEY_OPPONENT_NAME, null)
        ChallengeGameStore.pendingOpponentAvatarId = prefs.getString(KEY_OPPONENT_AVATAR_ID, null)
        ChallengeGameStore.pendingOpponentAvatarColorId = prefs.getString(KEY_OPPONENT_AVATAR_COLOR_ID, null)
        ChallengeGameStore.pendingOpponentAvatarIconColor = prefs.getString(KEY_OPPONENT_AVATAR_ICON_COLOR, null)
        ChallengeGameStore.pendingMyUserId = prefs.getString(KEY_MY_USER_ID, null)
        true
    }.getOrDefault(false)

    fun clear() = runCatching { prefs.edit().clear().apply() }
}
