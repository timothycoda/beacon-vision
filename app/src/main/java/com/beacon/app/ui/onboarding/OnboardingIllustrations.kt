package com.beacon.app.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.beacon.app.ui.theme.BeaconCardDark
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.app.ui.theme.BeaconOnDarkMuted
import com.beacon.app.ui.theme.BeaconOutlineDark
import com.beacon.app.ui.theme.BeaconWhiteCard
import com.beacon.app.ui.theme.BeaconYellow

@Composable
fun AheadIllustration(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(220.dp)
            .background(BeaconCardDark, RoundedCornerShape(32.dp))
            .clearAndSetSemantics {},
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Horizon line
            drawLine(
                color = BeaconOutlineDark,
                start = Offset(w * 0.1f, h * 0.62f),
                end = Offset(w * 0.9f, h * 0.62f),
                strokeWidth = 4f,
            )
            // Path ahead (lime)
            drawRoundRect(
                color = accent,
                topLeft = Offset(w * 0.35f, h * 0.62f),
                size = Size(w * 0.3f, h * 0.08f),
                cornerRadius = CornerRadius(12f, 12f),
            )
            // Sun / scene accent
            drawCircle(
                color = BeaconYellow,
                radius = w * 0.08f,
                center = Offset(w * 0.75f, h * 0.28f),
            )
            // Glasses frame (minimal)
            drawRoundRect(
                color = BeaconOnDark,
                topLeft = Offset(w * 0.22f, h * 0.22f),
                size = Size(w * 0.56f, h * 0.18f),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 6f),
            )
            drawLine(
                color = BeaconOnDark,
                start = Offset(w * 0.5f, h * 0.31f),
                end = Offset(w * 0.5f, h * 0.36f),
                strokeWidth = 5f,
            )
        }
    }
}

@Composable
fun ReadIllustration(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(220.dp)
            .background(BeaconCardDark, RoundedCornerShape(32.dp))
            .clearAndSetSemantics {},
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Document card
            drawRoundRect(
                color = BeaconWhiteCard,
                topLeft = Offset(w * 0.2f, h * 0.18f),
                size = Size(w * 0.6f, h * 0.64f),
                cornerRadius = CornerRadius(16f, 16f),
            )
            // Text lines
            val lineColor = BeaconCardDark
            repeat(4) { i ->
                val y = h * (0.32f + i * 0.1f)
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset(w * 0.28f, y),
                    size = Size(w * (0.44f - i * 0.04f), h * 0.04f),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
            // Sound waves
            val cx = w * 0.78f
            val cy = h * 0.72f
            listOf(0.06f, 0.09f, 0.12f).forEachIndexed { i, r ->
                drawCircle(
                    color = accent,
                    radius = w * r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 3f),
                )
            }
        }
    }
}

@Composable
fun HandsFreeIllustration(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(220.dp)
            .background(BeaconCardDark, RoundedCornerShape(32.dp))
            .clearAndSetSemantics {},
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Temple touch zone
            drawRoundRect(
                color = BeaconYellow,
                topLeft = Offset(w * 0.58f, h * 0.35f),
                size = Size(w * 0.12f, h * 0.22f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            // Glasses outline
            drawRoundRect(
                color = BeaconOnDark,
                topLeft = Offset(w * 0.18f, h * 0.38f),
                size = Size(w * 0.64f, h * 0.2f),
                cornerRadius = CornerRadius(28f, 28f),
                style = Stroke(width = 6f),
            )
            // Mic pulse
            drawCircle(color = accent, radius = w * 0.07f, center = Offset(w * 0.5f, h * 0.72f))
            val path = Path().apply {
                moveTo(w * 0.32f, h * 0.72f)
                cubicTo(w * 0.38f, h * 0.58f, w * 0.62f, h * 0.58f, w * 0.68f, h * 0.72f)
            }
            drawPath(path, color = accent, style = Stroke(width = 4f))
            // Command chip
            drawRoundRect(
                color = BeaconOutlineDark,
                topLeft = Offset(w * 0.22f, h * 0.78f),
                size = Size(w * 0.56f, h * 0.1f),
                cornerRadius = CornerRadius(12f, 12f),
            )
            drawRoundRect(
                color = accent,
                topLeft = Offset(w * 0.26f, h * 0.81f),
                size = Size(w * 0.2f, h * 0.04f),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }
    }
}

@Composable
fun GlassesOverviewIllustration(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(220.dp)) {
        AheadIllustration(Modifier.fillMaxSize())
    }
}

@Composable
fun HausaIllustration(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(220.dp)
            .background(BeaconCardDark, RoundedCornerShape(32.dp))
            .clearAndSetSemantics {},
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = BeaconWhiteCard,
                topLeft = Offset(w * 0.18f, h * 0.2f),
                size = Size(w * 0.64f, h * 0.22f),
                cornerRadius = CornerRadius(14f, 14f),
            )
            drawRoundRect(
                color = accent,
                topLeft = Offset(w * 0.24f, h * 0.28f),
                size = Size(w * 0.28f, h * 0.06f),
                cornerRadius = CornerRadius(6f, 6f),
            )
            drawCircle(color = accent, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.62f))
            listOf(0.14f, 0.2f, 0.26f).forEachIndexed { i, r ->
                drawCircle(
                    color = accent.copy(alpha = 0.25f + i * 0.15f),
                    radius = w * r,
                    center = Offset(w * 0.5f, h * 0.62f),
                    style = Stroke(width = 3f),
                )
            }
        }
    }
}

@Composable
fun TrustedHelpersIllustration(modifier: Modifier = Modifier) {
    PhoneHelpIllustration(modifier)
}

@Composable
fun PhoneCameraIllustration(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(220.dp)
            .background(BeaconCardDark, RoundedCornerShape(32.dp))
            .clearAndSetSemantics {},
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = BeaconWhiteCard,
                topLeft = Offset(w * 0.22f, h * 0.12f),
                size = Size(w * 0.56f, h * 0.76f),
                cornerRadius = CornerRadius(20f, 20f),
            )
            drawCircle(color = accent, radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.42f))
            drawRoundRect(
                color = accent.copy(alpha = 0.35f),
                topLeft = Offset(w * 0.3f, h * 0.55f),
                size = Size(w * 0.4f, h * 0.12f),
                cornerRadius = CornerRadius(8f, 8f),
            )
        }
    }
}

@Composable
fun PhoneHelpIllustration(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(220.dp)
            .background(BeaconCardDark, RoundedCornerShape(32.dp))
            .clearAndSetSemantics {},
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = BeaconOutlineDark,
                topLeft = Offset(w * 0.2f, h * 0.25f),
                size = Size(w * 0.6f, h * 0.5f),
                cornerRadius = CornerRadius(16f, 16f),
            )
            drawCircle(color = accent, radius = w * 0.08f, center = Offset(w * 0.35f, h * 0.45f))
            drawCircle(color = accent, radius = w * 0.08f, center = Offset(w * 0.65f, h * 0.45f))
            drawRoundRect(
                color = accent,
                topLeft = Offset(w * 0.32f, h * 0.68f),
                size = Size(w * 0.36f, h * 0.08f),
                cornerRadius = CornerRadius(12f, 12f),
            )
        }
    }
}
