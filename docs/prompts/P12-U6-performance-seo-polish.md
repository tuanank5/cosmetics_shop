# P12+ / U6 – Tối ưu hiệu năng, SEO cơ bản và hoàn thiện chất lượng giao diện

> **PHẦN MỞ RỘNG ĐỀ XUẤT NGOÀI P12 GỐC.** U6 chỉ bắt đầu sau khi U5 có baseline đo thật. Không đánh đổi tính đúng đắn checkout, security hoặc accessibility để tăng điểm Lighthouse.
>
> **Cách dùng:** lưu `docs/prompts/P12-U6-performance-seo-polish.md`, giao `Chỉ làm U6.1` rồi nghiệm thu theo từng lát cắt.

## 0. Vai trò và mục tiêu

Bạn là Web Performance Engineer + SEO Technical Reviewer + Frontend QA. Tối ưu storefront Luxury bằng số đo thực tế; không biến Spring MVC thành SPA, không mang thêm bundler/framework hoặc CDN.

## Quy tắc chung bắt buộc

1. **Nguồn sự thật:** đọc `AGENTS.md`, `README.md`, `docs/prompts/P12-luxury-ui-upgrade.md`, `docs/prompts/P12-U2-luxury-storefront.md`, `docs/reviews/frontend-audit-2026-11.md`, báo cáo U1/U2 nếu tồn tại, rồi **đối chiếu với mã nguồn hiện tại**. Báo cáo audit chụp tại 22/11/2026, không mặc định vẫn đúng sau các commit mới.
2. **Môi trường:** `E:\Nam 4 HK 1\WWW\cosmetics_shop`; Java 21; Spring Boot 3.5.16; Thymeleaf + Spring Security; MariaDB + Flyway; CSS/JS thuần. `mvn.cmd -version` phải chỉ Java 21; chỉ chỉnh `JAVA_HOME`/`Path` trong terminal hiện tại khi cần, không sửa máy toàn cục.
3. **Thay đổi nhỏ, có kiểm soát:** đọc `git status --short` trước tiên; không reset/stash/ghi đè sửa đổi không thuộc phiên; mỗi lát cắt là một đơn vị có thể rollback. **Không tự commit/push**.
4. **Giữ contract:** không đổi URL, HTTP method, `name=`, `th:field`, `th:action`, CSRF, Spring Security role/ownership, giá, tồn, vận chuyển, voucher, thanh toán, MoMo IPN/HMAC, quy tắc hủy/đổi trả, schema/entity/migration. Nếu buộc sửa Java, service, repository hoặc thêm dependency, **đưa đề xuất file/method, tác động và regression plan, rồi chờ duyệt**.
5. **Không tạo thông tin giả:** không bịa sản phẩm, ảnh mỹ phẩm/logo thương hiệu, review, số lượng bán, địa chỉ liên hệ, pháp lý, chính sách hoặc tình trạng thanh toán. Không dùng CDN, framework frontend mới, `th:utext`, `innerHTML`, inline script/style/event, trừ ngoại lệ có căn cứ và được phê duyệt.
6. **Database an toàn:** không tự seed, đặt đơn, sửa tồn, tạo tài khoản, upload hoặc thử thanh toán trên database đang dùng `beautyshop`. Trước bất kỳ test ghi nào phải xác minh *từ cùng tiến trình test* rằng JDBC URL trỏ `beautyshop_test`, schema dữ liệu đã được xác nhận, tài khoản có quyền phù hợp và thư mục upload riêng. `P01_MARIADB_TEST=true` **không được bật** khi chưa có những điều kiện đó.
7. **Bằng chứng:** phân biệt `S` = code/source, `H` = HTTP render/headers, `B` = browser, `T` = test thực chạy. Không có browser thì ghi `NOT VERIFIED`, không tự kết luận responsive/WCAG/Lighthouse PASS. Không có đủ dữ liệu hoặc môi trường thì ghi `BLOCKED`, không ngụy tạo số đo.
8. **Test:** chạy kiểm tra an toàn với JDK 21, thống kê `Tests run / Failures / Errors / Skipped` từ log thật. Ở baseline cũ `50 run, 34 skipped`; số này **không tự trở thành PASS** sau thay đổi. Không để test tùy tiện kết nối DB demo.
9. **Ngôn ngữ thị giác:** kế thừa U1 Quiet Luxury (kem, rosewood, champagne chỉ trang trí), type serif cho heading và sans cho nội dung; nút thông thường ≥48px, CTA ≥56px, không control nhấn nào thấp hơn 44px theo tiêu chuẩn thiết kế P12; chữ dễ đọc, tương phản đo thật.
10. **Dừng có chủ đích:** chỉ làm lát cắt được giao, tạo báo cáo tiến độ, chờ nghiệm thu; không tự chuyển sang lát cắt/giai đoạn tiếp theo.

## 1. Tính đúng trước khi nhanh

- Thu thập **LCP, CLS, INP, TTFB, request waterfall, transfer size, cache headers** trên đúng môi trường, thiết bị, mạng, fixture; nếu thiếu browser thì phần CWV để `NOT VERIFIED`.
- `theme.css`, `storefront.css`, `admin-catalog.css` trước audit tổng khoảng 11KB CSS/JS, chưa phải bằng chứng nghẽn; **đừng tối ưu mù** các file nhỏ.
- Audit ghi static assets trả `Cache-Control: no-store` tại **localhost dev**, không suy ra production đã sai.
- Không bật public cache cho account, order, checkout, response có session/CSRF; không thay CSP/security headers bừa bãi.

## 2. U6.1 – Lập baseline và performance budget

