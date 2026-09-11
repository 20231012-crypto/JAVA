-- EAUT Canteen Web App — PostgreSQL schema (Render + Neon deploy, see render-deploy.md)
--
-- This is the Postgres sibling of schema.sql (MySQL, used for local Tomcat dev per README).
-- Differences from the MySQL version, all forced by the engine switch, not by choice:
--   * No CREATE DATABASE/USE — Neon already provisions one database per project; connect to it
--     directly and run this script against it.
--   * No ENGINE=/CHARACTER SET/COLLATE clauses — Postgres has no storage-engine concept, and
--     Neon databases are UTF8 already, so utf8mb4 (MySQL's "real" UTF-8) has no Postgres analogue
--     to opt into.
--   * AUTO_INCREMENT -> SERIAL.
--   * Inline ENUM(...) -> VARCHAR + CHECK. UserDAOImpl/OrderDAOImpl etc. already read/write these
--     columns as plain strings (rs.getString + Enum.valueOf), so this is a schema-only change —
--     no Java code cares which of the two ways the DB enforces the allowed values.
--   * MySQL's "ON UPDATE CURRENT_TIMESTAMP" column clause has no Postgres equivalent; replaced
--     with an explicit trigger (set_updated_at) on the one table that used it (orders).
--
-- New in this schema, not present in the MySQL version — added together with the Postgres
-- migration because both were requested in the same pass:
--   * roles/permissions/role_permissions — dynamic RBAC (see below).
--   * users.wallet_balance / users.is_eaut_student — EAUT Pay wallet + Smart ID auto-discount.
--   * orders.order_code / orders.discount_amount — customer-facing ticket number + Smart ID
--     discount tracking.
--   * wallet_transactions — auditable ledger backing users.wallet_balance.

-- ============================================================
-- Reference data
-- ============================================================

CREATE TABLE buildings (
  building_id   SERIAL PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  description   VARCHAR(255) NULL,
  shipping_fee  DECIMAL(12,0) NOT NULL DEFAULT 0,
  is_active     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categories (
  category_id   SERIAL PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  is_active     BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================================
-- RBAC — roles are the admin-creatable "đối tượng sử dụng" (any number, any name); permissions
-- are the fixed catalog of real actions the code enforces, one per functional area/controller
-- group (see SecurityFilter). Toggling a permission on a role takes effect for every user with
-- that role immediately — no code change or redeploy needed to reshape who can do what.
-- ============================================================

CREATE TABLE permissions (
  permission_key  VARCHAR(60) PRIMARY KEY,
  group_name      VARCHAR(50) NOT NULL,
  display_name    VARCHAR(150) NOT NULL,
  sort_order      INT NOT NULL DEFAULT 0
);

CREATE TABLE roles (
  role_id             SERIAL PRIMARY KEY,
  role_key            VARCHAR(50) NOT NULL UNIQUE,
  display_name        VARCHAR(100) NOT NULL,
  is_system           BOOLEAN NOT NULL DEFAULT FALSE, -- seeded role; cannot be deleted (permissions still freely editable)
  is_customer_default BOOLEAN NOT NULL DEFAULT FALSE, -- role assigned to new self-service Google sign-ups
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- At most one role can be the customer-signup default at any time.
CREATE UNIQUE INDEX one_customer_default_role ON roles (is_customer_default) WHERE is_customer_default;

CREATE TABLE role_permissions (
  role_id         INT NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
  permission_key  VARCHAR(60) NOT NULL REFERENCES permissions(permission_key) ON DELETE CASCADE,
  PRIMARY KEY (role_id, permission_key)
);

-- ============================================================
-- Users
-- ============================================================

CREATE TABLE users (
  user_id         SERIAL PRIMARY KEY,
  username        VARCHAR(50) NOT NULL UNIQUE,
  password_hash   VARCHAR(60) NULL,         -- NULL for GOOGLE-provider accounts (no local password)
  google_sub      VARCHAR(255) NULL UNIQUE, -- Google account's stable "sub" claim; set when auth_provider=GOOGLE
  auth_provider   VARCHAR(10) NOT NULL DEFAULT 'LOCAL' CHECK (auth_provider IN ('LOCAL','GOOGLE')),
  full_name       VARCHAR(100) NOT NULL,
  email           VARCHAR(100) NOT NULL UNIQUE,
  phone           VARCHAR(15) NULL,
  role_id         INT NOT NULL REFERENCES roles(role_id) ON DELETE RESTRICT,
  status          VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISABLED')),
  building_id     INT NULL REFERENCES buildings(building_id) ON DELETE SET NULL,
  wallet_balance  DECIMAL(12,0) NOT NULL DEFAULT 0,     -- EAUT Pay balance; only ever changed via wallet_transactions
  is_eaut_student BOOLEAN NOT NULL DEFAULT FALSE,       -- Smart ID marker (email domain @eaut.edu.vn at signup) -> automatic checkout discount
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Catalog & inventory (two-tier: warehouse "kho" -> shelf "kệ")
-- ============================================================

CREATE TABLE products (
  product_id     SERIAL PRIMARY KEY,
  category_id    INT NOT NULL REFERENCES categories(category_id) ON DELETE RESTRICT,
  name           VARCHAR(150) NOT NULL,
  description    VARCHAR(500) NULL,
  price          DECIMAL(12,0) NOT NULL,
  image_filename VARCHAR(255) NULL,
  unit           VARCHAR(20) NULL,
  is_active      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 1:1 with products; a row is created here (quantity 0) in the same transaction as the product insert.
CREATE TABLE warehouse_stock (
  product_id  INT PRIMARY KEY REFERENCES products(product_id) ON DELETE CASCADE,
  quantity    INT NOT NULL DEFAULT 0
);

-- 1:1 with products; only this quantity is sellable/visible to customers.
CREATE TABLE shelf_stock (
  product_id  INT PRIMARY KEY REFERENCES products(product_id) ON DELETE CASCADE,
  quantity    INT NOT NULL DEFAULT 0
);

-- Nhập hàng: Admin/Manager stock-in receipt (header + lines)
CREATE TABLE stock_imports (
  import_id      SERIAL PRIMARY KEY,
  admin_id       INT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
  supplier_name  VARCHAR(150) NULL,
  imported_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note           VARCHAR(255) NULL
);

CREATE TABLE stock_import_items (
  import_item_id  SERIAL PRIMARY KEY,
  import_id       INT NOT NULL REFERENCES stock_imports(import_id) ON DELETE CASCADE,
  product_id      INT NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
  quantity        INT NOT NULL,
  unit_cost       DECIMAL(12,0) NOT NULL
);

-- Store staff transfers stock kho -> kệ; one row per product per physical restock action.
CREATE TABLE stock_transfers (
  transfer_id     SERIAL PRIMARY KEY,
  store_staff_id  INT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
  product_id      INT NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
  quantity        INT NOT NULL,
  transferred_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Orders (channel=ONLINE web orders and channel=COUNTER in-person POS sales)
-- ============================================================

-- Backs order_code below. A dedicated sequence (rather than deriving the code from order_id
-- after insert) means OrderDAOImpl can compute the code in one INSERT with no placeholder-then-
-- UPDATE step and no unique-constraint contention between concurrent checkouts.
CREATE SEQUENCE order_ticket_seq START 1;

CREATE TABLE orders (
  order_id              SERIAL PRIMARY KEY,
  order_code            VARCHAR(20) NOT NULL UNIQUE, -- short ticket code shown to customers/kitchen board, e.g. "A-142"
  customer_id           INT NULL REFERENCES users(user_id) ON DELETE SET NULL,     -- NULL for COUNTER walk-in sales
  building_id           INT NULL REFERENCES buildings(building_id) ON DELETE RESTRICT, -- NULL for COUNTER channel (picked up in person)
  channel               VARCHAR(10) NOT NULL DEFAULT 'ONLINE' CHECK (channel IN ('ONLINE','COUNTER')),
  sold_by               INT NULL REFERENCES users(user_id) ON DELETE SET NULL, -- sales staff who rang up a COUNTER sale
  subtotal              DECIMAL(12,0) NOT NULL,
  shipping_fee          DECIMAL(12,0) NOT NULL DEFAULT 0,
  discount_amount       DECIMAL(12,0) NOT NULL DEFAULT 0, -- EAUT Smart ID automatic discount (see AppConfig smartId.discountPercent)
  total_amount          DECIMAL(12,0) NOT NULL,
  order_status          VARCHAR(12) NOT NULL DEFAULT 'PENDING'
                          CHECK (order_status IN ('PENDING','CONFIRMED','REJECTED','SHIPPING','COMPLETED','CANCELLED')),
  payment_method        VARCHAR(10) NOT NULL CHECK (payment_method IN ('COD','VIETQR','CASH','WALLET')),
  payment_status        VARCHAR(10) NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID','PAID')),
  payment_confirmed_by  INT NULL REFERENCES users(user_id) ON DELETE SET NULL,
  payment_confirmed_at  TIMESTAMP NULL,
  note                  VARCHAR(255) NULL,
  created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_orders_updated_at BEFORE UPDATE ON orders
  FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE order_items (
  order_item_id  SERIAL PRIMARY KEY,
  order_id       INT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
  product_id     INT NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
  quantity       INT NOT NULL,
  unit_price     DECIMAL(12,0) NOT NULL,
  line_total     DECIMAL(12,0) NOT NULL
);

-- Audit trail; reject/cancel reasons live here (in `note`), not on `orders`, so `orders` never
-- carries duplicated/driftable state.
CREATE TABLE order_status_history (
  history_id  SERIAL PRIMARY KEY,
  order_id    INT NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
  old_status  VARCHAR(12) NULL CHECK (old_status IN ('PENDING','CONFIRMED','REJECTED','SHIPPING','COMPLETED','CANCELLED')),
  new_status  VARCHAR(12) NOT NULL CHECK (new_status IN ('PENDING','CONFIRMED','REJECTED','SHIPPING','COMPLETED','CANCELLED')),
  changed_by  INT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
  note        VARCHAR(255) NULL,
  changed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- EAUT Pay wallet ledger: every top-up/spend/refund is its own row, so users.wallet_balance is
-- always reconstructable/auditable instead of a number trusted blindly.
CREATE TABLE wallet_transactions (
  transaction_id  SERIAL PRIMARY KEY,
  user_id         INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  amount          DECIMAL(12,0) NOT NULL, -- positive = top-up/refund, negative = payment
  type            VARCHAR(10) NOT NULL CHECK (type IN ('TOPUP','PAYMENT','REFUND')),
  order_id        INT NULL REFERENCES orders(order_id) ON DELETE SET NULL,
  created_by      INT NULL REFERENCES users(user_id) ON DELETE SET NULL, -- admin who approved a top-up
  note            VARCHAR(255) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_history_order     ON order_status_history(order_id);
CREATE INDEX idx_orders_customer   ON orders(customer_id);
CREATE INDEX idx_orders_status     ON orders(order_status);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_wallet_tx_user    ON wallet_transactions(user_id);
