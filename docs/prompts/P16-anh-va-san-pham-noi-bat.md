# P16 – Ảnh sản phẩm thật + Dải "Sản phẩm nổi bật" trên trang chủ

> Dán toàn bộ file này vào Claude Agent / Claude Code trong IntelliJ.
> Chạy trên đúng máy đang chứa dự án (`E:\Nam 4 HK 1\WWW\cosmetics_shop`).

## 0. Vai trò và nguyên tắc đọc trước

Bạn là Senior Frontend Engineer tiếp tục dự án BeautyShop (website bán mỹ phẩm, Spring Boot + Thymeleaf, Java 21, MariaDB, thiết kế Dark Luxury đen `#0A0A0B` + gold `#A88A5A`).

Đọc trước khi sửa, không bỏ qua:
1. `AGENTS.md`
2. `docs/design/02-erd.md` và `V1__khoi_tao_co_so_du_lieu.sql` (bảng `san_pham`, `bien_the_san_pham`, cột `url_hinh_anh`)
3. `src/main/resources/templates/fragments/product-card.html`, `fragments/ui.html` (fragment `money`)
4. `src/main/resources/templates/home/index.html`, `static/js/home-carousel.js`, `static/css/storefront.css`
5. `CuaHang_Controller`, `CuaHang_Service` và repository tương ứng
6. `scripts/seed-demo.ps1`, `docs/data/product-research.csv`

**Không làm mất thay đổi chưa commit. Không `git reset/clean/stash/restore`, không commit/push khi chưa được owner cho phép.**

## 1. Mục tiêu

Hai việc, làm tuần tự:
- **A. Sản phẩm nổi bật:** thêm một dải sản phẩm thật trên trang chủ (ngay trên footer), dùng lại thẻ sản phẩm có sẵn.
- **B. Ảnh sản phẩm:** đảm bảo ảnh thật hiển thị đúng, và các ô ảnh đang trống trở nên thanh lịch khi chưa có ảnh.

## 2. CẤM (giữ an toàn dự án)

- KHÔNG đổi: business logic, bảo mật, Flyway schema đã áp dụng (V1–V5), COD/MoMo, giỏ/checkout, dữ liệu giá/tồn/SKU, các contract CSRF/route/tên field form.
- KHÔNG sửa luồng carousel đã chạy (1→2→3→footer→3→2→1); phần thêm vào **không được** làm hỏng luồng này, không hijack cuộn, vẫn chạy khi tắt JS / reduced-motion.
- KHÔNG thêm dependency, framework, CDN mới.
- KHÔNG bịa dữ liệu: không tự tạo review, chứng nhận, "đã bán", và **không nhúng ảnh/logo/tên thương hiệu thật mà nhóm chưa có quyền dùng**. Không tải ảnh từ web.
- KHÔNG dùng `th:utext`, inline `style=`, inline script/handler.
- Giữ nguyên hệ token Dark Luxury và các fragment dùng chung.

## 3. Phần A – Dải "Sản phẩm nổi bật"

**Chọn nguồn dữ liệu (ưu tiên không đổi schema):**
- Bảng `san_pham` hiện **không có** cột "nổi bật". Vì vậy mặc định lấy **6–8 sản phẩm active mới nhất còn hàng** (ví dụ order theo `ma_san_pham` giảm dần hoặc ngày tạo nếu có, chỉ lấy sản phẩm `active` và có ít nhất một SKU còn tồn).
- Thêm một phương thức truy vấn chỉ-đọc trong service/repository catalog cho mục đích này; tái dùng kiểu dữ liệu thẻ sản phẩm mà `/products` đang dùng (có `duongDan`, `tenThuongHieu`, `tenSanPham`, `giaTu`, `soLuongKhaDungLonNhat`, `urlHinhAnh`). Không tạo projection mới nếu cái sẵn có đủ.
- **Chỉ khi owner yêu cầu** một cờ nổi bật bật/tắt trong Admin thì mới thêm migration `V6__them_san_pham_noi_bat.sql` (cột boolean `noi_bat` mặc định false) + ô tick trong form sản phẩm Admin. Mặc định KHÔNG làm phần này.

