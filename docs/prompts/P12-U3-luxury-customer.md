# P12 / U3 – Tài khoản, hậu mua hàng và tư vấn khách hàng (Luxury Customer Experience)

> **Cách dùng:** lưu ở `docs/prompts/P12-U3-luxury-customer.md`. Dán file cho Codex và kết thúc bằng: **`Chỉ làm U3.1. Báo cáo, dừng và chờ duyệt.`** Tiếp tục từng lát cắt U3.2 → U3.5 khi đã nghiệm thu.
>
> **Nguồn:** mở rộng chi tiết *GIAI ĐOẠN U3* trong tài liệu P12 của Claude; không thay thế U1/U2.
>
> **Phụ thuộc:** U1 có design system; U2 đã ổn định layout mua hàng và template đơn dùng chung hoặc có danh sách blocker rõ ràng. Không tự sửa U2 ngoài phạm vi để hoàn thành U3.

## 0. Vai trò và mục tiêu

Bạn là Senior Frontend Engineer + UI/UX Designer + Thymeleaf/Spring Security Engineer + Accessibility QA.

Mục tiêu: làm toàn bộ hành trình **đăng nhập → tài khoản → địa chỉ → đơn hàng → yêu thích → đánh giá/đổi trả → tư vấn** thống nhất, dễ hiểu, đẹp theo Quiet Luxury và không phá vỡ phân quyền/ownership.

**Các route đang có theo audit 22/11:** `/login`, `/register`, `/account`, `/account/addresses`, `/account/wishlist`, `/orders`, `/orders/{id}`, `/account/returns`, `/orders/{id}/returns/new`, `/chat`. Đây chỉ là bản đồ tham khảo; đọc controller thực tế trước khi sửa.

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

## 1. Phạm vi và ranh giới

**Được sửa:** template trong `auth/`, `order/` phần hiển thị Customer, `return/`, `chat/`; CSS/JS và fragment đã có. Chỉ bổ sung view-model read-only sau khi được duyệt. Không tự mở route mới, thêm tính năng lưu lịch sử chat, làm refund tự động hay mở điều kiện review/return.

**Đặc biệt:** `order/list.html`, `order/detail.html`, `return/list.html`, `return/detail.html` được **dùng chung Customer/Admin**. Trước mọi thay đổi liệt kê từng nhánh `base`, role, action và kiểm regression cho cả hai vai trò. U2.5 có thể đã làm card đơn và timeline cơ bản; không viết lại, chỉ bổ sung/khắc phục phần còn thiếu.

**Issue chính:** F05, F06, F09, F10, F11, F12, F23; liên quan F02, F07, F19 và F20. Không tự công bố F nào PASS khi chỉ mới sửa source.

## 2. U3.1 – Điều hướng tài khoản + hồ sơ

**Phạm vi:** `auth/account.html`, phần header/profile navigation và fragment U1.

1. Tạo sub-navigation/tabs hoặc sidebar responsive cho **Hồ sơ / Địa chỉ / Đơn hàng / Yêu thích / Đổi trả**. Dùng URL hiện hữu, `aria-current="page"` đúng trang, không sinh link hỏng.
2. Hiển thị hồ sơ bằng nhóm form rõ ràng: họ tên, email read-only theo contract, số điện thoại. `autocomplete`, label thật, mô tả lỗi gắn `aria-describedby`, `aria-invalid` và summary lỗi.
3. Tiếp nhận success/validation flash nhưng không lộ email/token nhạy cảm trong URL, log hay JS.
4. Tách rõ thông tin cá nhân khỏi CTA lưu. Có trạng thái submitting phù hợp, khi JavaScript tắt form vẫn hoạt động.
5. Empty/expired session phải có hành vi chuẩn: login redirect do Spring Security; không tạo UI giả rằng vẫn đăng nhập.

**Acceptance:** thao tác hồ sơ giữ input khi lỗi; keyboard tới được mọi field; Customer thấy đúng dữ liệu của mình; Guest không có quyền vào tài khoản. Dùng test DB riêng cho POST.

