import { createHmac, randomBytes, timingSafeEqual } from "node:crypto";

export type Role = "user" | "helper";

const TOKEN_TTL_MS = Number(process.env.TOKEN_TTL_MS ?? 30 * 60 * 1000);

/** Set SIGNALING_SECRET in production (32+ random bytes). */
export const SIGNALING_SECRET =
  process.env.SIGNALING_SECRET ?? randomBytes(32).toString("hex");

if (!process.env.SIGNALING_SECRET) {
  console.warn(
    "SIGNALING_SECRET not set — using ephemeral secret (tokens invalid after restart).",
  );
}

function signPayload(payload: string): string {
  return createHmac("sha256", SIGNALING_SECRET).update(payload).digest("base64url");
}

export function issueJoinToken(roomId: string, role: Role): { token: string; expiresAt: number } {
  const expiresAt = Date.now() + TOKEN_TTL_MS;
  const payload = `${roomId}:${role}:${expiresAt}`;
  const token = `${expiresAt}.${signPayload(payload)}`;
  return { token, expiresAt };
}

export function verifyJoinToken(roomId: string, role: Role, token: string): boolean {
  const dot = token.indexOf(".");
  if (dot <= 0) return false;
  const expiresAt = Number(token.slice(0, dot));
  const sig = token.slice(dot + 1);
  if (!Number.isFinite(expiresAt) || Date.now() > expiresAt) return false;
  const expected = signPayload(`${roomId}:${role}:${expiresAt}`);
  try {
    const a = Buffer.from(sig);
    const b = Buffer.from(expected);
    return a.length === b.length && timingSafeEqual(a, b);
  } catch {
    return false;
  }
}
