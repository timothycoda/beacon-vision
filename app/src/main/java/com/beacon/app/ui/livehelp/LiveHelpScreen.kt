package com.beacon.app.ui.livehelp

import android.Manifest
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.R
import com.beacon.app.livehelp.LiveHelpConnectionState
import com.beacon.app.ui.accessibility.ScreenVoiceIntro
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BentoCard
import com.beacon.app.ui.components.SecondaryActionButton
import com.beacon.app.ui.theme.BeaconDimens
import com.beacon.app.ui.theme.bentoWhiteCardColor
import com.beacon.app.ui.theme.bentoWhiteCardOnColor
import com.beacon.domain.accessibility.AccessibilityScreenIds

@Composable
fun LiveHelpScreen(
    onBack: () -> Unit,
    viewModel: LiveHelpViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ScreenVoiceIntro(AccessibilityScreenIds.LIVE_HELP)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants[Manifest.permission.CAMERA] == true &&
            grants[Manifest.permission.RECORD_AUDIO] == true
        ) {
            viewModel.startSession()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
        )
    }

    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(
            title = stringResource(R.string.live_help_title),
            subtitle = stringResource(R.string.live_help_subtitle),
        )

        state.roomId?.let { room ->
            Text(
                text = stringResource(R.string.live_help_room_code, room),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        state.statusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(BeaconDimens.bentoGap),
        ) {
            if (state.connectionState == LiveHelpConnectionState.WaitingForHelper ||
                state.connectionState == LiveHelpConnectionState.Connected
            ) {
                BentoCard(
                    title = stringResource(R.string.live_help_share_invite),
                    onClick = {
                        val share = viewModel.shareHelperInvite()
                        context.startActivity(Intent.createChooser(share, null))
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    leadingIcon = Icons.Filled.Share,
                    minHeight = BeaconDimens.bentoWideMinHeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Share secure helper invite link"
                        },
                )
            }

            BentoCard(
                title = stringResource(R.string.live_help_end),
                onClick = { viewModel.endSession(onBack) },
                containerColor = bentoWhiteCardColor(),
                contentColor = bentoWhiteCardOnColor(),
                minHeight = BeaconDimens.bentoWideMinHeight,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.connectionState == LiveHelpConnectionState.Failed) {
                SecondaryActionButton(
                    label = stringResource(R.string.live_help_retry),
                    onClick = {
                        viewModel.endSession {}
                        viewModel.startSession()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
