> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P03 — Storefront lọc/tìm/chi tiết sản phẩm

**Chủ trì:** A


- `GET /products`, `GET /products/{slug}` với filter theo brand/category/skinType, query text, sort price/newest, pagination dùng Pageable; chỉ active products và active SKUs.
- Hiển thị giá theo SKU phù hợp (min starting price đúng), detail chọn SKU làm đổi giá, tình trạng hàng, hình; không cache giá sai.
- Template mobile/desktop Premium theo CSS chung, ảnh fallback có quyền, empty-state, alt-text, loading nhẹ; không thêm React.
- Integration tests filter, pagination, unavailable SKU, slug not found; xử lý ký tự tiếng Việt.
**Done:** khách xem catalog thực tế và chọn đúng SKU.

## Quy định thực hiện

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
