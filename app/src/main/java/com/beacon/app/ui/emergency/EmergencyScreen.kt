package com.beacon.app.ui.emergency

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.emergency.SmsIntentLauncher
import com.beacon.app.emergency.SmsLaunchResult
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.components.RowWithSwitch
import com.beacon.app.ui.theme.BeaconCardDark
import com.beacon.app.ui.theme.BeaconDanger
import com.beacon.app.ui.theme.BeaconDangerText
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.app.ui.theme.BeaconWhiteCard
import com.beacon.app.ui.theme.BeaconWhiteCardText
import com.beacon.core.permission.BeaconPermissions
import com.beacon.app.R

@Composable
fun EmergencyScreen(
    onBack: () -> Unit,
    viewModel: EmergencyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        viewModel.requestHelp()
    }

    var smsGranted by remember {
        mutableStateOf(BeaconPermissions.hasSendSmsPermission(context))
    }
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        smsGranted = granted
    }

    LaunchedEffect(viewModel) {
        viewModel.openSmsRequests.collect { draft ->
            val result = SmsIntentLauncher.launch(context, draft)
            val success = result is SmsLaunchResult.Success
            val reason = when (result) {
                is SmsLaunchResult.Success -> null
                is SmsLaunchResult.NoSmsApp ->
                    "No messaging app found. Install Messages or open your SMS app manually."
                is SmsLaunchResult.Failed -> result.reason
            }
            viewModel.onSmsLaunchResult(success, reason)
        }
    }

    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(
            title = "Emergency",
            subtitle = "Contact someone you trust. You review and send the message.",
        )

        Text(
            text = stringResource(R.string.emergency_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )

        OutlinedTextField(
            value = state.contactName,
            onValueChange = viewModel::updateContactName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Trusted contact name") },
            singleLine = true,
        )
        OutlinedTextField(
            value = state.contactPhone,
            onValueChange = viewModel::updateContactPhone,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
        )

        BentoCard(
            title = "Save contact",
            onClick = viewModel::saveContact,
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Save trusted contact.",
        )

        RowWithSwitch(
            label = "Include what is ahead from glasses",
            checked = state.includeSceneFromGlasses,
            onCheckedChange = viewModel::setIncludeScene,
        )
        RowWithSwitch(
            label = "Hands-free: send SMS automatically",
            checked = state.autoSendSms,
            onCheckedChange = viewModel::setAutoSendSms,
        )
        RowWithSwitch(
            label = "Hands-free: wait 3 seconds before sending",
            checked = state.confirmBeforeSend,
            onCheckedChange = viewModel::setConfirmBeforeSend,
        )
        RowWithSwitch(
            label = "Allow WhatsApp emergency sharing",
            checked = state.allowWhatsAppEmergencySharing,
            onCheckedChange = viewModel::setAllowWhatsAppEmergencySharing,
        )
        RowWithSwitch(
            label = "Include location in emergency alerts",
            checked = state.includeLocationInAlerts,
            onCheckedChange = viewModel::setIncludeLocationInAlerts,
        )
        RowWithSwitch(
            label = "Include latest image in emergency alerts",
            checked = state.includeLatestImageInAlerts,
            onCheckedChange = viewModel::setIncludeLatestImageInAlerts,
        )

        LaunchedEffect(state.allowWhatsAppEmergencySharing) {
            if (state.allowWhatsAppEmergencySharing) {
                viewModel.prepareWhatsAppSharePreview()
            }
        }

        state.sharePreview?.let { preview ->
            Text(
                text = buildString {
                    append("WhatsApp share preview: ")
                    append(if (preview.hasImage) "image yes" else "no image")
                    append(", ")
                    append(if (preview.hasLocation) "location yes" else "no location")
                    append(", ")
                    append("${preview.contactCount} contact(s)")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.countdownSeconds?.let { seconds ->
            Text(
                text = "Sending in $seconds…",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
            BentoCard(
                title = "Cancel",
                onClick = viewModel::cancelCountdown,
                containerColor = BeaconCardDark,
                contentColor = BeaconOnDark,
                minHeight = BeaconDimens.bentoLargeMinHeight,
                contentDescription = "Cancel emergency countdown",
            )
        }

        if (smsGranted) {
            Text(
                text = context.getString(R.string.emergency_auto_send_enabled),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            BentoCard(
                title = context.getString(R.string.emergency_auto_send_allow),
                onClick = {
                    smsLauncher.launch(BeaconPermissions.sendSmsPermission())
                },
                containerColor = BeaconWhiteCard,
                contentColor = BeaconWhiteCardText,
                minHeight = BeaconDimens.bentoWideMinHeight,
                contentDescription = context.getString(R.string.emergency_auto_send_prompt),
            )
            Text(
                text = context.getString(R.string.emergency_auto_send_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.statusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        if (state.isBusy) {
            CircularProgressIndicator(color = BeaconDanger)
        }

        BentoCard(
            title = if (state.isBusy) "Working…" else "Get help now (SMS)",
            onClick = {
                if (state.contactPhone.isBlank()) return@BentoCard
                val needsLocation = !BeaconPermissions.hasEmergencyLocationPermission(context)
                if (needsLocation) {
                    locationLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    viewModel.requestHelp()
                }
            },
            containerColor = BeaconDanger,
            contentColor = BeaconDangerText,
            leadingIcon = Icons.Filled.Emergency,
            minHeight = BeaconDimens.bentoLargeMinHeight,
            enabled = !state.isBusy && state.contactPhone.isNotBlank() && state.countdownSeconds == null,
            contentDescription = "Prepare emergency message and open your messaging app.",
        )

        BentoCard(
            title = "Call emergency contact",
            onClick = viewModel::callEmergencyContact,
            containerColor = BeaconDanger,
            contentColor = BeaconDangerText,
            minHeight = BeaconDimens.bentoWideMinHeight,
            enabled = state.contactPhone.isNotBlank(),
            contentDescription = "Call your trusted emergency contact",
        )

        BentoCard(
            title = "Send emergency alert to WhatsApp",
            onClick = viewModel::startWhatsAppEmergencyCountdown,
            containerColor = BeaconWhiteCard,
            contentColor = BeaconWhiteCardText,
            minHeight = BeaconDimens.bentoLargeMinHeight,
            enabled = state.allowWhatsAppEmergencySharing && !state.isBusy && state.countdownSeconds == null,
            contentDescription = "Prepare WhatsApp emergency alert with image and location",
        )

        BentoCard(
            title = "Send image and location (WhatsApp)",
            onClick = viewModel::shareEmergencyViaWhatsApp,
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            minHeight = BeaconDimens.bentoWideMinHeight,
            enabled = state.allowWhatsAppEmergencySharing && !state.isBusy,
            contentDescription = "Open WhatsApp to share emergency image and message",
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

