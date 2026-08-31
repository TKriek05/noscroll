package com.tkriek.scrollless.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Ink,
    secondary = MintDark,
    background = Ink,
    onBackground = Sand,
    surface = InkSoft,
    onSurface = Sand,
    surfaceVariant = InkSoft,
    onSurfaceVariant = Sand.copy(alpha = 0.7f),
    error = Coral
)

private val LightColors = lightColorScheme(
    primary = MintDark,
    onPrimary = SandSoft,
    secondary = Mint,
    background = Sand,
    onBackground = Ink,
    surface = SandSoft,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = Ink.copy(alpha = 0.7f),
    error = Coral
)

@Composable
fun ScrollLessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ScrollLessTypography,
        content = content
    )
}
