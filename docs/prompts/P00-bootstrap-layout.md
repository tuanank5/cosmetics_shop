> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P00 — Bootstrap & giao diện nền

**Chủ trì:** D


- Kiểm tra Java 21/POM Spring Boot 3.5.16, profile dev/prod, kết nối MariaDB, log UTF-8. Không phá login scaffold đang từ chối login cho tới P04.
- Tạo các Thymeleaf fragments header/footer, nav responsive, CSS Premium/Luxury dùng font thống nhất, button kích thước vừa; static assets local; home/products/login đang là placeholder thì ghi rõ.
- Tạo CI/local commands; cung cấp PowerShell test/run script khi thiếu mvnw; kiểm Flyway V1 có thật, không tạo trùng; boot dùng local MariaDB 3306. Không làm CRUD/sáng tạo dữ liệu giả.
- Chứng minh `GET /`, `/products`, `/login` load được sau khi DB cấu hình; test basic page + mobile layout 375/768/1440px.
**Done:** Maven build/test và homepage/placeholder hiển thị, không khẳng định catalog đã hoàn thành.

## Quy định thực hiện
1. Đọc `AGENTS.md`, `docs/DECISIONS.md`, `docs/requirements/01-srs.md`, `docs/requirements/02-business-rules.md`, `docs/design/02-erd.md` và code hiện tại trước khi sửa.
2. Chỉ làm task này; không tự bổ sung tính năng P2, không đổi stack/phân quyền/trạng thái mà không có duyệt thay đổi.
3. Làm theo package-by-feature, dùng DTO/validation/constructor injection/BigDecimal; thêm migration phiên bản và test thực tế khi cần.
4. Kết thúc: liệt kê changed files, API/pages, `mvn test`/package PASS hoặc blocker, cách chạy thử, vấn đề còn tồn. Không giả lập bằng chứng.
5. Không hardcode secret, không dùng giá từ client, không gắn cứng ảnh mỹ phẩm chưa rõ quyền.
