package com.reversimax.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reversimax.app.ui.Accent
import com.reversimax.app.ui.AccentWarm
import com.reversimax.app.ui.Ink
import com.reversimax.app.ui.Muted
import com.reversimax.app.ui.ReversiMaxTheme
import com.reversimax.app.ui.Space
import com.reversimax.app.ui.SpaceElevated
import com.reversimax.app.ui.SpaceSurface
import kotlinx.coroutines.delay

/** Вся игровая сессия в одном состоянии. */
class GameSession {
    var board by mutableStateOf(GameEngine.initialBoard())
        private set
    var turn by mutableStateOf(Disc.BLACK)
        private set
    var gameOver by mutableStateOf(false)
        private set
    var thinking by mutableStateOf(false)
        private set
    var status by mutableStateOf("Делайте первый ход")
        private set
    var lastMove by mutableStateOf<Int?>(null)
        private set
    var flipped by mutableStateOf<List<Int>>(emptyList())
        private set
    var humanDisc by mutableStateOf(Disc.BLACK)
        private set
    var difficulty by mutableStateOf(Difficulty.MEDIUM)
        private set
    var hint by mutableStateOf<Int?>(null)
        private set

    private val history = ArrayDeque<Pair<ByteArray, Disc>>()

    fun newGame() {
        board = GameEngine.initialBoard()
        turn = Disc.BLACK
        gameOver = false
        thinking = false
        lastMove = null
        flipped = emptyList()
        hint = null
        history.clear()
        refreshStatus()
    }

    fun changeDifficulty(value: Difficulty) {
        difficulty = value
        newGame()
    }

    fun chooseSide(value: Disc) {
        if (value == humanDisc) return
        humanDisc = value
        newGame()
    }

    fun play(index: Int): Boolean {
        if (gameOver || turn != humanDisc) return false
        val next = GameEngine.applyMove(board, index, turn) ?: return false
        history.addLast(board.copyOf() to turn)
        val flips = GameEngine.validMoves(board, turn)[index] ?: emptyList()
        board = next
        flipped = flips
        lastMove = index
        hint = null
        turn = turn.opponent
        refreshStatus()
        return true
    }

    /** Вызывается когда текущий игрок не может ходить. */
    fun passTurn() {
        thinking = false
        if (GameEngine.hasValidMove(board, turn)) return
        turn = turn.opponent
        refreshStatus()
    }

    fun markThinking() {
        thinking = true
    }

    fun markReady() {
        thinking = false
    }

    fun undo() {
        if (history.isEmpty()) return
        thinking = false
        while (history.isNotEmpty()) {
            val (snapshot, snapshotTurn) = history.removeLast()
            board = snapshot
            turn = snapshotTurn
            if (snapshotTurn == humanDisc) break
        }
        lastMove = null
        flipped = emptyList()
        hint = null
        refreshStatus()
    }

    fun showHint() {
        if (gameOver || turn != humanDisc) return
        hint = ReversiAi.chooseMove(board, turn, Difficulty.HARD)
    }

    private fun refreshStatus() {
        val result = GameEngine.result(board)
        if (result.finished) {
            gameOver = true
            val label = when (result.winner) {
                Disc.BLACK -> "Победили чёрные"
                Disc.WHITE -> "Победили белые"
                null -> "Ничья"
            }
            status = "$label  ${result.black} : ${result.white}"
            return
        }
        if (!GameEngine.hasValidMove(board, turn)) {
            turn = turn.opponent
            return refreshStatus()
        }
        gameOver = false
        status = if (turn == humanDisc) "Ваш ход" else "ИИ думает…"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReversiMaxTheme {
                GameScreen()
            }
        }
    }
}

@Composable
fun GameScreen() {
    val session = remember { GameSession() }

    // Ход ИИ с небольшой задержкой, чтобы игрок успевал следить за партией
    LaunchedEffect(session.turn, session.board, session.thinking) {
        if (!session.gameOver && !session.thinking && session.turn != session.humanDisc) {
            session.markThinking()
            delay(650)
            val moves = GameEngine.validMoves(session.board, session.turn)
            if (moves.isEmpty()) {
                session.passTurn()
                session.markReady()
                return@LaunchedEffect
            }
            val move = ReversiAi.chooseMove(session.board, session.turn, session.difficulty)
            if (move != null) session.play(move) else session.passTurn()
            session.markReady()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.reversimax.app.ui.Space)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "ReversiMax",
            fontSize = 26.sp,
            color = Ink,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = "Реверси против ИИ",
            fontSize = 13.sp,
            color = Muted,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Scoreboard(session)

        Spacer(Modifier.height(12.dp))

        BoardView(session)

        Spacer(Modifier.height(12.dp))

        Text(
            text = session.status,
            fontSize = 15.sp,
            color = if (session.thinking) Muted else AccentWarm,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Controls(session)
    }
}

@Composable
private fun Scoreboard(session: GameSession) {
    val black = GameEngine.count(session.board, Disc.BLACK)
    val white = GameEngine.count(session.board, Disc.WHITE)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SpaceSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ScoreChip("Чёрные", black, session.humanDisc == Disc.BLACK)
        ScoreChip("Белые", white, session.humanDisc == Disc.WHITE)
    }
}

@Composable
private fun ScoreChip(label: String, count: Int, isHuman: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(
                    if (label == "Чёрные") Color(0xFF10132E) else Color(0xFFF2F4FF),
                ),
        )
        Spacer(Modifier.size(8.dp))
        Column {
            Text(
                text = if (isHuman) "$label · вы" else label,
                fontSize = 11.sp,
                color = Muted,
            )
            Text(text = "$count", fontSize = 18.sp, color = Ink)
        }
    }
}

