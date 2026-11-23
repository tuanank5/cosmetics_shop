# P15-V · Visual Fidelity Audit — BeautyShop

**23/11/2026 · Chỉ audit, chưa sửa source.** Đã chụp mới Home, Catalog và PDP ở desktop1366×768/mobile390×844. Chẩn đoán thị giác BeautyShop có bằng chứng; **so sánh thị giác trực tiếp với Byredo còn BLOCKED** vì cả sáu lần mở bằng Chrome đều trả `net::ERR_NETWORK_ACCESS_DENIED`. Không có screenshot Byredo render thành công. Các hướng thiết kế dưới đây là **đề xuất BeautyShop để owner duyệt**, không phải số đo hoặc bản dựng lại Byredo.

Nhận định chính: storefront hiện tại nhất quán về màu và control, nhưng trọng tâm là văn bản hướng dẫn, form và các hộp nội dung. Hình ảnh thương phẩm chưa đóng vai trò chủ đạo trong bộ dữ liệu có thể quan sát. Serif được dùng cả cho hero, tên sản phẩm, tiêu đề nhóm và footer; các đường viền, thẻ trắng, badge và nút có trọng lượng gần nhau. Vì vậy nhịp trang giống một cửa hàng theo component hơn một trải nghiệm biên tập được art-direct. **Đây là đánh giá trên BeautyShop, chưa phải kết luận “Byredo dùng/không dùng” các pattern đó.**

## 1. Nguồn, phương pháp và ranh giới bằng chứng

Đã đối chiếu AGENTS, [Design System U1](../ui/design-system.md), [Byredo reference B0](byredo-reference-analysis.md), toàn bộ lịch sử B0/B1.1–B1.4/B2/B4 trong [báo cáo triển khai](byredo-inspired-ui-implementation.md) và [ma trận](byredo-component-gap-analysis.md), [audit công nghệ frontend](frontend-technology-enhancement.md), [pilot GSAP C19](gsap-c19-pilot.md). Các kết quả cũ được đọc theo thời điểm: lỗi footer và F07 trong các mục lịch sử đã có xử lý ở B1.4/B4; không mở lại chúng như lỗi mới.

Đọc frontend Home/Catalog/PDP và fragments head/header/footer/product-card, CSS tokens/base/components/storefront/admin, app.js, checkout.js, home-motion.js; kiểm điểm dùng chung với checkout. Source hiện có54 template. Kiến trúc vẫn MVC/Thymeleaf, CSS U1 và Vanilla JS; GSAP chỉ ở Home. Inventory SHA-256 trước audit lưu tại target/p15-v/before.csv, Git inventory/diff ở cùng thư mục. Mọi mã ứng dụng, test, dependency, Design System và báo cáo sẵn có phải khớp hash cuối lượt.

### Môi trường chụp mới

- Chrome Headless158 trên Windows, profile QA riêng, CDP trực tiếp theo chấp thuận trước của owner. Kênh browser tích hợp trả “No browser is available”, discovery trả danh sách rỗng. Không chờ extension hoặc giả nhận đã dùng Computer Use.
- Viewport1366×768 và390×844, zoom100%, DPR1, Guest, menu mặc định, JS bật. ClientWidth1351/375 do scrollbar15px. Đây là mô phỏng kích thước mobile trên Chrome desktop, không phải kiểm thiết bị vật lý hay mobile Safari.
- Dùng fixture Tomcat/JDK21 với MVC/Thymeleaf/Security và template/assets trực tiếp từ working tree; repository trong bộ nhớ. Loại DataSource/JPA/Flyway. Không POST/login/đặt đơn trong audit này, không ghi database hoặc thay dữ liệu ứng dụng.
- Fixture kế thừa B4, lưu riêng target/p15-v; tên/giá27 dòng catalog lấy từ hồ sơ nghiên cứu dự án. URL ảnh/tồn/ID là kịch bản kỹ thuật. Ảnh PNG tải được là screenshot FAQ của chính dự án, **không phải ảnh mỹ phẩm**; các trường hợp khác là thiếu URL/404/decode để kiểm fallback. PDP dùng demo-r026?variant=22 không có ảnh.
- GET localhost:8080/ hiện không kết nối được, HTTP000/curl7. Vì vậy không có screenshot endpoint demo/MariaDB thật mới. Không suy ra rằng mọi sản phẩm thực đều thiếu ảnh từ fixture. **F01 giữ BLOCKED.** [Log HTTP](../ui/screenshots/byredo-inspired/P15-V/demo-http.log)

Ảnh chụp sau700ms để tween C19 kết thúc/revert; không tắt, đổi vendor hoặc adapter GSAP. Mỗi trang có ảnh đầu viewport, toàn trang và footer; có thêm vùng card/filter/PDP buy. Capture full-page có thể kích hoạt lazy images khi mở rộng vùng capture; nhận xét vị trí đầu trang dùng ảnh `*-top.png` và số đo trước full capture. Không sửa DOM/style để làm ảnh đẹp hơn. Mở filter mobile là thao tác qua thuộc tính native details trong phiên QA, chỉ để chụp trạng thái mở.

