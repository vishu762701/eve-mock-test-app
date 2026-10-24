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
const { bannerRoutes } = require("../src/routes/banners");

class MockD1 {
  rows;

  constructor(rows = []) {
    this.rows = rows;
  }

  prepare(sql) {
    const statement = {
      values: [],
      bind: (...values) => {
        statement.values = values;
        return statement;
      },
      all: async () => {
        const rows = this.rows
          .filter((row) => row.active === 1)
          .sort((left, right) => left.order_index - right.order_index);
        return { results: rows };
      },
      first: async () => {
        if (sql.includes("MAX(order_index)")) {
          return { max_order: this.rows.length ? Math.max(...this.rows.map((row) => row.order_index)) : null };
        }
        const [id] = statement.values;
        return this.rows.find((row) => row.id === id) || null;
      },
      run: async () => {
        if (sql.includes("INSERT INTO home_banners")) {
          const [id, image_url, storage_path, order_index, uploaded_at, uploaded_by, link_url, link_label] = statement.values;
          this.rows.push({
            id, image_url, storage_path, order_index, uploaded_at, uploaded_by,
            active: 1, link_url, link_label,
          });
        } else if (sql.includes("UPDATE home_banners SET link_url")) {
          const [link_url, link_label, id] = statement.values;
          const row = this.rows.find((item) => item.id === id);
          if (row) Object.assign(row, { link_url, link_label });
        } else if (sql.includes("DELETE FROM home_banners")) {
          const [id] = statement.values;
          this.rows = this.rows.filter((row) => row.id !== id);
        } else if (sql.includes("UPDATE home_banners SET order_index")) {
          const [order_index, id] = statement.values;
          const row = this.rows.find((item) => item.id === id);
          if (row) row.order_index = order_index;
        }
        return { success: true };
      },
    };
    return statement;
  }

  async batch(statements) {
    return Promise.all(statements.map((statement) => statement.run()));
  }
}

function makeApp(db, isAdmin = false) {
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
  app.route("/api/banners", bannerRoutes);
  return {
    app,
    env: {
      DB: db,
      SUPABASE_PROJECT_URL: "https://storage.example",
      SUPABASE_BUCKET_NAME: "eve-media",
      SUPABASE_SERVICE_ROLE_KEY: "test-key",
    },
  };
}

async function request(app, env, url, method = "GET", body) {
  return app.request(url, {
    method,
    headers: body ? { "content-type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  }, env);
}

test("legacy active banners remain readable without link metadata", async () => {
  const db = new MockD1([{
    id: "legacy", image_url: "https://storage.example/old.jpg", storage_path: "banners/old.jpg",
    order_index: 0, uploaded_at: 100, uploaded_by: "admin@example.com", active: 1,
  }]);
  const { app, env } = makeApp(db);
  const response = await request(app, env, "/api/banners");
  const payload = await response.json();

  assert.equal(response.status, 200);
  assert.equal(payload.data.length, 1);
  assert.equal(payload.data[0].imageUrl, "https://storage.example/old.jpg");
  assert.equal(payload.data[0].linkUrl, "");
  assert.equal(payload.data[0].linkLabel, "");
});

test("students cannot upload or edit banner links", async () => {
  const db = new MockD1();
  const { app, env } = makeApp(db, false);
  const upload = await request(app, env, "/api/banners", "POST", { base64: "aW1hZ2U=" });
  const edit = await request(app, env, "/api/banners/one/link", "PUT", { linkUrl: "", linkLabel: "" });
  assert.equal(upload.status, 403);
  assert.equal(edit.status, 403);
});

test("admin upload stores a banner and its optional link metadata", async (t) => {
  const db = new MockD1();
  const { app, env } = makeApp(db, true);
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  globalThis.fetch = async () => new Response("{}", { status: 200 });

  const form = new FormData();
  form.append("file", new Blob(["image-bytes"], { type: "image/jpeg" }), "banner.jpg");
  form.append("linkUrl", "https://example.com/practice");
  form.append("linkLabel", "Start Practicing");
  const response = await app.request("/api/banners", { method: "POST", body: form }, env);
  const payload = await response.json();

  assert.equal(response.status, 201);
  assert.equal(payload.data.linkUrl, "https://example.com/practice");
  assert.equal(payload.data.linkLabel, "Start Practicing");
  assert.equal(db.rows[0].link_url, "https://example.com/practice");
  assert.equal(db.rows[0].link_label, "Start Practicing");
});

test("banner links require HTTP(S), a host, and a usable label", async (t) => {
  const db = new MockD1();
  const { app, env } = makeApp(db, true);
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  globalThis.fetch = async () => new Response("{}", { status: 200 });

  const invalidLinks = [
    ["javascript:alert(1)", "Open"],
    ["https://example.com/path", ""],
    ["https://example.com/path", "x".repeat(49)],
    ["https://user:pass@example.com", "Open"],
    ["", "Orphan label"],
  ];
  for (const [linkUrl, linkLabel] of invalidLinks) {
    const form = new FormData();
    form.append("file", new Blob(["image-bytes"], { type: "image/jpeg" }), "banner.jpg");
    form.append("linkUrl", linkUrl);
    form.append("linkLabel", linkLabel);
    const response = await app.request("/api/banners", { method: "POST", body: form }, env);
    assert.equal(response.status, 400);
  }
  assert.equal(db.rows.length, 0);
});

test("admin can update or clear a banner link without replacing its image", async () => {
  const row = {
    id: "one", image_url: "https://storage.example/one.jpg", storage_path: "banners/one.jpg",
    order_index: 0, uploaded_at: 100, uploaded_by: "admin@example.com", active: 1,
    link_url: "", link_label: "",
  };
  const db = new MockD1([row]);
  const { app, env } = makeApp(db, true);

  const updated = await request(app, env, "/api/banners/one/link", "PUT", {
    linkUrl: "https://example.com/practice",
    linkLabel: "Start Practicing",
  });
  assert.equal(updated.status, 200);
  assert.equal(row.link_url, "https://example.com/practice");
  assert.equal(row.link_label, "Start Practicing");

  const cleared = await request(app, env, "/api/banners/one/link", "PUT", { linkUrl: "", linkLabel: "" });
  assert.equal(cleared.status, 200);
  assert.equal(row.link_url, "");
  assert.equal(row.link_label, "");
  assert.equal(row.image_url, "https://storage.example/one.jpg");
});

test("existing banner reorder and delete routes remain functional", async (t) => {
  const db = new MockD1([
    { id: "one", image_url: "one", storage_path: "one.jpg", order_index: 0, uploaded_at: 1, uploaded_by: "a", active: 1 },
    { id: "two", image_url: "two", storage_path: "two.jpg", order_index: 1, uploaded_at: 2, uploaded_by: "a", active: 1 },
  ]);
  const { app, env } = makeApp(db, true);
  const originalFetch = globalThis.fetch;
  t.after(() => { globalThis.fetch = originalFetch; });
  globalThis.fetch = async () => new Response(null, { status: 204 });

  const reordered = await request(app, env, "/api/banners/two/reorder", "PUT", { moveUp: true });
  assert.equal(reordered.status, 200);
  assert.deepEqual(db.rows.map((row) => row.id).sort((a, b) => db.rows.find((row) => row.id === a).order_index - db.rows.find((row) => row.id === b).order_index), ["two", "one"]);

  const deleted = await request(app, env, "/api/banners/two", "DELETE");
  assert.equal(deleted.status, 200);
  assert.equal(db.rows.some((row) => row.id === "two"), false);
});
