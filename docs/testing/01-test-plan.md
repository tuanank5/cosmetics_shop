# Test Plan — 03/12/2026

## Đối tượng

Java domain helper unit tests; Controller/Service integration tests dùng MariaDB test database hoặc Testcontainers nếu môi trường hỗ trợ; E2E browser cho Guest/Customer/Admin; MoMo Sandbox kiểm thử hợp lệ và callback giả mạo; responsive và accessibility cơ bản.

## Các ca kiểm thử bắt buộc

| TC | Bước / điều kiện | Kết quả đúng |
|---|---|---|
| TC01 | Guest thêm SKU A vào giỏ rồi login | Giỏ tồn tại và chỉ merge 1 lần |
| TC02 | Giỏ có SKU hết hàng; checkout | Báo lỗi, không tạo paid order hay trừ hàng khác |
| TC03 | Hai khách checkout SKU cuối cùng đồng thời | Một giao dịch thắng, không âm available |
| TC04 | COD checkout + Admin hủy hợp lệ | Hoàn onHand đúng 1 lần |
| TC05 | Online checkout | Reserve đúng qty, tổng available cập nhật |
| TC06 | IPN valid signed SUCCESS | PAID và consume reserve/voucher đúng 1 lần |
| TC07 | IPN valid gửi lại nhiều lần | State/stock/voucher không đổi thêm |
| TC08 | IPN chữ ký sai/số tiền sai | Reject; log an toàn; không PAID |
| TC09 | Reservation 15 phút hết; gateway chưa rõ | Không giải phóng mù, đưa đối soát |
| TC10 | Gateway báo thành công sau khi reserve released | Không oversell; ghi reconciliation/refund path |
| TC11 | Voucher còn 1 lượt, 2 checkout | Không vượt usage limit |
| TC12 | Voucher 10%/max + phí ship 500k sau voucher | Tổng chính xác |
| TC13 | User A sửa URL order của User B | 403/404 không tiết lộ dữ liệu |
| TC14 | Customer chưa nhận hàng viết review | Bị từ chối |
| TC15 | Customer đã nhận hàng viết review 2 lần cùng order item | Lần thứ hai bị từ chối hoặc edit, không tạo row trùng |
| TC16 | Return request sau DELIVERED 8 ngày | Từ chối đúng chính sách 7 ngày |
| TC17 | Admin cố nhảy PENDING_PAYMENT -> DELIVERED | Bị từ chối |
| TC18 | Chatbot hỏi sản phẩm không có trong catalog | Trả lời không đủ dữ liệu, không bịa sản phẩm |
| TC19 | API upload ảnh đổi phần mở rộng/script | Reject/không thực thi file |
| TC20 | Responsive 375px/768px/1440px | Không tràn ngang, không lỗi font/nút |

## Báo cáo kết quả

Mỗi TC có: người chạy, ngày, commit, môi trường, dữ liệu đầu vào, expected, actual, PASS/FAIL/BLOCKED, ảnh/video/log (che bí mật). Không ghi PASS khi chưa chạy.

## Quality gates

- `mvn test`: toàn bộ unit test quan trọng PASS;
- `mvn -DskipTests package`: build thành công;
- DB transaction nghiệp vụ chứng minh bằng integration test;
- IPN error handling và authorization bắt buộc test trước khi demo;
- không có credentials hardcode, không giao code bắt buộc cho demo nếu bị lỗi blocker nghiêm trọng.

## Kiểm thử bắt buộc v4
- Migration: V1 trên MariaDB mới tạo đủ bảng, chạy lần 2 không lỗi, `flyway_schema_history` có checksum; DB cũ có bảng chưa baseline phải fail an toàn.
- Security: `POST /payment/momo/ipn` bypass CSRF, IPN cần kiểm chữ ký ở P07. Form POST cần CSRF. P02 có `GET /uploads/sku/{filename}` cho ảnh SKU đã ghi nhận, kiểm định dạng tên và kiểm tra file trong thư mục ảnh.
- COD P0: 2 request đồng thời cho SKU cuối cùng phải có đúng một đơn thành công; voucher không vượt giới hạn; hủy hoàn kho một lần.
- MoMo P1: mock phải không thực hiện thanh toán thật; kết quả qua return không chốt PAID; IPN ký đúng/replay/late-IPN cần test.
- RAG P1: offline fallback không giả vờ là AI; câu hỏi thiếu nguồn phải nói không đủ thông tin.
