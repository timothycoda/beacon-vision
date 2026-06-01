import { ICE_SERVERS } from "./config";
import type { SignalingClient } from "./signaling";

export interface WebRtcSessionOptions {
  signaling: SignalingClient;
  localPeerId: string;
  isInitiator: boolean;
  onRemoteStream: (stream: MediaStream) => void;
  onConnectionState: (state: RTCPeerConnectionState) => void;
}

export class WebRtcSession {
  private pc: RTCPeerConnection;
  private localStream: MediaStream | null = null;
  private remotePeerId: string | null = null;

  constructor(private readonly opts: WebRtcSessionOptions) {
    this.pc = new RTCPeerConnection({
      iceServers: ICE_SERVERS,
      iceCandidatePoolSize: 4,
      bundlePolicy: "max-bundle",
      rtcpMuxPolicy: "require",
    });
    this.pc.ontrack = (ev) => {
      if (ev.streams[0]) opts.onRemoteStream(ev.streams[0]);
    };
    this.pc.onicecandidate = (ev) => {
      if (!ev.candidate || !this.remotePeerId) return;
      opts.signaling.send({
        type: "ice",
        targetPeerId: this.remotePeerId,
        candidate: ev.candidate.toJSON(),
      });
    };
    this.pc.onconnectionstatechange = () => {
      opts.onConnectionState(this.pc.connectionState);
    };
  }

  setRemotePeer(peerId: string) {
    this.remotePeerId = peerId;
  }

  async startLocalMedia(video: boolean, audio: boolean): Promise<MediaStream> {
    this.localStream = await navigator.mediaDevices.getUserMedia({
      video: video ? { facingMode: "user" } : false,
      audio,
    });
    for (const track of this.localStream.getTracks()) {
      this.pc.addTrack(track, this.localStream);
    }
    return this.localStream;
  }

  attachSignalingHandler(
    handler: (msg: Record<string, unknown>) => void,
  ): (msg: Record<string, unknown>) => void {
    return (msg) => {
      const type = String(msg.type ?? "");
      if (type === "offer") void this.onOffer(msg);
      else if (type === "answer") void this.onAnswer(msg);
      else if (type === "ice") void this.onIce(msg);
      handler(msg);
    };
  }

  private async onOffer(msg: Record<string, unknown>) {
    const sdp = msg.sdp as RTCSessionDescriptionInit;
    await this.pc.setRemoteDescription(sdp);
    const answer = await this.pc.createAnswer();
    await this.pc.setLocalDescription(answer);
    this.opts.signaling.send({
      type: "answer",
      targetPeerId: String(msg.fromPeerId ?? this.remotePeerId),
      sdp: answer,
    });
  }

  private async onAnswer(msg: Record<string, unknown>) {
    const sdp = msg.sdp as RTCSessionDescriptionInit;
    await this.pc.setRemoteDescription(sdp);
  }

  private async onIce(msg: Record<string, unknown>) {
    const candidate = msg.candidate as RTCIceCandidateInit;
    if (candidate) {
      await this.pc.addIceCandidate(candidate);
    }
  }

  async createAndSendOffer() {
    const offer = await this.pc.createOffer();
    await this.pc.setLocalDescription(offer);
    this.opts.signaling.send({
      type: "offer",
      targetPeerId: this.remotePeerId,
      sdp: offer,
    });
  }

  toggleTrack(kind: "audio" | "video", enabled: boolean) {
    const tracks =
      kind === "audio"
        ? this.localStream?.getAudioTracks()
        : this.localStream?.getVideoTracks();
    tracks?.forEach((t) => {
      t.enabled = enabled;
    });
  }

  getLocalStream() {
    return this.localStream;
  }

  close() {
    this.localStream?.getTracks().forEach((t) => t.stop());
    this.pc.close();
  }
}
