-- Category hierarchy for the mega-menu (Danh mục sản phẩm). The 3 existing categories become
-- top-level "group" nodes (no products directly under them any more); 10 new leaf sub-categories
-- are added under them and every existing product is reassigned to its new leaf category.
-- Additive/reassignment only — no rows or columns are dropped. Safe to run against the live database.

ALTER TABLE categories ADD COLUMN IF NOT EXISTS parent_category_id INT NULL REFERENCES categories(category_id) ON DELETE SET NULL;

INSERT INTO categories (name, parent_category_id)
SELECT 'Cơm & Xôi', category_id FROM categories WHERE name = 'Đồ ăn'
UNION ALL SELECT 'Mì & Bún', category_id FROM categories WHERE name = 'Đồ ăn'
UNION ALL SELECT 'Bánh mì & Bánh bao', category_id FROM categories WHERE name = 'Đồ ăn'
UNION ALL SELECT 'Đồ ăn khác', category_id FROM categories WHERE name = 'Đồ ăn'
UNION ALL SELECT 'Cà phê & Trà', category_id FROM categories WHERE name = 'Đồ uống'
UNION ALL SELECT 'Nước ép & Sinh tố', category_id FROM categories WHERE name = 'Đồ uống'
UNION ALL SELECT 'Đồ uống khác', category_id FROM categories WHERE name = 'Đồ uống'
UNION ALL SELECT 'Món chiên', category_id FROM categories WHERE name = 'Snack - Ăn vặt'
UNION ALL SELECT 'Món nướng', category_id FROM categories WHERE name = 'Snack - Ăn vặt'
UNION ALL SELECT 'Snack đặc biệt', category_id FROM categories WHERE name = 'Snack - Ăn vặt';

UPDATE products SET category_id = (SELECT category_id FROM categories WHERE name = 'Cơm & Xôi')
WHERE name IN ('Cơm gà xối mỡ', 'Cơm rang dưa bò', 'Xôi mặn thập cẩm');

UPDATE products SET category_id = (SELECT category_id FROM categories WHERE name = 'Mì & Bún')
WHERE name IN ('Bún bò Huế', 'Mì trộn xúc xích', 'Mì Ý sốt bò băm');

UPDATE products SET category_id = (SELECT category_id FROM categories WHERE name = 'Bánh mì & Bánh bao')
WHERE name IN ('Bánh mì trứng', 'Bánh mì kẹp thập cẩm', 'Bánh mì chảo mini', 'Bánh bao thịt trứng cút');

UPDATE products SET category_id = (SELECT category_id FROM categories WHERE name = 'Đồ ăn khác')
WHERE name IN ('Hamburger mini');

UPDATE products SET category_id = (SELECT category_id FROM categories WHERE name = 'Cà phê & Trà')
WHERE name IN ('Trà đào cam sả', 'Cà phê sữa đá', 'Trà sữa trân châu', 'Trà chanh tắc');

UPDATE products SET category_id = (SELECT category_id FROM categories WHERE name = 'Nước ép & Sinh tố')
WHERE name IN ('Nước ép dưa hấu', 'Sinh tố bơ', 'Nước cam tươi');

UPDATE products SET category_id = (SELECT category_id FROM categories WHERE name = 'Đồ uống khác')
WHERE name IN ('Nước suối');

UPDATE products SET category_id = (SELECT category_id FROM categories WHERE name = 'Món chiên')
WHERE name IN ('Khoai tây lắc phô mai', 'Gà viên popcorn', 'Nem chua rán');

UPDATE products SET category_id = (SELECT category_id FROM categories WHERE name = 'Món nướng')
WHERE name IN ('Bánh tráng nướng');

UPDATE products SET category_id = (SELECT category_id FROM categories WHERE name = 'Snack đặc biệt')
WHERE name IN ('Tokbokki sốt cay phô mai', 'Bánh bông lan trứng muối', 'Snack khoai tây');
