# P19: Gắn bộ nhận diện BeautyShop

Trạng thái: hoàn thành, trừ phần kiểm chứng header Admin trên trình duyệt (xem "Chưa xác minh"). Chưa commit/push.

## 1. Tài sản
| Nguồn | Đích |
|---|---|
| `brand-kit/svg/*.svg` (trừ favicon) | `src/main/resources/static/img/brand/` (8 file) |
| `brand-kit/svg/favicon.svg` | thay `src/main/resources/static/img/favicon.svg` |
| `brand-kit/css/brand-hero.css` | `src/main/resources/static/css/brand-hero.css` |
| `README.md`, `licenses/`, ảnh xem thử | `docs/brand/` |

- Các file được chép nguyên bản (đã kiểm bằng `cmp`). Không sửa nội dung SVG/CSS nào của brand-kit.
- Ngoại lệ duy nhất là bản nhúng inline của `beautyshop-hero-inline.svg` trong template, theo đúng yêu cầu mục 3:
  - bỏ `role="img"`, `aria-labelledby` và `<title>`;
  - thêm `aria-hidden="true"`, `focusable="false"`, `width`/`height` theo viewBox;
  - thêm class `home-slide-wordmark` để định vị.
- `brand-kit/` đã được xóa sau khi đối chiếu.
  - **Lưu ý:** thư mục này đã được commit ở `1142887`, nên `git status` hiện nó là 14 file bị xóa (`D`), không phải thư mục chưa theo dõi.
  - Nội dung còn nguyên ở các vị trí mới và trong lịch sử Git. Owner quyết định có commit việc xóa này hay không.

## 2. Header
- `fragments/header.html`: link về trang chủ có `aria-label="BeautyShop – Trang chủ"`, bên trong là `<picture>`:
  - dưới 360px, `<source media="(max-width: 359px)">` hiện monogram (`120×160`);
  - từ 360px trở lên, `<img alt="BeautyShop" width="402" height="54">` hiện lockup. Kích thước khớp viewBox nên layout không bị giật.
- Trang `/admin*` dùng `beautyshop-lockup-light.svg`. Điều kiện chọn bản light giống cờ `data-storefront` có sẵn.
- Chiều cao: 40px khi rộng ≥ 1100px, 32px khi rộng ≤ 1099px (cả mobile, đúng mức tối thiểu 32px của README). Monogram cao 32px.
- Đã bỏ các rule cỡ chữ `.brand` / `.brand span` không còn dùng. Menu, ô tìm kiếm và giỏ hàng giữ nguyên.
- Trang chủ khi carousel chạy:
  - lockup nhỏ nằm giữa phía trên;
  - ở chương 1, hero lớn đã mang thương hiệu nên lockup header mờ hẳn (`opacity: 0`, không nhận click);
  - lockup hiện lại từ chương 2, hoặc ngay khi nhận focus bàn phím (`:focus-visible`), nên không có phần tử tàng hình nào nhận focus;
  - lockup vẫn nằm trong cây trợ năng, nhờ vậy hero được để `aria-hidden`.

## 3. Trang chủ – chương 1
- `.home-slide-wordmark` chữ "BEAUTYSHOP" được thay bằng SVG hero inline. `<h1>` "Vẻ đẹp. Theo cách bạn." giữ nguyên.
- **Nạp CSS:** `brand-hero.css` là file riêng, chỉ nạp ở trang chủ qua cờ `brandHeroCss`, cùng cơ chế với `homeCarouselCss` trong `head.html`. Tôi không gộp vào `storefront.css` để giữ file brand nguyên bản.
- **Kích thước:** đặt trong `storefront.css` với selector `.home-slide .bs-hero` để thắng độ rộng mặc định của brand-hero.
  - Desktop: `width: min(680px, 64vw, 100% - 64px)`, top 56px.
  - Từ 760px trở xuống: `min(520px, 100% - 48px)`.
  - Chiều cao thấp: `min(360px, 100% - 32px)`.
- Kết quả đo không chồng lên `<h1>`, thẻ "Khám phá sản phẩm" hay chấm phân trang ở mọi viewport (bảng bên dưới). Ở 1366×768, chương 1 vẫn vừa một màn hình và carousel vẫn bật.
- Hiệu ứng thuần CSS, chạy một lần (không có `infinite`):
  - vòm vẽ từ 0.1s;
  - 10 chữ lần lượt hiện từ 0.55s đến 1.09s;
  - kẻ vàng mở ra lúc 1.5s, dòng phụ hiện lúc 1.6s;
  - vệt sáng lướt qua lúc 1.9s.
- Quay lại chương 1 thì hiệu ứng không phát lại. Bật giảm chuyển động thì hiện bản tĩnh ngay. Tắt JS thì logo vẫn hiện.

## 4. Các chỗ khác
- **Footer:** `beautyshop-wordmark.svg`, cao 28px, `alt="BeautyShop"`.
- **Ô thay thế khi thiếu ảnh (P16, `product-card`):** chữ "BeautyShop" được thay bằng monogram 48px, `alt=""` vì nằm trong khối `aria-hidden`. Dòng "Chưa có ảnh sản phẩm" giữ nguyên.
- **Thẻ "BS" ở chương 1:** thay bằng monogram 30px.
- **Không đổi**, để tránh phá layout và vượt phạm vi:
  - chữ "B" ở PDP (`pdp-monogram`);
  - chữ "B" ở giỏ hàng (`cart-monogram`);
  - chữ "B" ở trang đăng nhập/đăng ký (`auth-monogram`).
