package com.beacon.data.speech

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.data.modelpack.ModelPackPaths
import com.beacon.data.modelpack.ModelPackStorage
import com.beacon.domain.modelpack.ModelPackId
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.math.min

/**
 * Lazy Hausa TTS via sherpa-onnx. Native libs load on first use (background thread).
 * Only one [AudioTrack] plays at a time so interrupted phrases do not echo.
 */
@Singleton
class HausaMmsTtsEngine @Inject constructor(
    private val paths: ModelPackPaths,
    private val storage: ModelPackStorage,
    private val packInstaller: HausaVoicePackInstaller,
    private val dispatchers: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val initMutex = Mutex()
    private var offlineTts: OfflineTts? = null
    private var speakJob: Job? = null

    @Volatile
    private var activeTrack: AudioTrack? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val speechAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    fun isAvailable(): Boolean = paths.isHausaVoiceInstalled()

    /** Loads sherpa + ONNX on a background thread so the first spoken cue is not silent. */
    fun warmUp() {
        if (!isAvailable()) return
        scope.launch {
            runCatching {
                if (!SherpaNativeLoader.ensureLoaded()) return@launch
                packInstaller.ensurePackLayout()
                ensureEngine()
            }.onFailure { BeaconLog.e(TAG, "Hausa MMS warmUp failed", it) }
        }
    }

    /**
     * @return true if audio was generated and played; false to fall back to system TTS.
     */
    fun speak(text: String, speed: Float, interrupt: Boolean, onComplete: (Boolean) -> Unit = {}) {
        if (text.isBlank() || !isAvailable()) {
            onComplete(false)
            return
        }
        if (interrupt) stop()
        speakJob?.cancel()
        speakJob = scope.launch {
            _isSpeaking.value = true
            var ok = false
            try {
                if (!SherpaNativeLoader.ensureLoaded()) return@launch
                val engine = ensureEngine() ?: return@launch
                val clipped = text.take(MAX_CHARS)
                val audio = engine.generate(
                    text = clipped,
                    speed = speed.coerceIn(0.5f, 2.0f),
                )
                if (audio.samples.isNotEmpty()) {
                    playSamples(audio.samples, audio.sampleRate)
                    ok = true
                }
            } catch (e: Throwable) {
                BeaconLog.e(TAG, "Hausa MMS speak failed", e)
            } finally {
                _isSpeaking.value = false
                withContext(dispatchers.main) {
                    onComplete(ok)
                }
            }
        }
    }

    fun stop() {
        speakJob?.cancel()
        speakJob = null
        stopActiveTrack()
        _isSpeaking.value = false
    }

    fun release() {
        stop()
        scope.launch {
            initMutex.withLock {
                offlineTts?.release()
                offlineTts = null
            }
        }
    }

    private fun stopActiveTrack() {
        val track = activeTrack ?: return
        activeTrack = null
        runCatching {
            track.pause()
            track.flush()
            track.stop()
            track.release()
        }
    }

    private suspend fun ensureEngine(): OfflineTts? = withContext(dispatchers.io) {
        initMutex.withLock {
            offlineTts?.let { return@withLock it }
            if (!SherpaNativeLoader.ensureLoaded()) return@withLock null
            if (!packInstaller.ensurePackLayout()) return@withLock null
            val dir = storage.packDir(ModelPackId.HAUSA_VOICE).absolutePath
            val config = getOfflineTtsConfig(
                modelDir = dir,
                modelName = MODEL_ONNX,
                numThreads = 1,
            )
            runCatching {
                OfflineTts(config = config).also {
                    offlineTts = it
                    BeaconLog.i(TAG, "Hausa MMS TTS ready (sampleRate=${it.sampleRate()})")
                }
            }.onFailure {
                BeaconLog.e(TAG, "Failed to load Hausa MMS TTS", it)
            }.getOrNull()
        }
    }

    private suspend fun playSamples(samples: FloatArray, sampleRate: Int) {
        stopActiveTrack()
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        ).coerceAtLeast(1)
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setSampleRate(sampleRate)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(speechAttributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuf * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        activeTrack = track
        try {
            track.play()
            var offset = 0
            while (offset < samples.size) {
                coroutineContext.ensureActive()
                if (activeTrack !== track) return
                val chunk = min(CHUNK_SAMPLES, samples.size - offset)
                track.write(samples, offset, chunk, AudioTrack.WRITE_BLOCKING)
                offset += chunk
            }
        } finally {
            runCatching {
                track.stop()
                track.release()
            }
            if (activeTrack === track) activeTrack = null
        }
    }

    private companion object {
        const val TAG = "HausaMmsTts"
        const val MODEL_ONNX = "model.onnx"
        const val MAX_CHARS = 280
        const val CHUNK_SAMPLES = 8_192
    }
}

/** Loads sherpa-onnx JNI once; failures disable MMS for this process. */
internal object SherpaNativeLoader {
    @Volatile
    private var loaded = false

    @Volatile
    private var failed = false

    fun ensureLoaded(): Boolean {
        if (loaded) return true
        if (failed) return false
        synchronized(this) {
            if (loaded) return true
            if (failed) return false
            return runCatching {
                System.loadLibrary("sherpa-onnx-jni")
                loaded = true
                true
            }.onFailure {
                failed = true
                BeaconLog.e("SherpaNative", "Failed to load sherpa-onnx-jni", it)
            }.getOrDefault(false)
        }
    }
}
