# BeautyShop — P12/U2, giao diện mua sắm

[U2.2 — Trang chi tiết sản phẩm](#u22)

[U2.3 — Giỏ hàng](#u23)

[U2.3 — Kiểm chứng ảnh, thương hiệu và liên kết SKU](#u23-du-lieu)

## U2.1 — Trang chủ và catalog

Ngày 22/11/2026, múi giờ Asia/Saigon. Bản gốc P12 hiện ở thư mục gốc của repo. Đợt này kế thừa nền tảng U1 trong working tree và dừng ở U2.1. [Prompt P12](../../P12-luxury-ui-upgrade.md) · [Prompt U2](../prompts/P12-U2-luxury-storefront.md) · [Audit frontend](frontend-audit-2026-11.md) · [Kết quả U1](ui-upgrade-2026-11.md)

## Thay đổi và hợp đồng

- Trang chủ có hero editorial với minh họa CSS, CTA đến catalog và lối vào các bộ lọc danh mục, thương hiệu, loại da. Mục sản phẩm mới dẫn đến truy vấn `sort=newest` hiện có vì controller trang chủ không cấp dữ liệu catalog. Nội dung bỏ nhãn đang phát triển và dẫn đến kênh tư vấn hiện có. Điều kiện yêu cầu đổi trả căn cứ BR17. [Trang chủ](../../src/main/resources/templates/home/index.html) · [Business Rules](../requirements/02-business-rules.md)
- Catalog dùng bộ lọc bên trái ở màn hình rộng, thu gọn bằng details trên mobile, và vẫn truy cập được form khi tắt JS. Mỗi chip bỏ một tiêu chí, giữ các tiêu chí còn lại, thứ tự và size trừ khi bỏ chính sort, rồi về trang đầu. Số kết quả lấy từ `Page.totalElements`. Hai trạng thái rỗng phân biệt catalog chưa có sản phẩm hiển thị và bộ lọc không có kết quả. [Danh sách sản phẩm](../../src/main/resources/templates/product/list.html)
- Card dùng ảnh SKU từ model với khung 4:5, lazy loading và object-fit contain. Khi model không cấp ảnh, dùng monogram BeautyShop trung tính. Giá dùng fragment VND U1. Nhãn còn/hết hàng dùng tồn khả dụng của biến thể khớp truy vấn. Danh mục từng card không có trong projection, nên placeholder dùng một nền trung tính. [Thẻ sản phẩm](../../src/main/resources/templates/fragments/product-card.html)
- CSS dùng token U1, thu gọn khoảng cách menu tại hai trang trên mobile và giữ các mục điều hướng hiển thị. JS thêm xử lý gập bộ lọc và mở theo đích danh mục, thương hiệu, loại da. [CSS storefront](../../src/main/resources/static/css/storefront.css) · [JS dùng chung](../../src/main/resources/static/js/app.js)
- Test HTTP mô phỏng kiểm link, số lượng, định dạng VND, trạng thái rỗng, escaping, chip và phân trang. Test trang chủ cũ cập nhật expected heading theo nội dung mới. [BootstrapPagesTest](../../src/test/java/com/beautyshop/controller/BootstrapPagesTest.java) · [GiaoDienPagesTest](../../src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java)
- Tài liệu cập nhật tiến độ, liên kết từ báo cáo U1 và bổ sung ảnh ở thư mục QA. [Roadmap](../03-roadmap-and-team.md) · [Ảnh U2.1](../ui/screenshots/U2.1/) · [Checklist](../ui/QA-checklist.md)

So sánh source xác nhận form catalog vẫn dùng `GET /products` và các field `q`, `brand`, `category`, `skinType`, `sort`. Link phân trang giữ các giá trị này cùng `page` và `size`. Link card trỏ `/products/{slug}`. Java ứng dụng, database, security, CSRF và các quy tắc giá, tồn, phí, voucher, đơn và thanh toán tiếp tục dùng bản hiện hành. Đợt này chỉ sửa Java trong test.

## Phát hiện liên quan

S là source hoặc kết quả MockMvc. H là HTTP trên ứng dụng đang chạy. B là trình duyệt chạy HTML Thymeleaf từ fixture.

| Mã | Trạng thái U2.1 | Bằng chứng |
|---|---|---|
| F01 | Blocked | H. Endpoint `http://localhost:8080/products` trả 200 với 0 card và đang chạy bản U1. Schema/profile/dataset demo chưa được xác nhận. S/B kiểm catalog rỗng và catalog có dữ liệu mock. |
| F04 | Đã xử lý trong catalog | S/B. Form kế thừa viền control U1 `#8A766F` trên trắng đạt 4.28:1. Chrome kiểm chiều cao control lọc khi mở đúng đích. |
| F06 | Đã giữ từ U1 tại hai trang | S/B. Có đích main ở cả hai trang. Đã kiểm Tab vào skip link và Enter đưa focus vào main tại catalog. |
| F09 | Đã xử lý tại hai trang | S/B. MockMvc xác nhận `aria-current="page"` đúng mục. Chrome hiển thị dấu gạch dưới của mục đang chọn. |
| F14 | Một phần | S/B. Card giữ vùng 4:5 và có placeholder trung tính. Ảnh nhiều kích thước và tối ưu pipeline nằm ngoài P12. |
| F16 | Một phần | S. Trang chủ dẫn đến `/chat` và nêu điều kiện yêu cầu đổi trả theo BR17. Chưa có route chính sách hoặc liên hệ riêng trong repo. |
| F17 | Một phần | S. Bỏ CSS hero và card cũ trong lát cắt, dùng lại token/control U1. |
| F18 | Giữ formatter catalog | S. MockMvc kiểm `1.234.567 ₫`. Format giá SKU Admin thuộc U4. |
| F20 | Một phần | S/B. Bỏ copy phát triển và bổ sung lối vào catalog theo nhu cầu. Danh sách tên danh mục, thương hiệu và sản phẩm mới nằm ở catalog vì route trang chủ chưa có model tương ứng. |
| F22 | Đã giữ từ U1 | S/H/B. Head chung khai báo SVG local. Endpoint U1 đang chạy trả 200 cho tài nguyên này. |

Các mã F02, F03, F05, F07, F08, F10–F13, F15, F19, F21, F23 và F24 ngoài phạm vi thay đổi U2.1. Dùng bảng U1 để tra trạng thái trước đợt này.

## Kiểm thử và bằng chứng UI

Maven 3.9.11 dùng Temurin 21.0.13. Baseline U1 có 63 test được Surefire liệt kê, 29 thành công, 34 bỏ qua, 0 lỗi. Test tập trung BootstrapPagesTest và GiaoDienPagesTest có 15 test thành công.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
mvn.cmd -version
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package
node --check src/main/resources/static/js/app.js
git diff --check
```

Kết quả suite cuối `Tests run: 65, Failures: 0, Errors: 0, Skipped: 34`. **31 test thành công**, 34 test MariaDB bỏ qua do không bật `P01_MARIADB_TEST`. Đóng gói JAR, kiểm cú pháp JS và diff đều thành công. Rà source hai trang xác nhận mỗi trang có một h1, đích skip link và không có CDN, inline style/script/event, th:utext hoặc innerHTML. MockMvc kiểm escaping của từ khóa ở input và chip, escaping tên sản phẩm và tính bảo toàn query ở link bỏ lọc/phân trang.

Trình duyệt tích hợp không có phiên kết nối. Chrome Headless 158.0.8127.36 đã chạy lúc 03:08–03:09 (UTC+7) trên máy Windows, vai trò Guest, ở local fixture `http://127.0.0.1:53118`. Thymeleaf render HTML bằng các controller thật và dịch vụ mock, sau đó máy chủ fixture phục vụ HTML và CSS/JS U2.1. Ảnh catalog có dữ liệu mang nhãn kiểm thử. Tất cả 15 tổ hợp trang/trạng thái và viewport có `scrollWidth == clientWidth`. Nút `.btn` đang hiển thị đo được tối thiểu 50.8 px, CTA chính 56 px và nằm trong viewport đầu ở 360/390 px. Khung ảnh có dữ liệu đo xấp xỉ 4:5. Tại catalog mobile, đã dùng Tab, Shift+Tab và Enter để kiểm skip link và bộ lọc. Ba đích lọc mở đúng panel. Khi tắt JS, form GET vẫn hiện. Console trong kịch bản kiểm không ghi lỗi.

| Ảnh | URL fixture | Viewport |
|---|---|---|
| `home-*` | `/` | 360×800, 390×844, 768×1024, 1366×768, 1920×1080 |
| `catalog-empty-*` | `/products` | Cùng năm viewport |
| `catalog-fixture-*` | `/qa/catalog` | Cùng năm viewport |
| `catalog-cards-1366x768.png` | `/qa/catalog`, cuộn đến kết quả | 1366×768 |

[Trang chủ mobile](../ui/screenshots/U2.1/home-360x800.png) · [Trang chủ desktop](../ui/screenshots/U2.1/home-1366x768.png) · [Catalog rỗng](../ui/screenshots/U2.1/catalog-empty-390x844.png) · [Thẻ sản phẩm kiểm thử](../ui/screenshots/U2.1/catalog-cards-1366x768.png)

## Nghiệm thu và khôi phục

NOT VERIFIED cho U2.1 trên dev MariaDB với dataset thật, phiên Customer/Admin, zoom trình duyệt 200%, trình đọc màn hình, axe/Lighthouse và Core Web Vitals. Luồng lọc/phân trang được kiểm ở controller và HTML render. Fixture browser là ảnh chụp bố cục và kiểm tương tác tại chỗ, không tái tạo truy vấn catalog theo link. Còn cần xác nhận endpoint demo có dữ liệu công khai để mở F01. [Checklist QA](../ui/QA-checklist.md)

Muốn khôi phục riêng U2.1, hoàn nguyên các phần hero/discovery trong home, bố cục/filter/card trong hai template catalog, CSS scoped tương ứng và block catalog trong app.js về nội dung U1. Hoàn nguyên các assertion U2.1, dòng roadmap và ảnh/báo cáo U2.1 cùng lượt. Working tree đã có U1 trước khi làm, nên tránh khôi phục toàn bộ các file này về Git HEAD.

U2.1 đã triển khai và có bằng chứng kiểm thử kể trên. Nghiệm thu với dữ liệu demo thật còn mở ở F01. Công việc dừng tại đây để chờ người dùng duyệt U2.1 trước khi quyết định U2.2.

<a id="u22"></a>

## U2.2 — Trang chi tiết sản phẩm

Ngày 22/11/2026. U2.2 đã đạt các điều kiện giao diện và hợp đồng HTTP trên môi trường kiểm thử độc lập. Nghiệm thu trên catalog MariaDB của bản demo **vẫn bị chặn bởi F01**. Điểm xuất phát là working tree đã có U1/U2.1 chưa commit. Prompt P12 được đọc từ thư mục gốc, nơi bản đặc tả hiện có trong repo. [P12](../../P12-luxury-ui-upgrade.md) · [Phạm vi U2.2](../prompts/P12-U2-luxury-storefront.md)

### File và hợp đồng đã kiểm

| File U2.2 | Thay đổi |
|---|---|
| [product/detail.html](../../src/main/resources/templates/product/detail.html) | Ảnh bên trái, thông tin mua bên phải trên desktop. Mỗi nút SKU hiển thị tên biến thể, mã, giá VND, tồn và nhãn “Đang chọn” cho lựa chọn hiện tại. Nút bán hết vẫn cho xem chi tiết. Thông tin lô, hạn dùng, mô tả, hướng dẫn, thành phần và loại da chỉ xuất hiện khi có dữ liệu tương ứng. |
| [storefront.css](../../src/main/resources/static/css/storefront.css) | Thay các quy tắc dành cho PDP cũ. Khung ảnh vuông giữ kích thước, focus dùng token U1, CTA nằm trong luồng cuộn. Ở chiều rộng rất nhỏ sau zoom, tên cửa hàng trong header/footer và các nút SKU vừa chiều ngang. |
| [app.js](../../src/main/resources/static/js/app.js) | Menu PDP thu gọn trên mobile, vẫn mở được bằng bàn phím. Ảnh SKU lỗi tải chuyển sang cùng placeholder với trường hợp thiếu URL. |
| [GiaoDienPagesTest.java](../../src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java) | Bổ sung bốn ca MVC cho SKU còn hàng, hết hàng, đánh giá đã escape và hành động của Customer/Admin. |
| [Roadmap](../03-roadmap-and-team.md), báo cáo này và [11 ảnh PNG](../ui/screenshots/U2.2/) | Tiến độ, điều kiện nghiệm thu và bằng chứng trước/sau trên cùng bộ dữ liệu kiểm thử. |

- Nút SKU gửi `GET /products/{slug}?variant={maBienThe}` bằng form HTML. Di chuyển focus qua các lựa chọn giữ nguyên URL. Khi kích hoạt, server chọn SKU và trả tên, giá, tồn cùng mã SKU dùng cho thao tác mua. Variant không hợp lệ về kiểu trả 400, ID không có trong danh sách SKU đang bán của sản phẩm trả 404. Trang lỗi tiếng Việt của U1 có lối quay lại catalog.
- Guest và Customer thêm một đơn vị qua `POST /cart/items`. Chrome ghi nhận payload gồm đúng `variant` và `_csrf`. Cùng request thiếu CSRF trả 403. Server redirect tới `/cart`, thông báo thành công có `role="status"`, lỗi thiếu tồn có `role="alert"`. Với SKU hết hàng, PDP hiển thị nút vô hiệu hóa và không render form mua. Customer giữ form `POST /account/wishlist/{id}`. Admin xem được PDP, không có form mua hoặc lưu yêu thích.
- Giá dùng fragment `ui :: money`, tồn dùng `soLuongKhaDung` của server. Thuộc tính như dung tích nằm trong tên biến thể do catalog cung cấp. Model chưa có trường công dụng hoặc danh sách đề xuất riêng. Đánh giá lấy từ `danhGiaCongKhai`. Bản sửa chỉ tác động lớp hiển thị và test, giữ nguyên controller, service, repository, quyền đánh giá sau mua, Flyway và schema.

### Bằng chứng trước và sau

Chrome Headless 158.0.8127.36 trên Windows chạy HTTP với Tomcat, Spring MVC, Thymeleaf, Spring Security, service chọn SKU và service giỏ thực. Repository được mô phỏng trong bộ nhớ, danh sách đánh giá của fixture rỗng. DataSource, JPA và Flyway bị tắt ở tiến trình fixture. Phiên Customer đăng nhập bằng form login của ứng dụng với tài khoản chỉ tồn tại trong bộ nhớ. Không chạy seed hoặc test ghi trên `beautyshop`.

Ảnh trước chụp PDP từ working tree ban đầu ở `http://127.0.0.1:59383/products/kiem-thu`. Ảnh sau chụp từ fixture cùng dữ liệu ở `http://127.0.0.1:60272/products/kiem-thu?variant=11` (còn 1) và `?variant=22` (hết). Các ảnh so sánh căn đầu vùng sản phẩm vào viewport. SKU 11 thiếu ảnh, SKU 22 dùng favicon của repo **chỉ trong fixture** để kiểm nhánh tải ảnh. Khi chặn tải ảnh bằng Chrome, JS hiện placeholder và tỷ lệ khung vẫn bằng 1.

| Nội dung | Trước | Sau |
|---|---|---|
| Desktop Guest 1366×768 | [Select và hai nút chính](../ui/screenshots/U2.2/before-1366x768.png) | [So sánh SKU và CTA mua](../ui/screenshots/U2.2/after-1366x768.png) · [SKU hết hàng](../ui/screenshots/U2.2/sold-out-1366x768.png) |
| Mobile Guest 390×844 | [PDP đầu kỳ](../ui/screenshots/U2.2/before-390x844.png) | [Ảnh và thông tin SKU](../ui/screenshots/U2.2/after-390x844.png) · [Cuộn tới vùng mua](../ui/screenshots/U2.2/purchase-390x844.png) |
| Phản hồi ở trang đích | | [Customer sau khi POST, /cart](../ui/screenshots/U2.2/customer-added-390x844.png) · [Focus tại CTA, zoom 200%](../ui/screenshots/U2.2/zoom-200.png) |

### Responsive và bàn phím

Ở zoom 100%, cả hai trạng thái SKU đã được kiểm tại cả năm viewport. Khung ảnh vuông ở cả 10 lượt. Nút mua cao 56 CSS px và chiếm trọn chiều rộng vùng mua trên mobile. Sau khi cuộn tới nút, kiểm tra phần tử ở tâm nút xác nhận không có thành phần nào che thao tác. Ở zoom trình duyệt thật 200%, đã đo lại SKU còn hàng tại cả năm kích thước và kiểm focus/điểm bấm CTA. Các cặp số ở cột cuối là `scrollWidth / clientWidth` tính bằng CSS px, đã trừ thanh cuộn dọc.

| Viewport | Cột ở 100% | CTA mua, rộng × cao | Chiều ngang 100% | Chiều ngang 200% |
|---|---:|---:|---:|---:|
| [360×800](../ui/screenshots/U2.2/after-360x800.png) | 1 | 313 × 56 | 345 / 345 | 172 / 172 |
| [390×844](../ui/screenshots/U2.2/after-390x844.png) | 1 | 343 × 56 | 375 / 375 | 187 / 187 |
| [768×1024](../ui/screenshots/U2.2/after-768x1024.png) | 2 | 337,14 × 56 | 753 / 753 | 376 / 376 |
| [1366×768](../ui/screenshots/U2.2/after-1366x768.png) | 2 | 540,69 × 56 | 1351 / 1351 | 675 / 675 |
| [1920×1080](../ui/screenshots/U2.2/after-1920x1080.png) | 2 | 536 × 56 | 1905 / 1905 | 952 / 952 |

Chrome kiểm Tab vào skip link, Enter tới main, Tab/Shift+Tab giữa các SKU, focus-ring 3 px và Enter để gửi đúng lựa chọn. Menu mobile và phần hướng dẫn sử dụng mở được bằng bàn phím. Ở 360×800, từ đầu trang thấy tên và giá, CTA nằm dưới các biến thể và truy cập bằng cuộn hoặc Tab. Với JS tắt, Guest chọn SKU bằng GET, thêm vào giỏ thành công rồi nhận lỗi tồn khi thêm lần hai. Với JS bật, Customer cũng nhận đúng hai phản hồi đó. Một lượt Guest tại zoom 200% đổi SKU qua lại và gửi POST thành công. Trong lượt này viewport CSS từ 1348 xuống 674 và devicePixelRatio từ 1 lên 2.

### Regression trên JDK 21

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:P01_MARIADB_TEST = 'false'
mvn.cmd -version
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' '-Dtest=GiaoDienPagesTest' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package
node --check src/main/resources/static/js/app.js
git diff --check
```

Maven 3.9.11 chạy bằng Temurin 21.0.13. Baseline là 65 test, **31 PASS, 0 FAIL, 0 ERROR, 34 SKIPPED**. Nhóm MVC tập trung đạt 15/15. Suite cuối ghi `Tests run: 69, Failures: 0, Errors: 0, Skipped: 34`, tức **35 PASS**. Cả 34 test MariaDB được bỏ qua vì chưa xác nhận database test độc lập. Đóng gói JAR thành công. SHA-256 của template PDP, storefront.css và app.js trong JAR khớp file nguồn. Kiểm cú pháp JS và `git diff --check` thành công.

Lần kiểm tra lại ngày 22/11/2026 trên working tree hiện tại cho cùng kết quả Maven và đóng gói. Chrome chạy lại năm viewport, zoom 200%, luồng chọn SKU và phản hồi giỏ cho Guest/Customer trên fixture HTTP tại `127.0.0.1:61023`. Ở 390×844, khi mô phỏng độ trễ mạng 800 ms và nhấn Enter hai lần cách nhau khoảng 80 ms, form Guest chỉ gửi một `POST /cart/items`. Trong lúc chờ, form có `aria-busy="true"`, nút hiển thị “Đang xử lý…” và giữ kích thước 343 × 56 CSS px. Sau redirect, giỏ hiển thị “Đã thêm SKU vào giỏ.” Chỉ báo cáo được cập nhật trong lần kiểm tra lại này.

### Đối chiếu audit và nghiệm thu

| Issue | Trạng thái trong U2.2 | Bằng chứng |
|---|---|---|
| F01 | **BLOCKED** | HTTP chỉ đọc `GET http://localhost:8080/products` trả 200, có trạng thái rỗng và 0 thẻ sản phẩm. Chưa có slug và SKU công khai trên endpoint này để đối chiếu với dữ liệu demo. |
| F03 | **Đã xử lý trên PDP** | Source, MVC và Chrome. Tab/Shift+Tab không phát sinh GET, Enter gửi đúng variant, JS tắt vẫn chọn được. |
| F02 | Đã kiểm hai nhánh lỗi PDP trên fixture | Request variant sai kiểu trả 400, ID không tồn tại trả 404. Chrome hiển thị heading tiếng Việt của U1. |
| F04, F06 | Đã kiểm trong phạm vi control và focus PDP | Nút SKU dùng viền control U1, trạng thái đang chọn có chữ, hết hàng có chữ. Chrome kiểm skip link, vòng focus, hai chiều Tab và zoom. |
| F11 | Đã kiểm trên CTA thêm giỏ | Chrome kiểm nhãn chờ, kích thước nút và một POST khi Guest nhấn Enter hai lần trong điều kiện mạng chậm. POST Customer cũng giữ payload hợp lệ. |
| F14 | **Một phần** | Khung 1:1 giữ chỗ cho ảnh có URL, thiếu URL, và lỗi tải có JS. Chưa kiểm ảnh upload thật. Tạo ảnh nhiều kích cỡ và đo ngân sách ảnh thuộc phạm vi khác. |
| F17 | Một phần | Đã thay CSS PDP cũ bằng quy tắc theo token U1. |

Các vấn đề còn lại của F01–F24 không có thay đổi trong lát cắt này. Chưa kiểm bằng screen reader, axe, Lighthouse, thiết bị di động vật lý hoặc ảnh upload thật. Khi JS tắt và URL ảnh trả lỗi, trình duyệt dùng alt trong khung cố định. Chưa kiểm việc lưu giỏ Customer, gộp giỏ sau đăng nhập, khóa tồn và dữ liệu đánh giá với MariaDB thật.

**Cổng nghiệm thu.** Trên fixture đã đạt điều kiện chọn SKU bằng bàn phím, phản hồi SKU sai/hết, hiển thị đúng giá và tồn được cấp, CTA 56 px, POST/CSRF cho Guest và Customer, phản hồi thành công/thất bại, năm viewport và zoom 200%. Để nghiệm thu demo, cần catalog có SKU công khai và một MariaDB test độc lập được phép ghi để đối chiếu giá, tồn, ảnh và giỏ lưu thực tế. Dừng tại U2.2. U2.3 chưa triển khai.

Nếu khôi phục riêng U2.2, trả template PDP về bản đầu kỳ, khôi phục các quy tắc CSS PDP cũ, bỏ phần JS riêng cho menu/ảnh PDP và bốn ca MVC mới, rồi gỡ ảnh, dòng roadmap và phần báo cáo U2.2. Giữ các thay đổi U1/U2.1 đã có trong working tree.

<a id="u23"></a>

## U2.3 — Giỏ hàng

Ngày 22/11/2026. Giỏ hiển thị giá hiện hành, số lượng, thành tiền từng SKU và tạm tính hàng hóa. Lần triển khai đầu kiểm luồng sửa, xóa, hết tồn và hợp nhất giỏ trên fixture, với placeholder trên mọi dòng. Sau khi được duyệt mở rộng truy vấn đọc, giỏ nhận thêm ảnh của SKU, thương hiệu và slug để mở đúng biến thể. [Đặc tả U2.3](../prompts/P12-U2-luxury-storefront.md) · [Kết quả kiểm chứng ba trường](#u23-du-lieu)

### File và hành vi

| File | Thay đổi trong U2.3 |
|---|---|
| [cart/index.html](../../src/main/resources/templates/cart/index.html) | Card từng SKU, đơn giá, thành tiền, bộ chỉnh số lượng, cảnh báo tồn, tạm tính, CTA theo vai trò, trạng thái trống và thông báo sau POST. |
| [storefront.css](../../src/main/resources/static/css/storefront.css) | Danh sách và tóm tắt hai cột trên desktop. Mobile xếp một cột. Dùng token U1, mục tiêu bấm ít nhất 44 px, CTA ít nhất 56 px. |
| [app.js](../../src/main/resources/static/js/app.js) | Nút tăng/giảm chỉ thay đổi ô nhập. Hiện lỗi tiếng Việt cạnh trường, trả focus khi nhập sai. Cả xóa trực tiếp và cập nhật về 0 đều dùng dialog xác nhận của U1 khi bật JS. |
| [GioHangPagesTest.java](../../src/test/java/com/beautyshop/controller/GioHangPagesTest.java) | Ba test MVC kiểm giỏ rỗng, tiền chính xác khi tồn giảm, escaping, contract cập nhật/xóa và CSRF. |
| [Roadmap](../03-roadmap-and-team.md), báo cáo này và [13 ảnh Chrome](../ui/screenshots/U2.3/) | Tiến độ, bằng chứng trước/sau và điều kiện nghiệm thu. |

Thymeleaf nhân đơn giá `BigDecimal` với số lượng đã lưu và cộng thành tiền trên server. Chỉnh ô số lượng giữ số tiền đã xác nhận đến khi POST thành công. Ví dụ fixture gồm hai đơn vị giá 125.000 ₫ và một đơn vị giá 185.000 ₫, tạm tính 435.000 ₫. Khi cập nhật dòng đầu thành ba đơn vị, server trả 560.000 ₫. Giá trị này chưa bao gồm voucher hoặc phí vận chuyển. Test MVC còn kiểm tổng 9.999.999.999.999.997 ₫ để phát hiện mất độ chính xác.

Hai form tiếp tục dùng `POST /cart/items/{variant}/quantity`, trường `quantity` và CSRF. Giá trị 0 xóa dòng. Việc nhấn +/− không gửi request. Nút thanh toán dẫn đến `GET /checkout`, Guest được chuyển đến form login. Giỏ rỗng dẫn về `/products`.

### Bằng chứng trước và sau

Chrome Headless 158.0.8127.36 trên Windows, fixture `http://127.0.0.1:55463/cart`. Ảnh trước/sau chụp Guest với cùng hai SKU kiểm thử và căn đầu vùng nội dung vào viewport. Fixture dùng Tomcat, Spring Security, Thymeleaf và các controller/service giỏ, đăng nhập thực. Repository mô phỏng trong bộ nhớ. Tiến trình fixture loại DataSource, JPA và Flyway. Không gửi POST đến endpoint demo.

| Màn hình | Trước | Sau |
|---|---|---|
| Desktop 1366×768 | [Danh sách cũ](../ui/screenshots/U2.3/before-1366x768.png) | [Dòng tiền và tóm tắt](../ui/screenshots/U2.3/cart-1366x768.png) · [Giỏ trống](../ui/screenshots/U2.3/empty-1366x768.png) |
| Mobile 390×844 | [Danh sách cũ](../ui/screenshots/U2.3/before-390x844.png) | [SKU và số lượng](../ui/screenshots/U2.3/cart-390x844.png) · [Tạm tính và CTA](../ui/screenshots/U2.3/summary-390x844.png) · [Giỏ trống](../ui/screenshots/U2.3/empty-390x844.png) |
| Tương tác | | [Tồn giảm trước POST](../ui/screenshots/U2.3/stock-changed-390x844.png) · [Xác nhận xóa](../ui/screenshots/U2.3/confirm-390x844.png) · [Zoom 200%](../ui/screenshots/U2.3/zoom-200.png) |

Chrome đo cả giỏ trống và giỏ có hai SKU tại năm viewport và hai mức zoom, tổng cộng 20 tổ hợp. Cả 20 đều có `scrollWidth == clientWidth`. Với giỏ có hàng, kết quả đo dưới đây là chiều rộng nội dung tính bằng CSS px sau khi trừ thanh cuộn. Nút nhỏ nhất đo được 44 × 44 CSS px, CTA thanh toán cao 56 px ở zoom 100%. Tâm CTA nhận đúng thao tác sau khi cuộn đến ở cả hai mức zoom.

| Viewport | Bố cục ở 100% | Rộng ở 100% | Rộng ở 200% |
|---|---|---:|---:|
| [360×800](../ui/screenshots/U2.3/cart-360x800.png) | Một cột | 345 | 172 |
| [390×844](../ui/screenshots/U2.3/cart-390x844.png) | Một cột | 375 | 187 |
| [768×1024](../ui/screenshots/U2.3/cart-768x1024.png) | Một cột | 753 | 376 |
| [1366×768](../ui/screenshots/U2.3/cart-1366x768.png) | Hai cột | 1351 | 675 |
| [1920×1080](../ui/screenshots/U2.3/cart-1920x1080.png) | Hai cột | 1905 | 952 |

### Regression và đối chiếu audit

| Phạm vi | Kết quả |
|---|---|
| Guest, bàn phím | Tab/Shift+Tab, skip link, vòng focus 3 px, tăng/giảm và nhập trực tiếp. Các giá trị rỗng, âm, thập phân hoặc vượt tồn bị chặn. Lỗi tiếng Việt nối với ô nhập trong cây accessibility của Chrome. |
| Tồn thay đổi | Giảm tồn fixture sau GET và trước POST. Server từ chối yêu cầu vượt tồn, giữ số lượng cũ, hiện thông báo lỗi và giới hạn mới. Giảm số lượng về mức hợp lệ cập nhật thành công. |
| Xóa và JS tắt | Escape hủy dialog và trả focus, Enter xác nhận gửi đúng SKU. Khi tắt JS, trường số và form HTML vẫn cập nhật, xóa trực tiếp hoặc xóa bằng số lượng 0. |
| Hợp nhất Guest → Customer | Đăng nhập qua form thật, hai SKU được gộp đúng lượng. Tải lại giỏ và đăng nhập lại giữ số lượng. Customer cập nhật và xóa được. Khi tồn giảm lúc đăng nhập, service giới hạn số lượng và thông báo một lần. Đây là kết quả fixture. |
| F10/F11 tại giỏ | Dialog nêu tên sản phẩm, biến thể và SKU. Ở độ trễ mô phỏng 800 ms, hai lần Enter cách nhau khoảng 80 ms chỉ gửi một POST cập nhật với `quantity` và `_csrf`. Nhãn “Đang xử lý…” và `aria-busy` xuất hiện, kích thước nút giữ nguyên. POST thiếu CSRF trả 403. |
| F08 | Đã kiểm đơn giá, thành tiền, tạm tính, lời giải thích về phí/voucher và hai CTA Guest/Customer. Kết quả bổ sung ảnh, thương hiệu và liên kết nằm ở [phần dữ liệu giỏ](#u23-du-lieu). |
| F23 | Đã xử lý control số lượng trên giỏ. Checkbox trang địa chỉ ngoài U2.3. |
| F01 | **BLOCKED.** `GET http://localhost:8080/products` trả 200 và 0 thẻ sản phẩm. Chưa có bằng chứng về catalog công khai trên MariaDB. |

Maven 3.9.11 chạy trên Temurin 21.0.13. Baseline U2.3 có 35 PASS, 0 FAIL, 0 ERROR, 34 SKIPPED. Nhóm `GioHangPagesTest` đạt 3/3. Suite cuối ghi `Tests run: 72, Failures: 0, Errors: 0, Skipped: 34`, tức **38 PASS, 0 FAIL, 0 ERROR, 34 SKIPPED**. Đóng gói JAR, kiểm cú pháp JS và kiểm diff thành công. Cả 34 integration test MariaDB được bỏ qua vì chưa xác nhận DB test độc lập.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:P01_MARIADB_TEST = 'false'
mvn.cmd -version
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' '-Dtest=GioHangPagesTest' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package
node --check src/main/resources/static/js/app.js
git diff --check
```

**Điều kiện còn mở.** Cần đối chiếu ảnh thật, giá, tồn, lưu giỏ và tính nguyên tử của thao tác gộp trên MariaDB test độc lập. Chưa chạy trình đọc màn hình, axe, Lighthouse hoặc thiết bị di động vật lý.

Để khôi phục riêng U2.3, hoàn nguyên template giỏ về bản đầu lát cắt, bỏ CSS/JS bổ sung cho giỏ, test MVC giỏ và mục U2.3 trong chứng cứ/roadmap. Giữ nguyên các phần U1/U2.1/U2.2 trong các file dùng chung.

<a id="u23-du-lieu"></a>

### Hoàn thiện U2.3 — Ảnh SKU, thương hiệu và liên kết sản phẩm

Ngày 22/11/2026. Cả giỏ Guest và Customer lấy ba trường từ cùng phép chiếu SQL. Người mua có thể quay về đúng SKU từ tên sản phẩm. Khi SKU, sản phẩm, thương hiệu hoặc danh mục ngừng hoạt động, giỏ vẫn hiện dòng và tên dưới dạng văn bản. SKU công khai hết tồn vẫn có liên kết PDP.

#### File sửa trong lần hoàn thiện

| File | Thay đổi |
|---|---|
| [GioHang_Repository.java](../../src/main/java/com/beautyshop/repository/GioHang_Repository.java) | Mở rộng hai SELECT, dùng chung mapper theo tên cột. Record `MucGio` có thêm `urlHinhAnh`, `tenThuongHieu` và `duongDan`. `voiSoLuong` giữ đủ ba trường cho Guest. |
| [cart/index.html](../../src/main/resources/templates/cart/index.html), [storefront.css](../../src/main/resources/static/css/storefront.css) | Thumbnail theo SKU, tên thương hiệu khi có, tên sản phẩm dẫn về PDP với tên truy cập phân biệt từng SKU. Khung ảnh 4:5 giữ nguyên kích thước khi tải lỗi. |
| [pom.xml](../../pom.xml), [GioHangRepositoryTest.java](../../src/test/java/com/beautyshop/repository/GioHangRepositoryTest.java) | Thêm H2 ở phạm vi test để chạy SQL của repository với hai SKU và hai chủ giỏ. |
| [GioHangPagesTest.java](../../src/test/java/com/beautyshop/controller/GioHangPagesTest.java) | Kiểm HTML cho slug, variant, thương hiệu, URL ảnh, metadata thiếu, CSRF và tổng tiền. |
| [Roadmap](../03-roadmap-and-team.md), báo cáo này và [10 ảnh Chrome](../ui/screenshots/U2.3/product-data/) | Cập nhật tiến độ và bằng chứng cho phần dữ liệu giỏ. |

#### Nguồn dữ liệu và số dòng

`docGio` vẫn lọc theo `g.ma_nguoi_dung = ?`. V1 đặt khóa duy nhất trên `gio_hang.ma_nguoi_dung` và trên cặp `chi_tiet_gio_hang(ma_gio_hang, ma_bien_the)`. Mỗi dòng tham chiếu một SKU. Các JOIN hiện có đi từ SKU đến sản phẩm rồi thương hiệu và danh mục bằng khóa ngoại bắt buộc trỏ tới khóa chính. Ảnh là cột nullable ngay trên SKU. V2–V5 không bổ sung quan hệ ảnh một-nhiều. [Schema V1](../../src/main/resources/db/migration/V1__khoi_tao_co_so_du_lieu.sql)

Cột bổ sung vào SELECT của `timBienThe` và `docGio`:

```sql
b.url_hinh_anh,
t.ten_thuong_hieu,
CASE WHEN b.dang_hoat_dong AND s.dang_hoat_dong
          AND t.dang_hoat_dong AND d.dang_hoat_dong
     THEN s.duong_dan END AS duong_dan
```

Điều kiện công khai chỉ quyết định có tạo link hay không. Ảnh NULL và trạng thái ngừng hoạt động không loại dòng. Các JOIN, WHERE, thứ tự SKU, khóa và câu lệnh ghi giữ nguyên. Mỗi lần đọc giỏ Customer vẫn dùng một SELECT. Guest vẫn đọc từng mã khác nhau trong session theo cơ chế sẵn có. Cả hai lấy ba trường cùng lượt đọc giá và tồn.

#### Kiểm chứng chức năng

- **Hai SKU và quyền sở hữu.** Repository chạy SQL thật trên H2 ở chế độ tương thích MariaDB. Các dòng fixture chỉ tồn tại trong biểu thức `WITH … VALUES` của SELECT. Giỏ thứ nhất nhận đúng hai SKU của R026, giỏ thứ hai nhận đúng một dòng của mình. Test kiểm cả giá, lượng, tồn sau tạm giữ, ảnh riêng của từng SKU và trường hợp sản phẩm hoặc thương hiệu ngừng hoạt động.
- **Liên kết và tiền.** Fixture Chrome dùng hai biến thể Vanicream Moisturizing Cream đã có trong dữ liệu nghiên cứu của dự án. Enter trên hai liên kết `/products/demo-r026?variant=11` và `?variant=22` mở PDP có lựa chọn tương ứng 16 oz (410.000 ₫) và 4 oz (245.000 ₫). Hai đơn vị 16 oz và một đơn vị 4 oz cho đúng **1.065.000 ₫**. Đăng nhập từ Guest rồi tải lại giỏ Customer vẫn thấy hai dòng, số lượng 2 và 1, cùng tạm tính. Tắt hiển thị sản phẩm trong fixture vẫn giữ hai dòng, tạm tính và cảnh báo tồn. ID 11 và 22 là ID của fixture. [Dữ liệu R026](../data/product-research.csv)
- **Ảnh.** Chỉ nhận đường dẫn theo định dạng do bộ upload hiện hành cấp. URL ngoài route, ảnh NULL, tệp 404 và PNG lỗi đều có placeholder. Chrome đã kiểm 404 và lỗi giải mã khi tắt JavaScript. Ở ca thành công, route ảnh thật của ứng dụng trả HTTP 200, Chrome giải mã PNG 375×900, nội dung trùng SHA-256 và placeholder bên trong phần tử ảnh được ẩn. Ca này dùng bản sao tạm của một PNG kiểm thử sẵn có trong thư mục fixture, với vùng ảnh ẩn và không chụp màn hình. Các ảnh nghiệm thu bên dưới thể hiện trường hợp thiếu ảnh SKU. Chưa có ảnh thương phẩm thực tế để đối chiếu với MariaDB.
- **Bàn phím và form.** Tab và Shift+Tab đến và quay lại tên sản phẩm, focus hiển thị viền 3 px. Tên truy cập chứa biến thể và mã SKU. Hai form cập nhật/xóa tiếp tục dùng POST, `quantity` và CSRF. Test giỏ kiểm tạm tính `BigDecimal` đến 9.999.999.999.999.997 ₫.

#### Bằng chứng Chrome

Chrome Headless 158.0.8127.36 trên Windows, Tomcat chạy controller/service/Thymeleaf thật với repository fixture trong bộ nhớ. Giỏ có hai SKU, một URL ảnh trả 404 và một SKU có ảnh NULL. Cả 10 tổ hợp đều có `scrollWidth == clientWidth`, khung ảnh 4:5 và liên kết sản phẩm cao ít nhất 44 CSS px. Hai cột chiều rộng dưới đây là số đo CSS px sau khi trừ thanh cuộn. Bản trước khi bổ sung dữ liệu có thể xem ở [desktop](../ui/screenshots/U2.3/cart-1366x768.png) và [mobile](../ui/screenshots/U2.3/cart-390x844.png).

| Viewport | 100% | 200% |
|---|---:|---:|
| 360×800 | [345 px](../ui/screenshots/U2.3/product-data/cart-360x800-100.png) | [172 px](../ui/screenshots/U2.3/product-data/cart-360x800-200.png) |
| 390×844 | [375 px](../ui/screenshots/U2.3/product-data/cart-390x844-100.png) | [187 px](../ui/screenshots/U2.3/product-data/cart-390x844-200.png) |
| 768×1024 | [753 px](../ui/screenshots/U2.3/product-data/cart-768x1024-100.png) | [376 px](../ui/screenshots/U2.3/product-data/cart-768x1024-200.png) |
| 1366×768 | [1351 px](../ui/screenshots/U2.3/product-data/cart-1366x768-100.png) | [675 px](../ui/screenshots/U2.3/product-data/cart-1366x768-200.png) |
| 1920×1080 | [1905 px](../ui/screenshots/U2.3/product-data/cart-1920x1080-100.png) | [952 px](../ui/screenshots/U2.3/product-data/cart-1920x1080-200.png) |

#### Kết quả và điều kiện nghiệm thu

Maven 3.9.11, Temurin JDK 21.0.13. Đã chạy offline với Maven repository cục bộ và `P01_MARIADB_TEST=false`.

| Lần chạy | PASS | FAIL | ERROR | SKIPPED |
|---|---:|---:|---:|---:|
| Trước lần bổ sung ba trường | 38 | 0 | 0 | 34 |
| Hai lớp test giỏ trong suite cuối | 8 | 0 | 0 | 0 |
| Toàn bộ `mvn test` sau thay đổi | **43** | **0** | **0** | **34** |

`mvn -DskipTests package` tạo thành công `target/beautyshop-0.0.1-SNAPSHOT.jar`. Toàn bộ 34 test bị bỏ qua thuộc nhóm tích hợp MariaDB. Chưa có database test độc lập được xác nhận. Còn cần đối chiếu ảnh SKU đã upload của người bán, giá/tồn thật, giỏ Customer lưu qua phiên và tính nguyên tử của hợp nhất trên MariaDB. Các POST của trình duyệt chỉ gửi đến fixture loopback không có DataSource.

| Audit | Trạng thái sau khi bổ sung dữ liệu |
|---|---|
| F01 | **BLOCKED**. Endpoint demo `GET /products` trả 200 và 0 thẻ sản phẩm. Chưa xác minh được SKU công khai trên MariaDB. |
| F08 | **Đạt trên fixture** cho giỏ Guest/Customer, thumbnail/fallback, thương hiệu, link đúng biến thể và tạm tính. Nghiệm thu demo chờ dữ liệu thật. |
| F14 | **Một phần**. Khung ảnh và hai nhánh tải thành công/lỗi đã kiểm trên Chrome. Chưa đo ảnh thương phẩm thật, ảnh nhiều kích thước hoặc ngân sách tải. |
| F23 | **Đạt trong giỏ**. Bộ chỉnh số lượng và hợp đồng cập nhật/xóa đã có regression. Checkbox địa chỉ nằm ngoài U2.3. |
