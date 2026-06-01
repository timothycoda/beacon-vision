package com.beacon.app.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.beacon.app.R
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BigActionButton
import com.beacon.app.ui.components.SecondaryActionButton
import com.beacon.core.permission.BeaconPermissions

@Composable
fun PermissionEducationScreen(
    onPermissionsReady: () -> Unit,
    viewModel: PermissionEducationViewModel = hiltViewModel(),
) {
    val phoneSetup by viewModel.phoneSetup.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var denied by remember { mutableStateOf(false) }

    val permissionsToRequest = remember(phoneSetup) {
        if (phoneSetup) {
            buildList {
                add(Manifest.permission.CAMERA)
                BeaconPermissions.notificationPermission()?.let { add(it) }
            }.toTypedArray()
        } else {
            buildList {
                addAll(BeaconPermissions.blePermissions())
                BeaconPermissions.notificationPermission()?.let { add(it) }
            }.toTypedArray()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (phoneSetup) {
            val cameraGranted = result[Manifest.permission.CAMERA] == true ||
                hasCamera(context)
            if (cameraGranted) {
                onPermissionsReady()
            } else {
                denied = true
            }
        } else {
            val bleGranted = BeaconPermissions.blePermissions().all { result[it] == true }
            if (bleGranted || hasBle(context)) {
                onPermissionsReady()
            } else {
                denied = true
            }
        }
    }

    val readyNow = if (phoneSetup) hasCamera(context) else hasBle(context)

    BeaconScreen {
        BeaconTopBar()
        BeaconHeading(title = stringResource(R.string.permissions_heading))
        Text(
            text = if (phoneSetup) {
                stringResource(R.string.permissions_intro_phone)
            } else {
                stringResource(R.string.permissions_intro)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (phoneSetup) {
            PermissionRow(
                title = stringResource(R.string.permissions_camera_title),
                reason = stringResource(R.string.permissions_camera_reason),
            )
        } else {
            PermissionRow(
                title = stringResource(R.string.permissions_bluetooth_title),
                reason = stringResource(R.string.permissions_bluetooth_reason),
            )
        }
        PermissionRow(
            title = stringResource(R.string.permissions_notifications_title),
            reason = stringResource(R.string.permissions_notifications_reason),
        )
        if (denied) {
            Text(
                text = if (phoneSetup) {
                    stringResource(R.string.permissions_camera_denied)
                } else {
                    stringResource(R.string.permissions_bluetooth_denied)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(8.dp))
        BigActionButton(
            label = if (denied) stringResource(R.string.permissions_try_again) else stringResource(R.string.permissions_allow),
            onClick = {
                if (readyNow) onPermissionsReady() else launcher.launch(permissionsToRequest)
            },
            modifier = Modifier.fillMaxWidth(),
            contentDescription = stringResource(R.string.permissions_allow_cd),
        )
        SecondaryActionButton(
            label = stringResource(R.string.permissions_skip),
            onClick = onPermissionsReady,
            contentDescription = stringResource(R.string.permissions_skip_cd),
        )
    }
}

@Composable
private fun PermissionRow(title: String, reason: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = reason,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

private fun hasBle(context: Context): Boolean = BeaconPermissions.hasBlePermissions(context)

private fun hasCamera(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
