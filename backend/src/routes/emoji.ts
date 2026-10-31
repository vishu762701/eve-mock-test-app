import { cleanupEmojiMedia } from "../services/mediaCleanup";
// ============================================================================
// Animated Emoji System Routes (Mission 03 F4)
// Telegram packs, Google Noto animated emoji, custom uploads, and surface placements.
// ============================================================================

import { Hono } from "hono";
import { requirePermission } from "../middleware/authMiddleware";
import { AuthUser, EmojiItemRow, EmojiPackRow, EmojiPlacementPublic, EmojiPlacementRow, Env } from "../types";
import { SupabaseStorage } from "../supabase";
import { prepareAdminAuditLog, recordAdminAction } from "../util/auditLog";

export const emojiRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();
export const emojiAdminRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

const MAX_ANIMATION_SIZE_BYTES = 300 * 1024; // 300 KB size cap

async function saveImportedItem(db: D1Database, env: Env, user: AuthUser,
  itemId: string, storageKey: string, statement: D1PreparedStatement): Promise<void> {
  const old = await db.prepare("SELECT storage_key FROM emoji_items WHERE id = ?").bind(itemId).first<{ storage_key: string }>();
  const { stmt: audit } = prepareAdminAuditLog(db, user, {
    actionType: "IMPORT_EMOJI_ITEM", description: "Imported emoji asset", targetType: "emoji_item", targetId: itemId,
  });
  try { await db.batch([statement, audit]); }
  catch (error) { await cleanupEmojiMedia(env, [storageKey]); throw error; }
  if (old && old.storage_key !== storageKey) await cleanupEmojiMedia(env, [old.storage_key]);
}

export async function sha256Hex(data: ArrayBuffer | Uint8Array): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", data);
  const bytes = new Uint8Array(digest);
  let hex = "";
  for (let i = 0; i < bytes.length; i++) {
    hex += bytes[i].toString(16).padStart(2, "0");
  }
  return hex;
}

export async function decompressGzipWithGuard(
  compressedBytes: Uint8Array,
  maxBytes: number = MAX_ANIMATION_SIZE_BYTES
): Promise<string> {
  const ds = new DecompressionStream("gzip");
  const writer = ds.writable.getWriter();
  writer.write(compressedBytes);
  writer.close();

  const reader = ds.readable.getReader();
  const chunks: Uint8Array[] = [];
  let totalBytes = 0;

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    if (value) {
      totalBytes += value.length;
      if (totalBytes > maxBytes) {
        throw new Error(`Decompressed animation exceeds maximum allowed size of ${maxBytes} bytes`);
      }
      chunks.push(value);
    }
  }

  const result = new Uint8Array(totalBytes);
  let offset = 0;
  for (const chunk of chunks) {
    result.set(chunk, offset);
    offset += chunk.length;
  }
  return new TextDecoder().decode(result);
}

export function validateLottieJson(jsonStr: string): any {
  let parsed: any;
  try {
    parsed = JSON.parse(jsonStr);
  } catch (_e) {
    throw new Error("Invalid JSON: failed to parse animation file");
  }
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    throw new Error("Invalid Lottie: root must be an object");
  }
  if (!("layers" in parsed) && !("v" in parsed)) {
    throw new Error("Invalid Lottie schema: missing layers or version property");
  }
  if (Array.isArray(parsed.assets)) {
    for (const asset of parsed.assets) {
      if (!asset || typeof asset !== "object") throw new Error("Invalid Lottie asset.");
      for (const value of [asset.u, asset.p]) {
        if (typeof value === "string" && !value.startsWith("data:") &&
            (/^[a-z][a-z0-9+.-]*:/i.test(value) || value.startsWith("//"))) {
          throw new Error("Security check failed: external network references in Lottie assets are prohibited");
        }
      }
    }
  }
  return parsed;
}

export async function getActiveEmojiPlacements(
  db: D1Database,
  env: Env
): Promise<{
  examPlacements: Map<string, EmojiPlacementPublic>;
  categoryPlacements: Record<string, EmojiPlacementPublic>;
  broadcastPlacements: Map<string, EmojiPlacementPublic>;
}> {
  let globalTelegramEnabled = true;
  try {
    const row = await db
      .prepare("SELECT body FROM app_content WHERE id = 'telegram_emoji_global'")
      .first<{ body: string }>();
    if (row && row.body) {
      const cfg = JSON.parse(row.body);
      if (cfg.enabled === false) {
        globalTelegramEnabled = false;
      }
    }
  } catch {}

  const storage = new SupabaseStorage(env);

  const query = `
    SELECT 
      ep.slot_type, ep.target_id,
      ei.id as item_id, ei.alt_emoji, ei.kind, ei.storage_key, ei.source as item_source,
      p.enabled as pack_enabled, ei.enabled as item_enabled
    FROM emoji_placements ep
    JOIN emoji_items ei ON ep.item_id = ei.id
    JOIN emoji_packs p ON ei.pack_id = p.id
    WHERE ei.enabled = 1 AND p.enabled = 1
  `;
  const { results } = await db.prepare(query).all<any>();

  const examPlacements = new Map<string, EmojiPlacementPublic>();
  const categoryPlacements: Record<string, EmojiPlacementPublic> = {};
  const broadcastPlacements = new Map<string, EmojiPlacementPublic>();

  for (const r of results || []) {
    if (!globalTelegramEnabled && (r.item_source === "telegram" || r.item_source === "telegram-ui")) {
      continue;
    }
    const url = r.storage_key.startsWith("res:raw/")
      ? r.storage_key
      : storage.getPublicUrl(r.storage_key);

    const publicItem: EmojiPlacementPublic = {
      id: r.item_id,
      url,
      kind: r.kind,
      alt: r.alt_emoji,
    };

    if (r.slot_type === "exam_card") {
      examPlacements.set(r.target_id, publicItem);
    } else if (r.slot_type === "category_header") {
      categoryPlacements[r.target_id] = publicItem;
    } else if (r.slot_type === "broadcast") {
      broadcastPlacements.set(r.target_id, publicItem);
    }
  }

  return { examPlacements, categoryPlacements, broadcastPlacements };
}

