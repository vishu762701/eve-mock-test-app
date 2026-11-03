import test from 'node:test';
import assert from 'node:assert/strict';
import { Hono } from 'hono';
import { adminRoutes } from '../src/routes/admin.ts';
import { premiumRoutes } from '../src/routes/premium.ts';
import { isolatedD1 } from './support/sqliteD1.mjs';

const valid={isEnabled:true,planName:'EVE Premium',priceInr:199,currency:'INR',durationDays:30,
  isLifetime:false,description:'Mock test access',benefits:['Mock tests'],qrEnabled:true,upiEnabled:true,
  sessionExpiryMinutes:10,merchantVpa:'fixture@bank',merchantName:'EVE'};
function fixture(t) {
  const {sqlite,d1}=isolatedD1();t.after(()=>sqlite.close());
  const email='premium-manager@example.test';
  sqlite.prepare('INSERT INTO admins(email,created_at) VALUES (?,1)').run(email);
  sqlite.prepare('INSERT INTO admin_rights(email,rights,updated_at) VALUES (?,?,1)').run(email,JSON.stringify(['manage_premium']));
  sqlite.exec("INSERT INTO premium_orders(id,user_id,plan_name,amount,payment_method,created_at,expires_at) VALUES ('existing-order','customer','Existing plan',199,'upi',1,9999999999999)");
  sqlite.exec("INSERT INTO premium_entitlements(user_id,activated_at,expires_at,created_at,updated_at) VALUES ('customer',1,9999999999999,1,1)");
  const app=new Hono();
  app.use('*',async(c,next)=>{c.set('user',{uid:'admin-fixture',email,isAdmin:true});await next();});
  app.onError((_,c)=>c.json({success:false,error:'Transaction failed'},500));
  app.route('/api/admin',adminRoutes);app.route('/api/premium',premiumRoutes);
  const env={DB:d1};
  const request=body=>app.request('/api/admin/premium/config',{method:'PUT',headers:{'Content-Type':'application/json'},body:typeof body==='string'?body:JSON.stringify(body)},env);
  const snapshot=()=>Object.fromEntries(['premium_config','admin_audit_log','premium_orders','premium_entitlements'].map(table=>[table,sqlite.prepare(`SELECT * FROM ${table}`).all()]));
  return {sqlite,d1,app,env,request,snapshot};
}

test('premium config rejects malformed JSON and missing authoritative fields without mutations',async t=>{
  const f=fixture(t),before=f.snapshot();
  for(const body of ['{','null','[]','true','42','{}',JSON.stringify(valid).replace('\"priceInr\":199','\"priceInr\":1e9999')]) {
    assert.equal((await f.request(body)).status,400,body);assert.deepEqual(f.snapshot(),before);
  }
  for(const name of Object.keys(valid)) {
    const body={...valid};delete body[name];
    assert.equal((await f.request(body)).status,400,`missing ${name}`);assert.deepEqual(f.snapshot(),before);
  }
});
test('premium config rejects coercion of invalid numbers, flags and payment fields',async t=>{
  const f=fixture(t),before=f.snapshot();
  const cases={priceInr:[null,'199','199oops',0,-1,1.5,1000001,true,{},[]],
    durationDays:[null,'30',0,-1,1.5,3651,false,{},[]],sessionExpiryMinutes:[null,'10',0,-1,1.5,1441,false,{},[]],
    currency:[null,'USD','',123,{},[]],planName:[null,'',' ',123,{},[],'x'.repeat(101)],
    description:[null,'',' ',123,{},[]],merchantVpa:[null,'',' ',123,{},[]],merchantName:[null,'',' ',123,{},[]],
    benefits:[null,'tests',{},[null],[''],[' '],[42]]};
  for(const key of ['isEnabled','isLifetime','qrEnabled','upiEnabled']) cases[key]=[null,0,1,2,'false','true',{},[]];
  for(const [key,values] of Object.entries(cases)) for(const value of values) {
    assert.equal((await f.request({...valid,[key]:value})).status,400,`${key}=${JSON.stringify(value)}`);
    assert.deepEqual(f.snapshot(),before,`${key} cannot change billing or audit state`);
  }
});
test('premium config retains valid boundaries, false flags, legacy GET and server-only payment verification',async t=>{
  const f=fixture(t),before=f.snapshot();
  for(const [priceInr,durationDays,sessionExpiryMinutes] of [[1,1,1],[1000000,3650,1440]]) {
    const body={...valid,priceInr,durationDays,sessionExpiryMinutes,isEnabled:false,isLifetime:true,
      qrEnabled:false,upiEnabled:false,benefits:[],webhookSecret:'client-value-must-never-authorize-payment'};
    assert.equal((await f.request(body)).status,200);
    const read=await (await f.app.request('/api/admin/premium/config',{},f.env)).json();
    assert.equal(read.success,true);assert.equal(read.data.price_inr,priceInr);assert.equal(read.data.duration_days,durationDays);
    assert.equal(read.data.session_expiry_minutes,sessionExpiryMinutes);assert.equal(read.data.is_enabled,0);
    assert.equal(read.data.isEnabled,false);assert.equal(read.data.is_lifetime,1);assert.equal(read.data.isLifetime,true);
    assert.equal(read.data.qrEnabled,false);assert.equal(read.data.upiEnabled,false);assert.deepEqual(read.data.benefits,[]);
    assert.equal('webhook_secret' in read.data,false);
    assert.equal(f.sqlite.prepare("SELECT webhook_secret FROM premium_config WHERE id='default'").get().webhook_secret,'');
    const plan=await (await f.app.request('/api/premium/plan',{},f.env)).json();
    assert.equal(plan.data.isEnabled,false);assert.deepEqual(plan.data.paymentMethods,[]);
  }
  assert.equal(f.snapshot().admin_audit_log.length,2);
  assert.deepEqual(f.snapshot().premium_orders,before.premium_orders);
  assert.deepEqual(f.snapshot().premium_entitlements,before.premium_entitlements);
});
test('premium config requires fresh Worker rights and membership after earlier success',async t=>{
  const f=fixture(t);assert.equal((await f.request(valid)).status,200);
  f.sqlite.prepare('UPDATE admin_rights SET rights=? WHERE email=?').run(JSON.stringify(['view_analytics_logs']),'premium-manager@example.test');
  const before=f.snapshot();assert.equal((await f.request(valid)).status,403);assert.deepEqual(f.snapshot(),before);
  f.sqlite.prepare('DELETE FROM admins WHERE email=?').run('premium-manager@example.test');
  assert.equal((await f.request(valid)).status,403);assert.deepEqual(f.snapshot(),before);
});
test('premium config and audit roll back together on database failure',async t=>{
  const f=fixture(t),before=f.snapshot();
  f.sqlite.exec("CREATE TRIGGER fail_premium_audit BEFORE INSERT ON admin_audit_log BEGIN SELECT RAISE(ABORT,'fixture audit failure'); END");
  assert.equal((await f.request(valid)).status,500);assert.deepEqual(f.snapshot(),before);
});
test('premium config cannot acknowledge missing D1 write evidence',async t=>{
  const f=fixture(t),before=f.snapshot();f.d1.batch=async()=>[];
  assert.equal((await f.request(valid)).status,500);assert.deepEqual(f.snapshot(),before);
});
