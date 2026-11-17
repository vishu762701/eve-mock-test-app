# EVE Engineering Standard

The single rules file for every agent (Antigravity, Codex, ChatGPT, Claude, Argon). Authority order: (1) the owner's latest explicit message, (2) this file, (3) SOURCE_OF_TRUTH.md (architecture + security), (4) existing code. Old prompts, reports and checkpoints are history, never requirements.

## 1. The 9 Laws

**LAW 1 - Finish it for real.** Do exactly what the owner asked, completely, in the running app. No substitute, no simplified version, no "inspired by". Done means it works on screen: Day/Night, RTL, Hindi/Devanagari, back/gesture navigation, small screens, accessibility, backend and tests where relevant. Think through touch states, frame timing and edge cases before coding.

**LAW 2 - Official Telegram source is the only truth for Telegram look and behavior.** For anything "like Telegram", copy upstream code from `DrKLO/Telegram`; never rewrite it from memory, never invent constants, never approximate. Follow the Telegram Port Protocol in section 2. If something cannot be ported exactly, label it honestly (ADAPTED or BLOCKED with the reason) and keep going on the rest. An honest label is never a failure; a false EXACT is.

**LAW 3 - Reuse the existing owner of a thing.** Ported Telegram files, shims and `Telegram*` components are the building blocks. Do not replace them with Material or hand-made lookalikes, and never add a second copy of a class that already exists (search first; replace the old one).

**LAW 4 - The owner can change anything.** No UI/UX choice is permanently locked. No test, script or old rule may block an owner-requested add, delete or change: update or delete the conflicting test in the same commit. Only the security and server-authority invariants in SOURCE_OF_TRUTH.md section 3 need an explicit owner decision to change.

**LAW 5 - A red build means the task is not done.** If any local build, test, FAST or FULL CI run fails, fetch the log (`gh run view <id> --log-failed`), fix forward, push to `main`, and poll (`gh run watch <id> --exit-status`) until it is green. Never hand back a broken build, never push a docs-only commit to dodge `build.yml`.

**LAW 6 - Work on 1 screen or 1 feature at a time.** Finish that one target 100% end to end (layout, Kotlin, Telegram motion, tests, backend sync), push, verify exact-SHA FAST green, deliver the APK, then move on. Never launch an app-wide change in one blast.

**LAW 7 - ZERO-SHORTCUT.** `tools/check_no_shortcuts.py` and `tools/check_no_stubs.py` run on every build. No fake `Telegram*` wrapper around a legacy `UiArc*`/`Design*`/`Material*` class, no typealias tricks, no no-op animation stubs, no low-contrast hardcoded colors (use semantic tokens / `TelegramThemeEngine` roles). A file that is not byte-identical to upstream is ADAPTED, never EXACT.

**LAW 8 - Backend and rules stay in sync.** If a screen or feature adds, changes or removes a backend field, permission or flow, update the Worker routes (`backend/src/routes/`), D1 migrations, `firestore.rules`, `storage.rules` and backend tests in the same change.

**LAW 9 - Purge dead files in the touched scope.** When a legacy class, layout or drawable is replaced and has zero callers, delete it in the same commit. Remove only what is proven obsolete.

## 2. Telegram Port Protocol (one screen or feature per run)

**Owner gives:** the Telegram class or screen name, a Telegram screenshot/video of it, and the Eve screen it replaces. Telegram source: the owner's zip (chat attachment or phone storage), or a pinned official `DrKLO/Telegram` checkout.

**A. Pin the source.** Record the full commit SHA (a GitHub zip usually carries it in the archive comment: `unzip -z <zip>`; with no SHA, record the archive sha256). Pin once per session and reuse it.

**B. Closure table first (before any code).** Open the real upstream entry class and trace every class, resource, style, string, animation and Lottie it needs, transitively. Give each item one disposition:
- **VERBATIM** - copy the byte-identical upstream file to the same path under `app/src/main/java/org/telegram/...` (or `res/`). Copy with `cp`/unzip, never retype. Register it in `docs/telegram-verbatim-imports.json` with its sha256.
- **SHIM** - an Eve implementation of a Telegram framework class that only exists because of Telegram's own app (`AndroidUtilities`, `LocaleController`, `Theme` colors, `ApplicationLoader`, ...). Put it under `org/telegram/messenger/` or `org/telegram/ui/ActionBar/`, header `// EVE-SHIM`, keep only what the screen needs, list every shim in the report.
- **CUT** - account, chat, network or message-database dependency. Replace with a small interface that the Eve adapter implements. Never silently drop it.
Show the table in the report; do not wait for approval.

**C. Never edit a verbatim file casually.** A needed change makes it ADAPTED: keep the change as a few marked `// EVE-PATCH:` lines and list each one in the report.

**D. Eve logic lives outside Telegram code.** Auth, exam data, admin rights, navigation and server calls go in `com.eve.app...` adapters only, never inside `org/telegram/...`.

**E. Build.** Push to `main`, compile on GitHub Actions (FAST). Do not run heavy Gradle/NDK/emulator builds on the phone.

**F. Compare and label.** For every element write: `upstream path:lines@SHA | EVE path:lines | EXACT / ADAPTED (reason) / NOT FOUND / BLOCKED`. EXACT needs proof by path/lines or hashes (byte/hash or diff of the unchanged upstream file) plus matching runtime behavior (physics, Day/Night, RTL, accessibility, cancellation, lifecycle). Audit every custom View, Canvas, Matrix, blur, pivot and inset in that screen's package before saying it is done.

**G. Clean up.** Delete the old Eve screen files that are now unused (LAW 9) in the same push.

## 3. Core workflow rules
1. Stay in scope. Unrelated problems get one line under "Noticed but not touched".
2. Authorized work needs no second approval. Ask only for an owner-only product, security, data or legal decision.
3. Never fake evidence. No EXACT, VERIFIED, TESTED, device-checked or CI-green claim without real proof. Report implementation present separately from functional, visual/gesture and device acceptance. Compiling is not acceptance.
4. Search for the existing owner of the state, navigation or mutation before adding another; replace, do not stack.
5. Commit and push coherent changes to `main` only (no feature branches, no force-push). Reconcile remote main before each push. Exact-SHA FAST CI must pass on the final SHA. Treat emulator/ADB infrastructure errors separately from app test failures.
6. `[skip ci]` batching: when pushing several sequential commits, put `[skip ci]` in every commit message except the last, so exactly one clean run happens on the final SHA.
7. Never commit secrets, google-services.json contents, tokens or admin emails. Never stage `functions/package-lock.json`.
8. Live Worker deployment, remote D1 migrations, destructive production data changes, public APK distribution and licensing exceptions need separate explicit owner authorization.
9. If a real tool or session limit stops you, leave one short handoff: HEAD, uncommitted work, failing gate, next action.

## 4. Report format (short)
- Screen/feature behavior and status per element (EXACT / ADAPTED / NOT FOUND / BLOCKED).
- Telegram source SHA, closure table (VERBATIM / SHIM / CUT counts) and main files used.
- Commit SHA(s), exact-SHA FAST/FULL CI result, APK artifact link.
- Noticed but not touched.
- What the owner should check on the device, compared to the Telegram screenshot.
