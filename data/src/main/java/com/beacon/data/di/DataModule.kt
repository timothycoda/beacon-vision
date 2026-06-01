package com.beacon.data.di

import com.beacon.data.accessibility.AccessibilityPreferencesImpl
import com.beacon.data.accessibility.AccessibilityVoiceGuideImpl
import com.beacon.data.accessibility.ResourceAccessibilityPhraseProvider
import com.beacon.data.device.DevicePreferencesImpl
import com.beacon.domain.accessibility.AccessibilityPhraseProvider
import com.beacon.domain.accessibility.AccessibilitySettingsRepository
import com.beacon.domain.accessibility.AccessibilityVoiceGuide
import com.beacon.data.guidance.GuidanceLanguageRepositoryImpl
import com.beacon.data.emergency.EmergencyRepositoryImpl
import com.beacon.data.helper.TrustedHelperRepositoryImpl
import com.beacon.data.phone.PhoneModeLatestImageHolder
import com.beacon.domain.helper.TrustedHelperRepository
import com.beacon.domain.vision.LatestImageProvider
import com.beacon.data.glasses.GlassesRepositoryImpl
import com.beacon.data.history.HistoryRepositoryImpl
import com.beacon.data.location.AndroidLocationProvider
import com.beacon.data.modelpack.ModelPackRepositoryImpl
import com.beacon.data.speech.SpeechController
import com.beacon.data.speech.SpeechPreferences
import com.beacon.data.text.MlKitTextRecognizer
import com.beacon.data.vision.CompositeSceneDescriber
import com.beacon.data.vision.MlKitObjectDetector
import com.beacon.domain.vision.ObjectDetector
import com.beacon.data.voice.AndroidSpeechToText
import com.beacon.domain.device.DevicePreferences
import com.beacon.domain.guidance.GuidanceLanguageRepository
import com.beacon.domain.emergency.EmergencyRepository
import com.beacon.domain.glasses.GlassesRepository
import com.beacon.domain.history.HistoryRepository
import com.beacon.domain.location.LocationProvider
import com.beacon.domain.modelpack.ModelPackRepository
import com.beacon.domain.speech.Speaker
import com.beacon.domain.speech.SpeechSettingsRepository
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
    abstract fun bindSceneDescriber(impl: CompositeSceneDescriber): SceneDescriber

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
    abstract fun bindTrustedHelperRepository(
        impl: TrustedHelperRepositoryImpl,
    ): TrustedHelperRepository

    @Binds
    @Singleton
    abstract fun bindLatestImageProvider(impl: PhoneModeLatestImageHolder): LatestImageProvider

    @Binds
    @Singleton
    abstract fun bindLocationProvider(impl: AndroidLocationProvider): LocationProvider

    @Binds
    @Singleton
    abstract fun bindSpeechToText(impl: AndroidSpeechToText): SpeechToText

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindModelPackRepository(impl: ModelPackRepositoryImpl): ModelPackRepository

    @Binds
    @Singleton
    abstract fun bindDevicePreferences(impl: DevicePreferencesImpl): DevicePreferences

    @Binds
    @Singleton
    abstract fun bindGuidanceLanguageRepository(
        impl: GuidanceLanguageRepositoryImpl,
    ): GuidanceLanguageRepository

    @Binds
    @Singleton
    abstract fun bindObjectDetector(impl: MlKitObjectDetector): ObjectDetector

    @Binds
    @Singleton
    abstract fun bindAccessibilitySettingsRepository(
        impl: AccessibilityPreferencesImpl,
    ): AccessibilitySettingsRepository

    @Binds
    @Singleton
    abstract fun bindAccessibilityPhraseProvider(
        impl: ResourceAccessibilityPhraseProvider,
    ): AccessibilityPhraseProvider

    @Binds
    @Singleton
    abstract fun bindAccessibilityVoiceGuide(
        impl: AccessibilityVoiceGuideImpl,
    ): AccessibilityVoiceGuide
}
