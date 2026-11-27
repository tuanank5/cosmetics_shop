# Khoảng cách báo lỗi trên biểu mẫu

- `auth/address-form.html` nhóm nhãn, ô nhập và dòng báo lỗi theo từng trường để giữ khoảng cách nhất quán ở màn thêm và sửa.
- Fragment `fieldError` trong `fragments/ui.html` dùng thuộc tính `hidden` khi trường hợp lệ. Đăng ký, địa chỉ và ô tải ảnh biến thể dùng chung fragment. Mô tả lỗi vẫn có ID để liên kết với ô nhập.
- `order/checkout.html` ẩn dòng báo lỗi địa chỉ và mã giảm giá khi không có nội dung. `storefront.css` chỉ dành chỗ cho khối lỗi báo giá khi khối có phần tử con hiển thị. JavaScript vẫn cập nhật các dòng lỗi khi báo giá thay đổi.
- `storefront.css` bỏ lề thừa dưới hướng dẫn mật khẩu đăng ký.

Chrome với MariaDB thử nghiệm riêng xác nhận các vùng rỗng cao 0 px ở 390 và 1366 px. Nhãn và ô nhập liên quan cách nhau 8 px ở form địa chỉ. Đã kiểm tra lỗi đăng ký, lỗi số điện thoại, lỗi địa chỉ thanh toán và quy trình nhập rồi xóa mã giảm giá không hợp lệ. [Kết quả Chrome](../qa/FIELD-SPACING/browser.json)

Maven ghi nhận 231 ca đạt, 82 bỏ qua, 0 thất bại và 0 lỗi. Đóng gói và `git diff --check` thành công. Bộ kiểm thử JavaScript báo giá đạt 5/5. [Kết quả Maven](../qa/FIELD-SPACING/maven.txt)
