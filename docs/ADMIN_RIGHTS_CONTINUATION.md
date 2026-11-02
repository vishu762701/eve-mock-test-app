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
