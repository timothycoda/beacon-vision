package com.beacon.domain.livehelp

/** Secure room created on the signaling server (user + helper tokens). */
data class LiveHelpRoomCredentials(
    val roomId: String,
    val userToken: String,
    val helperToken: String,
    val expiresAtEpochMs: Long,
)
