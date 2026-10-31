# Profile Bio native FULL failure — real-device gate repair (2026-10-31)

**Run:** `41178345602` at `e6274f88727d9c31488299ad6dc330ed580225e2`, six-job FULL **FAILED** on BOTH Android 35 navigation suites (gesture and three-button). Build, backend and Firebase Functions were SUCCESS; publish APK was SKIPPED.

**Two root-cause assertions, both on real emulator:**
- `ProfileBioNativeTest.kt:29`: `Disabled until server confirms Bio capability`.
- `ProfileMissionIntegrationNativeTest.kt:53`: `Old Worker must not expose a misleading Bio editor`.
Both test suites inflated `activity_profile.xml` without the actual `ProfileActivity.loadProfile` controller, then assumed the raw XML's `android:enabled=false` gave the final capability state. On Android 35 the raw binding reported the cell enabled. **An XML default is not a sufficient runtime safety contract.**

**Forward-only implementation repair:** Introduce `ProfileBioCapability.apply(cell, value, authenticatedBio)`, whose only authority is the nullable Bio field in the authenticated Profile GET. `null` **always disables** editing and explains server unavailability, `""` enables the editor as an empty supported Bio, and nonempty text enables and displays that Bio. Call this shared reducer immediately after Activity inflation, again at each new profile load, and after successful authenticated readback. Existing navigation and PUT/GET editor confirmation remain unchanged.

**Regression coverage:** `ProfileBioFeatureGateTest` statically asserts controller delegates to the single reducer; both failing real-device tests now call the same reducer and assert old server fail-closed, authenticated empty Bio enabled, reversion to unavailable disabled, Day/Night UI and original full-window/RTL geometry. No test was skipped, ignored, or weakened by removing its fail-closed assertion.

The repair's `[eve-full]` commit asks GitHub to execute six-job FULL again on its exact SHA. Re-run results and APK SHA-256 must be assessed independently. Bio and presence remain staged behind manual Worker production deployment; official Telegram Canvas reference parity and Apple SF Symbols Android licensing remain separately blocked.
