# Changelog v4 – 12/11/2026

1. Thêm `flyway-core`, `flyway-mysql`; bật Flyway cả dev/prod với JPA validate; thêm `V1__init_schema.sql` (empty DB required).
2. MariaDB dev mặc định 3306; Docker tùy chọn giữ host port 3307.
3. CODE security: public MoMo IPN POST + chỉ bypass CSRF route này, return GET public, `/uploads/**` public **chỉ phù hợp ảnh đã validate**; chưa có upload controller/handler.
4. P0 = COD E2E trọn vẹn; MoMo Sandbox P1 có điều kiện merchant/IPN; mock dev/test không phải thanh toán thật. **Giữ nguyên yêu cầu 15 phút reservation khi MoMo được triển khai** để tránh oversell.
5. Sửa trạng thái: `PaymentAttempt.PAID` là payment, **không phải `OrderStatus.PAID`**.
6. Ghi rõ `demo_batch_code`, `demo_expiry_date` tùy chọn để minh họa một lô/SKU, **không có FEFO/đa lô**.
7. Chốt bản nộp Class Diagram/ERD v4, đối chiếu 5 lớp bị bỏ/đổi tên của sơ đồ nhóm.
8. AI cấu hình disabled mặc định, P09 Ollama `qwen2.5:3b` + `bge-m3`, chatbot có deadline demo timebox.
9. Giao rõ owner/mốc 30–50 sản phẩm và bản quyền hình ảnh.
10. Chưa có Maven Wrapper tải được trong container; đã ghi lệnh cài wrapper bằng Maven khi có Internet; không viết wrapper giả.

## Chưa được kiểm chứng
- Chưa chạy Flyway/Maven Spring Boot trên MariaDB do runtime này không có Maven/DB và không tải dependency được. V1 là thiết kế SQL chưa được nghiệm thu thực tế.
- MoMo Sandbox merchant, HTTPS IPN, AI provider, hosting free-tier chưa kết nối.
- 30–50 sản phẩm demo có nguồn vẫn là công việc của nhóm, không tự bịa dữ liệu.
