# Approved UI implementation and UI Studio retirement

Source: `vishu762701/eve-mock-test-app`, verified clean `main` at
`cc47908aba6cbf55d27680749cc3223c866a8621`. No AGENTS.md was present.
The attached EVE_MASTER_DESIGN_HANDOFF.md was read as design context; the current
implementation request authorizes these changes and supersedes its old no-edit instruction.

## Scope

- Home retains existing pure black/white backgrounds and profile circle; banner
  radius 18dp, All/Other adjacency when Other exists, category radius 50dp, exam
  radius 8dp, bare header actions with 48dp targets and original icon artwork sizes,
  and the airplane remains hidden even when a floating link is configured.
- Test uses dedicated light/dark native colors, 12dp options and 20dp circular
  indicators, fixed approved typography, 44dp actions and 48dp equal-width
  navigation, exact approved gaps and strokes. The full question/option page
  scrolls rather than shrinking text or clipping long answers. Existing selection,
  clear, review marking, bookmark, report, navigation, submission and timing
  callbacks remain. The shared palette opts into Test styling only; its current
  cell retains answer/review color. Semantic correct/incorrect and timer warnings
  remain available.
- Result uses floating segmented Overview / Review / Leaderboard tabs with
  contrasting track/pill/text defaults. ResultTabs connects this order to existing
  content, including initial selection. Other Result cards, statistics, filters,
  sharing, reattempt, cutoff, analytics, and Full Leaderboard are preserved.
- UI Studio Activity/navigation, models, repository, engine, registry, renderers,
  interaction/material/baseline/policy/preset/color/preview support, layout, IDs,
  Android API methods, base-activity/timer/adapter hooks, Worker routes, policy,
  public auth exception, backend types, and feature-owned tests are removed.
- The existing build workflow now verifies pull requests, backend types/tests,
  Android lint/unit tests, and focused native UI behavior on an API 35 emulator.
  Worker deployment remains restricted to its existing main/master workflow.

## Cleanup evidence and retained files

The audit inspected the tracked source/resource/backend/build/CI/documentation
inventory, Kotlin and XML references, manifest declarations, ProGuard rules,
backend imports/configuration, tests, assets and resource lookup mechanisms.
No active getIdentifier or reflective R lookup remains. Premium's filename-based
asset loading is retained. No committed APK/ZIP/log/temp/build/node_modules
artifacts were found outside historical documentation evidence.

Removed obsolete TestThinMaterialPillHelper and its style/colors/dimension after
all callers were replaced with native presentation. Removed QuestionFitHelper
and its obsolete font-shrinking tests after its sole active caller was replaced
with the scrolling page. Removed bg_preview_canvas and studio_ids after the
Studio feature owning them was retired.

The following additional resource files have no references in active source,
XML, manifest, tests, configuration, Gradle, CI or documentation, and no dynamic
resource lookup path can select them:

- `app/src/main/res/anim/stay_visible.xml`
- `app/src/main/res/color/btn_submit_bg.xml`
- `app/src/main/res/color/btn_submit_stroke.xml`
- `app/src/main/res/color/btn_submit_text.xml`
- `app/src/main/res/drawable/bg_exam_card_frosted.xml`
- `app/src/main/res/drawable/bg_exam_icon_tile.xml`
- `app/src/main/res/drawable/bg_fade_to_canvas.xml`
- `app/src/main/res/drawable/bg_indicator_active.xml`
- `app/src/main/res/drawable/bg_liquid_glass_floating.xml`
- `app/src/main/res/drawable/bg_liquid_glass_pill.xml`
- `app/src/main/res/drawable/bg_login_tab_indicator.xml`
- `app/src/main/res/drawable/bg_rank_medallion.xml`
- `app/src/main/res/drawable/ic_moon.xml`
- `app/src/main/res/drawable/ic_paste.xml`
- `app/src/main/res/drawable/ic_redo.xml`
- `app/src/main/res/drawable/ic_state_error.xml`
- `app/src/main/res/drawable/ic_sun.xml`
- `app/src/main/res/drawable/ic_undo.xml`
- `app/src/main/res/font/poppins.xml`
- `app/src/main/res/font/source_serif_4.xml`

