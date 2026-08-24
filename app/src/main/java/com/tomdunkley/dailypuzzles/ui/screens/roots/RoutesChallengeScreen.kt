package com.tomdunkley.dailypuzzles.ui.screens.roots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tomdunkley.dailypuzzles.data.challenges.ChallengeGameStore

@Composable
fun RoutesChallengeScreen(
    challengeId: String,
    onBack: () -> Unit,
    onShowBottomBarChange: (Boolean) -> Unit,
    onChallengeComplete: (challengeId: String, bothPlayed: Boolean) -> Unit,
    viewModel: RootsUnlimitedViewModel = viewModel(),
) {
    val challengePlayResult by viewModel.challengePlayResult.collectAsState()

    LaunchedEffect(Unit) {
        val puzzle = ChallengeGameStore.pendingPuzzleData as? ChallengeGameStore.PuzzleData.Routes
        if (puzzle != null) {
            viewModel.setupChallengeMode(challengeId, puzzle.seed, puzzle.gridSize)
            viewModel.startGame()
            ChallengeGameStore.clear()
        }
    }

    LaunchedEffect(challengePlayResult) {
        val result = challengePlayResult ?: return@LaunchedEffect
        onChallengeComplete(challengeId, result.bothPlayed)
    }

    RootsUnlimitedScreen(
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
