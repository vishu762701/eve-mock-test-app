# EVE MOCK TEST APP — SOURCE OF TRUTH

> Purpose: stable map of the project (identity, architecture, invariants).
> It contains NO transient status. Never write commit hashes, test counts, or "verified on <date>" here.

## 0. How to use this file
- Read this file before starting any task.
- The latest explicit user-approved request defines current task scope; `ENGINEERING_STANDARD.md` and `AGENTS.md` define consistent workflow safeguards. `GEMINI.md` / `CLAUDE.md` are pointers, not additional policies. Default design choices here apply unless explicitly changed by the current task. Security and data-integrity safeguards must still hold.
- If this file and the actual code disagree, trust the code, and report the mismatch under "Noticed but not touched (outside scope)" in the Final Report.
- Never claim state from memory or from older chats. Inspect real files, run `git status` and `git log -1`, and run the relevant checks.

## Active-development product authority
EVE is actively being developed: **No visual component, layout, color, blur, theme, typography, navigation, animation, icon, size or feature is permanently locked.** The owner's latest safe scoped requirement can add, replace or remove any UI/UX feature. Current code and historical Telegram/iOS audits are reference snapshots only. Update resources and regression tests alongside approved changes. Keep equivalent accessibility and data/security protections.

## Telegram reference only for source-exact tasks
Telegram reference archives, approved SHA-256 values, internal manifests and GPL evidence are in `THIRD_PARTY_NOTICES.md`. Require and validate an actual source archive only when a task explicitly requires source-exact copying/comparison; absent source files are NOT FOUND. Never impose source ZIP verification on ordinary Eve fixes, original UI/UX work, backend tasks or repository cleanup.

## Current implementation, not frozen defaults
Source-traced Telegram UI, theme reveal, Lottie animations, existing tokens, and the `EveBottomPillBar` are current implementation choices, **not standing requirements**. Their appearance or presence can be changed at the owner's direction; do not reinstate older styles from `docs/telegram-typography-mission-01.md`, source registries, old tests, or historical audits. Retain unaffected behaviors by default and maintain Hindi/Devanagari and assistive accessibility with replacements.

## 1. Repository identity
- Remote origin: `git@github.com:vishu762701/eve-mock-test-app.git`
- Branch: `main` only (see `ENGINEERING_STANDARD.md` repository workflow).
- GitHub Actions workflow: "Build Eve APK".
- Local Termux path (only when working inside Termux): `/data/data/com.termux/files/home/eve-mock-test-app`. This is the single authoritative local copy.

### Termux workspace warning (only applies when working inside Termux home)
| Directory | What it is | Rule |
| :--- | :--- | :--- |
| `~/eve-mock-test-app` | Canonical repository | Do all work here |
| `~/Eve` | Stale clone (old phase) | Do not use |
| `~/EvePlayer`, `~/EveBuild`, `~/EvePlayer_backup_before_v4` | A different product (EvePlayer media player) | Do not touch |
| `~/eve_new`, `~/evefix` | Untracked temp directories | Do not use |

## 2. Architecture and module boundaries

### A. Native Android client (`app/`)
- Technology: Kotlin, AndroidX, Material 3, ViewBinding, MVVM.
- Test engine: `TestActivity.kt`, `CircularTimerView.kt`, `QuestionAdapter.kt` (scrollable question/options content, 2x2 action button grid, question palette, server-evaluated submissions).
- Result and analytics: `ResultActivity.kt`, `ResultDetailActivity.kt` (single-screen summary cards, Statistics, Performance, Analytics, Review tab with isolated scroll, Leaderboard).
- Home and discovery: `MainActivity.kt`, `HomeAppearance.kt` (plain themed canvas, single rotating content banner surface, cached streak pill with shared outline flame icon).
- Theme and transitions: `ThemeSwitchAnimator.kt`, `SystemBarHelper.kt`, `ThemeManager.kt` (Telegram-style circular reveal Day/Night animation, status/nav bar icon sync).
- EVE UI Studio (`UiStudioActivity.kt`, `UiStudioRegistry.kt`, `UiStudioEngine.kt`): RETIRED. Do not extend or redesign it. Retain historical migrations and production data.
- Admin Typography Studio editor and its admin draft/publish/write routes: RETIRED. Runtime `TypographyRepository`, Inter/fallback fonts and the read-only public published configuration GET remain for installed client compatibility. Retain the existing D1 typography migration and production records.
- Admin Activity privileged views and data loads must be initialized **after** fresh `AdminAccessGate` authorization. Backend authorization is independently required for mutations.
- Notification inbox merges local reminders/FCM and server broadcasts without discarding either; only the inbox acknowledges unread, never Home.

