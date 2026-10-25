# Telegram icon source mapping — current EVE

Source availability: Telegram source is not stored in this repository; the owner supplies it per session. See `SOURCE_OF_TRUTH.md` for attachment/hash requirements and `AGENTS.md` for the missing-source STOP rule.

Source: `docs/telegram-icon-source-registry.tsv` and original-byte Telegram UI source evidence archive pinned to SHA-256 `fabf0b7370abf2ee5661063ea1c274cb3e537048f008a6871ff3169cbe89cb1e`.

Existing imported source-exact icons are already verified by `TelegramIconSourceRegistryTest` (per-density SHA-256). Never re-encode or redraw these WEBP files.

Review-only closest-meaning candidates with source hashes are in `docs/telegram-icon-candidates.md`; import approval is still required.

Current registry: **74** icons; **32** already source-exact, **42** not currently source-exact.

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
| `ic_broadcast` | `msg_share.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_bug_report` | `msg_report.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_calendar` | `res/drawable-hdpi/msg_calendar.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_camera` | `res/drawable-hdpi/msg_camera.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_campaign` | `msg_share.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_check_circle` | `list_check.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_chevron_down` | `NONE (procedural)` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_chevron_right` | `NONE (procedural)` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_close` | `res/drawable-hdpi/msg_close.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_copy` | `res/drawable-hdpi/msg_copy.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_delete` | `res/drawable-hdpi/msg_delete.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_description` | `msg_filehq.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_download` | `res/drawable-hdpi/msg_download.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_edit` | `res/drawable-hdpi/msg_edit.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_email` | `msg2_email.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_exam_placeholder` | `msg_filehq.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_flag` | `msg_report.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_flame_line` | `filled_fire.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_history` | `res/drawable-hdpi/msg_archive.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_history_rounded` | `msg_archive.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_image` | `msg_addphoto.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_info` | `res/drawable-hdpi/msg_info.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_key` | `res/drawable-hdpi/msg_secret.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_language` | `res/drawable-hdpi/msg_language.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_leaderboard_star` | `ic_rating_star.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_link` | `res/drawable-hdpi/msg_link.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_logout` | `res/drawable-hdpi/msg_leave.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_maintenance` | `msg_settings.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_message_dots` | `profile_discuss.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_minus` | `NONE` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_more_vert` | `res/drawable-hdpi/ic_ab_other.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_notification` | `msg_notifications.webp (not suitable for status-bar use)` | NO EXACT COUNTERPART — approval required; EVE_ORIGINAL_REDRAW_NOT_SOURCE_EXACT |
| `ic_performance_increase` | `msg_stats.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_person` | `res/drawable-hdpi/msg_openprofile.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_phone` | `profile_phone.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_pin` | `chats_pin.webp (semantics require review)` | NO EXACT COUNTERPART — approval required; EVE_ORIGINAL_REDRAW_NOT_SOURCE_EXACT |
| `ic_pyq` | `msg_filehq.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_qr_code` | `res/drawable-hdpi/msg_qrcode.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_refresh` | `msg_retry.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_report_flag_premium` | `res/drawable-hdpi/msg_report.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_robot` | `msg_bots.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_save` | `msg_saved.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_search` | `res/drawable-hdpi/msg_search.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_send` | `res/drawable-hdpi/msg_send.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_settings` | `res/drawable-hdpi/msg_settings.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_share` | `res/drawable-hdpi/msg_share.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_social_google` | `NONE (Google provider branding)` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_star_bookmark_filled` | `ic_rating_star_filled.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_star_filled` | `ic_rating_star_filled.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_star_outline` | `ic_rating_star.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_status_circle_right` | `list_check.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_status_circle_unattempted` | `NONE` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_status_circle_wrong` | `msg_warning.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_syllabus` | `msg_filehq.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_theme_moon` | `NONE` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_theme_sun` | `NONE` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_timer` | `msg_stories_timer.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_topic` | `msg_topics.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_trophy` | `ic_rating_star_filled.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_upload` | `msg_mini_upload.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_user_profile` | `res/drawable-hdpi/msg_openprofile.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_users` | `res/drawable-hdpi/msg_groups.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `ic_visibility` | `intro_ic_smile_eye.webp (not semantic equivalent)` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `ic_warning` | `msg_warning.webp` | NO EXACT COUNTERPART — approval required; SEMANTIC_MISMATCH |
| `msg_home` | `res/drawable-hdpi/msg_home.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `msg_theme` | `res/drawable-hdpi/msg_theme.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |
| `msg_palette` | `res/drawable-hdpi/msg_palette.webp` | ALREADY IMPORTED, KEEP ORIGINAL HASHES |

Candidate suggestions in this table are **not** approved replacements. Their names are from source drawable resources; a similar name does not prove visual or semantic equivalence. No changes to exam-specific art or Lottie.
