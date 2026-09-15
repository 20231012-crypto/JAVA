-- Fixes two permission problems that predate this rebuild.
--
-- PROBLEM 1 — the ADMIN role cannot see orders at all.
-- seed.postgres.sql grants ADMIN eleven permissions, and orders.queue / orders.action are not
-- among them: they were given to SALES_STAFF only. That is defensible while /sales/orders is a
-- cashier's screen, but the new /admin/orders screen exists precisely so a manager can follow an
-- order and intervene, and orders.refund is useless to somebody who cannot reach an order in the
-- first place. ADMIN gets queue + action here.
-- Deliberately NOT granted: sales.counter (ringing up a walk-in sale is a cashier's job, and an
-- admin who needs it can be given it in /admin/roles) and store.transfer / store.fulfillment
-- (shop-floor tasks with their own screens).
--
-- PROBLEM 2 — seed drift. attendance.view and customers.manage are enforced by SecurityFilter but
-- were only ever added by migrations 010 and 011, never backported into seed.postgres.sql. A
-- database built from schema + seed alone therefore has 17 permission rows instead of 19, and its
-- ADMIN gets a 403 on /admin/attendance and /admin/customers. The seed file is corrected in the
-- same change as this migration; these INSERTs repair databases that already exist.
--
-- Data only, all idempotent — safe to run against the live database.

INSERT INTO permissions (permission_key, group_name, display_name, sort_order) VALUES
  ('attendance.view',  'Quản trị', 'Xem báo cáo chấm công nhân viên', 55),
  ('customers.manage', 'Quản trị', 'Quản lý tài khoản khách hàng',    52)
ON CONFLICT (permission_key) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, k.permission_key
  FROM roles r
  CROSS JOIN (VALUES
      ('orders.queue'),
      ('orders.action'),
      ('attendance.view'),
      ('customers.manage')
    ) AS k(permission_key)
  WHERE r.role_key = 'ADMIN'
ON CONFLICT DO NOTHING;
