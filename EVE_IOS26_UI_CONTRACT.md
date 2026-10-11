# Eve iOS 26 UI contract

## Authority and implementation

This shared Android foundation supersedes the monochrome-only accent direction.
It interprets Apple's semantic design principles with Android resource qualifiers,
Material 3, native sans-serif typography and Android drawables. UIKit, SF font
files, Apple's vibrancy pipeline and proprietary Liquid Glass rendering are not
Android dependencies or claimed capabilities.

Official sources reviewed on October 11, 2026:

- [Color](https://developer.apple.com/design/human-interface-guidelines/color): use
  consistent semantic meanings, adaptive appearances and increased contrast;
  never convey essential state through colour alone.
- [Buttons](https://developer.apple.com/design/human-interface-guidelines/buttons):
  distinguish primary actions by style, provide press feedback and adequate targets.
- [Labels](https://developer.apple.com/design/human-interface-guidelines/labels):
  primary information, supplemental secondary text, unavailable-item tertiary text,
  and quaternary watermark emphasis; prefer system typography.
- [Materials](https://developer.apple.com/design/human-interface-guidelines/materials):
  glass is a distinct functional navigation/control layer; use it sparingly and
  do not spread it into content cards.
- [Dark Mode](https://developer.apple.com/design/human-interface-guidelines/dark-mode):
  adaptive colours are not simple inversions; inspect both modes and increased
  contrast, with at least 4.5:1 for readable text.
- [WWDC25: Get to know the new design system](https://developer.apple.com/videos/play/wwdc2025/356/):
  design-language chapter at 2:06; fixed radii, capsules whose radius is half their
  height, and concentric inner radii obtained by subtracting parent padding.

The HIG pages' JavaScript shells were supplemented by Apple's official JSON at
`https://developer.apple.com/tutorials/data/design/human-interface-guidelines/{page}.json`.
The reference blue values requested for this task are reference colours, not a
claim that all iOS 26 platform palette values or rendering are replicated.
Android has no UIKit semantic resolver; our Day/Night resource roles supply the
adaptive equivalents. The existing in-app theme toggle remains intentional.

## Colour roles

Canonical roles live in `values/colors.xml` and `values-night/colors.xml`. Existing
names remain supported. Recovery colours are aliases into this same foundation.
Use a role according to its meaning, not because its HEX looks convenient.

| Role / resource | Day | Night | Use |
| --- | --- | --- | --- |
| systemBackground / `eve_system_background` | #FFFFFF | #000000 | Main canvas |
| secondarySystemBackground | #F2F2F7 | #1C1C1E | Secondary content / control surface |
| tertiarySystemBackground | #FFFFFF | #2C2C2E | Nested surface |
| systemGroupedBackground | #F2F2F7 | #000000 | Grouped area |
| secondarySystemGroupedBackground | #FFFFFF | #1C1C1E | Group content |
| tertiarySystemGroupedBackground | #F2F2F7 | #2C2C2E | Nested group content |
| Primary label | #000000 | #FFFFFF | Titles, questions, values, body |
| Secondary label | #50505A | #C5C5CC | Readable supplemental content |
| Tertiary label | #62626D | #ABABB3 | Unavailable-item descriptions |
| Quaternary label | #64646F | #9999A3 | Watermarks / lowest priority only |
| Placeholder | Tertiary label | Tertiary label | Input hints; never substitute for labels |
| Separator | #1A3C3C43 | #A6545458 | Decorative separation, alpha composited |
| Opaque separator | #C6C6C8 | #48484A | Opaque content divider |
| Functional outline | #74747E | #90909A | Readability, input and control boundaries |
| Reference system blue | #007AFF | #0A84FF | Reference / graphical accent |
| Primary interactive fill | #0062CC | #0A84FF | Prominent buttons and selected controls |
| On-primary | #FFFFFF | #000000 | Small labels and icons on primary fill |
| Interactive text | #005BC0 | #72B2FF | Small links, outlined actions, blue labels |
| Accent container | #E5F1FF | #142B49 | Soft action / selection surface |
| On-accent-container | Interactive text | Interactive text | Readable soft-action label |
| Destructive text | #96211C | #FF9A92 | Small destructive labels / login errors |

Contextual fills (`eve_fill_primary` through `eve_fill_quaternary`) retain adaptive
alpha values. Always composite them against the actual underlying surface before
checking contrast. Use opaque input and content surfaces where readability needs
predictability. A separator is not a functional focus outline.

General error and offline-banner text use readable foregrounds on their existing
status containers; their semantic test status colours remain untouched.

Small coloured text and primary filled-button roles are **accessibility-adjusted
Android equivalents**. White on reference #007AFF is only approximately 4.02:1;
white on #0062CC is 5.80:1. Black on Night #0A84FF is 5.76:1. Labels are opaque
and their lightness hierarchy is intentionally stronger than faint platform
examples, meeting AA across the supported content surfaces.

Increased contrast uses `ThemeOverlay.Eve.HighContrast`: secondary / tertiary text
#303038 Day and #E5E5EA Night; blue text #004A99 / #A8D1FF; outlines #484850 /
#CBCBD2; primary fills #004A99 / #409CFF. Main canvases stay unchanged.
`DesignContrast` applies this overlay before inflation in `EveBaseActivity` when
Android API 34+ `UiModeManager.contrast >= 0.5`. Returning from system settings
refreshes a changed contrast preference, while deferring during the theme reveal.
Older Android versions retain the AA default mapping. Theme-attribute shared
appearances, text/outlined actions and primary button selectors gain the stronger
roles; legacy direct colour call sites retain the already AA default roles.

Existing adaptive green/red/orange colours remain tied to status, result metrics
and meaningful categories. Do not add rainbow decoration or borrow test status
colours for general interaction. This task does not expand category colour use.

## Typography

Use `TextAppearance.Eve.*` and the same semantic M3 mappings. Android `sans-serif`
and `sans-serif-medium` support platform shaping and user font scaling; no Apple
font files are imported. Display 34sp bold, titles 28/22/20sp, headline 17sp
medium, body 17sp, callout 16sp, supplemental 15sp, footnote 13sp and captions
12/11sp are reusable roles. Compact buttons use medium weight.

Importance is independent of font size: small answer labels remain primary;
metadata remains secondary and readable. Do not reduce view alpha to make text
secondary. Tertiary and quaternary are not ordinary metadata defaults. Device-font
preferences and the test engine's independently fitted question typography remain
supported. Do not force shared body sizing into the fixed test viewport.

## Shape and component rules

| Component | Shared rule |
| --- | --- |
| Primary, neutral, soft and outlined action | `ShapeAppearance.Eve.Capsule`, 50% radius based on actual height; 48dp minimum target, balanced inherited Material padding |
| XML drawable capsule | `eve_radius_pill` saturation fallback; Android rendering clamps to half the bounds |
| Circular icon button | Capsule on equal width/height, 48dp target and 12dp padding; labelled content must not be placed in a forced circle |
| Content card / tile | Fixed 20dp corner; opaque surface, subtle single outline; 1.5dp light elevation, 0dp Night |
| Input | Fixed 12dp; dedicated functional outline / focused blue; filled input focus state retained |
| Category chip | Capsule with explicit checked colour and outline states; a checked chip is distinguished by fill and label |
| Target / recovery chip | Fixed 12dp for dense wrapping, 40dp visual height and 48dp touch target |
| Segmented track / child | 14dp parent; 3dp inset and 11dp child; avoid capsule child in a pinched fixed-radius track |
| Sheet / dialog | Fixed 28dp; sheet top corners only, dialog all corners; keep actual functional glass chrome |
| Popup menu | Fixed 16dp, opaque readable surface and one subtle outline |
| Switch | Checked blue track and on-accent thumb; neutral unchecked track; dedicated outline and disabled mapping |

Concentric example: a 28dp container with a 16dp inset input yields a 12dp inner
radius. Use `max(0, parentRadius - inset)` when the child actually shares the
parent's center. Do not apply that equation to unrelated side-by-side controls.
Material fill and clipping use one shape model. Retain borders for focus,
selection and readability. Never stack an XML outline over a Material outline.
Buttons retain the existing tactile animator and Material ripple; explicit
disabled colours keep unavailable labels readable. Cards remain content surfaces,
not additional glass controls.

`values/themes.xml` owns shared M3 colour/typography/widget/dialog definitions,
including the complete surface-container hierarchy and tertiary control fallback.
Disabled Read labels retain their disabled behaviour and use opaque readable text,
rather than fading text alpha on an outlined control.
`values-night/themes.xml` supplies appearance-specific window flags and the dark
bottom-sheet parent. The shared foundation eliminates duplicated Night widget
styles. Historical Recovery resource names remain compatibility aliases/styles,
not an independent design system.

## Protected boundaries

- Preserve the checked-in right/wrong/medium/marked colours, including legacy
  result tile variants and `approved_ui_colors.xml` test-specific mappings.
  Historical prose has conflicting HEX values; actual baseline resources and the
  regression fixture govern preservation for this task.
- Preserve `eve_test_shape_border`, monochrome test navigation and timer,
  `eve_test_selected` #34C759, and `eve_test_review` #FFCC00.
- Preserve rigid test-engine geometry, option semantics, answer visibility,
  server scoring/submission, authentication and backend logic.
- Preserve the Telegram-style circular theme reveal, system-bar synchronization,
  and functional BlurView/glass chrome. No new glass content cards.
- UI Studio is retired and outside scope. Single-role colour-reference corrections
  in dependent layouts are not authorization for screen-by-screen redesign.

## Verification gate

- Parse resource XML; AAPT/Gradle compile proves references and theme attributes.
- Resolve aliases and Day/Night overrides; composite foreground and background
  alpha against the real canvas before checking 4.5:1 normal text and 3:1 important
  graphics. Decorative separators and protected test colours have distinct roles.
- Run `python3 -B .github/scripts/check_design_system.py` and script regressions.
  Protected colour snapshots must remain unchanged. Do not lower thresholds to
  accommodate a failing new role.
- Run `./gradlew testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest`.
  Replace the superseded monochrome-only assertion with blue/AA assertions.
- Native `SharedDesignSystemTest` inflates actual shared widget styles and checks
  capsule geometry, opaque cards, disabled labels, resolved colours and contrast.
  Capture Day/Night plus increased-contrast component PNGs. The runner also emits
  these four deterministic fixture PNGs as base64 between `EVE_DESIGN_PNG_BEGIN`
  and `EVE_DESIGN_PNG_END` markers in job logs, for reviewers who cannot reach the
  artifact storage host. This fallback excludes live activity screenshots and user
  data. Existing tests cover
  production fixtures, font scales, test integrity, target selection and reveal.
- Require both gesture and three-button native CI jobs and visual gallery evidence
  enforcement. Review native PNGs; source inspection alone is not visual proof.
  Shared component PNGs are native Canvas fixtures, not live authenticated flows;
  existing recovery screenshots use hardware full-window captures. Neither claims
  Apple rendering or production authentication/network verification.
- Review the full diff, `git diff --check`, safely reconcile remote main, push without
  force, then confirm exact remote SHA and Actions conclusions.
