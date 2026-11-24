# P12 / U4 – Quản trị Luxury Beauty: rõ thông tin, thao tác chính xác (Admin UX)

> **Cách dùng:** lưu `docs/prompts/P12-U4-luxury-admin.md`; giao từng lát cắt, bắt đầu bằng **`Chỉ thực hiện U4.1.`** Không cho Codex tự làm toàn U4.
>
> **Nguồn:** chi tiết hóa *GIAI ĐOẠN U4* của Claude. Admin hướng tới **hiệu quả, chính xác, khả năng kiểm soát**, không sao chép toàn bộ bố cục storefront.
>
> **Phụ thuộc:** U1 hoàn tất và các component U2/U3 đã ổn định. Nếu order/return template dùng chung thay đổi, phải giữ nhánh Customer.

## 0. Vai trò và mục tiêu

Bạn là Senior Admin UX Engineer + Spring MVC/Thymeleaf Engineer + QA Engineer. Tối ưu tác vụ Admin: quản lý catalog/SKU/ảnh, người dùng, đơn, voucher, review, return và tình trạng FAQ/RAG; không mở rộng nghiệp vụ theo suy đoán.

**Issue trọng tâm:** F05, F09, F10, F11, F13, F15, F18, F23, F24. F13 chỉ được cải tiến UI theo contract hiện có; **thêm search/filter/pagination server-side phải xin duyệt**, vì P12 gốc đặt phần lớn F13 ngoài scope.

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

## 1. Quy tắc riêng Admin

- Đọc các controller `admin/**`, DTO và template thực tế; `GET /admin/catalog` đang redirect sang catalog theo audit, còn dashboard cơ bản nằm `/admin/orders`; không tự tạo dashboard route mới.
- Luôn hiện **đối tượng** thao tác: mã đơn, tên SKU, người dùng, voucher và hậu quả thay đổi. Dùng button thông dụng của U1, `btn--danger` cho hủy/xóa/khóa; tránh nút xác nhận thu tiền cạnh nút hủy.
- Bảo vệ role ADMIN cả backend và frontend; **ẩn button không phải authorization**. Không tự hiển thị PII cho người không có quyền.
- Không tự chuyển từ soft-delete sang hard-delete, sửa trạng thái vận đơn, số liệu doanh thu hoặc việc hoàn tiền.
- Chỉ hiển thị thông tin lô/FEFO nếu dữ liệu, policy và DTO đã xác nhận; không thêm nghiệp vụ.

## 2. U4.1 – Khung Admin và điều hướng

**Phạm vi:** header/nav fragment, layout trang Admin hiện có, CSS admin.

1. Sidebar trên desktop, menu có thể gập trên mobile; ưu tiên `<details>` nếu không cần JS, giữ đầy đủ đường tới Users, Orders, Catalog, SKU, Vouchers, Reviews, Returns, AI.
2. Breadcrumb ngắn + 1 `<h1>` mỗi view, `aria-current` theo route, skip link tới `<main id="main">`.
3. Header/tài khoản/đăng xuất vẫn theo Spring Security và CSRF hiện tại; không dùng URL sai hoặc link tắt bypass security.
4. Header phải không tràn tại 360px và 200% zoom; các CTA Admin không che khu vực dữ liệu.

**Acceptance:** mỗi mục Admin vào đúng route, bàn phím sử dụng được, không hiển thị menu Admin cho Guest/Customer; không yêu cầu tạo controller mới.

**Dừng sau U4.1.**

## 3. U4.2 – Danh sách quản trị và mật độ thông tin

**Phạm vi:** `admin/catalog/list.html`, `admin/variant/list.html`, `admin/user/list.html`, `admin/voucher/list.html`, `admin/user/reviews.html`, `order/list.html` Admin, `return/list.html` Admin.

