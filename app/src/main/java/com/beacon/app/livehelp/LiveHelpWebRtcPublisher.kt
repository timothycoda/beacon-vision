package com.beacon.app.livehelp

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveHelpWebRtcPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val eglBase: EglBase = EglBase.create()
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoTrack: VideoTrack? = null
    private var audioTrack: AudioTrack? = null

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
    )

    @Synchronized
    fun ensureFactory() {
        if (factory != null) return
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context.applicationContext)
                .setEnableInternalTracer(false)
                .createInitializationOptions(),
        )
        factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true),
            )
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
    }

    fun createConnection(
        onIceCandidate: (IceCandidate) -> Unit,
        onState: (PeerConnection.PeerConnectionState) -> Unit,
    ): PeerConnection {
        ensureFactory()
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceCandidatePoolSize = 4
        }
        val pc = factory!!.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(state: PeerConnection.SignalingState?) = Unit
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) = Unit
                override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) = Unit
                override fun onIceCandidate(candidate: IceCandidate?) {
                    candidate?.let(onIceCandidate)
                }

                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) = Unit
                override fun onAddStream(stream: org.webrtc.MediaStream?) = Unit
                override fun onRemoveStream(stream: org.webrtc.MediaStream?) = Unit
                override fun onDataChannel(channel: org.webrtc.DataChannel?) = Unit
                override fun onRenegotiationNeeded() = Unit
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out org.webrtc.MediaStream>?) =
                    Unit

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    newState?.let(onState)
                }
            },
        ) ?: error("peer_connection_failed")
        peerConnection = pc
        return pc
    }

    fun attachLocalMedia(pc: PeerConnection) {
        ensureFactory()
        val enumerator = Camera2Enumerator(context)
        val cameraName = enumerator.deviceNames.firstOrNull { enumerator.isBackFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()
            ?: error("no_camera")

        videoCapturer = enumerator.createCapturer(cameraName, null)
        surfaceTextureHelper = SurfaceTextureHelper.create("LiveHelpCapture", eglBase.eglBaseContext)
        val videoSource: VideoSource = factory!!.createVideoSource(false)
        videoCapturer!!.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
        videoCapturer!!.startCapture(640, 480, 24)

        videoTrack = factory!!.createVideoTrack("video0", videoSource).apply {
            setEnabled(true)
        }
        val audioSource: AudioSource = factory!!.createAudioSource(MediaConstraints())
        audioTrack = factory!!.createAudioTrack("audio0", audioSource).apply {
            setEnabled(true)
        }
        pc.addTrack(videoTrack, listOf("stream"))
        pc.addTrack(audioTrack, listOf("stream"))
    }

    suspend fun createOffer(pc: PeerConnection): SessionDescription = suspendCreateSdp { cb ->
        pc.createOffer(cb, MediaConstraints())
    }

    suspend fun createAnswer(pc: PeerConnection): SessionDescription = suspendCreateSdp { cb ->
        pc.createAnswer(cb, MediaConstraints())
    }

    suspend fun setLocalDescription(pc: PeerConnection, sdp: SessionDescription) =
        suspendSetSdp { cb -> pc.setLocalDescription(cb, sdp) }

    suspend fun setRemoteDescription(pc: PeerConnection, sdp: SessionDescription) =
        suspendSetSdp { cb -> pc.setRemoteDescription(cb, sdp) }

    fun addIceCandidate(pc: PeerConnection, candidate: IceCandidate) {
        pc.addIceCandidate(candidate)
    }

    fun release() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null
        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null
        videoTrack?.dispose()
        videoTrack = null
        audioTrack?.dispose()
        audioTrack = null
        peerConnection?.close()
        peerConnection = null
    }

    private suspend fun suspendCreateSdp(
        block: (org.webrtc.SdpObserver) -> Unit,
    ): SessionDescription = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        block(
            object : org.webrtc.SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp != null) cont.resume(sdp) { }
                    else cont.cancel(IllegalStateException("null_sdp"))
                }

                override fun onSetSuccess() = Unit
                override fun onCreateFailure(error: String?) {
                    cont.cancel(IllegalStateException(error ?: "sdp_create_failed"))
                }

                override fun onSetFailure(error: String?) = Unit
            },
        )
    }

    private suspend fun suspendSetSdp(block: (org.webrtc.SdpObserver) -> Unit) {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            block(
                object : org.webrtc.SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) = Unit
                    override fun onSetSuccess() {
                        cont.resume(Unit) { }
                    }

                    override fun onCreateFailure(error: String?) = Unit
                    override fun onSetFailure(error: String?) {
                        cont.cancel(IllegalStateException(error ?: "sdp_set_failed"))
                    }
                },
            )
        }
    }
}
