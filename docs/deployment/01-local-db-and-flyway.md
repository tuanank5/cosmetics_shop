# Chạy MariaDB local + Flyway (v5)

## 1. Tạo database một lần trên MariaDB cài trực tiếp (port 3306)

```sql
CREATE DATABASE IF NOT EXISTS beautyshop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'beautyshop'@'localhost' IDENTIFIED BY '<MAT_KHAU_RIENG_CUA_BAN>';
GRANT ALL PRIVILEGES ON beautyshop.* TO 'beautyshop'@'localhost';
```

Ví dụ trên chỉ tạo user local; **thay placeholder** bằng mật khẩu riêng, không đẩy mật khẩu vào Git. Nếu bạn đã có DB/user thì không tạo trùng. `FLUSH PRIVILEGES` thường không cần sau `GRANT`.

## 2. Khởi chạy Windows PowerShell

```powershell
$env:DB_URL='jdbc:mariadb://localhost:3306/beautyshop'
$env:DB_USER='beautyshop'
$env:DB_PASSWORD='<MAT_KHAU_RIENG_CUA_BAN>'
.\scripts\p00.ps1 -Task test
.\scripts\p00.ps1 -Task run
```

Dev `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.enabled=true` ở `application-dev.properties`.
Khi khởi động trên database trống, Flyway áp dụng V1 và các migration tiếp theo theo thứ tự, sau đó JPA kiểm tra các bảng được ánh xạ. Database đã có V1 chỉ nhận các phiên bản chưa áp dụng. Kiểm tra log Flyway rồi mở `/`, `/products` và `/login` trên cổng 8080.

## 3. Flyway checklist

- DB mới: không có bảng ứng dụng, Flyway tạo bảng + `flyway_schema_history`.
- DB thử nghiệm đã áp dụng V1 tên tiếng Anh: dùng một database riêng, trống để khởi tạo v5. Nếu cần chuyển dữ liệu thật, lập kế hoạch backup và migration giữ dữ liệu. Giữ checksum của V1 đã áp dụng.
- DB đã tồn tại bảng ngoài Flyway: kiểm tra lịch sử schema và backup trước. Không tự bật `baseline-on-migrate=true` hoặc xóa bảng.
- Sau khi V1 được áp dụng: không sửa V1; viết V2__description.sql thay đổi schema.
- `ddl-auto=validate` không sinh bảng; nếu lỗi validate => kiểm tra entity mapping, migration và tên cột.
- `flyway-mysql` là module cần cho MariaDB/MySQL trên Flyway mới, phiên bản do Spring Boot quản lý.

## 4. Docker tùy chọn

Nếu dùng Docker, khai báo `DB_PASSWORD` trước khi chạy `docker compose up -d`. Compose mở MariaDB trên **host 3307**, vì vậy đặt `DB_URL` thành `jdbc:mariadb://localhost:3307/beautyshop` trước khi chạy ứng dụng.

Xem [roadmap](../03-roadmap-and-team.md) để biết phiên bản MariaDB đã kiểm chứng. Đối chiếu tên bảng và cột trong [tài liệu dữ liệu](../data/README.md).
