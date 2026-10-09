# Native UI CI investigation — runs #209–#227

## Recovered stopping point

- COMPLETED: jobs and failed logs for #209–#227 reviewed; #227 reports/screenshots retrieved; unchanged attempt 2 reproduced both failures on the same SHA. Main and origin/main were `bf1622a21f4866bf1507da12f63d0c27be373bf0`. No concurrent work was overwritten.
- IN PROGRESS at interruption: spacing test correction, capture preconditions and independent APK/UI workflow jobs. Existing report modifications were preserved.
- REMAINING at recovery: finish the one-mode runner, local checks, review, one scoped main commit/push, then inspect new Actions results. Post-push results are reported in the delivery response; pending execution is not a pass.

## Verified failures and classification

1. **Test assumption, not app spacing.** `ApprovedUiRenderingTest.approvedMockRendersInBothThemes`: `java.lang.AssertionError: expected:<21> but was:<22>` at `verifyMock(ApprovedUiRenderingTest.kt:209)`. Pixel2 density is 2.625. The approved XML has two separately resolved 4dp margins: round(10.5) + round(10.5) = 22px. The test wrongly resolved combined 8dp to 21px. Preserve XML; assert each margin and their independently rounded sum. Fractional-density checks cover 420/440/480dpi and both themes.
2. **SystemUI failure invalidated capture.** `RepairRegressionTest.realThemeRecreationPreservesStateAndCapturesBothSystemBarsWithoutOverlays`: `System navigation indicator must actually contrast in mock-screen-dark/three-button; pixels=0; focus=false appearance=0 flags=80810100 legacy=0 navigation=Insets{left=0, top=0, right=0, bottom=126}` at `screenshot(RepairRegressionTest.kt:310)`, called from test line225. Both initial-Light and Dark screenshots visibly contain **System UI isn't responding**. Android dims the app behind that dialog; Eve does not own focus. This image cannot establish an Eve navigation-color defect. The old check also accepted the obscured initial-Light image because dark-pixel counting against white alone cannot establish a valid capture. Add explicit ANR/focus failures before contrast, retain saved screenshots and the existing 3:1 assertion. Never dismiss an ANR or skip the check.
3. **CI coupling blocked APK delivery.** `assembleDebug` passed, but APK upload followed emulator verification in the same job. Separate required native UI matrix jobs from the APK job; upload immediately after compilation. No `continue-on-error`. Both modes run the full suite, with `fail-fast: false` and distinct artifacts.

The underlying SystemUI ANR trigger is **not proven**: #227's narrowly filtered logs contain no ANR thread trace. Earlier generic-AVD failures with focus=true are not automatically attributed to this dialog. Their renderer issue remains unverified; stock Clock also showed dim buttons on that image. Prior production appearance/color hypotheses did not resolve those failures. This repair modifies no production Kotlin, layouts, SystemBarHelper, ThemeSwitchAnimator or navigation colors.

## Reproduction and evidence

