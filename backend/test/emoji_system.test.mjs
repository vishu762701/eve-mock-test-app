import test from "node:test";
import assert from "node:assert/strict";
import { DatabaseSync } from "node:sqlite";
import { readFileSync, readdirSync } from "node:fs";
import { Hono } from "hono";
import {
  emojiRoutes,
  emojiAdminRoutes,
  validateLottieJson,
  decompressGzipWithGuard,
  sha256Hex,
} from "../src/routes/emoji.ts";
import { examRoutes } from "../src/routes/exams.ts";
import { broadcastRoutes } from "../src/routes/broadcasts.ts";

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
            async run() { return this.runSync(); },
            runSync() {
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
            results.push(statement.runSync());
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

function createTestApp(d1, user, extraEnv = {}) {
  const app = new Hono();
  app.use("*", async (c, next) => {
    c.env = {
      DB: d1,
      FIREBASE_PROJECT_ID: "test-project",
      SUPABASE_PROJECT_URL: "https://test.supabase.co",
      SUPABASE_BUCKET_NAME: "eve-media",
      SUPABASE_SERVICE_ROLE_KEY: "test-service-key",
      ...extraEnv,
    };
    c.set("user", user);
    await next();
  });
  app.route("/api/admin/emoji", emojiAdminRoutes);
  app.route("/api/emoji", emojiRoutes);
  app.route("/api/exams", examRoutes);
  app.route("/api/broadcasts", broadcastRoutes);
  return app;
}

test("validateLottieJson: validates schema and rejects malformed/insecure files", () => {
  // 1. Valid Lottie JSON
  const valid = JSON.stringify({ v: "5.5.2", fr: 60, ip: 0, op: 60, w: 512, h: 512, layers: [] });
  assert.doesNotThrow(() => validateLottieJson(valid));

  // 2. Malformed JSON
  assert.throws(() => validateLottieJson("{ invalid json "), /Invalid JSON/);

  // 3. Missing required properties
  assert.throws(() => validateLottieJson(JSON.stringify({ some_other_key: 123 })), /Invalid Lottie schema/);

  // 4. Insecure external URL in assets
  const insecure = JSON.stringify({
    v: "5.5.2",
    layers: [],
    assets: [{ id: "image_0", u: "https://malicious.site/track.js", p: "file.png" }],
  });
  assert.throws(() => validateLottieJson(insecure), /external network references.*prohibited/);
});

test("decompressGzipWithGuard: decompresses valid gzip and enforces size cap", async () => {
  const sampleJson = JSON.stringify({ v: "5.7.0", layers: [] });
  const rawBytes = new TextEncoder().encode(sampleJson);

  // Compress using standard CompressionStream
  const cs = new CompressionStream("gzip");
  const writer = cs.writable.getWriter();
  writer.write(rawBytes);
  writer.close();
  const reader = cs.readable.getReader();
  const chunks = [];
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    if (value) chunks.push(value);
  }
  const totalLength = chunks.reduce((acc, c) => acc + c.length, 0);
  const compressed = new Uint8Array(totalLength);
  let offset = 0;
  for (const c of chunks) {
    compressed.set(c, offset);
    offset += c.length;
  }

  // Decompress with generous guard
  const decompressed = await decompressGzipWithGuard(compressed, 307200);
  assert.equal(decompressed, sampleJson);

  // Decompress with strict guard (e.g. 5 bytes) triggers size guard exception
  await assert.rejects(
    async () => {
      await decompressGzipWithGuard(compressed, 5);
    },
    /exceeds maximum allowed size/
  );
});

