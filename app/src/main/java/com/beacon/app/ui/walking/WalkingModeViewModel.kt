package com.beacon.app.ui.walking

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.app.walking.WalkingGuidanceService
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.glasses.model.ConnectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalkingModeUiState(
    val isRunning: Boolean = false,
    val glassesConnected: Boolean = false,
    val statusLine: String = "Tap Start when your glasses are connected.",
)

@HiltViewModel
class WalkingModeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val glassesRepository: GlassesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalkingModeUiState(isRunning = isServiceRunning()))
    val uiState: StateFlow<WalkingModeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            glassesRepository.connectionState.collect { connection ->
                refresh(connection)
            }
        }
    }

    fun refreshRunningState() {
        val connection = glassesRepository.connectionState.value
        refresh(connection, isServiceRunning())
    }

    fun start() {
        WalkingGuidanceService.start(context)
        refresh(glassesRepository.connectionState.value, running = true)
    }

    fun stop() {
        WalkingGuidanceService.stop(context)
        refresh(glassesRepository.connectionState.value, running = false)
    }

    private fun refresh(connection: ConnectionState, running: Boolean = isServiceRunning()) {
        val connected = connection is ConnectionState.Connected && connection.ready
        val status = when {
            running && connected ->
                "Walking mode is on. Beacon will describe what is ahead every few seconds."
            running && !connected ->
                "Walking mode is on, but glasses are not connected."
            !running && connected ->
                "Ready. Tap Start for periodic guidance while you walk."
            else ->
                "Connect your glasses first, then tap Start."
        }
        _uiState.update {
            WalkingModeUiState(
                isRunning = running,
                glassesConnected = connected,
                statusLine = status,
            )
        }
    }

    private fun isServiceRunning(): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == WalkingGuidanceService::class.java.name }
    }
}
