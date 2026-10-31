# EVE Worker deployment gate — 2026-10-31

**State:** Safe staging. No production D1 migration or Worker deployment is authorized by this document.

## Why this must precede Bio/presence

Former `.github/workflows/deploy-worker.yml` automatically executed `wrangler d1 migrations apply eve-database --remote` and `cloudflare/wrangler-action@v3` on any non-Markdown change under `backend/**` pushed to `main`. An unfinished personal Bio or presence migration would therefore be applied to LIVE production before all Android callers, API compatibility, and security tests were accepted.

## Changed behavior

- Only GitHub Actions `workflow_dispatch` can initiate the Worker deployment workflow.
- A required Boolean `confirm_production` is **false** by default. The only deploy job has `if: ${{ inputs.confirm_production == true }}`.
- The job still performs backend TypeScript check, backend tests, D1 migration, Worker deployment and live smoke tests **in the same prior order** when explicitly approved. No auth, secrets, D1 schema or cloud runtime is modified by this workflow-only commit.
- The Android build workflow remains automatic on `main` and still checks backend and Firebase Functions FAST; this does NOT bypass or disable those checks.
- `.github/scripts/tests/test_production_deploy_gate.py` is run by existing Android FAST's `python3 -m unittest discover -s .github/scripts -v` runner path; it asserts manual-only, default-false and ordered remote migration/deploy.

**Go-live checklist:** Owner expressly authorizes production rollout; migration and client GET/PUT contracts have passed exact-SHA FAST and native FULL; data migration is backwards compatible; production secrets and D1 backup/rollback are assessed; manually dispatch with `confirm_production=true`; inspect migration and live smoke results. Never claim this staging change deployed Bio or enabled online presence.

This is a release safety gate directly required by the ongoing Profile Bio/presence mission. All prior production Worker data remains untouched.

## Approved rollout preflight

An owner-approved rollout first dispatches with `confirm_production=true` and
`preflight_only=true` (the default). This runs typecheck/tests and SELECT-only
remote migration/schema inspection, then obtains a current D1 Time Travel
recovery bookmark. It performs no migration, secret update, deployment or smoke
write. Only schema metadata and migration names appear in logs, never account
records or a database dump. Workflow concurrency serializes production operations.

Review the reported pending suffix against the actual migration SQL. Unknown or
gapped history, a runtime-added `bio` column without its migration record, absent
legacy identity fields, incompatible schema or a missing recovery bookmark fails
closed. Do not blindly rerun an ALTER or mark a migration applied to suppress drift.

After successful preflight review and required exact-SHA CI, dispatch that same
approved main revision with `preflight_only=false`. It repeats preflight immediately
before migration, validates the complete migration history/schema afterward, then
continues the existing secret/deploy/smoke steps. No data/compatibility columns are
removed. Additive migrations stay in place if Worker code must be rolled back;
assess schema compatibility before rollback. The recorded Time Travel bookmark is
a retention-limited recovery point, not a permanent exported backup. Database
restore can discard later user writes and requires a separate recovery assessment;
this workflow never automatically restores a live database.

Green workflow status alone is insufficient: inspect executed versus skipped smoke
checks, public config readback and authenticated disposable-account behavior.
