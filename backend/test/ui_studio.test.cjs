const assert = require("node:assert/strict");
const fs = require("node:fs");
const test = require("node:test");
const ts = require("typescript");

require.extensions[".ts"] = (module, filename) => {
  const source = fs.readFileSync(filename, "utf8");
  const output = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
    fileName: filename,
  }).outputText;
  module._compile(output, filename);
};

const { Hono } = require("hono");
const { publicUiStudioRoutes, adminUiStudioRoutes, validateUiStudioConfig } = require("../src/routes/uiStudio");

class MockUiStudioD1 {
  published = [];
  drafts = [];
  versions = [];
  auditLog = [];
  adminAuditLog = [];

  async batch(stmts) {
    const results = [];
    for (const stmt of stmts) {
      results.push(await stmt.run());
    }
    return results;
  }

  prepare(sql) {
    const stmt = {
      values: [],
      bind: (...args) => {
        stmt.values = args;
        return stmt;
      },
      first: async () => {
        if (sql.includes("FROM ui_studio_published WHERE id = 'active'")) {
          return this.published.find((r) => r.id === "active") || null;
        }
        if (sql.includes("FROM ui_studio_drafts WHERE id = 'draft'")) {
          return this.drafts.find((r) => r.id === "draft") || null;
        }
        if (sql.includes("SELECT MAX(version) as max_v FROM ui_studio_versions")) {
          const max = this.versions.length ? Math.max(...this.versions.map((v) => v.version)) : 0;
          return { max_v: max };
        }
        if (sql.includes("FROM ui_studio_versions WHERE version = ?")) {
          const [v] = stmt.values;
          return this.versions.find((item) => item.version === v) || null;
        }
        return null;
      },
      all: async () => {
        if (sql.includes("FROM ui_studio_versions")) {
          return { results: [...this.versions].sort((a, b) => b.version - a.version) };
        }
        if (sql.includes("FROM ui_studio_audit_log")) {
          return { results: [...this.auditLog].sort((a, b) => b.timestamp - a.timestamp) };
        }
        return { results: [] };
      },
      run: async () => {
        if (sql.includes("INSERT INTO ui_studio_drafts")) {
          const [config_json, updated_at, updated_by] = stmt.values;
          const idx = this.drafts.findIndex((d) => d.id === "draft");
          const row = { id: "draft", config_json, updated_at, updated_by };
          if (idx >= 0) this.drafts[idx] = row;
          else this.drafts.push(row);
        } else if (sql.includes("INSERT INTO ui_studio_versions")) {
          const [version, config_json, created_at, created_by, notes] = stmt.values;
          this.versions.push({ version, config_json, created_at, created_by, notes });
        } else if (sql.includes("INSERT INTO ui_studio_published")) {
          const [version, config_json, published_at, published_by, notes] = stmt.values;
          const idx = this.published.findIndex((p) => p.id === "active");
          const row = { id: "active", version, config_json, published_at, published_by, notes };
          if (idx >= 0) this.published[idx] = row;
          else this.published.push(row);
        } else if (sql.includes("INSERT INTO ui_studio_audit_log")) {
          const [id, action, performed_by, version, details, timestamp] = stmt.values;
          this.auditLog.push({ id, action, performed_by, version, details, timestamp });
        } else if (sql.includes("INSERT INTO admin_audit_log")) {
          const [id, action_type, description, admin_email, timestamp] = stmt.values;
          this.adminAuditLog.push({ id, action_type, description, admin_email, timestamp });
        } else if (sql.includes("DELETE FROM ui_studio_drafts")) {
          this.drafts = this.drafts.filter((d) => d.id !== "draft");
        } else if (sql.includes("DELETE FROM ui_studio_published")) {
          this.published = this.published.filter((p) => p.id !== "active");
        }
        return { success: true };
      },
    };
    return stmt;
  }
}

