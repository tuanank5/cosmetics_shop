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
1. Đọc `AGENTS.md`, `docs/DECISIONS.md`, `docs/requirements/01-srs.md`, `docs/requirements/02-business-rules.md`, `docs/design/02-erd.md` và code hiện tại trước khi sửa.
2. Chỉ làm task này; không tự bổ sung tính năng P2, không đổi stack/phân quyền/trạng thái mà không có duyệt thay đổi.
3. Làm theo package-by-feature, dùng DTO/validation/constructor injection/BigDecimal; thêm migration phiên bản và test thực tế khi cần.
4. Kết thúc: liệt kê changed files, API/pages, `mvn test`/package PASS hoặc blocker, cách chạy thử, vấn đề còn tồn. Không giả lập bằng chứng.
5. Không hardcode secret, không dùng giá từ client, không gắn cứng ảnh mỹ phẩm chưa rõ quyền.