- **Trang lỗi:** các trang 400/403/404/409/413/500 đã có lockup trên header và chỉ hiện mã số lỗi, nên không sửa. `error.html` (trang lỗi chung) đang dùng chữ "BeautyShop" làm mã lỗi lớn; đã thay bằng lockup, chọn bản light khi đường dẫn là `/admin`. Dòng eyebrow "BEAUTYSHOP · LUÔN CÓ MỘT LỐI ĐI TIẾP" là khẩu hiệu nên giữ nguyên.

## 5. Kiểm thử
- **`mvn test`** (JDK 21, không đặt `P01_MARIADB_TEST`): 189 test, 0 fail, 0 error, 34 skipped. Log: `docs/qa/P19/mvn-test.log`.
  - Trước P19 không có test nào kiểm chữ thương hiệu trong header, nên không có assertion nào cần sửa.
  - Đã thêm assertion vào `TrangChuPagesTest`:
    - `logoThuongHieuTrenHeaderHeroVaFooter` kiểm lockup, nguồn monogram, wordmark footer, favicon, brand-hero.css, và hero `aria-hidden` không có `role`/`title`;
    - `cssHeroChiNapOTrangChu` kiểm trang `/login` không nạp CSS hero.
  - Đã thêm assertion vào `AdminCatalogSecurityTest`: header Admin dùng lockup light.
- **`mvn -DskipTests package`:** BUILD SUCCESS. Log: `docs/qa/P19/mvn-package.log`.
- **Rà template:** 0 `th:utext`, 0 `style=`, 0 script inline, 0 handler, 0 CDN. Mọi trang có đúng 1 `<h1>`. Kết quả: `docs/qa/P19/template-scan.txt`.
- **QA trình duyệt** (`beautyshop_demo`, app ở cổng 8081): **22/22 PASS, 0 lỗi JS**. Xem `docs/qa/P19/results.json` và `qa-run.log`.

| Viewport | Trang chủ | Logo header (`/` và `/products`) | Hero (px) | Chồng lấn | Tràn ngang |
|---|---|---|---|---|---|
| 340×720 | fallback | monogram 24×32 | 308×91 | không | không |
| 360×800 | fallback | lockup 189×32 | 312×92 | không | không |
| 390×844 | fallback | lockup 219×32 | 342×101 | không | không |
| 768×1024 | carousel | lockup 238×32 | 492×144 | không | không |
| 1366×768 | **carousel** | lockup 298×40 | 680×200 | không | không |
| 1920×1080 | carousel | lockup 298×40 | 680×200 | không | không |
| 1366×768 zoom 200% | fallback | lockup 238×32 | 360×106 | không | không |

- Ở 360px lockup vừa với "Menu cửa hàng" nhờ cột co giãn (rộng 189px thay vì 238px, cao đúng 32px).
- Carousel wheel 1→6→footer→6→1 không đổi so với P18.
- Favicon: `link rel="icon" type="image/svg+xml"`, file trả HTTP 200 `image/svg+xml`. Ảnh xem thử ở 128/32/16px.
- Ảnh placeholder: 12 ô hiện monogram 48px, nằm gọn trong khung.
- Trang 404: có lockup, 1 `<h1>`, không tràn ngang.

## Chưa xác minh
- **Header Admin trên trình duyệt.** Script QA định đăng ký một tài khoản tạm, nâng lên ADMIN bằng MariaDB CLI rồi hạ quyền lại. Bộ phân loại an toàn của môi trường đã chặn bước nâng quyền. Tôi không tìm cách vượt qua.
  - Phần đã kiểm: markup Admin (lockup light, aria-label) qua MockMvc trong `AdminCatalogSecurityTest`.
  - Phần chưa kiểm: hiển thị trên trình duyệt và ảnh `header-admin`. Script vẫn có sẵn đoạn Admin, tự bỏ qua khi không có biến DB.
  - Owner có thể tự chạy script với tài khoản admin của mình, hoặc cấp quyền cho bước này.
- **Favicon trên thanh tab:** Chrome headless không có thanh tab. Đã kiểm link, MIME và hình hiển thị của file.
- Safari, Firefox và thiết bị thật.
- Ảnh `hero-390x844`: ở chế độ fallback trên mobile, ảnh chụp bắt đầu từ vùng chương 1, giống ảnh mobile của P18. Đây là hành vi sẵn có, P19 không thay đổi.

## Bằng chứng
- QA: `docs/qa/P19/brand-logo.qa.mjs`, `results.json`, `qa-run.log`, `app.log` (mật khẩu DB đã che), `mvn-test.log`, `mvn-package.log`, `template-scan.txt`
- Ảnh `docs/ui/screenshots/P19/`:
  - hero: `hero-1366x768.png`, `hero-1366x768-mid-animation.png`, `hero-1366x768-reduced-motion.png`, `hero-390x844.png`
  - header storefront: `header-storefront-1366.png`, `header-storefront-340-monogram.png`
  - favicon: `favicon-128.png`
- Bộ nhận diện: `docs/brand/`
