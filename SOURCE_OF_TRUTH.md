# EVE MOCK TEST APP — SOURCE OF TRUTH

> Purpose: stable map of the project (identity, architecture, invariants).
> It contains NO transient status. Never write commit hashes, test counts, or "verified on <date>" here.

## 0. How to use this file
- Read this file before starting any task.
- The latest explicit owner request is the ONLY change scope: implement authorized outcomes and directly affected frontend/backend/resources/tests together when appropriate, never unrelated work. `ENGINEERING_STANDARD.md` defines workflow: never substitute, simplify or reinterpret the owner's requested behavior; resolve routine technical blockers autonomously and escalate unresolved product, security or legal decisions. Agent files are pointers. Security/data integrity and actual release obligations remain enforced.
- If this file and the actual code disagree, trust the code, and report the mismatch under "Noticed but not touched (outside scope)" in the Final Report.
- Never claim current state from memory or older chats. Inspect real files and current remote HEAD/CI; use `git status` and `git log -1` only when a local checkout is accessible.

## Product authority
EVE is in active development. No UI/UX element is permanently locked; the owner's latest safe scoped requirement can add, replace or remove any of it. When the owner asks for Telegram, `ENGINEERING_STANDARD.md` section 2 applies: official Telegram Android source only, exact port, no alternative implementation without explicit permission. Existing Telegram-derived code is the current implementation, not a standing requirement; change it when asked, or when it deviates from the source for a requested Telegram item. Telegram archive hashes and GPL evidence are in `THIRD_PARTY_NOTICES.md`. Do not impose Telegram source verification on ordinary Eve fixes, backend tasks or cleanup.

## 1. Repository identity
- Remote origin: `git@github.com:vishu762701/eve-mock-test-app.git`
- Branch: `main` only (see `ENGINEERING_STANDARD.md`).
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
- Home and discovery: `MainActivity.kt`, `HomeAppearance.kt` (themed Home canvas, exam discovery/search, category filtering, rotating content banner and drawer navigation; obsolete streak pill and bottom tabs were removed).
- Theme and transitions: `ThemeSwitchAnimator.kt`, `SystemBarHelper.kt`, `ThemeManager.kt` (Telegram-style circular reveal Day/Night animation, status/nav bar icon sync).
- EVE UI Studio was previously retired; that is historical state, NOT a ban on the owner adding or redesigning a studio again. Keep database migrations and production data until safe migration is established.
- Admin Typography Studio editor and draft/publish/write routes were retired, but the owner may safely redesign or restore them. Runtime `TypographyRepository`, fallback fonts and public published config GET may serve installed clients; investigate compatibility before altering. Retain the D1 migration and production records.
- Admin Activity privileged views and data loads must be initialized **after** fresh `AdminAccessGate` authorization. Backend authorization is independently required for mutations.
- Notification inbox merges local reminders/FCM and server broadcasts without discarding either; only the inbox acknowledges unread, never Home.
- Profile: `ProfileActivity`, `ProfilePullScrollView` and `ProfileHeaderExpansion` host the EVE-adapted Telegram profile. User-visible identity uses the editable server username; immutable Firebase/D1 IDs and legacy API fields remain internal compatibility contracts. The registered `eve-mock-test://username/<username>` route opens a username-only card, never authentication or a private profile lookup. Profile/presence requests carry local `ProfileRequestOwner` metadata to bind queued work to its initiating account. `ConfirmedProfileStore` retains only the active identity’s confirmed data and position in process memory, coalesces revalidation/presence reads and clears on account changes. The profile overflow uses the source-derived normal ActionBar popup renderer with EVE destinations. Category and Daily Practice Reminder share one source-derived section surface.
- Maintenance enforcement is scoped to Home startup/resume/retry, with fresh Worker authorization for admin bypass; active exams and other activities are not server-wide blocked. Admin maintenance writes are partial, and success requires server readback. Unavailable configuration is not a confirmed maintenance-off state.

### B. Cloudflare Worker backend (`backend/`)
- Technology: TypeScript, Hono, Cloudflare Workers.
- API base URL: `https://eve-backend.anyqueairdrop.workers.dev/`
- Database: Cloudflare D1 (incremental migrations in `backend/migrations/`).
- Media storage: Supabase Storage (banner images, exam question media).
- Authentication: Firebase Auth ID token verification via `/api/auth/me`.
- Answer integrity: `hideAnswers` uses the server-verified admin principal, never a client version header. Question-bank reads additionally require fresh `manage_questions` permission to disclose answer keys. Score evaluation, negative marking, attempt deduplication (`clientAttemptId`) and rank calculation happen on the server (`POST /api/attempts/submit`). Administrative endpoints independently enforce authenticated server permissions.
- Exam metadata saves own name, time, category and cutoff settings, verify authenticated API readback, and omit unedited hidden settings. Cutoffs use `exams.cutoffs_json` through the existing exam GET/PUT routes. They feed the existing result presentation without changing server score evaluation. Migration/deployment remains separately controlled.

### C. Firebase and security rules (`firestore.rules`, `functions/`)
- `firestore.rules` enforces admin write authorization through `isAdmin()`.
- Admin authority comes from Worker-verified Firebase identity and D1 `admins` membership. Android has no email allowlist or offline grant. Existing owner protection is server-side and still requires D1 membership; migration 0027 stages existing owner membership. Optional Firebase privileged callables verify identity/rights through the Worker. Legacy Firestore rules remain a separate compatibility surface, never an Android admin authority. Do not copy admin emails into docs.
- Lockfile rule: `functions/package-lock.json` is untracked and ignored. Never stage, commit, overwrite or delete it.

## 3. Safety and functional invariants (not UI freezes)
1. Scoring, rank, exam submission, authorization and answers remain server-authoritative. Changes must preserve these protections and safe data migration behavior.
2. Preserve unrelated screens and effects by default, but the owner can explicitly change/remove the current Telegram theme reveal, Lottie motion, navigation layout and any other UI/UX feature.
3. Validate Day/Night, contrast, readable font scaling, Hindi fallback, touch targets and supported Android API behavior for the chosen new design.
4. `design_colors.xml` and other existing tokens describe the current theme only. Old hardcoded colors, dimensions and "locked" tests must be updated together when design changes.
5. Automatically remove only files proven obsolete because of the current requested change; repository-wide cleanup requires a separate explicit owner request. Follow evidence rules in `ENGINEERING_STANDARD.md`.

## 4. Verification (run for the layer you changed; both if the change spans both)
- `Build Eve APK` runs compile-only FAST on normal main pushes (`:app:assembleDebug`), without automatic JVM, backend, Firebase, Rust, lint or emulator verification. Each pushed implementation batch needs exact-SHA FAST; use coherent batches with no numeric commit ceiling and avoid report-only commits. Only owner-requested manual `workflow_dispatch` enables FULL validation, both emulator navigation modes, signing checks and gated `Eve-debug-apk` publication. A successful client build never proves live Worker/D1 migration or production contract parity.
- Android: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
- Backend (inside `backend/`): `npm test` and `npm run build`.
- Run `git diff --check` when working in a checkout, targeted backend/API tests on server changes, and exact-SHA GitHub Actions after each pushed batch. Production Worker/D1 deployment stays explicitly manual-only.
- Report exact commands and results. Do not copy results into this file.

## 5. Keeping this file accurate
- Update this file only when architecture, module boundaries, or invariants change as part of the task.
- Never add transient status (commit hashes, test counts, dates, "currently working on").
