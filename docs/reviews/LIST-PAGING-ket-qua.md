# Phân trang danh sách và giới hạn địa chỉ

| Màn hình | Số mục mỗi trang | Hiện thanh phân trang từ |
|---|---:|---:|
| Đơn hàng của khách và yêu cầu đổi trả của khách | 10 | 11 |
| Sản phẩm yêu thích | 12 | 13 |
| Sản phẩm, danh mục, thương hiệu và biến thể trong quản trị | 15 | 16 |
| Quản lý tài khoản, mã giảm giá và đổi trả trong quản trị | 15 | 16 |
| Duyệt đánh giá | 20 | 21 |
| Đơn hàng quản trị | 20 mặc định, có lựa chọn 10 hoặc 50 | 21 mặc định, hoặc 11 và 51 theo lựa chọn |

Thanh phân trang nằm dưới danh sách và dùng tổng số kết quả sau lọc. `PhanTrang.moTa` định dạng số lượng ngắn cho 0 hoặc 1 trang và khoảng thứ tự kèm tổng khi có nhiều trang. Nhãn số lượng dùng đúng loại dữ liệu, gồm yêu cầu đổi trả và mã giảm giá.

`order/list.html` và `return/list.html` dùng tiêu đề cột cho các bảng của khách. Bảng quản trị tài khoản có họ tên, email, vai trò, trạng thái và thao tác. Bộ đếm nằm phía trên danh sách. Các màn quản trị tài khoản, đổi trả, catalog, biến thể, mã giảm giá và duyệt đánh giá căn trái. Đơn hàng và đổi trả của khách căn phải. Nút tạo mã giảm giá ở bên phải tiêu đề, đường phân cách nằm trên mục đầu tiên. Bộ lọc catalog gồm tên hoặc đường dẫn và trạng thái. Sản phẩm quản trị còn lọc được theo danh mục và thương hiệu. Mã giảm giá lọc theo mã và trạng thái. Tài khoản lọc được đồng thời theo từ khóa, trạng thái và vai trò.

`DiaChiNguoiDung_Service.luu` kiểm tra mốc 10 khi tạo mới trong giao dịch đã khóa hàng tài khoản. POST trực tiếp nhận thông báo khi đã đủ 10 địa chỉ và không tạo bản ghi. Trang danh sách ẩn nút thêm khi đạt mốc này, các thao tác sửa và xóa vẫn có sẵn.

Từ Duyệt đánh giá, liên kết Xem đơn mua ghi bộ lọc và trang vào nguồn điều hướng. Nút Quay lại đánh giá trên chi tiết đơn dùng các giá trị đã kiểm tra để về lại trang và đặt focus vào liên kết của đánh giá gốc. Nếu mục đã rời kết quả lọc, mục tiêu focus là tiêu đề kết quả. Trang gửi đánh giá dùng cùng kiểu liên kết có mũi tên với trang chi tiết sản phẩm.

Biểu mẫu mã giảm giá và chi tiết đổi trả trong quản trị dùng breadcrumb rút gọn. Khi vào từ Tổng quan, breadcrumb có thêm liên kết về kỳ thống kê đã chọn. Duyệt đánh giá dùng cùng đường phân cách với Mã giảm giá, đặt ngay dưới bộ đếm khi có kết quả.

## Vùng thông báo

Bản rà soát dưới đây ghi nhận hiện trạng trước đợt chỉnh khoảng cách biểu mẫu. [Chỉnh sửa đăng ký, địa chỉ và thanh toán](FIELD-SPACING-ket-qua.md)

| Khu vực | Thông báo và cách xử lý khi rỗng |
|---|---|
| Duyệt đánh giá | `review-notice` hiện khi có thông báo duyệt, ẩn hoặc lỗi. Đã bỏ chiều cao tối thiểu 5,5 rem. Khi kho đánh giá lỗi, thông báo riêng giải thích tình trạng tải. |
| Form đăng ký, thêm hoặc sửa địa chỉ | Fragment `fieldError` giữ phần tử lỗi theo từng trường. Các phần tử rỗng vẫn tham gia khoảng cách của CSS Grid. Hiện tượng dễ thấy ở biểu mẫu địa chỉ vì có ba vị trí lỗi trong lưới có gap lớn. |
| Thanh toán | Thông báo báo giá rỗng có chiều cao và lề bằng 0. Vùng lỗi tổng rỗng đã ẩn. Ô lỗi cạnh địa chỉ và mã giảm giá có thể còn một khoảng nhỏ theo gap của nhóm trường. |
| Giỏ, thanh toán, bảo mật, đăng ký, đơn hàng và đổi trả chi tiết, quản trị catalog, biến thể và tài khoản | Fragment `notices` tạo khung thành công hoặc lỗi khi có nội dung. Thông báo gộp giỏ cũng chỉ tạo khung khi có điều chỉnh. |
| Sản phẩm yêu thích, trang chủ, catalog và chi tiết sản phẩm | Vùng phản hồi yêu thích rỗng được đưa khỏi luồng bố cục. |
| Chat và sao chép mã đơn của khách | Hai vùng phản hồi được ẩn khi rỗng. |
| Địa chỉ và mã giảm giá | Thông báo lưu, xóa hoặc lỗi được render theo điều kiện của từng trang. |

## Kiểm chứng

- Maven dùng JDK 21 ghi nhận 313 ca, gồm 231 đạt, 82 bỏ qua, 0 thất bại và 0 lỗi. Đóng gói và `git diff --check` thành công. [Kết quả Maven](../qa/LIST-PAGING/maven.txt)
- Chín ca tích hợp với database MariaDB riêng đạt, gồm quy tắc 10 địa chỉ, lưu địa chỉ đồng thời, lọc tài khoản, catalog và đơn quản trị. [Kết quả tích hợp](../qa/LIST-PAGING/integration.txt)
- Chrome chạy với MariaDB và MongoDB thử nghiệm riêng ở 390 px cho luồng khách và 1366 px cho quản trị. Đã đo vị trí bộ đếm và thanh phân trang, xác nhận nhãn đầu cột, xóa mục cuối trang yêu thích và kiểm tra focus khi quay từ đơn mua về đúng đánh giá. [Bằng chứng Chrome](../qa/LIST-PAGING/browser.json)
- Đợt chỉnh căn lề xác nhận bộ đếm của sản phẩm, danh mục, thương hiệu, biến thể và mã giảm giá nằm bên trái. Bộ đếm duyệt đánh giá trùng họ chữ, cỡ chữ, độ đậm, chiều cao dòng và màu với các danh sách quản trị. Maven ghi nhận 231 ca đạt, 82 bỏ qua, 0 thất bại. Đóng gói thành công. [Chrome](../qa/LIST-PAGING/browser-count-alignment.json) · [Maven](../qa/LIST-PAGING/maven-count-alignment.txt)
- Đợt chỉnh breadcrumb xác nhận đường dẫn của trang thêm và sửa mã giảm giá, chi tiết đổi trả và liên kết về đúng kỳ Tổng quan. Chrome kiểm tra đường phân cách ở 390 và 1366 px. Maven ghi nhận 231 ca đạt, 82 bỏ qua, 0 thất bại. Đóng gói thành công. [Chrome](../qa/LIST-PAGING/browser-voucher-return-nav.json) · [Maven](../qa/LIST-PAGING/maven-voucher-return-nav.txt)
