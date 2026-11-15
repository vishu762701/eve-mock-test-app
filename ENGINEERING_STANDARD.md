# EVE Engineering Standard

The single workflow policy and repository constitution. Authority order: (1) the owner's latest explicit message, (2) this file, (3) SOURCE_OF_TRUTH.md (architecture + security), (4) existing code. Old prompts, reports and checkpoints are history, never requirements.

## 1. The 4 Permanent Core Laws

### LAW 1 — MASTERPIECE GENIUS HUMAN CRAFTSMANSHIP (100% EFFORT & HIGHEST THINKING ALWAYS)
Every AI model or agent working on EVE must operate at 100% maximum cognitive depth, architectural foresight, and attention to detail—NOT like a shortcut-taking AI, but like a world-class Principal Engineer and Masterpiece Product Designer. Think through every micro-interaction, frame timing, touch state, pixel alignment, memory lifecycle, dark/light transition, and edge case to 100% perfection. Never ship lazy approximations, placeholders, or "good enough" wrappers. Own the complete authorized outcome. Done means it works end-to-end in the running app: UI, state, backend, resources, Day/Night, RTL, Hindi/Devanagari, back/gesture navigation, small screens, accessibility and tests where relevant.

### LAW 2 — ZERO-OMISSION 1:1 TELEGRAM SOURCE PORTING (EXACT SETTINGS, PHYSICS, MATRIX, SHADERS & SUBSYSTEMS)
Whenever the owner provides official Telegram Android source (https://github.com/DrKLO/Telegram) or asks to build/port a feature like Telegram, shallow constant-picking and excuse-making are STRICTLY FORBIDDEN. Always authenticate its official origin and pin a full commit SHA. Port the minimum complete functional subsystem 1:1:
- **Exact Matrix & Canvas Pipeline**: Port every `Canvas` draw pass, `Matrix` transformation (`preTranslate`, `postScale`, `postRotate`, `concat`), `Camera` 3D rotation, `Shader` (`LinearGradient`, `RadialGradient`, `BitmapShader`, `RuntimeShader`), `PorterDuffXfermode`, `saveLayerAlpha`, clip path, and custom `Drawable` state.
- **Exact Physics, Springs & Motion**: Port every `AnimatedFloat`, `SpringAnimation` / `DynamicAnimation` (`stiffness`, `dampingRatio`), `CubicBezierInterpolator` (`EASE_OUT_QUINT`, `EASE_BOTH`, custom bezier curves), `ValueAnimator` frame progression, `VelocityTracker` fling/dismiss math, overscroll resistance math, and haptic feedback timing verbatim.
- **Exact Settings, Options & State Branches**: Port every sub-setting, toggle, popup menu item, gesture handler, error shake (`shakeViewSpring`), empty/loading skeleton state, and companion helper class from the upstream source so nothing is left out.
- **Zero Host Interference**: Ensure `EveBaseActivity`, `TelegramThemeApplier`, `DesignViewInflater`, and parent `ViewGroup` clipping (`clipChildren` / `clipToPadding`) never override or flatten the ported Telegram component's custom matrix, colors, or touch physics.
- **Elimination of Excuse Loopholes**: The former ADAPTED and BLOCKED excuse loopholes are permanently eliminated. Do not substitute an unofficial implementation without owner permission. Deliver the EXACT upstream implementation and runtime behavior.
- **Provenance & Attribution**: Provenance goes in code comments: `// Telegram: <path>:<lines> @ <short sha>`. Record path/lines or hashes for what was copied. Keep GPL attribution and THIRD_PARTY_NOTICES.md.

### LAW 3 — ALL TELEGRAM FILES & UI ARCHITECTURE ARE PERMANENT
All ported Telegram files, components (`TelegramActionBar`, `TelegramBottomSheet`, `TelegramNativeAlertDialog`, `TextCell`, `HeaderCell`, `ShadowSectionCell`, `DividerCell`, `TextCheckCell`, `TextDetailCell`, `TextInfoPrivacyCell`, `RadioCell`, `CheckBoxCell`, `TelegramSwitch`, `TelegramRadioButton`, `TelegramCheckBox2`, `TelegramTabsView`, `RLottieNative`, `CubicBezierInterpolator`, `LayoutHelper`, `BackDrawable`, `MenuDrawable`, `ButtonBounce`, `TelegramThemeEngine`, QR & Gooey renderers), and flat Telegram layouts are a permanent part of the repository. Never revert them to Material 3 or legacy iOS 26 components. Current Telegram-derived architecture is canonical.

### LAW 4 — 100% UNRESTRICTED OWNER FREEDOM TO ADD / DELETE / CHANGE ANYTHING (ZERO TEST LOCKS)
The Owner has absolute, unrestricted authority to add, delete, or change any file, screen, activity, layout, Telegram component, color, dimension, or feature at any time (No UI/UX choice is permanently locked against the Owner).
NO unit test, instrumentation test, lint check, shell runner, or Python CI script is ever permitted to block an Owner-requested addition, deletion, or modification. Whenever the Owner adds, deletes, or changes anything, the agent MUST automatically update or delete any conflicting test, fixture count, or script in the exact same commit—NEVER revert or compromise the Owner's requested change to appease an old test.

## 2. Core Workflow Rules
1. Do exactly what the owner asked. Never substitute, simplify, reinterpret or "improve" the requested behavior or look.
2. Stay in scope: the owner's request, not unrelated work. Unrelated problems get one line under "Noticed but not touched". Do not silently broaden product scope.
3. Authorized work needs no second approval: work proceeds without asking for a second approval. Ask only for an owner-only product, security, data or legal decision, state the exact blocker, and keep working on everything else.
4. If an instruction is wrong, impossible or unsafe: implement the rest, skip that part, and explain why in the report with a better option. Never blindly follow it and never silently change it.
5. Never fake evidence. No EXACT, VERIFIED, TESTED, device-checked or CI-green claim without real proof. Report implementation present separately from functional, visual/gesture, device and release acceptance; a green build does not prove exact UI/UX.
6. Search for the existing owner of the affected state, navigation or mutation before adding another. Replace old code, do not stack a second copy. Remove only files proven obsolete (check callers, keep required migrations, licenses and public contracts).
7. One request = one finished batch: implement, self-review, build (exact-SHA FAST), report. Do not invent a never-ending mission and do not stop at a plan, audit or checkpoint.
8. Commit and push as many coherent implementation batches as the task needs. There is no numeric commit ceiling, and no push ceiling; use a pull-request or authorized fast-forward batch with one writer. Reconcile remote main before each push. Keep each batch independently reviewable and implementation-focused. No force push, no history rewrite, no report-only or checkpoint-only commits.
9. Each pushed implementation batch must pass exact-SHA FAST CI on its final SHA. Diagnose the first real error and repair forward; do not rerun the same failure without a cause. Treat emulator/ADB/GPU infrastructure errors separately from application test failures. Full build-failure elimination: maintain zero build failures across FAST and FULL CI pipelines.

## 3. CI, git, security
- Ordinary main push runs compile-only FAST (ARM64 assembleDebug). FAST green means BUILDABLE only. FULL (all tests, both emulator navigation modes, signing, gated Eve-debug-apk) runs only when the owner dispatches it manually via workflow_dispatch.
- Do not run heavy Gradle/NDK/emulator builds on the phone when hosted CI is available. Termux is for source operations and light checks.
- The server stays authoritative for scoring, rank, answers, admin rights and attempt dedupe. Firebase Auth identity, Worker/D1 admin authority, answer secrecy and account isolation are never weakened.
- Live Worker deployment, remote D1 migrations, destructive production data changes, public APK distribution and licensing exceptions require separate explicit owner authorization.
- Never commit secrets, google-services.json contents, tokens or admin emails. Never stage functions/package-lock.json. Never bypass tests or permissions to force a green check.

## 4. Report format (short)
- Per item: changed behavior with status EXACT.
- Telegram source SHA and main files used.
- Commit SHA and FAST result.
- Noticed but not touched.
- What the owner must check on the device.
