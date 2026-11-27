# Điều hướng catalog và tài khoản

## Thay đổi giao diện

- Bộ sưu tập, chi tiết sản phẩm, Wishlist, địa chỉ, bảo mật, đơn hàng và danh sách đổi trả của khách dùng breadcrumb chung trong `fragments/ui.html`. Trang `/orders` dùng đường dẫn `Trang chủ › Tài khoản › Đơn hàng của bạn`. Trang địa chỉ vẫn có đường quay về thanh toán khi mở từ checkout. Trang bảo mật dùng mục Tài khoản trong breadcrumb để quay lại hồ sơ.
- `auth/account.html` hiển thị nhãn Email và địa chỉ trên cùng dòng khi đủ chỗ. Liên kết đơn hàng đứng đầu nhóm liên kết dưới hồ sơ, cách nút Lưu hồ sơ một khoảng theo token giao diện.
- Trang chi tiết dùng mục đóng mở cho Thành phần khi có dữ liệu. Liên kết quay lại tiếp tục dùng `navSource.duongVe`. CSS chỉ gạch chân phần chữ khi rê chuột hoặc dùng bàn phím. `CuaHang_Controller.chiTiet` lấy token CSRF trước khi Thymeleaf gửi HTML để khách mới có thể tải hết trang chứa form giỏ hàng.
- `auth/addresses.html` đặt thông tin bên trái và các nút thao tác bên phải trên màn hình rộng. Trên điện thoại, hai nút Sửa và Xóa đứng cạnh nhau bên dưới thông tin.
- Danh mục và Thương hiệu có hai liên kết riêng trong `fragments/admin-nav.html`. Ba danh sách catalog giữ các thẻ điều hướng. Biểu mẫu thêm và sửa hiển thị breadcrumb bắt đầu từ Sản phẩm, Danh mục hoặc Thương hiệu tương ứng. Danh sách biến thể dùng Sản phẩm › Biến thể sản phẩm, còn biểu mẫu biến thể có thêm bước thêm hoặc sửa.
- Footer quản trị dùng ít khoảng đệm dọc hơn và nằm sát đáy màn hình khi nội dung ngắn. Trên trang dài, footer xuất hiện sau nội dung khi cuộn đến cuối trang.
- Chi tiết đơn quản trị hiển thị Đơn hàng › Chi tiết. Liên kết Đơn hàng dùng `backToOrders` để giữ bộ lọc, trang và dòng đã mở. Khi vào từ tổng quan, breadcrumb có thêm liên kết về kỳ thống kê đã chọn. Chi tiết đơn của khách nối tiếp đường dẫn Trang chủ › Tài khoản › Đơn hàng của bạn.
- `admin/user/list.html` dùng bộ lọc trạng thái Tất cả, Đang hoạt động, Đã khóa cùng ô tìm họ tên hoặc email. `NguoiDung_Repository.timNguoiDung` dùng cùng điều kiện lọc cho dữ liệu và tổng số dòng. Trạng thái đã chọn đi cùng liên kết phân trang, chi tiết tài khoản và chuyển hướng sau thao tác quản trị.
- `product/list.html` và `order/list.html` đặt thanh phân trang dưới danh sách. `components.css` dành vị trí riêng cho lùi, số trang và tiến, chuyển sang hai hàng khi thiếu chiều rộng. Các trang dùng chung fragment nhận bố cục này.

## Rà soát danh sách

Kết quả trong bảng dưới đây ghi nhận hiện trạng lúc rà soát giao diện. Xem cấu hình phân trang hiện hành và giới hạn địa chỉ trong [báo cáo LIST-PAGING](LIST-PAGING-ket-qua.md).

| Tình trạng | Màn hình và căn cứ |
|---|---|
| Có phân trang | Catalog cửa hàng, đánh giá ở chi tiết sản phẩm, quản trị đơn hàng, tài khoản và duyệt đánh giá đều gọi fragment `pagination`. |
| Chưa phân trang | Ba danh sách quản trị sản phẩm, danh mục, thương hiệu dùng `AdminDanhMucSanPhamThuongHieu_Controller` và `DanhMucSanPhamThuongHieu_Service.list`. Danh sách biến thể của từng sản phẩm dùng `AdminBienTheSanPham_Controller.danhSach`. |
| Các danh sách khác chưa phân trang | Đơn hàng của khách (`DonHang_Controller.danhSach`), yêu thích, đổi trả của khách và quản trị (`SauMua_Controller`, `AdminSauMua_Controller`), địa chỉ giao hàng (`auth/addresses.html`), mã giảm giá quản trị (`AdminPhieuGiamGia_Controller.danhSach`). |

