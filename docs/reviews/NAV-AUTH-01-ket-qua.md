# NAV-AUTH-01 · Quay lại sản phẩm và logo trang tài khoản

Baseline `c79983a`. Owner báo chương 5 bị lệch và có khoảng trống khi nút quay về focus vào thẻ, danh sách sản phẩm thiếu điểm quay lại, còn phần giới thiệu đăng nhập/đăng ký chỉ hiện chữ B.

- `NguonDieuHuong.sanPham()` dùng `#home-featured` cho chương 5. Swiper chọn chương theo anchor của section. Các vùng sản phẩm phía dưới vẫn quay lại thẻ theo mã.
- Cùng luồng tạo URL gắn `#catalog-product-{id}` sau bộ lọc, sắp xếp và phân trang của `/products`. Đích trùng với ID thẻ render trên danh sách, nên trình duyệt cuộn được khi tắt JavaScript. Redirect yêu thích dùng lại URL chung.
- `auth/login.html`, `auth/register.html` và `storefront.css` dùng wordmark BeautyShop có sẵn, với chiều rộng co theo khung và kích thước gốc để dành chỗ khi tải.
- `GiaoDienPagesTest` kiểm URL chương 5 trên context path, URL catalog có bộ lọc và anchor, cùng hành vi hiện có của khu sản phẩm phổ biến.

## Kiểm chứng

- JDK 21 và Maven chạy toàn bộ `test` trong bản build riêng. 316 ca, 241 đạt, 75 bỏ qua, 0 lỗi. `mvn -DskipTests package` đạt. [Maven test](../qa/NAV-AUTH-01/test.txt) · [Đóng gói](../qa/NAV-AUTH-01/package.txt)
- `run-browser.ps1` chạy Chrome headless trên bản JAR mới và MariaDB riêng. Tại 1920 × 1080, chương 5 chiếm đúng từ y = 0 đến y = 1080 sau khi quay lại và focus nằm ngoài thẻ. Khu “Tất cả sản phẩm” trên trang chủ vẫn khôi phục focus về thẻ. Chương 5 khi tắt JavaScript tại 390 px vẫn hiển thị tiêu đề sau khi quay lại.
- Catalog về đúng thẻ trên trang 2 và giữ thương hiệu, sắp xếp giảm dần, cỡ trang và trạng thái mở bộ lọc. Logo tải thành công và có kích thước hiển thị trên cả hai trang xác thực. Cả hai luồng được kiểm ở 390 px với JavaScript tắt và 1366 px với JavaScript bật. Các số đo dùng Chrome mô phỏng, chưa kiểm trên thiết bị vật lý. [Kết quả Chrome](../qa/NAV-AUTH-01/browser-results.json)
