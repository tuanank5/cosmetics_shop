# P12 / U2 – Nâng cấp trải nghiệm mua sắm Luxury Beauty (Storefront)

> **Cách dùng:** đặt tài liệu này tại `docs/prompts/P12-U2-luxury-storefront.md` của BeautyShop. Giao toàn bộ tài liệu cho Codex nhưng chỉ ra lệnh **`Chỉ thực hiện U2.1. Dừng sau khi báo cáo và chờ tôi duyệt.`** Sau khi nghiệm thu U2.1, lần lượt giao U2.2 → U2.3 → U2.4 → U2.5. **Không chạy cả U2 trong một lần.**
>
> **Phụ thuộc:** U1 phải hoàn thành hoặc xác minh rõ thành phần U1 nào đã tồn tại. Nếu U1 chưa xong, không tự tạo design system thứ hai; ghi blocker và chỉ thực hiện phần độc lập, an toàn.

---

## 0. Vai trò, bối cảnh và nguồn sự thật

Bạn là **Senior Frontend Engineer + UI/UX Designer + Spring MVC/Thymeleaf Engineer + QA Engineer** của dự án **BeautyShop**, website thương mại điện tử mỹ phẩm đa thương hiệu, mô hình một cửa hàng (single-store B2C).

**Project:** `E:\Nam 4 HK 1\WWW\cosmetics_shop`.

**Stack:** Java 21, Spring Boot 3.5.16, Thymeleaf, Spring Security, MariaDB, JPA/JdbcClient, Flyway, HTML/CSS/JavaScript thuần. Mốc lập kế hoạch: 22/11/2026; feature freeze dự kiến 29/11; demo/nộp 03/12/2026.

**Đọc trước khi sửa:**

1. `AGENTS.md`, `README.md` và các tài liệu trong `docs/requirements/`, `docs/design/` nếu tồn tại.
2. `docs/prompts/P12-luxury-ui-upgrade.md` (đặc tả Claude gốc: phong cách, design tokens, nút, biểu mẫu, layout, U1 và giới hạn thay đổi).
3. `docs/reviews/frontend-audit-2026-11.md` (F01–F24); `docs/reviews/ui-upgrade-2026-11.md` và `docs/ui/design-system.md` nếu U1 đã tạo.
4. Template, CSS, JS, controller, DTO, service và test **thực tế** liên quan đến giai đoạn được giao; không giả định cấu trúc repo giống tài liệu nếu mã đã thay đổi.

**Bằng chứng nền từ frontend audit 22/11:** 29 template trang, 2 fragment, 18 controller, 73 route pattern; catalog tại `localhost:8080` chưa có sản phẩm hiển thị (F01); chưa có browser QA thực tế; không suy ra database toàn hệ thống trống. Các con số chỉ là mốc tham chiếu, phải xác minh lại nếu source đã thay đổi.

**Mục tiêu U2:** trải nghiệm mua sắm từ **khám phá → chọn SKU → giỏ → đăng nhập → checkout COD → xem đơn** nhất quán, tinh tế, dễ dùng và không làm sai nghiệp vụ.

---

## 1. Phạm vi và điều cấm

### 1.1. Được phép

- Sửa các template Thymeleaf thuộc **trang chủ, catalog, chi tiết sản phẩm, giỏ, checkout, danh sách/chi tiết đơn Customer và trang kết quả MoMo**.
- Sửa CSS/JS thuần bằng cách **tái sử dụng design system U1**: tokens, base, components, button, form, badge, alert, fragment và responsive conventions.
- Chỉnh văn bản hướng dẫn bằng tiếng Việt; thêm empty state, loading state, cấu trúc ngữ nghĩa và aria phù hợp.
- Chỉ khi thực sự bắt buộc: **đề xuất** bổ sung dữ liệu view-model dạng chỉ đọc như `lineTotal`, `subtotal`, `resultCount`; trình bày file/method cần đổi, ảnh hưởng, regression test và **chờ tôi duyệt trước khi sửa Java**.
- Tạo tài liệu QA, báo cáo tiến độ, ảnh chụp bằng công cụ browser sẵn có và favicon/đồ họa CSS/SVG tự tạo nếu nằm trong phạm vi đã thống nhất.

### 1.2. Tuyệt đối không được

