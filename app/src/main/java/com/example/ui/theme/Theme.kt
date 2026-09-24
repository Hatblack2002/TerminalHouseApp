package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TerminalHouseDarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    onPrimary = Color.White,
    primaryContainer = AccentOrangeContainer,
    onPrimaryContainer = AccentOrangeHover,
    secondary = AccentOrangeHover,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = CardBorderDark,
    outlineVariant = Color(0xFF1E1E26)
)

val TerminalHouseLightColorScheme = lightColorScheme(
    primary = AccentOrange,
    onPrimary = Color.White,
    primaryContainer = AccentOrangeContainer,
    onPrimaryContainer = AccentOrangeDark,
    secondary = AccentOrangeDark,
    onSecondary = Color.White,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = TextPrimaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = CardBorderLight,
    outlineVariant = Color(0xFFE5E7EB)
)

@Composable
fun TerminalHouseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TerminalHouseDarkColorScheme else TerminalHouseLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
