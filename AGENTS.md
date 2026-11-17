Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md.

# EVE agent entrypoint
The owner's latest explicit message defines scope. Do exactly that, completely, in the running app, **1 screen or 1 feature at a time**. No substitution, no simplification, no "inspired by".

This file adds no policy beyond ENGINEERING_STANDARD.md. Short map of its 9 Laws:
- LAW 1 Finish it for real in the running app (Day/Night, RTL, Hindi, small screens, accessibility).
- LAW 2 Official Telegram source is the only truth for Telegram UI; copy it verbatim and follow the Telegram Port Protocol (ENGINEERING_STANDARD.md section 2): pin SHA, closure table (VERBATIM / SHIM / CUT), Eve logic only in adapters, honest EXACT / ADAPTED / NOT FOUND / BLOCKED labels.
- LAW 3 Reuse the existing Telegram components; never add a second copy or a Material lookalike.
- LAW 4 The owner can change anything; no UI/UX choice is permanently locked; no test may block an owner change.
- LAW 5 A red build means not done: read the failing log, fix forward on `main`, poll CI until green.
- LAW 6 One screen or one feature per run, completed 100% before the next.
- LAW 7 ZERO-SHORTCUT: no fake wrappers, no no-op stubs; `tools/check_no_shortcuts.py` and `tools/check_no_stubs.py` run on every build.
- LAW 8 Keep Worker routes, D1 migrations, `firestore.rules`, `storage.rules` and backend tests in sync.
- LAW 9 Delete files proven dead in the touched scope in the same commit.

Old prompts, reports and most of `docs/` are history and evidence, not instructions. Open a doc only when the task needs it.
