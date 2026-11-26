# Ổn định baseline xác thực trước SEC-01

Mã nguồn bắt đầu từ `main` tại `72a2805`. Thay đổi xác thực nằm trên working tree. 7 Phase, 13 Work Package và thứ tự triển khai được ghi tại [Execution Order](implementation-prompts/00-EXECUTION_ORDER.md).

## Phạm vi thay đổi

- Tạo OAuth client theo từng cặp Client ID và Client Secret có giá trị. Trang đăng nhập chỉ hiện nhà cung cấp đã bật. Cấu hình mặc định chạy bằng Form Login. Redis và SMTP có địa chỉ mặc định, thông tin thật cần khi thực hiện OTP.
- OAuth chỉ nhận Customer đang hoạt động. Bộ lọc phiên từ chối phiên OAuth có vai trò khác Customer. Giữ chung hướng xử lý sau đăng nhập với Form Login. Luồng hiện tại lấy email đã xác minh từ provider. Liên kết danh tính bằng ID lâu dài và kiểm thử nhà cung cấp thật thuộc phần hoàn thiện OAuth sau baseline này.
- Giữ luồng đăng ký OTP rồi đăng nhập để tạo phiên. Sửa fixture kiểm thử theo hai bước này và khôi phục hiển thị lỗi ở trường đăng ký. Khôi phục quyền Guest trên trang tư vấn theo hợp đồng trước tích hợp xác thực.
- Thay đổi tập trung tại `OAuth2Client_Config`, `Security_Config`, `OAuth2NguoiDung_Service`, `TrangThaiTaiKhoan_Filter`, form login/register và cấu hình chạy. Schema hiện hành dùng migration V1–V5.

## Các điểm nghiệm thu khi triển khai

- **SEC-01** giữ profile chạy với nhà cung cấp tắt, kiểm quyền Admin, CSRF OTP và quy tắc 403/404 theo vai trò và chủ tài nguyên.
- **CART-01** tiếp tục dùng `LoginSuccess_Handler` cho hai đường đăng nhập. Ca gộp 5 + 7 với tồn 10 thuộc gói này. Xác minh OTP chưa tạo phiên và giỏ chỉ được gộp khi đăng nhập thành công.
- **QA-01** lặp lại ca thiếu/đủ thông tin từng provider, vai trò Admin, Form Login, đăng ký OTP, logout, session và merge. Kết quả dùng SMTP/Redis hoặc Google/GitHub thật được ghi riêng.

Khi đổi nhánh, chạy Maven clean trước khi đóng gói. Lần kiểm chứng ban đầu phát hiện `target/classes` còn V6 sinh từ nhánh khác. Dùng kết quả sau clean làm bằng chứng baseline.

## Kết quả tại máy local

JDK 21.0.13.1 và Maven 3.9.15. Build từ các thư mục biên dịch mới để mô phỏng checkout sạch của CI.

| Lệnh | Kết quả |
|---|---|
| `mvn -o -B -ntp test` | 274 tổng, 226 đạt, 48 bỏ qua theo môi trường, 0 lỗi. |
| `mvn -o -B -ntp -DskipTests package` | BUILD SUCCESS, JAR Spring Boot được đóng gói. |
| `P01_MARIADB_TEST=true`, `mvn -o -B -ntp -Dtest=XacThuc_MariaDb_Test,DonHangCOD_MariaDb_Test test` | 12/12 đạt trên MariaDB cô lập, Flyway áp dụng V1–V5. OTP dùng mock ở lớp gửi và xác minh, còn Form Login, CSRF, session, gộp giỏ và COD chạy qua ứng dụng với MariaDB. |
| JAR profile dev trên localhost, bỏ các biến thông tin OAuth/SMTP | Ứng dụng khởi động, GET `/login` trả HTTP 200, có form mật khẩu và không hiện nút OAuth. |

Nhật ký cục bộ nằm trong `docs/qa/AUTH-BASELINE/`. GitHub Actions chạy lại khi thay đổi được đưa lên GitHub.
