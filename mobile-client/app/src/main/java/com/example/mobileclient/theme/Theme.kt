package com.example.mobileclient.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightColors = AppColors(
    background = StarkWhite,
    foreground = StarkBlack,
    card = Color(0xFFF5F5F5),
    cardForeground = StarkBlack,
    primary = StarkBlack,
    primaryForeground = StarkWhite,
    border = Color(0xFFE5E5E5),
    mutedForeground = Color(0xFF737373),
    isDark = false
)

val DarkColors = AppColors(
    background = PureBlack,
    foreground = StarkWhite,
    card = Color(0xFF0C0C0C),
    cardForeground = StarkWhite,
    primary = StarkWhite,
    primaryForeground = PureBlack,
    border = Color(0xFF222222),
    mutedForeground = Color(0xFF888888),
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf { LightColors }
val LocalAppTypography = staticCompositionLocalOf { AppTypography() }

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable
        get() = LocalAppTypography.current
}

@Composable
fun MobileClientTheme(
    darkTheme: Boolean = ThemeManager.isDark,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val typography = AppTypography()

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        content = content
    )
}
