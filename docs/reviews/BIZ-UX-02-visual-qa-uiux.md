# BIZ-UX-02 · Visual QA và hoàn thiện UI/UX BeautyShop

Baseline là `main` tại `daea756053021310d9fd1c6ada9475564711fac6` cộng 15 tệp tracked thay đổi từ BIZ-UX-01. Index trống. Bản đóng gói baseline lấy từ working tree lúc tiếp nhận, để phép so sánh giữ các sửa lỗi nghiệp vụ của BIZ-UX-01. Build, Maven cache, datadir SQL/Mongo và uploads dùng vùng riêng trong `target/biz-ux-02/`. Windows 11 amd64, Temurin 21.0.13+9, Maven 3.9.11 và Node 26.13.0. Đã chọn JDK 21 cho Maven theo tiến trình vì `JAVA_HOME` kế thừa trỏ đến JDK 17. Fixture dùng MariaDB 12.2.2 và MongoDB 6.0 trên cổng loopback riêng.

## A · Trình duyệt

| Phép thử | Kết quả |
|---|---|
| Browser tích hợp | **BLOCKED**. Kết nối báo không có browser, discovery trả danh sách rỗng. |
| Chrome 158.0.8127.37 và Edge 158.0.4370.33 với profile mới, DevTools port tự cấp, HTML tối giản qua HTTP loopback | **PASS** `Browser.getVersion`, liệt kê target và mở WebSocket tab. **BLOCKED** ở `Page.enable`. Cả hai ghi nhận GPU process crash lặp và lỗi mở cache 0x20. [Chrome](../qa/BIZ-UX-02/chrome-probe.json) · [Edge](../qa/BIZ-UX-02/edge-probe.json) |
| Chrome với các tùy chọn kết xuất chẩn đoán | **BLOCKED**. Lượt thử timeout ở `Browser.getVersion`. Các cơ chế bảo mật của browser vẫn bật. [Log](../qa/BIZ-UX-02/chrome-cpu-probe.json) |
| Firefox headless và profile mới | **BLOCKED**. Gecko báo `SpawnTarget` mã `-2147024809`, tab renderer không được tạo và không sinh ảnh. [stderr](../qa/BIZ-UX-02/firefox-probe-stderr.log) |
| Điều hướng tới BeautyShop trong browser, DOM tab, click, nhập liệu, ảnh, console/network trang | **NOT RUN** sau các blocker trên. HTTP trực tiếp đã chạy trên BeautyShop thật trong fixture. |

Dữ liệu cho thấy lỗi xảy ra ngay với trang HTML đơn giản, trước khi tải BeautyShop và sau khi transport CDP của tiến trình chính đã hoạt động. Chưa xác định nguyên nhân hệ thống chính xác của việc subprocess không render được. Kiểm kê PATH và bộ cài sẵn không thấy WebDriver hoặc gói Playwright có thể gọi từ dự án. Cache browser có Firefox/WebKit. CDP trực tiếp đã kiểm tra được kết nối mà không cần cài driver.

## B–D · Phát hiện, sửa và kiểm chứng

