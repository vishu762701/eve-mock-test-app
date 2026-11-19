#!/usr/bin/env python3
"""
EVE Port Gate Validator and Manifest Generator.
Enforces LAW 2, LAW 3, and Telegram Porting Rules (R1–R10).

Modes:
  --record --upstream <dir> [--allow-known-debt]:
      Analyzes ported screens against the official Telegram upstream tree,
      calculates SHA-256, kept lines, and originality percentage, and writes
      docs/port-manifest.json. Exits 1 if any non-debt file has originality < 95%.
  --check:
      CI verification mode (no upstream directory required).
      Verifies that all org/telegram files are registered, hashes are current,
      originality >= 95% (or recognized as known pilot debt), host Activities
      bind the Telegram entry points in AndroidManifest.xml, and dead layouts
      are purged.
"""

import argparse
import hashlib
import json
import os
import re
import sys
import subprocess
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = REPO_ROOT / "docs" / "port-manifest.json"

KNOWN_DEBT_ALLOWLIST = {
    "app/src/main/java/org/telegram/messenger/AndroidUtilities.java",
    "app/src/main/java/org/telegram/messenger/ApplicationLoader.java",
    "app/src/main/java/org/telegram/messenger/LocaleController.java",
    "app/src/main/java/org/telegram/messenger/R.java",
    "app/src/main/java/org/telegram/ui/ActionBar/ActionBar.java",
    "app/src/main/java/org/telegram/ui/ActionBar/ActionBarMenu.java",
    "app/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java",
    "app/src/main/java/org/telegram/ui/ActionBar/BaseFragment.java",
    "app/src/main/java/org/telegram/ui/ActionBar/INavigationLayout.java",
    "app/src/main/java/org/telegram/ui/ActionBar/Theme.java",
    "app/src/main/java/org/telegram/ui/ActionBar/ThemeDescription.java",
    "app/src/main/java/org/telegram/ui/ChangeNameActivity.java",
    "app/src/main/java/org/telegram/ui/Components/EditTextBoldCursor.java",
}

EVE_ORIGINAL_ALLOWLIST = {
    "app/src/main/java/org/telegram/ui/Components/RLottieNative.java": "not a Telegram class; native Lottie bridge for EVE"
}

