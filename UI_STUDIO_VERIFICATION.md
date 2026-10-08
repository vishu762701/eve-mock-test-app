# UI Studio implementation and verification

Repository: `https://github.com/vishu762701/eve-mock-test-app.git`
Branch: `codex/ui-studio-functional`
Current task starting HEAD: `5b678765907604da0d7baed3435c7cba9261a60b`
Fetched upstream main: `585f8ff91628677732da9fbf7a53c3ea618fa7d0`

## Current focused visual continuation — 2026-10-07

Starting HEAD: `5b678765907604da0d7baed3435c7cba9261a60b`. Fetched `origin/main`: `585f8ff91628677732da9fbf7a53c3ea618fa7d0`; main had not advanced and the branch retained the previously pushed Studio implementation. No reset/clean/revert, unrelated business changes or broad redesign.

### Root causes and fixes

- The 30% floor existed in four places (control, Android policy, Worker policy, renderer). All four now permit 0–100%; the editor warns that transparent essential controls retain actions but may be hard to discover.
- Background color/shape/state application replaced native drawables, and icon tint reused border color. Shared `StudioVisual` now keeps native ripple/state structure, applies state-aware tint, uses MaterialButton/Card shape APIs, and independently styles image/compound/button icons. Baseline includes native tint, outlines, clipping, strokes and icon tint, with reset requesting layout when dimensions change. MaterialCard keeps its owned foreground drawable identity: cloning it disconnected the visible stroke from Material helper setters. A device-side border pixel assertion checks actual drawing.
- Glass was FrameLayout-only. `StudioMaterial` retains native views/IDs/layout params and adds a noninteractive backdrop host before the surface foreground. Linear/constraint containers receive an internal host; supported buttons get a sibling underlay. Frame hosts fill the frame; constraint hosts anchor to parent edges; linear hosts use compensating margins to avoid shifting native children. Pre-draw updates dimensions only when bounds change and follows transforms/visibility without screenshot capture or reparenting native controls. Toolbar blur is not offered because inserting a host can disturb its internal layout. Effect state is owned by its target, and reset removes hosts/listeners and stops blur updates.
- Anonymous image/text children lacked IDs. Structural IDs now include unnamed layout paths, normalize RecyclerView template roots and omit Studio/framework plumbing. Matching duplicates are labeled as template edits. Selection accounts for ancestor elevation, depth and transformed visible bounds.
- HEX led the color UI and long labels obscured the tools. The 12 requested categories now offer an HSV picker, current color swatch/HEX, recent/preset colors, reset and fine +/- adjustment. Controls identify screen, element and scope. Selected element is default; selected-screen bulk visual edits are explicit. Global scope routes to the existing opt-in global palette/branding controls, rather than silently applying glass everywhere. Button actions remain under Arrange; Advanced retains JSON, audit, versions and clipboard.

### Focused checks for this continuation

- Final production/debug and instrumentation APKs built successfully. Focused Android JVM `UiStudioTest`: **29 passed**, including Gson round-trip/new-field validation and native visual versus action protection.
- Backend TypeScript check and **85 tests passed**, including new native visual fields, opacity zero, invalid values, SQLite/revision/conflict/publication and fresh field readback.
- Lint still reports the two pre-existing `windowLightNavigationBar` API-27 attributes in day/night themes against minSdk 24. These unrelated theme files were not modified.
- Device verification initially exposed an idle-loop bug in a zero-sized linear glass host; fixed by using stable dimensions and compensating margins with updates only on bounds changes. A subsequent isolated glass scenario passed on API 31.
- The first combined run passed picker/recovery, low-versus-high blur, repeated templates, repository fresh-readback/rendering and pointer selection; its native-card test incorrectly cast Login's actual LinearLayout to MaterialCardView. The fixture now reuses the real Result MaterialCardView within the Login test hierarchy. The combined run ended with an input-timeout/emulator interruption before typography finished; it is not recorded as a complete pass. The cold emulator retry encountered launcher/System UI ANRs before verification; an isolated pointer check subsequently passed, but screenshot capture returned null during the native check and typography again timed out. Non-pixel evidence capture now retries and logs unavailability without skipping reset assertions; glass pixel tests still require real captures.

### Final device results and evidence

**Seven distinct focused scenarios passed across runs** on API 31 (360×800, font scale 1.3, system animation scales zero). System night mode was set, while Eve retained its configured theme as shown in the screenshots; exhaustive day/dark contrast is not claimed. The interrupted combined run is not a complete-suite pass.

