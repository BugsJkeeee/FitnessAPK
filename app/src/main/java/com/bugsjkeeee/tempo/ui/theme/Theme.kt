package com.bugsjkeeee.tempo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Accent = Color(0xFFFF5B1F)
private val AccentLight = Color(0xFFF2551A)
val Positive = Color(0xFF2ECC71)

/** Цвета фаз таймера одинаковы в обеих темах. */
object PhaseColors {
    val Prep = Color(0xFFFFC531)
    val Work = Color(0xFFE53935)
    val Rest = Color(0xFF27A65A)
    val OnPrep = Color(0xFF1A1A1A)
    val OnWork = Color.White
    val OnRest = Color.White
}

/** Параметры плиток, которые отличаются между тёмной и светлой темой. */
@Immutable
data class TempoStyle(
    val tile: Color,
    val tileBorder: Color,
    val tileShape: Shape,
    val muted: Color,
    val segmentIdle: Color,
    val positive: Color,
)

val LocalTempoStyle = staticCompositionLocalOf {
    TempoStyle(Color.DarkGray, Color.Gray, RoundedCornerShape(14.dp), Color.Gray, Color.DarkGray, Positive)
}

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Positive,
    background = Color(0xFF0E0F11),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF0E0F11),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF1A1C20),
    onSurfaceVariant = Color(0xFF8E9299),
    surfaceContainer = Color(0xFF15171A),
    surfaceContainerHigh = Color(0xFF1A1C20),
    surfaceContainerHighest = Color(0xFF24272C),
    outline = Color(0xFF2A2D33),
    error = Color(0xFFFF5449),
)

private val LightColors = lightColorScheme(
    primary = AccentLight,
    onPrimary = Color.White,
    secondary = Color(0xFF1E9E52),
    background = Color(0xFFEEEEF0),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFEEEEF0),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF6B6F76),
    surfaceContainer = Color(0xFFF6F6F7),
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE6E7EA),
    outline = Color(0xFFE2E3E6),
)

private val DarkStyle = TempoStyle(
    tile = Color(0xFF1A1C20),
    tileBorder = Color(0xFF2A2D33),
    tileShape = RoundedCornerShape(14.dp),
    muted = Color(0xFF8E9299),
    segmentIdle = Color(0xFF24272C),
    positive = Positive,
)

private val LightStyle = TempoStyle(
    tile = Color.White,
    tileBorder = Color(0xFFE2E3E6),
    tileShape = RoundedCornerShape(4.dp),
    muted = Color(0xFF6B6F76),
    segmentIdle = Color(0xFFE6E7EA),
    positive = Color(0xFF1E9E52),
)

/** Цифры одинаковой ширины: при отсчёте время не «прыгает». */
val Digits = TextStyle(fontFeatureSettings = "tnum", fontWeight = FontWeight.Medium)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.8.sp),
    )
}

@Composable
fun TempoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTempoStyle provides if (darkTheme) DarkStyle else LightStyle) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