All historical D1 migrations, including 0012_ui_studio.sql, are unchanged. No
forward migration is needed to disable the feature; stored Studio tables/configs
and production user data are not deleted. Historical Studio task documents,
progress records, verification reports and screenshots are retained as archival
evidence, not active implementation requirements. Shared BlurView, Lottie,
networking and Firebase dependencies remain because unrelated screens use them.
Referenced individual font files, font selection and font licenses remain.
Unused family XML wrappers and four unreachable font weights were removed. Release, signing, security and backend deployment configuration
are retained.

## Verification

Local verification completed successfully. Focused tests cover
Result order/visibility, option restore/clear/select, active palette state and
navigation, and unavailable retired backend routes without Studio table access.

- Full JDK 21 / Gradle 8.7 / Android SDK 36: assembleDebug, lintDebug,
  testDebugUnitTest and assembleDebugAndroidTest passed. Android unit results:
  189 tests, zero failures/errors/skips. Lint has existing nonfatal warnings;
  no errors remain. Resource linking and XML/light-dark token checks passed.
- Backend npm run build passed; all 73 Node tests passed, including two retired
  route tests against the actual Worker router. The session proxy is supplied to
  Node via an external Undici EnvHttpProxyAgent test bootstrap (no app dependency
  added). The initial direct live-endpoint request failed until proxy setup.
- Initial Android attempts required a full JDK (the system runtime lacked jlink)
  and the system CA trust store for proxy TLS. These environment limitations were
  resolved locally. Lint fixes use API 24-compatible explicit padding and move
  existing light-navigation-bar attributes to API 27 qualifiers; no behavior
  change on supported versions. One obsolete test asserting decorative report
  feedback was updated to the approved bare icon background.
- No active Studio references in app/src/main, backend/src or workflow runtime
  configuration. Expected remaining references are migrations, archival docs and
  negative tests for retired routes. Full Leaderboard code/layouts and all
  migrations have no diff.
- Native tests compile locally; device execution is delegated to the updated
  GitHub Actions workflow because this environment has no /dev/kvm or attached
  Android device. No production Worker deployment or database mutation was run.

A second reference audit after removing the unused wrappers found five additional
unreachable resources, also removed:

- `app/src/main/res/drawable/bg_report_button.xml`
- `app/src/main/res/font/poppins_bold.ttf`
- `app/src/main/res/font/poppins_regular.ttf`
- `app/src/main/res/font/source_serif_4_medium.ttf`
- `app/src/main/res/font/source_serif_4_semibold.ttf`

The final behavior review also keeps the option listener attached after Clear
Response (intermediate child notifications and NO_ID are suppressed only during
clearing), so immediate reselection
works without a rebind. The focused native test now covers this directly. Mark
for Review's selected button state uses approved yellow/black contrast and
follows the existing marked state on both toggle and page navigation.

The first GitHub Actions run passed both Android and backend jobs, including all
three native emulator tests:
https://github.com/vishu762701/eve-mock-test-app/actions/runs/37744940470
The final Clear Response/review-state refinement passed the same local Android
build/lint/189-unit-test/instrumentation-compilation commands and is submitted
for another CI run with the stronger native assertions.

Selected indicator fill stays inside its 2dp border; a native pixel assertion
checks that selecting an answer does not paint over the border in either theme.

The strengthened emulator test exposed RadioGroup.clearCheck's intermediate old-ID
notification. The final adapter suppresses callbacks during clearCheck only;
this prevents the old answer being saved again and retains immediate reselection.
The failing refinement run is retained as evidence:
https://github.com/vishu762701/eve-mock-test-app/actions/runs/37745551098

The immediate-clear regression passed on the emulator after callback suppression.
The initial indicator pixel assertion was too strict at a curved antialiased
edge; the final rendering check allows up to 24 levels of edge blending per
RGB channel, while still rejecting a fill covering the contrasting border.
The resource/paint colors and stroke width are unchanged.