- Final APK native check: **passed, 55.526 s**. Real Result MaterialCard in Login hierarchy: red fill, 20% item opacity, radius 24, green border verified by drawn edge pixels; nested green/serif text; actual MaterialButton selected/pressed colors, press scale, retained click listener, original color/radius/opacity/scale and normal drawable reset.
- Final isolated typography/undo/recovery/reopen check: **passed, 90.242 s**.
- Earlier focused runs passed the visual picker → selected native draft at 5% opacity → recreation; real NotificationAdapter template text/anonymous icon tint/reset; API/repository offline/reconnect/401/403/conflicts/fresh-published rendering; nested pointer/zoom/drag/overlap selection. These results are in the retained raw logs; live authenticated publication is not claimed.
- Final APK glass check: **passed, 33.192 s**. Checks compare low/high radius with identical tint/opacity, verify changed backdrop pixels with sharp independent child text, separate item/material alpha, button/linear hosts, border/highlight/depth, and remove hosts/effects on reset. Final screenshots/readout are recorded with the evidence below.

Evidence: [`docs/ui-studio/visual-controls/`](docs/ui-studio/visual-controls/). Native styled/reset screenshots, typography and overlap screenshots are from the final/retry device runs. Raw failed/interrupted logs are retained alongside passing logs so capture failures and emulator timeouts remain visible; screenshots from the first emulator were lost when it exited.

Changed implementation files: model DTOs; `UiStudioActivity`; `StudioPolicy`, `StudioBaseline`, `StudioMaterial`, `StudioRenderer`, `StudioPreview`, `UiStudioEngine`; new `StudioVisual`, `StudioInteraction`, `StudioColorPicker`, `StudioPresets` and keyed tag resource IDs. Changed checks: `StudioFunctionalTest`, `StudioRepositoryFunctionalTest`, new `StudioVisualFunctionalTest`, `UiStudioTest`, Worker `uiStudioPolicy.ts` and `ui_studio.test.cjs`. Continuity/evidence changes: this file, `CODEX_STATE.md`, and the focused evidence directory. No unrelated business files changed.

### Remaining limits for this continuation

- Authenticated production publication/student accounts and deployed Worker behavior were not exercised: no production credentials/deployment were available. Local fresh HTTP readback, disk/cache recovery, real shared renderer and SQLite Worker routes are tested separately.
- API 24–30 blur fallback, physical-device frame/memory performance, physical haptics and animated timing are not verified in this run. API 31 evidence uses disabled system animations; exhaustive rotation/accessibility and every one of the 39 host layouts are not re-tested here.
- Toolbar/internal scroll or adapter hosts, unsupported button parents and tiny image/text targets do not offer glass. Select a supported containing surface instead. Custom Canvas subparts and separately launched production dialogs are not automatically exposed.
- Global scope retains explicit palette/branding controls, not blanket global glass. Selected-screen bulk styles affect compatible currently rendered template views. Constraints still govern native layout.
- Historical border-color-as-icon-tint coupling was removed; icon styling now requires `iconTint`. Existing JSON is preserved. A GradientDrawable has no public original stroke getter; explicit stroke widths replace its border with a foreground edge, while color-only edits preserve the original drawable border.

### Visual versus behavior protection

`visuallyEditable` and `behaviorProtected` are separate policy concepts. Native visual fill, tint, opacity, text/icon style, supported padding/margins/dimensions, shape/depth, states and motion are editable. Native actions, enabled/visibility/structure, authentication/exam behavior and data-bound text remain owned by Eve. No touch/click listener is replaced for interaction styling: pre-draw observes pressed state. Existing draft/save/publish/undo/recovery/version/conflict features remain.

### iOS 26 capability mapping

| Capability | Android implementation | Exact limit |
|---|---|---|
| Clear/frosted/tinted/light/dark/floating material | True BlurView backdrop; theme-aware neutral, independent material/item/tint alpha; manual controls plus seven shortcuts | No Apple APIs, physical refraction or adaptive luminosity |
| Rounded/capsule/circle | Native Material shapes plus clipped rounded outlines; circle requires square size | Android round-rect approximation, not Apple's continuous curvature |
| Specular/depth | Two restrained static gradient edges, separate highlight strength, border and native elevation | No moving optical highlights, lens distortion or fluid morphing |
| Press/selected/disabled/focus | State-aware native fill/ripple, optional scale/haptic observer, configurable tint transitions | Haptics depend on device/settings; no action rewriting |
| Motion | Fade, scale, fade+scale, slide; spring-like overshoot release/entrance; explicit screen entrance | Overshoot approximation, not a physical spring; no shared-element navigation morphing |
| Bars/panels/pills/floating controls | Same material/visual path where supported existing containers/buttons expose real views | Scroll/adapter internals, custom Canvas subparts, separate production dialog windows and unsupported parents are not invented as editable glass surfaces |

Re-read Apple's current adopting-Liquid-Glass documentation JSON for this continuation; its guidance emphasizes restrained use, legibility, accessibility and preserving system controls. Prior HIG reference links remain below.

## Recovery and scope

Continued the interrupted working tree; did not recreate the implementation, reset, clean, revert, or discard it. Before editing, inspected status, staged/unstaged diffs, untracked files, commits, branches and worktrees and fetched origin. The remote base had not advanced. Recovery copies are outside the repository at `/workspace/tooling/interrupted-ui-studio.patch` and `/workspace/tooling/interrupted-ui-studio-untracked.tar.gz`.

