package com.tomdunkley.dailypuzzles.ui.screens.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomdunkley.dailypuzzles.ui.components.NumbersSolidColor
import com.tomdunkley.dailypuzzles.ui.components.RootsSolidColor
import com.tomdunkley.dailypuzzles.ui.components.WordsSolidColor

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

    var currentPage by remember { mutableIntStateOf(0) }
    val isFirst = currentPage == 0
    val isLast = currentPage == slides.size - 1
    val slide = slides[currentPage]

    Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Colored header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gameColor)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "$gameName Tutorial",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).padding(vertical = 12.dp),
                )
            }

            // Slide content
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                slide.illustration()
                Spacer(Modifier.height(24.dp))
                Text(
                    text = slide.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Page dots
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                slides.indices.forEach { i ->
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!isFirst) {
                    OutlinedButton(
                        onClick = { currentPage-- },
                        modifier = Modifier.weight(1f),
                    ) { Text("BACK") }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Button(
                    onClick = { if (isLast) onDone() else currentPage++ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gameColor,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(if (isLast) "LET'S PLAY!" else "NEXT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun boggleSlides(): List<TutorialSlide> = listOf(
    TutorialSlide(
        title = "Find the words",
        description = "Swipe through a 4×4 grid of letters to form words. Connect any adjacent letter — including diagonals.",
        illustration = { BoggleGridIllustration() },
    ),
    TutorialSlide(
        title = "Longer words score more",
        description = "Short words get you on the board. Long words win the game.",
        illustration = { BoggleScoringIllustration() },
    ),
    TutorialSlide(
        title = "You have 2 minutes",
        description = "Race against the clock. Find as many words as you can before time runs out!",
        illustration = { TimerIllustration(WordsSolidColor) },
    ),
)

private fun numbersSlides(): List<TutorialSlide> = listOf(
    TutorialSlide(
        title = "Reach the target",
        description = "You're given 6 numbers and a 3-digit target. Use maths to get as close to the target as you can.",
        illustration = { NumbersTargetIllustration() },
    ),
    TutorialSlide(
        title = "Any combination works",
        description = "Add, subtract, multiply or divide. You don't have to use all six numbers — just some or all of them.",
        illustration = { OperationsIllustration() },
    ),
    TutorialSlide(
        title = "Closest answer wins",
        description = "An exact hit scores highest. If you can't reach it exactly, the nearest answer wins.",
        illustration = { NumbersResultIllustration() },
    ),
)

private fun routesSlides(): List<TutorialSlide> = listOf(
    TutorialSlide(
        title = "Fill the grid",
        description = "Draw a path from Start to End that passes through every cell in the grid exactly once.",
        illustration = { RoutesGridIllustration(showPath = false) },
    ),
    TutorialSlide(
        title = "Visit every cell",
        description = "Move horizontally or vertically — no diagonals. Every cell must be visited exactly once.",
        illustration = { RoutesGridIllustration(showPath = true) },
    ),
    TutorialSlide(
        title = "Fastest time wins",
        description = "Each puzzle has exactly one correct path. Solve it as fast as you can — quickest time wins!",
        illustration = { TimerIllustration(RootsSolidColor) },
    ),
)

@Composable
private fun BoggleGridIllustration() {
    val letters = listOf("C", "A", "T", "E", "R", "O", "S", "N", "P", "L", "A", "G", "M", "U", "D", "E")
    val highlighted = setOf(0, 1, 2)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0..3) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0..3) {
                    val idx = row * 4 + col
                    val isHighlighted = idx in highlighted
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (isHighlighted) WordsSolidColor else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(8.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = letters[idx],
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isHighlighted) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoggleScoringIllustration() {
    val scores = listOf(
        "3 letters" to "1 pt",
        "4 letters" to "2 pts",
        "5 letters" to "3 pts",
        "6 letters" to "5 pts",
        "7 letters" to "8 pts",
        "8+ letters" to "11 pts",
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(200.dp),
    ) {
        scores.forEach { (length, pts) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(length, style = MaterialTheme.typography.bodyLarge)
                Text(pts, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = WordsSolidColor)
            }
        }
    }
}

@Composable
private fun TimerIllustration(color: Color) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(color.copy(alpha = 0.12f), CircleShape)
            .border(3.dp, color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "2:00",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun NumbersTargetIllustration() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 72.dp)
                .background(NumbersSolidColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TARGET", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
                Text("952", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        val numbers = listOf("25", "50", "75", "100", "3", "6")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0..1) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (j in 0..2) {
                        val num = numbers[i * 3 + j]
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(num, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationsIllustration() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("+", "−", "×", "÷").forEach { op ->
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(NumbersSolidColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(2.dp, NumbersSolidColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = op,
                    style = MaterialTheme.typography.headlineSmall,
                    color = NumbersSolidColor,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun NumbersResultIllustration() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(200.dp),
    ) {
        listOf(
            Triple("Exact!", "0 away", NumbersSolidColor),
            Triple("Close", "5 away", NumbersSolidColor.copy(alpha = 0.55f)),
            Triple("Far", "50+ away", MaterialTheme.colorScheme.onSurfaceVariant),
        ).forEach { (label, distance, color) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.SemiBold)
                }
                Text(distance, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun RoutesGridIllustration(showPath: Boolean) {
    // 3×3 grid. Valid path: (0,0)→(0,1)→(0,2)→(1,2)→(1,1)→(1,0)→(2,0)→(2,1)→(2,2)
    // Cell indices in visit order: 0, 1, 2, 5, 4, 3, 6, 7, 8
    val pathOrder = listOf(0, 1, 2, 5, 4, 3, 6, 7, 8)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0..2) {
                    val idx = row * 3 + col
                    val isStart = idx == 0
                    val isEnd = idx == 8
                    val pathPos = if (showPath) pathOrder.indexOf(idx) else -1
                    val bgColor = when {
                        isStart || isEnd -> RootsSolidColor
                        showPath -> RootsSolidColor.copy(alpha = 0.18f + (pathPos.toFloat() / pathOrder.size) * 0.3f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(bgColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when {
                                isStart -> "S"
                                isEnd -> "E"
                                showPath -> "${pathPos + 1}"
                                else -> ""
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isStart || isEnd) Color.White else RootsSolidColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
