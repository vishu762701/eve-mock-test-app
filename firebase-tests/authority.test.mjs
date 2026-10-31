import test, { before, after } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc } from 'firebase/firestore';
import { ref, uploadBytes, deleteObject, getBytes } from 'firebase/storage';
import { readFileSync } from 'node:fs';
let env;
const root = new URL('../', import.meta.url);
const ownerEmail = readFileSync(new URL('backend/src/auth.ts', root), 'utf8').match(/HARDCODED_ADMIN_EMAILS = new Set\(\[\s*"([^"]+)"/)[1];
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-eve-authority',
    firestore: { rules: readFileSync(new URL('firestore.rules', root), 'utf8') },
    storage: { rules: readFileSync(new URL('storage.rules', root), 'utf8') } });
  await env.withSecurityRulesDisabled(async ctx => {
    await setDoc(doc(ctx.firestore(), 'admins', ownerEmail), { role: 'Owner' });
    await setDoc(doc(ctx.firestore(), 'exams', 'existing'), { title: 'Existing exam' });
    await uploadBytes(ref(ctx.storage(), 'syllabi/existing.pdf'), new TextEncoder().encode('%PDF-1.4'), { contentType: 'application/pdf' });
  });
});
after(async () => { await env?.cleanup(); });
test('verified legacy owner and custom admin claims cannot bypass Worker authorization', async () => {
  for (const claims of [{ email: ownerEmail, email_verified: true }, { email: ownerEmail, email_verified: true, admin: true }]) {
    const db = env.authenticatedContext('legacy-owner', claims).firestore();
    for (const collection of ['exams', 'questions', 'admins', 'admin_audit_log', 'usage_stats', 'generated_tests']) {
      await assertFails(setDoc(doc(db, collection, 'client-write'), { title: 'Unauthorized' }));
    }
  }
});
test('student exam read and isolated own profile remain available', async () => {
  const db = env.authenticatedContext('student', { email: 'student@example.test' }).firestore();
  await assertSucceeds(getDoc(doc(db, 'exams', 'existing')));
  await assertSucceeds(setDoc(doc(db, 'users', 'student'), { displayName: 'Student' }));
  await assertSucceeds(getDoc(doc(db, 'users', 'student')));
  await assertFails(getDoc(doc(db, 'users', 'someone-else')));
  await assertFails(setDoc(doc(db, 'users', 'someone-else'), { displayName: 'Forged' }));
  await assertFails(getDoc(doc(db, 'questions', 'private-key')));
});
test('legacy storage is readable to students but has no client-side owner write bypass', async () => {
  const owner = env.authenticatedContext('legacy-owner', { email: ownerEmail, email_verified: true }).storage();
  await assertFails(uploadBytes(ref(owner, 'syllabi/client.pdf'), new Uint8Array([1]), { contentType: 'application/pdf' }));
  await assertFails(deleteObject(ref(owner, 'syllabi/existing.pdf')));
  const student = env.authenticatedContext('student').storage();
  await assertSucceeds(getBytes(ref(student, 'syllabi/existing.pdf')));
  await assertFails(getBytes(ref(env.unauthenticatedContext().storage(), 'syllabi/existing.pdf')));
});
