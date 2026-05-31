# Hardware QA checklist

Use this on a **physical phone** with **X01 / HeyCyan** glasses before release.
Run with `USE_FAKE_GLASSES=false` in `glasses/build.gradle.kts` for real hardware.

## Pairing and connection

- [ ] Scan lists the glasses; connect reaches **Connected / ready**
- [ ] Battery percentage updates on home chips
- [ ] Disconnect and reconnect without app restart
- [ ] Bluetooth off mid-session: spoken alert, no crash
- [ ] Unbind / forget device: app returns to pairable state

## Core guidance (glasses camera)

- [ ] **What is ahead** — capture, spoken summary, on-screen result
- [ ] **Read this** — OCR on a printed sign or screen
- [ ] **Walking mode** — periodic cues for 2+ minutes; stop cleanly
- [ ] **Voice command** — “what is ahead”, “read this”, “stop”
- [ ] **Emergency** — SMS flow (with test contact only)

## Model packs

- [ ] Download **Beacon Vision Plus** on Wi‑Fi; notification progress updates
- [ ] **Pause** mid-download; **Resume** continues from prior percent
- [ ] **Cancel** removes partial files; card returns to Download
- [ ] Kill app during download; reopen — pack shows **Paused**, resume works
- [ ] Set as default intelligence; home chip shows pack name
- [ ] **Hausa** toggle + voice pack: labels/speech in Hausa where supported

## Phone-only mode

- [ ] Home → **Use phone camera** — permission, preview, coloured boxes
- [ ] Auto-narration respects default intelligence / built-in ML Kit
- [ ] **Glasses home** returns to wearable home; glasses features unchanged

## Regression

- [ ] Low storage: download shows clear “not enough space” (no crash)
- [ ] Cellular with Wi‑Fi-only on: blocked with clear message
- [ ] No duplicate downloads when two packs queued (second waits or is ignored)

Record device model, Android version, glasses firmware, and any failures in the issue tracker.
