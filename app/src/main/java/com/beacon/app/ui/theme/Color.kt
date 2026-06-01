package com.beacon.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.beacon.app.BuildConfig

// Beacon bento palette
val BeaconLime = Color(0xFFC5F23D)
val BeaconLimeText = Color(0xFF0E1A00)
val BeaconYellow = Color(0xFFF5C518)
val BeaconYellowText = Color(0xFF1A1400)

// Elenii brand palette
val EleniiSkyBlue = Color(0xFF38BDF8)
val EleniiSkyBlueOn = Color(0xFF050816)
val EleniiPrimaryBrand = Color(0xFF0B4DBA)
val EleniiDeepBrand = Color(0xFF0A3D91)
val EleniiBackground = Color(0xFF050816)
val EleniiBackgroundAlt = Color(0xFF0A1020)
val EleniiSurface = Color(0xFF111827)
val EleniiSurfaceAlt = Color(0xFF1A2235)
val EleniiOnDark = Color(0xFFFFFFFF)
val EleniiOnDarkMuted = Color(0xFFD1D5DB)
val EleniiOutline = Color(0xFF2A3655)
val EleniiConnectedChipBg = Color(0xFF10223D)

// Shared safety red (unchanged across brands)
val BeaconDanger = Color(0xFFFF3B30)
val BeaconDangerText = Color(0xFFFFFFFF)

// Beacon surfaces (also used as shared white-card tokens)
val BeaconBackground = Color(0xFF0A0A0A)
val BeaconCardDark = Color(0xFF1A1A1A)
val BeaconCardDarkAlt = Color(0xFF222222)
val BeaconOnDark = Color(0xFFFFFFFF)
val BeaconOnDarkMuted = Color(0xFFB5B5B5)
val BeaconOutlineDark = Color(0xFF3A3A3A)
val BeaconWhiteCard = Color(0xFFF4F5F7)
val BeaconWhiteCardText = Color(0xFF0A0A0A)
val BeaconChipUnselected = Color(0xFF1F1F1F)
val BeaconChipSelected = Color(0xFFFFFFFF)
val BeaconChipSelectedText = Color(0xFF0A0A0A)
val BeaconChipUnselectedText = Color(0xFFCFCFCF)

fun isEleniiBrand(): Boolean = BuildConfig.BRAND == "elenii"

/** Non-Composable accent for canvas/overlays. */
fun brandAccentStatic(): Color = if (isEleniiBrand()) EleniiSkyBlue else BeaconLime
