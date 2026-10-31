# EVE Profile scroll-state restore contract — 2026-10-31

**Issue**: After the Profile full-window camera-host and source-derived actions-collapse changes, `ProfileActivity.kt:170-228` updated the complete avatar-to-cutout/blur/title/action state only from `setOnScrollChangeListener`. Its initial `post` replay restored **only** action alpha. A pre-restored or layout-restored `NestedScrollView.scrollY` could therefore render a mismatched header until another scroll event.

**Implementation**: `setupScrollAndGooey` now routes user scroll events and `NestedScrollView.doOnLayout` saved-scroll reconciliation into the same `renderProfileScrollState(scrollY,density)` method. All effects reuse the existing behavior, with collapse progress factored into `ProfileScrollState.progress` (100dp normalized clamp). This is **EVE lifecycle integration, ADAPTED**, not a claim that Telegram upstream uses `NestedScrollView`.

**Affected**: `app/src/main/java/com/eve/app/ui/profile/ProfileActivity.kt`, `ProfileScrollState.kt`, `app/src/test/java/com/eve/app/ui/profile/ProfileScrollRestoreContractTest.kt`. Regression tests cover boundary/overscroll values, densities and a single rendering path for restored and live scroll, including gooey motion/blur, avatar, toolbar, text and actions.

**Not covered**: Emulator recreation screenshots, actual OEM display cutout geometry, original Telegram `ProfileActionsView` renderer/touch physics, App-wide parity, bio/presence, official Apple bell and terminal FULL CI. FAST tests are not native motion verification. No backend, auth or production deployments changed.
