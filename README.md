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

Mở `http://localhost:8080/`, `/products` hoặc `/login`. Để chạy test tích hợp catalog, SKU, tồn kho và storefront, dùng một database MariaDB trống dành riêng cho test và đặt `P01_MARIADB_TEST=true` trước khi chạy `mvn test`.

## Trạng thái

- **P00:** ba trang giao diện nền trả HTTP 200 trong lần chạy thử. Xác nhận hiển thị ở 375, 768 và 1440 px còn mở.
- **P01:** CRUD danh mục, thương hiệu, sản phẩm tại `/admin/catalog` đã được kiểm thử với phiên Admin trong test tích hợp. Đăng nhập bằng tài khoản thật thuộc P04.
- **P02:** quản trị SKU, giá, tồn, ảnh và một lô minh họa tại `/admin/catalog/products/{productId}/variants`. Flyway V2 bổ sung ghi nhận trừ kho COD. Xem [contract tồn kho](docs/design/03-order-payment-inventory.md#contract-p02-cho-p06p07).
- **P03:** danh sách và lọc tại `/products`, chi tiết và chọn SKU tại `/products/{slug}`. Cần sản phẩm và SKU đang hoạt động trong database để xem kết quả.
- **MVP đang tiếp tục:** giỏ hàng và checkout COD theo roadmap. MoMo Sandbox và chatbot RAG là các mốc sau. Xem [tiến độ và bằng chứng kiểm thử](docs/03-roadmap-and-team.md).

## Tài liệu

- [Quy ước làm việc](AGENTS.md) · [SRS](docs/requirements/01-srs.md) · [Business rules](docs/requirements/02-business-rules.md) · [Quyết định](docs/DECISIONS.md)
- [Kiến trúc MVC](docs/design/01-architecture.md) · [ERD v5](docs/design/02-erd.md) · [Đối chiếu tên SQL và dữ liệu demo](docs/data/README.md)
- [Roadmap](docs/03-roadmap-and-team.md) · [Kế hoạch kiểm thử](docs/testing/01-test-plan.md) · [Triển khai](docs/deployment/README.md) · [Hồ sơ nộp](docs/report/README.md)

Các công việc P00–P11 nằm trong [docs/prompts](docs/prompts/).
