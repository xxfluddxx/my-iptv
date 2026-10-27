package com.myiptv.carlauncher

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object CarColors {
    val Background = Color(0xFF0B0D10)
    val Card = Color(0xFF161A20)
    val CardRaised = Color(0xFF1E232B)
    val Menu = Color(0xFF1C2026)
    val TextPrimary = Color(0xFFE8EAED)
    val TextSecondary = Color(0xFF9AA0A6)
    val Accent = Color(0xFF4FD1A5)
    val OnAccent = Color(0xFF07130F)
}

@Composable
fun CarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CarColors.Accent,
            onPrimary = CarColors.OnAccent,
            background = CarColors.Background,
            surface = CarColors.Card,
            onSurface = CarColors.TextPrimary,
            onBackground = CarColors.TextPrimary,
        ),
        content = content,
    )
}
