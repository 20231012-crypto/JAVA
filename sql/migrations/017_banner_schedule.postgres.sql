-- Scheduled banners: a start and end time so an event banner appears and retires by itself.
--
-- Today a banner is visible iff is_active, which means somebody has to remember to switch the
-- 20/11 banner on that morning and off again afterwards. In practice that means banners for
-- finished events stay up.
--
-- The window COMBINES with is_active rather than replacing it, because the two answer different
-- questions:
--   is_active  = the manual master switch — "take this down right now", regardless of dates
--   start_at / end_at = the automatic window — "show it only during the festival"
-- Visible therefore means:
--   is_active AND (start_at IS NULL OR start_at <= now) AND (end_at IS NULL OR end_at > now)
-- Both NULL keeps the current always-on behaviour, so every existing row is unaffected.
--
-- end_at is exclusive (> now, not >=) so a banner set to end at 00:00 on the 21st is gone the
-- instant that date starts rather than lingering for one more second.
--
-- Additive only, both columns nullable — safe to run against the live database.

ALTER TABLE banners ADD COLUMN IF NOT EXISTS start_at TIMESTAMP NULL;
ALTER TABLE banners ADD COLUMN IF NOT EXISTS end_at   TIMESTAMP NULL;

-- A window that ends before it starts can never show the banner; catching it here means the admin
-- form gets an error instead of silently saving something that will never appear.
ALTER TABLE banners DROP CONSTRAINT IF EXISTS chk_banner_window;
ALTER TABLE banners ADD CONSTRAINT chk_banner_window
  CHECK (start_at IS NULL OR end_at IS NULL OR end_at > start_at);

-- findActiveByPosition filters on position + is_active + the window, ordered by sort_order.
-- banners had no index at all beyond its primary key.
CREATE INDEX IF NOT EXISTS idx_banners_position ON banners(position, sort_order);
