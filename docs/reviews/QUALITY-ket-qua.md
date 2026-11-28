# QUALITY · Rà soát các module ổn định

**IN_PROGRESS · 28/11/2026.** Owner giao phần độc lập sau BASE và AUTH. Revision gốc trên main là `0ed547b5d4c18eab46fb3a194fbbdd327c37d827`. Bản được kiểm là revision này cộng hai tệp Java trong manifest. Tại thời điểm tiếp nhận, cả hai tệp, test đối chứng và dòng IN_PROGRESS đã có trong working tree. Tài liệu AUTH, BASE, báo cáo đồ án, ảnh, patch và bằng chứng untracked là công việc sẵn có. [Manifest SHA-256](../qa/QUALITY/verification/source.json)

### Finding đã kiểm chứng

`GET /orders/{id}` và `GET /admin/orders/{id}` cùng dựng `order/detail.html`. Trên bản gốc, khi đọc đơn MoMo, `chiTietAdmin` hỏi repository về trạng thái thanh toán để hiển thị, rồi `next` hỏi lại cùng trạng thái để quyết định nút chuyển bước. Hai ca PAID/PENDING trên bản gốc đều ghi nhận hai lần gọi. Đây là chi phí một lần đọc dư mỗi lần dựng trang MoMo. Chưa đo thời gian đáp ứng. [Đối chứng trước sửa](../qa/QUALITY/before.txt)

`DonHang_Service.docChiTiet` dùng một kết quả cho cả nhãn và bước tiếp theo. Điểm vào Customer kiểm sở hữu trước khi đọc, điểm vào Admin có `@PreAuthorize`. Thao tác ghi trạng thái vẫn khóa đơn và lấy lại trạng thái thanh toán trong transaction. Hai ca tham số của `CheckoutIntegrityTest` yêu cầu đúng một lần đọc, kiểm PAID cho phép CONFIRMED và PENDING chưa có bước tiếp theo ở PENDING_CONFIRMATION. Contract HTTP, DTO, schema và quy tắc chuyển đơn giữ nguyên. [Diff nguồn được kiểm](../qa/QUALITY/verification/source.json)

### Phạm vi rà soát và quyết định

| Vùng | Bằng chứng trong source và quyết định |
|---|---|
| Checkout, validation, xử lý lỗi | `Checkout_Controller` dùng form có validation, BindingResult, PRG khi đặt thành công và thông báo thử lại khi tranh chấp. `DonHang_Service` kiểm chủ địa chỉ, khóa giỏ, đọc lại SKU, lập snapshot BigDecimal và tự tính giảm giá, phí, tổng. Giữ các lần đọc giữa báo giá và đặt đơn vì chúng phục vụ hai thời điểm khác nhau. |
| COD, tồn và voucher | Transaction đặt đơn bao gồm trừ kho, ghi lượt voucher, chi tiết, payment và xóa mục giỏ. `TonKho_Repository` dùng tham số bind và khóa bản ghi theo đơn/SKU. Giữ các dấu ghi nhận xử lý lặp. Đã đối chiếu bằng DonHangCOD, BienTheSanPham, CatalogDonHang và PhieuGiamGia trên MariaDB. |
| MoMo, đồng thời và SQL | `Momo_Controller` giới hạn body IPN, kiểm JSON trùng key, trả 400/409 theo loại lỗi. `Momo_Facade` kiểm chữ ký. `ThanhToanMomo_Service` khóa đơn, so attempt, gateway, số tiền và mã giao dịch trước khi chốt hoặc giải phóng. Giữ lần đọc attempt mới nhất và trạng thái khi ghi. Repository bind giá trị, phần SQL ghép để lọc/order trong các truy vấn đã xem dùng đoạn SQL nội bộ. Bộ MoMo kiểm replay, retry, hết hạn và callback muộn bằng gateway mô phỏng. BR08 có yêu cầu currency, record IPN hiện không có trường currency riêng. Kế hoạch PAYMENT quy định VND cố định theo contract. Kiểm merchant thật thuộc PAY-LIVE. |
| Quyền, CSRF, XSS | `Security_Config` giới hạn Admin, Customer và chỉ miễn CSRF theo POST IPN. `QuyenDonHang_Service` bind mã đơn và mã người dùng, trả 404 khi sai chủ sở hữu. Template chi tiết dùng `th:text`. Rà template không thấy `th:utext`. Chat JS dùng `textContent`. Hai chỗ `innerHTML` trong home-carousel lưu và khôi phục markup phân trang có sẵn. Giữ selector và fragment. |
| OAuth | `Security_Config` dùng `LoginSuccess_Handler`, request repository theo session và `KhongLuuToken`. `OAuth2ThaoTac` dùng mutex khi tiêu thụ trạng thái. MariaDB và WebKit dùng provider fixture. Lớp trống `OAuth2LoginSuccess_Handler` là ứng viên bảo trì, chưa có nhu cầu đổi contract AUTH vừa tích hợp. Giữ để owner module review riêng. |
| AI | `TuVan_Service.hoi` kiểm PII/phạm vi trước khi gọi provider, giới hạn worker, timeout và fallback FAQ. Nạp lại index yêu cầu Admin. Đối chiếu TuVanFaq/TuVanRag trên fixture, chưa kiểm provider AI thật hoặc tải production. |
| Caller và chi phí | Giữ overload `danhSachCustomer(long)` và `danhSachDoiTra(Long)` vì DonHangCOD và SauMua integration test còn gọi. Controller dùng bản phân trang. `PhieuGiamGia_Service.danhSachAdmin()` chưa thấy caller tĩnh ngoài khai báo, chưa có bằng chứng chi phí runtime. Các phép đọc bổ sung ở màn catalog thuộc benchmark PERF. Giữ các ứng viên này, migration, JPQL, mapping và tài nguyên giao diện. |

