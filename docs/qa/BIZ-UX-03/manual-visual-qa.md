# BIZ-UX-03 · Checklist nghiệm thu thủ công

## Mở ứng dụng QA

1. Trong PowerShell ở thư mục dự án, chạy lệnh dưới đây. Fixture dùng JAR BIZ-UX-03, MariaDB/MongoDB riêng và cổng loopback tự chọn. Nếu chưa có JAR, xem hướng dẫn build trong checkpoint. [Checkpoint](../../reviews/BIZ-UX-03-tiep-tuc.md)

   ```powershell
   powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/BIZ-UX-03/run-fixture.ps1 -Phase manual
   ```

2. Đặt mật khẩu tạm riêng cho fixture khi được hỏi. Mở URL `BeautyShop QA` hiện trong cửa sổ PowerShell bằng Chrome hoặc Edge bình thường. Giữ cửa sổ PowerShell mở trong lúc kiểm.
3. Dùng phiên riêng cho Guest, Customer `buyer@example.invalid` và Admin `admin@example.invalid`. Lưu ảnh bằng dữ liệu fixture. Nhấn Enter trong cửa sổ PowerShell để dừng ứng dụng và database của lượt này.

## Kiểm theo màn

| Màn | Thao tác và kết quả cần thấy |
|---|---|
| Home, header và footer | Tại desktop phù hợp, đi qua đủ sáu chương, cuộn tiếp xuống vùng sản phẩm và footer, rồi cuộn ngược lại. Nhập từ khóa trong ô tìm kiếm và mở menu bằng bàn phím. Gõ hoặc chọn trong biểu mẫu không tự đổi chương. Tại màn hình chạm, cửa sổ thấp khoảng 540 px, khi bật reduced motion và khi tắt JS, phải đọc được các chương bằng cuộn tự nhiên. Kiểm hai nút tư vấn và menu/footer theo vai. |
| Catalog và chi tiết | Lọc Serum, thử từ khóa không khớp, mở `/products/fixture-serum`. Kiểm tên dài, placeholder ảnh và SKU 30 ml/50 ml. Chuyển SKU bằng Tab rồi Enter. SKU 50 ml hết hàng phải có thông báo, không cho thêm giỏ. |
| Giỏ | Thêm serum 30 ml và kem 25 ml. Đổi nhanh số lượng hai SKU, thử DevTools Network chậm, rồi so tạm tính và badge với giỏ tải lại. Với gián đoạn mạng, xem Network để xác nhận không có POST tự gửi lại và dùng nút đọc lại giỏ. Mở hai tab giỏ, kiểm cảnh báo khi dữ liệu có thể đã đổi. Nhập 0, nhấn Escape và kiểm số lượng cùng focus được phục hồi. Lượt mất phản hồi **sau ghi** đánh dấu NOT RUN nếu không chủ động xác định được thời điểm commit. |
| Checkout và COD | Với 1 serum 30 ml, vào thanh toán từ Guest, đăng nhập và kiểm giỏ gộp. Thêm địa chỉ tổng hợp. Thử mã sai để kiểm vẫn giữ giá trị đã nhập, rồi dùng `FIXTURE10`. Tổng đúng là 165.000 ₫ cho giỏ chỉ có một serum 30 ml. Đặt COD, kiểm trang chi tiết. Thử thao tác nút hai lần và xem lịch sử chỉ có đơn vừa tạo. |
| Login, đăng ký, OTP và reset | Kiểm tên trường, đường quên mật khẩu, focus và lỗi dữ liệu thiếu. Fixture mặc định tắt Redis/SMTP, nên đánh dấu BLOCKED cho gửi mã, xác minh OTP và email đặt lại mật khẩu đầu cuối. |
| Tư vấn | Truy cập `/chat` và `/tu-van-da` từ Home, thử form và lỗi dịch vụ. Khi Ollama chưa cấu hình, ghi BLOCKED cho ca có gợi ý và đi tiếp tới đúng SKU. |
| Wishlist, đơn, đánh giá và đổi trả | Lưu rồi bỏ lưu sản phẩm, kiểm trạng thái rỗng. Kiểm lịch sử và chi tiết đơn. Sau khi Admin chuyển một đơn COD mới tới đã giao, mở các thao tác đánh giá và đổi trả của chính đơn đó. |
| Admin Dashboard và đơn | Đăng nhập Admin, chuyển kỳ thống kê bằng Tab và Enter, xem viền focus của kỳ đang chọn, bảng và biểu đồ. Mở đơn COD ở danh sách, xác nhận rồi kiểm trạng thái kế tiếp. Mở lại Admin bằng phiên Customer phải bị từ chối. |
| Admin CRUD | Mở từng nhóm sản phẩm/SKU, danh mục, thương hiệu, voucher, tài khoản, đánh giá, đổi trả và AI. Kiểm hành động chính, bảng/list, cuộn ngang trong vùng bảng nếu cần. Trên form SKU, thử gửi giá hoặc số lượng sai và kiểm lỗi, trạng thái field, giá trị đã nhập. Thay đổi dữ liệu chỉ trong fixture. |
| Lỗi, rỗng và khôi phục | Mở URL sản phẩm không tồn tại. Kiểm màn 404, giỏ rỗng, catalog không có kết quả và trạng thái provider không sẵn sàng. Kiểm link quay lại và thông báo đủ rõ để tiếp tục. |

## Viewport và bằng chứng

- Chạy checklist ở 390 và 1440 CSS px trước. Sau đó mở cùng các màn ở **320, 360, 768, 1024, 1280 và 1920 CSS px** bằng Device Toolbar. Ghi cả chiều cao, chế độ touch/desktop và màn có tràn ngang, chữ bị cắt, mục tiêu bấm quá sát hoặc CTA khuất.
- Tắt Device Toolbar để kiểm **zoom trang thật 200%** bằng menu trình duyệt. Chạy Tab/Shift+Tab, Enter và Escape. Bật reduced motion trong DevTools Rendering để kiểm riêng. Nếu dùng DevTools để tắt JS, tải lại trước khi kiểm fallback.
- Chụp ảnh thật bằng DevTools Capture Screenshot hoặc công cụ hệ thống. Lưu ảnh chọn lọc tại `docs/ui/screenshots/BIZ-UX-03/`, theo mẫu `390x844-cart-timeout.png`. Khi so sánh, giữ cùng dữ liệu fixture, trạng thái, viewport, zoom và chế độ input. Che dữ liệu nhạy cảm trước khi đưa ảnh hay log vào repo.
- Điền kết quả cho mỗi màn bằng **PASS**, **FAIL**, **BLOCKED** hoặc **NOT RUN** và ghi đường dẫn ảnh nếu có. [Mẫu kết quả](mau-ket-qua.md)
