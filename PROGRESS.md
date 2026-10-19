# EVE design reset — current handoff

Continue the owner’s locked root UI reset; do not repeat completed phases.

- Phases 0–3 were committed; Phase 3 native light/dark, gesture/three-button checks passed on run 39216050826.
- Phase 4 presentation migration is implemented. Its later hosted checks failed; native fixes are included in Phase 5 and require hosted verification.
- Phase 5 removes unused legacy resources, fonts, effects and task documents with recorded reference/lint evidence. Local assembleDebug, testDebugUnitTest, lintDebug and assembleDebugAndroidTest pass.
- Phase 6 enforcement and final hosted delivery remain pending at this phase boundary. Do not publish an APK until all required jobs pass for its exact commit.
- Preserve Telegram reveal behavior, Lottie assets/motion/triggers, navigation, features and business logic. Device font is retired; API 24–25 light-mode black navigation fallback is owner-approved.
- Current evidence: [report](docs/design-reset/report.md), [screens](docs/design-reset/screens.md), [cleanup proof](docs/design-reset/cleanup-evidence.json).
