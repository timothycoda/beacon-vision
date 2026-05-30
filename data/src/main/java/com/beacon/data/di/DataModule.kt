package com.beacon.data.di

import com.beacon.data.emergency.EmergencyRepositoryImpl
import com.beacon.data.glasses.GlassesRepositoryImpl
import com.beacon.data.history.HistoryRepositoryImpl
import com.beacon.data.location.AndroidLocationProvider
import com.beacon.data.speech.SpeechController
import com.beacon.data.text.MlKitTextRecognizer
import com.beacon.data.vision.MlKitSceneDescriber
import com.beacon.data.voice.AndroidSpeechToText
import com.beacon.domain.emergency.EmergencyRepository
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.history.HistoryRepository
import com.beacon.domain.location.LocationProvider
import com.beacon.domain.speech.Speaker
import com.beacon.domain.speech.SpeechSettingsRepository
import com.beacon.data.speech.SpeechPreferences
import com.beacon.domain.text.TextRecognizer
import com.beacon.domain.vision.SceneDescriber
import com.beacon.domain.voice.SpeechToText
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindGlassesRepository(impl: GlassesRepositoryImpl): GlassesRepository

    @Binds
    @Singleton
    abstract fun bindSpeaker(impl: SpeechController): Speaker

    @Binds
    @Singleton
    abstract fun bindSceneDescriber(impl: MlKitSceneDescriber): SceneDescriber

    @Binds
    @Singleton
    abstract fun bindTextRecognizer(impl: MlKitTextRecognizer): TextRecognizer

    @Binds
    @Singleton
    abstract fun bindSpeechSettingsRepository(impl: SpeechPreferences): SpeechSettingsRepository

    @Binds
    @Singleton
    abstract fun bindEmergencyRepository(impl: EmergencyRepositoryImpl): EmergencyRepository

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: AndroidLocationProvider): LocationProvider

    @Binds
    @Singleton
    abstract fun bindSpeechToText(impl: AndroidSpeechToText): SpeechToText

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
}
