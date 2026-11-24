# P18 – Trang chủ storytelling 6 chương (giữ Swiper)

> Dán toàn bộ file này vào Claude Agent / Claude Code, chạy trên máy chứa dự án.
> Làm **sau khi P17 đã xong và được owner duyệt**.

## 0. Quy tắc làm việc

- Đọc và tuân theo `AGENTS.md`, đặc biệt mục "Báo cáo và bằng chứng" (lưu báo cáo, log, ảnh trong repo).
- Đọc trước khi sửa: `templates/home/index.html`, `static/js/home-carousel.js`, `home-story.js`, `home-motion.js`, `static/css/storefront.css`, `fragments/product-card.html`, `controller/Bootstrap_Controller.java`, `service/CuaHang_Service.java`, `repository/CuaHang_Repository.java`, `docs/reviews/P15-V-*.md`, `docs/reviews/P16-home-featured-and-images.md`.
- **Không** `git reset/clean/stash/restore`, **không** commit/push khi owner chưa cho phép.

## 1. Mục tiêu

Giữ **Swiper dọc** hiện tại (cuộn chuột / phím / pagination chuyển từng chương toàn màn hình, xong chương cuối thì cuộn xuống footer, cuộn ngược quay lại), mở rộng từ 3 lên **6 chương** theo kịch bản dẫn khách từ cảm hứng tới mua hàng:

| # | Chương | Nội dung | Dẫn tới |
|---|---|---|---|
| 1 | Mở đầu | Giữ nguyên chương hiện tại: logo lớn, "Vẻ đẹp. Theo cách bạn." | Cuộn tiếp |
| 2 | Hiểu làn da | Các loại da **lấy từ dữ liệu sản phẩm** | `/products?skinType=...` |
| 3 | Nghi thức chăm sóc | 5 bước làm sạch → cân bằng → tinh chất → dưỡng ẩm → chống nắng, hiện lần lượt khi chương được mở | `/products?category=...` |
| 4 | Thành phần | Vài hoạt chất phổ biến, mô tả ngắn, trung lập | `/products?q=...` |
| 5 | Sản phẩm nổi bật | Sản phẩm thật từ truy vấn P16 | Trang chi tiết sản phẩm |
| 6 | Trợ lý tư vấn | "Chưa biết chọn gì? Hỏi BeautyShop" | `/chat` |

## 2. Ràng buộc (CẤM)

- Không đổi: giỏ hàng, checkout, đơn hàng, thanh toán, bảo mật, schema/Flyway, CSRF, route hiện có.
- Không thêm thư viện, CDN, inline script/style/handler, `th:utext`.
- Không bịa: không đánh giá giả, không số liệu "đã bán", không chứng nhận, không ảnh/logo thương hiệu thật chưa có quyền. **Không viết công dụng điều trị hay khẳng định y khoa** cho thành phần hoặc loại da.
- Không làm hỏng: luồng 1→…→6→footer→6→…→1, fallback cuộn thường (tắt JS, reduced-motion, viewport thấp, nội dung tràn, thiếu CSS/JS Swiper), `inert`/`aria-hidden` cho slide không active, một `<h1>` duy nhất trên trang.
- **Thay đổi backend duy nhất được phép** là mục 4.4 (mở rộng tìm kiếm sang thành phần). Mọi thay đổi khác phía Java chỉ là truyền dữ liệu chỉ-đọc cho trang chủ.

## 3. Carousel (`home-carousel.js` + template)

1. Bỏ điều kiện cứng `slides.length !== 3`. Cho phép số chương từ 2 trở lên (hoặc đúng 6), các phần còn lại (pagination, a11y "Chương {{index}} / {{slidesLength}}", lưu slide qua `history.replaceState`, liên kết neo `#...`) phải tự chạy theo số chương.
2. Cập nhật các liên kết "bỏ qua tới chương" ẩn ở đầu `home/index.html` cho đủ 6 chương. Mỗi chương có `id` rõ nghĩa (`home-opening`, `home-skin`, `home-ritual`, `home-ingredients`, `home-featured`, `home-advisor`) và `aria-labelledby` trỏ tới tiêu đề `h2` của chương (chương 1 giữ `h1`).
3. **Dải "Sản phẩm nổi bật" của P16 chuyển vào chương 5.** Xóa section riêng phía trên footer để không trùng lặp. Nếu danh sách nổi bật rỗng hoặc lỗi, chương 5 vẫn hiện, thay lưới bằng lời mời "Khám phá tất cả sản phẩm".
4. Chữ "BEAUTYSHOP" cỡ lớn giữ ở chương 1. Ở chương 2–6 thu nhỏ hoặc bỏ để nhường chỗ cho nội dung.
5. Kiểm tra cơ chế "nội dung tràn → fallback cuộn thường" (`content-height`). Ở desktop 1366×768 và 1920×1080, cả 6 chương phải **vừa một màn hình** để carousel hoạt động. Ở mobile, nếu nội dung không vừa thì fallback cuộn thường là chấp nhận được, nhưng phải ghi rõ trong báo cáo ở viewport nào carousel bật, ở viewport nào fallback.
6. Hiệu ứng "hiện lần lượt" (chương 2, 3, 4): khi slide trở thành active thì các mục xuất hiện so le (CSS transition + delay, dựa vào class active do Swiper đặt). Ở chế độ fallback, tắt JS hoặc reduced-motion thì mọi mục hiện sẵn, không ẩn nội dung.
7. Giữ nguyên tham số đã đạt QA (speed, `releaseOnEdges`, `eventsTarget`, keyboard `onlyInViewport`, xử lý menu mở). Chỉ đổi khi có lý do kỹ thuật và ghi vào báo cáo.

