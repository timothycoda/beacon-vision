package com.beacon.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Beacon's bento dark theme. The reference design is dark-first with bold
 * lime/yellow/white accents, so the app uses a single high-contrast dark scheme
 * for a consistent, accessible look.
 */
private val BentoDarkColors = darkColorScheme(
    primary = BeaconLime,
    onPrimary = BeaconLimeText,
    secondary = BeaconYellow,
    onSecondary = BeaconYellowText,
    background = BeaconBackground,
    onBackground = BeaconOnDark,
    surface = BeaconCardDark,
    onSurface = BeaconOnDark,
    surfaceVariant = BeaconCardDarkAlt,
    onSurfaceVariant = BeaconOnDarkMuted,
    outline = BeaconOutlineDark,
    error = BeaconDanger,
    onError = BeaconDangerText,
)

@Composable
fun BeaconTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = BentoDarkColors,
        typography = BeaconTypography,
        shapes = BeaconShapes,
        content = content,
    )
}
