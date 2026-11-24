# P18: Trang chủ storytelling 6 chương

Trạng thái: hoàn thành, chờ owner xem. Chưa commit/push.

## File sửa
- `src/main/resources/templates/home/index.html`: 6 chương `home-opening`, `home-skin`, `home-ritual`, `home-ingredients`, `home-featured`, `home-advisor`, mỗi chương có `aria-labelledby` trỏ tới h2 của nó (chương 1 giữ h1 duy nhất). Đã bỏ section nổi bật P16 nằm riêng phía trên footer.
- `src/main/resources/static/js/home-carousel.js`: `slides.length !== 3` đổi thành `slides.length < 2`. Các tham số Swiper đã QA giữ nguyên.
- `src/main/resources/static/js/home-story.js`: focus spotlight chuyển thành tùy chọn (`?.`), dành cho chương không có spotlight.
- `src/main/resources/static/css/storefront.css`: thay khối P16 bằng:
  - layout các chương;
  - thu nhỏ wordmark từ chương 2 trở đi;
  - reveal lần lượt trong chương 2, 3, 4, chỉ chạy khi `is-ready` và `no-preference`.
- `src/main/java/com/beautyshop/repository/CuaHang_Repository.java`:
  - **4.4**: tìm kiếm thêm `lower(s.thanhPhan) like :q escape '!'`;
  - thêm 2 truy vấn chỉ đọc là `timLoaiDaDangBan` và `demDanhMucDangBan`.
- `src/main/java/com/beautyshop/service/TrangChu_Service.java` (mới): dữ liệu chỉ đọc cho các chương 2, 3, 4.
- `src/main/java/com/beautyshop/controller/Bootstrap_Controller.java`:
  - lấy service qua `ObjectProvider`;
  - bắt `DataAccessException` và ghi `log.warn`;
  - khi lỗi hoặc danh sách rỗng thì chương hiện bản tĩnh;
  - lấy `sanPhamNoiBat(4)`.
- Test:
  - mới: `TrangChuPagesTest`, `TrangChuServiceTest`, `CuaHangTimKiemThanhPhanTest` (H2);
  - đã cập nhật: `GiaoDienPagesTest` và `BootstrapPagesTest`. Link `?sort=newest` không còn trên trang chủ nên assertion của nó được thay bằng `/chat`.

## Nguồn dữ liệu từng chương
| Chương | Nguồn | Khi lỗi hoặc rỗng |
|---|---|---|
| 1 | Tĩnh, giữ nguyên | – |
| 2 Loại da | `san_pham.loai_da_phu_hop`: tách, chuẩn hóa, đếm bằng đúng bộ lọc `/products?skinType=`, lấy top 5 loại có số sản phẩm > 0 | Nút sang `/products#skin-type` |
| 3 Nghi thức | 5 bước cố định, slug lấy từ danh mục DB theo tên, chỉ khi danh mục có sản phẩm > 0 | 5 bước không có link |
| 4 Thành phần | 4 hoạt chất cố định, đếm bằng `/products?q=`, ẩn hoạt chất có 0 sản phẩm (Niacinamide trên demo) | Nút sang `/products` |
| 5 Nổi bật | `sanPhamNoiBat(4)` dùng `product-card` | Lời mời "Khám phá tất cả sản phẩm" |
| 6 Tư vấn | Tĩnh: 3 câu gợi ý, link `/chat`, disclaimer | – |

Số hiển thị trên trang chủ và số kết quả khi bấm link được tính bằng cùng một truy vấn.

## Kết quả kiểm chứng
- `mvn test` (JDK 21, không đặt `P01_MARIADB_TEST`): 187 test, 0 fail, 0 error, 34 skipped. Log: `docs/qa/P18/mvn-test.log`.
- `mvn -DskipTests package`: BUILD SUCCESS. Log: `docs/qa/P18/mvn-package.log`.
- QA trình duyệt trên `beautyshop_demo`, app ở cổng 8081: **16/16 PASS, 0 lỗi JS**. Xem `docs/qa/P18/results.json` và `docs/qa/P18/qa-run.log`.

| Viewport | Chế độ | Tràn ngang | Ghi chú |
|---|---|---|---|
| 360×800 | fallback (content-height) | không | cuộn dọc tự nhiên |
| 390×844 | fallback (content-height) | không | cuộn dọc tự nhiên |
| 768×1024 | carousel | không | wheel 1→6→footer→1 PASS |
| 1366×768 | carousel | không | wheel 1→6→footer→6→1 PASS |
| 1920×1080 | carousel | không | wheel 1→6→footer→6→1 PASS |
| 1366×768 zoom 200% | fallback (viewport-or-css) | không | – |

Các mục khác:
- Bàn phím: Arrow/PageDown đi 2→6, Arrow/PageUp lùi 5→1.
- Pagination có 6 nút, nhãn a11y đúng "Chương 6 / 6". Neo `#home-skin` hoạt động.
- Tab không lọt vào slide đang ẩn: 0 lần lọt, focus luôn hiển thị.
- Reveal lần lượt với delay 0 / 90 / 180 / 270 / 360ms. Wordmark 136.6px ở chương 1, 24px từ chương 2.
- Link: 5 loại da, 5 bước và 3 hoạt chất đều trả về > 0 kết quả. 4 thẻ nổi bật mở PDP với HTTP 200 và có h1. `/chat` trả HTTP 200.
- Không JS: đủ 6 chương, 13 item hiển thị. Reduced-motion: carousel fallback, mọi item hiển thị.
- Touch: swipe ở 768×1024 bằng `Input.synthesizeScrollGesture` (touch giả lập trong Chrome headless) chuyển sang chương 2. **Chưa kiểm chứng trên thiết bị thật.**
- Hồi quy `/products`: sort, phân trang, tìm theo tên/SKU/thành phần, `q=%` đều PASS.
- Hồi quy giỏ hàng P17: chạy lại script P17, kết quả ghi vào `docs/qa/P18/cart-regression/`, **41/41 PASS**.

## Điểm chưa xác minh
- Touch trên thiết bị thật, Safari/iOS và Firefox.
- Không có ảnh sản phẩm thật trên demo nên chương 5 hiện placeholder.
- QA giỏ hàng tạo một tài khoản khách QA tạm trong `beautyshop_demo`, giống P17.

## Bằng chứng
- `docs/qa/P18/home-storytelling.qa.mjs`, `results.json`, `qa-run.log`, `app.log`, `mvn-test.log`, `mvn-package.log`
- `docs/qa/P18/cart-regression/`
- `docs/ui/screenshots/P18/`: `desktop-1366-chapter1..6.png`, `mobile-390-chapter1.png`, `mobile-390-chapter3-fallback.png`