## 2. Bộ ảnh BeautyShop hiện tại

Đây là **baseline trước thiết kế lại**, không phải before/after của một thay đổi vừa triển khai. Đã xem ảnh ở cả hai kích thước và toàn trang mobile, vùng card, vùng mua PDP, footer desktop.

### Home

- Desktop: [Đầu trang](../ui/screenshots/byredo-inspired/P15-V/home-1366-top.png) · [Toàn trang](../ui/screenshots/byredo-inspired/P15-V/home-1366-full.png) · [Footer](../ui/screenshots/byredo-inspired/P15-V/home-1366-footer.png).
- Mobile: [Đầu trang](../ui/screenshots/byredo-inspired/P15-V/home-390-top.png) · [Toàn trang](../ui/screenshots/byredo-inspired/P15-V/home-390-full.png) · [Footer](../ui/screenshots/byredo-inspired/P15-V/home-390-footer.png).

### Catalog

- Desktop: [Đầu trang](../ui/screenshots/byredo-inspired/P15-V/catalog-1366-top.png) · [Lưới](../ui/screenshots/byredo-inspired/P15-V/catalog-1366-grid.png) · [Toàn trang](../ui/screenshots/byredo-inspired/P15-V/catalog-1366-full.png) · [Footer](../ui/screenshots/byredo-inspired/P15-V/catalog-1366-footer.png).
- Mobile: [Đầu trang](../ui/screenshots/byredo-inspired/P15-V/catalog-390-top.png) · [Lưới](../ui/screenshots/byredo-inspired/P15-V/catalog-390-grid.png) · [Filter mở](../ui/screenshots/byredo-inspired/P15-V/catalog-390-filters-open.png) · [Toàn trang](../ui/screenshots/byredo-inspired/P15-V/catalog-390-full.png) · [Footer](../ui/screenshots/byredo-inspired/P15-V/catalog-390-footer.png).

### PDP

- Desktop: [Đầu trang](../ui/screenshots/byredo-inspired/P15-V/pdp-1366-top.png) · [Vùng mua](../ui/screenshots/byredo-inspired/P15-V/pdp-1366-buy.png) · [Toàn trang](../ui/screenshots/byredo-inspired/P15-V/pdp-1366-full.png) · [Footer](../ui/screenshots/byredo-inspired/P15-V/pdp-1366-footer.png).
- Mobile: [Đầu trang](../ui/screenshots/byredo-inspired/P15-V/pdp-390-top.png) · [Vùng mua](../ui/screenshots/byredo-inspired/P15-V/pdp-390-buy.png) · [Toàn trang](../ui/screenshots/byredo-inspired/P15-V/pdp-390-full.png) · [Footer](../ui/screenshots/byredo-inspired/P15-V/pdp-390-footer.png).

Tổng23 ảnh BeautyShop,6 ảnh lỗi truy cập tham chiếu,29PNG. [Số đo6 trang](../ui/screenshots/byredo-inspired/P15-V/beautyshop-measurements.json) · [Capture harness](../ui/screenshots/byredo-inspired/P15-V/capture.mjs) · [Log](../ui/screenshots/byredo-inspired/P15-V/capture.log).

## 3. Byredo: truy cập được gì, chưa so sánh được gì

Ba URL được thử ở **cả hai viewport**:

