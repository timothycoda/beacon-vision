package com.beacon.app.ui.livehelp

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.app.livehelp.LiveHelpConnectionState
import com.beacon.app.livehelp.LiveHelpSessionController
import com.beacon.app.livehelp.LiveHelpSessionState
import com.beacon.data.phone.PhoneModeLatestImageHolder
import com.beacon.domain.vision.usecase.DescribeSceneUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveHelpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: LiveHelpSessionController,
    private val describeScene: DescribeSceneUseCase,
    private val latestImageHolder: PhoneModeLatestImageHolder,
) : ViewModel() {

    val uiState: StateFlow<LiveHelpSessionState> = session.state.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        LiveHelpSessionState(),
    )

    private var sceneJob: Job? = null
    private var started = false

    fun startSession() {
        if (started) return
        started = true
        session.start(viewModelScope)
        sceneJob = viewModelScope.launch {
            while (isActive) {
                delay(SCENE_INTERVAL_MS)
                if (session.state.value.connectionState != LiveHelpConnectionState.Connected) continue
                publishSceneSummary()
            }
        }
    }

    private suspend fun publishSceneSummary() {
        val summary = latestImageHolder.latestSceneSummary()
        if (!summary.isNullOrBlank()) {
            session.sendSceneSummary(summary)
            return
        }
        val jpeg = latestImageHolder.latestJpegBytes() ?: return
        val image = com.beacon.domain.vision.CapturedImage(jpegBytes = jpeg)
        when (val result = describeScene(image)) {
            is com.beacon.core.result.OperationResult.Success -> {
                session.sendSceneSummary(result.value.spokenSummary)
            }
            else -> Unit
        }
    }

    fun shareHelperInvite(): Intent {
        val url = session.helperInviteUrl() ?: return Intent()
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Elenii Live Help invite")
            putExtra(
                Intent.EXTRA_TEXT,
                "Join my Live Help session (secure link, expires in 30 minutes):\n$url",
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun copyInviteLabel(): String = session.helperInviteUrl() ?: ""

    fun endSession(onDone: () -> Unit) {
        sceneJob?.cancel()
        session.end()
        started = false
        onDone()
    }

    companion object {
        private const val SCENE_INTERVAL_MS = 12_000L
    }
}