### B. Cloudflare Worker backend (`backend/`)
- Technology: TypeScript, Hono, Cloudflare Workers.
- API base URL: `https://eve-backend.anyqueairdrop.workers.dev/`
- Database: Cloudflare D1 (incremental migrations in `backend/migrations/`).
- Media storage: Supabase Storage (banner images, exam question media).
- Authentication: Firebase Auth ID token verification via `/api/auth/me`.
- Answer integrity: during active tests, answers are hidden for non-admin students (`hideAnswers`, checked via the `X-Eve-Client` header). Score evaluation, negative marking, attempt deduplication (`clientAttemptId`) and rank calculation happen on the server (`POST /api/attempts/submit`). Admin endpoints require an authenticated admin email.

### C. Firebase and security rules (`firestore.rules`, `functions/`)
- `firestore.rules` enforces admin write authorization through `isAdmin()`.
- Admin email sync: the hardcoded admin emails must stay identical in `Constants.ADMIN_EMAILS` (`app/src/main/java/com/eve/app/util/Constants.kt`) and in `isHardcodedAdmin()` (`firestore.rules`). Those two files are the only source of the list. Do not copy the emails into docs.
- Lockfile rule: `functions/package-lock.json` is untracked and ignored. Never stage, commit, overwrite or delete it.

## 3. Safety and functional invariants (not UI freezes)
1. Scoring, rank, exam submission, authorization and answers remain server-authoritative. Changes must preserve these protections and safe data migration behavior.
2. Preserve unrelated screens and effects by default, but the owner can explicitly change/remove the current Telegram theme reveal, Lottie motion, navigation layout and any other UI/UX feature.
3. Validate Day/Night, contrast, readable font scaling, Hindi fallback, touch targets and supported Android API behavior for the chosen new design.
4. `design_colors.xml` and other existing tokens describe the current theme only. Old hardcoded colors, dimensions and "locked" tests must be updated together when design changes.
5. Automatically remove conclusively unused files during active tasks under the proof-based cleanup contract of `ENGINEERING_STANDARD.md`.

## 4. Verification (run for the layer you changed; both if the change spans both)
- `Build Eve APK` runs FAST compilation, JVM tests and Android instrumentation **source compilation** for ALL intermediate mission pushes, without starting an emulator. Native UI/FULL runs ONLY on the final implementation commit of an actually completed user-defined mission (`[eve-full]`), or via explicit owner-requested manual `workflow_dispatch`. No automatic daily, push-count, high-risk or mid-mission FULL checkpoints. After failed FULL, use FAST to repair and rerun FULL on the repaired final commit. Both emulator modes, Light/Dark evidence, signing safeguards and gated `Eve-debug-apk` publication remain mandatory for mission completion. See `ENGINEERING_STANDARD.md`.
- Android: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
- Backend (inside `backend/`): `npm test` and `npm run build`.
- Always also run `git diff --check`, and check the GitHub Actions result after pushing.
- Report exact commands and results. Do not copy results into this file.

## 5. Keeping this file accurate
- Update this file only when architecture, module boundaries, or invariants change as part of the task.
- Never add transient status (commit hashes, test counts, dates, "currently working on").
