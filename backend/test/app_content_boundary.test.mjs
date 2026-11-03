import test from 'node:test';
import assert from 'node:assert/strict';
import { Hono } from 'hono';
import { appContentRoutes } from '../src/routes/appContent.ts';
import { adminRoutes } from '../src/routes/admin.ts';
import { isolatedD1 } from './support/sqliteD1.mjs';

function fixture(t) {
  const {sqlite,d1}=isolatedD1();t.after(()=>sqlite.close());
  const email='content-manager@example.test';
  sqlite.prepare('INSERT INTO admins(email,created_at) VALUES (?,1)').run(email);
  sqlite.prepare('INSERT INTO admin_rights(email,rights,updated_at) VALUES (?,?,1)').run(email,JSON.stringify(['manage_content']));
  const app=new Hono();
  app.use('*',async(c,next)=>{if(c.req.method==='PUT')c.set('user',{uid:'content-manager',email,isAdmin:true});await next();});
  app.onError((_,c)=>c.json({success:false,error:'Isolated content transaction failed'},500));
  app.route('/api/app-content',appContentRoutes);app.route('/api/admin',adminRoutes);
  const env={DB:d1};
  const request=(type,body)=>app.request('/api/app-content/'+type,{method:'PUT',headers:{'Content-Type':'application/json'},body:typeof body==='string'?body:JSON.stringify(body)},env);
  const snapshot=()=>Object.fromEntries(['app_content','admin_audit_log'].map(table=>[table,sqlite.prepare(`SELECT * FROM ${table}`).all()]));
  return {sqlite,d1,email,app,env,request,snapshot};
}
const valid={title:'Fixture title',body:'Fixture body',supportEmail:'support@example.test',phone:'123',website:'https://example.test',address:'Fixture address'};

test('content routes cannot expose or overwrite another canonical configuration owner',async t=>{
  const f=fixture(t);
  f.sqlite.prepare("INSERT INTO app_content(id,title,body,updated_at) VALUES ('app_config','Internal',?,1)").run(JSON.stringify({maintenance_mode:false}));
  const before=f.snapshot();
  const normal=await f.app.request('/api/admin/config',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({maintenance_mode:true})},f.env);
  assert.equal(normal.status,403);
  for(const type of ['app_config','telegram_emoji_global','unknown']) {
    assert.equal((await f.app.request('/api/app-content/'+type,{},f.env)).status,404,type);
    assert.equal((await f.request(type,valid)).status,404,type);
    assert.deepEqual(f.snapshot(),before);
  }
});
test('supported content aliases preserve public defaults and typed server-confirmed writes',async t=>{
  const f=fixture(t);
  for(const [canonical,legacy] of [['privacy','privacy_policy'],['terms','terms_of_service'],['contact','contact_us']]) {
    const initial=await (await f.app.request('/api/app-content/'+legacy,{},f.env)).json();
    assert.equal(initial.success,true);assert.ok(initial.data.title);assert.ok(initial.data.body);
    assert.equal((await f.request(legacy,{...valid,title:'  Fixture title  ',updatedBy:'untrusted@example.test',updatedAt:1})).status,200);
    const read=await (await f.app.request('/api/app-content/'+canonical,{},f.env)).json();
    for(const field of Object.keys(valid))assert.equal(read.data[field],valid[field]);
    assert.ok(read.data.updatedAt>1);assert.equal(read.data.updatedBy,'');
    assert.equal(f.sqlite.prepare('SELECT updated_by FROM app_content WHERE id=?').get(canonical).updated_by,f.email);
  }
  assert.equal(f.snapshot().admin_audit_log.length,3);
});
test('malformed content input never clears a valid document or creates an audit',async t=>{
  const f=fixture(t);assert.equal((await f.request('privacy',valid)).status,200);const before=f.snapshot();
  const invalid=['{','null','[]','true','{}',{...valid,title:''},{...valid,body:'   '}];
  for(const field of ['title','body','supportEmail','phone','website','address']) {
    for(const value of [42,false,{},[]])invalid.push({...valid,[field]:value});
  }
  for(const field of ['title','body']) {
    const body={...valid};delete body[field];invalid.push(body,{...valid,[field]:null});
  }
  for(const body of invalid) {assert.equal((await f.request('privacy',body)).status,400);assert.deepEqual(f.snapshot(),before);}
});
test('content mutations require fresh rights and current membership',async t=>{
  const f=fixture(t);assert.equal((await f.request('privacy',valid)).status,200);
  f.sqlite.prepare("UPDATE admin_rights SET rights='[]' WHERE email=?").run(f.email);
  const before=f.snapshot();assert.equal((await f.request('privacy',valid)).status,403);assert.deepEqual(f.snapshot(),before);
  f.sqlite.prepare('UPDATE admin_rights SET rights=? WHERE email=?').run(JSON.stringify(['manage_content']),f.email);
  f.sqlite.prepare('DELETE FROM admins WHERE email=?').run(f.email);
  assert.equal((await f.request('privacy',valid)).status,403);assert.deepEqual(f.snapshot(),before);
});
test('content and audit roll back together and missing D1 acknowledgments cannot report success',async t=>{
  const f=fixture(t);assert.equal((await f.request('privacy',valid)).status,200);
  const before=f.snapshot();
  f.sqlite.exec("CREATE TRIGGER reject_content_audit BEFORE INSERT ON admin_audit_log BEGIN SELECT RAISE(ABORT,'fixture audit failure'); END");
  assert.equal((await f.request('privacy',{...valid,title:'Rejected update'})).status,500);assert.deepEqual(f.snapshot(),before);
  f.d1.batch=async()=>[];
  assert.equal((await f.request('privacy',valid)).status,500);assert.deepEqual(f.snapshot(),before);
});
