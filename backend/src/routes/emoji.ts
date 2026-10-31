// ============================================================================
// Animated Emoji System Routes (Mission 03 F4)
// Telegram packs, Google Noto animated emoji, custom uploads, and surface placements.
// ============================================================================

import { Hono } from "hono";
import { requireAdmin, requirePermission } from "../middleware/authMiddleware";
import { AuthUser, EmojiItemRow, EmojiPackRow, EmojiPlacementPublic, EmojiPlacementRow, Env } from "../types";
import { SupabaseStorage } from "../supabase";
import { prepareAdminAuditLog, recordAdminAction } from "../util/auditLog";

export const emojiRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();
export const emojiAdminRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

const MAX_ANIMATION_SIZE_BYTES = 300 * 1024; // 300 KB size cap

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
      if (typeof asset.u === "string" && (asset.u.startsWith("http://") || asset.u.startsWith("https://"))) {
        throw new Error("Security check failed: external network references in Lottie assets are prohibited");
      }
      if (typeof asset.p === "string" && (asset.p.startsWith("http://") || asset.p.startsWith("https://"))) {
        throw new Error("Security check failed: external network references in Lottie assets are prohibited");
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
    if (!globalTelegramEnabled && r.item_source === "telegram") {
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

  // 1. Fetch items to delete files from Supabase storage
  const { results: items } = await db
    .prepare("SELECT storage_key FROM emoji_items WHERE pack_id = ?")
    .bind(id)
    .all<{ storage_key: string }>();

  const storage = new SupabaseStorage(c.env);
  for (const item of items || []) {
    if (item.storage_key && !item.storage_key.startsWith("res:raw/")) {
      try {
        await storage.deleteFile(item.storage_key);
      } catch (_e) {}
    }
  }

  // 2. Cascade delete pack and items in DB
  const deletePlacements = db
    .prepare("DELETE FROM emoji_placements WHERE item_id IN (SELECT id FROM emoji_items WHERE pack_id = ?)")
    .bind(id);
  const deleteItems = db.prepare("DELETE FROM emoji_items WHERE pack_id = ?").bind(id);
  const deletePack = db.prepare("DELETE FROM emoji_packs WHERE id = ?").bind(id);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "DELETE_EMOJI_PACK",
    description: `Permanently removed emoji pack '${pack.title}' (${pack.name}) and deleted files`,
    targetType: "emoji_pack",
    targetId: id,
    metadata: { name: pack.name, title: pack.title, deletedItemsCount: items?.length || 0 },
  });

  await db.batch([deletePlacements, deleteItems, deletePack, auditStmt]);

  return c.json({
    success: true,
    message: `Emoji pack '${pack.title}' and ${items?.length || 0} items removed permanently.`,
  });
});

