import test from "node:test";
import assert from "node:assert/strict";
import workerModule from "../src/index.ts";
import { isolatedD1 } from "./support/sqliteD1.mjs";

// tsx's ESM bridge may expose the Worker default export under .default.
const worker = workerModule.default || workerModule;

test("diagnostics require signed Firebase identity, verified email, D1 membership and rights", async (t) => {
  const { sqlite, d1 } = isolatedD1();
  const email = "diagnostic-admin@example.test";
  const pair = await crypto.subtle.generateKey({
    name: "RSASSA-PKCS1-v1_5", modulusLength: 2048,
    publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256",
  }, true, ["sign", "verify"]);
  const jwk = { ...await crypto.subtle.exportKey("jwk", pair.publicKey),
    kid: "diagnostic-test-key", alg: "RS256", use: "sig" };
  t.mock.method(globalThis, "fetch", async () =>
    new Response(JSON.stringify({ keys: [jwk] }), { headers: { "Cache-Control": "max-age=300" } }));

  const token = async (claims = {}) => {
    const header = Buffer.from(JSON.stringify({ alg: "RS256", kid: jwk.kid })).toString("base64url");
    const payload = Buffer.from(JSON.stringify({
      iss: "https://securetoken.google.com/test-project",
      aud: "test-project", sub: "diag-user", email, email_verified: true,
      exp: Math.floor(Date.now() / 1000) + 3600, ...claims,
    })).toString("base64url");
    const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", pair.privateKey,
      new TextEncoder().encode(header + "." + payload));
    return header + "." + payload + "." + Buffer.from(signature).toString("base64url");
  };
  const env = { DB: d1, FIREBASE_PROJECT_ID: "test-project", DIAGNOSTIC_KEY: "long-secret-diagnostic-key" };
  const request = (headers = {}) =>
    worker.fetch(new Request("https://eve.test/api/diag/d1", { headers }), env, {});

  try {
    assert.equal((await request()).status, 401);
    assert.equal((await request({ Authorization: "Bearer malformed" })).status, 401);
    const verified = await token();
    assert.equal((await request({ Authorization: "Bearer " + verified })).status, 401,
      "Verified Firebase identity alone must not create administrator authority");

    sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(email, 1);
    sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
      .run(email, JSON.stringify(["manage_questions"]), 1);
    assert.equal((await request({ Authorization: "Bearer " + verified })).status, 401,
      "Membership without diagnostic rights is insufficient");

    sqlite.prepare("UPDATE admin_rights SET rights=? WHERE email=?")
      .run(JSON.stringify(["view_analytics_logs"]), email);
    const authorized = await request({ Authorization: "Bearer " + verified });
    assert.equal(authorized.status, 200);
    assert.equal((await authorized.json()).readWriteTest, "PASS");

    // Rights and membership are resolved on every diagnostic request. A
    // previously valid signed token must lose access immediately on revocation.
    sqlite.prepare("UPDATE admin_rights SET rights=? WHERE email=?").run("[]", email);
    assert.equal((await request({ Authorization: "Bearer " + verified })).status, 401,
      "Revoking view_analytics_logs must immediately revoke diagnostics");
    sqlite.prepare("UPDATE admin_rights SET rights=? WHERE email=?")
      .run(JSON.stringify(["view_analytics_logs"]), email);
    sqlite.prepare("DELETE FROM admins WHERE email=?").run(email);
    assert.equal((await request({ Authorization: "Bearer " + verified })).status, 401,
      "Stale rights without active D1 admin membership must fail closed");
    sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(email, 1);
    assert.equal((await request({ Authorization: "Bearer " + verified })).status, 200,
      "Restoring membership with explicit rights must restore verified access");

    assert.equal((await request({ Authorization: "Bearer " + await token({ email_verified: false }) })).status, 401);
    assert.equal((await request({ Authorization: "Bearer " + await token({ exp: 1 }) })).status, 401);

    // Secret-based diagnostics remain available to authorized operations,
    // independent of Firebase, without exposing the secret in a response.
    const withKey = await request({ "X-Diagnostic-Key": env.DIAGNOSTIC_KEY });
    assert.equal(withKey.status, 200);
    assert.equal(JSON.stringify(await withKey.json()).includes(env.DIAGNOSTIC_KEY), false);
  } finally { sqlite.close(); }
});
