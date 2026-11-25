# P04-OAuth2 — Đăng nhập Google và GitHub

Tích hợp chức năng từ `a7b5e51` của `dat` vào `feat/email-otp-registration`. Hai nút đăng nhập xuất hiện theo profile đã bật. Google dùng OIDC với `sub` và email đã xác minh. GitHub dùng OAuth2 với `id` và email chính đã xác minh. Các phiên CUSTOMER dùng chung phân quyền, kiểm tra khóa tài khoản và xử lý giỏ Guest của BeautyShop. [Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html) · [Google OIDC](https://developers.google.com/identity/openid-connect/openid-connect) · [GitHub REST](https://docs.github.com/en/rest/users/emails#list-email-addresses-for-the-authenticated-user).

V6 bổ sung cặp định danh nhà cung cấp duy nhất. Email trùng yêu cầu dùng phương thức đăng nhập của tài khoản hiện có. Tài khoản ADMIN dùng Form Login. D34 ghi quyết định và cách kết hợp với OTP đăng ký bằng mật khẩu.

## File thay đổi

- Cấu hình Spring Security tại `Security_Config.java`, ánh xạ danh tính tại `OAuth2NguoiDung_Service.java` và principal dùng chung qua `PhienNguoiDung.java`, `PhienOAuth2.java`.
- `NguoiDung.java`, `NguoiDung_Repository.java` và migration `V6__them_danh_tinh_oauth.sql`.
- `Bootstrap_Controller.java`, `auth/login.html`, `storefront.css`, `pom.xml`, hai file profile OAuth2, `.env.example` và [hướng dẫn cấu hình](../../README.md#chạy-local).
- `OAuth2_MariaDb_Test.java`, script QA và tài liệu D34, SRS, Business Rules, ERD, bảng đối chiếu tên và roadmap.

## Kiểm chứng

- `mvn -B -ntp test` và `mvn -B -ntp -DskipTests package` đạt. [Log test](../qa/P04-OAUTH2/mvn-test.log) · [Log đóng gói](../qa/P04-OAUTH2/mvn-package.log).
- Cả 16 test tích hợp xác thực, COD và OAuth2 đạt trên MariaDB 12.2.2 trống. Flyway áp dụng V1–V6. Nhà cung cấp mô phỏng chạy localhost, trả token, UserInfo và danh sách email qua HTTP. Test kiểm tra callback Google OIDC với JWT ký RSA và JWKS, callback GitHub OAuth2, email chưa xác minh, email trùng, state sai, vai trò và khóa phiên. [Log MariaDB](../qa/P04-OAUTH2/mvn-mariadb.log).
- Luồng OTP qua HTTP vẫn đạt với MariaDB, Redis và Mailpit local. [Log OTP](../qa/P04-OAUTH2/otp-http.log).

Chưa chạy đăng nhập với Google/GitHub thật vì chưa có Client ID, Client Secret và callback đã đăng ký cho môi trường này.
