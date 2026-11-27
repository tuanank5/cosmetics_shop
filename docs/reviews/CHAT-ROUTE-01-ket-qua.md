# CHAT-ROUTE-01 · Gỡ form tư vấn cũ

Theo D41, gỡ GET và POST của `/tu-van-da`, khai báo quyền riêng của route, form, DTO, service tìm sản phẩm và hai truy vấn repository dành riêng cho form đó. Các bộ test của luồng đã gỡ được xóa. Chuyển nguyên nội dung template đang phục vụ `/chat` từ `templates/TuVanDa.html` sang `templates/chat/index.html` và cập nhật controller cùng test view tương ứng. Hồ sơ kiểm thử lịch sử vẫn dùng để đối chiếu các bản cũ. [Quyết định D41](../DECISIONS.md)

## Kiểm chứng

- JDK 21, Maven `test` trên bản sao build sạch có 312 ca, 231 đạt, 81 bỏ qua và 0 lỗi. `mvn -DskipTests package` đạt. Kiểm danh sách thành phần JAR thấy controller và template của chat, các lớp và template của form đã gỡ vắng mặt. [Test](../qa/CHAT-ROUTE-01/test.txt) · [Package](../qa/CHAT-ROUTE-01/package.txt)
- JAR mới chạy với MariaDB fixture riêng. Trang chủ, catalog và chat trả 200 và liên kết tư vấn dẫn đến `/chat`. Khách chưa đăng nhập mở URL cũ đi qua quy tắc đăng nhập chung. Customer đã đăng nhập nhận 404 cho cả GET lẫn POST có CSRF ở URL cũ. Gửi câu hỏi tại `/chat` trả 200 và hiển thị phí giao hàng 25.000 ₫ từ FAQ. [HTTP](../qa/CHAT-ROUTE-01/http.json)
- Tích hợp MariaDB/MongoDB riêng đạt 6/6 cho FAQ và RAG với Ollama mô phỏng, gồm kiểm soát dữ liệu nhập, CSRF và giới hạn tần suất. [Tích hợp](../qa/CHAT-ROUTE-01/integration.txt)
