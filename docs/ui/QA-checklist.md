# P12/U1 — Checklist kiểm chứng thủ công

Ngày 22/11/2026. **Chưa kiểm chứng bằng trình duyệt.** Browser runtime báo `No browser is available`, discovery trả `[]`. Không có screenshot, axe hoặc Lighthouse mới; không khẳng định AA hoặc không cuộn ngang dựa trên source.

## Chuẩn bị

- Dùng JDK 21; cấu hình DB demo riêng, tuyệt đối không dùng production hoặc database đang chạy chưa xác định.
- Chạy Flyway trên DB demo trống theo hướng dẫn deployment, sau đó `scripts/seed-demo.ps1 -Database beautyshop_demo_ui -User <user-demo>` với DB_PASSWORD qua môi trường.
- Chạy app dev trỏ chính DB vừa chuẩn bị. Không bật P01_MARIADB_TEST với database có dữ liệu cần giữ.
- Trước khi đổi từ asset cũ sang U1, build sạch output classes hoặc chạy từ JAR mới để tránh asset đã xóa còn sót từ build tăng dần.

## Ma trận viewport

Chụp `/login`, `/register`, `/products/ui-not-found` (Guest) ở từng viewport dưới đây. Tên ảnh kèm trang, vai trò, viewport và timestamp trong `docs/ui/screenshots/U1/`. Ghi base URL, thời gian và build/commit trong ghi chú ảnh.

- [ ] 360 × 800
- [ ] 390 × 844
- [ ] 768 × 1024
- [ ] 1366 × 768
- [ ] 1920 × 1080

Ở mỗi trang/viewport:

- [ ] document.scrollWidth không lớn hơn clientWidth; không cắt dấu tiếng Việt, tiền, nút hoặc focus ring.
- [ ] Zoom 200% không mất nội dung/chức năng; thử tên dài và thông báo lỗi dài.
- [ ] Nút mặc định cao ≥48 px, CTA auth ≥56 px; mọi nút ≥44 px; control ≥48 px; nhóm nút cách nhau ≥12 px.
- [ ] Nút primary auth rộng hết form trên mobile; password toggle và tìm kiếm không làm tràn ngang.
- [ ] Bấm Tab đầu tiên thấy skip link; Enter đưa tới main; Tab/Shift+Tab đi theo thứ tự đọc.
- [ ] Focus rõ trên nền kem, trắng và footer tối; summary menu vẫn truy cập được sau đóng menu và đổi viewport.
- [ ] Bật reduced-motion và kiểm animation không gây chuyển động ngoài ý muốn.

## Form và progressive enhancement

- [ ] Tắt JS: menu, tìm kiếm, đăng nhập/đăng ký, chọn SKU bằng nút Xem biến thể và form POST còn dùng được; nút Hiện mật khẩu ẩn.
- [ ] Bật JS: Hiện/Ẩn mật khẩu đổi nhãn và aria-pressed; không mất giá trị đang nhập.
- [ ] Đăng ký lỗi: tóm tắt có link đúng ô, field error có ID, aria-invalid/describedby đúng; email/họ tên giữ lại; mật khẩu không được echo.
- [ ] Đăng nhập sai, tài khoản hết hiệu lực, lỗi gộp giỏ và đăng ký thành công có thông báo đúng điều kiện.
- [ ] Gửi form: hiển thị Đang xử lý…, nút giữ kích thước, bấm lặp không gửi thêm, name/value của submitter vẫn vào request. Back/forward khôi phục trạng thái nút.
- [ ] Gắn data-confirm theo ví dụ design-system trong môi trường kiểm thử: focus vào Quay lại, Tab không thoát dialog, Escape hủy, Xác nhận gửi đúng submitter. F10 trên thao tác nghiệp vụ chờ U2–U4.
- [ ] Trình đọc màn hình đọc nhãn, lỗi, badge giỏ và thông báo hợp lý; chạy axe nếu công cụ sẵn có, ghi lỗi còn lại.

## Runtime và dữ liệu

- [ ] Giỏ Guest/Customer có số lượng khác nhau và sau hợp nhất vẫn hiển thị đúng số đơn vị, không nhân đôi.
- [ ] Admin/Customer/Guest thấy đúng liên kết theo quyền. Backend tiếp tục chặn truy cập trái quyền và POST thiếu CSRF.
- [ ] URL sản phẩm không tồn tại trả 404; `/products?page=abc` trả 400 với trang tiếng Việt. URL lạ ngoài allowlist có thể chuyển đăng nhập theo security hiện hành, không coi đó là lỗi 404 đã kiểm.
- [ ] Kiểm 403 bằng tài khoản Customer mở Admin; tái hiện 409/500 trong môi trường test được kiểm soát; response giữ status và không lộ exception, stacktrace hoặc server version.
- [ ] Upload >2 MB và request >3 MB bằng Admin, có CSRF hợp lệ: xác minh 413 và lỗi cạnh ô ảnh. Dữ liệu đã lưu không đổi; form thông báo nhập lại phần chưa được nhận. Kiểm cả tạo mới và sửa SKU, đường dẫn cũ hoặc SKU đã xóa.
- [ ] Footer Danh mục/Thương hiệu dẫn đến đúng control lọc. Chưa có trang chính sách/liên hệ: không báo các trang này đã hoàn thiện.
- [ ] Smoke các trang ngoài U1 với head, header/footer và control chung; nghiệm thu đầy đủ luồng COD/MoMo mock ở U2, tài khoản ở U3, Admin ở U4.

Các mục trên chưa đánh dấu vì cần browser và DB demo độc lập. Test MVC với dịch vụ mock chỉ kiểm chứng render/escaping/status trong phạm vi test, không thay thế các bước này.

## P12/U2.1 — Trang chủ và catalog

Đã kiểm Chrome ở 360×800, 390×844, 768×1024, 1366×768 và 1920×1080 với vai trò Guest và HTML Thymeleaf dùng dữ liệu kiểm thử. Kết quả gồm catalog rỗng, catalog có thẻ, chiều ngang, kích thước nút, tỷ lệ ảnh và thao tác bàn phím ở bộ lọc mobile. [Báo cáo và ảnh](../reviews/ui-upgrade-U2-2026-11.md)

- [ ] Trên endpoint dev đã xác nhận schema/dataset, kiểm GET `/` và `/products` của build U2.1. Xác nhận SKU hoạt động, ảnh có quyền, giá và tồn thực để mở F01.
- [ ] Trên catalog thật, thử tìm theo tên/SKU, giữ từ khóa khi áp dụng nhiều bộ lọc, bỏ từng chip, đổi sort và đi qua lại hai trang. Xác nhận số kết quả và trạng thái rỗng khớp truy vấn.
- [ ] Kiểm render với phiên Customer/Admin đã được cấp, điều hướng và badge giỏ theo quyền hiện hành.
- [ ] Kiểm zoom trình duyệt 200%, thứ tự focus qua cả trang, trình đọc màn hình, axe/Lighthouse và Core Web Vitals nếu có công cụ.
