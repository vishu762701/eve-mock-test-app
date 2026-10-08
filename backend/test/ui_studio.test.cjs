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

// Execute the actual SQL, including conditional writes and transaction rollback.
const { DatabaseSync } = require('node:sqlite');
class MockUiStudioD1 {
  constructor() {
    this.db = new DatabaseSync(':memory:');
    this.db.exec(fs.readFileSync(require('node:path').join(__dirname, '../migrations/0012_ui_studio.sql'), 'utf8'));
    this.db.exec('CREATE TABLE admin_audit_log (id TEXT PRIMARY KEY, action_type TEXT, description TEXT, admin_email TEXT, timestamp INTEGER)');
  }
  get published() { return this.db.prepare('SELECT * FROM ui_studio_published').all(); }
  get drafts() { return this.db.prepare('SELECT * FROM ui_studio_drafts').all(); }
  get versions() { return this.db.prepare('SELECT * FROM ui_studio_versions').all(); }
  get auditLog() { return this.db.prepare('SELECT * FROM ui_studio_audit_log').all(); }
  get adminAuditLog() { return this.db.prepare('SELECT * FROM admin_audit_log').all(); }
  async batch(stmts) {
    this.db.exec('BEGIN');
    try { const results = stmts.map(s => s.execute()); this.db.exec('COMMIT'); return results; }
    catch(e) { this.db.exec('ROLLBACK'); throw e; }
  }
  prepare(sql) {
    const query=this.db.prepare(sql); let values=[];
    const stmt={
      bind: (...args) => { values=args;return stmt; },
      first: async () => query.get(...values) || null,
      all: async () => ({ results: query.all(...values) }),
      execute: () => ({ success:true,meta:{ changes:Number(query.run(...values).changes) } }),
      run: async () => stmt.execute()
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

async function saveAndPublish(app, env, body) {
  const remote = await (await request(app,env,'/api/admin/ui-studio/draft')).json();
  const saved = await request(app,env,'/api/admin/ui-studio/draft','PUT',{config:body.config,baseRevision:remote.data.config.revision});
  assert.equal(saved.status,200);
  const config=(await saved.json()).data.config;
  const published=await request(app,env,'/api/admin/ui-studio/publish','POST',{...body,config});
  assert.equal(published.status,200);
  return published;
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
  assert.equal(res.headers.get("cache-control"), "no-store");
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
test("optimistic concurrency detects conflict and rejects a force overwrite bypass", async () => {
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
  assert.equal(conflictData.data.serverDraft.revision, currentRevision);

  // 3. Force overwrite bypasses conflict
  const putForce = await request(app, env, "/api/admin/ui-studio/draft", "PUT", {
    config: conflictingConfig,
    baseRevision: "rev-outdated-12345",
    force: true
  });
  assert.equal(putForce.status, 409);
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
  await saveAndPublish(app, env, {
    config: { screens: { home: { components: { card: { appearance: { cornerRadius: 8 } } } } } },
    notes: "v1 config",
  });

  // Publish v2
  await saveAndPublish(app, env, {
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
  const restoreRes = await request(app, env, "/api/admin/ui-studio/restore/1", "POST", { target: "draft", baseRevision: JSON.parse(db.drafts[0].config_json).revision });
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

  await saveAndPublish(app, env, {
    config: { screens: { home: {} } },
    notes: "v1",
  });
  assert.equal(db.published.length, 1);

  const resetRes = await request(app, env, "/api/admin/ui-studio/reset", "POST", { target: "all", baseRevision: JSON.parse(db.drafts[0].config_json).revision, publishedVersion: db.published[0].version });
  assert.equal(resetRes.status, 200);
  assert.equal(db.published.length, 0);

  // Check public endpoint returns null config
  const pubRes = await request(app, env, "/api/ui-studio/published");
  const pubData = await pubRes.json();
  assert.equal(pubData.data.config, null);
  assert.equal(pubData.data.version, 0);
});

test('actions, protected elements, invalid parentage and cycles are rejected server-side', () => {
  const validate = c => validateUiStudioConfig({screens:{home:{components:c}}}).valid;
  assert.equal(validate({native_btnNext:{visible:false,isProtected:false}}),false);
  assert.equal(validate({native_btnNext:{enabled:false}}),false);
  assert.equal(validate({custom_x:{type:"card",layout:{height:"999999999"}}}),false);
  assert.equal(validate({native_btnNext:{appearance:{opacity:0}}}),true);
  assert.equal(validate({custom_x:{type:'button',actions:{actionType:'open_url',actionTarget:'javascript:alert(1)'}}}),false);
  assert.equal(validate({custom_x:{type:'button',actions:{actionType:'navigate',actionTarget:'submit'}}}),false);
  assert.equal(validate({custom_a:{type:'card',parentId:'custom_b'},custom_b:{type:'card',parentId:'custom_a'}}),false);
  assert.equal(validate({custom_x:{type:'text',parentId:'missing'}}),false);
  assert.equal(validate({custom_x:{type:'button',actions:{actionType:'open_url',actionTarget:'https://example.com/path'}}}),true);
});

test('two admins cannot overwrite newer fields; draft stays isolated until verified publication',async()=>{
  const db=new MockUiStudioD1();const {app,env}=createApp(db,{isAdmin:true});
  const initial={screens:{home:{components:{custom_glass:{id:'custom_glass',type:'card',material:{blurRadius:12,materialOpacity:0.65,tintColor:'#007AFF',tintOpacity:0.2},appearance:{opacity:0.2,iconTint:'#FFFFFF',shape:'capsule',highlightColor:'#FFFFFF',highlightOpacity:.4},animation:{pressScale:.94,springRelease:true,hapticFeedback:false},states:{pressedBackgroundColor:'#007AFF',focusedBackgroundColor:'#FFFFFF'}},custom_label:{id:'custom_label',type:'text',parentId:'custom_glass',content:{title:'Keep foreground sharp'}}}}}};
  const save=await request(app,env,'/api/admin/ui-studio/draft','PUT',{config:initial});
  const first=(await save.json()).data.config;
  assert.equal((await (await request(app,env,'/api/ui-studio/published')).json()).data.config,null);
  const edited=structuredClone(first);edited.screens.home.components.custom_glass.material.blurRadius=20;
  const second=await request(app,env,'/api/admin/ui-studio/draft','PUT',{config:edited,baseRevision:first.revision});
  const saved=(await second.json()).data.config;
  assert.equal((await request(app,env,'/api/admin/ui-studio/draft','PUT',{config:first,baseRevision:first.revision,force:true})).status,409);
  assert.equal((await request(app,env,'/api/admin/ui-studio/publish','POST',{config:first})).status,409);
  assert.equal((await request(app,env,'/api/admin/ui-studio/publish','POST',{config:saved})).status,200);
  const fresh=(await (await request(app,env,'/api/ui-studio/published')).json()).data.config;
  assert.deepEqual(fresh.screens,saved.screens);assert.equal(fresh.revision,saved.revision);
  const student=createApp(db,{isAdmin:false});
  assert.equal((await request(student.app,env,'/api/admin/ui-studio/publish','POST',{config:saved})).status,403);
});

test('simultaneous saves use an atomic SQL compare-and-swap',async()=>{
  const db=new MockUiStudioD1();const {app,env}=createApp(db,{isAdmin:true});
  const first=(await (await request(app,env,'/api/admin/ui-studio/draft','PUT',{config:{screens:{}}})).json()).data.config;
  const responses=await Promise.all(['#000000','#FFFFFF'].map(backgroundColor=>request(app,env,'/api/admin/ui-studio/draft','PUT',{baseRevision:first.revision,config:{...first,screens:{home:{backgroundColor,components:{}}}}})));
  assert.deepEqual(responses.map(r=>r.status).sort(),[200,409]);
});

test('stale restore and reset cannot replace newer admin work or publication', async () => {
  const db = new MockUiStudioD1();
  const {app,env} = createApp(db,{isAdmin:true});
  await saveAndPublish(app,env,{config:{screens:{home:{}}}});
  const original = JSON.parse(db.drafts[0].config_json);
  const saved = await request(app,env,'/api/admin/ui-studio/draft','PUT',{config:{screens:{profile:{}}},baseRevision:original.revision});
  assert.equal(saved.status,200);
  const newer = db.drafts[0].config_json;
  assert.equal((await request(app,env,'/api/admin/ui-studio/restore/1','POST',{target:'draft',baseRevision:original.revision})).status,409);
  assert.equal((await request(app,env,'/api/admin/ui-studio/reset','POST',{target:'all',baseRevision:original.revision,publishedVersion:1})).status,409);
  assert.equal(db.drafts[0].config_json,newer);
  assert.equal(db.published.length,1);
  assert.equal((await request(app,env,'/api/admin/ui-studio/reset','POST',{target:'published',publishedVersion:0})).status,409);
  const revision = JSON.parse(newer).revision;
  assert.equal((await request(app,env,'/api/admin/ui-studio/restore/1','POST',{target:'publish',baseRevision:revision,publishedVersion:0})).status,409);
  assert.equal((await request(app,env,'/api/admin/ui-studio/restore/1','POST',{target:'publish',baseRevision:revision,publishedVersion:1})).status,200);
  assert.equal(db.published[0].version,2);
  assert.equal(JSON.parse(db.published[0].config_json).status,'published');
});


test('visual fields are editable on protected native controls while behavior remains protected',()=>{
  const appearance={opacity:0,iconTint:'#FF007AFF',shape:'capsule',highlightColor:'#FFFFFF',highlightOpacity:.4};
  const config={screens:{home:{components:{native_btnNext:{id:'native_btnNext',isProtected:true,appearance,animation:{pressScale:.94,springRelease:true,hapticFeedback:true},states:{focusedBackgroundColor:'#123456'}}}}}};
  assert.equal(validateUiStudioConfig(config).valid,true);
  for(const patch of [{iconTint:'bad'},{shape:'apple_native'},{highlightOpacity:2}]){
    config.screens.home.components.native_btnNext.appearance={...appearance,...patch};assert.equal(validateUiStudioConfig(config).valid,false);
  }
  config.screens.home.components.native_btnNext.appearance=appearance;
  config.screens.home.components.native_btnNext.actions={actionType:'navigate',actionTarget:'home'};
  assert.equal(validateUiStudioConfig(config).valid,false);
});

test('fresh public readback never permits stale CDN/browser configuration including an empty reset', async () => {
  const db=new MockUiStudioD1();const {app,env}=createApp(db,{isAdmin:true});
  const empty=await request(app,env,'/api/ui-studio/published');
  assert.equal(empty.headers.get('cache-control'),'no-store');
  await saveAndPublish(app,env,{config:{screens:{home:{components:{native_panelHomeBanner:{appearance:{backgroundColor:'#123456',opacity:.35},material:{blurRadius:22,tintColor:'#FFFFFF',tintOpacity:.2,materialOpacity:.6}}}}}}});
  const readback=await request(app,env,'/api/ui-studio/published');
  assert.equal(readback.headers.get('cache-control'),'no-store');
  const json=await readback.json();assert.equal(json.data.config.screens.home.components.native_panelHomeBanner.appearance.opacity,.35);
  const conditional=await request(app,env,'/api/ui-studio/published','GET',null,{'if-none-match':readback.headers.get('etag')});
  assert.equal(conditional.headers.get('cache-control'),'no-store');
});
test('validation cannot publish a blur radius the cross-version renderer silently clamps', () => {
  for(const radius of [26,50]) {
    const result=validateUiStudioConfig({screens:{home:{components:{native_panelHomeBanner:{material:{blurRadius:radius}}}}}});
    assert.equal(result.valid,false);assert.ok(result.errors.some(e=>e.includes('25')));
  }
  assert.equal(validateUiStudioConfig({screens:{home:{components:{native_panelHomeBanner:{material:{blurRadius:25},appearance:{opacity:0}}}}}}).valid,true);
});
