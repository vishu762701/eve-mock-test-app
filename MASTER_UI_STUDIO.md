# EVE UI Studio: simple, functional visual editor

## Authorization, latest source, and delivery
The user authorizes this implementation and committing/pushing its scoped changes when complete. Work in the connected repository, expected https://github.com/vishu762701/eve-mock-test-app. Verify the actual git remote and branch; never assume the ZIP is current. Read applicable AGENTS.md files. Fetch origin, inspect status and remote HEAD, and start from the latest appropriate upstream revision in an isolated branch/worktree if necessary. Preserve existing uncommitted work. Do not reset, force-push, overwrite remote changes, or commit unrelated files. Before final push, fetch again and reconcile upstream changes safely. Push the completed task branch; use the original branch only when the environment explicitly supports that workflow and branch policy permits it. Report remote URL, branch, commit SHA, push result, and GitHub Actions results. If authentication or branch policy blocks pushing, state the exact blocker; never claim a push happened. No additional confirmation is needed for this authorized scoped commit/push.

## Scope and evidence
Modify only UI Studio and the runtime/config/backend integration necessary to make its edits work. Do not execute old root task documents as new requirements or redesign unrelated app features. Inspect CODEX_STATE.md, PROGRESS.md, MASTER_TASK_APPLE.md, and EVE_CODEX_FULL_IOS_REDESIGN.md for context only. Check whether MASTER_TASK.md and ADDENDUM_1.md exist in the latest checkout and verify any relevant completed work before redoing it.

The uploaded snapshot (archive revision f017eb0cbf421835064de8a4fc478f0c139b26ae) establishes these investigation starting points; re-verify against latest source:
- app/src/main/java/com/eve/app/ui/admin/uistudio/UiStudioActivity.kt: about 2,946 lines, 12 inspector tabs. renderRealScreenCanvas builds separate preview components through createRealPreviewForComponent and build*Preview functions. Interact mode currently animates and displays a bulletin rather than executing representative app actions.
- app/src/main/res/layout/activity_ui_studio.xml: crowded toolbar, several workspace modes and numerous editing controls.
- app/src/main/java/com/eve/app/util/UiStudioEngine.kt: applyToView applies layout, appearance, tint, typography and animations but does not consume material.blurRadius or material.materialOpacity.
- app/src/main/java/com/eve/app/ui/home/MainActivity.kt: applyUiStudioConfig applies a small fixed set of component IDs; generic component insertion is not established there.
- app/src/main/java/com/eve/app/data/model/uistudio/UiStudioModels.kt: existing schema contains layout, appearance, material, typography, content, actions, animation, states, parentId, order and protected components.
- app/src/main/java/com/eve/app/uistudio/UiStudioRegistry.kt: registry/default templates.
- app/src/main/java/com/eve/app/data/repository/UiStudioRepository.kt: local draft/session cache, revision conflict flow, published configuration and publish readback.
- backend/src/routes/uiStudio.ts: Worker/D1 draft, published, versions and audit routes with admin authorization. Check EveApiService.kt, backend types/migrations and authentication middleware. Do not introduce a parallel Firestore configuration store.

First trace each editable property from control -> draft -> serialization -> validation -> backend -> published config -> real screen renderer. Produce a capability matrix identifying working, broken, unsupported and newly implemented properties. Repair actual gaps, not just the editor's appearance.

## Required user experience
Opening Admin Dashboard -> UI Studio must show a simple vertical tool list or clear cards, not the full inspector. Tools:
1. Blur & Glass
2. Colors & Backgrounds
3. Text & Fonts
4. Size, Spacing & Position
5. Corners, Borders & Shadows
6. Icons, Images & Content
7. Add, Remove & Arrange
8. Buttons & Actions
9. Animations & Transitions
10. Component States
11. Branding & Theme
12. Presets & Restore
Keep technical JSON/import/export, audit and advanced options in a secondary Advanced menu. Preserve existing useful features while progressively disclosing complexity.

Each tool opens a dedicated workspace: screen selector, a large faithful live preview, current selected item's name/highlight, and only controls relevant to that tool. Example: Blur & Glass exposes blur/material settings only; it must not silently change text, actions or layout. Prefer a collapsible bottom sheet for controls on phones. Keep selected screen/item while switching tools. Use clear English labels, sliders with displayed values, color picker plus HEX entry, sensible ranges and visible reset. Global/screen/item scope must be explicit, defaulting to the selected item. Never silently apply changes globally.

Tap a visible preview item to select its precise stable component ID, including nested elements. Distinguish selection gestures from scrolling; coordinate mapping must remain correct during zoom, pan and orientation changes. Provide an element list for overlapping, hidden or off-screen items, breadcrumbs for nested selection, and clear deselection. Use an editor overlay for selection so outlines never alter or publish app styles. Edit mode selects; Preview mode uses sandboxed representative actions without submitting attempts, changing accounts or triggering destructive operations.

## Faithful rendering and runtime coverage
Replace divergent hand-built screen mockups with the actual screen layouts/shared rendering and binding used by the live app. Reuse production adapters/custom views with deterministic realistic preview data. Factor shared code where required rather than duplicating entire activities or launching student activities with side effects. A screenshot-only preview is insufficient because individual items must remain editable. Preview and live runtime must share property application, hierarchy, visibility, ordering and material rendering.

Inventory all existing screens and editable items in the latest project. Expose supported items on all those screens, not just Home; include repeated list items via explicitly labeled template-wide editing. Expose child text/icon/surface selections separately where meaningful. Respect real layout constraints instead of accepting ineffective position values. Preserve data-bound scores, questions, exam titles and timers: explain when edits affect a template or static label; never replace dynamic business data with fixture strings.

