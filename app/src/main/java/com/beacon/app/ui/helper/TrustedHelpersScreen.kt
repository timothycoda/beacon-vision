package com.beacon.app.ui.helper

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.ui.accessibility.ScreenVoiceIntro
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.domain.accessibility.AccessibilityScreenIds
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.components.RowWithSwitch
import com.beacon.app.ui.theme.BeaconCardDark
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.BeaconLime
import com.beacon.app.ui.theme.BeaconLimeText
import com.beacon.app.ui.theme.BeaconOnDark
import com.beacon.app.ui.theme.BeaconWhiteCard
import com.beacon.app.ui.theme.BeaconWhiteCardText
import com.beacon.domain.helper.TrustedHelper

@Composable
fun TrustedHelpersScreen(
    onBack: () -> Unit,
    viewModel: TrustedHelpersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenVoiceIntro(AccessibilityScreenIds.TRUSTED_HELPERS)

    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(
            title = "Trusted helpers",
            subtitle = "People you can reach on WhatsApp for help or emergencies.",
        )

        state.helpers.forEach { helper ->
            BentoCard(
                title = helper.displayName,
                value = helper.phoneNumber,
                onClick = { viewModel.startEdit(helper) },
                containerColor = BeaconCardDark,
                contentColor = BeaconOnDark,
                minHeight = BeaconDimens.bentoWideMinHeight,
                contentDescription = "Edit ${helper.displayName}",
            )
        }

        BentoCard(
            title = "Add helper",
            onClick = viewModel::startNew,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Add a trusted helper",
        )

        state.editing?.let {
            HelperEditor(state, viewModel)
        }

        state.statusMessage?.let { msg ->
            Text(text = msg, style = MaterialTheme.typography.bodyLarge)
        }

        BentoCard(
            title = "Back",
            onClick = onBack,
            containerColor = BeaconCardDark,
            contentColor = BeaconOnDark,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Go back",
        )
    }
}

@Composable
private fun HelperEditor(
    state: TrustedHelpersUiState,
    viewModel: TrustedHelpersViewModel,
) {
    Text(
        text = if (state.editing?.createdAt == state.editing?.updatedAt) "New helper" else "Edit helper",
        style = MaterialTheme.typography.titleLarge,
    )
    OutlinedTextField(
        value = state.displayName,
        onValueChange = viewModel::updateDisplayName,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Name") },
        singleLine = true,
    )
    OutlinedTextField(
        value = state.phoneNumber,
        onValueChange = viewModel::updatePhone,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Phone (international, e.g. +234…)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
    )
    OutlinedTextField(
        value = state.relationship,
        onValueChange = viewModel::updateRelationship,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Relationship (optional)") },
        singleLine = true,
    )
    RowWithSwitch("WhatsApp enabled", state.whatsappEnabled, viewModel::setWhatsappEnabled)
    RowWithSwitch(
        "Receive WhatsApp emergency alerts",
        state.receiveEmergencyWhatsApp,
        viewModel::setReceiveEmergency,
    )
    RowWithSwitch("Primary helper", state.isPrimaryHelper, viewModel::setPrimaryHelper)
    RowWithSwitch("Primary emergency contact", state.isPrimaryEmergency, viewModel::setPrimaryEmergency)
    BentoCard(
        title = "Save",
        onClick = viewModel::save,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        minHeight = BeaconDimens.bentoWideMinHeight,
        contentDescription = "Save trusted helper",
    )
    if (state.editing != null && state.helpers.any { it.id == state.editing!!.id }) {
        BentoCard(
            title = "Delete",
            onClick = { viewModel.delete(state.editing) },
            containerColor = BeaconWhiteCard,
            contentColor = BeaconWhiteCardText,
            minHeight = BeaconDimens.bentoWideMinHeight,
            contentDescription = "Delete trusted helper",
        )
    }
    BentoCard(
        title = "Cancel",
        onClick = viewModel::cancelEdit,
        containerColor = BeaconCardDark,
        contentColor = BeaconOnDark,
        minHeight = BeaconDimens.bentoWideMinHeight,
        contentDescription = "Cancel editing",
    )
}
