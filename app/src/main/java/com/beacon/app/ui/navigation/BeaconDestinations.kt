package com.beacon.app.ui.navigation

/** App navigation routes. */
object BeaconDestinations {
    const val WELCOME = "welcome"
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"
    const val PAIRING = "pairing"
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val HISTORY = "history"
    const val DEVICE_STATUS = "device_status"
    const val WHAT_IS_AHEAD = "what_is_ahead"
    const val READ_THIS = "read_this"
    const val VOICE_SETTINGS = "voice_settings"
    const val WALKING_MODE = "walking_mode"
    const val EMERGENCY = "emergency"
    const val VOICE_COMMAND = "voice_command"

    /** Argument name signalling a feature should run automatically on open. */
    const val ARG_AUTO_START = "autostart"

    /** Route pattern that accepts an optional [ARG_AUTO_START] flag. */
    fun routeWithAutoStart(base: String): String = "$base?$ARG_AUTO_START={$ARG_AUTO_START}"

    /** Concrete route that opens [base] and triggers its action immediately. */
    fun autoStart(base: String): String = "$base?$ARG_AUTO_START=true"
}
