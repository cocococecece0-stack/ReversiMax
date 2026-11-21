package com.reversimax.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ReversiAiTest {

    @Test
    fun `ai returns legal move at start`() {
        val board = GameEngine.initialBoard()
        val move = ReversiAi.chooseMove(board, Disc.BLACK, Difficulty.HARD)
        assertNotNull(move)
        assertTrue(GameEngine.validMoves(board, Disc.BLACK).containsKey(move!!))
    }

    @Test
    fun `hard ai beats greedy player in quick game`() {
        var board = GameEngine.initialBoard()
        var turn = Disc.BLACK
        val random = Random(7)
        var guard = 0
        while (!GameEngine.result(board).finished && guard++ < 200) {
            val moves = GameEngine.validMoves(board, turn)
            if (moves.isEmpty()) {
                turn = turn.opponent
                continue
            }
            // "Гreedy" противник всегда берёт ход с максимумом фишек
            val greedy = moves.maxByOrNull { it.value.size }!!.key
            val move = if (turn == Disc.BLACK) ReversiAi.chooseMove(board, turn, Difficulty.MEDIUM) else greedy
            board = GameEngine.applyMove(board, move!!, turn)!!
            turn = turn.opponent
        }
        val result = GameEngine.result(board)
        assertTrue(result.finished)
        assertTrue("AI должен побеждать жадного игрока: ${result.black} : ${result.white}", result.winner == Disc.BLACK)
    }

    @Test
    fun `ai finds winning move when available`() {
        // Вся доска чёрная, кроме h1 — белой, и одна пустая клетка f1.
        // Ход белых в f1 переворачивает g1 и заканчивает партию победой белых.
        val board = ByteArray(64)
        for (i in 0 until 64) board[i] = Disc.BLACK.id.toByte()
        board[7] = Disc.WHITE.id.toByte() // h1 белая
        board[5] = 0 // f1 пусто
        val moves = GameEngine.validMoves(board, Disc.WHITE)
        assertTrue("ожидался единственный ход f1", moves.keys == setOf(5))
        val move = ReversiAi.chooseMove(board, Disc.WHITE, Difficulty.MEDIUM)
        assertEquals(5, move)
        val after = GameEngine.applyMove(board, move!!, Disc.WHITE)!!
        assertEquals(3, GameEngine.count(after, Disc.WHITE))
        assertEquals(61, GameEngine.count(after, Disc.BLACK))
        val result = GameEngine.result(after)
        assertTrue(result.finished)
        assertEquals(Disc.BLACK, result.winner)
    }
}