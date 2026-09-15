-- Staff check-in / check-out (chấm công). One row per shift: check_in_at is set when the staff
-- member clocks in, check_out_at stays NULL until they clock out.
--
-- Why a partial unique index instead of an application-level check: two rapid clock-in POSTs
-- (double-click, or two tabs) would both pass a "SELECT ... WHERE check_out_at IS NULL" guard
-- before either inserted, leaving two open shifts and making hours-worked math meaningless.
-- The index makes the database refuse the second one outright, so the race cannot happen.
--
-- This is separate from the existing users.on_duty boolean (a live "is on shift right now"
-- switch read by the sales/store queue boards): on_duty answers "who is working now", this
-- table answers "who worked when, and for how long". Keeping both means the boards keep
-- working unchanged.
--
-- Additive only — safe to run against the live database.

CREATE TABLE IF NOT EXISTS staff_attendance (
  attendance_id  SERIAL PRIMARY KEY,
  user_id        INT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  check_in_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  check_out_at   TIMESTAMP NULL,
  note           VARCHAR(255) NULL,
  CONSTRAINT chk_attendance_order CHECK (check_out_at IS NULL OR check_out_at >= check_in_at)
);

-- At most one open (not yet checked out) shift per staff member.
CREATE UNIQUE INDEX IF NOT EXISTS one_open_shift_per_user
  ON staff_attendance(user_id) WHERE check_out_at IS NULL;

-- Report queries filter by staff member and date range.
CREATE INDEX IF NOT EXISTS idx_attendance_user_day
  ON staff_attendance(user_id, check_in_at);

INSERT INTO permissions (permission_key, group_name, display_name, sort_order) VALUES
  ('attendance.view', 'Quản trị', 'Xem báo cáo chấm công nhân viên', 55)
ON CONFLICT (permission_key) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, 'attendance.view' FROM roles r WHERE r.role_key = 'ADMIN'
ON CONFLICT DO NOTHING;
