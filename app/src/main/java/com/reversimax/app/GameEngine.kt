package com.reversimax.app

/**
 * Чистое игровое ядро Реверси (Отелло): не зависит от Android,
 * поэтому полностью покрывается обычными JVM-тестами.
 *
 * Доска — ByteArray(64). Индекс клетки = row * 8 + col.
 * 0 — пусто, 1 — чёрные, 2 — белые.
 */
enum class Disc(val id: Int) {
    BLACK(1),
    WHITE(2);

    val opponent: Disc get() = if (this == BLACK) WHITE else BLACK
}

enum class Difficulty { EASY, MEDIUM, HARD }

object GameEngine {

    const val BOARD_SIZE = 8
    const val CELL_COUNT = BOARD_SIZE * BOARD_SIZE

    /** Стандартная стартовая позиция: чёрные на e4/d5, белые на d4/e5. */
    fun initialBoard(): ByteArray {
        val board = ByteArray(CELL_COUNT)
        board[28] = Disc.BLACK.id.toByte() // e4
        board[35] = Disc.BLACK.id.toByte() // d5
        board[27] = Disc.WHITE.id.toByte() // d4
        board[36] = Disc.WHITE.id.toByte() // e5
        return board
    }

    /**
     * Все легальные ходы игрока: карта "клетка -> список переворачиваемых фишек".
     */
    fun validMoves(board: ByteArray, player: Disc): Map<Int, List<Int>> {
        val result = HashMap<Int, List<Int>>()
        for (index in 0 until CELL_COUNT) {
            if (board[index] != 0.toByte()) continue
            val flips = collectFlips(board, index, player)
            if (flips.isNotEmpty()) result[index] = flips
        }
        return result
    }

    fun hasValidMove(board: ByteArray, player: Disc): Boolean {
        for (index in 0 until CELL_COUNT) {
            if (board[index] != 0.toByte()) continue
            if (collectFlips(board, index, player).isNotEmpty()) return true
        }
        return false
    }

    private fun collectFlips(board: ByteArray, index: Int, player: Disc): List<Int> {
        val out = ArrayList<Int>()
        val row = index / BOARD_SIZE
        val col = index % BOARD_SIZE
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                var r = row + dr
                var c = col + dc
                val line = ArrayList<Int>()
                while (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE) {
                    val value = board[r * BOARD_SIZE + c]
                    if (value == 0.toByte()) {
                        line.clear()
                        break
                    }
                    if (value == player.id.toByte()) {
                        if (line.isNotEmpty()) out.addAll(line)
                        break
                    }
                    line.add(r * BOARD_SIZE + c)
                    r += dr
                    c += dc
                }
            }
        }
        return out
    }

    /**
     * Выполняет ход и возвращает НОВУЮ доску (исходная не меняется).
     * Возвращает null, если ход нелегален.
     */
    fun applyMove(board: ByteArray, index: Int, player: Disc): ByteArray? {
        val flips = collectFlips(board, index, player)
        if (board[index] != 0.toByte() || flips.isEmpty()) return null
        val next = board.copyOf()
        next[index] = player.id.toByte()
        for (cell in flips) next[cell] = player.id.toByte()
        return next
    }

    fun count(board: ByteArray, player: Disc): Int = board.count { it == player.id.toByte() }

    fun emptyCells(board: ByteArray): Int = board.count { it == 0.toByte() }

    class GameResult(
        val black: Int,
        val white: Int,
        val finished: Boolean,
    ) {
        val winner: Disc? get() = when {
            black > white -> Disc.BLACK
            white > black -> Disc.WHITE
            else -> null
        }
    }

    fun result(board: ByteArray): GameResult {
        val black = count(board, Disc.BLACK)
        val white = count(board, Disc.WHITE)
        val finished = emptyCells(board) == 0 ||
            (!hasValidMove(board, Disc.BLACK) && !hasValidMove(board, Disc.WHITE))
        return GameResult(black, white, finished)
    }
}