# Báo cáo SDLC BeautyShop

BeautyShop là website bán mỹ phẩm đa thương hiệu cho một cửa hàng. Bản demo P10 dùng dữ liệu sản phẩm đã đối chiếu từ trang thương hiệu và ưu tiên luồng đặt hàng, giao và thu tiền COD. Giá, lượng tồn và tài khoản dùng trong demo là dữ liệu mô phỏng.

## Yêu cầu

Guest tìm sản phẩm, xem các SKU và giữ giỏ theo phiên. Customer đăng nhập để dùng địa chỉ giao hàng, đặt COD và theo dõi đơn của mình. Admin quản lý catalog, giá, tồn và quá trình giao hàng. Hệ thống theo dõi đánh giá, yêu thích và đổi trả theo điều kiện của từng đơn. [SRS](../requirements/01-srs.md) · [Use cases](../design/04-use-cases.md).

Hai điều kiện nghiệm thu chính là hoàn thành COD từ catalog đến giao và thu tiền, đồng thời bảo toàn tồn kho khi checkout hoặc hủy. Hệ thống kiểm tra quyền sở hữu đơn ở backend và tính giá, voucher, phí giao tại máy chủ. [Business Rules](../requirements/02-business-rules.md).

## Thiết kế và triển khai

Spring Boot chạy một ứng dụng Java 21 theo MVC. Thymeleaf dựng HTML, Controller xử lý HTTP, Service điều phối nghiệp vụ và Repository truy cập MariaDB. Flyway quản lý lược đồ V1–V5, còn Hibernate kiểm tra ánh xạ khi khởi động. [Kiến trúc](../design/01-architecture.md) · [ERD](../design/02-erd.md) · [Sơ đồ lớp](../design/06-domain-classes.puml).

Giá và tồn đặt theo SKU. Checkout COD khóa bản ghi liên quan, kiểm tra số lượng khả dụng, ghi snapshot giỏ/địa chỉ, tính tiền và trừ tồn trong transaction. Đơn COD mới ở trạng thái chờ xác nhận, thanh toán `UNPAID`. Admin xác nhận đã giao và thu tiền để chuyển giao hàng sang `DELIVERED` và thanh toán sang `PAID`. Trang `/admin/orders` tổng hợp khoản đã thu theo từng đơn. [Thiết kế giao dịch](../design/03-order-payment-inventory.md) · [Sơ đồ tuần tự](../design/05-sequence.md).

Spring Security dùng phiên, mật khẩu BCrypt, phân quyền Customer/Admin và CSRF cho form. Backend xác thực IPN bằng HMAC khi bật tích hợp MoMo. Trạng thái redirect từ trình duyệt chỉ dùng để hiển thị. Tư vấn có chế độ tìm FAQ mặc định. RAG dùng chỉ mục catalog/FAQ và đọc lại giá, tồn từ MariaDB khi trả lời. [Bảo mật và quyền truy cập](../../AGENTS.md) · [Tư vấn RAG](../deployment/05-chatbot-rag.md).

## Kiểm thử và bàn giao

Maven chạy 49 test trên JDK 21 với MariaDB 12.2.2. Chrome hoàn tất quy trình từ chọn SKU đến xác nhận thu COD, và kiểm tra bảy trang ở 375, 768, 1440 px. Báo cáo ghi dữ liệu đầu vào, kết quả và bằng chứng cho từng TC. [Kết quả TC01–TC20](../testing/02-p10-results.md).

Gói demo local gồm JAR, catalog có nguồn, seeder, hướng dẫn backup/restore, slide và video. Bảng nguồn ghi ngày đối chiếu cho 31 sản phẩm, 32 SKU, 8 thương hiệu và 6 danh mục. [Catalog](../data/product-research.csv) · [Hướng dẫn vận hành](../deployment/06-p10-demo.md) · [Kịch bản và Q&A](demo-script.md).

| Hạng mục còn mở | Điều kiện nghiệm thu |
|---|---|
| Website công khai | Có host Java 21, MariaDB bền vững và HTTPS. Chạy lại smoke test trên URL đã cấp. |
| MoMo Sandbox thật | Có merchant test và IPN HTTPS công khai. Chạy giao dịch và kiểm tra IPN từ MoMo. |
| RAG với Ollama thật | Có Ollama và model phù hợp. Chạy bộ câu hỏi có nguồn và ngoài phạm vi. |

Sau bản demo P10, P11 đã thêm [quản trị voucher](../design/03-order-payment-inventory.md#contract-p11-cho-checkout-và-momo) và kiểm tra hồi quy checkout.