// ============================================================================
// Public Emoji Endpoints
// ============================================================================

emojiRoutes.get("/placements/public", async (c) => {
  const db = c.env.DB;
  const { examPlacements, categoryPlacements, broadcastPlacements } = await getActiveEmojiPlacements(db, c.env);

  const examMap: Record<string, EmojiPlacementPublic> = {};
  examPlacements.forEach((v, k) => {
    examMap[k] = v;
  });

  const broadcastMap: Record<string, EmojiPlacementPublic> = {};
  broadcastPlacements.forEach((v, k) => {
    broadcastMap[k] = v;
  });

  return c.json({
    success: true,
    data: {
      examPlacements: examMap,
      categoryPlacements,
      broadcastPlacements: broadcastMap,
    },
  });
});

// ============================================================================
// Protected Admin Emoji Endpoints (requirePermission("manage_content"))
// ============================================================================

emojiAdminRoutes.use("/*", requirePermission("manage_content"));

// GET /api/admin/emoji/packs - List all emoji packs with item counts
emojiAdminRoutes.get("/packs", async (c) => {
  const db = c.env.DB;

  let globalTelegramEnabled = true;
  try {
    const row = await db
      .prepare("SELECT body FROM app_content WHERE id = 'telegram_emoji_global'")
      .first<{ body: string }>();
    if (row && row.body) {
      const cfg = JSON.parse(row.body);
      if (cfg.enabled === false) {
        globalTelegramEnabled = false;
      }
    }
  } catch {}

  const query = `
    SELECT p.*, COUNT(i.id) as item_count
    FROM emoji_packs p
    LEFT JOIN emoji_items i ON p.id = i.pack_id
    GROUP BY p.id
    ORDER BY p.created_at DESC
  `;
  const { results } = await db.prepare(query).all<any>();

  const list = (results || []).map((r) => ({
    id: r.id,
    name: r.name,
    title: r.title,
    source: r.source,
    licenseAttribution: r.license_attribution || "",
    enabled: Boolean(r.enabled),
    itemCount: Number(r.item_count || 0),
    createdAt: r.created_at,
    createdBy: r.created_by || "",
  }));

  return c.json({
    success: true,
    data: {
      packs: list,
      globalTelegramEnabled,
      telegramBotConfigured: Boolean(c.env.TELEGRAM_BOT_TOKEN && c.env.TELEGRAM_BOT_TOKEN.trim().length > 0),
    },
  });
});

// POST /api/admin/emoji/packs - Create a new emoji pack
emojiAdminRoutes.post("/packs", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));

  const name = String(body.name || "").trim().toLowerCase();
  const title = String(body.title || "").trim();
  const source = String(body.source || "upload").trim();
  const licenseAttribution = String(body.licenseAttribution || "").trim();

  if (!name || !title || !/^[a-z0-9_-]{2,50}$/.test(name)) {
    return c.json({ success: false, error: "Invalid pack name or title. Name must be 2-50 alphanumeric chars." }, 400);
  }

  const existing = await db.prepare("SELECT id FROM emoji_packs WHERE name = ?").bind(name).first();
  if (existing) {
    return c.json({ success: false, error: `Emoji pack '${name}' already exists.` }, 409);
  }

  const id = `pack_${crypto.randomUUID()}`;
  const now = Date.now();

  const insertStmt = db
    .prepare(
      "INSERT INTO emoji_packs (id, name, title, source, license_attribution, enabled, created_at, created_by) VALUES (?, ?, ?, ?, ?, 1, ?, ?)"
    )
    .bind(id, name, title, source, licenseAttribution, now, user?.email || "admin");

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "CREATE_EMOJI_PACK",
    description: `Created emoji pack '${title}' (${name}) from source '${source}'`,
    targetType: "emoji_pack",
    targetId: id,
    metadata: { name, title, source },
  });

  await db.batch([insertStmt, auditStmt]);

  return c.json({
    success: true,
    data: {
      id,
      name,
      title,
      source,
      licenseAttribution,
      enabled: true,
      itemCount: 0,
      createdAt: now,
      createdBy: user?.email || "admin",
    },
  }, 201);
});

