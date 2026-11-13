# EVE Engineering Standard

The single workflow policy. Authority order: (1) the owner's latest explicit message, (2) this file, (3) SOURCE_OF_TRUTH.md (architecture + security), (4) existing code. Old prompts, reports and checkpoints are history, never requirements.

## 1. Core rules
1. Do exactly what the owner asked. Never substitute, simplify, reinterpret or "improve" the requested behavior or look.
2. Own the complete authorized outcome. Done means it works end-to-end in the running app: UI, state, backend, resources, Day/Night, RTL, Hindi/Devanagari, back/gesture navigation, small screens, accessibility and tests where relevant.
3. Stay in scope: the owner's request, not unrelated work. Unrelated problems get one line under "Noticed but not touched". Do not silently broaden product scope.
4. Authorized work needs no second approval. Do not stop for routine confirmation or repeated approvals. Ask only for an owner-only product, security, data or legal decision, state the exact blocker, and keep working on everything else. A task is implemented without asking for a second approval.
5. If an instruction is wrong, impossible or unsafe: implement the rest, skip that part, and explain why in the report with a better option. Never blindly follow it and never silently change it.
6. Never fake evidence. No EXACT, VERIFIED, TESTED, device-checked or CI-green claim without real proof. Unknown = UNKNOWN. Blocked = BLOCKED. Report implementation present separately from functional, visual/gesture, device and release acceptance; a green build does not prove exact UI/UX.
7. Search for the existing owner of the affected state, navigation or mutation before adding another. Replace old code, do not stack a second copy. Remove only files proven obsolete (check callers, keep required migrations, licenses and public contracts).
8. Development mode: the owner is building a private app and may change any UI/UX. No UI/UX choice is permanently locked. Current Telegram-derived code is the current implementation, not a standing requirement.

## 2. Telegram source rules (whenever the owner mentions Telegram or "exact")
1. Only source: official https://github.com/DrKLO/Telegram. Authenticate its official origin and pin a full commit SHA, using either a Git checkout of that repo or the owner's Telegram source zip (Telegram-master.zip / Telegrammaster.zip, in phone storage or attached) whose files match that repo. Reuse the verified pin within the same task; do not re-authenticate for ordinary EVE fixes. Never use memory, blogs, forks, Material/AndroidX lookalikes or an AI recreation. Do not substitute an unofficial implementation without owner permission.
2. Source missing or unreadable: mark that item BLOCKED, do not substitute, continue other work.
3. Take the minimum complete functional subsystem, not one screenshot: the Activity/Fragment/View class, its cells, Theme keys, strings, drawables and Lottie, animation and gesture/physics code, edge cases, and the callers that open it.
4. Port by copying real logic and constants, never by eye: dp sizes, text sizes, colors (Theme key_* light and dark values), durations, interpolators, spring/physics values, haptics, z-order, insets, RTL.
5. Allowed adaptation only: data source (EVE backend instead of MTProto/TL), account/identity, branding strings, removal of messaging/network. Everything else stays identical. Comment each adaptation with its reason. Never import Telegram credentials, messaging, network or unrelated services.
6. Provenance goes in code comments, not new docs: `// Telegram: <path>:<lines> @ <short sha>` on each ported class or constant block. Pin the full SHA in the commit message. No per-feature ledger or report files in the repo.
7. When a Telegram component is ported, remove the old EVE implementation of it.
8. Status per item: EXACT (actual unchanged upstream code or values, hash/diff verified), ADAPTED (anything modified, with what differs and why), NOT FOUND (Telegram has no equivalent), BLOCKED. Modified code is ADAPTED even if believed equivalent. Only the owner's device decides "looks exact".
9. EVE-only features with no Telegram equivalent: compose them from ported Telegram primitives and say so.
10. Keep GPL attribution and THIRD_PARTY_NOTICES.md. Deep procedure, byte verification and licensing gate: docs/telegram-integration-process.md and docs/telegram-release-compliance-gate.md.

## 3. Workflow for every request
1. Understand: write the requested outcome as a short item list. Every item must be covered at the end.
2. Inspect current remote main and the production code the request touches.
3. Source (Telegram tasks): locate and map each item to Telegram files, then port per section 2.
4. Implement the complete flow, including resources, state, backend and tests where relevant.
5. Self-review as a strict critic: compare the diff against each item. Hunt flicker, repeated reloads, leaks, wrong geometry/animation, dark mode, rotation, process death, empty/error states.
6. Run the smallest relevant local checks first (backend: npm test and npm run build).
7. Commit to main as one coherent batch. There is no numeric commit ceiling; use a pull-request or authorized fast-forward batch with one writer. Reconcile remote main before push. No force push, no history rewrite, no report-only or checkpoint-only commits.
8. Each pushed implementation batch must pass exact-SHA FAST CI on its final SHA. Diagnose the first real error and repair forward; do not rerun the same failure without a cause. Treat emulator/ADB/GPU infrastructure errors separately from application test failures.
9. Report (section 5).
10. The owner checks on a real device. Corrections return to step 1 with the owner's words as the new request.

## 4. CI, git, security
- Ordinary main push runs compile-only FAST (ARM64 assembleDebug). FAST green means BUILDABLE only. FULL (all tests, both emulator navigation modes, signing, gated Eve-debug-apk) runs only when the owner dispatches it manually via workflow_dispatch, and is the final integrated-candidate gate, not a tax on small commits.
- Do not run heavy Gradle/NDK/emulator builds on the phone when hosted CI is available. Termux is for source operations and light checks.
- The server stays authoritative for scoring, rank, answers, admin rights and attempt dedupe. Firebase Auth identity, Worker/D1 admin authority, answer secrecy and account isolation are never weakened.
- Live Worker deployment, remote D1 migrations, destructive production data changes, public APK distribution and licensing exceptions require separate explicit owner authorization.
- Never commit secrets, google-services.json contents, tokens or admin emails. Never stage functions/package-lock.json. Never bypass tests or permissions to force a green check.

## 5. Report format (short)
- Per item: changed behavior with status EXACT / ADAPTED / NOT FOUND / BLOCKED / UNKNOWN.
- Telegram source SHA and main files used.
- Commit SHA and FAST result.
- Noticed but not touched.
- What the owner must check on the device.

## 6. Never
- Never claim work continues after the session ends.
- Never treat a green build, a push or a summary as proof the UI is exact.
- Never execute old task documents without current owner authorization.
- Never add documentation files unless the owner asks.
