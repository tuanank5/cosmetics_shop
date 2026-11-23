# Bộ nhận diện BeautyShop

Logo gốc thiết kế riêng cho đồ án, không mô phỏng thương hiệu nào. Mọi chữ đã được chuyển thành nét vẽ (outline) nên hiển thị giống nhau trên mọi máy, không cần cài font.

## Ý tưởng
- **Monogram:** chữ **B** đặt trong một **vòm** (gợi gương soi bàn trang điểm và vai chai mỹ phẩm), có ngôi sao bốn cánh nhỏ tượng trưng cho vẻ rạng rỡ. Vòm đôi nét mảnh, đế ngang tạo cảm giác vững và cổ điển.
- **Wordmark:** BEAUTYSHOP viết hoa, kiểu serif cổ điển **Cardo**, giãn chữ 0.16em, toát chất nhà hương thơm/mỹ phẩm cao cấp.
- **Dòng phụ:** "MỸ PHẨM ĐA THƯƠNG HIỆU", kiểu **Montserrat** nét mảnh (300), giãn chữ rộng, hai đường kẻ vàng hai bên.

## Màu
| Vai trò | Mã |
|---|---|
| Nền tối | `#0A0A0B` |
| Chữ ngà | `#F5F2ED` |
| Vàng muted (monogram, kẻ, dòng phụ) | `#A88A5A` |
| Vàng sáng (vệt sáng, favicon) | `#E3CC9C` / `#D9C08F` |
| Chữ trên nền sáng (Admin) | `#1F1A18` |
| Vàng trên nền sáng | `#7A5F33` |

## File
| File | Dùng ở đâu |
|---|---|
| `svg/beautyshop-hero-inline.svg` | Chương 1 trang chủ, nhúng **inline** để chạy hiệu ứng (đi kèm `css/brand-hero.css`) |
| `svg/beautyshop-hero.svg` | Bản hero tĩnh (slide, báo cáo, ảnh bìa) |
| `svg/beautyshop-lockup.svg` | Header storefront (nền tối) |
| `svg/beautyshop-lockup-light.svg` | Header Admin (nền sáng) |
| `svg/beautyshop-mark.svg` | Monogram riêng (avatar, ô ảnh trống, slide) |
| `svg/beautyshop-wordmark.svg` / `-duo` / `-light` | Chữ BEAUTYSHOP một màu / hai màu / nền sáng |
| `svg/favicon.svg` | Biểu tượng tab trình duyệt |
| `css/brand-hero.css` | Hiệu ứng hero: vẽ vòm, chữ hiện lần lượt, kẻ vàng mở ra, vệt sáng lướt qua một lần (~3,7 giây). Tự tắt khi bật giảm chuyển động. Không cần JavaScript. |
| `beautyshop-brand-preview.png` | Ảnh xem thử toàn bộ |

## Quy tắc dùng
- Chừa khoảng trống quanh logo tối thiểu bằng chiều cao chữ "B" của wordmark.
- Không kéo méo, không đổi kiểu chữ, không thêm bóng đổ hay viền.
- Lockup header không nhỏ hơn 32px chiều cao; dưới 360px chỉ dùng monogram.
- Trên nền sáng dùng bản `-light`.

## Giấy phép
Cardo và Montserrat dùng giấy phép SIL Open Font License 1.1 (thư mục `licenses/`). Glyph đã được chuyển thành nét vẽ trong logo, giấy phép OFL cho phép dùng trong sản phẩm, kể cả mục đích thương mại; không bán lại bản thân font.