# Registered screen specifications for --record
SCREEN_SPECS = [
    {
        "screen": "ChangeNameActivity",
        "eve_host": "com.eve.app.ui.profile.ChangeNameActivity",
        "telegram_entry": "org.telegram.ui.ChangeNameActivity",
        "dead_layout_files": [
            "app/src/main/res/layout/activity_change_name.xml"
        ],
        "resources": [
            {
                "path": "app/src/main/res/drawable-mdpi/calendar_date.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/calendar_date.9.png"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/calendar_date.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/calendar_date.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/calendar_date.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/calendar_date.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/calendar_date.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/calendar_date.9.png"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/fast_scroll_shadow.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/fast_scroll_shadow.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/fast_scroll_shadow.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/fast_scroll_shadow.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/fast_scroll_shadow.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/fast_scroll_shadow.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/fast_scroll_shadow.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/fast_scroll_shadow.webp"
            },
            {
                "path": "app/src/main/res/drawable/transparent.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/transparent.xml"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/greydivider_bottom.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/greydivider_bottom.9.png"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/greydivider_bottom.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/greydivider_bottom.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/greydivider_bottom.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/greydivider_bottom.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/greydivider_bottom.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/greydivider_bottom.9.png"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/greydivider_top.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/greydivider_top.9.png"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/greydivider_top.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/greydivider_top.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/greydivider_top.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/greydivider_top.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/greydivider_top.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/greydivider_top.9.png"
            },
            {
                "path": "app/src/main/res/drawable/field_carret_empty.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/field_carret_empty.xml"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/search_dark.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/search_dark.9.png"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/search_dark.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/search_dark.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/search_dark.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/search_dark.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/search_dark.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/search_dark.9.png"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/checkbig.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/checkbig.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/checkbig.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/checkbig.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/checkbig.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/checkbig.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/checkbig.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/checkbig.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/sheet_shadow_round.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/sheet_shadow_round.9.png"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/sheet_shadow_round.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/sheet_shadow_round.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/sheet_shadow_round.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/sheet_shadow_round.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/sheet_shadow_round.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/sheet_shadow_round.9.png"
            },
            {
                "path": "app/src/main/res/anim/no_animation.xml",
                "upstream": "TMessagesProj/src/main/res/anim/no_animation.xml"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/header_shadow.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/header_shadow.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/header_shadow.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/header_shadow.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/header_shadow.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/header_shadow.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/header_shadow.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/header_shadow.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/header_shadow_reverse.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/header_shadow_reverse.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/header_shadow_reverse.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/header_shadow_reverse.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/header_shadow_reverse.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/header_shadow_reverse.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/header_shadow_reverse.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/header_shadow_reverse.webp"
            },
            {
                "path": "app/src/main/res/anim/popup_in.xml",
                "upstream": "TMessagesProj/src/main/res/anim/popup_in.xml"
            },
            {
                "path": "app/src/main/res/anim/popup_out.xml",
                "upstream": "TMessagesProj/src/main/res/anim/popup_out.xml"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/popup_fixed_alert4.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/popup_fixed_alert4.9.png"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/popup_fixed_alert4.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/popup_fixed_alert4.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/popup_fixed_alert4.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/popup_fixed_alert4.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/popup_fixed_alert4.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/popup_fixed_alert4.9.png"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/msg_arrowright.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/msg_arrowright.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/msg_arrowright.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/msg_arrowright.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/msg_arrowright.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/msg_arrowright.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/msg_arrowright.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/msg_arrowright.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/ic_ab_back.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/ic_ab_back.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/ic_ab_done.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/ic_ab_done.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/ic_ab_other.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/ic_ab_other.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/ic_ab_back.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/ic_ab_back.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/ic_ab_done.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/ic_ab_done.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/ic_ab_other.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/ic_ab_other.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/ic_ab_back.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/ic_ab_back.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/ic_ab_done.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/ic_ab_done.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/ic_ab_other.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/ic_ab_other.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/ic_ab_back.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/ic_ab_back.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/ic_ab_done.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/ic_ab_done.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/ic_ab_other.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/ic_ab_other.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/popup_fixed_alert2.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/popup_fixed_alert2.9.png"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/popup_fixed_alert2.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/popup_fixed_alert2.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/popup_fixed_alert2.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/popup_fixed_alert2.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/popup_fixed_alert2.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/popup_fixed_alert2.9.png"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/greydivider.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/greydivider.9.png"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/greydivider.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/greydivider.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/greydivider.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/greydivider.9.png"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/greydivider.9.png",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/greydivider.9.png"
            }
        ],
        "files": [
            {
                "eve": "app/src/main/java/org/telegram/ui/ChangeNameActivity.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ChangeNameActivity.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ChangeNameActivity.cut",
                "removed_methods_reason": "Cut TLRPC/MessagesController/UserConfig saveName in favor of com.eve.app ChangeNameDelegate CUT-INTERFACE; preserved full Telegram ChangeNameActivity layout, ActionBar, EditTextBoldCursor fields, and animations"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/LayoutHelper.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/LayoutHelper.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CubicBezierInterpolator.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CubicBezierInterpolator.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/Easings.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/Easings.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/RLottieNative.java",
                "upstream": "",
                "mode": "EVE-ORIGINAL",
                "removed_methods_reason": "not a Telegram class; native Lottie bridge for EVE"
            },
            # Pilot foundation shims (known debt until full foundation port)
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/EditTextBoldCursor.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/EditTextBoldCursor.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/EditTextBoldCursor.cut",
                "removed_methods_reason": "Cut EditTextEffects spoiler base, Choreographer60FpsContent, FloatingToolbar/FloatingActionMode, Blur3, and QuoteSpan; preserved full bold cursor, hint/header-hint/error animations, underline physics, and handle coloring"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/Theme.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/ThemeDescription.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ThemeDescription.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ThemeDescription.cut",
                "removed_methods_reason": "Cut unported component instanceof branches (RecyclerListView, RadioButton, BackupImageView, etc.); preserved full ThemeDescription flags, ActionBar/TextView/SimpleTextView/ImageView/CombinedDrawable/View/Paint theming"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/INavigationLayout.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/INavigationLayout.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/INavigationLayout.cut",
                "removed_methods_reason": "Cut DrawerLayoutContainer, MessageDrawable, BackButtonMenu, ActionBarLayout factory, and BottomSheetTabs getters; preserved full INavigationLayout contract"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/BaseFragment.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BaseFragment.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/BaseFragment.cut",
                "removed_methods_reason": "Cut Telegram account/chat controller getters, StoryViewer, ArticleViewer, BotWebViewAttachedSheet, glass engine, and LaunchActivity refs; preserved full BaseFragment lifecycle, actionBar creation, theming, and dialog management"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/ActionBar.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBar.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ActionBar.cut",
                "removed_methods_reason": "Cut glass/blur3 overlay host, holiday effects, chat avatar container, and animated emoji spans; preserved full ActionBar layout, title/subtitle, action mode, search transitions, and back/menu animations"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/ActionBarMenu.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenu.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ActionBarMenu.cut",
                "removed_methods_reason": "Cut chat search filters and RLottieDrawable branch; preserved full menu item creation, lazy items, search field orchestration, and layout"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ActionBarMenuItem.cut",
                "removed_methods_reason": "Cut chat search filters, glass blur3, overlay menu host, swipe-back submenus, and RLottie; preserved full menu item, popup submenu, search field, and touch/animation physics"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/AndroidUtilities.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/LocaleController.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/LocaleController.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/R.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/R.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/ApplicationLoader.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/ThemeColors.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ThemeColors.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CombinedDrawable.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CombinedDrawable.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/BaseCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/BaseCell.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/TypefaceSpan.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/TypefaceSpan.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/BuildVars.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/DispatchQueue.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/DispatchQueue.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/DispatchQueue2.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/DispatchQueue2.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/FileLog.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/FileLog.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/BackDrawable.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BackDrawable.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/MenuDrawable.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/MenuDrawable.java",
                "mode": "CUT",
                "removed_methods_reason": "Cut unused chat download progress indicator (setUpdateDownloadProgress); preserved full hamburger to back rotation physics"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/EmptyStubSpan.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/EmptyStubSpan.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/StaticLayoutEx.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/StaticLayoutEx.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/StaticLayoutEx.cut"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/SimpleTextView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/SimpleTextView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/SimpleTextView.cut",
                "removed_methods_reason": "Cut chat spoiler effects and custom animated emoji spans; preserved full text layout, drawing, scrolling, gradient fade, and side drawables"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/ActionBarPopupWindow.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarPopupWindow.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ActionBarPopupWindow.cut",
                "removed_methods_reason": "Cut swipe-back submenus and notification locker during popup animation; preserved full popup layout, animations, clipping, and shadows"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuSubItem.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuSubItem.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ActionBarMenuSubItem.cut",
                "removed_methods_reason": "Cut animated lottie icons, network avatar image, and checkView; preserved full item titles, subtitles, normal icons, colors, selectors, RTL, and accessibility"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CloseProgressDrawable2.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CloseProgressDrawable2.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/utils/ViewOutlineProviderImpl.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/utils/ViewOutlineProviderImpl.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/AnimatedFloat.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/AnimatedFloat.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/ScaleStateListAnimator.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/ScaleStateListAnimator.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/RadialProgressView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/RadialProgressView.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CircularProgressDrawable.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CircularProgressDrawable.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/TextViewWithLoading.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/TextViewWithLoading.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/LineProgressView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/LineProgressView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/LineProgressView.cut",
                "removed_methods_reason": "Cut CellFlickerDrawable shimmer overlay; preserved full progress bar animation and drawing"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/AlertDialog.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/AlertDialog.cut",
                "removed_methods_reason": "Cut Stars balance cloud, RLottie top animations, NotificationCenter emoji hooks, Browser.Progress, and chat blur; preserved full AlertDialog layout, buttons, loading/spinner styles, scrolling, shadows, and Builder"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/AnimationProperties.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/AnimationProperties.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/AnimationProperties.cut",
                "removed_methods_reason": "Cut ImageReceiver, PhotoViewer, and DialogCell properties; preserved all standard View/Drawable/Paint animation properties"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/BottomSheet.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BottomSheet.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/BottomSheet.cut",
                "removed_methods_reason": "Cut glass overlay menu, NotificationCenter heavy-ops, CameraView/ItemOptions, Bulletin hooks, WindowVisibilityManager, and LaunchActivity refs; preserved full BottomSheet nested scroll, drag-to-dismiss physics, keyboard insets, cells, and Builder"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/XiaomiUtilities.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/XiaomiUtilities.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/GenericProvider.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/GenericProvider.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/ButtonBounce.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/ButtonBounce.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/Switch.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/Switch.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/RadioButton.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/RadioButton.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/DividerCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/DividerCell.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/ShadowSectionCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/ShadowSectionCell.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CheckBoxBase.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CheckBoxBase.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/CheckBoxBase.cut",
                "removed_methods_reason": "Cut chat MessageDrawable gradient shader; preserved full checkbox drawing and animations"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CheckBox2.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CheckBox2.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/CheckBox2.cut",
                "removed_methods_reason": "Direct drawable color filter without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/HeaderCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/HeaderCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/HeaderCell.cut",
                "removed_methods_reason": "Cut AnimatedTextView mode and ThemeBind; preserved full HeaderCell TextView, secondary text, RTL, and accessibility"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/TextInfoPrivacyCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/TextInfoPrivacyCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextInfoPrivacyCell.cut",
                "removed_methods_reason": "Use standard TextView without LinkSpanDrawable and ThemeBind; preserved full privacy footer layout, padding, links, and RTL"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/TextCheckCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/TextCheckCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextCheckCell.cut",
                "removed_methods_reason": "Cut AvatarSpan and RLottieImageView; preserved full Switch check cell layout, animations, divider, and RTL"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/TextDetailCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/TextDetailCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextDetailCell.cut",
                "removed_methods_reason": "Use standard TextView without SpoilersTextView/LinkSpanDrawable and cut UItem Factory; preserved full TextDetailCell layout, subtitles, divider, and RTL"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CodepointsLengthInputFilter.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CodepointsLengthInputFilter.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/NumberTextView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/NumberTextView.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/AnimatedTextView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/AnimatedTextView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/AnimatedTextView.cut",
                "removed_methods_reason": "Cut AnimatedEmojiSpan and AnimatedEmojiDrawable cache constant (5 lines); preserved 99%+ of upstream AnimatedTextView morphing text engine"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/TextCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/TextCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextCell.cut",
                "removed_methods_reason": "Cut RLottie, AnimatedEmojiDrawable stickers, PeerColor lock, and FilterCreate NewSpan; preserved full TextCell layout, AnimatedTextView value, subtitle, switch/checkbox, and RTL"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/RecyclerListView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/RecyclerListView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/RecyclerListView.cut",
                "removed_methods_reason": "Cut Blur3 capture/drawables, NotificationCenter heavy-ops, and chat-specific cell checks; preserved full RecyclerListView fast scroll, sections, selectors, animations, and touch physics"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/GestureDetectorFixDoubleTap.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/GestureDetectorFixDoubleTap.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/RecyclerItemsEnterAnimator.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/RecyclerItemsEnterAnimator.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/EdgeEffectTrackerFactory.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/EdgeEffectTrackerFactory.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/PhotoCropActivity.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/PhotoCropActivity.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/PhotoCropActivity.cut",
                "removed_methods_reason": "Decode bitmap via Eve ProfilePhotoManager CUT-INTERFACE without ImageLoader; preserved 98.8% of upstream PhotoCropActivity crop square, gesture physics, and ActionBar"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/ForegroundColorSpanThemable.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/ForegroundColorSpanThemable.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/TextRadioCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/TextRadioCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextRadioCell.cut",
                "removed_methods_reason": "Direct Theme color resolution without ThemeBind; preserved 98.1% of upstream TextRadioCell"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/LanguageCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/LanguageCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/LanguageCell.cut",
                "removed_methods_reason": "Direct Theme color resolution without ThemeBind; preserved 93.5% of upstream LanguageCell"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/EmptyTextProgressView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/EmptyTextProgressView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/EmptyTextProgressView.cut",
                "removed_methods_reason": "Direct Theme color resolution without ThemeBind; preserved 97.4% of upstream EmptyTextProgressView"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/URLSpanNoUnderline.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/URLSpanNoUnderline.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/URLSpanNoUnderline.cut",
                "removed_methods_reason": "Cut TLObject and Telegram Browser helper in favor of standard Android ACTION_VIEW Intent"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/TextSettingsCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/TextSettingsCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextSettingsCell.cut",
                "removed_methods_reason": "Cut BackupImageView/AnimatedEmojiDrawable and RLottieImageView; preserved 92.6% of upstream TextSettingsCell"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/Utilities.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/Utilities.java",
                "mode": "CUT"
            }
        ]
    },
    {
        "screen": "ChangeBioActivity",
        "eve_host": "com.eve.app.ui.profile.ChangeBioActivity",
        "telegram_entry": "org.telegram.ui.ChangeBioActivity",
        "dead_layout_files": [
            "app/src/main/res/layout/activity_change_bio.xml"
        ],
        "resources": [],
        "files": [
            {
                "eve": "app/src/main/java/org/telegram/ui/ChangeBioActivity.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ChangeBioActivity.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ChangeBioActivity.cut",
                "removed_methods_reason": "Cut TLRPC/MessagesController/UserConfig saveName in favor of com.eve.app ChangeBioDelegate CUT-INTERFACE; preserved full Telegram ChangeBioActivity layout, CodepointsLengthInputFilter, NumberTextView counter, shake/vibration physics, and ActionBar"
            }
        ]
    },
    {
        "screen": "LanguageSelectActivity",
        "eve_host": "com.eve.app.ui.settings.LanguageSelectActivity",
        "telegram_entry": "org.telegram.ui.LanguageSelectActivity",
        "dead_layout_files": [],
        "resources": [
            {
                "path": "app/src/main/res/drawable/outline_header_search.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_header_search.xml"
            },
            {
                "path": "app/src/main/res/drawable/ic_ab_close.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/ic_ab_close.xml"
            }
        ],
        "files": [
            {
                "eve": "app/src/main/java/org/telegram/ui/LanguageSelectActivity.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/LanguageSelectActivity.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/LanguageSelectActivity.cut",
                "removed_methods_reason": "Cut chat translation/premium rows (CUT-HIDDEN per 6c) and remote langpack sync; wired to com.eve.app TelegramLanguageDelegate while preserving real RecyclerListView, LanguageCell, ActionBar search, and EmptyTextProgressView"
            }
        ]
    },
    {
        "screen": "ChangeUsernameActivity",
        "eve_host": "com.eve.app.ui.profile.ChangeUsernameActivity",
        "telegram_entry": "org.telegram.ui.ChangeUsernameActivity",
        "dead_layout_files": [
            "app/src/main/res/layout/activity_change_username.xml"
        ],
        "resources": [],
        "files": [
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CornerPath.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CornerPath.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/LoadingDrawable.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/LoadingDrawable.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/LinkPath.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/LinkPath.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/LinkPath.cut",
                "removed_methods_reason": "Pruned unused LiteMode import"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/LinkSpanDrawable.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/LinkSpanDrawable.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/LinkSpanDrawable.cut",
                "removed_methods_reason": "Cut LiteMode, ArticleViewer, and AnimatedEmojiSpan hooks"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ChangeUsernameActivity.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ChangeUsernameActivity.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ChangeUsernameActivity.cut",
                "removed_methods_reason": "Cut Fragment collectible usernames (CUT-HIDDEN) and TLRPC account calls; wired availability check & save to Eve Worker API via TelegramUsernameDelegate"
            }
        ]
    },
    {
        "screen": "PasscodeActivity",
        "eve_host": "com.eve.app.ui.settings.PasscodeActivity",
        "telegram_entry": "org.telegram.ui.PasscodeActivity",
        "dead_layout_files": [],
        "resources": [
            {
                "path": "app/src/main/res/anim/alpha_in.xml",
                "upstream": "TMessagesProj/src/main/res/anim/alpha_in.xml"
            },
            {
                "path": "app/src/main/res/anim/alpha_out.xml",
                "upstream": "TMessagesProj/src/main/res/anim/alpha_out.xml"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/msg_clear_input.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/msg_clear_input.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/msg_clear_input.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/msg_clear_input.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/msg_clear_input.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/msg_clear_input.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/msg_clear_input.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/msg_clear_input.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/msg_pin_code.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/msg_pin_code.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/msg_pin_code.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/msg_pin_code.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/msg_pin_code.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/msg_pin_code.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/msg_pin_code.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/msg_pin_code.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/ic_ab_other.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/ic_ab_other.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/ic_ab_other.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/ic_ab_other.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/ic_ab_other.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/ic_ab_other.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/ic_ab_other.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/ic_ab_other.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/msg_message.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/msg_message.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/msg_message.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/msg_message.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/msg_message.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/msg_message.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/msg_message.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/msg_message.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/msg_permissions.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/msg_permissions.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/msg_permissions.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/msg_permissions.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/msg_permissions.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/msg_permissions.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/msg_permissions.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/msg_permissions.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/qr_flashlight.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/qr_flashlight.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/qr_flashlight.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/qr_flashlight.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/qr_flashlight.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/qr_flashlight.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/qr_flashlight.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/qr_flashlight.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/qr_gallery.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/qr_gallery.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/qr_gallery.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/qr_gallery.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/qr_gallery.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/qr_gallery.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/qr_gallery.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/qr_gallery.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/arrow_more.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/arrow_more.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/arrow_more.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/arrow_more.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/arrow_more.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/arrow_more.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/arrow_more.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/arrow_more.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/sticker_added.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/sticker_added.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/sticker_added.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/sticker_added.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/sticker_added.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/sticker_added.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/sticker_added.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/sticker_added.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/msg_brightness_low.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/msg_brightness_low.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/msg_brightness_low.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/msg_brightness_low.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/msg_brightness_low.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/msg_brightness_low.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/msg_brightness_low.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/msg_brightness_low.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/msg_brightness_high.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/msg_brightness_high.webp"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/msg_brightness_high.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/msg_brightness_high.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/msg_brightness_high.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/msg_brightness_high.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/msg_brightness_high.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/msg_brightness_high.webp"
            },
            {
                "path": "app/src/main/res/drawable/action_share.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/action_share.xml"
            },
            {
                "path": "app/src/main/res/drawable/filled_profile_call_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/filled_profile_call_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/filled_profile_edit_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/filled_profile_edit_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/filled_profile_member_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/filled_profile_member_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/filled_profile_message_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/filled_profile_message_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/filled_profile_mute_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/filled_profile_mute_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/filled_profile_photo.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/filled_profile_photo.xml"
            },
            {
                "path": "app/src/main/res/drawable/filled_profile_settings.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/filled_profile_settings.xml"
            },
            {
                "path": "app/src/main/res/drawable/filled_profile_stop_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/filled_profile_stop_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/filled_profile_story.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/filled_profile_story.xml"
            },
            {
                "path": "app/src/main/res/drawable/filled_profile_unmute_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/filled_profile_unmute_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/filled_profile_video_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/filled_profile_video_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/gift.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/gift.xml"
            },
            {
                "path": "app/src/main/res/drawable/input_gift_s.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/input_gift_s.xml"
            },
            {
                "path": "app/src/main/res/drawable/leave.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/leave.xml"
            },
            {
                "path": "app/src/main/res/drawable/live_stream.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/live_stream.xml"
            },
            {
                "path": "app/src/main/res/drawable/outline_profile_call_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_profile_call_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/outline_profile_edit_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_profile_edit_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/outline_profile_member_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_profile_member_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/outline_profile_message_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_profile_message_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/outline_profile_mute_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_profile_mute_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/outline_profile_photo.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_profile_photo.xml"
            },
            {
                "path": "app/src/main/res/drawable/outline_profile_settings.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_profile_settings.xml"
            },
            {
                "path": "app/src/main/res/drawable/outline_profile_stop_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_profile_stop_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/outline_profile_story.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_profile_story.xml"
            },
            {
                "path": "app/src/main/res/drawable/outline_profile_unmute_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_profile_unmute_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/outline_profile_video_24.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/outline_profile_video_24.xml"
            },
            {
                "path": "app/src/main/res/drawable/report.xml",
                "upstream": "TMessagesProj/src/main/res/drawable/report.xml"
            },
            {
                "path": "app/src/main/res/drawable-hdpi/mini_boost_button.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-hdpi/mini_boost_button.webp"
            },
            {
                "path": "app/src/main/res/drawable-mdpi/mini_boost_button.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-mdpi/mini_boost_button.webp"
            },
            {
                "path": "app/src/main/res/drawable-xhdpi/mini_boost_button.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xhdpi/mini_boost_button.webp"
            },
            {
                "path": "app/src/main/res/drawable-xxhdpi/mini_boost_button.webp",
                "upstream": "TMessagesProj/src/main/res/drawable-xxhdpi/mini_boost_button.webp"
            }
        ],
        "files": [
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/SimpleFloatPropertyCompat.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/SimpleFloatPropertyCompat.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/TextViewSwitcher.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/TextViewSwitcher.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/VerticalPositionAutoAnimator.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/VerticalPositionAutoAnimator.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/OutlineTextContainerView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/OutlineTextContainerView.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/TransformableLoginButtonView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/TransformableLoginButtonView.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/CodeNumberField.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/CodeNumberField.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/CodeNumberField.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/CodeFieldContainer.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/CodeFieldContainer.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/CodeFieldContainer.cut",
                "removed_methods_reason": "Cut LoginActivity missed-call branch and PhoneFormat dependency"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/Scroller.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/Scroller.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/SeekBarAccessibilityDelegate.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/SeekBarAccessibilityDelegate.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/NumberPicker.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/NumberPicker.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/NumberPicker.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CustomPhoneKeyboardView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CustomPhoneKeyboardView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/CustomPhoneKeyboardView.cut",
                "removed_methods_reason": "Direct Theme colors without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/FragmentFloatingButton.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/FragmentFloatingButton.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/FragmentFloatingButton.cut",
                "removed_methods_reason": "Cut blur3/glass sub-button decoration"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/PasscodeActivity.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/PasscodeActivity.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/PasscodeActivity.cut",
                "removed_methods_reason": "Cut GramWallet rows (CUT-HIDDEN), NotificationCenter, RLottie, and glass ActionBar; bridged SharedConfig state to Eve PasscodeManager"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/CameraScanActivity.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/CameraScanActivity.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/CameraScanActivity.cut",
                "removed_methods_reason": "Cut legacy CameraView/GMS Vision/ZXing/MRZ backend in favor of EveCameraHost CUT-INTERFACE so CameraX/ZXing stay Eve-side; preserved full Telegram BottomSheet scanner overlay, animated corner bounds, torch, and gallery chrome"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionIntroActivity.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionIntroActivity.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ActionIntroActivity.cut",
                "removed_methods_reason": "Cut LocationController, RLottie, CellFlickerDrawable, and TLRPC phone lookup; wired primary action callback for Eve intro flows"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/IntSeekBarAccessibilityDelegate.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/IntSeekBarAccessibilityDelegate.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/SlideChooseView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/SlideChooseView.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/RadioButtonCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/RadioButtonCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/RadioButtonCell.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/RadioColorCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/RadioColorCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/RadioColorCell.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/TextDetailSettingsCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/TextDetailSettingsCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextDetailSettingsCell.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind and plain text without Emoji"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/TextCheckCell2.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/TextCheckCell2.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextCheckCell2.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/TextCheckbox2Cell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/TextCheckbox2Cell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextCheckbox2Cell.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/QrShare/QrGlass.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrGlass.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/QrShare/QrRender.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrRender.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/QrShare/QrStarDrawable.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrStarDrawable.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/QrShare/RimPainter.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/QrShare/RimPainter.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/QrShare/RimTune.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/QrShare/RimTune.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/FloatSeekBarAccessibilityDelegate.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/FloatSeekBarAccessibilityDelegate.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/TextColorCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/TextColorCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextColorCell.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/ThemeTypeCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/ThemeTypeCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ThemeTypeCell.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/SeekBarView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/SeekBarView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/SeekBarView.cut",
                "removed_methods_reason": "Cut AudioPlayerAlert timestamp switcher and MessageObject chapter parser; preserved full interactive Telegram SeekBarView"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/BrightnessControlCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/BrightnessControlCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/BrightnessControlCell.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/TelegramQRCodeWriter.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/TelegramQRCodeWriter.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TelegramQRCodeWriter.cut",
                "removed_methods_reason": "Remove SvgHelper/R.raw.qr_logo fallback plane icon when centerDrawable is null"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/NotchInfoUtils.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/NotchInfoUtils.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/IconBackgroundColors.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/IconBackgroundColors.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CheckBoxSquare.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CheckBoxSquare.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/QrShare/QrCodeLayer.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/QrShare/QrCodeLayer.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/QrCodeLayer.cut",
                "removed_methods_reason": "Remove SharedConfig performance class guard while keeping full QrRender vertices support"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/ProfileGooeyView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileGooeyView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ProfileGooeyView.cut",
                "removed_methods_reason": "Remove SharedConfig performance class guard while keeping Android 12+ GPUImpl gooey physics"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/TextHelper.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/TextHelper.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextHelper.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/ListView/AdapterWithDiffUtils.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/ListView/AdapterWithDiffUtils.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/GradientTools.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/GradientTools.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/AvatarDrawable.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/AvatarDrawable.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/AvatarDrawable.cut",
                "removed_methods_reason": "Cut TLRPC/Chat/User/PeerColor overloads and Emoji/ThemeBind generation checks while preserving core setInfo(long, firstName, lastName), gradient colors, and Canvas drawing"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/Text.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/Text.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/Text.cut",
                "removed_methods_reason": "Remove AnimatedEmojiSpan/AnimatedEmojiDrawable hooks"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/ProfileActionsView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileActionsView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ProfileActionsView.cut",
                "removed_methods_reason": "Cut blur3 glassFactory (falls back cleanly to Telegram built-in action button color fill)"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/SimpleThemeDescription.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/SimpleThemeDescription.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/Loadable.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/Loadable.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Stories/recorder/ButtonWithCounterView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Stories/recorder/ButtonWithCounterView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/ButtonWithCounterView.cut",
                "removed_methods_reason": "Direct setBackground without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/SlideIntChooseView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/SlideIntChooseView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/SlideIntChooseView.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind and remove AnimatedEmojiDrawable cache type"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/NotificationsCheckCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/NotificationsCheckCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/NotificationsCheckCell.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CrossfadeDrawable.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CrossfadeDrawable.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/ProgressButton.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/ProgressButton.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/DialogRadioCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/DialogRadioCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/DialogRadioCell.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/TextRightIconCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/TextRightIconCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/TextRightIconCell.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/CollapseTextCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/CollapseTextCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/CollapseTextCell.cut",
                "removed_methods_reason": "Direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Cells/GraySectionCell.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Cells/GraySectionCell.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/GraySectionCell.cut",
                "removed_methods_reason": "Use TextView without AnimatedEmojiSpan and direct Theme.getColor without ThemeBind"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/UItem.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/UItem.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/UItem.cut",
                "removed_methods_reason": "Cut TLRPC/Business/Stats/Monetization factory overloads while preserving all core UItem list factories (asHeader, asShadow, asButton, asCheck, asRadio, asCustom, asSlideView, asIntSlideView)"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/ViewHelper.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/ViewHelper.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/RecyclerViewportFrameLayout.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/RecyclerViewportFrameLayout.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/Size.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/Size.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/ExtendedGridLayoutManager.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/ExtendedGridLayoutManager.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/UniversalAdapter.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/UniversalAdapter.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/UniversalAdapter.cut",
                "removed_methods_reason": "Cut Telegram-only Business/QuickReply/Charts/Monetization/DialogCell/UserCell cases and ThemeBind while preserving all core Settings/Profile UniversalAdapter view types"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/UniversalRecyclerView.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/UniversalRecyclerView.java",
                "mode": "CUT",
                "recipe": "docs/port-recipes/UniversalRecyclerView.cut",
                "removed_methods_reason": "Remove RichEditor.DraggingDrawable hook"
            }
        ]
    }
]


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    h.update(path.read_bytes())
    return h.hexdigest()


