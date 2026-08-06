-- EAUT Canteen Web App — database schema
-- Charset utf8mb4 everywhere so Vietnamese diacritics store/round-trip correctly.

DROP DATABASE IF EXISTS eaut_canteen;
CREATE DATABASE eaut_canteen CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eaut_canteen;

-- ============================================================
-- Reference data
-- ============================================================

CREATE TABLE buildings (
  building_id   INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  description   VARCHAR(255) NULL,
  shipping_fee  DECIMAL(12,0) NOT NULL DEFAULT 0,
  is_active     BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE categories (
  category_id   INT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  is_active     BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- Users
-- ============================================================

CREATE TABLE users (
  user_id        INT AUTO_INCREMENT PRIMARY KEY,
  username       VARCHAR(50) NOT NULL UNIQUE,
  password_hash  VARCHAR(60) NOT NULL,
  full_name      VARCHAR(100) NOT NULL,
  email          VARCHAR(100) NOT NULL UNIQUE,
  phone          VARCHAR(15) NULL,
  role           ENUM('ADMIN','SALES_STAFF','STORE_STAFF','CUSTOMER') NOT NULL,
  status         ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
  building_id    INT NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_building FOREIGN KEY (building_id) REFERENCES buildings(building_id) ON DELETE SET NULL
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- Catalog & inventory (two-tier: warehouse "kho" -> shelf "kệ")
-- ============================================================

CREATE TABLE products (
  product_id     INT AUTO_INCREMENT PRIMARY KEY,
  category_id    INT NOT NULL,
  name           VARCHAR(150) NOT NULL,
  description    VARCHAR(500) NULL,
  price          DECIMAL(12,0) NOT NULL,
  image_filename VARCHAR(255) NULL,
  unit           VARCHAR(20) NULL,
  is_active      BOOLEAN NOT NULL DEFAULT TRUE,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE RESTRICT
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 1:1 with products; a row is created here (quantity 0) in the same transaction as the product insert.
CREATE TABLE warehouse_stock (
  product_id  INT PRIMARY KEY,
  quantity    INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_warehouse_stock_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 1:1 with products; only this quantity is sellable/visible to customers.
CREATE TABLE shelf_stock (
  product_id  INT PRIMARY KEY,
  quantity    INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_shelf_stock_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Nhập hàng: Admin/Manager stock-in receipt (header + lines)
CREATE TABLE stock_imports (
  import_id      INT AUTO_INCREMENT PRIMARY KEY,
  admin_id       INT NOT NULL,
  supplier_name  VARCHAR(150) NULL,
  imported_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note           VARCHAR(255) NULL,
  CONSTRAINT fk_stock_imports_admin FOREIGN KEY (admin_id) REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE stock_import_items (
  import_item_id  INT AUTO_INCREMENT PRIMARY KEY,
  import_id       INT NOT NULL,
  product_id      INT NOT NULL,
  quantity        INT NOT NULL,
  unit_cost       DECIMAL(12,0) NOT NULL,
  CONSTRAINT fk_import_items_import  FOREIGN KEY (import_id)  REFERENCES stock_imports(import_id) ON DELETE CASCADE,
  CONSTRAINT fk_import_items_product FOREIGN KEY (product_id) REFERENCES products(product_id)     ON DELETE RESTRICT
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Store staff transfers stock kho -> kệ; one row per product per physical restock action.
CREATE TABLE stock_transfers (
  transfer_id     INT AUTO_INCREMENT PRIMARY KEY,
  store_staff_id  INT NOT NULL,
  product_id      INT NOT NULL,
  quantity        INT NOT NULL,
  transferred_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_transfers_staff   FOREIGN KEY (store_staff_id) REFERENCES users(user_id)       ON DELETE RESTRICT,
  CONSTRAINT fk_transfers_product FOREIGN KEY (product_id)     REFERENCES products(product_id)  ON DELETE RESTRICT
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ============================================================
-- Orders (channel=ONLINE web orders and channel=COUNTER in-person POS sales)
-- ============================================================

CREATE TABLE orders (
  order_id              INT AUTO_INCREMENT PRIMARY KEY,
  customer_id           INT NULL,          -- NULL for COUNTER walk-in sales
  building_id           INT NULL,          -- NULL for COUNTER channel (picked up in person)
  channel               ENUM('ONLINE','COUNTER') NOT NULL DEFAULT 'ONLINE',
  sold_by               INT NULL,          -- sales staff who rang up a COUNTER sale
  subtotal              DECIMAL(12,0) NOT NULL,
  shipping_fee          DECIMAL(12,0) NOT NULL DEFAULT 0,
  total_amount          DECIMAL(12,0) NOT NULL,
  order_status          ENUM('PENDING','CONFIRMED','REJECTED','SHIPPING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  payment_method        ENUM('COD','VIETQR','CASH') NOT NULL,
  payment_status        ENUM('UNPAID','PAID') NOT NULL DEFAULT 'UNPAID',
  payment_confirmed_by  INT NULL,
  payment_confirmed_at  TIMESTAMP NULL,
  note                  VARCHAR(255) NULL,
  created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_orders_customer  FOREIGN KEY (customer_id)          REFERENCES users(user_id)     ON DELETE SET NULL,
  CONSTRAINT fk_orders_building  FOREIGN KEY (building_id)          REFERENCES buildings(building_id) ON DELETE RESTRICT,
  CONSTRAINT fk_orders_sold_by   FOREIGN KEY (sold_by)              REFERENCES users(user_id)     ON DELETE SET NULL,
  CONSTRAINT fk_orders_paid_by   FOREIGN KEY (payment_confirmed_by) REFERENCES users(user_id)     ON DELETE SET NULL
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE order_items (
  order_item_id  INT AUTO_INCREMENT PRIMARY KEY,
  order_id       INT NOT NULL,
  product_id     INT NOT NULL,
  quantity       INT NOT NULL,
  unit_price     DECIMAL(12,0) NOT NULL,
  line_total     DECIMAL(12,0) NOT NULL,
  CONSTRAINT fk_order_items_order   FOREIGN KEY (order_id)   REFERENCES orders(order_id)     ON DELETE CASCADE,
  CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Audit trail; reject/cancel reasons live here (in `note`), not on `orders`, so `orders` never
-- carries duplicated/driftable state.
CREATE TABLE order_status_history (
  history_id  INT AUTO_INCREMENT PRIMARY KEY,
  order_id    INT NOT NULL,
  old_status  ENUM('PENDING','CONFIRMED','REJECTED','SHIPPING','COMPLETED','CANCELLED') NULL,
  new_status  ENUM('PENDING','CONFIRMED','REJECTED','SHIPPING','COMPLETED','CANCELLED') NOT NULL,
  changed_by  INT NOT NULL,
  note        VARCHAR(255) NULL,
  changed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_history_order FOREIGN KEY (order_id)   REFERENCES orders(order_id) ON DELETE CASCADE,
  CONSTRAINT fk_history_user  FOREIGN KEY (changed_by) REFERENCES users(user_id)   ON DELETE RESTRICT,
  INDEX idx_history_order (order_id)
) ENGINE=InnoDB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_status   ON orders(order_status);
CREATE INDEX idx_products_category ON products(category_id);
