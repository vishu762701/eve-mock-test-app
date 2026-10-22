import { Context, Hono } from "hono";
import { AuthUser, Env } from "../types";

export const typographyRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

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
