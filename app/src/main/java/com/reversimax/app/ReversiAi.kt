package com.reversimax.app

import kotlin.math.abs
import kotlin.random.Random

/**
 * ИИ противника: негамакс с альфа-бета отсечениями, статическими весами клеток
 * и ограничением по числу исследованных узлов (чтобы не подвешивать UI).
 */
object ReversiAi {

    private val CELL_WEIGHTS = intArrayOf(
        20, -5, 11, 8, 8, 11, -5, 20,
        -5, -7, -4, 1, 1, -4, -7, -5,
        11, -4, 2, 2, 2, 2, -4, 11,
        8, 1, 2, -3, -3, 2, 1, 8,
        8, 1, 2, -3, -3, 2, 1, 8,
        11, -4, 2, 2, 2, 2, -4, 11,
        -5, -7, -4, 1, 1, -4, -7, -5,
        20, -5, 11, 8, 8, 11, -5, 20,
    )

    private const val WIN_SCORE = 1_000_000
    private const val NODE_BUDGET = 160_000

    private class Search(val weights: IntArray, val rootRandom: Random) {
        var nodes = 0
    }

    fun chooseMove(
        board: ByteArray,
        player: Disc,
        difficulty: Difficulty,
        random: Random = Random.Default,
    ): Int? {
        val moves = GameEngine.validMoves(board, player)
        if (moves.isEmpty()) return null

        if (difficulty == Difficulty.EASY) {
            // Лёгкий: приличная вероятность случайного/жадного выбора
            if (random.nextInt(100) < 35) {
                val keys = moves.keys.toList()
                return keys[random.nextInt(keys.size)]
            }
        }

        val depth = when (difficulty) {
            Difficulty.EASY -> 1
            Difficulty.MEDIUM -> 4
            Difficulty.HARD -> 7
        }

        val search = Search(CELL_WEIGHTS, random)
        var bestMove: Int? = null
        var bestScore = Int.MIN_VALUE

        // Сортируем ходы по "полезности" клетки — ускоряет альфа-бета
        val ordered = moves.entries.sortedByDescending { entry ->
            val flips = entry.value.size
            CELL_WEIGHTS[entry.key] - flips
        }

        for (entry in ordered) {
            val next = GameEngine.applyMove(board, entry.key, player) ?: continue
            val score = -search.negamax(next, player.opponent, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE)
            if (difficulty == Difficulty.EASY) {
                // Небольшой шум, чтобы лёгкий уровень не был беспощаден
                val noise = random.nextInt(8) - 4
                val value = score + noise
                if (value > bestScore) {
                    bestScore = value
                    bestMove = entry.key
                }
            } else if (score > bestScore) {
                bestScore = score
                bestMove = entry.key
            }
        }
        return bestMove
    }

    private fun Search.negamax(
        board: ByteArray,
        turn: Disc,
        depth: Int,
        alpha: Int,
        beta: Int,
    ): Int {
        nodes++
        val result = GameEngine.result(board)
        if (result.finished) {
            val mine = if (turn == Disc.BLACK) result.black else result.white
            val theirs = if (turn == Disc.BLACK) result.white else result.black
            return if (mine > theirs) WIN_SCORE + mine - theirs
            else -WIN_SCORE + mine - theirs
        }
        if (depth <= 0 || nodes > NODE_BUDGET) return evaluate(board, turn)

        val moves = GameEngine.validMoves(board, turn)
        if (moves.isEmpty()) {
            // Пас хода: сторона остаётся тем же игроком, меняется оппонент
            return -negamax(board, turn.opponent, depth - 1, -beta, -alpha)
        }

        var a = alpha
        var best = Int.MIN_VALUE
        val ordered = moves.keys.sortedByDescending { CELL_WEIGHTS[it] }
        for (index in ordered) {
            val next = GameEngine.applyMove(board, index, turn)
            if (next == null) continue
            val score = -negamax(next, turn.opponent, depth - 1, -beta, -a)
            if (score > best) best = score
            if (best > a) a = best
            if (a >= beta) break
        }
        return best
    }

    private fun evaluate(board: ByteArray, turn: Disc): Int {
        var total = 0
        for (i in board.indices) {
            when (board[i]) {
                Disc.BLACK.id.toByte() -> total += if (turn == Disc.BLACK) CELL_WEIGHTS[i] else -CELL_WEIGHTS[i]
                Disc.WHITE.id.toByte() -> total += if (turn == Disc.WHITE) CELL_WEIGHTS[i] else -CELL_WEIGHTS[i]
            }
        }
        // Небольшой бонус за подвижность
        val mine = GameEngine.validMoves(board, turn).size
        val theirs = GameEngine.validMoves(board, turn.opponent).size
        total += (mine - theirs) * 3
        // В поздней игре важнее дискриминант материала
        val empties = GameEngine.emptyCells(board)
        val discs = 64 - empties
        if (discs > 52) {
            val black = GameEngine.count(board, Disc.BLACK)
            val white = GameEngine.count(board, Disc.WHITE)
            val material = (if (turn == Disc.BLACK) black - white else white - black) * 12
            total += material
        }
        return if (abs(total) > WIN_SCORE) total.coerceIn(-WIN_SCORE, WIN_SCORE) else total
    }
}