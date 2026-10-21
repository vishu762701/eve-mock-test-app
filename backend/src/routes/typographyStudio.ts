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

const ALLOWED_ROLES = new Set([
  "large_title",
  "title_1",
  "title_2",
  "title_3",
  "headline",
  "body",
  "callout",
  "subheadline",
  "footnote",
  "caption_1",
  "caption_2",
  "button",
  "tab",
  "chip"
]);

const ALLOWED_WEIGHTS = new Set([100, 200, 300, 400, 500, 600, 700, 800, 900]);
const KEY_REGEX = /^[a-zA-Z0-9_]{2,60}$/;
const MAX_PAYLOAD_BYTES = 51200; // 50KB

export function createDefaultConfig() {
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
      caption_2: { text_size_sp: 11, line_height_sp: 13, weight: 500 },
      button: { text_size_sp: 17, line_height_sp: 22, weight: 600 },
      tab: { text_size_sp: 14, line_height_sp: 18, weight: 500 },
      chip: { text_size_sp: 12, line_height_sp: 16, weight: 600 }
    },
    screen_overrides: {},
    component_overrides: {},
    element_overrides: {},
    version: 1,
    status: "published",
    published_at: Date.now(),
    notes: "Factory default iOS 26 Balanced preset"
  };
}

function validateStyleRule(rule: any, context: string): { valid: boolean; error?: string } {
  if (!rule || typeof rule !== "object") {
    return { valid: false, error: `${context}: style rule must be an object` };
  }

  if (rule.font_family !== undefined && rule.font_family !== null) {
    if (typeof rule.font_family !== "string" || !ALLOWED_FONT_FAMILIES.has(rule.font_family.toLowerCase())) {
      return { valid: false, error: `${context}: invalid font_family '${rule.font_family}'` };
    }
  }

  if (rule.text_size_sp !== undefined && rule.text_size_sp !== null) {
    if (typeof rule.text_size_sp !== "number" || isNaN(rule.text_size_sp) || rule.text_size_sp < 8 || rule.text_size_sp > 60) {
      return { valid: false, error: `${context}: text_size_sp must be between 8 and 60sp (got ${rule.text_size_sp})` };
    }
  }

  if (rule.weight !== undefined && rule.weight !== null) {
    if (typeof rule.weight !== "number" || !ALLOWED_WEIGHTS.has(rule.weight)) {
      return { valid: false, error: `${context}: weight must be one of [100, 200, 300, 400, 500, 600, 700, 800, 900] (got ${rule.weight})` };
    }
  }

  if (rule.line_height_sp !== undefined && rule.line_height_sp !== null) {
    if (typeof rule.line_height_sp !== "number" || isNaN(rule.line_height_sp) || rule.line_height_sp > 80) {
      return { valid: false, error: `${context}: line_height_sp must be <= 80sp (got ${rule.line_height_sp})` };
    }
    if (rule.text_size_sp !== undefined && rule.text_size_sp !== null) {
      if (rule.line_height_sp < rule.text_size_sp) {
        return { valid: false, error: `${context}: line_height_sp (${rule.line_height_sp}) cannot be smaller than text_size_sp (${rule.text_size_sp})` };
      }
    }
  }

  if (rule.letter_spacing_em !== undefined && rule.letter_spacing_em !== null) {
    if (typeof rule.letter_spacing_em !== "number" || isNaN(rule.letter_spacing_em) || rule.letter_spacing_em < -0.10 || rule.letter_spacing_em > 0.50) {
      return { valid: false, error: `${context}: letter_spacing_em must be between -0.10 and 0.50 (got ${rule.letter_spacing_em})` };
    }
  }

  return { valid: true };
}

