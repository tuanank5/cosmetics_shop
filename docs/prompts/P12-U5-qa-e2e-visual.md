# P12+ / U5 – Kiểm thử E2E, visual regression và accessibility có bằng chứng

> **PHẦN MỞ RỘNG ĐỀ XUẤT, KHÔNG NẰM TRONG U1–U4 GỐC CỦA CLAUDE.** U5 không phải một gói redesign mới; đây là cổng nghiệm thu UI/UX và luồng nghiệp vụ trước feature freeze/demo.
>
> **Cách dùng:** lưu `docs/prompts/P12-U5-qa-e2e-visual.md`; bắt đầu bằng `Chỉ làm U5.1 – Thiết lập ma trận và môi trường kiểm thử.`
>
> **Phụ thuộc:** U1–U4 đã làm hoặc báo rõ lát cắt còn thiếu; catalog F01 phải được xác nhận nguồn dữ liệu trước khi chạy flow mua hàng. Không cần đợi UI "hoàn hảo" mới bắt đầu QA smoke.

## 0. Vai trò, mục tiêu và giới hạn

Bạn là QA Lead + Test Automation Engineer + Accessibility Engineer. Mục tiêu là biến tuyên bố "đã có giao diện" thành **bằng chứng định lượng và tái lập**, không tạo screenshot giả hoặc lấy log từ lần chạy cũ làm kết quả hiện tại.

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

## 1. Quy tắc riêng U5

- Ưu tiên **kiểm thử, không sửa giao diện**. Khi phát hiện lỗi, ghi issue có cách tái hiện và file/dòng; chỉ sửa nhỏ khi user giao riêng.
- Nếu trình duyệt/Playwright/axe **chưa được cài hoặc không khả dụng**, phát hành manual QA checklist và hướng dẫn setup; **không tự cài dependency** ngoài repo hoặc thay cấu hình hệ thống. Kiểm thử bằng browser có thể dùng máy Windows người dùng; không báo PASS từ parse HTML.
- Tạo test accounts **chỉ trên DB test** được owner cho phép; không dùng tài khoản thật/secrets; không export cookie/session.
- Ảnh chụp có thể chứa PII: dùng fixtures tổng hợp và che dữ liệu trước khi đưa vào report; không upload credentials.

## 2. U5.1 – Baseline và cách cô lập môi trường

1. Kiểm `git status`, commit, Java 21, Maven, version application, host/port thực, profile và JDBC URL hiệu lực **mà không in secret**.
2. Đề nghị xác minh `beautyshop_test` tồn tại; account test có quyền thích hợp; đường dẫn upload tách khỏi uploads demo. Xác minh Flyway V1–V5 chỉ trên test DB; không reset demo DB.
3. Lập `docs/qa/test-matrix.md` có bảng feature × Guest/Customer A/Customer B/Admin × viewport × expected result × environment.
4. Kiểm tra dữ liệu chuẩn tối thiểu trên test DB: danh mục/thương hiệu active, ≥2 product, SKU có tồn/hết tồn, voucher hợp lệ/hết hạn, địa chỉ và đơn test theo trạng thái. **Chỉ đề xuất seed test, phải được duyệt trước khi chạy.**
5. Ghi baseline `mvn.cmd -B clean verify` (nếu an toàn); so sánh số **34 skipped** cũ với kết quả thực, chỉ bật env gate sau khi xác nhận test DB.

**Nghiệm thu U5.1:** có ma trận kiểm thử và báo cáo rõ môi trường, test DB, browser; nếu thiếu thì ghi BLOCKED và dừng.

## 3. U5.2 – Guest / Customer / Admin E2E

**Flow mua hàng test (DB test):** Home → Catalog → filter/sort/page → PDP đổi SKU → thêm giỏ Guest → login → merge giỏ → địa chỉ → checkout quote/voucher → COD → đơn hàng → hủy/đánh giá/đổi trả tùy trạng thái có thật.

- Guest checkout phải chuyển login; Customer không đọc được đơn/địa chỉ Customer khác; Guest/Customer không thực hiện POST `/admin/**`.
- Test dữ liệu biên: SKU inactive/sold-out, số lượng 0/âm/vượt tồn, voucher hết hạn/hết lượt, 2 lần submit, session expired.
- Test Admin: catalog, SKU/ảnh, đơn, voucher, user status, reviews/returns, AI reindex mock; tất cả thao tác ghi chỉ DB/upload test.
- MoMo: chỉ dùng **mock**, verify thông điệp không nhận browser return làm thanh toán hoàn tất; không gọi IPN/gateway thật.
- Chạy từng nhóm và lưu **request trace đã loại token/password**, expected/actual, test case ID, screenshot nếu có.

