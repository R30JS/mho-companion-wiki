package com.mho.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MhoDarkColors = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color(0xFF001417),
    primaryContainer = Color(0xFF11343B),
    onPrimaryContainer = Color(0xFFB8F4FF),
    secondary = AccentViolet,
    onSecondary = Color(0xFF1B0A2A),
    tertiary = AccentGreen,
    background = OledBg,
    onBackground = TextPrimary,
    surface = GlassSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = Color(0xFF21262D),
    error = WeakRed,
    onError = Color(0xFF2A0000)
)

@Composable
fun MhoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MhoDarkColors,
        typography = MhoTypography,
        content = content
    )
}
