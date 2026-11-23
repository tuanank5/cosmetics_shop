# BeautyShop · Audit và kế hoạch bổ sung công nghệ frontend

Ngày 22–23/11/2026. **Pilot GSAP Core C19 đã tích hợp, gate dependency đạt và đã kiểm thử trên Chrome Headless/fixture; chờ owner nghiệm thu.** Core + adapter **29.189 byte gzip**, Maven **61 PASS / 0 FAIL / 0 ERROR / 34 SKIPPED**, package JAR thành công. [Kết quả pilot hiện hành](gsap-c19-pilot.md#ket-qua-tiep-tuc). Mục 1–10 giữ lịch sử audit/kế hoạch ban đầu, mục 11 là lần gate bị chặn trước khi owner cung cấp artifact. Alpine.js, htmx và Tailwind chưa được triển khai.

## 1. Working tree và nền tảng hiện tại

Đã đọc AGENTS.md, Design System U1, hai báo cáo P15 hiện hành và source header/footer/head, CSS, app.js, Security_Config, advice giỏ, catalog MVC và checkout. Nhánh main, HEAD 837e550, có nhiều file sửa/xóa/chưa commit từ các giai đoạn trước. Inventory Git và hash source được lưu trong target/frontend-tech-audit để kiểm soát phạm vi audit. [AGENTS](../../AGENTS.md) · [Design System U1](../ui/design-system.md)

Sau B1.4, ma trận ghi **20/30 PASS, 6 PARTIAL, 4 FAIL**, core 10/12. C01/C02/C05/C06/C07/C08/C28 đã có bằng chứng fixture. Baseline kiểm thử lịch sử là **58 PASS, 0 FAIL, 0 ERROR, 34 SKIPPED**, footer 110 tổ hợp không tràn ngang. F01 vẫn BLOCKED; C15, C27 và C21 còn mở. Lượt audit này không chạy lại Maven/browser và không thay trạng thái component. [Ma trận P15](byredo-component-gap-analysis.md) · [Báo cáo B1.4](byredo-inspired-ui-implementation.md#b14)

Ứng dụng render HTML bằng Thymeleaf. Maven hiện đóng gói tài nguyên tĩnh cùng JAR, chưa có package.json, lockfile npm hoặc bundler trong source dự án. Head chung nạp năm stylesheet và app.js defer cho cả storefront/Admin. app.js dài 163 dòng, 6.501 byte, gồm disclosure/focus/resize, fallback ảnh PDP, cart stepper/validation, filter anchor, hiện mật khẩu, confirm dialog và chống submit lặp. DOM được khởi tạo một lần khi trang tải. Quy mô trạng thái này vẫn phù hợp với Vanilla JavaScript và HTML native. [Head](../../src/main/resources/templates/fragments/head.html) · [app.js](../../src/main/resources/static/js/app.js) · [pom.xml](../../pom.xml)

## 2. Quyết định công nghệ đề xuất

| Công nghệ | Quyết định xin duyệt | Giá trị thực tế | Tác động dự kiến |
|---|---|---|---|
| GSAP Core | Thử nghiệm nhỏ C19 trước | Điều phối nhịp chuyển động của ba hình CSS trong hero, có cleanup khi thay đổi motion preference | Một bản phân phối local, một adapter cho home và hai script defer chỉ ở home. Không cần plugin hoặc bundler cho thử nghiệm này. |
| Alpine.js | Giữ dự phòng | Có thể có ích khi xuất hiện một component có nhiều trạng thái phụ thuộc nhau và nhiều nhánh DOM | Hiện thêm một chủ thể quản lý state sẽ trùng với listener/native disclosure đã PASS. Nếu mở lại, ưu tiên CSP build và chỉ chuyển một component có ranh giới rõ. |
| htmx | Đánh giá sau khi catalog có dữ liệu và yêu cầu giữ vị trí duyệt | Có thể thay vùng catalog qua GET trong cùng trang, giữ biểu mẫu và pagination SSR | Cần giải quyết lifecycle listener, focus, history, request chạy đua và phản hồi phiên hết hạn. Đợt đầu chỉ nên xét GET công khai. |
| Tailwind CSS | Tiếp tục dùng CSS/token U1 | Utilities có thể tiện cho một nhóm xây nhiều layout mới | Hiện thêm compiler, lớp theme và quy trình scan template, trong khi layout đã có CSS dùng chung. Chưa có phép đo tiết kiệm công sức hoặc giảm CSS để biện minh. |

GSAP hoạt động độc lập với framework và có bản minified dùng bằng script tag. Kết luận về phạm vi C19 là lựa chọn kỹ thuật cho BeautyShop sau khi xem source, chưa phải kết quả benchmark animation. [GSAP installation](https://gsap.com/docs/v3/Installation/)

## 3. Ánh xạ lợi ích cho C01–C30

Trạng thái lấy từ ma trận sau B1.4. “Điều kiện sau” nghĩa là chỉ mở lại nếu owner duyệt thêm phạm vi.

| ID | Component / hiện trạng | Hướng phù hợp | Lợi ích và giới hạn |
|---|---|---|---|
| C01 | Header · PASS | CSS U1 + native | Giữ kích thước, alignment và vùng thao tác đã kiểm. Không cần engine animation cho header. |
| C02 | Navigation nhiều cấp · PASS | details/summary + JS hiện có | Mở, đóng, Escape, resize và no-JS đã có. GSAP chỉ đáng xét sau khi có chuyển động cụ thể giúp định hướng; chưa đưa vào thử nghiệm C19. |
| C03 | Search · PASS | GET + HTML input | Enter, query và focus hiện có. htmx là điều kiện sau nếu có yêu cầu kết quả tìm trong trang; tránh tự thêm autocomplete/API. |
| C04 | Account entry · PASS | Thymeleaf theo role | Thư viện không tăng tính đúng của phân quyền. |
| C05 | Wishlist entry · PASS | SVG local + link SSR | Giữ route thật và accessible name. CSS hover/focus đáp ứng phản hồi cần thiết. |
| C06 | Cart indicator · PASS | Badge server + CSS | Sau POST mới có số xác nhận. Có thể dùng CSS nhấn nhẹ trạng thái thành công, không suy số từ DOM hoặc tween số lượng. |
| C07 | Navigation state · PASS | uiPath + Thymeleaf | Exact match/canonical và dấu nhóm là logic server. Không thêm state URL phía client. |
| C08 | Responsive/reflow · PASS | Grid/Flex/minmax + CSS | Animation không xử lý overflow. Giữ bộ đo 100%/200% sau mỗi thay đổi. |
| C09 | Collection heading · PASS | Typography U1 | Giữ tiêu đề và mô tả đọc được ngay khi HTML tới. |
| C10 | Product count · PASS | Page.totalElements | Nếu dùng htmx thì render lại count từ cùng response server. Không để thư viện tự đếm tổng sản phẩm. |
| C11 | Filters · PASS | Form GET + details | Alpine chỉ xét nếu có state phụ thuộc thật. htmx có thể cập nhật toàn vùng filter/results sau khi giải quyết lifecycle. |
| C12 | Chip và bộ đếm filter · PARTIAL | Thymeleaf, JS nhỏ nếu cần | Bổ sung số tiêu chí từ model hiện có. htmx có thể thay HTML sau GET; không phải điều kiện để hoàn thiện mục này. |
| C13 | Grid density · FAIL | CSS class + Vanilla JS | Hai mức mật độ là state nhỏ, có thể lưu lựa chọn bằng localStorage với fallback khi storage lỗi. Chưa cần Alpine. |
| C14 | Product grid · PASS | CSS Grid | Lưới, tên dài và thứ tự sản phẩm do layout/SSR quyết định. Chuyển động trên toàn bộ card chưa có lợi ích đủ rõ. |
| C15 | Product image · PARTIAL | HTML fallback + pipeline ảnh | Sửa URL lỗi, kích thước và nguồn ảnh. Thư viện animation không giải quyết dữ liệu hoặc quyền ảnh. |
| C16 | Badges · PASS | Server + CSS | Nhãn bám tồn/dữ liệu. Tránh biến nhãn “hết hàng” thành hiệu ứng khó nhận biết. |
| C17 | Quick actions · FAIL | Contract MVC + projection được duyệt | Xác định đúng SKU và ID trước. CSS hỗ trợ hover/focus; htmx POST chỉ xét ở phạm vi riêng sau khi có test role/CSRF/flash. |
| C18 | Progressive browsing · PARTIAL | Pagination SSR trước | Thêm tiến độ từ Page. htmx có thể giữ vùng duyệt và history; “Xem thêm” cần kiểm append không trùng và fallback pagination. |
| C19 | Editorial hero · PASS | GSAP Core thử nghiệm | Điều phối ba phần minh họa CSS đang có. H1, giá trị thông tin và CTA luôn hiển thị. Đây là phạm vi duyệt ban đầu. |
| C20 | Editorial giữa catalog · FAIL | Nội dung và Thymeleaf trước | Khi có khối nội dung được duyệt, GSAP có thể điều phối chuyển động của hình minh họa riêng. Giữ thứ tự và count sản phẩm. |
| C21 | Video/cinematic media · FAIL | HTML video khi đủ quyền | Video hợp lệ, controls, captions và reduced-motion là điều kiện chính. GSAP không cung cấp quyền sử dụng video. |
| C22 | Service cards · PASS | Nội dung U1 + CSS | Link, câu chữ và phân cấp hiện có. CSS transition ngắn đáp ứng tương tác. |
| C23 | PDP · PASS | SSR + ảnh/SKU thật | Hình, brand, giá và tồn dùng server. Zoom ảnh hoặc gallery chỉ xét khi có dữ liệu và yêu cầu riêng. |
| C24 | Variant selector · PASS | GET ?variant= + nút native | Giữ thao tác xác nhận và no-JS. Không tải lại bất ngờ vì animation/state framework. |
| C25 | Add-to-cart · PASS | POST + PRG + loading hiện có | Giữ chống gửi lặp và phản hồi thật. Thư viện không được phát “thành công” trước response server. |
| C26 | Cart · PASS | BigDecimal/SSR + stepper hiện có | GSAP/Alpine/htmx không cần cho giá, lượng hoặc hợp nhất. Partial cart cần xử lý cả badge/flash/tồn và CSRF, xét sau. |
| C27 | Checkout · PARTIAL | Sửa trạng thái quote bằng MVC | Lỗi voucher/quote cần xử lý ở hiển thị/trạng thái server đã được duyệt. Giữ COD/MoMo và thao tác tạo đơn ngoài pilot thư viện. |
| C28 | Footer · PASS | HTML + CSS role-aware | Reflow và link đã kiểm. Thêm thư viện không tạo lợi ích thực tế cho footer. |
| C29 | Service information · PARTIAL | Nội dung đã duyệt + link | Cần FAQ/chính sách thực, không cần library để tạo cảm giác đã có dịch vụ. |
| C30 | Breadcrumb/completion · PARTIAL | Thymeleaf + thông điệp server | Breadcrumb và trạng thái cuối cần dữ liệu/đích đúng. Animation trang hoàn tất chỉ xét sau khi có kết quả server đã xác nhận. |

Việc thêm thư viện không tự tăng coverage. Các khoảng trống C12/C13/C15/C17/C18/C20/C21/C27/C29/C30 vẫn cần nghiệm thu theo yêu cầu của từng mục.

## 4. Giấy phép, phiên bản và kích thước

### Giấy phép

GSAP dùng Standard “No Charge” License. Điều khoản hiện hành cho phép dùng trên website/giao diện, có hạn chế đối với công cụ tạo animation trực quan cạnh tranh với Webflow và yêu cầu giữ thông báo sở hữu. BeautyShop storefront được suy luận là phù hợp loại sử dụng website này; owner cần duyệt điều khoản của bản được chọn trước khi phân phối. [GSAP license](https://gsap.com/community/standard-license/)

Alpine và Tailwind có license MIT; khi phân phối cần giữ thông báo license/copyright. Gói @alpinejs/csp trong source đã xem khai báo MIT và phụ thuộc @vue/reactivity, nên cần kiểm kê thêm dependency này khi chốt artifact. htmx dùng 0BSD. [Alpine license](https://raw.githubusercontent.com/alpinejs/alpine/main/LICENSE.md) · [CSP package metadata](https://raw.githubusercontent.com/alpinejs/alpine/main/packages/csp/package.json) · [Tailwind license](https://raw.githubusercontent.com/tailwindlabs/tailwindcss/main/LICENSE) · [htmx license](https://raw.githubusercontent.com/bigskysoftware/htmx/master/LICENSE)

Các phiên bản quan sát được là GSAP 3.15.1, Alpine/CSP 3.17.5 trong metadata source, htmx 2.0.11 ở artifact tagged và Tailwind 4.3.5 ở npm registry. Đây là thông tin tại ngày audit. Trước tích hợp cần đối chiếu bản phát hành ổn định, pin chính xác và lưu hash của file tải được. Branch main/master có thể thay đổi. [GSAP metadata](https://raw.githubusercontent.com/greensock/GSAP/master/package.json) · [Alpine metadata](https://raw.githubusercontent.com/alpinejs/alpine/main/packages/csp/package.json) · [htmx artifact](https://github.com/bigskysoftware/htmx/blob/v2.0.11/dist/htmx.min.js) · [Tailwind registry](https://registry.npmjs.org/tailwindcss/latest)

### Baseline đo từ file hiện có

Đọc byte UTF-8 nguyên bản và nén bằng Node zlib tích hợp, gzip level 9, Brotli quality 11. Đơn vị dưới đây là byte. Đây là kích thước file và kết quả nén offline, chưa phải lượng transfer HTTP thực tế.

| Asset | Raw | gzip-9 | Brotli-11 |
|---|---:|---:|---:|
| tokens.css | 905 | 448 | 389 |
| base.css | 2.155 | 890 | 715 |
| components.css | 5.173 | 1.396 | 1.161 |
| storefront.css | 25.152 | 4.690 | 4.093 |
| admin.css | 1.036 | 419 | 324 |
| app.js | 6.501 | 1.906 | 1.565 |
| Tổng sáu file, cộng kích thước nén từng file | **40.922** | **9.749** | **8.247** |

Số tổng chưa gồm HTML, ảnh, header HTTP hoặc cache. Không thấy cấu hình compression tường minh trong application properties; proxy có thể xử lý ở ngoài source. Không dùng các số này làm điểm LCP/INP/CLS.

### Kiểm tra chi phí thư viện

| Artifact được xem | Thông tin kích thước kiểm được | Phần còn phải đo |
|---|---|---|
| GSAP dist/gsap.min.js, source 3.15.1 | GitHub hiển thị **71,6 KB** cho bản minified chưa nén HTTP | Exact bytes, gzip/Brotli và adapter của bản pin khi tải được. Chỉ lấy core, không lấy toàn bộ plugin. |
| htmx dist/htmx.min.js, tag v2.0.11 | GitHub hiển thị **51 KB** | Exact bytes, gzip/Brotli và phần cấu hình/lifecycle của BeautyShop. |
| @alpinejs/csp | Chưa tải được artifact để đo trong môi trường này | Đo riêng CSP build và plugin thực sự dùng, không dùng con số của Alpine thường để thay thế. |
| Tailwind 4.3.5 | Registry có kích thước gói compiler, chưa có CSS sinh cho BeautyShop | CSS output minified/gzip chỉ xác định sau khi chọn class, source scan và theme. Build dependency không phải runtime JS. |

Các số KB là metadata phân phối của upstream, không phải kết quả nén cục bộ. Truy cập registry trực tiếp bằng PowerShell thất bại, một số endpoint registry qua công cụ web cũng không truy cập được. Vì vậy chưa xác nhận số gzip thư viện và không dùng số package unpackedSize làm bundle browser. [GSAP file metadata](https://github.com/greensock/GSAP/blob/master/dist/gsap.min.js) · [htmx file metadata](https://github.com/bigskysoftware/htmx/blob/v2.0.11/dist/htmx.min.js)

Đề xuất budget để owner duyệt cho pilot GSAP là **tổng JS bổ sung ≤35 KiB gzip**, tính cả core và adapter, chỉ ở home. Các trang catalog/PDP/cart/checkout/account/Admin tăng 0 byte JS do pilot. Nếu bản pin vượt budget, đánh giá lại file/plugin và giá trị chuyển động trước khi đưa vào runtime. Budget này là mục tiêu đề xuất, chưa phải số đo đạt.

## 5. CSP và bảo mật tích hợp

### Hiện trạng kiểm được

Security_Config chưa khai báo CSP, head chưa có CSP meta. Spring Security không tự cung cấp CSP mặc định. GET localhost:8080 trong lượt audit timeout, nên CSP thực tế từ reverse proxy/production là **NOT VERIFIED**. Cần đọc response header tại đúng môi trường triển khai trước khi chốt cấu hình. [Security_Config](../../src/main/java/com/beautyshop/security/Security_Config.java) · [Head](../../src/main/resources/templates/fragments/head.html) · [Spring Security 6.5 CSP](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/headers.html#headers-csp)

Thumbnail giỏ hiện dùng object với URL ảnh SKU nội bộ. Chính sách object-src 'none' sẽ chặn nhánh ảnh này, dù img-src cho phép cùng origin. Vì vậy audit policy phải bao gồm GET ảnh giỏ có tệp thật và fallback. Với pilot có thể thử policy thích hợp trong fixture cô lập, trước mọi quyết định security production. [Cart template](../../src/main/resources/templates/cart/index.html#L27)

### Tương thích theo thư viện

- **GSAP.** Tải script nội bộ, mục tiêu thử script-src 'self'. Phân biệt CSSOM gán từng style property với setAttribute/style.cssText và style tag. CSP có thể xử lý chúng khác nhau. Bản GSAP được chọn, thao tác cleanup và plugin cụ thể phải được chạy dưới policy thực tế; chưa có chứng nhận CSP cho artifact chưa tải. [MDN style-src](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Content-Security-Policy/style-src)
- **Alpine.** Bản thường dùng Function để đánh giá expression, cần unsafe-eval ở CSP. CSP build là biến thể thay thế có giới hạn cú pháp cần kiểm theo component. Nếu xét tích hợp sẽ dùng bản này và đăng ký state từ script local. Các directive sửa style vẫn cần kiểm style-src. [Alpine CSP](https://alpinejs.dev/advanced/csp)
- **htmx.** Với pilot sau này, đề xuất allowEval=false, allowScriptTags=false, includeIndicatorStyles=false, selfRequestsOnly=true và CSS indicator trong file U1. Gắn listener trong script local, tránh hx-on hoặc js: expressions. Đây là cấu hình đề xuất, cần xác nhận với phiên bản pin. [htmx configuration](https://htmx.org/docs/#config)
- **Tailwind.** CSS được compile và phục vụ local, không dùng Play CDN trong production. Preflight có thể reset heading, margin, list và ảnh đang được U1 quản lý. Nếu có một thử nghiệm được duyệt riêng, phải chọn imports để bỏ Preflight và dùng prefix/theme từ token U1. [Tailwind CLI](https://tailwindcss.com/docs/installation/tailwind-cli) · [Preflight](https://tailwindcss.com/docs/preflight)

Policy cần xét cả object-src cho thumbnail giỏ, form-action/redirect khi MoMo Sandbox được bật và các nguồn ảnh đã được cho phép. Không đưa một CSP mẫu vào production chỉ để thư viện tải được. Pilot không cần thay CSRF, vai trò, session hoặc Security_Config. Nếu cần đổi policy triển khai thì trình riêng nội dung header và kết quả thử để duyệt.

## 6. Cách tích hợp từng công nghệ nếu được duyệt

### GSAP Core · phạm vi ưu tiên

Vị trí pilot là .shop-hero-art và ba phần minh họa CSS trong home/index.html. Di chuyển nhẹ khoảng 4–8 px, điều phối thời điểm giữa các hình để tạo nhịp mở đầu. Thời lượng dự kiến 160–260 ms cho mỗi chuyển động, toàn chuỗi dưới 500 ms. Nội dung, h1 và CTA ở trạng thái đọc/bấm được từ HTML đầu tiên. Chỉ animate transform/opacity của phần trang trí đã aria-hidden.

Dùng gsap.matchMedia với prefers-reduced-motion và cleanup/revert khi preference đổi. CSS reduced-motion hiện có chỉ quản lý CSS transition/animation, nên JS animation cần kiểm riêng. Khi reduce hoặc khi tải library lỗi, giữ nguyên bản tĩnh đang PASS. Không pin scroll, không loop, không chờ animation kết thúc để mở CTA. [GSAP matchMedia](https://gsap.com/docs/v3/GSAP/gsap.matchMedia()/) · [Hero hiện có](../../src/main/resources/templates/home/index.html#L7)

File dự kiến chỉ gồm bản vendor trong static/js/vendor, adapter static/js/home-motion.js và phần include script defer riêng ở template home. Đường dẫn /js/** đã được policy hiện tại cho phép. Gói vendor có version trong tên file, giữ license header và bản license/provenance/hash đi kèm. Maven tiếp tục copy static resources vào JAR. Với thử nghiệm script tag này, package.json, Node build plugin và thay đổi pom.xml đều không cần thiết.

### Alpine.js · chỉ mở lại khi có state cụ thể

C13 hai mức grid có thể viết bằng một class và một event handler, chưa cần Alpine. Nếu sau này một component có nhiều state phụ thuộc và chi phí listener tăng rõ, so sánh bản Vanilla với một component @alpinejs/csp được giới hạn phạm vi.

Một state chỉ do một bên quản lý. Không cho Alpine và app.js cùng đổi open của disclosure hoặc cùng xử lý submit. Template phải có nội dung/links/forms dùng được khi JS tắt; x-cloak không được che chức năng mua hàng gốc. Kiểm dependency và plugin từng phần trước khi chốt kích thước. File tác động khi đó là component template, một adapter local và nơi include script, không trải rộng header/Admin.

### htmx · GET catalog là ứng viên sau cùng trong đợt đầu

Catalog đang có GET /products với q, brand, category, skinType, sort, page, size và trả cả trang HTML. Phương án ít thay đổi nhất là giữ controller response này, dùng hx-select lấy .catalog-layout và thay toàn vùng tương ứng. Count, chip, form đã normalize và pagination được render đồng thời từ server. Vì response vẫn là full page, chỉ có DOM được thay một phần; chưa có lợi ích giảm byte HTML trên mạng. [Catalog controller](../../src/main/java/com/beautyshop/controller/CuaHang_Controller.java) · [hx-select](https://htmx.org/attributes/hx-select/)

Chỉ enhance form lọc và link chip/pagination GET được chỉ định. Các card PDP vẫn là link điều hướng đầy đủ. Không đặt hx-boost lên toàn body hoặc catalog wrapper vì nó sẽ bắt cả link PDP và link tài khoản. action/method/name/href giữ nguyên để no-JS dùng native GET.

Các việc phải xử lý trước nghiệm thu pilot này:

1. Tách khởi tạo filter hiện tại thành hàm có thể gọi lại, không bind trùng. app.js đang giữ biến catalogFilters trong closure và listener hashchange gắn một lần; sau swap biến đó trỏ tới DOM đã bị tháo.
2. Bảo toàn state GET và thứ tự dữ liệu server. Hủy/ưu tiên request cũ theo cùng vùng kết quả để response chậm không ghi đè query mới.
3. Khôi phục focus sau swap, thông báo kết quả ngắn qua live region, giữ loading/error và đường dùng form thường khi mạng lỗi.
4. Đồng bộ URL bằng history, kiểm Back/Forward và dùng cấu hình tránh lưu HTML cá nhân vào localStorage. Một fragment có thể nằm trong trang vẫn chứa header Customer, nên không mặc định cho rằng snapshot chỉ gồm catalog. [htmx history](https://htmx.org/attributes/hx-push-url/) · [Ngăn history cache cho dữ liệu riêng](https://htmx.org/attributes/hx-history/)
5. Kiểm phản hồi 400/403/500, redirect và session thay đổi. Pilot ưu tiên Guest công khai. Nếu role/navigation trong full response khác trang hiện tại hoặc response không có target mong đợi, chuyển sang full GET cùng URL để cập nhật khung trang. Không giữ header Customer cũ qua thay đổi phiên.
6. Nếu sau này muốn server trả fragment theo HX-Request trên cùng URL, cần duyệt thay đổi tối thiểu ở controller, test nhánh history restore và cache Vary phù hợp. Đây là đợt riêng sau phương án full-response/hx-select.

POST cart, wishlist và checkout tiếp tục dùng luồng hiện tại trong pilot GET. app.js đang dùng submitter, requestSubmit, PRG và pageshow để chống gửi lặp/khôi phục nút. Partial POST sẽ tác động đồng thời badge, flash, token CSRF, giá/tồn và redirect. Cần một thiết kế/test riêng trước khi xét áp dụng.

### Tailwind · chưa có phạm vi đáng duyệt

Nếu owner muốn đánh giá lại, benchmark trên một component mới, map utilities vào token U1, tắt Preflight, dùng prefix và đo CSS output thực tế. Thymeleaf class phải xuất hiện đầy đủ trong source để compiler phát hiện; tránh nối tên utility từ nhiều mảnh. Tính cả chi phí thêm CLI/lockfile/build step và visual regression Admin. [Source detection](https://tailwindcss.com/docs/detecting-classes-in-source-files)

Tailwind 4 có CLI compile CSS, có thể dùng npm hoặc executable riêng. Khi ấy cần pin tool/version, khai báo đường scan templates/JS, output CSS local và bước build trước Maven package. Chưa có lý do áp dụng quy trình này cho pilot GSAP hay CSS U1 hiện tại. [Tailwind build](https://tailwindcss.com/docs/installation/tailwind-cli)

## 7. Rủi ro và cổng kiểm chứng

| Rủi ro cụ thể | Cách kiểm khi triển khai |
|---|---|
| Mất chức năng khi library lỗi | Chặn vendor request, tắt JS hoàn toàn, xác nhận menu/SKU/cart/login/COD còn native flow như baseline. |
| Animation che focus hoặc gây chóng mặt | Thử reduce từ đầu và đổi preference khi đang chạy. Không animate ancestor có focus/CTA. Kiểm Tab/Shift+Tab, Enter/Space/Escape và zoom 200%. |
| Giảm tốc tải hoặc CLS | Đo cold/warm cache và CPU/network tương đương trước/sau. Hero content không đợi animation. Chỉ chuyển transform/opacity của vùng đã có kích thước. |
| Listener trùng hoặc DOM cũ | Với htmx/Alpine giả định, chạy swap/resize/history nhiều lần, đếm request và kiểm focus. Một thao tác vẫn chỉ có một kết quả. |
| CSP bị nới vì tiện dùng library | Thử trong fixture với response policy thật, kiểm console/violation. Ghi rõ artifact và cấu hình cần thiết trước đề nghị đổi production. |
| License hoặc version drift | Pin artifact, hash, kiểm license trực tiếp và provenance. Không tham chiếu latest/main ở đường tải runtime. |
| Hồi quy role hoặc dữ liệu | Lặp Guest/Customer/Admin, wishlist hidden, badge 0/1/5/12, SKU sai/hết, form CSRF và POST có submitter đúng. |
| Xâm nhập theme Admin/U1 | Dùng template home riêng để include pilot. Đối chiếu toàn bộ PASS cũ, token và ảnh B1.4. |

Không có số đo Lighthouse/LCP/INP/CLS mới trong audit. Lighthouse là phép đo lab; INP thực tế cần dữ liệu tương tác phù hợp. Khi thử nghiệm, ghi môi trường, mẫu dữ liệu, phương pháp và chỉ số thực đo thay vì quy chất lượng luxury thành số animation.

## 8. Build, phân phối và rollback

| Trường hợp được duyệt sau này | Build/dependency tối thiểu | Phân phối |
|---|---|---|
| GSAP pilot | File minified core đã pin + adapter local. Đo gzip/Brotli và ghi hash trước khi include. | Hai script chỉ ở home, Maven đóng gói trong JAR, không tải CDN ở runtime. |
| Alpine CSP component | Bản CSP local và adapter khởi tạo. Chỉ thêm plugin có use case thật. Nếu chọn npm thì package/lockfile phải pin và kiểm transitive license. | Include có điều kiện ở trang cần dùng. |
| htmx GET pilot | Một file htmx minified local, config/lifecycle local, template attributes và test. | Catalog riêng; server có thể tiếp tục full HTML. |
| Tailwind thử riêng | CLI/compiler được pin, source scan, token mapping, output CSS, build step trước package. | Chỉ ship CSS đã compile, không ship compiler/Play CDN. |

Mỗi bước triển khai tạo một diff độc lập, chỉ chứa component đã duyệt. Với working tree nhiều thay đổi hiện tại, lưu bản gốc các file thuộc diff trước khi sửa, đối chiếu hash, và giữ nguyên phần công việc U1–U2/P15 đã có.

Rollback GSAP bằng cách bỏ hai include và adapter, revert đúng phần selector/CSS pilot nếu có; hero tĩnh vẫn đủ nội dung. Alpine rollback trả component về bản native/Vanilla trước đó. htmx rollback bỏ attributes/config/script để href/action hiện có tiếp tục tải đầy đủ. Tailwind rollback bỏ stylesheet sinh thêm và trả class của component theo snapshot U1. Đóng gói JAR, so tài nguyên và chạy regression sau rollback. Không dùng reset/clean cho toàn working tree.

## 9. Thứ tự tích hợp và phạm vi xin duyệt

1. **Duyệt công nghệ/phạm vi** — chọn GSAP Core, chỉ phần trang trí C19, budget bổ sung và điều khoản license. Các bước còn lại là kế hoạch, chưa được thực thi.
2. **Chốt artifact** — xác minh release/pin/hash, tải local khi mạng cho phép, đo exact raw/gzip/Brotli và thử CSP trong fixture. Nếu artifact/policy không đạt điều kiện đã duyệt, đưa kết quả cụ thể để quyết định tiếp.
3. **Triển khai pilot C19** — giữ bản tĩnh, reduced-motion, xử lý vendor lỗi và cleanup. So sánh cùng hero có/không motion để owner đánh giá giá trị trực quan.
4. **Nghiệm thu riêng pilot** — năm viewport 360×800, 390×844, 768×1024, 1366×768, 1920×1080 ở zoom 100%/200%; keyboard, JS tắt, library bị chặn, reduced-motion và role regression. Chạy Maven JDK 21 test/package, báo PASS/FAIL/ERROR/SKIPPED thực tế. So size, CSP, tài nguyên JAR và số đo lab.
5. **Dừng để duyệt mở rộng** — chỉ sau pilot mới cân nhắc C20 hoặc motion khác. htmx cần yêu cầu duyệt catalog cụ thể. Alpine cần chứng minh state phức tạp hơn. Tailwind cần benchmark lợi ích trước đề nghị.
6. **Cổng dữ liệu demo riêng** — tiếp tục F01 và MariaDB test độc lập theo phạm vi owner cho phép. Không seed, sửa giá/tồn hoặc thay workflow để tạo bằng chứng cho thư viện.

## 10. Kết quả lượt audit này

Đã rà source, đối chiếu C01–C30, đo byte/gzip/Brotli của asset hiện tại và kiểm tài liệu chính thức về giấy phép, CSP, cách cài/build. Các kích thước GSAP/htmx lấy từ metadata upstream, Alpine và CSS Tailwind chưa có số đo artifact cho BeautyShop. CSP HTTP demo cũng chưa xác minh do timeout.

Chỉ thêm báo cáo này vào repo. Source, pom.xml, Design System và hai báo cáo P15 được đối chiếu hash trước/sau. Không cài dependency, không cập nhật component PASS và không chạy lại Maven/browser cho lượt tài liệu. Baseline 58 PASS / 34 SKIPPED nêu ở đầu là kết quả B1.4 đã có.

**Kết thúc audit ban đầu:** đã trình owner duyệt GSAP Core cho pilot C19 và phạm vi nêu ở bước 1. Quyết định phê duyệt và kết quả bước tiếp theo được ghi ở mục 11.

## 11. Pilot GSAP C19 đã duyệt · kết quả gate 22/11/2026

Owner đã phê duyệt GSAP Core, chỉ ba phần trang trí trong `.shop-hero-art`, tải local và riêng Home, tổng core + adapter không vượt **35 KiB gzip (35.840 byte)**. Điều kiện dừng bắt buộc là không xác minh được artifact/license hoặc vượt budget. Đây là phạm vi đã duyệt, không phải đề nghị xin duyệt lại.

Đã kiểm metadata tại tag **3.15.1** và đọc Standard “No Charge” License hiện hành. Hai lần tải trực tiếp tarball npm và `dist/gsap.min.js` từ GitHub Raw đều lỗi **curl exit 7, không kết nối được cổng 443**. Không có artifact local để đối chiếu integrity, ghi SHA-256, kiểm notice/license đi kèm hoặc đo raw/gzip/Brotli. Version này là ứng viên cố định, chưa được cài vào runtime; stable/dist-tag npm chưa xác minh. [Metadata tagged](https://raw.githubusercontent.com/greensock/GSAP/3.15.1/package.json) · [License](https://gsap.com/community/standard-license/)

**Gate BLOCKED, dừng trước Giai đoạn 2.** Chưa tạo vendor, adapter, include script, package.json hoặc bundler. JavaScript bổ sung vào source hiện là 0 byte do chưa tích hợp; không dùng con số đó để chấm đạt budget. CSP source chưa có khai báo tường minh; GET localhost:8080 trong lượt này cũng lỗi kết nối exit 7 nên CSP thực tế là NOT VERIFIED. Không nới CSP/Spring Security.

Không chạy Maven test/package, Chrome hoặc MariaDB trong lượt gate bị chặn này. **Không có số PASS/FAIL/ERROR/SKIPPED mới và không có ảnh animation mới.** Baseline B1.4 58/0/0/34 là lịch sử. Ma trận vẫn 20/30 PASS, core 10/12; C19 PASS của hero tĩnh không xác nhận pilot GSAP. F01 vẫn BLOCKED. Source, pom.xml, Design System và hai báo cáo P15 được đối chiếu hash; thay đổi chưa commit được bảo toàn.

Chi tiết nguồn tải, log, phép đo chưa thực hiện, các ca Chrome NOT RUN, điều kiện tiếp tục và rollback nằm tại [gsap-c19-pilot.md](gsap-c19-pilot.md). Cần artifact chính thức có provenance/integrity đối chiếu được trước khi tiếp tục cùng phạm vi đã duyệt. Không commit/push hoặc mở rộng giai đoạn.

## 12. Pilot C19 tiếp tục với artifact đã duyệt · 23/11/2026

Owner cung cấp file GSAP Core **3.15.1** tại D:\Downloads và expected SHA-256. Hash thực tế, header version/license và file trong JAR đều khớp **3D1BEA374AB1B703CC5C4105358EEC63C4465EFDC573931DC116E34A0A190C1B**. Giữ vendor nguyên byte và ghi rõ provenance nhận từ owner, không khẳng định đã tái tải/kiểm tarball npm độc lập. [Provenance](../../src/main/resources/static/js/vendor/gsap-3.15.1.provenance.md)

Đã đo core **73.357 raw / 28.511 gzip-9 / 25.842 Brotli-11 byte**; adapter cuối **1.483 / 678 / 538 byte**. Tổng gzip **29.189 ≤35.840 byte**, còn 6.651 byte. Dùng Node v26.13.0/zlib/Brotli tích hợp; đây là nén offline, chưa chứng minh HTTP deployment compression. Không thêm plugin ngoài core, package.json, bundler hoặc CDN runtime.

Chỉ Home include hai script defer local. Adapter chỉ animate arch/vessel/jar 6 px + opacity, chuỗi 380 ms, không loop; H1/CTA giữ tĩnh. Reduced motion từ đầu/đổi runtime, vendor lỗi, JS disabled, pagehide revert và pageshow persisted mô phỏng đều đã kiểm. Chờ hai frame trước animation để tách khỏi header đổi layout. Token, CSS và app.js chung không thay đổi.

Chrome Headless 158.0.8127.36 trực tiếp trên Windows theo chấp thuận bổ sung của owner sau khi kênh ChatGPT for Chrome không khả dụng. Bộ pilot **150 bản ghi**: 10 tổ hợp hero và 140 lượt trang/role × viewport/zoom. Bộ regression cuối **100/100 tổ hợp**, 30 kiểm tra luồng. Không tràn ngang hoặc Runtime.exceptionThrown. 110 lượt ngoài Home không request GSAP/adapter. 10 cặp ảnh hero tĩnh/kết thúc byte-identical; có ảnh chuyển động thật và ảnh cận hero tại mobile 200%. [Bằng chứng](../ui/screenshots/byredo-inspired/GSAP-C19/)

Maven JDK 21.0.13 cuối: **95 test =61 PASS +0 FAIL +0 ERROR +34 SKIPPED**. Package JAR thành công; vendor/adapter trong JAR có hash đúng. 34 SKIPPED là MariaDB, không bật P01_MARIADB_TEST hoặc ghi beautyshop. Bộ browser dùng MVC/Thymeleaf/Security thật với repository fixture, không thay thế MariaDB.

CSP fixture không có header. Policy QA cô lập `script-src 'self'; style-src 'self'` cùng default-src và các directive còn lại ghi trong báo cáo pilot chạy animation/revert với **0 violation**; không cần unsafe-inline/eval. Endpoint localhost:8080 vẫn không kết nối được, **CSP production NOT VERIFIED**, không đổi Security_Config.

Hero nghiệm thu cuối ghi CLS=0 ở mười tổ hợp. Lượt chẩn đoán riêng thấy header cũ có thể gây CLS 0,01564038499 ngay cả khi chặn GSAP; adapter đầu có mẫu 0,01744504480. Sau chờ hai frame, 24 lượt đối chứng chỉ còn 0 hoặc mức header cũ, không còn mẫu tăng. Không công bố toàn site CLS=0 hoặc điểm Lighthouse. [Chi tiết CLS, CSP, test và ảnh](gsap-c19-pilot.md#ket-qua-tiep-tuc)

Ma trận vẫn **20/30 PASS, core 10/12**; không sửa hai báo cáo P15 hoặc nâng component khác. **F01 BLOCKED**. MariaDB, screen reader, thiết bị thật, field performance và nghiệm thu thẩm mỹ của owner còn mở. Rollback chỉ bỏ hai include Home, adapter và vendor/provenance mới theo diff pilot; giữ hero tĩnh và các thay đổi chưa commit. Dừng tại C19, không commit/push hoặc mở rộng công nghệ/phạm vi.
