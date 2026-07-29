package com.tomdunkley.dailypuzzles.ui.screens.roots

import com.tomdunkley.dailypuzzles.data.roots.RootsCell
import kotlin.random.Random

data class RootsPuzzle(
    val gridSize: Int,
    val startCell: RootsCell,
    val endCell: RootsCell,
    val solution: List<RootsCell>,
    val rowClues: List<Int>,
    val colClues: List<Int>,
)

object RootsPuzzleGenerator {

    private val DIRS = listOf(
        RootsCell(-1, 0), RootsCell(1, 0),
        RootsCell(0, -1), RootsCell(0, 1),
    )

    fun gridSizeForDayOfWeek(dayOfWeek: Int): Int = when (dayOfWeek) {
        2 -> 4
        7, 1 -> 6
        else -> 5
    }

    fun generate(seed: Long, gridSize: Int): RootsPuzzle {
        val random = Random(seed)
        repeat(500) {
            val puzzle = tryGenerate(random, gridSize)
            if (puzzle != null) return puzzle
        }
        return generateFallback(gridSize)
    }

    private fun tryGenerate(random: Random, n: Int): RootsPuzzle? {
        val start = RootsCell(random.nextInt(n), random.nextInt(n))
        var end: RootsCell
        var attempts = 0
        do {
            end = RootsCell(random.nextInt(n), random.nextInt(n))
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

        return RootsPuzzle(n, start, end, path, rowClues, colClues)
    }

    private fun manhattan(a: RootsCell, b: RootsCell) =
        kotlin.math.abs(a.row - b.row) + kotlin.math.abs(a.col - b.col)

    private fun randomPath(random: Random, n: Int, start: RootsCell, end: RootsCell): List<RootsCell>? {
        val visited = Array(n) { BooleanArray(n) }
        val path = mutableListOf<RootsCell>()

        fun dfs(cell: RootsCell): Boolean {
            visited[cell.row][cell.col] = true
            path.add(cell)
            if (cell == end) return true
            for (dir in DIRS.shuffled(random)) {
                val next = RootsCell(cell.row + dir.row, cell.col + dir.col)
                if (next.row !in 0 until n || next.col !in 0 until n) continue
                if (visited[next.row][next.col]) continue
                if (dfs(next)) return true
            }
            visited[cell.row][cell.col] = false
            path.removeAt(path.size - 1)
            return false
        }

        return if (dfs(start)) path.toList() else null
    }

    private fun countTurns(path: List<RootsCell>): Int {
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
        start: RootsCell,
        end: RootsCell,
        rowClues: List<Int>,
        colClues: List<Int>,
    ): Boolean {
        var count = 0
        val rowUsed = IntArray(n)
        val colUsed = IntArray(n)
        val visited = Array(n) { BooleanArray(n) }
        var nodes = 0

        fun dfs(cell: RootsCell) {
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
                        val next = RootsCell(cell.row + dir.row, cell.col + dir.col)
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

    private fun generateFallback(n: Int): RootsPuzzle {
        val path = mutableListOf<RootsCell>()
        for (row in 0 until n) {
            if (row % 2 == 0) {
                for (col in 0 until n) path.add(RootsCell(row, col))
            } else {
                for (col in n - 1 downTo 0) path.add(RootsCell(row, col))
            }
        }
        val rowClues = List(n) { n }
        val colClues = List(n) { n }
        return RootsPuzzle(n, path.first(), path.last(), path, rowClues, colClues)
    }

    fun checkSolved(
        path: List<RootsCell>,
        rowClues: List<Int>,
        colClues: List<Int>,
        startCell: RootsCell,
        endCell: RootsCell,
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
