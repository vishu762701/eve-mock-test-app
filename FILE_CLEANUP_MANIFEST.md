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
