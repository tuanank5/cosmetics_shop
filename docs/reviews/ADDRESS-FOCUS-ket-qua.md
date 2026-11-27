# Khoảng đệm cho thẻ được focus

Áp dụng cho `/account/addresses`, `/admin/vouchers` và `/admin/reviews`. Các thẻ có khoảng đệm ngang 16 px cả trước và sau khi nhận focus. Viền focus dày 2 px và nằm trong mép thẻ. Các kích thước thẻ ổn định khi quay lại từ trang chi tiết hoặc trang sửa.

Quy tắc dùng chung nằm trong `src/main/resources/static/css/admin.css`. Ba template áp dụng lớp `catalog-list--spaced` cho danh sách tương ứng.

Kiểm tra bằng cách mở Sửa địa chỉ, Sửa mã giảm giá và Xem đơn mua từ Đánh giá rồi dùng đường quay lại của từng trang. Viền bao cả thẻ và có khoảng cách với nội dung lẫn các nút thao tác.

- Chrome đạt 10 kịch bản trên dữ liệu thử ở 320 px, 390 px và 1366 px, gồm lưu địa chỉ dài trên mobile. Nội dung và nút thao tác nằm trong thẻ. Trang vừa chiều rộng màn hình. [Kết quả Chrome](../qa/ADDRESS-FOCUS/browser.json)
- Maven có 231 ca đạt, 82 ca bỏ qua theo cấu hình mặc định. Đóng gói thành công. [Kết quả Maven](../qa/ADDRESS-FOCUS/maven.txt)
- HTTP `localhost:8080` đã phản hồi với CSS mới. `git diff --check` đạt.