Recovered the tool menu, native-layout preview, selection overlay, shared renderer/baseline/material classes, dynamic insertion, draft/publish flow, backend policies and initial instrumentation tests. Continued with compiler fixes, explicit global opt-in, fonts/static branding, adapter fixtures, subtree duplication/reset, input flushing, zoom/preview recovery, timer-color reset, guarded restore/reset/publication, real SQLite tests, and device-discovered layout/idle fixes.

No auth, scoring, attempt submission, theme reveal or floating-airplane business flow was redesigned. Production UI Studio integration uses the existing Worker/D1 configuration store. Preview never starts student/admin activities or changes accounts, attempts, payments or backend business data.

## Property path and capability matrix

All offered fields use `UiStudioActivity` controls → immutable `UiStudioConfig` draft / local session and undo history → Gson DTOs → Android validation and Worker validation → D1 draft and version/publication → fresh readback → `StudioRenderer` / `UiStudioEngine` in production activities. The draft is passed explicitly only to the sandbox; production collects the published flow.

| Tool / fields | Implementation and limits |
|---|---|
| Blur / radius, tint, tint opacity, material opacity | Backdrop `BlurView` is the surface's first child; foreground remains a separate view. API 31+ RenderEffect backend, earlier API RenderScript backend. Frame/card, linear and constraint surfaces plus buttons in supported parents use render-only hosts. Scroll/adapter-owned hosts and tiny text/image children do not offer blur. Radius UI 0–25; stored legacy 26–50 is preserved and rendered at 25. Material opacity affects the backdrop/fill; item opacity affects the whole item. Partial material/item alpha crossfades the blurred layer with the original backdrop, so some sharp backdrop remains visible; use 100% on both for full-strength blur. |
| Colors / background, opacity | Native/dynamic fill preserves drawable/ripple structure via tint. Whole-element opacity is 0–100% for all items; warnings call out essential transparent controls. Material, tint and selected text/image opacity remain separate paths. Reset restores captured native appearance. |
| Text / color, size, family, style, alignment | Select child TextViews. Built-in sans-serif, serif and monospace; no unsupported font URL control. Data-bound business text is preserved. |
| Layout / margin, padding, inserted height | Existing Android layout constraints remain authoritative. Native button theme tint cannot mask an explicitly edited fill/stroke; reset restores native tint. No free positioning control is advertised for constrained native items. Inserted items occupy a scrollable vertical container capped at one third of device height, preserving room for native content. |
| Shape / radius, border, elevation | Rounded outlines, capsule and square-bounded circle, stroke, elevation and layered gradient edge highlights. Highlights and continuous-looking corners approximate optics; no physical refraction. |
| Content / label, image URL, icon | Inserted text/buttons, HTTP(S) images and three bundled icon choices. Only native app-name labels permit static text overrides. Scores/questions/timers/exam titles remain bound to business data. |
| Arrange | Eight inserted types: text/button/image/icon/card/banner/divider/spacer. Stable UUIDs, sibling order, duplicate subtree, valid inserted card/banner parent, cycle rejection, hide/show and subtree removal. Native structure/actions/visibility are protected. Screens without a safe container explain insertion is unavailable. |
| Actions | Inserted actions allow none, eight safe navigation destinations or validated HTTP(S) links. Native exam/auth/navigation actions remain app-owned. Sandbox links show their destination without launching an external app. |
| Motion | Fade/scale/combined/slide entrance and screen entrance, spring-like overshoot curve, observed press/release scale, opt-in haptic and smooth state/material tint transitions. System animation settings are respected; business listeners are preserved. |
| States | Pressed/selected/checked/disabled/focused fill and selected text; optional color transition. Native state/ripple drawables remain intact. Enabled state on native controls remains business-owned. |
| Branding / theme | Selected-screen background by default; explicit opt-in global app-name labels/color and app/text palette. Launcher metadata, adaptive lighting and automatic color contrast are not implemented controls. |
| Presets / restore | Selected-material clear/tinted/frosted/opaque presets; selected item and screen resets; cached-published discard with retained server revision; version restore to draft; undo/redo. JSON/import/export/audit/style clipboard stay in Advanced. |

Opening Studio shows 12 tools. Workspaces retain screen/item across tools and recreation, with nested resource IDs, repeated-template labels, an element list and an overlay rather than published selection borders. Text changes are debounced/flushed on navigation; a slider gesture is one undo checkpoint. Zero blur removes the backdrop child. Baseline reset restores background, foreground, image filter/tint, typography, padding, margins and card properties without restoring fixture/business text.

`StudioScreens` inventories 39 existing production activity layouts. Production adapters are reused with deterministic local fixtures for Home, Test/palette, Result, History, Leaderboard, Bookmarks, Mistakes, Notifications, Syllabus, Performance, admin questions/admin emails, audit, analytics, existing exams, generated tests, exam tests, users, polls, feedback messages, sent broadcasts, flagged questions, premium users/transactions and managed syllabus. Managed syllabus's adapter was extracted for reuse without constructing its activity. Other layouts expose their native controls; this is not a simulated implementation of every activity's network/business lifecycle.

