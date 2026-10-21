import { Context, Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import { AuthUser, Env } from "../types";

export const typographyRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();
export const adminTypographyRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

const ALLOWED_FONT_FAMILIES = new Set(["inter", "poppins", "roboto", "noto_sans"]);
const ALLOWED_PRESETS = new Set([
  "ios26_balanced",
  "ios26_hig_reference",
  "poppins_clean",
  "roboto_neutral",
  "noto_readable"
]);

function createDefaultConfig() {
  return {
    schema_version: 1,
    profile_id: "default",
    preset_id: "ios26_balanced",
    font_family: "inter",
    global_styles: {
      large_title: { text_size_sp: 34, line_height_sp: 41, weight: 400 },
      title_1: { text_size_sp: 28, line_height_sp: 34, weight: 700 },
      title_2: { text_size_sp: 22, line_height_sp: 28, weight: 700 },
      title_3: { text_size_sp: 20, line_height_sp: 25, weight: 600 },
      headline: { text_size_sp: 17, line_height_sp: 22, weight: 600 },
      body: { text_size_sp: 17, line_height_sp: 22, weight: 400 },
      callout: { text_size_sp: 16, line_height_sp: 21, weight: 400 },
      subheadline: { text_size_sp: 15, line_height_sp: 20, weight: 400 },
      footnote: { text_size_sp: 13, line_height_sp: 18, weight: 400 },
      caption_1: { text_size_sp: 12, line_height_sp: 16, weight: 400 },
      caption_2: { text_size_sp: 11, line_height_sp: 13, weight: 500 }
    },
    screen_overrides: {},
    component_overrides: {},
    element_overrides: {},
    version: 1,
    status: "published",
    published_at: Date.now(),
    notes: "Default factory iOS 26 Balanced preset"
  };
}

function validateConfig(config: any): { valid: boolean; error?: string } {
  if (!config || typeof config !== "object") {
    return { valid: false, error: "Configuration must be a valid JSON object" };
  }
  if (config.font_family && !ALLOWED_FONT_FAMILIES.has(config.font_family.toLowerCase())) {
    return { valid: false, error: `Invalid font family: ${config.font_family}. Must be one of: inter, poppins, roboto, noto_sans.` };
  }
  if (config.preset_id && !ALLOWED_PRESETS.has(config.preset_id.toLowerCase())) {
    return { valid: false, error: `Invalid preset ID: ${config.preset_id}` };
  }
  return { valid: true };
}

// -----------------------------------------------------------------------------
// PUBLIC / STUDENT ENDPOINT: GET /api/typography/published
// -----------------------------------------------------------------------------
typographyRoutes.get("/published", async (c) => {
  const db = c.env.DB;
  try {
    const row = await db.prepare("SELECT version, config_json, published_at FROM typography_published WHERE id = 'active'").first<{
      version: number;
      config_json: string;
      published_at: number;
    }>();

    if (row && row.config_json) {
      const config = JSON.parse(row.config_json);
      const user = c.get("user");
      if (!user?.isAdmin) {
        delete config.published_by;
        delete config.updated_by;
      }
      c.header("ETag", `"typography-v${row.version}"`);
      c.header("Cache-Control", "public, max-age=60");
      return c.json({ success: true, data: config });
    }
  } catch (err: any) {
    console.error("Error fetching published typography:", err);
  }

  const defaultConfig = createDefaultConfig();
  return c.json({ success: true, data: defaultConfig });
});

// -----------------------------------------------------------------------------
// ADMIN ENDPOINTS: /api/admin/typography/*
// -----------------------------------------------------------------------------
adminTypographyRoutes.use("*", requireAdmin);

// GET /api/admin/typography/draft
adminTypographyRoutes.get("/draft", async (c) => {
  const db = c.env.DB;
  try {
    const row = await db.prepare("SELECT config_json, updated_at, updated_by FROM typography_drafts WHERE id = 'active'").first<{
      config_json: string;
      updated_at: number;
      updated_by: string;
    }>();

    if (row && row.config_json) {
      const draft = JSON.parse(row.config_json);
      return c.json({ success: true, data: draft });
    }
  } catch (err: any) {
    console.error("Error fetching draft typography:", err);
  }

  const pub = await db.prepare("SELECT config_json FROM typography_published WHERE id = 'active'").first<{ config_json: string }>();
  const config = pub?.config_json ? JSON.parse(pub.config_json) : createDefaultConfig();
  config.status = "draft";
  return c.json({ success: true, data: config });
});

// POST /api/admin/typography/draft
adminTypographyRoutes.post("/draft", async (c) => {
  const db = c.env.DB;
  const user = c.get("user");
  const body = await c.req.json();

  const validation = validateConfig(body);
  if (!validation.valid) {
    return c.json({ success: false, error: validation.error }, 400);
  }

  const now = Date.now();
  const draftConfig = {
    ...body,
    status: "draft",
    updated_at: now,
    updated_by: user.email
  };

  const jsonStr = JSON.stringify(draftConfig);
  if (jsonStr.length > 50000) {
    return c.json({ success: false, error: "Payload size exceeds 50KB limit" }, 400);
  }

  await db.prepare(
    "INSERT INTO typography_drafts (id, config_json, updated_at, updated_by) VALUES ('active', ?, ?, ?) ON CONFLICT(id) DO UPDATE SET config_json = excluded.config_json, updated_at = excluded.updated_at, updated_by = excluded.updated_by"
  ).bind(jsonStr, now, user.email).run();

  return c.json({ success: true, data: draftConfig });
});

// POST /api/admin/typography/publish
adminTypographyRoutes.post("/publish", async (c) => {
  const db = c.env.DB;
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));

  const draftRow = await db.prepare("SELECT config_json FROM typography_drafts WHERE id = 'active'").first<{ config_json: string }>();
  let configToPublish = draftRow?.config_json ? JSON.parse(draftRow.config_json) : null;

  if (!configToPublish && body && Object.keys(body).length > 1) {
    configToPublish = body;
  }
  if (!configToPublish) {
    configToPublish = createDefaultConfig();
  }

  const validation = validateConfig(configToPublish);
  if (!validation.valid) {
    return c.json({ success: false, error: validation.error }, 400);
  }

  const curRow = await db.prepare("SELECT version FROM typography_published WHERE id = 'active'").first<{ version: number }>();
  const nextVersion = (curRow?.version || 0) + 1;
  const now = Date.now();

  const publishedConfig = {
    ...configToPublish,
    version: nextVersion,
    status: "published",
    published_at: now,
    published_by: user.email,
    notes: body.notes || `Published version ${nextVersion}`
  };

  const jsonStr = JSON.stringify(publishedConfig);

  await db.batch([
    db.prepare("INSERT INTO typography_published (id, version, config_json, published_at, published_by, notes) VALUES ('active', ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET version = excluded.version, config_json = excluded.config_json, published_at = excluded.published_at, published_by = excluded.published_by, notes = excluded.notes").bind(nextVersion, jsonStr, now, user.email, publishedConfig.notes),
    db.prepare("INSERT INTO typography_versions (version, config_json, created_at, created_by, notes) VALUES (?, ?, ?, ?, ?)").bind(nextVersion, jsonStr, now, user.email, publishedConfig.notes),
    db.prepare("INSERT INTO typography_audit_log (id, action, performed_by, version, details, timestamp) VALUES (?, 'publish', ?, ?, ?, ?)").bind(`audit-${now}`, user.email, nextVersion, publishedConfig.notes, now)
  ]);

  return c.json({ success: true, data: publishedConfig });
});