- Đổi logic nghiệp vụ: giá, thuế, phí vận chuyển, mã giảm giá, giữ tồn, giảm tồn, khóa giao dịch, trạng thái đơn, hủy/đổi trả, thanh toán, MoMo IPN/HMAC.
- Đổi controller route, HTTP method, tên input `name`, `th:action`, tham số nhận vào, CSRF, quyền đăng nhập hoặc kiểm sở hữu dữ liệu.
- Sửa entity, schema, Flyway migrations, seed, tài khoản database, environment/secrets, Spring Security hoặc deployment chỉ để làm UI đẹp hơn.
- Sử dụng `th:utext`, `innerHTML`, inline script/style/event handler, CDN, React/Vue/Tailwind/Bootstrap, thư viện frontend hoặc build step mới.
- Đặt hàng, xóa giỏ, thanh toán, hủy đơn, đăng nhập đoán mật khẩu hay ghi dữ liệu trong database demo/production; chỉ thực hiện test ghi sau khi **đã xác nhận database test độc lập** và được phép.
- Bịa sản phẩm, đánh giá, hình người dùng, chứng nhận, ưu đãi, phí ship, số liệu bán hàng, thương hiệu, chính sách, nội dung thành phần hoặc hình ảnh có bản quyền không được cấp phép.
- Tự `git commit`, `git push`, reset/stash thay đổi của người khác, hoặc làm tiếp giai đoạn kế tiếp khi chưa được duyệt.

**Nếu yêu cầu UI mâu thuẫn với hợp đồng backend hoặc điều cấm: DỪNG phần đó, mô tả khác biệt, lựa chọn khả thi và xin duyệt; không âm thầm sửa nghiệp vụ.**

---

## 2. Ngôn ngữ thiết kế bắt buộc – kế thừa U1

Không tạo lại hệ token hoặc hệ nút nếu U1 đã triển khai. Tái sử dụng đúng các thành phần đã có; chỉ mở rộng có kiểm soát khi cần.

- **Quiet Luxury Beauty:** nền kem `#FAF7F3`, mặt thẻ trắng, chữ nâu mực đậm, rosewood `#5E2F3E` cho hành động chính, champagne gold `#B08D57` để trang trí. Không gradient rực, không shadow dày, không carousel tự chạy.
- **Typography:** serif thanh lịch cho heading, sans-serif dễ đọc cho form/nội dung; không giả định font Cormorant/Playfair/Be Vietnam Pro đã cài. Chỉ dùng `.woff2` local có giấy phép và hỗ trợ tiếng Việt khi được xác minh; font fallback vẫn phải đẹp và đọc được dấu.
- **Nút:** `.btn` tối thiểu 48px cao, CTA `.btn--lg` tối thiểu 56px, nút nhỏ không dưới 44px, mobile CTA chính rộng 100%; đủ hover/focus/active/disabled/loading; thao tác nguy hiểm dùng `.btn--danger` và xác nhận phù hợp.
- **Form:** nhãn thực, input tối thiểu 48px, text 16px, `aria-invalid`/`aria-describedby` và error summary khi lỗi; giữ lại thông tin đã nhập nếu có thể; không phụ thuộc JavaScript để gửi form cơ bản.
- **Độ tương phản:** chữ thông thường ≥ 4,5:1; viền xác định control ≥ 3:1 trong nền thực tế; focus hiện rõ; không dùng màu làm tín hiệu duy nhất.
- **Responsive:** kiểm tra `360×800`, `390×844`, `768×1024`, `1366×768`, `1920×1080`; không cuộn ngang không chủ ý, không che CTA khi sticky; tôn trọng reduced-motion/zoom 200%.
- **Tiếng Việt/VND:** tiếng Việt có dấu; tiền tệ lấy từ dữ liệu server và formatter dùng chung, ví dụ `1.234.567 ₫`; không tính giá ở JS để quyết định giao dịch.
- **Progressive enhancement:** khi tắt JavaScript, người dùng vẫn xem, chọn SKU, cập nhật giỏ, gửi checkout đúng phương thức; JS chỉ tăng tiện dụng, không tạo nguồn chân lý thứ hai.

---

## 3. Quy trình chung trước mỗi lát cắt U2.x

