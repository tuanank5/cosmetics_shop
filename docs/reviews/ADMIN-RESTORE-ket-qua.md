# Kích thước nút và khôi phục dòng quản trị

- `admin/user/list.html` và `return/list.html` dùng nút điều hướng cho Chi tiết tài khoản và Xem yêu cầu. Chi tiết tài khoản dùng breadcrumb Quản lý tài khoản › Chi tiết tài khoản.
- `NguonDieuHuong` giữ bộ lọc và số trang đã mở qua liên kết chi tiết, biểu mẫu và chuyển hướng sau khi lưu. Danh sách biến thể giữ riêng trang biến thể và nguồn danh sách sản phẩm. Khi quay lại từ biểu mẫu tải ảnh quá lớn, `AnhTaiLen_Advice` đọc các tham số nguồn đã kiểm tra từ URL biểu mẫu.
- Danh sách sản phẩm, danh mục, thương hiệu, biến thể, tài khoản, mã giảm giá và đổi trả có mốc cố định trên từng dòng. `app.js` đặt focus vào mốc khi quay lại bằng breadcrumb. Browser Back dùng vị trí cuộn của trình duyệt và khôi phục focus theo lịch sử của trang. Nếu dòng đã bị xóa hoặc ra khỏi bộ lọc, focus chuyển đến phần đếm kết quả.
- Sau khi duyệt hoặc từ chối yêu cầu đổi trả, trang chi tiết đặt focus vào thông báo kết quả. Breadcrumb dẫn lại trang danh sách và dòng yêu cầu.
- Yêu thích dùng cùng chiều rộng cho Thêm vào giỏ, Hết hàng và Chọn biến thể. Bỏ yêu thích nằm ở mép cuối vùng thao tác. Các nút trạng thái catalog và tài khoản có chiều rộng đồng nhất. Phân trang căn đều chiều cao. Bộ lọc quản trị chia hai nút đều nhau trên điện thoại.
- Các cặp thao tác mã giảm giá và đổi trả, bộ chọn kỳ Tổng quan và nút điều khiển video dùng chiều rộng nhất quán trong từng nhóm. Các nút video được chỉnh theo CSS của trạng thái phát và dừng.

## Kiểm thử

- Maven ghi nhận 231 ca đạt, 82 bỏ qua và 0 thất bại. Đóng gói thành công. [Kết quả Maven](../qa/ADMIN-RESTORE/maven.txt)
- Mười chín ca tích hợp trên MariaDB riêng đạt, gồm catalog, biến thể, tài khoản, bảo mật tài khoản và danh sách đơn quản trị. [Kết quả tích hợp](../qa/ADMIN-RESTORE/integration.txt)
- Chrome với MariaDB và MongoDB thử nghiệm xác nhận các trạng thái mua hàng ở Yêu thích đều 140 × 44 px từ 320 đến 1366 px. Đã đo cặp nút mã giảm giá ở 320, 390 và 1366 px và xác nhận chiều cao không kéo theo toàn bộ thẻ. Kiểm tra phân trang, nút trạng thái, Browser Back, đường quay lại từ trang 2, dòng đã xóa, luồng biến thể lồng nhau và focus sau cả duyệt lẫn từ chối đổi trả. [Kết quả Chrome](../qa/ADMIN-RESTORE/browser.json)
