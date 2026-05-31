package com.beacon.app.ui.modelpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beacon.domain.guidance.GuidanceLanguage
import com.beacon.domain.guidance.ObserveGuidanceLanguageUseCase
import com.beacon.domain.guidance.SetGuidanceLanguageUseCase
import com.beacon.domain.modelpack.CancelModelPackDownloadUseCase
import com.beacon.domain.modelpack.DeleteModelPackUseCase
import com.beacon.domain.modelpack.DownloadModelPackUseCase
import com.beacon.domain.modelpack.ModelPackId
import com.beacon.domain.modelpack.ModelPackInstallState
import com.beacon.domain.modelpack.ModelPackKind
import com.beacon.domain.modelpack.ModelPackStatus
import com.beacon.domain.modelpack.ObserveModelPacksUseCase
import com.beacon.domain.modelpack.PauseModelPackDownloadUseCase
import com.beacon.domain.modelpack.SetDefaultNarrationPackUseCase
import com.beacon.domain.modelpack.SetDefaultVoicePackUseCase
import com.beacon.domain.modelpack.SetModelPackWifiOnlyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModelPacksUiState(
    val packs: List<ModelPackStatus> = emptyList(),
    val wifiOnly: Boolean = true,
    val guidanceLanguage: GuidanceLanguage = GuidanceLanguage.English,
    val downloadingPackId: ModelPackId? = null,
)

@HiltViewModel
class ModelPacksViewModel @Inject constructor(
    observeModelPacks: ObserveModelPacksUseCase,
    private val downloadPack: DownloadModelPackUseCase,
    private val deletePack: DeleteModelPackUseCase,
    private val cancelDownload: CancelModelPackDownloadUseCase,
    private val pauseDownload: PauseModelPackDownloadUseCase,
    private val setModelPackWifiOnly: SetModelPackWifiOnlyUseCase,
    private val setDefaultNarration: SetDefaultNarrationPackUseCase,
    private val setDefaultVoice: SetDefaultVoicePackUseCase,
    observeGuidanceLanguage: ObserveGuidanceLanguageUseCase,
    private val setGuidanceLanguageUseCase: SetGuidanceLanguageUseCase,
) : ViewModel() {

    private val wifiOnly = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            setModelPackWifiOnly.observe().collect { wifiOnly.value = it }
        }
    }

    val uiState: StateFlow<ModelPacksUiState> =
        combine(
            observeModelPacks(),
            wifiOnly,
            observeGuidanceLanguage(),
        ) { packs, wifi, language ->
            ModelPacksUiState(
                packs = packs,
                wifiOnly = wifi,
                guidanceLanguage = language,
                downloadingPackId = packs.firstOrNull { it.state == ModelPackInstallState.Downloading }?.id,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelPacksUiState())

    fun download(id: ModelPackId) {
        viewModelScope.launch { downloadPack(id) }
    }

    fun delete(id: ModelPackId) {
        viewModelScope.launch { deletePack(id) }
    }

    fun cancel(id: ModelPackId) {
        viewModelScope.launch { cancelDownload(id) }
    }

    fun pause(id: ModelPackId) {
        viewModelScope.launch { pauseDownload(id) }
    }

    fun setWifiOnlyDownload(enabled: Boolean) {
        viewModelScope.launch { setModelPackWifiOnly(enabled) }
    }

    fun setDefault(pack: ModelPackStatus) {
        viewModelScope.launch {
            when (pack.kind) {
                ModelPackKind.Narration -> setDefaultNarration(pack.id)
                ModelPackKind.Voice -> setDefaultVoice(pack.id)
                ModelPackKind.Utility -> Unit
            }
        }
    }

    fun setGuidanceLanguage(language: GuidanceLanguage) {
        if (uiState.value.guidanceLanguage == language) return
        viewModelScope.launch {
            setGuidanceLanguageUseCase(language)
        }
    }

    companion object {
        fun primaryActionLabel(pack: ModelPackStatus): String = when (pack.state) {
            ModelPackInstallState.NotInstalled -> "Download"
            ModelPackInstallState.Downloading -> "Pause download"
            ModelPackInstallState.Paused -> "Resume download"
            ModelPackInstallState.Installed -> when (pack.kind) {
                ModelPackKind.Narration -> if (pack.isDefaultNarration) "Active intelligence" else "Set as default"
                ModelPackKind.Voice -> if (pack.isDefaultVoice) "Active voice" else "Use this voice"
                ModelPackKind.Utility -> "Delete"
            }
            ModelPackInstallState.Failed -> "Retry"
        }
    }
}
