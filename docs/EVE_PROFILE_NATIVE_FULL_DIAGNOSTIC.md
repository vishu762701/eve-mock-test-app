# Diagnostic FULL — Profile native integration matrix (2026-10-31)

This commit requests a **diagnostic six-job FULL run**, not a declaration that the entire Telegram-fidelity / Profile / Poll / Category / Apple bell mission has passed release acceptance.

Native test `app/src/androidTest/java/com/eve/app/ui/profile/ProfileMissionIntegrationNativeTest.kt` runs an Android 35 renderer on BOTH Day/Night and LTR/RTL with 1.35x font, inflates the actual Profile layout, measures the full-window avatar host, checks source-glyph color threshold and vectors, confirms server-Bio feature gating, and verifies scroll-collapse disables all three action touch targets. This combines previously isolated test-contract components in one mounted Android View tree. It does not rely on a live Firebase token or leak real users' data.

The workflow `.github/workflows/build.yml` selects FULL from the `[eve-full]` commit message, then invokes separate gesture and three-button emulator jobs, backend, functions, build and verified APK publication only when all prerequisites pass. A successful FULL verifies automated gates, NOT full pixel parity with the official Telegram app, Apple SF Symbols Android redistribution rights or live Cloudflare Worker/D1 migration.

Current known blockers explicitly remain: original dependency-complete `ProfileActionsView` Canvas/avatar expansion motion, matched reference device videos and physical cutout verification; Poll/Category real-device usability; official Apple iOS-26 bell rights; Bio and truthful Presence production manual rollout; privacy and GDPR acceptance. Do not label FID-01–FID-07 fully accepted solely because a CI pipeline is green. Production deploy remains opt-in via `confirm_production=true`.
