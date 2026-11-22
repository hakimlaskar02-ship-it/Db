package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class DpBossExtendedColors(
    val midnight: Color = DeepMidnight,
    val navy: Color = DarkNavy,
    val card: Color = CardBackground,
    val cardElevated: Color = CardElevated,
    val borderSubtle: Color = CardBorderSubtle,
    val electricBlue: Color = ElectricBlue,
    val electricBlueSoft: Color = ElectricBlueSoft,
    val neonPurple: Color = NeonPurple,
    val neonPurpleSoft: Color = NeonPurpleSoft,
    val cyan: Color = CyanAccent,
    val cyanSoft: Color = CyanAccentSoft,
    val emerald: Color = EmeraldGreen,
    val emeraldSoft: Color = EmeraldGreenSoft,
    val warning: Color = WarningOrange,
    val warningSoft: Color = WarningOrangeSoft,
    val danger: Color = ErrorRed,
    val dangerSoft: Color = ErrorRedSoft,
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textMuted: Color = TextMuted,
    val divider: Color = DividerDark
)

val LocalDpBossColors = staticCompositionLocalOf { DpBossExtendedColors() }

private val DarkFintechColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF163263),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D1E54),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = CyanAccent,
    onTertiary = DeepMidnight,
    tertiaryContainer = Color(0xFF0C3B44),
    onTertiaryContainer = Color(0xFFCFF8FD),
    background = DeepMidnight,
    onBackground = TextPrimary,
    surface = DarkNavy,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = DeepMidnight,
    surfaceContainerLow = DarkNavy,
    surfaceContainer = CardBackground,
    surfaceContainerHigh = CardElevated,
    surfaceContainerHighest = Color(0xFF23304A),
    outline = CardBorderSubtle,
    outlineVariant = DividerDark,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF4B1320),
    onErrorContainer = Color(0xFFFFD9DF)
)

private val DpBossShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun DpBossAnalyticsTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalDpBossColors provides DpBossExtendedColors()
    ) {
        MaterialTheme(
            colorScheme = DarkFintechColorScheme,
            typography = DpBossTypography,
            shapes = DpBossShapes,
            content = content
        )
    }
}

object DpBossTheme {
    val colors: DpBossExtendedColors
        @Composable get() = LocalDpBossColors.current
}
