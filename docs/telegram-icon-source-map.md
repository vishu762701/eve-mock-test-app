# Telegram icon source mapping — current EVE

Source availability: Telegram source is not stored in this repository; the owner supplies it per session. See `SOURCE_OF_TRUTH.md` for attachment/hash requirements and `AGENTS.md` for the missing-source STOP rule.

Source: `docs/telegram-icon-source-registry.tsv` and original-byte Telegram UI source evidence archive pinned to SHA-256 `fabf0b7370abf2ee5661063ea1c274cb3e537048f008a6871ff3169cbe89cb1e`.

Existing imported source-exact icons are already verified by `TelegramIconSourceRegistryTest` (per-density SHA-256). Never re-encode or redraw these WEBP files.

Historical closest-meaning candidates with source hashes are in `docs/telegram-icon-candidates.md`; remaining non-imported candidates require source/use-site compatibility review.

Current registry: **74** icons; **46** source-exact, **28** not currently source-exact.

Home toolbar notification fallback now references the already-imported `ic_bell` mapped to Telegram `msg_notifications.webp`. The Android manifest status icon `ic_notification` is intentionally retained; notification OS small-icon constraints differ. Lottie was not changed.

| EVE icon | Source / nearest candidate | Status / action |
|---|---|---|
| `ic_account_balance` | `NONE` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_add` | `res/drawable-hdpi/msg_add.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_analytics` | `res/drawable-hdpi/msg_stats.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_back` | `res/drawable-hdpi/ic_ab_back.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_bell` | `res/drawable-hdpi/msg_notifications.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_bell_off` | `res/drawable-hdpi/msg_mute.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_bookmark` | `res/drawable-hdpi/msg_saved.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_broadcast` | `res/drawable-hdpi/msg_channel.webp` | IMPORTED ORIGINAL WEBP; owner-approved MEDIUM |
| `ic_bug_report` | `msg_report.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_calendar` | `res/drawable-hdpi/msg_calendar.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_camera` | `res/drawable-hdpi/msg_camera.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_campaign` | `msg_share.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_check_circle` | `list_check.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_chevron_down` | `res/drawable-hdpi/msg_go_down.webp` | IMPORTED ORIGINAL WEBP; owner-approved MEDIUM |
| `ic_chevron_right` | `res/drawable-hdpi/msg_arrowright.webp` | ALREADY IMPORTED ORIGINAL WEBP |
| `ic_close` | `res/drawable-hdpi/msg_close.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_copy` | `res/drawable-hdpi/msg_copy.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_delete` | `res/drawable-hdpi/msg_delete.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_description` | `msg_filehq.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_download` | `res/drawable-hdpi/msg_download.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_edit` | `res/drawable-hdpi/msg_edit.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_email` | `res/drawable-hdpi/msg2_email.webp` | ALREADY IMPORTED ORIGINAL WEBP |
| `ic_exam_placeholder` | `msg_filehq.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_flag` | `msg_report.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_flame_line` | `filled_fire.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_history` | `res/drawable-hdpi/msg_archive.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_history_rounded` | `res/drawable-hdpi/msg_recent.webp` | ALREADY IMPORTED ORIGINAL WEBP |
| `ic_image` | `msg_addphoto.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_info` | `res/drawable-hdpi/msg_info.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_key` | `res/drawable-hdpi/msg_secret.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_language` | `res/drawable-hdpi/msg_language.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_leaderboard_star` | `ic_rating_star.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_link` | `res/drawable-hdpi/msg_link.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_logout` | `res/drawable-hdpi/msg_leave.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_maintenance` | `msg_settings.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_message_dots` | `profile_discuss.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_minus` | `res/drawable-hdpi/msg_remove.webp` | IMPORTED ORIGINAL WEBP; owner-approved MEDIUM |
| `ic_more_vert` | `res/drawable-hdpi/ic_ab_other.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_notification` | `msg_notifications.webp (not suitable for status-bar use)` | NO EXACT COUNTERPART — approval required; EVE_ORIGINAL_REDRAW_NOT_SOURCE_EXACT |
| `ic_performance_increase` | `msg_stats.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_person` | `res/drawable-hdpi/msg_openprofile.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_phone` | `res/drawable-hdpi/msg_calls.webp` | ALREADY IMPORTED ORIGINAL WEBP |
| `ic_pin` | `res/drawable-hdpi/msg_pin.webp` | IMPORTED ORIGINAL WEBP; owner-approved MEDIUM |
| `ic_pyq` | `msg_filehq.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_qr_code` | `res/drawable-hdpi/msg_qrcode.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_refresh` | `res/drawable-hdpi/msg_retry.webp` | ALREADY IMPORTED ORIGINAL WEBP |
| `ic_report_flag_premium` | `res/drawable-hdpi/msg_report.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_robot` | `res/drawable-hdpi/msg_bot.webp` | IMPORTED ORIGINAL WEBP; owner-approved MEDIUM |
| `ic_save` | `msg_saved.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_search` | `res/drawable-hdpi/msg_search.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_send` | `res/drawable-hdpi/msg_send.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_settings` | `res/drawable-hdpi/msg_settings.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_share` | `res/drawable-hdpi/msg_share.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_social_google` | `NONE (Google provider branding)` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_star_bookmark_filled` | `ic_rating_star_filled.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_star_filled` | `res/drawable-hdpi/ic_rating_star_filled.webp` | IMPORTED ORIGINAL WEBP; owner-approved MEDIUM |
| `ic_star_outline` | `res/drawable-hdpi/ic_rating_star.webp` | IMPORTED ORIGINAL WEBP; owner-approved MEDIUM |
| `ic_status_circle_right` | `list_check.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_status_circle_unattempted` | `NONE` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_status_circle_wrong` | `msg_warning.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_syllabus` | `msg_filehq.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_theme_moon` | `NONE` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_theme_sun` | `res/drawable-hdpi/msg_brightness_high.webp` | IMPORTED ORIGINAL WEBP; owner-approved MEDIUM |
| `ic_timer` | `msg_stories_timer.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_topic` | `msg_topics.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_trophy` | `ic_rating_star_filled.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_upload` | `res/drawable-hdpi/msg_mini_upload.webp` | IMPORTED ORIGINAL WEBP; owner-approved MEDIUM |
| `ic_user_profile` | `res/drawable-hdpi/msg_openprofile.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_users` | `res/drawable-hdpi/msg_groups.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_visibility` | `intro_ic_smile_eye.webp (not semantic equivalent)` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_warning` | `msg_warning.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `msg_home` | `res/drawable-hdpi/msg_home.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `msg_theme` | `res/drawable-hdpi/msg_theme.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `msg_palette` | `res/drawable-hdpi/msg_palette.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |

Candidate suggestions in this table are **not** approved replacements. Their names are from source drawable resources; a similar name does not prove visual or semantic equivalence. No changes to exam-specific art or Lottie.
