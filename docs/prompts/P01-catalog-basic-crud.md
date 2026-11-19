> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P01 — Catalog nền tảng: Category, Brand, Product

**Chủ trì:** A


- Tạo Entity/Repository/Service/DTO/Controller cho `DanhMuc`, `ThuongHieu`, `SanPham` (thông tin chung); quyền admin cho CRUD.
- Thuộc tính `product`: slug, title, description, ingredients, usageGuide, skinTypeTags (metadata dùng RAG), active, brand/category FK. Không đặt stock/price tổng ở Product.
- Ánh xạ Entity chính xác với Flyway V1 đã có, test migrate trên MariaDB sạch. Không sửa V1 khi đã migrate; thay đổi schema phải dùng V2+. Không dùng `ddl-auto=update` kể cả dev. Có validation slug/name, duplicate, active/inactive, prevent hard deletion khi đã có order.
- Form Thymeleaf admin tối thiểu rõ ràng, flash messages bằng tiếng Việt; không cần thương hiệu/ảnh thật ở bước này.
- Unit/integration tests repository & bảo vệ `/admin/**`.
**Done:** CRUD category/brand/product chạy thật trên MariaDB, dữ liệu nhất quán.

## Quy định thực hiện

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