export function validateConfig(config: any): { valid: boolean; error?: string } {
  if (!config || typeof config !== "object") {
    return { valid: false, error: "Configuration must be a valid JSON object" };
  }

  const jsonStr = JSON.stringify(config);
  if (jsonStr.length > MAX_PAYLOAD_BYTES) {
    return { valid: false, error: `Payload size exceeds ${MAX_PAYLOAD_BYTES / 1024}KB limit` };
  }

  if (config.font_family && !ALLOWED_FONT_FAMILIES.has(String(config.font_family).toLowerCase())) {
    return { valid: false, error: `Invalid font family: '${config.font_family}'. Must be one of: inter, poppins, roboto, noto_sans.` };
  }
  if (config.preset_id && !ALLOWED_PRESETS.has(String(config.preset_id).toLowerCase())) {
    return { valid: false, error: `Invalid preset ID: '${config.preset_id}'` };
  }

  if (config.global_styles) {
    if (typeof config.global_styles !== "object") {
      return { valid: false, error: "global_styles must be an object" };
    }
    for (const [role, rule] of Object.entries(config.global_styles)) {
      if (!ALLOWED_ROLES.has(role.toLowerCase())) {
        return { valid: false, error: `Invalid global style role: '${role}'. Must be an approved semantic typography role.` };
      }
      const ruleVal = validateStyleRule(rule, `global_styles.${role}`);
      if (!ruleVal.valid) return ruleVal;
    }
  }

  if (config.screen_overrides) {
    if (typeof config.screen_overrides !== "object") {
      return { valid: false, error: "screen_overrides must be an object" };
    }
    for (const [screenKey, elementMap] of Object.entries(config.screen_overrides)) {
      if (!KEY_REGEX.test(screenKey)) {
        return { valid: false, error: `Invalid screen key format: '${screenKey}'` };
      }
      if (!elementMap || typeof elementMap !== "object") {
        return { valid: false, error: `screen_overrides.${screenKey} must be an object mapping element keys to styles` };
      }
      for (const [elemKey, rule] of Object.entries(elementMap as Record<string, any>)) {
        if (!KEY_REGEX.test(elemKey)) {
          return { valid: false, error: `Invalid element key format: '${elemKey}' in screen '${screenKey}'` };
        }
        const ruleVal = validateStyleRule(rule, `screen_overrides.${screenKey}.${elemKey}`);
        if (!ruleVal.valid) return ruleVal;
      }
    }
  }

  if (config.component_overrides) {
    if (typeof config.component_overrides !== "object") {
      return { valid: false, error: "component_overrides must be an object" };
    }
    for (const [compKey, rule] of Object.entries(config.component_overrides)) {
      if (!KEY_REGEX.test(compKey)) {
        return { valid: false, error: `Invalid component key format: '${compKey}'` };
      }
      const ruleVal = validateStyleRule(rule, `component_overrides.${compKey}`);
      if (!ruleVal.valid) return ruleVal;
    }
  }

  if (config.element_overrides) {
    if (typeof config.element_overrides !== "object") {
      return { valid: false, error: "element_overrides must be an object" };
    }
    for (const [elemKey, rule] of Object.entries(config.element_overrides)) {
      if (!KEY_REGEX.test(elemKey)) {
        return { valid: false, error: `Invalid element key format: '${elemKey}'` };
      }
      const ruleVal = validateStyleRule(rule, `element_overrides.${elemKey}`);
      if (!ruleVal.valid) return ruleVal;
    }
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
  const body = await c.req.json().catch(() => null);

  if (!body) {
    return c.json({ success: false, error: "Invalid JSON body" }, 400);
  }

  // Version conflict detection for concurrent admin edits
  if (typeof body.base_version === "number") {
    const curPub = await db.prepare("SELECT version FROM typography_published WHERE id = 'active'").first<{ version: number }>();
    if (curPub && curPub.version > body.base_version) {
      return c.json({
        success: false,
        error: `Version conflict: published version is at v${curPub.version} while base version was v${body.base_version}. Please refresh before saving draft.`
      }, 409);
    }
  }

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
  if (jsonStr.length > MAX_PAYLOAD_BYTES) {
    return c.json({ success: false, error: `Payload size exceeds ${MAX_PAYLOAD_BYTES / 1024}KB limit` }, 413);
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

  // Version conflict check
  const curRow = await db.prepare("SELECT version FROM typography_published WHERE id = 'active'").first<{ version: number }>();
  if (typeof body?.base_version === "number" && curRow && curRow.version > body.base_version) {
    return c.json({
      success: false,
      error: `Version conflict: published version is at v${curRow.version} while base version was v${body.base_version}. Please refresh before publishing.`
    }, 409);
  }

  const draftRow = await db.prepare("SELECT config_json FROM typography_drafts WHERE id = 'active'").first<{ config_json: string }>();
  let configToPublish = draftRow?.config_json ? JSON.parse(draftRow.config_json) : null;

  if (!configToPublish && body && (body.font_family || body.global_styles)) {
    configToPublish = body;
  }

  // Refuse fallback publishing of an absent draft without explicit validated user intent
  if (!configToPublish) {
    return c.json({
      success: false,
      error: "No draft found to publish and no valid configuration provided. Please save a draft first."
    }, 400);
  }

  const validation = validateConfig(configToPublish);
  if (!validation.valid) {
    return c.json({ success: false, error: validation.error }, 400);
  }

  // Monotonically increasing version
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
  if (jsonStr.length > MAX_PAYLOAD_BYTES) {
    return c.json({ success: false, error: `Payload size exceeds ${MAX_PAYLOAD_BYTES / 1024}KB limit` }, 413);
  }

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
  const body = await c.req.json().catch(() => ({}));
  const { version, confirmed } = body;

  // Confirmation required for destructive rollback action
  if (confirmed !== true) {
    return c.json({ success: false, error: "Confirmation required to rollback typography configuration (must include confirmed: true)" }, 400);
  }

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

  const curRow = await db.prepare("SELECT version FROM typography_published WHERE id = 'active'").first<{ version: number }>();
  const nextVersion = (curRow?.version || 0) + 1;
  const now = Date.now();

  const restoredConfig = JSON.parse(targetRow.config_json);
  restoredConfig.version = nextVersion;
  restoredConfig.status = "published";
  restoredConfig.published_at = now;
  restoredConfig.published_by = user.email;
  restoredConfig.notes = `Rolled back to snapshot from version ${version}`;

  const jsonStr = JSON.stringify(restoredConfig);

  await db.batch([
    db.prepare("INSERT INTO typography_published (id, version, config_json, published_at, published_by, notes) VALUES ('active', ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET version = excluded.version, config_json = excluded.config_json, published_at = excluded.published_at, published_by = excluded.published_by, notes = excluded.notes").bind(nextVersion, jsonStr, now, user.email, restoredConfig.notes),
    db.prepare("INSERT INTO typography_versions (version, config_json, created_at, created_by, notes) VALUES (?, ?, ?, ?, ?)").bind(nextVersion, jsonStr, now, user.email, restoredConfig.notes),
    db.prepare("INSERT INTO typography_drafts (id, config_json, updated_at, updated_by) VALUES ('active', ?, ?, ?) ON CONFLICT(id) DO UPDATE SET config_json = excluded.config_json, updated_at = excluded.updated_at, updated_by = excluded.updated_by").bind(jsonStr, now, user.email),
    db.prepare("INSERT INTO typography_audit_log (id, action, performed_by, version, details, timestamp) VALUES (?, 'rollback', ?, ?, ?, ?)").bind(`audit-${now}`, user.email, nextVersion, restoredConfig.notes, now)
  ]);

  return c.json({ success: true, data: restoredConfig });
});

// POST /api/admin/typography/reset
adminTypographyRoutes.post("/reset", async (c) => {
  const db = c.env.DB;
  const user = c.get("user");
  const body = await c.req.json().catch(() => ({}));

  // Confirmation required for destructive factory reset
  if (body?.confirmed !== true) {
    return c.json({ success: false, error: "Confirmation required to reset typography configuration (must include confirmed: true)" }, 400);
  }

  const curRow = await db.prepare("SELECT version FROM typography_published WHERE id = 'active'").first<{ version: number }>();
  const nextVersion = (curRow?.version || 0) + 1;
  const now = Date.now();

  const defaultConfig = createDefaultConfig();
  defaultConfig.version = nextVersion;
  defaultConfig.published_at = now;
  (defaultConfig as any).published_by = user.email;
  defaultConfig.notes = "Reset to factory defaults (iOS 26 Balanced)";

  const jsonStr = JSON.stringify(defaultConfig);

  await db.batch([
    db.prepare("INSERT INTO typography_published (id, version, config_json, published_at, published_by, notes) VALUES ('active', ?, ?, ?, ?, ?) ON CONFLICT(id) DO UPDATE SET version = excluded.version, config_json = excluded.config_json, published_at = excluded.published_at, published_by = excluded.published_by, notes = excluded.notes").bind(nextVersion, jsonStr, now, user.email, defaultConfig.notes),
    db.prepare("INSERT INTO typography_versions (version, config_json, created_at, created_by, notes) VALUES (?, ?, ?, ?, ?)").bind(nextVersion, jsonStr, now, user.email, defaultConfig.notes),
    db.prepare("INSERT INTO typography_drafts (id, config_json, updated_at, updated_by) VALUES ('active', ?, ?, ?) ON CONFLICT(id) DO UPDATE SET config_json = excluded.config_json, updated_at = excluded.updated_at, updated_by = excluded.updated_by").bind(jsonStr, now, user.email),
    db.prepare("INSERT INTO typography_audit_log (id, action, performed_by, version, details, timestamp) VALUES (?, 'reset', ?, ?, ?, ?)").bind(`audit-${now}`, user.email, nextVersion, defaultConfig.notes, now)
  ]);

  return c.json({ success: true, data: defaultConfig });
});
