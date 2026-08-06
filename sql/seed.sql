-- EAUT Canteen Web App — dev/demo seed data
-- Run after schema.sql. Gives you a way to log in before the staff-account UI exists.

USE eaut_canteen;

-- Dev-only bootstrap admin account. Username: admin  Password: admin123
-- CHANGE OR REMOVE THIS ACCOUNT BEFORE ANY REAL/PUBLIC DEPLOYMENT.
INSERT INTO users (username, password_hash, full_name, email, phone, role, status) VALUES
  ('admin', '$2a$10$rLcFTtNZ1fUrSRRPschYb.SXPR7p7JkrjYiR/9D2tm37ispOtY6Ea', 'Quản trị viên', 'admin@eaut.edu.vn', '0900000000', 'ADMIN', 'ACTIVE');

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
