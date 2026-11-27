# Khôi phục nét trên logo chữ BeautyShop

SVG cũ có khung bắt đầu tại y = 0, trong khi nét chữ cao nhất ở y = −3,9. Chrome ghi nhận 126 pixel có màu trên hàng đầu tiên khi ảnh được hiển thị rộng 280 px. Phần nét vượt khung bị cắt. [Số đo trước khi sửa](../qa/WORDMARK-FIX/before.json) và [ảnh đăng nhập trước khi sửa](../ui/screenshots/WORDMARK-FIX/login-before.webp).

`src/main/resources/static/img/brand/beautyshop-wordmark.svg` dùng đúng 10 đường nét chữ của chương mở đầu. Khung mới để lại 8,2 đơn vị SVG phía trên và 9,5 đơn vị phía dưới. Màu chữ ngà phù hợp với nền tối ở cả ba vị trí. Các kích thước ảnh trong HTML khớp SVG.

| Nơi tham chiếu trực tiếp | Kết quả |
| --- | --- |
| `templates/fragments/footer.html` | Footer dùng chung nhận ảnh mới. [Ảnh footer](../ui/screenshots/WORDMARK-FIX/footer-after.webp) |
| `templates/auth/login.html` | Logo trong `auth-intro` hiển thị đầy đủ nét. [Ảnh đăng nhập](../ui/screenshots/WORDMARK-FIX/login-after.webp) |
| `templates/auth/register.html` | Logo trong `auth-intro` hiển thị đầy đủ nét trên mobile. [Ảnh đăng ký](../ui/screenshots/WORDMARK-FIX/register-mobile.webp) |

Rà soát `src/main` tìm thấy ba tham chiếu trực tiếp trên. Các trang khác nhận ảnh qua footer dùng chung. Trang mở đầu dùng SVG nội tuyến. Header cửa hàng và quản trị dùng các tệp lockup riêng, có khoảng đệm dương quanh nét vẽ. Hai biến thể wordmark `-light` và `-duo` hiện chưa có tham chiếu trong giao diện.

Chrome chạy 21 phép kiểm trên `localhost:8080` ở chiều rộng 320, 390 và 1366 px. SVG mới khớp từng đường nét với chương mở đầu và hàng pixel đầu tiên đã trong suốt. [Kết quả Chrome](../qa/WORDMARK-FIX/browser.json) và [ảnh tham chiếu chương mở đầu](../ui/screenshots/WORDMARK-FIX/opening-reference.webp).

Maven có 231 ca đạt và 82 ca được bỏ qua theo cấu hình mặc định. Đóng gói và `git diff --check` đều đạt. [Kết quả Maven](../qa/WORDMARK-FIX/maven.txt).
