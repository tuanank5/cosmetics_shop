> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P00 — Bootstrap & giao diện nền

**Chủ trì:** D


- Kiểm tra Java 21/POM Spring Boot 3.5.16, profile dev/prod, kết nối MariaDB, log UTF-8. Không phá login scaffold đang từ chối login cho tới P04.
- Tạo các Thymeleaf fragments header/footer, nav responsive, CSS Premium/Luxury dùng font thống nhất, button kích thước vừa; static assets local; home/products/login đang là placeholder thì ghi rõ.
- Tạo CI/local commands; cung cấp PowerShell test/run script khi thiếu mvnw; kiểm Flyway V1 có thật, không tạo trùng; boot dùng local MariaDB 3306. Không làm CRUD/sáng tạo dữ liệu giả.
- Chứng minh `GET /`, `/products`, `/login` load được sau khi DB cấu hình; test basic page + mobile layout 375/768/1440px.
**Done:** Maven build/test và homepage/placeholder hiển thị, không khẳng định catalog đã hoàn thành.

## Quy định thực hiện

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
