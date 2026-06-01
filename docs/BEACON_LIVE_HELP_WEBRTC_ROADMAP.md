# Beacon Live Help — WebRTC roadmap

WhatsApp helper/emergency sharing remains **handoff only** (see [BEACON_WHATSAPP_HELPER_AND_EMERGENCY.md](BEACON_WHATSAPP_HELPER_AND_EMERGENCY.md)).

## Phase 2 status

| Step | Status |
|------|--------|
| Local signaling + helper PWA | **Done** — see [live-help/README.md](../live-help/README.md) |
| Android WebRTC publisher | Planned |
| VPS (signaling + TURN) | Planned |

## Goals

- WebRTC session with a trusted helper
- User controls camera (phone CameraX; glasses when SDK allows)
- AI scene summaries alongside live video (signaling `scene` messages)
- Helper joins from browser — [LIVE_HELP_WEB_UI_STYLE.md](LIVE_HELP_WEB_UI_STYLE.md)
- Audio, location (with consent), emergency escalation — later

## Privacy

- No A/V recording on signaling server in Phase 2
- User must start/share explicitly; helper link is session-scoped
