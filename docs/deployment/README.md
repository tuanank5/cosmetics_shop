# Deployment — kế hoạch miễn phí, chưa chọn host

## Local

Dùng [hướng dẫn MariaDB và Flyway](01-local-db-and-flyway.md) để tạo database, chọn cổng và chạy profile `dev`.

## Production (chưa xác minh)

- Cần Java 21 runtime, HTTPS, MariaDB có lưu trữ bền vững và backup, cùng các biến `DB_URL`, `DB_USER`, `DB_PASSWORD`. Nếu bật MoMo Sandbox, cấu hình thêm endpoint IPN công khai.
- Hosting free tier thường sleep, hạn mức tài nguyên, không có DB free, hoặc giới hạn callback; **không đảm bảo** site + MoMo IPN luôn hoạt động. Kiểm tra điều khoản, thời gian sleep, egress, cổng callback, HTTPS và phí phát sinh trước khi đăng ký.
- Ảnh SKU mặc định lưu ở `uploads/sku`. Cấu hình `app.uploads.sku-dir` trỏ tới volume bền vững khi triển khai và backup thư mục ảnh cùng MariaDB.
- Khi có migration hoàn chỉnh, prod dùng `spring.jpa.hibernate.ddl-auto=validate`; không bật `update` production.
- AI local Ollama không tự chạy trên host miễn phí. Nếu không có provider, widget chat hiển thị trạng thái tạm ngưng, không làm hỏng luồng mua hàng.
- Không publish giao dịch online thật khi chỉ có merchant Sandbox; khai báo rõ "thanh toán thử nghiệm".

## Checklist triển khai

- [ ] Domain/HTTPS và webhook URL công khai được xác minh.
- [ ] MariaDB tạo DB + user đặc quyền tối thiểu, backup/restore kiểm thử.
- [ ] Migration đã áp dụng thành công và JPA validate.
- [ ] Environment secrets đặt ngoài repo, không log.
- [ ] Smoke test luồng catalog → giỏ → đăng nhập → COD → admin → đơn hàng.
- [ ] Nếu bật MoMo Sandbox, kiểm merchant, redirect, IPN, truy vấn giao dịch và timeout trên host.
- [ ] Có backup demo localhost + video thay thế khi free tier gặp sự cố.