1. Xác định chính xác phạm vi của **chỉ** lát cắt được giao, đọc các file liên quan, liệt kê route/form contract và các trang dùng chung Admin/Customer.
2. Chạy `git status --short`; bảo vệ mọi thay đổi sẵn có. Không tự sửa tập tin ngoài phạm vi, không ghi đè kết quả U1.
3. Kiểm tra `mvn -version` sử dụng **Java 21**; nếu PowerShell Maven đang dùng Java 17 thì chỉ đổi `JAVA_HOME`/`Path` cho phiên làm việc, không chỉnh global setting.
4. Lập baseline kiểm thử an toàn: số test PASS/FAIL/SKIPPED thật. **34 integration tests trước đây bị skipped bởi `P01_MARIADB_TEST`**; không tự bật cờ đó trên DB `beautyshop`.
5. Từng thay đổi nhỏ, giữ nguyên đường dẫn và form contract, chạy test liên quan; kiểm tra HTML thực tế ở endpoint nếu ứng dụng đang chạy và chỉ đọc.
6. Nếu browser không sẵn có, ghi **NOT VERIFIED** đối với responsive, visual quality, axe, keyboard/AT, console và Core Web Vitals; không tự tuyên bố đạt WCAG hay hoàn thiện giao diện.
7. Báo cáo kết quả cùng file/dòng, ảnh nếu có, ảnh hưởng và test còn thiếu; **dừng tại cuối lát cắt**.

---

## 4. GIAI ĐOẠN U2.1 – Trang chủ + Catalog: Khám phá sản phẩm

**Phạm vi chính:** `home/index.html`, `product/list.html`, CSS/fragment/component liên quan; controller/service chỉ **đọc** để biết model thực.

**Đối chiếu audit:** F01 (catalog rỗng), F09 (vị trí menu), F14 (ảnh/placeholder), F16 (chính sách), F20 (copy phát triển), F22 (favicon nếu U1 chưa xử lý).

### 4.1. Trang chủ

- Hero kiểu editorial, khoảng trắng rộng, heading serif rõ, mô tả ngắn bằng tiếng Việt. **Một CTA chính** “Khám phá sản phẩm”, CTA phụ “Xem danh mục” chỉ khi có trang đích thực.
- Khối danh mục/thương hiệu chỉ hiển thị **dữ liệu thực, đang hoạt động** mà controller hiện cung cấp. Không tự gắn tên nhãn hàng, logo, số lượng hay sản phẩm bán chạy nếu dữ liệu không chứng minh.
- Sản phẩm mới/nổi bật chỉ được hiển thị khi có model/query phù hợp và logic nguồn rõ ràng; nếu chưa có dữ liệu, dùng empty state trang nhã và liên kết catalog, **không dựng thẻ giả**.
- Bỏ các cụm “DỰ ÁN ĐANG PHÁT TRIỂN”, “tên thương hiệu tạm thời” trong phần storefront nếu nhóm đã thống nhất nhận diện; **không tự đặt tên thương hiệu mới**. Khi chưa chốt, dùng “Cửa hàng mỹ phẩm”/BeautyShop trung tính.
- Footer chỉ liên kết đến route/chính sách đã thực sự tồn tại; không viết cam kết hoàn tiền/giao hàng không có trong tài liệu nghiệp vụ.

### 4.2. Catalog

- Desktop: bố cục filter bên trái hoặc khu lọc tách bạch; mobile: `<details>` thu gọn, nút áp dụng rõ, nội dung bàn phím truy cập được. Chỉ dùng tiêu chí **đã được backend hỗ trợ** (keyword, danh mục, thương hiệu, loại da nếu thực tế có, sort, page).
- Hiển thị số kết quả **chỉ khi model có số chính xác**; không đánh đồng kích thước trang hiện tại với tổng số kết quả. Không tự thêm lọc khoảng giá nếu backend chưa hỗ trợ.
- Hiện các filter đang áp dụng và cơ chế bỏ filter an toàn giữ các tham số khác; nếu làm thay đổi contract controller thì báo cáo blocker trước.
- Card sản phẩm kiểu Luxury: ảnh tỉ lệ thống nhất (gợi ý 4:5), brand, tên, giá hiển thị đúng nguồn và lựa chọn SKU, tình trạng còn/hết hàng rõ; dùng placeholder CSS/SVG trung tính khi không có ảnh, không dùng ảnh tùy tiện từ internet.
- Giữ nguyên các giá trị query khi đổi trang; nhãn phân trang truy cập được, không có trang âm hoặc lặp kết quả do JS.
- Empty state phân biệt “chưa có dữ liệu” và “không tìm thấy kết quả lọc” nếu model phân biệt được, CTA về toàn bộ sản phẩm; không giấu vấn đề F01 bằng sản phẩm giả.

