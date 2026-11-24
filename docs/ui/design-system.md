# BeautyShop — Hệ thiết kế U1

## Biến thể storefront Dark Editorial · 23/11/2026

Owner phê duyệt chuyển storefront sang dark luxury trong P15-V. Giữ cùng hệ CSS U1:
`storefront.css` override custom properties dưới `:root:has(.site-header[data-storefront])`;
header gắn marker theo path không bắt đầu `/admin`. Admin giữ palette U1 bên dưới.
Không thêm theme/framework, font CDN hoặc hệ button song song.

Storefront: nền `#0A0A0B`, surface `#151518`, surface-alt `#1D1D22`, chữ
`#F5F2ED`, chữ phụ `#B9B6B0`, viền trang trí `rgba(245,242,237,.12)`, gold
`#A88A5A`. Viền control `#85817B`, focus gold 3px; CTA sáng có chữ tối.
Giữ vùng bấm44px, CTA mua56px, error/success có chữ và màu đủ tương phản.
Burgundy `#4B1F2D` chỉ xuất hiện rất ít trong visual biên tập.

Editorial dùng Palatino Linotype/Palatino/Georgia có fallback; UI/tên sản phẩm dùng
Segoe UI/Arial/sans-serif. Không khẳng định Cormorant/Inter đã cài, không có webfont mới.
Card4:5, desktop4 cột, tablet/mobile2 cột, dưới340 CSS px về1 cột. Filter là một
disclosure/form GET, không tự submit khi đổi lựa chọn. Video chỉ bổ sung từ metadata
được duyệt; chưa có video thương phẩm thật trong model hiện hành.

Home có3 section lớn với poster SVG trừu tượng do BeautyShop tự tạo, không phải ảnh
sản phẩm/chiến dịch thật. GSAP Core/pilot giữ nguyên; ScrollTrigger local3.15.1 chỉ
animate poster (scale/opacity), không pin/snap hoặc che CTA. Reduced motion/no-JS
dùng poster tĩnh. [Báo cáo nghiệm thu và giới hạn](../reviews/P15-V-dark-luxury-redesign.md).

Phần sau giữ baseline U1 cho Admin và lịch sử; palette/menu mặc định cũ không mô tả
storefront sau P15-V. Hợp đồng component, accessibility và nghiệp vụ tiếp tục áp dụng.

Ngày 22/11/2026. Phạm vi: nền tảng P12/U1. Đây là hướng dẫn triển khai; chưa nghiệm thu trực quan bằng trình duyệt.

## Nhận diện và token

Nền kem, chữ nâu đậm, rosewood cho hành động chính, champagne cho đường trang trí. Dùng tên BeautyShop hiện có; monogram B trong favicon do dự án tự tạo. Không bổ sung ảnh thương hiệu hoặc nội dung chứng nhận.

Nguồn duy nhất của token: `src/main/resources/static/css/tokens.css`.

| Token | Giá trị |
|---|---|
| `--color-bg` / `--color-surface` / `--color-surface-alt` | `#FAF7F3` / `#FFFFFF` / `#F3ECE4` |
| `--color-ink` / `--color-muted` | `#2A1F1D` / `#6B5A55` |
| `--color-primary` / `--color-primary-hover` | `#5E2F3E` / `#4A2330` |
| `--color-accent` / `--color-border` | `#B08D57` / `#E6DACE` |
| `--color-control-border` | `#8A766F` |
| `--color-success` / `--color-error` / `--color-warning` | `#2F6B4F` / `#A12B3F` / `#8A5A00` |
| `--color-footer-bg` | `#2A1F1D` |
| `--radius-sm/md/lg` | 4 / 8 / 14 px |
| `--space-1..8` | 4 / 8 / 12 / 16 / 24 / 32 / 48 / 72 px |
| `--shadow-soft` | `0 6px 24px rgba(42,31,29,.08)` |
| `--focus-ring` | `3px solid var(--color-primary)`, offset 3 px |

