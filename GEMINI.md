# Eve Project Operating Instructions & Autonomous Execution Guidelines

Apply this working style to all tasks from now on.

## Autonomous Execution Rules
1. **Autonomous Decision-Making**: When there are multiple reasonable ways to implement something and the task description doesn't specify one, pick the most standard, stable, and widely-used approach independently and proceed. Note what was chosen and why in the final summary.
2. **No Routine Confirmation Check-ins**: Do not repeatedly stop to ask "should I proceed?", "do you want me to continue?", "should I go with approach A or B?", or similar confirmation questions during implementation.
3. **Self-Healing Build & Bug Fixing**: If a build error, compilation issue, lint failure, or bug is encountered while implementing, diagnose and fix it directly and keep going. Do not stop to report the error and wait for permission to fix it.
4. **Full Scope Implementation**: Autonomously install dependencies, create and edit files, choose libraries already implied by the existing tech stack, write code across multiple files, and refactor as needed to make features work cleanly.
5. **Final Comprehensive Reporting**: At the end of the task, provide a concise summary of what was implemented, key design decisions made, and exact verification results.

## Stop & Ask Triggers (Strict Exceptions Only)
Only stop and ask directly if:
- Something in the task is genuinely ambiguous in a way that would lead to building the wrong thing entirely (not a minor implementation detail).
- An action is destructive or irreversible outside of normal version control (e.g., deleting production database records, not deleting a local file that can be regenerated or restored via Git).
- Missing credentials, secrets, or account access required to continue.

## Eve Architecture & Project Guardrails
- **Tech Stack**: Kotlin, MVVM, AndroidX, Material 3, Firebase (Firestore, Auth, Cloud Functions).
- **Lockfile Hygiene**: `functions/package-lock.json` must remain untouched and untracked. Do not stage, commit, or delete it.
- **Admin Consistency**: Keep `Constants.ADMIN_EMAILS` in `Constants.kt` strictly synchronized with `isHardcodedAdmin()` in `firestore.rules`.
- **Verification Integrity**: Never claim a feature or fix works without executing actual verification (e.g., `./gradlew assembleDebug`, `git diff --check`, or syntax/resource validation).
## Strict Scope Discipline (Critical)
- **Only touch what the current prompt asks for.** Do not re-open, re-edit, "improve," refactor, or re-verify code that was implemented for a previous, already-completed prompt — even if you notice something you'd personally do differently, even if it seems related.
- If you notice an unrelated bug or improvement opportunity outside the current prompt's scope while working, do NOT fix it. Just note it in one line in the final summary under a "Noticed but not touched (outside scope)" heading, and move on.
- Treat every previous prompt's implementation as **frozen/completed** unless the current prompt explicitly names that exact feature/file/bug to be changed again.
- Before editing any file, ask: "Does this specific prompt require changing this file?" If the answer is no, do not open or modify it — even for a "quick unrelated fix."
- This rule exists because re-touching completed work wastes build time and reintroduces risk of breaking things that were already verified working. Violating this rule is a failure condition for the task.
