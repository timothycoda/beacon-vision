package com.beacon.app.livehelp

import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveHelpSignalingClient @Inject constructor(
    private val gson: Gson,
    private val endpoints: LiveHelpEndpoints,
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun connect(
        onMessage: (Map<String, Any?>) -> Unit,
        onFailure: (Throwable) -> Unit,
    ): WebSocket {
        val request = Request.Builder().url(endpoints.wsUrl).build()
        return client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    @Suppress("UNCHECKED_CAST")
                    val map = gson.fromJson(text, Map::class.java) as Map<String, Any?>
                    onMessage(map)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    onFailure(t)
                }
            },
        )
    }

    fun join(
        socket: WebSocket,
        roomId: String,
        role: String,
        displayName: String,
        token: String,
    ) {
        val payload = mapOf(
            "type" to "join",
            "roomId" to roomId,
            "role" to role,
            "displayName" to displayName,
            "token" to token,
        )
        socket.send(gson.toJson(payload))
    }

    fun sendJson(socket: WebSocket, payload: Map<String, Any?>) {
        socket.send(gson.toJson(payload))
    }
}
