package com.tomdunkley.dailypuzzles.ui.screens.tutorial

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomdunkley.dailypuzzles.data.roots.RootsCell
import com.tomdunkley.dailypuzzles.ui.components.AvatarIcon
import com.tomdunkley.dailypuzzles.ui.components.NumbersAccentColor
import com.tomdunkley.dailypuzzles.ui.components.NumbersIconColor
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.RootsAccentColor
import com.tomdunkley.dailypuzzles.ui.components.RootsIconColor
import com.tomdunkley.dailypuzzles.ui.components.RootsSolidColor
import com.tomdunkley.dailypuzzles.ui.components.SectionTopBar
import com.tomdunkley.dailypuzzles.ui.components.WordsAccentColor
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor
import com.tomdunkley.dailypuzzles.ui.screens.roots.RootsGridCanvas
import kotlinx.coroutines.launch

private data class LeaderboardEntry(
    val displayName: String,
    val mainScore: String,
    val secondaryScore: String?,
    val avatarColorId: String?,
    val isSelf: Boolean,
)

private class TutorialSlide(
    val title: String,
    val description: String,
    val illustration: @Composable () -> Unit,
)

@Composable
fun GameTutorialScreen(
    game: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val gameAccentColor = when (game) {
        "boggle" -> WordsAccentColor
        "numbers" -> NumbersAccentColor
        else -> RootsAccentColor
    }
    val gameColor = when (game) {
        "boggle" -> WordsSolidColor
        "numbers" -> NumbersSolidColor
        else -> RootsSolidColor
    }
    val gameName = when (game) {
        "boggle" -> "Words"
        "numbers" -> "Numbers"
        else -> "Routes"
    }
    val slides = remember(game) {
        when (game) {
            "boggle" -> boggleSlides()
            "numbers" -> numbersSlides()
            else -> routesSlides()
        }
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage
    val isFirst = currentPage == 0
    val isLast = currentPage == slides.size - 1

    Scaffold(
        topBar = {
            SectionTopBar(
                title = gameName,
                onBack = onBack,
                backgroundColor = gameAccentColor,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { pageIndex ->
                val slide = slides[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight(0.15f))
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        slide.illustration()
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = slide.description,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(0.15f))
                }
            }

            // Page dots
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(slides.size) { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == currentPage) 10.dp else 8.dp)
                            .background(
                                if (i == currentPage) gameColor else gameColor.copy(alpha = 0.3f),
                                CircleShape,
                            ),
                    )
                }
            }

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!isFirst) {
                    OutlinedButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(currentPage - 1) } },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                    ) { Text("BACK") }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        if (isLast) onDone()
                        else scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Text(
                        if (isLast) "PLAY" else "NEXT",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ── Boggle / Words slides ──────────────────────────────────────────────────────

private fun boggleSlides(): List<TutorialSlide> = listOf(
    TutorialSlide(
        title = "Connect letters to form words",
        description = "Swipe through the 5×5 grid to connect adjacent letters, including diagonals.",
        illustration = { BoggleGridIllustration(highlighted = setOf(10, 6, 12, 18, 14)) },
    ),
    TutorialSlide(
        title = "Longer words score more",
        description = "The longer the word, the more points you score.",
        illustration = { BoggleScoringIllustration() },
    ),
    TutorialSlide(
        title = "90 seconds on the clock",
        description = "Race against the clock. Find as many words as you can!",
        illustration = { TimerIllustration(WordsSolidColor, "1:30") },
    ),
)

// ── Numbers slides ─────────────────────────────────────────────────────────────

private fun numbersSlides(): List<TutorialSlide> = listOf(
    TutorialSlide(
        title = "Reach the target",
        description = "Combine the six numbers you're given to get as close to the target as you can.",
        illustration = { NumbersFullIllustration() },
    ),
    TutorialSlide(
        title = "Climb the leaderboard",
        description = "The closer you get to the target, the higher up the leaderboard you'll climb.",
        illustration = {
            LeaderboardIllustration(
                entries = listOf(
                    LeaderboardEntry("AmberEagle42", "Got it", " (15s)", "green", false),
                    LeaderboardEntry("FrostyBadger17", "955", " (3 away)", "blue", false),
                    LeaderboardEntry("CoralFalcon85", "947", " (5 away)", "orange", true),
                    LeaderboardEntry("SlateBear31", "940", " (12 away)", "red", false),
                    LeaderboardEntry("IndigoBison67", "920", " (32 away)", "green", false),
                ),
                solidColor = NumbersSolidColor,
                iconColor = NumbersIconColor,
            )
        },
    ),
)

// ── Routes slides ──────────────────────────────────────────────────────────────

private fun routesSlides(): List<TutorialSlide> = listOf(
    TutorialSlide(
        title = "Draw a path between two points",
        description = "Connect the two end points using only horizontal and vertical moves.",
        illustration = { RoutesGridIllustration(showPath = false) },
    ),
    TutorialSlide(
        title = "Match the row and column counts",
        description = "Each number shows how many cells the path visits in that row or column. Use the clues to find the one correct route.",
        illustration = { RoutesGridIllustration(showPath = true) },
    ),
    TutorialSlide(
        title = "Fastest time wins",
        description = "The faster you solve it, the higher up the leaderboard you'll climb.",
        illustration = {
            LeaderboardIllustration(
                entries = listOf(
                    LeaderboardEntry("JadeCondor54", "34s", null, "green", false),
                    LeaderboardEntry("CrimsonBear28", "52s", null, "blue", false),
                    LeaderboardEntry("CoralFalcon85", "1m 08s", null, "orange", true),
                    LeaderboardEntry("FrostyBadger96", "1m 22s", null, "red", false),
                    LeaderboardEntry("SlateFalcon42", "1m 47s", null, "blue", false),
                ),
                solidColor = RootsSolidColor,
                iconColor = RootsIconColor,
            )
        },
    ),
)

// ── Shared illustrations ───────────────────────────────────────────────────────

@Composable
private fun TimerIllustration(color: Color, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction = 0.50f)
            .aspectRatio(1f)
            .background(color.copy(alpha = 0.12f), CircleShape)
            .border(4.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun LeaderboardIllustration(
    entries: List<LeaderboardEntry>,
    solidColor: Color,
    iconColor: Color,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        entries.forEachIndexed { i, entry ->
            if (i > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (entry.isSelf) iconColor.copy(alpha = 0.5f)
                        else solidColor.copy(alpha = 0.08f),
                    )
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        Text("#${i + 1}", style = MaterialTheme.typography.titleMedium)
                    }
                    AvatarIcon(
                        avatarId = "person",
                        avatarColorId = entry.avatarColorId,
                        size = 32.dp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(entry.mainScore, style = MaterialTheme.typography.titleMedium)
                    entry.secondaryScore?.let { secondary ->
                        Text(
                            secondary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ── Boggle illustrations ───────────────────────────────────────────────────────

@Composable
private fun BoggleGridIllustration(highlighted: Set<Int> = emptySet()) {
    // WORDS zigzags diagonally: W(2,0)→O(1,1)→R(2,2)→D(3,3)→S(2,4), indices {10,6,12,18,14}.
    val board = listOf(
        "A", "B", "C", "E", "T",
        "F", "O", "H", "I", "N",
        "W", "G", "R", "J", "S",
        "K", "L", "M", "D", "U",
        "V", "X", "Y", "Z", "Q",
    )
    Column(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
        for (row in 0..4) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0..4) {
                    val idx = row * 5 + col
                    val isHl = idx in highlighted
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(3.dp)
                            .background(
                                if (isHl) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = board.getOrElse(idx) { "" },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 20.sp,
                            ),
                            fontWeight = FontWeight.Bold,
                            color = if (isHl) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoggleScoringIllustration() {
    // Correct values from BoggleScoring.kt: 4→1, 5→2, 6→3, 7→5, 8+→11
    val rows = listOf(
        "4 letters" to "1 pt",
        "5 letters" to "2 pts",
        "6 letters" to "3 pts",
        "7 letters" to "5 pts",
        "8+ letters" to "11 pts",
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        rows.forEachIndexed { i, (len, pts) ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(len, style = MaterialTheme.typography.titleMedium)
                Text(
                    pts,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WordsSolidColor,
                )
            }
        }
    }
}

// ── Numbers illustrations ──────────────────────────────────────────────────────

@Composable
private fun NumbersFullIllustration() {
    val numbers = listOf("25", "50", "75", "100", "3", "6")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "TARGET",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "952",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Left: number tiles
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    numbers.chunked(2).forEach { rowNums ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowNums.forEach { num ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        num,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
                // Right: operators + reset
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("+" to "−", "×" to "÷").forEach { (a, b) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(a, b).forEach { op ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        op,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("RESET", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ── Routes illustrations ───────────────────────────────────────────────────────

@Composable
private fun RoutesGridIllustration(showPath: Boolean) {
    // 4×4 example: S(0,0)→(0,1)→(0,2)→(1,2)→(1,1)→(2,1)→(2,2)→(2,3)→(3,3)=E
    // Row clues: [3,2,3,1]   Col clues: [1,3,3,2]
    val startCell = RootsCell(0, 0)
    val endCell = RootsCell(3, 3)
    val path = if (showPath) listOf(
        RootsCell(0, 0), RootsCell(0, 1), RootsCell(0, 2),
        RootsCell(1, 2), RootsCell(1, 1),
        RootsCell(2, 1), RootsCell(2, 2), RootsCell(2, 3),
        RootsCell(3, 3),
    ) else emptyList()
    RootsGridCanvas(
        n = 4,
        startCell = startCell,
        endCell = endCell,
        rowClues = listOf(3, 2, 3, 1),
        colClues = listOf(1, 3, 3, 2),
        path = path,
        crossMarkers = emptySet(),
        tickMarkers = emptySet(),
        interactive = false,
        onDragStart = {},
        onCellDrag = {},
        onTapCell = {},
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
    )
}
