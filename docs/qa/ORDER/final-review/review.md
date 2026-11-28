# ORDER-FINAL · Review candidate

Nền `3da645dd76f22b8f83351d63ca419bd5646af6e8`. Hash tổng hợp của cả 12 tệp ban đầu được tính lại trên working tree và hai bản sao build/fixture theo thuật toán trong [manifest trước FINAL](../source.json). Kết quả `bea633c0898d00d2826f58db63376846a45215ca931286b36242cfa1537624b2`.

## Finding và xử lý

**Form mua lại dùng được sai đơn trong cùng tài khoản.** Ca MariaDB tạo hai đơn cùng Customer, lấy form sau GET đơn A và gửi với đích đơn B. Bản trước FINAL trả 302 về giỏ thay vì 404. Quyền sở hữu vẫn được bảo toàn, nhưng thao tác có thể cộng SKU từ đơn sai với ý định lúc mở form. [Lượt đỏ trước sửa](integration-before.txt)

UUID tại bảng V3 đã phân biệt hai đơn nên chỉ thay unique của bảng sẽ không xử lý được lần gửi đầu tới sai đích. Candidate ký UUID form, ID chủ và ID đơn bằng HMAC-SHA256 với khóa ngẫu nhiên trong session phía server. Kiểm chữ ký ở POST, sau đó service xác nhận ROLE_CUSTOMER và sở hữu đơn trước khi ghi. Form của phiên khác, form bị đổi đích và đơn của Customer khác bị từ chối. Khóa không đưa vào HTML. Session hết hạn cần đăng nhập và mở form mới. Biên nhận nghiệp vụ vẫn lưu bền trong DB cùng transaction cập nhật giỏ.

## V3, khóa và vòng đời

| Tình huống | Hợp đồng và phép kiểm |
|---|---|
| Gộp giỏ và mua lại | Guest cart chỉ sinh UUID v4 phía server. Mua lại sinh khóa v3 từ namespace reorder, ID chủ theo principal, ID đơn và UUID của form. Bit phiên bản khác nhau. Fixture chèn biên nhận merge với chính UUID thô của form rồi thực hiện retry. |
| Retry cùng thao tác | Unique PK ma_gop tuần tự hóa hai insert. Duplicate trả kết quả đã xử lý. Hai POST đồng thời cùng form chỉ cộng một lần. Gửi lại sau khi người dùng xóa SKU khỏi giỏ cũng không cộng lại. |
| Thao tác mới, cùng chủ | GET/reload chỉ đọc dữ liệu nghiệp vụ. GET cấp UUID mới. Hai form hợp lệ khác nhau tranh một đơn vị tồn được tuần tự hóa bằng khóa giỏ, SKU và khóa trạng thái cha. |
| Lỗi trước commit | Trigger trên fixture báo lỗi sau INSERT chi tiết giỏ, khi biên nhận đã được ghi trong transaction. SQL kiểm cả giỏ và số biên nhận của khách về 0. Retry cùng form thành công. |
| Khả dụng | SKU được xét theo thứ tự ID, dùng lượng hiện có trong giỏ và tồn sau giữ. Thiếu SKU hoặc ngừng bán bỏ qua. Hết tồn, giỏ đã đủ hoặc chỉ đủ một phần có thông báo riêng. Khóa đọc sản phẩm/cha được giữ đến commit. |
| Chi phí | Một thao tác mới đã commit sinh một hàng V3, kể cả khi mọi SKU đều bị bỏ qua. Retry cùng khóa sinh 0 hàng mới. PK phục vụ chống trùng, FK có index theo Customer. Đường ghi này chưa cần thêm index. |
| Lưu giữ | V3 chưa có TTL hoặc job dọn định kỳ. Xóa biên nhận trong khi form còn khả năng gửi lại sẽ cho phép cộng lại. Dọn cần hợp đồng thời hạn form/receipt và review riêng. Chưa đo tải, tốc độ tăng hoặc kích thước index production. |

## Danh sách, phân quyền và dữ liệu

