# P15 · B2 — BYREDO-INSPIRED LUXURY CATALOG

**BeautyShop · Implementation Brief for Codex · 22/11/2026**  
**Phạm vi duy nhất:** C12 (filter count), C15 (catalog image resilience), C18 (browsing progress).  
**Điểm bắt đầu được báo cáo:** 20/30 PASS = 66,67%; 10/12 core PASS; 58 PASS / 0 FAIL / 0 ERROR / 34 SKIPPED (JDK 21).  
**Kết quả kỳ vọng có điều kiện:** 23/30 PASS = 76,67%; 11/12 core PASS, nếu cả ba mục được nghiệm thu và không phát sinh regression.

---

## 0. Vai trò và nguyên tắc làm việc

Bạn là Principal Frontend Engineer, Spring MVC/Thymeleaf Engineer, Accessibility Specialist và QA Automation Engineer chịu trách nhiệm **triển khai trực tiếp B2** trên working tree hiện có của BeautyShop.

**Bắt tay sửa mã nguồn sau khi kiểm tra trạng thái ban đầu. Không dừng ở phân tích/kế hoạch. Chỉ sửa phần B2. Không tự chuyển sang B3/B4, không commit hoặc push.**

Đây là phiên bản tái tạo *design patterns* của trang [Byredo Holiday Selection](https://www.byredo.com/us_en/c/holiday-selection), không sao chép logo, ảnh, font độc quyền, nội dung quảng cáo hay mã nguồn Byredo. Giữ nhận diện BeautyShop, design tokens U1, Spring Boot + Thymeleaf + CSS + JavaScript thuần.

Bằng chứng Byredo trước đó chủ yếu dựa trên HTML công khai; trình duyệt trực tiếp đến website tham chiếu bị chặn. Không được tự tuyên bố so sánh pixel, font, màu hoặc animation với Byredo.

## 1. Kiểm tra nguồn sự thật trước khi sửa

Dự án: `E:\Nam 4 HK 1\WWW\cosmetics_shop`.

Đọc các file hiện có, tìm đúng đường dẫn nếu tài liệu được di chuyển:

- `AGENTS.md`.
- `P12-luxury-ui-upgrade.md` (bản P12 tại **gốc repo** đã được báo cáo xác nhận; không giả định có bản dưới `docs/prompts/`).
- `docs/prompts/P12-U2-luxury-storefront.md`.
- `docs/reviews/byredo-inspired-ui-implementation.md` — đặc biệt phần **B1.4**.
- `docs/reviews/byredo-component-gap-analysis.md` — trạng thái và bằng chứng C12, C15, C18.
- `docs/reviews/byredo-reference-analysis.md`.
- `docs/reviews/ui-upgrade-U2-2026-11.md`.
- `docs/ui/design-system.md`.
- `src/main/resources/templates/product/list.html`.
- `src/main/resources/templates/fragments/product-card.html`.
- `src/main/resources/templates/fragments/ui.html`.
- `src/main/resources/static/css/storefront.css`.
- `src/main/resources/static/js/app.js`.
- `CuaHang_Controller`, `CuaHang_Service`, `CuaHang_Repository` và projection `TheSanPham`.
- `GiaoDienPagesTest` cùng các test catalog/Thymeleaf liên quan.

Ghi lại `git status --porcelain=v1 -uall`, `git diff --check` và diff/status của các file dự kiến sửa. **Working tree đang có nhiều thay đổi chưa commit từ U1/U2/B1.1–B1.4**: không reset, clean, checkout hoặc overwrite các thay đổi ấy. Không ghi log secrets.

Xác minh thực tế trước khi thay đổi:

1. Hợp đồng GET `/products` và các parameter `q`, `brand`, `category`, `skinType`, `sort`, `page`, `size`.
2. Cách `Page<TheSanPham>` được đưa vào model, giá trị phân trang bắt đầu từ 0 hay 1, và cách fragment pagination giữ tham số.
3. CSS/JS đang xử lý ảnh lỗi ở PDP/cart và liệu có thể dùng lại cho card catalog.
4. Semantics của chip hiện tại: search `q` có được xem là một filter hay được biểu diễn riêng.
5. Header/footer mới của B1.4: **bảo toàn**, không viết lại.

Nếu thực tế khác báo cáo, ưu tiên mã nguồn hiện hành; ghi rõ sai khác và xử lý tối thiểu trong phạm vi B2.

## 2. Phạm vi và những thứ không được sửa

**Được phép** sửa có kiểm soát:

- `product/list.html`.
- `fragments/product-card.html`.
- `storefront.css` chỉ các selector catalog/card mới hoặc thật sự liên quan.
- `app.js` chỉ cơ chế xử lý ảnh lỗi và enhancement liên quan.
- Template fragment phân trang đang dùng, **chỉ khi cần thiết**.
- MVC/Thymeleaf tests và test frontend hiện có.
- Hai báo cáo P15 và bộ ảnh B2.

**Chỉ khi thật sự cần**, được bổ sung một trường view-model/chỉ số **read-only**, không tạo truy vấn ghi, thay đổi schema hay hành vi kinh doanh. Nếu đòi hỏi thay đổi transaction, repository write, Spring Security hoặc quy tắc chọn SKU, hãy **dừng phần đó và đánh dấu BLOCKED**, không tự sửa.

**Tuyệt đối không** thay đổi routes, GET query names, POST form contract, CSRF, quyền Guest/Customer/Admin, tính giá/stock/cart/checkout/voucher/MoMo, Flyway, entity, DB, seed; không thêm Next.js, React, Tailwind, jQuery, CDN hay thư viện mới. Không tự tải ảnh Byredo, không sử dụng ảnh thiếu quyền hoặc dữ liệu fixture như sản phẩm thực.

Không triển khai C13 (grid-density switch), C17 (quick add/wishlist trên card), C20 (editorial block), C21 (video) hoặc C27 (checkout) trong B2. Không tự nâng coverage cho những mục đó.

---

## 3. B2.1 — C15: Catalog image resilience (**CORE**, ưu tiên số 1)

### Hiện trạng được audit

Card có URL ảnh rỗng đã hiển thị monogram BeautyShop. Nhưng khi URL tồn tại mà máy chủ trả HTTP 404, Chrome hiển thị biểu tượng ảnh vỡ (`naturalWidth=0`) và không hiện placeholder; vấn đề có trên desktop/mobile. PDP có xử lý ảnh lỗi riêng, cần kiểm tra để tái sử dụng nhất quán.

### Phải triển khai

1. Tạo cơ chế fallback **nhất quán, không nhấp nháy gây khó chịu**, cho card với trường hợp URL `null`, rỗng, trắng; URL 404; tệp không giải mã được; tải thành công.
2. Giữ tỷ lệ ảnh **4:5** của catalog và hình sản phẩm hiển thị `object-fit: contain` hoặc quy tắc hiện có phù hợp. Tránh layout shift và tránh méo ảnh.
3. Placeholder mang bản sắc BeautyShop (monogram/label tự có) và có thông điệp accessibility hợp lý. Không để cả alt ảnh lỗi và văn bản fallback lặp thông tin khó nghe với screen reader.
4. JS xử lý `error` bằng `addEventListener`, không dùng inline `onerror`, inline JS hoặc `innerHTML`. Xử lý cả tài nguyên đã lỗi trước khi listener được gắn (cached failures) bằng kiểm tra `complete && naturalWidth === 0` nếu thích hợp.
5. Tránh vòng lặp fallback nếu ảnh placeholder cũng lỗi; ưu tiên HTML/CSS placeholder có sẵn. Không được thay ảnh hỏng bằng ảnh mỹ phẩm giả.
6. Ảnh dưới màn hình dùng lazy load phù hợp; ảnh đầu tiên quan trọng không được lazy-loading bừa bãi gây hại LCP. Không thay cơ chế phục vụ ảnh `/uploads/sku/{filename}`; giữ quy tắc URL an toàn hiện có.
7. Nếu JavaScript bị tắt: URL rỗng vẫn phải hiện placeholder từ SSR; không được chặn tương tác với card. Nếu URL 404 không thể ẩn hoàn hảo bằng HTML/CSS thuần, **ghi giới hạn một cách trung thực**, không giới thiệu plugin nguy hiểm hoặc đổi image-serving backend chỉ để đỗ test.
8. Không tạo thêm request cố tình tới ảnh lỗi để chấm bài; 404 do fixture là expected network condition, khác với uncaught JS error.

### Test bắt buộc cho C15

- Không có URL → placeholder.
- URL hợp lệ, tải thành công → ảnh hiển thị đúng.
- HTTP 404 → không còn icon ảnh vỡ, placeholder hiển thị rõ.
- Image decode failure → fallback.
- Nhiều card đồng thời; một URL 404 không ảnh hưởng card khác.
- Chuyển trang/tìm kiếm/bộ lọc → không mất cơ chế fallback.
- 360/390/768/1366/1920 và zoom 100%/200%, không tràn ngang, card vẫn có tỉ lệ ổn định.
- JS disabled: vẫn điều hướng PDP, URL rỗng vẫn fallback; nêu chính xác giới hạn với HTTP 404.

**Chỉ nâng C15 lên PASS khi có ảnh + assertion Chrome xác minh HTTP 404 đã rơi về fallback, không chỉ kiểm markup.**

---

## 4. B2.2 — C12: Filter count và active chips

### Hiện trạng được audit

Chips có thể bỏ từng điều kiện, xóa toàn bộ và giữ các query param khác. Nhưng catalog chưa cho người mua biết **bao nhiêu điều kiện lọc đang hoạt động**.

### Phải triển khai

1. Thêm count dễ nhìn vào header/summary của bộ lọc (desktop và disclosure mobile). Ví dụ `Bộ lọc (2)` và thông báo đọc được cho assistive technology.
2. Ưu tiên tính từ **state GET / model server** đã render, không dùng JS làm nguồn dữ liệu quyết định filter. Count phải khớp với các filter thực sự đang được áp dụng trong code.
3. Phân biệt **bộ lọc** với **sắp xếp**: `sort` không được tính là filter. Chọn quy tắc rõ ràng cho `q` (tìm kiếm) và áp dụng nhất quán với chip, summary và test. Mặc định đề xuất: đếm `brand`, `category`, `skinType` khi có giá trị hợp lệ; `q` hiển thị dưới dạng truy vấn tìm kiếm riêng, không cộng filter count. Nếu hệ thống có quy tắc khác, ưu tiên hợp đồng hiện có và giải thích.
4. Không đếm `page`, `size`, anchor fragment, giá trị empty/whitespace hoặc giá trị mặc định `Tất cả` như một filter.
5. Khi xóa một chip, giữ nguyên các điều kiện GET còn lại và reset `page` theo hành vi đã có (nếu có), không vô tình xóa `q`, `sort` hoặc `size`.
6. Khi xóa tất cả filters, nói rõ `q` có được giữ không. Không âm thầm thay đổi semantics của nút `Xóa tất cả` hiện hữu; nếu hiện tại nút xóa cả `q`, giữ hành vi cũ và ghi rõ.
7. Không tạo chips/bộ lọc khi catalog 0 sản phẩm mà server không cung cấp; vẫn hiển thị count/state đang áp dụng nếu URL có filter và không có kết quả.
8. Desktop và mobile dùng cùng kết quả count từ server; focus/keyboard/no-JS hoạt động.

### Test bắt buộc cho C12

- Không filter: `0` (hoặc nhãn ẩn khi 0 nhưng accessibility rõ).
- Chỉ từng filter: `1`.
- Kết hợp `brand + category`: `2`.
- Kết hợp `brand + category + skinType`: `3`.
- Có `sort` và `page/size` nhưng không filter: không tăng count.
- `q` riêng và `q` kết hợp các filter: theo quy tắc đã chốt.
- Empty/whitespace/`Tất cả`: không tính sai.
- Xóa từng chip, xóa tất cả, refresh, Back/Forward, JS disabled; URL và tổng kết quả server giữ đúng.
- Mobile: số bộ lọc nhìn được ở trạng thái panel đóng.

**Không nâng PASS chỉ vì thêm text tĩnh `(0)` hoặc chỉ đếm chip bằng DOM sau khi tải.**

---

## 5. B2.3 — C18: Product browsing progress từ Spring Data Page

### Hiện trạng được audit

Pagination GET `Previous/Next` giữ `q`, `brand`, `category`, `skinType`, `sort`, `page`, `size`. Chưa có dòng thông tin về vị trí duyệt trên tổng sản phẩm.

### Phải triển khai

1. Dùng `Page` thực trong model để hiển thị tiến độ dạng **`Đang hiển thị X–Y trong tổng Z sản phẩm`**. Các con số phản ánh kết quả **đã áp bộ lọc** từ server.
2. Xác minh zero-based/one-based thật trong controller trước khi tính, không giả định từ URL.
3. Với Page zero-based thông thường và `numberOfElements > 0`:
   - `X = page.number * page.size + 1`
   - `Y = X + page.numberOfElements - 1`
   - `Z = page.totalElements`
   Chỉ dùng biểu thức/thuộc tính tương đương nếu phù hợp model thực.
4. Với `Z=0`, hiển thị trạng thái rỗng rõ ràng (ví dụ `0 sản phẩm`) chứ không hiển thị `1–0`.
5. Với page ngoài vùng, `numberOfElements=0`, dữ liệu thay đổi giữa các request hoặc trang cuối ngắn hơn size: hiển thị không gây hiểu nhầm; không invent range lớn hơn total.
6. Giữ nguyên GET pagination, parameter, link Previous/Next, trạng thái disabled trang đầu/cuối và điều hướng với JS disabled. Không tự tạo AJAX/infinite scrolling hay API mới.
7. Đảm bảo nội dung không nhảy khi đổi filter; typography và spacing U1 phù hợp toolbar/lưới hiện có.
8. Không gắn `aria-live` lên toàn bộ lưới khiến screen reader đọc hàng chục sản phẩm; chỉ vùng kết quả có mô tả ngắn khi thích hợp.

### Test bắt buộc cho C18

- Tổng 0.
- Tổng 1.
- Tổng 12, size 12, trang đầu.
- Tổng 27, size 12: trang đầu `1–12/27`, trang hai `13–24/27`, trang ba `25–27/27` (nếu controller zero-based).
- Trang cuối thiếu sản phẩm.
- Trang yêu cầu nằm ngoài range.
- Filter/tìm kiếm thay đổi total.
- Sort không đổi total nhưng giữ thứ tự server.
- Previous/Next, Back/Forward, no-JS, không mất query params.

**Không nâng C18 PASS nếu tiến độ được hardcode theo size hoặc tính từ số card hiện có mà không đối chiếu `Page.totalElements`.**

---

## 6. Quality bar: mô phỏng trải nghiệm Byredo mà không sao chép

- Cân chỉnh toolbar, filter count, progress và lưới sản phẩm thành một nhịp biên tập gọn gàng; giữ hệ typography/màu/spacing U1.
- Product cards phải thể hiện cấp bậc rõ: khung ảnh → thương hiệu → tên → giá VND → tình trạng tồn, theo dữ liệu hiện có.
- **Không redesign toàn bộ catalog trong B2**, không chạm hero, footer hay header đã đạt PASS.
- Nếu thiếu ảnh thực, dùng placeholder tinh tế; **không coi placeholder là bằng chứng ảnh sản phẩm thật đã hoạt động**.
- Giữ card action đúng: sản phẩm có nhiều SKU phải qua PDP chọn variant; không tự thêm quick-add trong B2.

## 7. Ma trận QA bắt buộc

### Lớp MVC và regression

Dùng test thật với Spring MVC/Thymeleaf/Spring Security và repository fixture đã có. Thêm assertions cho C12, C15 (SSR markup), C18. Giữ test B1.1–B1.4 và U2.1–U2.3.

Không đổi số lượng test trong báo cáo một cách ước lượng; ghi kết quả Maven cuối cùng.

### Lớp browser thực

Chrome/Chromium nếu có, ở các viewport:

- 360×800; 390×844; 768×1024; 1366×768; 1920×1080.
- Zoom 100% và 200% thật, không giả lập bằng CSS transform.
- Tối thiểu: catalog 0 kết quả; catalog nhiều kết quả; nhiều bộ lọc; ảnh OK/NULL/404/decode fail; trang đầu/trang giữa/trang cuối.
- Chạy keyboard Tab/Shift+Tab, Enter/Space, touch/click, reload/Back, JavaScript disabled cho các thao tác GET.
- Kiểm tra `document.documentElement.scrollWidth <= document.documentElement.clientWidth` (có tolerance hợp lý nếu scrollbar và làm rõ), không cắt nội dung/CTA/focus.
- Không hồi quy menu B1.2, wishlist/cart badge B1.3, footer B1.4.
- Với image 404, ghi HTTP status, `naturalWidth` và trạng thái fallback hiển thị. Network 404 dự kiến không được báo nhầm thành lỗi runtime JavaScript.

Lưu screenshots/JSON (có URL, viewport, zoom, JS state, assertion) tại:

`docs/ui/screenshots/byredo-inspired/B2/`

### Build và kiểm soát dữ liệu

Dùng Temurin JDK 21 đã cài. Lệnh mẫu (điều chỉnh theo repo nếu cần):

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
$env:P01_MARIADB_TEST = 'false'
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package
node --check src/main/resources/static/js/app.js
git diff --check
```

Nếu `-o` thất bại do artifact chưa cache, giải thích và dùng lệnh Maven phù hợp, không tự thay đổi dependency.

**Không chạy các test MariaDB có khả năng ghi dữ liệu trên `beautyshop`. Không tự bật `P01_MARIADB_TEST=true`, không chạy migration/seed để khắc phục F01.** Nếu muốn kiểm thử DB, chỉ làm khi có database `beautyshop_test` độc lập và được owner xác nhận; điều đó nằm ngoài phạm vi B2. Không ghi secrets vào log hoặc báo cáo.

## 8. Quản lý các blocker và chỉ tiêu

- **F01 BLOCKED**: Endpoint demo từng trả catalog 0 sản phẩm và sau đó không truy cập được; B2 fixture không chứng minh DB công khai hoạt động. Giữ trạng thái này trừ khi có bằng chứng đọc trên môi trường thật và nguyên nhân đã giải quyết an toàn.
- **C15 là core**; sau B2 nếu PASS và không đổi core khác, core trở thành **11/12**. C27 vẫn PARTIAL, dành cho B4.
- **C21** vẫn FAIL/BLOCKED về media có quyền; chưa có quyền tự chuyển N/A.
- **C13/C17/C20** không làm trong B2; không cộng điểm cho các mục chưa có test.
- **Coverage** tính PASS thực tế / 30. Từ baseline 20 PASS, mục tiêu +3 = **23/30 = 76,67%**, chưa phải 80%. Cả báo cáo và UI không được ghi hoàn thành P15 sớm.

## 9. Báo cáo và Definition of Done

Cập nhật đúng **hai báo cáo hiện có**, giữ lịch sử B0–B1.4:

- `docs/reviews/byredo-inspired-ui-implementation.md`.
- `docs/reviews/byredo-component-gap-analysis.md`.

Bổ sung mục `B2 · Luxury Catalog` với:

1. Inventory working tree và diff/file thật sự sửa; các thay đổi sẵn có được bảo toàn.
2. Trước/sau C12, C15, C18, chi tiết thuật toán đếm filter/range và cách handle ảnh lỗi.
3. Kết quả test MVC/Chrome: PASS/FAIL/ERROR/SKIPPED, browser viewport/zoom, số lượt đã đo, bằng chứng nguồn ảnh 404/decode-fail.
4. Screenshots/JSON với đường dẫn cụ thể.
5. C01/C02/C05/C06/C07/C08/C28 và các chức năng U2 đã PASS có bị hồi quy không.
6. Coverage 30 component và 12 core tính lại từ evidence; không cộng PARTIAL.
7. F01 và MariaDB integration trạng thái nào, cái gì NOT VERIFIED.
8. Accessibility: keyboard/no-JS, focus, screen reader NOT VERIFIED nếu chưa chạy thật.
9. Các vấn đề còn mở và quyết định chuyển sang B3/B4.

**DoD:** C12, C15, C18 cùng PASS với test độc lập; hình 404 trên Chrome hiện fallback; count trùng state server; range trùng Page; GET/keyboard/no-JS không hồi quy; Maven test và package PASS; không thay đổi business contract/DB; báo cáo minh bạch.

Nếu một component chưa đạt, giữ PARTIAL/FAIL, ghi nguyên nhân và bằng chứng; các thành phần đạt vẫn được bàn giao. Không tuyên bố 23/30 khi chưa đủ bằng chứng.

## 10. Lệnh thực thi và điều kiện dừng

**BẮT ĐẦU TRIỂN KHAI B2 NGAY** theo thứ tự **B2.1 C15 → B2.2 C12 → B2.3 C18**. Sau mỗi phần, test trong phạm vi liên quan; sau cả ba phần, regression và cập nhật báo cáo.

**DỪNG SAU B2.** Không triển khai B3, B4, checkout, quick add, editorial blocks, video, backend feature khác. **Không commit hoặc push.**
