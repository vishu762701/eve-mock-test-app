import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import { examRoutes } from "../src/routes/exams.ts";

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

function fixture(t, rights = ["manage_exams"]) {
  const { sqlite, d1 } = isolatedD1(); t.after(() => sqlite.close());
  // D1 serializes atomic batches; serialize this SQLite connection equivalently.
  const runBatch = d1.batch.bind(d1);
  let batchQueue = Promise.resolve();
  let nextBatchHook = null;
  d1.batch = statements => {
    const pending = batchQueue.then(async () => {
      const hook=nextBatchHook; nextBatchHook=null;
      if(hook) await hook();
      return runBatch(statements);
    });
    batchQueue = pending.catch(() => {});
    return pending;
  };
  const email = "exam-admin@example.test"; grantRights(sqlite,email,rights);
  const app = new Hono();
  app.use("*", async (c,next) => { c.env={DB:d1}; c.set("user",{uid:"admin",email,isAdmin:true}); await next(); });
  app.onError((_,c) => c.json({success:false,error:"Transaction failed"},500));
  app.route("/api/exams",examRoutes);
  return { sqlite,
    beforeNextBatch: hook => { nextBatchHook=hook; },
    send: (id,body) => app.request(`/api/exams/${id}/rename`,{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)}),
    sendMetadata: (id,body) => app.request(`/api/exams/${id}`,{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)}),
    readExam: id => app.request(`/api/exams/${id}`),
    audits: () => sqlite.prepare("SELECT COUNT(*) AS n FROM admin_audit_log").get().n,
  };
}
function seed(db,id,name,parent="") {
  db.prepare("INSERT INTO exams (id,exam_name,parent_exam_id) VALUES (?,?,?)").run(id,name,parent);
  db.prepare("INSERT INTO generated_tests (id,exam_id,exam_name,title,generated_at) VALUES (?,?,?,?,1)").run(`g-${id}`,id,name,"Title");
  db.prepare("INSERT INTO attempts (id,user_id,exam_id,exam_name,score,total,correct,wrong,unattempted,timestamp) VALUES (?,?,?,?,1,2,1,0,1,1)").run(`a-${id}`,"student",id,name);
  db.prepare("INSERT INTO leaderboard (id,user_id,exam_id,exam_name,score,total,timestamp) VALUES (?,?,?,?,1,2,1)").run(`l-${id}`,"student",id,name);
  db.prepare("INSERT OR IGNORE INTO users (id,email,created_at,last_active) VALUES ('student','student@example.test',1,1)").run();
  db.prepare("INSERT INTO bookmarks (id,user_id,question_id,exam_id,exam_name,question_number,question_text,option_a,option_b,option_c,option_d,correct_answer,bookmarked_at) VALUES (?,'student','q',?,?,1,'Q','A','B','C','D','A',1)").run(`b-${id}`,id,name);
}
function names(db,id) {
  return ["exams","generated_tests","attempts","leaderboard","bookmarks"].map(table =>
    db.prepare(`SELECT exam_name FROM ${table} WHERE ${table==="exams"?"id":"exam_id"}=?`).get(id).exam_name);
}
test("rename accepts canonical and installed Android payloads and atomically cascades names",async t=>{
  const {sqlite,send,audits}=fixture(t); seed(sqlite,"exam","Old"); seed(sqlite,"unrelated","Unrelated");
  assert.equal((await send("exam",{newName:"  Canonical  "})).status,200);
  assert.deepEqual(names(sqlite,"exam"),Array(5).fill("Canonical"));
  assert.equal((await send("exam",{examName:"  Legacy Android  "})).status,200);
  assert.deepEqual(names(sqlite,"exam"),Array(5).fill("Legacy Android"));
  assert.deepEqual(names(sqlite,"unrelated"),Array(5).fill("Unrelated")); assert.equal(audits(),2);
});
test("rename rejects malformed names without data or audit changes",async t=>{
  const {sqlite,send,audits}=fixture(t); seed(sqlite,"exam","Old");
  for(const body of [null,[],{},{newName:null},{newName:7},{newName:[]},{newName:{}},{newName:" "},{examName:false},{newName:"",examName:"Must not win"}])
    assert.equal((await send("exam",body)).status,400,JSON.stringify(body));
  assert.deepEqual(names(sqlite,"exam"),Array(5).fill("Old")); assert.equal(audits(),0);
});
test("missing exam never reports success or creates false audit",async t=>{
  const {sqlite,send,audits}=fixture(t); seed(sqlite,"exam","Old");
  // Historical orphan rows must not become a successful rename of a missing exam.
  sqlite.prepare("INSERT INTO attempts (id,user_id,exam_id,exam_name,score,total,correct,wrong,unattempted,timestamp) VALUES ('orphan','student','missing','Historical',1,2,1,0,1,1)").run();
  assert.equal((await send("missing",{newName:"New"})).status,404);
  assert.equal(sqlite.prepare("SELECT exam_name FROM attempts WHERE id='orphan'").get().exam_name,"Historical");
  assert.deepEqual(names(sqlite,"exam"),Array(5).fill("Old")); assert.equal(audits(),0);
});
test("rename requires server manage_exams despite local admin claim",async t=>{
  const {sqlite,send,audits}=fixture(t,["manage_questions"]); seed(sqlite,"exam","Old");
  assert.equal((await send("exam",{newName:"New"})).status,403);
  assert.deepEqual(names(sqlite,"exam"),Array(5).fill("Old")); assert.equal(audits(),0);
});
test("rename enforces existing sibling-name rule inside write transaction",async t=>{
  const {sqlite,send,audits}=fixture(t); seed(sqlite,"exam","Old"); seed(sqlite,"other","Taken");
  assert.equal((await send("exam",{newName:"taken"})).status,409);
  assert.deepEqual(names(sqlite,"exam"),Array(5).fill("Old")); assert.equal(audits(),0);
  seed(sqlite,"parent","Parent"); seed(sqlite,"child","Child","parent");
  assert.equal((await send("child",{newName:"Taken"})).status,200); assert.equal(audits(),1);
});
test("rename cascade and audit roll back together on audit failure",async t=>{
  const {sqlite,send,audits}=fixture(t); seed(sqlite,"exam","Old");
  sqlite.exec("CREATE TRIGGER reject_rename_audit BEFORE INSERT ON admin_audit_log BEGIN SELECT RAISE(ABORT, 'injected audit failure'); END");
  assert.equal((await send("exam",{newName:"New"})).status,500);
  assert.deepEqual(names(sqlite,"exam"),Array(5).fill("Old")); assert.equal(audits(),0);
});