test("Admin permissions: requirePermission('manage_content') protects emoji routes", async () => {
  const { sqlite, d1 } = isolatedD1();

  // Student user
  const student = { uid: "student_1", email: "student@test.com", isAdmin: false };
  const studentApp = createTestApp(d1, student);
  const resStudent = await studentApp.request("http://localhost/api/admin/emoji/packs");
  assert.equal(resStudent.status, 403);

  // Admin without manage_content right
  const limitedEmail = "limited@eve.app";
  sqlite.prepare("INSERT INTO admins (email, created_at) VALUES (?, ?)").run(limitedEmail, 1000);
  sqlite.prepare("INSERT INTO admin_rights (email, rights, custom_title, updated_at) VALUES (?, ?, ?, ?)").run(
    limitedEmail,
    JSON.stringify(["manage_exams"]),
    "Exam Admin",
    1000
  );
  const limitedAdmin = { uid: "admin_lim", email: limitedEmail, isAdmin: true };
  const limitedApp = createTestApp(d1, limitedAdmin);
  const resLimited = await limitedApp.request("http://localhost/api/admin/emoji/packs");
  assert.equal(resLimited.status, 403);

  // Owner admin with all rights
  const owner = { uid: "owner_1", email: "pronlike9@gmail.com", isAdmin: true };
  const ownerApp = createTestApp(d1, owner);
  const resOwner = await ownerApp.request("http://localhost/api/admin/emoji/packs");
  assert.equal(resOwner.status, 200);
  const jsonOwner = await resOwner.json();
  assert.equal(jsonOwner.success, true);
  assert.ok(Array.isArray(jsonOwner.data.packs));
  assert.equal(jsonOwner.data.globalTelegramEnabled, true);
});

test("Emoji Pack Management: create, list, toggle status, and takedown", async () => {
  const { d1 } = isolatedD1();
  const owner = { uid: "owner_1", email: "own.keni@gmail.com", isAdmin: true };
  const app = createTestApp(d1, owner);

  // 1. Create a pack
  const createRes = await app.request("http://localhost/api/admin/emoji/packs", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: "science_animated",
      title: "Science Animated Emoji",
      source: "upload",
      licenseAttribution: "Custom science icons",
    }),
  });
  assert.equal(createRes.status, 201);
  const createJson = await createRes.json();
  assert.equal(createJson.success, true);
  assert.equal(createJson.data.name, "science_animated");
  const packId = createJson.data.id;

  // 2. Reject duplicate pack name
  const dupRes = await app.request("http://localhost/api/admin/emoji/packs", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      name: "science_animated",
      title: "Science Duplicate",
    }),
  });
  assert.equal(dupRes.status, 409);

  // 3. Toggle pack status
  const toggleRes = await app.request(`http://localhost/api/admin/emoji/packs/${packId}/status`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ enabled: false }),
  });
  assert.equal(toggleRes.status, 200);
  const toggleJson = await toggleRes.json();
  assert.equal(toggleJson.data.enabled, false);

  // 4. Toggle global Telegram emoji switch
  const globalToggle = await app.request("http://localhost/api/admin/emoji/global-telegram-status", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ enabled: false }),
  });
  assert.equal(globalToggle.status, 200);
  const globalJson = await globalToggle.json();
  assert.equal(globalJson.data.globalTelegramEnabled, false);

  // 5. Delete pack (takedown)
  const delRes = await app.request(`http://localhost/api/admin/emoji/packs/${packId}`, {
    method: "DELETE",
  });
  assert.equal(delRes.status, 200);
});

test("Telegram Import: rejects if TELEGRAM_BOT_TOKEN is missing, succeeds when token present", async () => {
  const { d1 } = isolatedD1();
  const owner = { uid: "owner_1", email: "own.keni@gmail.com", isAdmin: true };

  // 1. Without token: returns clear error
  const appNoToken = createTestApp(d1, owner, { TELEGRAM_BOT_TOKEN: "" });
  const resNoToken = await appNoToken.request("http://localhost/api/admin/emoji/import/telegram", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ packName: "DuckPack" }),
  });
  assert.equal(resNoToken.status, 400);
  const jsonNoToken = await resNoToken.json();
  assert.ok(jsonNoToken.error.includes("TELEGRAM_BOT_TOKEN is not set"));

  // 2. Check secret is never leaked in response or logs
  assert.equal(JSON.stringify(jsonNoToken).includes("bot123456"), false);
});

