-- Optional external CTA target and label for each existing Home banner.
ALTER TABLE home_banners ADD COLUMN link_url TEXT NOT NULL DEFAULT '';
ALTER TABLE home_banners ADD COLUMN link_label TEXT NOT NULL DEFAULT '';