**Nghiệm thu:** các hành trình thiết yếu P0 có PASS thật hoặc blocker cụ thể, không có test tác động production.

## 4. U5.3 – Responsive, keyboard và WCAG 2.2 AA

Kiểm **5 viewport bắt buộc**: `360×800`, `390×844`, `768×1024`, `1366×768`, `1920×1080`.

- Mobile navigation, card/catalog, PDP sticky, cart quantity, checkout errors, admin list/actions; đo `scrollWidth > clientWidth` cùng screenshot khi có overflow.
- Keyboard: Tab/Shift+Tab/Enter/Space, skip link, focus không bị sticky nav che, dialog xác nhận quản lý focus, escape/restore focus nếu dùng dialog.
- Zoom 200%, reflow, text spacing, reduced-motion; **không coi 44px là ngưỡng WCAG AA duy nhất**, kiểm target theo tiêu chí tương ứng.
- Axe-core khi được cài và cho phép: ghi rule, element, severity, false-positive triage; axe PASS không thay thế screen-reader/manual testing.
- Kiểm contrast trên computed style và screenshot, không chỉ token source. Các ảnh có alt thật, font có dấu tiếng Việt không bị cắt.

**Nghiệm thu:** bằng chứng cho từng trang trọng yếu, issue có ảnh và cách tái hiện; nếu browser không có, tất cả browser-only mục là NOT VERIFIED.

## 5. U5.4 – Visual regression và ma trận trạng thái

1. Lưu ảnh gốc từ **cùng browser, cùng viewport, cùng fixture, cùng font và network**; ghi rõ timestamp/build/commit. Không so ảnh khác dataset và gọi là regression.
2. Baseline cho 15 nhóm trạng thái: home, catalog có/không sản phẩm, PDP nhiều SKU/hết hàng, cart trống/có hàng, login lỗi, register lỗi, checkout có/bỏ voucher, order status, return/review, Admin list dài, upload lỗi, chat answer/429, error 404/413.
3. Tolerance thay đổi ảnh phải mô tả; ảnh sản phẩm động có thể mask đúng vùng; không mask phần CTA/giá/lỗi.
4. Ghi `docs/qa/visual-regression.md` bảng expected/actual và link ảnh; không tự gắn PASS nếu chỉ mới tạo ảnh baseline mà chưa compare.

**Nghiệm thu:** phát hiện được lỗi tràn, lệch style, mất nút, thiếu focus, sai trạng thái thực; có repro.

## 6. U5.5 – Regression checklist và cổng QA

- Chạy unit/MVC/integration trên test DB khi đủ điều kiện; báo thành tích theo **số bài chạy** và skipped, không theo class count.
- Kiểm không thay đổi URL/name/CSRF/role; smoke HTTP 400/403/404/409/413/500 giữ status; stale link, invalid page, upload oversize.
- Phân loại P0/P1/P2; mỗi lỗi có ID, severity, owner, file/line, repro, screenshot/test link, disposition.
- Đặt cổng **GO/NO-GO**: mọi P0 liên quan checkout/auth/authorization/data integrity phải PASS hoặc có biện pháp cô lập demo được owner duyệt; không che lỗi bằng màu xanh/đổi nội dung UI.

## 7. Kết quả cần tạo

- `docs/qa/test-matrix.md`
- `docs/qa/visual-regression.md`
- `docs/qa/manual-qa-checklist.md` (khi thiếu browser)
- `docs/reviews/qa-U5-2026-11.md`
- `docs/ui/screenshots/U5.x/` (chỉ ảnh thật, thông tin cá nhân đã loại bỏ)

**Definition of Done:** P0 flows chạy thật trên DB test, test counts được ghi, responsive/accessibility có browser evidence hoặc minh bạch NOT VERIFIED, không có POST lên DB demo, mọi phát hiện có repro.

**Lệnh cuối prompt:** `Chỉ làm U5.1; tạo ma trận và báo cáo, sau đó DỪNG.`
