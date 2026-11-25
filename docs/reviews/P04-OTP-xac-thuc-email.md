# P04-OTP — Xác thực email khi đăng ký

Tích hợp phần OTP của `734d094` trên nhánh `feat/email-otp-registration` tạo từ `main` tại `0b638db`. Áp dụng PRG cho các bước đăng ký thành công. Ba thao tác xác thực, gửi lại và hủy dùng POST có CSRF. MariaDB chỉ nhận tài khoản CUSTOMER sau khi xác thực email. Ràng buộc email duy nhất và mật khẩu BCrypt tiếp tục áp dụng.

OTP gắn với lần đăng ký trong HTTP session. Redis giữ mã băm trong 5 phút, thực hiện xác thực một lần và tăng số lần sai trong một thao tác nguyên tử. Giới hạn gửi áp dụng theo email đã chuẩn hóa trong 60 giây, kể cả khi đổi phiên hoặc hủy. Một phiên đăng ký kéo dài tối đa 15 phút. Nếu đã xác thực mà ghi tài khoản thất bại, người dùng có thể thử hoàn tất trong thời hạn của phiên. Quyết định nghiệp vụ nằm ở D33 và BR21.

## File thay đổi

- `TaiKhoan_Controller.java`, `NguoiDung_Service.java`, `XacThucEmail_Service.java`, `Security_Config.java`, `register.html` và script Redis `xac-thuc-otp.lua`.
- `pom.xml`, `.env.example`, `docker-compose.yml` và các profile Spring cho Redis và SMTP.
- Test MVC cho đăng ký, test xác thực và COD trên MariaDB, tài liệu cấu hình, SRS, Business Rules, ERD, Decisions và roadmap.

## Kiểm chứng

| Lệnh hoặc phép thử | Kết quả |
| --- | --- |
| `mvn -B -ntp test` | 225 ca đạt, 48 ca tích hợp bỏ qua theo cấu hình mặc định, 0 lỗi. [Log](../qa/P04-OTP/mvn-test.log) |
| `mvn -B -ntp -DskipTests package` | Đóng gói JAR thành công. [Log](../qa/P04-OTP/mvn-package.log) |
| `mvn -B -ntp -Dtest=XacThuc_MariaDb_Test,DonHangCOD_MariaDb_Test test` | 12/12 đạt trên MariaDB 12.2.2 trống, Flyway áp dụng V1–V5. Hai bộ test mô phỏng lớp OTP để kiểm tra hợp đồng MVC, xác thực, quyền và COD với MariaDB thật. [Log](../qa/P04-OTP/mvn-mariadb.log) |
| QA HTTP bằng JAR profile `dev` | Đạt khi dùng MariaDB thật, Redis portable 7.4.9 và Mailpit 1.34.0. Xác nhận chưa đăng nhập được trước OTP, CSRF, giới hạn gửi theo email, cô lập hai phiên, email duy nhất, dùng mã một lần, hết hạn, gửi lại, khóa sau 5 lần sai và hủy. QA rút ngắn TTL trên Redis riêng để kiểm tra hiệu ứng hết hạn. [Log](../qa/P04-OTP/otp-http.log) · [Redis dùng trong QA](https://github.com/redis-windows/redis-windows/releases/tag/7.4.9) · [Mailpit dùng trong QA](https://github.com/axllent/mailpit/releases/tag/v1.34.0) |

Hai script tái chạy nằm tại [docs/qa/P04-OTP](../qa/P04-OTP/). Script Windows dùng dịch vụ local chạy tạm và database riêng. Mailpit nhận email kiểm thử trên localhost, không chuyển tiếp ra ngoài. Chưa xác minh SMTP, Redis và TLS trên hạ tầng production.
