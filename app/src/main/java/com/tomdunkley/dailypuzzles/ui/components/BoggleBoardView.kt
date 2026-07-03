package com.tomdunkley.dailypuzzles.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val BOARD_SIZE = 5

/** The board stores the Q tile as "QU" (matching the dictionary's word form), but it
 * reads as a single die face -- shown as "Qu" rather than shouting "QU" alongside
 * single capital letters.
 */
fun displayLetter(letter: String): String = if (letter == "QU") "Qu" else letter

/** Read-only Boggle board renderer -- the non-interactive counterpart to BoggleScreen's
 * drag-gesture grid, shared by the score detail view where the board is just shown,
 * never played.
 */
@Composable
fun BoggleBoardView(board: List<String>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().aspectRatio(1f)) {
        for (row in 0 until BOARD_SIZE) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0 until BOARD_SIZE) {
                    val index = row * BOARD_SIZE + col
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(3.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = displayLetter(board.getOrElse(index) { "" }),
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontSize = 32.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
