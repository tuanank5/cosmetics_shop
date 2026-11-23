# BeautyShop — Báo cáo P12/U1

Ngày 22/11/2026, múi giờ Asia/Saigon. Nguồn yêu cầu: prompt `P12-luxury-ui-upgrade.md` ở root và audit `frontend-audit-2026-11.md`. Prompt yêu cầu làm từng giai đoạn, vì vậy đợt này chỉ thực hiện **U1 — nền tảng**, chưa triển khai U2–U4.

Báo cáo U1 được giữ làm mốc. Kết quả của đợt tiếp theo nằm tại [báo cáo U2.1](ui-upgrade-U2-2026-11.md).

## Kết quả và phạm vi

Đã tạo hệ token màu/spacing/font, control và nút chung, head/header/footer, favicon B, flash/field-error/money/pagination/product-card và dialog xác nhận. Hai trang auth có bố cục mới, lỗi liên kết với ô, toggle mật khẩu, gợi ý theo DTO. Có sáu trang lỗi theo status và một trang mặc định tiếng Việt, không đưa dữ liệu exception ra HTML.

Tất cả 29 trang hiện có dùng head chung, main đích cho skip link và hệ class btn. Bảy trang lỗi mới đưa tổng trang được rà source lên 36. Thẻ sản phẩm/phân trang hiện có dùng fragment chung để kiểm chứng tái sử dụng; không đổi truy vấn hoặc chính sách giá. Việc bỏ file JS cũ cũng bỏ hành vi tự gửi select SKU: nút Xem biến thể hiện có vẫn hoạt động.

Các form nghiệp vụ giữ nguyên th:action, th:formaction, method, name và th:field so với HEAD trước task. Chỉ bổ sung form tìm kiếm trên header. Không thay entity, migration, repository, service, security config hoặc logic tiền/tồn/đơn/voucher/thanh toán.

Java được bổ sung theo ngoại lệ UI trong prompt:

- `GiaoDien_Advice`: view-model chỉ đọc uiPath và uiCartCount, đếm số đơn vị từ Guest session hoặc giỏ Customer. Không tạo giỏ/ghi DB; không truy vấn giỏ trên trang lỗi, payment hoặc Admin; badge là thông tin tùy chọn khi DB lỗi.
- `AnhTaiLen_Advice`: xử lý MaxUploadSizeExceededException. Multipart có thể thất bại trước khi chọn controller, nên advice toàn cục nhưng chỉ dựng lại form SKU ở URL hợp lệ và khi principal có ROLE_ADMIN. Trả 413, BindingResult lỗi hinhAnh, không gọi ghi. Phần form chưa parse được không phục hồi; giao diện nói rõ cần nhập lại. Chưa kiểm request multipart thật qua Security/Tomcat.

Không có thay đổi nghiệp vụ cần duyệt hoặc cập nhật DECISIONS. Thông tin tích hợp cho các phần P01–P11: head mới nạp tokens/base/components/storefront/admin và app.js; không tiếp tục tham chiếu theme.css/admin-catalog.css/storefront.js. Không gửi thông báo bên ngoài hoặc tạo PR trong phiên này.

## File tạo/sửa

Đường dẫn dưới đây tính từ root repo.

- CSS: tạo `src/main/resources/static/css/{tokens,base,components,admin}.css`; sửa `storefront.css`; bỏ `theme.css`, `admin-catalog.css`. Phần minh họa homepage cũ chuyển sang storefront để không mất bố cục trước U2.
- JS/SVG: tạo `static/js/app.js`, `static/img/favicon.svg` dưới `src/main/resources/`; bỏ `static/js/storefront.js`.
- Fragment: sửa `templates/fragments/header.html`, `footer.html`; tạo `head.html`, `ui.html`, `product-card.html` cùng thư mục.
- Auth: sửa `templates/auth/{login,register,account,address-form,addresses,wishlist}.html`.
- Trang công khai/luồng mua: sửa `templates/home/index.html`, `product/{list,detail}.html`, `cart/index.html`, `chat/index.html`, `order/{checkout,detail,list,momo-mock,momo-return,review}.html`, `return/{detail,form,list}.html`.
- Admin: sửa `templates/admin/catalog/{form,list}.html`, `chat/index.html`, `user/{list,reviews}.html`, `variant/{form,list}.html`, `voucher/{form,list}.html`. Ngoài form ảnh SKU, thay đổi ở các trang chưa tới giai đoạn chủ yếu là head/main/class nút chung.
- Lỗi: tạo `templates/error.html` và `templates/error/{400,403,404,409,413,500}.html`.
- Java: tạo `src/main/java/com/beautyshop/controller/{GiaoDien_Advice,AnhTaiLen_Advice}.java`.
- Test: sửa `src/test/java/com/beautyshop/controller/BootstrapPagesTest.java` để kiểm asset mới; tạo `GiaoDienPagesTest.java`, `AnhTaiLenAdviceTest.java` cùng thư mục.
- Docs: tạo `docs/ui/design-system.md`, `docs/ui/QA-checklist.md`, báo cáo này; cập nhật `docs/03-roadmap-and-team.md` theo yêu cầu AGENTS về tiến độ.

