# EVE MOCK TEST APP — PERMANENT REPOSITORY SOURCE OF TRUTH

> **Status:** Active & Authoritative  
> **Last Verified:** 2026-10-09  
> **Canonical Path:** `/data/data/com.termux/files/home/eve-mock-test-app`  
> **Remote Origin:** `git@github.com:vishu762701/eve-mock-test-app.git`  
> **Tracking Branch:** `main`  
> **Verified Head Commit:** `55e6a05a1097fa668c2f55d0458fa2fe31e0b0db`  

---

## 1. Canonical Repository Identity

This directory (`/data/data/com.termux/files/home/eve-mock-test-app`) is the **single authoritative repository** for the EVE Mock Test Application. All active development, bug fixes, UI redesigns, builds, and commits must take place exclusively inside this tree.

### Workspace Disambiguation (Permanent Warning)
In the Termux home environment (`/data/data/com.termux/files/home`), several other directories exist. They must **never** be confused with or used in place of this repository:

| Directory | Type / Purpose | Rule for Agents & Developers |
| :--- | :--- | :--- |
| `~/eve-mock-test-app` | **Canonical Repository** (`origin/main`) | **SOLE SOURCE OF TRUTH**. Perform all work here. |
| `~/Eve` | Stale clone (commit `131ca40`, Phase 9) | **DO NOT USE**. Outdated by dozens of releases. |
| `~/EvePlayer` | Different Project (`EvePlayer` media player app) | **DO NOT TOUCH**. Completely different product. |
| `~/EveBuild` | EvePlayer build scratch directory | **DO NOT TOUCH**. |
| `~/EvePlayer_backup_before_v4` | EvePlayer backup directory | **DO NOT TOUCH**. |
| `~/eve_new` | Untracked temporary directory | **DO NOT USE**. |
| `~/evefix` | Untracked temporary directory | **DO NOT USE**. |

---

## 2. Architecture & Module Boundaries

The system is organized into three distinct, coupled layers:

### A. Native Android Client (`:app` / `app/`)
* **Technology:** Kotlin, AndroidX, Material 3, ViewBinding, MVVM.
* **Core Domains:**
  * **Test Engine:** `TestActivity.kt`, `CircularTimerView.kt`, `QuestionFitHelper.kt` (rigid no-scroll test viewport, 2x2 action button grid, question palette, multi-lingual Hindi/English font sizing, server-evaluated submissions).
  * **Result & Analytics:** `ResultActivity.kt`, `ResultDetailActivity.kt` (single-screen dashboard summary cards, Statistics, Performance, Analytics, Review tab with isolated scroll, Leaderboard).
  * **Home & Discovery:** `MainActivity.kt`, `HomePanelWashDrawable.kt` (light pastel wash grid, dark surface parity, single rotating banner surface, cached streak pill with subtle looping flame).
  * **EVE UI Studio (Visual App Builder):** `UiStudioActivity.kt`, `UiStudioRegistry.kt`, `UiStudioEngine.kt` (50/50 split canvas, authentic phone chrome, 8 screen renderers, 13 typed runtime adapters, undo/redo, draft persistence, versioned atomic publishing).
  * **Theme & Transitions:** `ThemeSwitchAnimator.kt`, `SystemBarHelper.kt`, `ThemeManager.kt` (Telegram-style circular reveal Day/Night animation, immersive status/nav bar icon synchronization).
* **Color System Invariants:**
  * **Light Backgrounds:** Pure `#FFFFFF`.
  * **Dark Backgrounds:** Pure `#101117` / `#000000`.
  * **Semantic Tokens (Never Decorative):**
    * Right / Answered: Light fill `#C9F0B8` / text `#1F6B3A`; Dark fill `#293B27` / text `#7FE3A5`.
    * Wrong: Light fill `#FFC1BB` / text `#9B2C26`; Dark fill `#602625` / text `#FF9A92`.
    * Medium / Accuracy / Marked: Light fill `#FEE3AA` / text `#7A4F00`; Dark fill `#634416` / text `#F2C26B`.
  * **Strict Ban:** Lime color `#E3FF3B` is strictly prohibited everywhere across `app/` and `backend/`.

### B. Cloudflare Worker Backend (`backend/`)
* **Technology:** TypeScript, Hono framework, running on Cloudflare Workers.
* **API Base URL:** `https://eve-backend.anyqueairdrop.workers.dev/`
* **Relational Database:** Cloudflare D1 SQLite database (`schema.sql`, incremental migrations in `backend/migrations/`).
* **Media & Object Storage:** Supabase Storage (banner image assets, exam question media).
* **Authentication:** Firebase Auth ID Token verification via `/api/auth/me`.
* **Security & Answer Integrity:**
  * During active tests, answers are hidden for non-admin students (`hideAnswers`, checked via `X-Eve-Client` header).
  * Test score evaluation, negative marking, attempt deduplication (`clientAttemptId`), and rank calculations are performed authoritatively on the server (`POST /api/attempts/submit`).
  * Admin endpoints require authenticated admin email authorization.

### C. Firebase & Security Rules (`firestore.rules`, `functions/`)
* **Firestore Rules:** `firestore.rules` enforces admin write authorization via `isAdmin()`.
* **Admin Email Synchronization:** The 4 hardcoded admin emails must remain strictly synchronized between `Constants.ADMIN_EMAILS` in `app/src/main/java/com/eve/app/util/Constants.kt` and `isHardcodedAdmin()` in `firestore.rules`:
  1. `pronlike9@gmail.com`
  2. `own.keni@gmail.com`
  3. `anyqueairdrop@gmail.com`
  4. `ghatisarkar56@gmail.com`
* **Lockfile Rule:** `functions/package-lock.json` is untracked and ignored. Never stage, commit, or overwrite it.

---

## 3. Permanent Operational Guardrails

Every agent, developer, and session operating on this repository must abide by the following rules:

1. **GitHub is the Single Source of Truth:**
   Always pull and push to `origin/main` (`git@github.com:vishu762701/eve-mock-test-app.git`). Never maintain conflicting detached local branches.
2. **Verify State First:**
   Never assume a task or fix discussed in a previous chat session succeeded. Always inspect real files, run `git status`, check `git log -1`, and execute the relevant test suites before declaring state.
3. **Dual Verification Requirement:**
   Before marking any major task complete:
   * **Android Client:** `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` must compile and pass cleanly.
   * **Backend:** `npm test` and `npm run build` inside `backend/` must pass cleanly (all 80+ tests).
4. **Strict Scope Discipline:**
   Only modify files explicitly required for the active prompt or task. Never re-touch, refactor, or "clean up" completed, verified modules from past tasks.
5. **Preserve Core Invariants:**
   * Telegram-style circular reveal Day/Night transition must never be degraded.
   * Server-side test timer countdown, question layout fitting, and submit mechanics must never be replaced by client-only logic.
   * Blur must only be applied to chrome/surfaces via matte `BlurView`, never over text, images, or Lottie animations.

---

## 4. Current Verification Record (Verified 2026-10-09)

* **Git Tree:** Clean, synced with `origin/main` at `55e6a05`.
* **Android Unit Tests:** **PASSED** (226 tests, 0 failures via `./gradlew testDebugUnitTest`).
* **Android Debug Build:** **PASSED** (Clean APK output via `./gradlew assembleDebug`).
* **Backend Unit Tests:** **PASSED** (80/80 tests passing via `npm test` in `backend/`).
* **Backend Build:** **PASSED** (Clean TypeScript compilation).