- [Home US](https://www.byredo.com/us_en).
- [Holiday collection](https://www.byredo.com/us_en/c/holiday-selection).
- [PDP Mojave Ghost](https://www.byredo.com/us_en/p/mojave-ghost-eau-de-parfum), URL tìm được từ nguồn chính thức, không tự đoán slug.

Cả6 navigation trả ERR_NETWORK_ACCESS_DENIED. [Log từng URL/viewport](../ui/screenshots/byredo-inspired/P15-V/reference-access.json). Các PNG `reference-attempt-{home|catalog|pdp}-{1366|390}.png` là **trang lỗi Chrome**, không phải ảnh Byredo và không được đặt cạnh BeautyShop như cặp so sánh hợp lệ.

Công cụ đọc web truy cập được nội dung công khai của ba URL. Home có các entry điều hướng và nhóm dịch vụ/footer; collection có tiêu đề, count, filter/grid controls và danh sách sản phẩm; PDP có ảnh, tên, lựa chọn dung tích và nội dung sản phẩm. Đây chỉ là quan sát nội dung/structure từ tài liệu đã trích xuất. **Không đủ xác nhận cột, lề, hierarchy thị giác, font render, màu, crop, chiều cao, sticky hay tương tác.** Bản đọc PDP và kết quả tìm kiếm còn khác về việc có hiện giá/CTA, nên không dùng snapshot đọc web để chốt trạng thái mua hàng hoặc bố cục. Các URL nguồn chính thức ở trên phục vụ truy xuất, không làm bằng chứng pixel.

**Chưa có so sánh trực tiếp** đối với: header; hero; typography; layout; grid; ảnh/crop; tỷ lệ khối; khoảng trắng; màu; nút; filter; product card; footer. Không có điểm visual fidelity, tỷ lệ tương đồng, pixel diff hoặc font Byredo được xác nhận trong audit này. Không tải logo, font, ảnh chiến dịch hoặc asset sản phẩm Byredo vào ứng dụng.

### Chính xác ảnh owner cần cung cấp

Tối thiểu **6 cặp ảnh (12PNG)**: mỗi URL dưới đây tại1366×768 và390×844 gồm một ảnh **đầu trang đúng kích thước viewport** và một ảnh **toàn trang với cùng chiều rộng/viewport khi chụp**:

1. Home `https://www.byredo.com/us_en`: desktop top/full, mobile top/full — cần thấy header, hero, trình tự các khối và footer.
2. Collection `https://www.byredo.com/us_en/c/holiday-selection`: desktop top/full, mobile top/full — cần thấy heading, toolbar, hàng card đầu, các khối giữa trang và footer.
3. PDP `https://www.byredo.com/us_en/p/mojave-ghost-eau-de-parfum`: desktop top/full, mobile top/full — cùng một dung tích được chọn, cần thấy media, vùng mua, nội dung và footer. Nếu owner muốn PDP khác, gửi URL thực và cặp ảnh cùng SKU/dung tích thay thế.

Để duyệt luôn giao diện mở: bổ sung4 ảnh viewport (menu mở desktop/mobile, filter collection mở desktop/mobile). Nếu full-page capture làm media/ảnh lazy sai vị trí, dùng chuỗi ảnh cuộn có phần chồng lấn thay thế, ghi scroll offset.

Chụp zoom100%, ghi browser/OS, ngày, locale US English, viewport CSS/DPR, trạng thái cookie/region banner và menu/filter; không resize PNG sau chụp. Đóng banner che nội dung theo cùng cách; nếu không đóng được, giữ và ghi rõ. Đây là ảnh tham khảo nội bộ để phân tích bố cục, **không cấp quyền tái sử dụng asset**. Chưa thể chốt thiết kế là “giống Byredo” trước khi nhận/quan sát được bộ ảnh này.

## 4. Số đo BeautyShop và phân tích từng vùng

Số dưới đây đo trên fixture của BeautyShop, đơn vị CSS px, làm tròn0,1. **Không phải số đo Byredo.**

- Header Guest:113,4px desktop;109,4px mobile ở cả3 trang. Đầu hero Home:y161,4; mobile:y141,4. Khối hero cao511,9/664,1; art bắt đầu y162,4/589,5, riêng art mobile cao216px. Khoảng70% chiều cao màn hình mobile đã đi qua trước khi hình xuất hiện.
- H1 cả3 trang:54,64px desktop và36px mobile. Chrome CSS domain xác nhận font thực tế H1 là **Palatino Linotype**, không phải hai tên webfont đầu stack. Không có webfont đóng gói. Hình thức chữ trên máy khác chưa được so sánh.
- Catalog grid bắt đầu:y410,9 desktop/y500,0 mobile. Desktop có3 track khoảng266,7px sau sidebar256px và gap32px; mobile có2 track163,5px. Vùng media card4:5; PDP media1:1. Trên desktop, ảnh card chiếm gần hết phần dưới fold; brand/tên/giá ở thấp hơn.
- PDP: media desktop540,7px vuông; mobile343px vuông. Vùng mua bắt đầu y202,4/y553,4; vùng CTA y622,8/y950,5. Ở mobile, CTA nằm sau fold844px dù tên/giá đã thấy. Đây là cơ hội giảm chiều cao phần chọn SKU, không phải lý do tự thêm sticky bar.
- Footer:267,5px desktop;742,7px mobile, gần một viewport844px. Cả6 trang có scrollWidth bằng clientWidth; không ghi Runtime.exceptionThrown trong lượt capture. Không suy rộng kết quả này thành QA mọi vai trò/zoom200%.

### Header và navigation

Wordmark serif hai màu, ô search dài, nút “Tìm kiếm”, summary “Đóng menu” và hàng link phía dưới đều có trọng lượng rõ. Desktop tìm kiếm trông như tác vụ chính của một biểu mẫu; còn mobile search luôn chiếm hàng thứ hai. Cart Guest nằm trong menu đóng ở mobile. Header đã gọn hơn baseline B0 và không có regression chiều cao mới, nhưng cảm giác thị giác vẫn thiên về thanh công cụ. [Header source](../../src/main/resources/templates/fragments/header.html) · [CSS header](../../src/main/resources/static/css/storefront.css#L59)

Hướng cần duyệt: giảm trọng lượng đường viền/search, sắp lại utility và navigation có nhịp rõ; cart/account vẫn dễ tìm. Có thể dùng search disclosure native với form GET giữ nguyên, thay vì ô dài luôn mở. Đó là thay đổi tương tác cần QA riêng C01–C08; không tự triển khai trong audit.

### Hero và tỷ lệ khối Home

Desktop hiện là text6fr/art5fr trong container1200px, nội dung thực1136px. Mobile xếp copy trước, rồi hai CTA rộng, cuối cùng mới thấy art. Minh họa chai/hũ CSS là đồ họa có chủ ý và không giả làm sản phẩm thật, nhưng hình phẳng cộng nền beige và đường viền champagne tạo cảm giác một theme mỹ phẩm tổng quát. Nó chưa cho thấy chất liệu, ánh sáng hoặc câu chuyện sản phẩm nhiều thương hiệu. [Home](../../src/main/resources/templates/home/index.html) · [Hero CSS](../../src/main/resources/static/css/storefront.css#L223)

Không thể kết luận art này “khác hero Byredo bao nhiêu” khi chưa có ảnh tham chiếu. Điểm có thể sửa về composition là đưa hình lên sớm hơn trên mobile, giảm phần copy, tăng chủ đích cho khoảng trống và chỉ giữ một CTA chiếm ưu thế.

### Typography

H1, h2, h3, brand và footer đều dựa vào serif; card mobile tên18px, desktop22px. Serif dài nhiều dòng cùng nhãn sans/giá đậm/badge xanh tạo nhiều điểm nhấn cục bộ. Tên skincare dài làm sự khác biệt giữa card rõ hơn. Không có cơ sở để nói Byredo dùng font nào, hoặc thay Palatino bằng một font “giống” chưa xác minh. [Base](../../src/main/resources/static/css/base.css) · [Tokens](../../src/main/resources/static/css/tokens.css)

Hướng cần duyệt: dành serif cho tiêu đề biên tập có chọn lọc; tên sản phẩm/control/giá dùng stack body U1, weight và cỡ ổn định. Giữ tiếng Việt, đọc tốt và không che tên đầy đủ. Nếu owner yêu cầu font render đồng nhất đa nền tảng, cần một quyết định font hợp pháp/self-host riêng, license/subset/dung lượng/CLS riêng; không tự thêm font ở đợt này.

### Catalog: layout, filter và nhịp khoảng trắng

Heading lớn + mô tả + đường chia, sidebar trắng bo góc chứa5 control và heading “Kết quả” + count + progress khiến phần trên có nhiều lớp dẫn nhập. Sidebar cố định theo bố cục desktop làm grid còn3 cột. Mobile đóng filter tốt, nhưng summary card cao khoảng80px cùng heading/khoảng cách đẩy ảnh đầu tới y500. Nhịp trang ưu tiên công cụ hơn nội dung hàng hóa. Đây là chọn lựa hình thức, không phải lỗi đếm/filter B2. [Catalog](../../src/main/resources/templates/product/list.html) · [CSS](../../src/main/resources/static/css/storefront.css#L329)

Hướng cần duyệt: một heading gọn, count/progress thứ cấp, toolbar/filter disclosure nằm ngang trên desktop, mobile disclosure mỏng hơn về cảm giác nhưng target≥44px. Cùng một form GET và cùng ID anchor; không nhân đôi form desktop/mobile hoặc bỏ chips/size/page logic. Grid sử dụng chiều rộng được giải phóng.

### Product card và ảnh

Mỗi card có viền ngoài, nền trắng, ô ảnh beige, đường chia ảnh/body, tên serif, giá burgundy đậm và badge đóng khung. Lặp12 lần tạo cảm giác danh mục “hộp sản phẩm”. Monogram B lớn lặp lại nhấn mạnh thiếu nội dung. Ảnh FAQ trong card đầu là đồ QA, khiến không thể đánh giá ánh sáng, tỷ lệ vật thể hay hình nhiều thương hiệu một cách hợp lệ. **Không dùng ảnh này để chấm chất lượng ảnh sản phẩm thực.** [Card](../../src/main/resources/templates/fragments/product-card.html) · [CSS](../../src/main/resources/static/css/storefront.css#L379)

Hướng cần duyệt: card phẳng, phân tách bằng khoảng cách; khung media nhất quán, `contain`, label thương hiệu nhỏ nhưng đủ đọc, tên đầy đủ, giá có một mức nhấn. “Hết hàng” rõ bằng chữ; “Còn hàng” có thể giảm trọng lượng viền/màu mà không xóa trạng thái. Placeholder trung tính rõ “Chưa có ảnh”, không tạo chai mỹ phẩm giả hoặc thay ảnh tốt bằng ảnh sinh. Giữ nguyên hidden/error handling và eager/lazy B2. Chưa thêm quick add/wishlist/card hover đổi ảnh vì dữ liệu và phạm vi chưa cho phép.

### PDP: layout, nút và nội dung

Hai cột đều nhau là nền tảng tốt. Nhưng ảnh placeholder lớn, H1 desktop gần55px, dòng SKU và các tile biến thể có cả tên/SKU/giá/tồn/đang chọn khiến phần mua giống bảng so sánh dữ liệu. Các dữ liệu này cần giữ; sự lặp và độ đậm cần được biên tập lại. CTA56px rõ ràng, không cần thu nhỏ để tạo “luxury”. Fixture không có mô tả/thành phần/hướng dẫn nên các phần này không render; chỉ thấy review empty. Không kết luận dữ liệu sản phẩm thực không có nội dung. [PDP](../../src/main/resources/templates/product/detail.html) · [CSS](../../src/main/resources/static/css/storefront.css#L115)

Hướng cần duyệt: media lớn, vùng mua hẹp hơn ở desktop; brand/tên/giá thành một nhóm, SKU phụ, tile biến thể gọn nhưng giữ giá/tồn và trạng thái chọn/hết. GET variant vẫn là nút submit; không đổi select để tự điều hướng. Mobile giảm chiều cao thông tin lặp và điều chỉnh media theo ảnh thật; CTA trong luồng, không sticky tự động. Nội dung thật dùng các accordion hiện có, không tạo review/chứng nhận/lợi ích mới.

### Màu, nút, spacing và footer

Kem/nâu/burgundy/champagne đang nhất quán với U1. Vấn đề không nằm ở việc “chưa đủ đen trắng”, mà ở mật độ nền beige, viền và nhiều loại chữ nhấn. Hai CTA hero đặt cạnh nhau desktop, cùng full-width mobile; card “Sản phẩm mới” bên dưới lại chỉ có chữ và một nút. Các khối kết nối bằng khoảng48px khá đều, ít thay đổi nhịp kể chuyện. Footer tối có wordmark lớn và nhiều nhóm serif, thành một khối thị giác nặng trên mobile. Link đúng role và reflow B1.4 cần giữ. [Footer](../../src/main/resources/templates/fragments/footer.html) · [Button](../../src/main/resources/static/css/components.css)

Hướng cần duyệt: giữ màu thương hiệu, dùng burgundy tiết chế ở CTA/focus/selected; champagne chỉ trang trí. Giảm viền trên bề mặt nội dung, không xóa viền/focus control. Giữ primary CTA56px, target44px tối thiểu. Footer tối vẫn là nhận diện BeautyShop; giảm khối chữ trang trí và nhịp khoảng cách, cân nhắc nhóm native disclosure mobile sau khi xem tham chiếu và kiểm no-JS. Không giả thêm newsletter/địa chỉ/liên hệ/chính sách chưa có nguồn.

## 5. Các nguyên nhân ưu tiên làm giảm cảm giác editorial

1. **Hình ảnh chưa đủ cơ sở nghiệm thu**: technical fixture/placeholder chiếm vùng hình. Đây là blocker nội dung và F01, không sửa bằng CSS hay GSAP. Cần ảnh có quyền, đúng sản phẩm/SKU và crop được duyệt.
2. **Hero copy/CTA lấn nhịp hình trên mobile**: art bắt đầu y589,5; hai nút lớn trước hình. Giảm text/CTA phụ và tái tổ chức composition có tác động trực tiếp hơn thêm chuyển động.
3. **Nhiều khung và trọng lượng nhấn giống nhau**: sidebar card, product card, badge, đường champagne, serif và giá đậm làm nội dung bị chia vụn.
4. **Catalog giống biểu mẫu tìm hàng**: search trùng header, sidebar lớn, heading “Kết quả” tách riêng. B2 đã đúng chức năng; bước tiếp theo cần thay nhịp bố cục, không tăng số tính năng.
5. **Typography chưa có vai trò đủ riêng**: cùng họ serif trên nhiều cấp làm điểm nhấn biên tập không còn đặc biệt; tên font khai báo chưa bảo đảm font thực tế.
6. **PDP tập trung chứng minh dữ liệu hơn biên tập thông tin**: SKU/giá/tồn lặp nhiều lần, dù dữ liệu đúng. Cần giảm trọng lượng phụ, giữ đầy đủ tính rõ ràng.
7. **Home ít chất liệu nội dung**: “Sản phẩm mới” là link chứ không có product story/grid; không có lý do backend/content để tự tạo gallery mới. Không đưa thêm dữ liệu giả để lấp khoảng trống.

Đây là thứ tự vấn đề của BeautyShop, **không phải điểm chênh lệch đã đo so với Byredo**. Coverage chức năng hiện có không chứng minh hoặc bác bỏ bất kỳ nhận xét thị giác nào ở trên.

## 6. Hướng thiết kế đề nghị owner duyệt

### Nguyên tắc chung

Đề nghị **editorial tiết chế, ưu tiên hình và thông tin sản phẩm**, giữ tên BeautyShop, nền ngà/chữ nâu/burgundy U1, nhiều thương hiệu và tiếng Việt. Không đổi token toàn cục trong bước đầu. Không coi trắng/đen, font mảnh hoặc khoảng trống lớn đơn lẻ là bằng chứng luxury.

Mục tiêu số học dưới đây là **brief BeautyShop đề xuất**, chưa duyệt và không lấy từ Byredo: lề mobile16px; lưới theo nhịp U1 8/16/24/32/48/72px; phần biên tập có thể72–96px desktop nếu nội dung đủ. Khoảng96px cần được duyệt như cách dùng nhịp, không tự bổ sung token. Heading mobile32–36px, desktop40–48px là điểm bắt đầu để so ảnh; text/control16px giữ U1. Không điều chỉnh chỉ để ép số đo.

### Home đề xuất

Desktop: header gọn về trọng lượng → hero một thông điệp và một CTA chính, art rộng rãi → ba lối khám phá ít khung → khối “Sản phẩm mới”/hỗ trợ như các đoạn biên tập thay vì card trắng → footer nhẹ hơn. Thay CTA phụ hero bằng text link vẫn target44px; copy rút gọn cần owner duyệt.

Mobile: header/utility → hero có art ở nửa trên của khối hoặc copy ngắn hơn để art xuất hiện trước y420px trên fixture390×844 → CTA chính → khám phá và hỗ trợ. Đây là mục tiêu composition cần thử bằng mockup; không đảo visual order khác DOM/keyboard.

**Ràng buộc GSAP:** giữ nguyên vendor/adapter, selector `.shop-hero-art`, ba phần arch/vessel/jar, aria-hidden, tween380ms và static/reduced-motion fallback. Có thể đề xuất thay padding/grid/cỡ khung quanh chúng, không animate nội dung. Không xóa/ẩn ba mục tiêu để đổi sang hero ảnh rồi gọi là “bảo toàn GSAP”. Nếu owner muốn thay toàn bộ minh họa bằng campaign photo, phải duyệt riêng vai trò mới của C19/asset; không nằm trong phương án triển khai mặc định. Hiện chưa có ảnh chiến dịch BeautyShop được xác nhận quyền, nên brief dùng **placeholder bố cục được ghi rõ**, không tạo chiến dịch/sản phẩm giả. Một hero ảnh hoàn toàn mới là quyết định chưa được duyệt, không thể hứa mức giống Byredo khi vẫn giữ minh họa hiện tại.

### Catalog đề xuất

Desktop: heading ngắn → toolbar kết quả/filter → chips khi có → grid4 cột trên1366 nếu tên/ảnh thật đọc tốt; chuyển3 cột nếu không đạt. Cố định thứ tự dữ liệu, không thêm density switch. Mục tiêu ban đầu đưa grid từ y411 về khoảng y300–350 trên baseline không chip. Filter vẫn native disclosure chứa nguyên form; mất sidebar không đồng nghĩa bỏ bộ lọc.

Mobile: heading → một dòng count/filter → chips →2 cột ảnh4:5. Mục tiêu đưa grid từ y500 về khoảng y340–400 ở baseline không chip. Khi có nhiều chips/zoom200%, cho nội dung nở và reflow, không giữ y bằng cách cắt dữ liệu. Ảnh/card phẳng, tên sans full text, thương hiệu nhỏ; không biến cả trạng thái tồn thành chỉ màu. Progress giữ số từ Page ở vị trí thứ cấp, pagination GET giữ đầy đủ params. Không thêm AJAX, sort tự submit hoặc dữ liệu thương mại.

### PDP đề xuất

Desktop: thử media khoảng55% và vùng mua45% với chiều rộng nội dung mua được giới hạn; tên40–48px, brand nhỏ, giá/tồn một cụm, SKU secondary. Cùng1 ảnh SKU hiện có; không gallery, zoom ảnh hoặc carousel không có dữ liệu. Tile SKU giữ toàn bộ thông tin nhưng giảm lặp emphasis/spacing. CTA56px giữ rõ và toàn chiều rộng vùng mua.

Mobile: art/media → brand/tên/giá → lựa chọn SKU → CTA → nội dung thật. Thử media khoảng300–320px với contain thay cho343px nếu ảnh thật không bị nhỏ; giảm tile spacing để CTA lên sớm hơn baseline y950,5. Không bắt buộc CTA trên fold bằng cách ẩn thông tin, không crop sản phẩm và không sticky che focus. Giá/tồn từ server và form GET/POST hiện tại giữ nguyên. Duyệt cả ảnh thật và trường hợp thiếu ảnh trước khi chốt tỷ lệ.

## 7. Tác động hệ thống và giới hạn thay đổi sau khi được duyệt

- **Thymeleaf Home/Catalog/PDP — trung bình:** thay wrapper/composition/vị trí nhóm nội dung; giữ biến model, condition dữ liệu, route, field, method, CSRF, GET variant và Page/chips/pagination. Không cần backend mới cho phương án trên. Home không có product list model, nên bổ sung sản phẩm thật ở Home cần phạm vi đọc dữ liệu được duyệt riêng; không hardcode sản phẩm.
- **CSS storefront — cao về thị giác, cục bộ về code:** chỉnh các block `.shop-home`, `.shop-catalog`, `.product-page` trong file hiện tại. Tái dùng U1, không mở theme/framework/file CSS song song. Không chỉnh toàn cục `.btn`, `h1/h2/h3`, `.card`, `.money`, `.page-content` để “đẹp nhanh”, vì tác động B4/cart/auth/Admin.
- **Header/footer dùng chung — cao về hồi quy:** mọi đổi visual có thể chạm Guest/Customer/Admin và checkout. Nếu owner duyệt header/search/footer mới, phải giữ một DOM/route/role contract và chạy lại B1.1–B1.4/B4. Có thể giữ Admin visual hiện tại bằng phạm vi storefront được xác định rõ, không suy vai trò từ JS.
- **JavaScript — thấp nếu giữ disclosure hiện có; trung bình nếu đổi search/menu:** giữ native details/no-JS, Escape/focus/resize, ID anchor và error fallback B2. Không chạm submit guard, quote invalidation B4 hoặc cart math. Không thêm thư viện, GSAP ngoài Home, endpoint hay request mới.
- **Responsive — cao:** thay tỷ lệ hero/grid/PDP ảnh hưởng thứ tự đọc, tên nhiều dòng, breakpoint và zoom. Hai ảnh audit không thay thế QA5 viewport/200% sau triển khai. Không dùng overflow:hidden, line-clamp tên hoặc font quá nhỏ để đạt bố cục.
- **Ảnh và font — có dependency quyền/nội dung:** cần ảnh hợp pháp, SKU mapping/crop/sizes nhất quán; metadata thật chưa được chứng minh bởi fixture. Thêm font tự host là quyết định riêng, phải có license và đo CLS/budget; không lấy từ Byredo.
- **Backend/security/database/payment — không thuộc diff đề xuất:** giữ B4 view-state F07, COD/MoMo, giá/tồn/BigDecimal và merge giỏ. Bất kỳ ý tưởng đòi dữ liệu/contract mới phải tách đề xuất, dừng trước sửa.

## 8. Thứ tự triển khai sau duyệt, không thực thi trong audit

1. **V0 — khóa reference và art direction:** nhận bộ ảnh ở mục3, chọn3 ảnh thật đa thương hiệu có quyền cho QA visual và một SKU có ảnh. Duyệt vai trò serif/body, mật độ grid, hero giữ GSAP và màu U1. Chưa đủ reference thì chỉ duyệt hướng BeautyShop, không nghiệm thu “giống Byredo”.
2. **V1 — bản thiết kế tĩnh6 màn hình:** Home/Catalog/PDP ×desktop/mobile với dữ liệu tương đương; ghi rõ placeholder. So từng vùng với reference tại cùng viewport. Owner duyệt trước khi code; không dùng thêm animation để che composition chưa đạt.
3. **V2 — Catalog/card trước:** làm một diff cục bộ để hiệu chỉnh ảnh/typography/khung/toolbar; giữ toàn bộ B2 assertions, no-JS và GET. Đây là nơi thấy dữ liệu nhiều thương hiệu rõ nhất.
4. **V3 — PDP:** áp dụng cùng nhịp ảnh/typography/CTA, kiểm SKU dài/hết/thiếu ảnh vàGuest/Customer, không sửa nghiệp vụ. Chỉ chuyển tiếp sau khi so ảnh và regressionV2 đạt.
5. **V4 — Home:** sửa composition/copy và art frame đã duyệt, giữ GSAP byte/hash/hành vi. Không tự đưa video/campaign/grid sản phẩm mới ngoài dữ liệu đang hỗ trợ.
6. **V5 — shared chrome nếu owner duyệt:** header/search/footer sau khi đã có hướng rõ, diff riêng vì rủi ro dùng chung cao. Kiểm Admin/checkout/cart, role links, focus/no-JS và F07; không ép checkout thành layout ảnh biên tập.
7. **V6 — nghiệm thu visual và regression:** cùng dataset,viewport/zoom/trạng thái, before/after/cận từng vùng; Maven JDK21 test/package và kiểm Chrome5 viewport/200% theo scope mới. Dừng mỗi diff để review, không cộng coverage để bỏ qua lỗi thị giác.

Các nhãn V0–V6 là kế hoạch P15-V đề xuất, không phải B3/B4 mới đã được cấp phép hoặc đang triển khai.

## 9. Tiêu chí nghiệm thu thị giác sau thiết kế lại

Chấm riêng cho **6 màn hình bắt buộc** và từng vùng ở mục4; trạng thái **ĐẠT / CẦN SỬA / CHƯA ĐỦ BẰNG CHỨNG**. Không tính phần trăm similarity hay lấy24/30 component làm số đo. Hiện cột đối chiếu Byredo của mọi vùng là CHƯA ĐỦ BẰNG CHỨNG.

- [ ] Reference đúng loại trang, viewport, trạng thái và ngày được đính kèm; before/after BeautyShop dùng cùng fixture/dataset/ảnh/font/zoom100%, menu đóng, trang catalog/page/filter và SKU giống nhau. Không so trang rỗng với collection có ảnh rồi chấm CSS.
- [ ] Header có một trục căn lề rõ với nội dung; wordmark/actions/search không cạnh tranh ngang hero; mọi control còn tìm được, role/current không đổi. Số đo tương đối được ghi từ ảnh reference, không nhớ hoặc đoán.
- [ ] Hero có một điểm nhìn chính, ảnh/art nhìn thấy đủ sớm, một CTA nổi trội; tỷ lệ copy/media và khoảng trống có chủ đích. Bản static/reduced-motion vẫn đạt cùng composition.
- [ ] Typography có vai trò nhất quán: editorial, product name, metadata, price/control. Ghi font thực qua browser, kiểm đủ dấu tiếng Việt, tên dài và xuống dòng; không tự coi font stack là font render.
- [ ] Grid/crop/card có cùng nhịp; ảnh thật không bị cắt nhãn/nắp, vật thể có tỷ lệ hợp lý giữa thương hiệu. Thiếu ảnh là fallback trung thực; không có icon lỗi ngoài giới hạn no-JS404 đã ghi B2.
- [ ] Tên/brand/giá/tồn đọc rõ mà không hình thành nhiều badge/khung nhấn ngang nhau. Không cắt tên hoặc bỏ stock để làm card thấp. Chiều rộng, gutter, heading-to-grid được đối chiếu ảnh và ghi nhận khác biệt có chủ ý.
- [ ] Filter/chips/progress dễ thấy nhưng không áp đảo sản phẩm. Khi filter mở/chips nhiều, bố cục vẫn có khoảng thở và giữ query. Empty/error có thiết kế đủ rõ, không tạo dữ liệu giả.
- [ ] PDP có thứ tự image→brand/name/price→SKU→CTA rõ; so cả viewport đầu và vùng mua. Price/stock/SKU chính xác, CTA56px, không che focus hoặc phải cuộn ngang.
- [ ] Footer kết trang nhẹ về nhịp, không là “màn hình hộp” lặp; tất cả link thật và đúng vai trò. Khác biệt màu nhận diện so với reference được owner chấp nhận, không mặc định phải đen trắng.
- [ ] Xem cạnh nhau và overlay50% trên **mockup đã được duyệt và BeautyShop mới cùng viewport** để kiểm alignment/tỷ lệ. Không đòi pixel identical với Byredo vì asset/nội dung/nhận diện khác. Lưu nhận xét theo vùng, không công bố điểm tự sinh.
- [ ] Không có issue thị giác mức chặn do owner chỉ ra ở6 màn hình; tất cả khác biệt chủ ý về brand/multibrand/VND/Tiếng Việt có giải thích và được duyệt. Owner nghiệm thu thẩm mỹ riêng với regression kỹ thuật.

**Cổng kỹ thuật bảo vệ chức năng** sau code:0 overflow ở5 viewport/100%/200%; keyboard/JS disabled/focus/error; B2 fallback/filter/chips/Page; U2 variant/cart; B4 quote/error/COD/MoMo fixture; Guest/Customer/Admin; GSAP chỉ Home và hash/hành vi giữ nguyên. Những cổng này cần thiết nhưng **không thay cho cổng thị giác**.

## 10. Kết quả thực thi audit, phần còn mở và điểm dừng

- **Đã thực thi mới:**6 trang BeautyShop được render/chụp,23PNG;6 navigation Byredo lỗi,6PNG lỗi kèm JSON; số đo DOM/font/overflow/scripts.0 Runtime.exceptionThrown trên6 trang BeautyShop,0 horizontal overflow ở2 viewport100% đã chụp. Không nhận là một bộ E2E checkout hoặc test mọi vai trò.
- **Không chạy lại Maven test/package** vì không sửa code; không có số PASS/FAIL/ERROR/SKIPPED mới. Kết quả B4 **118/0/0/34** và JAR là **lịch sử**, không gắn nhãn test mới của audit. Không kiểm MariaDB,không bật P01_MARIADB_TEST.
- **Chưa xác minh:** ảnh render Byredo và pixel/visual comparison; ảnh thương phẩm thật/nguồn cấp phép/SKU mapping; font đa OS; responsive200%/keyboard/role regression mới; screen reader; Lighthouse/field performance; MariaDB/F01; CSP production/MoMo/IPN thật. Không sửa các trạng thái ma trận hiện hành từ audit thị giác.
- **File mới:** báo cáo này và thư mục [bằng chứng P15-V](../ui/screenshots/byredo-inspired/P15-V/). Helper/snapshot/fixture chỉ trong target/p15-v; fixture đã đóng sau chụp, credential tạm được xóa. Không sửa source/pom/Design System/báo cáo P15 cũ, backend/form/CSRF/security/DB/B2/B4/GSAP. [Đối chiếu hash](../ui/screenshots/byredo-inspired/P15-V/preservation.json)
- **Kết luận nghiệm thu audit:** baseline và chẩn đoán BeautyShop đã có; việc chốt độ giống Byredo còn chờ ảnh reference ở mục3. Hướng đề nghị là bớt khung/giảm trọng lượng công cụ, tăng vai trò hình, phân vai typography và biên tập PDP, đồng thời giữ U1 và các hợp đồng hiện tại. Không hứa thay đổi một vài CSS sẽ giải quyết thiếu ảnh thương phẩm hoặc thay thế được tham chiếu chưa nhìn thấy.

**DỪNG P15-V tại audit để owner nghiệm thu hướng thiết kế và bổ sung reference. Chưa code, chưa triển khai B3 hoặc giai đoạn khác, không commit/push.**