| ID | Mức | Trước và nguyên nhân | Sau và bằng chứng |
|---|---|---|---|
| UX02-01 · Home carousel | P2 | Predicate ở `home-carousel.js` chỉ yêu cầu rộng từ 320 px, cao từ 560 px và có stylesheet. Ví dụ 390×844, chạm được phép khởi tạo Swiper nếu không yêu cầu giảm chuyển động. Không đồng nghĩa Swiper đã hoạt động trên thiết bị thật, vì còn kiểm chiều cao từng chương sau init. | Chỉ thử khởi tạo khi từ 901 px, cao từ 560 px và thiết bị chính có hover/con trỏ chính xác. Media-query change gọi lại cơ chế chuyển chế độ. Giữ nội dung cuộn tự nhiên và link HTML ở các nhánh fallback. **PASS** 12 ca predicate, **BLOCKED** cho tương tác thật. [Bằng chứng trước/sau](../qa/BIZ-UX-02/source-check.json) |
| UX02-02 · Tư vấn da | P2 | Chương tư vấn Home chỉ có CTA cho `/chat`. Đường tư vấn theo loại da nằm trong điều hướng chung. Sau khi gợi ý, nội dung là một đoạn văn bản với tên SKU, khách phải tìm lại bằng catalog. `TuVanDaService` đã có danh sách biến thể được chọn, nhưng API nội bộ trả về chỉ `String`. | Home có hai CTA với nhãn và đích riêng. Service trả nội dung và danh sách link từ slug/mã SKU đã xác minh trong catalog. Thymeleaf tạo link vào đúng PDP và biến thể, không dùng URL do model sinh. Khi danh sách rỗng, giao diện chỉ hiển thị thông báo. **PASS** HTTP cho hai link Home, service test với phản hồi model mô phỏng, MVC cho URL kết quả và HTTP cho provider tắt. [HTTP trước](../qa/BIZ-UX-02/before-http.json) · [HTTP sau](../qa/BIZ-UX-02/after-http.json) · [Maven](../qa/BIZ-UX-02/maven-test.log) |
| UX02-03 · Lỗi form SKU Admin | P2 | POST multipart với giá âm trả lại form và thông báo lỗi, nhưng ô giá không có `aria-invalid` hoặc tham chiếu thông báo. Các trường mã, tên, số lượng, lô và hạn dùng cùng mẫu. | Nối sáu trường với lỗi riêng và chỉ đặt `aria-invalid` khi server báo lỗi. Giữ form multipart, dữ liệu nhập được render lại và quy tắc validate. **PASS** HTTP trước/sau trên ô giá âm. [Trước](../qa/BIZ-UX-02/before-http.json) · [Sau](../qa/BIZ-UX-02/after-http.json) |
| UX02-04 · Focus kỳ thống kê Admin | P2 | Viền inset dùng màu `--color-primary` #5e2f3e bên trong badge kỳ đang chọn nền #7a5f33. Tỷ lệ màu kề nhau là **1,80:1** theo CSS. | Kỳ đang chọn dùng `currentColor` trắng cho viền focus, đạt **5,98:1** theo cùng phép tính. Phần còn lại của hệ màu Admin giữ nguyên. **PASS** phép tính màu, **BLOCKED** quan sát bằng bàn phím. [Phép tính](../qa/BIZ-UX-02/source-check.json) · [WCAG 2.2 SC 1.4.11](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html) |

### Kiểm kê màn và trạng thái

| Màn | Cấu trúc và hành trình từ source/SSR | Phần còn mở |
|---|---|---|
| Home, header, footer | Sáu chương, heading và link HTML, menu disclosure, search gắn nhãn, footer theo vai. Tối ưu điều kiện Swiper. Cả hai đường tư vấn ở chương 6. | Hiển thị từng phần, chữ trên media, GSAP, chuột/chạm, resize và bàn phím. |
| Catalog, PDP | Danh sách có filter và sort GET, paginator dùng fragment, mã biến thể và tồn trong khu giá. HTTP có catalog lọc, SKU còn/hết, thêm vào giỏ. | Grid, ảnh lỗi, chuỗi lọc dài, chuyển trang, keyboard trên SKU và phối hợp CSS fallback. |
| Wishlist, cart | Danh sách wishlist trạng thái hiện hành. Giỏ giữ SKU, cập nhật server-side, vùng phản hồi và tạm tính từ BIZ-UX-01. | Hai tab, focus thật khi timeout và native dialog xác nhận xóa. |
| Checkout và kết quả thanh toán | Thứ tự địa chỉ → mã giảm giá → tổng tiền → phương thức. Mã nhập giữ khi quote lỗi. HTTP COD dùng thông tin và giá do server đọc. Trang kết quả MoMo và chế độ mock có template riêng. | Layout tổng kết trên màn nhỏ. MoMo Sandbox/provider thật và màn kết quả trong browser. |
| Lịch sử, chi tiết đơn, đổi trả, đánh giá | Màn danh sách và chi tiết có trạng thái, vùng hành động và tách đơn/đổi trả. HTTP mở lịch sử, chi tiết và danh sách đổi trả. Tích hợp review/đổi trả có fixture mua hàng. | Trình bày review và đổi trả với dữ liệu nhiều dòng và trường hợp kho review lỗi trên browser. |
| Login, đăng ký, OTP, quên mật khẩu và reset | Form có label và gợi ý trường ở các màn chính. HTTP đăng nhập Guest → Customer và Admin, render đăng ký và quên mật khẩu. Template OTP/reset có trong code. | OTP thực, SMTP, Redis, OAuth, mật khẩu/keyboard và Back trong browser. |
| Hồ sơ, sổ địa chỉ, bảo mật | HTTP vào hồ sơ, địa chỉ và bảo mật. Lưu địa chỉ mới trả về checkout. | Trạng thái lỗi trường và vỡ bố cục với địa chỉ dài trong browser. |
| Chat, tư vấn da, lỗi và empty | SSR mở hai route riêng. Tư vấn da có lỗi dịch vụ và giữ mô tả. HTTP kiểm 403/404 và các trang rỗng từ fixture. Có template 400/409/413/429/500 ngoài các trạng thái đã chạy HTTP. | Lỗi dịch vụ chậm, tính rõ khi nhập lại và thông báo assistive tech. Các mã lỗi khác và reset chưa chạy qua HTTP của task. |
| Admin dashboard và tồn | Heading, bộ chọn kỳ, bảng SSR theo khu, vùng cuộn có tên và tabindex. Tồn hiện tại hiển thị ở dashboard và danh sách/quản trị SKU. | Chart.js, độ rộng bảng, nút lọc, nội dung có/không dữ liệu và focus thật. |
| Admin sản phẩm, SKU, danh mục và thương hiệu | Danh sách tách CRUD và trạng thái, form SKU có lỗi gắn trường. Kiểm HTTP danh sách và form SKU. | Tương tác upload file, tên dài, bố cục và tab order thực tế của form. |
| Admin đơn, đổi trả, voucher, tài khoản, đánh giá và AI | HTTP vào các nhóm danh sách, dashboard và chi tiết/xác nhận đơn. Bảng ưu tiên trạng thái và hành động hiện có, các form dùng component chung. | Kiểm trực quan form sửa, pagination, kiểm duyệt và thao tác khi dịch vụ phụ thuộc không khả dụng. |

