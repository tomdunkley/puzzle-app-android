package com.tomdunkley.dailypuzzles.ui.screens.numbers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.data.challenges.ChallengeGameStore
import com.tomdunkley.dailypuzzles.data.challenges.InProgressChallengeStore

@Composable
fun NumbersChallengeScreen(
    challengeId: String,
    onBack: () -> Unit,
    onShowBottomBarChange: (Boolean) -> Unit,
    onChallengeComplete: (challengeId: String, bothPlayed: Boolean) -> Unit,
    viewModel: NumbersUnlimitedViewModel = viewModel(),
) {
    val challengePlayResult by viewModel.challengePlayResult.collectAsState()

    LaunchedEffect(Unit) {
        val puzzle = ChallengeGameStore.pendingPuzzleData as? ChallengeGameStore.PuzzleData.Numbers
        if (puzzle != null) {
            InProgressChallengeStore.save(
                challengeId = challengeId,
                game = "numbers",
                puzzleData = puzzle,
                seed = ChallengeGameStore.pendingSeed,
                opponentName = ChallengeGameStore.pendingOpponentName,
                opponentAvatarId = ChallengeGameStore.pendingOpponentAvatarId,
                opponentAvatarColorId = ChallengeGameStore.pendingOpponentAvatarColorId,
                opponentAvatarIconColor = ChallengeGameStore.pendingOpponentAvatarIconColor,
                myUserId = ChallengeGameStore.pendingMyUserId,
            )
            viewModel.setupChallengeMode(challengeId, puzzle.numbers, puzzle.target)
            viewModel.startGame()
            ChallengeGameStore.clear()
        }
    }

    LaunchedEffect(challengePlayResult) {
        val result = challengePlayResult ?: return@LaunchedEffect
        InProgressChallengeStore.clear()
        onChallengeComplete(challengeId, result.bothPlayed)
    }

    NumbersUnlimitedScreen(
        onBack = onBack,
        onShowBottomBarChange = onShowBottomBarChange,
        viewModel = viewModel,
        challengeSeed = ChallengeGameStore.pendingSeed,
        challengeOpponentName = ChallengeGameStore.pendingOpponentName,
        challengeOpponentAvatarId = ChallengeGameStore.pendingOpponentAvatarId,
        challengeOpponentAvatarColorId = ChallengeGameStore.pendingOpponentAvatarColorId,
        challengeOpponentAvatarIconColor = ChallengeGameStore.pendingOpponentAvatarIconColor,
    )
}
