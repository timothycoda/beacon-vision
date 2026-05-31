package com.beacon.app.ui.phone

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.beacon.app.ui.theme.BeaconLime
import com.beacon.domain.vision.DetectedObject

private val BOX_COLORS = listOf(
    BeaconLime,
    Color(0xFF00E5FF),
    Color(0xFFFFB300),
    Color(0xFFE040FB),
    Color(0xFF69F0AE),
    Color(0xFFFF5252),
)

@Composable
fun DetectionOverlay(
    objects: List<DetectedObject>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = if (objects.isEmpty()) {
                    "No objects detected yet"
                } else {
                    "Detected: ${objects.joinToString { it.label }}"
                }
            },
    ) {
        val stroke = Stroke(width = 4.dp.toPx())
        objects.forEach { obj ->
            val color = BOX_COLORS[obj.colorIndex % BOX_COLORS.size]
            val left = obj.bounds.left * size.width
            val top = obj.bounds.top * size.height
            val width = (obj.bounds.right - obj.bounds.left) * size.width
            val height = (obj.bounds.bottom - obj.bounds.top) * size.height
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(width.coerceAtLeast(1f), height.coerceAtLeast(1f)),
                style = stroke,
            )
        }
    }
}
