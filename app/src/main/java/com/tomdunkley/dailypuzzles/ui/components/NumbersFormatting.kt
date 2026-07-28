package com.tomdunkley.dailypuzzles.ui.components

/** Shared between NumbersScreen (live play) and ScoreDetailScreen (viewing your own or
 * a friend's derivation) so steps render identically in both places.
 */
fun numbersOpSymbol(op: String): String = when (op) {
    "+" -> "+"
    "-" -> "−"
    "*" -> "×"
    "/" -> "/"
    else -> op
}