function createApp(db, { isAdmin = false, isStudent = true } = {}) {
  const app = new Hono();
  app.use("*", async (c, next) => {
    if (isStudent || isAdmin) {
      c.set("user", {
        uid: "test-user-123",
        email: isAdmin ? "admin@eve.com" : "student@gmail.com",
        displayName: isAdmin ? "Eve Admin" : "Eve Student",
        isAdmin,
      });
    }
    await next();
  });
  app.route("/api/ui-studio", publicUiStudioRoutes);
  app.route("/api/admin/ui-studio", adminUiStudioRoutes);

  return {
    app,
    env: { DB: db },
  };
}

async function request(app, env, url, method = "GET", body = null, headers = {}) {
  const reqHeaders = { ...headers };
  if (body) {
    reqHeaders["content-type"] = "application/json";
  }
  return app.request(
    url,
    {
      method,
      headers: Object.keys(reqHeaders).length ? reqHeaders : undefined,
      body: body ? JSON.stringify(body) : undefined,
    },
    env
  );
}

// 1. Validator Unit Tests
test("validateUiStudioConfig enforces valid colors and dimensions", () => {
  // Valid config
  const validConfig = {
    screens: {
      home: {
        components: {
          hero_banner: {
            appearance: {
              backgroundColor: "#1E293B",
              cornerRadius: 16,
              strokeWidth: 2,
              strokeColor: "#334155",
              opacity: 0.95,
            },
            layout: {
              marginTop: 12,
              paddingStart: 16,
            },
            typography: {
              textColor: "#FFFFFF",
              textSize: 18,
            },
            visible: true,
          },
        },
      },
    },
  };
  assert.equal(validateUiStudioConfig(validConfig).valid, true);

  // Invalid color
  const invalidColor = JSON.parse(JSON.stringify(validConfig));
  invalidColor.screens.home.components.hero_banner.appearance.backgroundColor = "not-a-color";
  assert.equal(validateUiStudioConfig(invalidColor).valid, false);

  // Invalid negative dimension
  const negativeDim = JSON.parse(JSON.stringify(validConfig));
  negativeDim.screens.home.components.hero_banner.layout.marginTop = -5;
  assert.equal(validateUiStudioConfig(negativeDim).valid, false);

  // Invalid opacity
  const invalidOpacity = JSON.parse(JSON.stringify(validConfig));
  invalidOpacity.screens.home.components.hero_banner.appearance.opacity = 1.5;
  assert.equal(validateUiStudioConfig(invalidOpacity).valid, false);

  // Valid Material / Blur controls
  const withMaterial = JSON.parse(JSON.stringify(validConfig));
  withMaterial.screens.home.components.hero_banner.material = {
    blurRadius: 20,
    materialOpacity: 0.85,
    tintColor: "#1C1C1E",
    tintOpacity: 0.5,
  };
  assert.equal(validateUiStudioConfig(withMaterial).valid, true);

  // Invalid Blur Radius (> 50)
  const invalidBlur = JSON.parse(JSON.stringify(withMaterial));
  invalidBlur.screens.home.components.hero_banner.material.blurRadius = 99;
  assert.equal(validateUiStudioConfig(invalidBlur).valid, false);

  // Valid Animation
  const withAnim = JSON.parse(JSON.stringify(validConfig));
  withAnim.screens.home.components.hero_banner.animation = {
    enabled: true,
    durationMs: 350,
    delayMs: 50,
  };
  assert.equal(validateUiStudioConfig(withAnim).valid, true);
});

// 2. Public read fallback when no config exists
test("public /api/ui-studio/published returns empty config safely when not configured", async () => {
  const db = new MockUiStudioD1();
  const { app, env } = createApp(db, { isAdmin: false });

  const res = await request(app, env, "/api/ui-studio/published");
  assert.equal(res.status, 200);
  const data = await res.json();
  assert.equal(data.success, true);
  assert.equal(data.data.version, 0);
  assert.equal(data.data.config, null);
  assert.equal(res.headers.get("cache-control"), "public, max-age=60");
});

