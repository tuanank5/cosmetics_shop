# AGENTS.md — Quy ước làm việc của BeautyShop

## Nguồn yêu cầu

Trước khi sửa, đọc [README](README.md), [SRS](docs/requirements/01-srs.md), [Business Rules](docs/requirements/02-business-rules.md), [ERD v5](docs/design/02-erd.md), [nhật ký quyết định](docs/DECISIONS.md) và prompt của task đang làm. Nếu có xung đột, nêu rõ và ghi quyết định đã duyệt trước khi đổi nghiệp vụ.

Ưu tiên COD trọn luồng cho P0. MoMo Sandbox và chatbot RAG theo phạm vi P1 trong SRS. Khi dùng mock, ghi rõ phạm vi mô phỏng và kết quả đã kiểm chứng.

## Mã nguồn và dữ liệu

- Java 21, Spring Boot 3.5.16, Spring MVC, Thymeleaf, Spring Data JPA và MariaDB. Ứng dụng là một monolith dùng Maven.
- Tổ chức theo tầng dưới `com.beautyshop`: `controller` (quản trị tại `controller.admin`), `service`, `repository`, `entity`, `dto`, `security`. Giữ các quy tắc domain độc lập trong package phù hợp. Tạo `api` hoặc `exception` khi có lớp thực tế.
- Dùng constructor injection, DTO với `@Valid` và `BindingResult`, PRG sau POST thành công, giao diện tiếng Việt UTF-8. Template ở `src/main/resources/templates/`; assets tối thiểu lưu trong repo. Giữ giao diện responsive và phong cách Premium/Luxury.
- Tên vật lý MariaDB dùng tiếng Việt không dấu theo [bảng đối chiếu](docs/data/bang_doi_chieu_ten.csv). Ánh xạ đúng bằng `@Table`, `@Column`, `@JoinColumn` hoặc override tương ứng. Với các lớp gắn với dữ liệu, dùng tên nghiệp vụ tiếng Việt không dấu và dấu `_` trước hậu tố kỹ thuật. Ví dụ `ThuongHieu_Repository` và `DanhMucSanPhamThuongHieu_Service`. Thuộc tính JPA theo camelCase từ tên cột, ví dụ `ThuongHieu.maThuongHieu` ánh xạ `ma_thuong_hieu`. Quan hệ JPA dùng tên đối tượng nghiệp vụ. Giữ từ tiếng Anh chuyên ngành và dùng dấu `_` trước hậu tố kỹ thuật, ví dụ `Bootstrap_Controller`, `AdminDanhMucSanPhamThuongHieu_Controller` và `ShippingFee_Policy`. Dùng tiếng Việt không dấu cho các thuộc tính và biến mô tả nghiệp vụ, ví dụ `thuongHieuRepository`, `maThuongHieu`, `tamTinhSauGiamGia`. Giữ các tên kỹ thuật như `path`, `label` và hợp đồng HTTP hiện hành.
- Flyway bật ở dev/prod, JPA dùng `ddl-auto=validate`. V1 chuẩn nằm tại [migration hiện hành](src/main/resources/db/migration/V1__khoi_tao_co_so_du_lieu.sql). Giữ nguyên V1 đã áp dụng và tạo migration phiên bản mới cho thay đổi schema. Dùng database trống riêng để kiểm tra khởi tạo.

## Bảo mật và bất biến nghiệp vụ

- Spring Security dùng Form Login, Session, BCrypt và kiểm tra vai trò tại backend. Web MVP dùng xác thực theo session. Guest là anonymous. Tài khoản Customer và Admin dùng `ROLE_CUSTOMER` và `ROLE_ADMIN`. Kiểm tra quyền sở hữu từng đơn. Giữ CSRF cho thao tác ghi theo session. Ngoại lệ duy nhất được phép là `POST /payment/momo/ipn` với HMAC theo spec.
- Lấy secrets từ môi trường hoặc secret store. Credentials production không có mặc định. Giữ `.env` và dữ liệu runtime ngoài Git. Không tạo mật khẩu admin demo cố định. P04 phụ trách xác thực dựa trên repository.
- Upload ảnh dùng thư mục riêng, giới hạn MIME, phần mở rộng và dung lượng, đặt tên UUID, chống path traversal. Chỉ công khai ảnh đã kiểm tra và dùng nguồn có quyền phù hợp.
- Giữ đầy đủ [quy tắc nghiệp vụ](docs/requirements/02-business-rules.md) và [thiết kế giao dịch](docs/design/03-order-payment-inventory.md). Tiền dùng BigDecimal/DECIMAL, tính server-side. Tồn theo SKU. Giỏ Guest vào DB đúng một lần sau đăng nhập và kiểm tra lại giá/tồn. COD xuất kho trong transaction.
- Khi triển khai MoMo, giữ tồn 15 phút và giữ lượt voucher nguyên tử; IPN đối chiếu chữ ký, định danh giao dịch, số tiền, currency và kết quả. Ghi nhận và giải phóng đúng một lần, đối soát khi trạng thái chưa rõ. Browser redirect dùng để thông báo.
- Tách trạng thái đơn, thanh toán và đổi trả. Áp dụng điều kiện hủy trước PACKING và quy trình hoàn tiền. Review chỉ từ order item thuộc người mua trong đơn DELIVERED, tối đa một lần mỗi item. Chatbot chỉ tư vấn dựa trên nguồn catalog/FAQ kiểm chứng, đọc lại giá/tồn hiện hành, không ghi dữ liệu, tiết lộ thông tin cá nhân hoặc chẩn đoán da liễu.

## Thực hiện và kiểm chứng

- Làm một task/lần, xem trạng thái Git và giữ các thay đổi sẵn có. Báo cho owner liên quan khi thay đổi contract dùng chung và cập nhật test tương ứng. Theo yêu cầu hiện hành về commit/PR.
- Khi đổi mã, chạy `mvn test`, `mvn -DskipTests package` và kiểm chứng dev với MariaDB nếu liên quan. Bổ sung test cho quy tắc nghiệp vụ bị tác động, gồm tính tiền, trạng thái đơn, khóa SKU, hết hạn, IPN trùng, voucher hoặc sở hữu đơn.
- Báo file sửa, lệnh đã chạy, kết quả, điểm chưa xác minh và link PR nếu có. Cập nhật quyết định khi có thay đổi đã duyệt và roadmap khi tiến độ thay đổi.
