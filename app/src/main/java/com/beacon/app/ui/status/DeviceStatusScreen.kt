package com.beacon.app.ui.status

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconStatusCard
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BigActionButton
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.components.SecondaryActionButton
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconLime
import com.beacon.app.ui.theme.BeaconLimeText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import com.beacon.domain.glasses.model.ConnectionState

@Composable
fun DeviceStatusScreen(
    onUnbound: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onBack: () -> Unit,
    viewModel: DeviceStatusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BeaconScreen {
        BeaconTopBar(title = "Beacon")
        BeaconHeading(
            title = "Glasses status",
            subtitle = "Connection, battery, and device details.",
        )
        BeaconStatusCard(
            label = "Connection",
            value = connectionLabel(state.connectionState),
            emphasize = true,
        )

        BeaconStatusCard(
            label = "Battery",
            value = state.battery?.let {
                "${it.levelPercent}%" + if (it.isCharging) " (charging)" else ""
            } ?: "Unknown",
        )

        val info = state.deviceInfo
        if (info != null) {
            BeaconStatusCard(label = "Firmware", value = info.firmwareVersion.ifBlank { "Unknown" })
            BeaconStatusCard(label = "Hardware", value = info.hardwareVersion.ifBlank { "Unknown" })
            if (info.wifiFirmwareVersion.isNotBlank()) {
                BeaconStatusCard(label = "Wi-Fi firmware", value = info.wifiFirmwareVersion)
            }
        }

        BigActionButton(
            label = if (state.isBusy) "Refreshing..." else "Refresh",
            onClick = viewModel::refresh,
            enabled = !state.isBusy && state.connectionState is ConnectionState.Connected,
            contentDescription = "Refresh battery and device information.",
        )

        BentoCard(
            title = "Voice settings",
            onClick = onOpenVoiceSettings,
            containerColor = BeaconLime,
            contentColor = BeaconLimeText,
            leadingIcon = Icons.Filled.RecordVoiceOver,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Choose and test the voice Beacon uses to speak.",
        )

        SecondaryActionButton(
            label = "Disconnect",
            onClick = viewModel::disconnect,
            enabled = state.connectionState is ConnectionState.Connected,
            contentDescription = "Disconnect from the glasses.",
        )

        SecondaryActionButton(
            label = "Forget / reset device",
            onClick = { viewModel.unbind(onUnbound) },
            contentDescription = "Forget this device and reset the connection.",
        )

        SecondaryActionButton(
            label = "Back",
            onClick = onBack,
            contentDescription = "Go back.",
        )
    }
}

private fun connectionLabel(state: ConnectionState): String = when (state) {
    ConnectionState.Disconnected -> "Disconnected"
    ConnectionState.BluetoothOff -> "Bluetooth is off"
    ConnectionState.Scanning -> "Scanning"
    is ConnectionState.Connecting -> "Connecting to ${state.deviceName ?: "glasses"}..."
    is ConnectionState.Connected ->
        if (state.ready) "Connected and ready" else "Connected, preparing..."
    is ConnectionState.Failed -> "Failed: ${state.reason}"
}