// PUT /api/admin/emoji/packs/:id/status - Toggle pack enabled/disabled (per-pack kill switch)
emojiAdminRoutes.put("/packs/:id/status", async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));

  const pack = await db.prepare("SELECT * FROM emoji_packs WHERE id = ?").bind(id).first<EmojiPackRow>();
  if (!pack) {
    return c.json({ success: false, error: "Emoji pack not found" }, 404);
  }

  const enabled = body.enabled === false ? 0 : 1;

  const updateStmt = db.prepare("UPDATE emoji_packs SET enabled = ? WHERE id = ?").bind(enabled, id);
  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "TOGGLE_EMOJI_PACK_STATUS",
    description: `${enabled ? "Enabled" : "Disabled"} emoji pack '${pack.title}' (${pack.name})`,
    targetType: "emoji_pack",
    targetId: id,
    metadata: { enabled: Boolean(enabled), name: pack.name },
  });

  await db.batch([updateStmt, auditStmt]);

  return c.json({ success: true, data: { id, enabled: Boolean(enabled) } });
});

// PUT /api/admin/emoji/global-telegram-status - Toggle global Telegram emoji kill switch
emojiAdminRoutes.put("/global-telegram-status", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));

  const enabled = body.enabled === false ? false : true;
  const now = Date.now();
  const configJson = JSON.stringify({ enabled });

  const stmt = db
    .prepare(
      "INSERT INTO app_content (id, title, body, updated_at) VALUES ('telegram_emoji_global', 'Telegram Emoji Global Switch', ?, ?) ON CONFLICT(id) DO UPDATE SET body = excluded.body, updated_at = excluded.updated_at"
    )
    .bind(configJson, now);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "TOGGLE_GLOBAL_TELEGRAM_EMOJI",
    description: `${enabled ? "Enabled" : "Disabled"} global Telegram emoji switch`,
    targetType: "global_setting",
    targetId: "telegram_emoji_global",
    metadata: { enabled },
  });

  await db.batch([stmt, auditStmt]);

  return c.json({ success: true, data: { globalTelegramEnabled: enabled } });
});

// DELETE /api/admin/emoji/packs/:id - Permanently delete an emoji pack (Takedown)
emojiAdminRoutes.delete("/packs/:id", async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const db = c.env.DB;

  const pack = await db.prepare("SELECT * FROM emoji_packs WHERE id = ?").bind(id).first<EmojiPackRow>();
  if (!pack) {
    return c.json({ success: false, error: "Emoji pack not found" }, 404);
  }

  const snapshotQuery = "SELECT json_group_array(json_object('id', id, 'key', storage_key)) AS snapshot FROM (SELECT id, storage_key FROM emoji_items WHERE pack_id = ? ORDER BY id)";
  const snapshot = (await db.prepare(snapshotQuery).bind(id).first<{ snapshot: string }>())?.snapshot || "[]";
  const items = JSON.parse(snapshot) as { id: string; key: string }[];
  const { id: auditId, stmt: audit } = prepareAdminAuditLog(db, user, {
    actionType: "DELETE_EMOJI_PACK", description: `Removed emoji pack '${pack.title}' (${pack.name})`,
    targetType: "emoji_pack", targetId: id, metadata: { deletedItemsCount: items.length },
    condition: {
      sql: `EXISTS (SELECT 1 FROM emoji_packs WHERE id = ? AND created_at = ?) AND (${snapshotQuery.replace(" AS snapshot", "")}) = ?`,
      bindings: [id, pack.created_at, id, snapshot],
    },
  });
  const gate = "EXISTS (SELECT 1 FROM admin_audit_log WHERE id = ?)";
  const result = await db.batch([
    audit,
    db.prepare(`DELETE FROM emoji_placements WHERE item_id IN (SELECT id FROM emoji_items WHERE pack_id = ?) AND ${gate}`).bind(id, auditId),
    db.prepare(`DELETE FROM emoji_items WHERE pack_id = ? AND ${gate}`).bind(id, auditId),
    db.prepare(`DELETE FROM emoji_packs WHERE id = ? AND ${gate}`).bind(id, auditId),
  ]);
  if (!result[3]?.meta?.changes) return c.json({ success: false, code: "EMOJI_CHANGED", error: "This pack changed. Refresh before deleting it." }, 409);
  const mediaCleanupComplete = await cleanupEmojiMedia(c.env, items.map(item => item.key), c.res.headers.get("X-Request-ID"));
  return c.json({ success: true, data: { mediaCleanupComplete }, message: "Emoji pack removed.",
    ...(mediaCleanupComplete ? {} : { warning: "Pack removed. Some media cleanup could not be completed." }) });
});

// GET /api/admin/emoji/items - List emoji items with pagination and filters
emojiAdminRoutes.get("/items", async (c) => {
  const db = c.env.DB;
  const packId = c.req.query("packId")?.trim();
  const q = c.req.query("q")?.trim();
  const offset = Number(c.req.query("offset") ?? "0");
  const limit = Number(c.req.query("limit") ?? "50");
  if (!Number.isSafeInteger(offset) || offset < 0 || !Number.isSafeInteger(limit) || limit < 1 || limit > 100) {
    return c.json({ success: false, error: "offset must be a non-negative integer and limit must be 1-100." }, 400);
  }

  let query = "SELECT i.*, p.title as pack_title, p.name as pack_slug FROM emoji_items i JOIN emoji_packs p ON i.pack_id = p.id WHERE 1=1";
  const params: any[] = [];

  if (packId) {
    query += " AND i.pack_id = ?";
    params.push(packId);
  }
  if (q) {
    query += " AND (i.alt_emoji LIKE ? OR i.source LIKE ? OR i.pack_name LIKE ?)";
    const wildcard = `%${q}%`;
    params.push(wildcard, wildcard, wildcard);
  }

  query += " ORDER BY i.created_at DESC, i.id DESC LIMIT ? OFFSET ?";
  params.push(limit, offset);

  const { results } = await db.prepare(query).bind(...params).all<any>();
  const storage = new SupabaseStorage(c.env);

  const list = (results || []).map((r) => ({
    id: r.id,
    packId: r.pack_id,
    packTitle: r.pack_title,
    packSlug: r.pack_slug,
    altEmoji: r.alt_emoji,
    kind: r.kind,
    url: r.storage_key.startsWith("res:raw/") ? r.storage_key : storage.getPublicUrl(r.storage_key),
    storageKey: r.storage_key,
    byteSize: r.byte_size,
    sha256: r.sha256,
    enabled: Boolean(r.enabled),
    premiumOrigin: Boolean(r.premium_origin),
    telegramCustomEmojiId: r.telegram_custom_emoji_id,
    source: r.source,
    createdAt: r.created_at,
  }));

  return c.json({ success: true, data: list, offset, limit });
});

