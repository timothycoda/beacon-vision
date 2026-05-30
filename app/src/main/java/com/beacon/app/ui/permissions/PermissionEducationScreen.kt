package com.beacon.app.ui.permissions

import android.content.Context
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
import androidx.compose.ui.unit.dp
import com.beacon.app.ui.components.BeaconHeading
import com.beacon.app.ui.components.BeaconScreen
import com.beacon.app.ui.components.BeaconTopBar
import com.beacon.app.ui.components.BigActionButton
import com.beacon.app.ui.components.SecondaryActionButton
import com.beacon.core.permission.BeaconPermissions

@Composable
fun PermissionEducationScreen(onPermissionsReady: () -> Unit) {
    val context = LocalContext.current
    var denied by remember { mutableStateOf(false) }

    val permissionsToRequest = remember {
        buildList {
            addAll(BeaconPermissions.blePermissions())
            BeaconPermissions.notificationPermission()?.let { add(it) }
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        // Only the Bluetooth permissions are required to continue; notifications
        // are nice-to-have. Degrade gracefully if Bluetooth is denied.
        val bleGranted = BeaconPermissions.blePermissions().all { result[it] == true }
        if (bleGranted || hasBle(context)) {
            onPermissionsReady()
        } else {
            denied = true
        }
    }

    BeaconScreen {
        BeaconTopBar(title = "Beacon")
        BeaconHeading(title = "A few permissions")
        Text(
            text = "Beacon needs your permission for a few things. We only ask when a feature needs it.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        PermissionRow(
            title = "Bluetooth",
            reason = "To find and connect to your smart glasses.",
        )
        PermissionRow(
            title = "Notifications",
            reason = "To keep guidance running and show glasses status.",
        )
        if (denied) {
            Text(
                text = "Bluetooth permission was not granted. Beacon cannot connect to your glasses without it. You can try again or enable it in Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(8.dp))
        BigActionButton(
            label = if (denied) "Try again" else "Allow permissions",
            onClick = {
                if (hasBle(context)) onPermissionsReady() else launcher.launch(permissionsToRequest)
            },
            modifier = Modifier.fillMaxWidth(),
            contentDescription = "Allow permissions for Bluetooth and notifications.",
        )
        SecondaryActionButton(
            label = "Skip for now",
            onClick = onPermissionsReady,
            contentDescription = "Skip permissions for now. Some features will be limited.",
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
