# Eve Visual Recovery Foundation

**Authority:** AGENTS.md and SOURCE_OF_TRUTH.md remain controlling. This phase creates evidence and inspection infrastructure, not a silent redesign.

## Reference direction
- Apple iOS 26: disciplined visual hierarchy, purposeful depth, selective translucent chrome.
- Samsung One UI: Android reachability, adaptive geometry, accessible touch and typography.
- Telegram Android: precise interaction feedback, panels, menus, and the protected Day/Night circular reveal.

Eve must have its own consistent design system. Do not copy source code/assets or indiscriminately combine styles.

## What already exists
The Android native tests generate Day/Night production-layout *fixtures* for Home, Test, Profile, Result tabs, Target Exams, system bars and selected interactions. Existing GitHub Actions runs both gesture and three-button emulators. Keep these checks.

## What this phase adds
- Automatically generated visual-review.html: offline/mobile-friendly screenshot gallery; download and extract the native-ui artifact from a failed or manually dispatched run to open it.
- visual-review.json: required light/dark coverage, PNG headers/dimensions, capture warnings and source-based review candidates.
- CI contract: after native tests pass, missing critical screenshot pairs and malformed PNGs fail the native-ui job. Even if the emulator fails, available evidence is packaged.
- Host-only tests for the report generator.

The XML review queue is **heuristic**: large hardcoded widths, strong strokes, high elevation and excessive radius. These are not automatic defect findings. Custom Kotlin views, hardware blur, animation smoothness, touch interactions, font scaling and authenticated app data still require native/device assessment.

## Approval-based repair sequence
1. For a failed or manually dispatched run, open the native-ui artifact, visual gallery, JSON and relevant Kotlin/XML. For successful push runs, dispatch manually to generate these artifacts.
2. Take ONE logical component family and document the actual problem in both themes, exact files, severity, alternatives, performance/accessibility trade-offs and expected result.
3. Propose the best fix to the product owner and obtain approval.
4. Implement and verify only approved work on main. Compare before/after screenshots, lint, unit tests, native UI tests and Actions status.
5. Never compromise immutable test mechanics, semantic colors, BlurView placement, or the Telegram circular theme transition.

## Priority for next phases
1. P1: increase real capture coverage for small-screen, large-font, RTL and untested states.
2. P1: diagnose motion behavior/frame pacing and circular theme transition without changing it.
3. P2: inspect visually inconsistent strokes, spacing, clipping, responsive card geometry and source inventory.
4. P2: verify GPU blur and material depth on physical devices.

This is an evidence foundation, **not** an autonomous continuously running LLM coding agent. A future event-driven agent can consume review artifacts after an explicit approval-gated execution design is established.
