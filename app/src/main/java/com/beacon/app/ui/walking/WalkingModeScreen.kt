package com.beacon.app.ui.walking

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.ui.accessibility.ScreenVoiceIntro
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.domain.accessibility.AccessibilityScreenIds
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.theme.BeaconCardDark
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.app.ui.theme.BeaconYellow
import com.beacon.app.ui.theme.BeaconYellowText

@Composable
fun WalkingModeScreen(
    onBack: () -> Unit,
    viewModel: WalkingModeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshRunningState() }

    ScreenVoiceIntro(AccessibilityScreenIds.WALKING_MODE)

    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(
            title = "Walking mode",
            subtitle = "Short cues about what is ahead while you walk.",
        )

        Text(
            text = state.statusLine,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        if (!state.isRunning) {
            BentoCard(
                title = "Start walking",
                onClick = viewModel::start,
                containerColor = BeaconYellow,
                contentColor = BeaconYellowText,
                leadingIcon = Icons.Filled.DirectionsWalk,
                minHeight = BeaconDimens.bentoLargeMinHeight,
                enabled = state.glassesConnected,
                contentDescription = "Start walking mode. Periodic guidance while you walk.",
            )
        } else {
            BentoCard(
                title = "Stop walking",
                onClick = viewModel::stop,
                containerColor = BeaconYellow,
                contentColor = BeaconYellowText,
                leadingIcon = Icons.Filled.Stop,
                minHeight = BeaconDimens.bentoLargeMinHeight,
                contentDescription = "Stop walking mode.",
            )
        }

        BentoCard(
            title = "Back",
            onClick = {
                if (state.isRunning) viewModel.stop()
                onBack()
            },
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Go back to home.",
        )
    }
}
