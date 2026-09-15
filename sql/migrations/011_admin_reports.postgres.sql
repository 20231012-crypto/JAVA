-- Customer management + revenue/best-seller reporting for the rebuilt admin area.
--
-- No new tables: customers are already users rows (role with is_customer_default = TRUE), and
-- every number the reports need is derivable from orders / order_items. What was missing was
-- a permission to gate the new screens, and one index.
--
-- Why the index: the best-seller report and the "Hot hit tuần" catalog section both GROUP BY
-- order_items.product_id, and that column had no index (the only indexes on order_items were
-- via its primary key and the orders FK). At current data volume a sequential scan is fine, but
-- this table is the one that grows with every single line of every order, so it is the one place
-- an index is worth adding ahead of time.
--
-- Deliberately NOT added: a pg_trgm GIN index for the product-name search. ILIKE '%term%'
-- cannot use a btree index, but the products table holds tens of rows (26 seeded), so a
-- sequential scan is the correct plan and installing an extension for it would be noise.
-- Revisit only if the menu grows into the thousands.
--
-- Additive only — safe to run against the live database.

CREATE INDEX IF NOT EXISTS idx_order_items_product
  ON order_items(product_id);

INSERT INTO permissions (permission_key, group_name, display_name, sort_order) VALUES
  ('customers.manage', 'Quản trị', 'Quản lý tài khoản khách hàng', 52)
ON CONFLICT (permission_key) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, 'customers.manage' FROM roles r WHERE r.role_key = 'ADMIN'
ON CONFLICT DO NOTHING;