**Dừng sau U3.1.**

## 3. U3.2 – Địa chỉ giao hàng và thao tác phá hủy

**Phạm vi:** `auth/addresses.html`, `auth/address-form.html`, JS/dialog xác nhận U1.

1. Thẻ địa chỉ: người nhận, số điện thoại, địa chỉ, nhãn "Mặc định" từ dữ liệu backend; không tự suy ra địa chỉ chính.
2. Form: trường địa chỉ dài có thể dùng `textarea` **chỉ khi giữ nguyên `name`, DTO binder và validation**; label/feedback/summary lỗi rõ; checkbox `type=checkbox` không nhận style cao 48px của input text (F23).
3. Phân biệt Sửa, Đặt mặc định (nếu endpoint có), Xóa; không tự thêm action chưa tồn tại. Nút xóa có nhãn ngữ cảnh.
4. **Xóa địa chỉ phải có xác nhận an toàn khi không có JS**. Nếu cần route xác nhận mới, nêu phương án xin duyệt; không dùng `data-confirm` thiếu fallback mà coi là hoàn tất.
5. Giữ ownership guard ở backend; thử ID khác chỉ trên hai Customer test riêng, có quyền và dữ liệu đã phê duyệt.

**Acceptance:** địa chỉ dài không tràn ở 360px, không làm lệch checkbox; dữ liệu hợp lệ/không hợp lệ được xử lý; xóa đúng địa chỉ khi đã xác nhận và test trên DB riêng.

**Dừng sau U3.2.**

## 4. U3.3 – Đơn hàng, đánh giá và đổi trả

**Phạm vi:** `order/list.html`, `order/detail.html`, `order/review.html`, `return/list.html`, `return/form.html`, `return/detail.html`; **chỉ phần Customer**.

1. Tái sử dụng giao diện order list/detail của U2.5; thêm ngày đặt **nếu model đã có**; thẻ trạng thái văn bản + màu, tổng tiền VND, CTA xem chi tiết.
2. Timeline phải dựa trên trạng thái có thực của domain, không tạo mốc thời gian vận chuyển hoặc khả năng hoàn tiền giả. Tách rõ trạng thái **đơn** và **thanh toán**.
3. Hủy đơn: bước xác nhận nêu mã đơn và hậu quả; nếu đang thanh toán/đã giao, copy đúng policy hiện tại. Không chỉnh thuật toán hủy.
4. Review chỉ mở cho item đủ điều kiện đã giao; form 1–5 sao và nội dung, lỗi từng field; trạng thái chờ duyệt/ẩn đúng backend. Không thêm đánh giá mẫu.
5. Đổi trả: giữ hạn 7 ngày nếu đang có trong policy backend; không đổi ngày/quantity; khi không có item còn quyền trả thì vô hiệu/ẩn submit với lời giải thích. Trình bày trạng thái yêu cầu như backend trả về.
6. Empty state có liên kết hữu ích; không đưa link nghiệp vụ chưa tồn tại. Khi Customer/Admin cùng dùng template, bắt buộc smoke hai role.

**Acceptance:** link order Customer không trỏ sang Admin, không lộ hành động Admin; review/return chỉ trong điều kiện policy, hủy có xác nhận; regression quyền và ownership bằng dữ liệu test.

**Dừng sau U3.3.**

## 5. U3.4 – Wishlist và tư vấn FAQ/RAG một lượt

**Phạm vi:** `auth/wishlist.html`, `chat/index.html` và CSS/JS.

1. Wishlist: card sản phẩm có tên, **ảnh thật nếu URL hợp lệ hoặc placeholder CSS**, giá hiện hành từ server nếu sẵn có; CTA xem sản phẩm/bỏ thích. Nếu muốn nút "Thêm vào giỏ", kiểm tra backend cho phép và có SKU xác định; nếu không thì chỉ dẫn tới PDP, không tự chọn SKU sai.
2. Chat: hiện **câu hỏi + một câu trả lời** bằng 2 khối hội thoại, không giả lập chat real-time/đa lượt; giữ lại câu hỏi sau POST, validation error hoặc 429 (F12).
3. Nhãn "Thông tin tham khảo" và nguồn FAQ **chỉ khi backend trả đúng dữ liệu**, không bịa citations từ AI. Không đưa HTML thô từ model qua `th:utext`.
4. Hiển thị pending accessible; tránh double click nhưng giữ đúng tên field/form/CSRF; input trả lời dài không tràn hoặc phá layout.
5. Không thay đổi limit hiện có của FAQ/Ollama, không gọi provider thật nếu chưa được cho phép; test mock có ghi rõ.

