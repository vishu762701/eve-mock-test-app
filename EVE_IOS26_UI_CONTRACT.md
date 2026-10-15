# Eve iOS 26 UI contract

## Authority

The current user authorizes full geometry migration on all screens (ADR-009:
Full-screen iOS 26 geometry). This overrides earlier geometry-protected rules.
Screen regions, interactions, English strings, theme reveal, Lottie assets/triggers/
sizes and icon files remain protected. Answer-state colours remain unchanged.

Eve interprets Apple's design principles on Android with Material 1.12.0, resource
qualifiers, native sans-serif and BlurView 2.0.6. No UIKit, SF font files or Apple's
proprietary glass renderer are used. Numeric corner radii below are Eve choices;
Apple publishes no numeric radii for this three-shape interpretation.

## Verified references

The official Color, Typography and Materials JSON pages were fetched and read.
[Extracted Apple rows and source hashes](docs/ios26/APPLE_REFERENCE_ROWS.md) and
saved JSON provide the evidence. All 12 default system colours, light and dark,
already matched colors.xml. Core text sizes/leading in type.xml already matched
the iOS/iPadOS Large (default) table. No type-scale change was needed.

- https://developer.apple.com/tutorials/data/design/human-interface-guidelines/color.json
- https://developer.apple.com/tutorials/data/design/human-interface-guidelines/typography.json
- https://developer.apple.com/tutorials/data/design/human-interface-guidelines/materials.json

## Colour and backgrounds

| Role | Light | Dark |
|---|---|---|
| systemBackground | #FFFFFF | #000000 |
| secondarySystemBackground | #F2F2F7 | #1C1C1E |
| tertiarySystemBackground | #FFFFFF | #2C2C2E |
| systemGroupedBackground | #F2F2F7 | #000000 |
| secondarySystemGroupedBackground | #FFFFFF | #1C1C1E |
| tertiarySystemGroupedBackground | #F2F2F7 | #2C2C2E |
| Primary label | #000000 | #FFFFFF |
| Reference systemBlue / interactive blue text | #0088FF | #0091FF |

Home, Login, Test and dialog content use plain backgrounds with secondary raised
and tertiary nested surfaces. Settings, Profile, Admin lists, Result Overview,
Leaderboard and About use grouped backgrounds with secondary cells and tertiary
nested surfaces. Opaque content surfaces must never acquire a blur layer.

`eve_system_blue` is the sole default interactive-blue source. Accent text,
on-accent-container, category blue foreground, input focus stroke, selected icons
and links resolve through aliases to it. `eve_accent_text_readable` retains unused
#005BC0 / #72B2FF for a one-line revert of the accent alias.

#0088FF on white is about 3.5:1: a known user-chosen trade-off, below 4.5:1.
Blue on some secondary/tertiary surfaces also falls below 4.5:1. Checks report
these explicit exact-blue exceptions without weakening the threshold for other
text. This is not a claim of AA compliance for blue text. Protected green/red
answer-state and review colours retain their baseline values and meaning.

## Shapes and density

| Shape | Components | Eve implementation |
|---|---|---|
| Capsule | Buttons, chips, segmented tracks and thumbs, single-line search/filter fields, switches, sliders, badges, floating actions and navigation/tab containers | `ShapeAppearance.Eve.Capsule`, cornerSize 50%; XML drawable fallback `eve_radius_pill` saturates to half bounds |
| Fixed | Cards, grouped containers, tiles, popup menus | 26dp |
| Fixed modal | Sheets and dialogs | 34dp; bottom sheets have top corners only |
| Concentric | Nested surfaces and multiline/form fields | max(4dp, parent radius minus actual padding); 26−12=14dp, 34−16=18dp |

`dimens.xml`, ShapeAppearance styles and `EveShapes.concentric` centralize geometry.
Touch targets are at least 48dp; list rows have a 52dp minimum. Result uses a
56dp segmented track so its 4dp insets leave 48dp targets. Preserve screen
regions and existing responsive sizing. Do not place decorative circles, rings
or tiles behind Lottie animations. Shape fill, outline and clipping must agree.

## Material scope

Glass is restricted to four categories: navigation/top bars, tab bars, popup menus,
and modal sheets/dialogs. Cards, exam/result cards, list rows, banners and drawer
content are opaque. The Sign In prompt's outside area stays undimmed and unblurred.

EveGlassToolbar uses the thin approximation and EveBlurHelper with BlurView's
RenderEffect engine on API 31+;
foreground children are drawn after the backdrop. Lower APIs use an opaque tint.
Popup RenderEffect is applied only to the captured background ImageView.
Floating dialogs use API 31 window-background blur clipped to their background shape;
GlassmorphismHelper retains window-local lifecycle ownership for sheet blur.
EveGlassPill uses semantic primary text and a capsule; a pill alone does not enable blur.

Material tokens are documented Android approximations, not Apple numeric values:

| Token | Light tint | Dark tint |
|---|---|---|
| ultraThin | #CCFFFFFF | #CC1C1C1E |
| thin | #D9FFFFFF | #D91C1C1E |
| regular | #E6FFFFFF | #E61C1C1E |
| thick | #F2FFFFFF | #F21C1C1E |

Their raised alpha preserves at least 4.5:1 for primary labels against even an
opposing black/white backdrop. Text, icons and Lottie foregrounds are never blurred.
Use primary labels on glass. Device compositor behavior requires native evidence;
source-level contrast calculations alone do not verify hardware rendering.

## Typography

Use semantic `TextAppearance.Eve.*` roles in sp. The verified default sizes are
34/28/22/20/17/17/16/15/13/12/11 for Large Title through Caption 2. Headline is
semibold (Android medium-family approximation). Top-level titles use Large Title;
pushed-screen headers use Headline, 17sp. Preserve the Eve/Device font switch and
independent test-question fitting. Avoid local numeric text-size or literal colour overrides.

## Verification

Preserve protected palette snapshots and native behavior tests. Update superseded
geometry assertions to the authorized contract; never suppress unrelated failures.
Run XML parsing, design-system checks, script tests, Android unit tests, lint, APK
and instrumentation compilation. Both gesture and three-button native suites and
visual evidence enforcement must pass before publishing the sole Eve-debug-apk
artifact. Inspect light/dark evidence, verify the pushed main SHA and Actions status.
Any missing device, screenshot or authenticated-flow evidence must be reported plainly.

## Shared foundation retained

`Theme.Eve.Foundation` continues to own the 26 semantic colour attributes, shared
Material typography, widgets, disabled states and surface hierarchy. Existing
Recovery names remain aliases into this foundation. General error/offline text
and protected test states keep their own semantic colours; blue is an interaction
role. The higher-contrast overlay retains stronger labels, outlines and fills;
its blue-text alias follows the current user's systemBlue choice.

Importance remains independent of type size. Supplemental metadata stays readable,
answer labels stay primary, and question fitting retains its own measured viewport.
The Eve/Device preference remains implemented by FontManager and the device-font
overlay. No font assets or preference logic change in this migration.

Production screen traces and the earlier native-fixture methodology remain in
[EVE_STUDENT_UI_COVERAGE.md](EVE_STUDENT_UI_COVERAGE.md) and
[EVE_ADMIN_UI_COVERAGE.md](EVE_ADMIN_UI_COVERAGE.md). Their older numeric shape or
Home-background descriptions are superseded by this contract and ADR-009. Canvas
fixtures, full-window screenshots and authenticated production flows are distinct
forms of evidence; report them separately.
