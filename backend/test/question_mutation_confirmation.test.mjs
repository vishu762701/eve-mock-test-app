import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync,readdirSync } from "node:fs";
import { Hono } from "hono";
import { questionRoutes } from "../src/routes/questions.ts";

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

const original={examId:"exam",questionText:"Original Q",optionA:"A",optionB:"B",optionC:"C",optionD:"D",correctAnswer:"A"};
function fixture(t,rights=["manage_questions"]) {
  const {sqlite,d1}=isolatedD1(); t.after(()=>sqlite.close());
  const email="question-admin@example.test"; grantRights(sqlite,email,rights);
  sqlite.prepare("INSERT INTO exams (id,exam_name) VALUES ('exam','Exam')").run();
  const app=new Hono();
  app.use("*",async(c,next)=>{ c.env={DB:d1}; c.set("user",{uid:"admin",email,isAdmin:true}); await next(); });
  app.onError((_,c)=>c.json({success:false,error:"Transaction failed"},500));
  app.route("/api/questions",questionRoutes);
  const send=(method,id,body)=>app.request(`/api/questions/${id}`,{method,headers:{"Content-Type":"application/json"},...(body!==undefined?{body:JSON.stringify(body)}:{})});
  return {sqlite,send,app,
    audits:()=>sqlite.prepare("SELECT action_type FROM admin_audit_log ORDER BY timestamp,id").all().map(x=>x.action_type),
    row:id=>sqlite.prepare("SELECT question_text FROM questions WHERE id=?").get(id),
  };
}
async function create(f) {
  const response=await f.app.request("/api/questions",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(original)});
  assert.equal(response.status,201); const id=(await response.json()).data.id;
  f.sqlite.exec("DELETE FROM admin_audit_log"); return id;
}
test("question edit confirms target write and one atomic audit",async t=>{
  const f=fixture(t); const id=await create(f);
  assert.equal((await f.send("PUT",id,{...original,questionText:"Edited Q"})).status,200);
  assert.equal(f.row(id).question_text,"Edited Q"); assert.deepEqual(f.audits(),["QUESTION_EDITED"]);
});
test("missing question edit cannot produce success or false audit",async t=>{
  const f=fixture(t);
  const response=await f.send("PUT","missing",original);
  assert.equal(response.status,404); assert.equal((await response.json()).success,false);
  assert.deepEqual(f.audits(),[]);
});
test("delete confirms absence and repeated delete does not invent another audit",async t=>{
  const f=fixture(t); const id=await create(f);
  assert.equal((await f.send("DELETE",id)).status,200); assert.equal(f.row(id),undefined);
  assert.deepEqual(f.audits(),["QUESTION_DELETED"]);
  assert.equal((await f.send("DELETE",id)).status,200); assert.equal(f.row(id),undefined);
  assert.deepEqual(f.audits(),["QUESTION_DELETED"]);
});
test("question mutations reject local admin claims lacking manage_questions",async t=>{
  const f=fixture(t,["manage_exams"]);
  assert.equal((await f.send("PUT","missing",original)).status,403);
  assert.equal((await f.send("DELETE","missing")).status,403);
  assert.deepEqual(f.audits(),[]);
});
test("malformed question edit fails before any row or audit change",async t=>{
  const f=fixture(t); const id=await create(f);
  for(const body of [null,[],{}, {...original,correctAnswer:"Z"},{...original,optionA:""}])
    assert.equal((await f.send("PUT",id,body)).status,400,JSON.stringify(body));
  assert.equal(f.row(id).question_text,"Original Q"); assert.deepEqual(f.audits(),[]);
});
test("question edit rolls back when audit insertion fails",async t=>{
  const f=fixture(t); const id=await create(f);
  f.sqlite.exec("CREATE TRIGGER reject_question_audit BEFORE INSERT ON admin_audit_log BEGIN SELECT RAISE(ABORT, 'injected audit failure'); END");
  assert.equal((await f.send("PUT",id,{...original,questionText:"Edited Q"})).status,500);
  assert.equal(f.row(id).question_text,"Original Q"); assert.deepEqual(f.audits(),[]);
});
test("question delete rolls back when audit insertion fails",async t=>{
  const f=fixture(t); const id=await create(f);
  f.sqlite.exec("CREATE TRIGGER reject_question_audit BEFORE INSERT ON admin_audit_log BEGIN SELECT RAISE(ABORT, 'injected audit failure'); END");
  assert.equal((await f.send("DELETE",id)).status,500);
  assert.equal(f.row(id).question_text,"Original Q"); assert.deepEqual(f.audits(),[]);
});
test("UPDATE after real hard deletion cannot claim the original question was restored",async t=>{
  const f=fixture(t); const id=await create(f);
  assert.equal((await f.send("DELETE",id)).status,200);
  assert.equal((await f.send("PUT",id,original)).status,404);
  assert.equal(f.row(id),undefined); assert.deepEqual(f.audits(),["QUESTION_DELETED"]);
});
