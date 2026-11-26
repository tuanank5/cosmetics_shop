# UI-01C · Ô mật khẩu, disclosure và thông báo

Checkpoint `main`, HEAD `6368b6723d425c23d8901a5ebc69bbaea3ac09b4` cộng working diff G1–G5. NAV-01 đã đạt G4 trên cùng cây làm việc. Chỉ triển khai UI-01C. Không stage, commit hoặc push.

## Thay đổi

- `fragments/ui.html`, `components.css`, `app.js` và các form `auth/{login,register,security,reset-password}.html` dùng chung ô mật khẩu có cột điều khiển 48px. SVG giữ cùng kích thước, nhãn đọc màn hình ổn định và trạng thái `aria-pressed` phản ánh hiện/ẩn. Các trường giữ tên, id, autocomplete và ràng buộc. Form đăng ký liên kết lỗi BCrypt vào mô tả trường và không render lại mật khẩu lỗi. `storefront.css` dành chỗ cho hàng điều hướng desktop trên trang đăng nhập/đăng ký để khi app.js mở menu, biểu mẫu không dịch xuống.
- `product/{list,detail}.html`, `fragments/{header,footer,product-card}.html`, `home/index.html`, `CuaHang_Controller.java` và `NguonDieuHuong.java` thêm trạng thái hiển thị bộ lọc qua GET. PDP gắn ID từng disclosure theo slug và mục, lưu lựa chọn trong entry history. Menu native giữ trạng thái tạm thời, Escape đưa focus về summary.
- `admin/dashboard.html`, `dashboard.js` và `admin.css` giữ bảng mở mặc định, tạo vùng cuộn có tên và có thể lấy focus. Mỗi bảng có thể giữ lựa chọn đóng trong tab khi đổi kỳ; biểu đồ Chart.js lấy dữ liệu từ cùng bảng. CSS giữ marker riêng của summary và phong cách quản trị hiện có.
- `fragments/admin-nav.html`, `fragments/header.html`, `admin/user/{list,detail}.html` thống nhất nhãn **Quản lý tài khoản**. Query danh sách và count theo tên hoặc email của cả hai vai trò. Trang chi tiết giữ Back theo q/page và dùng dialog xác nhận gửi yêu cầu đặt lại mật khẩu có sẵn. Khi tắt JS, summary và nút xác nhận trực tiếp vẫn hoạt động.
- `order/{detail,checkout}.html`, `return/detail.html`, `auth/address-form.html`, `cart/index.html` và `admin/user/reviews.html` chuẩn hóa vùng thông báo, badge trạng thái hoặc mô tả lỗi trường. Bảng kiểm kê ghi các màn giữ component hiện hữu và lý do. [Kiểm kê giao diện](../qa/UI-01C/screen-inventory.md)
- `BootstrapPagesTest`, `TrangChuPagesTest` và `GiaoDienPagesTest` kiểm đường dẫn khám phá và allowlist trạng thái bộ lọc. Thêm fixture PowerShell/Chrome, cập nhật sổ triển khai và roadmap.

## Hợp đồng cho các gói sau

- UI-01P giữ `filterPanel=open|closed` cùng q, bộ lọc, sort, page, size qua link catalog. `data-catalog-filter-pages` xác định nhóm link nhận trạng thái hiện tại khi người dùng mở/đóng. Giá trị khác trở về mặc định SSR theo bộ lọc/sort đang áp dụng.
- WISH-01 và QA-01 giữ `filterPanel` trong nguồn catalog và giữ selector `.catalog-results .store-card`. Nguồn từ home, vùng và anchor theo G4 tiếp tục dùng định dạng cũ. `data-entry-disclosure` và ID theo slug/mục dành cho trạng thái Back của PDP.
- CART-01 và REVIEW-01 có vùng `fragments/ui :: notices(message, error)` với selector `[data-page-notices]`. Component render thông báo hiện có và giữ role status/alert cho nội dung; phần đọc/tiêu thụ flash, nội dung và điều kiện hiển thị thuộc caller.