### 4.3. Cổng nghiệm thu U2.1

- Không có link chết hoặc chữ thông báo thử nghiệm xuất hiện không chủ ý trên home/catalog.
- GET `/` và `/products` hoạt động, giữ query phân trang/lọc; không có XSS từ từ khóa tìm kiếm.
- F01 được phân loại rõ: **đã giải quyết bằng dữ liệu thật/route đúng** hoặc **BLOCKED do môi trường**, không được đánh dấu PASS chỉ nhờ placeholder.
- Hiển thị tốt khi catalog rỗng và khi có sản phẩm thực trên **môi trường test phù hợp**; ghi rõ nếu chưa thể kiểm tra tình huống có dữ liệu.
- Dừng, cập nhật báo cáo và **không tự chạy U2.2**.

---

## 5. GIAI ĐOẠN U2.2 – Chi tiết sản phẩm: SKU, tồn kho, lựa chọn

**Phạm vi:** `product/detail.html`, JS/UI chọn biến thể và các component liên quan. **Issue chính:** F03, F14.

- Desktop hai cột: khu hình bên trái, vùng quyết định mua bên phải; cột mua hàng sticky chỉ nếu không che footer/focus. Mobile một cột, ảnh/giá/SKU và CTA có thứ tự hợp lý.
- Hiển thị brand, tên, giá server-side, tình trạng SKU, thông tin biến thể/loại da/dung tích **chỉ khi dữ liệu tồn tại**. Thành phần/cách dùng không được sáng tác; `<details>` chỉ khi có nội dung thật.
- **Bỏ tự submit khi người dùng đổi `<select>`**. Dùng nút “Xem biến thể” rõ ràng hoặc các liên kết `?variant=...` phù hợp **contract hiện có**. Điều khiển không tự chuyển trang ngoài dự kiến, giữ keyboard/focus hợp lý.
- SKU đang chọn có trạng thái rõ; URL có variant sai/hết bán có fallback thông báo tiếng Việt, không suy diễn giá hoặc lấy giá từ client.
- Nút mua `.btn--lg`: thay đổi nhãn/trạng thái theo điều kiện backend; bán hết thì không cho gửi form mua, nhưng giữ nguyên validation phía server.
- Có thể thêm chọn số lượng +/− bằng JS **chỉ nếu** POST giỏ hiện tại chấp nhận quantity; nếu contract hiện chỉ thêm 1 SKU/lần thì **không tự ý thêm field quantity không được backend hỗ trợ**. Có thể giữ CTA “Thêm vào giỏ (1)” và nêu hạn chế.
- Vùng đánh giá và sản phẩm liên quan chỉ lấy dữ liệu thực; không bịa rating/số lượt mua/đề xuất.
- Không bắt buộc sticky bottom purchase bar trên mobile nếu nó che content, lỗi viewport/zoom hoặc làm trùng hành động; chỉ triển khai khi được browser QA.

**Nghiệm thu:** chọn SKU bằng bàn phím không đổi context bất ngờ; tên/giá/tồn hiển thị khớp server; JS tắt vẫn chọn được biến thể và gửi form; sold-out/invalid variant có phản hồi; màn hình nhỏ không che CTA; **không đổi nghiệp vụ tồn kho**.

Dừng sau khi báo cáo U2.2.

---

## 6. GIAI ĐOẠN U2.3 – Giỏ hàng: Đối chiếu giá và số lượng

**Phạm vi:** `cart/index.html`, component sản phẩm/line item, CSS/JS liên quan. **Issue:** F08, F23; liên đới F10, F11.