## Apple documentation mapping

Read the current official documentation and its published JSON on 2026-10-07:

- https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass
- https://developer.apple.com/design/human-interface-guidelines/materials

Apple distinguishes Liquid Glass for controls/navigation from standard content materials, recommends sparing use, distinguishes regular/clear variants, and requires attention to reduced transparency/motion, contrast and icon labels. Android's scoped frosted/tinted/clear surfaces, theme-aware opaque accessibility preset, native corner/stroke/elevation and system-scale motion are applicable approximations. Users choose readable foreground colors; there is no claim of Apple's adaptive luminosity/contrast behavior. Apple SwiftUI/UIKit glass APIs, physical refraction, dynamic lighting, moving highlights, fluid morphing, layered app-icon effects and OS-level appearance settings are unsupported here. No unrelated iOS features were added.

## Security, persistence and concurrency

Existing Firebase authentication and Worker admin authorization remain in place. Action URLs/destinations, native protection, inserted types/parents/cycles, field types/ranges and colors are validated. Client `force` cannot bypass stale revision rejection. Draft save uses an atomic SQL comparison against the previously read draft JSON. Publish requires the exact saved revision/fields and conditionally snapshots the same draft inside a D1 transaction. Concurrent publication returns a conflict. Restore/reset also guard expected revisions/active publication and atomic writes; history is retained.

Save results distinguish local/offline, server success, conflict and rejection (including 401/403). Local work is retained on failure. Publication first saves, then publishes, then performs a no-cache/no-store request and compares version, revision, screen fields, branding, theme and schema; cached version equality is insufficient. Failed readback does not replace last-good published state. Existing JSON/history remain stored; unsupported unsafe historical configs require repair before new publication rather than silent rewriting.

## Checks and evidence

The final complete suite passed all seven tests in 242.659 s on API 31, night mode, font scale 1.3, with system animation scales set to zero. It includes additional native button fill/corner/pressed-state/tint-reset assertions, configured parent-card action with non-clickable child text, native opacity protection, and bounded insertion viewport height. Final screenshots and raw output: `docs/ui-studio/final/`.

Six unique instrumentation scenarios passed in light/default font mode (248.882 s), then all six passed on the updated implementation in night mode with font scale 1.3 (234.732 s). They exercise the actual tool menu/workspace, typography control → disk draft → shared renderer, undo/redo, close/reopen/recreation, inserted hierarchy/hide/show/reset, real backdrop pixel change with sharp foreground and zero effect removal, all 39 layouts with production fixtures, and repository offline/reconnect/401/403/conflict/publication/readback/student-cache isolation against the local fake API. Failed intermediate runs were used to find defects, not counted as passing verification.

A seventh focused instrumentation scenario passed (24.071 s): actual injected pointer taps select the precise nested resource ID at 0.75× and 1.2× zoom; a drag does not select; native foregrounds remain unchanged by the overlay. Evidence: `docs/ui-studio/pointer/`. This also verified edit mode consumes taps on non-clickable labels without executing native actions.

Screenshots and raw passing instrumentation output: `docs/ui-studio/day/` and `docs/ui-studio/night/`. `04-material-before.png`, `05-material-blur.png`, and `06-material-zero.png` show real changed backdrop pixels with unchanged sharp foreground. Screenshot capture waits for drawing, rather than treating an added blur child as visual proof. Night/1.3× font screenshots are a smoke check, not exhaustive accessibility or contrast certification.

- Backend TypeScript `npm --prefix backend run build`: passed.
- `NODE_USE_ENV_PROXY=1 npm --prefix backend test`: 84 tests passed, zero failed. Includes real in-memory SQLite execution of migration/route SQL, field round trips, draft isolation, invalid actions/protection/parentage/cycles, concurrent saves and stale restore/reset/publication. One existing test also reads the public live app-config endpoint; this does not establish authenticated UI Studio production publication.
- Android `assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest`: passed; 226 JVM tests, zero failures/errors, including 28 UI Studio tests and existing timer/result/theme/floating-link regression contracts.
- `lintDebug`: fails on the two pre-existing `NewApi` errors at `values/themes.xml:14` and `values-night/themes.xml:14` (`windowLightNavigationBar` API 27 versus minSdk 24). Confirmed both attributes exist at the starting upstream commit. Scoped lint defects were repaired. Existing warnings remain.
- Device: software-emulated API 31 x86_64, 360×800 pixels, density 160. No `/dev/kvm`; initial emulator/system UI timeout and screenshot failure occurred. Functional checks subsequently found/fixed the workspace LayoutParams crash and unchanged-label layout loop. Structural blur assertion initially passed with identical screenshots; the final test requires changed backdrop pixels and laid-out dimensions. A later memory-pressure run killed the emulator and a JVM test worker timed out; after stopping idle build daemons, APK/JVM checks completed. Cold-boot Launcher/System UI ANR dialogs then blocked a pointer injection; that run is not counted as passing. The final rerun uses no concurrent build daemons.

