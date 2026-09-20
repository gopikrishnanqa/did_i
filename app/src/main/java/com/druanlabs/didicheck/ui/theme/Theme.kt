package com.druanlabs.didicheck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueSoft,
    onPrimaryContainer = BrandInk,
    secondary = BrandGreen,
    onSecondary = Color.White,
    secondaryContainer = BrandGreenSoft,
    onSecondaryContainer = BrandInk,
    tertiary = BrandPurple,
    onTertiary = Color.White,
    tertiaryContainer = BrandPurpleSoft,
    onTertiaryContainer = BrandInk,
    background = BrandCanvas,
    onBackground = BrandInk,
    surface = Color.White,
    onSurface = BrandInk,
    surfaceVariant = BrandCloud,
    onSurfaceVariant = BrandMuted,
    outline = BrandOutline,
    outlineVariant = BrandOutlineSoft,
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = RecallIndigoDark,
    onPrimary = NightInk,
    primaryContainer = Color(0xFF243A6B),
    onPrimaryContainer = Color(0xFFE4EAF8),
    secondary = Color(0xFF7DCEA0),
    onSecondary = NightInk,
    background = NightInk,
    onBackground = Color(0xFFE8E7E2),
    surface = NightSurface,
    onSurface = Color(0xFFE8E7E2),
    surfaceVariant = Color(0xFF262A35),
    onSurfaceVariant = Color(0xFFB7B9C4),
    outline = Color(0xFF3A3E4A),
    outlineVariant = Color(0xFF2A2E38),
)

@Composable
fun DidITheme(
    // Product is designed for the light canvas + blue/green accents.
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val useDark = darkTheme && isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        typography = DidITypography,
        content = content,
    )
}
