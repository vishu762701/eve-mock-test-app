# EVE — Admin Function Matrix

Date: 2026-10-22. **Not a claim that every Admin feature works.** Each item below has a real source location (or a retired component); `Source present` means code was inspected, **not** verified persistence. Backend auth middleware and `requireAdmin` independently gate privileged API mutations. No production account/data mutation, push broadcast or paid transaction was executed for this audit.

| Feature | Source (inspection point) | Backend/guard | Status at this checkpoint | Missing evidence |
|---|---|---|---|---|
| Dashboard security | AdminActivity.kt | Telegram ActionBar + AdminAccessGate.requireAdminFresh | Telegram-exact Admin Hub (Mission 03 F1) | Header block (AvatarDrawable), search expand, 5 sections (Content, People, Communication, Insights, System); verified reachable |
| Exam hierarchy/create | ManageExamsActivity.kt:194–311 | ExamRepository + exam backend | Source present; fresh gate added | Full create/edit persistence NOT VERIFIED |
| Exam rename and image | ManageQuestionsActivity.kt / AdminActivity.kt | AdminViewModel/ExamRepository | Moved to ManageQuestionsActivity in F1 | Media provider/full backend NOT VERIFIED |
| Exam deletion | AdminActivity.kt / ManageQuestionsActivity.kt | AdminDeleteFlow / backend | Source present; wired in F1 hub | Destructive integration on isolated DB NOT VERIFIED |
| Publish/duration/negative marking | ManageExamsActivity.kt:311–335 | ExamRepository / backend | Source present | Real limits/save/refresh NOT VERIFIED |
| Question manual create/edit | ManageQuestionsActivity.kt / AdminActivity.kt | AdminViewModel/Questions backend | Moved to ManageQuestionsActivity in F1 | Answer secrecy negative integration NOT VERIFIED |
| Bulk question import | ManageQuestionsActivity.kt | QuestionImportHelper | Moved to ManageQuestionsActivity in F1 | Large/invalid real PDF inputs NOT VERIFIED |
| Generated tests | AdminActivity.kt; GeneratedTestsActivity.kt | Generation jobs/backend | Reachable from Content section in F1 hub | Live AI provider/schedule NOT VERIFIED |
| Admin users, counts and paging | ManageUsersActivity.kt:64,138+ | Admin user endpoint paged | Fresh authorization plus backend 200+ simulated cases | Real 200+ accounts NOT VERIFIED |
| Ban/unban and role changes | ManageUsersActivity.kt; AdminActivity.kt | AdminRepository / POST /api/admin/users/:id/status | HARDENED IN MISSION 02 (F2) | Self-ban rejected (409); admin-ban rejected (409); users.disabled indexed (migration 0020); lightweight SELECT disabled query in authMiddleware; proved by admin_guards.test.mjs |
| Admin add/remove | AdminsAndRightsActivity.kt; AdminActivity.kt | AdminRepository / /api/admin/admins | HARDENED IN MISSION 02 (F2); TELEGRAM RIGHTS IN MISSION 03 (F2) | Granular 13 canonical admin rights (migration 0022), ChatRightsEditActivity, owner immutability, no self-escalation, requirePermission middleware; verified by admin_rights.test.mjs |
| Syllabus PDF upload | ManageSyllabusActivity.kt:86,131–395 | Syllabus repository | Auth gate/20MB bounded reader in source | Live provider success/failure NOT VERIFIED |
| Banners | AdminActivity.kt:207 | HomeBannerRepository | Source present | Production publication/dedupe NOT VERIFIED |
| Floating link | AdminActivity.kt:210; MainActivity.kt | FloatingLinkRepository | HTTPS validator present | Cross-account placement NOT VERIFIED |
| Send broadcast | SendNotificationActivity.kt:72,330–403 | POST /api/broadcasts / requireAdmin | Fresh UI gate and backend guard | Safe live push transport NOT VERIFIED; UI verified sending to All Users only with no dead category controls |
| Flagged questions | AdminActivity.kt:224 | FlaggedQuestionsActivity.kt | Source present | Real moderation persistence NOT VERIFIED |
| Student feedback and replies | AdminActivity.kt:227–234 | Feedback repository | Source present | Reply delivery/update NOT VERIFIED |
| Polls | ManagePollsActivity.kt | Poll backend | Source present | All CRUD paths NOT VERIFIED |
| Analytics | AdminActivity.kt:241 | AdminAnalyticsActivity.kt | Source present | Source-derived aggregates/time filters NOT VERIFIED |
| System Monitor | AdminActivity.kt:237 | SystemMonitorActivity.kt | Source present | Production monitoring signals NOT VERIFIED |
| API Usage | AdminActivity.kt:247 | ApiUsageActivity.kt / GET /api/admin/api-usage | HARDENED IN MISSION 02 (F3) | Server-side D1 usage tracking via migration 0021 (api_usage_monthly); real attempts and audit writes counted; client Firestore imports and client-side increment calls completely removed; firebase-firestore and firebase-functions removed from app module; proved by api_usage.test.mjs |
| Activity and audit logs | AdminActivity.kt:244 | ActivityLogActivity.kt / /api/admin/audit-log | HARDENED IN MISSION 02 (F1); TELEGRAM RECENT ACTIONS IN MISSION 03 (F3) | Telegram ChannelAdminLogActivity model, cursor pagination (before), admin & action filters, multilingual search, sticky date headers, initials avatar, metadata redaction, compound indexes (migration 0023); verified by recent_actions.test.mjs |, target_id, metadata, source); audited across all admin routes in D1; client log writes removed; proved by admin_audit_log.test.mjs |
| Export data | AdminActivity.kt:250 | Admin export flow / GET /api/admin/export/exams | HARDENED IN MISSION 02 (F4) | Server-side chunked export endpoint; strictly zero student/user PII; server-side audit logging; atomic file write (temp file + rename) on Android; client-side N+1 loops removed; proved by admin_export.test.mjs |
| Premium grant/config | AdminActivity.kt:213 | ManagePremiumActivity.kt / /api/admin/premium/users/* | HARDENED IN MISSION 02 (F2) | Lifetime grant requires confirmLifetime: true; duration validated 1..3650 days; lifetime extend rejected (409); planName trimmed and capped (100 chars); client confirmation dialog added; proved by admin_guards.test.mjs |
| App config + maintenance | AdminActivity.kt:253,1244+ | AppConfigActivity.kt / PUT /api/admin/config | HARDENED IN MISSION 02 (F2/F3) | PUT /api/admin/config validates types/bounds; redundant Firebase callable removed; Worker endpoint is single authoritative path; layout updated; proved by admin_guards.test.mjs |
| Minimum app version | AdminActivity.kt version status | Backend app-config | Source present; verified bound at runtime | Real app-update force-gate NOT VERIFIED |
| Typography Studio | Retired: activity_admin.xml and manifest | All writable editor API paths removed | Retired by owner | Public published GET intentionally retained for old clients |

## Prioritized unresolved actions

1. Run isolated data-backed add/edit/remove and admin authorization-negative tests for each privileged route; high-risk changes (payments, roles, bans, deletion, mass push) should not be sent to production as QA.
2. Run live-account send/receive/read-all notification QA only in a permitted test target, never broadcast to real users.
3. Verify actual 200+ user pagination, count and stale search response handling against an authorized staging backend.
4. Test all dialog cancellation, loading, error, retry, and refresh outcomes on Android including TalkBack.

## Status semantics

- **VERIFIED SOURCE**: click wiring, route or gate code actually read; not an end-to-end pass.
- **FIXED IN SOURCE**: code mutation committed; runtime success requires CI/device result.
- **NOT VERIFIED**: no safe full execution/evidence this session. No row is called WORKS without end-to-end proof.

## Fresh-gate extension (2026-10-22)

Source changes additionally gate initialization for: ActivityLogActivity.kt, ApiUsageActivity.kt, AdminAnalyticsActivity.kt, FlaggedQuestionsActivity.kt, AppConfigActivity.kt, EditAboutActivity.kt, FeedbackMessagesActivity.kt, EditExamActivity.kt, GeneratedTestsActivity.kt, ManagePremiumActivity.kt, SystemMonitorActivity.kt. Together with previously gated AdminActivity, ManageExams, SendNotification, ManageUsers, ManageSyllabus, ManageExistingExams and ManagePolls, all **18 enumerated Admin Activity classes** now call a fresh `AdminAccessGate` before their protected loaders; this statement covers direct source usage, not runtime authorization/device behavior. Always retain separate backend `requireAdmin` checks.

## Additional broadcast integrity controls (2026-10-22)

Broadcast list limit parsing now rejects malformed/non-positive inputs and caps valid requests to 100; send validates title/message/type/category bounds server-side with matching Android form limits; bulk-delete rejects invalid/duplicate/unbounded IDs before constructing D1 batch statements. These are code + deterministic backend-test changes, not a proof of safe live push.

**HIGH PRIORITY UNKNOWN:** Admin UI labels category notifications as targeted, but the inspected `GET /api/broadcasts` route does not filter by recipient. The user `category` field in D1 represents profile metadata and has not been shown to represent enrolled-exam notification targeting. Do **not** claim audience restriction works or send sensitive category notifications until an authoritative recipient-mapping contract is established and end-to-end tested. This audit does not silently change live audience or existing broadcast records.


## Verified notification-delivery architecture (source audit)

`backend/src/routes/broadcasts.ts` is a D1 inbox route while `functions/index.js` contains an FCM publisher bound to **Firestore** document creation. They are **not** the same event stream. The Admin composer now accurately states in-app inbox publication only. Category sends are rejected at the API with HTTP 422 until recipient membership can be proven; non-admin GET excludes pre-existing category-targeted rows. This is a defensive reduction of a misleading interface, not an implementation of live push. Real FCM mass-send testing was intentionally NOT RUN.


## Admin deletion acknowledgment hardening

D1 deletion results now determine user-visible success: single DELETE returns 404 for zero actual changed rows; bulk delete returns `data.count` of confirmed changes (and `requested`) rather than falsely reporting every selected ID removed. The Admin list refreshes and displays actual counts, including partial success. Isolated fake-D1 tests exercise real Hono handlers. Production destructive QA remains NOT RUN.


## Shared-device inbox isolation

`NotificationStore` now stores data per authenticated UID (opaque hashed preference namespace) instead of exposing the unowned legacy global inbox to subsequent accounts. Historical global bytes are retained for safety but never attributed automatically. Server inbox data is fetched independently for each account. Cross-device push and local guest migration remain UNKNOWN.


## Account deletion safety (2026-10-22)

Settings now rejects any unsuccessful D1 deletion response and propagates Firebase Auth deletion failures, rather than reporting false success. Successful deletion clears only UID-owned notification/test session/offline queue/attempt-limit artifacts on the device. This is a code-level repair, not a live Firebase account test. Cross-provider partial-delete recovery still needs staging credentials and end-to-end verification.
