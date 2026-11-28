# PAYMENT-FINAL · Lịch sử từng lần thanh toán

Phạm vi PR là phần độc lập sau BASE, trên parent `98d2fa3da4e10ae030f99c3e9a5899de17e2cf28`. Checkout kiểm thử `target/payment-final-run` được tạo mới từ main. Tám tệp source/test và bốn script dựng fixture đã được đối chiếu từng byte với [manifest bản triển khai](../source.json). Hai runner dùng MariaDB riêng trên loopback và tài khoản sinh cho fixture.

| Nội dung | Kết quả review |
|---|---|
| Quyền Customer | Security yêu cầu CUSTOMER cho `/orders/**`. Service kiểm tra cả mã đơn và ID người dùng trong session trước khi đọc. SQL của lịch sử Customer trả NULL cho ID attempt, request ID và mã giao dịch. |
| Quyền Admin | `/admin/**` và `chiTietAdmin` đều yêu cầu ADMIN. Service quyết định projection. Tham số HTTP không chọn được projection Admin. Ref hiển thị giới hạn theo định dạng UUID/COD và mã giao dịch số. |
| Kênh xuất dữ liệu | Hai controller trả về view SSR. Fragment dùng `th:text` cho tham chiếu và nhãn, không có route fragment hoặc JSON mới. Review mã mới không thấy ghi DTO, payload hoặc tham chiếu vào log hay exception. WebKit kiểm riêng mã yêu cầu và mã cổng trong HTML. |
| Chính xác và chi phí đọc | Một SELECT bind mã đơn, sắp `ngay_tao, ma_lan_thanh_toan`. DTO chứa giá trị scalar, không lazy load. Chi phí truy vấn thêm cố định một lần mỗi chi tiết, xác định từ code. Chưa đo latency. Fixture có 0/1/2 dòng và hai timestamp tạo trùng nhau. Mốc thanh toán NULL hiển thị “Chưa ghi nhận”. |
| Dữ liệu cũ | Chỉ hiển thị các dòng thực trong V1. Nhãn và timestamp đến từ cột lưu. V1 không lưu nguồn đối soát, lý do lỗi hay timestamp cập nhật chung. |
| Retry và IPN | Luồng ghi hiện hành khóa hàng đơn bằng FOR UPDATE trong transaction. Sau FAILED, hai caller tranh cùng khóa nhận chung một PENDING mới. IPN xác minh chữ ký, đối chiếu gateway/order/amount. Ca replay và success đến muộn kiểm kho, lượt, trạng thái đơn và số attempt. |
| Thanh toán hiệu lực và COD | Truy vấn trạng thái MoMo ưu tiên PAID rồi ID. Lịch sử chỉ bổ sung model của trang chi tiết. Bộ lọc Admin, transition, thu COD khi giao và cấu hình CSRF dùng logic hiện hành. WebKit kiểm nút xác nhận hiển thị, integration kiểm các thao tác ghi. |

**Finding.** Review phần PAYMENT không phát hiện lỗi source cần sửa. Chênh lệch số SKIP có nguyên nhân xác định được. Bản PERF có 3 test trong lớp `Momo_MariaDb_Test`, PAYMENT có 4. Test mới `haiRetrySauFailedChiTaoMotLanMoi` thừa hưởng điều kiện `@EnabledIfEnvironmentVariable(named="P01_MARIADB_TEST", matches="true")`. Unit `LanThanhToanTest` chạy trong lượt mặc định. [Bằng chứng SKIP](default-case.json)

**Phạm vi nghiệm thu.** Tổng tiền tích hợp VOUCHER, snapshot, đối soát hai slot và regression sau VOUCHER tiếp tục BLOCKED_DEPENDENCY / NOT RUN. MoMo Sandbox thật (PAY-LIVE), Google/GitHub thật, manual UAT và CI của nhánh QA chưa chạy. OAuth và MoMo trong fixture là mô phỏng. Tiến độ dự án 4/19 DONE, AUTH-LIVE tiếp tục BLOCKED_EXTERNAL.

## Verdict local

**PAYMENT — PARTIAL_ACCEPTED / BLOCKED_DEPENDENCY** cho phần lịch sử độc lập.

| Lượt chạy lại | Kết quả |
|---|---|
| Temurin JDK 21, Maven test | 238 PASS, 89 SKIP, 0 FAIL. [Log](test.txt) |
| MariaDB 12.2.2, fixture mới | 95 PASS, 0 SKIP, 0 FAIL. Test retry đồng thời chạy trong 0,396 giây. [Log](integration.txt) · [Testcase](fixture-case.json) |
| WebKit | 8/8 PASS. Guest tới login, Customer khác 404, chủ đơn và Admin đọc đúng số dòng và đúng quyền ở 1280 và 390 px. Quét stdout/stderr ứng dụng sau E2E tìm hai request ID và mã cổng gieo riêng cho đơn 301, kết quả 0 trùng khớp. [Log](webkit.txt) |
| Maven package, git diff --check | PASS. [Log package](package.txt) |

Bước tiếp theo là review PR và CI đúng revision. Trạng thái cục bộ của phần lịch sử chưa đủ điều kiện đóng toàn bộ PAYMENT.