- Card mỗi SKU: ảnh thật hoặc placeholder, tên sản phẩm có link PDP, nhãn biến thể, đơn giá, số lượng, **thành tiền từng dòng**. Sắp xếp sao cho dễ kiểm tra trên mobile.
- Khu tóm tắt: **tạm tính hàng hóa** từ backend, giải thích phí vận chuyển/mã giảm giá sẽ xác nhận tại checkout. Không hiển thị tổng cuối cùng trước khi có báo giá hợp lệ; không bịa “miễn phí vận chuyển”.
- Nếu cần thêm thuộc tính `lineTotal`/`subtotal` trong view-model, **dừng và xin duyệt Java read-only**. Không tính tổng bằng JS hay sửa kiểu tiền; tránh double và lỗi làm tròn, dùng BigDecimal/logic server hiện hành.
- Điều khiển quantity `input type=number` có label cụ thể theo SKU, nút cập nhật và hành động xóa riêng đủ kích thước; tôn trọng **quantity = 0 có nghĩa xóa** trong contract hiện tại, không âm thầm sửa API.
- Thông báo trường hợp thay đổi tồn kho/giá từ server và hành động tiếp theo; chống bấm lặp ở UI nhưng không thay khóa/transaction backend.
- Guest có thể thêm/xem giỏ; khi checkout yêu cầu đăng nhập, **giỏ phải hợp nhất đúng** theo logic đã có. Không tự chỉnh cookie, session, phân quyền hoặc merge service.
- Giỏ rỗng có CTA quay lại catalog, không đưa nút checkout không hợp lệ.

**Nghiệm thu:** dòng tiền hiển thị khớp server; chỉnh/xóa số lượng đúng; không có duplicate form hoặc field sai; Guest → đăng nhập không mất/multiply SKU; không tính ship giả; viewport 360/390 không tràn. Các thao tác ghi chỉ thử trên DB test độc lập được phép.

Dừng sau khi báo cáo U2.3.

---

## 7. GIAI ĐOẠN U2.4 – Checkout: Báo giá rõ, không tạo đơn trùng

**Phạm vi:** `order/checkout.html`, CSS/JS, các component form; controller/service chỉ đọc đến khi được duyệt. **Issue:** F07, F11, F19; liên đới F05, F10.

- Hiển thị tiến trình 3 bước: **Giỏ hàng → Giao hàng & thanh toán → Hoàn tất**; xác định bước hiện tại bằng chữ và `aria-current` phù hợp, không giả vờ đã hoàn tất thanh toán.
- Desktop hai cột: địa chỉ/phương thức bên trái, tóm tắt đơn/chi phí bên phải; mobile trình bày một cột, CTA đặt hàng rõ; không để sticky tổng tiền che nút hoặc nội dung.
- Phân biệt các số từ server: **tạm tính, giảm voucher đã được xác nhận, phí ship, tổng thanh toán**. Mọi giá trị phụ thuộc backend; không tự tính lại từ input hoặc snapshot cũ.
- Voucher có trường mã, nút **“Áp dụng”**, **“Bỏ mã”** riêng nếu khả thi với route hiện có. Preview khi POST đặt đơn lỗi phải **được tính lại hợp lệ hoặc hiển thị trạng thái “Báo giá cần tính lại”**, không hiển thị input mã còn mà mức giảm = 0 gây hiểu nhầm. **Không tự thêm endpoint/thay đổi quote logic hoặc hạn lượt voucher**; nếu backend chưa đủ dữ liệu để làm đúng, ghi blocker xin duyệt.
- COD là hành trình demo P0, MoMo chỉ hiển thị khi runtime cho phép. Cần **giữ nguyên tên/giá trị nút submit** để server phân biệt phương thức; với JS loading, không được vô tình bỏ `submitter`/`name=value` hoặc làm POST thiếu CSRF.
- Khi người dùng gửi form: hiện “Đang xử lý…”/`aria-busy` và chặn kích hoạt lặp phía UI; vẫn cần backend xử lý tính nhất quán. **Không khẳng định không tạo đơn trùng** nếu chưa test transaction/idempotency trên môi trường riêng.
- Nếu địa chỉ không hợp lệ, session hết hạn, giỏ rỗng, SKU hết hàng, voucher hết hạn, lỗi thanh toán: đưa thông báo dễ hiểu, giữ input nào an toàn và phù hợp; **không tự điều chỉnh policy**.
- Không tạo giao dịch MoMo thật; không động vào callback/IPN, chữ ký, số tiền, trạng thái thanh toán hoặc mock gateway.