1. Chuẩn hóa 3–5 trang điển hình: Home, Catalog nhiều ảnh, PDP, Cart, Checkout, Admin list (role test). Ghi build/commit và dataset.
2. Browser thật: Lighthouse/Lab và Web Vitals nếu có; ghi condition CPU/network, viewport, retry. Không tạo score giả; thiếu browser thì audit source/resource và blocker.
3. Liệt kê file ảnh/font/CSS/JS, kích thước và nguồn gốc; ghi **ảnh sản phẩm thực chưa có theo audit cũ** nếu vẫn đúng; không đo trang catalog rỗng rồi kết luận performance catalog đủ.
4. Đề xuất budget riêng cho ảnh card/detail, JS, CSS, số HTTP requests; budget là **mục tiêu tương lai**, không ghi là metric đã đo.

**Nghiệm thu:** có bảng baseline có thể tái lập hoặc toàn bộ mục browser là NOT VERIFIED.

## 3. U6.2 – Media pipeline và font đúng giấy phép

1. Xác minh **quyền sử dụng ảnh thương hiệu/sản phẩm**; nếu không có quyền, chỉ dùng placeholder CSS/SVG do dự án tạo, không scrape và dùng ảnh tùy tiện.
2. Đề xuất derivative thumbnail/card/detail theo đúng nhu cầu; không phá controller ảnh hiện tại, upload re-encode, giới hạn 2MB/2048px, chống path traversal. Thay backend xử lý ảnh chỉ khi user duyệt.
3. `srcset`, `sizes`, lazy cho ảnh dưới fold, ưu tiên ảnh hero/PDP quan trọng nếu có; không auto-lazy ảnh LCP; dimension/aspect-ratio chống CLS.
4. Webfont local `.woff2` khi có giấy phép và glyph tiếng Việt; không có thì sử dụng system fallback. `font-display: swap`, test dấu tiếng Việt/CLS, không copy font từ máy khác khi không có quyền.
5. Tạo checklist kiểm ảnh ALT, lỗi tải, fallback, kích thước, file bytes.

**Nghiệm thu:** ảnh đúng nguồn/quyền, có fallback, bytes giảm thật nếu đã thử; không làm giảm an toàn upload.

## 4. U6.3 – Tài nguyên tĩnh, cache và network

1. Đánh giá HTTP caching của static có version/fingerprint; chỉ cấu hình long cache cho immutable assets, tránh stale CSS/JS khi deploy.
2. Xác minh compression (gzip/brotli), reverse proxy behavior, ETag cho ảnh *nếu môi trường có*, ghi khác nhau giữa dev/prod.
3. Loại CSS chết/override sau U1–U4 **chỉ bằng kiểm chứng trên DOM/role/viewport**; không xóa selector vì không thấy ở trang Guest.
4. Giữ `defer` cho JS, tránh blocking scripts, không import thư viện để làm animation hoặc carousel.
5. Không thay chính sách CSRF/session vì muốn cache hay tối ưu TTFB; không cache HTML cá nhân.

**Nghiệm thu:** request/header đúng, không hiển thị CSS cũ sau deploy; không mất form CSRF/role behavior.

## 5. U6.4 – SEO storefront và metadata chính xác

1. Review `<title>`, description, canonical, Open Graph từ **sản phẩm/slug thật** cho Home/Catalog/PDP; không bịa review stars, rating, giá, availability hoặc sản phẩm.
2. Tránh index login, account, checkout, orders, admin, cart và URL có dữ liệu cá nhân; cân nhắc `noindex` phù hợp. `robots.txt` **không phải bảo mật/authorization**.
3. Đề xuất sitemap cho URL public chỉ khi backend cung cấp slug active ổn định; thêm route/generation phải xin duyệt.
4. Nếu dùng structured data Product, mọi trường phải khớp dữ liệu server và hình ảnh được cấp quyền; thiếu data thì không phát schema giả.
5. Chính sách, trang liên hệ và tên cửa hàng chỉ công khai khi owner duyệt nội dung chính xác; website đồ án có thể ưu tiên thông tin giới thiệu trung thực.

**Nghiệm thu:** meta không sai tên/giá/voucher/status, protected URLs không vô tình được index theo policy.

## 6. U6.5 – Polish & hiệu năng lại sau thay đổi

- Browser 5 viewport, zoom 200%, keyboard và axe manual check cho ảnh/font/card/header/CTA.
- So Lighthouse/CWV **trước–sau cùng điều kiện**, ghi trung vị/biên độ khi có mẫu, không tự tuyên bố "đạt 90+" nếu chưa đo.
- Regression: image upload, PDP SKU, checkout session/CSRF, Admin dashboard và order role views.
- Giữ riêng list issue F14 (ảnh), F17 (CSS), F20 (home), F21 (cache) và F22 (favicon), đánh dấu theo chứng cứ.

## 7. Kết quả cần tạo và cổng

- `docs/performance/baseline.md`, `docs/performance/assets-budget.md`, `docs/reviews/performance-U6-2026-11.md`.
- Bảng metric `Trang / Môi trường / Viewport / LCP / CLS / INP / Lighthouse / Thời điểm / Evidence`, dùng `NV` cho dữ liệu không đo.
- Test counts PASS/FAIL/ERROR/SKIPPED; screenshots trong `docs/ui/screenshots/U6.x/` nếu có.

**Definition of Done:** có baseline và so sánh định lượng hoặc tuyên bố trung thực không thể đo; image/font có nguồn hợp lệ; HTML riêng tư không cache public; SEO không bịa dữ liệu; không hồi quy UI/checkout/authorization.

**Lệnh cuối prompt:** `Chỉ làm U6.1, không thay source trước khi có baseline; báo cáo và DỪNG.`
