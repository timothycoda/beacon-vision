package com.beacon.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beacon.app.assistant.BeaconAssistantService
import com.beacon.app.ui.BeaconApp
import com.beacon.app.ui.theme.BeaconTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyRoute(intent)
        setContent {
            BeaconTheme {
                BeaconApp(
                    startRoute = pendingRoute,
                    onStartRouteHandled = { pendingRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyRoute(intent)
    }

    /** Reads a deep-link route from the launching intent (e.g. from the
     * hands-free assistant) and, for emergencies, shows over the lock screen. */
    private fun applyRoute(intent: Intent?) {
        val route = intent?.getStringExtra(EXTRA_ROUTE) ?: return
        pendingRoute = route
        if (route == BeaconAssistantService.ROUTE_EMERGENCY &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
        ) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
    }

    companion object {
        const val EXTRA_ROUTE = "beacon_route"
    }
}