// DELETE /api/admin/emoji/items/:id - Delete an individual emoji item
emojiAdminRoutes.delete("/items/:id", async (c) => {
  const user = c.get("user");
  const id = c.req.param("id");
  const db = c.env.DB;

  const item = await db.prepare("SELECT * FROM emoji_items WHERE id = ?").bind(id).first<EmojiItemRow>();
  if (!item) {
    return c.json({ success: false, error: "Emoji item not found" }, 404);
  }

  const { id: auditId, stmt: audit } = prepareAdminAuditLog(db, user, {
    actionType: "DELETE_EMOJI_ITEM", description: `Removed emoji item ${item.alt_emoji}`,
    targetType: "emoji_item", targetId: id,
    condition: { sql: "EXISTS (SELECT 1 FROM emoji_items WHERE id = ? AND storage_key = ? AND created_at = ?)",
      bindings: [id, item.storage_key, item.created_at] },
  });
  const gate = "EXISTS (SELECT 1 FROM admin_audit_log WHERE id = ?)";
  const result = await db.batch([
    audit,
    db.prepare(`DELETE FROM emoji_placements WHERE item_id = ? AND ${gate}`).bind(id, auditId),
    db.prepare(`DELETE FROM emoji_items WHERE id = ? AND ${gate}`).bind(id, auditId),
  ]);
  if (!result[2]?.meta?.changes) return c.json({ success: false, code: "EMOJI_CHANGED", error: "This item changed. Refresh before deleting it." }, 409);
  const mediaCleanupComplete = await cleanupEmojiMedia(c.env, [item.storage_key], c.res.headers.get("X-Request-ID"));
  return c.json({ success: true, data: { mediaCleanupComplete }, message: "Emoji item removed.",
    ...(mediaCleanupComplete ? {} : { warning: "Item removed. Some media cleanup could not be completed." }) });
});

