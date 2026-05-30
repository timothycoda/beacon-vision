package com.beacon.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

// Bold, geometric, tightly-set type to match the reference's grotesque look.
private val tightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val BeaconTypography = Typography(
    displayMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1).sp, lineHeightStyle = tightLineHeight),
    displaySmall = TextStyle(fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp, lineHeightStyle = tightLineHeight),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp, lineHeightStyle = tightLineHeight),
    headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 30.sp, lineHeightStyle = tightLineHeight),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 19.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp),
)