### Kiểm chứng và bàn giao

JDK Temurin 21.0.13, Maven 3.9.11, Windows. Lệnh Maven dùng `-o -B -ntp` và cache `target/auth-03-maven`. Cache đầu tiên thiếu dependency. Lần chạy `mvn test` ở root phát hiện 332 ca và 6 lỗi từ hai lớp test cũ chỉ còn bytecode trong target. Đã chuyển sang checkout mới từ HEAD, chép đúng hai tệp trong manifest để build lại toàn bộ. [Log root](../qa/QUALITY/retest-test.txt)

Suite tích hợp dùng `mvn -Dtest=… test` với 14 lớp liệt kê trong log, MariaDB riêng ở loopback, ba schema mới và Flyway. 94/94 đạt, gồm 82 ca tích hợp và 12 ca unit chạy kèm. WebKit OAuth dùng callback fixture. Không có kết quả provider ngoài, tải production hay UAT thủ công. [Log tích hợp](../qa/QUALITY/verification/integration.txt)

Checkout mới chạy mvn test đạt 236/324, 88 skip theo điều kiện fixture, 0 fail/error. mvn -DskipTests package thành công. WebKit chạy JAR profile dev trên MariaDB mới, với đơn MoMo mock đã trả tiền được seed để kiểm giao diện. Chủ đơn nhận HTTP 200 và thấy mã đơn, Customer khác nhận 404, Admin nhận 200 và thấy PAID cùng nút CONFIRMED. [Maven test](../qa/QUALITY/verification/test.txt) · [Package](../qa/QUALITY/verification/package.txt) · [WebKit](../qa/QUALITY/verification/browser.txt) · [Fixture](../qa/QUALITY/run-order-detail.ps1)

Điều kiện nghiệm thu còn lại là review diff và finding, CI gắn đúng SHA ứng viên và owner nghiệm thu cùng revision. AUTH-LIVE vẫn BLOCKED_EXTERNAL. Tổng tiến độ vẫn 2/19 DONE.


## QUALITY-FINAL · 28/11/2026

Đã đối chiếu từng hunk với parent `0ed547b` và hash hai tệp nguồn. Clone `target/qa-quality` trên nhánh `qa/quality` có index riêng. Review ghi nhận thứ tự khóa khi IPN và POST Admin cùng xử lý một đơn, giới hạn của snapshot GET và nguyên nhân sáu lỗi bytecode cũ. QUALITY đang IN_PROGRESS, chờ PR, CI và owner nghiệm thu. [Review cuối](../qa/QUALITY/final-review/review.md)
