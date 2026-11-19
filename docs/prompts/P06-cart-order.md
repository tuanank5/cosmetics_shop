> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P06 — Cart + Order COD E2E (P0) + hợp đồng mở rộng online

**Chủ trì:** C


- Implement Guest Session cart (variantId, qty), Customer cart DB và merge idempotent theo contract B; update qty không trừ kho, validate available mỗi lúc checkout.
- Checkout server tính lại giá, voucher qua B, phí 25.000đ và miễn 500.000đ sau voucher; snapshot địa chỉ, SKU, giá, qty trong Order.
- COD: transaction kiểm tra available atomically và chốt onHand, tạo Order PENDING_CONFIRMATION, giảm lượt voucher; không rollback nửa chừng.
- Online (P1 cho P07): định nghĩa contract/reservation 15 phút nhưng **không làm COD phụ thuộc MoMo**; khi bật online mới triển khai. Call gateway ngoài transaction. Không giải phóng giữ hàng khi giao dịch chưa đối soát.
- `OrderState_Policy` kiểm chuyển trạng thái, owner kiểm với `Order.userId`. Admin cập nhật thủ công CONFIRMED → PACKING → SHIPPING → DELIVERED.
- Customer tự hủy trước PACKING nếu policy cho phép, không hoàn hàng hai lần; Order history và detail.
- Tests: E2E COD, hủy, lỗi thiếu stock, concurrency, cập nhật trạng thái không hợp lệ, khác user xem order.
**Done:** có thể mua COD từ catalog đến admin giao hàng trên MariaDB.

## Quy định thực hiện

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
