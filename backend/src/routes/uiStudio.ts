// ============================================================================
// EVE UI Studio / App Builder API Routes
// ============================================================================

import { Hono } from "hono";
import { requireAdmin } from "../middleware/authMiddleware";
import {
  AuthUser,
  Env,
  UiStudioPublishedRow,
  UiStudioDraftRow,
  UiStudioVersionRow,
  UiStudioAuditLogRow,
} from "../types";

export const publicUiStudioRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();
export const adminUiStudioRoutes = new Hono<{ Bindings: Env; Variables: { user: AuthUser } }>();

// Guard all admin routes with admin authorization
adminUiStudioRoutes.use("*", requireAdmin);

const COLOR_HEX_REGEX = /^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/;

export function validateUiStudioConfig(config: any): { valid: boolean; errors: string[] } {
  const errors: string[] = [];

  if (!config || typeof config !== "object") {
    return { valid: false, errors: ["Configuration must be an object"] };
  }

  // Validate Branding
  if (config.branding && typeof config.branding === "object") {
    if (config.branding.brandColor && !COLOR_HEX_REGEX.test(config.branding.brandColor)) {
      errors.push(`Invalid brandColor '${config.branding.brandColor}' in branding`);
    }
    if (config.branding.globalBackgroundColor && !COLOR_HEX_REGEX.test(config.branding.globalBackgroundColor)) {
      errors.push(`Invalid globalBackgroundColor '${config.branding.globalBackgroundColor}' in branding`);
    }
  }

  // Validate Design System
  if (config.designSystem && typeof config.designSystem === "object") {
    const ds = config.designSystem;
    const colorFields = [
      "appBackground", "surfaceBackground", "textPrimary", "textSecondary",
      "accentColor", "successColor", "warningColor", "errorColor", "borderColor", "dividerColor"
    ];
    for (const f of colorFields) {
      if (ds[f] && !COLOR_HEX_REGEX.test(ds[f])) {
        errors.push(`Invalid ${f} '${ds[f]}' in designSystem`);
      }
    }
  }

  const screens = config.screens;
  if (screens !== undefined) {
    if (typeof screens !== "object" || screens === null) {
      errors.push("screens must be a dictionary of screen objects");
      return { valid: false, errors };
    }

    for (const [screenKey, screen] of Object.entries<any>(screens)) {
      if (!screen || typeof screen !== "object") {
        errors.push(`Screen '${screenKey}' must be an object`);
        continue;
      }

      if (screen.backgroundColor && !COLOR_HEX_REGEX.test(screen.backgroundColor)) {
        errors.push(`Invalid backgroundColor '${screen.backgroundColor}' in screen '${screenKey}'`);
      }

      const components = screen.components;
      if (components !== undefined) {
        if (typeof components !== "object" || components === null) {
          errors.push(`Screen '${screenKey}.components' must be an object`);
          continue;
        }

        for (const [compKey, comp] of Object.entries<any>(components)) {
          if (!comp || typeof comp !== "object") {
            errors.push(`Component '${screenKey}.${compKey}' must be an object`);
            continue;
          }

          // Check Appearance
          if (comp.appearance && typeof comp.appearance === "object") {
            const app = comp.appearance;
            if (app.backgroundColor && !COLOR_HEX_REGEX.test(app.backgroundColor)) {
              errors.push(`Invalid background color '${app.backgroundColor}' in ${screenKey}.${compKey}`);
            }
            if (app.strokeColor && !COLOR_HEX_REGEX.test(app.strokeColor)) {
              errors.push(`Invalid stroke color '${app.strokeColor}' in ${screenKey}.${compKey}`);
            }
            if (app.cornerRadius !== undefined && (typeof app.cornerRadius !== "number" || app.cornerRadius < 0 || app.cornerRadius > 120)) {
              errors.push(`Invalid cornerRadius in ${screenKey}.${compKey} (must be 0-120)`);
            }
            if (app.strokeWidth !== undefined && (typeof app.strokeWidth !== "number" || app.strokeWidth < 0 || app.strokeWidth > 30)) {
              errors.push(`Invalid strokeWidth in ${screenKey}.${compKey} (must be 0-30)`);
            }
            if (app.elevation !== undefined && (typeof app.elevation !== "number" || app.elevation < 0 || app.elevation > 40)) {
              errors.push(`Invalid elevation in ${screenKey}.${compKey} (must be 0-40)`);
            }
            if (app.opacity !== undefined && (typeof app.opacity !== "number" || app.opacity < 0 || app.opacity > 1)) {
              errors.push(`Invalid opacity in ${screenKey}.${compKey} (must be between 0.0 and 1.0)`);
            }
          }

          // Check Material / Glass (including Blur control)
          if (comp.material && typeof comp.material === "object") {
            const mat = comp.material;
            if (mat.blurRadius !== undefined && (typeof mat.blurRadius !== "number" || mat.blurRadius < 0 || mat.blurRadius > 50)) {
              errors.push(`Invalid blurRadius in ${screenKey}.${compKey} (must be 0-50)`);
            }
            if (mat.materialOpacity !== undefined && (typeof mat.materialOpacity !== "number" || mat.materialOpacity < 0 || mat.materialOpacity > 1)) {
              errors.push(`Invalid materialOpacity in ${screenKey}.${compKey} (must be 0.0 to 1.0)`);
            }
            if (mat.tintColor && !COLOR_HEX_REGEX.test(mat.tintColor)) {
              errors.push(`Invalid tintColor '${mat.tintColor}' in ${screenKey}.${compKey}`);
            }
            if (mat.tintOpacity !== undefined && (typeof mat.tintOpacity !== "number" || mat.tintOpacity < 0 || mat.tintOpacity > 1)) {
              errors.push(`Invalid tintOpacity in ${screenKey}.${compKey} (must be 0.0 to 1.0)`);
            }
          }

          // Check Animation
          if (comp.animation && typeof comp.animation === "object") {
            const anim = comp.animation;
            if (anim.enabled !== undefined && typeof anim.enabled !== "boolean") {
              errors.push(`Invalid animation.enabled in ${screenKey}.${compKey} (must be boolean)`);
            }
            if (anim.durationMs !== undefined && (typeof anim.durationMs !== "number" || anim.durationMs < 0 || anim.durationMs > 10000)) {
              errors.push(`Invalid animation.durationMs in ${screenKey}.${compKey} (must be 0-10000 ms)`);
            }
            if (anim.delayMs !== undefined && (typeof anim.delayMs !== "number" || anim.delayMs < 0 || anim.delayMs > 10000)) {
              errors.push(`Invalid animation.delayMs in ${screenKey}.${compKey} (must be 0-10000 ms)`);
            }
          }

          // Check Layout
          if (comp.layout && typeof comp.layout === "object") {
            const lay = comp.layout;
            const dimProps = [
              "marginTop", "marginBottom", "marginStart", "marginEnd",
              "paddingTop", "paddingBottom", "paddingStart", "paddingEnd",
            ];
            for (const prop of dimProps) {
              if (lay[prop] !== undefined && (typeof lay[prop] !== "number" || lay[prop] < 0 || lay[prop] > 300)) {
                errors.push(`Invalid ${prop} in ${screenKey}.${compKey} (must be 0-300 dp)`);
              }
            }
          }

          // Check Typography
          if (comp.typography && typeof comp.typography === "object") {
            const typo = comp.typography;
            if (typo.textColor && !COLOR_HEX_REGEX.test(typo.textColor)) {
              errors.push(`Invalid textColor '${typo.textColor}' in ${screenKey}.${compKey}`);
            }
            if (typo.textSize !== undefined && (typeof typo.textSize !== "number" || typo.textSize < 6 || typo.textSize > 96)) {
              errors.push(`Invalid textSize in ${screenKey}.${compKey} (must be 6-96 sp)`);
            }
          }

          // Check Visibility
          if (comp.visible !== undefined && typeof comp.visible !== "boolean") {
            errors.push(`Invalid visible property in ${screenKey}.${compKey} (must be boolean)`);
          }
        }
      }
    }
  }

  return { valid: errors.length === 0, errors };
}

