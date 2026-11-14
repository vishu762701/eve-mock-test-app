# Telegram Android Parity Report (V3)

> Generated on: 2026-11-14T14:16:56.857459+00:00
> Commit SHA: `47bf994494ba5e6999fde807a6aeeb2f0a71fef9`
> Branch: `main`
> Latest CI Status: `in_progress` ()

---

## 1. Crash Log (Phase 0 Evidence)

# EVE Crash Log (Phase 0)

| Screen / Component | Exception / Signal | Root Cause | Fix Commit |
|---|---|---|---|
| `DebugCrashReporter` | Missing external crash trace | Debug crash reporter only saved stack traces to internal `filesDir`, preventing external log capture from shared storage | Staged in Phase 0 commit |
| `RLottieNative` / `telegram_lottie.cpp` | SIGSEGV / SIGBUS / Fatal signal 11 | Concurrent `getFrame` and `destroy` without thread synchronization caused use-after-free; unaligned `reinterpret_cast<uint32_t*>` on row strides; missing zero-dimension checks | Staged in Phase 0 commit |
| `ProfileGooeyView` | `IllegalArgumentException: radius must be > 0` / HWUI RenderNode crash | Blur radius `b <= 0f` passed to `RenderEffect.createBlurEffect`; empty/negative `wholeOptimized` bounds on non-intersecting rects; unhandled GPU driver pipeline aborts | Staged in Phase 0 commit |
| `ProfileActivity` | Logic inversion on toolbar back click | `binding.btnBack` in toolbar was wired to `showPersonalQr()` instead of `finish()`, causing unexpected navigation and crash loops on back tap | Staged in Phase 0 commit |


---

## 2. Parity Manifest Constants (Phase 1 Single Source of Truth)

Total authoritative constants tracked: **50**

