package com.beacon.domain.phone

enum class PhoneModeSessionState {
    IDLE,
    ACTIVE,
    PAUSED_FOR_WHATSAPP,
    PAUSED_FOR_EMERGENCY,
    PAUSED_BY_USER,
    ERROR,
}