// GET /api/admin/emoji/items - List emoji items with pagination and filters
emojiAdminRoutes.get("/items", async (c) => {
  const db = c.env.DB;
  const packId = c.req.query("packId")?.trim();
  const q = c.req.query("q")?.trim();
  const offset = parseInt(c.req.query("offset") || "0", 10) || 0;
  const limit = Math.min(100, Math.max(1, parseInt(c.req.query("limit") || "50", 10) || 50));

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

  query += " ORDER BY i.created_at DESC LIMIT ? OFFSET ?";
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

  if (item.storage_key && !item.storage_key.startsWith("res:raw/")) {
    try {
      const storage = new SupabaseStorage(c.env);
      await storage.deleteFile(item.storage_key);
    } catch (_e) {}
  }

  const deletePlacements = db.prepare("DELETE FROM emoji_placements WHERE item_id = ?").bind(id);
  const deleteItem = db.prepare("DELETE FROM emoji_items WHERE id = ?").bind(id);

  const { stmt: auditStmt } = prepareAdminAuditLog(db, user, {
    actionType: "DELETE_EMOJI_ITEM",
    description: `Deleted emoji item ${item.alt_emoji} (${item.id}) from pack '${item.pack_name || item.pack_id}'`,
    targetType: "emoji_item",
    targetId: id,
    metadata: { altEmoji: item.alt_emoji, storageKey: item.storage_key },
  });

  await db.batch([deletePlacements, deleteItem, auditStmt]);

  return c.json({ success: true, message: "Emoji item deleted successfully." });
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

  if (contentType.includes("application/json")) {
    const jsonBody = await c.req.json().catch(() => ({}));
    if (jsonBody.animationData) {
      const str = typeof jsonBody.animationData === "string" ? jsonBody.animationData : JSON.stringify(jsonBody.animationData);
      validateLottieJson(str);
      rawBytes = new TextEncoder().encode(str);
    } else {
      const str = JSON.stringify(jsonBody);
      validateLottieJson(str);
      rawBytes = new TextEncoder().encode(str);
    }
    if (jsonBody.packId) packId = String(jsonBody.packId);
    if (jsonBody.altEmoji) altEmoji = String(jsonBody.altEmoji);
  } else {
    const arrayBuffer = await c.req.arrayBuffer();
    rawBytes = new Uint8Array(arrayBuffer);
    if (contentType.includes("image/webp") || contentType.includes("image/png")) {
      kind = "static";
    } else {
      const str = new TextDecoder().decode(rawBytes);
      validateLottieJson(str);
    }
  }

  if (rawBytes.length === 0) {
    return c.json({ success: false, error: "Empty upload body" }, 400);
  }
  if (rawBytes.length > MAX_ANIMATION_SIZE_BYTES) {
    return c.json(
      {
        success: false,
        error: `File size (${rawBytes.length} bytes) exceeds maximum limit of ${MAX_ANIMATION_SIZE_BYTES} bytes (300KB)`,
      },
      400
    );
  }

  // Ensure target pack exists or use custom pack
  if (!packId) {
    let customPack = await db.prepare("SELECT id FROM emoji_packs WHERE source = 'upload' LIMIT 1").first<EmojiPackRow>();
    if (!customPack) {
      const newPackId = "pack_custom_uploads";
      await db
        .prepare(
          "INSERT OR IGNORE INTO emoji_packs (id, name, title, source, license_attribution, enabled, created_at, created_by) VALUES (?, 'custom_uploads', 'Custom Uploads', 'upload', 'Admin user uploaded assets', 1, ?, ?)"
        )
        .bind(newPackId, Date.now(), user?.email || "admin")
        .run();
      packId = newPackId;
    } else {
      packId = customPack.id;
    }
  }

  const hash = await sha256Hex(rawBytes);
  const ext = kind === "lottie" ? "json" : "webp";
  const storageKey = `emoji/uploads/${hash}.${ext}`;

  const storage = new SupabaseStorage(c.env);
  const publicUrl = await storage.uploadFile(
    storageKey,
    rawBytes,
    kind === "lottie" ? "application/json" : "image/webp"
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

  await db.batch([insertStmt, auditStmt]);

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
  const offset = Math.max(0, parseInt(body.offset || "0", 10) || 0);
  const limit = Math.min(10, Math.max(1, parseInt(body.limit || "5", 10) || 5));

  if (packName.includes("addstickers/")) {
    packName = packName.split("addstickers/")[1].split(/[?#/]/)[0].trim();
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
        const errText = await setRes.text();
        return c.json({ success: false, error: `Telegram Bot API error (${setRes.status}): ${errText}` }, 400);
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
    return c.json({ success: false, error: `Network error connecting to Telegram Bot API: ${err.message}` }, 502);
  }

  const packSlug = (packName || `tg_${customEmojiIds[0] || "custom"}`).toLowerCase().replace(/[^a-z0-9_-]/g, "_");
  let packRow = await db.prepare("SELECT * FROM emoji_packs WHERE name = ?").bind(packSlug).first<EmojiPackRow>();
  let packId: string;

  if (!packRow) {
    packId = `pack_tg_${packSlug}`;
    await db
      .prepare(
        `INSERT INTO emoji_packs (id, name, title, source, license_attribution, enabled, created_at, created_by)
        VALUES (?, ?, ?, 'telegram', 'Telegram Official Sticker/Custom Emoji Pack. User-consented import.', 1, ?, ?)`
      )
      .bind(packId, packSlug, packTitle, Date.now(), user?.email || "admin")
      .run();
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
      const storageKey = `emoji/telegram/${packSlug}/${hash}.${ext}`;

      await storage.uploadFile(storageKey, finalBytes, kind === "lottie" ? "application/json" : "image/webp");

      const itemId = `emoji_tg_${hash.slice(0, 16)}`;
      const altEmoji = sticker.emoji || "✨";
      const isPremium = Boolean(sticker.custom_emoji_id);

      await db
        .prepare(
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
        .run();

      importedCount++;
    } catch (itemErr: any) {
      errors.push(`Sticker ${sticker.emoji}: ${itemErr.message}`);
    }
  }

  await recordAdminAction(db, user, {
    actionType: "IMPORT_TELEGRAM_EMOJI",
    description: `Imported ${importedCount} items from Telegram pack '${packTitle}' (offset: ${offset})`,
    targetType: "emoji_pack",
    targetId: packId,
    metadata: { packName, packTitle, importedCount, offset, limit, totalStickers: stickers.length },
  });

  const nextOffset = offset + limit < stickers.length ? offset + limit : null;

  return c.json({
    success: true,
    data: {
      packId,
      packName,
      packTitle,
      totalStickers: stickers.length,
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

  const packId = "pack_noto";
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
      const storageKey = `emoji/noto/${hash}.json`;

      await storage.uploadFile(storageKey, rawBytes, "application/json");

      const itemId = `emoji_noto_${codepoint}`;
      await db
        .prepare(
          `INSERT INTO emoji_items 
          (id, pack_id, alt_emoji, kind, storage_key, byte_size, sha256, enabled, premium_origin, source, pack_name, uploader, created_at)
          VALUES (?, ?, ?, 'lottie', ?, ?, ?, 1, 0, 'noto', 'Noto Animated Emoji', ?, ?)
          ON CONFLICT(id) DO UPDATE SET storage_key = excluded.storage_key, enabled = 1`
        )
        .bind(itemId, packId, alt, storageKey, rawBytes.length, hash, user?.email || "admin", Date.now())
        .run();

      importedCount++;
    } catch (err: any) {
      errors.push(`Error importing Noto ${codepoint}: ${err.message}`);
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
