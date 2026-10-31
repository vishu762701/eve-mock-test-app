import { AuthUser } from "../types";

export interface AuditActionOptions {
  actionType: string;
  description: string;
  targetType?: string | null;
  targetId?: string | null;
  metadata?: Record<string, any> | null;
  source?: "server" | "client";
  // Internal callers supply constant SQL only; all resource values stay bound.
  condition?: { sql: string; bindings: (string | number | null)[] };
}

const FORBIDDEN_METADATA_KEY_REGEX = /(token|secret|password|auth|answer|option|questiontext|credential|phone|username|api_?key|secret_?key)/i;

export function sanitizeMetadata(metadata?: Record<string, any> | null): string | null {
  if (!metadata || typeof metadata !== "object") return null;
  const safeObj: Record<string, any> = {};

  for (const [k, v] of Object.entries(metadata)) {
    if (FORBIDDEN_METADATA_KEY_REGEX.test(k)) continue;
    if (v === undefined) continue;

    if (typeof v === "string") {
      safeObj[k] = v.length > 200 ? v.substring(0, 200) + "..." : v;
    } else if (typeof v === "number" || typeof v === "boolean" || v === null) {
      safeObj[k] = v;
    } else if (Array.isArray(v)) {
      safeObj[k] = v.slice(0, 50).map((item) => {
        if (typeof item === "string") return item.length > 100 ? item.substring(0, 100) + "..." : item;
        if (typeof item === "number" || typeof item === "boolean") return item;
        return null;
      });
    } else if (typeof v === "object") {
      const nested: Record<string, any> = {};
      for (const [nk, nv] of Object.entries(v)) {
        if (FORBIDDEN_METADATA_KEY_REGEX.test(nk)) continue;
        if (typeof nv === "string") nested[nk] = nv.length > 100 ? nv.substring(0, 100) + "..." : nv;
        else if (typeof nv === "number" || typeof nv === "boolean") nested[nk] = nv;
      }
      safeObj[k] = nested;
    }
  }

  const serialized = JSON.stringify(safeObj);
  if (serialized.length <= 2048) return serialized;

  // Truncating serialized JSON corrupts Recent Actions metadata. Keep only
  // complete, sanitized entries that fit the bound.
  const bounded: Record<string, any> = {};
  for (const [key, value] of Object.entries(safeObj)) {
    const candidate = JSON.stringify({ ...bounded, [key]: value });
    if (candidate.length <= 2048) bounded[key] = value;
  }
  return JSON.stringify(bounded);
}

export function prepareAdminAuditLog(
  db: D1Database,
  user: AuthUser,
  options: AuditActionOptions
): { id: string; stmt: D1PreparedStatement } {
  const id = crypto.randomUUID();
  const actionType = String(options.actionType || "UNKNOWN_ACTION").trim().substring(0, 64);
  const description = String(options.description || "").trim().substring(0, 500);
  const adminEmail = String(user?.email || "unknown_admin@eve.app").trim().toLowerCase().substring(0, 254);
  const actorUid = user?.uid ? String(user.uid).trim().substring(0, 128) : null;
  const targetType = options.targetType ? String(options.targetType).trim().substring(0, 64) : null;
  const targetId = options.targetId ? String(options.targetId).trim().substring(0, 128) : null;
  const metadata = sanitizeMetadata(options.metadata);
  const source = options.source === "client" ? "client" : "server";
  const now = Date.now();

  const stmt = db
    .prepare(
      `INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp, actor_uid, target_type, target_id, metadata, source)
       SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?, ?${options.condition ? ` WHERE ${options.condition.sql}` : ""}`
    )
    .bind(id, actionType, description, adminEmail, now, actorUid, targetType, targetId, metadata, source,
      ...(options.condition?.bindings || []));

  return { id, stmt };
}

/**
 * Administrative mutations and their audit records must commit atomically.
 * A missing audit migration is an operational failure, never permission to
 * execute privileged changes without a Recent Actions record.
 */
export async function runBatch(
  db: D1Database,
  statements: D1PreparedStatement[]
): Promise<any[]> {
  if (typeof db.batch !== "function") {
    throw new Error("Atomic D1 batch is required for audited admin changes.");
  }
  return db.batch(statements);
}

export async function recordAdminAction(
  db: D1Database,
  user: AuthUser,
  options: AuditActionOptions
): Promise<string> {
  const { id, stmt } = prepareAdminAuditLog(db, user, options);
  await stmt.run();
  return id;
}
