-- Adds real dish/drink photography to the catalog: 20 new products (reusing the 3 existing
-- categories) plus a real photo for the "Trà đào cam sả" product already seeded. Every
-- image_filename here is served from the WAR itself (see ImageServlet's /WEB-INF/seed-images/
-- fallback), not the ephemeral upload directory, so it survives every Render redeploy.
-- Additive only — safe to run against the live database.

-- Đồ ăn (category 1)
INSERT INTO products (category_id, name, description, price, image_filename, unit, avg_prep_minutes)
SELECT 1, v.name, v.description, v.price, v.image_filename, 'phần', v.avg_prep_minutes
FROM (VALUES
  ('Mì trộn xúc xích', 'Mì trộn tương ớt cay, trứng lòng đào, xúc xích chiên', 20000, '01_mi_tron_xuc_xich.jpg', 12),
  ('Cơm rang dưa bò', 'Cơm rang dưa chua thịt bò giòn thơm nóng hổi', 28000, '02_com_rang_dua_bo.jpg', 12),
  ('Bánh mì kẹp thập cẩm', 'Bánh mì kẹp pate, chả lụa, trứng ốp la, dưa leo, rau ngò', 20000, '03_banh_mi_kep_thap_cam.jpg', 6),
  ('Bánh mì chảo mini', 'Bánh mì chảo sốt cà chua, xíu mại, xúc xích', 18000, '04_banh_mi_chao_mini.jpg', 8),
  ('Xôi mặn thập cẩm', 'Xôi mặn thập cẩm lạp xưởng, gà xé, trứng cút, chà bông', 25000, '05_xoi_man_thap_cam.jpg', 10),
  ('Mì Ý sốt bò băm', 'Mì Ý sốt bò băm cà chua phô mai', 30000, '06_mi_y_sot_bo_bam.jpg', 12),
  ('Bánh bao thịt trứng cút', 'Bánh bao hấp nóng nhân thịt, trứng cút, lạp xưởng', 15000, '11_banh_bao_thit_trung_cut.jpg', 5),
  ('Hamburger mini', 'Hamburger bò phô mai size mini', 20000, '19_hamburger_mini.jpg', 8)
) AS v(name, description, price, image_filename, avg_prep_minutes);

-- Đồ uống (category 2)
INSERT INTO products (category_id, name, description, price, image_filename, unit, avg_prep_minutes)
SELECT 2, v.name, v.description, v.price, v.image_filename, 'ly', v.avg_prep_minutes
FROM (VALUES
  ('Cà phê sữa đá', 'Cà phê phin truyền thống pha cùng sữa đặc, đá', 15000, '13_ca_phe_sua_da.jpg', 5),
  ('Trà sữa trân châu', 'Trà sữa béo thơm kèm trân châu đen dẻo dai', 20000, '15_tra_sua_tran_chau.jpg', 4),
  ('Nước ép dưa hấu', 'Nước ép dưa hấu tươi mát giải nhiệt', 18000, '16_nuoc_ep_dua_hau.jpg', 4),
  ('Sinh tố bơ', 'Sinh tố bơ sánh mịn béo ngậy', 22000, '17_sinh_to_bo.jpg', 5),
  ('Nước cam tươi', 'Nước cam vắt nguyên chất', 18000, '18_nuoc_cam_tuoi.jpg', 4),
  ('Trà chanh tắc', 'Trà chanh tắc chua ngọt giải khát', 12000, '21_tra_chanh_tac.jpg', 3)
) AS v(name, description, price, image_filename, avg_prep_minutes);

-- Snack - Ăn vặt (category 3)
INSERT INTO products (category_id, name, description, price, image_filename, unit, avg_prep_minutes)
SELECT 3, v.name, v.description, v.price, v.image_filename, 'phần', v.avg_prep_minutes
FROM (VALUES
  ('Khoai tây lắc phô mai', 'Khoai tây chiên lắc bột phô mai giòn rụm', 18000, '07_khoai_tay_lac_pho_mai.jpg', 8),
  ('Gà viên popcorn', 'Gà viên chiên xù giòn rụm kèm sốt cay', 20000, '08_ga_vien_popcorn.jpg', 8),
  ('Bánh tráng nướng', 'Bánh tráng nướng Đà Lạt trứng cút, xúc xích, sốt mayo', 20000, '09_banh_trang_nuong.jpg', 10),
  ('Tokbokki sốt cay phô mai', 'Bánh gạo Hàn Quốc sốt cay phủ phô mai béo ngậy', 25000, '10_tokbokki_sot_cay_pho_mai.jpg', 10),
  ('Bánh bông lan trứng muối', 'Bánh bông lan trứng muối sốt hoàng kim béo thơm', 15000, '12_banh_bong_lan_trung_muoi.jpg', 3),
  ('Nem chua rán', 'Nem chua rán giòn kèm tương ớt', 20000, '20_nem_chua_ran.jpg', 8)
) AS v(name, description, price, image_filename, avg_prep_minutes);

-- Stock so the new items actually show as sellable (two-tier warehouse -> shelf model).
INSERT INTO warehouse_stock (product_id, quantity)
SELECT product_id, 100 FROM products
WHERE image_filename IN (
  '01_mi_tron_xuc_xich.jpg','02_com_rang_dua_bo.jpg','03_banh_mi_kep_thap_cam.jpg','04_banh_mi_chao_mini.jpg',
  '05_xoi_man_thap_cam.jpg','06_mi_y_sot_bo_bam.jpg','11_banh_bao_thit_trung_cut.jpg','19_hamburger_mini.jpg',
  '13_ca_phe_sua_da.jpg','15_tra_sua_tran_chau.jpg','16_nuoc_ep_dua_hau.jpg','17_sinh_to_bo.jpg',
  '18_nuoc_cam_tuoi.jpg','21_tra_chanh_tac.jpg',
  '07_khoai_tay_lac_pho_mai.jpg','08_ga_vien_popcorn.jpg','09_banh_trang_nuong.jpg','10_tokbokki_sot_cay_pho_mai.jpg',
  '12_banh_bong_lan_trung_muoi.jpg','20_nem_chua_ran.jpg'
)
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO shelf_stock (product_id, quantity)
SELECT product_id, 30 FROM products
WHERE image_filename IN (
  '01_mi_tron_xuc_xich.jpg','02_com_rang_dua_bo.jpg','03_banh_mi_kep_thap_cam.jpg','04_banh_mi_chao_mini.jpg',
  '05_xoi_man_thap_cam.jpg','06_mi_y_sot_bo_bam.jpg','11_banh_bao_thit_trung_cut.jpg','19_hamburger_mini.jpg',
  '13_ca_phe_sua_da.jpg','15_tra_sua_tran_chau.jpg','16_nuoc_ep_dua_hau.jpg','17_sinh_to_bo.jpg',
  '18_nuoc_cam_tuoi.jpg','21_tra_chanh_tac.jpg',
  '07_khoai_tay_lac_pho_mai.jpg','08_ga_vien_popcorn.jpg','09_banh_trang_nuong.jpg','10_tokbokki_sot_cay_pho_mai.jpg',
  '12_banh_bong_lan_trung_muoi.jpg','20_nem_chua_ran.jpg'
)
ON CONFLICT (product_id) DO NOTHING;

-- Real photo for the pre-existing "Trà đào cam sả" seed product (previously had no image).
UPDATE products SET image_filename = '14_tra_dao_cam_sa.jpg'
WHERE name = 'Trà đào cam sả' AND image_filename IS NULL;
