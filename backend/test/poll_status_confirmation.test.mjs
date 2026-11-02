import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import { pollRoutes } from "../src/routes/polls.ts";

function grantRights(sqlite, email, rights) {
  sqlite.prepare("INSERT OR IGNORE INTO admins (email, created_at) VALUES (?, ?)").run(email, 1000);
  sqlite.prepare("INSERT INTO admin_rights (email, rights, updated_at) VALUES (?, ?, ?)")
    .run(email, JSON.stringify(rights), 1000);
}

function isolatedD1() {
  const sqlite = new DatabaseSync(":memory:");
  sqlite.exec("PRAGMA foreign_keys = ON");
  const migrationsDir = new URL("../migrations/", import.meta.url);
  const files = readdirSync(migrationsDir).filter((x) => x.endsWith(".sql")).sort();
  for (const name of files) {
    sqlite.exec(readFileSync(new URL("../migrations/" + name, import.meta.url), "utf8"));
  }
  return {
    sqlite,
    d1: {
      prepare(query) {
        function makeStmt(boundValues = []) {
          return {
            bind(...values) {
              return makeStmt(values);
            },
            async run() {
              const info = sqlite.prepare(query).run(...boundValues);
              return { meta: { changes: info.changes } };
            },
            async first() {
              return sqlite.prepare(query).get(...boundValues) || null;
            },
            async all() {
              return { results: sqlite.prepare(query).all(...boundValues) };
            },
          };
        }
        return makeStmt();
      },
      async batch(statements) {
        sqlite.exec("BEGIN");
        try {
          const results = [];
          for (const statement of statements) {
            results.push(await statement.run());
          }
          sqlite.exec("COMMIT");
          return results;
        } catch (error) {
          sqlite.exec("ROLLBACK");
          throw error;
        }
      },
    },
  };
}

function createAdminApp(d1, user) {
  const app = new Hono();
  app.use("*", async (c, next) => {
    c.env = { DB: d1, FIREBASE_PROJECT_ID: "test-project" };
    c.set("user", user);
    await next();
  });
  app.route("/api/polls", pollRoutes);
  return app;
}


test("poll status requires real boolean and existing row without false audit", async () => {
  const {sqlite,d1}=isolatedD1();
  const email="moderator@example.test";
  grantRights(sqlite,email,["moderate_feedback"]);
  const app=createAdminApp(d1,{uid:"moderator",email,isAdmin:true});
  const send=(id,body)=>app.request(`/api/polls/${id}/status`,{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)});
  for(const body of [null,[],{},{active:"false"},{active:0}]) assert.equal((await send("missing",body)).status,400);
  assert.equal((await send("missing",{active:false})).status,404);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM admin_audit_log").get().n,0);
  sqlite.prepare("INSERT INTO polls (id,question,options_json,created_at,ends_at,active,created_by) VALUES (?,?,?,?,?,?,?)").run("poll","Q",'["A","B"]',1,0,1,email);
  assert.equal((await send("poll",{active:false})).status,200);
  assert.equal(sqlite.prepare("SELECT active FROM polls WHERE id='poll'").get().active,0);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM admin_audit_log").get().n,1);
  sqlite.close();
});

test("poll status rejects local admin claims without the server right", async () => {
  const {sqlite,d1}=isolatedD1();
  const email="viewer@example.test";
  grantRights(sqlite,email,["view_analytics_logs"]);
  const app=createAdminApp(d1,{uid:"viewer",email,isAdmin:true});
  const response=await app.request("/api/polls/missing/status",{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify({active:true})});
  assert.equal(response.status,403);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM admin_audit_log").get().n,0);
  sqlite.close();
});

test("poll status and audit roll back together when audit insertion fails", async () => {
  const {sqlite,d1}=isolatedD1();
  const email="moderator@example.test";
  grantRights(sqlite,email,["moderate_feedback"]);
  sqlite.prepare("INSERT INTO polls (id,question,options_json,created_at,ends_at,active,created_by) VALUES (?,?,?,?,?,?,?)").run("poll","Q",'["A","B"]',1,0,1,email);
  sqlite.exec("CREATE TRIGGER reject_poll_audit BEFORE INSERT ON admin_audit_log BEGIN SELECT RAISE(ABORT, 'injected audit failure'); END");
  const app=createAdminApp(d1,{uid:"moderator",email,isAdmin:true});
  app.onError((error,c)=>c.json({success:false,error:"Transaction failed"},500));
  const response=await app.request("/api/polls/poll/status",{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify({active:false})});
  assert.equal(response.status,500);
  assert.equal(sqlite.prepare("SELECT active FROM polls WHERE id='poll'").get().active,1);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM admin_audit_log").get().n,0);
  sqlite.close();
});