**Acceptance:** hỏi/lỗi/429 không mất câu hỏi; XSS-escaping vẫn hoạt động; wishlist không cho sai SKU; không tuyên bố có bộ nhớ chat.

**Dừng sau U3.4.**

## 6. U3.5 – Hoàn thiện authentication UX

**Phạm vi:** `auth/login.html`, `auth/register.html`, fragment form, CSS/JS U1.

1. Nhãn đăng nhập/đăng ký, đường dẫn qua lại, hướng dẫn mật khẩu theo yêu cầu **hiện có** (audit ghi tối thiểu 12 ký tự; xác minh lại DTO/security).
2. Nút Hiện/Ẩn mật khẩu với `type=button`, `aria-pressed` hoặc tên truy cập phù hợp; không thay đổi giá trị field, `name`, autocomplete hay form action. Tắt JS vẫn nhập/đăng nhập được.
3. Giữ lại email khi validation lỗi **chỉ khi phù hợp với security/privacy hiện hành**; không giữ mật khẩu trong HTML hoặc báo lỗi.
4. Phân biệt lỗi đăng nhập, khóa tài khoản, đăng ký thành công bằng copy rõ ràng **không tiết lộ tài khoản có tồn tại nếu backend đã chủ ý ẩn thông tin đó**.
5. Review focus, Tab, zoom, thông báo lỗi và session hết hạn trên test environment.

**Acceptance:** auth UX không làm suy yếu bảo mật hoặc CSRF, login và register còn hoạt động; keyboard và thông báo đọc được.

**Dừng sau U3.5; không chuyển U4.**

## 7. QA và báo cáo U3

- `mvn.cmd -B test` với Java 21; báo thật PASS/FAIL/ERROR/SKIPPED; chạy integration chỉ khi database test/URL/upload dir đã được cô lập.
- 5 viewport: 360×800, 390×844, 768×1024, 1366×768, 1920×1080; thêm 200% zoom, keyboard, axe nếu browser có.
- Matrix: Guest / Customer A / Customer B / Admin; form hợp lệ/lỗi; địa chỉ dài; wishlist hết hàng; chat dài/429; review/return quá hạn. Trên DB đang dùng chỉ GET read-only.
- Dùng `docs/ui/screenshots/U3.x/` cho ảnh có **URL, role, viewport, thời điểm và môi trường**.
- Cập nhật `docs/reviews/ui-upgrade-U3-2026-11.md`: file sửa, bảng F-issue (S/H/B/T), contract giữ nguyên, test count, blocker và quyết định cần owner.

## 8. Definition of Done U3

- [ ] Tài khoản có điều hướng rõ, active state đúng vai trò và trang.
- [ ] Form hồ sơ/địa chỉ có lỗi accessible, checkbox đúng type, địa chỉ dài không tràn.
- [ ] Xóa/hủy dùng xác nhận an toàn; không làm sai ownership và policy.
- [ ] Order/review/return đúng dữ liệu hiện hữu và không ảnh hưởng nhánh Admin.
- [ ] Wishlist và chat không bịa dữ liệu; câu hỏi chat được giữ sau POST/lỗi.
- [ ] Login/register có UX tốt hơn mà không lộ mật khẩu hoặc thay auth flow.
- [ ] Test đếm đúng và UX browser có bằng chứng hoặc ghi rõ NOT VERIFIED.

**Lệnh cuối prompt:** `Chỉ làm U3.1. Báo cáo kết quả rồi DỪNG; không tự commit/push.`
