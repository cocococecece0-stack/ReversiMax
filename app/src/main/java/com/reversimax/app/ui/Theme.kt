package com.reversimax.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Space = Color(0xFF0B1026)
val SpaceSurface = Color(0xFF151C3D)
val SpaceElevated = Color(0xFF1E2650)
val Accent = Color(0xFF7C8CFF)
val AccentWarm = Color(0xFFFFB35C)
val Ink = Color(0xFFE9ECFF)
val Muted = Color(0xFF9AA3CF)

@Composable
fun ReversiMaxTheme(content: @Composable () -> Unit) {
    // Приложение всегда в космической тёмной палитре — так доска смотрится лучше
    val scheme = darkColorScheme(
        primary = Accent,
        secondary = AccentWarm,
        background = Space,
        surface = SpaceSurface,
        onPrimary = Space,
        onBackground = Ink,
        onSurface = Ink,
    )
    val typography = Typography(
        headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Ink),
        titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Ink),
        bodyLarge = TextStyle(fontSize = 16.sp, color = Ink),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink),
    )
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}