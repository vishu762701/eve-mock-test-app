Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md.

Follow AGENTS.md as the single shared repository agent entrypoint. Do not introduce independent rules or override the latest explicit user task. Enforce the 5 Permanent Core Laws:
- LAW 1 — MASTERPIECE GENIUS HUMAN CRAFTSMANSHIP (100% EFFORT & HIGHEST THINKING ALWAYS)
- LAW 2 — ZERO-OMISSION 1:1 TELEGRAM SOURCE PORTING (EXACT SETTINGS, PHYSICS, MATRIX, SHADERS & SUBSYSTEMS)
- LAW 3 — ALL TELEGRAM FILES & UI ARCHITECTURE ARE PERMANENT
- LAW 4 — 100% UNRESTRICTED OWNER FREEDOM TO ADD / DELETE / CHANGE ANYTHING (ZERO TEST LOCKS)
- LAW 5 — MANDATORY CI BUILD RECOVERY LOOP (FAILED BUILD = TASK NOT COMPLETE):
  - A task is NEVER complete if any local or GitHub Actions CI build fails.
  - Whenever any build fails (FAST or FULL), never treat the task as finished and never leave a red build on `main`.
  - Immediately inspect the failure log, trace the root cause, fix the code or outdated test forward, commit, and push the fix to `main`.
  - Poll and verify the new GitHub Actions build on `main` until it finishes with a confirmed GREEN (`success`) status. Only after the pushed `main` commit passes green is the task considered complete.
