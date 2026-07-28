package com.tomdunkley.dailypuzzles.ui.screens.lines

import com.tomdunkley.dailypuzzles.data.lines.LinesCell
import kotlin.random.Random

data class LinesPuzzle(
    val gridSize: Int,
    val startCell: LinesCell,
    val endCell: LinesCell,
    val solution: List<LinesCell>,
    val rowClues: List<Int>,
    val colClues: List<Int>,
)

object LinesPuzzleGenerator {

    private val DIRS = listOf(
        LinesCell(-1, 0), LinesCell(1, 0),
        LinesCell(0, -1), LinesCell(0, 1),
    )

    fun gridSizeForDayOfWeek(dayOfWeek: Int): Int = when (dayOfWeek) {
        // dayOfWeek: 1=Mon, 7=Sun (Java Calendar / ISO)
        2 -> 4   // Monday
        7, 1 -> 6 // Saturday, Sunday
        else -> 5
    }

    fun generate(seed: Long, gridSize: Int): LinesPuzzle {
        val random = Random(seed)
        repeat(500) {
            val puzzle = tryGenerate(random, gridSize)
            if (puzzle != null) return puzzle
        }
        return generateFallback(gridSize)
    }

    private fun tryGenerate(random: Random, n: Int): LinesPuzzle? {
        val start = LinesCell(random.nextInt(n), random.nextInt(n))
        var end: LinesCell
        var attempts = 0
        do {
            end = LinesCell(random.nextInt(n), random.nextInt(n))
            attempts++
            if (attempts > 50) return null
        } while (end == start || manhattan(start, end) < (n + 1) / 2)

        val path = randomPath(random, n, start, end) ?: return null

        val minLength = maxOf((n * n * 0.35).toInt(), n + 2)
        if (path.size < minLength) return null
        if (countTurns(path) < n - 1) return null

        val rowClues = List(n) { r -> path.count { it.row == r } }
        val colClues = List(n) { c -> path.count { it.col == c } }

        if (!isUnique(n, start, end, rowClues, colClues)) return null

        return LinesPuzzle(n, start, end, path, rowClues, colClues)
    }

    private fun manhattan(a: LinesCell, b: LinesCell) =
        kotlin.math.abs(a.row - b.row) + kotlin.math.abs(a.col - b.col)

    private fun randomPath(random: Random, n: Int, start: LinesCell, end: LinesCell): List<LinesCell>? {
        val visited = Array(n) { BooleanArray(n) }
        val path = mutableListOf<LinesCell>()

        fun dfs(cell: LinesCell): Boolean {
            visited[cell.row][cell.col] = true
            path.add(cell)
            if (cell == end) return true
            for (dir in DIRS.shuffled(random)) {
                val next = LinesCell(cell.row + dir.row, cell.col + dir.col)
                if (next.row !in 0 until n || next.col !in 0 until n) continue
                if (visited[next.row][next.col]) continue
                if (dfs(next)) return true
            }
            visited[cell.row][cell.col] = false
            path.removeLast()
            return false
        }

        return if (dfs(start)) path.toList() else null
    }

    private fun countTurns(path: List<LinesCell>): Int {
        if (path.size < 3) return 0
        var turns = 0
        var prevDr = path[1].row - path[0].row
        var prevDc = path[1].col - path[0].col
        for (i in 2 until path.size) {
            val dr = path[i].row - path[i - 1].row
            val dc = path[i].col - path[i - 1].col
            if (dr != prevDr || dc != prevDc) turns++
            prevDr = dr; prevDc = dc
        }
        return turns
    }

    private fun isUnique(
        n: Int,
        start: LinesCell,
        end: LinesCell,
        rowClues: List<Int>,
        colClues: List<Int>,
    ): Boolean {
        var count = 0
        val rowUsed = IntArray(n)
        val colUsed = IntArray(n)
        val visited = Array(n) { BooleanArray(n) }
        var nodes = 0

        fun dfs(cell: LinesCell) {
            if (count >= 2 || nodes > 100_000) return
            nodes++
            rowUsed[cell.row]++
            colUsed[cell.col]++
            visited[cell.row][cell.col] = true

            if (cell == end) {
                if ((0 until n).all { rowUsed[it] == rowClues[it] } &&
                    (0 until n).all { colUsed[it] == colClues[it] }
                ) count++
            } else {
                // Prune: check that remaining row/col budget is achievable
                var feasible = true
                for (r in 0 until n) {
                    val remaining = rowClues[r] - rowUsed[r]
                    if (remaining < 0) { feasible = false; break }
                    if (remaining > 0) {
                        val available = (0 until n).count { c -> !visited[r][c] }
                        if (available < remaining) { feasible = false; break }
                    }
                }
                if (feasible) {
                    for (r in 0 until n) {
                        val remaining = colClues[r] - colUsed[r]
                        if (remaining < 0) { feasible = false; break }
                        if (remaining > 0) {
                            val available = (0 until n).count { rr -> !visited[rr][r] }
                            if (available < remaining) { feasible = false; break }
                        }
                    }
                }
                if (feasible) {
                    for (dir in DIRS) {
                        if (count >= 2) break
                        val next = LinesCell(cell.row + dir.row, cell.col + dir.col)
                        if (next.row !in 0 until n || next.col !in 0 until n) continue
                        if (visited[next.row][next.col]) continue
                        if (rowUsed[next.row] >= rowClues[next.row]) continue
                        if (colUsed[next.col] >= colClues[next.col]) continue
                        dfs(next)
                    }
                }
            }

            rowUsed[cell.row]--
            colUsed[cell.col]--
            visited[cell.row][cell.col] = false
        }

        dfs(start)
        return count == 1
    }

    private fun generateFallback(n: Int): LinesPuzzle {
        val path = mutableListOf<LinesCell>()
        for (row in 0 until n) {
            if (row % 2 == 0) {
                for (col in 0 until n) path.add(LinesCell(row, col))
            } else {
                for (col in n - 1 downTo 0) path.add(LinesCell(row, col))
            }
        }
        val rowClues = List(n) { n }
        val colClues = List(n) { n }
        return LinesPuzzle(n, path.first(), path.last(), path, rowClues, colClues)
    }

    fun checkSolved(
        path: List<LinesCell>,
        rowClues: List<Int>,
        colClues: List<Int>,
        startCell: LinesCell,
        endCell: LinesCell,
    ): Boolean {
        if (path.isEmpty()) return false
        val first = path.first()
        val last = path.last()
        if (!((first == startCell && last == endCell) || (first == endCell && last == startCell))) return false
        val n = rowClues.size
        val rowCount = IntArray(n) { r -> path.count { it.row == r } }
        val colCount = IntArray(n) { c -> path.count { it.col == c } }
        return rowCount.indices.all { rowCount[it] == rowClues[it] } &&
            colCount.indices.all { colCount[it] == colClues[it] }
    }
}