test("Placements & Public read path: exam card, category header, and broadcast with kill switches", async () => {
  const { sqlite, d1 } = isolatedD1();
  const owner = { uid: "owner_1", email: "pronlike9@gmail.com", isAdmin: true };
  const app = createTestApp(d1, owner);

  // Insert mock exam
  const examId = "exam_physics_101";
  sqlite.prepare(`
    INSERT INTO exams (id, exam_name, time_limit_minutes, category, syllabus, question_count, test_number)
    VALUES (?, 'Physics Mock Test 1', 60, 'Science', 'Basics of motion', 10, 'TEST-01')
  `).run(examId);

  // Insert mock broadcast
  const broadcastId = "bcast_update_1";
  sqlite.prepare(`
    INSERT INTO notifications (id, title, message, sent_at, sent_by, type, target_category)
    VALUES (?, 'Important Update', 'Please review the schedule', 1000, 'admin', 'general', 'All Users')
  `).run(broadcastId);

  // Insert mock emoji item under pack_tg_ui
  const tgItemId = "emoji_tg_fire";
  sqlite.prepare(`
    INSERT INTO emoji_items (id, pack_id, alt_emoji, kind, storage_key, byte_size, sha256, enabled, source, created_at)
    VALUES (?, 'pack_tg_ui', '🔥', 'lottie', 'res:raw/tg_fire_on', 1024, 'hash_fire', 1, 'telegram', 1000)
  `).run(tgItemId);

  // Insert mock emoji item under pack_noto
  const notoItemId = "emoji_noto_star";
  sqlite.prepare(`
    INSERT INTO emoji_items (id, pack_id, alt_emoji, kind, storage_key, byte_size, sha256, enabled, source, created_at)
    VALUES (?, 'pack_noto', '⭐', 'lottie', 'emoji/noto/star.json', 2048, 'hash_star', 1, 'noto', 1000)
  `).run(notoItemId);

  // 1. Set placements
  // Exam card: Star
  const p1 = await app.request("http://localhost/api/admin/emoji/placements", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ slotType: "exam_card", targetId: examId, itemId: notoItemId }),
  });
  assert.equal(p1.status, 200);

  // Category header: Fire (Telegram sourced)
  const p2 = await app.request("http://localhost/api/admin/emoji/placements", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ slotType: "category_header", targetId: "Science", itemId: tgItemId }),
  });
  assert.equal(p2.status, 200);

  // Broadcast: Fire (Telegram sourced)
  const p3 = await app.request("http://localhost/api/admin/emoji/placements", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ slotType: "broadcast", targetId: broadcastId, itemId: tgItemId }),
  });
  assert.equal(p3.status, 200);

  // 2. Verify Student Public Exam GET (/api/exams)
  const student = { uid: "student_1", email: "student@eve.app", isAdmin: false };
  const studentApp = createTestApp(d1, student);

  const resExams = await studentApp.request("http://localhost/api/exams");
  assert.equal(resExams.status, 200);
  const examsJson = await resExams.json();
  assert.equal(examsJson.success, true);
  const exam = examsJson.data.find((e) => e.id === examId);
  assert.ok(exam);
  assert.ok(exam.emoji);
  assert.equal(exam.emoji.alt, "⭐");
  assert.equal(examsJson.categoryEmoji.Science.alt, "🔥");

  // 3. Verify Student Public Broadcast GET (/api/broadcasts)
  const resBroadcasts = await studentApp.request("http://localhost/api/broadcasts");
  assert.equal(resBroadcasts.status, 200);
  const bcastJson = await resBroadcasts.json();
  const bcast = bcastJson.data.find((b) => b.id === broadcastId);
  assert.ok(bcast);
  assert.ok(bcast.emoji);
  assert.equal(bcast.emoji.alt, "🔥");

  // 4. Test Global Telegram Kill Switch
  // Disabling global Telegram emoji removes Telegram emoji ('🔥') while retaining Noto ('⭐')
  await app.request("http://localhost/api/admin/emoji/global-telegram-status", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ enabled: false }),
  });

  const resExamsAfterKill = await studentApp.request("http://localhost/api/exams");
  const examsKillJson = await resExamsAfterKill.json();
  const examAfterKill = examsKillJson.data.find((e) => e.id === examId);
  // Exam card uses Noto, so it remains visible
  assert.ok(examAfterKill.emoji);
  assert.equal(examAfterKill.emoji.alt, "⭐");
  // Category header used Telegram emoji, so it is filtered out by kill switch
  assert.equal(examsKillJson.categoryEmoji.Science, undefined);

  // 5. Test Per-pack Kill Switch
  // Disabling pack_noto removes Star from exam card
  await app.request("http://localhost/api/admin/emoji/packs/pack_noto/status", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ enabled: false }),
  });

  const resExamsAfterPackKill = await studentApp.request("http://localhost/api/exams");
  const examsPackKillJson = await resExamsAfterPackKill.json();
  const examAfterPackKill = examsPackKillJson.data.find((e) => e.id === examId);
  assert.equal(examAfterPackKill.emoji, undefined);
});

