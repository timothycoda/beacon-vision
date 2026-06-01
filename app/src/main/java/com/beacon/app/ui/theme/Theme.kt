package com.beacon.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.beacon.app.BuildConfig

private val BeaconBentoColors = darkColorScheme(
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

private val EleniiBentoColors = darkColorScheme(
    primary = EleniiSkyBlue,
    onPrimary = EleniiSkyBlueOn,
    secondary = EleniiPrimaryBrand,
    onSecondary = EleniiOnDark,
    background = EleniiBackground,
    onBackground = EleniiOnDark,
    surface = EleniiSurface,
    onSurface = EleniiOnDark,
    surfaceVariant = EleniiSurfaceAlt,
    onSurfaceVariant = EleniiOnDarkMuted,
    outline = EleniiOutline,
    error = BeaconDanger,
    onError = BeaconDangerText,
)

private val AppColorScheme
    get() = if (BuildConfig.BRAND == "elenii") EleniiBentoColors else BeaconBentoColors

/**
 * Shared Compose theme for Beacon and Elenii product flavors. Visual tokens come
 * from [AppColorScheme]; prefer MaterialTheme.colorScheme over hardcoded brand colors.
 */
@Composable
fun BeaconTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = BeaconTypography,
        shapes = BeaconShapes,
        content = content,
    )
}

/** Bento white action cards (shared layout token). */
@Composable
fun bentoWhiteCardColor(): Color = BeaconWhiteCard

@Composable
fun bentoWhiteCardOnColor(): Color = BeaconWhiteCardText
