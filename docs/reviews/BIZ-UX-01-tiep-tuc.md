# BIZ-UX-01 · Checkpoint tiếp tục

Ngày 27/11/2026. HEAD `daea756053021310d9fd1c6ada9475564711fac6`, nhánh `main`. Ban đầu tracked tree và index sạch. Có các tệp chưa theo dõi của AUTH-03, REPORT-01, `docs/report/`, prompt và `env`. Giữ các tệp này và tránh đưa `env` vào Git.

## Trạng thái

- Backend thay đổi trong `Checkout_Controller`, `GioHang_Repository` và `GioHang_Service`, cùng kiểm thử `CheckoutPagesTest` và `DocView_MariaDb_Test`.
- Storefront thay đổi trong `cart.js`, `home-carousel.js`, `storefront.css`, template cart/checkout/Home/tư vấn da và fragment header/footer. Cart giữ POST/PRG. Timeout dùng 8 giây, lỗi chưa rõ dẫn người dùng đọc lại giỏ bằng GET. Kênh thông báo dùng BroadcastChannel khi các tab giỏ có hỗ trợ.
- Xem [báo cáo](BIZ-UX-01-ra-soat-nghiep-vu-uiux.md) và [bằng chứng](../qa/BIZ-UX-01/).
- Đã xác nhận bản gốc có báo giá khi SKU hết hàng, 7 SELECT cho giỏ Guest 7 SKU, giỏ JS đảo thứ tự phản hồi và gửi lại POST khi mất phản hồi. MVC checkout và mô phỏng Node VM trước/sau qua assertions. MariaDB sau sửa đo 1 SELECT cho 7 SKU. Smoke HTTP đã qua luồng COD và Admin xác nhận.
- Baseline build riêng 316 ca, 241 đạt, 75 skip. Maven cuối 318 ca, 242 đạt, 76 skip. Package cuối và `git diff --check` exit 0. Integration baseline 75 ca, 70 đạt, 5 skip. Integration sau sửa 76 ca, 71 đạt, 5 skip. Migration khớp lịch sử 8/8. V5 cũ được Flyway từ chối đúng checksum, giữ đủ history của fixture.
- Lượt cuối gồm `maven-handoff-test.log`, `maven-handoff-package.log`, `maven-integration-verified.log` và `before-http.json`/`after-http.json`. Test, package, tích hợp và hai lượt HTTP đều exit 0. JAR sau sửa nằm tại `target/biz-ux-01/current/target/beautyshop-0.0.1-SNAPSHOT.jar`.
- Chrome headless kết nối DevTools nhưng `Page.enable` timeout. Browser tích hợp không tìm thấy phiên. Firefox headless không tạo được renderer. Chưa có ảnh trước/sau hay nghiệm thu bố cục.

## Môi trường và cách chạy lại

Trên máy hiện tại, Maven 3.9.11 cần Java 21 ở `JAVA_HOME` của tiến trình. Cache đọc ghi trong `target/biz-ux-01/maven`. Bản dựng baseline và hiện tại cùng nằm trong `target/biz-ux-01/`. Khi dựng lại trên máy khác, dùng `git archive --format=zip --output=target/biz-ux-01/baseline-source.zip daea756 pom.xml src` và giải nén vào thư mục baseline riêng. Sao chép `pom.xml` và `src` từ working tree vào thư mục dựng hiện tại. Dùng `mvn -B -o -ntp "-Dmaven.repo.local=<cache>" -f <build>/pom.xml test`, sau đó `-DskipTests package`. Cách này tránh đọc lớp Java cũ trong `target` gốc.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/BIZ-UX-01/run-fixture.ps1 -Phase after -Build target/biz-ux-01/current -HttpOnly
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/BIZ-UX-01/run-fixture.ps1 -Phase integration -Build target/biz-ux-01/current -EvidenceTag rerun
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/BIZ-UX-01/run-fixture.ps1 -Phase v5-drift -Build target/biz-ux-01/current
node docs/qa/BIZ-UX-01/cart-vm-check.mjs src/main/resources/static/js/cart.js after
```

Fixture tự cấp cổng loopback riêng, datadir SQL/Mongo riêng dưới `target/biz-ux-01` và upload riêng. Redis/SMTP đi tới loopback cổng 1. Google/GitHub, MoMo thật và Ollama thật không có cấu hình trong fixture. Chỉ stop process được script tạo. App có sẵn ở 8081 và DB của owner nằm ngoài fixture.

Một lượt `mvn clean test` ban đầu đã chạy nhầm ở gốc và dừng khi gặp JAR đang khóa. Nó xóa thư mục fixture QA cũ `target/auth-03-db-4798ea844a3c4296b1cf26c4427ee3df` và một phần cache `target/auth-03-maven`. Cache thiếu đã được chép bù từ cache Maven. Lượt này không khôi phục được datadir fixture cũ. Các báo cáo/bằng chứng AUTH-03 trong `docs/` không bị lệnh đó tác động. Từ sau lỗi này, mọi bản dựng của task chạy trong thư mục riêng.

## Việc tiếp theo

1. Trên máy có browser điều khiển được, chạy chặng `before` và `after` của fixture, bỏ `-HttpOnly`. Dùng bản JAR baseline và bản JAR mới, giữ dữ liệu chuẩn của script. Giữ bằng chứng lượt cũ khi chạy lại.
2. Kiểm tra hai tab, xác nhận xóa 0/Escape, bàn phím, zoom thật 200%, ma trận responsive theo prompt và đường cuộn Home. Mở ảnh trước/sau và cập nhật báo cáo theo quan sát. Đo CSS coverage trước khi xem xét stylesheet dùng chung.
3. Xác nhận GitHub Actions của đúng HEAD và provider ngoài khi có môi trường phù hợp. Chưa có commit, push hay deploy.