## 4. Dữ liệu cho từng chương

Truyền vào model trong `Bootstrap_Controller` (đọc qua service, chỉ đọc, `ObjectProvider` + bắt `DataAccessException` giống P16 và **ghi `log.warn`** khi lỗi; lỗi thì chương đó hiện bản tĩnh tối giản, trang chủ không được sập).

### 4.1. Chương 2 – Hiểu làn da
- Cột `san_pham.loai_da_phu_hop` là chuỗi nhiều giá trị ngăn bằng dấu phẩy, chữ hoa/thường không đồng nhất (ví dụ "Da khô", "da nhạy cảm").
- Tách, chuẩn hóa (trim, viết hoa chữ đầu), đếm số sản phẩm **đang bán** theo từng loại da, lấy tối đa 5 loại có nhiều sản phẩm nhất. **Chỉ hiện loại da có ít nhất 1 sản phẩm**; không tự thêm loại da không có dữ liệu.
- Mỗi mục: tên loại da, số sản phẩm, một dòng mô tả trung lập (bảng nội dung mục 5), liên kết `/products?skinType=<giá trị>`. Kiểm tra liên kết thật sự cho kết quả > 0 với bộ lọc `skinType` hiện có.

### 4.2. Chương 3 – Nghi thức chăm sóc
- 5 bước ánh xạ sang danh mục **theo tên danh mục trong DB**, lấy `duong_dan` từ DB (không hardcode slug):
  1. Làm sạch → danh mục "Làm sạch"
  2. Cân bằng → "Toner & xịt khoáng"
  3. Tinh chất → "Tinh chất"
  4. Dưỡng ẩm → "Dưỡng ẩm"
  5. Chống nắng → "Chống nắng"
- Danh mục không tồn tại hoặc không có sản phẩm đang bán thì bước đó vẫn hiện chữ nhưng **không có liên kết**.
- Trình bày dạng danh sách có thứ tự (`<ol>`), số bước lớn kiểu serif, đường nối mảnh gold.

### 4.3. Chương 4 – Thành phần
- Danh sách hoạt chất cố định trong code (hằng số hoặc file cấu hình), mỗi mục gồm tên hiển thị, từ khóa tìm kiếm, mô tả trung lập (mục 5).
- Khi render, **chỉ hiện hoạt chất có ít nhất 1 sản phẩm đang bán** khớp từ khóa (sau khi làm 4.4). Với dữ liệu hiện tại, dự kiến có Hyaluronic Acid, Ceramide, Salicylic Acid (BHA); Niacinamide chỉ hiện nếu dữ liệu có.
- Liên kết `/products?q=<từ khóa>`.

### 4.4. Mở rộng tìm kiếm sang thành phần (thay đổi backend duy nhất)
- Truy vấn `/products` hiện chỉ tìm `q` trong tên, mô tả, thương hiệu, SKU. Thêm điều kiện `or lower(s.thanhPhan) like :q escape '!'` trong `CuaHang_Repository` (giữ nguyên cách escape hiện có).
- Thêm test: tìm "hyaluronic" ra sản phẩm có thành phần đó; ký tự `%` và `_` trong `q` vẫn được escape; các test tìm kiếm cũ vẫn pass.
- Ghi thay đổi này vào báo cáo.

### 4.5. Chương 5 – Sản phẩm nổi bật
- Dùng lại `CuaHang_Service.sanPhamNoiBat(...)` và fragment `product-card`. Số thẻ đủ vừa một màn hình desktop (đề xuất 4 thẻ, 4 cột). Kèm nút "Xem tất cả sản phẩm" → `/products`.

### 4.6. Chương 6 – Trợ lý tư vấn
- Tiêu đề, 1–2 câu mô tả, 2–3 câu hỏi gợi ý dạng chữ tĩnh (không phải form), nút chính "Hỏi BeautyShop" → `/chat`.
- Ghi rõ đây là tư vấn tham khảo, không thay thế lời khuyên của bác sĩ da liễu.

## 5. Nội dung đề xuất (owner sẽ duyệt, agent dùng nguyên văn trừ khi owner sửa)

