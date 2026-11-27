# Rà soát kích thước nút BeautyShop

Báo cáo này lưu số đo trước khi chỉnh. Xem [thay đổi và kiểm tra sau chỉnh sửa](ADMIN-RESTORE-ket-qua.md).

Rà soát 56 mẫu Thymeleaf cùng các quy tắc CSS dùng chung và mã JavaScript đổi nhãn nút. Đo 47 lượt mở trang theo vai trò Guest, Customer và Admin trên Chrome với MariaDB và MongoDB thử nghiệm riêng ở 360, 390, 768 và 1366 px. Đo bổ sung các trạng thái và điểm gãy của Yêu thích từ 320 đến 1366 px. Số đo trong báo cáo là pixel CSS. [Bằng chứng đo](../qa/BUTTON-WIDTH/browser.json) · [Đo bổ sung](../qa/BUTTON-WIDTH/browser-states.json)

## Các nhóm có kích thước lệch

| Mức ưu tiên | Vị trí | Kết quả và nguyên nhân |
|---|---|---|
| Cao | `/account/wishlist` | **Thêm vào giỏ** rộng 130 px, **Hết hàng** 104 px, **Chọn biến thể** 132,8 px. Cả ba cao 44 px. Ở 1366 px, nút **Bỏ yêu thích** bắt đầu ở x=1073, 1099 hoặc 1102 tùy trạng thái. Khi sản phẩm ngừng bán, **Bỏ yêu thích** dời về đầu cụm. `auth/wishlist.html` dùng ba dạng phần tử cho cùng vị trí. `storefront.css` bỏ chiều rộng tối thiểu và `reference-pages.css` để nhóm nút dàn theo bề rộng nội dung. |
| Cao | Quản trị Sản phẩm, Danh mục, Thương hiệu và Biến thể | **Ngừng hoạt động** rộng 131,5 px, **Kích hoạt lại** rộng 96,8 px, cùng cao 44 px. Ô thao tác giữ nguyên chức năng nhưng chiều rộng thay đổi theo trạng thái. `admin/catalog/list.html` và `admin/variant/list.html` dùng `.admin-button` với chiều rộng tối thiểu 44 px. |
| Cao | Phân trang dùng chung | Số trang hiện tại cao 44 px, các số trang bấm được cao 50,8 px ở desktop và 46 px ở 390 px. Trên màn hẹp, nút văn bản **Trang sau** và **Trang cuối** có thể xuống hai dòng và cao 66 px. Phần lệch giữa các số trang đến từ phần tử hiện tại và liên kết dùng padding và line-height riêng trong `components.css`. |
| Vừa | Quản lý tài khoản | **Khóa** rộng 52,1 px và **Mở khóa** rộng 75,3 px, cùng cao 44 px trên một trang kết quả. Hai trạng thái dùng cùng vị trí với chiều rộng theo nhãn trong `admin/user/list.html`. |
| Vừa | Bộ lọc quản trị sản phẩm, danh mục, thương hiệu, mã giảm giá và tài khoản | Ở 390 px, **Áp dụng** rộng 313 px, **Xóa bộ lọc** rộng 138,1 px và xuống hàng riêng. Quy tắc cho nút chính trong form ở `components.css` cho nút chiếm hết hàng. Nút xóa giữ chiều rộng theo nội dung. **Lọc** trên Duyệt đánh giá rộng 120 px ở cùng viewport. |
| Vừa | Mã giảm giá | **Sửa** rộng 120 px, **Xóa nếu chưa sử dụng** rộng 226,8 px. Cùng nhóm thao tác, cùng cao 50,8 px. Điện thoại hiển thị hai nút trên hai hàng. Hai thao tác có mức độ rủi ro khác nhau, nên đây là vấn đề cân đối bố cục. |
| Thấp | Chi tiết đổi trả quản trị | **Đã duyệt** rộng 126,2 px, **Đã từ chối** rộng 136,2 px ở desktop. Ở 390 px cả hai rộng 358 px. |
| Thấp | Bộ chọn kỳ trên Tổng quan quản trị | **7 ngày**, **30 ngày**, **90 ngày** rộng 79,4, 90,6 và 89 px ở 1366 px. Ở 390 px, ba mục rộng khoảng 113–114 px. |
| Theo mã | Nút phát và dừng video ở Trang chủ và Catalog | `home-story.js` và `catalog-preview.js` đổi nội dung nút khi phát hoặc dừng. CSS cho nút lấy chiều rộng theo nội dung nên chiều rộng có thể đổi. Fixture chưa có video mẫu để đo nhánh đang phát. |

## Các nhóm đã đối chiếu

- **Chi tiết sản phẩm.** Hai trạng thái còn hàng và hết hàng cùng rộng 540,7 px và cao 56 px ở 1366 px. Cả hai dùng chiều rộng của vùng thao tác. Nút yêu thích phía dưới có chiều cao riêng 50,8 px.
- **Địa chỉ giao hàng.** **Sửa** rộng 86 px và **Xóa** rộng 86,6 px, cùng cao 50,8 px. Cặp này đứng cùng hàng trên điện thoại ở viewport 390 px.
- **Đăng nhập, đăng ký, hồ sơ, form địa chỉ, bảo mật, gửi đánh giá và gửi yêu cầu đổi trả.** Các nút gửi thông thường lấp độ rộng form. Cặp đăng nhập và gửi mã xác minh có cùng kích thước ở từng viewport. Nút mắt mật khẩu rộng 48 px ở các form đã đo.
- **Giỏ hàng và thanh toán.** Nút cộng và trừ đều 44 × 44 px. Nút xóa và nút thanh toán có kích thước theo vai trò riêng. Nút **Đặt đơn hàng** luôn lấp độ rộng vùng thanh toán và cao 56 px trong các ca đã đo.
- **Chat.** Các chủ đề gợi ý có chiều rộng theo nhãn và cùng chiều cao ở từng viewport. Nút gửi biểu tượng đo được 46 × 46 px.
- **Đơn hàng quản trị.** Ba trạng thái đã đo gồm **Xác nhận đơn hàng**, **Bắt đầu đóng gói** và **Xác nhận đã giao và thu tiền**. Cả ba cùng kích thước 289,7 × 50,8 px ở desktop.
- **Duyệt đánh giá.** **Duyệt đánh giá** và **Ẩn đánh giá** đều đo được 184 × 50,8 px.
- **Trang lỗi.** Trên điện thoại, hai nút **Xem sản phẩm** và **Về trang chủ** lấp cùng chiều rộng. Desktop dùng hai nhãn có độ dài khác nhau ở hai cấp hành động.

Yêu thích tự xuống dòng theo viewport. Ở 390 px, **Bỏ yêu thích** xuống dòng trên các thẻ đang bán. Ở 1366 px, ba nút nằm cùng hàng. Các viewport bổ sung từ 320 đến 1366 px đã kiểm tra đều giữ trang trong chiều rộng màn hình.

Các nhánh OTP, Google/GitHub, MoMo, video đang phát và tái lập chỉ mục chatbot được rà theo mã khi fixture thiếu cấu hình hoặc dữ liệu kích hoạt.
