# Live Help — VPS (`elenii.zeustek.com.ng`)

Production URLs (Android **release** and PWA **production build**):

| Service | URL |
|---------|-----|
| Signaling HTTP | `https://elenii.zeustek.com.ng` |
| WebSocket | `wss://elenii.zeustek.com.ng/ws` |
| Helper PWA | `https://elenii.zeustek.com.ng` |

Invite links from the app look like:

`https://elenii.zeustek.com.ng/?room=ABC123&token=…`

## Server env (signaling)

```bash
export SIGNALING_SECRET="<long-random-secret>"
export CORS_ORIGIN="https://elenii.zeustek.com.ng"
export PORT=8787
export HOST=127.0.0.1
```

Run signaling behind nginx TLS termination; proxy `/ws` to the Node process with WebSocket upgrade headers.

## nginx sketch

```nginx
server {
    listen 443 ssl http2;
    server_name elenii.zeustek.com.ng;

    # Helper PWA static files → live-help/helper-pwa/dist
    root /var/www/elenii-live-help;
    index index.html;
    try_files $uri $uri/ /index.html;

    location /ws {
        proxy_pass http://127.0.0.1:8787;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }

    location /rooms {
        proxy_pass http://127.0.0.1:8787;
        proxy_set_header Host $host;
    }

    location /health {
        proxy_pass http://127.0.0.1:8787;
    }
}
```

Deploy PWA: `cd live-help/helper-pwa && npm run build` → copy `dist/` to `/var/www/elenii-live-help`.

## coturn (recommended for helpers on strict NAT)

Run coturn on the VPS; add TURN URLs to Android `LiveHelpWebRtcPublisher` and `live-help/helper-pwa/src/config.ts` when credentials are ready.

## Local Mac testing (debug APK)

In `local.properties`:

```properties
liveHelp.host=192.168.0.180
```

Or point debug at production:

```properties
liveHelp.useProduction=true
```
