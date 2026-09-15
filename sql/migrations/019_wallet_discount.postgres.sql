-- A second discount line on the order: the incentive for paying with EAUT Pay.
--
-- orders.discount_amount already exists and holds the EAUT Smart ID student discount. The new
-- EAUT Pay discount could have been folded into it, and that would have needed no migration — but
-- an order is a financial record, and the receipt already prints discount_amount under the label
-- "Giảm giá EAUT Smart ID". Adding a second, differently-earned discount to that number would
-- make the label a lie and leave no way to answer "how much did the wallet incentive cost us?".
--
-- Defaults to 0, so every existing order reads as "no wallet discount", which is exactly what they
-- are: the incentive did not exist when they were placed.
--
-- Additive only — safe to run against the live database.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS wallet_discount_amount DECIMAL(12,0) NOT NULL DEFAULT 0;

ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_wallet_discount_nonneg;
ALTER TABLE orders ADD CONSTRAINT chk_orders_wallet_discount_nonneg CHECK (wallet_discount_amount >= 0);
