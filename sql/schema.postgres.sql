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

-- Accent-insensitive menu search: Vietnamese is often typed without diacritics when searching,
-- so ProductDAOImpl.search compares unaccent(name) against unaccent(query).
CREATE EXTENSION IF NOT EXISTS unaccent;

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
  category_id        SERIAL PRIMARY KEY,
  name                VARCHAR(100) NOT NULL,
  parent_category_id INT NULL REFERENCES categories(category_id) ON DELETE SET NULL, -- NULL = top-level group (mega-menu column header); set = a leaf sub-category products actually belong to
  is_active           BOOLEAN NOT NULL DEFAULT TRUE
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
  loyalty_points  INT NOT NULL DEFAULT 0,               -- earned on completed online orders; only ever changed via loyalty_transactions
  student_id      VARCHAR(20) NULL,                     -- MSSV, collected via the checkout info gate for @eaut.edu.vn customers
  class_name      VARCHAR(50) NULL,                      -- Khoa/Lớp, collected alongside student_id
  on_duty         BOOLEAN NOT NULL DEFAULT FALSE,        -- staff self-reported "đang trực" status shown on the Kanban boards
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
  original_price DECIMAL(12,0) NULL,                     -- "was" price shown crossed out when set and > price; NULL/unset = no promo
  promo_target_quantity INT NULL,                        -- real "Đã bán X/Y" progress target; X computed live from COMPLETED order_items
  image_filename VARCHAR(255) NULL,
  unit           VARCHAR(20) NULL,
  is_active      BOOLEAN NOT NULL DEFAULT TRUE,           -- FALSE hides the dish from the menu entirely
  is_available   BOOLEAN NOT NULL DEFAULT TRUE,           -- FALSE leaves it visible but greyed out and unorderable
  low_stock_threshold INT NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),  -- per dish: "under 5" is meaningless across bottled water and set lunches
  -- VAT rate for this dish, set once when it is created so the till never has to choose one.
  -- price above is tax-INCLUSIVE; this records how much of it is tax, for the receipt breakdown.
  tax_percent    DECIMAL(5,2) NOT NULL DEFAULT 0 CHECK (tax_percent >= 0 AND tax_percent < 100),
  avg_prep_minutes INT NOT NULL DEFAULT 10,              -- backs the KDS countdown timer (orders.estimated_ready_at)
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 1:1 with products; a row is created here (quantity 0) in the same transaction as the product insert.
CREATE TABLE warehouse_stock (
  product_id  INT PRIMARY KEY REFERENCES products(product_id) ON DELETE CASCADE,
  -- The database's rule, not just decrementIfEnough's: application guards can be forgotten by a
  -- future caller, a CHECK cannot.
  quantity    INT NOT NULL DEFAULT 0 CHECK (quantity >= 0)
);

-- 1:1 with products; only this quantity is sellable/visible to customers.
CREATE TABLE shelf_stock (
  product_id  INT PRIMARY KEY REFERENCES products(product_id) ON DELETE CASCADE,
  quantity    INT NOT NULL DEFAULT 0 CHECK (quantity >= 0)
);

