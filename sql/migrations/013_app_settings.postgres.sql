-- Business configuration moves from app.properties into the database.
--
-- Until now every business rule lived in /app.properties, read through util/AppConfig with an
-- env-var override: the loyalty earn rate, the redeem value, the Smart ID discount. Changing any
-- of them meant editing a file and redeploying, which is not something a canteen manager can do.
-- The new admin "Cài đặt" screen needs to write them at runtime, so they need a home in the DB.
--
-- Key/value rows rather than one wide settings row: the settings screen renders itself FROM these
-- rows — group_name groups them into sections, display_name is the field label, and value_type
-- chooses the input control and the parser. Adding a setting later is one INSERT, not a schema
-- change plus a form edit. The cost is that values are text and typed on read, which is why
-- value_type is a CHECK rather than a free string.
--
-- AppConfig does NOT go away: secrets and infrastructure (db.*, google.clientId, vietqr.*,
-- upload.dir) stay there, because those belong to the deployment, not to the business. Settings
-- reads the DB first and falls back to AppConfig, so a key that is missing here still resolves.
--
-- Additive only — safe to run against the live database.

CREATE TABLE IF NOT EXISTS app_settings (
  setting_key   VARCHAR(60) PRIMARY KEY,
  setting_value VARCHAR(500) NOT NULL,
  value_type    VARCHAR(10)  NOT NULL DEFAULT 'STRING'
                CHECK (value_type IN ('STRING', 'INT', 'DECIMAL', 'BOOL', 'TIME')),
  group_name    VARCHAR(40)  NOT NULL,
  display_name  VARCHAR(150) NOT NULL,
  hint          VARCHAR(255) NULL,
  sort_order    INT          NOT NULL DEFAULT 0,
  updated_by    INT          NULL REFERENCES users(user_id) ON DELETE SET NULL,
  updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seeded with exactly the values the code uses today, so running this migration changes no
-- behaviour on its own: loyalty.vndPerPoint 10000 and loyalty.redeemValuePerPoint 500 are the
-- Java-side defaults in OrderFulfillmentServlet/CheckoutServlet, and smartId.discountPercent 10
-- is CheckoutServlet's DEFAULT_SMART_ID_DISCOUNT_PERCENT.
INSERT INTO app_settings (setting_key, setting_value, value_type, group_name, display_name, hint, sort_order) VALUES
  ('canteen.timezone',             'Asia/Ho_Chi_Minh', 'STRING',  'Vận hành',  'Múi giờ căng tin',              'Mọi mốc ngày trong báo cáo tính theo múi giờ này', 10),
  ('canteen.openTime',             '06:30',            'TIME',    'Vận hành',  'Giờ mở cửa',                    'Ngoài khung giờ này khách không đặt được đơn',      20),
  ('canteen.closeTime',            '18:00',            'TIME',    'Vận hành',  'Giờ đóng cửa',                  NULL,                                                30),
  ('shipping.freeOverAmount',      '0',                'INT',     'Vận hành',  'Miễn phí ship cho đơn từ (đ)',  '0 = không áp dụng, luôn thu phí theo tòa nhà',      40),
  ('stock.defaultLowThreshold',    '5',                'INT',     'Kho',       'Ngưỡng cảnh báo sắp hết mặc định', 'Dùng cho món chưa đặt ngưỡng riêng',             50),
  ('loyalty.vndPerPoint',          '10000',            'INT',     'Tích điểm', 'Số tiền đổi được 1 điểm (đ)',   'Ví dụ 10.000đ = 1 điểm',                            60),
  ('loyalty.redeemValuePerPoint',  '500',              'INT',     'Tích điểm', 'Giá trị 1 điểm khi thanh toán (đ)', 'Ví dụ 1 điểm = 500đ',                           70),
  ('smartId.discountPercent',      '10',               'DECIMAL', 'Tích điểm', 'Chiết khấu EAUT Smart ID (%)',  'Áp dụng cho sinh viên có email @eaut.edu.vn',       80),
  ('wallet.discountPercent',       '3',                'DECIMAL', 'Tích điểm', 'Chiết khấu khi trả bằng Ví EAUT Pay (%)', 'Khuyến khích dùng ví nội bộ',             90),
  ('alert.soundEnabled',           'true',             'BOOL',    'Thông báo', 'Bật âm báo đơn mới',            NULL,                                               100),
  ('alert.pollSeconds',            '20',               'INT',     'Thông báo', 'Tần suất kiểm tra đơn mới (giây)', 'Càng nhỏ càng nhanh nhưng tốn tài nguyên hơn',  110),
  ('effects.mode',                 'NONE',             'STRING',  'Giao diện', 'Hiệu ứng trang chủ',            'NONE / SNOW / FIREWORKS / LEAVES',                 120),
  ('effects.intensity',            '2',                'INT',     'Giao diện', 'Mức độ hiệu ứng',               '1 = nhẹ, 2 = vừa, 3 = mạnh',                       130)
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO permissions (permission_key, group_name, display_name, sort_order) VALUES
  ('settings.manage', 'Quản trị', 'Cấu hình hệ thống & giao diện', 160)
ON CONFLICT (permission_key) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, 'settings.manage' FROM roles r WHERE r.role_key = 'ADMIN'
ON CONFLICT DO NOTHING;
