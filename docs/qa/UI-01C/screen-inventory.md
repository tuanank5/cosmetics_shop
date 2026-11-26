# UI-01C · Kiểm kê giao diện

| Màn | Tiêu đề và đường quay lại | Hành động, trạng thái và bố cục |
|---|---|---|
| Catalog | Giữ tiêu đề, số kết quả và các chip đang áp dụng | SSR mở bộ lọc khi đang lọc hoặc đổi sort. Link khám phá mở đúng bộ lọc khi JavaScript tắt. Form giữ kích thước trang, pager giữ lựa chọn đóng/mở và các tham số truy vấn. |
| PDP | Giữ Back của G4, tiêu đề sản phẩm và nhãn SKU | Dùng disclosure native độc lập, ID theo slug và mục. Back giữ lựa chọn của entry, truy cập mới bắt đầu đóng. Giữ badge tồn, ảnh SKU, nút mua và thông báo kho đánh giá. |
| Đơn Customer/Admin | Giữ đường về danh sách hoặc kỳ dashboard và mã đơn | Dùng badge có nhãn trạng thái đơn và vùng thông báo chung. Giữ thông tin thanh toán, danh sách SKU và các nút/điều kiện hiện có. |
| Đổi trả Customer/Admin | Giữ đường về danh sách hoặc dashboard, tiêu đề và mã yêu cầu | Dùng badge trạng thái trả hàng và vùng thông báo chung. Danh sách mục đã dùng bố cục wrap và nhãn số lượng. |
| Checkout và địa chỉ | Giữ bước mua hàng và Back/hủy về đúng nguồn G4 | Thêm vùng thông báo cho consumer sau này. Các input địa chỉ liên kết lỗi trường bằng aria-describedby/aria-invalid. Giữ lỗi checkout và báo giá theo cơ chế hiện hành. |
| SKU và catalog Admin | Giữ tiêu đề theo sản phẩm/loại danh mục và link về màn cha | Form và danh sách hiện hữu dùng catalog-form/catalog-list, có wrap, nhãn và lỗi trường. Giữ CRUD và hành động tồn kho. |
| Voucher Admin | Giữ tiêu đề tạo/chỉnh sửa và đường về dashboard hoặc danh sách | Dùng catalog-form hiện hữu, nhóm giờ UTC và lỗi tại từng trường. Giữ validation, hành động và điều hướng G4. |
| Danh sách/chi tiết tài khoản Admin | Nhãn ở cả sidebar và menu là Quản lý tài khoản. Chi tiết giữ q/page, đưa link quay lại lên trước tiêu đề. | Giữ nhãn vai trò và quy tắc tự khóa. Disclosure đặt lại mật khẩu dùng xác nhận, Escape trả focus, form vẫn dùng được khi tắt JavaScript. |
| Đăng nhập, đăng ký, bảo mật, đặt lại mật khẩu | Giữ tiêu đề, liên kết và trạng thái của form | Dùng ô nút 48px và SVG cùng kích thước, accessible name ổn định theo từng trường, aria-pressed và focus. Giữ autocomplete, tên trường, CSRF và lỗi BindingResult. |
| Dashboard | Giữ tiêu đề, kỳ, KPI và tên từng biểu đồ | Bảng native mở mặc định, vùng cuộn có tên và điểm Tab. Chỉ ghi lựa chọn đóng từng bảng vào bộ nhớ tab khi có JavaScript. Chart.js vẫn đọc cùng bảng và render canvas. |
| Giỏ và duyệt đánh giá | Giữ heading, Back, trạng thái dữ liệu và luật thao tác | Chuẩn bị vùng thông báo chung dùng message/error đang có cho CART-01 và REVIEW-01. |
