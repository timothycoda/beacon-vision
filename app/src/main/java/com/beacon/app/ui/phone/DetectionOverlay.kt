package com.beacon.app.ui.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beacon.app.ui.theme.brandAccentStatic
import com.beacon.data.vision.DetectionDisplayLabel
import com.beacon.domain.guidance.GuidanceLanguage
import com.beacon.domain.vision.DetectedObject

private val BOX_COLOR_EXTRAS = listOf(
    Color(0xFF00E5FF),
    Color(0xFFFFB300),
    Color(0xFFE040FB),
    Color(0xFF69F0AE),
    Color(0xFFFF5252),
)

@Composable
fun DetectionOverlay(
    objects: List<DetectedObject>,
    guidanceLanguage: GuidanceLanguage,
    modifier: Modifier = Modifier,
) {
    val primary = brandAccentStatic()
    val boxColors = listOf(primary) + BOX_COLOR_EXTRAS
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = if (objects.isEmpty()) {
                    "No objects detected yet"
                } else {
                    "Detected: ${objects.joinToString {
                        DetectionDisplayLabel.format(it.label, guidanceLanguage)
                    }}"
                }
            },
    ) {
        val frameWidthPx = with(density) { maxWidth.toPx() }
        val frameHeightPx = with(density) { maxHeight.toPx() }

        objects.forEach { obj ->
            val color = boxColors[obj.colorIndex % boxColors.size]
            val label = DetectionDisplayLabel.format(obj.label, guidanceLanguage)

            val leftDp = with(density) { (obj.bounds.left * frameWidthPx).toDp() }
            val topDp = with(density) { (obj.bounds.top * frameHeightPx).toDp() }
            val widthDp = with(density) {
                ((obj.bounds.right - obj.bounds.left) * frameWidthPx).toDp().coerceAtLeast(40.dp)
            }
            val heightDp = with(density) {
                ((obj.bounds.bottom - obj.bounds.top) * frameHeightPx).toDp().coerceAtLeast(40.dp)
            }

            Box(
                modifier = Modifier
                    .offset(x = leftDp, y = topDp)
                    .width(widthDp)
                    .height(heightDp)
                    .border(3.dp, color, RoundedCornerShape(6.dp)),
            ) {
                Text(
                    text = label.ifBlank { "object" },
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(
                            Color.Black.copy(alpha = 0.72f),
                            RoundedCornerShape(bottomEnd = 6.dp, topStart = 4.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}