test("racing sibling renames cannot both claim the same new name",async t=>{
  const {sqlite,send,audits}=fixture(t); seed(sqlite,"first","First"); seed(sqlite,"second","Second");
  const responses=await Promise.all([send("first",{newName:"Shared"}),send("second",{newName:"Shared"})]);
  assert.deepEqual(responses.map(r=>r.status).sort(),[200,409]);
  assert.equal(sqlite.prepare("SELECT COUNT(*) AS n FROM exams WHERE exam_name='Shared'").get().n,1);
  assert.equal(audits(),1);
});
test("late cascade failure rolls back target, earlier cascades and audit",async t=>{
  const {sqlite,send,audits}=fixture(t); seed(sqlite,"exam","Old");
  sqlite.exec("CREATE TRIGGER reject_bookmark_rename BEFORE UPDATE ON bookmarks BEGIN SELECT RAISE(ABORT, 'injected late failure'); END");
  assert.equal((await send("exam",{newName:"New"})).status,500);
  assert.deepEqual(names(sqlite,"exam"),Array(5).fill("Old")); assert.equal(audits(),0);
});

// Existing metadata fields must not overwrite hidden exam settings.
test("metadata persists cutoff settings and preserves hidden settings",async t=>{
  const {sqlite,sendMetadata,readExam}=fixture(t); seed(sqlite,"parent","Parent"); seed(sqlite,"exam","Old","parent");
  sqlite.prepare("UPDATE exams SET question_count=100, negative_marking_text='1/3', negative_marking_value=?, publish_mode='live', auto_generation_enabled=0, auto_gen_time='13:00', generation_prompt='Existing prompt' WHERE id='exam'").run(1/3);
  const cutoffs={General:70.5,OBC:65};
  assert.equal((await sendMetadata("exam",{examName:"Edited",timeLimitMinutes:45,category:"Custom",cutoffs})).status,200);
  const row=sqlite.prepare("SELECT * FROM exams WHERE id='exam'").get();
  assert.equal(row.exam_name,"Edited"); assert.equal(row.time_limit_minutes,45); assert.equal(row.category,"Custom");
  assert.equal(row.question_count,100); assert.equal(row.negative_marking_text,"1/3"); assert.equal(row.negative_marking_value,1/3);
  assert.equal(row.parent_exam_id,"parent"); assert.equal(row.publish_mode,"live"); assert.equal(row.auto_generation_enabled,0);
  assert.equal(row.auto_gen_time,"13:00"); assert.equal(row.generation_prompt,"Existing prompt");
  assert.deepEqual(JSON.parse(row.cutoffs_json),cutoffs);
  const verified=await (await readExam("exam")).json(); assert.equal(verified.success,true); assert.deepEqual(verified.data.cutoffs,cutoffs);
});
test("metadata rejects malformed cutoffs before changes or audit",async t=>{
  const {sqlite,sendMetadata,audits}=fixture(t); seed(sqlite,"exam","Old");
  for(const cutoffs of [null,[],{General:-1},{General:"70"},{Unknown:1},{General:null}])
    assert.equal((await sendMetadata("exam",{examName:"Must not change",cutoffs})).status,400);
  assert.equal(sqlite.prepare("SELECT exam_name FROM exams WHERE id='exam'").get().exam_name,"Old"); assert.equal(audits(),0);
});
test("metadata omission preserves cutoffs and clearing is explicit",async t=>{
  const {sqlite,sendMetadata,readExam}=fixture(t); seed(sqlite,"exam","Old");
  sqlite.prepare("UPDATE exams SET cutoffs_json=? WHERE id='exam'").run(JSON.stringify({General:80}));
  assert.equal((await sendMetadata("exam",{examName:"New"})).status,200);
  assert.deepEqual((await (await readExam("exam")).json()).data.cutoffs,{General:80});
  assert.equal((await sendMetadata("exam",{cutoffs:{}})).status,200);
  assert.deepEqual((await (await readExam("exam")).json()).data.cutoffs,{});
});
test("metadata cutoffs require Worker manage_exams",async t=>{
  const {sqlite,sendMetadata,audits}=fixture(t,["manage_questions"]); seed(sqlite,"exam","Old");
  assert.equal((await sendMetadata("exam",{cutoffs:{General:70}})).status,403);
  assert.equal(sqlite.prepare("SELECT cutoffs_json FROM exams WHERE id='exam'").get().cutoffs_json,"{}"); assert.equal(audits(),0);
});
test("cutoff migration preserves old exam settings and defaults to empty",()=>{
  const sqlite=new DatabaseSync(":memory:");
  try {
    const directory=new URL("../migrations/",import.meta.url);
    for(const file of readdirSync(directory).filter(x=>x.endsWith(".sql")&&x!=="0030_exam_cutoff_settings.sql").sort()) sqlite.exec(readFileSync(new URL(file,directory),"utf8"));
    sqlite.prepare("INSERT INTO exams(id,exam_name,question_count,negative_marking_text,negative_marking_value,publish_mode) VALUES('old','Existing',100,'1/3',?,'live')").run(1/3);
    sqlite.exec(readFileSync(new URL("0030_exam_cutoff_settings.sql",directory),"utf8"));
    const row=sqlite.prepare("SELECT * FROM exams WHERE id='old'").get();
    assert.equal(row.exam_name,"Existing"); assert.equal(row.question_count,100); assert.equal(row.negative_marking_text,"1/3");
    assert.equal(row.negative_marking_value,1/3); assert.equal(row.publish_mode,"live"); assert.equal(row.cutoffs_json,"{}");
  } finally { sqlite.close(); }
});

