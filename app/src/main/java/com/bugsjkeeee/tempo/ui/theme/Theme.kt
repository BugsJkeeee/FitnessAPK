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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugsjkeeee.tempo.R

/** Акцент из макета редизайна. */
val Accent = Color(0xFFFF5C00)
val Positive = Color(0xFF34C759)

/** Шрифт Inter (SIL Open Font License), начертания 300–800. */
val Inter = FontFamily(
    Font(R.font.inter_300, FontWeight.Light),
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
    Font(R.font.inter_800, FontWeight.ExtraBold),
)

/** Цвета фаз таймера: фон и цвет текста. */
@Immutable
data class PhasePalette(
    val prep: Color, val onPrep: Color,
    val work: Color, val onWork: Color,
    val rest: Color, val onRest: Color,
    /** Подложка кнопок управления таймером в фазе работы. */
    val workControl: Color,
    val workTrack: Color,
)

private val LightPhases = PhasePalette(
    prep = Accent, onPrep = Color.White,
    work = Color(0xFFFAFAFA), onWork = Color(0xFF1A1A1A),
    rest = Color(0xFFE8FFB0), onRest = Color(0xFF1A1A1A),
    workControl = Color.White, workTrack = Color(0xFFE8E8E8),
)

private val DarkPhases = PhasePalette(
    prep = Accent, onPrep = Color(0xFF111111),
    work = Color.Black, onWork = Color.White,
    rest = Color(0xFFC8FF00), onRest = Color(0xFF111111),
    workControl = Color.Transparent, workTrack = Color(0x26FFFFFF),
)

/** Токены оформления, которые отличаются между тёмной и светлой темой. */
@Immutable
data class TempoStyle(
    val dark: Boolean,
    /** Фон карточек. */
    val tile: Color,
    /** Рамка карточек (в светлой теме вместо рамки — мягкая тень). */
    val tileBorder: Color,
    val tileShape: Shape,
    val smallShape: Shape,
    val muted: Color,
    /** Фон неактивных чипов и полей. */
    val segmentIdle: Color,
    val chipText: Color,
    /** Обводка «пустых» чипов и второстепенных кнопок. */
    val outline: Color,
    val secondaryText: Color,
    val positive: Color,
    val phases: PhasePalette,
)

val LocalTempoStyle = staticCompositionLocalOf {
    TempoStyle(
        false, Color.White, Color.Transparent, RoundedCornerShape(16.dp), RoundedCornerShape(12.dp),
        Color.Gray, Color.LightGray, Color.DarkGray, Color.LightGray, Color.Gray, Positive, LightPhases,
    )
}

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Positive,
    background = Color(0xFF0B0B0B),
    onBackground = Color.White,
    surface = Color(0xFF0B0B0B),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFF8E8E93),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF252525),
    outline = Color(0xFF2E2E2E),
    error = Color(0xFFFF453A),
)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Positive,
    background = Color(0xFFF7F7F8),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFF7F7F8),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF8E8E93),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF2F2F7),
    outline = Color(0xFFE5E5EA),
    error = Color(0xFFFF3B30),
)

private val DarkStyle = TempoStyle(
    dark = true,
    tile = Color(0xFF1A1A1A),
    tileBorder = Color(0xFF2E2E2E),
    tileShape = RoundedCornerShape(16.dp),
    smallShape = RoundedCornerShape(12.dp),
    muted = Color(0xFF8E8E93),
    segmentIdle = Color(0xFF252525),
    chipText = Color(0xFFBBBBBB),
    outline = Color(0xFF2E2E2E),
    secondaryText = Color(0xFFBBBBBB),
    positive = Positive,
    phases = DarkPhases,
)

private val LightStyle = TempoStyle(
    dark = false,
    tile = Color.White,
    tileBorder = Color.Transparent,
    tileShape = RoundedCornerShape(16.dp),
    smallShape = RoundedCornerShape(12.dp),
    muted = Color(0xFF8E8E93),
    segmentIdle = Color(0xFFF2F2F7),
    chipText = Color(0xFF3A3A3C),
    outline = Color(0xFFE5E5EA),
    secondaryText = Color(0xFF636366),
    positive = Color(0xFF28A745),
    phases = LightPhases,
)

/** Цифры одинаковой ширины: при отсчёте время не «прыгает». */
val Digits = TextStyle(fontFamily = Inter, fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold)

private val AppTypography = Typography().run {
    fun TextStyle.inter(weight: FontWeight? = null, size: Int? = null, spacing: Float? = null) = copy(
        fontFamily = Inter,
        fontWeight = weight ?: fontWeight,
        fontSize = size?.sp ?: fontSize,
        letterSpacing = spacing?.sp ?: letterSpacing,
    )
    copy(
        displayLarge = displayLarge.inter(),
        displayMedium = displayMedium.inter(),
        displaySmall = displaySmall.inter(FontWeight.Bold),
        headlineLarge = headlineLarge.inter(FontWeight.Bold, 28, -0.8f),
        headlineMedium = headlineMedium.inter(FontWeight.Bold, 24, -0.5f),
        headlineSmall = headlineSmall.inter(FontWeight.Bold, 20, -0.3f),
        titleLarge = titleLarge.inter(FontWeight.SemiBold, 18),
        titleMedium = titleMedium.inter(FontWeight.SemiBold, 15),
        titleSmall = titleSmall.inter(FontWeight.SemiBold, 14),
        bodyLarge = bodyLarge.inter(FontWeight.Normal, 15, 0f),
        bodyMedium = bodyMedium.inter(FontWeight.Normal, 14, 0f),
        bodySmall = bodySmall.inter(FontWeight.Normal, 12, 0f),
        labelLarge = labelLarge.inter(FontWeight.SemiBold, 13, 0f),
        labelMedium = labelMedium.inter(FontWeight.Medium, 12),
        labelSmall = labelSmall.inter(FontWeight.SemiBold, 11, 0.5f),
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