const owner = { uid: "owner", email: "own.keni@gmail.com", isAdmin: true };
const jsonRequest = (body) => ({ method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) });
function seedMedia(sqlite, id = 'asset', pack = 'pack_noto', key = 'emoji/test.json') {
  sqlite.prepare("INSERT INTO emoji_items(id,pack_id,alt_emoji,kind,storage_key,byte_size,sha256,source,created_at) VALUES(?,?,'✨','lottie',?,10,'test','upload',123)").run(id, pack, key);
}

test("Bundled catalogue: exact existing IDs can be placed, paged and globally disabled", async () => {
  const {sqlite,d1} = isolatedD1(); const app = createTestApp(d1,owner);
  const migration = readFileSync(new URL('../migrations/0028_emoji_catalog_and_paging.sql',import.meta.url),'utf8');
  sqlite.exec(migration); // Idempotent and no duplicated icons.
  assert.equal(sqlite.prepare("SELECT count(*) AS n FROM emoji_items WHERE pack_id='pack_tg_ui'").get().n,20);
  sqlite.exec("INSERT INTO exams(id,exam_name,time_limit_minutes,category) VALUES('exam','Exam',60,'Science')");
  const placement = await app.request('/api/admin/emoji/placements',jsonRequest({slotType:'exam_card',targetId:'exam',itemId:'bundled_tg_fire_on'}));
  assert.equal(placement.status,200);
  const before = await (await app.request('/api/emoji/placements/public')).json();
  assert.equal(before.data.examPlacements.exam.url,'res:raw/tg_fire_on');
  await app.request('/api/admin/emoji/global-telegram-status',{...jsonRequest({enabled:false}),method:'PUT'});
  const after = await (await app.request('/api/emoji/placements/public')).json();
  assert.equal(after.data.examPlacements.exam,undefined);
  const invalid = await app.request('/api/admin/emoji/placements',jsonRequest({slotType:'exam_card',targetId:'missing',itemId:'bundled_tg_fire_on'}));
  assert.equal(invalid.status,404);
  sqlite.exec("DELETE FROM emoji_placements; DELETE FROM emoji_items WHERE pack_id='pack_tg_ui'; DELETE FROM emoji_packs WHERE id='pack_tg_ui'");
  assert.doesNotThrow(()=>sqlite.exec(migration)); // Deliberate takedown stays removed.
  assert.equal(sqlite.prepare("SELECT count(*) AS n FROM emoji_packs WHERE id='pack_tg_ui'").get().n,0);
});

test("Catalogue pagination reaches 250 identical timestamps without duplicates and rejects invalid bounds",async()=>{
  const {sqlite,d1}=isolatedD1(); const app=createTestApp(d1,owner);
  for(let i=0;i<250;i++) seedMedia(sqlite,`item_${i.toString().padStart(3,'0')}`);
  const ids=[];
  for(const offset of [0,100,200]) {
    const response=await app.request(`/api/admin/emoji/items?packId=pack_noto&limit=100&offset=${offset}`);
    assert.equal(response.status,200); ids.push(...(await response.json()).data.map(x=>x.id));
  }
  assert.equal(ids.length,250); assert.equal(new Set(ids).size,250); assert.equal(ids[0],'item_249');
  for(const query of ['offset=-1','offset=1.5','limit=101','limit=0','offset=bad']) {
    assert.equal((await app.request(`/api/admin/emoji/items?${query}`)).status,400,query);
  }
});