1. Dùng **semantic table có caption và scope** nếu dữ liệu thật dạng bảng; dùng card responsive nếu table làm mất khả năng truy cập trên màn nhỏ. Không bắt mọi danh sách phải là table; chọn theo nội dung và thao tác.
2. Cột cần: tên/mã, trạng thái **chữ + màu**, ngày/giá/số lượng khi DTO có, hành động dễ nhận biết. SKU: available, on-hand, reserved **chỉ theo giá trị server**.
3. Giá VND nhất quán có dấu nhóm nghìn (F18), không đổi BigDecimal hay quy tắc làm tròn.
4. Các nút hành động ≥44px, accessible name có tên đối tượng; xác nhận phá hủy, không dùng `confirm()` làm trải nghiệm chính nếu U1 có component dialog đạt chuẩn.
5. Dùng search/page **đã có** ở danh sách user; không tạo UI lọc giả không có backend. Nếu thêm search/page cho order/reviews/vouchers/returns (F13), đưa đề xuất riêng để owner duyệt trước khi sửa Java/repository.
6. Giữ filter/đường về trang/cursor khi endpoint hỗ trợ; empty state chỉ dẫn tạo mới hoặc quay lại.

**Acceptance:** không mất nút khi text dài/mobile, số tiền đúng, Guest/Customer không xem list Admin; không suy diễn F13 đã khắc phục hoàn toàn nếu mới thay giao diện.

**Dừng sau U4.2.**

## 4. U4.3 – Form Catalog, SKU, ảnh và voucher

**Phạm vi:** `admin/catalog/form.html`, `admin/variant/form.html`, `admin/voucher/form.html` và thành phần dùng chung.

1. Chia form bằng `fieldset/legend` và microcopy, phân biệt trường bắt buộc/tùy chọn; giữ nguyên DTO/form field `name`, `th:field`, `th:action`.
2. Label, lỗi gắn với trường, error summary; loại input đúng (number, datetime-local, file, checkbox); không để checkbox kế thừa chiều cao input text.
3. Khi sửa SKU, hiển thị **giá, tồn và ảnh hiện hành** từ model; preview ảnh dùng file local trên client để xem trước nhưng **không upload trước/ghi thay file trên DB**; hướng dẫn PNG/JPEG, <=2 MB, <=2048 px đúng giới hạn server hiện có; nếu source thay đổi, cập nhật theo giới hạn thực.
4. Lỗi multipart quá dung lượng (F24) chỉ có thể được coi PASS sau khi kiểm HTTP có server-side handler; nếu cần Java `@ControllerAdvice`, phải được duyệt trước.
5. Voucher: giải thích rõ thời gian theo **UTC** nếu đó là contract hiện tại; không tự chuyển VN giờ và lưu sai timestamp; format amount/percentage, min/max theo logic server.
6. Form long/edit có navigation thoát hợp lý; khi lỗi giữ dữ liệu hợp lệ nếu backend cho phép, không lộ secret/path upload.

**Acceptance:** admin có thể xem và sửa dữ liệu bằng môi trường test; lỗi upload được báo rõ hoặc ghi BLOCKED, CSRF còn đúng, không làm sai thời gian voucher.

**Dừng sau U4.3.**

## 5. U4.4 – Quản lý đơn, thu tiền và xử lý hậu mua

**Phạm vi:** `order/list.html`/`order/detail.html` Admin, `return/list.html`/`return/detail.html` Admin, `admin/user/reviews.html`.

1. Admin order detail phân biệt trạng thái đơn và thanh toán; **không hiển thị "đã thu" nếu chưa có trạng thái PAID xác nhận**.
2. Với nút **"Đã giao và thu tiền"**, xác nhận có mã đơn, số tiền và hậu quả; không đặt cạnh nút hủy. Bước xác nhận phải tồn tại cả khi không có JS hoặc ghi blocker xin duyệt.
3. Hiển thị các state transition **server cho phép**; không tạo option trạng thái mới; tránh double submit UI nhưng backend transaction vẫn là nguồn chân lý.
4. Review approval giữ revision/token hiện hữu, không làm mất param ẩn sau refactor; tách "duyệt" và "ẩn".
5. Return status: hiển thị chính xác trạng thái xử lý, không tự nhập kho hay hoàn tiền; chỗ chưa có dữ liệu dùng trung tính.
6. Những template dùng chung cần regression Customer/ADMIN với vai trò và action riêng.