Giữ nguyên file prompt ở root và báo cáo frontend audit vốn đã untracked trước task. Không di chuyển/chỉnh sửa các tài liệu đó.

## Đối chiếu F01–F24

**S** = source, phép tính offline hoặc test tự động với MockMvc/mock service, ghi rõ tại từng dòng. **H** = HTTP mạng thật. **B** = browser. Đợt này **không có H/B thành công**; MockMvc không được gọi là HTTP runtime hoặc E2E MariaDB.

| Mã | Trạng thái U1 | Bằng chứng/giới hạn |
|---|---|---|
| F01 | Chưa | Không seed hoặc xác nhận dataset trên endpoint demo; cần DB demo riêng |
| F02 | Một phần | S: 6 error view + fallback; MVC kiểm 400/403/404/409/413/500/503, đúng status, không lộ chi tiết; chưa H/B |
| F03 | Đã xử lý (S) | Bỏ storefront.js auto-submit; giữ select và nút Xem biến thể; chưa B |
| F04 | Đã xử lý (S) | Viền control chung ≥3.66:1 trên ba nền đã tính; xem design-system; chưa đo rendering B |
| F05 | Một phần | Register có summary/link/aria-describedby/invalid; login liên kết lỗi; ảnh SKU có lỗi riêng; MVC kiểm register lỗi. Form khác chờ U2–U4 |
| F06 | Đã xử lý (S) | Header skip link; cả 36 trang có main id; MVC kiểm đích tồn tại; chưa Tab B |
| F07 | Chưa | Voucher preview checkout thuộc U2 |
| F08 | Chưa | Tổng tiền/line total giỏ thuộc U2 |
| F09 | Đã xử lý (S) | Header dùng uiPath và aria-current theo nhóm route; MVC kiểm trang login; chưa B |
| F10 | Một phần | Dialog/data-confirm + focus/escape/requestSubmit có source; chưa gắn từng thao tác nghiệp vụ hoặc kiểm B, thực hiện U2–U4 |
| F11 | Một phần | app.js xử lý loading/chặn submit lặp trên POST, giữ submitter name/value, pageshow phục hồi; chưa B/network |
| F12 | Chưa | Giữ câu hỏi tư vấn thuộc U3 |
| F13 | Ngoài phạm vi | Không triển khai filter/pagination đầy đủ Admin trong P12 |
| F14 | Ngoài phạm vi | Không tạo ảnh responsive nhiều kích thước |
| F15 | Ngoài phạm vi | Không làm CRUD FAQ |
| F16 | Một phần | Footer link thật và control lọc tồn tại; chưa có trang policy/contact, không bịa trang hoặc nội dung |
| F17 | Một phần | Bỏ theme/control overrides cũ, token chung không undefined, thứ tự head rõ. CSS bố cục storefront cũ giữ cho U2; chưa tuyên bố dọn xong toàn site |
| F18 | Một phần | money fragment và card dùng chung, MVC xác nhận 1.234.567 ₫; giá SKU Admin chờ U4 |
| F19 | Chưa | Tiêu đề checkout COD giữ nguyên, xử lý U2 |
| F20 | Một phần | Bỏ copy thương hiệu tạm ở footer; homepage vẫn giữ copy phát triển để xử lý trọn U2 |
| F21 | Ngoài phạm vi | Không chỉnh cache runtime/host |
| F22 | Đã xử lý (S) | Favicon SVG tự tạo và head chung; chưa H/B |
| F23 | Một phần | Control/checkbox/radio có CSS chung; chưa B ở cart/address. Bố cục riêng hoàn thiện U2/U3 |
| F24 | Một phần | Advice trả 413/form-error, unit kiểm quyền và không ghi; chưa multipart thật/DB/B |

## Kiểm chứng đã chạy

JDK: Eclipse Adoptium **21.0.13**, Maven **3.9.11**. JAVA_HOME chỉ đổi trong tiến trình, không đổi cấu hình máy.

