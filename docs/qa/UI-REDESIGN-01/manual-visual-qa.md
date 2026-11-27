# UI-REDESIGN-01 · Kiểm trực quan trên trình duyệt thật

Browser tích hợp của phiên này không có phiên khả dụng. Các bước dưới đây dành cho Chrome hoặc Edge khởi động bình thường trên máy có giao diện. Dùng fixture riêng và dữ liệu đã được seed để so sánh hai bản.

## Mở fixture

Từ root, dựng bản mới bằng `docs/qa/UI-REDESIGN-01/build.ps1`. Mở PowerShell thứ nhất và chạy bản trước. Mở PowerShell thứ hai và chạy bản sau. Mỗi cửa sổ in một URL loopback.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-01/run-fixture.ps1 -Phase manual -Revision before
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-01/run-fixture.ps1 -Phase manual -Revision after
```

Dùng cùng trình duyệt, viewport, mức zoom, vai trò và lựa chọn dữ liệu cho từng cặp ảnh. Mở URL fixture trong cửa sổ riêng, chọn sản phẩm “Fixture Serum”, biến thể “30 ml”, thêm một món, đăng nhập Customer mà script in và chọn địa chỉ thứ hai nếu muốn so sánh. Mã voucher fixture hợp lệ là `FIXTURE10`. Kiểm trường lỗi bằng `FIXTUREEXPIRED` và `FIXTUREMIN`. Ảnh PNG 1 px trong seed dùng để kiểm đường ảnh; để chụp giao diện minh họa ảnh, tải cùng một ảnh thử có quyền sử dụng vào SKU qua Admin của cả hai fixture.

## Kịch bản

| Màn hình | Tương tác cần kiểm | Trạng thái ban đầu |
| --- | --- | --- |
| Checkout | Tổng ban đầu, đổi địa chỉ, nhập và xóa voucher, voucher hết hạn, lỗi field và nút đặt đơn. Giả lập mạng chậm khi đổi mã liên tiếp, kiểm phản hồi cũ bị bỏ qua. Ngắt mạng để kiểm trạng thái và nút Thử lại báo giá. | NOT RUN |
| Checkout tắt JS | Lần mở có địa chỉ mặc định, đổi mã rồi dùng Xem báo giá. Xác nhận nơi giao/mã ở tóm tắt và thử gửi COD. | NOT RUN |
| Đơn hàng | Chốt một đơn COD từ cùng dữ liệu. Kiểm tiến trình, thanh toán, snapshot từng dòng, tổng, nút có xác nhận. Dùng đơn riêng để thử hủy, Admin để chuyển đơn sang đã giao. | NOT RUN |
| Chat | Mở CTA và link cũ. Dùng gợi ý, Tab vào ô nhập, Enter trong ô xuống dòng, Tab/Enter gửi. Đọc trạng thái đang xử lý, trả lời FAQ khi fixture tắt AI và thông báo quá giới hạn. | NOT RUN |
| Header và Khám phá | Kiểm thứ tự Guest, Customer, Admin, tìm kiếm và POST đăng xuất. Mở Khám phá, đóng bằng nút ở trái. Mở lại, Tab qua nhóm, Escape quay về summary, kiểm focus và trạng thái đóng. | NOT RUN |
| Wishlist | Lưu cả hai sản phẩm fixture rồi xem ảnh, giá, 2 biến thể, còn/hết hàng. Bỏ từng mục bằng phím, kiểm focus và trạng thái rỗng. Dùng một tài khoản Customer QA khác để đối chiếu owner. | NOT RUN |
| Home | Với desktop cao, rê chuột qua chương, thao tác tìm kiếm, đi tới chương 6 rồi cuộn xuống footer. Chạy lại với thiết bị touch, viewport thấp, reduced motion và tắt JavaScript. | NOT RUN |

## Responsive, bàn phím và ảnh

- Chạy tám độ rộng 320, 360, 390, 768, 1024, 1280, 1440 và 1920 CSS px. Xác nhận độ rộng CSS đang dùng trong DevTools. Trong cửa sổ desktop riêng, đặt menu Zoom của browser lên 200% và thử lại.
- Dùng Tab, Shift+Tab, Enter và Escape. Kiểm thứ tự trực quan so với focus, tên control khi accessibility tree mở, label và lỗi trường, bị cắt chữ hoặc tràn ngang khi dữ liệu dài. Thử ảnh bị mất bằng chặn request ảnh trong DevTools.
- Với ảnh trước/sau, ưu tiên 390 và 1440 px, cùng trạng thái và dữ liệu. Lưu ảnh thật vào `docs/ui/screenshots/UI-REDESIGN-01/before/` và `docs/ui/screenshots/UI-REDESIGN-01/after/`, tối đa khoảng 10 ảnh chọn lọc tổng cộng. Tránh lưu thông tin đăng nhập hoặc dữ liệu cá nhân.
- Nếu phát hiện vấn đề, ghi **Route / vai trò / viewport / zoom / thao tác / mong đợi / thực tế / PASS hoặc FAIL / đường dẫn ảnh**. Ghi BLOCKED nếu môi trường cản trở, NOT RUN nếu chưa thao tác. Nhấn Enter ở terminal giữ fixture để dừng đúng các tiến trình QA.