// POST /api/admin/emoji/upload - Upload custom Lottie JSON or static WebP emoji
emojiAdminRoutes.post("/upload", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const contentType = c.req.header("content-type") || "";

  let packId = c.req.query("packId") || "";
  let altEmoji = c.req.query("altEmoji") || "✨";
  let rawBytes: Uint8Array;
  let kind: "lottie" | "static" = "lottie";

  let mime = "application/json";
  try {
    // Bound the stream, including JSON wrappers, before allocating/parsing it.
    const reader = c.req.raw.body?.getReader();
    const chunks: Uint8Array[] = [];
    let total = 0;
    if (reader) {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        total += value.length;
        if (total > 1024 * 1024) { await reader.cancel(); return c.json({ success: false, error: "Upload request exceeds 1MB." }, 413); }
        chunks.push(value);
      }
    }
    const bytes = new Uint8Array(total);
    let position = 0;
    for (const chunk of chunks) { bytes.set(chunk, position); position += chunk.length; }
    if (!total) return c.json({ success: false, error: "Empty upload body" }, 400);
    if (contentType.includes("image/webp") || contentType.includes("image/png")) {
      kind = "static";
      const png = bytes.length >= 24 && [137,80,78,71,13,10,26,10].every((v,i) => bytes[i] === v);
      const webp = bytes.length >= 16 && new TextDecoder().decode(bytes.slice(0,4)) === "RIFF" && new TextDecoder().decode(bytes.slice(8,12)) === "WEBP";
      if ((!png && !webp) || (png && !contentType.includes("image/png")) || (webp && !contentType.includes("image/webp"))) {
        return c.json({ success: false, error: "Image bytes do not match the PNG/WebP content type." }, 400);
      }
      mime = png ? "image/png" : "image/webp";
      rawBytes = bytes;
    } else {
      const parsed = JSON.parse(new TextDecoder().decode(bytes));
      const animation = parsed?.animationData ?? parsed;
      const str = typeof animation === "string" ? animation : JSON.stringify(animation);
      validateLottieJson(str);
      rawBytes = new TextEncoder().encode(str);
      if (parsed.packId !== undefined) {
        if (typeof parsed.packId !== "string") throw new Error("packId must be text.");
        packId = parsed.packId;
      }
      if (parsed.altEmoji !== undefined) {
        if (typeof parsed.altEmoji !== "string") throw new Error("altEmoji must be text.");
        altEmoji = parsed.altEmoji;
      }
    }
  } catch (err) {
    return c.json({ success: false, error: err instanceof SyntaxError ? "Invalid JSON animation." : (err as Error).message }, 400);
  }
  if (!rawBytes.length || rawBytes.length > MAX_ANIMATION_SIZE_BYTES) {
    return c.json({ success: false, error: "Emoji must be between 1 byte and 300KB." }, 413);
  }
  if (!altEmoji.trim() || altEmoji.length > 100) return c.json({ success: false, error: "altEmoji must contain 1-100 characters." }, 400);
  if (packId && !await db.prepare("SELECT id FROM emoji_packs WHERE id = ?").bind(packId).first()) {
    return c.json({ success: false, error: "Emoji pack not found." }, 404);
  }

  // Ensure target pack exists or use custom pack
  if (!packId) {
    let customPack = await db.prepare("SELECT id FROM emoji_packs WHERE source = 'upload' LIMIT 1").first<EmojiPackRow>();
    if (!customPack) {
      const newPackId = "pack_custom_uploads";
      const packInsert = db.prepare(
          "INSERT OR IGNORE INTO emoji_packs (id, name, title, source, license_attribution, enabled, created_at, created_by) VALUES (?, 'custom_uploads', 'Custom Uploads', 'upload', 'Admin user uploaded assets', 1, ?, ?)"
        )
        .bind(newPackId, Date.now(), user?.email || "admin")
;
      const { stmt: packAudit } = prepareAdminAuditLog(db, user, {
        actionType: "CREATE_EMOJI_PACK", description: "Created custom uploads pack", targetType: "emoji_pack", targetId: newPackId,
        condition: { sql: "changes() = 1", bindings: [] },
      });
      await db.batch([packInsert, packAudit]);
      packId = newPackId;
    } else {
      packId = customPack.id;
    }
  }

  const hash = await sha256Hex(rawBytes);
  const ext = kind === "lottie" ? "json" : mime === "image/png" ? "png" : "webp";
  const storageKey = `emoji/uploads/${crypto.randomUUID()}/${hash}.${ext}`;

  const storage = new SupabaseStorage(c.env);
  const publicUrl = await storage.uploadFile(
    storageKey,
    rawBytes,
    mime
  );

  const id = `emoji_${crypto.randomUUID()}`;
  const now = Date.now();

  const insertStmt = db
    .prepare(
      `INSERT INTO emoji_items 
      (id, pack_id, alt_emoji, kind, storage_key, byte_size, sha256, enabled, premium_origin, source, pack_name, uploader, created_at)
      VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0, 'upload', 'Custom Uploads', ?, ?)`
    )
    .bind(id, packId, altEmoji, kind, storageKey, rawBytes.length, hash, user?.email || "admin", now);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "UPLOAD_EMOJI",
    description: `Uploaded ${kind} emoji ${altEmoji} (${rawBytes.length} bytes)`,
    targetType: "emoji_item",
    targetId: id,
    metadata: { altEmoji, kind, storageKey, byteSize: rawBytes.length, sha256: hash },
  });

  try { await db.batch([insertStmt, auditStmt]); }
  catch (error) {
    await cleanupEmojiMedia(c.env, [storageKey], c.res.headers.get("X-Request-ID"));
    throw error;
  }

  return c.json(
    {
      success: true,
      data: {
        id,
        packId,
        altEmoji,
        kind,
        url: publicUrl,
        storageKey,
        byteSize: rawBytes.length,
        sha256: hash,
        enabled: true,
      },
    },
    201
  );
});

