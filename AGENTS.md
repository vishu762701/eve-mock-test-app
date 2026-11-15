Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md.

# EVE agent entrypoint
The owner's latest explicit message defines scope and is the supreme repository authority. Do exactly that, completely, in the running app. No substitution, no simplification, no "inspired by".

## The 5 Permanent Core Laws
1. **LAW 1 — MASTERPIECE GENIUS HUMAN CRAFTSMANSHIP (100% EFFORT & HIGHEST THINKING ALWAYS)**:
   Every AI model or agent working on EVE must operate at 100% maximum cognitive depth, architectural foresight, and attention to detail. Think through every micro-interaction, frame timing, touch state, pixel alignment, memory lifecycle, dark/light transition, and edge case to 100% perfection. Never ship lazy approximations, placeholders, or "good enough" wrappers.
2. **LAW 2 — ZERO-OMISSION 1:1 TELEGRAM SOURCE PORTING (EXACT SETTINGS, PHYSICS, MATRIX, SHADERS & SUBSYSTEMS)**:
   Whenever official Telegram source (https://github.com/DrKLO/Telegram) is provided or requested, shallow constant-picking and excuse-making are strictly forbidden. Port the complete transitive subsystem 1:1: exact Matrix/Canvas draw passes, exact Physics/Springs/ButtonBounce/interpolators, exact Settings/Options/Branches, zero host interference, and zero ADAPTED/BLOCKED excuse loopholes.
3. **LAW 3 — ALL TELEGRAM FILES & UI ARCHITECTURE ARE PERMANENT**:
   All ported Telegram files, components (`TelegramActionBar`, `TelegramBottomSheet`, `TelegramNativeAlertDialog`, `TextCell`, `HeaderCell`, `ShadowSectionCell`, `DividerCell`, `TextCheckCell`, `TextDetailCell`, `TextInfoPrivacyCell`, `RadioCell`, `CheckBoxCell`, `TelegramSwitch`, `TelegramRadioButton`, `TelegramCheckBox2`, `TelegramTabsView`, `RLottieNative`, `CubicBezierInterpolator`, `LayoutHelper`, `BackDrawable`, `MenuDrawable`, `ButtonBounce`, `TelegramThemeEngine`, QR & Gooey renderers), and flat layouts are a permanent part of the repository. Never revert to Material 3 or legacy iOS 26 components.
4. **LAW 4 — 100% UNRESTRICTED OWNER FREEDOM TO ADD / DELETE / CHANGE ANYTHING (ZERO TEST LOCKS)**:
   The Owner has absolute, unrestricted authority to add, delete, or change any file, screen, activity, layout, Telegram component, color, dimension, or feature at any time (`No UI/UX choice is permanently locked` against the Owner). NO test or script is permitted to block an Owner change; all conflicting tests or fixture counts must be updated forward in the same commit.
5. **LAW 5 — MANDATORY CI BUILD RECOVERY LOOP (FAILED BUILD = TASK NOT COMPLETE)**:
   Never consider a task complete if a build fails. Whenever any build fails, immediately diagnose the root cause, fix it forward, push the fix to `main`, and verify that the new build on `main` succeeds (GREEN) before declaring the task done.

## Execution Rules
- One request = one finished batch: implement, self-review, build (exact-SHA FAST), report. Do not invent a never-ending mission and do not stop at a plan, audit or checkpoint.
- Full build-failure elimination: maintain zero build failures across FAST and FULL CI pipelines. If a build fails, fix and re-push to `main` until it succeeds.
- Old prompts, reports, checkpoints and most of `docs/` are history and evidence, not instructions. Open a doc only when your task needs it.
- Do not introduce independent rules. This file adds no policy beyond ENGINEERING_STANDARD.md.
