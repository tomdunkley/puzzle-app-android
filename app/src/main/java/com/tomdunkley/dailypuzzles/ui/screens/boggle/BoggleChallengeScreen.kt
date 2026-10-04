package com.tomdunkley.dailypuzzles.ui.screens.boggle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.data.challenges.ChallengeGameStore
import com.tomdunkley.dailypuzzles.data.challenges.InProgressChallengeStore

@Composable
fun BoggleChallengeScreen(
    challengeId: String,
    onBack: () -> Unit,
    onShowBottomBarChange: (Boolean) -> Unit,
    onChallengeComplete: (challengeId: String, bothPlayed: Boolean) -> Unit,
    viewModel: BoggleUnlimitedViewModel = viewModel(),
) {
    val challengePlayResult by viewModel.challengePlayResult.collectAsState()

    LaunchedEffect(Unit) {
        val puzzle = ChallengeGameStore.pendingPuzzleData as? ChallengeGameStore.PuzzleData.Boggle
        if (puzzle != null) {
            InProgressChallengeStore.save(
                challengeId = challengeId,
                game = "boggle",
                puzzleData = puzzle,
                seed = ChallengeGameStore.pendingSeed,
                opponentName = ChallengeGameStore.pendingOpponentName,
                opponentAvatarId = ChallengeGameStore.pendingOpponentAvatarId,
                opponentAvatarColorId = ChallengeGameStore.pendingOpponentAvatarColorId,
                opponentAvatarIconColor = ChallengeGameStore.pendingOpponentAvatarIconColor,
                myUserId = ChallengeGameStore.pendingMyUserId,
            )
            viewModel.setupChallengeMode(challengeId, puzzle.board)
            viewModel.startGame()
            ChallengeGameStore.clear()
        }
    }

    LaunchedEffect(challengePlayResult) {
        val result = challengePlayResult ?: return@LaunchedEffect
        InProgressChallengeStore.clear()
        onChallengeComplete(challengeId, result.bothPlayed)
    }

    BoggleUnlimitedScreen(
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
