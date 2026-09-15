-- Refunds, and the indexes the admin order list needs.
--
-- THE BUG THIS FIXES: cancelling an order that was paid with EAUT Pay currently takes the
-- student's money and keeps it. CheckoutServlet debits the wallet (adjustWalletBalance(-total)
-- plus a PAYMENT ledger row) and spends loyalty points (adjustLoyaltyPoints(-used) plus a REDEEM
-- row), but OrderActionServlet.cancel() only puts the stock back and writes a history line. The
-- money and the points are simply gone. WalletTransactionType.REFUND exists in the enum and in
-- wallet_transactions' CHECK constraint, and not one line of code has ever written it.
--
-- Why these columns live on orders rather than being inferred from a REFUND ledger row:
-- the guard against refunding twice has to be atomic. With a column, the refund is claimed by
--     UPDATE orders SET refunded_at = ... WHERE order_id = ? AND refunded_at IS NULL
-- and the affected-row count tells the caller whether it won — the same guarded-UPDATE pattern
-- OrderDAO.updateStatus already uses for status transitions. Inferring it from the ledger would
-- mean SELECT-then-INSERT, and two admins clicking at once would both see "no refund yet" and
-- both pay out.
--
-- refunded_amount is stored even though it is usually total_amount, because it will not always be:
-- an unpaid COD order is cancelled with a refund of 0, and storing that explicitly distinguishes
-- "refunded nothing, correctly" from "never processed".
--
-- Additive only, all three columns nullable or defaulted — safe to run against the live database.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS refunded_amount DECIMAL(12,0) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refunded_at     TIMESTAMP NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refunded_by     INT NULL REFERENCES users(user_id) ON DELETE SET NULL;

-- A refund is either fully recorded or not recorded at all; half a record would defeat the
-- double-refund guard, which keys on refunded_at alone.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_refund_complete;
ALTER TABLE orders ADD CONSTRAINT chk_orders_refund_complete
  CHECK ((refunded_at IS NULL AND refunded_by IS NULL) OR (refunded_at IS NOT NULL AND refunded_by IS NOT NULL));

ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_refund_nonneg;
ALTER TABLE orders ADD CONSTRAINT chk_orders_refund_nonneg CHECK (refunded_amount >= 0);

-- The admin order screen lists newest-first and filters by date range; orders had indexes on
-- customer_id and order_status only, so every page load sorted the whole table.
CREATE INDEX IF NOT EXISTS idx_orders_created ON orders(created_at DESC);
-- Backs both the payment-method filter and the payment-mix donut on the dashboard.
CREATE INDEX IF NOT EXISTS idx_orders_payment_method ON orders(payment_method);

INSERT INTO permissions (permission_key, group_name, display_name, sort_order) VALUES
  ('orders.manage', 'Bán hàng', 'Xem & lọc toàn bộ đơn hàng',            95),
  ('orders.refund', 'Tài chính', 'Hủy đơn và hoàn tiền vào ví khách',    145)
ON CONFLICT (permission_key) DO NOTHING;

-- Refunding moves real money, so it stays with ADMIN only — sales staff keep orders.action
-- (confirm/reject/cancel) but cannot pay anyone back.
INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, k.permission_key
  FROM roles r CROSS JOIN (VALUES ('orders.manage'), ('orders.refund')) AS k(permission_key)
  WHERE r.role_key = 'ADMIN'
ON CONFLICT DO NOTHING;
