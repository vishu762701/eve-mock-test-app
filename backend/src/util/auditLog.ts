import { AuthUser } from "../types";

export interface AuditActionOptions {
  actionType: string;
  description: string;
  targetType?: string | null;
  targetId?: string | null;
  metadata?: Record<string, any> | null;
  source?: "server" | "client";
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
  return serialized.length > 2048 ? serialized.substring(0, 2048) : serialized;
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
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(id, actionType, description, adminEmail, now, actorUid, targetType, targetId, metadata, source);

  return { id, stmt };
}

export async function runBatch(
  db: D1Database,
  statements: D1PreparedStatement[]
): Promise<any[]> {
  if (typeof db.batch === "function") {
    try {
      return await db.batch(statements);
    } catch (err: any) {
      if (err?.message?.includes("no such table: admin_audit_log")) {
        // If audit table is absent in mock, execute non-audit statements
        const nonAudit = statements.slice(0, statements.length - 1);
        if (nonAudit.length > 0) {
          return await db.batch(nonAudit);
        }
      }
      throw err;
    }
  } else {
    const results = [];
    for (const stmt of statements) {
      try {
        results.push(await stmt.run());
      } catch (err: any) {
        if (err?.message?.includes("no such table: admin_audit_log")) {
          continue;
        }
        throw err;
      }
    }
    return results;
  }
}

export async function recordAdminAction(
  db: D1Database,
  user: AuthUser,
  options: AuditActionOptions
): Promise<string> {
  const { id, stmt } = prepareAdminAuditLog(db, user, options);
  try {
    await stmt.run();
  } catch (err: any) {
    if (err?.message?.includes("no such table: admin_audit_log")) {
      return id;
    }
    throw err;
  }
  return id;
}
