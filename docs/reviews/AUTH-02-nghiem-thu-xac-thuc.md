# Xác thực đa phương thức BeautyShop

## Kết quả kiểm thử tự động

| Lệnh / môi trường | Kết quả |
|---|---|
| `mvn -o -B -ntp verify`, JDK Temurin 21.0.13.1 | 284 ca, 226 đạt và 58 bỏ qua theo cấu hình. Không có lỗi. [Log](../qa/AUTH-02/mvn-verify.log) |
| MariaDB 12.2 cô lập, Flyway V1–V7 | 26/26 ca đạt. Kiểm DB mới, V5/V6 có dữ liệu, OAuth callback mô phỏng, khóa tài khoản, đặt lại mật khẩu đồng thời, session, gộp giỏ, checkout, COD và MoMo mock. [Script](../qa/AUTH-02/run-mariadb.ps1) · [Log](../qa/AUTH-02/mvn-mariadb.log) |
| `mvn -o -B -ntp -DskipTests package` | BUILD SUCCESS, tạo JAR Spring Boot. [Log](../qa/AUTH-02/mvn-package.log) |

Google được kiểm bằng máy chủ mô phỏng phát ID token ký RSA. GitHub được kiểm bước đổi code và state qua HTTP mô phỏng, dữ liệu người dùng đã xác minh dùng fixture. Ca thiết lập mật khẩu lần đầu, reset qua email và lỗi gửi thư dùng mock cho SMTP hoặc OTP/Redis. Chưa xác minh với callback Google/GitHub thật, Redis thật hoặc SMTP thật. Bộ tích hợp AUTH-02 chạy riêng trên MariaDB và chủ động trỏ MongoDB tới địa chỉ không có dịch vụ.

## Luồng có thể thử

| Ca | Thao tác | Kết quả mong đợi |
|---|---|---|
| Đăng ký | `/register` → nhập mã email | Mã sai/hết hạn báo lỗi. Mã hợp lệ tạo Customer, xác minh email chính và chuyển về `/login?registered`. |
| Đăng nhập mật khẩu | Mở checkout, đăng nhập Customer | Về lại checkout. Giỏ guest gộp theo ID nội bộ. Admin thử riêng bằng mật khẩu. |
| OAuth mới | `/login` → Google hoặc GitHub với email mới | Tạo Customer có một danh tính đã gắn, mật khẩu ban đầu chưa có. Đăng nhập lần sau giữ ID. |
| OAuth trùng email | Thử provider có email của Customer đã đăng ký, chưa liên kết | Đăng nhập OAuth dừng. Vào BeautyShop bằng phương thức hiện có rồi liên kết trong Bảo mật tài khoản. |
| Liên kết và gỡ | `/account/security` → xác thực lại bằng mật khẩu hoặc provider đã gắn kèm mã email → bắt đầu liên kết → xác nhận | Chỉ ghi liên kết sau xác nhận. Tài khoản yêu cầu đăng nhập lại sau khi thay đổi. Gỡ liên kết yêu cầu xác thực qua phương thức vẫn còn dùng được. |
| Thiết lập mật khẩu | Đăng nhập vào Customer OAuth chưa có mật khẩu → Bảo mật → xác thực lại bằng provider đã gắn → nhận và nhập mã email → đặt mật khẩu | Lưu hash trên cùng ID. Phiên cũ ngừng hợp lệ. |
| Đổi mật khẩu | Bảo mật → nhập mật khẩu hiện tại, mật khẩu mới và xác nhận | Mật khẩu cũ hết hiệu lực, phiên cũ yêu cầu đăng nhập lại. |
| Quên mật khẩu | `/forgot-password` → nhập email của Customer đã xác minh và đã có mật khẩu → mở thư → nhập mật khẩu mới | Phản hồi xin link giống nhau cho mọi email. Link hết hạn sau 15 phút. Chỉ POST hợp lệ tiêu thụ link và đổi mật khẩu. |
| Admin hỗ trợ | `/admin/users` → Chi tiết Customer → Gửi yêu cầu đặt lại mật khẩu → chọn lý do | Khách nhận link qua email. Admin thấy kết quả gửi. Sau khi khách tự hoàn tất, nhật ký ghi sự kiện hoàn tất riêng với yêu cầu và gửi thư. |

## Thiết lập kiểm thử thủ công

- Dùng MariaDB test riêng có tài khoản kết nối được cấp quyền trên schema test. Khi chạy phiên bản này, Flyway áp dụng tới V7. Trỏ MongoDB vào DB test riêng. Dùng Redis test, SMTP test hoặc hộp thư thử nghiệm. Với SMTP local có thể xem OTP và link ngay trong hộp thư Mailpit.
- Nếu chạy `localhost:8080`, đặt `AUTH_PUBLIC_URL=http://localhost:8080`. Cấu hình callback tương ứng ở OAuth app `http://localhost:8080/login/oauth2/code/google` và `http://localhost:8080/login/oauth2/code/github`. Google ở chế độ Testing cần thêm đúng tài khoản thử nghiệm trong cấu hình của app.
- Điền Client ID và Client Secret của từng nhà cung cấp trong biến môi trường, cùng `REDIS_HOST`, `REDIS_PORT` và các biến `MAIL_*` khi thử OTP/cấp lại mật khẩu. Giữ secret trong phiên chạy hoặc secret store.
- Bắt đầu bằng một Customer email/mật khẩu mới có OTP và một OAuth Customer mới. Dùng tài khoản Admin test sẵn có để thử yêu cầu hỗ trợ. Với tài khoản cũ có mật khẩu nhưng chưa có mốc xác minh email, vào **Bảo mật**, xác thực lại bằng mật khẩu rồi xác minh hộp thư trước khi thử khôi phục.

## Dữ liệu cũ và vận hành

V6 trong source được phục hồi nguyên văn từ nhánh feature. V7 giữ ID, hash và cột cũ, chỉ chuyển cặp danh tính V6 thực có bằng chứng sang bảng mới nếu chủ sở hữu là Customer. Trước khi dùng một database có dữ liệu cần giữ, sao lưu và diễn tập restore trên bản sao, đối chiếu checksum cùng số lượng tài khoản, danh tính và giỏ. JAR mới chạy Flyway trước khi nhận yêu cầu. DDL MariaDB có thể commit theo từng bước. Nếu migration lỗi giữa chừng, xem schema và lịch sử trên bản sao để quyết định phục hồi hoặc sửa tiến tới.

Tài khoản cũ chỉ từng đăng nhập theo email OAuth, chưa có provider ID và không biết mật khẩu nội bộ, cần xử lý hỗ trợ theo quy trình xác minh quyền sở hữu độc lập trước cutover thật. Luồng công khai chỉ nhận ID provider đã liên kết hoặc email mới. Giữ tài khoản cũ và dữ liệu, rà soát bằng chứng thủ công theo quy trình đã duyệt trước khi cấp đường truy cập. Bản này chưa cung cấp màn hình chuyển tiếp riêng cho nhóm đó.

Tách quyền ghi và đọc nhật ký khi đưa lên môi trường dùng thật. Review nội dung nhật ký, hạn lưu đề xuất 90 ngày và quy trình dọn token hết hạn với tài khoản vận hành. Máy chủ proxy/access log nên bỏ query của `/reset-password`, `/login/oauth2/code/*` và `/oauth2/authorization/*`. OTP dùng khóa HMAC của tiến trình nên restart làm mã đang chờ hết hiệu lực. Cấu hình nhiều instance cần chia sẻ khóa và phiên trước khi đưa vào sử dụng.
