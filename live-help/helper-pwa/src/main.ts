import { signalingHttpBase } from "./config";
import { SignalingClient, type PeerInfo } from "./signaling";
import { WebRtcSession } from "./webrtc-session";

const app = document.getElementById("app")!;

function getJoinParamsFromUrl(): { room: string; token: string } {
  const params = new URLSearchParams(window.location.search);
  return {
    room: (params.get("room") ?? "").toUpperCase(),
    token: params.get("token") ?? "",
  };
}

function formatTimer(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  return [h, m, s].map((n) => String(n).padStart(2, "0")).join(":");
}

function renderJoin(onSubmit: (room: string, name: string) => void, initialRoom: string) {
  app.innerHTML = `
    <div class="join-screen">
      <div class="join-card">
        <p class="logo">elenii</p>
        <p class="tagline">Live Help — join as a trusted helper. Video stays between you and the user; nothing is recorded on our servers.</p>
        <form id="joinForm">
          <label for="roomInput">Session code</label>
          <input id="roomInput" maxlength="6" autocomplete="off" placeholder="ABC123" value="${initialRoom}" required />
          <label for="nameInput">Your name</label>
          <input id="nameInput" maxlength="40" placeholder="Helper name" value="Helper" required />
          <button type="submit" class="btn-primary">Join session</button>
          <p id="joinError" class="join-error hidden"></p>
        </form>
      </div>
    </div>
  `;
  const form = document.getElementById("joinForm") as HTMLFormElement;
  const err = document.getElementById("joinError")!;
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const room = (document.getElementById("roomInput") as HTMLInputElement).value.trim().toUpperCase();
    const name = (document.getElementById("nameInput") as HTMLInputElement).value.trim();
    if (!/^[A-Z0-9]{6}$/.test(room)) {
      err.textContent = "Enter a 6-character session code.";
      err.classList.remove("hidden");
      return;
    }
    err.classList.add("hidden");
    onSubmit(room, name || "Helper");
  });
}

function renderSession(roomId: string, helperName: string) {
  app.innerHTML = `
    <div class="session-shell">
      <div class="browser-frame">
        <header class="topbar">
          <span class="brand">elenii</span>
          <div class="session-meta">
            <p class="title">Live Help · Room ${roomId}</p>
            <p class="subtitle">Trusted helper session</p>
          </div>
          <div class="topbar-actions">
            <button type="button" class="icon-btn" id="btnCopyLink" title="Copy user dev link" aria-label="Copy link">⎘</button>
            <button type="button" class="btn-finish" id="btnLeave">End session</button>
          </div>
        </header>
        <div class="session-body">
          <section class="stage">
            <div class="video-wrap">
              <div class="placeholder" id="videoPlaceholder">Waiting for the user to share their camera…</div>
              <video id="remoteVideo" autoplay playsinline class="hidden"></video>
              <div class="live-badge hidden" id="liveBadge"><span class="dot"></span> LIVE</div>
              <span class="session-timer hidden" id="sessionTimer">00:00:00</span>
              <div class="pip-local hidden" id="pipLocal"><video id="localVideo" autoplay playsinline muted></video></div>
            </div>
            <nav class="controls-bar" aria-label="Call controls">
              <div class="control-item"><button type="button" id="btnCam" class="active" title="Camera">📷</button><span>Cam</span></div>
              <div class="control-item"><button type="button" id="btnMic" class="active" title="Microphone">🎤</button><span>Mic</span></div>
              <div class="control-item"><button type="button" id="btnShare" disabled title="Coming soon">↗</button><span>Share</span></div>
              <div class="control-item"><button type="button" id="btnRec" disabled title="Not recorded on server"><span class="rec-dot"></span></button><span>Rec</span></div>
            </nav>
          </section>
          <aside class="sidebar">
            <div class="sidebar-tabs">
              <button type="button" class="active" data-tab="chat">Chat</button>
              <button type="button" data-tab="scene">Scene</button>
            </div>
            <div class="sidebar-panel active" data-panel="chat">
              <div class="scene-card" id="quickScene">
                <h3>What the user sees</h3>
                <p class="empty" id="scenePreview">AI scene summary will appear when the app sends updates.</p>
              </div>
              <div class="chat-feed" id="chatFeed"></div>
              <form class="chat-input-row" id="chatForm">
                <input id="chatInput" placeholder="Write your message…" autocomplete="off" />
                <button type="submit" class="send">Send</button>
              </form>
            </div>
            <div class="sidebar-panel" data-panel="scene">
              <div class="scene-card" style="margin:16px">
                <h3>Scene summary</h3>
                <p class="empty" id="sceneFull">No scene updates yet.</p>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  `;

  document.querySelectorAll(".sidebar-tabs button").forEach((btn) => {
    btn.addEventListener("click", () => {
      const tab = (btn as HTMLButtonElement).dataset.tab!;
      document.querySelectorAll(".sidebar-tabs button").forEach((b) => b.classList.toggle("active", b === btn));
      document.querySelectorAll(".sidebar-panel").forEach((p) => {
        p.classList.toggle("active", (p as HTMLElement).dataset.panel === tab);
      });
    });
  });

  return {
    remoteVideo: document.getElementById("remoteVideo") as HTMLVideoElement,
    localVideo: document.getElementById("localVideo") as HTMLVideoElement,
    placeholder: document.getElementById("videoPlaceholder")!,
    liveBadge: document.getElementById("liveBadge")!,
    sessionTimer: document.getElementById("sessionTimer")!,
    pipLocal: document.getElementById("pipLocal")!,
    chatFeed: document.getElementById("chatFeed")!,
    chatForm: document.getElementById("chatForm") as HTMLFormElement,
    chatInput: document.getElementById("chatInput") as HTMLInputElement,
    scenePreview: document.getElementById("scenePreview")!,
    sceneFull: document.getElementById("sceneFull")!,
    btnCam: document.getElementById("btnCam")!,
    btnMic: document.getElementById("btnMic")!,
    btnLeave: document.getElementById("btnLeave")!,
    btnCopyLink: document.getElementById("btnCopyLink")!,
    helperName,
    roomId,
  };
}

