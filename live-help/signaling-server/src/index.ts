import cors from "cors";
import express from "express";
import { createServer } from "node:http";
import { randomBytes } from "node:crypto";
import { WebSocketServer, type WebSocket } from "ws";
import { issueJoinToken, verifyJoinToken, type Role } from "./tokens.js";

const PORT = Number(process.env.PORT ?? 8787);
const HOST = process.env.HOST ?? "0.0.0.0";
const MAX_ROOM_PEERS = 2;
const MAX_MESSAGE_BYTES = 16_384;
const ROOM_ID_PATTERN = /^[A-Z0-9]{6}$/;
const CORS_ORIGIN = process.env.CORS_ORIGIN ?? "*";

interface Peer {
  id: string;
  role: Role;
  displayName: string;
  ws: WebSocket;
}

interface Room {
  id: string;
  peers: Map<string, Peer>;
  createdAt: number;
}

const rooms = new Map<string, Room>();
const joinAttempts = new Map<string, { count: number; resetAt: number }>();

function generateRoomId(): string {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = randomBytes(6);
  return Array.from(bytes, (b) => alphabet[b % alphabet.length]).join("");
}

function send(ws: WebSocket, payload: unknown) {
  if (ws.readyState === ws.OPEN) {
    ws.send(JSON.stringify(payload));
  }
}

function peerList(room: Room) {
  return Array.from(room.peers.values()).map((p) => ({
    id: p.id,
    role: p.role,
    displayName: p.displayName,
  }));
}

function otherPeer(room: Room, peerId: string): Peer | undefined {
  for (const [id, peer] of room.peers) {
    if (id !== peerId) return peer;
  }
  return undefined;
}

function removePeer(roomId: string, peerId: string) {
  const room = rooms.get(roomId);
  if (!room) return;
  room.peers.delete(peerId);
  const remaining = peerList(room);
  for (const peer of room.peers.values()) {
    send(peer.ws, { type: "peer-left", peerId, peers: remaining });
  }
  if (room.peers.size === 0) {
    rooms.delete(roomId);
  }
}

function rateLimitJoin(key: string): boolean {
  const now = Date.now();
  const entry = joinAttempts.get(key);
  if (!entry || now > entry.resetAt) {
    joinAttempts.set(key, { count: 1, resetAt: now + 60_000 });
    return true;
  }
  if (entry.count >= 20) return false;
  entry.count += 1;
  return true;
}

const app = express();
app.use(
  cors({
    origin: CORS_ORIGIN === "*" ? true : CORS_ORIGIN.split(","),
    methods: ["GET", "POST", "OPTIONS"],
  }),
);
app.use(express.json({ limit: "8kb" }));

app.get("/health", (_req, res) => {
  res.json({ ok: true, rooms: rooms.size });
});

app.post("/rooms", (_req, res) => {
  let id = generateRoomId();
  while (rooms.has(id)) {
    id = generateRoomId();
  }
  rooms.set(id, { id, peers: new Map(), createdAt: Date.now() });
  const user = issueJoinToken(id, "user");
  const helper = issueJoinToken(id, "helper");
  res.json({
    roomId: id,
    userToken: user.token,
    helperToken: helper.token,
    expiresAt: user.expiresAt,
  });
});

app.get("/rooms/:roomId", (req, res) => {
  const roomId = String(req.params.roomId).toUpperCase();
  const room = rooms.get(roomId);
  if (!room) {
    res.status(404).json({ error: "not_found" });
    return;
  }
  res.json({
    roomId,
    peerCount: room.peers.size,
    peers: peerList(room),
  });
});

const httpServer = createServer(app);
const wss = new WebSocketServer({ server: httpServer, path: "/ws", maxPayload: MAX_MESSAGE_BYTES });

