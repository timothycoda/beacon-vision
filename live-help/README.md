# Elenii Live Help (Phase 2 — local)

WebRTC **signaling server** + **helper PWA** for trusted live video. Matches the [webio-style UI guide](../docs/LIVE_HELP_WEB_UI_STYLE.md). Android integration comes next.

## Quick start

Terminal 1 — signaling (port **8787**):

```bash
cd live-help/signaling-server
npm install
npm run start
```

Terminal 2 — helper UI (port **5174**):

```bash
cd live-help/helper-pwa
npm install
npm run dev
```

1. Open http://localhost:5174 — a **session code** is prefilled (or create one via API).
2. Join as **helper** (allow camera/mic for your preview).
3. In another browser/tab open http://localhost:5174/user.html?room=**CODE** — simulates the Android user camera until the app is wired.

## LAN / phone testing

Use your machine’s LAN IP instead of `localhost` on both URLs. Ensure the phone can reach ports **8787** and **5174** (firewall).

```bash
# Example
export VITE_SIGNALING_HTTP=http://192.168.1.10:8787
export VITE_SIGNALING_WS=ws://192.168.1.10:8787/ws
cd live-help/helper-pwa && npm run dev -- --host
```

## Architecture

| Piece | Role |
|-------|------|
| `signaling-server` | Rooms, WebSocket relay for SDP/ICE, chat & scene text |
| `helper-pwa` | Helper browser UI (video + chat + scene panel) |
| `user.html` | Dev-only user publisher (replaced by Android) |

## Production (VPS)

- Host: **https://elenii.zeustek.com.ng** — invite links and release APK use this domain.
- See [docs/LIVE_HELP_VPS.md](../docs/LIVE_HELP_VPS.md) for nginx + env vars.

**Privacy:** No recording or media storage on the signaling server. Add coturn on the VPS for NAT traversal.

## Docs

- [LIVE_HELP_PHASE2.md](../docs/LIVE_HELP_PHASE2.md) — phase scope & Android next steps
- [LIVE_HELP_WEB_UI_STYLE.md](../docs/LIVE_HELP_WEB_UI_STYLE.md) — colors, layout, components