test("metadata name edits preserve canonical denormalized-name consistency",async t=>{
  const {sqlite,sendMetadata}=fixture(t); seed(sqlite,"exam","Old");
  assert.equal((await sendMetadata("exam",{examName:"Renamed"})).status,200);
  assert.deepEqual(names(sqlite,"exam"),Array(5).fill("Renamed"));
});
test("metadata omission preserves an independent committed hidden-settings edit",async t=>{
  const {sqlite,sendMetadata,beforeNextBatch}=fixture(t); seed(sqlite,"parent","Parent"); seed(sqlite,"exam","Old");
  beforeNextBatch(()=>sqlite.prepare("UPDATE exams SET question_count=120,negative_marking_text='1/4',negative_marking_value=0.25,parent_exam_id='parent',publish_mode='paused' WHERE id='exam'").run());
  assert.equal((await sendMetadata("exam",{examName:"Renamed",category:"UPSC",cutoffs:{General:70}})).status,200);
  const row=sqlite.prepare("SELECT * FROM exams WHERE id='exam'").get();
  assert.equal(row.question_count,120); assert.equal(row.negative_marking_text,"1/4"); assert.equal(row.negative_marking_value,0.25);
  assert.equal(row.parent_exam_id,"parent"); assert.equal(row.publish_mode,"paused");
});
test("metadata write-time deletion cannot report success or create false audit",async t=>{
  const {sqlite,sendMetadata,beforeNextBatch,audits}=fixture(t); seed(sqlite,"exam","Old");
  beforeNextBatch(()=>sqlite.prepare("DELETE FROM exams WHERE id='exam'").run());
  assert.equal((await sendMetadata("exam",{examName:"New",cutoffs:{General:70}})).status,404); assert.equal(audits(),0);
});
test("metadata write-time sibling collision rejects all changes",async t=>{
  const {sqlite,sendMetadata,beforeNextBatch,audits}=fixture(t); seed(sqlite,"exam","Old");
  beforeNextBatch(()=>seed(sqlite,"sibling","Taken"));
  assert.equal((await sendMetadata("exam",{examName:"Taken",cutoffs:{General:70}})).status,409);
  assert.deepEqual(names(sqlite,"exam"),Array(5).fill("Old")); assert.equal(audits(),0);
  assert.equal(sqlite.prepare("SELECT cutoffs_json FROM exams WHERE id='exam'").get().cutoffs_json,"{}");
});
test("metadata audit failure rolls back fields and cutoff configuration",async t=>{
  const {sqlite,sendMetadata,audits}=fixture(t); seed(sqlite,"exam","Old");
  sqlite.exec("CREATE TRIGGER deny_metadata_audit BEFORE INSERT ON admin_audit_log BEGIN SELECT RAISE(ABORT,'audit unavailable'); END");
  assert.equal((await sendMetadata("exam",{examName:"New",cutoffs:{General:70}})).status,500);
  assert.deepEqual(names(sqlite,"exam"),Array(5).fill("Old")); assert.equal(audits(),0);
  assert.equal(sqlite.prepare("SELECT cutoffs_json FROM exams WHERE id='exam'").get().cutoffs_json,"{}");
});
test("metadata rejects malformed bodies and unknown-only field sets",async t=>{
  const {sqlite,sendMetadata,audits}=fixture(t); seed(sqlite,"exam","Old");
  for(const body of [null,[],{},{unknown:true},{examName:null},{examName:7},{examName:[]},{examName:{}}])
    assert.equal((await sendMetadata("exam",body)).status,400);
  assert.equal(sqlite.prepare("SELECT exam_name FROM exams WHERE id='exam'").get().exam_name,"Old"); assert.equal(audits(),0);
});
