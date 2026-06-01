import { signalingHttpBase } from "./config";
import { SignalingClient } from "./signaling";
import { WebRtcSession } from "./webrtc-session";

const statusEl = document.getElementById("status")!;
const preview = document.getElementById("localPreview") as HTMLVideoElement;
const form = document.getElementById("joinForm") as HTMLFormElement;
const roomInput = document.getElementById("roomInput") as HTMLInputElement;

function setStatus(text: string) {
  statusEl.textContent = text;
}

const params = new URLSearchParams(window.location.search);
if (params.get("room")) {
  roomInput.value = params.get("room")!.toUpperCase();
}

form.addEventListener("submit", async (e) => {
  e.preventDefault();
  const roomId = roomInput.value.trim().toUpperCase();
  if (!/^[A-Z0-9]{6}$/.test(roomId)) {
    setStatus("Invalid room code.");
    return;
  }

  let userToken = params.get("token") ?? "";
  if (!userToken) {
    setStatus("Creating secure room…");
    try {
      const res = await fetch(`${signalingHttpBase()}/rooms`, { method: "POST" });
      if (!res.ok) throw new Error("create_failed");
      const data = (await res.json()) as { roomId: string; userToken: string };
      roomInput.value = data.roomId;
      userToken = data.userToken;
      const helperUrl = `${window.location.origin}/?room=${data.roomId}&token=NEEDS_HELPER_TOKEN`;
      setStatus(`Room ${data.roomId}. Open helper link from app. (Dev: check server logs / API for helperToken)`);
      console.info("userToken", userToken, "open helper with helperToken from POST /rooms");
      console.info(helperUrl);
    } catch {
      setStatus("Cannot create room — is signaling running?");
      return;
    }
  }

  const signaling = new SignalingClient();
  let webrtc: WebRtcSession | null = null;
  let helperPeerId: string | null = null;

  const handleMsg = (msg: Record<string, unknown>) => {
    const type = String(msg.type ?? "");
    if (type === "error") {
      setStatus(`Error: ${String(msg.message)}`);
      return;
    }
    if (type === "joined") {
      const localPeerId = String(msg.peerId);
      const peers = (msg.peers ?? []) as { id: string; role: string }[];
      const helper = peers.find((p) => p.role === "helper");
      if (helper) helperPeerId = helper.id;
      void startAsUser(localPeerId, helper?.id);
      setStatus(
        helper
          ? "Helper waiting. Starting camera…"
          : `Joined ${roomId}. Open helper invite in another browser.`,
      );
      return;
    }
    if (type === "peer-joined") {
      const peer = msg.peer as { id: string; role: string };
      if (peer.role === "helper") {
        helperPeerId = peer.id;
        webrtc?.setRemotePeer(peer.id);
        setStatus("Helper connected.");
      }
      return;
    }
    if (type === "ready" && msg.initiator === true && webrtc && helperPeerId) {
      void webrtc.createAndSendOffer();
      setStatus("Streaming camera to helper.");
    }
  };

  async function startAsUser(localPeerId: string, existingHelperPeerId?: string) {
    webrtc = new WebRtcSession({
      signaling,
      localPeerId,
      isInitiator: true,
      onRemoteStream: () => {},
      onConnectionState: (s) => {
        if (s === "connected") setStatus("Connected to helper.");
      },
    });
    signaling.setHandler((msg) => webrtc!.attachSignalingHandler(handleMsg)(msg));
    try {
      const stream = await webrtc.startLocalMedia(true, true);
      preview.srcObject = stream;
      if (existingHelperPeerId) {
        helperPeerId = existingHelperPeerId;
        webrtc.setRemotePeer(existingHelperPeerId);
        await webrtc.createAndSendOffer();
        setStatus("Streaming camera to helper.");
      }
    } catch {
      setStatus("Camera permission required.");
    }
  }

  try {
    await signaling.connect(handleMsg);
    signaling.join(roomId, "user", "User", userToken);
  } catch {
    setStatus("Cannot reach signaling server on port 8787.");
  }
});
