-- Adds the KDS-facing fields from the latest design spec: student ID/class on the customer
-- profile (shown on kitchen order cards), per-product prep time (backs the countdown timer),
-- an estimated-ready timestamp per order, a staff on-duty roster, and a single-row shop-status
-- switch (Mở đơn / Tạm ngưng nhận đơn). Additive only — safe to run against the already-deployed
-- Neon database; schema.postgres.sql (fresh installs) already includes these from the start.

ALTER TABLE users ADD COLUMN IF NOT EXISTS student_id VARCHAR(20) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS class_name VARCHAR(50) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS on_duty BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE products ADD COLUMN IF NOT EXISTS avg_prep_minutes INT NOT NULL DEFAULT 10;

ALTER TABLE orders ADD COLUMN IF NOT EXISTS estimated_ready_at TIMESTAMP NULL;

-- Single-row switch: Admin/sales staff can pause new orders when the kitchen is overloaded
-- ("Mở đơn / Tạm ngưng nhận đơn" in the top bar of the kitchen dashboard spec).
CREATE TABLE IF NOT EXISTS shop_status (
  status_id           INT PRIMARY KEY DEFAULT 1 CHECK (status_id = 1),
  is_accepting_orders BOOLEAN NOT NULL DEFAULT TRUE,
  updated_by          INT NULL REFERENCES users(user_id) ON DELETE SET NULL,
  updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO shop_status (status_id, is_accepting_orders) VALUES (1, TRUE) ON CONFLICT (status_id) DO NOTHING;
