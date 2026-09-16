-- Phiếu nhập hàng: từ "ghi sổ sau khi hàng đã vào kho" thành một quy trình có kiểm hàng.
--
-- Cách cũ chỉ có một bước: điền form là kho cộng ngay. Không có chỗ nào ghi lại việc đã đặt
-- những gì mà hàng chưa về, và không có bước đối chiếu giữa số đặt và số thực nhận — mà đó
-- chính là lúc sai sót xảy ra (nhà cung cấp giao thiếu, hàng vỡ, giao nhầm loại). Sổ sách khi
-- ấy luôn khớp với đơn đặt, kể cả khi thực tế trong kho không khớp.
--
-- Mô hình mới theo Sapo: đặt hàng (DRAFT) -> kiểm hàng điền số thực nhận -> xác nhận thì kho mới
-- cộng. Số đã đặt mà chưa nhận là "hàng đang về".
--
-- Không phá dữ liệu: cộng thêm cột, giữ nguyên hàng cũ. Idempotent, chạy lại được.

-- ============================================================
-- Nhà cung cấp — trước đây chỉ là một ô chữ tự do trên phiếu, nên hai người gõ "Cty Minh Anh"
-- và "minh anh" là hai nhà cung cấp khác nhau, và không thể xem lại đã nhập gì từ ai.
-- ============================================================
CREATE TABLE IF NOT EXISTS suppliers (
  supplier_id  SERIAL PRIMARY KEY,
  name         VARCHAR(150) NOT NULL,
  phone        VARCHAR(20)  NULL,
  email        VARCHAR(150) NULL,
  address      VARCHAR(255) NULL,
  note         VARCHAR(255) NULL,
  is_active    BOOLEAN NOT NULL DEFAULT TRUE,
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Trùng tên là lỗi nhập liệu, không phải hai nhà cung cấp thật. Chỉ ràng buộc trên bản ghi còn
-- hoạt động, để tên của một nhà cung cấp đã ngừng vẫn dùng lại được cho bản ghi mới.
CREATE UNIQUE INDEX IF NOT EXISTS uq_suppliers_name_active
  ON suppliers (lower(name)) WHERE is_active;

-- ============================================================
-- Phiếu nhập
-- ============================================================
ALTER TABLE stock_imports ADD COLUMN IF NOT EXISTS code            VARCHAR(20) NULL;
ALTER TABLE stock_imports ADD COLUMN IF NOT EXISTS supplier_id     INT NULL REFERENCES suppliers(supplier_id) ON DELETE SET NULL;
ALTER TABLE stock_imports ADD COLUMN IF NOT EXISTS status          VARCHAR(16) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE stock_imports ADD COLUMN IF NOT EXISTS expected_date   DATE NULL;
ALTER TABLE stock_imports ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(12,0) NOT NULL DEFAULT 0;
ALTER TABLE stock_imports ADD COLUMN IF NOT EXISTS other_cost      DECIMAL(12,0) NOT NULL DEFAULT 0;
ALTER TABLE stock_imports ADD COLUMN IF NOT EXISTS paid_amount     DECIMAL(12,0) NOT NULL DEFAULT 0;
ALTER TABLE stock_imports ADD COLUMN IF NOT EXISTS received_at     TIMESTAMP NULL;
ALTER TABLE stock_imports ADD COLUMN IF NOT EXISTS received_by     INT NULL REFERENCES users(user_id) ON DELETE SET NULL;
ALTER TABLE stock_imports ADD COLUMN IF NOT EXISTS cancelled_at    TIMESTAMP NULL;
ALTER TABLE stock_imports ADD COLUMN IF NOT EXISTS updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- supplier_name ở lại làm bản chụp tại thời điểm nhập, đúng lý do order_items giữ unit_price:
-- đổi tên nhà cung cấp về sau không được viết lại lịch sử của phiếu đã nhập.

-- SL thực nhận. Mặc định 0 = chưa kiểm hàng; quantity giữ nguyên nghĩa là SL đặt.
ALTER TABLE stock_import_items ADD COLUMN IF NOT EXISTS received_quantity INT NOT NULL DEFAULT 0;

-- ============================================================
-- Backfill TRƯỚC khi đặt ràng buộc — phiếu cũ đã cộng kho rồi
-- ============================================================
-- Đây là bước quan trọng nhất của migration. Mọi phiếu cũ đều đã cộng vào warehouse_stock ngay
-- lúc tạo. Nếu để chúng ở DRAFT thì màn hình mới sẽ mời "xác nhận nhập kho" một lần nữa và cộng
-- kho lần thứ hai. Chúng phải vào hệ thống mới ở đúng trạng thái đã hoàn tất.
UPDATE stock_import_items SET received_quantity = quantity WHERE received_quantity = 0;

UPDATE stock_imports
   SET status      = 'RECEIVED',
       received_at = imported_at,
       received_by = admin_id,
       updated_at  = imported_at
 WHERE status = 'DRAFT';

UPDATE stock_imports SET code = 'PN-' || import_id WHERE code IS NULL;

-- ============================================================
-- Ràng buộc — đặt sau backfill để không fail trên dữ liệu thật
-- ============================================================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_stock_imports_status') THEN
    ALTER TABLE stock_imports ADD CONSTRAINT chk_stock_imports_status
      CHECK (status IN ('DRAFT', 'PARTIAL', 'RECEIVED', 'CANCELLED'));
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_stock_imports_money') THEN
    ALTER TABLE stock_imports ADD CONSTRAINT chk_stock_imports_money
      CHECK (discount_amount >= 0 AND other_cost >= 0 AND paid_amount >= 0);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_import_items_received') THEN
    ALTER TABLE stock_import_items ADD CONSTRAINT chk_import_items_received
      CHECK (received_quantity >= 0 AND received_quantity <= quantity);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_import_items_quantity') THEN
    ALTER TABLE stock_import_items ADD CONSTRAINT chk_import_items_quantity
      CHECK (quantity > 0);
  END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_stock_imports_code ON stock_imports(code);
CREATE INDEX IF NOT EXISTS idx_stock_imports_status   ON stock_imports(status, imported_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_imports_supplier ON stock_imports(supplier_id);
CREATE INDEX IF NOT EXISTS idx_import_items_product   ON stock_import_items(product_id);

-- Mã phiếu lấy từ sequence, không suy ra từ import_id sau khi insert — cùng lý do với
-- order_ticket_seq: có mã trước khi có hàng, một câu INSERT, không tranh chấp unique.
CREATE SEQUENCE IF NOT EXISTS stock_import_code_seq START 1;
-- Đẩy sequence lên quá các mã đã backfill, nếu không phiếu mới sẽ đụng 'PN-1'.
SELECT setval('stock_import_code_seq', COALESCE((SELECT MAX(import_id) FROM stock_imports), 0) + 1, false);

-- ============================================================
-- Quyền
-- ============================================================
INSERT INTO permissions (permission_key, group_name, display_name, sort_order) VALUES
  ('suppliers.manage', 'Kho & vận hành', 'Quản lý nhà cung cấp', 72)
ON CONFLICT (permission_key) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_key)
  SELECT r.role_id, 'suppliers.manage' FROM roles r WHERE r.role_key = 'ADMIN'
ON CONFLICT DO NOTHING;