**Acceptance:** admin không thể xác nhận thu tiền nhầm vì chỉ có 1 click bất ngờ, không đổi quyền/state machine, Customer không thấy action Admin.

**Dừng sau U4.4.**

## 6. U4.5 – Dashboard điều phối và FAQ/RAG status

**Phạm vi:** `order/list.html` Admin/dashboard cơ bản và `admin/chat/index.html`.

1. Dùng card KPIs có nhãn, đơn vị VND, mốc thời gian nếu backend có; không tự suy đoán revenue = tất cả đơn hoặc đã thu khi chưa trả tiền.
2. Nêu trạng thái AI thật từ backend: FAQ fallback, Ollama enable/disable, last indexed, số mục nếu có; không báo "AI thông minh" dựa trên mock.
3. Nút reindex có trạng thái pending và cảnh báo tác động; giới hạn xem trạng thái/tái lập chỉ mục theo quyền.
4. **CRUD FAQ (F15) là ngoài phạm vi**, không tự thêm route/schema hay hiện nút lưu giả. Nếu use case bắt buộc, ghi gap và tách backlog/duyệt.
5. Nếu muốn đồ thị/revenue theo ngày/đơn cần API mới, xin owner phê duyệt; U4 không làm dashboard BI mở rộng.

**Acceptance:** Admin hiểu việc cần làm, số liệu không sai bản chất, AI fallback/mock được gắn nhãn trung thực.

**Dừng sau U4.5; không tự chuyển U5.**

## 7. Ma trận kiểm thử bắt buộc

| Kiểm tra | Guest | Customer | Admin | Môi trường |
|---|---|---|---|---|
| Vào `/admin/**` bằng URL | bị từ chối | bị từ chối | theo quyền | HTTP chỉ đọc + test role |
| Catalog/SKU form | không được | không được | xem/sửa | DB test cho ghi |
| Danh sách dài, tên dài | không được | không được | đầy đủ nút | browser 5 viewport |
| Thu tiền/hủy đơn | không được | không được qua Admin | đúng state | DB test cô lập |
| Review/returns | không được | chỉ dữ liệu Customer | thao tác Admin | test quyền 2 vai trò |
| Upload lỗi | không được | không được | lỗi 413/validation | test upload dir riêng |
| AI reindex | không được | không được | theo quyền | mock/test |

- Không claim 403/404/500 PASS nếu chưa truy cập thật với đúng phiên/role.
- Không chạy POST nghiệp vụ lên database `beautyshop`.
- `mvn.cmd -B test` JDK 21; báo count thật; với 5 viewport, zoom 200%, keyboard Tab, axe nếu browser khả dụng.

## 8. Báo cáo và Definition of Done U4

Tạo `docs/reviews/ui-upgrade-U4-2026-11.md`: list file, mapping F-issues, test và môi trường, screenshot, regression Customer/Admin, quyết định backend cần owner duyệt.

- [ ] Nav Admin theo role, keyboard và responsive có bằng chứng.
- [ ] List/catalog/SKU/voucher/readable với dữ liệu dài và trạng thái rõ.
- [ ] VND nhất quán, form accessible, upload 2MB báo lỗi đúng hoặc có blocker ghi rõ.
- [ ] Tất cả nút hủy/xóa/thu tiền có bước xác nhận phù hợp và giữ policy server.
- [ ] Template dùng chung không hỏng nhánh Customer.
- [ ] Không tuyên bố F13/F15 hoàn tất nếu chưa có backend và test.
- [ ] Tests và browser findings đều có kết quả thật.

**Lệnh cuối prompt:** `Chỉ thực hiện U4.1; báo cáo và DỪNG.`
