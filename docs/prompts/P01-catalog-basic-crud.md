> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P01 — Catalog nền tảng: Category, Brand, Product

**Chủ trì:** A


- Tạo Entity/Repository/Service/DTO/Controller cho Category, Brand, Product (thông tin chung); quyền admin cho CRUD.
- Thuộc tính `product`: slug, title, description, ingredients, usageGuide, skinTypeTags (metadata dùng RAG), active, brand/category FK. Không đặt stock/price tổng ở Product.
- Ánh xạ Entity chính xác với Flyway V1 đã có, test migrate trên MariaDB sạch. Không sửa V1 khi đã migrate; thay đổi schema phải dùng V2+. Không dùng `ddl-auto=update` kể cả dev. Có validation slug/name, duplicate, active/inactive, prevent hard deletion khi đã có order.
- Form Thymeleaf admin tối thiểu rõ ràng, flash messages bằng tiếng Việt; không cần thương hiệu/ảnh thật ở bước này.
- Unit/integration tests repository & bảo vệ `/admin/**`.
**Done:** CRUD category/brand/product chạy thật trên MariaDB, dữ liệu nhất quán.

## Quy định thực hiện
1. Đọc `AGENTS.md`, `docs/DECISIONS.md`, `docs/requirements/01-srs.md`, `docs/requirements/02-business-rules.md`, `docs/design/02-erd.md` và code hiện tại trước khi sửa.
2. Chỉ làm task này; không tự bổ sung tính năng P2, không đổi stack/phân quyền/trạng thái mà không có duyệt thay đổi.
3. Làm theo package-by-feature, dùng DTO/validation/constructor injection/BigDecimal; thêm migration phiên bản và test thực tế khi cần.
4. Kết thúc: liệt kê changed files, API/pages, `mvn test`/package PASS hoặc blocker, cách chạy thử, vấn đề còn tồn. Không giả lập bằng chứng.
5. Không hardcode secret, không dùng giá từ client, không gắn cứng ảnh mỹ phẩm chưa rõ quyền.
