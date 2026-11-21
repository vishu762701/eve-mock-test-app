# Telegram screenshot-to-source map

**Authoritative upstream:** `TelegramMessenger/Telegram-Android`
**Pinned revision:** `21f7d109da6ab3506c9e9577f98c6a38771ab335`
**App source version:** `13.2.2 (7303)`
**Archive identity:** see `docs/TELEGRAM_SOURCE_DISCOVERY.md`.

This map describes the three owner-supplied screenshots. The screenshots themselves are reference evidence, not image assets to place into EVE. Telegram constructs these screens dynamically from the source classes, resources, runtime state, and user data.

## 1. Settings screenshot

**Primary screen**
- `TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java` — original Settings screen; `SettingCell` is an inner implementation in this class, not a separate source file.
- `TMessagesProj/src/main/res/values/strings.xml` — contains the exact row labels/subtitles: Account, Chat Settings, Privacy & Security, Notifications, Data and Storage, Chat Folders, Devices, Power Saving, Language, Telegram Premium, and Telegram Stars.
- `TMessagesProj/src/main/res/drawable*/settings_*.{xml,webp,png}` — includes the original settings row icon resources (account, chat, privacy, sounds, data, folders, devices, power, language, premium, stars).

**Bottom tab bar visible in the screenshot**
- `TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java`
- `TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivityController.java`
- `TMessagesProj/src/main/java/org/telegram/ui/MainTabsLayout.java`
- `TMessagesProj/src/main/java/org/telegram/ui/Components/glass/GlassTabView.java`
- `TMessagesProj/src/main/java/org/telegram/ui/Components/glass/GlassTabsView.java`

## 2. Profile screenshot

**Primary screen**
- `TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java` — original profile screen, including profile header/actions and shared-media/story navigation.
- `TMessagesProj/src/main/java/org/telegram/ui/Stories/ProfileStoriesView.java` — profile story/post surface.
- `TMessagesProj/src/main/java/org/telegram/ui/Stories/StoriesController.java` and `StoryViewer.java` — story/post state and viewing behavior.
- `TMessagesProj/src/main/java/org/telegram/ui/Components/ImageUpdater.java` — profile photo camera/gallery/update flow.
- The profile source imports and uses the shared media/story components; their full dependency closure must be traced before implementation.

The pinned `strings.xml` contains `ProfileStories = Posts`, `ProfileArchivedStories = Archived Posts`, and `StoriesAddPost = Add a post`, matching the screenshot's post tabs and action label. Runtime text, account details, avatar, online status, channel preview, and post data are not hardcoded screenshot assets; they must be supplied by EVE-owned state/adapters.

## 3. QR-share screenshot

**Primary screen and renderer**
- `TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrShareActivity.java` — QR-share fragment and user/chat QR screen.
- `TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrShareView.java` — QR page rendering, controls, interactions, and motion.
- `TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrCodeLayer.java` — QR matrix rendering and animated code transitions.
- `TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrRender.java` and `QrGlass.java` — platform rendering, blend/effect, and glass behavior.

The full `org/telegram/ui/QrShare/` package contains 15 Java files and is the starting closure, not just the three files above:
`Backdrops.java`, `EmojiPickOverlay.java`, `QrBackdrops.java`, `QrCodeLayer.java`, `QrEmojiPanel.java`, `QrGiftSymbol.java`, `QrGlass.java`, `QrRender.java`, `QrScanOverlay.java`, `QrShareActivity.java`, `QrShareServer.java`, `QrShareView.java`, `QrStarDrawable.java`, `RimPainter.java`, and `RimTune.java`.
- `TMessagesProj/src/main/java/org/telegram/ui/CameraScanActivity.java` — scanner destination used by the Scan QR action; its required scanner/camera dependencies must be traced.

## 4. Shared host/framework required by all three

All three primary screen classes (`SettingsActivity`, `ProfileActivity`, and `QrShareActivity`) extend Telegram's `BaseFragment`; they are **not standalone Android Activities**. Exact integration therefore needs the relevant original fragment lifecycle, ActionBar, theme, navigation/container contracts, and tab-host behavior. The complete upstream app root is `LaunchActivity.java`; do not blindly import its whole account/chat/MTProto stack into EVE. Build the smallest host/adapter closure that can run the original fragments while keeping EVE authentication, data, navigation, and APIs in `com.eve.app`.

Shared source candidates to trace (do not copy blindly):
- `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BaseFragment.java`
- `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/INavigationLayout.java`
- `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBar.java`
- `TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java`
- `TMessagesProj/src/main/java/org/telegram/messenger/LocaleController.java`
- `TMessagesProj/src/main/java/org/telegram/ui/Components/RecyclerListView.java`
- `TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java` (reference for host contract, not an instruction to import the full Telegram application).

## 5. EVE integration boundary

- Keep Telegram's visible fragment/view hierarchy, geometry, drawing, touch dispatch, animations, and theme/resource behavior as the screen renderer.
- Replace only the account, profile, post, QR payload, permissions, and navigation data sources that must belong to EVE. Do this through explicit `com.eve.app` adapters and minimal documented `EVE-PATCH` changes.
- Do not import Telegram account credentials, MTProto, chats, or Telegram service APIs merely because an upstream screen references them. Trace the call site and map only the required behavior to an EVE operation.
- A copied source file that is not reachable from the active EVE navigation route is not an integrated screen.
- A screenshot-only clone or a Kotlin/XML/Compose redraw is not acceptable source-exact work.

## 6. Acceptance sequence

1. Establish the shared fragment/theme/host closure required by the three screens.
2. Integrate Settings plus the real bottom-tab surface as the first vertical slice.
3. Accept Settings only after its source renderer is active, every row/tab action is wired, and matched-device visual/motion/state checks pass.
4. Integrate and accept Profile (including Posts/Archived Posts and add-post flow).
5. Integrate and accept QR-share (including dynamic QR rendering, customization controls, Scan QR navigation, share/save behavior).
6. Keep any unverified item OPEN/BLOCKED. Do not call a screen complete from a screenshot or green compile alone.

**Reference privacy:** do not commit the supplied screenshots to Git. The profile reference contains personal account/phone information; redact private data before sharing additional captures with any external coding model. Use anonymized fixtures in repository tests and CI.
