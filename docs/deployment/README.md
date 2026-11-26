# Deployment — kế hoạch miễn phí, chưa chọn host

## Local

Dùng [hướng dẫn MariaDB và Flyway](01-local-db-and-flyway.md) để tạo database, chọn cổng và chạy profile `dev`.

Tài khoản Admin được tạo theo [quy trình cấp quyền](03-cap-quyen-admin.md).

Xem [cấu hình, mô phỏng và đối soát MoMo](04-momo-sandbox.md) cho P07.

Xem [FAQ và RAG với Ollama](05-chatbot-rag.md) cho P09.

Xem [catalog, luồng COD và backup demo P10](06-p10-demo.md) để chuẩn bị trình diễn.

Xem [MongoDB cho đánh giá sản phẩm P23](07-mongodb-danh-gia.md) để cài đặt, cấu hình `MONGODB_URI` và chuẩn bị đánh giá demo sau khi nạp `seed-demo.ps1 -WithOrders`.

## Production (chưa xác minh)

- Từ P23 cần thêm MongoDB 6.0+ có xác thực và backup cho đánh giá; đặt `MONGODB_URI` từ secret store.
- Cần Java 21 runtime, HTTPS, MariaDB có lưu trữ bền vững và backup, cùng các biến `DB_URL`, `DB_USER`, `DB_PASSWORD`. Nếu bật MoMo Sandbox, cấu hình thêm endpoint IPN công khai.
- Hosting free tier thường sleep, hạn mức tài nguyên, không có DB free, hoặc giới hạn callback; **không đảm bảo** site + MoMo IPN luôn hoạt động. Kiểm tra điều khoản, thời gian sleep, egress, cổng callback, HTTPS và phí phát sinh trước khi đăng ký.
- Ảnh SKU mặc định lưu ở `uploads/sku`. Cấu hình `app.uploads.sku-dir` trỏ tới volume bền vững khi triển khai và backup thư mục ảnh cùng MariaDB.
- Khi có migration hoàn chỉnh, prod dùng `spring.jpa.hibernate.ddl-auto=validate`; không bật `update` production.
- Khi Ollama chưa sẵn sàng, `/chat` dùng chế độ tìm FAQ. Kiểm tra chế độ hiển thị trên host trước demo.
- Không publish giao dịch online thật khi chỉ có merchant Sandbox; khai báo rõ "thanh toán thử nghiệm".

## Checklist triển khai

- [ ] Domain/HTTPS và webhook URL công khai được xác minh.
- [ ] MariaDB tạo DB + user đặc quyền tối thiểu, backup/restore kiểm thử.
- [ ] Migration đã áp dụng thành công và JPA validate.
- [ ] Environment secrets đặt ngoài repo, không log.
- [ ] Smoke test luồng catalog → giỏ → đăng nhập → COD → admin → đơn hàng.
- [ ] Nếu bật MoMo Sandbox, kiểm merchant, redirect, IPN, truy vấn giao dịch và timeout trên host.
- [ ] Có backup demo localhost + video thay thế khi free tier gặp sự cố.
