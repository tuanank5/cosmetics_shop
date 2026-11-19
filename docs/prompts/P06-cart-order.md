> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P06 — Cart + Order COD E2E (P0) + hợp đồng mở rộng online

**Chủ trì:** C


- Implement Guest Session cart (variantId, qty), Customer cart DB và merge idempotent theo contract B; update qty không trừ kho, validate available mỗi lúc checkout.
- Checkout server tính lại giá, voucher qua B, phí 25.000đ và miễn 500.000đ sau voucher; snapshot địa chỉ, SKU, giá, qty trong Order.
- COD: transaction kiểm tra available atomically và chốt onHand, tạo Order PENDING_CONFIRMATION, giảm lượt voucher; không rollback nửa chừng.
- Online (P1 cho P07): định nghĩa contract/reservation 15 phút nhưng **không làm COD phụ thuộc MoMo**; khi bật online mới triển khai. Call gateway ngoài transaction. Không giải phóng giữ hàng khi giao dịch chưa đối soát.
- `OrderStatePolicy` kiểm chuyển trạng thái, owner kiểm với `Order.userId`. Admin manually CONFIRMED->PACKING->SHIPPING->DELIVERED.
- Customer tự hủy trước PACKING nếu policy cho phép, không hoàn hàng hai lần; Order history và detail.
- Tests: E2E COD, hủy, lỗi thiếu stock, concurrency, cập nhật trạng thái không hợp lệ, khác user xem order.
**Done:** có thể mua COD từ catalog đến admin giao hàng trên MariaDB.

## Quy định thực hiện
1. Đọc `AGENTS.md`, `docs/DECISIONS.md`, `docs/requirements/01-srs.md`, `docs/requirements/02-business-rules.md`, `docs/design/02-erd.md` và code hiện tại trước khi sửa.
2. Chỉ làm task này; không tự bổ sung tính năng P2, không đổi stack/phân quyền/trạng thái mà không có duyệt thay đổi.
3. Làm theo package-by-feature, dùng DTO/validation/constructor injection/BigDecimal; thêm migration phiên bản và test thực tế khi cần.
4. Kết thúc: liệt kê changed files, API/pages, `mvn test`/package PASS hoặc blocker, cách chạy thử, vấn đề còn tồn. Không giả lập bằng chứng.
5. Không hardcode secret, không dùng giá từ client, không gắn cứng ảnh mỹ phẩm chưa rõ quyền.
