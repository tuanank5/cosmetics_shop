# BeautyShop

Website bán mỹ phẩm đa thương hiệu cho một cửa hàng. Nhóm 4 người, hạn nộp 03/12/2026.

Stack gồm Java 21, Maven, Spring Boot 3.5.16, Spring MVC, Thymeleaf, Spring Data JPA và MariaDB. Mã Java tổ chức theo tầng MVC dưới `com.beautyshop`. Xem [kiến trúc](docs/design/01-architecture.md).

## Chạy local

Cần JDK 21, Maven 3.9+ và MariaDB. Chuẩn bị database và đặt `DB_URL`, `DB_USER`, `DB_PASSWORD` theo [hướng dẫn local](docs/deployment/01-local-db-and-flyway.md). MariaDB cài trực tiếp mặc định dùng cổng 3306, Compose tùy chọn dùng 3307. Spring Boot đọc biến môi trường. Dùng [.env.example](.env.example) để tra tên biến.

```powershell
.\scripts\p00.ps1 -Task run
mvn test
mvn -DskipTests package
```

Mở `http://localhost:8080/`, `/products` hoặc `/login`. Để chạy test tích hợp với MariaDB, dùng một database trống dành riêng cho test và đặt `P01_MARIADB_TEST=true` trước khi chạy `mvn test`.

Đăng ký bằng mật khẩu cần Redis và SMTP. Với cấu hình `dev` mặc định, sau khi đặt `DB_PASSWORD` hãy chạy `docker compose up -d redis mailpit` và đọc mã trong hộp thư local tại `http://127.0.0.1:8025`. Mailpit mô phỏng hộp thư, không chuyển tiếp email ra ngoài. Profile `prod` dùng Redis và SMTP từ các biến OTP trong [.env.example](.env.example).

Đăng nhập Google và GitHub bật độc lập. Đặt `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET` hoặc `GITHUB_CLIENT_ID` và `GITHUB_CLIENT_SECRET` trong môi trường. Thêm `oauth2-google`, `oauth2-github` tương ứng vào `SPRING_PROFILES_ACTIVE` cùng profile `dev` hoặc `prod`. URL callback local là `http://localhost:8080/login/oauth2/code/google` hoặc `http://localhost:8080/login/oauth2/code/github`. Khi triển khai, dùng URL callback HTTPS của website với cùng đường dẫn.

## Trạng thái

- **P00:** ba trang giao diện nền trả HTTP 200 trong lần chạy thử. Xác nhận hiển thị ở 375, 768 và 1440 px còn mở.
- **P01:** CRUD danh mục, thương hiệu, sản phẩm tại `/admin/catalog` đã được kiểm thử trên MariaDB.
- **P02:** quản trị SKU, giá, tồn, ảnh và một lô minh họa tại `/admin/catalog/products/{productId}/variants`. Flyway V2 bổ sung ghi nhận trừ kho COD. Xem [contract tồn kho](docs/design/03-order-payment-inventory.md#contract-p02-cho-p06p07).
- **P03:** danh sách và lọc tại `/products`, chi tiết và chọn SKU tại `/products/{slug}`. Cần sản phẩm và SKU đang hoạt động trong database để xem kết quả.
- **P04:** đăng ký bằng mật khẩu tại `/register` và xác thực OTP email trước khi tạo tài khoản CUSTOMER. Trang `/login` hỗ trợ mật khẩu và Google/GitHub khi cấu hình. Giỏ Guest được hợp nhất khi Customer đăng nhập. Xem [quy trình cấp quyền Admin](docs/deployment/03-cap-quyen-admin.md).
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
