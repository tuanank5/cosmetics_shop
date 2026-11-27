# Hiện trạng BeautyShop ngày 27/11/2026

## Mốc đánh giá

Branch `main`, HEAD và ref local `origin/main` cùng ở `b03e0bf0533889ca8ae3ff75db7cca6b657efc35`. Lịch sử gần nhất đi từ `38fb1ca → c79983a → daea756 → b03e0bf`. Git xác nhận 195 đường dẫn giữa hai commit cuối. Phạm vi `b03e0bf..HEAD` rỗng. Đây là kết luận về checkout local, chưa truy vấn GitHub trong lượt này.

Trước khi viết bốn báo cáo, index trống, có 80 tệp tracked sửa, 9 xóa và 55 untracked. Hai tệp nguồn mới là `BoLocDanhSachAdmin.java` và `templates/chat/index.html`. Phần còn lại chưa theo dõi gồm báo cáo, QA và ảnh. Khi bàn giao phải lấy đủ tệp mới để build lại từ checkout sạch. Danh mục cuối tài liệu là snapshot trước bốn báo cáo. Diff theo đường dẫn cho thấy Entity, Flyway, scripts, Compose, pom, cấu hình ứng dụng và workflow không đổi so với HEAD.

## Đối chiếu các mốc

| Hạng mục | Đánh giá hiện tại | Bằng chứng và việc còn thiếu |
|---|---|---|
| BIZ-UX-01/02/03 | Đã triển khai, có hồi quy theo luồng | Báo cáo BIZ-UX-01/02/03 và bộ COD, giỏ, tồn, voucher tại checkpoint. Giao diện và đường về đã thay đổi tiếp. |
| UI-REDESIGN-01/02 | Có triển khai, nghiệm thu trực quan một phần | Các template checkout, order, header, wishlist đang tồn tại. UI-REDESIGN-02-ket-qua.md dòng 35–36 thiếu ảnh so sánh 1672×941 và zoom. Các ca Chrome mới chỉ bổ sung màn đã chọn. |
| ADMIN-UX-BIZ-01 | Chặn ngừng hoạt động, footer và bảng đã triển khai và kiểm theo fixture | D40, BR21, NgungHoatDongCatalog_Service, ADMIN-UX-BIZ-01-ket-qua.md. Cần giữ gate giao dịch khi thay cách tính tồn. |
| Kho nhiều lô | Chưa triển khai | V1–V7 và TonKho_Service dùng biến thể làm đơn vị tồn. Mã lô/HSD minh họa trên PDP chưa biểu diễn nhiều lô. |
| FEFO | Hoàn thành một phần thiết kế | ADMIN-UX-BIZ-01-tiep-tuc.md đã có khóa, phân bổ, legacy và rollback. Còn quyết định nghiệp vụ, kiểm kê và diễn tập. |
| Google/GitHub OAuth | Đã tích hợp, thiếu nghiệm thu thật | AUTH-02-nghiem-thu-xac-thuc.md dòng 11, 29–30. Google dùng token ký bởi server mô phỏng, GitHub dùng fixture. |
| Redis/SMTP/OTP | Đã triển khai, thiếu xác nhận toàn chuỗi thật | AUTH-02, docker-compose.yml. Kết nối TCP không chứng minh OTP được nhận, hết hạn, dùng một lần hay giao tới hộp thư ngoài. |
| MoMo | Gateway mock và xử lý giao dịch đã kiểm, Sandbox chưa xác minh | Momo_MariaDb_Test dùng mode mock. SCROLL-RESTORE/integration.txt có 3 ca MoMo. |
| Ollama/RAG | FAQ và RAG mock đã kiểm, provider thật còn mở | CHAT-ROUTE-01 có 6/6 FAQ/RAG. D41 chỉ giữ /chat. Nghiệm thu lại form /tu-van-da không còn phù hợp. |
| Google Maps | Hoàn thành một phần | fragments/footer.html:68–72 có iframe, fallback. ADMIN-UX-BIZ-01-ket-qua.md:90 chưa xác nhận pin và liên hệ chính thức. |
| Zoom native 200% | Thiếu nghiệm thu | ADMIN-UX-BIZ-01:73,91 có reflow mô phỏng và 66 phép đo, thiếu thao tác zoom trực tiếp. |
| Toàn bộ storefront/Admin | Đã kiểm một phần, chịu ảnh hưởng diff | NAV-CATALOG, LIST-PAGING, ADMIN-RESTORE, SCROLL-RESTORE, ADDRESS-FOCUS, WORDMARK-FIX có bằng chứng riêng. Còn thiếu ma trận chung theo vai trò và trạng thái. |
| Báo cáo 91 trang | Chưa đủ bằng chứng | Có docs/report/README.md và tài liệu nguồn. Kiểm kê Git hiện tại không tìm thấy PDF/DOCX xác định bản 91 trang. Cần owner cung cấp bản nộp để đếm trang và đối chiếu. |