| Symbol | Component | Value | Kind | Upstream Telegram Source & Snippet |
|---|---|---|---|---|
| `ACTION_BAR_BACK_SIZE_DP` | ActionBar | `54` | dp | org/telegram/ui/ActionBar/ActionBar.java:270<br>`addView(backButtonImageView, LayoutHelper.createFrame(54, 54, Gravity.LEFT | Gravity.TOP));` |
| `ACTION_BAR_HEIGHT_LANDSCAPE_DP` | ActionBar | `48` | dp | org/telegram/ui/ActionBar/ActionBar.java:1916<br>`return dp(48);` |
| `ACTION_BAR_HEIGHT_PORTRAIT_DP` | ActionBar | `56` | dp | org/telegram/ui/ActionBar/ActionBar.java:1918<br>`return dp(56);` |
| `BACK_DRAWABLE_ANIMATION_TIME_MS` | BackDrawable | `300` | ms | org/telegram/ui/ActionBar/BackDrawable.java:39<br>`private float animationTime = 300.0f;` |
| `BACK_DRAWABLE_STROKE_WIDTH_DP` | BackDrawable | `2` | dp | org/telegram/ui/ActionBar/BackDrawable.java:49<br>`paint.setStrokeWidth(AndroidUtilities.dp(2));` |
| `BOTTOM_SHEET_DIM_ALPHA` | BottomSheet | `0.2` | alpha | org/telegram/ui/ActionBar/BottomSheet.java:222<br>`protected int dimBehindAlpha = 51;` |
| `BOTTOM_SHEET_DISMISS_DURATION_MS` | BottomSheet | `320` | ms | org/telegram/ui/ActionBar/BottomSheet.java:510<br>`animator.setDuration(320);` |
| `BOTTOM_SHEET_DISMISS_FAST_MS` | BottomSheet | `180` | ms | org/telegram/ui/ActionBar/BottomSheet.java:1902<br>`currentSheetAnimation.setDuration(cellType == Builder.CELL_TYPE_CALL ? 330 : 180);` |
| `BOTTOM_SHEET_OPEN_DELAY_MS` | BottomSheet | `20` | ms | org/telegram/ui/ActionBar/BottomSheet.java:1777<br>`currentSheetAnimation.setStartDelay(waitingKeyboard ? 0 : 20);` |
| `BOTTOM_SHEET_OPEN_DURATION_MS` | BottomSheet | `400` | ms | org/telegram/ui/ActionBar/BottomSheet.java:213<br>`public int openDuration = 400;` |
| `BUTTON_BOUNCE_DURATION_MS` | ButtonBounce | `320` | ms | org/telegram/ui/Components/ButtonBounce.java:115<br>`bounceAnimator.setDuration(320);` |
| `BUTTON_BOUNCE_OVERSHOOT` | ButtonBounce | `5` | ratio | org/telegram/ui/Components/ButtonBounce.java:20<br>`this(viewToInvalidate, 1f, 5.0f);` |
| `BUTTON_BOUNCE_PRESS_MS` | ButtonBounce | `60` | ms | org/telegram/ui/Components/ButtonBounce.java:85<br>`animator.setDuration((long) (60 * durationPressMultiplier));` |
| `BUTTON_BOUNCE_RELEASE_MS` | ButtonBounce | `350` | ms | org/telegram/ui/Components/ButtonBounce.java:89<br>`animator.setDuration((long) (350 * durationReleaseMultiplier));` |
| `CHECKBOX_ANIMATION_DURATION_MS` | CheckBox2 | `200` | ms | org/telegram/ui/Components/CheckBoxBase.java:278<br>`public long animationDuration = 200;` |
| `CHECKBOX_DEFAULT_SIZE_DP` | CheckBox | `22` | dp | org/telegram/ui/Components/CheckBox.java:54<br>`private int size = 22;` |
| `CHECKBOX_PROGRESS_BOUNCE_DIFF` | CheckBox | `0.2` | ratio | org/telegram/ui/Components/CheckBox.java:59<br>`private final static float progressBounceDiff = 0.2f;` |
| `FLICKER_LOADING_DEFAULT_GRADIENT_WIDTH_DP` | FlickerLoadingView | `200` | dp | org/telegram/ui/Components/FlickerLoadingView.java:964<br>`gradientWidth = dp(200);` |
| `FLICKER_LOADING_SPEED_DIVISOR` | FlickerLoadingView | `400` | ratio | org/telegram/ui/Components/FlickerLoadingView.java:933<br>`totalTranslation += dt * width / 400.0f;` |
| `MENU_DRAWABLE_ROTATION_DURATION_MS` | MenuDrawable | `200` | ms | org/telegram/ui/ActionBar/MenuDrawable.java:94<br>`currentAnimationTime = (int) (currentRotation * 200);` |
| `MENU_DRAWABLE_STROKE_WIDTH_DP` | MenuDrawable | `2` | dp | org/telegram/ui/ActionBar/MenuDrawable.java:67<br>`paint.setStrokeWidth(dp(2));` |
| `POLL_MAX_ANSWER_LENGTH` | Poll | `100` | ratio | org/telegram/ui/Components/ChatAttachAlertPollLayout.java:192<br>`public static final int MAX_ANSWER_LENGTH = 100;` |
| `POLL_MAX_OPTIONS_DEFAULT` | Poll | `12` | ratio | org/telegram/messenger/AppGlobalConfig.java:70<br>`public final ConfigInt pollAnswersMax = ofInt("poll_answers_max", 12);` |
| `POLL_MAX_QUESTION_LENGTH` | Poll | `255` | ratio | org/telegram/ui/Components/ChatAttachAlertPollLayout.java:191<br>`public static final int MAX_QUESTION_LENGTH = 255;` |
| `POLL_MAX_SOLUTION_LENGTH` | Poll | `200` | ratio | org/telegram/ui/Components/ChatAttachAlertPollLayout.java:193<br>`public static final int MAX_SOLUTION_LENGTH = 200;` |
| `PROFILE_ACTION_BETWEEN_PADDING_DP` | ProfileActionsView | `7` | dp | org/telegram/ui/Components/ProfileActionsView.java:253<br>`final float betweenPadding = xpadding / 2f;` |
| `PROFILE_ACTION_DRAWABLE_SIZE_DP` | ProfileActionsView | `24` | dp | org/telegram/ui/Components/ProfileActionsView.java:390<br>`final int drawableSize = dp(24);` |
| `PROFILE_ACTION_ROUND_RADIUS_DP` | ProfileActionsView | `16` | dp | org/telegram/ui/Components/ProfileActionsView.java:513<br>`return dp(16);` |
| `PROFILE_ACTION_TARGET_HEIGHT_DP` | ProfileActionsView | `74` | dp | org/telegram/ui/ProfileActivity.java:3909<br>`actionsView = new ProfileActionsView(context, dp(74));` |
| `PROFILE_ACTION_XPADDING_DP` | ProfileActionsView | `14` | dp | org/telegram/ui/Components/ProfileActionsView.java:134<br>`xpadding = dpf2(14);` |
| `PROFILE_AVATAR_SIZE_DP` | ProfileGooeyView | `100` | dp | org/telegram/ui/Components/ProfileGooeyView.java:37<br>`private static final float AVATAR_SIZE_DP = 100;` |
| `PROFILE_BLACK_BAR_DP` | ProfileGooeyView | `32` | dp | org/telegram/ui/Components/ProfileGooeyView.java:38<br>`private static final float BLACK_KING_BAR = 32;` |
| `PROFILE_GOOEY_INTENSITY` | ProfileGooeyView | `15` | ratio | org/telegram/ui/Components/ProfileGooeyView.java:61<br>`setIntensity(15f);` |
| `PULL_FOREGROUND_END_PULL_PARALLAX` | PullForegroundDrawable | `0.25` | ratio | org/telegram/ui/Components/PullForegroundDrawable.java:37<br>`public final static float endPullParallax = 0.25f;` |
| `PULL_FOREGROUND_MIN_PULLING_TIME_MS` | PullForegroundDrawable | `200` | ms | org/telegram/ui/Components/PullForegroundDrawable.java:39<br>`public final static long minPullingTime = 200L;` |
| `PULL_FOREGROUND_SNAP_HEIGHT` | PullForegroundDrawable | `0.85` | ratio | org/telegram/ui/Components/PullForegroundDrawable.java:35<br>`public final static float SNAP_HEIGHT = 0.85f;` |
| `PULL_FOREGROUND_START_PULL_OVERSCROLL` | PullForegroundDrawable | `0.2` | ratio | org/telegram/ui/Components/PullForegroundDrawable.java:38<br>`public final static float startPullOverScroll = 0.2f;` |
| `PULL_FOREGROUND_START_PULL_PARALLAX` | PullForegroundDrawable | `0.45` | ratio | org/telegram/ui/Components/PullForegroundDrawable.java:36<br>`public final static float startPullParallax = 0.45f;` |
| `RADIO_BUTTON_DURATION_MS` | RadioButton | `200` | ms | org/telegram/ui/Components/RadioButton.java:124<br>`checkAnimator.setDuration(200);` |
| `RADIO_BUTTON_SIZE_DP` | RadioButton | `16` | dp | org/telegram/ui/Components/RadioButton.java:45<br>`private int size = AndroidUtilities.dp(16);` |
| `RADIO_BUTTON_STROKE_WIDTH_DP` | RadioButton | `2` | dp | org/telegram/ui/Components/RadioButton.java:51<br>`paint.setStrokeWidth(AndroidUtilities.dp(2));` |
| `SWITCH_ANIMATION_DURATION_MS` | Switch | `200` | ms | org/telegram/ui/Components/Switch.java:238<br>`checkAnimator.setDuration(200);` |
| `SWITCH_BACKPLATE_RADIUS_DP` | Switch | `10` | dp | org/telegram/ui/Components/Switch.java:450<br>`canvasToDraw.drawCircle(tx, ty, AndroidUtilities.dpf2(10), paint);` |
| `SWITCH_RIPPLE_RADIUS_DP` | Switch | `18` | dp | org/telegram/ui/Components/Switch.java:199<br>`rippleDrawable.setRadius(AndroidUtilities.dp(18));` |
| `SWITCH_THUMB_OFFSET_DP` | Switch | `7` | dp | org/telegram/ui/Components/Switch.java:384<br>`int tx = x + AndroidUtilities.dp(7) + (int) (AndroidUtilities.dp(17) * progress);` |
| `SWITCH_THUMB_RADIUS_DP` | Switch | `8` | dp | org/telegram/ui/Components/Switch.java:497<br>`canvasToDraw.drawCircle(tx, ty, AndroidUtilities.dp(8), paint);` |
| `SWITCH_THUMB_TRAVEL_DP` | Switch | `17` | dp | org/telegram/ui/Components/Switch.java:384<br>`int tx = x + AndroidUtilities.dp(7) + (int) (AndroidUtilities.dp(17) * progress);` |
| `SWITCH_TRACK_HEIGHT_DP` | Switch | `14` | dp | org/telegram/ui/Components/Switch.java:448<br>`rectF.set(x, y, x + width, y + AndroidUtilities.dpf2(14));` |
| `SWITCH_TRACK_RADIUS_DP` | Switch | `7` | dp | org/telegram/ui/Components/Switch.java:449<br>`canvasToDraw.drawRoundRect(rectF, AndroidUtilities.dpf2(7), AndroidUtilities.dpf2(7), paint);` |
| `SWITCH_TRACK_WIDTH_DP` | Switch | `31` | dp | org/telegram/ui/Components/Switch.java:380<br>`int width = AndroidUtilities.dp(31);` |