def normalize_code_line(line: str) -> str:
    line = line.strip()
    return re.sub(r"\s+", " ", line)


def is_visual_class(eve_path: str) -> bool:
    name = Path(eve_path).stem
    if name.startswith("ActionBar") or name.startswith("ActionBarMenu") or name == "EditTextBoldCursor":
        return True
    if name.endswith("Cell"):
        return True
    if "ui/Components" in eve_path:
        return True
    return False


def calculate_originality(eve_path: Path, upstream_path: Path) -> tuple[int, int, float]:
    """
    Returns (upstream_lines, kept_lines, originality_pct).
    originality_pct is the share of non-blank, non-comment eve lines
    (excluding // EVE-PATCH lines) that exist in the upstream file.
    """
    if not upstream_path or not upstream_path.is_file():
        eve_lines = len(eve_path.read_text(encoding="utf-8").splitlines())
        return 0, eve_lines, 0.0

    up_raw = upstream_path.read_text(encoding="utf-8").splitlines()
    eve_raw = eve_path.read_text(encoding="utf-8").splitlines()

    up_lines = set()
    for l in up_raw:
        s = l.strip()
        if not s or s.startswith("//") or s.startswith("/*") or s.startswith("*") or s.startswith("*/"):
            continue
        up_lines.add(normalize_code_line(l))

    evaluated_eve = []
    matched_eve = []
    in_patch_block = False
    for l in eve_raw:
        s = l.strip()
        if not s:
            continue
        if "// EVE-PATCH: BEGIN" in l:
            in_patch_block = True
            continue
        if "// EVE-PATCH: END" in l:
            in_patch_block = False
            continue
        if in_patch_block or "// EVE-PATCH" in l:
            continue
        if s.startswith("//") or s.startswith("/*") or s.startswith("*") or s.startswith("*/"):
            continue
        norm = normalize_code_line(l)
        evaluated_eve.append(norm)
        if norm in up_lines:
            matched_eve.append(norm)

    total_eval = len(evaluated_eve)
    matched = len(matched_eve)
    pct = round((matched / total_eval) * 100.0, 2) if total_eval > 0 else 100.0
    return len(up_raw), len(eve_raw), pct