// 3. Admin authorization guard
test("student accounts cannot access or mutate admin UI Studio endpoints", async () => {
  const db = new MockUiStudioD1();
  const { app, env } = createApp(db, { isAdmin: false });

  const draftRes = await request(app, env, "/api/admin/ui-studio/draft");
  assert.equal(draftRes.status, 403);

  const saveRes = await request(app, env, "/api/admin/ui-studio/draft", "PUT", { config: {} });
  assert.equal(saveRes.status, 403);

  const publishRes = await request(app, env, "/api/admin/ui-studio/publish", "POST", {});
  assert.equal(publishRes.status, 403);
});

// 4. Draft save & retrieval
test("admin can save and retrieve draft configuration", async () => {
  const db = new MockUiStudioD1();
  const { app, env } = createApp(db, { isAdmin: true });

  const testConfig = {
    screens: {
      home: {
        components: {
          streak_pill: {
            appearance: { backgroundColor: "#FFB020", cornerRadius: 20 },
            visible: true,
          },
        },
      },
    },
  };

  const putRes = await request(app, env, "/api/admin/ui-studio/draft", "PUT", { config: testConfig });
  assert.equal(putRes.status, 200);
  const putData = await putRes.json();
  assert.equal(putData.success, true);

  // Retrieve draft
  const getRes = await request(app, env, "/api/admin/ui-studio/draft");
  assert.equal(getRes.status, 200);
  const getData = await getRes.json();
  assert.equal(getData.data.config.screens.home.components.streak_pill.appearance.backgroundColor, "#FFB020");
  assert.equal(getData.data.source, "draft");
});

// 4b. Optimistic concurrency check (Section 73)
test("optimistic concurrency detects conflict on revision mismatch and permits force overwrite", async () => {
  const db = new MockUiStudioD1();
  const { app, env } = createApp(db, { isAdmin: true });

  const initialConfig = {
    screens: {
      home: { components: {} }
    }
  };

  // 1. Initial save
  const put1 = await request(app, env, "/api/admin/ui-studio/draft", "PUT", { config: initialConfig });
  assert.equal(put1.status, 200);

  // Get revision
  const draftRes = await request(app, env, "/api/admin/ui-studio/draft");
  const draftData = await draftRes.json();
  const currentRevision = draftData.data.config.revision;
  assert.ok(currentRevision);

  // 2. Conflict attempt with outdated revision
  const conflictingConfig = {
    screens: {
      home: { components: {} }
    }
  };
  const putConflict = await request(app, env, "/api/admin/ui-studio/draft", "PUT", {
    config: conflictingConfig,
    baseRevision: "rev-outdated-12345"
  });
  assert.equal(putConflict.status, 409);
  const conflictData = await putConflict.json();
  assert.equal(conflictData.error, "NEWER_DRAFT_EXISTS");
  assert.equal(conflictData.currentRevision, currentRevision);

  // 3. Force overwrite bypasses conflict
  const putForce = await request(app, env, "/api/admin/ui-studio/draft", "PUT", {
    config: conflictingConfig,
    baseRevision: "rev-outdated-12345",
    force: true
  });
  assert.equal(putForce.status, 200);
});

// 5. Validation failure in draft PUT
test("admin cannot save invalid draft config with malformed properties", async () => {
  const db = new MockUiStudioD1();
  const { app, env } = createApp(db, { isAdmin: true });

  const invalidConfig = {
    screens: {
      home: {
        components: {
          streak_pill: {
            appearance: { backgroundColor: "bad-hex" },
          },
        },
      },
    },
  };

  const putRes = await request(app, env, "/api/admin/ui-studio/draft", "PUT", { config: invalidConfig });
  assert.equal(putRes.status, 400);
  const errData = await putRes.json();
  assert.equal(errData.success, false);
});

