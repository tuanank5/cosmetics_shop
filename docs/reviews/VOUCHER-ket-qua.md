# VOUCHER · Phạm vi, hai slot và vòng đời lượt

**IN_PROGRESS · 28/11/2026.** Owner giao VOUCHER-1–3 sau khi nghiệm thu AUDIT-A. Nền `5bdada6bb370b281319ff671fd8a9b62b5673560`. Bảng triển khai ban đầu được giữ làm lịch sử. [Kết quả cuối](../qa/VOUCHER/final-review/review.md). 5/19 DONE. PAYMENT giữ PARTIAL_ACCEPTED / BLOCKED_DEPENDENCY cho tới nghiệm thu trên revision tích hợp. [Quyết định và ví dụ](VOUCHER-quyet-dinh-1-3.md) · [Tiến độ](BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md)

## Mã và dữ liệu

| Đường thực hiện | Thay đổi |
|---|---|
| Giỏ, checkout và chi tiết | GioHang_Controller và Checkout_Controller lấy principal từ phiên. Preview trên SKU server, hiển thị riêng giảm HÀNG/SHIP, phí chuẩn, tiền cuối. checkout.js gửi form báo giá và kiểm hai mã trước submit. Đơn mới lưu phí thực trả cho consumer cũ, đồng thời lưu phí chuẩn và giảm SHIP. |
| Policy và quota | PhieuGiamGia_Service lọc chiều hàng và Customer, phân bổ tiền xuống dòng bằng BigDecimal, xét minSpend SHIP sau phân bổ. PhieuGiamGia_Repository kiểm thời gian DB UTC, quota tổng và số chứng từ còn HELD/CONSUMED của khách. |
| Admin | Form giữ tìm kiếm, phân trang và số USED/HELD/còn. Cấu hình slot, giới hạn từng khách và bốn tập ID. Backend kiểm đối tượng, role Customer, quota và thay đổi thực trước khi ghi sự kiện audit. |
| Giao dịch | DonHang_Service khóa giỏ và SKU theo ID, lấy giá dưới khóa. Khóa từng voucher theo ID trước khi xét quota. Khóa voucher dùng chung cho các Customer và thao tác Admin. Hai slot, tiền đơn, attempt và kho cùng transaction. Hủy COD chỉ trả lượt trên đơn UNPAID đủ điều kiện. MoMo chuyển lượt theo dữ liệu đã xác minh dưới khóa đơn. Khi còn REFUND_PENDING giữ tài nguyên đang chờ, chặn tạo attempt mới và ẩn thao tác thử lại cho tới khi có kết quả cuối. |
| V9 | Bốn bảng phạm vi có FK, loại và quota khách trên phiếu. Mở rộng bảng lượt V1 với unique (đơn, slot) và (đơn, mã), dùng CONSUMED cho USED. Thêm snapshot phí/giảm ship trên đơn, giảm HÀNG trên dòng. Đơn cũ giữ nguyên số tiền, attempt và bộ đếm. Đơn cũ không có phân bổ lưu NULL. |

Trong giỏ, một SKU là một dòng. ID SKU làm khóa phân xử đồng dư trước khi có ID chi tiết đơn. Danh sách 24 tệp source, migration, test và SHA-256 đối chiếu ở cả hai checkout. Hash tổng hợp `96310599a6c2c5b19ed3d0aafce8a0bf1c568ff0a73cc87d0295983d559892df`. [Manifest](../qa/VOUCHER/source.json) · [Migration V9](../../src/main/resources/db/migration/V9__phieu_giam_gia_hai_slot.sql)

## Đối soát trên fixture

| Kịch bản | Tiền và kết quả quota |
|---|---|
| A 120.000 ₫, C 80.000 ₫ cùng hãng, mã chọn A và hãng, minSpend 150.000 ₫ | Giá trị hợp lệ 120.000 ₫. Server từ chối mã. Quote ghi 0 lượt. |
| 520.000 − 30.000 + 25.000 − 10.000 ₫ | Tổng 505.000 ₫. Phí thực trả 15.000 ₫. COD ghi hai CONSUMED. Khi quota khách đã đủ, đơn tiếp bị từ chối. Hủy COD hai lần chỉ trả mỗi mã một lượt. |
| 530.000 − 30.000 ₫ | Tiền hàng 500.000 ₫, phí 0, tổng 500.000 ₫. Bỏ SHIP trước khi ghi chứng từ. |
| A 200.000 ₫, B 300.000 ₫, giảm HÀNG 50.000 ₫ | Phân bổ 20.000/30.000 ₫. SHIP chỉ chọn A nên minSpend 180.001 ₫ thất bại và 180.000 ₫ đạt. Mã FIXED 90.000 ₫ giảm tối đa 25.000 ₫, tổng 450.000 ₫. |
| Ba dòng 100.000 ₫, mã FIXED 10.000 ₫ | ID 10/20/30 nhận 3.334/3.333/3.333 ₫ dù thứ tự đầu vào khác. |
| MoMo mock trên SKU 150.000 ₫, HÀNG 10.000 ₫, SHIP 1.000 ₫ | Attempt chốt đúng 164.000 ₫. Hai slot HELD khi chờ, CONSUMED khi chốt, RELEASED khi thất bại cuối đủ điều kiện. Replay không trừ thêm. Attempt cũ thành công sau attempt mới được ghi REFUND_PENDING, số đã dùng không tăng. |
| Lỗi giữa hai slot và tranh lượt cuối | Fixture gây lỗi ở slot hai, kiểm rollback của COD và HELD. Hai giỏ khác SKU tranh hai mã còn một lượt, chỉ một đơn thành công. Sai số tiền, retry, hai callback đồng thời và cuộc đua giữa kết quả hủy/thành công được kiểm riêng. |
| Legacy V8→V9 | Mỗi schema thử có đơn COD và MoMo 65.000 ₫. Sau nâng cấp tổng vẫn 130.000 ₫, đúng một CONSUMED và một HELD HÀNG. Không tăng bộ đếm để backfill. |