Storefront tiếp tục dùng token charcoal, chữ sáng và muted gold. Bản sửa dùng lại `button-group`, button primary/secondary, `text-link`, palette và component Admin. Không có framework hoặc runtime dependency mới. Khoảng cách, radius, container, typography, badge, modal, toast, bảng và pagination hiện có được giữ trong phạm vi các lỗi được chứng minh.

## E–F · Ma trận responsive, accessibility và hồi quy

| Rộng CSS px | Điều kiện cho Swiper trước/sau khi chạy predicate | Layout, tràn ngang và clipping thật |
|---|---|---|
| 320 | Có → Không | BLOCKED |
| 360 | Có → Không | BLOCKED |
| 390 | Có → Không | BLOCKED |
| 768 | Có → Không | BLOCKED |
| 1024 | Có → Có với chuột/hover | BLOCKED |
| 1280 | Có → Có với chuột/hover | BLOCKED |
| 1440 | Có → Có với chuột/hover | BLOCKED |
| 1920 | Có → Có với chuột/hover | BLOCKED |

Predicate chạy với 844 px chiều cao. Ca bổ sung có desktop thấp 540 px, thiết bị chạm rộng, reduced motion và thiếu stylesheet. Tất cả về nhánh fallback sau sửa. Zoom browser 200%, phím Tab/Escape, Enter, screen reader, clipping, thao tác chart và Swiper/GSAP trong browser đều **BLOCKED**. Các độ rộng trên chỉ xác nhận điều kiện JavaScript, chưa phải nghiệm thu theo [WCAG 2.2 Reflow](https://www.w3.org/WAI/WCAG22/Understanding/reflow.html).

| Kiểm tra | Kết quả và mức độ |
|---|---|
| Heading, skip target, ID trùng, label `for`, tham chiếu `aria-describedby`/`aria-labelledby` | PASS với quét markup HTTP trên các GET 200 thuộc fixture trước và sau. Đây là kiểm tra cấu trúc, chưa kiểm accessible name tính từ cây accessibility. |
| Tương phản focus, lỗi gắn trường | PASS có giới hạn ở phép tính CSS kỳ đang chọn và markup lỗi ô giá SKU. Contrast toàn bộ trạng thái và focus thực tế BLOCKED. |
| Không JS | PASS hành trình request/form SSR đã chạy qua HTTP. Không kiểm browser với JS bị tắt. |
| Giỏ BIZ-UX-01 | PASS khi chạy lại DOM/transport mô phỏng cho liên tiếp hai SKU, cùng SKU, phản hồi mất/treo, tab khác, badge/tạm tính và không tự gửi lại POST. Real browser, focus/Escape khi xóa 0 và hai tab thật BLOCKED. [VM](../qa/BIZ-UX-02/cart-vm-after.json) |
| Checkout, COD và tồn | PASS HTTP cho mã giữ khi lỗi, quote 165.000 ₫ và hai POST đồng thời trả `200`/`302`. MVC bao gồm báo giá khi SKU thiếu tồn. Integration kiểm khấu trừ kho, phân vai và khóa giao dịch theo fixture. |
| Hai đường tư vấn | PASS SSR, MVC và service tests. Provider thật SKIPPED. |

## G · Hiệu năng đo được

Đo body HTML và script/CSS khai báo bằng HTTP với cùng fixture và hai JAR cô lập. Mỗi đường có 7 mẫu thời gian đến headers sau khi mở một lần. Số thẻ mở lấy từ HTML, chưa phải số node DOM đã dựng. Tất cả script/CSS trong phép đo trả 200, không có đường dẫn khai báo lặp. [Trước](../qa/BIZ-UX-02/before-http.json) · [Sau](../qa/BIZ-UX-02/after-http.json)

| Màn | Thẻ script/CSS | Byte body script/CSS trước → sau | Byte HTML trước → sau | Số thẻ mở trước → sau | Median headers ms trước → sau |
|---|---:|---:|---:|---:|---:|
| Home | 14 | 366.394 → 366.648 | 51.427 → 51.577 | 401 → 403 | 57,7 → 55,9 |
| Catalog | 8 | 106.818 → 106.904 | 13.567 → 13.567 | 191 → 191 | 53,5 → 39,7 |
| Admin dashboard | 8 | 313.736 → 313.822 | 38.950 → 38.950 | 675 → 675 | 65,3 → 36,2 |

Chênh lệch thời gian phản hồi là quan sát từ hai lượt trên máy local, chưa đủ kết luận hiệu năng tăng. Điều kiện mới giảm số viewport được phép khởi tạo Swiper, nhưng tài nguyên thư viện Swiper vẫn được khai báo và tải như trước. CSS coverage, LCP, CLS, độ trễ tương tác, ảnh/font lúc trình duyệt tải và JS console/network trong tab đều **BLOCKED**.

## H · Kiểm thử và bàn giao

- Maven isolated `-B -o -ntp test` chạy 318 ca, **242 đạt, 76 SKIPPED, 0 fail/error**. `package -DskipTests` và `git diff --check` đạt. [Test](../qa/BIZ-UX-02/maven-test.log) · [Package](../qa/BIZ-UX-02/maven-package.log)
- Integration MariaDB/MongoDB **22/22 đạt**. Các bộ chạy gồm catalog, SKU, COD, dashboard, đánh giá sau mua và số SELECT trong giỏ Guest. [Log](../qa/BIZ-UX-02/maven-integration.log)
- HTTP trước/sau **PASS 35 lượt kiểm route mỗi bên**, gồm 32 trả 200, một redirect và hai lỗi quyền/route theo kỳ vọng. Hành trình đi Guest → Catalog lọc → chọn SKU → giỏ → đăng nhập/gộp → tạo địa chỉ → voucher → quote → COD → chi tiết → Admin xác nhận. Chạy trong Tomcat dev và datadir riêng. Lượt baseline đầu vấp assertion vì checkout chưa có địa chỉ nên chưa render danh sách, harness đã kiểm SKU từ giỏ vừa gộp rồi tiếp tục qua bước thêm địa chỉ. [Lượt đầu](../qa/BIZ-UX-02/before-http-first.json)
- Ảnh thật **BLOCKED**. Hai thư mục `docs/qa/BIZ-UX-02/screenshots/before/` và `after/` đang chờ lượt capture có renderer. Chưa có ảnh để quan sát hoặc kết luận về hierarchy, spacing hay clipping trực quan.
- OAuth, Redis/SMTP, MoMo Sandbox và Ollama thật **SKIPPED**. Test tư vấn dùng model giả lập và HTTP provider tắt. Rủi ro chưa giải quyết là hành vi trình duyệt, dữ liệu review/provider thực, chức năng chưa có fixture toàn trạng thái và bố cục ở zoom 200%.

Các tệp source của task gồm `TuVanDaService.java`, `TuVanDa_Controller.java`, `templates/chat/TuVanDa.html`, `templates/home/index.html`, `templates/admin/variant/form.html`, `static/js/home-carousel.js`, `static/css/admin.css`, `TuVanDaServiceTest.java` và `TuVanDaControllerTest.java`. Hạ tầng kiểm thử gồm `docs/qa/BIZ-UX-02/`, báo cáo này, checkpoint và mục BIZ-UX-02 trong roadmap. [Chạy lại](BIZ-UX-02-tiep-tuc.md)