-- Nhà cung cấp. Phải khai báo TRƯỚC stock_imports vì phiếu nhập tham chiếu tới đây.
CREATE TABLE suppliers (
  supplier_id  SERIAL PRIMARY KEY,
  name         VARCHAR(150) NOT NULL,
  phone        VARCHAR(20)  NULL,
  email        VARCHAR(150) NULL,
  address      VARCHAR(255) NULL,
  note         VARCHAR(255) NULL,
  is_active    BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Chỉ ràng buộc trùng tên trên bản ghi còn hoạt động, để tên của một NCC đã ngừng vẫn dùng lại được.
CREATE UNIQUE INDEX uq_suppliers_name_active ON suppliers (lower(name)) WHERE is_active;

-- Nhập hàng: phiếu nhập từ nhà cung cấp (đầu phiếu + các dòng).
--
-- Quy trình hai bước, không phải một: lập phiếu ghi lại việc ĐẶT hàng (status DRAFT, kho chưa
-- động), rồi khi hàng về mới điền số thực nhận và xác nhận thì warehouse_stock mới cộng. Khoảng
-- chênh giữa quantity (đặt) và received_quantity (nhận) chính là chỗ bắt được giao thiếu, hàng vỡ
-- và giao nhầm — mô hình một bước trước đây luôn ghi sổ đúng bằng số đặt, kể cả khi kho không khớp.
CREATE TABLE stock_imports (
  import_id       SERIAL PRIMARY KEY,
  code            VARCHAR(20) NULL UNIQUE,
  admin_id        INT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
  supplier_id     INT NULL REFERENCES suppliers(supplier_id) ON DELETE SET NULL,
  -- Bản chụp tên NCC tại thời điểm lập phiếu, cùng lý do order_items giữ unit_price: đổi tên nhà
  -- cung cấp về sau không được viết lại chứng từ đã phát sinh.
  supplier_name   VARCHAR(150) NULL,
  status          VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
                    CHECK (status IN ('DRAFT', 'PARTIAL', 'RECEIVED', 'CANCELLED')),
  expected_date   DATE NULL,
  discount_amount DECIMAL(12,0) NOT NULL DEFAULT 0,
  other_cost      DECIMAL(12,0) NOT NULL DEFAULT 0,
  paid_amount     DECIMAL(12,0) NOT NULL DEFAULT 0,
  imported_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  received_at     TIMESTAMP NULL,
  received_by     INT NULL REFERENCES users(user_id) ON DELETE SET NULL,
  cancelled_at    TIMESTAMP NULL,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note            VARCHAR(255) NULL,
  CONSTRAINT chk_stock_imports_money
    CHECK (discount_amount >= 0 AND other_cost >= 0 AND paid_amount >= 0)
);

CREATE INDEX idx_stock_imports_status   ON stock_imports(status, imported_at DESC);
CREATE INDEX idx_stock_imports_supplier ON stock_imports(supplier_id);

-- Mã phiếu lấy từ sequence chứ không suy ra từ import_id sau khi chèn — cùng lý do với
-- order_ticket_seq: có mã trước khi có hàng, một câu INSERT, không tranh chấp unique.
CREATE SEQUENCE stock_import_code_seq START 1;

CREATE TABLE stock_import_items (
  import_item_id    SERIAL PRIMARY KEY,
  import_id         INT NOT NULL REFERENCES stock_imports(import_id) ON DELETE CASCADE,
  product_id        INT NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
  quantity          INT NOT NULL CHECK (quantity > 0),
  received_quantity INT NOT NULL DEFAULT 0,
  unit_cost         DECIMAL(12,0) NOT NULL,
  CONSTRAINT chk_import_items_received
    CHECK (received_quantity >= 0 AND received_quantity <= quantity)
);

CREATE INDEX idx_import_items_product ON stock_import_items(product_id);

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
  loyalty_points_used   INT NOT NULL DEFAULT 0,           -- points redeemed on this order (see AppConfig loyalty.redeemValuePerPoint)
  loyalty_discount_amount DECIMAL(12,0) NOT NULL DEFAULT 0, -- đồng value of loyalty_points_used, tracked separately from discount_amount so the two show as distinct line items
  total_amount          DECIMAL(12,0) NOT NULL,
  order_status          VARCHAR(12) NOT NULL DEFAULT 'PENDING'
                          CHECK (order_status IN ('PENDING','CONFIRMED','REJECTED','SHIPPING','COMPLETED','CANCELLED')),
  payment_method        VARCHAR(10) NOT NULL CHECK (payment_method IN ('COD','VIETQR','CASH','WALLET')),
  payment_status        VARCHAR(10) NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID','PAID')),
  payment_confirmed_by  INT NULL REFERENCES users(user_id) ON DELETE SET NULL,
  payment_confirmed_at  TIMESTAMP NULL,
  estimated_ready_at    TIMESTAMP NULL, -- set when confirmed; drives the KDS countdown timer
  -- The EAUT Pay incentive, kept apart from discount_amount so the receipt can name each discount
  -- rather than printing a combined figure under the Smart ID label.
  wallet_discount_amount DECIMAL(12,0) NOT NULL DEFAULT 0 CHECK (wallet_discount_amount >= 0),
  -- VAT contained in the sale, snapshotted at the time: a rate edited next term must not rewrite
  -- last term's receipts.
  tax_amount            DECIMAL(12,0) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
  -- Refund bookkeeping. refunded_at doubles as the "already refunded" flag: the admin refund
  -- claims the order with UPDATE ... WHERE refunded_at IS NULL, which is what stops two
  -- simultaneous clicks both paying the student back.
  refunded_amount       DECIMAL(12,0) NOT NULL DEFAULT 0 CHECK (refunded_amount >= 0),
  refunded_at           TIMESTAMP NULL,
  refunded_by           INT NULL REFERENCES users(user_id) ON DELETE SET NULL,
  CONSTRAINT chk_orders_refund_complete
    CHECK ((refunded_at IS NULL AND refunded_by IS NULL) OR (refunded_at IS NOT NULL AND refunded_by IS NOT NULL)),
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

-- ===== Stock ledger =====
-- Declared here rather than beside warehouse_stock/shelf_stock, which is where it
-- belongs conceptually: it carries an FK to orders(order_id) for the RESTOCK rows a
-- cancellation writes, so it cannot be created before orders exists.
-- warehouse_stock/shelf_stock above stay the fast current counts; this is the history that
-- explains them, the same split users.wallet_balance and wallet_transactions already use. Every
-- write that changes a counter records a row here in the SAME transaction — a ledger that can be
-- half-written is worse than none, because it looks authoritative while being wrong.
CREATE TABLE stock_movements (
  movement_id  SERIAL PRIMARY KEY,
  product_id   INT NOT NULL REFERENCES products(product_id) ON DELETE RESTRICT,
  location     VARCHAR(10) NOT NULL CHECK (location IN ('WAREHOUSE', 'SHELF')),
  delta        INT NOT NULL CHECK (delta <> 0),          -- signed; zero would explain nothing
  reason       VARCHAR(16) NOT NULL CHECK (reason IN
                 ('IMPORT', 'TRANSFER_OUT', 'TRANSFER_IN', 'SALE', 'RESTOCK', 'WRITE_OFF', 'STOCK_TAKE')),
  ref_order_id INT NULL REFERENCES orders(order_id) ON DELETE SET NULL,
  note         VARCHAR(255) NULL,
  -- NOT NULL and RESTRICT: every movement is somebody's action, and an audit trail that can lose
  -- its actor is not an audit trail.
  created_by   INT NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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

-- Self-service EAUT Pay top-up: customer requests an amount, gets a VietQR code (same trust model
-- as VietQR order payments — there is no real bank API link, so a human still has to look at the
-- bank account and confirm the transfer actually landed before the balance moves).
CREATE TABLE wallet_topup_requests (
  request_id    SERIAL PRIMARY KEY,
  user_id       INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  amount        DECIMAL(12,0) NOT NULL,
  status        VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','CONFIRMED','REJECTED')),
  confirmed_by  INT NULL REFERENCES users(user_id) ON DELETE SET NULL,
  confirmed_at  TIMESTAMP NULL,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Loyalty/tích điểm: 1 point earned per AppConfig "loyalty.vndPerPoint" spent on a completed
-- ONLINE order (see OrderFulfillmentServlet), redeemable at checkout for a discount (see
-- AppConfig "loyalty.redeemValuePerPoint"). Ledgered the same way as wallet_transactions so
-- users.loyalty_points is always reconstructable.
CREATE TABLE loyalty_transactions (
  transaction_id  SERIAL PRIMARY KEY,
  user_id         INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  points          INT NOT NULL, -- positive = earn, negative = redeem
  type            VARCHAR(10) NOT NULL CHECK (type IN ('EARN','REDEEM')),
  order_id        INT NULL REFERENCES orders(order_id) ON DELETE SET NULL,
  note            VARCHAR(255) NULL,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Single-row switch: Admin/sales staff can pause new orders when the kitchen is overloaded
-- ("Mở đơn / Tạm ngưng nhận đơn" in the kitchen dashboard's top bar).
CREATE TABLE shop_status (
  status_id           INT PRIMARY KEY DEFAULT 1 CHECK (status_id = 1),
  is_accepting_orders BOOLEAN NOT NULL DEFAULT TRUE,
  updated_by          INT NULL REFERENCES users(user_id) ON DELETE SET NULL,
  updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO shop_status (status_id, is_accepting_orders) VALUES (1, TRUE);

-- Wishlist / "yêu thích" — a customer can favorite a product; admin can see which products are
-- favorited most (real counts, computed from this table, never fabricated).
CREATE TABLE favorites (
  user_id     INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  product_id  INT NOT NULL REFERENCES products(product_id) ON DELETE CASCADE,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, product_id)
);

-- Admin-configurable banners by position on the catalog page (HEAD/FOOTER/LEFT/RIGHT). Images
-- stored as bytea directly here (not the ephemeral upload directory) so they survive redeploys.
CREATE TABLE banners (
  banner_id          SERIAL PRIMARY KEY,
  position            VARCHAR(10) NOT NULL CHECK (position IN ('HEAD', 'FOOTER', 'LEFT', 'RIGHT')),
  title               VARCHAR(150) NULL,
  subtitle            VARCHAR(300) NULL,
  link_url            VARCHAR(500) NULL,
  image_data          BYTEA NULL,
  image_content_type  VARCHAR(100) NULL,
  sort_order          INT NOT NULL DEFAULT 0,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,   -- the manual master switch: takes it down now
  -- The automatic window, AND-ed with is_active. Both NULL keeps the always-on behaviour, and
  -- end_at is exclusive so a banner ending at midnight is gone the instant that date starts.
  start_at            TIMESTAMP NULL,
  end_at              TIMESTAMP NULL,
  CONSTRAINT chk_banner_window CHECK (start_at IS NULL OR end_at IS NULL OR end_at > start_at),
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===== Business configuration =====
-- Key/value rather than one wide row: the settings screen renders itself FROM these rows —
-- group_name makes the section, display_name the label, value_type the input control — so adding
-- a setting later is one INSERT with no schema change and no form to edit. AppConfig keeps the
-- deployment's secrets; this table holds what the canteen owns.
CREATE TABLE app_settings (
  setting_key   VARCHAR(60) PRIMARY KEY,
  setting_value VARCHAR(500) NOT NULL,
  value_type    VARCHAR(10) NOT NULL DEFAULT 'STRING'
                  CHECK (value_type IN ('STRING','INT','DECIMAL','BOOL','TIME')),
  group_name    VARCHAR(40) NOT NULL,
  display_name  VARCHAR(150) NOT NULL,
  hint          VARCHAR(255) NULL,
  sort_order    INT NOT NULL DEFAULT 0,
  updated_by    INT NULL REFERENCES users(user_id) ON DELETE SET NULL,
  updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Staff check-in/check-out (chấm công). One row per shift; check_out_at NULL = still on shift.
-- Distinct from users.on_duty, which is a live "working right now" switch for the queue boards:
-- on_duty answers "who is working now", this table answers "who worked when, for how long".
CREATE TABLE staff_attendance (
  attendance_id  SERIAL PRIMARY KEY,
  user_id        INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  check_in_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  check_out_at   TIMESTAMP NULL,
  note           VARCHAR(255) NULL,
  CONSTRAINT chk_attendance_order CHECK (check_out_at IS NULL OR check_out_at >= check_in_at)
);

CREATE INDEX idx_history_order     ON order_status_history(order_id);
CREATE INDEX idx_orders_customer   ON orders(customer_id);
CREATE INDEX idx_orders_status     ON orders(order_status);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_wallet_tx_user    ON wallet_transactions(user_id);
CREATE INDEX idx_topup_user        ON wallet_topup_requests(user_id);
CREATE INDEX idx_loyalty_tx_user   ON loyalty_transactions(user_id);
-- Every line of every order lands in order_items, and both the best-seller report and the
-- "Hot hit tuần" catalog section GROUP BY this column.
CREATE INDEX idx_order_items_product ON order_items(product_id);
-- At most one open shift per staff member: two rapid clock-in requests would both pass an
-- application-level "is there an open shift?" check, so the database refuses the second.
CREATE UNIQUE INDEX one_open_shift_per_user
  ON staff_attendance(user_id) WHERE check_out_at IS NULL;
CREATE INDEX idx_attendance_user_day ON staff_attendance(user_id, check_in_at);

-- The stock ledger is read two ways: one dish's history, and everything that happened recently.
CREATE INDEX idx_stock_mov_product ON stock_movements(product_id, created_at DESC);
CREATE INDEX idx_stock_mov_created ON stock_movements(created_at DESC);
-- The admin order list sorts newest-first and filters by payment method; orders previously had
-- indexes on customer_id and order_status only, so every page load sorted the whole table.
CREATE INDEX idx_orders_created ON orders(created_at DESC);
CREATE INDEX idx_orders_payment_method ON orders(payment_method);
-- findActiveByPosition filters on position and orders by sort_order; banners had no index at all
-- beyond its primary key.
CREATE INDEX idx_banners_position ON banners(position, sort_order);
