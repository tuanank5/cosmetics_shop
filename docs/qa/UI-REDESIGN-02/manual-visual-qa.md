# UI-REDESIGN-02 · Đối chiếu trực quan thủ công

Browser tích hợp trả `No browser is available`. Hướng dẫn này dùng Chrome hoặc Edge mở bình thường trên máy có giao diện.

## Dựng ứng dụng thật

Chạy từ root dự án. Dùng hai cửa sổ PowerShell nếu muốn mở hai bản đồng thời. Mỗi lần chạy tạo MariaDB, MongoDB, upload và cổng HTTP riêng.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-02/build.ps1 -Phase package
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-02/run-fixture.ps1 -Phase manual -Revision before
powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-REDESIGN-02/run-fixture.ps1 -Phase manual -Revision after
```

Fixture hỏi mật khẩu tạm và in URL loopback cùng tên tài khoản thử. Đăng nhập bằng hai cửa sổ riêng để tránh trùng session. Chọn viewport CSS **1672 × 941**, zoom 100%. Mở ảnh tham chiếu bên cạnh, cùng tỷ lệ 100% khi có thể. Nếu cần ảnh sản phẩm rõ hơn, tải cùng ảnh thử có quyền sử dụng vào SKU của cả hai fixture qua Admin. Dữ liệu seed dùng ảnh PNG 1 pixel để kiểm luồng tải ảnh.

## Kịch bản tại 1672 × 941

| Ảnh | Trạng thái để chụp | Đối chiếu |
| --- | --- | --- |
| 01 Checkout | Thêm SKU có hàng, đăng nhập Customer, chọn địa chỉ đã lưu. Chụp lần có báo giá hợp lệ rồi thử đổi địa chỉ và mã. | Hero và tiến trình, tỷ lệ 60/40, ba card trái, ảnh và bảng tổng ở card phải, CTA, alert đúng trạng thái. Dùng DevTools mô phỏng mạng chậm và timeout. |
| 02 Chi tiết đơn | Đặt COD để có đơn chờ xác nhận. Mở chi tiết đơn. | Mã đơn, ngày đặt, badge và timeline nằm chung một dải. Snapshot địa chỉ và bảng dòng hàng ở trái, tổng và payment card ở phải. Thử nút sao chép và mở xác nhận hủy trên đơn thử. |
| 03 Chat | Mở `/chat`, hỏi “Chính sách đổi trả thế nào?” để có cặp câu hỏi và câu trả lời thật trong fixture FAQ. | Heading giữa, tỷ lệ 30/70, sidebar, avatar, hai màu bubble, sáu chip, composer. Chiều cao transcript và thanh cuộn khi câu trả lời dài. |
| 04 Wishlist và mega menu | Lưu các sản phẩm fixture, gồm một SKU, nhiều SKU và hết hàng. Mở Khám phá trước khi chụp. | Thanh header, cột đóng, ba nhóm, promo, hero dưới menu, row-card, giá/tồn, nhóm CTA và footer. Kiểm tra lần vào đầu menu đóng. |

Ghi theo từng mục **header / container / tỷ lệ cột / card / chữ / khoảng cách / nút / ảnh / màu**. Sau khi sửa một sai lệch, chụp lại cùng trạng thái. Lưu ảnh thật chọn lọc vào `docs/ui/screenshots/UI-REDESIGN-02/`. Dùng tối đa khoảng 10 ảnh. Tránh hiện thông tin cá nhân hoặc thông tin đăng nhập.

## Responsive và accessibility

- Chạy lại tại 320, 360, 390, 768, 1024, 1280, 1440 và 1920 CSS px. Dùng Zoom trình duyệt 200% trong cửa sổ riêng. Kiểm tra cuộn ngang, chữ dài, focus và khả năng thao tác khi mở bàn phím điện thoại.
- Dùng Tab, Shift+Tab, Enter, Escape. Kiểm tra thứ tự đóng Khám phá, trạng thái `aria-expanded`, focus quay lại nút Khám phá và không mất nhóm đăng xuất.
- Tắt JavaScript. Kiểm tra chọn địa chỉ/voucher bằng Áp dụng, radio COD, xác nhận hủy, yêu thích và mở chi tiết SKU. Mở Home bằng mobile, reduced motion và JavaScript tắt để kiểm tra native scroll.
- Trong Chat, dùng chip để điền câu hỏi, kiểm vùng transcript dài bằng bàn phím và đọc lỗi trả lời. Trong Wishlist, bỏ từng mục và xem focus, đếm và thông báo rỗng cập nhật.
- Ghi mỗi lần kiểm theo mẫu **URL | vai trò | viewport | zoom | thao tác | kỳ vọng | kết quả | ảnh**. Dùng PASS/FAIL cho thao tác đã thực hiện, BLOCKED khi có lỗi môi trường và NOT RUN cho mục chưa thao tác.
