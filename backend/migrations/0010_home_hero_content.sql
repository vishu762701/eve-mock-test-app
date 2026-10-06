-- Admin-managed Home hero content in the existing app_content store.
ALTER TABLE app_content ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1;
ALTER TABLE app_content ADD COLUMN cta_label TEXT NOT NULL DEFAULT '';
ALTER TABLE app_content ADD COLUMN cta_action TEXT NOT NULL DEFAULT '';
