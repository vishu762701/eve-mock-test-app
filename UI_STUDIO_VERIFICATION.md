# UI Studio implementation and verification

Repository: `https://github.com/vishu762701/eve-mock-test-app.git`
Branch: `codex/ui-studio-functional`
Starting HEAD / fetched base: `585f8ff91628677732da9fbf7a53c3ea618fa7d0`

## Recovery and scope

Continued the interrupted working tree; did not recreate the implementation, reset, clean, revert, or discard it. Before editing, inspected status, staged/unstaged diffs, untracked files, commits, branches and worktrees and fetched origin. The remote base had not advanced. Recovery copies are outside the repository at `/workspace/tooling/interrupted-ui-studio.patch` and `/workspace/tooling/interrupted-ui-studio-untracked.tar.gz`.

Recovered the tool menu, native-layout preview, selection overlay, shared renderer/baseline/material classes, dynamic insertion, draft/publish flow, backend policies and initial instrumentation tests. Continued with compiler fixes, explicit global opt-in, fonts/static branding, adapter fixtures, subtree duplication/reset, input flushing, zoom/preview recovery, timer-color reset, guarded restore/reset/publication, real SQLite tests, and device-discovered layout/idle fixes.

No auth, scoring, attempt submission, theme reveal or floating-airplane business flow was redesigned. Production UI Studio integration uses the existing Worker/D1 configuration store. Preview never starts student/admin activities or changes accounts, attempts, payments or backend business data.

## Property path and capability matrix

All offered fields use `UiStudioActivity` controls → immutable `UiStudioConfig` draft / local session and undo history → Gson DTOs → Android validation and Worker validation → D1 draft and version/publication → fresh readback → `StudioRenderer` / `UiStudioEngine` in production activities. The draft is passed explicitly only to the sandbox; production collects the published flow.

| Tool / fields | Implementation and limits |
|---|---|
| Blur / radius, tint, tint opacity, material opacity | Backdrop `BlurView` is the surface's first child; foreground remains a separate view. API 31+ RenderEffect backend, earlier API RenderScript backend. Only compatible card/frame containers offer blur. Other targets explicitly offer flat tint only. Radius UI 0–25; stored legacy 26–50 is preserved and rendered at 25. Material opacity affects the backdrop/fill; item opacity affects the whole item. |
| Colors / background, opacity | Native/dynamic item fill; screen background is explicitly separate. Native opacity has a 30% readability floor in controls, both validators and the renderer. Reset restores the captured native drawable. |
| Text / color, size, family, style, alignment | Select child TextViews. Built-in sans-serif, serif and monospace; no unsupported font URL control. Data-bound business text is preserved. |
| Layout / margin, padding, inserted height | Existing Android layout constraints remain authoritative. Native button theme tint cannot mask an explicitly edited fill/stroke; reset restores native tint. No free positioning control is advertised for constrained native items. Inserted items occupy a scrollable vertical container capped at one third of device height, preserving room for native content. |
| Shape / radius, border, elevation | Android corners, stroke and elevation; blurred frame edge uses a foreground overlay that resets with baseline. This is a static highlight approximation. |
| Content / label, image URL, icon | Inserted text/buttons, HTTP(S) images and three bundled icon choices. Only native app-name labels permit static text overrides. Scores/questions/timers/exam titles remain bound to business data. |
| Arrange | Eight inserted types: text/button/image/icon/card/banner/divider/spacer. Stable UUIDs, sibling order, duplicate subtree, valid inserted card/banner parent, cycle rejection, hide/show and subtree removal. Native structure/actions/visibility are protected. Screens without a safe container explain insertion is unavailable. |
| Actions | Inserted actions allow none, eight safe navigation destinations or validated HTTP(S) links. Native exam/auth/navigation actions remain app-owned. Sandbox links show their destination without launching an external app. |
| Motion | Configurable item entrance and explicit selected-screen entrance; replay, duration and reset. Android animation scale is respected. No optical morphing or refraction is advertised. |
| States | Pressed/selected/disabled background and selected text state lists. Inserted enabled state is configurable; native enabled state is business-owned. |
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
