package com.reversimax.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    @Test
    fun `initial position has four discs and black to move`() {
        val board = GameEngine.initialBoard()
        assertEquals(2, GameEngine.count(board, Disc.BLACK))
        assertEquals(2, GameEngine.count(board, Disc.WHITE))
        assertEquals(60, GameEngine.emptyCells(board))
        assertTrue(GameEngine.hasValidMove(board, Disc.BLACK))
    }

    @Test
    fun `black has exactly four legal moves at start`() {
        val board = GameEngine.initialBoard()
        val moves = GameEngine.validMoves(board, Disc.BLACK)
        assertEquals(4, moves.size)
        assertTrue(moves.keys.containsAll(listOf(19, 26, 37, 44)))
    }

    @Test
    fun `a move flips the expected discs`() {
        var board = GameEngine.initialBoard()
        board = GameEngine.applyMove(board, 26, Disc.BLACK)!!
        // d3 (26) переворачивает d4
        assertEquals(Disc.BLACK.id.toByte(), board[27])
        assertEquals(4, GameEngine.count(board, Disc.BLACK))
        assertEquals(1, GameEngine.count(board, Disc.WHITE))
        assertEquals(Disc.BLACK.id.toByte(), board[26])
    }

    @Test
    fun `illegal moves are rejected`() {
        val board = GameEngine.initialBoard()
        assertNull(GameEngine.applyMove(board, 0, Disc.BLACK)) // занято? нет — но линия пуста
        assertNull(GameEngine.applyMove(board, 35, Disc.BLACK)) // своя фишка
        assertNull(GameEngine.applyMove(board, 27, Disc.BLACK)) // чужая фишка
        assertNotNull(GameEngine.applyMove(board, 19, Disc.BLACK))
    }

    @Test
    fun `pass switch finishes game when nobody can move`() {
        // Доска: остались две пустые клетки в ряд, оба игрока вынуждены пасовать
        val board = ByteArray(64)
        for (i in 0 until 64) board[i] = Disc.BLACK.id.toByte()
        board[0] = Disc.WHITE.id.toByte()
        board[1] = 0
        board[2] = 0
        board[8] = 0
        board[16] = 0
        assertFalse(GameEngine.hasValidMove(board, Disc.WHITE))
        assertFalse(GameEngine.hasValidMove(board, Disc.BLACK))
        val result = GameEngine.result(board)
        assertTrue(result.finished)
        assertEquals(Disc.BLACK, result.winner)
    }

    @Test
    fun `full replays produce growing discs`() {
        var board = GameEngine.initialBoard()
        var turn = Disc.BLACK
        var passes = 0
        while (!GameEngine.result(board).finished && passes < 70) {
            val moves = GameEngine.validMoves(board, turn)
            if (moves.isEmpty()) {
                turn = turn.opponent
                passes++
                continue
            }
            passes = 0
            val entry = moves.entries.first()
            val next = GameEngine.applyMove(board, entry.key, turn)!!
            assertEquals(GameEngine.count(board, turn) + entry.value.size + 1, GameEngine.count(next, turn))
            board = next
            turn = turn.opponent
        }
        assertTrue(GameEngine.result(board).finished)
    }
}