## Môi trường và cách chuẩn bị

Maven CLI 3.9.15 mặc định nhận Java 25 trong shell được kiểm. Bản build kiểm thử dùng JDK 21. Chọn JDK 21 qua JAVA_HOME và xác nhận bằng `mvn -version`. Docker CLI chưa có trong PATH của shell. Compose khai báo MariaDB 11.4, MongoDB 7, Redis 7, Mailpit 1.27. Kiểm TCP loopback thấy 8080, 3306, 27017, 6379, 1025 và 8025 mở; 3307 và 11434 đóng. Đây là tín hiệu cổng tại thời điểm rà, chưa định danh tiến trình hoặc xác minh môi trường từ xa.

| Chuỗi | Chuẩn bị cho lần chạy được duyệt | Bằng chứng cần lấy |
|---|---|---|
| SQL/Mongo/Compose | Schema và DB test độc lập, account giới hạn, thư mục ảnh thử. Chọn cổng theo DB_URL. Nếu dùng Docker, chuẩn bị CLI/daemon và kiểm health. README:18 lưu ý lịch sử checksum V5 cũ. | Flyway validate, khởi tạo trống, nâng bản có dữ liệu rồi phục hồi và đối chiếu |
| Redis/Mailpit/Gmail | Chọn đúng endpoint test, cấu hình REDIS_* và MAIL_*, địa chỉ gửi, TLS và hộp thư do owner kiểm soát. Mailpit cho phép nghiệm thu local, Gmail cần tài khoản gửi được phép. | Đăng ký, nhận thư, sai/hết hạn/gửi lại/dùng lại OTP, lỗi gửi, xác minh chỉ một lần |
| Google/GitHub | Client test riêng, AUTH_PUBLIC_URL, callback khớp /login/oauth2/code/google và /github theo cùng tiền tố, test user đã cấp quyền | Callback thật, từ chối cấp quyền, email đã có, liên kết đúng tài khoản |
| MoMo Sandbox | Bộ MOMO_PARTNER_CODE, ACCESS_KEY, SECRET_KEY, REDIRECT_URL, IPN_URL với tiền tố MOMO_; endpoint HTTPS nhận được callback; bật sandbox theo cấu hình dự án | Đối chiếu amount, trạng thái cuối, IPN lặp, voucher, giữ/trả tồn |
| Ollama | Endpoint truy cập được từ ứng dụng, đủ tài nguyên, tải đúng chat model và embedding model, bật AI trên fixture catalog/FAQ | Nạp nguồn, truy xuất, câu trả lời có căn cứ, timeout, câu bị từ chối không gọi provider |
| Maps | Owner xác nhận địa chỉ, pin và liên hệ chính thức, mạng truy cập Google | Mở iframe và liên kết trên mobile/desktop, so pin với vị trí owner duyệt |

.env được Git ignore. Chỉ kiểm tên khóa có khai báo, không kiểm giá trị hoặc hiệu lực. Có khai báo OAuth, Redis, mail và model AI. Năm khóa MoMo nêu trên không xuất hiện dưới dạng có giá trị trong .env đã kiểm; máy chủ có thể cấp từ môi trường. Chỉ lưu kết quả đã ẩn danh của các phép thử.

## Giá trị của bằng chứng

Đối chiếu SHA-256 tất cả 321 tệp src và pom với bản build target/wordmark-fix-20261127 cho kết quả khớp. [Maven cuối](../qa/WORDMARK-FIX/maven.txt) ghi 231 đạt, 82 skip, package đạt. [Tích hợp chọn lọc](../qa/SCROLL-RESTORE/integration.txt) đạt 27/27 trên DB độc lập. Đây là tập chạy riêng, không cộng hai tổng. Chrome headless điều khiển tự động trên fixture khác với thao tác tay của owner. [Chrome logo](../qa/WORDMARK-FIX/browser.json) chỉ đo logo. [Tiến độ 13 WP](BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md) vẫn ghi QA-01 BLOCKED dù G13 local từng đạt; CI của revision chứa diff chưa có. Lượt này chỉ đọc và so bằng chứng.

## Danh mục Git trước khi viết báo cáo

