package com.tomdunkley.dailypuzzles.data.boggle

private const val BOARD_SIZE = 5
private const val MIN_WORD_LENGTH = 4

private val ADJACENCY: List<List<Int>> = (0 until BOARD_SIZE * BOARD_SIZE).map { i ->
    val row = i / BOARD_SIZE; val col = i % BOARD_SIZE
    buildList {
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = row + dr; val nc = col + dc
            if (nr in 0 until BOARD_SIZE && nc in 0 until BOARD_SIZE) add(nr * BOARD_SIZE + nc)
        }
    }
}

object BoggleSolver {

    suspend fun findAllWords(board: List<String>): List<String> {
        val dictionary = BoggleDictionary.getAll()
        val prefixes = buildSet {
            for (word in dictionary) for (i in 1..word.length) add(word.substring(0, i))
        }
        val found = mutableSetOf<String>()

        fun dfs(index: Int, visited: MutableSet<Int>, current: String) {
            val word = current + board[index]
            if (word !in prefixes) return
            if (word.length >= MIN_WORD_LENGTH && word in dictionary) found.add(word)
            visited.add(index)
            for (neighbor in ADJACENCY[index]) {
                if (neighbor !in visited) dfs(neighbor, visited, word)
            }
            visited.remove(index)
        }

        for (start in board.indices) dfs(start, mutableSetOf(), "")

        return found.sortedWith(compareByDescending<String> { it.length }.thenBy { it })
    }
}