## Kiểm thử

- Temurin 21.0.13.1, Maven 3.9.15. `mvn -o test` đạt **234/293**, 59 skip theo cấu hình mặc định, 0 lỗi. `mvn -o -DskipTests package` thành công. Maven cần quyền truy cập cache cục bộ khi biên dịch lại trong máy này.
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/UI-01C/run-fixture.ps1 -Integration` đạt **34/34** trên schema MariaDB trống độc lập. Suite gồm auth bằng mật khẩu, auth đa phương thức, Admin catalog/dashboard, COD, voucher, HTTP error và saved request. Redis, email và các callback ở suite auth dùng fixture.
- Trình duyệt dùng Chrome headless, ứng dụng profile dev trên loopback với Flyway V1–V7 và MariaDB riêng. Dữ liệu tài khoản và catalog do script tạo trong fixture. MongoDB, Redis và SMTP thật để tắt.
- `run-fixture.ps1 -Baseline` đo JAR trước sửa. Không có ô nút khi JS chưa chạy. Ở 390px, hiện nút làm form cao thêm 62,8 CSS px. Ở 1366px, nút làm ô nhập thu từ 514,38 xuống 333,92 CSS px; đổi nhãn làm ô nhập rộng lại thành 348,27 CSS px. Hàng menu desktop cũng dịch form xuống 52 CSS px. [Số đo trước sửa](../qa/UI-01C/browser-before.json)
- `run-fixture.ps1` kiểm JAR sau sửa. Cả **12/12** tổ hợp login/register giữ nguyên rect của input, nút và form theo tọa độ tài liệu trước init, sau init, khi hiện và khi ẩn mật khẩu. Chrome 158 kiểm ở 320, 390, 768, 1366 và 1920 CSS px, cùng viewport 683 CSS px ở DPR 2 tương đương bố cục zoom 200% của cửa sổ 1366px. Bàn phím giữ focus của nút và vùng ký tự đang chọn trong input. [Số đo và kết quả sau sửa](../qa/UI-01C/browser-after.json) · [Ô mật khẩu 390px](../ui/screenshots/UI-01C/login-390-password-focus.webp)
- Chrome đạt 8 kiểm tra luồng. Bộ lọc giữ trạng thái đóng khi chuyển trang, form sort chạy khi tắt JS, các disclosure PDP mở độc lập và được phục hồi khi Back. Menu mobile mở bằng bàn phím, đóng cùng nhóm con và đưa focus về summary khi Escape. Khi tắt JS, đăng nhập sai và đúng trả về đích tương ứng, đăng ký vượt giới hạn byte báo lỗi và để trống mật khẩu, bảng số đọc được bằng Tab và cuộn ngang bằng phím. Với JS, có canvas được Chart.js dựng từ dữ liệu fixture và đóng một bảng giữ lựa chọn qua đổi kỳ mà bảng khác vẫn mở. Xác nhận đặt lại mật khẩu Admin kiểm lý do bắt buộc, token CSRF, Escape, trả focus, Back và nhãn hiện hành ở hai kích thước. [Bảng dashboard 390px](../ui/screenshots/UI-01C/dashboard-390-table-nojs.webp)
- `git diff --check` đạt. Diff đã staged trống. Giữ checkpoint HEAD cộng working diff và bằng chứng các gói trước.

**UI-01C DONE, G5 đạt trên fixture.** Browser dùng zoom mô phỏng qua viewport/DPR. Chưa xác minh giao diện trên tài khoản thật hoặc việc gửi OTP/email và callback OAuth thật. Suite tích hợp kiểm nghiệp vụ xác thực với dịch vụ giả lập. UI-01P chờ lệnh riêng.

## Quay lui

Có thể quay lui riêng các fragment, CSS và JS UI-01C, rồi chạy lại G4/G5. Khi quay lui giữ nguyên dữ liệu, các thay đổi G1–G4 và điều hướng của NAV-01.
