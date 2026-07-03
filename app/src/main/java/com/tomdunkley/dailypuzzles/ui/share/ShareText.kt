package com.tomdunkley.dailypuzzles.ui.share

import android.content.Context
import android.content.Intent

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

fun shareText(context: Context, text: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}
