package com.bugsjkeeee.fitness.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Orange = Color(0xFFFF6B35)
private val OrangeDark = Color(0xFFE85A26)
private val Teal = Color(0xFF2EC4B6)
private val TealDark = Color(0xFF1A9E92)

private val DarkColors = darkColorScheme(
    primary = Orange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5A2410),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Teal,
    onSecondary = Color(0xFF00201D),
    secondaryContainer = Color(0xFF0F4A45),
    onSecondaryContainer = Color(0xFFB2F1EA),
    background = Color(0xFF121214),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF121214),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF26262B),
    onSurfaceVariant = Color(0xFFC8C5CA),
    surfaceContainer = Color(0xFF1C1C20),
    surfaceContainerHigh = Color(0xFF232328),
    surfaceContainerHighest = Color(0xFF2B2B31),
    outline = Color(0xFF6E6E76),
)

private val LightColors = lightColorScheme(
    primary = OrangeDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = TealDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2F1EA),
    onSecondaryContainer = Color(0xFF00201D),
    background = Color(0xFFFBF8F6),
    surface = Color(0xFFFBF8F6),
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold, fontSize = 34.sp),
    )
}

@Composable
fun FitnessTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
