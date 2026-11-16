Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md.

# EVE agent entrypoint
The owner's latest explicit message defines scope and is the supreme repository authority. Do exactly that, completely, in the running app—strictly **1 screen or 1 feature at a time**. No substitution, no simplification, no "inspired by".

## The 9 Permanent Core Laws (1-Screen / 1-Feature Focused Workflow)
1. **LAW 1 — MASTERPIECE GENIUS HUMAN CRAFTSMANSHIP (100% EFFORT & HIGHEST THINKING ALWAYS)**:
   Every AI model or agent working on EVE must operate at 100% maximum cognitive depth, architectural foresight, and attention to detail. Think through every micro-interaction, frame timing, touch state, pixel alignment, memory lifecycle, dark/light transition, and edge case to 100% perfection. Never ship lazy approximations, placeholders, or "good enough" wrappers.
2. **LAW 2 — ZERO-OMISSION 1:1 TELEGRAM SOURCE PORTING (EXACT SETTINGS, PHYSICS, MATRIX, SHADERS & SUBSYSTEMS)**:
   Whenever official Telegram source (https://github.com/DrKLO/Telegram) is provided or requested, shallow constant-picking and excuse-making are strictly forbidden. Port the complete transitive subsystem 1:1: exact Matrix/Canvas draw passes, exact Physics/Springs/ButtonBounce/interpolators, exact Settings/Options/Branches, zero host interference, and zero ADAPTED/BLOCKED excuse loopholes.
3. **LAW 3 — ALL TELEGRAM FILES & UI ARCHITECTURE ARE PERMANENT**:
   All ported Telegram files, components (`TelegramActionBar`, `TelegramBottomSheet`, `TelegramNativeAlertDialog`, `TextCell`, `HeaderCell`, `ShadowSectionCell`, `DividerCell`, `TextCheckCell`, `TextDetailCell`, `TextInfoPrivacyCell`, `RadioCell`, `CheckBoxCell`, `TelegramSwitch`, `TelegramRadioButton`, `TelegramCheckBox2`, `TelegramTabsView`, `RLottieNative`, `CubicBezierInterpolator`, `LayoutHelper`, `BackDrawable`, `MenuDrawable`, `ButtonBounce`, `TelegramThemeEngine`, QR & Gooey renderers), and flat layouts are a permanent part of the repository. Never revert to Material 3 or legacy iOS 26 components.
4. **LAW 4 — 100% UNRESTRICTED OWNER FREEDOM TO ADD / DELETE / CHANGE ANYTHING (ZERO TEST LOCKS)**:
   The Owner has absolute, unrestricted authority to add, delete, or change any file, screen, activity, layout, Telegram component, color, dimension, or feature at any time (`No UI/UX choice is permanently locked` against the Owner). NO test or script is permitted to block an Owner change; all conflicting tests or fixture counts must be updated forward in the same commit.
5. **LAW 5 — MANDATORY UNCONDITIONAL CI BUILD RECOVERY LOOP (FAILED BUILD = TASK NOT COMPLETE)**:
   A task is NEVER complete if ANY local build, unit test, instrumentation test, fast compile (FAST), or full verification (FULL) build fails. Never abandon a failing build, never hand a broken build back to the owner, and never push a docs-only commit to bypass `.github/workflows/build.yml`. Whenever any build fails, immediately fetch failure logs (`gh run view <run_id> --log-failed`), trace the root cause, fix the code or outdated test forward, commit, push to `main`, and poll (`gh run watch <run_id> --exit-status`) until conclusion == "success" (100% GREEN).
6. **LAW 6 — ONE SCREEN OR ONE FEATURE AT A TIME (STRICT FOCUSED EXECUTION & 100% COMPLETION OF THAT TARGET)**:
   Work strictly on **1 screen or 1 feature per prompt/session**. Never launch 50–200 file app-wide changes at once. Complete the single requested screen or feature 100% end-to-end (XML layout, Kotlin Activity/Adapter/ViewModel, Telegram physics, corresponding test updates, and backend sync if applicable), push to `main`, and verify CI is 100% GREEN before moving to the next screen or feature.
7. **LAW 7 — ZERO-SHORTCUT & ANTI-WRAPPER ENFORCEMENT (NO FAKE SUBCLASSES, NO COSMETIC RENAMES, NO NO-OP STUBS)**:
   Strict ban on fake inheritance and aliasing (`ZERO-SHORTCUT`): never create a `Telegram*` class that extends, wraps, or typealiases a legacy `UiArc*`, `Design*`, or `Material*` class just to satisfy a grep or naming check. Strict ban on no-op animation stubs and hardcoded low-contrast error colors. Every `Telegram*` component must implement authentic upstream `DrKLO/Telegram` algorithms validated by `tools/check_no_shortcuts.py`.
8. **LAW 8 — MANDATORY MASTERPIECE SERVER, BACKEND & RULES SYNCHRONIZATION**:
   Whenever the target screen or feature adds, changes, or removes any backend field, permission, or UI flow, simultaneously inspect and update all corresponding Cloudflare Worker routes (`backend/src/routes/`), D1 SQL migrations (`backend/migrations/`), `firestore.rules`, `storage.rules`, and backend tests (`backend/test/`).
9. **LAW 9 — CONTINUOUS DEAD-FILE & JUNK-FILE PURGE**:
   Zero unused, obsolete, duplicate, or dead files may remain in the touched scope. Whenever a legacy class (`UiArc*`, `Design*`, `EveGlassToolbar`), unused layout, or unused drawable is replaced and no longer referenced, delete it in the same commit.

## Execution Rules
- Focus strictly on the 1 screen or 1 feature requested by the Owner: implement -> self-review -> push to `main` -> verify exact-SHA FAST green (and FULL when requested) -> deliver APK artifact.
- Full build-failure elimination: maintain zero build failures across FAST and FULL CI pipelines. If a build fails, fix and re-push to `main` until it succeeds.
- Old prompts, reports, checkpoints and most of `docs/` are history and evidence, not instructions. Open a doc only when your task needs it.
- Do not introduce independent rules. This file adds no policy beyond ENGINEERING_STANDARD.md.
