package com.beacon.app.livehelp

import com.beacon.domain.livehelp.LiveHelpRoomCredentials
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.WebSocket
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import javax.inject.Inject
import javax.inject.Singleton

enum class LiveHelpConnectionState {
    Idle,
    Connecting,
    WaitingForHelper,
    Connected,
    Failed,
    Ended,
}

data class LiveHelpSessionState(
    val connectionState: LiveHelpConnectionState = LiveHelpConnectionState.Idle,
    val roomId: String? = null,
    val statusMessage: String? = null,
    val helperConnected: Boolean = false,
)

@Singleton
class LiveHelpSessionController @Inject constructor(
    private val api: LiveHelpApi,
    private val signaling: LiveHelpSignalingClient,
    private val webrtc: LiveHelpWebRtcPublisher,
    private val endpoints: LiveHelpEndpoints,
) {
    private val _state = MutableStateFlow(LiveHelpSessionState())
    val state: StateFlow<LiveHelpSessionState> = _state.asStateFlow()

    private var socket: WebSocket? = null
    private var peerConnection: PeerConnection? = null
    private var localPeerId: String? = null
    private var remotePeerId: String? = null
    private var credentials: LiveHelpRoomCredentials? = null
    private var scope: CoroutineScope? = null
    private var offerSent = false

    fun helperInviteUrl(): String? {
        val creds = credentials ?: return null
        val base = endpoints.helperWebBase.trimEnd('/')
        return "$base/?room=${creds.roomId}&token=${creds.helperToken}"
    }

    fun start(scope: CoroutineScope) {
        this.scope = scope
        offerSent = false
        _state.value = LiveHelpSessionState(connectionState = LiveHelpConnectionState.Connecting)
        scope.launch {
            _state.update {
                it.copy(
                    connectionState = LiveHelpConnectionState.Connecting,
                    statusMessage = "Creating secure session…",
                )
            }
            val creds = api.createRoom().getOrElse { err ->
                _state.update {
                    it.copy(
                        connectionState = LiveHelpConnectionState.Failed,
                        statusMessage = "Could not reach Live Help server. Check Wi‑Fi and liveHelp.host in local.properties.",
                    )
                }
                return@launch
            }
            credentials = creds
            _state.update {
                it.copy(
                    roomId = creds.roomId,
                    statusMessage = "Connecting…",
                )
            }
            connectSignaling(creds, scope)
        }
    }

    private fun connectSignaling(creds: LiveHelpRoomCredentials, scope: CoroutineScope) {
        socket = signaling.connect(
            onMessage = { msg -> handleSignalingMessage(msg, scope) },
            onFailure = {
                _state.update {
                    it.copy(
                        connectionState = LiveHelpConnectionState.Failed,
                        statusMessage = "Signaling connection lost.",
                    )
                }
            },
        )
        signaling.join(socket!!, creds.roomId, "user", "User", creds.userToken)
    }

    private fun handleSignalingMessage(msg: Map<String, Any?>, scope: CoroutineScope) {
        when (msg["type"]) {
            "error" -> {
                _state.update {
                    it.copy(
                        connectionState = LiveHelpConnectionState.Failed,
                        statusMessage = "Join failed: ${msg["message"]}",
                    )
                }
            }
            "joined" -> {
                localPeerId = msg["peerId"]?.toString()
                @Suppress("UNCHECKED_CAST")
                val peers = msg["peers"] as? List<Map<String, Any?>> ?: emptyList()
                val helper = peers.firstOrNull { it["role"] == "helper" }
                if (helper != null) {
                    remotePeerId = helper["id"]?.toString()
                }
                scope.launch { ensureWebRtc(helperAlreadyPresent = helper != null) }
                _state.update {
                    it.copy(
                        connectionState = LiveHelpConnectionState.WaitingForHelper,
                        statusMessage = if (helper != null) {
                            "Helper connected. Starting video…"
                        } else {
                            "Share the invite link with your helper."
                        },
                    )
                }
            }
            "peer-joined" -> {
                @Suppress("UNCHECKED_CAST")
                val peer = msg["peer"] as? Map<String, Any?> ?: return
                if (peer["role"] == "helper") {
                    remotePeerId = peer["id"]?.toString()
                    _state.update {
                        it.copy(helperConnected = true, statusMessage = "Helper joined.")
                    }
                    scope.launch {
                        ensureWebRtc(helperAlreadyPresent = true)
                        maybeCreateOffer()
                    }
                }
            }
            "ready" -> {
                if (msg["initiator"] == true) {
                    scope.launch { maybeCreateOffer() }
                }
            }
            "offer", "answer", "ice" -> scope.launch { handleWebRtcSignal(msg) }
        }
    }

    private suspend fun ensureWebRtc(helperAlreadyPresent: Boolean) {
        if (peerConnection != null) return
        val pc = webrtc.createConnection(
            onIceCandidate = { candidate -> sendIce(candidate) },
            onState = { state ->
                when (state) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        _state.update {
                            it.copy(
                                connectionState = LiveHelpConnectionState.Connected,
                                helperConnected = true,
                                statusMessage = "Live — helper can see your camera.",
                            )
                        }
                    }
                    PeerConnection.PeerConnectionState.FAILED,
                    PeerConnection.PeerConnectionState.DISCONNECTED,
                    -> {
                        _state.update {
                            it.copy(
                                connectionState = LiveHelpConnectionState.Failed,
                                statusMessage = "Video connection failed.",
                            )
                        }
                    }
                    else -> Unit
                }
            },
        )
        peerConnection = pc
        webrtc.attachLocalMedia(pc)
        if (helperAlreadyPresent && remotePeerId != null) {
            maybeCreateOffer()
        }
    }

    private suspend fun maybeCreateOffer() {
        val pc = peerConnection ?: return
        if (remotePeerId.isNullOrBlank() || offerSent) return
        offerSent = true
        val offer = webrtc.createOffer(pc)
        webrtc.setLocalDescription(pc, offer)
        sendSdp("offer", offer)
    }

    private suspend fun handleWebRtcSignal(msg: Map<String, Any?>) {
        val pc = peerConnection ?: return
        val fromPeerId = msg["fromPeerId"]?.toString()
        if (remotePeerId == null && fromPeerId != null) remotePeerId = fromPeerId

        when (msg["type"]) {
            "offer" -> {
                @Suppress("UNCHECKED_CAST")
                val sdpMap = msg["sdp"] as? Map<String, Any?> ?: return
                val sdp = SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(sdpMap["type"]?.toString() ?: "offer"),
                    sdpMap["sdp"]?.toString() ?: return,
                )
                webrtc.setRemoteDescription(pc, sdp)
                val answer = webrtc.createAnswer(pc)
                webrtc.setLocalDescription(pc, answer)
                sendSdp("answer", answer)
            }
            "answer" -> {
                @Suppress("UNCHECKED_CAST")
                val sdpMap = msg["sdp"] as? Map<String, Any?> ?: return
                val sdp = SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(sdpMap["type"]?.toString() ?: "answer"),
                    sdpMap["sdp"]?.toString() ?: return,
                )
                webrtc.setRemoteDescription(pc, sdp)
            }
            "ice" -> {
                @Suppress("UNCHECKED_CAST")
                val cand = msg["candidate"] as? Map<String, Any?> ?: return
                val candidate = IceCandidate(
                    cand["sdpMid"]?.toString(),
                    (cand["sdpMLineIndex"] as? Number)?.toInt() ?: 0,
                    cand["candidate"]?.toString() ?: return,
                )
                webrtc.addIceCandidate(pc, candidate)
            }
        }
    }

    private fun sendSdp(type: String, description: SessionDescription) {
        val socket = socket ?: return
        val target = remotePeerId ?: return
        signaling.sendJson(
            socket,
            mapOf(
                "type" to type,
                "targetPeerId" to target,
                "sdp" to mapOf(
                    "type" to description.type.canonicalForm(),
                    "sdp" to description.description,
                ),
            ),
        )
    }

    private fun sendIce(candidate: IceCandidate) {
        val socket = socket ?: return
        val target = remotePeerId ?: return
        signaling.sendJson(
            socket,
            mapOf(
                "type" to "ice",
                "targetPeerId" to target,
                "candidate" to mapOf(
                    "sdpMid" to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex,
                    "candidate" to candidate.sdp,
                ),
            ),
        )
    }

    fun sendSceneSummary(summary: String) {
        val socket = socket ?: return
        val text = summary.trim().take(500)
        if (text.isEmpty()) return
        signaling.sendJson(socket, mapOf("type" to "scene", "summary" to text))
    }

    fun end() {
        webrtc.release()
        socket?.close(1000, "user_ended")
        socket = null
        peerConnection = null
        credentials = null
        localPeerId = null
        remotePeerId = null
        offerSent = false
        _state.value = LiveHelpSessionState(connectionState = LiveHelpConnectionState.Ended)
    }
}
