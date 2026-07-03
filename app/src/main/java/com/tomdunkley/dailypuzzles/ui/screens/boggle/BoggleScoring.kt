package com.tomdunkley.dailypuzzles.ui.screens.boggle

// Client-side mirror of the backend's scoring table (app/services/boggle.py), used
// purely for the live in-game display -- the server independently recomputes the
// real score from the board + dictionary at submission time.
private val SCORE_BY_LENGTH = mapOf(4 to 1, 5 to 2, 6 to 3, 7 to 5)
private const val SCORE_FOR_LONG_WORDS = 11
private const val MIN_WORD_LENGTH = 4

fun scoreForWord(word: String): Int =
    if (word.length < MIN_WORD_LENGTH) 0 else SCORE_BY_LENGTH[word.length] ?: SCORE_FOR_LONG_WORDS

fun estimatedScoreForWords(words: List<String>): Int = words.sumOf { scoreForWord(it) }
