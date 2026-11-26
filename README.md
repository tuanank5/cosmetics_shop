# BeautyShop

Website bán mỹ phẩm đa thương hiệu cho một cửa hàng. Nhóm 4 người, hạn nộp 03/12/2026.

Stack gồm Java 21, Maven, Spring Boot 3.5.16, Spring MVC, Thymeleaf, Spring Data JPA và MariaDB. Mã Java tổ chức theo tầng MVC dưới `com.beautyshop`. Xem [kiến trúc](docs/design/01-architecture.md).

## Chạy local

Cần JDK 21, Maven 3.9+ và MariaDB. Trên máy mới dùng Docker Compose, sao chép [.env.example](.env.example) thành `.env` rồi điền `DB_PASSWORD`. Mẫu đã chọn cổng host 3307 và dùng database/user mặc định `beautyshop`.

```powershell
Copy-Item .env.example .env
# Điền DB_PASSWORD trong .env trước khi chạy tiếp.
docker compose up -d --wait
.\scripts\p00.ps1 -Task run
```

Mở `http://localhost:8080/`, `/products` hoặc `/login`. MariaDB cài trực tiếp thường dùng cổng 3306. **Nếu database đã chạy V5 trước đợt cập nhật FAQ, hãy tạo một database trống và trỏ `DB_URL` đến database mới.** Với database có checksum khớp phiên bản hiện tại, có thể sao lưu và dùng `p00.ps1 -Task migrate` để áp dụng các phiên bản còn thiếu. Lệnh `p00.ps1 -Task demo` nạp dữ liệu mẫu và mở web trên schema demo riêng. [MariaDB và nâng cấp](docs/deployment/01-local-db-and-flyway.md) · [Demo](docs/deployment/06-p10-demo.md)

Dev dùng Redis và SMTP Mailpit cục bộ mặc định. Khi dùng Compose, mở hộp thư OTP tại `http://127.0.0.1:8025`. Google và GitHub là tùy chọn. Mỗi nút chỉ xuất hiện khi cấu hình đủ cặp `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` hoặc `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET`. Callback local dùng `http://localhost:8080/login/oauth2/code/google` hoặc `http://localhost:8080/login/oauth2/code/github`. Admin đăng nhập bằng mật khẩu.

Dùng `mvn test` và `mvn -DskipTests package` để kiểm thử và đóng gói. Với test tích hợp, dùng database trống riêng, đặt biến của fixture và bật `P01_MARIADB_TEST=true` theo test đang chạy.

Thiết lập `AUTH_PUBLIC_URL` khớp URL website để tạo link đặt lại mật khẩu. Profile dev mặc định dùng `http://localhost:8080`. Các môi trường triển khai dùng URL HTTPS đã cấu hình. `MAIL_FROM` có thể đặt riêng, mặc định dùng `MAIL_USERNAME`. Redis phục vụ OTP và giới hạn tần suất, MariaDB lưu hash token đặt lại mật khẩu. Xem cách thử từng luồng và chuẩn bị migration ở [hướng dẫn AUTH-02](docs/reviews/AUTH-02-nghiem-thu-xac-thuc.md).

## Trạng thái

- **P00:** ba trang giao diện nền trả HTTP 200 trong lần chạy thử. Xác nhận hiển thị ở 375, 768 và 1440 px còn mở.
- **P01 / CAT-01:** Admin thêm, sửa, ngừng hoạt động và kích hoạt lại danh mục, thương hiệu, sản phẩm tại `/admin/catalog`. Đã kiểm thử trên MariaDB. [Quyết định D21](docs/DECISIONS.md)
- **P02:** quản trị SKU, giá, tồn, ảnh và một lô minh họa tại `/admin/catalog/products/{productId}/variants`. Flyway V2 bổ sung ghi nhận trừ kho COD. Xem [contract tồn kho](docs/design/03-order-payment-inventory.md#contract-p02-cho-p06p07).
- **P03:** danh sách và lọc tại `/products`, chi tiết và chọn SKU tại `/products/{slug}`. Cần sản phẩm và SKU đang hoạt động trong database để xem kết quả.
- **P04:** đăng ký tại `/register`, đăng nhập bằng email tại `/login` và hợp nhất giỏ Guest khi Customer đăng nhập. Xem [quy trình cấp quyền Admin](docs/deployment/03-cap-quyen-admin.md).
- **P05:** Customer chỉnh hồ sơ tại `/account` và quản lý địa chỉ tại `/account/addresses`. Admin tìm kiếm, phân trang, khóa và mở tài khoản tại `/admin/users`.
- **P06:** chỉnh giỏ tại `/cart`, chọn địa chỉ và đặt COD tại `/checkout`, theo dõi hoặc hủy đơn hợp lệ tại `/orders`. Admin xác nhận giao và thu tiền tại `/admin/orders`.
- **P07:** tích hợp có adapter mô phỏng cho dev/test. Sandbox thật vẫn `BLOCKED_EXTERNAL` vì thiếu merchant và HTTPS công khai. [Cách chạy MoMo](docs/deployment/04-momo-sandbox.md).
- **P08:** đánh giá từ chi tiết đơn đã giao, danh sách yêu thích tại `/account/wishlist` và theo dõi đổi trả tại `/account/returns`. Admin duyệt đánh giá và xử lý đổi trả tại `/admin/reviews` và `/admin/returns`.
- **P09:** tư vấn tại `/chat`, mặc định là tìm FAQ. RAG đã được kiểm thử với Ollama mô phỏng. Ollama thật còn `BLOCKED_AI`. [Hướng dẫn P09](docs/deployment/05-chatbot-rag.md).
- **P10:** catalog demo 31 sản phẩm, dashboard thu tiền tại `/admin/orders` và COD E2E trên Chrome/MariaDB. [Chạy demo](docs/deployment/06-p10-demo.md) · [Báo cáo TC01–TC20](docs/testing/02-p10-results.md) · [Hồ sơ trình bày](docs/report/README.md). Host công khai còn `BLOCKED_HOST`.
- **P11:** Admin tạo, xem, sửa, ngừng hoạt động và xóa voucher chưa có lịch sử tại `/admin/vouchers`. Nhập giờ UTC và giới hạn sử dụng. Customer nhập mã tại `/checkout`. [Contract voucher và kết quả kiểm thử](docs/design/03-order-payment-inventory.md#contract-p11-cho-checkout-và-momo) · [Roadmap](docs/03-roadmap-and-team.md#kết-quả-kiểm-chứng).

## Tài liệu

- [Quy ước làm việc](AGENTS.md) · [SRS](docs/requirements/01-srs.md) · [Business rules](docs/requirements/02-business-rules.md) · [Quyết định](docs/DECISIONS.md)
- [Kiến trúc MVC](docs/design/01-architecture.md) · [ERD v5](docs/design/02-erd.md) · [Đối chiếu tên SQL và dữ liệu demo](docs/data/README.md)
- [Roadmap](docs/03-roadmap-and-team.md) · [Kế hoạch kiểm thử](docs/testing/01-test-plan.md) · [Triển khai](docs/deployment/README.md) · [Hồ sơ nộp](docs/report/README.md)

Các công việc P00–P11 nằm trong [docs/prompts](docs/prompts/).
