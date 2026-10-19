# EVE design reset evidence

The owner's current locked specification supersedes historical UI authority.
Only the Telegram day/night toggle animation and its behavior/resources remain
protected. Business logic, data, navigation flow and features remain invariant.

## Phase 0

- Source baseline: `40ecfd366ccd3df94cbeed8f2526355f940dcec5`, clean `main`,
  synchronized with `origin/main` before editing.
- UI: XML Views, Material 3, ViewBinding; no Compose dependency or build feature.
- AGP 8.5.2; Kotlin 1.9.24; compile/target SDK 36; minimum SDK 24.
- Counts and full layout/source inventory: `baseline.json`; reproducible with
  `python3 scripts/design_inventory.py`. Counts include production comments and
  token definitions, all resource qualifiers, XML font-family resources and JSON.
- `baseline-sha256.json` records every production file's SHA-256 for subsequent
  protected-code/resource comparisons. Nothing has been deleted.
- `python3 -B -m unittest discover -s .github/scripts/tests -v`: 15 tests passed.
  These verify the existing design/runner contracts, not the requested new UI.
- `./gradlew assembleDebug`: exit 127; Gradle is not installed and the repository
  launcher falls back to a nonexistent Termux Gradle path. No Android build result
  is claimed. Build, lint and device verification need local tool setup or Actions.

## Implementation choice and failure analysis

Use native shared resource tokens and theme/widget styles, followed by explicit
screen migration. A runtime tree rewrite was considered and rejected because it
could overwrite feature-specific states and the protected reveal. Principal risks:
qualified resources overriding defaults, clipping ancestors hiding card shadows,
and runtime styling overriding migrated XML. Inventory all qualifiers and callers,
fix clipping in shared hosts, and verify runtime color/font bindings per screen.

## Pending

Phases 1–6, light/dark device verification, final shadow mapping, font/icon license
report, unmapped hue audit, small colored text contrast listing, deleted-resource
proof and `scripts/design_check.sh` PASS remain pending. No screen is marked migrated.

## Phase boundary and requirement conflict

**BLOCKED before Phase 1; no production files changed.** Settings exposes a working
font-choice feature (`SettingsActivity.showFontPickerBottomSheet`,
`bottom_sheet_font_picker.xml`, `FontManager.typeface/mediumTypeface`). Device mode
uses the platform typeface; default mode uses bundled fonts. The locked request
both mandates Poppins with no substitution/only one protected exception and requires
every feature to keep working. Retiring this preference changes a feature; retaining
it introduces another font exception. The product owner must choose precedence.
A clarification has been requested; no preference has been silently removed,
disabled or made ineffective.

| Phase | Status |
| --- | --- |
| 0 — baseline | DONE-VERIFIED: inventory, original-HEAD tag, static checks, direct-main push |
| 1 — root | BLOCKED on font-preference conflict; font download preflight succeeded |
| 2 — components | Pending |
| 3 — navigation/icons/transitions | Pending |
| 4 — every screen | Pending; all sources/layouts listed in `screens.md` |
| 5 — remove old | Pending; no files deleted |
| 6 — enforce | Pending; no design-check PASS claimed |

| Production metric | Before | At Phase 0 boundary |
| --- | ---: | ---: |
| Hex color occurrences | 496 | 496 |
| Literal dp occurrences | 3007 | 3007 |
| Literal sp occurrences | 181 | 181 |
| Style definitions including qualifiers | 279 | 279 |
| Drawables including qualifiers | 133 | 133 |
| Font resources including XML families | 8 | 8 |
| Layouts | 95 | 95 |
| Kotlin sources | 237 | 237 |

### Font preflight

Regular 400, Medium 500 and Bold 700 TrueType files downloaded successfully from
Google Fonts' official `google/fonts` repository, `ofl/poppins/Poppins-Regular.ttf`,
`Poppins-Medium.ttf`, `Poppins-Bold.ttf`. `file` identifies all three as Poppins
TrueType fonts. These files are in workspace scratch only, not bundled into the app
before the root phase. The official `ofl/poppins/OFL.txt` was fetched and read:

