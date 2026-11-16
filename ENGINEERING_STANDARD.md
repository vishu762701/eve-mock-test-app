# EVE Engineering Standard

The single workflow policy and repository constitution. Authority order: (1) the owner's latest explicit message, (2) this file, (3) SOURCE_OF_TRUTH.md (architecture + security), (4) existing code. Old prompts, reports and checkpoints are history, never requirements.

## 1. The 9 Permanent Core Laws (1-Screen / 1-Feature Focused Workflow)

### LAW 1 — MASTERPIECE GENIUS HUMAN CRAFTSMANSHIP (100% EFFORT & HIGHEST THINKING ALWAYS)
Every AI model or agent working on EVE must operate at 100% maximum cognitive depth, architectural foresight, and attention to detail—like a world-class Principal Engineer and Masterpiece Product Designer. Think through every micro-interaction, frame timing, touch state, pixel alignment, memory lifecycle, dark/light transition, and edge case to 100% perfection. Never ship lazy approximations, placeholders, or "good enough" wrappers. Own the complete authorized outcome. Done means it works end-to-end in the running app: UI, state, backend, resources, Day/Night, RTL, Hindi/Devanagari, back/gesture navigation, small screens, accessibility and tests where relevant.

### LAW 2 — ZERO-OMISSION 1:1 TELEGRAM SOURCE PORTING (EXACT SETTINGS, PHYSICS, MATRIX, SHADERS & SUBSYSTEMS)
Whenever the owner provides official Telegram Android source (https://github.com/DrKLO/Telegram) or asks to build/port a feature like Telegram, shallow constant-picking and excuse-making are STRICTLY FORBIDDEN. Always authenticate its official origin and pin a full commit SHA. Port the minimum complete functional subsystem 1:1:
- **Exact Matrix & Canvas Pipeline**: Port every `Canvas` draw pass, `Matrix` transformation (`preTranslate`, `postScale`, `postRotate`, `concat`), `Camera` 3D rotation, `Shader` (`LinearGradient`, `RadialGradient`, `BitmapShader`, `RuntimeShader`), `PorterDuffXfermode`, `saveLayerAlpha`, clip path, and custom `Drawable` state.
- **Exact Physics, Springs & Motion**: Port every `AnimatedFloat`, `SpringAnimation` / `DynamicAnimation` (`stiffness`, `dampingRatio`), `CubicBezierInterpolator` (`EASE_OUT_QUINT`, `EASE_BOTH`, custom bezier curves), `ValueAnimator` frame progression, `VelocityTracker` fling/dismiss math, overscroll resistance math, and haptic feedback timing verbatim.
- **Exact Settings, Options & State Branches**: Port every sub-setting, toggle, popup menu item, gesture handler, error shake (`shakeViewSpring`), empty/loading skeleton state, and companion helper class from the upstream source so nothing is left out.
- **Zero Host Interference**: Ensure `EveBaseActivity`, `TelegramThemeApplier`, `DesignViewInflater`, and parent `ViewGroup` clipping (`clipChildren` / `clipToPadding`) never override or flatten the ported Telegram component's custom matrix, colors, or touch physics.
- **Elimination of Excuse Loopholes**: Do not substitute an unofficial implementation without owner permission. Deliver the EXACT upstream implementation and runtime behavior.
- **Provenance & Attribution**: Provenance goes in code comments: `// Telegram: <path>:<lines> @ <short sha>`. Keep GPL attribution and THIRD_PARTY_NOTICES.md.

### LAW 3 — ALL TELEGRAM FILES & UI ARCHITECTURE ARE PERMANENT
All ported Telegram files, components (`TelegramActionBar`, `TelegramBottomSheet`, `TelegramNativeAlertDialog`, `TextCell`, `HeaderCell`, `ShadowSectionCell`, `DividerCell`, `TextCheckCell`, `TextDetailCell`, `TextInfoPrivacyCell`, `RadioCell`, `CheckBoxCell`, `TelegramSwitch`, `TelegramRadioButton`, `TelegramCheckBox2`, `TelegramTabsView`, `RLottieNative`, `CubicBezierInterpolator`, `LayoutHelper`, `BackDrawable`, `MenuDrawable`, `ButtonBounce`, `TelegramThemeEngine`, QR & Gooey renderers), and flat Telegram layouts are a permanent part of the repository. Never revert them to Material 3 or legacy iOS 26 components.

### LAW 4 — 100% UNRESTRICTED OWNER FREEDOM TO ADD / DELETE / CHANGE ANYTHING (ZERO TEST LOCKS)
The Owner has absolute, unrestricted authority to add, delete, or change any file, screen, activity, layout, Telegram component, color, dimension, or feature at any time (No UI/UX choice is permanently locked against the Owner).
NO unit test, instrumentation test, lint check, shell runner, or Python CI script is ever permitted to block an Owner-requested addition, deletion, or modification. Whenever the Owner adds, deletes, or changes anything, the agent MUST automatically update or delete any conflicting test, fixture count, or script in the exact same commit—NEVER revert or compromise the Owner's requested change to appease an old test.

### LAW 5 — MANDATORY UNCONDITIONAL CI BUILD RECOVERY LOOP (FAILED BUILD = TASK NOT COMPLETE)
A task is NEVER complete if ANY local build, unit test, instrumentation test, fast compile (FAST), or full verification (FULL) build fails.
1. Never abandon a failing build, never hand a broken build back to the owner, and never push a docs-only commit to bypass `.github/workflows/build.yml`.
2. Whenever any build fails, immediately fetch the failure logs (`gh run view <run_id> --log-failed`), trace the root cause, fix the code or outdated test forward, commit, and push the fix to `main`.
3. Poll and verify the new GitHub Actions run on `main` (`gh run watch <run_id> --exit-status`) until `conclusion == "success"` (100% GREEN). Repeat as many times as needed until the build succeeds.

### LAW 6 — ONE SCREEN OR ONE FEATURE AT A TIME (STRICT FOCUSED EXECUTION & 100% COMPLETION OF THAT TARGET)
To guarantee rock-solid stability and zero cascading regressions, development is strictly scoped to **1 screen or 1 feature at a time**:
1. **Strict Single-Screen / Single-Feature Scope**: Work ONLY on the specific 1 screen or 1 feature requested by the Owner in the current prompt. Do not silently broaden product scope, and do not touch 50–200 unrelated files across the app in one giant blast.
2. **100% End-to-End Completion of That 1 Screen/Feature**: Complete that 1 requested screen or feature 100% end-to-end (XML layout, Activity/Adapter/ViewModel, Telegram physics/animations, corresponding unit/androidTest updates, and backend/rules sync if applicable) before finishing.
3. **Verify Green Build Before Next Feature**: Push the completed single screen/feature to `main`, verify exact-SHA FAST (and FULL when requested) passes 100% green, and deliver the APK artifact before moving to the next screen or feature.

### LAW 7 — ZERO-SHORTCUT & ANTI-WRAPPER ENFORCEMENT (NO FAKE SUBCLASSES, NO COSMETIC RENAMES, NO NO-OP STUBS)
Taking shortcuts to bypass grep checks or fake Telegram compatibility is strictly forbidden (`ZERO-SHORTCUT`) and actively validated by `tools/check_no_shortcuts.py`:
1. **Strict Ban on Fake Inheritance / Aliasing**: Never create a `Telegram*` class that extends, wraps, or typealiases a legacy `UiArc*`, `Design*`, or `Material*` class just to satisfy a grep or naming check.
2. **Strict Ban on No-Op Physics / Animation Stubs**: Never replace an animated component with a static assignment or empty method.
3. **Strict Ban on Low-Contrast Hardcoded Colors**: Always use WCAG-compliant semantic color tokens (`R.color.system_red_text`, `R.color.system_green_text`, `TelegramThemeEngine` palette roles) rather than raw low-contrast hex literals.

### LAW 8 — MANDATORY MASTERPIECE SERVER, BACKEND & RULES SYNCHRONIZATION
Whenever the target screen or feature adds, changes, or removes any backend field, permission, or API flow, simultaneously inspect and update the corresponding Cloudflare Worker routes (`backend/src/routes/`), D1 SQL migrations (`backend/migrations/`), `firestore.rules`, `storage.rules`, and backend tests (`backend/test/`) so the feature works end-to-end.

### LAW 9 — CONTINUOUS DEAD-FILE & JUNK-FILE PURGE
Remove only files proven obsolete within the touched scope. Whenever a legacy class, unused layout, or unused drawable is replaced by the current screen/feature change and has zero remaining callers, delete it in the same commit.

## 2. Core Workflow Rules
1. Do exactly what the owner asked—1 screen or 1 feature at a time. Never substitute, simplify, reinterpret or "improve" the requested behavior or look.
2. Stay in scope: the owner's request, not unrelated work. Unrelated problems get one line under "Noticed but not touched". Do not silently broaden product scope.
3. Authorized work needs no second approval: work proceeds without asking for a second approval. Ask only for an owner-only product, security, data or legal decision.
4. Never fake evidence. No EXACT, VERIFIED, TESTED, device-checked or CI-green claim without real proof. Report implementation present separately from functional, visual/gesture, device and release acceptance.
5. Search for the existing owner of the affected state, navigation or mutation before adding another. Replace old code, do not stack a second copy. Remove only files proven obsolete.
6. Commit and push coherent implementation changes for the target screen/feature. There is no numeric commit ceiling, and no push ceiling; use a pull-request or authorized fast-forward batch with one writer. Reconcile remote main before each push.
7. Each pushed implementation change must pass exact-SHA FAST CI on its final SHA. Treat emulator/ADB/GPU infrastructure errors separately from application test failures. Maintain zero build failures across FAST and FULL CI pipelines.

## 3. CI, git, security
- Ordinary main push runs compile-only FAST (ARM64 assembleDebug, test compilation, and lint). FULL (all tests, both emulator navigation modes, signing, gated Eve-debug-apk) runs when dispatched manually via workflow_dispatch, while `build-universal-dev-apk.yml` provides a fast 4-ABI debug APK artifact on demand.
- Do not run heavy Gradle/NDK/emulator builds on the phone when hosted CI is available.
- Live Worker deployment, remote D1 migrations, destructive production data changes, public APK distribution and licensing exceptions require separate explicit owner authorization.
- Never commit secrets, google-services.json contents, tokens or admin emails. Never stage functions/package-lock.json.

## 4. Report format (short)
- Changed screen/feature behavior with status EXACT.
- Telegram source SHA and main files used.
- Commit SHA(s) and verified green FAST/FULL CI results + APK artifact link.
- Noticed but not touched.
- What the owner should check on the device.

## 5. Minimal reporting & Development-only fast-build mode
- During execution: report only material failures or decisions.
- The owner is developing a private app and may change any UI/UX behavior. No UI/UX choice is permanently locked. Own the complete authorized outcome, but not unrelated work. Do not silently broaden product scope.
- Remove only files proven obsolete. For a source-exact request, authenticate its official origin and record path/lines or hashes plus runtime behavior where available. Do not substitute an unofficial implementation without owner permission. Proceed within the authorized scope without asking for a second approval.
- On ordinary main pushes, compile the debug APK and test sources rapidly. Keep manual FULL and release/security protections available and truthful.
- Automatic FAST is ARM64-only for rapid Android development; manual FULL builds all four native ABIs and exercises both emulator navigation modes.
