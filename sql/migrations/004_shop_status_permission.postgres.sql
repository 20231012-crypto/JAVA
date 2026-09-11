-- New permission: toggle the shop-wide "Mở đơn / Tạm ngưng nhận đơn" switch (see ShopStatusServlet).
-- Additive only — safe to run against the already-deployed Neon database.

INSERT INTO permissions (permission_key, group_name, display_name, sort_order) VALUES
  ('shop.status', 'Bán hàng', 'Bật/tắt nhận đơn toàn hệ thống', 105)
ON CONFLICT (permission_key) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, 'shop.status' FROM roles r WHERE r.role_key IN ('ADMIN', 'SALES_STAFF')
ON CONFLICT DO NOTHING;