Implement approved dynamic component types in real screen containers, including text, button, image/icon, card/banner, divider and spacer. Added components must render after save, publish and restart. Support reorder, duplicate, hide/show, delete and restoration with stable IDs, valid parentage and cycle prevention. Remove must mean real removal/hiding of an allowed element, not just deletion from the preview. Essential controls such as submit, login, navigation and active exam timer require protection and understandable explanation; retain protected-component rules. Allow visual styling wherever safe. Do not turn this into unrestricted arbitrary code execution.

Changing a setting updates preview immediately without publishing. Handle numeric typing/debounce and slider gestures without rebuilding the entire screen on every frame. Undo/redo must cover every edit including hierarchy, add/remove and presets; one slider gesture is one useful undo step. Switching tools/screens, recreation, process death and returning to Studio must preserve the recoverable draft/session. Discard and reset must restore actual baseline styling, including clearing previously applied effects. Separate Save Draft from Publish. Explicitly distinguish unsaved, saved locally/offline, saved on server, publish failed and published/verified.

## iOS 26 inspired visual controls on Android
This is an Android Kotlin/XML app. Do not claim native Apple APIs or implement unrelated iOS operating-system features. Verify current official Apple documentation on Liquid Glass/materials and create an explicit applicable-feature mapping, with implemented behavior and platform limits. Reference:
https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass
https://developer.apple.com/design/human-interface-guidelines/materials

Implement functional controls for applicable Liquid Glass inspired surfaces: real backdrop blur, clear/tinted/frosted material presets, tint and tint opacity, material opacity separate from whole-item opacity, corner shape/radius, subtle edge highlights, border/shadow/elevation, readable foreground contrast, pressed/selected/disabled states, floating navigation/control styling where existing structures support it, and restrained configurable motion/transitions. Any refraction, highlight movement or morphing advertised by the editor must have an actual renderer and verified behavior. Do not sell a translucent flat fill as real blur or an ordinary fade as refraction/morphing. Explicitly label approximations and unsupported capabilities rather than shipping inert controls.

Blur must blur the content behind a surface while keeping its foreground text/icons sharp. Blurring the entire target View including its children is not an acceptable substitute. Inspect existing glass rendering/custom views before choosing implementation. Respect Android API capability and minSdk: provide an honest performant fallback with explanatory UI on unsupported devices. Avoid per-frame full-screen bitmap captures, recursive capture and memory leaks. Verify zero blur and reset remove effects. Honor reduced motion/disabled system animations and transparency/contrast needs. Preserve readability in light and dark mode and avoid automatically imposing glass on every content surface. Scope an iOS-inspired preset to selected item/screen unless global application is explicitly chosen.

## Configuration, security and failure handling
Preserve existing saved drafts/published configurations, versions, restore and audit history. Migrate schema additively with defaults and round-trip all supported fields; keep Android and Worker validation aligned. Verify each new property in production rendering before offering it. Keep UI Studio source of truth in the existing Worker/D1 flow unless latest evidence shows otherwise. Preserve Firebase Auth/admin authorization and validate mutation permissions server-side, including protected elements and action targets.

Use an allowlist of navigation/action types and supported destinations; validate external URLs and keep exam/auth flows intact. Handle offline saves, HTTP 401/403, malformed configuration, invalid inputs, lost connectivity, revision conflicts and multiple admins without silently overwriting newer edits. Retain the last good published config on network failure. Publication readback must verify the actual returned configuration/revision and edited fields, not merely a cached matching version number. Applying draft preview must never leak into students' published configuration.

## Verification: build success is not completion
A passing Gradle build or unit tests alone is NOT acceptable functional proof. Run appropriate Android build/lint and backend type/test checks, then exercise the feature in an emulator/device and record screenshots plus scenario results. Use mock/local backend scenarios when credentials are absent, labeling them accurately. If an emulator, authenticated backend or another dependency is unavailable, state exactly what could not be verified; do not mark those scenarios passed or claim fully verified completion. Push authorized scoped work with an honest report, not a fabricated success claim.

Required scenarios:
- Open Studio on narrow phone, select Blur, tap a nested card/surface, change blur: backdrop visibly changes, foreground stays crisp, other categories/other items stay unchanged.
- Colors, text, spacing, corners, content, actions, animation and states each change only intended fields and produce matching production rendering.
- Add, duplicate, reorder, hide, show and remove components in supported real containers; published live screen matches preview after restart. Essential controls remain usable.
- Select overlapping/repeated/hidden elements, zoom/pan/scroll, rotate and change tools without wrong selection or losing draft.
- Undo/redo and reset accurately restore the prior render, including blur/tint/visibility and removal of stale overrides.
- Save, close/reopen, process recreation, publish, fetch fresh published config and view it in a separate student session; ensure draft isolation and field parity.
- Offline draft, reconnect, two-admin revision conflict, server validation rejection and unauthorized publication have truthful recoverable UI.
- Verify day/night, large fonts, accessibility labels/touch targets, keyboard, lower supported API fallback, reduced motion and acceptable scrolling/rendering performance.
- Regression-check theme circular reveal, exam timer/submission, reattempt, result question isolation and floating airplane integration wherever touched.

Deliver code, a concise UI_STUDIO_VERIFICATION.md with capability matrix, files changed, reproducible functional evidence, limitations and checks actually executed. Commit only scoped files, push as authorized, and report exact GitHub Actions status. Do not claim CI passed if it is still running. Complete the implementation rather than stopping at a plan or static mockup.