Danh sách dùng tiền tố mã bind tham số. Dấu gạch dưới được escape để tìm ký tự thật. Giá trị ngoài allowlist (gồm % và chữ có dấu) đưa tiêu chí mã về rỗng, các predicate chủ và trạng thái vẫn áp dụng. Sort chỉ chọn biểu thức định sẵn với ID tie-break. SELECT và COUNT cùng predicate chủ, cùng transaction REPEATABLE_READ, không đọc dòng đơn hay attempt theo từng dòng danh sách. Giữ cỡ 10; page âm được chuẩn hóa, page lớn được chặn hoặc đưa về trang hợp lệ. Ngày Việt Nam chuyển sang UTC theo khoảng đóng đầu, mở cuối. Sai định dạng và đảo khoảng ngày trả 400. URL Back được ghép từ route cố định, trạng thái, query đã chuẩn hóa, trang và anchor theo ID. Fixture xác minh biên 17:00 UTC, 11 đơn đồng thời điểm và tie-break của trang thứ hai. WebKit bổ sung kiểm activeElement trùng dòng quay lại. Bộ lọc Admin dùng lại predicate và regression hiện hành.

Guest cần đăng nhập. Admin không mua thay Customer. Thiếu CSRF trả 403. Controller không nhận ID Customer, giá hay trạng thái SKU làm quyền ghi. Sau POST trả redirect về giỏ. SQL so toàn bộ hàng của đơn, các dòng, attempt, chứng từ hai mã, giữ SKU, chứng từ trừ kho COD và vận chuyển trước/sau. Snapshot địa chỉ và giá nằm trong phép so này. Fixture xác nhận đơn/attempt 505.000 VND, giá cũ 510.000 VND, giá giỏ mới 490.000 VND, hai chứng từ CONSUMED 30.000 và 10.000 VND, mỗi mã USED=1. Kho ba SKU sau mua lại bằng 11, 2 và 0. WebKit đặt đơn riêng với giá giỏ hiện hành và cả hai ô voucher rỗng.

## Kiểm chứng cuối

JDK 21, Maven offline, checkout build/IT riêng, fixture MariaDB/MongoDB loopback. Maven mặc định 252 PASS/107 SKIP, package PASS. Integration 132/132 PASS. WebKit ORDER 2/2, VOUCHER 4/4 nhóm cùng đối chiếu SQL, PAYMENT 8/8 vai trò/viewport PASS. Hai JAR dùng để package và chạy browser khớp cả 414 entry ứng dụng. Các lượt có suite giao nhau; không cộng thành một tổng. [Maven](verified/maven.txt) · [Integration](verified/integration.txt) · [Package](verified/package.txt) · [ORDER và VOUCHER](webkit.txt) · [PAYMENT](webkit-payment.txt)

107 phương thức skip mặc định do thiếu cờ fixture: P01 MariaDB 92, P23 Mongo 9, bootstrap 3, AUTH migration 1, demo P21 1 và ARCH BSON 1. Có 91 phương thức khớp invocation PASS ở suite riêng, bao gồm cả ba ca ORDER cũ và hai ca FINAL mới. 16 phương thức NOT RUN trong lượt FINAL: 9 Mongo, 3 bootstrap, 2 MariaDB, 1 demo và 1 BSON. Schema không đổi, không chạy riêng gate Flyway upgrade. Các lượt cũ trước FINAL được lưu ở thư mục cha. [Đối chiếu từng phương thức](skip.json)

SHA-256 tổng hợp 13 tệp source/test cuối `a5a6f42ad7254159453ee59ae4e5dc547f528445c68c695f4850602809c5e3c2`. [Manifest](source.json)

ORDER IN_PROGRESS. Chờ review PR, CI tại đúng revision và owner nghiệm thu. Manual UAT, MoMo Sandbox thật và tải production NOT RUN. AUTH-LIVE, PAY-LIVE và AUDIT PURGE giữ BLOCKED_EXTERNAL. VOUCHER MERGED / READY_FOR_OWNER_ACCEPTANCE; PAYMENT PARTIAL_ACCEPTED / READY_FOR_OWNER_ACCEPTANCE trong phạm vi COD/MoMo mock. Tiến độ 5/19 DONE.
