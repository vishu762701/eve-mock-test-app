# EVE MOCK TEST APP — SOURCE OF TRUTH

> Purpose: stable map of the project (identity, architecture, invariants).
> It contains NO transient status. Never write commit hashes, test counts, or "verified on <date>" here.

## 0. How to use this file
- Read this file before starting any task.
- The latest explicit user-approved request defines current task scope; `ENGINEERING_STANDARD.md` and `AGENTS.md` define consistent workflow safeguards. `GEMINI.md` / `CLAUDE.md` are pointers, not additional policies. Default design choices here apply unless explicitly changed by the current task. Security and data-integrity safeguards must still hold.
- If this file and the actual code disagree, trust the code, and report the mismatch under "Noticed but not touched (outside scope)" in the Final Report.
- Never claim state from memory or from older chats. Inspect real files, run `git status` and `git log -1`, and run the relevant checks.

## Current UI design authority

The owner's **latest** aesthetic direction is source-traced official Telegram Android component typography and icons, not prior iOS/Inter/Poppins lookalikes. The controlled, partially migrated text roles and source references live in `docs/telegram-typography-mission-01.md` and `telegram_typography.xml` with matching Day/Night color resources. Do not label remaining iOS typography or non-Telegram icons as already migrated. Do not automatically replace exam-specific graphics with unrelated Telegram semantics. Never import Telegram GPL artwork/source or distribute a derived APK containing it before resolving applicable licensing/source-publication obligations.
Older iOS/Inter values in `design_type.xml`, `themes.xml` and `FontManager` are still present as transitional, **not** Telegram-approved defaults. Preserve production API compatibility and text content while replacing them incrementally under source evidence and CI.
Retain existing structural constraints until expressly replaced: 26dp solid cards, 12dp screen margins, 10dp card padding, 9dp elevation, no list dividers; glass limited to top/bottom bars and menus. Home has **no floating bottom navigation bar**; History and Saved/Bookmarks are Drawer destinations; preserve the Android system navigation bar.
The device-font preference is retired. Preserve full Hindi/Devanagari fallback, accessibility, Telegram circular reveal and all existing Lottie timing/triggers. Never alter scoring, ranking or business flows for typography.

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
- Database: Cloudflare D1 (`schema.sql`, incremental migrations in `backend/migrations/`).
- Media storage: Supabase Storage (banner images, exam question media).
- Authentication: Firebase Auth ID token verification via `/api/auth/me`.
- Answer integrity: during active tests, answers are hidden for non-admin students (`hideAnswers`, checked via the `X-Eve-Client` header). Score evaluation, negative marking, attempt deduplication (`clientAttemptId`) and rank calculation happen on the server (`POST /api/attempts/submit`). Admin endpoints require an authenticated admin email.

### C. Firebase and security rules (`firestore.rules`, `functions/`)
- `firestore.rules` enforces admin write authorization through `isAdmin()`.
- Admin email sync: the hardcoded admin emails must stay identical in `Constants.ADMIN_EMAILS` (`app/src/main/java/com/eve/app/util/Constants.kt`) and in `isHardcodedAdmin()` (`firestore.rules`). Those two files are the only source of the list. Do not copy the emails into docs.
- Lockfile rule: `functions/package-lock.json` is untracked and ignored. Never stage, commit, overwrite or delete it.

## 3. Permanent invariants (never break these)
1. Colors: use the owner's locked `design_colors.xml` day/night tokens and
   matching system hue tokens, preserving alpha and meaning. Only equal-value `eve_bg` / `eve_text` aliases remain for the protected theme
   manager; other UI colors and dimensions use the root tokens.
2. The Telegram-style circular reveal Day/Night transition must never be degraded.
3. Server-side test timer, question layout fitting, and submit mechanics must never be replaced by client-only logic.
4. Blur is applied only to top bars, bottom bars and menus, never over foreground text, images, icons or Lottie animations. Dialogs and sheets are solid surfaces.
5. Test scoring, ranking, and answer visibility stay server-authoritative.

## 4. Verification (run for the layer you changed; both if the change spans both)
- `Build Eve APK` runs FAST compilation and JVM unit tests on intermediate pushes; it does not automatically run emulators every N builds. FULL runs at completed mission's final commit via `[eve-full]` or manual `workflow_dispatch`; earlier risk-based checkpoints are optional. FULL is required for both emulator modes, Light/Dark evidence, signing safeguards and publishing `Eve-debug-apk`. See `ENGINEERING_STANDARD.md`.
- Android: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
- Backend (inside `backend/`): `npm test` and `npm run build`.
- Always also run `git diff --check`, and check the GitHub Actions result after pushing.
- Report exact commands and results. Do not copy results into this file.

## 5. Keeping this file accurate
- Update this file only when architecture, module boundaries, or invariants change as part of the task.
- Never add transient status (commit hashes, test counts, dates, "currently working on").
