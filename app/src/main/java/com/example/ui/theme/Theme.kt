package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val JarvisGoldenHolographicScheme = darkColorScheme(
    primary = SolarGold,
    onPrimary = ObsidianBlack,
    primaryContainer = DeepBronzeGold,
    onPrimaryContainer = RadiantChampagne,
    secondary = HoloCyan,
    onSecondary = ObsidianBlack,
    secondaryContainer = HudSurfaceElevated,
    onSecondaryContainer = TextPrimaryGoldWhite,
    tertiary = MoltenAmber,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextPrimaryGoldWhite,
    surface = DeepNavy,
    onSurface = TextPrimaryGoldWhite,
    surfaceVariant = HudSurface,
    onSurfaceVariant = TextSecondaryMuted,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = DeepNavy,
    surfaceContainer = HudSurface,
    surfaceContainerHigh = HudSurfaceElevated,
    outline = HudSurfaceBorder,
    outlineVariant = DeepBronzeGold,
    error = HoloCrimson,
    onError = TextPrimaryGoldWhite
)

private val JarvisShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisGoldenHolographicScheme,
        typography = Typography,
        shapes = JarvisShapes,
        content = content
    )
}
