-- Adds loyalty points (tích điểm) and self-service EAUT Pay top-up requests.
-- Additive only — safe to run against the already-deployed Neon database; schema.postgres.sql
-- (fresh installs) already includes these from the start.

ALTER TABLE users ADD COLUMN IF NOT EXISTS loyalty_points INT NOT NULL DEFAULT 0;

ALTER TABLE orders ADD COLUMN IF NOT EXISTS loyalty_points_used INT NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS loyalty_discount_amount DECIMAL(12,0) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS wallet_topup_requests (
  request_id    SERIAL PRIMARY KEY,
  user_id       INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  amount        DECIMAL(12,0) NOT NULL,
  status        VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','REJECTED')),
  confirmed_by  INT NULL REFERENCES users(user_id) ON DELETE SET NULL,
  confirmed_at  TIMESTAMP NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loyalty_transactions (
  transaction_id  SERIAL PRIMARY KEY,
  user_id         INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  points          INT NOT NULL,
  type            VARCHAR(10) NOT NULL CHECK (type IN ('EARN','REDEEM')),
  order_id        INT NULL REFERENCES orders(order_id) ON DELETE SET NULL,
  note            VARCHAR(255) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_topup_user      ON wallet_topup_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_loyalty_tx_user ON loyalty_transactions(user_id);
