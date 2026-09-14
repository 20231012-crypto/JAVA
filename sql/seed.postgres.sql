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
  ('banners.manage',         'Quản trị',       'Quản lý banner trang chủ',             45),
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
    ('admin.dashboard','categories.manage','products.manage','buildings.manage','banners.manage',
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

-- Top-level groups (mega-menu column headers) + leaf sub-categories products actually belong to
-- (see DESIGN.md / migration 008 for why: a customer-facing mega-menu needs more than 3 flat
-- categories to be worth showing as a dropdown). Ordering here fixes categories 1-3 as the three
-- top-level groups and 4-13 as their leaf children, so the product inserts below can reference
-- leaf category_id directly.
INSERT INTO categories (name) VALUES
  ('Đồ ăn'),          -- 1
  ('Đồ uống'),        -- 2
  ('Snack - Ăn vặt'); -- 3

INSERT INTO categories (name, parent_category_id) VALUES
  ('Cơm & Xôi', 1),             -- 4
  ('Mì & Bún', 1),              -- 5
  ('Bánh mì & Bánh bao', 1),    -- 6
  ('Đồ ăn khác', 1),            -- 7
  ('Cà phê & Trà', 2),          -- 8
  ('Nước ép & Sinh tố', 2),     -- 9
  ('Đồ uống khác', 2),          -- 10
  ('Món chiên', 3),             -- 11
  ('Món nướng', 3),             -- 12
  ('Snack đặc biệt', 3);        -- 13

INSERT INTO products (category_id, name, description, price, image_filename, unit) VALUES
  (4, 'Cơm gà xối mỡ', 'Cơm gà giòn kèm rau dưa và nước mắm', 30000, NULL, 'phần'),
  (5, 'Bún bò Huế', 'Bún bò cay đậm đà kiểu Huế', 32000, NULL, 'tô'),
  (6, 'Bánh mì trứng', 'Bánh mì giòn kèm trứng ốp và pate', 18000, NULL, 'ổ'),
  (8, 'Trà đào cam sả', 'Trà đào mát lạnh thêm cam sả', 20000, '14_tra_dao_cam_sa.jpg', 'ly'),
  (10, 'Nước suối', 'Nước suối đóng chai 500ml', 8000, NULL, 'chai'),
  (13, 'Snack khoai tây', 'Snack khoai tây lát giòn', 10000, NULL, 'gói');

-- Real dish/drink photography (see sql/migrations/005_dish_catalog_images.postgres.sql for the
-- same catalog applied as an additive migration against the already-deployed database).
INSERT INTO products (category_id, name, description, price, image_filename, unit, avg_prep_minutes) VALUES
  (5, 'Mì trộn xúc xích', 'Mì trộn tương ớt cay, trứng lòng đào, xúc xích chiên', 20000, '01_mi_tron_xuc_xich.jpg', 12),
  (4, 'Cơm rang dưa bò', 'Cơm rang dưa chua thịt bò giòn thơm nóng hổi', 28000, '02_com_rang_dua_bo.jpg', 12),
  (6, 'Bánh mì kẹp thập cẩm', 'Bánh mì kẹp pate, chả lụa, trứng ốp la, dưa leo, rau ngò', 20000, '03_banh_mi_kep_thap_cam.jpg', 6),
  (6, 'Bánh mì chảo mini', 'Bánh mì chảo sốt cà chua, xíu mại, xúc xích', 18000, '04_banh_mi_chao_mini.jpg', 8),
  (4, 'Xôi mặn thập cẩm', 'Xôi mặn thập cẩm lạp xưởng, gà xé, trứng cút, chà bông', 25000, '05_xoi_man_thap_cam.jpg', 10),
  (5, 'Mì Ý sốt bò băm', 'Mì Ý sốt bò băm cà chua phô mai', 30000, '06_mi_y_sot_bo_bam.jpg', 12),
  (6, 'Bánh bao thịt trứng cút', 'Bánh bao hấp nóng nhân thịt, trứng cút, lạp xưởng', 15000, '11_banh_bao_thit_trung_cut.jpg', 5),
  (7, 'Hamburger mini', 'Hamburger bò phô mai size mini', 20000, '19_hamburger_mini.jpg', 8);

INSERT INTO products (category_id, name, description, price, image_filename, unit, avg_prep_minutes) VALUES
  (8, 'Cà phê sữa đá', 'Cà phê phin truyền thống pha cùng sữa đặc, đá', 15000, '13_ca_phe_sua_da.jpg', 5),
  (8, 'Trà sữa trân châu', 'Trà sữa béo thơm kèm trân châu đen dẻo dai', 20000, '15_tra_sua_tran_chau.jpg', 4),
  (9, 'Nước ép dưa hấu', 'Nước ép dưa hấu tươi mát giải nhiệt', 18000, '16_nuoc_ep_dua_hau.jpg', 4),
  (9, 'Sinh tố bơ', 'Sinh tố bơ sánh mịn béo ngậy', 22000, '17_sinh_to_bo.jpg', 5),
  (9, 'Nước cam tươi', 'Nước cam vắt nguyên chất', 18000, '18_nuoc_cam_tuoi.jpg', 4),
  (8, 'Trà chanh tắc', 'Trà chanh tắc chua ngọt giải khát', 12000, '21_tra_chanh_tac.jpg', 3);

INSERT INTO products (category_id, name, description, price, image_filename, unit, avg_prep_minutes) VALUES
  (11, 'Khoai tây lắc phô mai', 'Khoai tây chiên lắc bột phô mai giòn rụm', 18000, '07_khoai_tay_lac_pho_mai.jpg', 8),
  (11, 'Gà viên popcorn', 'Gà viên chiên xù giòn rụm kèm sốt cay', 20000, '08_ga_vien_popcorn.jpg', 8),
  (12, 'Bánh tráng nướng', 'Bánh tráng nướng Đà Lạt trứng cút, xúc xích, sốt mayo', 20000, '09_banh_trang_nuong.jpg', 10),
  (13, 'Tokbokki sốt cay phô mai', 'Bánh gạo Hàn Quốc sốt cay phủ phô mai béo ngậy', 25000, '10_tokbokki_sot_cay_pho_mai.jpg', 10),
  (13, 'Bánh bông lan trứng muối', 'Bánh bông lan trứng muối sốt hoàng kim béo thơm', 15000, '12_banh_bong_lan_trung_muoi.jpg', 3),
  (11, 'Nem chua rán', 'Nem chua rán giòn kèm tương ớt', 20000, '20_nem_chua_ran.jpg', 8);

INSERT INTO warehouse_stock (product_id, quantity)
  SELECT product_id, 100 FROM products;

INSERT INTO shelf_stock (product_id, quantity)
  SELECT product_id, 30 FROM products;