**Nghiệm thu:** form có đầy đủ nhãn lỗi/CSRF/route nguyên vẹn; total minh bạch; không stale voucher preview; COD hiển thị đúng ý nghĩa; MoMo mock có nhãn; máy mạng chậm có loading state; Guest bị điều hướng login theo cơ chế hiện tại. Test đặt đơn **chỉ trên DB test đã xác nhận**.

Dừng sau khi báo cáo U2.4.

---

## 8. GIAI ĐOẠN U2.5 – Đơn hàng và kết quả thanh toán

**Phạm vi:** `order/list.html`, `order/detail.html`, `order/momo-return.html`, `order/momo-mock.html` và phần CSS/UI tương ứng.

**CẢNH BÁO:** `order/list.html` và `order/detail.html` có thể được dùng chung bởi **Customer và Admin**. Không sửa layout hay điều kiện theo vai trò mà làm hỏng nhánh Admin. Nếu cần refactor lớn, xin duyệt trước.

- Customer list: card đơn có mã, thời điểm đặt (chỉ khi model hỗ trợ), trạng thái đơn, tổng tiền, CTA chi tiết, empty state có lối quay shop.
- Customer detail: phân biệt **trạng thái đơn**, **trạng thái thanh toán**, **tiến trình giao hàng**; không suy diễn mốc timeline từ trạng thái chưa có; hủy đơn có bước xác nhận phù hợp với các hạn chế progressive enhancement.
- Khi hiển thị Admin cùng template, giữ nguyên URL hành động Admin/Customer, quyền, hidden field, điều kiện hiển thị, CSRF và các trạng thái chuyển hợp lệ.
- MoMo return: thông điệp trung lập “Đã quay về cửa hàng; kết quả thanh toán cần được xác nhận từ hệ thống”, không nói “đã thanh toán” chỉ vì browser redirect.
- Mock MoMo: nhãn **“MÔ PHỎNG – KHÔNG THU TIỀN THẬT”**, các lựa chọn kịch bản giữ chính xác ý nghĩa; không gửi mock/IPN/đối soát trên database đang dùng.
- Đồng nhất nhãn, nút và card với U1/U2. Không thêm logic đổi trả/hủy/thu tiền.

**Nghiệm thu:** Customer và Admin render cùng template đúng quyền, không lộ hành động sai vai trò; trạng thái không đánh lừa người dùng; các nhánh thanh toán chỉ được kiểm thử động trên môi trường mock/test an toàn.

Dừng sau khi báo cáo U2.5; **không tự động bắt đầu U3**.

---

## 9. QA bắt buộc cuối mỗi U2.x

### 9.1. Build và regression

- Xác minh Java 21 thực tế trong `mvn.cmd -version`. Chạy `mvn.cmd -B test` khi môi trường an toàn; báo nguyên văn số `Tests run / Failures / Errors / Skipped`, không đánh đồng `BUILD SUCCESS` với 100% test đã chạy.
- Kiểm tra các route và HTML resource không bị thiếu; CSRF token vẫn được render đối với form POST; không đổi `name`, `method`, `th:action`.
- Không bật `P01_MARIADB_TEST=true` trước khi xác minh JDBC URL trỏ `beautyshop_test`, quyền và dữ liệu test độc lập; chạy test ghi hoặc đặt hàng chỉ khi được cho phép.
- Kiểm tra nguồn HTML không có `th:utext`, `innerHTML`, inline `<script>`, inline `onclick`, CDN; phân biệt ngoại lệ có căn cứ nếu source đã tồn tại.

### 9.2. Browser / UX / accessibility

Chạy ở cả 5 viewport: `360×800`, `390×844`, `768×1024`, `1366×768`, `1920×1080` khi browser khả dụng.

- Không horizontal overflow, không clip nội dung/đè CTA, keyboard Tab/Shift+Tab sử dụng được, zoom 200%, focus-ring và skip link hiện rõ.
- Kiểm tra product có nhiều SKU, sold-out, lỗi form, giỏ rỗng/giỏ có sản phẩm, đăng nhập và sau đăng nhập, voucher hợp lệ/sai/hết hạn (trên test DB).
- Chạy axe-core/Lighthouse nếu công cụ đã có; chỉ ghi chỉ số **đã đo thật**, không tự chế báo cáo. Không có browser thì ghi `NOT VERIFIED`, tạo checklist thao tác thủ công.
- Nếu có screenshot: đặt trong `docs/ui/screenshots/U2.x/`, ghi URL, vai trò, kích thước và môi trường. Không dùng ảnh từ báo cáo cũ làm chứng cứ của đợt này.

