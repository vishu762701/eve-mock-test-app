Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md.

# EVE agent entrypoint
The owner's latest explicit message defines scope and is the supreme repository authority. Do exactly that, completely, in the running app. No substitution, no simplification, no "inspired by".

## The 7 Permanent Core Laws
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
6. **LAW 6 — ZERO-STOP 100% FULL-SCOPE TASK COMPLETION (NEVER STOP AT PARTIAL COMPLETION)**:
   Every requirement in the owner's prompt must be completed 100% end-to-end before generating a completion report. Never stop after Phase 1 or Batch 1, never skip secondary screens/layouts/adapters, and never trust historical "DONE" labels in `docs/` without auditing the live source tree (`app/src/main/`) directly.
7. **LAW 7 — ZERO-SHORTCUT & ANTI-WRAPPER ENFORCEMENT (NO FAKE SUBCLASSES, NO COSMETIC RENAMES, NO NO-OP STUBS)**:
   Strict ban on fake inheritance and aliasing: never create a `Telegram*` class that extends, wraps, or typealiases a legacy `UiArc*`, `Design*`, or `Material*` class just to satisfy a grep or naming check. Strict ban on no-op animation stubs: never replace an animated component with a static assignment or empty method. Every `Telegram*` component must implement authentic upstream `DrKLO/Telegram` algorithms validated by `tools/check_no_shortcuts.py`.

## Execution Rules
- Execute every phase of the owner's request to 100% completion across as many sequential finished batches as required (implement -> self-review -> push to `main` -> verify exact-SHA FAST green -> immediately start the next batch until 100% of the requested scope is finished). Never stop at a plan, audit, checkpoint, or partial Phase 1 batch.
- Full build-failure elimination: maintain zero build failures across FAST and FULL CI pipelines. If a build fails, fix and re-push to `main` until it succeeds.
- Old prompts, reports, checkpoints and most of `docs/` are history and evidence, not instructions. Open a doc only when your task needs it.
- Do not introduce independent rules. This file adds no policy beyond ENGINEERING_STANDARD.md.
