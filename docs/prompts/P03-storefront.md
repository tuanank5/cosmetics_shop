> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P03 — Storefront lọc/tìm/chi tiết sản phẩm

**Chủ trì:** A


- `GET /products`, `GET /products/{slug}` với filter theo brand/category/skinType, query text, sort price/newest, pagination dùng Pageable; chỉ active products và active SKUs.
- Hiển thị giá theo SKU phù hợp (min starting price đúng), detail chọn SKU làm đổi giá, tình trạng hàng, hình; không cache giá sai.
- Template mobile/desktop Premium theo CSS chung, ảnh fallback có quyền, empty-state, alt-text, loading nhẹ; không thêm React.
- Integration tests filter, pagination, unavailable SKU, slug not found; xử lý ký tự tiếng Việt.
**Done:** khách xem catalog thực tế và chọn đúng SKU.

## Quy định thực hiện
1. Đọc `AGENTS.md`, `docs/DECISIONS.md`, `docs/requirements/01-srs.md`, `docs/requirements/02-business-rules.md`, `docs/design/02-erd.md` và code hiện tại trước khi sửa.
2. Chỉ làm task này; không tự bổ sung tính năng P2, không đổi stack/phân quyền/trạng thái mà không có duyệt thay đổi.
3. Làm theo package-by-feature, dùng DTO/validation/constructor injection/BigDecimal; thêm migration phiên bản và test thực tế khi cần.
4. Kết thúc: liệt kê changed files, API/pages, `mvn test`/package PASS hoặc blocker, cách chạy thử, vấn đề còn tồn. Không giả lập bằng chứng.
5. Không hardcode secret, không dùng giá từ client, không gắn cứng ảnh mỹ phẩm chưa rõ quyền.
