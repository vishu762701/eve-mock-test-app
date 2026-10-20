# Eve — Free Mock Test & Exam Preparation App

Eve is an Android exam-practice application with a Kotlin/XML client, Firebase authentication and Firestore, a Cloudflare Worker/D1 backend, and optional Firebase Functions. This repository is the current source; old phase instructions and prior Gemini Knowledge sync documents are not active implementation rules.

## Layout
- `app/`: Android Kotlin app (XML Views, MVVM), production screens, resources and Android tests.
- `backend/`: TypeScript Cloudflare Worker, API routes, database schema/migrations, tests.
- `functions/`: Firebase Functions and supporting tests.
- `firestore.rules`, `storage.rules`: Firebase access controls.
- `.github/workflows/build.yml`: Android, backend, Functions and native UI verification before publishing a debug APK.
- `.github/workflows/deploy-worker.yml`: Worker deployment and D1 migration workflow.
- `docs/design-reset/report.md`: latest UI design-reset implementation evidence and limitations.

## Current engineering instructions
Read `ENGINEERING_STANDARD.md`, `SOURCE_OF_TRUTH.md`, and `AGENTS.md` before editing. `GEMINI.md` and `CLAUDE.md` redirect to the same instructions. The user's latest explicit, safe, scoped requirement takes precedence over older design requests; historical `DECISIONS.md` entries are an audit trail, not work to replay. Approved UI defaults are described in `APPROVED_UI_IMPLEMENTATION_AUDIT.md`.

## Validation
- Android: `./gradlew assembleDebug testDebugUnitTest lintDebug` (or use the configured GitHub Actions workflow).
- Worker: `cd backend && npm install --no-package-lock && npm run build && npm test`.
- Firebase Functions: `npm run build --prefix functions && npm test --prefix functions`.
- Documentation: `node .github/scripts/verify-docs.mjs`.
- Native UI verification runs through GitHub Actions on gesture and three-button navigation, with light/dark evidence gates.

Do not commit credentials or service-account JSON. Configure production secrets in the deployment environment. Do not run obsolete setup directions or weaken Firebase/Worker authorization to get a build passing.

The retired EVE JARVIS Gemini Knowledge / Google Drive code-sync workflow is not needed to build, test, or deploy Eve. Gemini-backed exam-question generation in `backend/` and `functions/` is an **independent runtime feature** and must not be removed by repository-maintenance cleanup.

## Premium payment safety
Payment creation and payment webhooks are disabled unless Cloudflare Worker secret `PREMIUM_WEBHOOK_SECRET` contains an independent high-entropy value of at least 32 characters. The old database/client development secret is never accepted. Supply the key using Cloudflare Worker secrets or the optional GitHub Actions repository secret of the same name; configure the matching value with the actual payment callback sender. Do not commit it to source control. Restart/redeploy the Worker as required.

The sandbox payment simulation endpoint is **off by default**, and requires both a verified admin account and `PREMIUM_SANDBOX_ENABLED=true` in a controlled test environment. Only the admin's own test orders are eligible. Do not enable sandbox simulation in production. Old historical database migration defaults are not security credentials and are never trusted.

If the signing secret is not configured, the public Premium plan marks purchases unavailable to avoid accepting payments that the backend cannot securely verify.
