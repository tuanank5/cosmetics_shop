# P19 – Gắn bộ nhận diện logo BeautyShop

> Dán file này vào Claude Agent / Claude Code. Làm **sau P18** (vì P18 sửa nhiều ở `home/index.html`).
> Trước khi bắt đầu, giải nén `beautyshop-brand.zip` vào thư mục gốc dự án thành `brand-kit/` (thư mục tạm, sẽ xóa sau khi chép xong).

## 0. Quy tắc
- Tuân theo `AGENTS.md` (báo cáo, log, ảnh lưu trong repo). Không `git reset/clean/stash/restore`, không commit/push khi owner chưa cho phép.
- Đọc trước: `brand-kit/README.md`, `fragments/header.html`, `fragments/footer.html`, `fragments/head.html`, `templates/home/index.html`, `static/css/storefront.css`, `static/css/admin.css`, `static/img/favicon.svg`, báo cáo P18.
- **Không đổi** nội dung các file SVG/CSS trong `brand-kit/` trừ khi có lý do kỹ thuật (ghi vào báo cáo). Không thêm font, thư viện, CDN, inline `style=`/script/handler.

## 1. Chép tài sản vào dự án
- `brand-kit/svg/*.svg` → `src/main/resources/static/img/brand/` (trừ `favicon.svg`).
- `brand-kit/svg/favicon.svg` → thay `src/main/resources/static/img/favicon.svg`.
- `brand-kit/css/brand-hero.css` → `src/main/resources/static/css/brand-hero.css`.
- `brand-kit/README.md` + `licenses/` → `docs/brand/`; ảnh xem thử → `docs/brand/beautyshop-brand-preview.png`.
- Xóa `brand-kit/` sau khi chép (thư mục tạm, không commit).

## 2. Header storefront
- Thay chữ BEAUTY/SHOP hiện tại bằng `<img src="/img/brand/beautyshop-lockup.svg" alt="BeautyShop" width height>` bọc trong link về `/` có `aria-label="BeautyShop – Trang chủ"`. Đặt `width`/`height` theo tỉ lệ viewBox để không giật layout.
- Chiều cao lockup khoảng 40px ở desktop, 32px ở tablet; dưới 360px chỉ hiện monogram (`beautyshop-mark.svg`). Giữ menu, ô tìm kiếm, giỏ hàng như cũ; không gây cuộn ngang.
- Trang Admin (nền sáng) dùng `beautyshop-lockup-light.svg`.

## 3. Trang chủ – chương 1
- Thay khối chữ `BEAUTYSHOP` lớn (`.home-slide-wordmark`) **của chương 1** bằng nội dung **inline** của `beautyshop-hero-inline.svg` (dán thẳng thẻ `<svg>` vào template). Vì header đã mang tên thương hiệu, đặt `aria-hidden="true"` cho SVG hero, bỏ `role="img"` và `<title>` để trình đọc màn hình không đọc lặp. Giữ nguyên `<h1>` "Vẻ đẹp. Theo cách bạn.".
- Nạp `brand-hero.css` **chỉ ở trang chủ** (cùng cơ chế cờ như CSS Swiper trong `head.html`), hoặc gộp vào `storefront.css` nếu đơn giản hơn; ghi rõ lựa chọn.
- Kiểm tra vị trí: hero nằm giữa phía trên, không đè lên `<h1>`, thẻ "Khám phá sản phẩm" hay chấm phân trang ở 360/390/768/1366/1920 px. Nếu ở chương 1 nội dung bị tràn khiến carousel rơi về fallback trên desktop thì giảm kích thước hero (`width: min(...)`), không được để carousel tắt ở 1366×768.
- Hiệu ứng chạy một lần khi tải trang; bật giảm chuyển động thì hiện ngay bản tĩnh; tắt JS vẫn hiện logo (hiệu ứng thuần CSS).

## 4. Các chỗ khác
- Footer: thay chữ "BeautyShop" serif bằng `beautyshop-wordmark.svg` (cao ~28px), `alt="BeautyShop"`.
- Ô thay thế khi thiếu ảnh sản phẩm (placeholder P16) và ô monogram "BS" trong các chương: cân nhắc dùng `beautyshop-mark.svg` để thống nhất; chỉ làm nếu không phá layout.
- Trang lỗi 400–500: dùng lockup ở đầu trang nếu đang hiển thị chữ thương hiệu.

## 5. Kiểm thử
- `mvn test` + `mvn -DskipTests package` trên JDK 21, báo số thật. Cập nhật test có kiểm chữ thương hiệu trong header (nếu có) cho khớp, không xóa assertion.
- Trên trình duyệt (DB `beautyshop_demo`): 5 viewport + zoom 200%, không cuộn ngang; header đúng ở storefront và Admin; favicon hiện trên tab; hero chạy hiệu ứng đúng thứ tự, reduced-motion và tắt JS hiện bản tĩnh; carousel 1→6→footer→1 không đổi.
- Rà template: không `th:utext`, `style=`, script inline, CDN; mỗi trang một `<h1>`.

## 6. Báo cáo
`docs/reviews/P19-brand-logo.md`, ảnh ở `docs/ui/screenshots/P19/` (hero 1366×768, hero 390×844, header storefront, header Admin, favicon). Tin nhắn cuối liệt kê đường dẫn. **Dừng để owner xem, không commit/push.**