// ----------------------------------------------------------------------------
// Public Endpoint: Fetch currently published configuration
// ----------------------------------------------------------------------------
publicUiStudioRoutes.get("/published", async (c) => {
  const db = c.env.DB;
  try {
    const row = await db
      .prepare("SELECT * FROM ui_studio_published WHERE id = 'active'")
      .first<UiStudioPublishedRow>();

    if (!row || !row.config_json) {
      c.header("Cache-Control", "public, max-age=60");
      return c.json({
        success: true,
        data: {
          version: 0,
          publishedAt: 0,
          publishedBy: "",
          notes: "",
          config: null,
        },
      });
    }

    const etag = `W/"ui-studio-v${row.version}-${row.published_at}"`;
    const clientEtag = c.req.header("if-none-match");

    if (clientEtag && clientEtag === etag) {
      return new Response(null, {
        status: 304,
        headers: {
          ETag: etag,
          "Cache-Control": "public, max-age=60",
        },
      });
    }

    const config = JSON.parse(row.config_json);
    c.header("ETag", etag);
    c.header("Cache-Control", "public, max-age=60");
    return c.json({
      success: true,
      data: {
        version: row.version,
        publishedAt: row.published_at,
        publishedBy: row.published_by,
        notes: row.notes || "",
        config,
      },
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// ----------------------------------------------------------------------------
// Admin Endpoints: Draft, Publish, Versions, Restore, Reset, Audit Log
// ----------------------------------------------------------------------------

// GET /api/admin/ui-studio/draft - Retrieve active draft configuration
adminUiStudioRoutes.get("/draft", async (c) => {
  const db = c.env.DB;
  try {
    const draftRow = await db
      .prepare("SELECT * FROM ui_studio_drafts WHERE id = 'draft'")
      .first<UiStudioDraftRow>();

    if (draftRow && draftRow.config_json) {
      return c.json({
        success: true,
        data: {
          config: JSON.parse(draftRow.config_json),
          updatedAt: draftRow.updated_at,
          updatedBy: draftRow.updated_by,
          source: "draft",
        },
      });
    }

    // Fall back to active published config if available
    const publishedRow = await db
      .prepare("SELECT * FROM ui_studio_published WHERE id = 'active'")
      .first<UiStudioPublishedRow>();

    if (publishedRow && publishedRow.config_json) {
      return c.json({
        success: true,
        data: {
          config: JSON.parse(publishedRow.config_json),
          updatedAt: publishedRow.published_at,
          updatedBy: publishedRow.published_by,
          source: "published",
        },
      });
    }

    return c.json({
      success: true,
      data: {
        config: { screens: {} },
        updatedAt: 0,
        updatedBy: "",
        source: "default",
      },
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// PUT /api/admin/ui-studio/draft - Save draft configuration
adminUiStudioRoutes.put("/draft", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));
  const config = body.config;
  const baseRevision = body.baseRevision;
  const force = body.force === true;

  const validation = validateUiStudioConfig(config);
  if (!validation.valid) {
    return c.json({ success: false, error: "Validation failed", details: validation.errors }, 400);
  }

  // Optimistic Concurrency Check (Section 73)
  if (baseRevision && !force) {
    const existing = await db
      .prepare("SELECT config_json, updated_at, updated_by FROM ui_studio_drafts WHERE id = 'draft'")
      .first<UiStudioDraftRow>();

    if (existing && existing.config_json) {
      try {
        const parsed = JSON.parse(existing.config_json);
        if (parsed.revision && parsed.revision !== baseRevision) {
          return c.json({
            success: false,
            error: "NEWER_DRAFT_EXISTS",
            message: "A newer draft exists on the server. Please reload or confirm overwrite.",
            currentRevision: parsed.revision,
            serverDraft: parsed,
            updatedAt: existing.updated_at,
            updatedBy: existing.updated_by
          }, 409);
        }
      } catch (_) {}
    }
  }

  const now = Date.now();
  const email = user.email || "admin";
  const newRevision = `rev-${now}-${Math.random().toString(36).substring(2, 8)}`;
  config.revision = newRevision;
  config.updatedAt = now;
  const jsonStr = JSON.stringify(config);

  await db
    .prepare(
      `INSERT INTO ui_studio_drafts (id, config_json, updated_at, updated_by)
       VALUES ('draft', ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         config_json = excluded.config_json,
         updated_at = excluded.updated_at,
         updated_by = excluded.updated_by`
    )
    .bind(jsonStr, now, email)
    .run();

  // Audit log entry
  const auditId = `audit-${now}-${Math.random().toString(36).substring(2, 7)}`;
  await db
    .prepare(
      "INSERT INTO ui_studio_audit_log (id, action, performed_by, version, details, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
    )
    .bind(auditId, "draft_saved", email, null, "Saved working draft", now)
    .run();

  return c.json({
    success: true,
    data: {
      config,
      revision: newRevision,
      updatedAt: now,
      updatedBy: email,
    },
  });
});

// POST /api/admin/ui-studio/publish - Publish draft or provided configuration
adminUiStudioRoutes.post("/publish", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));
  const notes = typeof body.notes === "string" ? body.notes.trim() : "";

  let configToPublish = body.config;

  // If config not passed in body, load from draft
  if (!configToPublish) {
    const draftRow = await db
      .prepare("SELECT * FROM ui_studio_drafts WHERE id = 'draft'")
      .first<UiStudioDraftRow>();
    if (draftRow && draftRow.config_json) {
      configToPublish = JSON.parse(draftRow.config_json);
    }
  }

  if (!configToPublish) {
    return c.json({ success: false, error: "No configuration found to publish" }, 400);
  }

  const validation = validateUiStudioConfig(configToPublish);
  if (!validation.valid) {
    return c.json({ success: false, error: "Validation failed", details: validation.errors }, 400);
  }

  const now = Date.now();
  const email = user.email || "admin";

  // Determine next version number
  const maxRow = await db
    .prepare("SELECT MAX(version) as max_v FROM ui_studio_versions")
    .first<{ max_v: number | null }>();
  const nextVersion = (maxRow?.max_v || 0) + 1;

  // Stamp version into config
  configToPublish.version = nextVersion;
  configToPublish.publishedAt = now;
  const jsonStr = JSON.stringify(configToPublish);

  // 1. Snapshot into immutable versions table
  const insertVersionStmt = db
    .prepare(
      "INSERT INTO ui_studio_versions (version, config_json, created_at, created_by, notes) VALUES (?, ?, ?, ?, ?)"
    )
    .bind(nextVersion, jsonStr, now, email, notes);

  // 2. Update active published table
  const upsertPublishedStmt = db
    .prepare(
      `INSERT INTO ui_studio_published (id, version, config_json, published_at, published_by, notes)
       VALUES ('active', ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET
         version = excluded.version,
         config_json = excluded.config_json,
         published_at = excluded.published_at,
         published_by = excluded.published_by,
         notes = excluded.notes`
    )
    .bind(nextVersion, jsonStr, now, email, notes);

  // 3. UI Studio audit log
  const auditId = `audit-${now}-${Math.random().toString(36).substring(2, 7)}`;
  const studioAuditStmt = db
    .prepare(
      "INSERT INTO ui_studio_audit_log (id, action, performed_by, version, details, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
    )
    .bind(auditId, "published", email, nextVersion, notes || `Published v${nextVersion}`, now);

  // 4. Main Admin Audit log
  const adminAuditStmt = db
    .prepare(
      "INSERT INTO admin_audit_log (id, action_type, description, admin_email, timestamp) VALUES (?, ?, ?, ?, ?)"
    )
    .bind(
      `audit-general-${now}-${Math.random().toString(36).substring(2, 7)}`,
      "UI_STUDIO_PUBLISH",
      `Published EVE UI Studio v${nextVersion}${notes ? `: ${notes}` : ""}`,
      email,
      now
    );

  if (typeof (db as any).batch === "function") {
    await (db as any).batch([insertVersionStmt, upsertPublishedStmt, studioAuditStmt, adminAuditStmt]);
  } else {
    await insertVersionStmt.run();
    await upsertPublishedStmt.run();
    await studioAuditStmt.run();
    await adminAuditStmt.run();
  }

  return c.json({
    success: true,
    data: {
      version: nextVersion,
      publishedAt: now,
      publishedBy: email,
      notes,
    },
  });
});

// GET /api/admin/ui-studio/versions - List historical versions
adminUiStudioRoutes.get("/versions", async (c) => {
  const db = c.env.DB;
  try {
    const { results } = await db
      .prepare(
        "SELECT version, created_at, created_by, notes FROM ui_studio_versions ORDER BY version DESC LIMIT 50"
      )
      .all<UiStudioVersionRow>();

    return c.json({
      success: true,
      data: results || [],
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});

// POST /api/admin/ui-studio/restore/:versionId - Rollback/restore a historical version
adminUiStudioRoutes.post("/restore/:versionId", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const versionId = parseInt(c.req.param("versionId"), 10);
  const body = await c.req.json().catch(() => ({}));
  const target = body.target === "publish" ? "publish" : "draft";

  if (isNaN(versionId) || versionId <= 0) {
    return c.json({ success: false, error: "Invalid versionId" }, 400);
  }

  const verRow = await db
    .prepare("SELECT * FROM ui_studio_versions WHERE version = ?")
    .bind(versionId)
    .first<UiStudioVersionRow>();

  if (!verRow || !verRow.config_json) {
    return c.json({ success: false, error: `Version ${versionId} not found` }, 404);
  }

  const now = Date.now();
  const email = user.email || "admin";

  if (target === "draft") {
    const upsertDraftStmt = db
      .prepare(
        `INSERT INTO ui_studio_drafts (id, config_json, updated_at, updated_by)
         VALUES ('draft', ?, ?, ?)
         ON CONFLICT(id) DO UPDATE SET
           config_json = excluded.config_json,
           updated_at = excluded.updated_at,
           updated_by = excluded.updated_by`
      )
      .bind(verRow.config_json, now, email);

    const auditId = `audit-${now}-${Math.random().toString(36).substring(2, 7)}`;
    const auditStmt = db
      .prepare(
        "INSERT INTO ui_studio_audit_log (id, action, performed_by, version, details, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
      )
      .bind(auditId, "restored", email, versionId, `Restored v${versionId} to draft`, now);

    if (typeof (db as any).batch === "function") {
      await (db as any).batch([upsertDraftStmt, auditStmt]);
    } else {
      await upsertDraftStmt.run();
      await auditStmt.run();
    }

    return c.json({
      success: true,
      message: `Restored version ${versionId} to draft`,
    });
  } else {
    // Restore directly to published -> bumps to next version number
    const maxRow = await db
      .prepare("SELECT MAX(version) as max_v FROM ui_studio_versions")
      .first<{ max_v: number | null }>();
    const nextVersion = (maxRow?.max_v || 0) + 1;

    const restoredConfig = JSON.parse(verRow.config_json);
    restoredConfig.version = nextVersion;
    restoredConfig.publishedAt = now;
    const jsonStr = JSON.stringify(restoredConfig);
    const restoreNotes = `Restored from v${versionId}`;

    const insertVerStmt = db
      .prepare(
        "INSERT INTO ui_studio_versions (version, config_json, created_at, created_by, notes) VALUES (?, ?, ?, ?, ?)"
      )
      .bind(nextVersion, jsonStr, now, email, restoreNotes);

    const upsertPubStmt = db
      .prepare(
        `INSERT INTO ui_studio_published (id, version, config_json, published_at, published_by, notes)
         VALUES ('active', ?, ?, ?, ?, ?)
         ON CONFLICT(id) DO UPDATE SET
           version = excluded.version,
           config_json = excluded.config_json,
           published_at = excluded.published_at,
           published_by = excluded.published_by,
           notes = excluded.notes`
      )
      .bind(nextVersion, jsonStr, now, email, restoreNotes);

    const auditId = `audit-${now}-${Math.random().toString(36).substring(2, 7)}`;
    const auditStmt = db
      .prepare(
        "INSERT INTO ui_studio_audit_log (id, action, performed_by, version, details, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
      )
      .bind(auditId, "restored", email, nextVersion, `Restored v${versionId} as v${nextVersion}`, now);

    if (typeof (db as any).batch === "function") {
      await (db as any).batch([insertVerStmt, upsertPubStmt, auditStmt]);
    } else {
      await insertVerStmt.run();
      await upsertPubStmt.run();
      await auditStmt.run();
    }

    return c.json({
      success: true,
      message: `Restored version ${versionId} as new published v${nextVersion}`,
      data: { version: nextVersion },
    });
  }
});

// POST /api/admin/ui-studio/reset - Clear configuration back to native defaults
adminUiStudioRoutes.post("/reset", async (c) => {
  const user = c.get("user");
  const db = c.env.DB;
  const body = await c.req.json().catch(() => ({}));
  const target = body.target || "draft"; // "draft" | "published" | "all"
  const now = Date.now();
  const email = user.email || "admin";

  if (target === "draft" || target === "all") {
    await db.prepare("DELETE FROM ui_studio_drafts WHERE id = 'draft'").run();
  }

  if (target === "published" || target === "all") {
    await db.prepare("DELETE FROM ui_studio_published WHERE id = 'active'").run();
  }

  const auditId = `audit-${now}-${Math.random().toString(36).substring(2, 7)}`;
  await db
    .prepare(
      "INSERT INTO ui_studio_audit_log (id, action, performed_by, version, details, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
    )
    .bind(auditId, "reset", email, null, `Reset UI Studio config target: ${target}`, now)
    .run();

  return c.json({
    success: true,
    message: `Reset completed for ${target}`,
  });
});

// GET /api/admin/ui-studio/audit-log - View UI Studio audit log
adminUiStudioRoutes.get("/audit-log", async (c) => {
  const db = c.env.DB;
  try {
    const { results } = await db
      .prepare("SELECT * FROM ui_studio_audit_log ORDER BY timestamp DESC LIMIT 100")
      .all<UiStudioAuditLogRow>();

    return c.json({
      success: true,
      data: results || [],
    });
  } catch (err: any) {
    return c.json({ success: false, error: err.message }, 500);
  }
});
