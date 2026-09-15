-- One ledger for every stock movement.
--
-- Stock is currently two counters — warehouse_stock.quantity and shelf_stock.quantity — mutated
-- from five different places (stock import, kho->kệ transfer, checkout, counter sale, and the
-- restock that reject/cancel performs). Nothing records WHY a number changed, so when the shelf
-- count is wrong there is no way to find out where it went. stock_imports and stock_transfers
-- each log their own slice, but a sale or a cancellation logs nothing at all.
--
-- This table is the missing audit trail, and it follows a pattern the codebase already uses:
-- users.wallet_balance is a fast current balance while wallet_transactions is the ledger behind
-- it. warehouse_stock/shelf_stock keep exactly that role here — they stay the number you read,
-- stock_movements becomes the history that explains it.
--
-- delta is signed and CHECK (delta <> 0): a movement of zero is never meaningful, and letting one
-- in would put rows in the ledger that explain nothing. The direction lives in the sign, so a
-- transfer writes two rows (TRANSFER_OUT negative on WAREHOUSE, TRANSFER_IN positive on SHELF)
-- and both sides of the move stay visible.
--
-- created_by is NOT NULL and RESTRICT: every movement is somebody's action, and the point of an
-- audit trail is that it cannot lose the actor. ref_order_id is SET NULL because an order may be
-- deleted while its stock effect remains real.
--
-- Additive only — safe to run against the live database. See the CHECK note at the bottom.

CREATE TABLE IF NOT EXISTS stock_movements (
  movement_id  SERIAL PRIMARY KEY,
  product_id   INT NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
  location     VARCHAR(10) NOT NULL CHECK (location IN ('WAREHOUSE', 'SHELF')),
  delta        INT NOT NULL CHECK (delta <> 0),
  reason       VARCHAR(16) NOT NULL CHECK (reason IN
                 ('IMPORT', 'TRANSFER_OUT', 'TRANSFER_IN', 'SALE', 'RESTOCK', 'WRITE_OFF', 'STOCK_TAKE')),
  ref_order_id INT NULL REFERENCES orders(order_id) ON DELETE SET NULL,
  note         VARCHAR(255) NULL,
  created_by   INT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- The ledger is read two ways: "history of this dish" (product page) and "everything that happened
-- recently" (the inventory screen's default view). Both want newest first.
CREATE INDEX IF NOT EXISTS idx_stock_mov_product ON stock_movements(product_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_mov_created ON stock_movements(created_at DESC);

INSERT INTO permissions (permission_key, group_name, display_name, sort_order) VALUES
  ('stock.adjust', 'Kho & vận hành', 'Kiểm kê, hủy hàng & xem sổ kho', 75)
ON CONFLICT (permission_key) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, 'stock.adjust' FROM roles r WHERE r.role_key IN ('ADMIN', 'STORE_STAFF')
ON CONFLICT DO NOTHING;

-- Neither counter has ever been constrained, and decrementIfEnough is the only guard against a
-- negative — a guard in application code, which a future caller can forget. These make it the
-- database's rule instead.
--
-- RUN THIS FIRST. If either query returns a row, the ALTER below will fail and the data has to be
-- corrected before this migration can be applied:
--   SELECT * FROM shelf_stock     WHERE quantity < 0;
--   SELECT * FROM warehouse_stock WHERE quantity < 0;
ALTER TABLE warehouse_stock DROP CONSTRAINT IF EXISTS chk_warehouse_qty_nonneg;
ALTER TABLE warehouse_stock ADD CONSTRAINT chk_warehouse_qty_nonneg CHECK (quantity >= 0);

ALTER TABLE shelf_stock DROP CONSTRAINT IF EXISTS chk_shelf_qty_nonneg;
ALTER TABLE shelf_stock ADD CONSTRAINT chk_shelf_qty_nonneg CHECK (quantity >= 0);