Unchanged [#227 attempt2](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37879269485/attempts/2), Android job113660190558, reproduced both assertions and the same ANR screenshot. APK compilation, backend and Functions passed. Configuration: API35/Android15, x86_64, google_apis, pixel_2, animations enabled, original gesture-then-three-button script including reboot. Attempt1 job113654936094 and attempt2 used the same source SHA above.

Local emulator execution is blocked: no `/dev/kvm`, emulator binary or installed system image. Hosted CI provided actual same-configuration reproduction, not a claimed local emulator pass.

Unmodified artifact evidence in [docs/verification/ci-227](docs/verification/ci-227/) includes both attempts' complete failure stacks, both Dark ANR captures, initial-Light ANR capture, and unobscured gesture Light/Dark screenshots. These are debug fixtures, not authenticated production screens. Artifact IDs: attempt1 `11593853077`, attempt2 `11594670873`.

The new matrix retains API35/google_apis/x86_64/pixel_2 and animations. Each fresh emulator selects one mode before the full suite, avoiding a second suite after changing mode/rebooting the same device. This isolates cross-suite state; it does not establish a permanent platform ANR fix. Failed jobs retain configuration, fingerprint, density/size, safe runtime logs, ANR/crash event categories, JUnit reports and screenshots. A remaining ANR/contrast failure stays red, separately from APK status.

## Run history (original attempts)

All Functions jobs passed. Backend passed #211–#227; #209/#210 failed TypeScript checks with TS2345 null/undefined parameter mismatches. Android results:

| Run | Android result / evidence |
|---|---|
| [209](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37868829371) | Passed; backend failed |
| [210](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37869303370) | Passed; backend failed |
| [211](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37869703285) | Passed |
| [212](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37870654344) | AdminDeletionRegressionTest.confirmedDelete409OfflineAnd500KeepHonestListStateAndNeverCreateUndoReplacement: RootViewWithoutFocusException |
| [213](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37871263268) | Same admin test/root-focus exception |
| [214](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37871777964) | Passed before contrast assertion was added; not evidence of visual acceptance |
| [215](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37872294394) | Theme test: mock-screen-light/gesture contrast |
| [216](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37872711338) | Theme test: mock-screen-dark/three-button contrast |
| [217](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37873302479) | Same three-button contrast assertion |
| [218](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37873931200) | Same three-button contrast assertion |
| [219](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37874462085) | Same; focus=true, appearance=0, legacy=0, bottom=48 |
| [220](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37875096645) | Same focused-window contrast failure |
| [221](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37875535770) | AdminDeletionRegressionTest.bannerRepositoryFailureIsUnwrappedAndNeverAnnouncedAsDeleted: RootViewWithoutFocusException |
| [222](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37876181142) | Three-button contrast, focus=true, appearance=0, flags=80810100, bottom=48 |
| [223](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37876832512) | Same after reboot |
| [224](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37877947312) | Same; stock Clock control also dim |
| [225](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37878041005) | Same on Google APIs image |
| [226](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37878483545) | Pixel2 test expectations: expected14.0/actual14.095238sp; expected99/actual100px; expected13.0/actual12.952381sp |
| [227](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37879269485) | Expected21/actual22px and SystemUI ANR-obscured contrast; identical unchanged rerun |

## Changed files and decisions

- `.github/workflows/build.yml`: independent APK and two UI jobs, upload ordering, runner contract checks, always-uploaded evidence. Retain build job ID and backend/Functions jobs.
- `.github/scripts/verify-native-ui.sh`: validate one mode, full suite, preserve failure exit status and diagnostics. No automatic test retry or suppression.
- `.github/scripts/tests/test_native_ui_runner.py`: both modes, instrumentation failure propagation, invalid/mismatched configuration, evidence collection.
- `ApprovedUiRenderingTest.kt`: independently rounded margin assertions without broad tolerances.
- `RepairRegressionTest.kt`: explicit ANR/unfocused capture rejection; preserve contrast, insets, state, recreation and overlay assertions.
- This report, existing audit/verification reports and fixture evidence: preserve history and correct unverified navigation explanations.

## Local verification before push

- `python3 -B -m unittest discover -s .github/scripts/tests -v`: 4 tests passed, exercising both navigation modes and failure paths with mocked adb/Gradle. This verifies runner behavior, not device rendering.
- `bash -n .github/scripts/verify-native-ui.sh`: passed.
- Parsed workflow structure: APK job independent of native UI, upload before lint, no emulator action in APK job, both required modes, matrix fail-fast=false. Passed.
- Java21 / Gradle8.7: `assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug` with two workers, in-process Kotlin and workspace proxy/truststore settings. **BUILD SUCCESSFUL in 1m58s; 86 tasks, 8 executed, 78 up-to-date.** Full local output: `/workspace/tools/eve-ci-isolation-verification.log`.
- Changed instrumentation compiled successfully; runtime outcome must come from the new hosted run. Do not equate compilation with instrumentation acceptance.
- JUnit XML: **196 unit tests, 0 failures, 0 errors**. Lint XML: **0 errors, 1525 existing warnings**.
- Backend/Functions source unchanged; #227 attempt2 passed both jobs. No unrelated local backend retest claimed.

Exact local Android command (exit0):

```sh
JAVA_HOME=/workspace/tools/jdk /workspace/tools/gradle-8.7/bin/gradle --no-daemon --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process '-Dorg.gradle.jvmargs=-Xmx3g -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=512m -Dfile.encoding=UTF-8' -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8080 -Dhttp.proxyHost=proxy -Dhttp.proxyPort=8080 assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug
```

## Limits and follow-ups

No latest ZIP was available; revision comparison is against connected main. No authenticated live generation/submission test or unrelated redesign is claimed. No extra production fix was made. Local emulator reproduction is constrained by missing KVM; the unchanged hosted rerun supplies actual same-config evidence. Suggested next steps: retain exact image/emulator revisions with artifacts; add a physical-device navigation check; capture a bounded SystemUI ANR trace if isolated verification still fails. Do not infer an app tint defect from an obscured screenshot.

## Post-push verification and confirmed setup race

Commit `a5dbb3a9d5e716dc564efb5797ac39e72449fa71` was pushed directly and verified against origin/main. [Run #228](https://github.com/vishu762701/eve-mock-test-app/actions/runs/37882548325) independently passed the APK/build/unit/lint job, backend and Functions. Artifact `Eve-debug-apk` (11594629067) was downloaded successfully; ZIP integrity passed and `app-debug.apk` contains the Android manifest and classes. Thus a failing emulator job no longer prevents APK delivery.

Three-button job113665256786 failed **before instrumentation** with `Navigation configuration failed: expected 0, observed 2`. Its artifact11595047891 subsequently recorded `navigation-mode.log: 0` and SystemUI `mNavigationMode=0`. Overlay selection is asynchronous; the immediate check ran before SystemUI published the requested setting. A bounded 30-second poll now waits for actual configuration, still failing if it never arrives. An added host test simulates two stale reads followed by the requested mode; the permanent-wrong-mode failure test remains. Five runner tests, shell syntax and diff checks pass. This follow-up corrects a measured CI setup race; it makes no production change and does not retry failed instrumentation.

Gesture job113665257001 executed all16 tests: 15 passed, including the real theme/contrast test; `approvedMockRendersInBothThemes` progressed past the repaired footer assertion and failed at line281: `expected:<49.875> but was:<50.0>`. The palette circle is correctly 100px wide (rounded38dp), so its circular radius is50px, not unrounded19dp=49.875px. The same test's subsequent padding expectations contradicted the existing fractional-density test and production adapter: palette total8dp is rounded once to21px and split10/11, not11/11. Align these exact assertions with rounded diameter and total gap; leave palette implementation untouched. This is a confirmed test expectation correction, grouped with the measured CI setup race in one follow-up after local verification.

Follow-up local `assembleDebugAndroidTest` using the same Java/Gradle/proxy flags above passed in1m24s (54 tasks;5 executed,49 up-to-date), exit0. Log: `/workspace/tools/eve-ci-confirmed-followup.log`. Five runner tests and `bash -n` passed. App production code remains identical to the successful full local build and #228 APK job. #228's exact new failure stack and mode-transition evidence are retained in `docs/verification/ci-228/`. The next hosted outcome is reported with delivery rather than presumed in advance.