## Kiểm chứng local

Temurin 21.0.13, Maven offline, MariaDB 12.2 trên loopback. Checkout build và checkout integration trong target riêng. Mỗi lần chạy integration tạo datadir MariaDB/MongoDB mới. WebKit dùng JAR đã package và MariaDB mới trên loopback.

| Nhóm | Kết quả |
|---|---|
| Maven test | **244 PASS, 101 SKIP, 0 FAIL**. Trong 101 phương thức SKIP mặc định có 80 phương thức tìm thấy invocation PASS trên fixture và 21 NOT RUN. [Log](../qa/VOUCHER/maven.txt) · [Đối chiếu từng phương thức](../qa/VOUCHER/skip.json) |
| Integration | **121/121 PASS**, gồm MoMo mock, COD, rollback, audit, Admin, KPI, payment history, sau mua và migration. Số này tính riêng với Maven mặc định. [Log](../qa/VOUCHER/integration.txt) |
| Flyway/JPA | Fixture mới V1→V9 khởi động với validate. Hai schema thử nâng có dữ liệu V8, Flyway validate và migrate lại 0 thay đổi. V1–V8 giữ nguyên nội dung trên main. |
| WebKit | **4/4 nhóm PASS**, Customer đặt COD hai mã với JS/no-JS, Guest dự tính và merge giỏ, Admin tạo mã SHIP giới hạn sản phẩm/quota. SQL sau WebKit khớp tiền đơn, attempt và số slot ở cả ba đơn. [Kết quả](../qa/VOUCHER/webkit.txt) |
| Package | **PASS**. [Log](../qa/VOUCHER/package.txt) |

Đã kiểm `git diff --check` và đối chiếu source/hash giữa working tree và hai checkout cuối. [Biên nhận fixture và JAR](../qa/VOUCHER/verification.json)

## Finding và giới hạn

WebKit khi tạo voucher có phạm vi phát hiện bind sai vị trí tham số quota trong UPDATE. Đã chuyển sang tham số đặt tên. Test MariaDB tạo/sửa quota và gửi lại form kiểm chỉ có một VOUCHER_UPDATE. Lượt WebKit đầu cho Guest chuyển trang trước khi POST thêm hàng hoàn tất. Runner hiện chờ điều hướng đã xác nhận.

**BLOCKED_EXTERNAL** áp dụng cho AUDIT PURGE, AUTH-LIVE và provider MoMo thật theo gate riêng. MoMo trong regression là mock. **BLOCKED_DEPENDENCY** áp dụng cho nghiệm thu PAYMENT sau tích hợp VOUCHER. **NOT RUN** gồm CI/owner UAT trên candidate xuất bản, hoàn tiền thực tế và 21 phương thức ngoài suite fixture đã ghi. Không mở luồng hoàn tiền mới. Các số đo tải thực và chính sách vận hành clearance nằm ngoài đợt này.

### Tái lập

Từ checkout cô lập có source và các harness QA trong repo, đặt JAVA_HOME tới JDK 21, BASE_PLAYWRIGHT tới playwright-core có WebKit. Chỉ chạy trên fixture loopback. Runner integration nhận đường dẫn tuyệt đối tới checkout, Maven cache và thư mục bằng chứng. Chạy WebKit từ checkout chứa JAR.

```powershell
mvn -o "-Dmaven.repo.local=$repo" test
mvn -o "-Dmaven.repo.local=$repo" -DskipTests package
powershell -ExecutionPolicy Bypass -File docs/qa/VOUCHER/run-mariadb.ps1 -Checkout . -MavenRepo $repo -EvidenceDirectory docs/qa/VOUCHER
powershell -ExecutionPolicy Bypass -File docs/qa/VOUCHER/run-browser.ps1
git diff --check
```

VOUCHER-FINAL cần so manifest với candidate, review khóa và dữ liệu lịch sử, tách hunk tài liệu dùng chung, kiểm PR/CI đúng tree rồi trình owner. PAYMENT đối chiếu riêng số tiền attempt, snapshot và hai lượt trên revision tích hợp.

## VOUCHER-FINAL

VOUCHER-FINAL · IN_PROGRESS. Đã review SQL, quota, số tiền gateway mock và migration V9. Maven 244 PASS/102 SKIP, MariaDB/MongoDB 127/127, bootstrap 10/10, WebKit 4/4, package PASS. Chờ PR, CI đúng revision và owner nghiệm thu. [Review, bằng chứng và 21 ca ban đầu chưa chạy](../qa/VOUCHER/final-review/review.md) · [Manifest nguồn cuối](../qa/VOUCHER/final-review/source.json)
