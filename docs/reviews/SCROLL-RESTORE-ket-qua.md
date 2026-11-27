# Khôi phục vị trí trong danh sách

| Luồng | Vị trí sau khi quay lại |
| --- | --- |
| Quản trị → Chi tiết đơn → Đơn hàng | Toàn bộ dòng đơn, theo trang và bộ lọc đã mở. Giữ mốc `#admin-order-{id}`. |
| Quản trị → Đánh giá → Xem đơn mua → Quay lại đánh giá | Toàn bộ thẻ đánh giá. Duyệt hoặc ẩn đánh giá cũng focus thẻ nếu nó vẫn thuộc bộ lọc. Nếu thẻ đã rời danh sách, focus phần kết quả. |
| Quản trị → Thêm sản phẩm, danh mục, thương hiệu, biến thể hoặc mã giảm giá | Quay về trang và bộ lọc đã mở. Các thao tác Sửa vẫn focus dòng đã mở. |
| Đơn hàng của bạn → Xem đơn → Đơn hàng của bạn | Toàn bộ dòng đơn ở trang xuất phát. |
| Địa chỉ giao hàng → Sửa → Quay lại địa chỉ hoặc Lưu địa chỉ | Toàn bộ thẻ địa chỉ, kể cả sau lỗi form rồi lưu lại. |
| Thanh toán → Quản lý địa chỉ → Sửa → Quay lại hoặc Lưu | Quay về `/checkout` với dữ liệu địa chỉ cập nhật khi lưu. |
| Yêu cầu đổi trả → Xem yêu cầu → Quay lại yêu cầu đổi trả | Toàn bộ dòng yêu cầu ở trang xuất phát. |
| Giỏ hàng → Chi tiết sản phẩm → Quay lại giỏ hàng | Toàn bộ dòng biến thể đã mở. Đổi biến thể đang xem vẫn giữ biến thể nguồn. Dòng đã mất thì focus tiêu đề Giỏ hàng. |

Trang chi tiết đơn và đổi trả mở từ luồng khác dùng đường về danh sách mặc định. Với Back của trình duyệt, nếu Chrome phục hồi bản giỏ hàng từ bộ nhớ đệm, giỏ vẫn khóa thao tác và hiện nút **Xem giỏ hàng hiện tại** để tải dữ liệu mới.

## Kịch bản kiểm tra thủ công

1. **Địa chỉ từ hai nguồn.** Từ Tài khoản, sửa địa chỉ và thử cả Quay lại địa chỉ lẫn Lưu. Thẻ tương ứng có viền focus. Từ Thanh toán, vào Quản lý địa chỉ và thử hai thao tác tương tự. Cả hai về `/checkout`. Khi lưu, kiểm tra địa chỉ và tổng tiền dự kiến. Xóa nội dung một trường bắt buộc rồi gửi form để kiểm tra lỗi và đích quay lại.
2. **Đơn hàng từ nhiều nguồn.** Mở đơn ở trang 2 rồi dùng breadcrumb. Trang 2 và cả dòng đơn được khôi phục. Mở một đơn khác trên cùng trang rồi dùng Back của trình duyệt. Dòng vừa mở phải nằm trong khung nhìn và có viền focus. Tạo đơn COD thử hoặc mở URL chi tiết trực tiếp. Breadcrumb dẫn về danh sách đơn mặc định.
3. **Đổi trả từ nhiều nguồn.** Mở một yêu cầu ở trang 2 danh sách đổi trả và dùng Quay lại yêu cầu đổi trả. Kiểm tra trang 2 và dòng tương ứng. Sau đó mở một yêu cầu từ chi tiết đơn hoặc sau khi tạo yêu cầu. Đường về dẫn tới danh sách đổi trả mặc định.
4. **Giỏ hàng và biến thể.** Thêm hai biến thể của cùng sản phẩm. Mở tên sản phẩm từ dòng A rồi chọn biến thể B ở trang chi tiết. Dùng Quay lại giỏ hàng để kiểm tra viền quanh dòng A. Làm lại và xóa dòng A ở tab khác trước khi quay lại, khi đó tiêu đề Giỏ hàng nhận focus. Nếu dùng Back của trình duyệt và gặp cảnh báo giỏ cũ, bấm Xem giỏ hàng hiện tại để tiếp tục.
5. **Quản trị.** Vào trang 2 của một danh sách catalog, mở Thêm rồi quay lại, sau đó mở Sửa và quay lại. Cả hai giữ trang. Đường về từ Sửa focus dòng đã mở. Trong Đơn hàng và Đánh giá, thử quay lại sau khi có bộ lọc để kiểm tra cả dòng hoặc thẻ.

## Mã nguồn

- Điều hướng và dữ liệu view nằm trong `NguonDieuHuong`, `DiaChiNguoiDung_Controller`, `DonHang_Controller` và `SauMua_Controller`.
- Viền focus và xử lý lịch sử nằm trong `components.css`, `admin.css` và `app.js`. Mốc focus đặt trên hàng bảng hoặc phần tử thẻ của các template đơn hàng, đổi trả, địa chỉ, đánh giá và giỏ.
- Đã cập nhật kiểm tra template và điều hướng trong `GioHangPagesTest`, `AdminDanhGiaPagesTest`, `AnhTaiLenAdviceTest` và `XacThuc_MariaDb_Test`.

## Kiểm chứng

- `mvn test` chạy 313 ca. Có 231 ca đạt và 82 ca được bỏ qua theo cấu hình mặc định. `mvn -DskipTests package` thành công. [Kết quả test](../qa/SCROLL-RESTORE/maven-test.txt) · [Kết quả đóng gói](../qa/SCROLL-RESTORE/maven-package.txt)
- Kiểm thử tích hợp với MariaDB và MongoDB độc lập đạt 27/27, gồm địa chỉ và quyền sở hữu, danh sách đơn quản trị, COD, đổi trả, đánh giá và MoMo chế độ mock. [Kết quả tích hợp](../qa/SCROLL-RESTORE/integration.txt)
- Chrome headless trên dữ liệu thử kiểm tra trang 2, nút Back, kích thước mobile, giỏ khách, đổi hoặc xóa biến thể, quay lại từ đơn mua và các nút Thêm/Sửa ở quản trị. [Bằng chứng Chrome](../qa/SCROLL-RESTORE/browser.json)
- `node --check` và `git diff --check` đạt. MoMo Sandbox cần môi trường và tài khoản kiểm thử riêng.
