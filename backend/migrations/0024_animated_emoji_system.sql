-- Migration 0024: Animated Emoji System (Packs, Items, Placements)
-- Mission 03 F4: Telegram packs, Google Noto animated emoji, custom uploads, and surface placements.

CREATE TABLE IF NOT EXISTS emoji_packs (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    source TEXT NOT NULL, -- 'telegram' | 'noto' | 'telegram-ui' | 'upload'
    license_attribution TEXT,
    enabled INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    created_by TEXT
);

CREATE INDEX IF NOT EXISTS idx_emoji_packs_source ON emoji_packs (source);
CREATE INDEX IF NOT EXISTS idx_emoji_packs_enabled ON emoji_packs (enabled);

CREATE TABLE IF NOT EXISTS emoji_items (
    id TEXT PRIMARY KEY,
    pack_id TEXT NOT NULL REFERENCES emoji_packs(id) ON DELETE CASCADE,
    alt_emoji TEXT NOT NULL,
    kind TEXT NOT NULL, -- 'lottie' | 'static'
    storage_key TEXT NOT NULL,
    thumbnail_key TEXT,
    byte_size INTEGER NOT NULL DEFAULT 0,
    sha256 TEXT,
    enabled INTEGER NOT NULL DEFAULT 1,
    premium_origin INTEGER NOT NULL DEFAULT 0,
    telegram_custom_emoji_id TEXT,
    source TEXT NOT NULL,
    pack_name TEXT,
    uploader TEXT,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_emoji_items_pack ON emoji_items (pack_id);
CREATE INDEX IF NOT EXISTS idx_emoji_items_enabled ON emoji_items (enabled);

CREATE TABLE IF NOT EXISTS emoji_placements (
    id TEXT PRIMARY KEY,
    slot_type TEXT NOT NULL, -- 'exam_card' | 'category_header' | 'broadcast'
    target_id TEXT NOT NULL, -- exam id, category name, or broadcast id
    item_id TEXT NOT NULL REFERENCES emoji_items(id) ON DELETE CASCADE,
    updated_by TEXT,
    updated_at INTEGER NOT NULL,
    UNIQUE(slot_type, target_id)
);

CREATE INDEX IF NOT EXISTS idx_emoji_placements_target ON emoji_placements (slot_type, target_id);

-- Default system packs for Telegram UI icons and Noto Animated Emoji
INSERT OR IGNORE INTO emoji_packs (id, name, title, source, license_attribution, enabled, created_at, created_by) VALUES
('pack_tg_ui', 'telegram_ui_icons', 'Telegram UI Icons', 'telegram-ui', 'GNU GPL v2 or later. Copyright (C) Nikolai Kudashov / Telegram FZ-LLC.', 1, unixepoch() * 1000, 'system'),
('pack_noto', 'noto_animated_emoji', 'Noto Animated Emoji', 'noto', 'CC BY 4.0. Copyright Google LLC (googlefonts/noto-emoji).', 1, unixepoch() * 1000, 'system');

-- Global Telegram emoji switch (defaults to enabled: true)
INSERT OR IGNORE INTO app_content (id, title, body, updated_at) VALUES
('telegram_emoji_global', 'Telegram Emoji Global Switch', '{"enabled":true}', unixepoch() * 1000);