> Copyright 2020 The Poppins Project Authors (https://github.com/itfoundry/Poppins)
>
> This Font Software is licensed under the SIL Open Font License, Version 1.1.
>
> The OFL allows the licensed fonts to be used, studied, modified and
> redistributed freely as long as they are not sold by themselves.

The full copyright notice and license must be distributed with the bundled fonts
when Phase 1 resumes. No font substitution was made.

### Checks and limits

- `git diff --check`: PASS.
- `git diff pre-design-reset -- app/src/main`: empty, including all protected
  animation files and resources. Their behavior has not been modified.
- `git ls-remote origin refs/heads/main refs/tags/pre-design-reset` verified the
  Phase 0 commit on remote main and original HEAD as the remote tag.
- Actions run https://github.com/vishu762701/eve-mock-test-app/actions/runs/39186602486
  was checked after push: in progress. Backend and Firebase Functions jobs passed;
  Android build/lint/native gesture and three-button jobs were still running.
  This is baseline CI, not evidence of a migrated UI.
- Light/dark rendering, lint delta, final `assembleDebug`, new design enforcement,
  zero-old-token grep, shadow values and contrast audit have **not** been verified.
- Unmapped hues and every small system-colored text use on white remain unaudited;
  no color migration has occurred. No resource-deletion evidence is claimed.
- No new library added, no CI changes made, and no backend or feature changes made.

### Test on phone

1. Open Settings, tap the font preference, and choose Device font.
2. Confirm that the app currently switches typography after recreation.
3. Decide whether this feature should be retired for mandatory Poppins or retained
   as a second explicit exception. No redesigned APK is available at this boundary.

## Phase 1 — root implementation (supersedes the font blocker above)

The owner explicitly chose **remove Device font; Poppins everywhere**. The old
Settings font row and picker entry point are removed. The font lifecycle override
and font-version recreation loop are removed; all other settings remain intact.
Font resource cleanup stays in Phase 5 with the required deletion proof.

- Existing canonical day/night palette verified in `colors.xml`; new inset, text,
  hairline, track, Mono, shadow and glass tokens added in `design_colors.xml` with exact owner values
  (fractional alpha rounded to the nearest 8-bit channel).
- Bundled Regular 400 / Medium 500 / Bold 700 Poppins binaries and XML family;
  complete SIL OFL 1.1 included in APK assets and quoted in third-party notices.
- Locked sizes, type/line heights, shapes, elevation, motion durations and press
  scale defined; identical size resources supplied for sw360dp and sw600dp.
- Root Material 3/2, AppCompat/framework typography, palette, background, control,
  link, selection/cursor, ripple, shape and transparent system-bar defaults bound.
  `DesignTokens` resolves theme colors for custom Views without caching themes.
- Default button, card, chip, input, switch/control tint and ring styles installed.
  Shared runtime behavior and exact switch geometry remain Phase 2.
- Default modal themes are solid, 26dp, with API 31+ modal blur disabled.
- Depth mapping: elevation 9dp; intended y=6dp/blur=20dp. API 28+ ambient/spot
  shadow colors black 16% light / 60% dark, multipliers 1.0. API 24–27 platform
  black shadows use ambient/spot alpha .16 light / .60 dark. Platform shadow
  geometry approximates the requested blur; clipping remediation is Phase 2.
- Old per-screen styling is deliberately still pending; this phase does not
  certify every screen or remove any old resources.

Checks run before this phase's commit: all resource XML parsed; all three size
copies compared equal; TrueType OS/2 weights checked as 400/500/700; 15 Python
contract tests passed; `git diff --check` passed; protected toggle code/resources
have an empty diff. A native light/dark default-widget test was added. JVM UI
assertions changed only for the retired font setting, new theme token bindings
and explicitly disabled modal blur; all other checks remain enabled.

Local Gradle 8.7 and Android SDK tools/platform 36 were installed outside the
repository. `./gradlew assembleDebug` is running with the local tool PATH and
session HTTP proxy; the existing AGP 8.5.2 compileSdk 36 compatibility warning is
recorded, not suppressed. Build/lint/native results are pending at commit time.

Root resource verification caught and fixed an AppCompat `track` attribute name
collision (custom color attributes now use the `design` prefix), and verified the
framework's public `ambientShadowAlpha` / `spotShadowAlpha` attribute names using
the installed SDK 36's resources. Resource merge/link and Kotlin compilation now
pass locally. The next local Java step found missing `jlink` in the environment's
Java installation; a complete JDK is being installed outside the repository.
This does not establish a successful APK build yet. The first root CI run failed
on the fixed resource collision; a new run is required for the correction commit.

### Phase 1 local verification passed

A complete Temurin JDK 21 was installed outside the repository. With Gradle 8.7,
Android SDK 36 and the session proxy/trust store, the following completed with
`BUILD SUCCESSFUL`:

```
./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
```

- 281 JVM tests, 0 failures/errors/skipped.
- Lint: 0 errors/fatal issues; warning inventory remains for later cleanup.
- Debug APK and instrumentation APK assembled successfully.
- The native Settings fixture no longer references the owner-retired font field.
- Existing exact system hue/background tokens remain in `colors.xml`, since those
  values already matched the owner's spec. This preserves existing color audits;
  new roles are in `design_colors.xml`. No color assertion was weakened.
- Font identity checks parse both legacy family and typographic family names from
  the actual TrueType `name` table; Medium correctly has legacy family
  `Poppins Medium` and typographic family `Poppins`.
- Material elevation tinting is disabled so a 9dp card retains exact `surface`.
  The native test also checks its rendered center pixel in both themes.
- Root implementation is committed; hosted light/dark native checks remain pending
  for the correction commit. Full screen migration and design_check PASS are not
  claimed. No resources have been deleted.

Phase 2 diagnosis (before changes): 89 CardView declarations inspected; 9 have
ancestors retaining default child/padding clipping (Result's four summary cards,
four Premium cards and the palette-circle host). ViewPager2's internal RecyclerView
also requires runtime host handling. Existing explicit elevations, custom surface
helpers and zero-margin rows must be migrated through shared components, not
border/alpha workarounds.
