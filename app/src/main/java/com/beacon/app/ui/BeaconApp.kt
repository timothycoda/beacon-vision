package com.beacon.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.beacon.app.assistant.BeaconAssistantService
import com.beacon.app.ui.ahead.WhatIsAheadScreen
import com.beacon.app.ui.emergency.EmergencyScreen
import com.beacon.app.ui.history.HistoryScreen
import com.beacon.app.ui.home.HomeGuidanceScreen
import com.beacon.app.ui.navigation.AppNavViewModel
import com.beacon.app.ui.navigation.BeaconDestinations
import com.beacon.app.ui.pairing.GlassesPairingScreen
import com.beacon.app.ui.permissions.PermissionEducationScreen
import com.beacon.app.ui.read.ReadThisScreen
import com.beacon.app.ui.modelpack.ModelPacksScreen
import com.beacon.app.ui.settings.SettingsScreen
import com.beacon.app.ui.status.DeviceStatusScreen
import com.beacon.app.ui.voice.VoiceSettingsScreen
import com.beacon.app.ui.voicecommand.AssistantViewModel
import com.beacon.app.ui.voicecommand.VoiceCommandScreen
import com.beacon.app.ui.walking.WalkingModeScreen
import com.beacon.app.ui.onboarding.OnboardingScreen
import com.beacon.app.ui.phone.PhoneGuidanceScreen
import com.beacon.app.ui.helper.TrustedHelpersScreen
import com.beacon.app.ui.accessibility.AccessibilitySettingsScreen
import com.beacon.app.ui.accessibility.ProvideAccessibilityVoiceGuide
import com.beacon.app.ui.welcome.WelcomeScreen
import com.beacon.domain.device.GuidanceInputMode
import com.beacon.domain.glasses.model.ConnectionState

