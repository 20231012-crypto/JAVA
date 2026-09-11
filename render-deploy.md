# Deploy lên Render + Neon (PostgreSQL)

## Vì sao đổi từ MySQL/ngrok sang Neon

Bản trước dùng MySQL chạy trên máy cá nhân, lộ ra ngoài qua tunnel ngrok — nghĩa là app trên
Render chỉ sống khi máy đó **đang bật và ngrok đang chạy**, và địa chỉ tunnel đổi mỗi lần khởi
động lại (bản ngrok miễn phí). **Neon** là PostgreSQL managed, có sẵn 24/7, không phụ thuộc máy cá
nhân nào — đây là kiến trúc phù hợp để deploy thật, không chỉ demo tạm.

Ảnh sản phẩm upload trên Render vẫn bị mất khi redeploy/restart (ổ đĩa container là ephemeral).
Muốn ảnh tồn tại lâu dài cần Render Persistent Disk (trả phí) hoặc dịch vụ lưu trữ ngoài (S3,
Cloudinary...) — ngoài phạm vi tài liệu này.

## Bước 1 — Chuẩn bị database trên Neon

1. Tạo project tại [neon.tech](https://neon.tech) (có sẵn nếu bạn đã có `neondb`).
2. Lấy connection string dạng `postgresql://<user>:<password>@<host>/<database>?sslmode=require`
   từ Neon Console → Connection Details.
3. Khởi tạo schema — chạy `sql/schema.postgres.sql` rồi `sql/seed.postgres.sql` (theo đúng thứ tự
   này) trên database đó, qua Neon SQL Editor (dán nội dung file, chạy) hoặc `psql`:
   ```
   psql "postgresql://<user>:<password>@<host>/<database>?sslmode=require" -f sql/schema.postgres.sql
   psql "postgresql://<user>:<password>@<host>/<database>?sslmode=require" -f sql/seed.postgres.sql
   ```
   `seed.postgres.sql` tạo sẵn tài khoản quản lý: **admin / admin123** — đổi mật khẩu hoặc xoá tài
   khoản này trước khi dùng thật, và tạo sẵn 4 vai trò mặc định + toàn bộ danh mục quyền (xem
   `/admin/roles` sau khi đăng nhập để tạo thêm vai trò hoặc chỉnh quyền).
4. **Không chạy `sql/schema.sql`/`sql/seed.sql`** (bản MySQL) lên Neon — hai cú pháp không tương
   thích (ENUM/AUTO_INCREMENT/ENGINE=... là MySQL-only). Bản MySQL vẫn dùng được cho dev local theo
   README nếu bạn chạy Tomcat trên máy mình.

## Bước 2 — Deploy trên Render

1. Vào [render.com](https://render.com) → **New** → **Web Service**.
2. Chọn repo GitHub `20231012-crypto/JAVA`, branch `main`.
3. Runtime: **Docker** (Render tự nhận diện `Dockerfile` ở gốc repo — driver PostgreSQL đã có sẵn
   trong `pom.xml`, `DBConnection` tự chọn đúng driver theo scheme của `DB_URL`, không cần khai báo
   thêm biến `DB_DRIVER`).
4. Instance type: Free (đủ cho demo) hoặc Starter.
5. Thêm **Environment Variables** (mục *Environment* trong Render):

   | Key | Value | Ghi chú |
   |---|---|---|
   | `DB_URL` | `jdbc:postgresql://<host>/<database>?sslmode=require` | Thêm tiền tố `jdbc:` vào connection string Neon; **bỏ** `channel_binding` (tham số riêng của libpq, driver JDBC không hiểu) |
   | `DB_USERNAME` | `<user>` từ Neon | |
   | `DB_PASSWORD` | `<password>` từ Neon | |
   | `GOOGLE_CLIENTID` | Client ID Google OAuth đang dùng | |
   | `UPLOAD_DIR` | `/tmp/eaut-canteen-uploads` | Thư mục trong container Render (mất khi restart, xem giới hạn ở trên) |
   | `VIETQR_BANKID` | `970436` | |
   | `VIETQR_ACCOUNTNO` | `0000000000` | |
   | `VIETQR_ACCOUNTNAME` | `CANTEEN EAUT` | |
   | `SMARTID_DISCOUNTPERCENT` | `10` | Tuỳ chọn — % giảm giá tự động cho tài khoản @eaut.edu.vn, mặc định 10 nếu bỏ trống |

6. Bấm **Create Web Service** → Render tự build Docker image và deploy.
7. Sau khi deploy xong, Render cấp một URL dạng `https://<ten-service>.onrender.com`.

## Bước 3 — Cập nhật Google Cloud Console

Vào Google Cloud Console → APIs & Services → Credentials → OAuth Client ID đang dùng, thêm:

- **Authorized JavaScript origins**: `https://<ten-service>.onrender.com`
- **Authorized redirect URIs**: `https://<ten-service>.onrender.com/auth/google`

(Vẫn giữ nguyên các URL `localhost:8080` cũ nếu còn test local.)

## Kiểm tra sau khi deploy

- Mở `https://<ten-service>.onrender.com/products` — phải load được danh sách sản phẩm (chứng tỏ
  kết nối Neon hoạt động).
- Thử đăng nhập nhân viên tại `/login/staff` (username/password) và khách hàng tại `/login/customer`
  (Google Sign-In).
- Nếu lỗi 500 ngay khi mở trang: kiểm tra Render → Logs. Lỗi thường gặp:
  - `DB_URL` thiếu tiền tố `jdbc:`, hoặc còn tham số `channel_binding` (driver JDBC không nhận).
  - Sai mật khẩu/host Neon (kiểm tra lại trong Neon Console → có thể cần reset password nếu đã
    từng lộ connection string ra ngoài).
  - Chưa chạy `schema.postgres.sql`/`seed.postgres.sql` lên đúng database đang trỏ tới.
