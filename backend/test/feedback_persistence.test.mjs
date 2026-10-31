import test from 'node:test';
import assert from 'node:assert/strict';
import { Hono } from 'hono';
import { isolatedD1 } from './support/sqliteD1.mjs';
import { feedbackRoutes } from '../src/routes/feedback.ts';

const owner={uid:'owner',email:'own.keni@gmail.com',isAdmin:true};
const student={uid:'student',email:'student@example.test',isAdmin:false};
function appFor(d1,user=owner) {
 const app=new Hono(); app.use('*',async(c,next)=>{c.env={DB:d1};c.set('user',user);await next();});
 app.onError(()=>new Response('Test database failure',{status:500}));
 app.route('/api/feedback',feedbackRoutes);return app;
}
const write=(body,method='POST')=>({method,headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
async function message(app) {const res=await app.request('/api/feedback/messages',write({message:'Help with this question'}));assert.equal(res.status,201);return(await res.json()).data.id;}

 test('Admin replies persist and read back with message state/audit; missing or failed saves cannot succeed',async()=>{
 const {sqlite,d1}=isolatedD1();const app=appFor(d1);const id=await message(app);
 let res=await app.request(`/api/feedback/messages/${id}/reply`,write({reply:'We will check.'}));assert.equal(res.status,200);
 let list=await(await app.request('/api/feedback/messages')).json();assert.equal(list.data[0].replyText,'We will check.');assert.equal(list.data[0].read,true);assert.ok(list.data[0].repliedAt>0);
 assert.equal(sqlite.prepare("SELECT count(*) AS n FROM admin_audit_log WHERE action_type='FEEDBACK_REPLIED'").get().n,1);
 sqlite.exec("CREATE TRIGGER fail_reply BEFORE UPDATE ON feedback_admin_replies BEGIN SELECT RAISE(ABORT,'simulated failure'); END");
 res=await app.request(`/api/feedback/messages/${id}/reply`,write({reply:'Must not replace saved reply'}));assert.equal(res.status,500);
 list=await(await app.request('/api/feedback/messages')).json();assert.equal(list.data[0].replyText,'We will check.');
 assert.equal((await app.request('/api/feedback/messages/missing/reply',write({reply:'Reply'}))).status,404);
 for(const reply of ['',{},'x'.repeat(1001)]) assert.equal((await app.request(`/api/feedback/messages/${id}/reply`,write({reply}))).status,400);
 assert.equal(sqlite.prepare("SELECT count(*) AS n FROM admin_audit_log WHERE action_type='FEEDBACK_REPLIED'").get().n,1);
});

test('Legacy reply columns remain readable and new reply storage takes precedence without ALTER',async()=>{
 const {sqlite,d1}=isolatedD1();const app=appFor(d1);const id=await message(app);
 sqlite.exec("ALTER TABLE feedback_messages ADD COLUMN reply_text TEXT; ALTER TABLE feedback_messages ADD COLUMN replied_at INTEGER");
 sqlite.prepare('UPDATE feedback_messages SET reply_text=?,replied_at=? WHERE id=?').run('Legacy reply',123,id);
 let list=await(await app.request('/api/feedback/messages')).json();assert.equal(list.data[0].replyText,'Legacy reply');assert.equal(list.data[0].repliedAt,123);
 assert.equal((await app.request(`/api/feedback/messages/${id}/reply`,write({reply:'Current reply'}))).status,200);
 list=await(await app.request('/api/feedback/messages')).json();assert.equal(list.data[0].replyText,'Current reply');
 assert.equal((await app.request(`/api/feedback/messages/${id}`,{method:'DELETE'})).status,200);
 assert.equal(sqlite.prepare('SELECT count(*) AS n FROM feedback_admin_replies').get().n,0);
});

test('Reply moderation binds parent ID, protects private identities and logs only actual changes',async()=>{
 const {sqlite,d1}=isolatedD1();const admin=appFor(d1);const user=appFor(d1,student);
 const posts=[];for(const title of ['One','Two']) {const res=await admin.request('/api/feedback/posts',write({title,message:'Feedback'}));posts.push((await res.json()).data.id);}
 const replyRes=await user.request(`/api/feedback/posts/${posts[0]}/replies`,write({text:'Please clarify',email:'forged@example.test'}));assert.equal(replyRes.status,201);const reply=(await replyRes.json()).data.id;
 assert.equal((await user.request(`/api/feedback/posts/${posts[0]}/replies`)).status,403);
 assert.equal((await user.request(`/api/feedback/posts/${posts[0]}/replies/${reply}/read`,write({},'PUT'))).status,403);
 for(const [method,suffix]of [['PUT','/read'],['DELETE','']]) {
   const res=await admin.request(`/api/feedback/posts/${posts[1]}/replies/${reply}${suffix}`,method==='PUT'?write({},method):{method});assert.equal(res.status,404);
 }
 assert.ok(sqlite.prepare('SELECT id FROM feedback_post_replies WHERE id=?').get(reply));
 let list=await(await admin.request(`/api/feedback/posts/${posts[0]}/replies`)).json();assert.equal(list.data[0].email,student.email);
 assert.equal((await admin.request(`/api/feedback/posts/${posts[0]}/replies/${reply}/read`,write({},'PUT'))).status,200);
 assert.equal((await admin.request(`/api/feedback/posts/${posts[0]}/replies/${reply}`,{method:'DELETE'})).status,200);
 assert.equal(sqlite.prepare("SELECT count(*) AS n FROM admin_audit_log WHERE action_type IN ('FEEDBACK_REPLY_READ','FEEDBACK_REPLY_DELETED')").get().n,2);
 assert.equal((await user.request('/api/feedback/posts/missing/replies',write({text:'Missing post'}))).status,404);
});

test('Deleted feedback records report not found without false audit success',async()=>{
 const {sqlite,d1}=isolatedD1();const app=appFor(d1);
 for(const [path,options]of [['/messages/missing/read',write({},'PUT')],['/messages/missing',{method:'DELETE'}],['/posts/missing',write({title:'New',message:'Text'},'PUT')],['/posts/missing',{method:'DELETE'}]]) {
  assert.equal((await app.request('/api/feedback'+path,options)).status,404,path);
 }
 assert.equal(sqlite.prepare('SELECT count(*) AS n FROM admin_audit_log').get().n,0);
});
