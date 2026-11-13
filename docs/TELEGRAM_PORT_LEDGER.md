# EVE Telegram UI Port Ledger

## F1 ActionBar on every screen - PENDING_CI
| value | Telegram (File.java:line) | Eve before | Eve after |
|---|---|---|---|
| ActionBar height | `ActionBar.java:1914-1920` (56dp portrait, 48dp landscape) | 48dp (`design_touch_min`) / `wrap_content` | `@dimen/telegram_action_bar_height` (56dp port, 48dp land) |
| Back button width | `ActionBar.java:270,1404` (54dp) | 48dp (`design_touch_min`) | `@dimen/telegram_action_bar_back_width` (54dp) |
| Title start margin | `ActionBar.java:1527` (72dp from left with back button) | Large title block outside bar | Inside bar at 72dp (`@dimen/telegram_action_bar_title_start` after 54dp button) |
| Title text size | `ActionBar.java:534,1445` (20sp medium, 18sp with subtitle) | 34sp / 24sp large title below bar | 20sp roboto medium (`TextAppearance.Eve.Telegram.NavigationTitle`) |
| Subtitle text size | `ActionBar.java:1451` (14sp) | Secondary text below bar | 14sp inside action bar (`TextAppearance.Eve.Telegram.NavigationSubtitle`) |
| Animated BackDrawable | `BackDrawable.java:1-180` (220ms DecelerateInterpolator arrow-to-X) | Static vector icon | Ported `com.eve.app.designsystem.BackDrawable` |
| Morphing MenuDrawable | `MenuDrawable.java:1-260` (200ms DecelerateInterpolator menu-to-arrow) | None | Ported `com.eve.app.designsystem.MenuDrawable` |
| LargeTitleBlock removal | Telegram has no iOS-style large titles | 28 activity layouts used `LargeTitleBlock` | 0 layouts use `LargeTitleBlock` (all 28 migrated to action bar) |

Notes / leftovers:
- SettingsActivity was migrated in pilot commit `d41bdc3b`.
- All 28 remaining layouts with `LargeTitleBlock` migrated to standard 56dp Telegram action bar.
- `TelegramActionBar` ported to `designsystem/TelegramActionBar.kt` with backward-compatibility subclass in `ui/admin/tg/`.