### 9.3. Ma trận trọng tâm

| Luồng | Không đăng nhập | Customer | Admin | Có dữ liệu test | Không có dữ liệu |
|---|---|---|---|---|---|
| Trang chủ/catalog | Đọc được | Đọc được | Đọc được | Thẻ/giá/filter đúng | Empty state hữu ích |
| PDP và SKU | Chọn và thêm giỏ | Chọn và thêm giỏ | Không làm hỏng view | Giá/tồn đúng | 404/sold-out rõ |
| Giỏ | Session Guest | Giỏ hợp nhất | Giữ quyền đã có | Tổng/quantity đúng | CTA quay lại |
| Checkout COD | Đến login | Đặt đơn test | Không lộ thao tác Customer | Báo giá đúng | Hướng dẫn giỏ/địa chỉ |
| Đơn hàng/MoMo | Theo quyền | Đơn của chính mình | View Admin được bảo toàn | Trạng thái thực | Empty/error hợp lý |

Các cột là **điều kiện cần test**, không phải trạng thái PASS mặc định.

---

## 10. Tài liệu báo cáo và quy tắc kết thúc

Tạo/cập nhật `docs/reviews/ui-upgrade-U2-2026-11.md` sau **mỗi lát cắt** với:

1. Lát cắt đã làm và file thêm/sửa (template, CSS, JS, Java nếu được duyệt, docs).
2. Bảng vấn đề F01–F24 liên quan: **Đã xử lý / Một phần / Chưa xử lý / Blocked / Ngoài phạm vi**, bằng chứng `S` (source), `H` (HTTP), `B` (browser).
3. Contract bảo toàn: URL, method, `name`, Thymeleaf binding, CSRF, quyền, giá/tồn/voucher/ship/thanh toán.
4. Test thật: lệnh, JDK, kết quả PASS/FAIL/SKIPPED; browser, viewport, screenshot, accessibility đã/không kiểm chứng.
5. Rủi ro, quyết định cần owner duyệt, test còn thiếu và cách rollback từng thay đổi; không che giấu blocker F01.
6. Điều kiện nghiệm thu và **kết luận có thể sang U2.x tiếp theo hay chưa**.

### Definition of Done toàn U2 (chỉ đánh dấu khi có bằng chứng)

- [ ] Home và catalog hiển thị dữ liệu thực hoặc xác định rõ blocker F01; không có sản phẩm/chứng nhận giả.
- [ ] Card sản phẩm, lọc, sort, phân trang và empty state dễ sử dụng.
- [ ] PDP đổi SKU không tự reload bất ngờ, giá/tồn đúng backend.
- [ ] Guest có thể duyệt → thêm giỏ → đăng nhập và giữ/merge giỏ theo nghiệp vụ có sẵn.
- [ ] Giỏ có line total + subtotal chính xác từ server, thao tác quantity/xóa rõ ràng.
- [ ] Checkout COD có phân rã phí/minh bạch voucher; lỗi không hiển thị báo giá sai hoặc stale.
- [ ] Loading/submit không làm mất tên phương thức thanh toán, CSRF hoặc request contract.
- [ ] Trang đơn và MoMo return/mock không báo nhầm “thanh toán thành công”.
- [ ] Template đơn dùng chung Customer/Admin không bị hồi quy.
- [ ] Trải nghiệm tiếng Việt, nút đủ lớn, focus/contrast và 5 viewport được **kiểm chứng** hoặc ghi rõ `NOT VERIFIED`.
- [ ] Test được báo đầy đủ, không dùng database demo/production để thử ghi, không đổi nghiệp vụ.
- [ ] Không tự commit/push, không bắt đầu U3 trước khi nghiệm thu U2.

---

**Lệnh bắt đầu trong Codex:**

> Đọc `docs/prompts/P12-luxury-ui-upgrade.md`, `docs/reviews/frontend-audit-2026-11.md`, tài liệu U2 này và trạng thái source hiện tại. **Chỉ làm GIAI ĐOẠN U2.1 – Trang chủ và Catalog.** Thực hiện từng thay đổi nhỏ, kiểm tra và báo cáo. **Dừng lại, không làm U2.2–U2.5 và không commit/push.**
