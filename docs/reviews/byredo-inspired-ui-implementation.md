# P15 · Báo cáo triển khai giao diện BeautyShop

**Hiện tại sau B4 · 24/30 PASS (80%), core 12/12 trong phạm vi UI/MVC/fixture.** C27 đạt sau sửa F07; B2/GSAP và các PASS trước giữ nguyên. Maven JDK21: **118 PASS / 0 FAIL / 0 ERROR / 34 SKIPPED**. **F01/MariaDB còn BLOCKED**, chưa tuyên bố production-ready. [Bằng chứng B4](#b4). Các phần B0–B2 bên dưới giữ lịch sử kiểm chứng; B3 chưa triển khai.

Ngày 22/11/2026, **13/30 thành phần PASS** (**43,33%**), 13 PARTIAL, 4 FAIL. Có **8/12 thành phần cốt lõi PASS**. Để đạt 80%, bốn mục cốt lõi còn thiếu C01, C02, C15, C27 và ít nhất bảy mục khác phải đạt. Điểm này đo bằng chứng về chức năng giao diện. Cổng nghiệm thu demo trên MariaDB còn mở. [Ma trận 30 mục, nguồn từng dòng và kế hoạch](byredo-component-gap-analysis.md) · [Phân tích tham chiếu](byredo-reference-analysis.md)

## Nguồn và phạm vi

- Working tree thực tế là nhánh `main`, HEAD `837e550`. Trước audit, `git status --porcelain=v1 -uall` có 36 file đã sửa, 3 file đã xóa và 86 file chưa được theo dõi. Trong đó có hệ thống U1, trang U2 và phần đọc dữ liệu giỏ. Đã lấy dấu SHA-256 của `src/` và `pom.xml` và đã đối chiếu đủ 185 file cuối lần chạy. Tất cả hash khớp.
- Đã đọc AGENTS, README, SRS, business rules, ERD, decisions, P12, prompt U2, bộ tài liệu U3–U8, audit F01–F24, báo cáo U1/U2 và design system. Đường dẫn `docs/prompts/P12-luxury-ui-upgrade.md` được yêu cầu không tồn tại trong checkout. Bản cùng tên nằm ở gốc repo. [P12 thực tế](../../P12-luxury-ui-upgrade.md) · [Prompt U2](../prompts/P12-U2-luxury-storefront.md) · [Roadmap U3–U8](../prompts/README-roadmap-U3-U8.md)
- Đã rà cả 41 template HTML, 5 stylesheet, 1 tệp JS, controller, DTO, repository và service liên quan đến catalog, SKU, giỏ, yêu thích và checkout. U3–U8 cung cấp yêu cầu và cổng kiểm thử. Kết quả triển khai được chấm theo source, test và ảnh hiện có. [Audit ban đầu](frontend-audit-2026-11.md) · [U1](ui-upgrade-2026-11.md) · [U2](ui-upgrade-U2-2026-11.md)

## Kiến trúc và hợp đồng cần bảo toàn

| Phần | Dữ liệu và điểm dùng lại |
|---|---|
| Khung giao diện | MVC render Thymeleaf trên server. `head` nạp tokens → base → components → storefront → admin và `app.js` defer. Header/footer dùng chung cả storefront và Admin. `GiaoDien_Advice` cung cấp path và tổng đơn vị giỏ theo vai trò. [Head L8](../../src/main/resources/templates/fragments/head.html#L8) · [Advice L23](../../src/main/java/com/beautyshop/controller/GiaoDien_Advice.java#L23) |
| Catalog | GET `/products` nhận `q,brand,category,skinType,sort,page,size`. `Page<TheSanPham>` có tổng đếm DISTINCT, ảnh đại diện và tồn tổng hợp theo sản phẩm. Thứ tự do server quyết định. [Controller L25](../../src/main/java/com/beautyshop/controller/CuaHang_Controller.java#L25) · [Repository L29](../../src/main/java/com/beautyshop/repository/CuaHang_Repository.java#L29) |
| PDP và wishlist | GET `/products/{slug}?variant={id}` chỉ chọn SKU công khai thuộc sản phẩm. Wishlist dùng mã sản phẩm, chỉ Customer được lưu/xóa qua POST theo session. [Chọn SKU L49](../../src/main/java/com/beautyshop/service/CuaHang_Service.java#L49) · [Wishlist L99](../../src/main/java/com/beautyshop/controller/SauMua_Controller.java#L99) |
| Giỏ và thanh toán | POST `/cart/items` nhận `variant`. POST `/cart/items/{variant}/quantity` nhận `quantity`. Guest dùng session, Customer đọc giỏ theo chủ sở hữu. Hai POST checkout giữ nguyên `addressId`, `voucher`, phương thức `payment` khi chọn MoMo và CSRF. Server tính `BigDecimal`, phí và giảm giá. [Giỏ L38](../../src/main/java/com/beautyshop/controller/GioHang_Controller.java#L38) · [Checkout L39](../../src/main/java/com/beautyshop/controller/Checkout_Controller.java#L39) · [Security L35](../../src/main/java/com/beautyshop/security/Security_Config.java#L35) |

## 30 thành phần trước và sau B0

Cột “Cốt lõi” đánh giá riêng điều kiện bắt buộc của P15. B0 chỉ thu bằng chứng nên hai cột trạng thái trùng nhau. C21 vẫn nằm trong tổng số 30 vì chưa có video được cấp quyền và chưa có quyết định N/A của owner.

| ID | Thành phần | Trước B0 | Sau B0 | Cốt lõi | Điểm đọc mã |
|---|---|---|---|---|---|
| C01 | Header tối giản | PARTIAL | PARTIAL | Chưa đạt | [header.html L4](../../src/main/resources/templates/fragments/header.html#L4) |
| C02 | Menu nhiều cấp | PARTIAL | PARTIAL | Chưa đạt | [header.html L14](../../src/main/resources/templates/fragments/header.html#L14) |
| C03 | Tìm kiếm | PASS | PASS | Đạt | [header.html L9](../../src/main/resources/templates/fragments/header.html#L9) |
| C04 | Lối vào tài khoản theo vai trò | PASS | PASS | — | [header.html L23](../../src/main/resources/templates/fragments/header.html#L23) |
| C05 | Lối vào wishlist | PARTIAL | PARTIAL | — | [account.html L20](../../src/main/resources/templates/auth/account.html#L20) |
| C06 | Chỉ báo giỏ | PARTIAL | PARTIAL | — | [header.html L20](../../src/main/resources/templates/fragments/header.html#L20) |
| C07 | Trạng thái điều hướng | PARTIAL | PARTIAL | — | [header.html L5](../../src/main/resources/templates/fragments/header.html#L5) |
| C08 | Header responsive | PARTIAL | PARTIAL | — | [storefront.css L100](../../src/main/resources/static/css/storefront.css#L100) |
| C09 | Tiêu đề bộ sưu tập | PASS | PASS | Đạt | [list.html L8](../../src/main/resources/templates/product/list.html#L8) |
| C10 | Số kết quả | PASS | PASS | — | [list.html L55](../../src/main/resources/templates/product/list.html#L55) |
| C11 | Điều khiển bộ lọc | PASS | PASS | Đạt | [list.html L14](../../src/main/resources/templates/product/list.html#L14) |
| C12 | Chip và số bộ lọc | PARTIAL | PARTIAL | — | [list.html L47](../../src/main/resources/templates/product/list.html#L47) |
| C13 | Chuyển mật độ lưới | FAIL | FAIL | — | [list.html L98](../../src/main/resources/templates/product/list.html#L98) |
| C14 | Lưới sản phẩm | PASS | PASS | Đạt | [list.html L98](../../src/main/resources/templates/product/list.html#L98) |
| C15 | Ảnh sản phẩm trong catalog | PARTIAL | PARTIAL | Chưa đạt | [product-card.html L5](../../src/main/resources/templates/fragments/product-card.html#L5) |
| C16 | Nhãn tồn kho | PASS | PASS | — | [product-card.html L13](../../src/main/resources/templates/fragments/product-card.html#L13) |
| C17 | Wishlist / quick add trên card | FAIL | FAIL | — | [product-card.html L4](../../src/main/resources/templates/fragments/product-card.html#L4) |
| C18 | Duyệt nhiều trang | PARTIAL | PARTIAL | — | [list.html L103](../../src/main/resources/templates/product/list.html#L103) |
| C19 | Hero biên tập | PASS | PASS | — | [index.html L7](../../src/main/resources/templates/home/index.html#L7) |
| C20 | Nội dung xen trong catalog | FAIL | FAIL | — | [list.html L98](../../src/main/resources/templates/product/list.html#L98) |
| C21 | Video / media điện ảnh | FAIL | FAIL | — | [index.html L17](../../src/main/resources/templates/home/index.html#L17) |
| C22 | Thẻ dịch vụ / tư vấn | PASS | PASS | — | [index.html L41](../../src/main/resources/templates/home/index.html#L41) |
| C23 | PDP cao cấp | PASS | PASS | Đạt | [detail.html L9](../../src/main/resources/templates/product/detail.html#L9) |
| C24 | Chọn biến thể | PASS | PASS | Đạt | [detail.html L27](../../src/main/resources/templates/product/detail.html#L27) |
| C25 | Thêm vào giỏ | PASS | PASS | Đạt | [detail.html L44](../../src/main/resources/templates/product/detail.html#L44) |
| C26 | Giỏ hàng | PASS | PASS | Đạt | [index.html L25](../../src/main/resources/templates/cart/index.html#L25) |
| C27 | Checkout | PARTIAL | PARTIAL | Chưa đạt | [checkout.html L10](../../src/main/resources/templates/order/checkout.html#L10) |
| C28 | Footer nhiều cột | PARTIAL | PARTIAL | — | [footer.html L6](../../src/main/resources/templates/fragments/footer.html#L6) |
| C29 | Thông tin hỗ trợ | PARTIAL | PARTIAL | — | [footer.html L8](../../src/main/resources/templates/fragments/footer.html#L8) |
| C30 | Breadcrumb và trạng thái cuối | PARTIAL | PARTIAL | — | [detail.html L7](../../src/main/resources/templates/product/detail.html#L7) |

## Kiểm thử và bằng chứng

| Lớp kiểm chứng | Kết quả thực tế trong B0 |
|---|---|
| Static | Đối chiếu template, CSS, JS với Spring MVC, chính sách role, form, projection và báo cáo cũ. `git diff --check` đạt. |
| Maven/JDK 21 | Maven 3.9.11, Temurin 21.0.13. Suite cuối liệt kê **77 = 43 PASS + 0 FAIL + 0 ERROR + 34 SKIPPED**. Trong đó có MockMvc render template, các test nghiệp vụ độc lập và hai SELECT giỏ trên H2. BootstrapPagesTest, GiaoDienPagesTest, GioHangPagesTest là các lớp liên quan trực tiếp. Lệnh chạy lại bên dưới. |
| HTTP demo | GET tới `http://localhost:8080/products` và các trang public không kết nối được trong B0. Lần U2 gần nhất trả 200 và 0 thẻ sản phẩm. B0 không có kết quả HTTP 200 mới cho demo. **F01 BLOCKED**. |
| Browser BeautyShop | Chrome Headless 158.0.8127.36, Windows, 1366×768 và 390×844, zoom mặc định 100%. Tomcat loopback render template/asset trực tiếp từ working tree. DataSource, JPA, Flyway bị loại khỏi fixture. Ba card dùng tên/giá từ hồ sơ nghiên cứu R026/R002/R003, số lượng tồn là kịch bản test. Tìm kiếm được trả bởi repository fixture. Dùng phiên Guest, Customer và Admin cô lập. [Nguồn sản phẩm fixture](../data/product-research.csv) |
| Browser và HTTP tham chiếu | Chrome không tải được trang ngoài do chặn mạng ở cả hai viewport. Đọc được tài liệu HTML công khai và theo liên kết sang trang 2. Không có screenshot tham chiếu hoặc phép đo trực tiếp CSS/animation. [Phân tích tham chiếu](byredo-reference-analysis.md) |
| MariaDB, thanh toán | 34 test MariaDB được bỏ qua với `P01_MARIADB_TEST=false`. Chưa xác nhận DB test độc lập. Các POST thêm giỏ và đăng nhập chỉ đến fixture. B0 không gửi POST đặt đơn, không giao dịch với MoMo, không seed hoặc ghi vào `beautyshop`. |

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
$env:P01_MARIADB_TEST = 'false'
mvn.cmd -version
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test
```

### Baseline trực quan

14 PNG chụp mới ở zoom 100%. Trang chủ và Admin chụp từ đầu trang. Ảnh có tên theo vùng cuộn đến đầu nội dung, lưới hoặc footer để đọc được vùng đó trong viewport. Header được đo ở đầu tài liệu trước khi cuộn. [Danh sách ảnh](../ui/screenshots/byredo-inspired/B0/)

| Màn hình / trạng thái | 1366×768 | 390×844 |
|---|---|---|
| Trang chủ Guest, header mặc định | [Desktop](../ui/screenshots/byredo-inspired/B0/home-1366.png) | [Mobile](../ui/screenshots/byredo-inspired/B0/home-390.png) |
| Catalog 3 card, có SKU hết và URL ảnh hỏng | [Catalog](../ui/screenshots/byredo-inspired/B0/catalog-1366.png) · [Vùng card](../ui/screenshots/byredo-inspired/B0/cards-1366.png) | [Catalog](../ui/screenshots/byredo-inspired/B0/catalog-390.png) · [Không khớp từ khóa](../ui/screenshots/byredo-inspired/B0/no-match-390.png) |
| PDP, bản 4 oz | [Desktop](../ui/screenshots/byredo-inspired/B0/pdp-1366.png) | [Mobile](../ui/screenshots/byredo-inspired/B0/pdp-390.png) |
| Giỏ Guest | — | [Giỏ trống](../ui/screenshots/byredo-inspired/B0/empty-cart-390.png) · [Đã thêm SKU](../ui/screenshots/byredo-inspired/B0/guest-cart-390.png) |
| Checkout Customer, COD, địa chỉ kiểm thử | [Desktop](../ui/screenshots/byredo-inspired/B0/checkout-1366.png) | [Mobile](../ui/screenshots/byredo-inspired/B0/checkout-390.png) |
| Điều hướng dùng chung | [Footer Guest](../ui/screenshots/byredo-inspired/B0/footer-1366.png) · [Admin danh mục](../ui/screenshots/byredo-inspired/B0/admin-1366.png) | — |

### Phát hiện trực tiếp

1. Header Guest mở sẵn cao 240 px trên desktop và 296 px trên mobile ở home/catalog. Header Customer tại checkout mobile cao 494 px, khoảng 59% màn hình 844 px. Giảm chiều cao mặc định có tác dụng ngay cả khi giữ toàn bộ ngôn ngữ thiết kế U1.
2. Catalog đo được 3 cột desktop, 2 cột mobile. Đáy của ba card trên hàng đầu desktop bằng nhau tại y=1038 CSS px. Khung ảnh 4:5 vẫn giữ chỗ khi tải lỗi. Card có tệp 404 báo `naturalWidth=0` và hiện biểu tượng ảnh vỡ của Chrome thay vì monogram. Hai card có URL NULL hiển thị monogram đúng.
3. Enter trong ô tìm của header trả `?q=Hydrating` và số kết quả fixture từ 3 còn 1. Vào `/products#brands` trên mobile mở đúng bộ lọc. Áp dụng loại da giữ giá trị trong query và hiện chip. `scrollWidth == clientWidth` ở home/catalog/checkout đã đo (1351 px desktop hoặc 375 px mobile).
4. Giá SKU đã chọn là 245.000 ₫. Sau khi thêm bằng Enter và đăng nhập, checkout có một dòng, địa chỉ được chọn, phí 25.000 ₫ và tổng 270.000 ₫. MoMo ẩn trong fixture. Theo mã hiện tại, lỗi khi đặt đơn có thể render báo giá mặc định với giảm giá 0 trong khi ô voucher còn mã. F07 cần một ca regression riêng trước khi chấm C27 PASS.
5. Admin vào danh mục bằng Enter, có mục quản trị đang chọn và không có nút giỏ ở header. Hai link `/account` và `/orders` trong footer Admin đều trả 403. Ở trang đăng ký của Guest, link `/login` mang `aria-current="page"`. Có thể sửa cả hai vấn đề ngay ở fragment mà vẫn giữ nguyên phân quyền.

### Accessibility và các mục chưa xác minh

B0 đã thử Enter trên form và liên kết, trạng thái active, nhãn, disclosure bộ lọc, phân quyền và chiều rộng tài liệu. U2.1–U2.3 đã có bằng chứng lịch sử cho skip link, Tab/Shift+Tab, focus, JS tắt và các viewport ghi riêng trong từng báo cáo. B0 chưa đánh giá toàn trang theo WCAG 2.2 AA, screen reader, cảm ứng thiết bị thật, tất cả màn hình ở zoom 200% hay toàn bộ trạng thái Admin. Chưa có số Lighthouse, LCP, INP, CLS hoặc ngân sách ảnh thương phẩm trên dataset chuẩn. [Phạm vi các lượt kiểm U2](ui-upgrade-U2-2026-11.md) · [Checklist dự án](../ui/QA-checklist.md)

## Quyết định thiết kế và điều kiện trước demo

Giữ nền ngà `#faf7f3`, burgundy `#5e2f3e`, heading serif, stack chữ nội dung có fallback và khoảng cách 4–72 px của U1. Header gọn, chức năng đổi mật độ và tiến độ duyệt là những cải tiến phù hợp với dữ liệu nhiều thương hiệu. Giữ phân trang GET và bước chọn SKU của BeautyShop. Video, quà, mẫu thử, nhãn “giới hạn” và tài sản Byredo không có cơ sở để đưa vào storefront. Không đổi token trong B0. [Tokens U1](../../src/main/resources/static/css/tokens.css#L1) · [Phân tích tham chiếu](byredo-reference-analysis.md)

Đường ngắn nhất đã xác định là thêm bảy PASS qua header/footer, ba PASS ở catalog và một PASS ở checkout. Mở lại endpoint demo với dữ liệu được phép đọc và ảnh thương phẩm có quyền. Khi owner xác nhận DB test độc lập, đối chiếu hai SKU cùng sản phẩm, quyền sở hữu giỏ, hợp nhất sau login, quote/voucher và giao dịch COD. Kiểm các trạng thái MoMo theo đúng cấu hình. Chỉ xác nhận P15 sau khi có 24/30 PASS trở lên, đủ 12 core, bằng chứng responsive/accessibility và regression theo yêu cầu. [Kế hoạch sửa nhỏ và các dependency](byredo-component-gap-analysis.md)

**Diff B0.** Thêm ba báo cáo Markdown và 14 ảnh PNG trong `docs/ui/screenshots/byredo-inspired/B0/`. Không sửa mã ứng dụng, test, dependency, migration hoặc roadmap trong lần audit này. Dừng ở B0, chưa triển khai B1–B6, chưa commit/push.

## B1.1 · Compact header · 22/11/2026

Header mobile đóng mặc định trên toàn bộ trang. Desktop từ 1100 CSS px mở điều hướng thành một hàng. Ngưỡng này dựa theo khoảng trống cần cho các liên kết Admin. Bản HTML mặc định đóng và dùng native details/summary nên vẫn mở được khi tắt JavaScript. Nhãn chuyển từ “Menu cửa hàng” sang “Đóng menu”, Escape đóng menu và đưa focus về summary. Resize xuống mobile đưa focus về summary trước khi đóng nếu đang ở trong menu.

File sửa gồm header.html (bỏ topnote và trạng thái open, thêm nhãn), storefront.css (grid, vùng bấm, khung search và reflow hẹp), app.js (thay điều kiện riêng PDP/giỏ bằng một disclosure dùng chung). Các thay đổi mã khác đã có trong working tree được bảo toàn. Head, backend, security, footer, token và các template nội dung giữ nguyên trong lát cắt này.

### Chiều cao mặc định sau thay đổi

Các giá trị dưới đây tính bằng CSS px. Mỗi nhóm đo lại trên tất cả trang đã liệt kê, ở cả năm viewport và hai mức zoom. Tổng cộng 90 tổ hợp. [Dữ liệu từng lượt đo](../ui/screenshots/byredo-inspired/B1.1/measurements.json)

| Viewport vật lý | Guest 100% | Customer 100% | Admin 100% | Mọi vai trò 200% |
|---|---:|---:|---:|---:|
| 360×800 | 109 | 109 | 109 | 226 |
| 390×844 | 109 | 109 | 109 | 226 |
| 768×1024 | 109 | 109 | 109 | 109 |
| 1366×768 | 113 | 116 | 116 | 109 |
| 1920×1080 | 113 | 116 | 116 | 109 |

So với B0, Guest desktop giảm 240 → 113 px, Guest mobile giảm 296 → 109 px và Customer Checkout mobile giảm 494 → 109 px. Ở zoom 200% trên 360/390, viewport CSS chỉ còn khoảng 172/187 px. Header xếp logo, menu và search thành nhiều hàng để giữ vùng thao tác và chữ đọc được.

### Bằng chứng và giới hạn

Chrome 158, Windows, Spring MVC/Thymeleaf với repository fixture trong bộ nhớ, tắt DataSource/JPA/Flyway. Guest được kiểm Home, Catalog, PDP, Cart, Login và lỗi sản phẩm 404. Customer kiểm Checkout và Account. Admin kiểm Catalog. Cả 90 lượt đều kiểm Enter/Space mở/đóng, Tab/Shift+Tab qua menu, Escape, và mở/đóng với JavaScript disabled. Quyền trong header đúng vai trò, Admin không thấy liên kết giỏ hoặc tài khoản Customer. Không ghi nhận Runtime.exceptionThrown. Những lượt này dùng fixture, chưa nghiệm thu MariaDB.

[Ảnh trước B0](../ui/screenshots/byredo-inspired/B0/home-390.png) · [Ảnh sau và các trang/role](../ui/screenshots/byredo-inspired/B1.1/) · [Checkout trước B0](../ui/screenshots/byredo-inspired/B0/checkout-390.png)

Header vẫn nằm trong luồng tài liệu và menu mở đẩy nội dung xuống. Chưa chạy screen reader, thiết bị cảm ứng vật lý hoặc chứng nhận WCAG toàn trang. B0 đã ghi hai link footer Admin dẫn đến 403. Vấn đề này vẫn thuộc B1.4.

**C01 PASS trên fixture. C08 PARTIAL.** Header đạt mục tiêu chiều cao ở zoom 100% và thao tác native/keyboard. Khi zoom 200% ở 360/390, toàn trang vẫn tràn ngang đến 208 CSS px. Probe chỉ ra chữ BeautyShop ở footer rộng 192 px trong cột 141 px, ngoài phạm vi header. Header được reflow, nhưng tiêu chí toàn trang không cuộn ngang chưa đạt. C08 tiếp tục PARTIAL để phản ánh giới hạn này. Coverage hiện tại 14/30 = 46,67%, core 9/12. Các thành phần khác theo ma trận B0.

Maven 3.9.11 chạy Temurin 21.0.13, P01_MARIADB_TEST=false. mvn test đạt 43 PASS, 0 FAIL, 0 ERROR, 34 SKIPPED (MariaDB). mvn -DskipTests package thành công và node --check app.js đạt. F01 vẫn BLOCKED. Chưa xác nhận DB test riêng, ảnh thương phẩm hay giao dịch checkout thật.

Dừng ở B1.1. Các mục menu nhiều cấp, biểu tượng wishlist/giỏ và footer tiếp tục theo các lát cắt riêng.

<a id="b12"></a>

## B1.2 · Luxury Multi-level Navigation · 22/11/2026

**C02 PASS trên MVC/Chrome fixture. C01 giữ PASS. C08 giữ PARTIAL.** Coverage hiện tại **15/30 = 50%**, 10/12 core PASS. C15 và C27 còn PARTIAL. Các trạng thái B0/B1.1 trong phần lịch sử thể hiện thời điểm tương ứng.

### Thay đổi và cách chọn dữ liệu

Panel “Khám phá” có bốn disclosure con Danh mục, Thương hiệu, Loại da và Đồng hành. Ba nhóm mua sắm dẫn tới GET `/products#categories`, `/products#brands` và `/products#skin-type`. Đích là select/input đang có trên catalog. Nhóm Đồng hành dẫn về `/` và `/chat`. Giữ liên kết Sản phẩm tới `/products` và các entry tài khoản, đơn hàng, giỏ theo vai trò.

Đã chọn fallback anchor được prompt B1.2 cho phép vì F01 chưa được xác minh. Không liệt kê nhãn danh mục/thương hiệu tự tạo. Người dùng chọn giá trị công khai tại filter hiện có. Bước này không cần bổ sung view-model hoặc truy vấn backend.

Desktop giữ thanh điều hướng B1.1. Panel Khám phá và các nhóm con luôn đóng khi tải. Khi mở, panel có bốn cột, đường chia màu U1 và giới hạn chiều cao 65vh, cuộn bên trong. Mobile dùng disclosure xếp dọc, nằm trong luồng tài liệu. Các nhóm có thể mở độc lập. Native details/summary tiếp tục hoạt động khi tắt JS. JS chỉ tăng cường Escape đóng cấp đang mở và đưa focus về summary, cùng xử lý resize qua breakpoint 1100 px ở cả hai chiều.

### Inventory và diff trong B1.2

Đã lưu Git inventory và hash source trước lần sửa trong thư mục build `target/p15-b12/`. Working tree đầu kỳ gồm nhiều thay đổi U1–U2 và B1.1 chưa commit.

| File | Thay đổi B1.2 |
|---|---|
| [header.html](../../src/main/resources/templates/fragments/header.html#L20) | Thêm panel Khám phá, bốn nhóm con và năm đích GET. Panel chỉ hiển thị cho Guest/Customer. |
| [storefront.css](../../src/main/resources/static/css/storefront.css#L451) | Quy tắc panel, disclosure, mục tiêu bấm ít nhất 44×44 px, cuộn và breakpoint dùng token U1. |
| [app.js](../../src/main/resources/static/js/app.js#L7) | Escape xử lý cấp gần nhất đang mở. Resize phục hồi focus trước khi đóng các disclosure con. |
| [GiaoDienPagesTest.java](../../src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java#L63) | Ba lượt MVC mới kiểm liên kết bộ lọc, form search GET và điều kiện hiển thị theo role. |
| Hai báo cáo P15 | Cập nhật C02, coverage, bằng chứng và những lỗi tiếp tục mở. |
| [Bộ ảnh B1.2](../ui/screenshots/byredo-inspired/B1.2/) | Ảnh trước được sao nguyên từ B1.1, ảnh sau chụp mới. JSON lưu số đo từng lượt và các kiểm tra đích. |

Hash cuối lần chạy chỉ thay đổi bốn file source kể trên. Các controller, repository, security, DB/Flyway, token, footer và nội dung PDP/cart/checkout nằm ngoài diff B1.2.

### Số đo và visual regression

Chrome Headless 158.0.8127.36 trên Windows. Fixture Tomcat dùng MVC, Thymeleaf, Spring Security và service thật. Repository mô phỏng trong bộ nhớ, DataSource/JPA/Flyway bị loại. Phiên Customer/Admin chỉ tồn tại trong fixture. Ảnh sản phẩm và số tồn không được dùng để nghiệm thu dữ liệu demo. [Số đo 90 tổ hợp](../ui/screenshots/byredo-inspired/B1.2/measurements.json) · [Nhật ký kiểm tra đích](../ui/screenshots/byredo-inspired/B1.2/checks.json)

Tất cả 90 số đo chiều cao khớp B1.1 khi panel Khám phá đóng. Các số dưới đây là CSS px. Thanh điều hướng desktop vẫn hiện mặc định theo B1.1.

| Viewport | Guest 100% | Customer 100% | Admin 100% | Mọi vai trò 200% |
|---|---:|---:|---:|---:|
| 360×800 | 109 | 109 | 109 | 226 |
| 390×844 | 109 | 109 | 109 | 226 |
| 768×1024 | 109 | 109 | 109 | 109 |
| 1366×768 | 113 | 116 | 116 | 109 |
| 1920×1080 | 113 | 116 | 116 | 109 |

Khung panel khi mở luôn có cạnh trái ≥0 và cạnh phải ≤clientWidth trong 80 tổ hợp Guest/Customer. Summary nhóm đo ít nhất 44×44 px. Tại zoom 200% và viewport 360/390, header tăng số hàng để giữ chữ và control. Cả 14 lượt tràn ngang toàn tài liệu thuộc các viewport hẹp này và còn nguyên mức lỗi đã biết của B1.1. C08 tiếp tục PARTIAL.

| Bằng chứng 100% | Trước B1.1 | Sau B1.2 |
|---|---|---|
| Guest desktop 1366×768 | [Trước](../ui/screenshots/byredo-inspired/B1.2/before-guest-1366.png) | [Đóng](../ui/screenshots/byredo-inspired/B1.2/guest-1366-100-closed.png) · [Panel](../ui/screenshots/byredo-inspired/B1.2/guest-1366-100-panel.png) · [Nhóm mở](../ui/screenshots/byredo-inspired/B1.2/guest-1366-100-group.png) |
| Guest mobile 390×844 | [Trước](../ui/screenshots/byredo-inspired/B1.2/before-guest-390.png) | [Đóng](../ui/screenshots/byredo-inspired/B1.2/guest-390-100-closed.png) · [Panel](../ui/screenshots/byredo-inspired/B1.2/guest-390-100-panel.png) · [Nhóm mở](../ui/screenshots/byredo-inspired/B1.2/guest-390-100-group.png) |
| Checkout Customer mobile | [Trước](../ui/screenshots/byredo-inspired/B1.2/before-checkout-390.png) | [Đóng](../ui/screenshots/byredo-inspired/B1.2/customer-390-100-closed.png) · [Nhóm mở](../ui/screenshots/byredo-inspired/B1.2/customer-390-100-group.png) |
| Admin catalog desktop | [Trước](../ui/screenshots/byredo-inspired/B1.2/before-admin-1366.png) | [Sau](../ui/screenshots/byredo-inspired/B1.2/admin-1366-100-closed.png) |
| JavaScript disabled | | [Mở cả bốn nhóm](../ui/screenshots/byredo-inspired/B1.2/guest-1366-100-nojs-all-groups.png) |
| Zoom 200% | | [Guest 360×800, nhóm mở](../ui/screenshots/byredo-inspired/B1.2/guest-360-200-group.png) · [Customer 1920×1080](../ui/screenshots/byredo-inspired/B1.2/customer-1920-200-group.png) |

Các ảnh mang tên `role-width-zoom-state` ghi đúng độ rộng vật lý, zoom và trạng thái. Mỗi bộ Home Guest/Checkout Customer có cả năm viewport, hai mức zoom và ba trạng thái. Admin có mười ảnh để đối chiếu menu đúng quyền. Ảnh trước giữ nguyên dữ liệu và thời điểm B1.1, không phải lượt render lại bản cũ.

### Kiểm thử thực tế

- **90 tổ hợp.** Guest Home, Catalog, PDP, Cart, Login, 404; Customer Account và Checkout; Admin Catalog. Mỗi trang chạy năm viewport × hai zoom. Mở/đóng menu ngoài, mở/đóng panel, Space/Enter trên nhóm, Tab/Shift+Tab và Escape có assertion. Guest/Customer có thêm click chuột và touch event để mở/đóng panel ở mỗi tổ hợp. No-JS chạy lại trên toàn bộ 90 tổ hợp, mở bằng Enter qua các cấp để tới link con.
- **Đích GET.** Guest và Customer, desktop/mobile, JS bật/tắt: 24 lượt kích hoạt link con để mở đúng ba ID trên catalog và xác nhận filter panel mở. Tám lượt tới /chat xác nhận textarea câu hỏi. Kiểm bổ sung kích hoạt link về Home và header search GET với từ khóa Hydrating, nhận một kết quả fixture. Đây là kiểm điều hướng tới control, chưa phải kiểm query MariaDB theo giá trị danh mục/thương hiệu.
- **Focus và resize.** Escape từ summary nhóm đóng nhóm và trả focus tại summary. Escape tiếp đóng panel và focus summary Khám phá. Desktop → mobile khi đang ở submenu và mobile → desktop khi focus ở link con đều phục hồi về summary ngoài, đóng nhóm, giữ navigation sử dụng được.
- **Vai trò và giỏ.** Customer sau login, duyệt menu và thay viewport vẫn có một SKU, badge 1, tạm tính 245.000 ₫. Logout trở lại Guest đúng entry. Admin không có panel mua sắm, link /account hoặc giỏ trong header và vẫn có liên kết quản trị. Hai link Customer trong footer Admin là known issue B1.4 đã ghi từ B0.
- **JavaScript.** 0 Runtime.exceptionThrown trong lượt QA chính. Ảnh URL hỏng có thể tiếp tục trả 404 theo fixture và card catalog vẫn thuộc vấn đề C15. Không có thay đổi resource từ bên thứ ba.

### Maven và build

Maven 3.9.11 dùng Temurin JDK 21.0.13. `P01_MARIADB_TEST=false`.

| Phạm vi | PASS | FAIL | ERROR | SKIPPED |
|---|---:|---:|---:|---:|
| Suite trước B1.2 theo B1.1 | 43 | 0 | 0 | 34 |
| Suite sau B1.2, 80 test | **46** | **0** | **0** | **34** |

Đã chạy `mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test`, `mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package`, `node --check src/main/resources/static/js/app.js` và `git diff --check`. JAR tạo tại `target/beautyshop-0.0.1-SNAPSHOT.jar`. 34 SKIPPED là tích hợp MariaDB, chưa có DB test độc lập được xác nhận.

### Những việc còn mở

**KNOWN ISSUE B1.4** là footer tràn ngang ở zoom 200% và liên kết Customer hiện cho Admin. **F01 BLOCKED**, catalog demo chưa có bằng chứng mới trên MariaDB. Ảnh/giá/tồn và gộp giỏ nguyên tử trên MariaDB chưa được kiểm trong fixture. **C21** còn chờ quyền media hoặc quyết định N/A. Chưa kiểm screen reader, thiết bị cảm ứng vật lý, Lighthouse hoặc toàn bộ tiêu chí WCAG. Không có bằng chứng browser mới về Byredo trong B1.2.

B1.2 hoàn tất ở lớp UI/MVC/fixture với các đích fallback đã được prompt phê duyệt. Các giai đoạn B1.3/B1.4/B2/B3/B4 tiếp tục theo phạm vi riêng.

<a id="b13"></a>

## B1.3 · Header actions và trạng thái điều hướng · 22/11/2026

**C05, C06, C07 PASS trên source/MVC/Chrome fixture. C01, C02 giữ PASS.** Coverage hiện tại **18/30 = 60%**, core **10/12 PASS**. C08 tiếp tục PARTIAL vì footer gây tràn ngang ở zoom 200%. Các mục lịch sử B0–B1.2 ghi trạng thái ở thời điểm kiểm tương ứng.

### Quyết định và diff

- **Wishlist.** Chỉ Customer thấy action trái tim tới GET `/account/wishlist`, có nhãn “Sản phẩm yêu thích”. Guest tiếp tục dùng đăng nhập hiện có. Admin không có entry Customer. B1.3 bổ sung lối vào danh sách, không bổ sung thao tác lưu.
- **Giỏ.** SVG túi mua sắm tự vẽ, dùng stroke currentColor và badge gắn bên cạnh. Nhãn link đọc “Giỏ hàng · N sản phẩm”. N lấy trực tiếp từ `uiCartCount` của advice. Khi thiếu dữ liệu badge, link chỉ đọc “Giỏ hàng”. SVG trang trí có `aria-hidden=true` và `focusable=false`.
- **Trạng thái.** Link chỉ có `aria-current=page` khi khớp đúng path. Các namespace con dùng `data-active-group` và độ đậm chữ để định vị nhóm. Link Admin Catalog vẫn trỏ tới /admin/catalog, là canonical redirect đã kiểm tới /admin/catalog/categories. Trang danh mục brands/products và form con không được đánh current cho link categories. Các link Admin khác chỉ nhận current khi đúng đích danh sách.

| File sửa | Nội dung |
|---|---|
| [header.html](../../src/main/resources/templates/fragments/header.html#L45) | Action giỏ, wishlist Customer và phép so khớp path chính xác. |
| [storefront.css](../../src/main/resources/static/css/storefront.css#L435) | Icon 22 px, vùng bấm ≥44 px, badge không co và nền/viền chỉ trạng thái current cho action. Giữ focus U1. |
| [GiaoDienPagesTest.java](../../src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java#L64) | Thêm 9 lượt MVC kiểm role, badge 0/1/5/12 và current ở login/register/catalog/PDP. |
| Báo cáo này và ma trận | Cập nhật C05–C08, coverage và bằng chứng. |
| [Ảnh và JSON B1.3](../ui/screenshots/byredo-inspired/B1.3/) | 78 ảnh PNG, gồm 4 ảnh trước sao nguyên từ B1.2, 74 ảnh mới. Ba JSON lưu kích thước, current, tên action và đích đã kiểm. |

Inventory Git và hash của source đầu kỳ được lưu trong `target/p15-b13/`. So sánh cuối kỳ có đúng ba file source thay đổi. Các thay đổi chưa commit trước B1.3 được giữ. app.js, GiaoDien_Advice, controller, repository, Security_Config, dữ liệu, migration và nội dung trang ngoài header không thuộc diff B1.3.

### Trạng thái điều hướng đã kiểm

| URL | Link header nhận aria-current=page |
|---|---|
| / | / |
| /login | /login |
| /register | Không có |
| /products | /products |
| /products/demo-r026?variant=22 | Không có. Link Sản phẩm chỉ mang data-active-group. |
| /cart | /cart |
| /checkout | Không có |
| /account | /account |
| /account/wishlist | /account/wishlist. Tài khoản chỉ mang data-active-group. |
| /admin/catalog/categories | /admin/catalog, canonical đã xác minh |
| /admin/catalog/products | Không có. Quản trị chỉ mang data-active-group. |

### Chrome và regression C01/C02

Chrome Headless 158.0.8127.36, Windows, Tomcat MVC/Thymeleaf và Spring Security thực trên loopback. Repository fixture trong bộ nhớ, loại DataSource/JPA/Flyway. Tên/giá sản phẩm theo dữ liệu nghiên cứu hiện có, stock fixture được đặt đủ cho kịch bản 12 đơn vị. Các POST chỉ tới fixture. Chưa xác minh lưu wishlist, khóa tồn hoặc giao dịch trên MariaDB.

**100 tổ hợp** gồm Guest Home/Catalog/Login/Register/PDP/Cart (60), Customer Account/Wishlist/Checkout (30) và Admin Catalog (10). Mỗi trang có năm viewport × zoom 100%/200%. Các lượt kiểm trạng thái menu và current theo vai trò, rồi mở lại khi tắt JS. 90 lượt Guest/Customer có thêm cấp con, Enter/Space/Tab/Shift+Tab/Escape, click và touch mô phỏng. Panel mua sắm ẩn cho Admin. 80 tổ hợp trùng trang với B1.2 đều khớp đúng chiều cao cũ. Các trang mới có cùng chiều cao theo vai trò.

| Viewport vật lý | Guest 100% | Customer 100% | Admin 100% | Mọi vai trò 200% |
|---|---:|---:|---:|---:|
| 360×800 | 109 | 109 | 109 | 226 |
| 390×844 | 109 | 109 | 109 | 226 |
| 768×1024 | 109 | 109 | 109 | 109 |
| 1366×768 | 113 | 116 | 116 | 109 |
| 1920×1080 | 113 | 116 | 116 | 109 |

Khung header khi mở menu và panel nằm trong chiều rộng CSS khả dụng ở cả 90 lượt Guest/Customer. Từng icon có bounding box ít nhất 44×44 px và nằm trong viewport. Không che lỗi bằng overflow-x:hidden. Toàn trang còn tràn ngang trong 16 lượt tại 360/390 và zoom 200%, tập trung vào nội dung/footer đã ghi từ B1.1. C08 vẫn PARTIAL, nguyên nhân hiện tại được cập nhật ngay trong hàng ma trận. [Chi tiết từng lượt](../ui/screenshots/byredo-inspired/B1.3/measurements.json)

**C05.** Trên cả mobile/desktop, Tab từ giỏ sang wishlist, Shift+Tab quay lại giỏ, focus có outline 3 px. Enter mở được danh sách bằng controller thật. Touch mô phỏng vẫn mở wishlist khi tắt JS. Cây accessibility Chrome xác nhận hai tên link “Sản phẩm yêu thích” và “Giỏ hàng · 12 sản phẩm”. Guest truy cập thẳng route bảo vệ chuyển tới login. Admin truy cập thẳng nhận 403 và không có action trong header. [Kiểm tra action](../ui/screenshots/byredo-inspired/B1.3/action-checks.json)

**C06.** Guest bắt đầu 0, thêm một SKU thành 1, đổi lượng và thêm SKU thứ hai để tổng 5 đơn vị. Sau đó cập nhật SKU thứ nhất lên 9 và giữ SKU thứ hai 3, badge là 12. Tải giỏ khi tắt JS vẫn giữ đúng 12. Login qua form thật hợp nhất về fixture Customer, tiếp tục duyệt menu và wishlist giữ hai dòng, tổng 12 và tạm tính 4.425.000 ₫. Đây là minh chứng đếm đơn vị server, không xác nhận tính nguyên tử MariaDB. [Kiểm tra luồng giỏ và GET menu](../ui/screenshots/byredo-inspired/B1.3/checks.json)

**C02.** Đã kiểm lại 24 lượt tới ba bộ lọc theo Guest/Customer, desktop/mobile và JS bật/tắt. Có thêm các lượt tới tư vấn và resize desktop → mobile. Panel và nhóm con giữ hành vi native. Không có Runtime.exceptionThrown trong lượt chính. Các ảnh SKU 404 theo fixture tiếp tục là C15, chưa xử lý ở B1.3.

### Ảnh trước và sau

| Trạng thái | Trước B1.2 | Sau B1.3 |
|---|---|---|
| Guest desktop 1366×768 | [Trước](../ui/screenshots/byredo-inspired/B1.3/before-guest-1366.png) | [Sau](../ui/screenshots/byredo-inspired/B1.3/guest-1366-100-closed.png) |
| Guest mobile 390×844, nhóm mở | [Trước](../ui/screenshots/byredo-inspired/B1.3/before-guest-390-group.png) | [Sau](../ui/screenshots/byredo-inspired/B1.3/guest-390-100-group.png) |
| Customer desktop, header checkout | [Trước](../ui/screenshots/byredo-inspired/B1.3/before-customer-1366.png) | [Sau](../ui/screenshots/byredo-inspired/B1.3/customer-1366-100-closed.png) |
| Customer mobile, nhóm mở | [Trước](../ui/screenshots/byredo-inspired/B1.3/before-customer-390-group.png) | [Sau](../ui/screenshots/byredo-inspired/B1.3/customer-390-100-group.png) |
| Wishlist current | | [390×844](../ui/screenshots/byredo-inspired/B1.3/customer-wishlist-390.png) · [1366×768](../ui/screenshots/byredo-inspired/B1.3/customer-wishlist-1366.png) |
| Badge hai SKU | | [5 đơn vị](../ui/screenshots/byredo-inspired/B1.3/guest-two-sku-five-units.png) · [12 đơn vị](../ui/screenshots/byredo-inspired/B1.3/guest-two-sku-twelve-units.png) |
| Zoom 200% | | [Guest 360](../ui/screenshots/byredo-inspired/B1.3/guest-360-200-group.png) · [Customer 390](../ui/screenshots/byredo-inspired/B1.3/customer-390-200-group.png) · [Admin 1920](../ui/screenshots/byredo-inspired/B1.3/admin-1920-200-closed.png) |

### Maven JDK 21 và build

Maven 3.9.11 dùng Temurin 21.0.13. P01_MARIADB_TEST=false.

| Lượt | PASS | FAIL | ERROR | SKIPPED |
|---|---:|---:|---:|---:|
| Baseline B1.2 | 46 | 0 | 0 | 34 |
| Lượt đầu B1.3 | 46 | 0 | 9 | 34 |
| Suite cuối B1.3 (89 test) | **55** | **0** | **0** | **34** |

9 ERROR của lượt đầu do dùng bộ phân tích XML XPath cho HTML5 có thuộc tính defer. Đã sửa kiểm tra để đọc phần header HTML, không đổi markup hợp lệ của ứng dụng. Các kiểm tra DOM và cây accessibility thực được chạy trên Chrome.

Đã chạy Maven test, Maven -DskipTests package với repository cục bộ `target/maven-repository`, node --check app.js và git diff --check. JAR ở `target/beautyshop-0.0.1-SNAPSHOT.jar`. 34 SKIPPED thuộc MariaDB integration vì chưa xác nhận database test riêng. Các test cũ về SKU, quyền, CSRF và tiền tiếp tục đạt.

### Những mục còn mở

- **C08 PARTIAL, B1.4** còn lỗi footer tràn ngang tại 360/390 khi zoom 200%. Header đã gọn từ B1.1 và không phải các giá trị 296/494 px của B0.
- **C28 PARTIAL, B1.4** còn hai liên kết Customer trong footer Admin dẫn 403.
- **F01 BLOCKED**, dữ liệu catalog/ảnh thật và giỏ bền vững trên MariaDB chưa có bằng chứng mới. C15, C27 và C21 giữ trạng thái trước đó.
- Cây accessibility xác nhận tên link, chưa có lượt dùng screen reader hoặc thiết bị cảm ứng vật lý. Không tuyên bố WCAG toàn trang hay điểm Lighthouse.

B1.3 dừng sau header actions và navigation state. Chưa commit/push.

<a id="b14"></a>

## B1.4 · Luxury Footer, quyền và reflow · 22/11/2026

**C08 và C28 PASS trên source/MVC/Chrome fixture.** C01/C02/C05/C06/C07 tiếp tục đạt. Coverage hiện tại **20/30 = 66,67%**, core **10/12**. C15 và C27 là hai core còn PARTIAL. Các mục B0–B1.3 bên trên giữ nguyên lịch sử, gồm lượt test B1.3 có 9 ERROR đã được sửa.

### Nguyên nhân và phép đo trước/sau

Đã chụp và đo trước khi sửa, trên cùng fixture và dữ liệu với lượt sau. Bộ kiểm footer có **110 tổ hợp**, gồm bộ 100 của B1.3 và thêm Guest 404 tại năm viewport × hai zoom. Trước sửa có 18 lượt toàn trang tràn ngang. Hai lượt tăng so với B1.3 thuộc trang 404 mới được thêm. Sau sửa, **110/110 có scrollWidth == clientWidth**.

Ở các trang tràn, footer-brand có font 36 px và chuỗi BeautyShop không có điểm ngắt. Cột rộng 141 hoặc 156 px nhưng phần chữ có scrollWidth 192 px, đẩy scrollWidth của footer/tài liệu thành 208 px. Grid desktop trước đó dùng track có min-size tự động. B1.4 dùng minmax(0, …), min-width:0 và overflow-wrap:anywhere trong các cột footer. Thêm điểm ngắt tự nhiên Beauty<wbr>Shop. Font, màu và các token U1 giữ nguyên.

| Nhóm đo tại zoom 200% | clientWidth | Brand rect left/right | Brand clientWidth | Brand scroll trước → sau | Trang scroll trước → sau |
|---|---:|---|---:|---:|---:|
| Guest Home/Catalog/Login/Register/404, 360×800 | 172 | 16 / 156,5 | 141 | 192 → 141 | 208 → 172 |
| Customer Account/Checkout/Wishlist, 360×800 | 172 | 16 / 156,5 | 141 | 192 → 141 | 208 → 172 |
| Admin Catalog, 360×800 | 172 | 16 / 156,5 | 141 | 192 → 141 | 208 → 172 |
| Cùng các nhóm tại 390×844 | 187 | 16 / 171,5 | 156 | 192 → 156 | 208 → 187 |

Rect là hộp bố cục. Phần chữ trước sửa tràn ra ngoài hộp, được thể hiện bằng scrollWidth. Guest PDP/Cart vốn dùng footer-brand 24 px theo CSS U2 nên không nằm trong 18 lượt lỗi. Quy tắc đó được giữ nguyên.

Probe cũng ghi nhận vài phần tử khác có nội dung rộng hơn ô chứa, như button-group ở hero, form lọc và login tại viewport CSS hẹp. Chúng vẫn nằm trong mép trang. Label visually-hidden là phần tử được ẩn cho mục đích accessibility. Sau sửa footer, không thành phần nào trong bộ trang kiểm làm toàn tài liệu tràn ngang. B1.4 không thêm clip hoặc overflow:hidden để che lỗi. [JSON trước](../ui/screenshots/byredo-inspired/B1.4/before-measurements.json) · [JSON sau](../ui/screenshots/byredo-inspired/B1.4/after-measurements.json)

### Inventory và file sửa

Đầu kỳ đã lưu Git inventory, diff stat và hash source trong target/p15-b14. Hash cuối chỉ đổi ba file source dưới đây. Header, app.js, tokens, Security_Config, controller, service, repository, entity, pom.xml và Flyway giữ nguyên trong B1.4.

| File | Thay đổi |
|---|---|
| [footer.html](../../src/main/resources/templates/fragments/footer.html#L5) | Bốn cột có tên navigation phân biệt. Cột tài khoản render riêng Guest/Customer; Admin có cột quản trị. Wordmark có wbr để ngắt tại Beauty/Shop. |
| [storefront.css](../../src/main/resources/static/css/storefront.css#L76) | Chỉ sửa quy tắc grid/cột/brand footer. Track có min-size 0, cột cho phép ngắt chữ. |
| [GiaoDienPagesTest.java](../../src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java#L64) | Ba test MVC đọc đúng node footer, kiểm các link phải hiện và phải ẩn theo role. |
| Hai báo cáo P15 và [bằng chứng B1.4](../ui/screenshots/byredo-inspired/B1.4/) | Cập nhật trạng thái hiện tại và lưu 96 PNG cùng JSON. Có 24 ảnh trước/sau footer và 72 ảnh regression header/menu/giỏ. |

### Liên kết theo vai trò

Tất cả link hiển thị đều được lấy từ đúng node footer, gửi GET với session của vai trò tương ứng và kích hoạt bằng Enter. Mỗi link được kiểm cả JS bật và tắt. 23 link theo ba vai trò tạo **46 lượt GET + keyboard/no-JS**, toàn bộ trả HTTP 200.

| Vai trò | Các link footer hiển thị | HTTP / hành vi |
|---|---|---|
| Guest | /products, /products#categories, /products#brands, /login, /register, /cart, /chat | Cả 7 đích trả 200. Link tài khoản ghi rõ Đăng nhập/Tạo tài khoản. Không có link Customer hoặc Admin. |
| Customer | /products, /products#categories, /products#brands, /account, /orders, /account/wishlist, /cart, /chat, /account/returns | Cả 9 đích trả 200. Hồ sơ/đơn/wishlist/đổi trả truy cập đúng session. |
| Admin | /products, /products#categories, /products#brands, /admin/catalog/categories, /admin/catalog/brands, /admin/catalog/products, /chat | Cả 7 đích trả 200. Không có /account, /orders, /cart hoặc wishlist trong DOM footer. |

Hai đích /account và /orders của footer Admin cũ vẫn trả 403 khi kiểm trực tiếp với phiên Admin. Việc sửa nằm ở điều kiện render server, không nới quyền backend. [Kết quả từng link](../ui/screenshots/byredo-inspired/B1.4/footer-links.json)

Tab/Shift+Tab theo thứ tự nhìn và Enter mở đúng URL. Focus footer giữ outline 3 px màu nền ngà. Các nav có accessible name phân biệt, link có tên hiển thị. Touch mô phỏng ở 390×844, zoom 200% và JS tắt mở đúng /products#brands cho cả ba role; vùng bấm rộng và cao ít nhất 44 CSS px. Không khóa cuộn body. [Kiểm tra touch](../ui/screenshots/byredo-inspired/B1.4/footer-touch.json)

### Bằng chứng trước/sau cùng viewport và vai trò

Các ảnh dưới đây chụp mới trước và sau khi chỉnh footer trên cùng fixture. Role, zoom, viewport và dữ liệu sản phẩm/giỏ giống nhau. Tại desktop, footer ngắn hơn viewport nên một phần nội dung trước footer xuất hiện. Tại mobile/zoom 200%, ảnh là vùng đầu footer và phần còn lại truy cập bằng cuộn. Các link cuối footer đã được kiểm thực tế bằng bàn phím.

| Vai trò / viewport / zoom | Trước | Sau |
|---|---|---|
| Guest 1366×768 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-guest-1366-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-guest-1366-100.png) |
| Guest 390×844 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-guest-390-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-guest-390-100.png) |
| Guest 360×800 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-guest-360-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-guest-360-200.png) |
| Guest 390×844 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-guest-390-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-guest-390-200.png) |
| Customer 1366×768 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-customer-1366-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-customer-1366-100.png) |
| Customer 390×844 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-customer-390-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-customer-390-100.png) |
| Customer 360×800 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-customer-360-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-customer-360-200.png) |
| Customer 390×844 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-customer-390-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-customer-390-200.png) |
| Admin 1366×768 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-admin-1366-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-admin-1366-100.png) |
| Admin 390×844 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-admin-390-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-admin-390-100.png) |
| Admin 360×800 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-admin-360-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-admin-360-200.png) |
| Admin 390×844 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-admin-390-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-admin-390-200.png) |

### Regression và chiều cao header

Đã chạy lại bộ **100 tổ hợp B1.3** sau sửa footer với menu đóng/mở, nhóm con, Guest/Customer/Admin, action giỏ/wishlist, active state, Enter/Space/Escape, Tab/Shift+Tab, click/touch, resize và no-JS. Cả 100 tài liệu đều không tràn ngang. Cả 110 số đo header trong cặp trước/sau footer đều khớp nhau.

| Viewport | Guest 100% | Customer 100% | Admin 100% | Mọi role 200% |
|---|---:|---:|---:|---:|
| 360×800 | 109 | 109 | 109 | 226 |
| 390×844 | 109 | 109 | 109 | 226 |
| 768×1024 | 109 | 109 | 109 | 109 |
| 1366×768 | 113 | 116 | 116 | 109 |
| 1920×1080 | 113 | 116 | 116 | 109 |

Menu Khám phá vẫn mở tới đúng categories/brands/skin-type. Badge server được kiểm 0 → 1 → 5 → 12 đơn vị. Sau login và duyệt menu, Customer giữ hai SKU, tổng 12 đơn vị và tạm tính 4.425.000 ₫. 0 Runtime.exceptionThrown trong lượt regression. [Số đo regression](../ui/screenshots/byredo-inspired/B1.4/measurements.json) · [Kiểm tra luồng](../ui/screenshots/byredo-inspired/B1.4/checks.json)

### Maven, JAR và giới hạn kiểm chứng

Maven 3.9.11, Temurin JDK 21.0.13. Lượt cuối ghi Tests run 92, Failures 0, Errors 0, Skipped 34, tức **58 PASS / 0 FAIL / 0 ERROR / 34 SKIPPED**. Ba test thêm kiểm footer Guest/Customer/Admin. Baseline B1.3 là 55 PASS và 34 SKIPPED.

Đã chạy các lệnh sau với P01_MARIADB_TEST=false.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
$env:P01_MARIADB_TEST = 'false'
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package
git diff --check
```

JAR tạo thành công tại target/beautyshop-0.0.1-SNAPSHOT.jar. App.js không thay đổi. Chrome Headless 158.0.8127.36 trên Windows dùng zoom thật của browser qua settingsPrivate.setDefaultZoom(1/2), viewport qua DevTools; không dùng transform hoặc CSS zoom.

Các kết quả UI/HTTP/browser dùng Spring MVC, Thymeleaf và Spring Security thật trên Tomcat loopback với repository trong bộ nhớ. Fixture loại DataSource/JPA/Flyway. Tăng số trang kiểm bằng controller Đơn hàng thật trong fixture để xác minh /orders, không thêm controller hoặc route vào ứng dụng. Không gửi POST nghiệp vụ tới database thật.

**F01 vẫn BLOCKED.** 34 MariaDB integration test tiếp tục SKIPPED vì chưa có database test riêng được xác nhận. C15 ảnh 404, C27 báo giá voucher và C21 quyền media tiếp tục mở. Screen reader, cảm ứng trên thiết bị vật lý và đánh giá WCAG 2.2 AA toàn trang là NOT VERIFIED. Không nâng C29/C30.

C08/C28 đạt trong phạm vi bộ trang, vai trò và viewport đã kiểm trên fixture. B1.4 hoàn thành và dừng tại đây. B2 tiếp tục cần xử lý các khoảng trống catalog theo ma trận sau khi được yêu cầu.

<a id="b2"></a>

## B2 · Luxury Catalog · 23/11/2026

**C15 → C12 → C18 đã triển khai đúng thứ tự, PASS trên source/MVC/Chrome fixture.** Coverage **23/30 =76,67%**, core **11/12**. C27 vẫn PARTIAL, C13/C17/C20/C21 không triển khai. Chưa hoàn thành P15 80%. GSAP C19 được owner nghiệm thu kỹ thuật có điều kiện trước B2; vendor, adapter và Home animation được giữ nguyên.

### Nguồn sự thật, inventory và phạm vi

Đã đọc prompt B2 hiện có, AGENTS, README/SRS/Business Rules/ERD/DECISIONS, P12 gốc, prompt U2, báo cáo U2, Design System U1, phân tích tham chiếu và các báo cáo P15/GSAP/công nghệ. Kiểm tra controller/service/repository/projection chỉ đọc và các template/CSS/JS/test thực tế. GET `/products` nhận q,brand,category,skinType,sort,page,size; controller strip chuỗi, giới hạn size 1–48, Page zero-based và đọc lại trang cuối khi page vượt vùng. Không cần đổi Java production.

Working tree đầu kỳ đã có nhiều thay đổi chưa commit, gồm GSAP C19 và các giai đoạn trước. Git inventory, diff/hash và bản trước của năm file source lưu trong `target/p15-b2/`. Baseline test thực tế sau GSAP là **61 PASS/34 SKIPPED**, khác mốc 58 PASS cũ ghi trong prompt B2. Không reset/clean/commit/push.

| File sửa trong B2 | Thay đổi |
|---|---|
| [product-card.html](../../src/main/resources/templates/fragments/product-card.html#L4) | Trim URL trước khi quyết định có img, placeholder HTML có sẵn, img alt rỗng/400×500, ưu tiên eager card đầu và lazy các card sau. Fragment nhận thêm cờ uuTienAnh, cập nhật đúng nơi gọi duy nhất trong catalog. |
| [product/list.html](../../src/main/resources/templates/product/list.html#L6) | Count SSR từ model, nhãn chip q riêng, truyền cờ eager đầu; tiến độ lấy Page và nhánh rỗng an toàn. GET form/chips/pagination giữ contract. |
| [storefront.css](../../src/main/resources/static/css/storefront.css#L328) | Ba quy tắc catalog: spacing/tabular numbers cho progress, bố trí fallback và tôn trọng hidden. Giữ token, header/footer, lưới4:5 và object-fit contain. |
| [app.js](../../src/main/resources/static/js/app.js#L35) | Thêm vòng xử lý error riêng cho img catalog, bắt cả complete/naturalWidth=0 trước listener. Không sửa nhánh PDP/cart/menu/dialog/POST. |
| [GiaoDienPagesTest.java](../../src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java#L213) | 20 ca bổ sung: 4 SSR ảnh, 8 tổ hợp filter và 8 ca range/controller clamp/empty page. |
| Hai báo cáo P15, [bằng chứng B2](../ui/screenshots/byredo-inspired/B2/) | Ma trận hiện hành, lịch sử, 178 PNG cùng JSON/log thực tế. |

Head/header/footer/ui pagination, GSAP vendor/provenance/adapter, Home, checkout, toàn bộ backend, Spring Security, Flyway, pom.xml và token giữ nguyên. Không thêm dependency, CDN, ảnh runtime hoặc dữ liệu vào database.

### B2.1 — C15: fallback ảnh

Trước B2, URL 404/decode có naturalWidth=0 nhưng img vẫn hiện và không có fallback. Sau B2, mỗi card luôn có placeholder HTML; URL NULL/rỗng/trắng render ngay placeholder, không phát request src rỗng. Khi URL có giá trị, error listener ẩn img và hiện placeholder. Không đổi src, không request ảnh thay thế hoặc tạo vòng lặp. `complete && naturalWidth===0` xử lý ảnh đã lỗi trước lúc listener gắn. CSS riêng bảo đảm hidden không bị display:grid ghi đè.

Ảnh có alt rỗng vì tên/brand đã nằm trong cùng link; placeholder thêm đúng một thông điệp “Chưa có ảnh sản phẩm”, monogram B aria-hidden. Chrome accessibility tree xác nhận link ảnh hợp lệ không đọc thông điệp thiếu ảnh, link lỗi đọc đúng một lần. Chưa dùng screen reader thật.

Chrome đã kiểm 6 trường hợp độc lập đồng thời trên card, tại 5 viewport ×2 zoom ×JS bật/tắt:

| Trường hợp | HTTP/naturalWidth | Khi bật JS | Khi tắt JS |
|---|---|---|---|
| NULL, chuỗi rỗng, trắng | Không có img/request | SSR placeholder | SSR placeholder |
| PNG hợp lệ | 200, naturalWidth=1280 | Ảnh hiện, fallback ẩn | Ảnh hiện, fallback ẩn |
| URL tệp thiếu | 404, naturalWidth=0 | img.hidden=true, fallback hiện | Có thể hiện icon ảnh lỗi native; không có fallback chuyển bằng JS |
| PNG lỗi giải mã | 200 image/png, naturalWidth=0 | img.hidden=true, fallback hiện | Cùng giới hạn native như 404 |

Trong cả hai nhánh JS, card vẫn điều hướng PDP bằng link. Đã kiểm riêng no-JS từ card R026 sang PDP/SKU. Khung ảnh đo gần đúng4:5 (tolerance 0,02 ở tỷ lệ do pixel rounding), ảnh contain, card đầu eager, các ảnh sau lazy. Ảnh lỗi không làm hỏng card có ảnh tốt. Chuyển trang/tìm kiếm/bộ lọc tạo SSR mới và gắn lại cơ chế. Lượt chặn app.js rồi gắn handler sau khi ảnh lỗi đã complete xác nhận nhánh cached failure. [Chi tiết image states](../ui/screenshots/byredo-inspired/B2/measurements.json) · [HTTP và cached-failure](../ui/screenshots/byredo-inspired/B2/checks.json)

**Nguồn ảnh QA:** file PNG OK là bản sao screenshot FAQ sẵn có của chính dự án `docs/report/evidence-p10/faq.png`, chỉ trong thư mục upload fixture. Đây là ảnh kiểm thử kỹ thuật để chứng minh tải/giải mã/contain, không phải ảnh mỹ phẩm thật. Ảnh lỗi là tệp byte cố ý không hợp lệ trong cùng fixture, không upload hoặc seed production. Tên/giá 27 dòng lấy từ `docs/data/product-research.csv`; tồn, URL và ID là kịch bản mock. Không dùng ảnh Byredo, ảnh sinh hoặc fixture để che F01. [Nguồn và hash fixture](../ui/screenshots/byredo-inspired/B2/image-fixture.json)

**Giới hạn no-JS 404 được giữ minh bạch theo prompt B2:** việc thay icon lỗi bằng monogram cần JS; thiếu URL vẫn có fallback SSR. C15 PASS theo tiêu chí image resilience và fallback có JS đã được Chrome chứng minh, không tuyên bố ảnh thương phẩm MariaDB đã được nghiệm thu. F14 toàn bộ pipeline ảnh nhiều kích thước/budget vẫn PARTIAL.

### B2.2 — C12: count và chip

Count là tổng ba điều kiện brand/category/skinType không rỗng từ model đã được controller strip; không đếm q, sort, page, size hoặc anchor. Summary native hiển thị “Bộ lọc (N)” ngay khi panel đóng, kèm nội dung accessible “đang áp dụng”. q giữ chip “Từ khóa” và tên hành động “Bỏ tìm kiếm”; sort giữ chip “Bỏ sắp xếp”. Không đếm số chip bằng JS.

Chip bỏ từng tiêu chí giữ q và các filter khác, sort, size; bỏ page để controller bắt đầu lại. Chip q/sort cũng giữ các trường còn lại. **“Xóa tất cả” giữ href=/products như trước**, xóa cả q/sort/size/page. Default “Tất cả” có value rỗng nên count0; không tạo đặc lệ cho literal slug “Tất cả” gửi thủ công, vì backend hiện xem mọi slug không rỗng là điều kiện lọc. Slug lạ vẫn hiển thị state thật và có thể cho 0 kết quả, không tự bỏ/đổi ý nghĩa query.

8 tổ hợp MVC kiểm 0–3 filter, q/sort/page/size không cộng, whitespace không cộng, và links giữ query. Chrome kiểm xóa đủ5 chip riêng lẻ, clear-all, refresh, Back/Forward, no-JS, Space/Enter/touch mở panel và nút Áp dụng gửi form rõ ràng; đổi select không tự điều hướng. Count3 vẫn đọc được khi panel đóng ở cả20 tổ hợp viewport/zoom/JS. [Tương tác và AX](../ui/screenshots/byredo-inspired/B2/interaction-checks.json)

### B2.3 — C18: tiến độ Page

Với Page có phần tử: `X=1L*number*size+1`, `Y=min(X+numberOfElements-1,totalElements)`, Z=totalElements. Dùng phép nhân long để tránh tràn int. Total0 hiện “0 sản phẩm”; Page rỗng nhưng total dương hoặc offset không còn trong total hiện thông điệp “Không có sản phẩm ở trang này…” thay vì bịa1–0 hoặc range vượt tổng. Controller cũ đọc lại trang cuối khi page ngoài vùng; URL yêu cầu vẫn có thể là page99 nhưng nội dung/pager dựa Page server trả về, không sửa contract.

Maven kiểm tổng0/1/12/27, trang đầu/giữa/cuối, page99 và Page rỗng/tổng dương. Chrome kiểm tổng0/1/12/27, 1–12/27, 13–24/27, 25–27/27, trang99 về nội dung trang cuối; query sau lọc còn1. Next/Previous hoạt động cả keyboard/no-JS, trang đầu không có Previous, trang cuối không có Next. Không sửa fragment pagination dùng chung hoặc thêm AJAX/aria-live trên grid.

### Kết quả kiểm thử và môi trường

Maven3.9.11, Temurin **JDK21.0.13**, `P01_MARIADB_TEST=false`.

| Lượt | PASS | FAIL | ERROR | SKIPPED |
|---|---:|---:|---:|---:|
| Baseline GSAP, lịch sử | 61 | 0 | 0 | 34 |
| MVC sau C15 | 34 | 0 | 0 | 0 |
| MVC sau C12 | 42 | 0 | 0 | 0 |
| MVC sau C18 | 50 | 0 | 0 | 0 |
| **Toàn suite cuối, 115 test** | **81** | **0** | **0** | **34** |

`mvn test` và `mvn -DskipTests package` offline đều BUILD SUCCESS. JAR `target/beautyshop-0.0.1-SNAPSHOT.jar`; node --check app.js và git diff --check đạt. Tổng được đối chiếu log và XML của đúng20 lớp đã chạy, không tính XML cũ còn trong target. [Maven summary](../ui/screenshots/byredo-inspired/B2/maven-summary.json) · [Test log](../ui/screenshots/byredo-inspired/B2/tests.log) · [Package log](../ui/screenshots/byredo-inspired/B2/package.log)

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:Path="$env:JAVA_HOME/bin;$env:Path"
$env:P01_MARIADB_TEST='false'
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package
node --check src/main/resources/static/js/app.js
git diff --check
```

Chrome Headless **158.0.8127.36**, Windows, profile QA riêng và CDP trực tiếp theo chỉ định owner. Zoom thật qua settingsPrivate 100%/200%, xác nhận devicePixelRatio1/2. Không dùng CSS zoom/transform. Tomcat fixture chạy MVC/Thymeleaf/Security/service thật, repository bộ nhớ, DataSource/JPA/Flyway bị loại. Các query qa-one/qa-twelve/qa-zero/qa-all chỉ là kịch bản repository mock trong fixture, không thêm endpoint hoặc logic vào backend production.

| Bộ browser | Kết quả cuối |
|---|---|
| Baseline trước B2 | 3 tổ hợp (1366/100%,390/100%,390/200%), 6 PNG, cùng dữ liệu với bản sau. Dùng bản sao template/assets trước B2, không ghi đè working tree để chụp. |
| Catalog | **160/160 bản ghi** =5 viewport ×2 zoom ×2 JS ×8 trạng thái. Đủ empty/single/12/27, trang đầu/giữa/cuối/out-of-range, nhiều filter và sáu loại URL ảnh. scrollWidth==clientWidth;0 Runtime.exceptionThrown. |
| Tương tác bổ sung | **20/20 tổ hợp**: summary count đóng, Space/Enter/touch, đổi sort không submit, Áp dụng, thứ tự server tăng345.000→355.000→395.000, Next/Previous và link biên. AX kiểm tên link ảnh tốt/lỗi. |
| Regression B1/U2 | **100/100 tổ hợp**, 30 kiểm tra luồng: Guest/Customer/Admin, menu cấp con, actions/current, badge0→1→5→12, login giữ hai SKU/12 đơn vị/4.425.000 ₫, footer, resize, keyboard/touch/no-JS.0 JS exception/tràn ngang. |
| MariaDB | **34 SKIPPED** trong Maven; không có DB test riêng được xác nhận. Không POST tới beautyshop, không seed/migration hoặc đặt đơn. |

Đã giữ nguyên kết quả C01/C02/C05/C06/C07/C08/C28, chiều cao header109/113/116px ở100%,226px tại mobile hẹp200%; không thêm GSAP vào Catalog. Source vendor SHA-256 vẫn `3D1BEA374AB1B703CC5C4105358EEC63C4465EFDC573931DC116E34A0A190C1B`; adapter vẫn `E804B020D6FCDA54A2617D7552A68A7FBEFA859EFFDD7A214B2E377D34FFE88C`. Home/GSAP và hai báo cáo công nghệ/GSAP không thuộc diff B2.

### Bằng chứng trước/sau

178 PNG: 6 baseline +80 catalog +20 tương tác +72 regression. Mỗi JSON catalog có URL, viewport, zoom, JS state, count/range và trạng thái img/fallback. Các ảnh vùng lưới/kết quả đã cuộn tới vùng được nêu; ảnh filter-count chụp summary khi đóng. Không có so sánh pixel trực tiếp với Byredo.

| Trạng thái | Trước B2 | Sau B2 |
|---|---|---|
| Card,1366×768/100% | [Trước](../ui/screenshots/byredo-inspired/B2/before-cards-1366-100.png) | [Fallback](../ui/screenshots/byredo-inspired/B2/after-cards-1366-100-js.png) |
| Card,390×844/100% | [Trước](../ui/screenshots/byredo-inspired/B2/before-cards-390-100.png) | [Fallback](../ui/screenshots/byredo-inspired/B2/after-cards-390-100-js.png) |
| Bộ lọc,390×844/200% | [Trước](../ui/screenshots/byredo-inspired/B2/before-filters-390-200.png) | [Chip/progress](../ui/screenshots/byredo-inspired/B2/after-filters-390-200-js.png) · [Count khi đóng](../ui/screenshots/byredo-inspired/B2/filter-count-390-200-js.png) |
| Trang cuối/empty | — | [25–27/27](../ui/screenshots/byredo-inspired/B2/after-last-1366-100-js.png) · [Rỗng](../ui/screenshots/byredo-inspired/B2/after-empty-390-100-js.png) |
| No-JS | — | [Card](../ui/screenshots/byredo-inspired/B2/after-cards-390-200-nojs.png) · [Count khi đóng](../ui/screenshots/byredo-inspired/B2/filter-count-390-200-nojs.png) |

[160 lượt đo](../ui/screenshots/byredo-inspired/B2/measurements.json) · [HTTP404/decode/cached failure và GET/history](../ui/screenshots/byredo-inspired/B2/checks.json) · [20 tương tác/AX](../ui/screenshots/byredo-inspired/B2/interaction-checks.json) · [Regression100](../ui/screenshots/byredo-inspired/B2/regression/measurements.json) · [Regression luồng](../ui/screenshots/byredo-inspired/B2/regression/checks.json).

### Giới hạn và kết luận

**F01 vẫn BLOCKED.** Dataset public/ảnh thương phẩm thật và giao dịch trên MariaDB chưa có bằng chứng mới. F14 cải thiện fallback/loading/ratio trên catalog; pipeline ảnh responsive/budget còn PARTIAL. C27 báo giá voucher vẫn PARTIAL, C21 media vẫn FAIL/BLOCKED; C13/C17/C20/C29/C30 giữ trạng thái trước. Chưa chạy screen reader, thiết bị cảm ứng vật lý, axe/Lighthouse, đo CLS/LCP/INP hoặc CSP production trong B2. Kiểm hình học/khung4:5 không đồng nghĩa có số CLS mới.

Hai lỗi khởi tạo fixture (constructor/nested Mockito stubbing) đã sửa trong mã QA dưới target, không chạm entity/app backend. Một expected trong lượt tương tác ban đầu dự kiến2 sản phẩm thay vì3 dòng CeraVe dưỡng ẩm thực có trong CSV; đã đối chiếu nguồn và sửa test, không sửa kết quả server cho khớp kỳ vọng. Kết quả công bố là lượt cuối hoàn tất, không nâng PASS từ lượt dở dang.

Chỉ năm file source nêu trên đổi so với snapshot, các file khác giữ hash; bảo toàn mọi thay đổi chưa commit. [Kiểm chứng phạm vi và GSAP](../ui/screenshots/byredo-inspired/B2/preservation.json). Rollback bằng đúng các hunk B2 hoặc snapshot đầu kỳ của năm file, không checkout cả file về HEAD và không reset/clean. **B2 hoàn tất ở lớp UI/MVC/fixture và dừng; không triển khai B3/B4, checkout hoặc commit/push.**

<a id="b4"></a>

## B4 · Luxury Checkout / C27 / F07 · 23/11/2026

**C27 PARTIAL → PASS trong phạm vi UI/MVC/fixture.** Tổng **24/30 =80%**, core **12/12**. Cổng MariaDB/F01, gateway/IPN thật và production độc lập với coverage này. B3 chưa triển khai; C13/C17/C20/C21 không đổi trạng thái. GSAP C19 giữ nguyên, vẫn được nghiệm thu kỹ thuật có điều kiện theo quyết định owner trước B2.

### Baseline và nguyên nhân F07

Đã đọc toàn bộ prompt B4 và phần B2 mới nhất, AGENTS, README/SRS/Business Rules/ERD/Decisions, Design System, tài liệu U2/U3–U8 liên quan checkout, audit F07, GSAP và source/test checkout/voucher/order/security/MoMo. Source of truth là GET /checkout, POST /checkout/quote và POST /checkout; không có lớp Checkout_Service, checkout dùng DonHang_Service cùng PhieuGiamGia_Service và ShippingFee_Policy.

Đầu kỳ lưu Git inventory/branch/HEAD/diff, SHA-256 source và snapshot trong target/p15-b4. Nhánh main/HEAD837e550 vẫn có nhiều thay đổi chưa commit. **Baseline chạy lại JDK21:115 test =81 PASS +0 FAIL +0 ERROR +34 SKIPPED**, khớp B2.

F07 đã tái hiện **qua controller/view thật** trước khi sửa trên cùng fixture: giỏ2×245.000=490.000 ₫, SAVE50 giảm50.000, phí25.000, tổng465.000 ₫. Khi đặt COD gặp ConcurrencyFailureException, controller cũ gọi hienCheckout→themBaoGia(...,ZERO); input vẫn SAVE50 nhưng giảm0 và tổng515.000 ₫. Đã chụp3 tổ hợp trước (1366/100%,390/100%,390/200%), gồm6 ảnh valid/error. [Before JSON](../ui/screenshots/byredo-inspired/B4/before.json)

### B4.1 — State báo giá, giữ contract và ranh giới backend

Controller chỉ sửa **dựng model, validation hiển thị và thông điệp lỗi** trong phạm vi prompt cho phép. Không sửa service/repository/transaction hoặc quy tắc tính tiền.

| Trạng thái response | Model/UI hiện tại |
|---|---|
| GET mới, chưa xác nhận | tamTinh lấy từ giỏ server; baoGiaHopLe=false. Không dựng/hiện giam,phi,tong như khoản đã xác nhận. Hướng dẫn “Cần cập nhật báo giá”. |
| Quote hợp lệ | Chỉ khi DTO hợp lệ, giỏ không rỗng, địa chỉ nằm trong danh sách của Customer và xemTruoc thành công mới đặt baoGiaHopLe=true. Giảm/phí/tổng vẫn dùng hàm tính BigDecimal và ShippingFee_Policy cũ. |
| Không voucher hoặc giảm thật0 | Vẫn là quote hợp lệ nếu server trả thành công; hiển thị0 theo kết quả server, không gắn nhãn ưu đãi/“đã áp dụng” tự tạo. Service test xác minh1% của1₫ làm tròn xuống0 là nhánh có thật trong code. |
| Voucher sai/hết hạn/hết lượt/chưa đủ điều kiện | Giữ input để sửa; lỗi field voucher có aria-invalid và error link. Không hiện một giảm giá0/tổng có vẻ hợp lệ. Điều kiện thời gian/lượt/minimum vẫn do code service/repository cũ quyết định. |
| Quote conflict/state error | Global error ngắn gọn, yêu cầu cập nhật lại; không có tổng đã xác nhận. Lỗi hạ tầng ngoài các exception đã xử lý không bị che thành thành công. |
| Submit lỗi validation/stock/voucher/conflict | Dựng lại giỏ/địa chỉ như trước, baoGiaHopLe=false; giữ mã nhưng **không render** khoản giảm/phí/tổng chưa xác nhận. Không tự dùng quote cũ hoặc tính lại sai0. |
| COD thành công | Giữ redirect /orders/{id} và flash “Đã đặt đơn COD.” |
| MoMo đã tạo đơn nhưng chưa có URL gateway | Giữ redirect về đơn và flash chờ thanh toán/đối soát, không thông báo đã thu tiền. |

POST quote vẫn trả view200 như trước; không thêm quote token/session, AJAX hoặc PRG mới. POST checkout tiếp tục chuyển đúng datCOD/datMomo. URL/method/addressId/voucher/payment/CSRF không đổi. Cờ model không phải field form được tin cậy.

Nút đặt hàng disabled khi chưa có quote là hướng dẫn UI. **Không dùng disabled để thay bảo vệ server:** direct POST không cần mang cờ quote, service hiện có vẫn kiểm quyền địa chỉ, giá SKU/tồn/voucher và tính lại tổng. MVC gửi thêm price/tong/baoGiaHopLe từ client vẫn chỉ gọi service bằng addressId/voucher; service thật với repository doubles dùng giá SKU245.000 thay giá giỏ cũ1 để chốt490.000/50.000/25.000/465.000. Không sửa logic này và không cần vượt STOP gate sang nghiệp vụ.

### B4.2 — Giao diện và khả năng truy cập

Checkout có tiến trình Giỏ→Giao hàng và thanh toán→Hoàn tất, tiêu đề trung lập, desktop hai cột (địa chỉ/voucher và tóm tắt), một cột ở≤900px. Không sticky hoặc che focus. VND/typography/màu/spacing giữ U1; CSS chỉ thêm selector checkout. Quote action secondary, COD primary56px, MoMo secondary56px khi cấu hình cho phép. Không thêm phương thức hoặc field thanh toán mới.

Snapshot hợp lệ ghi **“Báo giá lần tính gần nhất”**, mã/không voucher và **địa chỉ báo giá cụ thể** từ model server. Input còn mã không phải dấu hiệu đã áp dụng. Thông điệp nhắc cập nhật khi đổi địa chỉ/voucher và server luôn kiểm lại khi đặt đơn.

checkout.js chỉ tải ở checkout: khi sửa input/select sau quote, ẩn phần tiền cũ, hiện trạng thái cần cập nhật, khóa CTA đặt hàng; không tính tiền hoặc gửi request. Lỗi server focus về error summary; link lỗi đưa tới field, field gắn aria-describedby/aria-invalid. Address type-mismatch dùng thông điệp tiếng Việt trong view, không lộ chuỗi lỗi chuyển kiểu của framework. Empty cart/thiếu địa chỉ có lối về giỏ/thêm địa chỉ; không hiện CTA giao dịch.

**No-JS:** form, quote và đặt COD/MoMo đều chạy native. Snapshot vẫn ghi mã/địa chỉ ở lần tính gần nhất. Đã thử đổi mã sau quote rồi submit trực tiếp khi JS tắt: server từ chối, giữ mã lỗi và render trạng thái chưa xác nhận. JS không phải nguồn quyết định giá hay quyền submit tại backend.

### File sửa và bảo toàn B2/GSAP

| File | Thay đổi B4 |
|---|---|
| [Checkout_Controller.java](../../src/main/java/com/beautyshop/controller/Checkout_Controller.java) | Quote state trong model, kiểm giỏ/địa chỉ từ dữ liệu đã đọc cho preview, thông báo quote failure. Giữ nguyên nhánh đặt đơn/redirect và hàm tính tiền. |
| [order/checkout.html](../../src/main/resources/templates/order/checkout.html) | Layout, snapshot quote, lỗi, trạng thái empty và action hierarchy; giữ form contract. |
| [storefront.css](../../src/main/resources/static/css/storefront.css) | Chỉ selector checkout, min-width0/reflow và tiền dài; không đổi token/CSS catalog/header/footer. |
| [checkout.js](../../src/main/resources/static/js/checkout.js) | File mới: focus lỗi và invalidation trình bày khi sửa field; không thêm listener submit. app.js giữ nguyên. |
| [CheckoutPagesTest.java](../../src/test/java/com/beautyshop/controller/CheckoutPagesTest.java) |29 ca MVC với controller/Thymeleaf/Security thật: state/F07, DTO/ownership view, money, CSRF/role, COD/MoMo/redirect/pending. |
| [CheckoutIntegrityTest.java](../../src/test/java/com/beautyshop/service/CheckoutIntegrityTest.java) |8 ca chạy DonHang_Service/PhieuGiamGia_Service thật với repository doubles, không JDBC/gateway/transaction thật. |
| Hai báo cáo P15 và [bằng chứng B4](../ui/screenshots/byredo-inspired/B4/) | Coverage, before/after, fixture, log/JSON, giới hạn. |

187 file source đầu kỳ→190 cuối kỳ:3 file cũ đổi và3 file mới;184 file cũ giữ hash. B2 templates/app.js/tests, Home/GSAP, header/footer/head/ui pagination, toàn bộ service/repository/entity/Flyway/security/DTO/pom.xml/Design System không thuộc diff. Báo cáo GSAP và frontend technology giữ hash. Không reset/clean/stash/commit/push hoặc ghi beautyshop. [Preservation](../ui/screenshots/byredo-inspired/B4/preservation.json)

### B4.3 — Test Maven JDK21 và JAR

Maven3.9.11, Eclipse Temurin **21.0.13**, P01_MARIADB_TEST=false.

| Lượt | Tổng | PASS | FAIL | ERROR | SKIPPED |
|---|---:|---:|---:|---:|---:|
| Baseline B4 chạy lại |115|81|0|0|34|
| MVC checkout riêng |29|29|0|0|0|
| Toàn suite cuối |**152**|**118**|**0**|**0**|**34**|

Trong suite cuối có8 ca service thật với doubles: giá SKU hiện hành/discount/ship, preview không consume, voucher hết hiệu lực ở lần đặt, địa chỉ khác chủ, giỏ rỗng, tồn giảm, MoMo OFF, online reserve/giữ lượt đúng nhánh. Đây **không phải kiểm chứng transaction/lock/rollback/idempotency MariaDB**.22 lớp chạy được đối chiếu log/XML; không tính báo cáo XML cũ trong target. [Maven summary](../ui/screenshots/byredo-inspired/B4/maven-summary.json) · [Test log](../ui/screenshots/byredo-inspired/B4/tests-final.log) · [Baseline log](../ui/screenshots/byredo-inspired/B4/baseline-tests.log)

```powershell
$env:JAVA_HOME='C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:Path="$env:JAVA_HOME/bin;$env:Path"
$env:P01_MARIADB_TEST='false'
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package
node --check src/main/resources/static/js/checkout.js
git diff --check
```

Package JAR **BUILD SUCCESS** tại target/beautyshop-0.0.1-SNAPSHOT.jar; kiểm entry checkout/template/JS và vendor/adapter GSAP khớp source. [Package log](../ui/screenshots/byredo-inspired/B4/package-final.log) · [JAR assets](../ui/screenshots/byredo-inspired/B4/jar-assets.json). Log xuất vào docs đã ẩn mật khẩu test tạm tự sinh của Spring, không xuất CSRF token/payload bí mật.

### Chrome Headless và hồi quy

Chrome **158.0.8127.36**, Windows, profile riêng, CDP trực tiếp theo yêu cầu owner. Zoom browser thật100%/200%, xác minh DPR1/2; không CSS zoom/transform.20 tổ hợp gồm5 viewport360×800,390×844,768×1024,1366×768,1920×1080 ×2 zoom ×JS on/off (20 khi tính cả hai nhóm JS). Mỗi tổ hợp chạy16 trạng thái, tổng **320 bản ghi checkout**, không cộng vào test Maven.

| Bộ kiểm | Kết quả thực tế cuối |
|---|---|
| Checkout320 | GET chưa quote; không voucher; valid SAVE50; sửa quote JS hoặc no-JS direct-submit mã sai; F07 order conflict; requote; invalid/minimum; quote-read failure; địa chỉ khác chủ; COD success fixture; MoMo ON mock/redirect/pending gateway; empty/no address. **320/320**, không horizontal overflow/Runtime.exceptionThrown, CTA≥56px. |
| Keyboard/role | Tab/Shift+Tab giữa voucher/quote, outline3px, Enter gửi đúng action, error link focus đúng field, touch quote. Guest checkout→login theo saved request, Admin checkout403 và không có nút giao dịch. |
| Submit nhanh | Hai Enter cách80ms dưới độ trễ fixture800ms: **1 POST COD,1 POST MOMO**. Submit event probe ghi loading/aria-busy, kích thước không đổi; lượt hai bị preventDefault, payload giữ payment=MOMO khi có và CSRF. Không suy diễn idempotency server. |
| B1/U2 regression |**100/100 tổ hợp**,30 checks Guest/Customer/Admin: menu/current/actions/badge/footer/resize/keyboard/touch/no-JS; hai SKU/12 đơn vị/4.425.000 ₫ vẫn giữ sau login. |
| B2 regression |**160/160 lượt catalog**,27 checks; count/chips/range/fallback/GET/history/no-JS giữ nguyên,0 tràn/JS exception. Lưu kết quả mới trong B4, không ghi đè ảnh B2. |
| Home/GSAP |**10/10** viewport/zoom; reduce giữ tĩnh, no-preference chạy đúng3 phần trang trí380ms rồi revert. Home không tải checkout.js; checkout/catalog không thêm GSAP. Vendor/adapter giữ SHA-256 cũ. |

[320 measurements](../ui/screenshots/byredo-inspired/B4/measurements.json) · [Checks/role/contract](../ui/screenshots/byredo-inspired/B4/checks.json) · [Rapid Enter](../ui/screenshots/byredo-inspired/B4/rapid-checks.json) · [Regression100](../ui/screenshots/byredo-inspired/B4/regression/measurements.json) · [B2 regression160](../ui/screenshots/byredo-inspired/B4/catalog-regression/measurements.json) · [Home/GSAP](../ui/screenshots/byredo-inspired/B4/home-regression.json)

Fixture dùng MVC/Thymeleaf/Security/controller thật và voucher calculation thật với repository mock. Loại DataSource/JPA/Flyway; không tạo service gateway thật. Order service trong browser là double có kiểm địa chỉ/config, fault injection conflict và snapshot trạng thái phù hợp source: COD PENDING_CONFIRMATION/UNPAID, MoMo PENDING_PAYMENT/PENDING. MoMoFacade double redirect tới **controller/trang mock thật**, có nhãn MÔ PHỎNG-KHÔNG THU TIỀN. Không bấm kết quả mock để tuyên bố thanh toán đã được xác nhận.

MoMo non-mock ON được kiểm **MVC label/contract/redirect-only** tới example.invalid, không theo URL và không gọi Sandbox. Browser kiểm OFF và ON mock. Không gọi gateway/IPN thật hoặc xác nhận merchant. Khi gateway mock không có URL, controller trả về đơn pending và thông điệp chờ đối soát đúng source.

### Ảnh trước/sau và log

262 PNG:6 before +100 checkout after (5 trạng thái ×20 tổ hợp) +72 navigation regression +80 catalog regression +4 Home. Ảnh vùng summary/error đã cuộn tới vùng ghi trong tên; các phần còn lại kiểm bằng cuộn/keyboard, không được che bởi thanh sticky.

| Trạng thái | Trước | Sau |
|---|---|---|
| Quote valid,1366/100% | [Trước](../ui/screenshots/byredo-inspired/B4/before-valid-1366-100.png) | [Sau](../ui/screenshots/byredo-inspired/B4/after-valid-1366-100-js.png) |
| F07 lỗi,1366/100% | [Voucher còn nhưng giảm0/tổng515.000](../ui/screenshots/byredo-inspired/B4/before-F07-1366-100.png) | [Chưa xác nhận, không hiện tổng sai](../ui/screenshots/byredo-inspired/B4/after-F07-1366-100-js.png) |
| F07 mobile390/100% | [Trước](../ui/screenshots/byredo-inspired/B4/before-F07-390-100.png) | [Sau](../ui/screenshots/byredo-inspired/B4/after-F07-390-100-js.png) |
| Mobile390/200% | [Trước](../ui/screenshots/byredo-inspired/B4/before-F07-390-200.png) | [Valid](../ui/screenshots/byredo-inspired/B4/after-valid-390-200-js.png) · [Invalid](../ui/screenshots/byredo-inspired/B4/after-invalid-390-200-js.png) |
| No-JS/MoMo mock | — | [No-JS valid390/100%](../ui/screenshots/byredo-inspired/B4/after-valid-390-100-nojs.png) · [MoMo mock1366/100%](../ui/screenshots/byredo-inspired/B4/after-momo-1366-100-js.png) |

Trong lúc dựng harness, đã sửa giả định login luôn về/ (luồng thật dùng saved request) và cách lấy loading sau navigation. Loading cuối được đo ngay trong submit event, thay vì query DOM đã rời trang. Một probe Fetch-paused thử nghiệm không dùng làm bằng chứng PASS. Các số công bố là lượt hoàn tất và assertion thực, không tính lượt dở dang là pass.

### Giới hạn, cổng nghiệm thu và dừng

**F07 đã xử lý ở UI/controller view-state**, có before/after qua POST thật trên fixture. **F01 vẫn BLOCKED**,34 MariaDB integration vẫn SKIPPED vì chưa có database test độc lập được duyệt. Không kiểm giao dịch COD lưu bền, khóa/rollback/concurrency/idempotency thật, merchant/IPN/MoMo Sandbox thật, production CSP/load, RUM/Lighthouse hoặc screen reader/thiết bị cảm ứng vật lý. Full transaction gate không được thay bằng fixture.

CSS/native labels/error semantics, keyboard/no-JS và reflow đã kiểm; không tuyên bố chứng nhận WCAG2.2 AA toàn site. CSP/Spring Security không sửa. Bộ test service không có TransactionManager/JDBC nên không khẳng định độ nguyên tử. Fixture/mock tokens/credentials không dùng cho production; helper đã đóng và credential Admin tạm được xóa sau QA.

Coverage **24 PASS +2 PARTIAL(C29/C30) +4 FAIL(C13/C17/C20/C21)=30**, core12/12 ở lớp UI/MVC/fixture. C21 vẫn BLOCKED media có quyền, F14 pipeline ảnh còn PARTIAL như B2. Đạt80% component **không đồng nghĩa P15 production-ready** hoặc F01 đã hết.

Rollback chỉ các hunk B4 trong controller/checkout/CSS và ba file mới checkout.js/CheckoutPagesTest/CheckoutIntegrityTest theo snapshot đầu kỳ; không checkout nguyên file về HEAD vì có U1–U2 chưa commit. Giữ B2/GSAP và các thay đổi khác. **Dừng sau B4 để owner nghiệm thu; không triển khai B3, không commit/push.**
