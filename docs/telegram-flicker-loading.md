# Telegram-Source-Traced Flicker Loading for EVE Student Lists

## 1. Overview & Screen Intent
In the Home exam discovery list, Exam Tests list, and Test History list, students receive immediate visual feedback via a lightweight, Canvas-based Telegram-source-derived flicker loading skeleton view (`TelegramFlickerSkeletonView`).

- **Primary user action**: Kept dominant on populated screens (Home → pick exam, Exam Tests → start test, History → review attempt). The flicker loading skeleton is strictly noninteractive.
- **Surface preservation**: All headers, bottom floating glass tabs, list geometries, card corner radii (26dp), padding, margins, and empty/error states are preserved.
- **Centralized colors**: Light base `#FFFFFFFF` / highlight `#0F000000`; Dark base `#FF282829` / highlight `#0DFFFFFF`. Global canvas (`#F1F1F3` / `#000000`) and surface (`#FFFFFF` / `#181819`) colors remain unchanged.

---

## 2. Official Source Contract & Mapping Table

| Upstream Property / Contract | Telegram Upstream Source Reference | Upstream Verified Value / Hash | EVE Implementation Path & Line | Fidelity Classification | Deviations / Adaptation |
| :--- | :--- | :--- | :--- | :--- | :--- |
| Upstream Source File | `TMessagesProj/.../Components/FlickerLoadingView.java` | SHA-256 `571148bccebf77edc7a19493a1d58f23b8a4047560d3ad0504086a9b147bee47` | `app/src/main/java/com/eve/app/designsystem/TelegramFlickerSkeletonView.kt:1` | SOURCE-DERIVED EVE ADAPTATION | Native Kotlin implementation tailored to EVE card architecture |
| Base color (Light) | `ThemeColors.java:205` (`actionBarDefaultSubmenuBackground`) | `#FFFFFFFF` (`0xFFFFFFFF`) | `TelegramColorRoles.kt:36` / `telegram_colors.xml:13` | VERIFIED SOURCE-EXACT | Exact Telegram default color |
| Base color (Dark) | `night.attheme:35` (`actionBarDefaultSubmenuBackground`) | `-14145495` (`#FF282829`) | `TelegramColorRoles.kt:36` / `values-night/telegram_colors.xml:13` | VERIFIED SOURCE-EXACT | Exact Telegram dark theme key |
| Highlight color (Light) | `ThemeColors.java:129` (`listSelector`) | `0x0F000000` | `TelegramColorRoles.kt:35` / `telegram_colors.xml:15` | VERIFIED SOURCE-EXACT | Exact Telegram default color |
| Highlight color (Dark) | `night.attheme:308` (`listSelectorSDK21`) | `234881023` (`0x0DFFFFFF`) | `TelegramColorRoles.kt:50` / `values-night/telegram_colors.xml:15` | VERIFIED SOURCE-EXACT | Exact Telegram dark theme key |
| Gradient stops | `FlickerLoadingView.java:969-971` | `[0.0, 0.4, 0.6, 1.0]` | `TelegramFlickerSkeletonView.kt:127` | VERIFIED SOURCE-EXACT | Exact stop positions |
| Gradient colors order | `FlickerLoadingView.java:969-971` | `[color1, color0, color0, color1]` | `TelegramFlickerSkeletonView.kt:126` | VERIFIED SOURCE-EXACT | `[highlight, base, base, highlight]` |
| Tile Mode | `FlickerLoadingView.java:969-971` | `Shader.TileMode.CLAMP` | `TelegramFlickerSkeletonView.kt:128` | VERIFIED SOURCE-EXACT | Clamp at edges |
| List gradient width | `FlickerLoadingView.java:966` | `dp(600)` | `TelegramFlickerSkeletonView.kt:119` | VERIFIED SOURCE-EXACT | 600dp vertical gradient width |
| Frame delta clamping | `FlickerLoadingView.java:914-919` | `dt > 17 -> 16; dt < 4 -> 0` | `TelegramFlickerSkeletonView.kt:156-157` | VERIFIED SOURCE-EXACT | Eliminates frame jitter |
| Translation progression | `FlickerLoadingView.java:939` | `totalTranslation += dt * height / 400.0f` | `TelegramFlickerSkeletonView.kt:161` | VERIFIED SOURCE-EXACT | Exact speed formula |
| Wrap boundary | `FlickerLoadingView.java:940-942` | `>= height * 2 -> -gradientWidth * 2` | `TelegramFlickerSkeletonView.kt:162-164` | VERIFIED SOURCE-EXACT | Continuous seamless vertical looping |
| RTL adaptation | `FlickerLoadingView.java:977-989` | Mirror horizontal coordinates | `TelegramFlickerSkeletonView.kt:205-295` | SOURCE-DERIVED EVE ADAPTATION | Mirrors start/end offsets across row variants |
| Row geometry | `item_exam.xml`, `item_exam_test.xml`, `item_history.xml` | Authentic EVE Card Layouts | `TelegramFlickerSkeletonView.kt:216-318` | SOURCE-DERIVED EVE ADAPTATION | Real EVE cards (26dp radius, 8dp gap), NOT Telegram chat cells |

