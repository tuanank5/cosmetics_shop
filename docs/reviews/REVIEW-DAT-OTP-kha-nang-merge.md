# Review commit xác thực email bằng OTP

**Kết luận:** Chưa sẵn sàng merge vào `main`. Đã đối chiếu `origin/dat` tại `734d094` với `main` tại `0b638db` sau khi fetch. Nhánh `dat` còn chứa commit OAuth2 Google/GitHub `a7b5e51`. Nếu phạm vi chỉ là OTP, đề xuất lấy riêng phần OTP lên nhánh mới từ `main` và đưa qua PR.

## Xung đột Git

| Cách tích hợp | File xung đột |
| --- | --- |
| Merge toàn nhánh `origin/dat` | `.env.example`, `pom.xml`, `src/main/java/com/beautyshop/security/Security_Config.java` |
| Áp riêng `734d094` bằng mô phỏng three-way với commit cha làm base | Ba file trên và `src/main/resources/application-dev.properties` |

Khi xử lý xung đột security, cần giữ quyền truy cập hiện hành của `main` cho `/tu-van-da` và giới hạn ngoại lệ CSRF đúng theo hợp đồng MoMo. Ba POST của luồng OTP cần truy cập được từ phiên Guest và tiếp tục kiểm tra CSRF.

## Vấn đề cần xử lý trước merge

1. **Ràng buộc OTP với một lần đăng ký.** Redis định danh OTP và bộ đếm chỉ bằng email trong khi họ tên và hash mật khẩu nằm trong từng HTTP session. Các phiên đăng ký cùng email dùng chung trạng thái OTP và thao tác hủy ảnh hưởng đến phiên khác. Kiểm tra thời gian chờ gửi lại và xác thực/xóa OTP dùng nhiều lệnh Redis rời nhau. Cần ràng buộc OTP với giao dịch đăng ký, thực thi nguyên tử việc giới hạn gửi lại và xác thực một lần, đồng thời xử lý nhất quán khi SMTP, Redis hoặc ghi tài khoản thất bại. Nguồn tại `734d094`: `EmailVerification_Service.java:32–124`, `TaiKhoan_Controller.java:65–126, 171–181`.
2. **Cấu hình triển khai.** SMTP chỉ được khai báo trong profile `dev`, trong khi `Email_Service` luôn cần `JavaMailSender`. Với cấu hình `prod` trong repo, cần bổ sung cấu hình SMTP để cấp bean. Compose Redis local chạy không mật khẩu nhưng profile `dev` tham chiếu `REDIS_PASSWORD` và file mẫu thiếu biến này. Cần thống nhất cấu hình local, test và prod, gồm địa chỉ gửi và quản lý secrets qua môi trường. Nguồn tại `734d094`: `application-dev.properties:20–34`, `application-prod.properties`, `Email_Service.java:8–15`, `docker-compose.yml:1–17`, `.env.example`.
3. **Test và hợp đồng đăng ký.** `mvn test` trên snapshot `734d094` có 120 lỗi tải Spring context trong 219 ca, 38 ca bỏ qua. Các lỗi xác nhận gồm việc thiếu `EmailVerification_Service` và `OAuth2NguoiDung_Service` trong các test MVC. Test MariaDB cho xác thực và COD vẫn kỳ vọng `POST /register` chuyển thẳng đến `/login?registered`, trong khi luồng OTP thay đổi bước này. Cần cập nhật test và phủ OTP đúng, sai, hết hạn, gửi lại, dùng một lần, cô lập giữa các phiên và CSRF. `GiaoDienPagesTest.java:497` còn kiểm tra liên kết tới trường lỗi và `aria-invalid` mà template mới đã bỏ.
4. **Phạm vi nghiệp vụ.** FR-05, P04 và D25 mô tả đăng ký CUSTOMER bằng email và BCrypt. Việc bắt buộc xác minh email và thêm phụ thuộc SMTP/Redis thay đổi hợp đồng đăng ký P0. Cần ghi quyết định được owner duyệt cho thay đổi này và cập nhật tài liệu cùng PR.

## Bằng chứng

- [Mô phỏng merge](../qa/REVIEW-DAT-OTP/merge-tree.log), cả hai cách trả mã 1 do xung đột.
- [Maven test](../qa/REVIEW-DAT-OTP/mvn-test.log), thất bại trên snapshot `734d094`.
- [Maven package](../qa/REVIEW-DAT-OTP/mvn-package.log), thành công với `-DskipTests` trên snapshot `734d094`.
- `git diff --check 734d094^ 734d094` thành công. Chưa chạy luồng SMTP/Redis/MariaDB đầu cuối hoặc kiểm thử bản đã xử lý xung đột.
