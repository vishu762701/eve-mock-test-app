# EVE Engineering Standard

The single workflow policy. Authority order: (1) owner's latest message, (2) this file, (3) SOURCE_OF_TRUTH.md (architecture + security), (4) existing code. Old prompts, reports and checkpoints are history, never requirements.

## 1. Core rules
1. Do exactly what the owner asked. Never substitute, simplify, reinterpret or "improve" the requested behavior or look.
2. Done means it works end-to-end in the running app: UI, state, backend, resources, Day/Night, Hindi/Devanagari, back/gesture navigation, small screens.
3. Stay in scope. Unrelated problems get one line under "Noticed but not touched". Do not silently widen scope.
4. Do not ask for routine confirmation. Ask only for an owner-only product/security/legal decision, and keep working on everything else meanwhile.
5. If an instruction is wrong, impossible or unsafe: implement the rest, skip that part, and say why in the report with a better option. Do not blindly follow it and do not silently change it.
6. Never fake evidence. No EXACT, VERIFIED, TESTED, device-checked or CI-green claim without real proof. Unknown = UNKNOWN. Blocked = BLOCKED.
7. Search for the existing owner of the affected state, navigation or mutation before adding another. Replace old code, do not stack a second copy. Delete only code proven obsolete (check callers).

## 2. Telegram source rules (apply whenever the owner mentions Telegram or "exact")
1. Only source: official https://github.com/DrKLO/Telegram, pinned to a full commit SHA, or the owner's Telegram-master.zip if its files match that repo. Never use memory, blogs, forks, Material/AndroidX lookalikes or an AI recreation.
2. Source missing or unreadable: mark that item BLOCKED, do not substitute, continue other work.
3. Take the whole subsystem, not one screenshot: the Activity/Fragment/View class, its cells, Theme keys, strings, drawables and Lottie, animation and gesture/physics code, edge cases, and the callers that open it.
4. Port by copying real logic and constants, never by eye: dp sizes, text sizes, colors (Theme key_* light and dark values), durations, interpolators, spring/physics values, haptics, z-order, insets, RTL.
5. Allowed adaptation only: data source (Eve backend instead of MTProto/TL), account/identity, branding strings, removal of messaging/network. Everything else stays identical. Comment every adaptation with the reason.
6. Provenance lives in code comments, not new docs: `// Telegram: <path>:<lines> @ <short sha>` on each ported class/constant block. Pin the full SHA in the commit message. No per-feature ledger or report files in the repo.
7. When a Telegram component is ported, remove the old Eve implementation of it.
8. Status per item: EXACT (same code/values, only data plumbing changed), ADAPTED (what differs and why), NOT FOUND (Telegram has no equivalent), BLOCKED. Implementation present is not visual acceptance; only the owner's device decides "looks exact".
9. Eve-only features with no Telegram equivalent: compose from the ported Telegram primitives and say so.
10. Keep GPL attribution and THIRD_PARTY_NOTICES.md. Never import Telegram credentials, messaging, MTProto or unrelated services.

## 3. Workflow for every request
1. Understand: write the requested outcome as a short item list. Every item must be covered at the end.
2. Inspect: current remote main and the production code the request touches.
3. Source (Telegram tasks): locate and map items to Telegram files, then port per section 2.
4. Implement the complete flow, including resources, state, backend and tests where relevant.
5. Self-review as a strict critic: compare the diff against each item. Hunt flicker, repeated reloads, leaks, wrong geometry/animation, dark mode, rotation, process death, empty/error states.
6. Run the smallest relevant local checks (backend: npm test, npm run build; Android: focused unit tests only when relevant).
7. Commit and push to main as one coherent batch. Reconcile remote first. No force push, no history rewrite, no report-only or checkpoint-only commits.
8. Check the exact-SHA FAST build. Fix the first real error forward. Do not rerun the same failure without a cause.
9. Report (section 5).
10. The owner checks on a real device. Corrections go back to step 1 with the owner's words as the new request.

## 4. Git, CI, security
- Branch: main only. Ordinary push runs compile-only FAST (ARM64 assembleDebug). FAST green means BUILDABLE only.
- FULL (all tests, both emulator nav modes, signing, gated Eve-debug-apk) runs only when the owner dispatches it manually.
- Do not run heavy Gradle/NDK/emulator builds on the phone; use hosted CI. Termux is for source operations and light checks.
- Server stays authoritative for scoring, rank, answers, admin rights, attempt dedupe. Firebase Auth identity, Worker/D1 admin authority, answer secrecy and account isolation are never weakened.
- Live Worker deploy, remote D1 migration, destructive production data changes, public APK distribution and licensing exceptions need separate explicit owner approval.
- Never commit secrets, google-services.json contents, tokens or admin emails. Never stage functions/package-lock.json.

## 5. Report format (short)
- Changed behavior (one line per item) with status EXACT / ADAPTED / NOT FOUND / BLOCKED / UNKNOWN.
- Telegram source SHA and main files used.
- Commit SHA and FAST result.
- Noticed but not touched.
- What the owner must check on the device.

## 6. Never
- Never claim work continues after the session ends.
- Never treat a green build, a push or a summary as proof the UI is exact.
- Never execute old task documents without current owner authorization.
- Never add documentation files unless the owner asks.
