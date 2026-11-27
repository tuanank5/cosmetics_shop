# Quay về Yêu thích và rà soát nguồn điều hướng của khách

## Yêu thích

Liên kết Quay lại sản phẩm yêu thích ở chi tiết sản phẩm giữ số trang gốc và thêm mốc của đúng thẻ. Danh sách đánh dấu các thẻ theo mã sản phẩm trong `auth/wishlist.html`. `wishlist.js` đặt focus khi đi theo liên kết có mốc. Nếu thẻ đã bị bỏ lưu hoặc đã đổi trang, focus về tiêu đề của trang danh sách đang hiển thị. Nguồn Yêu thích tiếp tục được kiểm tra vai trò như trước. Các lệnh lưu và bỏ yêu thích tiếp tục dùng đường chuyển hướng hiện hành.

Chrome và MariaDB thử nghiệm riêng xác nhận focus đúng thẻ ở trang 1 trên điện thoại và trang 2 trên desktop. Khi bỏ sản phẩm cuối của trang 2, liên kết dẫn về trang hợp lệ và focus vào tiêu đề. Khách chưa đăng nhập quay về danh sách sản phẩm. Maven ghi nhận 231 ca đạt, 82 bỏ qua và 0 thất bại. Đóng gói và `git diff --check` thành công. [Chrome](../qa/WISH-RESTORE/browser.json) · [Maven](../qa/WISH-RESTORE/maven.txt)

## Thiết kế cho ba luồng sau mua và địa chỉ

Ba luồng đã được triển khai ở lượt tiếp theo. Kết quả và kịch bản kiểm tra nằm trong [báo cáo khôi phục vị trí](SCROLL-RESTORE-ket-qua.md).

| Màn hình | Nguồn khác đang dùng trang đích | Cách triển khai và rủi ro |
|---|---|---|
| `/orders` | `Checkout_Controller` chuyển sang chi tiết sau COD. `Momo_Controller` và `SauMua_Controller` chuyển sang chi tiết sau thao tác thanh toán hoặc đánh giá. Chi tiết còn mở được bằng URL trực tiếp. | Đổi Xem đơn thành nút điều hướng. Chỉ liên kết từ danh sách mang số trang gốc. Breadcrumb Đơn hàng của bạn dùng số trang và mã đơn để tìm dòng. Với các đường vào khác, breadcrumb dùng danh sách như hiện hành. Rủi ro thấp nếu thay đổi chỉ nằm trên đường về giao diện. |
| `/account/addresses` | Trang địa chỉ có thể mở từ tài khoản hoặc từ Checkout với `from=checkout`. Cả nút Quay lại và thao tác Lưu trong `DiaChiNguoiDung_Controller` dùng `NguonDieuHuong.veDiaChi` để chọn đích. | Chỉ liên kết Sửa từ danh sách địa chỉ của tài khoản đánh dấu nguồn để focus theo mã địa chỉ sau khi quay lại hoặc lưu thành công. Nguồn Checkout tiếp tục quay về `/checkout` để đọc lại địa chỉ và tính báo giá. Lỗi form tiếp tục hiện tại form. Đây là luồng nhạy nhất vì sửa nhầm điều kiện nguồn sẽ đưa khách rời quy trình thanh toán. |
| `/account/returns` | Chi tiết được mở từ bảng đổi trả, từ một đơn hàng hoặc từ chuyển hướng sau khi gửi yêu cầu mới trong `SauMua_Controller`. | Đổi Xem yêu cầu thành nút điều hướng. Chỉ mang trang và mốc dòng khi vào từ danh sách. Chi tiết mở từ đơn, yêu cầu vừa tạo và URL trực tiếp dùng đường về hiện hành. Rủi ro thấp nếu giữ nguyên thao tác tạo yêu cầu, kiểm tra quyền sở hữu và chuyển hướng sau POST. |

Chỉ chấp nhận nguồn đã định nghĩa, số trang trong miền hợp lệ và mã đối tượng thuộc danh sách của người đăng nhập. Các mốc focus trỏ tới dòng đã render và dùng tiêu đề danh sách làm đích dự phòng khi dòng đã đổi trang hoặc biến mất. Giữ link GET hoạt động khi tắt JavaScript.

Khi triển khai, kiểm tra trang 2, mở URL trực tiếp, Back, dòng biến mất và khách khác cố truy cập đơn hoặc yêu cầu. Kiểm riêng nhánh Checkout → Sửa địa chỉ → Quay lại hoặc Lưu, gồm địa chỉ bị xóa, lỗi form và báo giá sau khi thay địa chỉ. Kiểm nhánh gửi đánh giá, tạo đổi trả và mở lại thanh toán để xác nhận mỗi nguồn vẫn nhận đúng đích.

## Màn hình có thể bổ sung

- **Giỏ hàng → Chi tiết sản phẩm.** Dòng giỏ đã có mã biến thể. Có thể dùng mã này để quay về đúng dòng, với tiêu đề giỏ làm đích dự phòng nếu dòng biến mất sau cập nhật hoặc gộp giỏ.
- **Chi tiết đơn → Gửi hoặc sửa đánh giá.** Có thể đưa focus về dòng sản phẩm đã đánh giá khi bấm Quay lại đơn hoặc sau khi gửi thành công. Nên kiểm tra riêng chuyển hướng sau POST.
- **Chi tiết đơn → Tạo yêu cầu đổi trả.** Có thể đưa focus về vùng đổi trả của đơn khi bấm Quay lại đơn từ form. Đường sau khi gửi yêu cầu mới vẫn đi đến chi tiết yêu cầu.
- **Tổng quan quản trị.** Các nguồn từ Tổng quan đã có kỳ thống kê trong URL. Có thể thêm mốc cho từng nhóm hoặc dòng đã mở mà vẫn giữ kỳ.

Danh sách sản phẩm ngoài cửa hàng và các bảng quản trị đã có mốc quay về. Trang chủ Chương 5 có quy tắc riêng đưa người dùng về đầu chương theo D38.
