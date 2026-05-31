package com.beacon.app.vision

import android.content.Context
import com.beacon.core.concurrency.DispatcherProvider
import com.beacon.core.log.BeaconLog
import com.beacon.data.guidance.GuidanceLanguagePreferences
import com.beacon.data.modelpack.ModelPackPaths
import com.beacon.data.modelpack.ModelPackPreferences
import com.beacon.domain.guidance.GuidanceLanguage
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GemmaNarrationEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val paths: ModelPackPaths,
    private val packPrefs: ModelPackPreferences,
    private val languagePrefs: GuidanceLanguagePreferences,
    private val dispatchers: DispatcherProvider,
) {
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedPath: String? = null

    suspend fun narrate(detectedLabels: List<String>): String? = withContext(dispatchers.io) {
        if (detectedLabels.isEmpty()) return@withContext null
        val language = languagePrefs.language.first()
        mutex.withLock {
            if (!ensureEngineLocked()) return@withLock null
            val prompt = buildPrompt(detectedLabels, language)
            runCatching {
                conversation?.sendMessage(prompt)?.toString()?.trim()
            }.getOrElse {
                BeaconLog.e(TAG, "Gemma narration failed", it)
                null
            }
        }
    }

    fun reset() {
        runCatching { conversation?.close() }
        runCatching { engine?.close() }
        conversation = null
        engine = null
        loadedPath = null
    }

    private suspend fun ensureEngineLocked(): Boolean {
        val preferred = packPrefs.defaultNarrationPack.first()
        val modelFile = paths.installedNarrationModelFile(preferred) ?: return false
        val path = modelFile.absolutePath
        if (engine != null && loadedPath == path) return true

        reset()
        return runCatching {
            val config = EngineConfig(
                modelPath = path,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.absolutePath,
            )
            val newEngine = Engine(config)
            newEngine.initialize()
            engine = newEngine
            conversation = newEngine.createConversation()
            loadedPath = path
            BeaconLog.i(TAG, "Narration model ready: ${modelFile.name}")
            true
        }.getOrElse {
            BeaconLog.e(TAG, "Narration model init failed", it)
            reset()
            false
        }
    }

    private fun buildPrompt(labels: List<String>, language: GuidanceLanguage): String {
        val list = labels.joinToString(", ") { it.lowercase() }
        return when (language) {
            GuidanceLanguage.Hausa -> """
                You help blind users navigate. Detected objects: $list.
                Reply in Hausa only with one short spoken sentence for text-to-speech.
                Start with "A gaban ka". Under 25 words. No English. No lists.
            """.trimIndent()
            GuidanceLanguage.English -> """
                You help blind users navigate. Detected objects: $list.
                Reply with one short spoken sentence for text-to-speech.
                Start with "Ahead of you". Under 25 words. No lists.
            """.trimIndent()
        }
    }

    private companion object {
        const val TAG = "GemmaNarrationEngine"
    }
}
