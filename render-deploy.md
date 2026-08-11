# Deploy lên Render (DB chạy tại máy local)

## Giới hạn cần biết trước

- **DB chạy trên máy bạn** → app trên Render chỉ hoạt động khi máy tính này **đang bật** và
  **ngrok đang chạy**. Tắt máy/tắt ngrok = app trên Render mất kết nối DB.
- **Ảnh sản phẩm upload trên Render bị mất khi redeploy/restart** — ổ đĩa của Render là ephemeral
  (không lưu trạng thái). Muốn ảnh tồn tại lâu dài cần Render Persistent Disk (trả phí) hoặc
  chuyển sang lưu ảnh ở dịch vụ ngoài (S3, Cloudinary...). Ngoài phạm vi hôm nay — chấp nhận giới
  hạn này cho mục đích demo/nộp bài trước.
- Đây là giải pháp phù hợp để **demo/nộp bài**, không phải kiến trúc production thật.

## Bước 1 — Cho MySQL local nhận kết nối từ xa

1. Mở file cấu hình MySQL (`my.ini` trên Windows, thường ở
   `C:\ProgramData\MySQL\MySQL Server 8.0\my.ini`), tìm dòng `bind-address` và đổi thành:
   ```
   bind-address = 0.0.0.0
   ```
   Khởi động lại service MySQL sau khi sửa.

2. Tạo (hoặc sửa) user MySQL cho phép kết nối từ host bất kỳ (không chỉ `localhost`):
   ```sql
   CREATE USER 'canteen_app'@'%' IDENTIFIED BY 'MAT_KHAU_MANH_O_DAY';
   GRANT ALL PRIVILEGES ON eaut_canteen.* TO 'canteen_app'@'%';
   FLUSH PRIVILEGES;
   ```
   Dùng user riêng này (không dùng `root`) khi expose ra internet.

## Bước 2 — Mở tunnel ngrok cho cổng 3306 (MySQL)

```
ngrok tcp 3306
```

Ngrok in ra một địa chỉ dạng:
```
Forwarding   tcp://0.tcp.ngrok.io:12345 -> localhost:3306
```

Ghi lại **host** (`0.tcp.ngrok.io`) và **port** (`12345`) — đây là địa chỉ Render sẽ dùng để kết
nối vào MySQL của bạn.

> Lưu ý: với ngrok bản miễn phí, địa chỉ này **đổi mỗi lần khởi động lại tunnel** → mỗi lần chạy
> lại `ngrok tcp 3306`, phải vào Render cập nhật lại biến `DB_URL`.

## Bước 3 — Deploy trên Render

1. Vào [render.com](https://render.com) → **New** → **Web Service**.
2. Chọn repo GitHub `20231012-crypto/JAVA`, branch `main`.
3. Runtime: chọn **Docker** (Render tự nhận diện `Dockerfile` ở gốc repo).
4. Instance type: Free (đủ cho demo) hoặc Starter.
5. Thêm **Environment Variables** (mục *Environment* trong Render):

   | Key | Value | Ghi chú |
   |---|---|---|
   | `DB_URL` | `jdbc:mysql://0.tcp.ngrok.io:12345/eaut_canteen?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh` | Thay host:port bằng địa chỉ ngrok ở Bước 2 |
   | `DB_USERNAME` | `canteen_app` | |
   | `DB_PASSWORD` | `MAT_KHAU_MANH_O_DAY` | |
   | `GOOGLE_CLIENTID` | `958284026141-oaeoh76l8j00jv4ms5qcrug35tim8ber.apps.googleusercontent.com` | Lấy từ `app.properties` hiện tại |
   | `UPLOAD_DIR` | `/tmp/eaut-canteen-uploads` | Thư mục trong container Render (mất khi restart, xem giới hạn ở trên) |
   | `VIETQR_BANKID` | `970436` | |
   | `VIETQR_ACCOUNTNO` | `0000000000` | |
   | `VIETQR_ACCOUNTNAME` | `CANTEEN EAUT` | |

6. Bấm **Create Web Service** → Render tự build Docker image và deploy.
7. Sau khi deploy xong, Render cấp một URL dạng `https://<ten-service>.onrender.com`.

## Bước 4 — Cập nhật Google Cloud Console

Vào Google Cloud Console → APIs & Services → Credentials → OAuth Client ID đang dùng, thêm:

- **Authorized JavaScript origins**: `https://<ten-service>.onrender.com`
- **Authorized redirect URIs**: `https://<ten-service>.onrender.com/auth/google`

(Vẫn giữ nguyên các URL `localhost:8080` cũ nếu còn test local.)

## Kiểm tra sau khi deploy

- Mở `https://<ten-service>.onrender.com/products` — phải load được danh sách sản phẩm (chứng tỏ
  kết nối DB qua ngrok hoạt động).
- Thử đăng nhập admin/nhân viên (username/password) và đăng nhập Google.
- Nếu lỗi 500 ngay khi mở trang: kiểm tra Render → Logs, thường do `DB_URL` sai hoặc tunnel ngrok
  đã đổi địa chỉ.

## Mỗi lần khởi động lại máy / ngrok

1. Bật MySQL (nếu chưa tự khởi động cùng Windows).
2. Chạy lại `ngrok tcp 3306`.
3. Lấy địa chỉ tunnel mới → vào Render → Environment → sửa `DB_URL` → **Save Changes** (Render tự
   redeploy).
