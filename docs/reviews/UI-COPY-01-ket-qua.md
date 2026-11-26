# UI-COPY-01 · Chuẩn hóa thuật ngữ giao diện

Owner duyệt [bảng đối chiếu thuật ngữ](UI-COPY-01-ra-soat-thuat-ngu-giao-dien.md) và chọn khởi tạo database mới cho lần bàn giao. Thay đổi thực hiện trên working tree tại `38fb1ca` cùng phần việc sẵn có.

## Nội dung đã cập nhật

| Phạm vi | Thay đổi và vị trí chính |
|---|---|
| Khách và người mua | Tìm theo tên hoặc mã biến thể. Trang chi tiết, giỏ hàng, thanh toán và đơn hàng dùng “Mã biến thể sản phẩm” bên cạnh mã thực tế. Tồn khả dụng thành “Số lượng còn mua được”. “Voucher” thành “Mã giảm giá”, “Cập nhật báo giá” thành “Tính lại tổng tiền”. Giải thích thanh toán khi nhận hàng và chỉ rõ các màn hình MoMo thử nghiệm. [Templates cửa hàng](../../src/main/resources/templates) |
| Quản trị | Cập nhật tên mục, nút, nhãn và lỗi nhập biến thể. Phân biệt số trong kho, số đang giữ cho đơn hàng và số còn bán được. Chuẩn hóa nhãn mã giảm giá, chỉ số tổng quan và trang quản lý nguồn tư vấn. [Templates quản trị](../../src/main/resources/templates/admin) |
| Tài khoản và tư vấn | Dùng “mã xác minh qua email”, ghi rõ Google hoặc GitHub, đổi FAQ sang “Câu hỏi thường gặp” và giải thích nguồn của câu trả lời AI. Câu chữ hiển thị từ Java được đồng bộ cùng template. [Bảo mật tài khoản](../../src/main/resources/templates/auth/security.html) · [Dịch vụ tư vấn](../../src/main/java/com/beautyshop/service/TuVan_Service.java) · [Tư vấn da](../../src/main/java/com/beautyshop/ai/service/TuVanDaService.java) |
| Tương tác | Thông báo cập nhật số lượng của giỏ lấy mã thực tế từ `data-sku`. Khi yêu thích gặp HTTP 403, thông báo được chọn theo lỗi nội bộ. [Giỏ hàng](../../src/main/resources/static/js/cart.js) · [Yêu thích](../../src/main/resources/static/js/wishlist.js) |
| Nội dung FAQ mẫu | Sửa ba câu về giá và số lượng của biến thể, thanh toán khi nhận hàng và đổi trả. Phí giao hàng 25.000 ₫, ngưỡng miễn phí 500.000 ₫ sau giảm giá và hạn đổi trả 7 ngày được giữ. [V5](../../src/main/resources/db/migration/V5__nap_faq_beautyshop.sql) |

Mã biến thể, route, tham số API, cấu trúc database và quy tắc nghiệp vụ được giữ. Nhật ký kỹ thuật và nội dung email thuộc phạm vi riêng. Ca hồi quy mới đối chiếu trực tiếp bản ghi FAQ trong MariaDB với nội dung `/chat`. [TuVanFaq_MariaDb_Test](../../src/test/java/com/beautyshop/integration/TuVanFaq_MariaDb_Test.java)

## Áp dụng trên máy của nhóm

Tạo một database trống và trỏ `DB_URL` vào database đó. Flyway chạy V1–V7 khi khởi động, trong đó V5 nạp nội dung FAQ cố định. Database từng áp dụng V5 cũ sẽ báo khác checksum. Lượt triển khai này chỉ ghi vào database kiểm thử riêng. [Hướng dẫn local](../deployment/01-local-db-and-flyway.md) · [Quyết định D37](../DECISIONS.md)

## Kiểm chứng

- `mvn -o -B -ntp test` với JDK 21 trên bản chép build riêng báo 316 ca, 241 đạt, 75 bỏ qua, 0 lỗi. `mvn -DskipTests package` tạo JAR thành công. [Maven test](../qa/UI-COPY-01/test.txt) · [Đóng gói](../qa/UI-COPY-01/package.txt)
- Flyway áp dụng đủ bảy migration khi mở JAR trên MariaDB trống. Bộ tích hợp MariaDB/MongoDB báo 318 ca, 313 đạt, 5 bỏ qua, 0 lỗi. FAQ và RAG với Ollama mô phỏng đạt 3/3 ca mỗi nhóm. Năm ca bỏ qua gồm ba ca của fixture nâng cấp local, một ca tạo báo cáo dữ liệu demo và một ca BSON chạy riêng. [Flyway](../qa/UI-COPY-01/flyway.txt) · [Tích hợp](../qa/UI-COPY-01/data/integration.txt)
- BSON chạy riêng đạt. Kiểm khôi phục khớp checksum SQL, BSON và ảnh, khớp liên kết ảnh và kiểm được chỉ mục duy nhất MongoDB. [BSON](../qa/UI-COPY-01/data/bson.txt) · [Khôi phục](../qa/UI-COPY-01/data/restore.txt)
- Chrome chạy JAR trên MariaDB riêng. Guest ở 390 px thấy đúng mã và cập nhật số lượng giỏ. Customer nhận đúng thông báo khi mô phỏng HTTP 403 và trạng thái yêu thích giữ nguyên. Admin ở 1366 px thấy đúng tên danh sách, form thêm và form sửa, cùng mã đã lưu. [Chrome](../qa/UI-COPY-01/browser-results.json)
- `git diff --check` và `git diff --cached --check` đạt.

Ollama, OAuth Google/GitHub, Redis, SMTP và thanh toán từ nhà cung cấp thật chưa được xác minh trong lượt này. Trạng thái QA-01 giữ theo báo cáo nghiệm thu hiện hành.