---

## 3. Component Implementation & Verification Status

| Component | Status | Source | Evidence |
|---|---|---|---|
| Profile (Header, Gooey, Actions, Cells) | IN_PROGRESS | `ProfileActivity.java`, `ProfileGooeyView.java`, `ProfileActionsView.java` | Manifest + GPU/CPU safe math |
| Switch / CheckBox / Radio | VERIFIED | `Switch.java`, `CheckBox2.java` | TelegramParity constants + TelegramSwitch |
| BottomSheet | VERIFIED | `BottomSheet.java:213,222,510,1777` | 400ms open, 20ms delay, 0.2 dim, 20dp arc |
| ActionBar Architecture | VERIFIED | `ActionBar.java:270,1916,1918` | 56dp portrait, 48dp landscape, 54dp back |
| ButtonBounce Physics | VERIFIED | `ButtonBounce.java:20,85,89,115` | 60ms press, 350ms release, 5.0 overshoot, 320ms bounce |
| Polls Flow | VERIFIED | `ChatAttachAlertPollLayout.java:191-193`, `AppGlobalConfig.java:70` | 255 question, 100 option, 200 explanation, 12 options cap |

---

## 4. Verification Checkpoints
- `tools/check_no_stubs.py`: Enforced on FAST & FULL builds.
- `TelegramParityTest`: Asserts every symbol in `TelegramParity.kt` matches `manifest.json`.
- `ScreenSmokeTest`: ActivityScenario launches every manifest activity with valid extras.
