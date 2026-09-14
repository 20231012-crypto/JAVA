-- Admin-configurable banners, placed by position (HEAD/FOOTER/LEFT/RIGHT on the catalog page).
-- Images are stored as bytea directly in the database — NOT the upload directory — because the
-- upload directory is Render container-local and is wiped on every redeploy (the same reason
-- the seeded dish photos were moved into the WAR itself). A DB-stored banner image survives
-- redeploys with no extra infrastructure.
-- Additive only — safe to run against the live database.

CREATE TABLE IF NOT EXISTS banners (
  banner_id          SERIAL PRIMARY KEY,
  position            VARCHAR(10) NOT NULL CHECK (position IN ('HEAD', 'FOOTER', 'LEFT', 'RIGHT')),
  title               VARCHAR(150) NULL,
  subtitle            VARCHAR(300) NULL,
  link_url            VARCHAR(500) NULL,
  image_data          BYTEA NULL,
  image_content_type  VARCHAR(100) NULL,
  sort_order          INT NOT NULL DEFAULT 0,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO permissions (permission_key, group_name, display_name, sort_order) VALUES
  ('banners.manage', 'Quản trị', 'Quản lý banner trang chủ', 45)
ON CONFLICT (permission_key) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, 'banners.manage' FROM roles r WHERE r.role_key = 'ADMIN'
ON CONFLICT DO NOTHING;