Reproduce with the installed Gradle/JDK/SDK (or equivalent local tooling):

```sh
npm --prefix backend install --no-package-lock
npm --prefix backend run build
NODE_USE_ENV_PROXY=1 npm --prefix backend test
./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.eve.app.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.eve.app/files/studio-evidence
```

Repository instrumentation tests label their fake API explicitly; they are not production authentication tests. Backend tests run actual route SQL with a local D1-compatible SQLite wrapper and authorization fixtures, not Cloudflare production D1.

## Verification limits

No authenticated production admin/student credentials were available. Real remote UI Studio publication, a separate authenticated student account, production D1 deployment, real exam submission/reattempt, timer operation against a live attempt, result isolation with real accounts and airplane/reveal interaction remain unverified end-to-end. No Worker deployment is requested by this implementation commit. API 24–30 blur fallback, physical-device GPU/performance, TalkBack, exhaustive keyboard/font-scale/theme combinations and every adapter interaction still require device checks. Source/unit regression checks are not presented as those device checks. Optical behavior unsupported above is not claimed.

## Files changed

```text
CODEX_STATE.md
UI_STUDIO_VERIFICATION.md
app/build.gradle.kts
app/src/androidTest/java/com/eve/app/uistudio/StudioFunctionalTest.kt
app/src/androidTest/java/com/eve/app/uistudio/StudioRepositoryFunctionalTest.kt
app/src/main/java/com/eve/app/data/model/uistudio/UiStudioModels.kt
app/src/main/java/com/eve/app/data/remote/EveApiService.kt
app/src/main/java/com/eve/app/data/repository/UiStudioRepository.kt
app/src/main/java/com/eve/app/ui/admin/AdminActivity.kt
app/src/main/java/com/eve/app/ui/admin/AdminSyllabusAdapter.kt
app/src/main/java/com/eve/app/ui/admin/ManageSyllabusActivity.kt
app/src/main/java/com/eve/app/ui/admin/uistudio/UiStudioActivity.kt
app/src/main/java/com/eve/app/ui/common/CircularTimerView.kt
app/src/main/java/com/eve/app/ui/common/EveBaseActivity.kt
app/src/main/java/com/eve/app/ui/home/MainActivity.kt
app/src/main/java/com/eve/app/ui/login/LoginActivity.kt
app/src/main/java/com/eve/app/ui/notifications/NotificationsActivity.kt
app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt
app/src/main/java/com/eve/app/ui/result/ResultActivity.kt
app/src/main/java/com/eve/app/ui/syllabus/SyllabusActivity.kt
app/src/main/java/com/eve/app/ui/test/QuestionAdapter.kt
app/src/main/java/com/eve/app/ui/test/TestActivity.kt
app/src/main/java/com/eve/app/uistudio/StudioBaseline.kt
app/src/main/java/com/eve/app/uistudio/StudioMaterial.kt
app/src/main/java/com/eve/app/uistudio/StudioPolicy.kt
app/src/main/java/com/eve/app/uistudio/StudioPreview.kt
app/src/main/java/com/eve/app/uistudio/StudioRenderer.kt
app/src/main/java/com/eve/app/uistudio/StudioScreens.kt
app/src/main/java/com/eve/app/util/UiStudioEngine.kt
app/src/main/res/layout/activity_ui_studio.xml
backend/src/routes/uiStudio.ts
backend/src/routes/uiStudioPolicy.ts
backend/test/ui_studio.test.cjs
```

Screenshot and raw instrumentation evidence files are under `docs/ui-studio/day/`, `docs/ui-studio/night/`, `docs/ui-studio/pointer/` and `docs/ui-studio/final/`. No build artifacts, SDK/tooling downloads, credentials, lockfile or unrelated source files are included.

## 2026-10-08 — Root-cause repair (current work)

This section records the focused repair starting at `8cff56f1fa99303f20790ce6b5f223c1d6333b41` on `main`, remote `https://github.com/vishu762701/eve-mock-test-app.git`. The workspace was initially empty; the fresh clone had a clean working tree and matched fetched `origin/main`. No applicable `AGENTS.md` was found in the workspace ancestry or repository. Existing implementation, history and earlier evidence above were preserved. No Git reset, clean, forced update, architecture migration or student-screen redesign was used.

**Completion status: authenticated production Edit → Save → Publish → real student screen remains UNVERIFIED.** Public production readback was exercised, but no authenticated admin/student credentials are configured. A code fix, local backend result, APK build or version increment does not establish that completion rule.

### Confirmed causes, reproductions and repairs