@Composable
private fun BoardView(session: GameSession) {
    val legal: Map<Int, List<Int>> = remember(session.board, session.turn, session.thinking, session.gameOver) {
        if (!session.gameOver && !session.thinking && session.turn == session.humanDisc) {
            GameEngine.validMoves(session.board, session.turn)
        } else {
            emptyMap()
        }
    }

    val progress = remember(session.lastMove) {
        Animatable(if (session.lastMove == null) 1f else 0f)
    }
    LaunchedEffect(session.lastMove) {
        if (session.lastMove != null) {
            progress.snapTo(0f)
            progress.animateTo(1f, animationSpec = tween(320))
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF121A40), Color(0xFF0D1230))),
            )
            .pointerInput(session.board, legal) {
                detectTapGestures { offset ->
                    val cell = size.width / 8f
                    val col = (offset.x / cell).toInt().coerceIn(0, 7)
                    val row = (offset.y / cell).toInt().coerceIn(0, 7)
                    val index = row * 8 + col
                    if (legal.containsKey(index)) session.play(index)
                }
            },
    ) {
        val cell = size.width / 8f

        // сетка
        for (i in 0..8) {
            val x = i * cell
            drawLine(
                color = Color(0x22335599),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1.5f,
            )
            drawLine(
                color = Color(0x22335599),
                start = Offset(0f, x),
                end = Offset(size.width, x),
                strokeWidth = 1.5f,
            )
        }

        // ориентиры в углах
        val star = cell * 0.06f
        listOf(Pair(2f, 2f), Pair(6f, 2f), Pair(2f, 6f), Pair(6f, 6f)).forEach { (c, r) ->
            drawCircle(
                color = Color(0x335C7BFF),
                radius = star,
                center = Offset(c * cell, r * cell),
            )
        }

        // подсказка возможных ходов
        legal.keys.forEach { index ->
            val row = index / 8
            val col = index % 8
            drawCircle(
                color = Accent.copy(alpha = 0.35f),
                radius = cell * 0.12f,
                center = Offset(col * cell + cell / 2, row * cell + cell / 2),
            )
        }

        // фишки
        for (index in 0 until 64) {
            val value = session.board[index]
            if (value == 0.toByte()) continue
            val row = index / 8
            val col = index % 8
            val center = Offset(col * cell + cell / 2, row * cell + cell / 2)
            val isNew = session.lastMove == index
            val isFlipped = session.flipped.contains(index)
            val scale = when {
                isNew -> 0.35f + 0.65f * progress.value
                isFlipped -> 0.85f + 0.15f * progress.value
                else -> 1f
            }
            drawDisc(
                center = center,
                radius = cell * 0.38f * scale,
                white = value == Disc.WHITE.id.toByte(),
                alpha = if (isFlipped && progress.value < 1f) 0.7f else 1f,
            )
        }

        // отметка последнего хода
        session.lastMove?.let { index ->
            val row = index / 8
            val col = index % 8
            drawCircle(
                color = AccentWarm.copy(alpha = 0.9f * progress.value),
                radius = cell * 0.45f,
                center = Offset(col * cell + cell / 2, row * cell + cell / 2),
                style = Stroke(width = cell * 0.06f),
            )
        }

        // подсказка лучшего хода
        session.hint?.let { index ->
            val row = index / 8
            val col = index % 8
            drawCircle(
                color = AccentWarm,
                radius = cell * 0.30f,
                center = Offset(col * cell + cell / 2, row * cell + cell / 2),
                style = Stroke(width = cell * 0.08f),
            )
        }

        // затемнение в конце партии
        if (session.gameOver) {
            drawRoundRect(
                color = Space.copy(alpha = 0.35f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
            )
        }
    }
}

@Composable
private fun Controls(session: GameSession) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Difficulty.EASY to "Лёгкий",
                Difficulty.MEDIUM to "Средний",
                Difficulty.HARD to "Сложный",
            ).forEach { (value, label) ->
                val selected = session.difficulty == value
                FilterChip(
                    selected = selected,
                    onClick = { session.changeDifficulty(value) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = SpaceElevated,
                        labelColor = Muted,
                        selectedContainerColor = Accent,
                        selectedLabelColor = Space,
                    ),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = session.humanDisc == Disc.BLACK,
                onClick = { session.chooseSide(Disc.BLACK) },
                label = { Text("Я за чёрных", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SpaceElevated,
                    labelColor = Muted,
                    selectedContainerColor = SpaceElevated,
                    selectedLabelColor = Ink,
                ),
            )
            FilterChip(
                selected = session.humanDisc == Disc.WHITE,
                onClick = { session.chooseSide(Disc.WHITE) },
                label = { Text("Я за белых", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SpaceElevated,
                    labelColor = Muted,
                    selectedContainerColor = SpaceElevated,
                    selectedLabelColor = Ink,
                ),
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { session.newGame() },
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
            ) {
                Text("Новая игра", color = com.reversimax.app.ui.Space)
            }
            OutlinedButton(
                onClick = { session.undo() },
                enabled = !session.gameOver,
            ) {
                Text("Отменить")
            }
            OutlinedButton(
                onClick = { session.showHint() },
                enabled = !session.gameOver && session.turn == session.humanDisc,
            ) {
                Text("Подсказка")
            }
        }
    }
}

/** Рисует фишку с мягким градиентом. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDisc(
    center: Offset,
    radius: Float,
    white: Boolean,
    alpha: Float = 1f,
) {
    val brush = if (white) {
        Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFD6DCFF)),
            center = center,
            radius = radius,
        )
    } else {
        Brush.radialGradient(
            colors = listOf(Color(0xFF2A3266), Color(0xFF10132E)),
            center = center,
            radius = radius,
        )
    }
    drawCircle(
        brush = brush,
        radius = radius,
        center = center,
        alpha = alpha,
    )
}