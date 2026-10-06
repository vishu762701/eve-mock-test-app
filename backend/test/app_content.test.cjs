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
const { appContentRoutes } = require("../src/routes/appContent");
const { authMiddleware } = require("../src/middleware/authMiddleware");

class MockD1 {
  row = null;
  writes = 0;

  prepare() {
    return {
      bind: (...values) => ({
        first: async () => this.row,
        run: async () => {
          const [id, title, body, updated_at, updated_by, ...extra] = values;
          this.row = {
            id,
            title,
            body,
            updated_at,
            updated_by,
            support_email: "",
            phone: "",
            website: "",
            address: "",
            enabled: extra[0] ?? 1,
            cta_label: extra[1] ?? "",
            cta_action: extra[2] ?? "",
          };
          this.writes += 1;
          return { success: true };
        },
      }),
    };
  }
}

function makeApp(db, isAdmin) {
  const app = new Hono();
  app.use("*", async (c, next) => {
    c.set("user", {
      uid: "test-user",
      email: isAdmin ? "admin@example.com" : "student@example.com",
      displayName: "Test User",
      isAdmin,
    });
    await next();
  });
  app.route("/api/app-content", appContentRoutes);
  return { app, env: { DB: db } };
}

async function jsonRequest(app, env, url, method = "GET", body) {
  const response = await app.request(url, {
    method,
    headers: body ? { "content-type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  }, env);
  return { response, payload: await response.json() };
}

test("Home hero is empty publicly until enabled and only admins can manage drafts", async () => {
  const db = new MockD1();
  const student = makeApp(db, false);
  const empty = await jsonRequest(student.app, student.env, "/api/app-content/home_hero");
  assert.equal(empty.response.status, 200);
  assert.deepEqual(
    { enabled: empty.payload.data.enabled, title: empty.payload.data.title, body: empty.payload.data.body },
    { enabled: false, title: "", body: "" }
  );

  const denied = await jsonRequest(student.app, student.env, "/api/app-content/home_hero", "PUT", {
    enabled: true,
    title: "Headline",
    body: "Supporting text",
  });
  assert.equal(denied.response.status, 403);
  assert.equal(db.writes, 0);

  const admin = makeApp(db, true);
  const invalid = await jsonRequest(admin.app, admin.env, "/api/app-content/home_hero", "PUT", {
    enabled: true,
    title: "Headline",
    body: "",
  });
  assert.equal(invalid.response.status, 400);
  assert.equal(db.writes, 0);

  const saved = await jsonRequest(admin.app, admin.env, "/api/app-content/home_hero", "PUT", {
    enabled: true,
    title: "<b>Exam prep</b>",
    body: "Prepare today\u0000, succeed tomorrow.",
    ctaLabel: "Start now",
    ctaAction: "open_practice",
  });
  assert.equal(saved.response.status, 200);
  assert.equal(saved.payload.success, true);
  assert.equal(db.row.title, "Exam prep");
  assert.equal(db.row.body, "Prepare today, succeed tomorrow.");

  const published = await jsonRequest(student.app, student.env, "/api/app-content/home_hero");
  assert.equal(published.payload.data.enabled, true);
  assert.equal(published.payload.data.title, "Exam prep");
  assert.equal(published.payload.data.ctaAction, "open_practice");

  const disabled = await jsonRequest(admin.app, admin.env, "/api/app-content/home_hero", "PUT", {
    enabled: false,
    title: "Draft headline",
    body: "Draft body",
  });
  assert.equal(disabled.response.status, 200);
  const hidden = await jsonRequest(student.app, student.env, "/api/app-content/home_hero");
  assert.equal(hidden.payload.data.enabled, false);
  assert.equal(hidden.payload.data.title, "");
  assert.equal(hidden.payload.data.body, "");
  const draft = await jsonRequest(admin.app, admin.env, "/api/app-content/home_hero/admin");
  assert.equal(draft.response.status, 200);
  assert.equal(draft.payload.data.title, "Draft headline");
});

test("Home hero rejects unsupported and incomplete CTA actions", async () => {
  const db = new MockD1();
  const admin = makeApp(db, true);
  const invalid = await jsonRequest(admin.app, admin.env, "/api/app-content/home_hero", "PUT", {
    enabled: true,
    title: "Headline",
    body: "Body",
    ctaLabel: "Continue",
  });
  assert.equal(invalid.response.status, 400);

  const unsupported = await jsonRequest(admin.app, admin.env, "/api/app-content/home_hero", "PUT", {
    enabled: true,
    title: "Headline",
    body: "Body",
    ctaLabel: "Continue",
    ctaAction: "open_settings",
  });
  assert.equal(unsupported.response.status, 400);
  assert.equal(db.writes, 0);
});

test("the admin draft endpoint is not in the public app-content read allowlist", async () => {
  const app = new Hono();
  app.use("*", authMiddleware);
  app.get("/api/app-content/home_hero/admin", (c) => c.json({ success: true }));
  app.get("/api/app-content/home_hero", (c) => c.json({ success: true }));

  const env = { DB: new MockD1(), FIREBASE_PROJECT_ID: "test-project" };
  const publicRead = await app.request("/api/app-content/home_hero", {}, env);
  const draftRead = await app.request("/api/app-content/home_hero/admin", {}, env);
  assert.equal(publicRead.status, 200);
  assert.equal(draftRead.status, 401);
});