def record_manifest(upstream_root: Path, output_path: Path, allow_known_debt: bool) -> int:
    pinned_rev = "21f7d109da6ab3506c9e9577f98c6a38771ab335"
    upstream_repo = "https://github.com/TelegramMessenger/Telegram-Android"

    failed_files = []
    manifest_screens = []

    for spec in SCREEN_SPECS:
        screen_entry = {
            "screen": spec["screen"],
            "eve_host": spec["eve_host"],
            "telegram_entry": spec["telegram_entry"],
            "dead_layout_files": spec.get("dead_layout_files", []),
            "files": []
        }

        for fspec in spec["files"]:
            eve_rel = fspec["eve"]
            mode = fspec["mode"]
            eve_file = REPO_ROOT / eve_rel

            if not eve_file.is_file():
                print(f"ERROR: Missing local file {eve_rel}", file=sys.stderr)
                return 1

            eve_hash = sha256_file(eve_file)
            up_rel = fspec.get("upstream", "")
            up_file = (upstream_root / up_rel) if up_rel else None

            up_hash = sha256_file(up_file) if (up_file and up_file.is_file()) else ""
            up_lines, kept_lines, orig_pct = 0, 0, 0.0

            if mode == "VERBATIM":
                if not up_file or not up_file.is_file():
                    print(f"ERROR: Upstream file missing for VERBATIM {eve_rel}: {up_rel}", file=sys.stderr)
                    return 1
                if eve_hash != up_hash:
                    print(f"ERROR: Hash mismatch for VERBATIM {eve_rel} vs {up_rel}", file=sys.stderr)
                    return 1
                up_lines = len(up_file.read_text(encoding="utf-8").splitlines())
                kept_lines = len(eve_file.read_text(encoding="utf-8").splitlines())
                orig_pct = 100.0
                is_known = False

            elif mode == "EVE-ORIGINAL":
                kept_lines = len(eve_file.read_text(encoding="utf-8").splitlines())
                orig_pct = 100.0
                is_known = False

            else:  # CUT
                up_lines, kept_lines, orig_pct = calculate_originality(eve_file, up_file)
                is_known = eve_rel in KNOWN_DEBT_ALLOWLIST

                if orig_pct < 95.0:
                    if not (allow_known_debt and is_known):
                        failed_files.append((eve_rel, orig_pct, "originality < 95.0%"))

            recipe_path = fspec.get("recipe_path", "")
            if not recipe_path and (REPO_ROOT / f"docs/port-recipes/{Path(eve_rel).stem}.cut").is_file():
                recipe_path = f"docs/port-recipes/{Path(eve_rel).stem}.cut"

            recipe_sha = sha256_file(REPO_ROOT / recipe_path) if (recipe_path and (REPO_ROOT / recipe_path).is_file()) else ""

            file_entry = {
                "eve": eve_rel,
                "upstream": up_rel,
                "mode": mode,
                "recipe_path": recipe_path,
                "recipe_sha256": recipe_sha,
                "result_sha256": eve_hash,
                "eve_sha256": eve_hash,
                "upstream_sha256": up_hash,
                "upstream_lines": up_lines,
                "kept_lines": kept_lines,
                "originality_pct": orig_pct,
                "removed_methods_reason": fspec.get("removed_methods_reason", ""),
                "known_debt": is_known if mode == "CUT" else False
            }
            screen_entry["files"].append(file_entry)

        screen_entry["resources"] = []
        for rspec in spec.get("resources", []):
            r_path = rspec["path"]
            r_up = rspec["upstream"]
            local_res = REPO_ROOT / r_path
            if not local_res.is_file():
                failed_files.append((r_path, 0.0, "Resource file missing on disk"))
                continue
            r_hash = sha256_file(local_res)
            if upstream_root:
                up_res = upstream_root / r_up
                if not up_res.is_file():
                    failed_files.append((r_path, 0.0, f"Upstream resource missing: {r_up}"))
                    continue
                up_hash = sha256_file(up_res)
                if r_hash != up_hash:
                    failed_files.append((r_path, 0.0, f"Resource byte mismatch with upstream: {r_path}"))
                    continue
            screen_entry["resources"].append({
                "path": r_path,
                "upstream": r_up,
                "sha256": r_hash
            })

        manifest_screens.append(screen_entry)

    if failed_files:
        print("PORT GATE RECORD FAILED:", file=sys.stderr)
        for path, pct, reason in failed_files:
            print(f"  - {path}: {pct:.2f}% ({reason})", file=sys.stderr)
        print("Run with --allow-known-debt to record known pilot debt.", file=sys.stderr)
        return 1

    manifest_data = {
        "version": 1,
        "pinned_revision": pinned_rev,
        "upstream_repo": upstream_repo,
        "known_debt_allowlist": sorted(list(KNOWN_DEBT_ALLOWLIST)),
        "screens": manifest_screens
    }

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(manifest_data, indent=2) + "\n", encoding="utf-8")
    print(f"Successfully recorded port manifest to {output_path}")
    return 0


