// ============================================================================
// Firebase ID Token Verification & Admin Authorization
// ============================================================================

import { AuthUser, Env } from "./types";

export const HARDCODED_ADMIN_EMAILS = new Set([
  "pronlike9@gmail.com",
  "own.keni@gmail.com",
  "anyqueairdrop@gmail.com",
  "ghatisarkar56@gmail.com",
]);

interface JwkKey {
  kty: string;
  alg: string;
  use: string;
  kid: string;
  n: string;
  e: string;
}

interface JwksResponse {
  keys: JwkKey[];
}

let cachedJwks: { keys: JwkKey[]; expiresAt: number } | null = null;

async function fetchGoogleJwks(): Promise<JwkKey[]> {
  const now = Date.now();
  if (cachedJwks && cachedJwks.expiresAt > now) {
    return cachedJwks.keys;
  }

  const res = await fetch(
    "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
  );
  if (!res.ok) {
    throw new Error(`Failed to fetch Google JWKS: ${res.statusText}`);
  }

  const cacheControl = res.headers.get("cache-control") || "";
  const maxAgeMatch = cacheControl.match(/max-age=(\d+)/);
  const maxAgeSeconds = maxAgeMatch ? parseInt(maxAgeMatch[1], 10) : 3600;

  const data = (await res.json()) as JwksResponse;
  cachedJwks = {
    keys: data.keys,
    expiresAt: now + maxAgeSeconds * 1000,
  };

  return data.keys;
}

function base64UrlDecode(str: string): Uint8Array {
  let base64 = str.replace(/-/g, "+").replace(/_/g, "/");
  while (base64.length % 4) {
    base64 += "=";
  }
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function base64UrlDecodeJson<T = any>(str: string): T {
  const bytes = base64UrlDecode(str);
  const decoded = new TextDecoder().decode(bytes);
  return JSON.parse(decoded);
}

export async function verifyFirebaseIdToken(token: string, projectId: string): Promise<AuthUser> {
  const parts = token.split(".");
  if (parts.length !== 3) {
    throw new Error("Invalid token format");
  }

  const [headerB64, payloadB64, signatureB64] = parts;
  const header = base64UrlDecodeJson<{ kid: string; alg: string }>(headerB64);
  if (header.alg !== "RS256") {
    throw new Error(`Unsupported algorithm: ${header.alg}`);
  }
  if (!header.kid) {
    throw new Error("Token header missing kid");
  }

  const jwks = await fetchGoogleJwks();
  const jwk = jwks.find((k) => k.kid === header.kid);
  if (!jwk) {
    throw new Error("Matching public key not found in Google JWKS");
  }

  // Import public key into Web Crypto API
  const cryptoKey = await crypto.subtle.importKey(
    "jwk",
    jwk,
    {
      name: "RSASSA-PKCS1-v1_5",
      hash: "SHA-256",
    },
    false,
    ["verify"]
  );

  const dataToVerify = new TextEncoder().encode(`${headerB64}.${payloadB64}`);
  const signature = base64UrlDecode(signatureB64);

  const isValid = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    signature,
    dataToVerify
  );

  if (!isValid) {
    throw new Error("Token signature verification failed");
  }

  const payload = base64UrlDecodeJson<{
    iss: string;
    aud: string;
    sub: string;
    exp: number;
    email?: string;
    name?: string;
  }>(payloadB64);

  const nowSeconds = Math.floor(Date.now() / 1000);
  if (payload.exp < nowSeconds) {
    throw new Error("Token expired");
  }

  if (payload.aud !== projectId) {
    throw new Error(`Invalid audience: expected ${projectId}, got ${payload.aud}`);
  }

  const expectedIssuer = `https://securetoken.google.com/${projectId}`;
  if (payload.iss !== expectedIssuer) {
    throw new Error(`Invalid issuer: expected ${expectedIssuer}, got ${payload.iss}`);
  }

  if (!payload.sub || typeof payload.sub !== "string") {
    throw new Error("Invalid subject in token");
  }

  const email = (payload.email || "").toLowerCase().trim();

  return {
    uid: payload.sub,
    email,
    displayName: payload.name || "Student",
    isAdmin: HARDCODED_ADMIN_EMAILS.has(email),
  };
}

export async function isUserAdmin(db: D1Database, email: string): Promise<boolean> {
  const cleanEmail = email.toLowerCase().trim();
  if (!cleanEmail) return false;
  if (HARDCODED_ADMIN_EMAILS.has(cleanEmail)) return true;

  try {
    const res = await db
      .prepare("SELECT 1 FROM admins WHERE email = ?")
      .bind(cleanEmail)
      .first();
    return res !== null && res !== undefined;
  } catch (_e) {
    return false;
  }
}
