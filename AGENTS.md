Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md.

# Repository agent entrypoint

- The latest explicit user request defines the authorized task and takes precedence over historical plans and design documents, subject to safety and data-integrity safeguards.
- Inspect actual code, callers, resources, Day/Night behavior, backend contracts, tests and CI relevant to that task. Implement the requested change end to end; do not stop at proposing prompts unless asked.
- Use APPROVED_UI_IMPLEMENTATION_AUDIT.md for current UI baseline, not as an override of later user-approved changes.
- Preserve Telegram reveal, Lottie assets/playback, server-side scoring and required CI gates unless explicitly authorized to modify them.
- Work safely on origin/main; fetch and check its head before writes, never force-push, and report blocked verification truthfully.