def check_manifest(manifest_path: Path, upstream_path: Path | None = None) -> int:
    if not manifest_path.is_file():
        print(f"ERROR: Manifest file not found: {manifest_path}", file=sys.stderr)
        return 1

    try:
        data = json.loads(manifest_path.read_text(encoding="utf-8"))
    except Exception as e:
        print(f"ERROR: Corrupt manifest JSON: {e}", file=sys.stderr)
        return 1

    errors = []
    warnings = []

    # 1. Check all disk files under org/telegram are registered
    tg_dir = REPO_ROOT / "app" / "src" / "main" / "java" / "org" / "telegram"
    disk_files = set()
    if tg_dir.is_dir():
        for f in tg_dir.rglob("*.java"):
            disk_files.add(str(f.relative_to(REPO_ROOT)))
        for f in tg_dir.rglob("*.kt"):
            disk_files.add(str(f.relative_to(REPO_ROOT)))

    manifest_files = set()
    screens = data.get("screens", [])
    known_debt_allowlist = set(data.get("known_debt_allowlist", []))

    manifest_path_map = {}

    for screen in screens:
        screen_name = screen.get("screen", "Unknown")
        eve_host = screen.get("eve_host", "")
        telegram_entry = screen.get("telegram_entry", "")

        # Check host Activity
        host_parts = eve_host.split(".")
        host_rel = f"app/src/main/java/{'/'.join(host_parts)}.kt"
        host_file = REPO_ROOT / host_rel
        if not host_file.is_file():
            host_rel_java = f"app/src/main/java/{'/'.join(host_parts)}.java"
            host_file = REPO_ROOT / host_rel_java
            if not host_file.is_file():
                errors.append(f"Screen {screen_name}: Host activity file missing: {host_rel}")
            else:
                host_rel = host_rel_java

        if host_file.is_file():
            host_content = host_file.read_text(encoding="utf-8")
            entry_simple = telegram_entry.split(".")[-1]
            if telegram_entry not in host_content and entry_simple not in host_content:
                errors.append(
                    f"Screen {screen_name}: Host {host_rel} does not import or instantiate {telegram_entry}"
                )

        # Check AndroidManifest.xml
        manifest_xml = REPO_ROOT / "app" / "src" / "main" / "AndroidManifest.xml"
        if manifest_xml.is_file():
            xml_content = manifest_xml.read_text(encoding="utf-8")
            host_simple = host_parts[-1]
            if f".{host_simple}" not in xml_content and eve_host not in xml_content:
                errors.append(
                    f"Screen {screen_name}: Host {eve_host} is not registered in AndroidManifest.xml"
                )
        else:
            errors.append("AndroidManifest.xml missing")

        # Check dead layouts
        for dead_rel in screen.get("dead_layout_files", []):
            if (REPO_ROOT / dead_rel).is_file():
                errors.append(
                    f"Screen {screen_name}: Dead layout file still exists: {dead_rel}"
                )

        # Check screen files
        for f in screen.get("files", []):
            eve_rel = f.get("eve", "")
            manifest_files.add(eve_rel)
            manifest_path_map[eve_rel] = f

            local_path = REPO_ROOT / eve_rel
            if not local_path.is_file():
                errors.append(f"Manifest file missing on disk: {eve_rel}")
                continue

            current_hash = sha256_file(local_path)
            expected_hash = f.get("eve_sha256", "")
            result_hash = f.get("result_sha256", expected_hash)
            if current_hash != expected_hash:
                errors.append(f"Stale hash for {eve_rel}: manifest={expected_hash[:8]} vs disk={current_hash[:8]}")
            if result_hash and current_hash != result_hash:
                errors.append(f"Result sha256 mismatch for {eve_rel}: manifest result={result_hash[:8]} vs disk={current_hash[:8]}")

            mode = f.get("mode", "")
            orig_pct = f.get("originality_pct", 0.0)
            is_debt = f.get("known_debt", False)
            recipe_path = f.get("recipe_path", "")
            recipe_sha = f.get("recipe_sha256", "")

            if mode == "CUT":
                if not is_debt:
                    if not recipe_path:
                        errors.append(f"mode CUT requires recipe under docs/port-recipes/ for {eve_rel}")
                    elif not recipe_path.startswith("docs/port-recipes/"):
                        errors.append(f"recipe_path must be located under docs/port-recipes/: {recipe_path}")
                    else:
                        r_file = REPO_ROOT / recipe_path
                        if not r_file.is_file():
                            errors.append(f"Recipe file missing on disk: {recipe_path}")
                        else:
                            r_hash = sha256_file(r_file)
                            if r_hash != recipe_sha:
                                errors.append(f"Recipe sha256 mismatch for {recipe_path}: disk={r_hash[:8]} vs manifest={recipe_sha[:8]}")
                else:
                    if recipe_path:
                        r_file = REPO_ROOT / recipe_path
                        if not r_file.is_file():
                            errors.append(f"Recipe file missing on disk: {recipe_path}")
                        else:
                            r_hash = sha256_file(r_file)
                            if r_hash != recipe_sha:
                                errors.append(f"Recipe sha256 mismatch for {recipe_path}: disk={r_hash[:8]} vs manifest={recipe_sha[:8]}")

            if is_debt:
                if eve_rel not in known_debt_allowlist:
                    errors.append(f"File marked known_debt is not in allowlist: {eve_rel}")
                else:
                    warnings.append(f"WARN-KNOWN: {eve_rel} (originality {orig_pct}%)")
            else:
                if mode == "CUT" and orig_pct < 95.0:
                    errors.append(f"Originality below 95.0% for {eve_rel}: {orig_pct}%")

            if mode == "EVE-ORIGINAL":
                reason = f.get("removed_methods_reason", "")
                if eve_rel not in EVE_ORIGINAL_ALLOWLIST and not reason:
                    errors.append(f"EVE-ORIGINAL file requires an allowlist reason: {eve_rel}")

            # Visual class ratio check (< 50% lines kept without reason)
            if is_visual_class(eve_rel):
                up_lines = f.get("upstream_lines", 0)
                kept_lines = f.get("kept_lines", 0)
                reason = f.get("removed_methods_reason", "")
                if up_lines > 0 and (kept_lines / up_lines) < 0.50 and not reason and not is_debt:
                    ratio = (kept_lines / up_lines) * 100.0
                    errors.append(
                        f"Visual class {eve_rel} kept ratio < 50% ({ratio:.1f}%) with no removed_methods_reason"
                    )

    # Optional rebuild check if upstream path provided
    if upstream_path:
        if not upstream_path.is_dir():
            errors.append(f"Specified upstream directory not found: {upstream_path}")
        else:
            tgcut_tool = Path.home() / "tg-tools" / "tgcut.py"
            if not tgcut_tool.is_file():
                errors.append(f"Tool tgcut.py not found at {tgcut_tool}")
            else:
                import tempfile
                with tempfile.TemporaryDirectory() as tmpdir:
                    for s in screens:
                        for f in s.get("files", []):
                            r_path = f.get("recipe_path", "")
                            if r_path and (REPO_ROOT / r_path).is_file():
                                res = subprocess.run(
                                    [sys.executable, str(tgcut_tool), "build", str(REPO_ROOT / r_path), "--repo", tmpdir, "--src", str(upstream_path)],
                                    capture_output=True,
                                    text=True
                                )
                                if res.returncode != 0:
                                    errors.append(f"tgcut build failed for {r_path}: {res.stderr}\n{res.stdout}")
                                    continue
                                built_file = Path(tmpdir) / f["eve"]
                                if not built_file.is_file():
                                    errors.append(f"tgcut build output missing for {f['eve']}")
                                    continue
                                if sha256_file(built_file) != sha256_file(REPO_ROOT / f["eve"]):
                                    errors.append(f"tgcut rebuild byte mismatch for {f['eve']}")

        # Check resources
        for r in screen.get("resources", []):
            r_path = r.get("path", "")
            r_up = r.get("upstream", "")
            r_expected_hash = r.get("sha256", "")

            local_res = REPO_ROOT / r_path
            if not local_res.is_file():
                errors.append(f"Screen {screen_name}: Manifest resource missing on disk: {r_path}")
                continue

            current_r_hash = sha256_file(local_res)
            if current_r_hash != r_expected_hash:
                errors.append(f"Screen {screen_name}: Stale resource hash for {r_path}: manifest={r_expected_hash[:8]} vs disk={current_r_hash[:8]}")

            if upstream_path:
                up_res = upstream_path / r_up
                if not up_res.is_file():
                    errors.append(f"Screen {screen_name}: Upstream resource missing: {r_up}")
                else:
                    up_r_hash = sha256_file(up_res)
                    if current_r_hash != up_r_hash:
                        errors.append(f"Screen {screen_name}: Resource differs from upstream sha: {r_path}")

    # Check for unmanifested files
    unregistered = disk_files - manifest_files
    for unreg in sorted(unregistered):
        errors.append(f"Unregistered file under app/src/main/java/org/telegram: {unreg}")

    # Print results
    for w in warnings:
        print(f"  {w}")

    if errors:
        print("PORT GATE CHECK FAILED:", file=sys.stderr)
        for e in errors:
            print(f"  ERROR: {e}", file=sys.stderr)
        return 1

    print(f"PORT GATE CHECK PASSED: {len(screens)} screens, {len(manifest_files)} files verified.")
    return 0


