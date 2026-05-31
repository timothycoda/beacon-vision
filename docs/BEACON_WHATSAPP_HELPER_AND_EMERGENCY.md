# Beacon WhatsApp helper and emergency sharing

## Scope (Phase 1)

WhatsApp is a **handoff channel only**. Beacon:

- Opens `wa.me` or Android share intents with prefilled text
- Can attach a compressed emergency JPEG via `FileProvider`
- Pauses phone-mode camera analysis during **Call helper on WhatsApp** handoff
- Never silently sends messages or images
- Never controls WhatsApp’s in-call camera

Not in scope: WebRTC live help, volunteer backend, WhatsApp UI automation, unofficial APIs.

## Phone mode — helper

1. **Call helper on WhatsApp** — pauses CameraX analysis, opens WhatsApp chat with helper message, resumes on return if phone mode was active.
2. **Message helper** — opens chat without pausing camera (unless user starts a WhatsApp call).

## Emergency — WhatsApp

- **Send emergency alert to WhatsApp** — optional 3-second cancelable countdown, then share sheet / `wa.me` with text, optional image and map link.
- SMS and dialer flows unchanged.

## Trusted helpers

Settings → **Trusted helpers**. Migrates legacy single trusted contact from Emergency prefs. Fields: name, phone, WhatsApp toggles, primary helper / emergency flags.

## Privacy

User must review and tap Send in WhatsApp or the share sheet. Temporary images live under `cache/emergency_images/` and are cleared on next compress.

## Future

See [BEACON_LIVE_HELP_WEBRTC_ROADMAP.md](BEACON_LIVE_HELP_WEBRTC_ROADMAP.md).
