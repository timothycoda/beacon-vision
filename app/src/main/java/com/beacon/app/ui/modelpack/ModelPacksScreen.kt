package com.beacon.app.ui.modelpack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.components.SecondaryActionButton
import com.beacon.app.ui.theme.BeaconLime
import com.beacon.app.ui.theme.BeaconLimeText
import com.beacon.app.ui.theme.BeaconOnDarkMuted
import com.beacon.app.ui.theme.BeaconWhiteCard
import com.beacon.app.ui.theme.BeaconWhiteCardText
import com.beacon.domain.guidance.GuidanceLanguage
import com.beacon.domain.modelpack.ModelPackDownloadFailure
import com.beacon.domain.modelpack.ModelPackInstallState
import com.beacon.domain.modelpack.ModelPackKind
import com.beacon.domain.modelpack.ModelPackStatus

@Composable
fun ModelPacksScreen(
    onBack: () -> Unit,
    viewModel: ModelPacksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BeaconScreen {
        BeaconTopBar(title = "Beacon")
        BeaconHeading(
            title = "Offline AI packs",
            subtitle = "Download models, then choose which powers your guidance.",
        )

        GuidanceLanguageToggle(
            language = state.guidanceLanguage,
            onSelect = viewModel::setGuidanceLanguage,
        )

        WifiOnlyToggle(
            enabled = state.wifiOnly,
            onToggle = viewModel::setWifiOnlyDownload,
        )

        state.packs.forEach { pack ->
            when (pack.state) {
                ModelPackInstallState.Downloading,
                -> if (state.downloadingPackId == pack.id) {
                    DownloadingPackBanner(
                        pack = pack,
                        onPause = { viewModel.pause(pack.id) },
                        onCancel = { viewModel.cancel(pack.id) },
                    )
                } else {
                    PackCard(
                        pack = pack,
                        onPrimaryAction = { handlePrimaryAction(pack, viewModel) },
                        onDelete = { viewModel.delete(pack.id) },
                    )
                }
                else -> PackCard(
                    pack = pack,
                    onPrimaryAction = { handlePrimaryAction(pack, viewModel) },
                    onDelete = { viewModel.delete(pack.id) },
                    onCancelPartial = if (pack.state == ModelPackInstallState.Paused) {
                        { viewModel.cancel(pack.id) }
                    } else {
                        null
                    },
                )
            }
        }

        SecondaryActionButton(
            label = "Back",
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = "Go back to settings.",
        )
    }
}

private fun handlePrimaryAction(pack: ModelPackStatus, viewModel: ModelPacksViewModel) {
    when (pack.state) {
        ModelPackInstallState.NotInstalled,
        ModelPackInstallState.Paused,
        ModelPackInstallState.Failed,
        -> viewModel.download(pack.id)
        ModelPackInstallState.Downloading -> viewModel.pause(pack.id)
        ModelPackInstallState.Installed -> when (pack.kind) {
            ModelPackKind.Narration -> if (!pack.isDefaultNarration) viewModel.setDefault(pack)
            ModelPackKind.Voice -> if (!pack.isDefaultVoice) viewModel.setDefault(pack)
            ModelPackKind.Utility -> viewModel.delete(pack.id)
        }
    }
}

@Composable
private fun GuidanceLanguageToggle(language: GuidanceLanguage, onSelect: (GuidanceLanguage) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Guidance language",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Hausa uses translated labels and Hausa speech when the voice pack is installed.",
                style = MaterialTheme.typography.bodyMedium,
                color = BeaconOnDarkMuted,
            )
        }
        Switch(
            checked = language == GuidanceLanguage.Hausa,
            onCheckedChange = { onSelect(if (it) GuidanceLanguage.Hausa else GuidanceLanguage.English) },
            modifier = Modifier.semantics {
                contentDescription = if (language == GuidanceLanguage.Hausa) {
                    "Hausa guidance on"
                } else {
                    "English guidance on"
                }
            },
        )
    }
}

@Composable
private fun DownloadingPackBanner(
    pack: ModelPackStatus,
    onPause: () -> Unit,
    onCancel: () -> Unit,
) {
    val progress = if (pack.sizeBytes > 0) {
        (pack.bytesDownloaded.toFloat() / pack.sizeBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percent = (progress * 100).toInt()

    Column(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        BentoCard(
            title = "Downloading ${pack.displayName}",
            value = "$percent% · notification shows progress",
            onClick = onPause,
            containerColor = BeaconLime,
            contentColor = BeaconLimeText,
            contentDescription = "Downloading ${pack.displayName}. Double tap to pause.",
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
        )
        SecondaryActionButton(
            label = "Cancel download",
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = "Cancel download and remove partial files for ${pack.displayName}.",
        )
    }
}

@Composable
private fun PackCard(
    pack: ModelPackStatus,
    onPrimaryAction: () -> Unit,
    onDelete: () -> Unit,
    onCancelPartial: (() -> Unit)? = null,
) {
    val active = pack.isDefaultNarration || pack.isDefaultVoice
    val statusLine = when {
        active && pack.isDefaultNarration -> "Active · default intelligence"
        active && pack.isDefaultVoice -> "Active · default voice"
        pack.state == ModelPackInstallState.Installed -> "Installed · tap to activate"
        pack.state == ModelPackInstallState.Paused -> {
            val percent = if (pack.sizeBytes > 0) {
                ((pack.bytesDownloaded.toFloat() / pack.sizeBytes) * 100).toInt()
            } else {
                0
            }
            "Paused at $percent% · tap to resume"
        }
        pack.state == ModelPackInstallState.Failed ->
            pack.errorMessage ?: pack.failureKind?.userMessage() ?: "Download failed"
        else -> pack.capability
    }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        BentoCard(
            title = pack.displayName,
            value = "${pack.ramHint}\n$statusLine",
            onClick = onPrimaryAction,
            containerColor = if (active) BeaconLime else BeaconWhiteCard,
            contentColor = if (active) BeaconLimeText else BeaconWhiteCardText,
            contentDescription = "${pack.displayName}. ${ModelPacksViewModel.primaryActionLabel(pack)}.",
        )
        if (pack.state == ModelPackInstallState.Paused && onCancelPartial != null) {
            LinearProgressIndicator(
                progress = {
                    if (pack.sizeBytes > 0) {
                        (pack.bytesDownloaded.toFloat() / pack.sizeBytes).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 6.dp),
            )
            SecondaryActionButton(
                label = "Discard partial download",
                onClick = onCancelPartial,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = "Discard partial download for ${pack.displayName}.",
            )
        }
        if (pack.state == ModelPackInstallState.Installed) {
            SecondaryActionButton(
                label = "Remove ${pack.displayName}",
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentDescription = "Remove ${pack.displayName} from this device.",
            )
        }
    }
}

@Composable
private fun WifiOnlyToggle(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Wi‑Fi only downloads",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}