def get_git_changed_files(diff_ref: str = None) -> list[str]:
    import subprocess
    cmd_candidates = []
    if diff_ref:
        cmd_candidates.append(["git", "diff", "--name-only", diff_ref])
    else:
        before = os.environ.get("GITHUB_EVENT_BEFORE")
        sha = os.environ.get("GITHUB_SHA")
        if before and sha and before != "0000000000000000000000000000000000000000":
            cmd_candidates.append(["git", "diff", "--name-only", f"{before}..{sha}"])

        cmd_candidates.append(["git", "diff", "--name-only", "HEAD~1..HEAD"])
        cmd_candidates.append(["git", "diff", "--name-only", "HEAD~1"])
        cmd_candidates.append(["git", "diff", "--name-only", "HEAD"])
        cmd_candidates.append(["git", "status", "--porcelain"])

    for cmd in cmd_candidates:
        try:
            res = subprocess.run(
                cmd,
                cwd=REPO_ROOT,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                check=True
            )
            files = []
            for line in res.stdout.splitlines():
                line = line.strip()
                if not line:
                    continue
                if len(line) > 3 and line[2] == " ":
                    files.append(line[3:].strip())
                else:
                    files.append(line)
            if diff_ref is not None or files:
                return files
        except Exception:
            continue
    return []


