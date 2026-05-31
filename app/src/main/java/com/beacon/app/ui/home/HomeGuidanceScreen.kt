package com.beacon.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.ui.components.BeaconChip
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.theme.BeaconCardDark
import com.beacon.app.ui.theme.BeaconDanger
import com.beacon.app.ui.theme.BeaconDangerText
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconLime
import com.beacon.app.ui.theme.BeaconLimeText
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.app.ui.theme.BeaconWhiteCard
import com.beacon.app.ui.theme.BeaconWhiteCardText
import com.beacon.app.ui.theme.BeaconYellow
import com.beacon.app.ui.theme.BeaconYellowText
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.guidance.GuidanceLanguage
import kotlinx.coroutines.launch

@Composable
fun HomeGuidanceScreen(
    onOpenSettings: () -> Unit,
    onWhatIsAhead: () -> Unit,
    onReadThis: () -> Unit,
    onWalkingMode: () -> Unit,
    onEmergency: () -> Unit,
    onVoiceCommand: () -> Unit,
    onPhoneGuidance: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun comingSoon(feature: String) {
        scope.launch { snackbarHostState.showSnackbar("$feature is coming in a later update.") }
    }

    BeaconScreen {
        BeaconTopBar(
            title = "Beacon",
            trailingIcon = Icons.Filled.Settings,
            trailingContentDescription = "Settings",
            onTrailingClick = onOpenSettings,
        )

        ConnectionChips(state)
        ActiveModelChips(state)
        PhoneModeChip(
            phoneMode = state.phoneOnlyMode,
            onOpenPhone = { viewModel.enterPhoneMode(onPhoneGuidance) },
        )

        BentoCard(
            title = "Speak a command",
            onClick = onVoiceCommand,
            containerColor = BeaconLime,
            contentColor = BeaconLimeText,
            leadingIcon = Icons.Filled.Mic,
            minHeight = BeaconDimens.bentoLargeMinHeight,
            contentDescription = "Speak a command. Use your voice to control Beacon hands-free.",
        )

        BentoCard(
            title = "What is ahead?",
            onClick = onWhatIsAhead,
            containerColor = BeaconWhiteCard,
            contentColor = BeaconWhiteCardText,
            leadingIcon = Icons.Filled.Visibility,
            minHeight = BeaconDimens.bentoLargeMinHeight,
            contentDescription = "What is ahead. Describe what is in front of you.",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BeaconDimens.bentoGap),
        ) {
            BentoCard(
                title = "Read this",
                onClick = onReadThis,
                containerColor = BeaconYellow,
                contentColor = BeaconYellowText,
                leadingIcon = Icons.Filled.MenuBook,
                minHeight = BeaconDimens.bentoSmallMinHeight,
                modifier = Modifier.weight(1f),
                contentDescription = "Read this. Read text, signs, or documents.",
            )
            BentoCard(
                title = "Walking mode",
                onClick = onWalkingMode,
                containerColor = BeaconWhiteCard,
                contentColor = BeaconWhiteCardText,
                leadingIcon = Icons.Filled.DirectionsWalk,
                minHeight = BeaconDimens.bentoSmallMinHeight,
                modifier = Modifier.weight(1f),
                contentDescription = "Walking mode. Continuous guidance while you walk.",
            )
        }

        BentoCard(
            title = "Emergency",
            onClick = onEmergency,
            containerColor = BeaconDanger,
            contentColor = BeaconDangerText,
            leadingIcon = Icons.Filled.Emergency,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Emergency. Contact a trusted person quickly.",
        )

        BentoCard(
            title = "Glasses status",
            onClick = onOpenSettings,
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Open glasses status and settings.",
        )

        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun ConnectionChips(state: HomeUiState) {
    val c = state.connectionState
    val connected = c is ConnectionState.Connected && c.ready
    val connectionText = when (c) {
        ConnectionState.Disconnected -> "Not connected"
        ConnectionState.BluetoothOff -> "Bluetooth off"
        ConnectionState.Scanning -> "Scanning…"
        is ConnectionState.Connecting -> "Connecting…"
        is ConnectionState.Connected -> if (c.ready) "Connected" else "Connecting…"
        is ConnectionState.Failed -> "Connection failed"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BeaconChip(
            label = connectionText,
            selected = connected,
            accent = if (connected) BeaconLime else null,
        )
        state.battery?.let {
            BeaconChip(label = "Battery ${it.levelPercent}%", selected = false)
        }
    }
}

@Composable
private fun PhoneModeChip(phoneMode: Boolean, onOpenPhone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BeaconChip(
            label = if (phoneMode) "Phone mode on" else "Use phone camera",
            selected = phoneMode,
            accent = if (phoneMode) BeaconLime else null,
            onClick = onOpenPhone,
        )
    }
}

@Composable
private fun ActiveModelChips(state: HomeUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BeaconChip(
            label = "Using: ${state.activeIntelligenceName}",
            selected = state.activeIntelligenceName != "Built-in vision",
            accent = if (state.activeIntelligenceName != "Built-in vision") BeaconLime else null,
        )
        state.activeVoiceName?.let { voice ->
            BeaconChip(label = "Voice: $voice", selected = true, accent = BeaconLime)
        }
        if (state.guidanceLanguage == GuidanceLanguage.Hausa) {
            BeaconChip(label = "Hausa guidance", selected = true)
        }
    }
}