1. `mvn -version`: ban đầu Java 17; sau đặt JAVA_HOME xác nhận Java 21.0.13.
2. `mvn -DskipTests package` ban đầu bị chặn vì Maven mặc định trỏ `C:\.m2\repository`. Lần trỏ cache người dùng bị lỗi truy cập JAR lúc compile. Đã sao chép cache vào `target/maven-repository` trong workspace và dùng offline; không sửa pom/settings hoặc cài dependency.
3. `mvn -o "-Dmaven.repo.local=target/maven-repository" test`: **BUILD SUCCESS**, Surefire liệt kê **63 test: 29 thành công, 0 failure, 0 error, 34 bỏ qua**. Không bật P01_MARIADB_TEST; 34 test MariaDB không được tính là đã chạy.
4. `mvn -o "-Dmaven.repo.local=target/maven-repository" -DskipTests package`: **BUILD SUCCESS**, tạo `target/beautyshop-0.0.1-SNAPSHOT.jar`. Đã xóa ba asset cũ trong target/classes trước đóng gói để tránh output tăng dần giữ file đã xóa.
5. `node --check src/main/resources/static/js/app.js`: thành công.
6. Rà source: 36 trang đều một h1 và main id; mọi button có class btn; không th:utext, inline style/script/event handler, innerHTML, CDN, !important; không CSS var chưa định nghĩa. Các URL namespace Thymeleaf/SVG không phải CDN.
7. So sánh thuộc tính của các thẻ form/input/select/textarea/button với Git HEAD: không đổi th:action, th:formaction, method, name, th:field trên các trang hiện có. Kiểm CSRF và quyền Admin/Customer vẫn qua test MVC hiện có.
8. Tính contrast từ tokens.css: chữ ink/bg 14.99:1; trắng/primary 10.76:1; muted/bg 6.11:1; viền control/nền trắng 4.28:1. Accent/bg 2.89:1 chỉ dùng trang trí. Bảng đầy đủ trong design-system.

Test mới kiểm: escaping/giữ email-họ tên và không echo mật khẩu, summary/field error, số đơn vị giỏ Guest, current/skip link, card/formatter/query pagination, bảy error view và bảo vệ role khi dựng lại form upload. Dịch vụ trong các test UI được mock; không có ghi giao dịch hoặc gọi MoMo/Ollama thật.

Các lỗi phát hiện và đã sửa trong quá trình chạy: Thymeleaf chặn query parameter trực tiếp trong th:attr, đổi sang biến boolean tại form; MVC slice thiếu resolver status view của Boot nên test khai báo chính DefaultErrorViewResolver của production; không sửa error behavior production chỉ để làm test xanh.

## Chưa kiểm chứng và chạy lại

**Chưa kiểm chứng bằng trình duyệt.** Browser báo không khả dụng, discovery `[]`; không có ảnh chụp mới. Năm viewport, Tab/Shift+Tab, 200% zoom, dialog focus trap, trạng thái gửi, axe/Lighthouse và không cuộn ngang đều còn mở theo [checklist](../ui/QA-checklist.md).

Không có DB_URL/DB_USER/DB_PASSWORD hoặc DB test độc lập được xác nhận trong shell. Không lấy credential từ tiến trình khác, không seed database đang dùng. Bốn probe GET `/login`, `/register`, `/products/ui-probe-not-found`, `/products?page=abc` tại localhost:8080 đều không kết nối được. Không có kết quả dev MariaDB, HTTP runtime hoặc COD E2E mới trong đợt này.

Chạy lại kiểm thử/build trong môi trường hiện tại:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
mvn -version
mvn -o '-Dmaven.repo.local=target/maven-repository' test
mvn -o '-Dmaven.repo.local=target/maven-repository' -DskipTests package
```

Trên máy cấu hình Maven bình thường, dùng `mvn test` và `mvn -DskipTests package`. Để nghiệm thu dev, tạo DB demo riêng, chạy migration/seed theo checklist rồi chạy JAR với DB_* qua môi trường. Chỉ bật integration test khi đã có schema test độc lập có thể ghi; không dùng database demo cần giữ hoặc production.

Chưa có font/ảnh/policy mới cần duyệt. Hai advice UI nằm trong ngoại lệ Java đã cho phép ở prompt. U2–U4 sẽ thực hiện sau khi người dùng xem U1 theo trình tự P12. Chưa có PR hoặc deploy.

Đã thử stage riêng các file U1 để tạo commit theo prompt. Git trả `Unable to create .git/index.lock: Permission denied`; metadata Git đang chỉ đọc trong sandbox. Không stage được, không có commit mới. Không yêu cầu nâng quyền và không chỉnh metadata để vượt giới hạn. Gợi ý commit khi chạy ngoài giới hạn này: `feat(ui): establish luxury design system and auth layouts (U1)`; chỉ stage các file U1 ở trên, giữ nguyên prompt/audit chưa track của người dùng.
