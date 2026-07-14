package com.tomdunkley.dailypuzzles.data.unlimited

import kotlin.random.Random

object UnlimitedPuzzleGenerator {

    private const val SEED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private const val SEED_LENGTH = 5

    fun generateSeedCode(): String = (1..SEED_LENGTH)
        .map { SEED_CHARS[kotlin.random.Random.nextInt(SEED_CHARS.length)] }
        .joinToString("")

    fun seedCodeToLong(code: String): Long =
        code.uppercase().fold(0L) { acc, c -> acc * SEED_CHARS.length + SEED_CHARS.indexOf(c).coerceAtLeast(0) }

    // Weighted letter selection based on Scrabble tile distribution (98 tiles, no blanks)
    private const val LETTER_POOL =
        "AAAAAAAAABBCCDDDDEEEEEEEEEEEEFFGGGHHIIIIIIIIIJKLLLLMMNNNNNNOOOOOOOOPPQRRRRRRSSSSTTTTTTUUUUVVWWXYYZ"

    private val LARGE_NUMBERS = listOf(25, 50, 75, 100)
    private val SMALL_NUMBERS = listOf(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10)

    fun generateBoggleBoard(seed: Long): List<String> {
        val random = Random(seed)
        return List(25) {
            val letter = LETTER_POOL[random.nextInt(LETTER_POOL.length)].toString()
            if (letter == "Q") "QU" else letter
        }
    }

    fun generateNumbersPuzzle(seed: Long): Pair<Int, List<Int>> {
        val random = Random(seed)
        val target = random.nextInt(900) + 100
        // Pick 0-4 large numbers, matching the daily mode's distribution range
        val numLarge = random.nextInt(5)
        val selected = LARGE_NUMBERS.shuffled(random).take(numLarge) +
            SMALL_NUMBERS.shuffled(random).take(6 - numLarge)
        return target to selected.sortedDescending()
    }
}