// 6. Publishing flow, version stamping, and public consumption with ETag
test("admin publishing creates immutable version snapshot and enables public consumption with ETag", async () => {
  const db = new MockUiStudioD1();
  const { app, env } = createApp(db, { isAdmin: true });

  const studioConfig = {
    screens: {
      home: {
        components: {
          hero_banner: {
            appearance: { backgroundColor: "#0F172A", cornerRadius: 16 },
            visible: true,
          },
        },
      },
    },
  };

  // 1. Save draft
  await request(app, env, "/api/admin/ui-studio/draft", "PUT", { config: studioConfig });

  // 2. Publish
  const pubRes = await request(app, env, "/api/admin/ui-studio/publish", "POST", {
    notes: "Updated hero banner to slate theme",
  });
  assert.equal(pubRes.status, 200);
  const pubData = await pubRes.json();
  assert.equal(pubData.data.version, 1);
  assert.equal(pubData.data.notes, "Updated hero banner to slate theme");

  // Verify versions table and audit logs
  assert.equal(db.versions.length, 1);
  assert.equal(db.versions[0].version, 1);
  assert.equal(db.auditLog.some((a) => a.action === "published"), true);
  assert.equal(db.adminAuditLog.some((a) => a.action_type === "UI_STUDIO_PUBLISH"), true);

  // 3. Public GET /api/ui-studio/published
  const publicRes = await request(app, env, "/api/ui-studio/published");
  assert.equal(publicRes.status, 200);
  const etag = publicRes.headers.get("etag");
  assert.ok(etag, "ETag header must be present");
  const pubJson = await publicRes.json();
  assert.equal(pubJson.data.version, 1);
  assert.equal(pubJson.data.config.screens.home.components.hero_banner.appearance.backgroundColor, "#0F172A");

  // 4. Conditional GET with If-None-Match
  const cachedRes = await request(app, env, "/api/ui-studio/published", "GET", null, {
    "if-none-match": etag,
  });
  assert.equal(cachedRes.status, 304);
});

// 7. Version history & rollback
test("admin can view version history and restore previous version", async () => {
  const db = new MockUiStudioD1();
  const { app, env } = createApp(db, { isAdmin: true });

  // Publish v1
  await request(app, env, "/api/admin/ui-studio/publish", "POST", {
    config: { screens: { home: { components: { card: { appearance: { cornerRadius: 8 } } } } } },
    notes: "v1 config",
  });

  // Publish v2
  await request(app, env, "/api/admin/ui-studio/publish", "POST", {
    config: { screens: { home: { components: { card: { appearance: { cornerRadius: 24 } } } } } },
    notes: "v2 config",
  });

  // Check version history
  const historyRes = await request(app, env, "/api/admin/ui-studio/versions");
  assert.equal(historyRes.status, 200);
  const historyData = await historyRes.json();
  assert.equal(historyData.data.length, 2);
  assert.equal(historyData.data[0].version, 2);
  assert.equal(historyData.data[1].version, 1);

  // Restore v1 to draft
  const restoreRes = await request(app, env, "/api/admin/ui-studio/restore/1", "POST", { target: "draft" });
  assert.equal(restoreRes.status, 200);

  // Verify draft now has cornerRadius: 8
  const draftRes = await request(app, env, "/api/admin/ui-studio/draft");
  const draftData = await draftRes.json();
  assert.equal(draftData.data.config.screens.home.components.card.appearance.cornerRadius, 8);
});

// 8. Reset to native defaults
test("admin can reset configuration back to native defaults", async () => {
  const db = new MockUiStudioD1();
  const { app, env } = createApp(db, { isAdmin: true });

  await request(app, env, "/api/admin/ui-studio/publish", "POST", {
    config: { screens: { home: {} } },
    notes: "v1",
  });
  assert.equal(db.published.length, 1);

  const resetRes = await request(app, env, "/api/admin/ui-studio/reset", "POST", { target: "all" });
  assert.equal(resetRes.status, 200);
  assert.equal(db.published.length, 0);

  // Check public endpoint returns null config
  const pubRes = await request(app, env, "/api/ui-studio/published");
  const pubData = await pubRes.json();
  assert.equal(pubData.data.config, null);
  assert.equal(pubData.data.version, 0);
});
