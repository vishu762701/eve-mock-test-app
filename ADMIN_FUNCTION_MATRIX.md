# EVE — Admin Function Matrix

Date: 2026-10-22. **Not a claim that every Admin feature works.** Each item below has a real source location (or a retired component); `Source present` means code was inspected, **not** verified persistence. Backend auth middleware and `requireAdmin` independently gate privileged API mutations. No production account/data mutation, push broadcast or paid transaction was executed for this audit.

| Feature | Source (inspection point) | Backend/guard | Status at this checkpoint | Missing evidence |
|---|---|---|---|---|
| Dashboard security | AdminActivity.kt:120–135 | AdminAccessGate.requireAdminFresh | Fresh gate added | Native authorized/unauthorized live flow NOT VERIFIED |
| Exam hierarchy/create | ManageExamsActivity.kt:194–311 | ExamRepository + exam backend | Source present; fresh gate added | Full create/edit persistence NOT VERIFIED |
| Exam rename and image | AdminActivity.kt:183–184 | AdminViewModel/ExamRepository | Source present | Media provider/full backend NOT VERIFIED |
| Exam deletion | AdminActivity.kt:185 | AdminDeleteFlow / backend | Source present | Destructive integration on isolated DB NOT VERIFIED |
| Publish/duration/negative marking | ManageExamsActivity.kt:311–335 | ExamRepository / backend | Source present | Real limits/save/refresh NOT VERIFIED |
| Question manual create/edit | AdminActivity.kt:262–271 | AdminViewModel/Questions backend | Source present | Answer secrecy negative integration NOT VERIFIED |
| Bulk question import | AdminActivity.kt:272+ | QuestionImportHelper | Source present | Large/invalid real PDF inputs NOT VERIFIED |
| Generated tests | AdminActivity.kt:204; GeneratedTestsActivity.kt | Generation jobs/backend | Source present | Live AI provider/schedule NOT VERIFIED |
| Admin users, counts and paging | ManageUsersActivity.kt:64,138+ | Admin user endpoint paged | Fresh authorization plus backend 200+ simulated cases | Real 200+ accounts NOT VERIFIED |
| Ban/unban and role changes | ManageUsersActivity.kt; AdminActivity.kt | AdminRepository | Source present | Destructive negative tests/live write NOT VERIFIED |
| Admin add/remove | AdminActivity.kt:186; admin users | AdminRepository | Source present | Last-admin protection NOT VERIFIED |
| Syllabus PDF upload | ManageSyllabusActivity.kt:86,131–395 | Syllabus repository | Auth gate/20MB bounded reader in source | Live provider success/failure NOT VERIFIED |
| Banners | AdminActivity.kt:207 | HomeBannerRepository | Source present | Production publication/dedupe NOT VERIFIED |
| Floating link | AdminActivity.kt:210; MainActivity.kt | FloatingLinkRepository | HTTPS validator present | Cross-account placement NOT VERIFIED |
| Send broadcast | SendNotificationActivity.kt:72,330–403 | POST /api/broadcasts / requireAdmin | Fresh UI gate and backend guard | Safe live push transport NOT VERIFIED |
| Flagged questions | AdminActivity.kt:224 | FlaggedQuestionsActivity.kt | Source present | Real moderation persistence NOT VERIFIED |
| Student feedback and replies | AdminActivity.kt:227–234 | Feedback repository | Source present | Reply delivery/update NOT VERIFIED |
| Polls | ManagePollsActivity.kt | Poll backend | Source present | All CRUD paths NOT VERIFIED |
| Analytics | AdminActivity.kt:241 | AdminAnalyticsActivity.kt | Source present | Source-derived aggregates/time filters NOT VERIFIED |
| System Monitor | AdminActivity.kt:237 | SystemMonitorActivity.kt | Source present | Production monitoring signals NOT VERIFIED |
| API Usage | AdminActivity.kt:247 | ApiUsageActivity.kt | Source present | Provider cost correlation NOT VERIFIED |
| Activity and audit logs | AdminActivity.kt:244 | ActivityLogActivity.kt | Source present | Redaction/retention NOT VERIFIED |
| Export data | AdminActivity.kt:250 | Admin export flow | Source present | PII exposure permissions NOT VERIFIED |
| Premium grant/config | AdminActivity.kt:213 | ManagePremiumActivity.kt | Source present | Real payment/entitlements NOT VERIFIED |
| App config + maintenance | AdminActivity.kt:253,1244+ | AppConfigActivity.kt / backend | Source present | Cross-device maintenance/write confirm NOT VERIFIED |
| Minimum app version | AdminActivity.kt version status | Backend app-config | Source present | Real app-update force-gate NOT VERIFIED |
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