---

## 3. Screen State Matrices

### A. Home Exam Discovery (`MainActivity.kt` / `activity_main.xml`)
| State | Initial / No-Cache | Refresh with Cached Items | Action / Result |
| :--- | :--- | :--- | :--- |
| **Loading** | `loadingHome` VISIBLE, `rvExams` GONE | `loadingHome` GONE, `rvExams` VISIBLE | Skeleton shows on cold start; cached exams remain visible on reload |
| **Success (Filled)** | `loadingHome` GONE, `rvExams` VISIBLE | `loadingHome` GONE, `rvExams` VISIBLE | `DesignLoading.showContent` smoothly swaps to populated list |
| **Success (Empty)** | `loadingHome` GONE, `messageGroup` VISIBLE | `loadingHome` GONE, `messageGroup` VISIBLE | Empty message and Lottie animation displayed |
| **Error / Offline** | `loadingHome` GONE, `messageGroup` + `btnRetry` VISIBLE | `loadingHome` GONE, `rvExams` VISIBLE | Error state shown; cached data preserved if already present |

### B. Exam Tests List (`ExamTestsActivity.kt` / `activity_exam_tests.xml`)
| State | Initial / No-Cache | Refresh with Cached Items | Action / Result |
| :--- | :--- | :--- | :--- |
| **Loading** | `loadingTests` VISIBLE, `rvTests` GONE, `progressBar` GONE | `loadingTests` GONE, `rvTests` VISIBLE, `progressBar` VISIBLE | Toolbar progress on reload; in-list skeleton on initial fetch |
| **Success (Filled)** | `loadingTests` GONE, `progressBar` GONE, `rvTests` VISIBLE | `loadingTests` GONE, `progressBar` GONE, `rvTests` VISIBLE | Real test items rendered with status buttons |
| **Success (Empty)** | `loadingTests` GONE, `progressBar` GONE, `tvEmpty` VISIBLE | `loadingTests` GONE, `progressBar` GONE, `tvEmpty` VISIBLE | "No tests available yet" empty state |
| **Error / Offline** | `loadingTests` GONE, `compactErrorView` VISIBLE | `loadingTests` GONE, `compactErrorView` VISIBLE, `rvTests` VISIBLE | Retry banner shown without hiding cached tests |

### C. Test History (`HistoryActivity.kt` / `activity_history.xml`)
| State | Initial / No-Cache | Refresh with Cached Items | Action / Result |
| :--- | :--- | :--- | :--- |
| **Loading** | `loadingHistory` VISIBLE, `rvHistory` GONE | `loadingHistory` GONE, `rvHistory` VISIBLE | Skeleton shows on cold start; cached attempts remain visible |
| **Success (Filled)** | `loadingHistory` GONE, `rvHistory` VISIBLE | `loadingHistory` GONE, `rvHistory` VISIBLE | Populated test attempt history rows |
| **Success (Empty)** | `loadingHistory` GONE, `messageGroup` VISIBLE | `loadingHistory` GONE, `messageGroup` VISIBLE | "No test attempts yet" with empty state animation |
| **Error / Offline** | `loadingHistory` GONE, `messageGroup` + `btnRetry` VISIBLE | `loadingHistory` GONE, `rvHistory` VISIBLE | Explicit retry message; cached history preserved |

---

## 4. Accessibility & Performance Safeguards
1. **Accessibility**: `importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO`; `isClickable = false`; `isFocusable = false`. Taps and accessibility navigation pass directly through to parent structures without ghost touch targets.
2. **Reduced Motion**: Respects `Settings.Global.ANIMATOR_DURATION_SCALE == 0` and `ValueAnimator.areAnimatorsEnabled() == false`. Draws static base/highlight placeholders without frame callbacks (`postInvalidateOnAnimation`).
3. **Lifecycle Integrity**: Pauses animation when detached from window, invisible, or window visibility changes. Resumes immediately upon re-attachment or visibility restoration.
4. **Zero onDraw Allocations**: Paints, RectFs, Matrices, and Shaders are allocated as instance members and updated only on layout/theme changes.
