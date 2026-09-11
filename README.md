# Căng tin EAUT — Ứng dụng đặt hàng căng tin trường

Ứng dụng web bán hàng cho căng tin nội bộ trường, xây dựng bằng JSP + Servlet (Jakarta EE 10) và MySQL. Bài tập lớn Java (BTL_JAVA), phát triển dựa trên khung dự án gốc [Sinu00/FoodDeliveryWebApp](https://github.com/Sinu00/FoodDeliveryWebApp) nhưng đã viết lại gần như toàn bộ cho đúng nghiệp vụ căng tin.

## Đối tượng sử dụng

- **Khách hàng** — sinh viên/giảng viên: tự đăng ký qua Google (đăng nhập riêng tại `/login/customer`), xem thực đơn, đặt hàng, thanh toán COD/VietQR/Ví EAUT Pay, theo dõi đơn hàng. Tài khoản đăng ký bằng email `@eaut.edu.vn` (EAUT Smart ID) được tự động giảm giá khi đặt hàng.
- **Nhân viên** — đăng nhập riêng tại `/login/staff`. Không còn giới hạn ở 3 vai trò cố định: Admin tạo và đặt tên vai trò tuỳ ý tại **Vai trò & phân quyền** (`/admin/roles`), rồi chọn chính xác những chức năng vai trò đó được dùng (quản lý danh mục/sản phẩm, tòa nhà, nhập hàng, tài khoản nhân viên, duyệt đơn, bán tại quầy, chuyển hàng lên kệ, giao hàng, nạp ví, xem báo cáo...). Ba vai trò mặc định (Quản lý, Nhân viên bán hàng, Nhân viên cửa hàng) được seed sẵn với phân quyền tương ứng nghiệp vụ gốc, nhưng có thể chỉnh hoặc bổ sung vai trò mới bất cứ lúc nào.

## Công nghệ

- JSP + Jakarta Servlet 6.0 (Tomcat 10.1+), JSTL 3.0, JDBC thuần (không dùng ORM/connection pool — mục tiêu học thuật)
- MySQL 8 (utf8mb4) cho dev local, hoặc PostgreSQL (Neon) cho deploy — `DBConnection` tự chọn driver theo scheme của `DB_URL`, xem [render-deploy.md](render-deploy.md)
- Maven (kèm Maven Wrapper `mvnw`/`mvnw.cmd`, không cần cài Maven riêng)
- Java 17+

## Kiến trúc

Mô hình MVC tách rõ theo package:

```
src/main/java/com/eaut/canteen/
  model/       Model — POJO + enum, không phụ thuộc Servlet API
  dao/         Model — interface truy xuất dữ liệu
  dao/impl/    Model — cài đặt JDBC thuần (mỗi phương thức nhận Connection do caller quản lý transaction)
  controller/  Controller — servlet theo khu vực: auth/, customer/, admin/, sales/, store/
  filter/      SecurityFilter (xác thực + phân quyền theo permission, không hard-code role — xem RoleDAO), EncodingFilter (UTF-8)
  util/        DBConnection, PasswordUtil (bcrypt), FileUploadUtil, VietQRUtil, AppConfig

src/main/webapp/
  WEB-INF/views/   View — toàn bộ JSP (không thể truy cập trực tiếp qua URL), chỉ dùng JSTL/EL
  assets/          CSS dùng chung
```

## Cài đặt & chạy local

### Yêu cầu
- JDK 17+
- MySQL 8 đang chạy (XAMPP/MySQL Server độc lập đều được)
- Tomcat 10.1+ (Jakarta EE 10 — **không dùng Tomcat 9 trở xuống**, sai namespace `javax`/`jakarta`)

### Các bước

1. **Tạo cơ sở dữ liệu** (bản MySQL, dùng cho dev local — bản PostgreSQL cho Neon/Render là `sql/schema.postgres.sql` + `sql/seed.postgres.sql`, xem [render-deploy.md](render-deploy.md)):
   ```
   mysql -u root -p < sql/schema.sql
   mysql -u root -p < sql/seed.sql
   ```
   `seed.sql` tạo sẵn tài khoản quản lý để đăng nhập lần đầu: **admin / admin123** — đổi mật khẩu hoặc xoá tài khoản này trước khi triển khai thật. Cũng seed sẵn 4 vai trò mặc định + toàn bộ danh mục quyền — vào `/admin/roles` sau khi đăng nhập để tạo thêm vai trò hoặc chỉnh phân quyền.

2. **Cấu hình kết nối:**
   ```
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   cp src/main/resources/app.properties.example src/main/resources/app.properties
   ```
   Sửa `db.properties` theo user/password MySQL của bạn. Sửa `app.properties`: `upload.dir` là thư mục lưu ảnh sản phẩm (phải tồn tại và ghi được, nằm ngoài thư mục deploy để không bị Tomcat xoá khi redeploy); `vietqr.*` là thông tin ngân hàng nhận chuyển khoản.

3. **Build:**
   ```
   ./mvnw clean package
   ```
   Sinh ra `target/canteen-webapp.war`.

4. **Deploy:** copy `canteen-webapp.war` vào thư mục `webapps/` của Tomcat 10.1+, khởi động Tomcat.

5. **Truy cập:** `http://localhost:8080/canteen-webapp/`

## Quy trình đơn hàng

```
Đặt hàng (web) ──► PENDING ──► CONFIRMED ──► SHIPPING ──► COMPLETED
                       │  (NV bán hàng duyệt,        (NV cửa hàng lấy hàng   (NV cửa hàng giao xong;
                       │   VietQR phải thanh toán      và bắt đầu giao)       COD tự đánh dấu đã
                       │   trước mới duyệt được)                             thanh toán)
                       ├──► REJECTED (NV bán hàng từ chối)
                       └──► CANCELLED (khách/NV bán hàng/NV cửa hàng huỷ)

Bán tại quầy ──► COMPLETED ngay (NV bán hàng thực hiện trong 1 bước, không qua các bước trên)
```

Tồn kho quản lý 2 cấp: **Kho** (nhập hàng bởi Admin) → **Kệ** (chuyển bởi NV cửa hàng, chỉ hàng trên Kệ mới bán được). Trừ/hoàn kho đều dùng `UPDATE ... WHERE quantity >= ?` để tránh bán vượt tồn khi có nhiều người thao tác cùng lúc.

Mỗi đơn có một mã ticket ngắn (`order_code`, ví dụ `A-142` cho đơn web, `C-142` cho bán tại quầy) sinh từ một sequence riêng — hiển thị cho khách và trên bảng Kanban của nhân viên bán hàng/cửa hàng (`/sales/orders`, `/store/orders`, tự làm mới mỗi 15 giây). Thanh toán hỗ trợ COD, VietQR, tiền mặt tại quầy, và **Ví EAUT Pay** (số dư nội bộ, nạp bởi Admin tại `/admin/wallet`, có sổ ghi giao dịch `wallet_transactions`). Tài khoản đăng ký bằng email `@eaut.edu.vn` được tự động giảm giá theo `smartId.discountPercent` (mặc định 10%) khi đặt hàng qua web.
