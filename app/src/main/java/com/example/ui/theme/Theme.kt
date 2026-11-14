package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LuxuryDarkColorScheme =
  darkColorScheme(
    primary = MetallicGold,
    onPrimary = MatteBlack,
    primaryContainer = GoldTintSurface,
    onPrimaryContainer = BrightGold,
    secondary = BrightGold,
    onSecondary = MatteBlack,
    secondaryContainer = ElevatedSurface,
    onSecondaryContainer = IvoryText,
    tertiary = EmeraldProfit,
    onTertiary = MatteBlack,
    background = MatteBlack,
    onBackground = IvoryText,
    surface = DeepObsidian,
    onSurface = IvoryText,
    surfaceVariant = CardSurface,
    onSurfaceVariant = WarmMuted,
    surfaceContainerLowest = MatteBlack,
    surfaceContainerLow = DeepObsidian,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = ElevatedSurface,
    surfaceContainerHighest = GoldTintSurface,
    outline = HairlineGold,
    outlineVariant = SubtleDivider,
    error = CrimsonAlert,
    onError = IvoryText,
  )

private val LuxuryShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )

@Composable
fun TwoMStoreTheme(
  forceRtl: Boolean = true,
  content: @Composable () -> Unit,
) {
  val layoutDirection = if (forceRtl) LayoutDirection.Rtl else LocalLayoutDirection.current
  CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
    MaterialTheme(
      colorScheme = LuxuryDarkColorScheme,
      typography = AppTypography,
      shapes = LuxuryShapes,
      content = content,
    )
  }
}
