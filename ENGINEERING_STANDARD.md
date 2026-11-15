# EVE Engineering Standard

The single workflow policy and repository constitution. Authority order: (1) the owner's latest explicit message, (2) this file, (3) SOURCE_OF_TRUTH.md (architecture + security), (4) existing code. Old prompts, reports and checkpoints are history, never requirements.

## 1. The 9 Permanent Core Laws

### LAW 1 — MASTERPIECE GENIUS HUMAN CRAFTSMANSHIP (100% EFFORT & HIGHEST THINKING ALWAYS)
Every AI model or agent working on EVE must operate at 100% maximum cognitive depth, architectural foresight, and attention to detail—NOT like a shortcut-taking AI, but like a world-class Principal Engineer and Masterpiece Product Designer. Think through every micro-interaction, frame timing, touch state, pixel alignment, memory lifecycle, dark/light transition, and edge case to 100% perfection. Never ship lazy approximations, placeholders, or "good enough" wrappers. Own the complete authorized outcome. Done means it works end-to-end in the running app: UI, state, backend, resources, Day/Night, RTL, Hindi/Devanagari, back/gesture navigation, small screens, accessibility and tests where relevant.

### LAW 2 — ZERO-OMISSION 1:1 TELEGRAM SOURCE PORTING (EXACT SETTINGS, PHYSICS, MATRIX, SHADERS & SUBSYSTEMS)
Whenever the owner provides official Telegram Android source (https://github.com/DrKLO/Telegram) or asks to build/port a feature like Telegram, shallow constant-picking and excuse-making are STRICTLY FORBIDDEN. Always authenticate its official origin and pin a full commit SHA. Port the minimum complete functional subsystem 1:1:
- **Exact Matrix & Canvas Pipeline**: Port every `Canvas` draw pass, `Matrix` transformation (`preTranslate`, `postScale`, `postRotate`, `concat`), `Camera` 3D rotation, `Shader` (`LinearGradient`, `RadialGradient`, `BitmapShader`, `RuntimeShader`), `PorterDuffXfermode`, `saveLayerAlpha`, clip path, and custom `Drawable` state.
- **Exact Physics, Springs & Motion**: Port every `AnimatedFloat`, `SpringAnimation` / `DynamicAnimation` (`stiffness`, `dampingRatio`), `CubicBezierInterpolator` (`EASE_OUT_QUINT`, `EASE_BOTH`, custom bezier curves), `ValueAnimator` frame progression, `VelocityTracker` fling/dismiss math, overscroll resistance math, and haptic feedback timing verbatim.
- **Exact Settings, Options & State Branches**: Port every sub-setting, toggle, popup menu item, gesture handler, error shake (`shakeViewSpring`), empty/loading skeleton state, and companion helper class from the upstream source so nothing is left out.
- **Zero Host Interference**: Ensure `EveBaseActivity`, `TelegramThemeApplier`, `DesignViewInflater`, and parent `ViewGroup` clipping (`clipChildren` / `clipToPadding`) never override or flatten the ported Telegram component's custom matrix, colors, or touch physics.
- **Elimination of Excuse Loopholes**: The former ADAPTED and BLOCKED excuse loopholes are permanently eliminated (`NOT FOUND` only applies if an upstream file genuinely does not exist in the archive). Do not substitute an unofficial implementation without owner permission. Deliver the EXACT upstream implementation and runtime behavior.
- **Provenance & Attribution**: Provenance goes in code comments: `// Telegram: <path>:<lines> @ <short sha>`. Record path/lines or hashes for what was copied. Keep GPL attribution and THIRD_PARTY_NOTICES.md.

### LAW 3 — ALL TELEGRAM FILES & UI ARCHITECTURE ARE PERMANENT
All ported Telegram files, components (`TelegramActionBar`, `TelegramBottomSheet`, `TelegramNativeAlertDialog`, `TextCell`, `HeaderCell`, `ShadowSectionCell`, `DividerCell`, `TextCheckCell`, `TextDetailCell`, `TextInfoPrivacyCell`, `RadioCell`, `CheckBoxCell`, `TelegramSwitch`, `TelegramRadioButton`, `TelegramCheckBox2`, `TelegramTabsView`, `RLottieNative`, `CubicBezierInterpolator`, `LayoutHelper`, `BackDrawable`, `MenuDrawable`, `ButtonBounce`, `TelegramThemeEngine`, QR & Gooey renderers), and flat Telegram layouts are a permanent part of the repository. Never revert them to Material 3 or legacy iOS 26 components. Current Telegram-derived architecture is canonical.

### LAW 4 — 100% UNRESTRICTED OWNER FREEDOM TO ADD / DELETE / CHANGE ANYTHING (ZERO TEST LOCKS)
The Owner has absolute, unrestricted authority to add, delete, or change any file, screen, activity, layout, Telegram component, color, dimension, or feature at any time (No UI/UX choice is permanently locked against the Owner).
NO unit test, instrumentation test, lint check, shell runner, or Python CI script is ever permitted to block an Owner-requested addition, deletion, or modification. Whenever the Owner adds, deletes, or changes anything, the agent MUST automatically update or delete any conflicting test, fixture count, or script in the exact same commit—NEVER revert or compromise the Owner's requested change to appease an old test.

### LAW 5 — MANDATORY UNCONDITIONAL CI BUILD RECOVERY LOOP (FAILED BUILD = TASK NOT COMPLETE)
A task is NEVER complete if ANY local build, unit test, instrumentation test, fast compile (FAST), or full verification (FULL) build fails.
1. Never abandon a failing build, never hand a broken build back to the owner, and never push a docs-only commit to bypass `.github/workflows/build.yml`.
2. Whenever any build fails, immediately fetch the failure logs (`gh run view <run_id> --log-failed`), trace the root cause, fix the code or outdated test forward, commit, and push the fix to `main`.
3. Poll and verify the new GitHub Actions run on `main` (`gh run watch <run_id> --exit-status`) until `conclusion == "success"` (100% GREEN). Repeat as many times as needed until the build succeeds. Only after the pushed `main` commit passes green is the task considered complete.

### LAW 6 — ZERO-STOP 100% FULL-SCOPE TASK COMPLETION (NEVER STOP AT PARTIAL COMPLETION)
Every requirement in the owner's prompt must be completed 100% end-to-end before generating a completion report. When the Owner requests a multi-phase or app-wide task:
1. **Never Stop After Phase 1 or Batch 1**: Never interpret batch discipline as permission to stop after Batch 1. Execute sequential, coherent `main` batches continuously (Batch 1 -> push & verify green CI -> Batch 2 -> push & verify green CI -> Batch 3 -> push & verify green CI -> ...) in the same uninterrupted session until 100% of every requested phase, screen, layout, adapter, and component across the entire repository is completed.
2. **Never Skip Secondary Screens or Adapters**: Never omit any layout, activity, dialog, cell, adapter, or component within scope.
3. **Never Trust Historical `"DONE"` Labels in Docs**: Historical `"DONE"` entries in `docs/TELEGRAM_PORT_LEDGER.md` or old reports do NOT prove that live XML layouts (`app/src/main/res/layout/*.xml`) or Kotlin screens (`app/src/main/java/com/eve/app/ui/**/*.kt`) are finished. Always audit live code and layouts directly (`grep`) and complete every remaining file before generating the final completion report.

### LAW 7 — ZERO-SHORTCUT & ANTI-WRAPPER ENFORCEMENT (NO FAKE SUBCLASSES, NO COSMETIC RENAMES, NO NO-OP STUBS)
Taking shortcuts to bypass grep checks or fake Telegram compatibility is strictly forbidden and actively validated by `tools/check_no_shortcuts.py`:
1. **Strict Ban on Fake Inheritance / Aliasing**: Never create a `Telegram*` class that extends, wraps, or typealiases a legacy `UiArc*`, `Design*`, or `Material*` class (e.g., `class TelegramX : UiArcX` is strictly forbidden) just to satisfy a grep or naming check.
2. **Strict Ban on No-Op Physics / Animation Stubs**: Never replace an animated component with a static `text = value.toString()` or empty method. Every `Telegram*` component must implement the complete upstream `DrKLO/Telegram` (3de37c8cfb03) Canvas, Matrix, CubicBezierInterpolator, SpringAnimation, ButtonBounce, or NumberTextView algorithm.

### LAW 8 — MANDATORY MASTERPIECE SERVER, BACKEND & RULES SYNCHRONIZATION
Whenever any feature, field, permission, or UI flow is added, changed, or removed on Android, the agent MUST simultaneously inspect and update all corresponding Cloudflare Worker routes (`backend/src/routes/`), D1 SQL migrations (`backend/migrations/`), `firestore.rules`, `storage.rules`, and backend tests (`backend/test/`) to masterpiece production quality so every function works end-to-end with zero client/server contract mismatch.

### LAW 9 — CONTINUOUS DEAD-FILE & JUNK-FILE PURGE
Zero unused, obsolete, duplicate, or dead files may remain in the repository. Whenever a legacy class (`UiArc*`, `Design*`, `EveGlassToolbar`), unused layout, unused drawable, or temporary script is replaced or no longer referenced, delete it in the same commit and keep the tree 100% clean.

## 2. Core Workflow Rules
1. Do exactly what the owner asked. Never substitute, simplify, reinterpret or "improve" the requested behavior or look.
2. Stay in scope: the owner's request, not unrelated work. Unrelated problems get one line under "Noticed but not touched". Do not silently broaden product scope.
3. Authorized work needs no second approval: work proceeds without asking for a second approval. Ask only for an owner-only product, security, data or legal decision, state the exact blocker, and keep working on everything else.
4. If an instruction is wrong, impossible or unsafe: implement the rest, skip that part, and explain why in the report with a better option. Never blindly follow it and never silently change it.
5. Never fake evidence. No EXACT, VERIFIED, TESTED, device-checked or CI-green claim without real proof. Report implementation present separately from functional, visual/gesture, device and release acceptance; a green build does not prove exact UI/UX.
6. Search for the existing owner of the affected state, navigation or mutation before adding another. Replace old code, do not stack a second copy. Remove only files proven obsolete (check callers, keep required migrations, licenses and public contracts).
7. Execute every phase of the owner's request to 100% completion across as many sequential finished batches as required: for each batch, implement, self-review, push to `main`, and verify exact-SHA FAST build green before starting the next batch, and write the final report ONLY after all requested phases and screens are 100% completed. Do not stop at a plan, audit, checkpoint, or partial Phase 1 batch.
8. Commit and push as many coherent implementation batches as the task needs. There is no numeric commit ceiling, and no push ceiling; use a pull-request or authorized fast-forward batch with one writer. Reconcile remote main before each push. Keep each batch independently reviewable and implementation-focused. No force push, no history rewrite, no report-only or checkpoint-only commits.
9. Each pushed implementation batch must pass exact-SHA FAST CI on its final SHA. Diagnose the first real error and repair forward; do not rerun the same failure without a cause. If any build fails, fix it, push to `main`, and wait until the new build succeeds before proceeding or reporting completion. Treat emulator/ADB/GPU infrastructure errors separately from application test failures. Full build-failure elimination: maintain zero build failures across FAST and FULL CI pipelines.

## 3. CI, git, security
- Ordinary main push runs compile-only FAST (ARM64 assembleDebug). FAST green means BUILDABLE only. FULL (all tests, both emulator navigation modes, signing, gated Eve-debug-apk) runs only when the owner dispatches it manually via workflow_dispatch.
- Do not run heavy Gradle/NDK/emulator builds on the phone when hosted CI is available. Termux is for source operations and light checks.
- The server stays authoritative for scoring, rank, answers, admin rights and attempt dedupe. Firebase Auth identity, Worker/D1 admin authority, answer secrecy and account isolation are never weakened.
- Live Worker deployment, remote D1 migrations, destructive production data changes, public APK distribution and licensing exceptions require separate explicit owner authorization.
- Never commit secrets, google-services.json contents, tokens or admin emails. Never stage functions/package-lock.json. Never bypass tests or permissions to force a green check.

## 4. Report format (short)
- Per item: changed behavior with status EXACT.
- Telegram source SHA and main files used.
- Commit SHA(s) for all completed batches and verified green FAST/FULL CI results.
- Noticed but not touched.
- What the owner must check on the device.
