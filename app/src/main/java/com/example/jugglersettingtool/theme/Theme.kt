package com.example.jugglersettingtool.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = NeonPink,
    tertiary = NeonYellow,
    background = DarkBackground,
    surface = CardBackground,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = DarkBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun JugglerSettingToolTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