```text
 M docs/03-roadmap-and-team.md
 M docs/DECISIONS.md
 M docs/testing/03-huong-dan-kiem-thu-thu-cong.md
 D src/main/java/com/beautyshop/ai/dto/TuVanDaRequest.java
 D src/main/java/com/beautyshop/ai/service/LoaiSanPham_TimKiem.java
 D src/main/java/com/beautyshop/ai/service/TuVanDaService.java
 M src/main/java/com/beautyshop/controller/AnhTaiLen_Advice.java
 M src/main/java/com/beautyshop/controller/CuaHang_Controller.java
 M src/main/java/com/beautyshop/controller/DiaChiNguoiDung_Controller.java
 M src/main/java/com/beautyshop/controller/DonHang_Controller.java
 M src/main/java/com/beautyshop/controller/NguonDieuHuong.java
 M src/main/java/com/beautyshop/controller/SauMua_Controller.java
 D src/main/java/com/beautyshop/controller/TuVanDa_Controller.java
 M src/main/java/com/beautyshop/controller/TuVan_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminBaoMatTaiKhoan_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminBienTheSanPham_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminDanhMucSanPhamThuongHieu_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminDonHang_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminNguoiDung_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminPhieuGiamGia_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminSauMua_Controller.java
 M src/main/java/com/beautyshop/dto/PhanTrang.java
 M src/main/java/com/beautyshop/repository/BienTheSanPham_Repository.java
 M src/main/java/com/beautyshop/repository/DanhMuc_Repository.java
 M src/main/java/com/beautyshop/repository/DoiTra_Repository.java
 M src/main/java/com/beautyshop/repository/DonHang_Repository.java
 M src/main/java/com/beautyshop/repository/NguoiDung_Repository.java
 M src/main/java/com/beautyshop/repository/PhieuGiamGia_Repository.java
 M src/main/java/com/beautyshop/repository/SanPham_Repository.java
 M src/main/java/com/beautyshop/repository/ThuongHieu_Repository.java
 M src/main/java/com/beautyshop/repository/YeuThich_Repository.java
 M src/main/java/com/beautyshop/security/Security_Config.java
 M src/main/java/com/beautyshop/service/BienTheSanPham_Service.java
 M src/main/java/com/beautyshop/service/DanhMucSanPhamThuongHieu_Service.java
 M src/main/java/com/beautyshop/service/DiaChiNguoiDung_Service.java
 M src/main/java/com/beautyshop/service/DonHang_Service.java
 M src/main/java/com/beautyshop/service/NguoiDung_Service.java
 M src/main/java/com/beautyshop/service/PhieuGiamGia_Service.java
 M src/main/java/com/beautyshop/service/SauMua_Service.java
 M src/main/resources/static/css/admin.css
 M src/main/resources/static/css/components.css
 M src/main/resources/static/css/reference-pages.css
 M src/main/resources/static/css/storefront.css
 M src/main/resources/static/img/brand/beautyshop-wordmark.svg
 M src/main/resources/static/js/app.js
 M src/main/resources/static/js/wishlist.js
 D src/main/resources/templates/TuVanDa.html
 M src/main/resources/templates/admin/catalog/form.html
 M src/main/resources/templates/admin/catalog/list.html
 M src/main/resources/templates/admin/user/detail.html
 M src/main/resources/templates/admin/user/list.html
 M src/main/resources/templates/admin/user/reviews.html
 M src/main/resources/templates/admin/variant/form.html
 M src/main/resources/templates/admin/variant/list.html
 M src/main/resources/templates/admin/voucher/form.html
 M src/main/resources/templates/admin/voucher/list.html
 M src/main/resources/templates/auth/account.html
 M src/main/resources/templates/auth/address-form.html
 M src/main/resources/templates/auth/addresses.html
 M src/main/resources/templates/auth/login.html
 M src/main/resources/templates/auth/register.html
 M src/main/resources/templates/auth/security.html
 M src/main/resources/templates/auth/wishlist.html
 M src/main/resources/templates/cart/index.html
 D src/main/resources/templates/chat/TuVanDa.html
 M src/main/resources/templates/fragments/admin-nav.html
 M src/main/resources/templates/fragments/admin-table.html
 M src/main/resources/templates/fragments/footer.html
 M src/main/resources/templates/fragments/nav-fields.html
 M src/main/resources/templates/fragments/ui.html
 M src/main/resources/templates/order/checkout.html
 M src/main/resources/templates/order/customer-detail.html
 M src/main/resources/templates/order/detail.html
 M src/main/resources/templates/order/list.html
 M src/main/resources/templates/order/review.html
 M src/main/resources/templates/product/detail.html
 M src/main/resources/templates/product/list.html
 M src/main/resources/templates/return/detail.html
 M src/main/resources/templates/return/list.html
 D src/test/java/com/beautyshop/ai/service/TuVanDaServiceTest.java
 M src/test/java/com/beautyshop/controller/AnhTaiLenAdviceTest.java
 M src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java
 M src/test/java/com/beautyshop/controller/GioHangPagesTest.java
 D src/test/java/com/beautyshop/controller/TuVanDaControllerTest.java
 M src/test/java/com/beautyshop/controller/TuVan_ControllerTest.java
 M src/test/java/com/beautyshop/controller/admin/AdminDanhGiaPagesTest.java
 M src/test/java/com/beautyshop/integration/XacThuc_MariaDb_Test.java
 D src/test/java/com/beautyshop/repository/TuVanDaRepositoryTest.java
 M src/test/java/com/beautyshop/service/AdminServiceMethodSecurityTest.java
?? docs/qa/ADDRESS-FOCUS/browser.json
?? docs/qa/ADDRESS-FOCUS/maven.txt
?? docs/qa/ADMIN-RESTORE/browser.json
?? docs/qa/ADMIN-RESTORE/integration.txt
?? docs/qa/ADMIN-RESTORE/maven.txt
?? docs/qa/BUTTON-WIDTH/browser-states.json
?? docs/qa/BUTTON-WIDTH/browser.json
?? docs/qa/CHAT-ROUTE-01/http.json
?? docs/qa/CHAT-ROUTE-01/integration.txt
?? docs/qa/CHAT-ROUTE-01/package.txt
?? docs/qa/CHAT-ROUTE-01/test.txt
?? docs/qa/FIELD-SPACING/browser.json
?? docs/qa/FIELD-SPACING/maven.txt
?? docs/qa/LIST-PAGING/browser-count-alignment.json
?? docs/qa/LIST-PAGING/browser-voucher-return-nav.json
?? docs/qa/LIST-PAGING/browser.json
?? docs/qa/LIST-PAGING/integration.txt
?? docs/qa/LIST-PAGING/maven-count-alignment.txt
?? docs/qa/LIST-PAGING/maven-voucher-return-nav.txt
?? docs/qa/LIST-PAGING/maven.txt
?? docs/qa/NAV-CATALOG/admin-integration.txt
?? docs/qa/NAV-CATALOG/browser-addresses.json
?? docs/qa/NAV-CATALOG/browser-admin.json
?? docs/qa/NAV-CATALOG/browser-followup.json
?? docs/qa/NAV-CATALOG/browser-footer.json
?? docs/qa/NAV-CATALOG/browser-tabs.json
?? docs/qa/NAV-CATALOG/browser.json
?? docs/qa/NAV-CATALOG/maven-tabs.txt
?? docs/qa/SCROLL-RESTORE/browser.json
?? docs/qa/SCROLL-RESTORE/integration.txt
?? docs/qa/SCROLL-RESTORE/maven-package.txt
?? docs/qa/SCROLL-RESTORE/maven-test.txt
?? docs/qa/WISH-RESTORE/browser.json
?? docs/qa/WISH-RESTORE/maven.txt
?? docs/qa/WORDMARK-FIX/before.json
?? docs/qa/WORDMARK-FIX/browser.json
?? docs/qa/WORDMARK-FIX/maven.txt
?? docs/reviews/ADDRESS-FOCUS-ket-qua.md
?? docs/reviews/ADMIN-RESTORE-ket-qua.md
?? docs/reviews/BUTTON-WIDTH-ket-qua.md
?? docs/reviews/CHAT-ROUTE-01-ket-qua.md
?? docs/reviews/FIELD-SPACING-ket-qua.md
?? docs/reviews/FUNCTIONAL-REVIEW-doi-chieu-sau-nang-cap.md
?? docs/reviews/LIST-PAGING-ket-qua.md
?? docs/reviews/NAV-CATALOG-ket-qua.md
?? docs/reviews/SCROLL-RESTORE-ket-qua.md
?? docs/reviews/WISH-RESTORE-ket-qua.md
?? docs/reviews/WORDMARK-FIX-ket-qua.md
?? docs/ui/screenshots/WORDMARK-FIX/footer-after.webp
?? docs/ui/screenshots/WORDMARK-FIX/login-after.webp
?? docs/ui/screenshots/WORDMARK-FIX/login-before.webp
?? docs/ui/screenshots/WORDMARK-FIX/opening-reference.webp
?? docs/ui/screenshots/WORDMARK-FIX/register-mobile.webp
?? src/main/java/com/beautyshop/dto/BoLocDanhSachAdmin.java
?? src/main/resources/templates/chat/index.html
```
