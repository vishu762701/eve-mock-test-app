# EVE MOCK TEST APP — SOURCE OF TRUTH

> Purpose: stable map of the project (identity, architecture, invariants).
> It contains NO transient status. Never write commit hashes, test counts, or "verified on <date>" here.

## 0. How to use this file
- Read this file before starting any task.
- Working rules (workflow, scope, reporting) live in `AGENTS.md` / `GEMINI.md`. If anything here conflicts with them or with the current user prompt, the current user prompt wins, then AGENTS.md / GEMINI.md, then this file.
- If this file and the actual code disagree, trust the code, and report the mismatch under "Noticed but not touched (outside scope)" in the Final Report.
- Never claim state from memory or from older chats. Inspect real files, run `git status` and `git log -1`, and run the relevant checks.

## 1. Repository identity
- Remote origin: `git@github.com:vishu762701/eve-mock-test-app.git`
- Branch: `main` only (see AGENTS.md "Repository workflow").
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
- Test engine: `TestActivity.kt`, `CircularTimerView.kt`, `QuestionFitHelper.kt` (rigid no-scroll test viewport, 2x2 action button grid, question palette, Hindi/English font sizing, server-evaluated submissions).
- Result and analytics: `ResultActivity.kt`, `ResultDetailActivity.kt` (single-screen summary cards, Statistics, Performance, Analytics, Review tab with isolated scroll, Leaderboard).
- Home and discovery: `MainActivity.kt`, `HomePanelWashDrawable.kt` (light pastel wash grid, dark surface parity, single rotating banner surface, cached streak pill with looping flame).
- Theme and transitions: `ThemeSwitchAnimator.kt`, `SystemBarHelper.kt`, `ThemeManager.kt` (Telegram-style circular reveal Day/Night animation, status/nav bar icon sync).
- EVE UI Studio (`UiStudioActivity.kt`, `UiStudioRegistry.kt`, `UiStudioEngine.kt`): RETIRED. Do not extend or redesign it. Retain historical migrations and production data.

### B. Cloudflare Worker backend (`backend/`)
- Technology: TypeScript, Hono, Cloudflare Workers.
- API base URL: `https://eve-backend.anyqueairdrop.workers.dev/`
- Database: Cloudflare D1 (`schema.sql`, incremental migrations in `backend/migrations/`).
- Media storage: Supabase Storage (banner images, exam question media).
- Authentication: Firebase Auth ID token verification via `/api/auth/me`.
- Answer integrity: during active tests, answers are hidden for non-admin students (`hideAnswers`, checked via the `X-Eve-Client` header). Score evaluation, negative marking, attempt deduplication (`clientAttemptId`) and rank calculation happen on the server (`POST /api/attempts/submit`). Admin endpoints require an authenticated admin email.

### C. Firebase and security rules (`firestore.rules`, `functions/`)
- `firestore.rules` enforces admin write authorization through `isAdmin()`.
- Admin email sync: the hardcoded admin emails must stay identical in `Constants.ADMIN_EMAILS` (`app/src/main/java/com/eve/app/util/Constants.kt`) and in `isHardcodedAdmin()` (`firestore.rules`). Those two files are the only source of the list. Do not copy the emails into docs.
- Lockfile rule: `functions/package-lock.json` is untracked and ignored. Never stage, commit, overwrite or delete it.

## 3. Permanent invariants (never break these)
1. Color system:
   - Light backgrounds: pure `#FFFFFF`. Dark backgrounds: pure `#101117` / `#000000`.
   - Semantic tokens (never decorative):
     - Right / Answered: light fill `#C9F0B8`, text `#1F6B3A`; dark fill `#293B27`, text `#7FE3A5`.
     - Wrong: light fill `#FFC1BB`, text `#9B2C26`; dark fill `#602625`, text `#FF9A92`.
     - Medium / Accuracy / Marked: light fill `#FEE3AA`, text `#7A4F00`; dark fill `#634416`, text `#F2C26B`.
   - Lime `#E3FF3B` is banned everywhere in `app/` and `backend/`.
2. The Telegram-style circular reveal Day/Night transition must never be degraded.
3. Server-side test timer, question layout fitting, and submit mechanics must never be replaced by client-only logic.
4. Blur is applied only to chrome/surfaces through a matte `BlurView`, never over text, images, or Lottie animations.
5. Test scoring, ranking, and answer visibility stay server-authoritative.

## 4. Verification (run for the layer you changed; both if the change spans both)
- Android: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
- Backend (inside `backend/`): `npm test` and `npm run build`.
- Always also run `git diff --check`, and check the GitHub Actions result after pushing.
- Report exact commands and results. Do not copy results into this file.

## 5. Keeping this file accurate
- Update this file only when architecture, module boundaries, or invariants change as part of the task.
- Never add transient status (commit hashes, test counts, dates, "currently working on").
