package com.beacon.app.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import com.beacon.app.ui.components.BeaconHeading
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

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onGlassesStatus: () -> Unit,
    onVoiceSettings: () -> Unit,
    onModelPacks: () -> Unit,
    onEmergency: () -> Unit,
    onTrustedHelpers: () -> Unit,
    onHistory: () -> Unit,
) {
    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(
            title = "Settings",
            subtitle = "Glasses, voice, emergency, and your recent activity.",
        )

        BentoCard(
            title = "Glasses status",
            onClick = onGlassesStatus,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            leadingIcon = Icons.Filled.Visibility,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Connection, battery, and device info.",
        )
        BentoCard(
            title = "Voice settings",
            onClick = onVoiceSettings,
            containerColor = BeaconWhiteCard,
            contentColor = BeaconWhiteCardText,
            leadingIcon = Icons.Filled.Mic,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Text-to-speech voice and speed.",
        )
        BentoCard(
            title = "Offline AI packs",
            onClick = onModelPacks,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            leadingIcon = Icons.Filled.CloudDownload,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Download Gemma and other optional on-device packs.",
        )
        BentoCard(
            title = "Emergency",
            onClick = onEmergency,
            containerColor = BeaconDanger,
            contentColor = BeaconDangerText,
            leadingIcon = Icons.Filled.Emergency,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Trusted contact and hands-free emergency options.",
        )
        BentoCard(
            title = "Trusted helpers",
            onClick = onTrustedHelpers,
            containerColor = BeaconWhiteCard,
            contentColor = BeaconWhiteCardText,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Manage helpers for WhatsApp and emergencies.",
        )
        BentoCard(
            title = "History",
            onClick = onHistory,
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            leadingIcon = Icons.Filled.History,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Recent descriptions and readings.",
        )
        BentoCard(
            title = "Back",
            onClick = onBack,
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Go back to home.",
        )
    }
}