| Cause / reproduction at the starting code | Exact repair / affected code |
|---|---|
| Home publishes a card background but `ExamAdapter.ExamVH.bind` restores native `cardBackgroundColor`, stroke and `alpha`. `StudioRenderer.fingerprint` omitted all of those MaterialCard fields and alpha. Rebind an already styled row: configuration is unchanged, and fill/alpha-only changes are invisible to the cache check. | Renderer fingerprints include stable alpha, MaterialCard fill/radius/stroke, MaterialButton properties and margins. Adapter rebinding is reconciled before drawing. `StudioRenderer`, `StudioInteraction`, `UiStudioEngine`, `EveBaseActivity`. |
| A fill/opacity setter can invalidate pixels without causing global layout; the base activity only reapplied in global-layout callbacks. | One lifecycle-scoped pre-draw listener plus one published-flow collector for every registered activity. Listener removed when stopped/destroyed. Home's duplicate collector/fetch and seven redundant one-shot integrations removed. No adapter or business listener replaced. |
| Anonymous layout roots with screen background edits went through a special `screen` render and the normal component render. Each replaced the other's applied identity, forcing two renders on every draw/layout despite unchanged styling. | Use the normal element/background/fingerprint path once for anonymous roots. A regression assertion checks that an unchanged screen background retains its fingerprint. `StudioRenderer`, `StudioRepairFunctionalTest`. |
| Home's `panelHomeBanner` background is behind its image pager and full-size matte. Editing that parent cannot color the visible matte; a backdrop inserted as its first child is also behind images. | Name the existing matte `homeBannerMatte`; retain its previous structural ID as an alias. Parent surface fill edits target the matte; glass is inserted above images and below foreground CTA, hides the original matte only while active and restores it on reset. `activity_main.xml`, `StudioVisual`, `StudioMaterial`, `StudioRenderer`. |
| Baseline restoration reset every captured text size, padding and dimension on any later color edit, including values that Studio never overrode. Rebind/autofit a text view, then edit only color: its current native size/spacing reverted. | Record property ownership and restore only the fields previously overridden. Preserve app-owned text, enabled/visibility, unedited autofit and spacing. Remember later native adapter color/icon/text writes beneath the override, so reset uses the current recycled item rather than its first category. Restore relative padding/margins correctly. `StudioBaseline`, `UiStudioEngine`. |
| The public endpoint allowed 60 seconds of caching, including empty configurations. Independently, a fetch started before publication could finish after verified readback and overwrite memory/disk. Refresh failure reloaded disk over current memory; older/envelope-inconsistent versions were accepted. | Published responses (200/304/empty) use `no-store`; existing Android no-cache/no-store request remains. Serialize refresh/publication, reject lower or inconsistent versions and malformed empty envelopes, retain in-memory last-good state on failure. Disk initializes the repository for process restart/offline use. `uiStudio.ts`, `UiStudioRepository`. |
| Save succeeds before Publish/readback fails, but the editor retained the pre-save revision. Retry then conflicts despite its own successful save. Success text could also describe an earlier snapshot when newer edits exist. | Retain the successfully saved revision on failure/retry; preserve newer local edits; show Publishing, Published and verified, Publish failed/readback failed and newer-unsaved states. Preserve status across tool navigation. Verify saved fields against edited fields, and published fields against the original edited snapshot, including `configVersion`. `UiStudioRepository`, `UiStudioActivity`. |
| `reset("draft")` left a stale local draft; `reset("published")` cleared legitimate local editing work. Published reset and published-version restore reported success without verified readback. | Clear only the targeted draft. Preserve draft on published-only reset. Require fresh empty published readback before replacing last-good runtime style. Version restore also requires fresh readback and caches the restored draft/publication only after verification. `UiStudioRepository`. |
| Branding applied brand color after an explicit label text color, overriding the latter. Repeated label assignment could request unnecessary layouts. | Explicit component typography takes precedence. Update static label text only when changed. `StudioRenderer`. |
| `CircularTimerView.onDraw` replaced configured track color, and `setTime`/color animation replaced independently configured text color. Its inherited legacy typography had no paint-size/font path. Legacy `score_card` typography also targeted only the non-text parent. | Respect configured track/text paints through drawing and ticks; text-only edits no longer recolor the independent ring; add the existing typography fields to timer paint and restore defaults on reset. Route legacy timer typography to the timer and score-card typography to its score labels, with child overrides taking precedence. Countdown progress/thresholds/actions unchanged. `CircularTimerView`, `StudioRenderer`, `UiStudioActivity`. |
| Imported/pasted settings can address missing views or request text/icon/blur properties on incompatible targets. Validation allowed blur 26–50 while the portable renderer clamped to 25. Exposed `ease_in`/`ease_out` names fell through to the default interpolator. Capsule radius applied by post could trigger repeated mismatch/reapplication. | Preflight all edited production layouts with adapter fixtures before Publish, checking effective child properties after legacy inheritance rather than incorrectly applying the surface blur to text children; report exact screen/ID/property and compatible child/containing surface. New draft/publication blur range is 0–25, while existing 26–50 published caches remain readable at their legacy capped treatment. Validate supported typography/motion enums; map exposed curves; resolve laid-out capsule radius synchronously. `StudioRenderer`, `UiStudioActivity`, both policies, repository, `StudioVisual`, `UiStudioEngine`. |

