> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P08 — Reviews + Wishlist + đổi trả cơ bản

**Chủ trì:** D


- Review được tạo bởi Customer sở hữu OrderItem của Order DELIVERED, `order_item_id` unique. Rating 1–5; có thể sửa review của mình, Admin duyệt/ẩn; không review dựa đơn chưa giao.
- Wishlist Customer unique(userId, productId), chống thêm trùng; guest yêu cầu login khi dùng wishlist.
- ReturnRequest trong 7 ngày từ deliveredAt, nhiều ReturnRequestItem liên kết OrderItem, không yêu cầu quá số lượng đã mua/chưa trả; Admin approved/rejected/received/completed thủ công; không tự cộng kho/PAID/refund.
- Thymeleaf trang tương ứng, validation tiếng Việt; tests ownership/review duplicate/return after 8 days.
**Done:** review verified purchase & ReturnRequest workflow thực thi được, không gian lận quyền.

## Quy định thực hiện

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
