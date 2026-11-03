# Admin permission-bound navigation continuation

Scope: C1/C2/C3/C5 entry, loading, malformed response, cancellation and identity
isolation. Whole Admin action/UI/source-fidelity acceptance remains NOT MET.
Base main: 2e5655911e0755a0b3a1892a15d4bafd683631dd; FAST
[41394531948](https://github.com/vishu762701/eve-mock-test-app/actions/runs/41394531948)
SUCCESS; docs 41394531950 SUCCESS. Earlier scanner glass FAST 41392841830 is green.
No production write, Worker deploy, migration or entitlement change is involved.

## Actual defects and implementation

The dashboard's fresh membership gate returned before /api/admin/me rights loaded.
setupSearchIndex(null) exposed all actions; applyAdminPermissions(null) returned
without hiding them if rights failed. AdminViewModel construction also immediately
requested exams, the admin directory and user statistics regardless of destination
rights. Most privileged destinations required fresh membership but did not check
their specific Worker right before binding controls/loading data.

AdminAccessGate now reads fresh membership and permission data before revealing the
protected root or invoking its callback. AdminPermissionSnapshot validates the
response email, rights shape and destination right, including malformed owner
snapshots; the existing UID/email AdminIdentityGuard rechecks current Firebase
identity immediately before UI delivery. Cancellation propagates and failures
close the protected screen. Server owner status is read only from the authenticated
Worker response, never an email allowlist/Intent/local badge. No offline grant or
shared persisted rights cache is added. AdminRepository rejects missing response
data instead of synthesizing an empty success object.

The dashboard consumes this non-null confirmed snapshot directly, eliminating the
second unguarded loading/failure path. Search and visible rows use the same rights;
Version Status is read-only without maintenance/config rights and cannot open that
editor via search. All five original Telegram pager sections and legitimate owner
controls remain. ViewModel constructors no longer issue unrelated privileged reads:
all four actual callers were traced (AdminActivity, ManageQuestionsActivity,
AdminsAndRightsActivity, AdminRightsEditActivity). Existing explicit post-gate loads
remain; the dashboard requests user statistics only for its manage_users right.

| Existing destination(s) | Worker permission | Actual server path/evidence |
|---|---|---|
| ManageExams, ManageExistingExams, EditExam | manage_exams | routes/exams.ts create/edit/image/rename/delete |
| ManageQuestions | manage_questions | routes/questions.ts POST/batch/PUT/DELETE |
| GeneratedTests | manage_generated_tests | routes/generatedTests.ts schedule/status/delete/generate-now |
| ManageSyllabus | manage_syllabus | routes/exams.ts syllabus POST/DELETE |
| EmojiStudio, EditAbout | manage_content | routes/emoji.ts admin middleware; appContent.ts PUT |
| ManageUsers (create and resume gate) | manage_users | routes/admin.ts users/stats/status/attempts |
| AdminsAndRights, AdminRightsEdit | manage_admins_rights | routes/admin.ts directory/rights/insert/delete |
| ManagePremium | manage_premium | routes/admin.ts premium config/users/transactions/grant/extend/revoke |
| SendNotification | send_broadcasts | routes/broadcasts.ts create/delete/bulk-delete |
| ManagePolls, FeedbackMessages, FlaggedQuestions | moderate_feedback | routes/polls.ts mutations; feedback.ts messages/posts/replies; reports.ts admin |
| AdminAnalytics, ActivityLog, ApiUsage, SystemMonitor | view_analytics_logs | admin.ts analytics/api-usage; reports.ts audit; monitoring.ts whole router |
| AppConfig | manage_config_maintenance | routes/admin.ts config PUT |

These are **21 destination screens plus the dashboard**, with 23 existing gate
calls including ManageUsers resume. All existing gate calls are updated, not only
visible navigation. Extra compound actions are isolated: ManageQuestions hides
exam image/rename/delete controls without manage_exams; SystemMonitor only offers
and accepts generation retry with manage_generated_tests in addition to its parent
view_analytics_logs right (matching monitoring.ts:9,67). Existing QR stays free.
No EVE premium entitlement, gift inventory or payment behavior is introduced.

## Authentication/source boundary

The actual request chain remains Firebase token -> ApiClient/EveApiService ->
backend/src/index.ts:266 authMiddleware -> authMiddleware.ts:40-44 verified
Firebase identity and D1 membership -> requireAdmin/requirePermission at the routes.
admin.ts:20-36 authenticates /admin/me and returns the current server permissions.
Every privileged handler independently checks its right on the Worker. The client
checks above improve UI correctness, not backend authorization or live role changes.
Existing server tests and data are unchanged; no production credentials are used.

Telegram has no EVE exam-admin or Firebase/Worker permission service equivalent:
that business/auth subsystem is NOT FOUND upstream, implemented as an **ADAPTED
EVE integration**, with no false EXACT claim. The unchanged source UI remains
Components/PagerSlidingTabStrip.java:249-250 at official
DrKLO/Telegram@3de37c8cfb039452be7398909461da6288babf15 ->
app/src/main/java/com/eve/app/telegram/components/PagerSlidingTabStrip.java:249 and
ui/admin/AdminDashboardPager.kt:32,84 (ADAPTED package/theme/EVE section host).
Original 350ms quint indicator and actual bound row callbacks remain unchanged.
The existing source map, notices and GPL release gate are preserved.

## Verification and remaining work

AdminPermissionSnapshotTest covers every pair of 13 granted/requested rights,
server-owner behavior, real Gson null/malformed metadata, foreign-account metadata,
loading before callback, cancellation/failure, rejected membership and actual
repository missing-data behavior through an offline EveApiService fixture.
All 109 scoped Admin/policy/identity JVM tests pass; AndroidTest compilation and
lint pass. Two older source assertions needed updating for the confirmed callback
variable and added retry-right predicate; all existing server-confirmation,
anti-stacking, cancellation and email-allowlist assertions remain. The test log was
polluted by overlapping old/new command output; the final serial, uniquely named broad
run passes: 740 tests / 170 suites, zero failures/errors/skips; AndroidTest compilation,
lint (zero errors / 1645 warnings) and x86_64 APK assembly pass (53s). No gate/assertion is removed.

Still NOT MET: complete per-action valid/invalid/loading/failure/success and
rotation/concurrent signed-in native UI matrix; every remaining source row/dialog/
button/navigation/scroll/motion primitive; actual device Poll label visibility;
original/EVE capture parity and all app-wide acceptance requirements. The host tests
do not establish any physical-device behavior, live role/production parity, legal
clearance or mission-final acceptance. Exact-SHA FAST is required for this batch.

## Live control-to-Worker reconciliation — 2026-11-03 continuation

Base `93e2f87717a630bfad6404de5a2a5060842e5cd8`, FAST41646834423 SUCCESS.
This is an actual production caller inventory, not a claim that each action passed
all runtime scenarios. The canonical ownership registry remains
PRODUCTION_OWNERSHIP.md. All listed business behavior is EVE ADAPTED integration;
Telegram supplies existing visual primitives, not exam/admin backend behavior.

Every group still needs a completed per-action device matrix: authorized success,
denied/revoked rights, account change, duplicates, delayed/malformed/network replies,
recreation, cancellation and exact acknowledgment. Existing positive/negative host
and Worker tests below cover named contracts only. No destructive production test.

| Real Hub control and child actions | Activity / state owner → repository → API | Worker owner / permission | Current evidence and remaining finding |
| --- | --- | --- | --- |
| Exams → Manage Existing: refresh, search, expand/collapse, row/edit, preview, Delete/Unpublish | ManageExistingExamsActivity / ExamTree / AdminDeleteFlow → ExamRepository → getExams/getExamAnalytics/examDeletionInfo/deleteExam/unpublishExamTests | exams.ts / generatedTests.ts; manage_exams; analytics additionally view_analytics_logs | Existing hierarchy and dependency deletion tests preserved. Optional analytics errors currently become zero attempts; Activity/read/delete lifetime needs actual regression, not assumed accepted. |
| Exams → Manage Hierarchy: main/subexam selection, create, save, discard/cancel, auto-generation switch/time, duration/count/negative marking/prompt/publish controls | ManageExamsActivity → ExamRepository.addExam/updateExamFullSettings → createExam/updateExam | exams.ts; manage_exams | Existing hierarchy/parser/metadata contracts preserved. This separate full-settings editor is not EditExam's metadata-only save. Owner/selection during suspended save must be verified. |
| Hierarchy Generate Now, generated-list refresh, status toggle, preview, Delete/Unpublish | ManageExamsActivity / AdminDeleteFlow → ExamRepository and triggerAiTestGeneration → generated test APIs | generatedTests.ts / services/generation.ts; manage_generated_tests | Source shows success=true with missing questionCount falls back to requested count; fresh compound rights, selected exam, lifecycle and exact acknowledgment require reproduction. No second generation manager is proposed. |
| Questions: exam selection, detail, Add/Edit/Delete, bulk file picker/preview/import, exam image picker, rename, exam delete | ManageQuestionsActivity → canonical question form / AdminViewModel → ExamRepository → existing question/exam APIs | questions.ts manage_questions; exam metadata/image/delete manage_exams | Existing confirmed mutation, parser, picker ownership, image bounds and selected-exam regressions preserved; device provider/recreation matrix remains OPEN. |
| Existing Exam Editor: name/time/category/cutoffs Save, question Add/Edit/Delete, bulk import/preview, Emoji Studio navigation | EditExamActivity → canonical question form / own repository → updateExam/getExam and question APIs | exams.ts manage_exams; questions.ts manage_questions; emoji.ts manage_content | Confirmed metadata-only save, cutoff readback and question/emoji rights tests preserved. Delete immediately changes local list and offers UPDATE-based Undo; actual server DELETE+UPDATE cannot restore. Product timing/restore choice remains unanswered; no fake restore introduced. |
| Syllabus: list search, exam dropdown, PDF picker/stepper, upload, preview/open, replace, reassign, remove | ManageSyllabusActivity → ExamRepository.uploadSyllabusPdf/removeSyllabusPdf/updateExamFullSettings → existing exam APIs | exams.ts manage_syllabus for PDF; full settings PUT manage_exams | Upload/replacement/removal owner/PDF/compensation tests and two green commits preserved. Reassign performs two independent full-setting writes and can overwrite unrelated stale metadata; atomic server transfer/idempotency/rollback remains NOT MET and third successful feature integration needs an exception. |
| Home Banner: list, create/edit media/link, enable/disable/delete | AdminActivity dialogs → HomeBannerRepository → existing banner APIs | banners.ts; manage_content | Confirmed Result, fresh owner/right, single-flight and 5-MiB input tests preserved; physical provider/role matrix OPEN. |
| Floating Link: load, edit position/URL, save, clear/cancel | AdminActivity → FloatingLinkRepository → get/update floating link | admin.ts /floating-link; manage_content | Existing server-readback/acknowledgment and owner-bound dialog repair preserved. |
| Animated Emoji: search/tabs, preview/details, import configured Telegram pack/Noto/custom upload; global/pack enable, pack/item delete, placement slot/target/apply/remove | EmojiStudioActivity → EmojiRepository → existing emoji/pack/import/placement APIs | emoji.ts; manage_content; public read routes remain separate | Existing media/parser/placement/partial-import tests preserved. Account/stop/fresh-right behavior of actual retained sheets/imports requires regression. No new Telegram credential/protocol integration is authorized. |
| Users: refresh/search/page, user detail/attempts, status change | ManageUsersActivity → AdminRepository → users/stats/status/attempts APIs | admin.ts; manage_users | Existing pagination/identity/malformed ack and Worker strict disabled-boolean tests preserved; physical revoked-right/recreation matrix OPEN. |
| Admins and Rights: directory refresh, Add, row/open editor, rights/title Save, Dismiss/Remove | AdminsAndRightsActivity / AdminRightsEditActivity → AdminViewModel → AdminRepository → admin directory/rights APIs | admin.ts; manage_admins_rights | Edit owner/fresh-right/server-confirmed behavior at12a98738 / FAST41638494267 SUCCESS verified. Directory Add lacks synchronous duplicate acquisition/owner binding; directory retained Remove callbacks need separate regression. Existing feature has two successful scoped commits; do not silently integrate a third or weaken confirmations. |
| Premium: config Save/Refresh, settings/transactions/users tabs, grant, lifetime confirmation, extend, revoke | ManagePremiumActivity → EveApiService directly → /admin/premium/config,transactions,users,grant,extend,revoke | admin.ts and existing premium.ts entitlement/payment authority; manage_premium | Numeric/boolean DTO repair74836d34 / FAST41643535083 SUCCESS. Current second/final planned batch adds strict Worker config validation, D1 acknowledgment and actual stop/resume mutation tests. No entitlement/payment policy or signing secret change/deploy. |
| Broadcast: compose/send, list/refresh, select all/cancel selection, single/bulk delete | SendNotificationActivity → EveApiService broadcast APIs | broadcasts.ts; send_broadcasts | Existing one pending write, fresh owner/right, IDs/count and list-order tests preserved; inbox semantics unchanged. |
| Flagged Questions: report-type filters, detail, edit/save, dismiss, exam editor navigation | FlaggedQuestionsActivity → FlaggedQuestionRepository / ExamRepository → reports/questions APIs | reports.ts moderate_feedback; questions.ts manage_questions; editor manage_exams | Existing scoped target identity, canonical form, held-read order and compound-right tests preserved. |
| Feedback Messages: list, mark read, reply/cancel, Delete/Undo | FeedbackMessagesActivity → FeedbackRepository → feedback message/read/reply/delete APIs | feedback.ts; moderate_feedback | Repository failed-list/cancellation and reply result checks preserved. Actual Activity's retained reply/delete callbacks still need owner/stop/fresh-right regression. This is distinct from the already repaired Hub Feedback Posts dialogs. |
| Create/Manage Feedback Posts: create/edit/delete, reply list/read/delete, delete post from replies | AdminActivity → FeedbackRepository → posts/replies APIs | feedback.ts; moderate_feedback | Existing1a8d6fd7 fresh-owner/right/single-flight and server-issued ID repair preserved; no replay. |
| Analytics: range/exam controls, flagged-question edit, console link | AdminAnalyticsActivity / AdminAnalyticsViewModel → AdminAnalyticsRepository / ExamRepository → analytics/questions APIs | admin.ts view_analytics_logs; edits require manage_questions | Existing request-order/selected-filter and truthful missing Crashlytics telemetry tests preserved. Question aggregates have no days parameter; no fabricated range-specific statistics. |
| Recent Actions: range tabs, search/collapse, paging, admin/type filters, row detail/close | ActivityLogActivity → AuditLogRepository / AdminRepository → audit logs/types/admin directory | reports.ts audit routes view_analytics_logs; admin directory additionally manage_admins_rights | Source shows filter reload clears rows while isLoading can drop the replacement request, then old response publishes. Failed list also becomes empty. Reproduce before repair; directory permission must not be bypassed to populate a filter. |
| System Monitor: refresh, next page, failed-job retry/cancel | SystemMonitorActivity → EveApiService system reads/retry → monitoring.ts / services/generation.ts | view_analytics_logs AND manage_generated_tests for retry | 93e2f877 / FAST41646834423 SUCCESS, actual Activity18 + source contracts2 and Worker2 host cases pass. Runtime/device acceptance OPEN. |
| API Usage: refresh/current-month counters | ApiUsageActivity → ApiUsageRepository → getApiUsage | admin.ts /api-usage → usageTracker.ts; view_analytics_logs | Prepared repair rejects denied/missing/contradictory/malformed responses and failed Worker aggregates; genuine zero is valid. Actual Activity/repository/Worker tests cover owner/stop/recreation/stale responses and fresh Worker rights; existing repository remains the only transport caller. Exact-SHA FAST required after push; physical acceptance OPEN. No invented telemetry or new service. |
| Hub Maintenance switch, App Config, Version Status, AI connection test | AdminActivity / AppConfigActivity → AdminRepository / AppConfigManager / EveApiService → config GET/PUT and ai-health | admin.ts manage_config_maintenance; AI test additionally manage_questions | Existing partial write/readback, owner/current-right, resume and truthful health tests preserved. Home remains maintenance-presentation owner. |
| Export JSON, share/cancel | AdminActivity.exportExamData → existing paged export API → bounded owner cache/FileProvider | admin.ts /export/exams; export_data | Existing complete/matching/unique page sequence, fresh export right/share owner and background file write tests preserved. |
| Edit About / Privacy / Terms / Contact: tabs, fields, Save, unsaved Save/Discard/Cancel, back | EditAboutActivity → AppContentRepository → get/update AppContent | appContent.ts; manage_content for PUT; public GET/defaults separate | Prepared canonical Activity/repository repair separates public fallback from confirmed admin data; fresh UID/email/right/epoch and matching readback protect Save, tabs and restored drafts. Actual Hono regression proves and closes anonymous internal-row reads and manage_content writes into config-owned rows: only supported document namespaces/aliases remain. Legal text unchanged; handler/Activity checks and exact-SHA FAST required; no legal approval/deployment. |

The UI's Back, Cancel, search, filters and previews remain navigation/presentation
controls of these owners; sharing a destination or renderer is not duplication.
No proven production duplicate was removed in this continuation. Host source tests
are not a universal security, no-duplication or runtime correctness certificate.
