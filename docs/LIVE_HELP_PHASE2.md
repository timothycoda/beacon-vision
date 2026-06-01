# Live Help — Phase 2 (in progress)

## Done (local)

- [x] WebSocket signaling server (`live-help/signaling-server`)
- [x] Helper PWA — webio layout, **sky blue** Elenii theme (`live-help/helper-pwa`)
- [x] HMAC join tokens, rate limits, no room join without token
- [x] Android `LiveHelpScreen` + WebRTC user publisher — [LIVE_HELP_ANDROID.md](LIVE_HELP_ANDROID.md)
- [x] Home + Phone entry, share helper invite intent
- [x] Dev user page (`user.html`) for browser-only tests
- [x] UI style guide — [LIVE_HELP_WEB_UI_STYLE.md](LIVE_HELP_WEB_UI_STYLE.md)

## Next

1. **VPS** — HTTPS/WSS, `SIGNALING_SECRET`, coturn TURN, fixed `CORS_ORIGIN`
2. **Navigation** — Home / Phone “Live Help” entry; trusted helper share link
3. **VPS** — deploy signaling + coturn via `ssh kali-lab`; env-based URLs in app + PWA
4. **Security** — short-lived room tokens, optional helper PIN

## Out of scope (Phase 2 local)

- Server-side recording
- Volunteer marketplace
- WhatsApp in-call camera control (remains Phase 1 handoff only)

## Related

- [BEACON_LIVE_HELP_WEBRTC_ROADMAP.md](BEACON_LIVE_HELP_WEBRTC_ROADMAP.md)
- [BEACON_WHATSAPP_HELPER_AND_EMERGENCY.md](BEACON_WHATSAPP_HELPER_AND_EMERGENCY.md)
- [live-help/README.md](../live-help/README.md)
