package com.example.mobileclient.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LightColors = AppColors(
    background = StarkWhite,
    foreground = StarkBlack,
    card = StarkWhite,
    cardForeground = StarkBlack,
    primary = Color(0xFF6366F1),
    primaryForeground = StarkWhite,
    border = SoftBorderLight,
    mutedForeground = MutedTextLight,
    isDark = false
)

val DarkColors = AppColors(
    background = PureBlack,
    foreground = NearWhite,
    card = DeepCardDark,
    cardForeground = NearWhite,
    primary = Color(0xFF818CF8),
    primaryForeground = PureBlack,
    border = CharcoalBorderDark,
    mutedForeground = MutedTextDark,
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
