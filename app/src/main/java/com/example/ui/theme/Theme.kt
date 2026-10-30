package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color.White,
    primaryContainer = CoralLight,
    onPrimaryContainer = CoralDark,

    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealLight,
    onSecondaryContainer = TealDark,

    tertiary = PurpleAccent,
    onTertiary = Color.White,
    tertiaryContainer = PurpleLight,
    onTertiaryContainer = PurpleAccent,

    background = SoftCreamBackground,
    onBackground = CharcoalText,

    surface = SoftSurfaceWhite,
    onSurface = CharcoalText,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = MutedSlate,

    outline = SoftBorder
)

@Composable
fun CuentaConmigoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