// POST /api/admin/emoji/import/telegram - Import Telegram sticker / emoji pack (Batched & Resumable)
emojiAdminRoutes.post("/import/telegram", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const botToken = c.env.TELEGRAM_BOT_TOKEN?.trim();

  if (!botToken) {
    return c.json(
      {
        success: false,
        error: "Telegram import is not configured. TELEGRAM_BOT_TOKEN is not set on the Cloudflare Worker.",
      },
      400
    );
  }

  const body = await c.req.json().catch(() => ({}));
  let packName = String(body.packName || body.name || "").trim();
  const customEmojiIds: string[] = Array.isArray(body.customEmojiIds) ? body.customEmojiIds : [];
  const offset = Number(body.offset ?? 0);
  const limit = Number(body.limit ?? 5);
  if (!Number.isSafeInteger(offset) || offset < 0 || !Number.isSafeInteger(limit) || limit < 1 || limit > 10 ||
      customEmojiIds.length > 100 || customEmojiIds.some(id => typeof id !== "string" || !/^\d{1,30}$/.test(id))) {
    return c.json({ success: false, error: "Provide valid pagination and up to 100 numeric custom emoji IDs." }, 400);
  }

  if (/add(?:stickers|emoji)\//.test(packName)) {
    packName = packName.split(/add(?:stickers|emoji)\//)[1].split(/[?#/]/)[0].trim();
  }
  if (packName && !/^[a-zA-Z0-9_]{1,100}$/.test(packName)) {
    return c.json({ success: false, error: "Provide a valid Telegram sticker pack name or link." }, 400);
  }

  if (!packName && customEmojiIds.length === 0) {
    return c.json({ success: false, error: "Must specify a Telegram pack name or customEmojiIds list" }, 400);
  }

  let stickers: any[] = [];
  let packTitle = packName || "Telegram Custom Emoji";

  try {
    if (packName) {
      const getSetUrl = `https://api.telegram.org/bot${botToken}/getStickerSet?name=${encodeURIComponent(packName)}`;
      const setRes = await fetch(getSetUrl);
      if (!setRes.ok) {
        return c.json({ success: false, error: "Telegram could not load this sticker pack. Check the name and try again." }, 502);
      }
      const setData: any = await setRes.json();
      if (!setData.ok || !setData.result) {
        return c.json({ success: false, error: `Telegram returned: ${setData.description || "Sticker pack not found"}` }, 404);
      }
      stickers = setData.result.stickers || [];
      packTitle = setData.result.title || packName;
    } else if (customEmojiIds.length > 0) {
      const getStickersUrl = `https://api.telegram.org/bot${botToken}/getCustomEmojiStickers?custom_emoji_ids=${encodeURIComponent(
        JSON.stringify(customEmojiIds.slice(offset, offset + limit))
      )}`;
      const res = await fetch(getStickersUrl);
      if (!res.ok) {
        return c.json({ success: false, error: "Failed to fetch custom emoji stickers from Telegram Bot API" }, 400);
      }
      const data: any = await res.json();
      stickers = data.result || [];
    }
  } catch (err: any) {
    return c.json({ success: false, error: "Unable to connect to Telegram. Please retry." }, 502);
  }

  const packSlug = (packName || `tg_${customEmojiIds[0] || "custom"}`).toLowerCase().replace(/[^a-z0-9_-]/g, "_");
  let packRow = await db.prepare("SELECT * FROM emoji_packs WHERE name = ?").bind(packSlug).first<EmojiPackRow>();
  let packId: string;

  if (!packRow) {
    packId = `pack_tg_${packSlug}`;
    const packInsert = db.prepare(
        `INSERT INTO emoji_packs (id, name, title, source, license_attribution, enabled, created_at, created_by)
        VALUES (?, ?, ?, 'telegram', 'Telegram Official Sticker/Custom Emoji Pack. User-consented import.', 1, ?, ?)`
      )
      .bind(packId, packSlug, packTitle, Date.now(), user?.email || "admin")
;
    const { stmt: packAudit } = prepareAdminAuditLog(db, user, {
      actionType: "CREATE_EMOJI_PACK", description: `Created imported emoji pack '${packTitle}'`, targetType: "emoji_pack", targetId: packId,
    });
    await db.batch([packInsert, packAudit]);
  } else {
    packId = packRow.id;
  }

  const batchSlice = packName ? stickers.slice(offset, offset + limit) : stickers;
  const storage = new SupabaseStorage(c.env);
  let importedCount = 0;
  const errors: string[] = [];

  for (const sticker of batchSlice) {
    if (sticker.is_video) {
      errors.push(`Sticker ${sticker.emoji || sticker.file_id}: Video (.webm) is not supported.`);
      continue;
    }

    try {
      const getFileUrl = `https://api.telegram.org/bot${botToken}/getFile?file_id=${sticker.file_id}`;
      const fileRes = await fetch(getFileUrl);
      if (!fileRes.ok) continue;
      const fileData: any = await fileRes.json();
      if (!fileData.ok || !fileData.result?.file_path) continue;

      const filePath = fileData.result.file_path;
      const downloadUrl = `https://api.telegram.org/file/bot${botToken}/${filePath}`;
      const downRes = await fetch(downloadUrl);
      if (!downRes.ok) continue;

      const fileBytes = new Uint8Array(await downRes.arrayBuffer());
      let finalBytes: Uint8Array;
      let kind: "lottie" | "static" = "lottie";

      if (filePath.endsWith(".tgs") || sticker.is_animated) {
        const decompressedJson = await decompressGzipWithGuard(fileBytes, MAX_ANIMATION_SIZE_BYTES);
        validateLottieJson(decompressedJson);
        finalBytes = new TextEncoder().encode(decompressedJson);
        kind = "lottie";
      } else if (filePath.endsWith(".webp") || filePath.endsWith(".png")) {
        if (fileBytes.length > MAX_ANIMATION_SIZE_BYTES) {
          errors.push(`Sticker ${sticker.emoji}: Exceeds 300KB limit.`);
          continue;
        }
        finalBytes = fileBytes;
        kind = "static";
      } else {
        errors.push(`Sticker ${sticker.emoji}: Unsupported file extension '${filePath}'`);
        continue;
      }

      const hash = await sha256Hex(finalBytes);
      const ext = kind === "lottie" ? "json" : "webp";
      const storageKey = `emoji/telegram/${packSlug}/${crypto.randomUUID()}/${hash}.${ext}`;

      await storage.uploadFile(storageKey, finalBytes, kind === "lottie" ? "application/json" : "image/webp");

      const itemId = `emoji_tg_${hash.slice(0, 16)}`;
      const altEmoji = sticker.emoji || "✨";
      const isPremium = Boolean(sticker.custom_emoji_id);

      const itemStatement = db.prepare(
          `INSERT INTO emoji_items 
          (id, pack_id, alt_emoji, kind, storage_key, byte_size, sha256, enabled, premium_origin, telegram_custom_emoji_id, source, pack_name, uploader, created_at)
          VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, 'telegram', ?, ?, ?)
          ON CONFLICT(id) DO UPDATE SET storage_key = excluded.storage_key, enabled = 1`
        )
        .bind(
          itemId,
          packId,
          altEmoji,
          kind,
          storageKey,
          finalBytes.length,
          hash,
          isPremium ? 1 : 0,
          sticker.custom_emoji_id || null,
          packTitle,
          user?.email || "admin",
          Date.now()
        )
;
      await saveImportedItem(db, c.env, user, itemId, storageKey, itemStatement);

      importedCount++;
    } catch (itemErr: any) {
      errors.push(`Sticker ${sticker.emoji || ""}: Import failed. Please retry.`);
    }
  }

  await recordAdminAction(db, user, {
    actionType: "IMPORT_TELEGRAM_EMOJI",
    description: `Imported ${importedCount} items from Telegram pack '${packTitle}' (offset: ${offset})`,
    targetType: "emoji_pack",
    targetId: packId,
    metadata: { packName, packTitle, importedCount, offset, limit, totalStickers: stickers.length },
  });

  const totalStickers = packName ? stickers.length : customEmojiIds.length;
  const nextOffset = offset + limit < totalStickers ? offset + limit : null;

  return c.json({
    success: true,
    data: {
      packId,
      packName,
      packTitle,
      totalStickers,
      offset,
      limit,
      importedCount,
      nextOffset,
      errors: errors.length > 0 ? errors : undefined,
    },
  });
});

