package com.tertiaryinfotech.runtrackgps.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Accent ported from the iOS AccentColor (deep blue light / lighter blue dark).
val AccentLight = Color(0xFF1C4594)
val AccentDark = Color(0xFF5C8FF5)

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    secondary = AccentLight,
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF06122B),
    secondary = AccentDark,
)

private val AppTypography = Typography()

@Composable
fun RunTrackGPSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = colors.background.toArgb()
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
