package com.beacon.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Rounded, bold bento shapes — part of Beacon's visual language. */
val BeaconShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(BeaconDimens.cornerMedium),
    medium = RoundedCornerShape(BeaconDimens.cornerLarge),
    large = RoundedCornerShape(BeaconDimens.cornerLarge),
    extraLarge = RoundedCornerShape(BeaconDimens.cornerExtraLarge),
)
