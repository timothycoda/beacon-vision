package com.beacon.app.livehelp

import com.beacon.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveHelpEndpoints @Inject constructor() {
    val httpBase: String = BuildConfig.LIVE_HELP_HTTP
    val wsUrl: String = BuildConfig.LIVE_HELP_WS
    val helperWebBase: String = BuildConfig.LIVE_HELP_WEB
}
