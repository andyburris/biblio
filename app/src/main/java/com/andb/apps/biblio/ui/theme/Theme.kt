package com.andb.apps.biblio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = BiblioColorScheme(
    background = Color.White,
    onBackground = Color.Black,
    onBackgroundSecondary = Color.Black.copy(alpha = 0.5f),
    onBackgroundTertiary = Color.Black.copy(alpha = 0.3f),
    divider = Color.Black.copy(alpha = 0.12f),
    surface = Color.Black.copy(alpha = 0.05f),
)

private object NoRipple : RippleTheme {
    @Composable override fun defaultColor() = Color.Unspecified
    @Composable override fun rippleAlpha(): RippleAlpha = RippleAlpha(0.0f,0.0f,0.0f,0.0f)
}

internal val LocalColorScheme = staticCompositionLocalOf { LightColorScheme }
internal val LocalTypography = staticCompositionLocalOf { Typography }

object BiblioTheme {
    val colors: BiblioColorScheme
        @Composable get() = LocalColorScheme.current as BiblioColorScheme
    val typography: BiblioTypography
        @Composable get() = LocalTypography.current
}

@Composable
fun BiblioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    CompositionLocalProvider(
        LocalRippleTheme provides NoRipple,
        LocalColorScheme provides LightColorScheme,
        LocalTypography provides Typography,
    ) {
        ProvideTextStyle(value = Typography.body, content = content)
    }
}