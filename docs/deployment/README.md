# Deployment — kế hoạch miễn phí, chưa chọn host

## Local

- Java 21 + Maven 3.9+ + MariaDB local, hoặc tùy chọn `docker compose up -d` (MariaDB cổng 3307, chỉ localhost).
- Lệnh (PowerShell):

```powershell
$env:DB_URL='jdbc:mariadb://localhost:3307/beautyshop'
$env:DB_USER='beautyshop'
$env:DB_PASSWORD='beautyshop_dev_only'
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- `application-dev.properties` kết nối DB; migration sẽ được tạo ở P01+. Trang chủ scaffold không có business data.

## Production (chưa xác minh)

- Cần Java 21 runtime, HTTPS, endpoint công khai nhận MoMo IPN, MariaDB có persistence và backup, biến môi trường `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- Hosting free tier thường sleep, hạn mức tài nguyên, không có DB free, hoặc giới hạn callback; **không đảm bảo** site + MoMo IPN luôn hoạt động. Kiểm tra điều khoản, thời gian sleep, egress, cổng callback, HTTPS và phí phát sinh trước khi đăng ký.
- File ảnh tải lên không nên lưu duy nhất trên filesystem tạm của cloud host. Dùng bucket phù hợp hoặc thư mục local cho demo và phương án dự phòng.
- Khi có migration hoàn chỉnh, prod dùng `spring.jpa.hibernate.ddl-auto=validate`; không bật `update` production.
- AI local Ollama không tự chạy trên host miễn phí. Nếu không có provider, widget chat hiển thị trạng thái tạm ngưng, không làm hỏng luồng mua hàng.
- Không publish giao dịch online thật khi chỉ có merchant Sandbox; khai báo rõ "thanh toán thử nghiệm".

## Checklist triển khai

- [ ] Domain/HTTPS và webhook URL công khai được xác minh.
- [ ] MariaDB tạo DB + user đặc quyền tối thiểu, backup/restore kiểm thử.
- [ ] Migration đã áp dụng thành công và JPA validate.
- [ ] Environment secrets đặt ngoài repo, không log.
- [ ] MoMo Sandbox merchant/redirect/IPN/transaction query được kiểm chứng trên host.
- [ ] Smoke: catalog → cart → login → COD → admin → order; kiểm IPN/timeout.
- [ ] Có backup demo localhost + video thay thế khi free tier gặp sự cố.
