package com.beacon.app.ui.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.domain.device.GuidanceInputMode
import com.beacon.domain.device.ObserveGuidanceInputModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PermissionEducationViewModel @Inject constructor(
    observeGuidanceMode: ObserveGuidanceInputModeUseCase,
) : ViewModel() {

    val phoneSetup: StateFlow<Boolean> = observeGuidanceMode()
        .map { it == GuidanceInputMode.PhoneCamera }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