Tiêu đề dùng stack Cormorant Garamond → Playfair Display → Palatino Linotype → Palatino → Cambria → Georgia → serif. Nội dung dùng Be Vietnam Pro → system-ui → Segoe UI → Roboto → sans-serif. **Không có webfont được đóng gói hoặc tải về**: tên đầu stack chỉ được dùng nếu máy đã cài font. Không có CDN. Nội dung/control 16 px, line-height 1.65; tiêu đề 1.25. H1 dùng clamp 36–56 px.

## Contrast đo từ giá trị sRGB

Chuyển từng kênh sRGB về tuyến tính: `c / 12.92` khi `c <= 0.04045`, ngược lại `((c + 0.055) / 1.055)^2.4`. Độ chói `L = .2126R + .7152G + .0722B`; tỷ lệ `(L_sáng + .05) / (L_tối + .05)`. Tính offline trực tiếp từ tokens.css, không lấy tỷ lệ đề xuất làm kết quả.

| Cặp foreground / background | Tỷ lệ | Ngưỡng áp dụng |
|---|---:|---:|
| Ink / bg | 14.99:1 | 7:1 |
| Muted / bg | 6.11:1 | 4.5:1 |
| Muted / surface | 6.52:1 | 4.5:1 |
| Muted / surface-alt, kể cả nút disabled | 5.57:1 | 4.5:1 |
| Trắng / primary | 10.76:1 | 7:1 |
| Trắng / primary-hover | 13.34:1 | 7:1 |
| Control-border / surface | 4.28:1 | 3:1 |
| Control-border / bg | 4.01:1 | 3:1 |
| Control-border / surface-alt | 3.66:1 | 3:1 |
| Success / bg; surface | 5.89:1; 6.29:1 | 4.5:1 |
| Error / bg; surface | 6.70:1; 7.16:1 | 4.5:1 |
| Warning / bg; surface | 5.55:1; 5.93:1 | 4.5:1 |
| Bg / footer | 14.99:1 | 7:1 |
| Accent / bg | 2.89:1 | Chỉ trang trí, không dùng chữ hoặc viền control |

Focus trên nền sáng dùng primary; liên kết footer dùng vòng kem. Những tỷ lệ này chỉ xác nhận các cặp màu đã tính, không phải chứng nhận toàn site đạt WCAG AA. Minh họa homepage, ảnh và các trạng thái ở U2–U4 cần được kiểm tiếp.

## CSS và bố cục

`fragments/head :: head(title)` nạp đúng thứ tự: tokens → base → components → storefront → admin, sau đó app.js với defer. Các trang đã chuyển sang head chung. Đã bỏ theme.css, admin-catalog.css, storefront.js; khi triển khai cần restart/build lại để loại asset cũ trong output build.

Container tối đa 1200 px; lề 32 / 24 / 16 px. Hai trang auth dùng hai cột và xếp một cột ở 640 px. Menu dùng details/summary, mặc định mở để mọi link có sẵn khi không dùng JavaScript. Summary luôn truy cập được cả khi thay đổi kích thước màn hình. Footer chỉ trỏ route hoặc control lọc tồn tại; chưa có trang chính sách/liên hệ nên không tạo link giả.

## Nút

Mọi nút có `.btn`; chọn một biến thể: `btn--primary`, `btn--secondary`, `btn--ghost`, `btn--link`, `btn--danger`. `.btn` mặc định secondary; variant secondary viết rõ để dễ đọc. Giữ một CTA nổi bật cho mỗi khu vực mua hàng khi thực hiện U2–U4.

- Mặc định: tối thiểu 48 px cao, 120 px rộng, padding ngang 28 px, chữ 16 px/600.
- `btn--lg`: cao 56 px, chữ 17 px. `btn--sm`: cao 44 px, dành cho Admin. `btn--icon`: tối thiểu 44 × 44 px và cần aria-label cụ thể.
- Nhóm `.button-group` có khoảng cách 12 px; mobile xếp dọc. Nút primary trong form rộng 100%, riêng nút tìm kiếm ở header giữ cạnh ô tìm.
- Có hover, active, focus-visible, disabled/aria-disabled và is-loading. Loading giữ nội dung cũ ẩn bằng visibility để bảo toàn kích thước, đặt nhãn xử lý lên trên; nút submit vẫn enabled để giữ name/value trong POST. Form chặn submit lặp và khôi phục khi quay lại qua pageshow.
- Không dùng aria-disabled để thay thế điều kiện nghiệp vụ; backend vẫn kiểm tra như cũ.

