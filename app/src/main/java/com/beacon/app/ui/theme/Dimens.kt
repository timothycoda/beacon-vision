package com.beacon.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Beacon layout constants. Prefer these over magic numbers so accessibility
 * targets stay consistent across screens.
 */
object BeaconDimens {
    /** Minimum primary action height (well above 48dp Material minimum). */
    val primaryButtonMinHeight = 88.dp

    /** Minimum secondary action height. */
    val secondaryButtonMinHeight = 72.dp

    /** Screen edge padding. */
    val screenHorizontalPadding = 24.dp
    val screenVerticalPadding = 32.dp

    /** Vertical gap between major blocks on a screen. */
    val sectionSpacing = 20.dp

    /** Inner padding for cards and list rows. */
    val cardPadding = 16.dp

    /** Corner radius for buttons and cards (bento: large, friendly, easy to tap). */
    val cornerExtraLarge = 28.dp
    val cornerLarge = 24.dp
    val cornerMedium = 18.dp

    /** Visible border on status cards for high contrast. */
    val cardBorderWidth = 2.dp

    val cardElevation = 0.dp

    val buttonElevation = 0.dp
    val buttonElevationPressed = 2.dp

    /** Bento card heights. */
    val bentoLargeMinHeight = 150.dp
    val bentoSmallMinHeight = 130.dp
    val bentoWideMinHeight = 96.dp

    /** Gap between bento cells. */
    val bentoGap = 14.dp

    /** Circular affordance (arrow / menu) size. */
    val arrowCircle = 44.dp
}
