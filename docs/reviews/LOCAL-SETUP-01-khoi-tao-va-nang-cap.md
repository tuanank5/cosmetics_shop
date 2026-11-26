# LOCAL-SETUP-01 · Thiết lập local, demo và nâng cấp

Baseline `38fb1ca` cộng working diff có trước. Mục tiêu là giảm số biến môi trường phải điền, gom lệnh seed và kiểm đường nâng cấp của các schema đã triển khai.

## Thay đổi

- `.env.example` dùng hai dòng active cho Compose, gồm cổng 3307 và ô mật khẩu cần điền. `scripts/local-env.ps1` cho launcher/seed đọc `.env` theo dạng key-value. Shell đã có biến được ưu tiên. `DB_URL` cũ được giữ nguyên nếu đã khai báo. Profile dev mặc định dùng cổng native 3306 và Mailpit loopback. Production giữ chính sách cấu hình hiện có.
- `docker-compose.yml` gom MariaDB, Redis, MongoDB và Mailpit cho dev. Các cổng bind loopback. Tùy chọn đổi schema/user khi tạo volume mới. MongoDB local giữ DB mặc định để đọc lại collection review hiện có.
- `p00.ps1 -Task migrate` chạy Flyway và xác thực lịch sử mà chưa khởi động web. `-Task demo` chạy migration, catalog, đơn mẫu và yêu thích rồi mở web trên loopback. `seed-demo.ps1 -WithOrders` là lệnh chung để nạp cả hai phần. Lệnh seed catalog cũ còn hoạt động qua cùng entrypoint. Cấu hình demo chấp nhận schema local có tên `beautyshop_demo` hoặc hậu tố phân biệt.
- `BeautyShopApplication` chuyển riêng cờ `--migrate-only` cho `LocalDatabaseMigration`. Migration V1–V7 được giữ theo đúng blob Git hiện tại. Trong ancestry của repo, V6 ở nhánh OTP và bản đang dùng có cùng nội dung. Schema khác checksum hoặc thiếu Flyway history cần bản SQL nguồn và một đường chuyển riêng.
- Cập nhật README, runbook local/demo, tài liệu MongoDB và quyết định D36. Bản thay đổi chỉ thực thi trên fixture thử nghiệm.

## Kết quả xác minh

| Phép kiểm | Kết quả |
|---|---|
| `run-fixture.ps1` · MariaDB 12.2.2 loopback | Schema trống lên V7 từ `.env`. Mật khẩu fixture có khoảng trắng, `#` và `$`. Chạy migrate lại thành công. |
| Nâng cấp có lịch sử cũ | 8/8 ca đạt. Sáu mốc V1–V6 lên V7 và giữ ID, hash của tài khoản mẫu. Ca schema không quản lý và ca checksum lệch đều bị từ chối. |
| Seed lặp | 31 sản phẩm, 32 SKU, 120 đơn, 30 yêu thích. Hai lần nạp có cùng số bản ghi, cùng tổng tồn 123 và schema V7. |
| `run-fixture.ps1 -DemoOnly` | `p00 -Task demo` chạy từ schema trống đến HTTP 200 trên trang SKU. Biến `DB_PORT` có sẵn trong shell thắng giá trị khác trong `.env`. |
| JDK 21 Maven test/package | 241 đạt, 74 skip, 0 lỗi ở lượt mặc định. Package đạt. JAR profile dev khởi động với `DB_PORT`/`DB_NAME`, Flyway và JPA validate, trang SKU trả HTTP 200. |
| Compose và dịch vụ ngoài | YAML parse được. Docker runtime không có trên máy kiểm thử. MongoDB, Redis, Mailpit và OAuth thật chưa chạy trong bằng chứng này. |

`git diff --check` đạt. Bằng chứng lưu trong `docs/qa/LOCAL-SETUP-01/`. [Kết quả Maven, seed và migration](../qa/LOCAL-SETUP-01/results.txt) · [Chạy demo một bước](../qa/LOCAL-SETUP-01/demo-task.txt) · [Kiểm tĩnh Compose](../qa/LOCAL-SETUP-01/compose-static.txt)
