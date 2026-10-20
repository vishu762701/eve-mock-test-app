# EVE Engineering Backlog — Active Items Only

This file lists outstanding work and verification gaps, **not instructions to replay**. Start work only when the current user request authorizes it. Completed historical changes are retained in Git history and `DECISIONS.md`; latest UI evidence is under `docs/design-reset/`.

## P0 — Premium payment production readiness
- Security guard: `backend/src/routes/premium.ts`. Default development signing secret is rejected; sandbox payment simulation is admin-only and opt-in.
- **UNKNOWN:** Confirm a unique high-entropy `PREMIUM_WEBHOOK_SECRET` is set on the Cloudflare Worker and that the actual payment callback sender uses the matching value. No secret should be stored in source, client models or the legacy D1 config field.
- Verify against the real provider using a legitimate test order, including callback authenticity, paid/failed/refunded statuses and entitlement consistency. Do not enable `PREMIUM_SANDBOX_ENABLED` in production.
- Impact: Without a configured signing secret, new purchases correctly remain unavailable. Do not bypass this gate to restore checkout.

## P1 — Real device and account verification
- Perform a signed-in Android phone walkthrough (light/dark, gesture/three-button): login, exam discovery, start/submit, review/history, Admin controls, and payment status.
- Hosted Android CI checks are not equivalent to this authenticated production verification.
- Verify API 24–25 navigation fallback and real compositor blur, which emulator evidence cannot fully certify.

## P1 — GitHub branch integrity
- GitHub `main` currently has branch protection disabled (observed during repository audit). Enable a ruleset preventing force-push and deletion when account controls permit, without breaking the approved direct-push workflow.
- This setting cannot be changed by normal repository file commits.

## P2 — UI accessibility and platform surfaces
- Audit 320dp widths at system font scales 1.78x and 2.0x, especially drawer rows and forms.
- Inspect OS-controlled notifications/toasts and native DatePicker limits; do not invent custom platform replacements without product approval.
- Lottie artwork color adaptation remains a product decision. Preserve existing Lottie resources, timing, playback and triggers until the owner explicitly approves changing them.

## P2 — Release acceptance
- Android release workflow validates credentials and app-owned lint/unit checks before uploading APK/AAB.
- **UNKNOWN:** Confirm a tagged release's actual R8/minification behavior, signature, artifact provenance and device installation before distributing production binaries.
- Release/signing secrets are external configuration; do not commit them.

## Out-of-scope historical records
Use `DECISIONS.md`, `docs/design-reset/report.md`, and Git history for prior UI migration and deleted-resource proof. Those records do not override the current task.