// GET /api/admin/typography/versions
adminTypographyRoutes.get("/versions", async (c) => {
  const db = c.env.DB;
  const { results } = await db.prepare("SELECT version, created_at, created_by, notes FROM typography_versions ORDER BY version DESC LIMIT 30").all<{
    version: number;
    created_at: number;
    created_by: string;
    notes: string;
  }>();

  return c.json({ success: true, data: results || [] });
});

// POST /api/admin/typography/rollback
adminTypographyRoutes.post("/rollback", async (c) => {
  const db = c.env.DB;
  const user = c.get("user");
  const { version } = await c.req.json();

  if (!version || typeof version !== "number") {
    return c.json({ success: false, error: "A valid version number is required for rollback" }, 400);
  }

  const targetRow = await db.prepare("SELECT config_json, notes FROM typography_versions WHERE version = ?").bind(version).first<{
    config_json: string;
    notes: string;
  }>();

  if (!targetRow || !targetRow.config_json) {
    return c.json({ success: false, error: `Version ${version} not found in snapshot history` }, 404);
  }

  const restoredConfig = JSON.parse(targetRow.config_json);
  const now = Date.now();
  restoredConfig.updated_at = now;
  restoredConfig.updated_by = user.email;
  restoredConfig.notes = `Rolled back to version ${version}`;

  const jsonStr = JSON.stringify(restoredConfig);

  await db.batch([
    db.prepare("INSERT INTO typography_published (id, version, config_json, published_at, published_by, notes) VALUES ('active', ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET version = excluded.version, config_json = excluded.config_json, published_at = excluded.published_at, published_by = excluded.published_by, notes = excluded.notes").bind(restoredConfig.version, jsonStr, now, user.email, restoredConfig.notes),
    db.prepare("INSERT INTO typography_drafts (id, config_json, updated_at, updated_by) VALUES ('active', ?, ?, ?) ON CONFLICT(id) DO UPDATE SET config_json = excluded.config_json, updated_at = excluded.updated_at, updated_by = excluded.updated_by").bind(jsonStr, now, user.email),
    db.prepare("INSERT INTO typography_audit_log (id, action, performed_by, version, details, timestamp) VALUES (?, 'rollback', ?, ?, ?, ?)").bind(`audit-${now}`, user.email, version, `Rolled back to version ${version}`, now)
  ]);

  return c.json({ success: true, data: restoredConfig });
});

// POST /api/admin/typography/reset
adminTypographyRoutes.post("/reset", async (c) => {
  const db = c.env.DB;
  const user = c.get("user");
  const defaultConfig = createDefaultConfig();
  const now = Date.now();
  const jsonStr = JSON.stringify(defaultConfig);

  await db.batch([
    db.prepare("INSERT INTO typography_published (id, version, config_json, published_at, published_by, notes) VALUES ('active', 1, ?, ?, ?, 'Reset to factory defaults') ON CONFLICT(id) DO UPDATE SET version = 1, config_json = excluded.config_json, published_at = excluded.published_at, published_by = excluded.published_by, notes = excluded.notes").bind(jsonStr, now, user.email),
    db.prepare("INSERT INTO typography_drafts (id, config_json, updated_at, updated_by) VALUES ('active', ?, ?, ?) ON CONFLICT(id) DO UPDATE SET config_json = excluded.config_json, updated_at = excluded.updated_at, updated_by = excluded.updated_by").bind(jsonStr, now, user.email),
    db.prepare("INSERT INTO typography_audit_log (id, action, performed_by, version, details, timestamp) VALUES (?, 'reset', ?, 1, 'Reset to factory defaults', ?)").bind(`audit-${now}`, user.email, now)
  ]);

  return c.json({ success: true, data: defaultConfig });
});
