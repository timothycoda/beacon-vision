package com.beacon.app.emergency

/** Normalizes a phone number for SMS intents and [SmsManager] sends. */
internal fun normalizeEmergencyPhone(raw: String): String {
    val trimmed = raw.trim()
    val builder = StringBuilder()
    for (ch in trimmed) {
        if (ch.isDigit() || (ch == '+' && builder.isEmpty())) {
            builder.append(ch)
        }
    }
    return builder.toString()
}