def load_manifest(manifest_path: Path) -> dict:
    if not manifest_path.is_file():
        return {}
    try:
        return json.loads(manifest_path.read_text(encoding="utf-8"))
    except Exception:
        return {}


def check_should_run_smoke(manifest_path: Path, diff_ref: str = None) -> int:
    manifest = load_manifest(manifest_path)
    host_activity_files = set()
    for s in manifest.get("screens", []):
        eve_host = s.get("eve_host", "")
        if eve_host:
            rel = "app/src/main/java/" + eve_host.replace(".", "/")
            host_activity_files.add(rel + ".kt")
            host_activity_files.add(rel + ".java")
            cls_name = eve_host.split(".")[-1]
            host_activity_files.add(f"{cls_name}.kt")
            host_activity_files.add(f"{cls_name}.java")

    changed_files = get_git_changed_files(diff_ref)
    if not changed_files:
        if diff_ref:
            print("PORT SMOKE CHECK: No changed files detected for diff-ref; skipping.")
            return 1
        print("PORT SMOKE CHECK: No changed files detected or git diff unavailable; defaulting to RUN.")
        return 0

    triggered = []
    for f in changed_files:
        norm = f.replace("\\", "/")
        if norm.startswith("app/src/main/java/org/telegram/"):
            triggered.append(f)
        elif norm == "docs/port-manifest.json":
            triggered.append(f)
        elif any(norm.endswith(act) or norm == act for act in host_activity_files):
            triggered.append(f)

    if triggered:
        print(f"PORT SMOKE CHECK: Triggered by {len(triggered)} relevant file(s):")
        for t in triggered:
            print(f"  - {t}")
        return 0
    else:
        print("PORT SMOKE CHECK: No org/telegram, manifest, or host Activity changes.")
        return 1


