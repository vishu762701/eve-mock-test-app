# Eve — Free Mock Test & Exam Preparation App

Eve is an Android exam-practice application with a Kotlin/XML client, Firebase authentication and Firestore, a Cloudflare Worker/D1 backend, and optional Firebase Functions. This repository is the current source; old phase instructions and prior Gemini Knowledge sync documents are not active implementation rules.

## Layout
- `app/`: Android Kotlin app (XML Views, MVVM), production screens, resources and Android tests.
- `backend/`: TypeScript Cloudflare Worker, API routes, database schema/migrations, tests.
- `functions/`: Firebase Functions and supporting tests.
- `firestore.rules`, `storage.rules`: Firebase access controls.
- `.github/workflows/build.yml`: Android, backend, Functions and native UI verification before publishing a debug APK.
- `.github/workflows/deploy-worker.yml`: Worker deployment and D1 migration workflow.

## Engineering instructions for agents
Read `ENGINEERING_STANDARD.md` (the single workflow policy) and `SOURCE_OF_TRUTH.md` (architecture and security) before editing. `AGENTS.md` is the shared entrypoint; `GEMINI.md` and `CLAUDE.md` are pointers to it. The owner's latest explicit request defines scope: change only the requested items and their minimum direct dependencies and tests, with no unrelated redesign, refactor or cleanup. EVE is under active development, so no UI/UX element is permanently locked.

When the owner asks for Telegram, the only source is the official `DrKLO/Telegram` repository pinned to a full commit SHA (see `ENGINEERING_STANDARD.md` section 2); no alternative implementation is permitted without explicit owner approval. Agents delete only files made provably obsolete by the authorized change. Preserve uncertain assets, migrations, licenses and production data. Most files in `docs/` are historical evidence, not instructions.

## Downloading an installable Android APK
- **FAST** (every push to `main`): compiles the debug APK only (ARM64). It does not run tests and does not publish an APK. A green FAST status means buildable, not tested, and is not an APK download link.
- **FULL** (manual `workflow_dispatch` run by the owner): runs lint, unit tests, signing safeguards, both emulator navigation modes (gesture and three-button), backend and Functions checks. Only after all gates pass, and only if the `publish_verified_apk` input is true, does GitHub Actions upload the **Eve-debug-apk** artifact.
- On GitHub open **Actions → Build Eve APK → latest successful FULL run → Artifacts → Eve-debug-apk**. Download the ZIP and extract the debug APK to install it. A source commit alone is not an APK release, and a debug APK is not a production release.

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

## Open-source license and original Telegram resources

The EVE-authored application source is distributed under **GPL-3.0-or-later**, subject to separate third-party notices; see `LICENSE` and `THIRD_PARTY_NOTICES.md`. Telegram-derived theme behavior and source-exact Telegram action/search WebP artwork retain their original authorship. The reference archive contains GPL version 2; per-file "or later" grants and GPLv3 compatibility remain under review. EVE is not affiliated with Telegram; no Telegram logo or branding is used as the EVE app mark. License and corresponding-source obligations apply to any distributed APK, and bundled Google/third-party SDK compatibility must be reviewed before production release.
