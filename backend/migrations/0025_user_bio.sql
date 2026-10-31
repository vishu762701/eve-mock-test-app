-- Migration 0025: Private authenticated profile biography (Telegram-style About/Bio text)
-- Server-owned identity is unchanged. Existing user rows receive empty bio.
ALTER TABLE users ADD COLUMN bio TEXT NOT NULL DEFAULT '';
