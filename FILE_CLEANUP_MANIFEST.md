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

- `UiArcProgressBar.kt`: explicit source and JVM test found, no confirmed active production host. Kept pending full reflection/XML reachability and dependency tests, not called a live feature in the catalog.
- `ic_nav_home.xml`, `design_bottom_bar_height`, `design_bottom_bar_margin`: Home overlay uses were removed. Retained because wider res qualifiers, design test baselines or dynamic references were not completely disproven; do not mass-delete blindly.
- `docs/design-reset/*` baseline and `backend/migrations/0017_typography_studio.sql`: retained as historical reference/schema state; removing them could damage source audit or deployed D1 compatibility.
- The deleted Studio editor entries are audited above; no production D1 tables, app user preferences, font files, secrets or Firebase rules were deleted.

Source scan covered committed files and direct imports/XML references. It is **not** a complete proof for reflection, remotely supplied resource names, release shrinker behavior, or files outside the repo. Ambiguous artifacts are intentionally retained.
