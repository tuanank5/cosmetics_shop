> **BeautyShop v5:** tên bảng và cột MariaDB đã đổi sang tiếng Việt không dấu. Migration chuẩn là `db/migration/V1__khoi_tao_co_so_du_lieu.sql`. Bảng ánh xạ: `docs/data/bang_doi_chieu_ten.csv`. Nếu prompt cũ nhắc tên bảng tiếng Anh, phải áp dụng tên v5; không tạo trùng bảng tiếng Anh. Chỉ chạy migration mới trên database trống, không sửa V1 đã được áp dụng.

# BeautyShop — Website bán mỹ phẩm đa thương hiệu

> **Bản scaffold v4 · cập nhật 12/11/2026 · hạn nộp 03/12/2026**  
> Nhóm **4 người**, đồ án môn học, ngân sách triển khai ưu tiên **miễn phí**. `BeautyShop` là **tên tạm**, chưa chốt thương hiệu chính thức.

## Trạng thái thực tế

- **Đã tạo:** bộ khung Spring Boot Maven, web page Thymeleaf giới thiệu dự án, CSS responsive, Spring Security cấu hình các đường dẫn, mẫu chính sách phí giao hàng & trạng thái đơn, unit test, tài liệu nghiệp vụ/thiết kế, 12 prompt Codex P00–P11.
- **Đã bổ sung Flyway V1:** schema SQL cho toàn bộ domain; CHƯA chạy xác nhận với MariaDB tại môi trường tạo ZIP.
- **Chưa hoàn thành:** entity/repository/service CRUD, tài khoản đăng nhập bằng MariaDB, giỏ, checkout, MoMo, AI, seed 30–50 sản phẩm, deployment. **Không dùng scaffold này như bằng chứng rằng MVP đã hoàn tất.**
- Security hiện **chặn mọi lần đăng nhập** qua `BootstrapUserDetailsService` cho tới khi nhóm triển khai P04. Không chứa tài khoản/mật khẩu demo hardcode.

## Stack / phạm vi

- Java **21**, Maven, Spring Boot **3.5.16**, Spring MVC, Thymeleaf, Spring Data JPA, MariaDB, Spring Security Form Login + HttpSession (không dùng JWT cho web).
- Monolith chia module theo nghiệp vụ: `security`, `user`, `catalog`, `inventory`, `cart`, `order`, `shipping`, `payment`, `promotion`, `review`, `ai`, `admin`.
- COD **P0**; MoMo **Sandbox P1 có điều kiện** và giữ hàng theo SKU 15 phút khi triển khai online, phí ship 25.000đ/đơn và miễn phí từ 500.000đ; một voucher/đơn; đổi trả trong 7 ngày, admin duyệt thủ công.
- AI dùng Spring AI + RAG qua catalog/FAQ, triển khai **sau core**, cấu hình provider khi làm P09 (starter AI chưa đưa vào POM scaffold để tránh lỗi khởi động nếu thiếu model/API).
- Spring AI 1.1.x tương thích Spring Boot 3.5.x theo công bố của dự án Spring AI: https://github.com/spring-projects/spring-ai

## Chạy thử scaffold (Windows PowerShell)

Yêu cầu: **JDK 21**, **Maven 3.9+**, **MariaDB** (hoặc Docker Desktop nếu muốn). Không cần Docker nếu MariaDB local có sẵn.

```powershell
cd beautyshop
java -version
mvn -version
# Tạo database beautyshop + user local theo docs/deployment/01-local-db-and-flyway.md
# Docker chỉ tùy chọn; nếu dùng compose, cần đặt DB_URL port 3307.
# Nếu dùng MariaDB local, set biến DB_URL, DB_USER, DB_PASSWORD phù hợp.
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- Mặc định profile dev: `jdbc:mariadb://localhost:3306/beautyshop` cho MariaDB cài trực tiếp. Với Docker Compose dùng cổng 3307 (set `DB_URL`).
- Truy cập http://localhost:8080 (homepage scaffold), http://localhost:8080/products (placeholder).
- Kiểm thử: `mvn test` ; đóng gói: `mvn -DskipTests package`.
- **Chưa có Maven Wrapper chuẩn (`mvnw`)** do môi trường dựng ZIP không thể tải Maven Wrapper JAR. Dùng `mvn` đã cài; khi máy có Internet chạy `mvn -N wrapper:wrapper` để tạo wrapper (commit `mvnw`, `mvnw.cmd`, `.mvn/wrapper/*`). Tránh tạo script giả mạo wrapper.
- `.env.example` chỉ là tài liệu tham khảo. Spring Boot không tự đọc `.env`: cần khai báo biến qua PowerShell/IntelliJ hoặc dùng `.env` cùng một công cụ nạp biến có kiểm soát.

## Tài liệu / thứ tự đọc

1. `AGENTS.md` (quy tắc nhất quán cho nhóm/Codex)
2. `docs/requirements/01-srs.md` và `docs/requirements/02-business-rules.md`
3. `docs/design/02-erd.md` và `docs/design/03-order-payment-inventory.md`
4. `docs/03-roadmap-and-team.md` (21 ngày, owner và điểm tích hợp)
5. `docs/prompts/P00` đến `P11` (đã đổi VNPay → MoMo)
6. `docs/testing/01-test-plan.md`, `docs/deployment/01-local-db-and-flyway.md`, `docs/data/README.md`

## Quy tắc nhóm

- `main` luôn có khả năng build; mỗi task tạo branch, PR và review chéo.
- Không sửa trạng thái thanh toán theo return URL của browser; MoMo IPN và đối soát là cơ sở xác minh.
- Chưa có chứng nhận bản quyền cho ảnh/dữ liệu mỹ phẩm. `docs/data/product-research.csv` là **danh sách nghiên cứu**, KHÔNG phải seed đã được xác thực.
- Tên dự án, logo, website tham khảo, tài khoản Sandbox, địa chỉ deploy sẽ được điền sau, không giả lập.

## Quyết định thiết kế v4

- Catalog/SKU/Cart/COD/P0 không lệ thuộc MoMo Sandbox; MoMo chỉ được báo PASS nếu có merchant/IPN HTTPS và chứng cứ thực tế.
- AI (P09) dùng Ollama `qwen2.5:3b` + `bge-m3` tại máy phù hợp, mặc định disabled, không ép máy 8GB RAM tải model khi startup.
- `ProductVariant.demoBatchCode/demoExpiryDate` chỉ là **metadata một lô demo**, không hỗ trợ tồn kho nhiều lô/FEFO.
- Class Diagram/ERD nộp theo v4, xem `docs/design/07-group-diagram-mapping.md`.