**Controller:** trong controller trang chủ (`Bootstrap_Controller` hoặc `CuaHang_Controller`, theo nơi render `home/index`), truyền danh sách sản phẩm nổi bật vào model. Nếu danh sách rỗng, section tự ẩn (không hiện khối trống).

**Template `home/index.html`:**
- Đặt section **sau carousel, trong vùng cuộn thường chứa footer** (không biến nó thành slide thứ 4 của Swiper, để không ảnh hưởng luồng carousel). Nếu footer nằm trong một vùng cuộn native sau slide 3, đặt dải sản phẩm ngay trên footer trong cùng vùng đó.
- Tiêu đề section bằng serif (ví dụ "Sản phẩm nổi bật" + một dòng mô tả ngắn), đúng phong cách Dark Luxury.
- Lưới sản phẩm **dùng lại fragment** `~{fragments/product-card :: productCard(the=..., uuTienAnh=false)}`. Không viết lại thẻ sản phẩm.
- Responsive: 4 cột desktop, 2 cột tablet, 1 cột mobile; không cuộn ngang ở 360/390/768/1366/1920.
- Có nút "Xem tất cả sản phẩm" dẫn tới `/products` (dùng `.btn`).
- Phải hiển thị và bấm được khi tắt JS; keyboard focus đi qua được; link sản phẩm dẫn đúng `/products/{slug}`.

## 4. Phần B – Ảnh sản phẩm

1. **Ô ảnh trống trên trang chủ:** các thẻ nổi trong 3 chương (thumbnail nhỏ đang là ô tối) và placeholder thẻ sản phẩm cần trông thanh lịch khi chưa có ảnh. Thay ô tối trống bằng xử lý CSS theo token Dark Luxury: nền `--color-surface-alt`, viền mảnh gold, monogram/nhãn chữ nhẹ. Dùng lại `.store-card-fallback` đã có nếu phù hợp. Không để một khối đen trơn trông như lỗi.
2. **Ảnh thật:** xác nhận đường ảnh hiện tại hoạt động: thẻ dùng `the.urlHinhAnh`; ảnh SKU phục vụ qua `GET /uploads/sku/{filename}`. Khi nhóm đã bỏ file ảnh vào thư mục upload và điền `url_hinh_anh`/CSV, ảnh phải hiển thị, ảnh lỗi/404 phải rơi về placeholder (không vỡ layout). Không đổi cơ chế phục vụ ảnh, chỉ kiểm tra và sửa nếu thật sự lỗi.
3. **KHÔNG** tự thêm ảnh thương phẩm hay video chưa có nguồn hợp lệ. Nếu chưa có ảnh, giữ placeholder đẹp.

## 5. Kiểm thử bắt buộc (cuối việc)

- `mvn test` và `mvn -DskipTests package` trên **JDK 21** (đặt JAVA_HOME đúng), báo số test PASS/FAIL/SKIPPED thật. Không bật `P01_MARIADB_TEST`.
- Mở trang chủ với dữ liệu demo đã seed: dải sản phẩm nổi bật hiện 6–8 sản phẩm, ảnh hoặc placeholder đẹp, link vào PDP đúng.
- Kiểm 5 viewport (360/390/768/1366/1920) + zoom 200%: không cuộn ngang, carousel vẫn 1→2→3→footer→3→2→1, dải sản phẩm nằm trên footer, tắt JS vẫn thấy sản phẩm.
- Regression: `/products` filter/sort/paging, checkout/voucher (fixture) không đổi.
- Rà grep: không `th:utext`, `style=`, inline script, CDN; mỗi trang một `<h1>`.

## 6. Báo cáo
Ghi `docs/reviews/P16-home-featured-and-images.md`: file đã sửa, truy vấn/section đã thêm, cách đặt section để không đụng carousel, kết quả test thật, phần chưa kiểm chứng (ảnh thật còn thiếu, v.v.). **Dừng để owner xem, không commit/push.**

---
**Thứ tự:** đọc mục 0 → làm A → làm B → test → báo cáo. Làm từng bước nhỏ, build sau mỗi bước.
