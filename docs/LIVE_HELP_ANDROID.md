# Live Help — Android + web

## Flow

1. User opens **Live Help** (Home or Phone mode).
2. App creates a secure room on the signaling server and joins as `user`.
3. User taps **Share secure helper link** — sends URL with `room` + **helper-only** token (30 min).
4. Helper opens the link in a browser (sky-blue PWA), joins with token.
5. WebRTC video is **peer-to-peer** (640×480 @ 24fps). Signaling only relays SDP/ICE/chat/scene text.

## Dev setup (phone + Mac)

1. **Mac IP** — add to `local.properties` (git-ignored):

   ```properties
   liveHelp.host=192.168.x.x
   ```

   Default without this is `10.0.2.2` (emulator only).

2. **Start servers** (same Wi‑Fi as phone):

   ```bash
   cd live-help/signaling-server && npm run start
   cd live-help/helper-pwa && npm run dev -- --host 0.0.0.0
   ```

3. **Rebuild & install** Elenii debug after changing `liveHelp.host`.

4. On phone: Live Help → share link → open on laptop browser (or helper’s phone).

## Security (local stack)

- HMAC join tokens per role (`user` / `helper`); rooms are not joinable without token.
- Rate limit on join attempts; max message size 16 KB.
- No media stored on signaling server.
- Production: set `SIGNALING_SECRET`, HTTPS/WSS, TURN (coturn), restrict `CORS_ORIGIN`.

## Speed

- WebRTC: `max-bundle`, ICE pool, hardware encode/decode.
- VP8/H264 via device codecs; 640×480 capture to limit uplink on mobile networks.
