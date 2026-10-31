// Privileged optional Firebase callables share the Worker's D1 authority.
const WORKER_ORIGIN = 'https://eve-backend.anyqueairdrop.workers.dev';

async function verifyWorkerAdmin(request, requiredRight, fetcher = fetch) {
  const authorization = request.rawRequest?.headers?.authorization;
  const uid = request.auth?.uid;
  const email = request.auth?.token?.email;
  if (!uid || typeof email !== 'string' || request.auth.token.email_verified !== true ||
      typeof authorization !== 'string' || !/^Bearer\s+\S+$/i.test(authorization)) return false;
  const headers = { Authorization: authorization };
  try {
    const identityResponse = await fetcher(`${WORKER_ORIGIN}/api/auth/me`, { headers, signal: AbortSignal.timeout(5000) });
    if (!identityResponse.ok) return false;
    const identity = await identityResponse.json();
    if (identity.success !== true || identity.data?.isAdmin !== true ||
        identity.data.uid !== uid || typeof identity.data.email !== 'string' ||
        identity.data.email.toLowerCase() !== email.toLowerCase()) return false;
    const rightsResponse = await fetcher(`${WORKER_ORIGIN}/api/admin/me`, { headers, signal: AbortSignal.timeout(5000) });
    if (!rightsResponse.ok) return false;
    const rights = await rightsResponse.json();
    return rights.success === true && typeof rights.data?.email === 'string' &&
      rights.data.email.toLowerCase() === email.toLowerCase() &&
      Array.isArray(rights.data.rights) && rights.data.rights.includes(requiredRight);
  } catch {
    // Never grant from an offline list, custom claim or independent Firestore role.
    return false;
  }
}
module.exports = { verifyWorkerAdmin };
