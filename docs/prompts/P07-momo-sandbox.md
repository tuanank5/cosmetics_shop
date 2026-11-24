> **Quy tắc DB (v5):** Tên bảng/cột MariaDB dùng tiếng Việt không dấu theo `docs/data/bang_doi_chieu_ten.csv`; JPA ánh xạ `@Table`, `@Column`, `@JoinColumn` với tên mới, kể cả khi thuật ngữ cũ trong prompt còn tiếng Anh. Migration hiện hành: `V1__khoi_tao_co_so_du_lieu.sql`.

# P07 — MoMo Sandbox P1 có điều kiện, MockAdapter chỉ dành cho demo nội bộ

**Chủ trì:** C


- **MoMo Sandbox** thay VNPay; trước code hãy chọn API/version tài liệu MoMo chính thức (https://developers.momo.vn/v3/docs/payment/api/), ghi `docs/DECISIONS.md` và config partnerCode/accessKey/secretKey/redirectUrl/ipnUrl qua Environment.
- PaymentAttempt nhiều lần/Order, unique attemptRef; IPN có signature HMAC đúng spec đã chọn. Kiểm order/attempt/amount/result; không đánh dấu paid chỉ từ browser redirect.
- URL IPN public + HTTPS và merchant Sandbox là điều kiện tích hợp thật. `POST /payment/momo/ipn` phải permitAll + miễn CSRF browser, **nhưng bắt buộc** xác thực chữ ký HMAC, ràng buộc request/order/amount, chống replay và rate limit. Return URL không chốt PAID. Chỉ public GET `/payment/momo/return` cho thông báo UX. Không log keys/raw PII.
- Order state **PENDING_PAYMENT -> PENDING_CONFIRMATION** chỉ khi `PaymentAttempt.status = PAID` đã được xác minh; **PAID không phải OrderStatus**. Consume reservation/voucher **idempotent**, release trên failure/expiry chỉ sau xác minh trạng thái provider. Thêm reconciliation và late-IPN path, tuyệt đối không oversell.
- Unit test signature với vectors chuẩn, IPN trùng/sai chữ ký/khác amount, retry; integration test với DB. Nếu chưa có merchant Sandbox, báo `BLOCKED_EXTERNAL`; tạo `MockPaymentGateway` **chỉ dành profile dev/test** và gắn nhãn giao diện `MÔ PHỎNG - KHÔNG THU TIỀN`; không chấp nhận kết quả mock trong production hoặc kết luận đã tích hợp MoMo thật.
**Done (conditional):** MoMo Sandbox thật end-to-end có bằng chứng. Nếu thiếu điều kiện: mock integration tests + ghi BLOCKED_EXTERNAL; **COD P0 không bị chặn**.

## Quy định thực hiện

Áp dụng [AGENTS.md](../../AGENTS.md) và chỉ làm trong phạm vi prompt này. Báo các trang hoặc API bị tác động, cách chạy thử và kết quả kiểm chứng.