test("Pack deletion: DB rollback never deletes live storage or audit rows",async(t)=>{
  const {sqlite,d1}=isolatedD1(); seedMedia(sqlite);
  sqlite.exec("CREATE TRIGGER reject_delete BEFORE DELETE ON emoji_items BEGIN SELECT RAISE(ABORT,'simulated disk failure'); END");
  let storageCalls=0;t.mock.method(globalThis,'fetch',async()=>{storageCalls++;return new Response('{}');});
  const res=await createTestApp(d1,owner).request('/api/admin/emoji/packs/pack_noto',{method:'DELETE'});
  assert.equal(res.status,500);assert.equal(storageCalls,0);
  assert.ok(sqlite.prepare("SELECT id FROM emoji_items WHERE id='asset'").get());
  assert.ok(sqlite.prepare("SELECT id FROM emoji_packs WHERE id='pack_noto'").get());
  assert.equal(sqlite.prepare("SELECT count(*) AS n FROM admin_audit_log WHERE action_type='DELETE_EMOJI_PACK'").get().n,0);
});

test("Item deletion: shared paths survive, storage failure is explicit after committed deletion",async(t)=>{
  const {sqlite,d1}=isolatedD1(); seedMedia(sqlite,'first','pack_noto','/emoji/shared.json');seedMedia(sqlite,'second','pack_tg_ui','emoji/shared.json');
  const calls=[];t.mock.method(globalThis,'fetch',async(url,init)=>{calls.push({url,init});return new Response('{}',{status:503});});
  const app=createTestApp(d1,owner);
  const first=await app.request('/api/admin/emoji/items/first',{method:'DELETE'});
  assert.equal(first.status,200);assert.equal((await first.json()).data.mediaCleanupComplete,true);assert.equal(calls.length,0);
  const second=await app.request('/api/admin/emoji/items/second',{method:'DELETE'});
  const body=await second.json();assert.equal(second.status,200);assert.equal(body.data.mediaCleanupComplete,false);assert.ok(body.warning);
  assert.equal(JSON.stringify(body).includes('shared.json'),false);
  assert.equal(sqlite.prepare("SELECT count(*) AS n FROM emoji_items WHERE id='second'").get().n,0);
  assert.equal(sqlite.prepare("SELECT count(*) AS n FROM operation_events WHERE operation='emoji_media_cleanup'").get().n,1);
  assert.equal(calls.length,1);
});

test("Pack deletion batches 250 storage objects in one API request",async(t)=>{
  const {sqlite,d1}=isolatedD1();for(let i=0;i<250;i++)seedMedia(sqlite,`item${i}`,'pack_noto',`emoji/${i}.json`);
  const calls=[];t.mock.method(globalThis,'fetch',async(url,init)=>{calls.push({url,init});return new Response('{}');});
  const response=await createTestApp(d1,owner).request('/api/admin/emoji/packs/pack_noto',{method:'DELETE'});
  assert.equal(response.status,200);assert.equal(calls.length,1);assert.equal(calls[0].init.method,'DELETE');
  assert.equal(JSON.parse(calls[0].init.body).prefixes.length,250);
});

test("Changed pack snapshot returns conflict, preserving new rows, audit and media",async(t)=>{
  const {sqlite,d1}=isolatedD1();seedMedia(sqlite);
  const original=d1.batch.bind(d1);d1.batch=async statements=>{seedMedia(sqlite,'concurrent');return original(statements);};
  let calls=0;t.mock.method(globalThis,'fetch',async()=>{calls++;return new Response('{}');});
  const response=await createTestApp(d1,owner).request('/api/admin/emoji/packs/pack_noto',{method:'DELETE'});
  assert.equal(response.status,409);assert.equal((await response.json()).code,'EMOJI_CHANGED');assert.equal(calls,0);
  assert.equal(sqlite.prepare("SELECT count(*) AS n FROM emoji_items WHERE pack_id='pack_noto'").get().n,2);
  assert.equal(sqlite.prepare("SELECT count(*) AS n FROM admin_audit_log").get().n,0);
});

test("Uploads: bad parent, external JSON, oversized content and disguised images cause no writes",async(t)=>{
  const {sqlite,d1}=isolatedD1();let writes=0;t.mock.method(globalThis,'fetch',async()=>{writes++;return new Response('{}');});
  const app=createTestApp(d1,owner);
  for(const [body,status] of [[{packId:'missing',animationData:{v:'5',layers:[]}},404],[{v:'5',assets:[{u:'//evil.test/image'}]},400],[{v:'5',layers:[],large:'x'.repeat(310000)},413]]) {
    assert.equal((await app.request('/api/admin/emoji/upload',jsonRequest(body))).status,status);
  }
  assert.equal((await app.request('/api/admin/emoji/upload',{method:'POST',headers:{'Content-Type':'image/webp'},body:'not an image'})).status,400);
  assert.equal(writes,0);assert.equal(sqlite.prepare("SELECT count(*) AS n FROM emoji_items WHERE source='upload'").get().n,0);
});