// POST /api/admin/emoji/import/noto - Import Google Noto animated emoji (Verified CDN)
emojiAdminRoutes.post("/import/noto", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));

  const NOTO_PRESETS: Record<string, { codepoint: string; alt: string; name: string }[]> = {
    popular: [
      { codepoint: "1f600", alt: "😀", name: "Grinning Face" },
      { codepoint: "1f602", alt: "😂", name: "Tears of Joy" },
      { codepoint: "1f525", alt: "🔥", name: "Fire" },
      { codepoint: "2b50", alt: "⭐", name: "Star" },
      { codepoint: "1f389", alt: "🎉", name: "Party Popper" },
      { codepoint: "2764_fe0f", alt: "❤️", name: "Red Heart" },
      { codepoint: "1f44d", alt: "👍", name: "Thumbs Up" },
      { codepoint: "1f4af", alt: "💯", name: "Hundred Points" },
      { codepoint: "1f4a1", alt: "💡", name: "Light Bulb" },
      { codepoint: "1f3c6", alt: "🏆", name: "Trophy" },
    ],
    study: [
      { codepoint: "1f4d6", alt: "📖", name: "Open Book" },
      { codepoint: "270f_fe0f", alt: "✏️", name: "Pencil" },
      { codepoint: "1f393", alt: "🎓", name: "Graduation Cap" },
      { codepoint: "23f0", alt: "⏰", name: "Alarm Clock" },
      { codepoint: "1f4ca", alt: "📊", name: "Bar Chart" },
      { codepoint: "2705", alt: "✅", name: "Check Mark" },
    ],
  };

  const category = String(body.category || "popular").toLowerCase();
  const itemsToImport = Array.isArray(body.items) && body.items.length > 0
    ? body.items
    : NOTO_PRESETS[category] || NOTO_PRESETS.popular;

  if (itemsToImport.length > 20 || itemsToImport.some((item: any) =>
      !item || typeof item.codepoint !== "string" || !/^[0-9a-f]{2,6}(?:_[0-9a-f]{2,6})*$/i.test(item.codepoint) ||
      (item.alt !== undefined && (typeof item.alt !== "string" || item.alt.length > 100)))) {
    return c.json({ success: false, error: "Provide up to 20 valid Unicode codepoints and text alternatives." }, 400);
  }
  const packId = "pack_noto";
  if (!await db.prepare("SELECT id FROM emoji_packs WHERE id = ?").bind(packId).first()) {
    return c.json({ success: false, error: "The Noto pack was removed. Create or restore its pack before importing." }, 409);
  }
  const storage = new SupabaseStorage(c.env);
  let importedCount = 0;
  const errors: string[] = [];

  for (const item of itemsToImport) {
    const codepoint = String(item.codepoint || "").toLowerCase().trim();
    const alt = item.alt || "✨";
    if (!codepoint) continue;

    const cdnUrl = `https://fonts.gstatic.com/s/e/notoemoji/latest/${codepoint}/lottie.json`;
    try {
      const res = await fetch(cdnUrl);
      if (!res.ok) {
        errors.push(`Failed to download Noto emoji ${codepoint} (${res.status})`);
        continue;
      }
      const jsonStr = await res.text();
      validateLottieJson(jsonStr);
      const rawBytes = new TextEncoder().encode(jsonStr);

      if (rawBytes.length > MAX_ANIMATION_SIZE_BYTES) {
        errors.push(`Noto emoji ${codepoint} exceeds 300KB limit.`);
        continue;
      }

      const hash = await sha256Hex(rawBytes);
      const storageKey = `emoji/noto/${crypto.randomUUID()}/${hash}.json`;

      await storage.uploadFile(storageKey, rawBytes, "application/json");

      const itemId = `emoji_noto_${codepoint}`;
      const itemStatement = db.prepare(
          `INSERT INTO emoji_items 
          (id, pack_id, alt_emoji, kind, storage_key, byte_size, sha256, enabled, premium_origin, source, pack_name, uploader, created_at)
          VALUES (?, ?, ?, 'lottie', ?, ?, ?, 1, 0, 'noto', 'Noto Animated Emoji', ?, ?)
          ON CONFLICT(id) DO UPDATE SET storage_key = excluded.storage_key, enabled = 1`
        )
        .bind(itemId, packId, alt, storageKey, rawBytes.length, hash, user?.email || "admin", Date.now())
;
      await saveImportedItem(db, c.env, user, itemId, storageKey, itemStatement);

      importedCount++;
    } catch (err: any) {
      errors.push(`Error importing Noto ${codepoint}. Please retry.`);
    }
  }

  await recordAdminAction(db, user, {
    actionType: "IMPORT_NOTO_EMOJI",
    description: `Imported ${importedCount} Google Noto animated emoji items (${category})`,
    targetType: "emoji_pack",
    targetId: packId,
    metadata: { category, importedCount },
  });

  return c.json({
    success: true,
    data: {
      packId,
      importedCount,
      errors: errors.length > 0 ? errors : undefined,
    },
  });
});

