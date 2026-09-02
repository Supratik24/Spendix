package com.smartspend.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = OnIndigoContainerDark,
    secondary = SlateSecondaryDark,
    background = SlateBgDark,
    surface = SlateSurfaceDark,
    onSurface = SlateTextDark
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = OnIndigoContainerLight,
    secondary = SlateSecondaryLight,
    background = SlateBgLight,
    surface = SlateSurfaceLight,
    onSurface = SlateTextLight
)

@Composable
fun SmartSpendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
