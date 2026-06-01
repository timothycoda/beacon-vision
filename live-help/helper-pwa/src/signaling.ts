import { signalingWsUrl } from "./config";

export type Role = "user" | "helper";

export interface PeerInfo {
  id: string;
  role: Role;
  displayName: string;
}

type MessageHandler = (msg: Record<string, unknown>) => void;

export class SignalingClient {
  private ws: WebSocket | null = null;
  private handler: MessageHandler = () => {};

  setHandler(onMessage: MessageHandler) {
    this.handler = onMessage;
  }

  connect(onMessage: MessageHandler): Promise<void> {
    this.setHandler(onMessage);
    return new Promise((resolve, reject) => {
      const ws = new WebSocket(signalingWsUrl());
      this.ws = ws;
      ws.onopen = () => resolve();
      ws.onerror = () => reject(new Error("websocket_failed"));
      ws.onmessage = (ev) => {
        try {
          const msg = JSON.parse(String(ev.data)) as Record<string, unknown>;
          this.handler(msg);
        } catch {
          /* ignore */
        }
      };
      ws.onclose = () => {
        this.handler({ type: "socket-closed" });
      };
    });
  }

  send(payload: Record<string, unknown>) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(payload));
    }
  }

  join(roomId: string, role: Role, displayName: string, token: string) {
    this.send({ type: "join", roomId, role, displayName, token });
  }

  close() {
    this.ws?.close();
    this.ws = null;
  }
}