```html
<div class="button-group">
  <button class="btn btn--primary btn--lg" type="submit">Lưu</button>
  <a class="btn btn--secondary" href="/products">Xem sản phẩm</a>
</div>
```

## Form, lỗi và xác nhận

Control cao tối thiểu 48 px, viền control-border. Checkbox/radio có quy tắc riêng, vùng label cao ít nhất 44 px. Không áp chiều cao control văn bản lên checkbox. Nhãn đặt trên ô; gợi ý mật khẩu theo đúng DTO: 12–72 ký tự, tối đa 72 byte UTF-8.

Trong form có th:object, dùng `ui :: fieldError(field, id)` và aria-describedby trỏ ID tương ứng. Fragment giữ node ID rỗng khi không lỗi. Khi có lỗi, đặt aria-invalid và thêm error-summary với role alert, link tới từng ô. Đăng ký giữ họ tên/email khi lỗi và không phản chiếu mật khẩu. Đăng nhập không bổ sung lưu email trong session; yêu cầu giữ email qua redirect xử lý ở U3.

```html
<div class="field">
  <label for="email">Email</label>
  <input id="email" name="email" type="email" aria-describedby="email-error">
  <!-- Trong Thymeleaf: th:replace="~{fragments/ui :: fieldError('email', 'email-error')}" -->
</div>
```

`ui :: flash(type, message)` hỗ trợ error/alert hoặc success/status. Điều kiện hiển thị phải đặt ở phần tử cha của phần tử th:replace. `.badge` kèm chữ, có biến thể success/error/warning; không biểu đạt trạng thái chỉ bằng màu.

Footer chèn một dialog dùng chung ngoài nền tối. Gắn `data-confirm` trên form hoặc nút submit, nội dung phải có tên đối tượng và hệ quả. app.js mở dialog native, ưu tiên focus Quay lại, Escape đóng và trả focus, Xác nhận dùng requestSubmit với chính submitter ban đầu. Trình duyệt thiếu dialog dùng confirm(); khi tắt JS form vẫn submit theo fallback đã cho phép trong P12. U2–U4 sẽ gắn xác nhận cụ thể lên các thao tác nghiệp vụ hiện có; U1 chỉ cung cấp cơ chế.

```html
<button class="btn btn--danger" type="submit"
        data-confirm="Xóa địa chỉ Nhà riêng? Bạn sẽ cần chọn địa chỉ khác khi đặt hàng.">Xóa địa chỉ</button>
```

## Fragment dùng chung

| Fragment | Hợp đồng |
|---|---|
| `head :: head(title)` | Metadata, title escaped, favicon và asset local |
| `header :: siteHeader` | Skip link, tìm kiếm, menu theo quyền; uiPath và uiCartCount từ GiaoDien_Advice |
| `footer :: siteFooter` | Link thật, kèm một dialog ngoài footer |
| `ui :: flash(type, message)` | type error hoặc success; message được escape |
| `ui :: fieldError(field, id)` | Dùng trong ngữ cảnh th:object, ID duy nhất |
| `ui :: money(amount)` | BigDecimal/số VND, `1.234.567 ₫`; không tính lại giá |
| `ui :: pagination(page, previous, next)` | Page hiện có và URL giữ nguyên query filter |
| `product-card :: productCard(the)` | TheSanPham hiện có, ảnh/placeholder, tên, brand, giá từ, hết hàng |
| `ui :: confirmDialog` | Chỉ chèn một lần qua footer |

GiaoDien_Advice chỉ đọc số lượng Guest session hoặc danh sách giỏ Customer, không tạo giỏ hay cập nhật DB; bỏ badge nếu truy vấn DB lỗi. AnhTaiLen_Advice chỉ dựng lại form SKU cho Admin sau MaxUploadSizeExceededException, trả HTTP 413 và field error. Các giá trị chưa parse được trong multipart không thể phục hồi; giao diện yêu cầu nhập lại, không thông báo lưu thành công.