function appendChat(
  feed: HTMLElement,
  opts: { name: string; text: string; mine?: boolean; system?: boolean },
) {
  const row = document.createElement("div");
  row.className = `chat-message ${opts.system ? "system" : opts.mine ? "mine" : "other"}`;
  const initial = opts.name.slice(0, 1).toUpperCase();
  row.innerHTML = `
    <div class="avatar" aria-hidden="true">${initial}</div>
    <div class="bubble-wrap">
      <div class="name">${opts.name}</div>
      <div class="bubble">${escapeHtml(opts.text)}</div>
    </div>
  `;
  feed.appendChild(row);
  feed.scrollTop = feed.scrollHeight;
}

function escapeHtml(s: string): string {
  const d = document.createElement("div");
  d.textContent = s;
  return d.innerHTML;
}

async function runSession(roomId: string, helperName: string, helperToken: string) {
  const ui = renderSession(roomId, helperName);
  let camOn = true;
  let micOn = true;
  let timerSec = 0;
  let timerId: number | undefined;

  const signaling = new SignalingClient();
  let webrtc: WebRtcSession | null = null;
  let userPeerId: string | null = null;

  const handleMsg = (msg: Record<string, unknown>) => {
    const type = String(msg.type ?? "");
    if (type === "error") {
      appendChat(ui.chatFeed, {
        name: "System",
        text: `Could not join: ${String(msg.message)}`,
        system: true,
      });
      return;
    }
    if (type === "joined") {
      const localPeerId = String(msg.peerId);
      const peers = (msg.peers ?? []) as PeerInfo[];
      const existingUser = peers.find((p) => p.role === "user");
      if (existingUser) userPeerId = existingUser.id;
      void setupWebRtc(localPeerId, existingUser?.id);
      appendChat(ui.chatFeed, {
        name: "System",
        text: existingUser ? `${existingUser.displayName} is in the room.` : "Waiting for the user…",
        system: true,
      });
      return;
    }
    if (type === "peer-joined") {
      const peer = msg.peer as PeerInfo;
      if (peer.role === "user") {
        userPeerId = peer.id;
        webrtc?.setRemotePeer(peer.id);
        appendChat(ui.chatFeed, {
          name: "System",
          text: `${peer.displayName} joined.`,
          system: true,
        });
      }
      return;
    }
    if (type === "chat") {
      appendChat(ui.chatFeed, { name: "User", text: String(msg.text ?? "") });
      return;
    }
    if (type === "scene") {
      const summary = String(msg.summary ?? "");
      ui.scenePreview.textContent = summary;
      ui.scenePreview.classList.remove("empty");
      ui.sceneFull.textContent = summary;
      ui.sceneFull.classList.remove("empty");
      return;
    }
    if (type === "socket-closed") {
      appendChat(ui.chatFeed, { name: "System", text: "Disconnected.", system: true });
    }
  };

  async function setupWebRtc(localPeerId: string, existingUserPeerId?: string) {
    webrtc = new WebRtcSession({
      signaling,
      localPeerId,
      isInitiator: false,
      onRemoteStream: (stream) => {
        ui.remoteVideo.srcObject = stream;
        ui.remoteVideo.classList.remove("hidden");
        ui.placeholder.classList.add("hidden");
        ui.liveBadge.classList.remove("hidden");
        ui.sessionTimer.classList.remove("hidden");
        if (!timerId) {
          timerId = window.setInterval(() => {
            timerSec += 1;
            ui.sessionTimer.textContent = formatTimer(timerSec);
          }, 1000);
        }
      },
      onConnectionState: (state) => {
        if (state === "failed" || state === "disconnected") {
          ui.placeholder.textContent = "Connection lost. Ask the user to rejoin.";
          ui.placeholder.classList.remove("hidden");
        }
      },
    });
    signaling.setHandler((msg) => webrtc!.attachSignalingHandler(handleMsg)(msg));
    if (existingUserPeerId) webrtc.setRemotePeer(existingUserPeerId);
    try {
      const local = await webrtc.startLocalMedia(true, true);
      ui.localVideo.srcObject = local;
      ui.pipLocal.classList.remove("hidden");
    } catch {
      appendChat(ui.chatFeed, {
        name: "System",
        text: "Camera/mic permission denied.",
        system: true,
      });
    }
  }

  try {
    await signaling.connect(handleMsg);
    signaling.join(roomId, "helper", helperName, helperToken);
  } catch {
    appendChat(ui.chatFeed, {
      name: "System",
      text: "Cannot reach signaling (port 8787). Run: cd live-help/signaling-server && npm run start",
      system: true,
    });
  }

  ui.btnCam.addEventListener("click", () => {
    camOn = !camOn;
    webrtc?.toggleTrack("video", camOn);
    ui.btnCam.classList.toggle("active", camOn);
    ui.btnCam.classList.toggle("off", !camOn);
  });
  ui.btnMic.addEventListener("click", () => {
    micOn = !micOn;
    webrtc?.toggleTrack("audio", micOn);
    ui.btnMic.classList.toggle("active", micOn);
    ui.btnMic.classList.toggle("off", !micOn);
  });
  ui.chatForm.addEventListener("submit", (e) => {
    e.preventDefault();
    const text = ui.chatInput.value.trim();
    if (!text) return;
    signaling.send({ type: "chat", text });
    appendChat(ui.chatFeed, { name: helperName, text, mine: true });
    ui.chatInput.value = "";
  });
  ui.btnCopyLink.addEventListener("click", () => {
    const userUrl = `${window.location.origin}/user.html?room=${ui.roomId}`;
    void navigator.clipboard.writeText(userUrl);
    appendChat(ui.chatFeed, { name: "System", text: "User test link copied.", system: true });
  });
  ui.btnLeave.addEventListener("click", () => {
    if (timerId) clearInterval(timerId);
    webrtc?.close();
    signaling.close();
    window.location.href = window.location.pathname;
  });
}

const { room: urlRoom, token: urlToken } = getJoinParamsFromUrl();
if (urlRoom && /^[A-Z0-9]{6}$/.test(urlRoom) && urlToken) {
  void runSession(urlRoom, "Helper", urlToken);
} else {
  renderJoin(
    (room, name, token) => {
      const u = new URL(window.location.href);
      u.searchParams.set("room", room);
      u.searchParams.set("token", token);
      window.history.replaceState({}, "", u);
      void runSession(room, name, token);
    },
    urlRoom,
    urlToken,
  );
}
