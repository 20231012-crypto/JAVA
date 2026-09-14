-- Real promo pricing per product (admin-set, not fabricated): original_price is the "was" price
-- shown crossed out; price stays the actual current/charged price everywhere (cart, checkout,
-- order totals) so nothing about money calculation changes. A product is "on promo" whenever
-- original_price is set and greater than price — clearing original_price turns the promo off.
-- promo_target_quantity backs a real "Đã bán X/Y" progress bar: X is computed live from actual
-- COMPLETED order_items (see ProductDAOImpl), Y is this admin-set target. NULL means no bar shown.
-- Additive only — safe to run against the live database.

ALTER TABLE products ADD COLUMN IF NOT EXISTS original_price DECIMAL(12,0) NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS promo_target_quantity INT NULL;