def main():
    parser = argparse.ArgumentParser(description="EVE Port Gate tool")
    parser.add_argument("--record", action="store_true", help="Record manifest against upstream source")
    parser.add_argument("--upstream", type=str, help="Path to upstream Telegram source directory")
    parser.add_argument("--manifest", type=str, default=str(DEFAULT_MANIFEST), help="Path to port-manifest.json")
    parser.add_argument("--check", action="store_true", help="Verify repository against manifest")
    parser.add_argument("--allow-known-debt", action="store_true", help="Allow known pilot debt during record")
    parser.add_argument("--should-run-smoke", action="store_true", help="Check whether port-smoke test is triggered by git diff")
    parser.add_argument("--diff-ref", type=str, default=None, help="Base ref for git diff check")

    args = parser.parse_args()
    manifest_path = Path(args.manifest)

    if args.record:
        if not args.upstream:
            print("ERROR: --upstream <dir> is required with --record", file=sys.stderr)
            return 1
        upstream_path = Path(args.upstream).resolve()
        if not upstream_path.is_dir():
            print(f"ERROR: Upstream directory not found: {upstream_path}", file=sys.stderr)
            return 1
        return record_manifest(upstream_path, manifest_path, args.allow_known_debt)

    elif args.check:
        upstream_path = Path(args.upstream).resolve() if args.upstream else None
        return check_manifest(manifest_path, upstream_path)

    elif args.should_run_smoke:
        return check_should_run_smoke(manifest_path, args.diff_ref)

    else:
        parser.print_help()
        return 1


if __name__ == "__main__":
    sys.exit(main())
