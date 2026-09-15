-- Two product columns the admin menu screen needs: a manual "sold out" switch, and a per-dish
-- low-stock threshold.
--
-- Why is_available is a SEPARATE column from is_active, rather than reusing it:
--   is_active  = FALSE  -> the dish disappears from the menu entirely (retired, off-season)
--   is_available = FALSE -> the dish still shows, greyed out, with ordering blocked (sold out today)
-- Those are different things a manager wants to say, and the customer-facing difference matters:
-- hiding a popular dish makes students think it was removed, greying it out tells them to come
-- back tomorrow.
--
-- Why it is a manual override rather than a computed state: the student menu ALREADY derives
-- "hết hàng" from shelf_stock.quantity <= 0 (customer/_product-card.jsp). This column exists for
-- the case the quantity cannot express — the shelf still shows 8 portions but the kitchen ran out
-- of an ingredient, so it must be closed by hand. Sellable therefore means all three:
--   is_active AND is_available AND shelf_stock.quantity > 0
--
-- Why low_stock_threshold is per-product: "dưới 5" is meaningless across a menu where bottled
-- water moves in hundreds and a set lunch moves in tens. Today that 5 is hardcoded TWICE in Java
-- (StoreReportServlet.LOW_STOCK_THRESHOLD and a separate literal in AdminDashboardServlet), so the
-- two screens can silently disagree. Default 5 keeps current behaviour for every existing row.
--
-- Additive only, both columns have defaults — safe to run against the live database.

ALTER TABLE products ADD COLUMN IF NOT EXISTS is_available BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE products ADD COLUMN IF NOT EXISTS low_stock_threshold INT NOT NULL DEFAULT 5;

-- A threshold of 0 would mean "never warn", which is a legitimate choice; negative is not.
ALTER TABLE products DROP CONSTRAINT IF EXISTS chk_products_threshold_nonneg;
ALTER TABLE products ADD CONSTRAINT chk_products_threshold_nonneg CHECK (low_stock_threshold >= 0);
