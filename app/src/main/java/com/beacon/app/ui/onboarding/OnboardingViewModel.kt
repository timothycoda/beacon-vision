package com.beacon.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.domain.device.GuidanceInputMode
import com.beacon.domain.device.SetGuidanceInputModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val selectedMode: GuidanceInputMode? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val setGuidanceInputMode: SetGuidanceInputModeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun selectMode(mode: GuidanceInputMode) {
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun persistModeAndFinish(onDone: (GuidanceInputMode) -> Unit) {
        val mode = _uiState.value.selectedMode ?: return
        viewModelScope.launch {
            setGuidanceInputMode(mode)
            onDone(mode)
        }
    }
}