wss.on("connection", (ws) => {
  let joinedRoomId: string | null = null;
  let joinedPeerId: string | null = null;

  ws.on("message", (raw) => {
    if (Buffer.byteLength(raw) > MAX_MESSAGE_BYTES) {
      send(ws, { type: "error", message: "message_too_large" });
      ws.close();
      return;
    }

    let msg: Record<string, unknown>;
    try {
      msg = JSON.parse(String(raw)) as Record<string, unknown>;
    } catch {
      send(ws, { type: "error", message: "invalid_json" });
      return;
    }

    const type = String(msg.type ?? "");

    if (type === "join") {
      const roomId = String(msg.roomId ?? "").toUpperCase();
      const role = msg.role as Role;
      const token = String(msg.token ?? "");
      const displayName =
        String(msg.displayName ?? "").trim() || (role === "helper" ? "Helper" : "User");

      if (!rateLimitJoin(`${roomId}:${role}`)) {
        send(ws, { type: "error", message: "rate_limited" });
        return;
      }
      if (!ROOM_ID_PATTERN.test(roomId)) {
        send(ws, { type: "error", message: "invalid_room_id" });
        return;
      }
      if (role !== "user" && role !== "helper") {
        send(ws, { type: "error", message: "invalid_role" });
        return;
      }
      if (!verifyJoinToken(roomId, role, token)) {
        send(ws, { type: "error", message: "invalid_token" });
        return;
      }

      const room = rooms.get(roomId);
      if (!room) {
        send(ws, { type: "error", message: "room_not_found" });
        return;
      }

      const roleTaken = Array.from(room.peers.values()).some((p) => p.role === role);
      if (roleTaken) {
        send(ws, { type: "error", message: "role_already_joined" });
        return;
      }
      if (room.peers.size >= MAX_ROOM_PEERS) {
        send(ws, { type: "error", message: "room_full" });
        return;
      }

      const peerId = randomBytes(8).toString("hex");
      const peer: Peer = { id: peerId, role, displayName, ws };
      room.peers.set(peerId, peer);
      joinedRoomId = roomId;
      joinedPeerId = peerId;

      const peers = peerList(room);
      send(ws, { type: "joined", peerId, roomId, role, peers });

      const partner = otherPeer(room, peerId);
      if (partner) {
        send(partner.ws, { type: "peer-joined", peer: { id: peerId, role, displayName } });
        send(ws, {
          type: "peer-joined",
          peer: { id: partner.id, role: partner.role, displayName: partner.displayName },
        });
        send(ws, { type: "ready", initiator: role === "user" });
        send(partner.ws, { type: "ready", initiator: partner.role === "user" });
      }
      return;
    }

    if (!joinedRoomId || !joinedPeerId) {
      send(ws, { type: "error", message: "not_joined" });
      return;
    }

    const room = rooms.get(joinedRoomId);
    if (!room) {
      send(ws, { type: "error", message: "room_gone" });
      return;
    }

    if (type === "chat" || type === "scene") {
      const text = String(msg.text ?? msg.summary ?? "").trim().slice(0, 2_000);
      if (!text) return;
      for (const peer of room.peers.values()) {
        if (peer.id === joinedPeerId) continue;
        send(peer.ws, {
          type,
          fromPeerId: joinedPeerId,
          fromRole: room.peers.get(joinedPeerId)?.role,
          text: type === "scene" ? undefined : text,
          summary: type === "scene" ? text : undefined,
        });
      }
      return;
    }

    if (type === "offer" || type === "answer" || type === "ice") {
      const targetPeerId = String(msg.targetPeerId ?? "");
      const target = room.peers.get(targetPeerId) ?? otherPeer(room, joinedPeerId);
      if (!target) {
        send(ws, { type: "error", message: "no_peer" });
        return;
      }
      send(target.ws, { ...msg, fromPeerId: joinedPeerId });
      return;
    }

    send(ws, { type: "error", message: "unknown_type" });
  });

  ws.on("close", () => {
    if (joinedRoomId && joinedPeerId) {
      removePeer(joinedRoomId, joinedPeerId);
    }
  });
});

httpServer.listen(PORT, HOST, () => {
  console.log(`Live Help signaling on http://${HOST}:${PORT} (ws path /ws)`);
});
