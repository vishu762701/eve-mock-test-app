# Authenticated personal Bio — staged server contract (2026-10-31)

Scope: the personal **Bio** text field on the EVE Profile page. This does not re-add the unrelated removed `About` navigation screen.

## Persisted contract

- `backend/migrations/0025_user_bio.sql` adds `users.bio TEXT NOT NULL DEFAULT ''` to existing D1 user records; additive and backwards compatible.
- `backend/src/types.ts:UserRow` adds nullable `bio` mapping.
- `GET /api/users/profile` (authenticated) returns `data.bio`; absent profiles return `bio: ''`; no unauthenticated or other-user Bio read endpoint is added.
- `PUT /api/users/profile` includes only the authenticated caller's `bio` in the existing allowlist (string up to 160 UTF-16 code units, trimmed) and binds it in the atomic `INSERT … ON CONFLICT DO UPDATE` field-by-field SQL. Setting `bio:''` clears it; unrelated updates never replace an existing Bio.
- Existing `/sync`, FCM-token and other profile mutations preserve Bio; all user identity is server-token-derived.
- `backend/test/username_and_phone.test.mjs` exercises migration, Hindi/multiline value, limit/malformed validation, cross-account isolation, independent edits and clearing.

## Safety and rollout

`.github/workflows/deploy-worker.yml` is now **manual-only** with required explicit confirmation via commit `896f3d995ba9235f6ae71dfa58ca708ea43866a3`. Backend changes pushed to `main` are staged and checked by normal FAST but do **not** automatically run production remote D1 migrations or deploy Cloudflare Worker. The Android Bio UI must feature-gate on presence of `data.bio` so pre-rollout production returns no misleading 'saved' result.

**NOT DEPLOYED / NO PRODUCTION ACCEPTANCE** until owner explicitly authorizes manual Worker and D1 release, current server migration is proven, and device authenticated GET/PUT readback is tested. Final six-job FULL and application rollout remain separate gates.
