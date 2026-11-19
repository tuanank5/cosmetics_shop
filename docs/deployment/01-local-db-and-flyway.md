# Chạy MariaDB local + Flyway (v4)

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
$env:SPRING_PROFILES_ACTIVE='dev'
mvn test
mvn spring-boot:run
```

Dev `spring.jpa.hibernate.ddl-auto=validate`, `spring.flyway.enabled=true` ở `application-dev.properties`.
Spring Boot tự gọi Flyway `V1__init_schema.sql` khi ứng dụng bắt đầu (nếu đã có schema hợp lệ và dependency); sau đó JPA validate bảng/entity. Không cần chạy `mvn flyway:migrate` thủ công để khởi động lần đầu.

## 3. Flyway checklist

- DB mới: không có bảng ứng dụng, Flyway tạo bảng + `flyway_schema_history`.
- DB đã tồn tại bảng từ thử nghiệm cũ: **backup trước**; không tự bật `baseline-on-migrate=true` hoặc xóa bảng. Tạo schema mới cho project nếu có thể.
- Sau khi V1 được áp dụng: không sửa V1; viết V2__description.sql thay đổi schema.
- `ddl-auto=validate` không sinh bảng; nếu lỗi validate => kiểm tra entity mapping, migration và tên cột.
- `flyway-mysql` là module cần cho MariaDB/MySQL trên Flyway mới, phiên bản do Spring Boot quản lý.

## 4. Docker tùy chọn

Nếu dùng `docker compose up -d`, compose expose MariaDB **host 3307** để không xung đột MariaDB cài trực tiếp; khi đó đổi `DB_URL` thành `jdbc:mariadb://localhost:3307/beautyshop`. Không cần Docker để chạy Maven/JAR.

**Chưa xác minh chạy thực tế MariaDB/Flyway trong môi trường tạo bộ ZIP này.**
