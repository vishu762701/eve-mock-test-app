import { test } from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.js";

function createMockDb() {
  const store = new Map<string, { id: string; body: string; updated_at?: number; updated_by?: string }>();
  return {
    prepare(sql: string) {
      let boundParams: any[] = [];
      return {
        bind(...params: any[]) {
          boundParams = params;
          return this;
        },
        async first<T = any>(): Promise<T | null> {
          if (sql.includes("FROM admins")) return { email: "own.keni@gmail.com" } as T;
          if (sql.includes("FROM app_content WHERE id = 'floating_link'")) {
            const item = store.get("floating_link");
            return (item || null) as T | null;
          }
          return null;
        },
        async run() {
          if (sql.includes("DELETE FROM app_content WHERE id = 'floating_link'")) {
            store.delete("floating_link");
            return { success: true };
          }
          if (sql.includes("INSERT INTO app_content")) {
            const [body, updated_at, updated_by] = boundParams;
            store.set("floating_link", { id: "floating_link", body, updated_at, updated_by });
            return { success: true };
          }
          return { success: true };
        }
      };
    }
  };
}

test("floating-link endpoint handles plain string, json with coordinates, and validation", async () => {
  const db = createMockDb();
  const env: any = { DB: db, JWT_SECRET: "test-jwt-secret-with-sufficient-length-here" };

  // 1. Initial GET on empty returns success with empty url and null coordinates
  const req1 = new Request("https://eve.test/api/floating-link");
  const res1 = await worker.fetch(req1, env, {} as any);
  assert.equal(res1.status, 200);
  const json1: any = await res1.json();
  assert.equal(json1.success, true);
  assert.equal(json1.data.url, "");
  assert.equal(json1.data.x, null);
  assert.equal(json1.data.y, null);

  // 2. Test admin routes with admin user harness
  const { Hono } = await import("hono");
  const { adminRoutes } = await import("../src/routes/admin.js");
  const adminApp = new Hono<{ Bindings: any; Variables: { user: any } }>();
  adminApp.use("*", async (c, next) => {
    c.set("user", { email: "own.keni@gmail.com", isAdmin: true });
    await next();
  });
  adminApp.route("/api/admin", adminRoutes);

  const putReq = new Request("https://eve.test/api/admin/floating-link", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ url: "https://t.me/eve_community", x: 0.85, y: 0.75 })
  });
  const res2 = await adminApp.fetch(putReq, env);
  assert.equal(res2.status, 200);
  const json2: any = await res2.json();
  assert.equal(json2.success, true);
  assert.equal(json2.data.url, "https://t.me/eve_community");
  assert.equal(json2.data.x, 0.85);
  assert.equal(json2.data.y, 0.75);

  // 3. Public GET via worker.fetch returns saved url and coordinates
  const res3 = await worker.fetch(req1, env, {} as any);
  assert.equal(res3.status, 200);
  const json3: any = await res3.json();
  assert.equal(json3.success, true);
  assert.equal(json3.data.url, "https://t.me/eve_community");
  assert.equal(json3.data.x, 0.85);
  assert.equal(json3.data.y, 0.75);

  // 4. Update coordinates only, preserving url
  const putReq2 = new Request("https://eve.test/api/admin/floating-link", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ x: 0.5, y: 0.9 })
  });
  const res4 = await adminApp.fetch(putReq2, env);
  assert.equal(res4.status, 200);
  const json4: any = await res4.json();
  assert.equal(json4.success, true);
  assert.equal(json4.data.url, "https://t.me/eve_community");
  assert.equal(json4.data.x, 0.5);
  assert.equal(json4.data.y, 0.9);

  // 5. Clamps out-of-range coordinates to [0, 1]
  const putReq3 = new Request("https://eve.test/api/admin/floating-link", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ x: 1.5, y: -0.2 })
  });
  const res5 = await adminApp.fetch(putReq3, env);
  assert.equal(res5.status, 200);
  const json5: any = await res5.json();
  assert.equal(json5.data.x, 1.0);
  assert.equal(json5.data.y, 0.0);
});

