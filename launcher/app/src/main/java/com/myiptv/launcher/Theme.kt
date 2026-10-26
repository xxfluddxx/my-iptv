package com.myiptv.launcher

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

object BoxColors {
    val Background = Color(0xFF0E1013)
    val Tile = Color(0xFF1A1E24)
    val TileQuiet = Color(0xFF15181D)
    val Menu = Color(0xFF1C2026)
    val TextPrimary = Color(0xFFE8EAED)
    val TextSecondary = Color(0xFF9AA0A6)
    val TextMuted = Color(0xFFC3C8CF)
    val Accent = Color(0xFF7CC4FA)
}

@Composable
fun BoxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = BoxColors.Accent,
            background = BoxColors.Background,
            surface = BoxColors.Tile,
            onSurface = BoxColors.TextPrimary,
            onBackground = BoxColors.TextPrimary,
        ),
        content = content,
    )
}
