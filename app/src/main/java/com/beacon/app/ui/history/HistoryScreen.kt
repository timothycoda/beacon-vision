package com.beacon.app.ui.history

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.theme.BeaconCardDark
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.domain.history.HistoryType
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(
            title = "History",
            subtitle = "Your last descriptions, readings, and emergency alerts.",
        )

        if (entries.isEmpty()) {
            Text(
                text = "Nothing here yet. Use What is ahead, Read this, or Emergency.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        } else {
            entries.forEach { entry ->
                val label = when (entry.type) {
                    HistoryType.AHEAD -> "What is ahead"
                    HistoryType.READ -> "Read this"
                    HistoryType.EMERGENCY -> "Emergency"
                }
                val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    .format(Date(entry.timestampMs))
                BentoCard(
                    title = label,
                    onClick = {},
                    containerColor = BeaconCardDark,
                    contentColor = BeaconOnDark,
                    value = entry.summary,
                    minHeight = BeaconDimens.bentoWideMinHeight,
                    showArrow = false,
                    enabled = false,
                    contentDescription = "$label, $time. ${entry.summary}",
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BentoCard(
                title = "Clear history",
                onClick = viewModel::clearAll,
                containerColor = BeaconCardDark,
                contentColor = BeaconOnDark,
                minHeight = BeaconDimens.bentoWideMinHeight,
                contentDescription = "Clear all history entries.",
            )
        }

        BentoCard(
            title = "Back",
            onClick = onBack,
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Go back to settings.",
        )
    }
}