**Chương 2 – Hiểu làn da**
- Nhãn: `02 / HIỂU LÀN DA` · Tiêu đề: "Bắt đầu từ làn da của bạn." · Mô tả: "Mỗi làn da có nhu cầu riêng. Chọn loại da gần với bạn nhất để xem sản phẩm phù hợp."
- Da khô: "Thường cảm thấy căng, cần thêm độ ẩm."
- Da nhạy cảm: "Dễ phản ứng, nên ưu tiên công thức dịu nhẹ."
- Da thiếu ẩm: "Có thể đổ dầu nhưng vẫn thiếu nước."
- Da thường: "Cân bằng, ít khi khô hoặc bóng dầu."
- Da dầu: "Dễ bóng nhờn, thường hợp kết cấu mỏng nhẹ."
- Da hỗn hợp: "Vùng chữ T dễ dầu, hai má có thể khô." (chỉ hiện nếu có sản phẩm)

**Chương 3 – Nghi thức chăm sóc**
- Nhãn: `03 / NGHI THỨC CHĂM SÓC` · Tiêu đề: "Năm bước, một nhịp riêng." · Mô tả: "Một trình tự cơ bản để bắt đầu. Bạn có thể lược bớt tùy nhu cầu."
- 1 Làm sạch: "Loại bỏ bụi bẩn và dầu thừa." · 2 Cân bằng: "Chuẩn bị da cho các bước tiếp theo." · 3 Tinh chất: "Bổ sung hoạt chất theo mục tiêu riêng." · 4 Dưỡng ẩm: "Giữ ẩm và làm mềm da." · 5 Chống nắng: "Bước cuối mỗi buổi sáng."

**Chương 4 – Thành phần**
- Nhãn: `04 / THÀNH PHẦN` · Tiêu đề: "Hiểu điều bạn thoa lên da." · Mô tả: "Một vài hoạt chất thường gặp trong mỹ phẩm. Thông tin chỉ mang tính tham khảo."
- Hyaluronic Acid: "Hoạt chất giữ nước, thường có trong sản phẩm dưỡng ẩm và tinh chất."
- Ceramide: "Thành phần lipid vốn có trong da, thường dùng trong sản phẩm hỗ trợ hàng rào bảo vệ da."
- Salicylic Acid (BHA): "Acid tan trong dầu, thường có trong sản phẩm làm sạch; nên dùng theo hướng dẫn trên nhãn và chống nắng đầy đủ."
- Niacinamide: "Dạng vitamin B3, thường gặp trong tinh chất và kem dưỡng." (chỉ hiện nếu có sản phẩm)

**Chương 5 – Sản phẩm nổi bật**
- Nhãn: `05 / SẢN PHẨM NỔI BẬT` · Tiêu đề: "Lựa chọn mới tại BeautyShop."

**Chương 6 – Trợ lý tư vấn**
- Nhãn: `06 / TRỢ LÝ TƯ VẤN` · Tiêu đề: "Chưa biết chọn gì? Hỏi BeautyShop." · Mô tả: "Trợ lý trả lời dựa trên thông tin sản phẩm và chính sách của cửa hàng."
- Gợi ý: "Da khô nên chọn kem dưỡng nào?" · "Sản phẩm nào có Hyaluronic Acid?" · "Chính sách đổi trả thế nào?"
- Chú thích nhỏ: "Thông tin mang tính tham khảo, không thay thế tư vấn của bác sĩ da liễu."

## 6. Kiểm thử bắt buộc

- `mvn test` + `mvn -DskipTests package` trên **JDK 21**; báo số PASS/FAIL/SKIPPED thật; không bật `P01_MARIADB_TEST`.
- Chạy app với DB `beautyshop_demo` đã seed, kiểm trên trình duyệt:
  - Wheel, phím mũi tên/PageDown, pagination: 1→2→3→4→5→6→footer→6→…→1.
  - Mọi liên kết ở chương 2, 3, 4 mở `/products` có **kết quả > 0**; liên kết chương 5 vào đúng PDP; chương 6 vào `/chat`.
  - 360/390/768/1366/1920 px và zoom 200%: không cuộn ngang; ghi rõ viewport nào carousel bật, viewport nào fallback.
  - Tắt JS, reduced-motion: thấy đủ 6 chương và mọi liên kết, nội dung không bị ẩn.
  - Keyboard: Tab không lọt vào chương đang ẩn; focus nhìn thấy được.
  - Regression: `/products` (lọc, sắp xếp, phân trang, tìm kiếm cũ), giỏ hàng P17.
- Ghi lại kết quả QA touch nếu chạy được; nếu chưa, ghi "chưa kiểm chứng" (không giả lập kết quả).

## 7. Báo cáo

Theo `AGENTS.md`: `docs/reviews/P18-home-storytelling.md`, log/JSON ở `docs/qa/P18/`, tối đa ~10 ảnh chọn lọc ở `docs/ui/screenshots/P18/` (mỗi chương ở 1366×768, cộng 1–2 ảnh mobile). Báo cáo gồm: file đã sửa, dữ liệu từng chương lấy thế nào, thay đổi tìm kiếm thành phần, bảng viewport bật/fallback, kết quả test thật, phần chưa kiểm chứng. Tin nhắn cuối liệt kê đường dẫn. **Dừng để owner xem, không commit/push.**
