package com.beacon.domain.accessibility

/** Stable keys for localized Voice Guide phrases (see `accessibility_phrases.xml`). */
enum class AccessibilityPhraseKey(val resSuffix: String) {
    HOME_INTRO("home_intro"),
    PHONE_MODE_INTRO("phone_mode_intro"),
    DOCUMENT_READER_INTRO("document_reader_intro"),
    WALKING_MODE_INTRO("walking_mode_intro"),
    EMERGENCY_INTRO("emergency_intro"),
    TRUSTED_HELPERS_INTRO("trusted_helpers_intro"),
    SETTINGS_INTRO("settings_intro"),
    LIVE_HELP_INTRO("live_help_intro"),
    SPEAK_COMMAND_DESCRIPTION("speak_command_description"),
    WHAT_IS_AHEAD_DESCRIPTION("what_is_ahead_description"),
    READ_THIS_DESCRIPTION("read_this_description"),
    WALKING_MODE_DESCRIPTION("walking_mode_description"),
    CALL_HELPER_DESCRIPTION("call_helper_description"),
    START_LIVE_HELP_DESCRIPTION("start_live_help_description"),
    EMERGENCY_DESCRIPTION("emergency_description"),
    CAMERA_PERMISSION_MISSING("camera_permission_missing"),
    MICROPHONE_PERMISSION_MISSING("microphone_permission_missing"),
    LOCATION_PERMISSION_MISSING("location_permission_missing"),
    LOW_LIGHT_WARNING("low_light_warning"),
    BLURRY_IMAGE_WARNING("blurry_image_warning"),
    GLASSES_DISCONNECTED("glasses_disconnected"),
    PHONE_MODE_RESUMED("phone_mode_resumed"),
    HELPER_CALL_STARTING("helper_call_starting"),
    LIVE_HELP_WAITING("live_help_waiting"),
    LIVE_HELP_CONNECTED("live_help_connected"),
    LIVE_HELP_FAILED("live_help_failed"),
    LIVE_HELP_ENDED("live_help_ended"),
    EMERGENCY_COUNTDOWN("emergency_countdown"),
    EMERGENCY_CANCELLED("emergency_cancelled"),
    VOICE_GUIDE_ENABLED("voice_guide_enabled"),
    VOICE_GUIDE_DISABLED("voice_guide_disabled"),
    ;

    val stringResourceName: String get() = "a11y_phrase_$resSuffix"
}
