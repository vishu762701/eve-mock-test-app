# File cleanup manifest — Final Consolidation (in progress)

This is a source-evidence record, not a claim of completed QA.

## Editor-only removal (Checkpoint 1)

| Path | Proof / reason |
|---|---|
| `backend/src/routes/typographyStudio.ts` | Removed retired editor-only entry point, draft editor, preview, or editor-specific check after source dependency tracing. |
| `app/src/main/java/com/eve/app/ui/admin/TypographyStudioActivity.kt` | Removed retired editor-only entry point, draft editor, preview, or editor-specific check after source dependency tracing. |
| `app/src/main/java/com/eve/app/typography/TypographyStudioViewModel.kt` | Removed retired editor-only entry point, draft editor, preview, or editor-specific check after source dependency tracing. |
| `app/src/main/res/layout/activity_typography_studio.xml` | Removed retired editor-only entry point, draft editor, preview, or editor-specific check after source dependency tracing. |
| `app/src/main/res/layout/item_typography_preview_row.xml` | Removed retired editor-only entry point, draft editor, preview, or editor-specific check after source dependency tracing. |
| `app/src/test/java/com/eve/app/ui/admin/TypographyStudioTest.kt` | Removed retired editor-only entry point, draft editor, preview, or editor-specific check after source dependency tracing. |
| `app/src/test/java/com/eve/app/typography/TypographySliderRangeTest.kt` | Removed retired editor-only entry point, draft editor, preview, or editor-specific check after source dependency tracing. |
| `scripts/verify_typography_studio.py` | Removed retired editor-only entry point, draft editor, preview, or editor-specific check after source dependency tracing. |

Retained: `app/src/main/java/com/eve/app/typography/{TypographyRepository,TypographyApplier,TypographyResolver,TypographyModels,TypographyRegistry}.kt`, `FontManager`, app font assets, public published typography GET route, and immutable `backend/migrations/0017_typography_studio.sql`. The old published config and historical tables are NOT deleted. `backend/test/typography_studio.test.mjs` retains compatibility/security assertions for the retired route and public schema (filename to be consolidated later).

Verification status: source graph checked for direct call sites; full Android/backend build and dynamic/reflection scans pending CI. Other ambiguous or remotely referenced assets stay intact.

## Retained ambiguous/cross-cutting candidates

- `UiArcProgressBar.kt`: removed after source search found no production XML/Kotlin constructor/import/reflection callers. The only non-doc inbound reference was a JVM file-existence assertion; removed that obsolete test without modifying the actual UI behavior suites.
- `ic_nav_home.xml` and `design_bottom_bar_height`: retained: the icon remains referenced in historical design evidence, and the height dimension is still used for Settings tap rows. A zero-caller check for the rest of the repository (including remote naming) was not exhaustive.
- `docs/design-reset/*` baseline and `backend/migrations/0017_typography_studio.sql`: retained as historical reference/schema state; removing them could damage source audit or deployed D1 compatibility.
- The deleted Studio editor entries are audited above; no production D1 tables, app user preferences, font files, secrets or Firebase rules were deleted.

Source scan covered committed files and direct imports/XML references. It is **not** a complete proof for reflection, remotely supplied resource names, release shrinker behavior, or files outside the repo. Ambiguous artifacts are intentionally retained.


## Proven dead source/style resource cleanup (2026-10-22)

| Deleted element | Active inbound reference proof | Compatibility note |
|---|---|---|
| `app/src/main/java/com/eve/app/designsystem/UiArcProgressBar.kt` | GitHub code search for `UiArcProgressBar` found only the class, a JVM file-existence test and documentation; no Android XML or Kotlin runtime caller. | Kept existing native upload/status progress; removed only dead demonstration code and its existence assertion. |
| `Widget.Eve.Token.Glass.BottomBar` in `app/src/main/res/values/design_widgets.xml` | Search for full style name found only style declaration, after deleting the Home floating bar. | No screen uses this style after removal. |
| `design_bottom_bar_margin` from values, values-sw360dp, values-sw600dp | Search found the three declarations and the dead bottom-bar style. | `design_bottom_bar_height` is retained for Settings touch target. |

**NOT DELETED:** `ic_nav_home.xml` (historic design references), source migrations/fonts, current tests, source of truth or production data. Final Android linking and native suite CI still required; dynamic names beyond committed source remain UNKNOWN.
