> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P02 — SKU & Inventory theo biến thể — thông tin lô demo (không FEFO)

**Chủ trì:** A


- Tạo ProductVariant unique SKU, label (màu/dung tích), price BigDecimal, onHand, reserved, imageUrl, active; giá không âm, `0<=reserved<=onHand`.
- Tạo `InventoryService` có 4 contract: `reserveAtomically`, `consumeReservation`, `releaseReservation`, `deductCOD`, tất cả transaction/idempotent khi tích hợp Order. Làm rõ type DTO/method trước khi giao C.
- Sử dụng SQL conditional update hoặc lock/version; concurrency tests cho hai khách đua SKU còn 1, tránh oversell.
- CRUD SKU/ảnh admin, validate upload safe; hiển thị tùy chọn `demoBatchCode` và `demoExpiryDate` cho đúng 1 lô minh họa của mỗi SKU, không coi là quản lý nhiều lô, không tự động FEFO. Nếu sản phẩm có nhiều batch phải ghi rõ chưa hỗ trợ.
- Dùng cột đã có từ Flyway V1. Chỉ tạo migration V2+ nếu schema đổi; không `ddl-auto=update`.
**Done:** nhiều SKU có giá, ảnh, stock riêng; available tính đúng; test cạnh tranh không âm kho.

## Quy định thực hiện

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
