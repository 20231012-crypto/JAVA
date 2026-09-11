-- EAUT Canteen Web App — PostgreSQL dev/demo seed data (Render + Neon deploy)
-- Run after schema.postgres.sql. Gives you a way to log in before the roles UI has been used
-- to create anything further.

-- Permissions catalog: one row per real, enforced action/functional area (see SecurityFilter).
-- This table is a fixed catalog, not admin-editable — adding a new permission means adding the
-- enforcement code for it too, not just a row here. What IS admin-editable is which roles have
-- which of these (via /admin/roles), and how many roles exist.
INSERT INTO permissions (permission_key, group_name, display_name, sort_order) VALUES
  ('admin.dashboard',        'Quản trị',       'Xem tổng quan quản trị',              10),
  ('categories.manage',      'Quản trị',       'Quản lý danh mục',                    20),
  ('products.manage',        'Quản trị',       'Quản lý sản phẩm',                    30),
  ('buildings.manage',       'Quản trị',       'Quản lý tòa nhà & phí ship',           40),
  ('staff.manage',           'Quản trị',       'Quản lý tài khoản nhân viên',          50),
  ('roles.manage',           'Quản trị',       'Quản lý vai trò & phân quyền',         60),
  ('stock.import',           'Kho & vận hành', 'Nhập hàng vào kho',                    70),
  ('store.transfer',         'Kho & vận hành', 'Chuyển hàng kho lên kệ',               80),
  ('store.fulfillment',      'Kho & vận hành', 'Lấy hàng & giao đơn đã duyệt',         90),
  ('orders.queue',           'Bán hàng',       'Xem hàng đợi đơn đặt online',         100),
  ('orders.action',          'Bán hàng',       'Duyệt / từ chối / hủy đơn',           110),
  ('orders.payment_confirm', 'Bán hàng',       'Xác nhận thanh toán VietQR',          120),
  ('sales.counter',          'Bán hàng',       'Bán hàng trực tiếp tại quầy',         130),
  ('shop.status',            'Bán hàng',       'Bật/tắt nhận đơn toàn hệ thống',      105),
  ('wallet.topup',           'Tài chính',      'Nạp ví EAUT Pay cho khách hàng',      140),
  ('reports.view',           'Tài chính',      'Xem báo cáo doanh thu',               150);

-- Default roles. is_system=TRUE just protects these four from deletion (so the app can't be
-- left with zero admin-capable roles); their permissions are still fully editable from
-- /admin/roles like any other role, and any number of additional roles can be created there.
INSERT INTO roles (role_key, display_name, is_system, is_customer_default) VALUES
  ('ADMIN',       'Quản lý',            TRUE, FALSE),
  ('SALES_STAFF', 'Nhân viên bán hàng', TRUE, FALSE),
  ('STORE_STAFF', 'Nhân viên cửa hàng', TRUE, FALSE),
  ('CUSTOMER',    'Khách hàng',         TRUE, TRUE);

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, p.permission_key FROM roles r, permissions p
  WHERE r.role_key = 'ADMIN' AND p.permission_key IN
    ('admin.dashboard','categories.manage','products.manage','buildings.manage',
     'staff.manage','roles.manage','stock.import','wallet.topup','reports.view','shop.status');

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, p.permission_key FROM roles r, permissions p
  WHERE r.role_key = 'SALES_STAFF' AND p.permission_key IN
    ('orders.queue','orders.action','orders.payment_confirm','sales.counter','shop.status');

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, p.permission_key FROM roles r, permissions p
  WHERE r.role_key = 'STORE_STAFF' AND p.permission_key IN
    ('store.transfer','store.fulfillment');

-- CUSTOMER intentionally gets no rows in role_permissions: the customer-facing routes
-- (/products, /cart, /checkout, /orders) are gated by "is anyone logged in" / ownership checks,
-- not by a permission key, so no admin.*/sales.*/store.* grant is ever needed for shoppers.

-- Dev-only bootstrap admin account. Username: admin  Password: admin123
-- CHANGE OR REMOVE THIS ACCOUNT BEFORE ANY REAL/PUBLIC DEPLOYMENT.
INSERT INTO users (username, password_hash, full_name, email, phone, role_id, status)
  SELECT 'admin', '$2a$10$rLcFTtNZ1fUrSRRPschYb.SXPR7p7JkrjYiR/9D2tm37ispOtY6Ea',
         'Quản trị viên', 'admin@eaut.edu.vn', '0900000000', role_id, 'ACTIVE'
  FROM roles WHERE role_key = 'ADMIN';

INSERT INTO buildings (name, description, shipping_fee) VALUES
  ('Tòa A', 'Khu giảng đường A', 5000),
  ('Tòa B', 'Khu giảng đường B', 5000),
  ('Tòa C', 'Khu giảng đường C', 7000),
  ('Ký túc xá', 'Khu ký túc xá sinh viên', 10000);

INSERT INTO categories (name) VALUES
  ('Đồ ăn'),
  ('Đồ uống'),
  ('Snack - Ăn vặt');

INSERT INTO products (category_id, name, description, price, unit) VALUES
  (1, 'Cơm gà xối mỡ', 'Cơm gà giòn kèm rau dưa và nước mắm', 30000, 'phần'),
  (1, 'Bún bò Huế', 'Bún bò cay đậm đà kiểu Huế', 32000, 'tô'),
  (1, 'Bánh mì trứng', 'Bánh mì giòn kèm trứng ốp và pate', 18000, 'ổ'),
  (2, 'Trà đào cam sả', 'Trà đào mát lạnh thêm cam sả', 20000, 'ly'),
  (2, 'Nước suối', 'Nước suối đóng chai 500ml', 8000, 'chai'),
  (3, 'Snack khoai tây', 'Snack khoai tây lát giòn', 10000, 'gói');

INSERT INTO warehouse_stock (product_id, quantity)
  SELECT product_id, 100 FROM products;

INSERT INTO shelf_stock (product_id, quantity)
  SELECT product_id, 30 FROM products;
