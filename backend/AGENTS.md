# EVE backend — fast batch implementation rules

Read `../ENGINEERING_STANDARD.md`, `../AGENTS.md` and `../SOURCE_OF_TRUTH.md`. This file adds server-specific guidance, not a separate or conflicting policy.

- Implement the authorized client/server feature as one coordinated batch when appropriate. Multiple coherent commits can be staged then pushed together; a single commit can change Worker routes, D1 migrations, Android DTOs, UI and tests.
- Keep Firebase token-derived identity, server-authoritative scoring and answer secrecy, admin permission boundaries, idempotency and cross-account isolation. Never weaken `firestore.rules` or privileged endpoints for faster delivery.
- Test the real Worker route/handler, request validation, negative authorization, malformed inputs, migration/old-client compatibility, concurrent updates, and authenticated write/readback. Run `npm test` and `npm run build` whenever the Worker changes; coordinate Android callers in the same batch.
- Additive D1 migrations must preserve old rows and have a safe rollout/rollback plan. If a UI requires new fields, use capability detection or backward-compatible responses until backend deployment is authorized.
- Backend pushes to `main` are **staged only**. `../.github/workflows/deploy-worker.yml` requires a separate explicit, manual `confirm_production` dispatch to apply remote migrations and deploy. Do not auto-dispatch; do not interpret a green FAST/FULL as production deployment.
- Record observed latency from real request/query timings before optimizing. Do not declare a server-performance cause from UI appearance alone.
