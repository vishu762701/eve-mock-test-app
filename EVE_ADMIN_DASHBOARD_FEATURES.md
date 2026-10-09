# Eve admin dashboard features

The existing native Admin Dashboard now has a **System Health & Operations** entry. It reuses native Eve colors and Material controls and leaves Home/Test/Result design intact. `SystemMonitorActivity` is not exported; actual authorization is enforced by the backend, independently of Android navigation.

| Feature | Actual backend data source | Supported actions / limits |
| --- | --- | --- |
| Overview | `GET /api/admin/system/overview`: SQL COUNT subqueries over D1 users, exams, published generated_tests, question bank plus stored generated question counts, attempts | Refresh totals and recent20 submissions. Label is registered profiles, not Firebase Auth account count. No collection downloads for totals. |
| Generation monitor | `GET /api/admin/system/operations`: generation_jobs | Show exam, source, requested/committed/failed counts, status, start/completion, safe category, job ID. Successful count means committed questions; provider partial output is discarded. |
| Generation retry | `POST /api/admin/system/jobs/:id/retry` | Confirmation, only failed retryable jobs, current exam settings and original count; stable retry request key and shared lease prevent double-tap duplicate publication. Refresh afterward; DB/provider failure surfaces safe message. Existing generation rate limiter also covers retry. |
| Submission monitor | operations/overview SELECT from actual attempts | Recent confirmed result ID, exam, time and counted eligibility. Successful submissions have committed backend records. No student token, UID or answer data. |
| Failed operations | operation_events; X-Request-ID on monitored requests | Operation, time, safe category, HTTP failure status, correlation ID and retryable flag. Twenty-five rows per collection per page, bounded offset, next page and refresh. Student submission failures are informational; dashboard cannot replay private student answers. |
| System health | `GET /api/admin/system/health` | Worker request reachability verified; D1 real query/schema check; Firebase Auth public signing-key fetch with timeout; AI/media configured/unconfigured; Firestore/FCM probe unavailable. Distinguishes verified, configured, degraded, unavailable and unconfigured. Existing AI connection test supplies explicit provider check. No secret values returned. |
| Test validation / management | Existing generated-test list/detail/status routes | Status and validation errors, detail-backed Preview, invalid publish blocked both UI/backend; publish whitelist, unpublish retained. Attempts/snapshots survive edits/unpublish; deletion refused for tests/exams referenced by attempts or active sessions. |

## Authorization and privacy

Every new monitoring/retry route runs server-side `requireAdmin` following signed Firebase token verification and D1 admin lookup. Email allowlists require a verified email claim; students get403 and missing/invalid token gets401. Disabled profiles are rejected. Health is an admin diagnostic, not a public privileged configuration endpoint.

The new tables store safe categories and operation identifiers, never passwords, bearer tokens, API keys, submitted answers or unnecessary student identity. Confirmed submission monitor reads only nonprivate result metadata. Thirty-day monitoring retention runs in the existing Worker cron; student history is not purged. Failed D1 logging never changes submission responses. No dashboard failure entry is fabricated for a request that never reached the backend.

## Loading, errors and empty states

Refresh and pagination use a single loading guard with cancellation cleanup; retry buttons are disabled while processing. Empty generation/submission/failure lists say no recorded activity; API failures expose a useful message and functional Retry. Destructive existing management actions retain confirmation. AI configured is explicitly not reported as verified provider health; Firebase key availability does not claim Firestore or messaging health.

## Deployment

Forward D1 migrations0013–0016 must precede Worker rollout. Deploy through the existing repository main workflow; install the updated Android build to access the new screen. Firebase rule/callable changes are separate and require authorized Firebase deployment. Tests exercise actual overview, monitor, retry, pagination, privacy and degraded-health handlers; live authenticated dashboard and device smoke checks remain release verification requirements.

## 2026-10-09 deletion reliability update
All three exam/generated-test management entry points share `AdminDeleteFlow`.
New admin-only GET `/api/exams/:id/deletion-info` and
`/api/generated-tests/:id/deletion-info` return aggregate dependency counts and
structured blocking reasons without student identifiers. Eligible records use
an irreversible confirmation and server-confirmed deletion; Undo never recreates
an exam. Failed operations keep the original list and offer safe request-ID copy.
For protected tests, the existing paused status is the Unpublish action.
For exams, POST `/api/exams/:id/unpublish-tests` pauses their generated tests and
disables automatic generation, preserving attempts, active/paused sessions and
question bank records. This is unpublishing, not a new archive feature; a later
manual generation can create another test. Both operations require server-side
admin authorization. No retention policy or student-history deletion is added.

Banner, syllabus, poll and feedback-post deletes now use the guarded confirmation
or existing confirmed-action variant. Repository Result failures are unwrapped,
with honest failure feedback rather than silently refreshing as if successful.
Media cleanup follows D1 confirmation; incomplete cleanup returns a visible
notice and a safe `media_cleanup` diagnostic entry in existing System Monitor,
with30-day retention. It has no automatic Retry because no safe cleanup payload
is retained. No credentials, media paths or student answers are logged.