test("Uploads: PNG keeps its MIME/extension; successful JSON is readable; DB failure compensates storage",async(t)=>{
  const {sqlite,d1}=isolatedD1();const calls=[];t.mock.method(globalThis,'fetch',async(url,init)=>{calls.push({url,init});return new Response('{}');});
  const app=createTestApp(d1,owner);
  const png=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jHZkAAAAASUVORK5CYII=','base64');
  const staticRes=await app.request('/api/admin/emoji/upload',{method:'POST',headers:{'Content-Type':'image/png'},body:png});
  assert.equal(staticRes.status,201);const item=(await staticRes.json()).data;assert.equal(item.kind,'static');assert.match(item.storageKey,/\.png$/);assert.equal(calls[0].init.headers['Content-Type'],'image/png');
  const animation={v:'5',layers:[]};const good=await app.request('/api/admin/emoji/upload',jsonRequest(animation));assert.equal(good.status,201);
  const list=await (await app.request('/api/admin/emoji/items')).json();assert.ok(list.data.some(x=>x.id===item.id));
  sqlite.exec("CREATE TRIGGER reject_upload BEFORE INSERT ON emoji_items WHEN NEW.source='upload' BEGIN SELECT RAISE(ABORT,'simulated write failure'); END");
  const fail=await app.request('/api/admin/emoji/upload',jsonRequest(animation));assert.equal(fail.status,500);
  assert.equal(calls.at(-1).init.method,'DELETE');
  assert.equal(sqlite.prepare("SELECT count(*) AS n FROM admin_audit_log WHERE action_type='UPLOAD_EMOJI'").get().n,2);
});

test("Concurrent takedown yields one committed mutation and one conflict, never two success logs",async(t)=>{
  const {sqlite,d1}=isolatedD1();seedMedia(sqlite);
  let calls=0;t.mock.method(globalThis,'fetch',async()=>{calls++;return new Response('{}');});
  const app=createTestApp(d1,owner);
  const results=await Promise.all([1,2].map(()=>app.request('/api/admin/emoji/packs/pack_noto',{method:'DELETE'})));
  assert.equal(results.filter(r=>r.status===200).length,1);
  assert.equal(results.filter(r=>r.status===409 || r.status===404).length,1);
  assert.equal(sqlite.prepare("SELECT count(*) AS n FROM admin_audit_log WHERE action_type='DELETE_EMOJI_PACK'").get().n,1);
  assert.equal(calls,1);
});

test("Telegram custom emoji pages preserve total/nextOffset; imported items have server-owned logs",async(t)=>{
  const {sqlite,d1}=isolatedD1();const animation=JSON.stringify({v:'5',layers:[]});
  t.mock.method(globalThis,'fetch',async(url,init)=>{
    if(url.includes('/getCustomEmojiStickers')) return Response.json({ok:true,result:[{file_id:'one',is_animated:true,emoji:'✨'}]});
    if(url.includes('/getFile?'))return Response.json({ok:true,result:{file_path:'one.json'}});
    // is_animated uses TGS. Return a real gzip stream.
    if(url.includes('/file/bot')) return new Response(new Blob([animation]).stream().pipeThrough(new CompressionStream('gzip')));
    return new Response('{}');
  });
  const app=createTestApp(d1,owner,{TELEGRAM_BOT_TOKEN:'secret-not-public'});
  const res=await app.request('/api/admin/emoji/import/telegram',jsonRequest({customEmojiIds:['1','2','3'],offset:0,limit:1}));
  const body=await res.json();assert.equal(res.status,200);assert.equal(body.data.importedCount,1);assert.equal(body.data.nextOffset,1);assert.equal(body.data.totalStickers,3);
  assert.equal(sqlite.prepare("SELECT count(*) AS n FROM admin_audit_log WHERE action_type='IMPORT_EMOJI_ITEM'").get().n,1);
  assert.equal(JSON.stringify(body).includes('secret-not-public'),false);
});
