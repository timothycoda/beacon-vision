package com.beacon.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.data.glasses.GlassesPreferences
import com.beacon.data.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Resolves the initial navigation destination (skip onboarding when already set up). */
@HiltViewModel
class AppNavViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    glassesPreferences: GlassesPreferences,
) : ViewModel() {

    fun completeFeatureTour() {
        viewModelScope.launch { appPreferences.setFeatureTourComplete() }
    }

    /** True when glasses were already paired before (returning user finishing the new tour). */
    val alreadySetUp: StateFlow<Boolean> = combine(
        appPreferences.onboardingComplete,
        glassesPreferences.lastAddress,
    ) { onboardingComplete, lastAddress ->
        onboardingComplete && !lastAddress.isNullOrBlank()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )

    val startDestination: StateFlow<String?> = combine(
        appPreferences.featureTourComplete,
        appPreferences.onboardingComplete,
        glassesPreferences.lastAddress,
    ) { tourComplete, onboardingComplete, lastAddress ->
        when {
            !tourComplete -> BeaconDestinations.WELCOME
            !onboardingComplete -> BeaconDestinations.PAIRING
            !lastAddress.isNullOrBlank() -> BeaconDestinations.HOME
            else -> BeaconDestinations.PAIRING
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
}
