package com.tomdunkley.dailypuzzles.ui.share

import android.content.Context
import android.content.Intent
import com.tomdunkley.dailypuzzles.data.roots.RootsCell

/** rankToday is null for a guest's own score -- guests aren't ranked publicly. */
fun buildBoggleShareText(date: String, score: Int, wordCount: Int, rankToday: Int?): String {
    val rankClause = if (rankToday != null) " — Global rank #$rankToday today" else ""
    return "td Puzzles — Words — $date\nScore: $score ($wordCount words)$rankClause"
}

/** rankToday is null for a guest's own score -- guests aren't ranked publicly. */
fun buildNumbersShareText(
    date: String,
    target: Int,
    resultValue: Int,
    distance: Int,
    durationSeconds: Int,
    rankToday: Int?,
): String {
    val resultClause = if (distance == 0) "Got it in ${durationSeconds}s!" else "$resultValue ($distance away)"
    val rankClause = if (rankToday != null) " — Global rank #$rankToday today" else ""
    return "td Puzzles — Numbers — $date\nTarget: $target — $resultClause$rankClause"
}

fun buildRootsShareText(
    date: String,
    durationSeconds: Int,
    rankToday: Int?,
    solution: List<RootsCell>,
    gridSize: Int,
): String {
    val m = durationSeconds / 60
    val s = durationSeconds % 60
    val timeStr = if (m > 0) "${m}m ${s}s" else "${s}s"
    val rankClause = if (rankToday != null) " — Global rank #$rankToday today" else ""
    val grid = buildString {
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                append(if (solution.contains(RootsCell(r, c))) "🟩" else "⬜")
            }
            if (r < gridSize - 1) append("\n")
        }
    }
    return "td Puzzles — Routes — $date\nSolved in $timeStr$rankClause\n\n$grid"
}

fun shareText(context: Context, text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}
