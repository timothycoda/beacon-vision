/** Signaling base URL. Vite dev proxies /api and /ws; production uses same host or env. */
export function signalingHttpBase(): string {
  const env = import.meta.env.VITE_SIGNALING_HTTP;
  if (env) return String(env).replace(/\/$/, "");
  if (import.meta.env.DEV) return "/api";
  return `${window.location.protocol}//${window.location.hostname}:8787`;
}

export function signalingWsUrl(): string {
  const env = import.meta.env.VITE_SIGNALING_WS;
  if (env) return String(env);
  if (import.meta.env.DEV) {
    const proto = window.location.protocol === "https:" ? "wss:" : "ws:";
    return `${proto}//${window.location.host}/ws`;
  }
  const proto = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${proto}//${window.location.hostname}:8787/ws`;
}

export const ICE_SERVERS: RTCIceServer[] = [
  { urls: "stun:stun.l.google.com:19302" },
  { urls: "stun:stun1.l.google.com:19302" },
];