@Composable
fun BeaconApp(
    startRoute: String? = null,
    onStartRouteHandled: () -> Unit = {},
) {
    val appNavViewModel: AppNavViewModel = hiltViewModel()
    val initialDestination by appNavViewModel.startDestination.collectAsStateWithLifecycle()
    val guidanceMode by appNavViewModel.guidanceMode.collectAsStateWithLifecycle()
    if (initialDestination == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var navGraphStart by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(initialDestination) {
        if (navGraphStart == null && initialDestination != null) {
            navGraphStart = initialDestination
        }
    }
    val startDestination = navGraphStart ?: return

    val navController = rememberNavController()
    val context = LocalContext.current

    val assistantViewModel: AssistantViewModel = hiltViewModel()
    val connection by assistantViewModel.connectionState.collectAsStateWithLifecycle()
    LaunchedEffect(connection) {
        if (connection is ConnectionState.Connected && (connection as ConnectionState.Connected).ready) {
            BeaconAssistantService.start(context)
        }
    }

    LaunchedEffect(startRoute) {
        val route = startRoute ?: return@LaunchedEffect
        navController.navigate(route) { launchSingleTop = true }
        onStartRouteHandled()
    }

    ProvideAccessibilityVoiceGuide {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(BeaconDestinations.WELCOME) {
            WelcomeScreen(
                onGetStarted = { navController.navigate(BeaconDestinations.ONBOARDING) },
            )
        }
        composable(BeaconDestinations.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    appNavViewModel.completeFeatureTour()
                    navController.navigate(BeaconDestinations.PERMISSIONS) {
                        popUpTo(BeaconDestinations.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(BeaconDestinations.PERMISSIONS) {
            PermissionEducationScreen(
                onPermissionsReady = {
                    appNavViewModel.markSetupComplete()
                    val destination = if (guidanceMode == GuidanceInputMode.PhoneCamera) {
                        BeaconDestinations.PHONE_GUIDANCE
                    } else {
                        BeaconDestinations.HOME
                    }
                    navController.navigate(destination) {
                        popUpTo(BeaconDestinations.PERMISSIONS) { inclusive = true }
                    }
                },
            )
        }
        composable(BeaconDestinations.PAIRING) {
            GlassesPairingScreen(
                onConnected = {
                    navController.navigate(BeaconDestinations.HOME) {
                        popUpTo(BeaconDestinations.PAIRING) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(BeaconDestinations.HOME) {
                        popUpTo(BeaconDestinations.PAIRING) { inclusive = true }
                    }
                },
            )
        }
        composable(BeaconDestinations.HOME) {
            HomeGuidanceScreen(
                onOpenSettings = { navController.navigate(BeaconDestinations.SETTINGS) },
                onWhatIsAhead = { navController.navigate(BeaconDestinations.WHAT_IS_AHEAD) },
                onReadThis = { navController.navigate(BeaconDestinations.READ_THIS) },
                onWalkingMode = { navController.navigate(BeaconDestinations.WALKING_MODE) },
                onEmergency = { navController.navigate(BeaconDestinations.EMERGENCY) },
                onVoiceCommand = { navController.navigate(BeaconDestinations.VOICE_COMMAND) },
                onPhoneGuidance = { navController.navigate(BeaconDestinations.PHONE_GUIDANCE) },
            )
        }
        composable(BeaconDestinations.PHONE_GUIDANCE) {
            PhoneGuidanceScreen(
                onBackToHome = {
                    navController.navigate(BeaconDestinations.HOME) {
                        popUpTo(BeaconDestinations.PHONE_GUIDANCE) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(BeaconDestinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onGlassesStatus = { navController.navigate(BeaconDestinations.DEVICE_STATUS) },
                onVoiceSettings = { navController.navigate(BeaconDestinations.VOICE_SETTINGS) },
                onAccessibility = { navController.navigate(BeaconDestinations.ACCESSIBILITY_SETTINGS) },
                onModelPacks = { navController.navigate(BeaconDestinations.MODEL_PACKS) },
                onEmergency = { navController.navigate(BeaconDestinations.EMERGENCY) },
                onTrustedHelpers = { navController.navigate(BeaconDestinations.TRUSTED_HELPERS) },
                onHistory = { navController.navigate(BeaconDestinations.HISTORY) },
            )
        }
        composable(BeaconDestinations.TRUSTED_HELPERS) {
            TrustedHelpersScreen(onBack = { navController.popBackStack() })
        }
        composable(BeaconDestinations.MODEL_PACKS) {
            ModelPacksScreen(onBack = { navController.popBackStack() })
        }
        composable(BeaconDestinations.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = BeaconDestinations.routeWithAutoStart(BeaconDestinations.WHAT_IS_AHEAD),
            arguments = listOf(autoStartArg()),
        ) { entry ->
            WhatIsAheadScreen(
                autoStart = entry.arguments?.getBoolean(BeaconDestinations.ARG_AUTO_START) ?: false,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = BeaconDestinations.routeWithAutoStart(BeaconDestinations.READ_THIS),
            arguments = listOf(autoStartArg()),
        ) { entry ->
            ReadThisScreen(
                autoStart = entry.arguments?.getBoolean(BeaconDestinations.ARG_AUTO_START) ?: false,
                onBack = { navController.popBackStack() },
            )
        }
        composable(BeaconDestinations.DEVICE_STATUS) {
            DeviceStatusScreen(
                onUnbound = {
                    navController.navigate(BeaconDestinations.PAIRING) {
                        popUpTo(BeaconDestinations.HOME) { inclusive = true }
                    }
                },
                onOpenVoiceSettings = { navController.navigate(BeaconDestinations.VOICE_SETTINGS) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(BeaconDestinations.VOICE_SETTINGS) {
            VoiceSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(BeaconDestinations.WALKING_MODE) {
            WalkingModeScreen(onBack = { navController.popBackStack() })
        }
        composable(BeaconDestinations.EMERGENCY) {
            EmergencyScreen(onBack = { navController.popBackStack() })
        }
        composable(BeaconDestinations.VOICE_COMMAND) {
            fun go(route: String) {
                navController.navigate(route) {
                    popUpTo(BeaconDestinations.VOICE_COMMAND) { inclusive = true }
                }
            }
            VoiceCommandScreen(
                onWhatIsAhead = { go(BeaconDestinations.autoStart(BeaconDestinations.WHAT_IS_AHEAD)) },
                onReadThis = { go(BeaconDestinations.autoStart(BeaconDestinations.READ_THIS)) },
                onWalkingMode = { go(BeaconDestinations.WALKING_MODE) },
                onEmergency = { go(BeaconDestinations.EMERGENCY) },
                onGlassesStatus = { go(BeaconDestinations.SETTINGS) },
                onVoiceSettings = { go(BeaconDestinations.VOICE_SETTINGS) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(BeaconDestinations.ACCESSIBILITY_SETTINGS) {
            AccessibilitySettingsScreen(onBack = { navController.popBackStack() })
        }
    }
    }
}

private fun autoStartArg() = navArgument(BeaconDestinations.ARG_AUTO_START) {
    type = NavType.BoolType
    defaultValue = false
}