// GET /api/admin/emoji/placements - List all surface placements
emojiAdminRoutes.get("/placements", async (c) => {
  const db = c.env.DB;
  const storage = new SupabaseStorage(c.env);

  const query = `
    SELECT 
      ep.*,
      ei.alt_emoji, ei.kind, ei.storage_key, ei.source as item_source,
      p.title as pack_title
    FROM emoji_placements ep
    JOIN emoji_items ei ON ep.item_id = ei.id
    JOIN emoji_packs p ON ei.pack_id = p.id
    ORDER BY ep.updated_at DESC
  `;
  const { results } = await db.prepare(query).all<any>();

  const list = (results || []).map((r) => ({
    id: r.id,
    slotType: r.slot_type,
    targetId: r.target_id,
    itemId: r.item_id,
    altEmoji: r.alt_emoji,
    kind: r.kind,
    url: r.storage_key.startsWith("res:raw/") ? r.storage_key : storage.getPublicUrl(r.storage_key),
    packTitle: r.pack_title,
    updatedBy: r.updated_by || "",
    updatedAt: r.updated_at,
  }));

  return c.json({ success: true, data: list });
});

// POST /api/admin/emoji/placements - Set placement for exam card, category header, or broadcast
emojiAdminRoutes.post("/placements", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));

  const slotType = String(body.slotType || "").trim();
  const targetId = String(body.targetId || "").trim();
  const itemId = String(body.itemId || "").trim();

  if (!["exam_card", "category_header", "broadcast"].includes(slotType)) {
    return c.json(
      { success: false, error: "slotType must be one of: exam_card, category_header, broadcast" },
      400
    );
  }
  if (!targetId || !itemId) {
    return c.json({ success: false, error: "targetId and itemId are required" }, 400);
  }

  const targetQuery = slotType === "exam_card" ? "SELECT id FROM exams WHERE id = ?"
    : slotType === "broadcast" ? "SELECT id FROM notifications WHERE id = ?"
    : "SELECT id FROM exams WHERE category = ? LIMIT 1";
  if (!await db.prepare(targetQuery).bind(targetId).first()) {
    return c.json({ success: false, code: "PLACEMENT_TARGET_NOT_FOUND", error: "Placement target not found. Refresh and check its ID or category name." }, 404);
  }

  const item = await db.prepare("SELECT * FROM emoji_items WHERE id = ?").bind(itemId).first<EmojiItemRow>();
  if (!item) {
    return c.json({ success: false, error: `Emoji item '${itemId}' not found` }, 404);
  }

  const id = `plc_${crypto.randomUUID()}`;
  const now = Date.now();

  const insertStmt = db
    .prepare(
      `INSERT INTO emoji_placements (id, slot_type, target_id, item_id, updated_by, updated_at)
      VALUES (?, ?, ?, ?, ?, ?)
      ON CONFLICT(slot_type, target_id) DO UPDATE SET
        item_id = excluded.item_id,
        updated_by = excluded.updated_by,
        updated_at = excluded.updated_at`
    )
    .bind(id, slotType, targetId, itemId, user?.email || "admin", now);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "SET_EMOJI_PLACEMENT",
    description: `Placed emoji ${item.alt_emoji} on ${slotType} for '${targetId}'`,
    targetType: "emoji_placement",
    targetId: `${slotType}:${targetId}`,
    metadata: { slotType, targetId, itemId, altEmoji: item.alt_emoji },
  });

  await db.batch([insertStmt, auditStmt]);

  return c.json({
    success: true,
    data: {
      slotType,
      targetId,
      itemId,
      altEmoji: item.alt_emoji,
      updatedBy: user?.email || "admin",
      updatedAt: now,
    },
  });
});

// DELETE /api/admin/emoji/placements - Clear an emoji placement
emojiAdminRoutes.delete("/placements", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const slotType = c.req.query("slotType") || "";
  const targetId = c.req.query("targetId") || "";

  if (!slotType || !targetId) {
    return c.json({ success: false, error: "slotType and targetId query params required" }, 400);
  }

  const deleteStmt = db
    .prepare("DELETE FROM emoji_placements WHERE slot_type = ? AND target_id = ?")
    .bind(slotType, targetId);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "CLEAR_EMOJI_PLACEMENT",
    description: `Removed emoji placement on ${slotType} for '${targetId}'`,
    targetType: "emoji_placement",
    targetId: `${slotType}:${targetId}`,
    metadata: { slotType, targetId },
  });

  await db.batch([deleteStmt, auditStmt]);

  return c.json({ success: true, message: "Placement cleared successfully." });
});
