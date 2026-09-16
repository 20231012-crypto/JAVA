-- Let a manager open the till.
--
-- Migration 018 deliberately withheld sales.counter from ADMIN, on the reasoning that ringing up a
-- walk-in sale is a cashier's job. That reasoning does not survive contact with the requirement:
-- the admin dashboard now offers the two sales channels — the student website and the POS — and a
-- manager who is shown the till but refused at the door is worse than one who was never shown it.
--
-- In a canteen this size the manager covers the counter anyway. Any organisation that wants the
-- separation back can take the permission off the role in /admin/roles without touching code,
-- which is the whole point of the permissions being data.
--
-- Data only, idempotent — safe to run against the live database.

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, 'sales.counter' FROM roles r WHERE r.role_key = 'ADMIN'
ON CONFLICT DO NOTHING;