The live public endpoint was read on 2026-10-08: HTTP 200, version **19**, `configVersion` 1, status `published`. It includes Home `native_path_rvExams_MaterialCardView_0.appearance.backgroundColor = #FFFFFF`, matching the exact adapter/cache omission above. Its response still had `Cache-Control: public, max-age=60` at inspection. The unauthenticated admin-draft GET returned HTTP 401 (`Missing or malformed Authorization header`). No remote draft/publication was mutated by this repair. Backend deployment subsequently passed on GitHub (details below).

### Screen and property audit

All 39 `StudioScreens` entries have their declared layout and an activity extending `EveBaseActivity`; no missing base integration was found. Shared lifecycle application now owns their initial/resumed configuration and dynamic/repeated-view reconciliation. Static inventory is not proof of every screen's authenticated business lifecycle. Native IDs, anonymous structural IDs, legacy aliases and repeated-template IDs are resolved against actual views; unknown/incompatible edited targets block editor publication with an explanation.

| Property | Repair status and practical limits |
|---|---|
| Background/fill, whole opacity | Card/native setter and adapter overwrite detection repaired; Home's visible matte routed correctly. Whole opacity applies to the complete element. An opaque foreground child can still cover its parent fill; select that visible child surface. |
| Blur, glass, material opacity, tint/tint opacity | Existing backdrop renderer preserved; Home's layer order repaired. Blur supported on the existing safe container/button hosts, 0–25. Foreground remains separate and sharp. Text/image/scroll/adapter hosts that cannot host it safely receive a containing-surface explanation. Default tint opacity now agrees with the editor's 20% display; tint-opacity-only edits use a theme-neutral tint. |
| Text color/size/font/style/alignment | Independent TextView colors retained through rebinding; unedited autofit preserved; branding precedence repaired. Timer supports color/size/font/style; centered single-line timer alignment is fixed and unsupported requests explained. Legacy score labels inherit score-card typography. |
| Icon tint | Existing image/button/compound-icon path retained and native overwrites fingerprinted. Incompatible targets fail preflight. |
| Border/corners/shadows | Native card/button border/radius fingerprinted, capsule updates avoid posted-radius loops; existing outline/elevation rendering retained. |
| Size/spacing | Existing constrained layout path retained; relative reset and unedited spacing preservation repaired. Android constraints remain authoritative. |
| States | Native enabled/actions protected; existing selected/pressed/focused/disabled fill and selected text retained. State-transition colors are distinguished from adapter overwrites. |
| Motion | Existing entrance/press/state animations retained; enabled entrance changes and exposed response curves now reach rendering. System animation scale respected; no new animation feature. |
| Visibility/layout | Existing native business visibility/actions/structure remain app-owned; inserted visibility/layout unchanged. Anonymous containers are selectable for safe visual edits. |

### Verification for this repair

Focused coverage added in `UiStudioPublicationTest` (repository ordering, stale/malformed/offline readback, field/version verification and unsupported radius), `StudioRepairFunctionalTest` (Home card/banners, actual adapter rebind, pixels/opacity, production Notifications lifecycle/recreation, timer paints and unsupported targets), existing `StudioRepositoryFunctionalTest` (restart/offline and reset draft isolation), and backend UI Studio tests (real SQLite routes, non-cacheable readback, supported ranges). Existing glass/pixel/editor/protected-action regression suites are retained.

Executed for this repair (historical evidence above is not substituted):

- Backend `npm run build` and `npm test`: **PASS**, 87 tests, 0 failures, including route storage/validation, auth guards, publication readback and protected business properties.
- Android Gradle 8.7 / JDK 17 / API 36: `assembleDebug testDebugUnitTest assembleDebugAndroidTest`: **PASS**. 232 JVM tests in 28 suites, 0 failures/errors; APK and instrumentation APK built. Five new repository regression cases execute locally.
- `lintDebug`: **FAIL**, 2 existing NewApi errors (`android:windowLightNavigationBar` in `values/themes.xml:14` and `values-night/themes.xml:14`, minSdk 24/API 27 attribute), plus 1607 warnings. Both error lines exist unchanged at the starting commit. No theme behavior change made to hide this unrelated result.
- Production public readback: **PASS for read-only availability and existing configuration inspection**; not proof of a new publication.
- Authenticated production Save Draft / Publish: **UNVERIFIED**. User authorized `own.keni@gmail.com`, but the environment has no authenticated Eve session, account connection or credential binding. Email alone cannot authenticate Firebase/admin API. No authentication bypass or remote publication attempted.

### Account-holder verification steps (required for production completion)

