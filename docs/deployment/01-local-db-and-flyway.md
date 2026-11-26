# Thiết lập local và nâng cấp MariaDB

## Máy mới

Với JDK 21, Maven 3.9+ và Docker Compose, sao chép `.env.example` thành `.env` và điền mật khẩu MariaDB riêng. Bao giá trị có dấu `#`, ký tự `$` hoặc khoảng trắng bằng nháy đơn.

```powershell
Copy-Item .env.example .env
# Điền DB_PASSWORD trong .env trước khi chạy tiếp.
docker compose up -d --wait
.\scripts\p00.ps1 -Task run
```

Compose dùng MariaDB trên 3307, Redis 6379, MongoDB 27017 và Mailpit 1025. Hộp thư thử nghiệm ở `http://127.0.0.1:8025`. Các cổng chỉ bind loopback. Java chạy trên máy host. Tên database và user mặc định là `beautyshop`. Khi dùng MariaDB cài trực tiếp, chọn `DB_PORT=3306` và tạo database/user một lần nếu máy chưa có. Chạy SQL sau bằng tài khoản quản trị MariaDB local với mật khẩu đã chọn.

```sql
CREATE DATABASE IF NOT EXISTS beautyshop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'beautyshop'@'127.0.0.1' IDENTIFIED BY '<MAT_KHAU_RIENG_CUA_BAN>';
GRANT ALL PRIVILEGES ON beautyshop.* TO 'beautyshop'@'127.0.0.1';
```

Nếu máy đã chạy một dịch vụ ngoài Compose trên các cổng này, chỉ khởi động các dịch vụ còn thiếu bằng cách thêm tên dịch vụ vào lệnh Compose, chẳng hạn `docker compose up -d --wait mariadb redis`.

`p00.ps1` và `seed-demo.ps1` đọc `.env` ở gốc repo. Biến đã có trong phiên shell được ưu tiên. `DB_URL` đã khai báo tiếp tục được dùng nguyên vẹn thay cho `DB_PORT` và `DB_NAME`. Với IntelliJ, Maven chạy trực tiếp hoặc `java -jar`, khai báo biến trong cấu hình chạy hoặc dùng launcher PowerShell. File `.env` nằm ngoài Git. Với PowerShell hạn chế chạy script, có thể dùng `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\p00.ps1 -Task run` cho riêng tiến trình này.

MongoDB mặc định giữ tên `beautyshop_demo` để tương thích dữ liệu review local đang có. Khi chạy nhiều schema SQL độc lập, đặt `MONGODB_URI` riêng cho từng schema để tách dữ liệu đánh giá. Dev mặc định gửi thư vào Mailpit. Khi tiếp tục dùng SMTP từ môi trường cũ, giữ host, port, thông tin xác thực và đặt rõ `MAIL_SMTP_AUTH` cùng `MAIL_SMTP_STARTTLS`. Profile production khai báo `DB_URL`, `DB_USER`, `DB_PASSWORD` và các biến thật từ secret store.

## Máy đang dùng database của bản cũ

Với đợt cập nhật FAQ UI-COPY-01, nếu máy đã chạy V5 cũ, tạo database trống cùng quyền cho tài khoản ứng dụng. Khi dùng cấu hình local mặc định, đặt `DB_NAME=beautyshop_moi` trong `.env`. Nếu đã khai báo `DB_URL`, đổi tên database ở cuối URL cho khớp. Với demo có thể dùng tên `beautyshop_demo_moi`. Tách cả `MONGODB_URI` nếu muốn bắt đầu với kho đánh giá trống.

Các bước dưới áp dụng cho trường hợp cần giữ dữ liệu và lịch sử Flyway khớp với nhánh hiện tại.

1. Dừng app cũ. Ghi nhận database, phiên bản app và lịch sử Flyway. Sao lưu MariaDB, MongoDB nếu có đánh giá và thư mục ảnh upload. Thử khôi phục vào schema riêng trước khi chuyển database đang dùng. Với Compose, giữ volume hiện tại. `DB_NAME`, `DB_USER` và `DB_PASSWORD` của Compose chỉ tạo database/user khi volume khởi tạo lần đầu.
2. Giữ kết nối cũ trong `.env` hoặc shell. Kiểm schema bằng client MariaDB nếu có nghi ngờ về môi trường.

```sql
SELECT DATABASE();
SELECT version, description, checksum, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

3. Trên bản sao phù hợp, chạy migration và khởi động dev. Bước `migrate` chỉ nâng schema. Bước `run` mở web và Hibernate xác thực schema.

```powershell
.\scripts\p00.ps1 -Task migrate
.\scripts\p00.ps1 -Task run
```

| Trạng thái database | Kết quả hoặc xử lý |
|---|---|
| Trống | Flyway tạo bảng lịch sử và áp dụng V1–V7. |
| Đã áp dụng V1 đến V4 với checksum khớp repo | Flyway áp dụng các phiên bản còn thiếu, gồm V5 với nội dung FAQ đã chuẩn hóa. |
| Đã áp dụng V5, V6 hoặc V7 với checksum khớp repo | Flyway kiểm lịch sử rồi áp dụng những phiên bản còn thiếu. V7 bổ sung dữ liệu xác thực đa phương thức và giữ ID người dùng. |
| Đã áp dụng V5 trước UI-COPY-01 | Flyway báo khác checksum. Với đợt chuyển này, nhóm dùng một database trống và trỏ `DB_URL` đến database mới để Flyway nạp FAQ đã sửa. |
| Có bảng nhưng thiếu lịch sử, dùng V1 tiếng Anh hoặc checksum khác | Dừng ở lỗi Flyway. Cần bản SQL cũ và đối chiếu schema/dữ liệu để lập đường chuyển riêng trên bản sao. Giữ lịch sử hiện tại cho đến khi có phương án. |

Các file V1–V7 trong `src/main/resources/db/migration` là lịch sử triển khai, mỗi phiên bản có một file chuẩn trong nhánh hiện tại. Thay đổi schema tiếp theo dùng phiên bản mới. `baseline-on-migrate=false` và `ddl-auto=validate` được giữ ở dev/prod. Sao lưu và khôi phục dữ liệu mẫu xem [runbook demo](06-p10-demo.md). [Kiểm checksum Flyway](https://documentation.red-gate.com/flyway/reference/commands/validate)
