# TELEGRAM FIDELITY CONTRACT

1. If Telegram source is unavailable in this session, STOP and ask the owner for `Telegram-master-core.zip` (default; SHA-256 `3ecbbeab06e0cafebd462721f048b637a454522167db94d26c52ccad10ea1db0`), `Telegram-master-extra.zip` (SHA-256 `ef00b8495bbd7775e5e9a9c2acecbf87b9ebac472a30651964fd38b833245517`), or the original complete `Telegram-master.zip` (SHA-256 `0785365538ce05cf5bdf44a15d8fd5f5219f8193db0a2d124af9665fc63fd7b3`). Accept only these archive SHA-256 values; verify the attached archive's `EVE_MANIFEST.sha256`. A file absent from the attached archive is NOT FOUND; request the specific `extra` or `rest-1..5` archive. Never redraw or guess.
2. Port small leaf classes as-is (Java may stay Java), with only thin glue. Adapt screens using cited source values.
3. Every Telegram task delivers a parity table: property | EVE file:line | Telegram file:line | value | action.
4. A green build is never proof of fidelity.
5. List every deviation explicitly.

Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md.

# Repository agent entrypoint

Telegram source is supplied by the owner as a session attachment, not stored in this repository. Source identity/provenance: see `SOURCE_OF_TRUTH.md`; apply the fidelity contract above.

- The latest explicit user request defines the authorized task and takes precedence over historical plans and design documents, subject to safety and data-integrity safeguards.
- Inspect actual code, callers, resources, Day/Night behavior, backend contracts, tests and CI relevant to that task. Implement the requested change end to end; do not stop at proposing prompts unless asked.
- Use APPROVED_UI_IMPLEMENTATION_AUDIT.md for current UI baseline, not as an override of later user-approved changes.
- Preserve Telegram reveal, Lottie assets/playback, server-side scoring and required CI gates unless explicitly authorized to modify them.
- **Permanent mission-final Native UI policy (all models/chats):** every intermediate change, repair, checkpoint and partial implementation uses FAST only (APK compilation, source compilation, JVM tests and backend; NO Android emulator). Do NOT trigger FULL based on elapsed time, 15–20 pushes, perceived risk or partial task completion. Treat a multi-step mission as incomplete until ALL its specified deliverables are implemented. Then trigger FULL exactly for that final implementation commit with `[eve-full]`; manual FULL dispatch is reserved for an explicit owner request. If final FULL fails, fix via FAST commits and rerun FULL at the repaired final commit. Both native navigation modes, Day/Night evidence and verified APK publication remain required for mission completion; see ENGINEERING_STANDARD.md.
- **Main-only delivery policy:** implement directly on `main` (no feature branches or implementation PRs unless the owner explicitly reverses this). Read the current remote SHA, make small atomic commits based on it, and update `main` only with a non-force fast-forward and expected-head lease. Keep a reversible commit SHA and revert on a proven failure; never force-push or silently overwrite concurrent commits. Report blocked verification truthfully.