1. Install the APK from this repair's successful GitHub Android workflow. Keep the existing Eve account/data/theme settings. Sign in with the authorized admin account and open Admin → UI Studio.
2. Choose Home. Use Elements to select the repeated exam card (`native_path_rvExams_MaterialCardView_0`). In Colors set a distinctive card background and whole opacity 35%. Select its child exam title (`native_tvExamName`) and set a different text color. Confirm each change immediately in preview.
3. Select the Home banner (`native_panelHomeBanner`) with a real image backdrop. Use Glass & Materials to set blur 22, material opacity 60%, a tint and tint opacity 20%. Add radius 16 and border 2 with a distinct color. Confirm backdrop softens while foreground text stays sharp. A text/icon target should explain the unsupported property and guide selection of a containing surface.
4. Save Draft. Close and reopen Studio. Confirm every value remains and the normal student screen still shows the earlier publication.
5. Publish. Wait for **Published and verified**; any auth/conflict/network/readback failure must remain a failure. Read `/api/ui-studio/published` freshly and compare the exact edited fields/IDs to the draft, not merely its version.
6. Open the real Home screen. Scroll away/back to rebind cards, refresh normal data, then close/restart Eve. Capture the card/title/banner pixels after each step. Confirm the visible styling matches the fresh publication.
7. Repeat title/card edits on Notifications with repeated rows. Check recreation and normal data updates. Repeat the relevant visual checks in Day and Night using Eve's existing theme selection.
8. Check a safe test attempt: timer text/track stays styled through ticks; Next, Previous, Mark Review, Clear and Submit retain their original behavior. Do not change scoring/attempt logic.
9. Reset the intended target; confirm correct native baseline, fresh reset readback and retention of unrelated local draft work. Restore the desired publication afterwards. Record screenshots, fresh public fields/version, theme and APK commit.

### Deployed backend / GitHub evidence

- Repair code commit `46548a280bc5489c344c01e6a44ea9c49eba7e95` was pushed normally to `origin/main` after fetching and reviewing the scoped diff. No newer upstream changes existed.
- [Build Eve APK](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37716794500): **SUCCESS**, downloadable `Eve-debug-apk` artifact.
- [Deploy Eve Worker](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37716794801): **SUCCESS**, TypeScript, backend tests, D1 migration step, Worker deployment and existing production smoke checks passed. Those smoke checks are not authenticated UI Studio publication verification.
- Fresh public readback after deployment (2026-10-08 02:14:55 UTC): **HTTP 200, `Cache-Control: no-store`, version 21, `configVersion` 1, status `published`**. Newer configuration was preserved; this repair did not write versions 20/21. Version 21 still includes the exact Home repeated-card ID, and a green `native_contentSheet` background. These fields were inspected; their production pixels were not verified from an authenticated session.

### Device and account-holder results for this repair

- User installed the linked repair APK and replied **“yes color change works”** when asked about publishing a Home background change and reopening/restarting Eve. This is recorded as **account-holder-reported color success**, not independent screenshot/pixel verification. The reply did not provide a publish status/version, edited ID/fields, separate Day/Night results, opacity or blur evidence.
- The environment had no physical device or `/dev/kvm`. An API 31 Google APIs x86_64 software emulator was downloaded and booted (360×800, density 160). Initial Android boot suffered a system-server watchdog restart. Lab recovery increased the emulator timeout multiplier, completed lab provisioning and disabled its package verifier; none of these settings changes modify the repository/app or server validation.
- APK/test-APK installations eventually succeeded, but were slow/unstable. First Day instrumentation failed with `INSTRUMENTATION_FAILED` before the test package registered. The next runner was interrupted by APK replacement (`shortMsg=Process crashed`), yielding no test result. It is not counted as a test pass or a confirmed Eve source crash.
- Final-source build after the additional anonymous-root, preflight and independent timer fixes: `assembleDebug testDebugUnitTest assembleDebugAndroidTest` **PASS** (1m 58s); **232 JVM tests, 0 failures/errors**. The final clean device retry timed out during APK installation after 180 seconds and did not reach the instrumentation suite.
- New Android regression cases are **compiled, execution UNVERIFIED** for this repair. No new Eve app screenshots/pixel assertions were obtained. The available emulator screenshot showed its launcher/first-boot overlay and is not presented as Eve evidence. Historical screenshots earlier in this document are not reused as proof of these changes.
- Day visual/pixel verification: **UNVERIFIED**. Night visual/pixel verification: **UNVERIFIED**. Blur radius pixels, sharp foreground, opacity/tint/material combinations, authenticated production Notifications/restart and protected live exam actions remain pending. API 24–30 blur fallback and enabled-animation device checks also remain unverified.

**Remaining completion blocker:** an authenticated production Edit → Save → Publish with exact fresh edited-field readback and corresponding real-screen pixels, including Day/Night, blur/opacity and restart persistence. Only public readback, deployed cache headers, local backend/JVM tests and the user's reported color success have been established. The account-holder checklist above is the next concrete verification path; do not label the whole Studio repair fully verified.
