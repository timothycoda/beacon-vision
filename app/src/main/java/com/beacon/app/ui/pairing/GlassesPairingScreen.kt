package com.beacon.app.ui.pairing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BigActionButton
import com.beacon.app.ui.components.SecondaryActionButton
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconOnDarkMuted
import com.beacon.core.permission.BeaconPermissions
import com.beacon.domain.glasses.model.ConnectionState
import com.beacon.domain.glasses.model.GlassesDevice

@Composable
fun GlassesPairingScreen(
    onConnected: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Re-check against the system rather than trusting the result map, so a
        // partial grant (e.g. only CONNECT) is still treated as not-ready.
        if (BeaconPermissions.hasBlePermissions(context)) {
            permissionDenied = false
            viewModel.onPermissionsReady()
        } else {
            permissionDenied = true
        }
    }

    // A BLE scan started without BLUETOOTH_SCAN returns zero results silently, so
    // gate the initial scan/reconnect on the permission being held first.
    LaunchedEffect(Unit) {
        if (BeaconPermissions.hasBlePermissions(context)) {
            permissionDenied = false
            viewModel.onPermissionsReady()
        } else {
            permissionLauncher.launch(BeaconPermissions.blePermissions().toTypedArray())
        }
    }

    LaunchedEffect(state.connectionState) {
        if (state.connectionState is ConnectionState.Connected &&
            (state.connectionState as ConnectionState.Connected).ready
        ) {
            onConnected()
        }
    }

    val hasLastPaired = state.lastPaired != null

    BeaconScreen {
        BeaconTopBar(title = "Beacon")
        BeaconHeading(
            title = if (hasLastPaired) "Your glasses" else "Connect your glasses",
            subtitle = if (hasLastPaired) {
                "Turn on your glasses, then tap Scan to connect."
            } else {
                "Scan, then tap your glasses in the list."
            },
        )
        if (!state.isBluetoothOn) {
            StatusText(
                text = "Bluetooth is off. Please turn on Bluetooth to find your glasses.",
                isError = true,
            )
        }

        if (permissionDenied) {
            StatusText(
                text = "Beacon needs the Bluetooth (Nearby devices) permission to find " +
                    "your glasses. Please allow it to continue.",
                isError = true,
            )
            BigActionButton(
                label = "Allow Bluetooth permission",
                onClick = { permissionLauncher.launch(BeaconPermissions.blePermissions().toTypedArray()) },
                contentDescription = "Allow Bluetooth permission",
            )
        }

        val connecting = state.connectionState as? ConnectionState.Connecting
        val paired = state.lastPaired
        if (connecting != null || state.isReconnecting) {
            ConnectingRow(
                text = when {
                    connecting != null -> "Connecting to ${connecting.deviceName ?: "glasses"}..."
                    paired != null -> "Reconnecting to ${paired.name}..."
                    else -> "Connecting..."
                },
            )
        }

        state.errorMessage?.let {
            StatusText(text = it, isError = true)
        }

        SecondaryActionButton(
            label = if (state.isScanning) "Stop scan" else "Scan for glasses",
            onClick = { if (state.isScanning) viewModel.stopScan() else viewModel.startScan() },
            enabled = state.isBluetoothOn,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = if (state.isScanning) {
                "Scanning for glasses. Tap to stop."
            } else {
                "Scan for nearby glasses."
            },
        )

        if (state.devices.isEmpty()) {
            if (!hasLastPaired || state.isScanning) {
                Text(
                    text = when {
                        state.isScanning -> "Looking for glasses nearby..."
                        hasLastPaired -> "Waiting for your glasses to power on."
                        else -> "No glasses found yet. Tap Scan to search."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        } else {
            Text(
                text = "Found ${state.devices.size} device(s). Tap one to connect.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            state.devices.forEach { device ->
                DeviceCard(device = device, onClick = { viewModel.connect(device) })
            }
        }
    }
}

@Composable
private fun DeviceCard(device: GlassesDevice, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "${device.name}, signal ${device.rssi} decibels. Tap to connect."
                role = Role.Button
                onClick(action = { onClick(); true })
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(BeaconDimens.cardBorderWidth, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${device.address}  •  signal ${device.rssi} dBm",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ConnectingRow(text: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator()
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun StatusText(text: String, isError: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