Các màn hình phù hợp để bổ sung breadcrumb tiếp theo gồm thêm/sửa địa chỉ, chi tiết yêu cầu đổi trả, biểu mẫu mã giảm giá và chi tiết tài khoản quản trị. Các đường quay về có bộ lọc hoặc nguồn checkout cần được giữ khi bổ sung.

## Kiểm chứng

JDK 21, bản sao build sạch trong `target`. `mvn -o test` ghi nhận 312 ca, 231 đạt, 81 bỏ qua, 0 thất bại và 0 lỗi. `mvn -o -DskipTests package` và `git diff --check` thành công.

Chrome headless với Spring Boot và MariaDB riêng đã kiểm tra liên kết cha và mục hiện tại theo từng vai trò, thành phần hiển thị trực tiếp, kiểu chữ của nút quay lại, cùng tọa độ ngang ba nhóm phân trang ở trang 1 và 2 trên viewport 390 và 1366 px. Fixture có hai sản phẩm và 41 đơn. Đã kiểm tra tràn ngang trên hai màn danh sách ở hai kích thước này. Fixture không bật dịch vụ MongoDB thật. Danh sách màn hình đã chạy nằm trong [browser.json](../qa/NAV-CATALOG/browser.json).

Lần kiểm tra sau xác nhận thanh phân trang nằm dưới danh sách, breadcrumb đơn hàng liên kết về trang chủ, nhãn và địa chỉ email cùng dòng ở cả hai chiều rộng, khoảng cách dưới nút lưu và đường về tài khoản ở trang bảo mật. Maven ghi nhận 231 ca đạt, 81 bỏ qua, 0 thất bại và 0 lỗi. Đóng gói thành công. [Browser](../qa/NAV-CATALOG/browser-followup.json) · [Maven](../qa/NAV-CATALOG/test-followup.log)

Lần kiểm tra bố cục địa chỉ xác nhận hai nút cùng hàng ở 390 và 1366 px, breadcrumb `/orders` có liên kết về trang chủ và tài khoản, mục Thành phần mở bằng Enter và đóng bằng Space, và nút gợi ý điền câu hỏi vào ô chat. Maven ghi nhận 231 ca đạt, 81 bỏ qua, 0 thất bại và 0 lỗi. Đóng gói thành công. [Browser](../qa/NAV-CATALOG/browser-addresses.json) · [Maven](../qa/NAV-CATALOG/test-addresses.log)

Lần kiểm tra quản trị xác nhận breadcrumb trên biểu mẫu catalog và chi tiết đơn, đường về danh sách đơn đúng trang, bộ lọc và dòng đã mở. Chrome kiểm tra lọc tài khoản, đi đến trang 2 và từ chi tiết quay lại đúng bộ lọc ở 390 và 1366 px, cùng chuyển hướng sau khi khóa tài khoản. Maven ghi nhận 231 ca đạt, 81 bỏ qua trong cấu hình mặc định. Chín ca tích hợp với MariaDB riêng đạt. Đóng gói và kiểm tra diff thành công. [Browser](../qa/NAV-CATALOG/browser-admin.json) · [Maven](../qa/NAV-CATALOG/test-admin.log) · [MariaDB](../qa/NAV-CATALOG/admin-integration.txt)

Chrome và MariaDB thử nghiệm xác nhận breadcrumb ở `/admin/catalog/products/9/variants`. Footer cao 38 px ở chiều rộng 390 và 1366 px, nằm sát đáy màn hình trên trang biến thể trống và nằm dưới danh sách trên trang quản lý tài khoản dài. Maven ghi nhận 231 ca đạt, 81 bỏ qua và 0 thất bại. Đóng gói và kiểm tra diff thành công. [Browser](../qa/NAV-CATALOG/browser-footer.json) · [Maven](../qa/NAV-CATALOG/test-footer.log)

Các tab ở ba màn danh sách catalog theo thứ tự Thương hiệu › Danh mục › Sản phẩm. Chrome xác nhận thứ tự và trạng thái tab đang mở ở 390 và 1366 px. Maven ghi nhận 231 ca đạt, 82 bỏ qua và 0 thất bại. Đóng gói thành công. [Browser](../qa/NAV-CATALOG/browser-tabs.json) · [Maven](../qa/NAV-CATALOG/maven-tabs.txt)
