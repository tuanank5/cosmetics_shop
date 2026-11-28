# QUALITY-FINAL · Kết quả review ngày 28/11/2026

**Ứng viên** `qa/quality`, parent `0ed547b5d4c18eab46fb3a194fbbdd327c37d827`. Owner giao rà và stage. Review local đã thực hiện, CI và nghiệm thu còn mở.

## Phạm vi nguồn

Cả hai tệp Java đã khác HEAD lúc tiếp nhận QUALITY. Git chỉ xác định được nội dung, không xác định được người viết hoặc thời điểm viết từng dòng chưa commit. QUALITY nhận và kiểm các hunk sau:

- `DonHang_Service.java` tách phần đọc riêng cho hai entry point, tái sử dụng trạng thái thanh toán khi dựng DTO, truyền điều kiện PAID vào `next` và lấy điều kiện đó từ DB trong thao tác ghi. Toàn bộ diff service phục vụ chi tiết đơn và caller của `next`.
- `CheckoutIntegrityTest.java` thêm import OrderStatus và đúng một test tham số với hai đầu vào. Các test checkout có sẵn nằm ngoài diff. SHA-256 của cả hai file trùng manifest lần regression trước. QUALITY-FINAL không sửa thêm Java.
- Báo cáo, dòng tiến độ QUALITY, log chọn lọc và fixture QUALITY ghi lại phạm vi và kết quả. Roadmap và sổ tiến độ trong clone chỉ nhận dòng QUALITY trên nội dung của revision nền. Các chỉnh sửa trạng thái BASE/AUTH/phụ thuộc trong cùng tệp ở root cần bàn giao riêng.

## Khi trạng thái thanh toán thay đổi đồng thời

Truy vấn hiển thị ưu tiên bất kỳ attempt PAID, sau đó ưu tiên ID attempt lớn hơn. GET đọc giá trị này một lần vào biến cục bộ. Nhãn và điều kiện thanh toán cho nút cùng dùng kết quả đó. Nếu IPN commit sau lần đọc, trang đã dựng có thể cũ tới lần GET sau. GET là transaction chỉ đọc với isolation mặc định. Phạm vi bảo đảm ở đây là nhất quán giữa hai chỗ dùng giá trị thanh toán, không phải snapshot xuyên request hoặc mọi phép đọc ở mọi isolation.

POST chuyển bước kiểm vai trò Admin, khóa hàng đơn bằng SELECT FOR UPDATE trong READ_COMMITTED, đọc lại trạng thái thanh toán của MoMo, kiểm bước kế tiếp và OrderState_Policy trước khi ghi. Nhánh IPN/đối soát cũng khóa hàng đơn trước khi áp dụng. Hai writer trên cùng đơn lần lượt thấy kết quả đã commit. HTML cũ, hai lần bấm hoặc hai tab không cung cấp giá trị PAID cho POST. COD đi theo điều kiện riêng. Hủy/hoàn tồn, trừ kho và lượt voucher, thu COD khi giao, chữ ký IPN, CSRF và kiểm sở hữu giữ nguyên. Không thêm nguồn nhập HTML/SQL, credential hay quyền ghi.

## Bằng chứng và giới hạn

Test tham số buộc đúng một lần gọi repository cho cả PAID và PENDING, kiểm nhãn và bước CONFIRMED/null. Nó dùng Mockito và trạng thái đơn cố định để tách điều kiện PAID. Không mô phỏng hai DB transaction. Đối chứng trước sửa ghi hai lần gọi. Kiểm thêm CheckoutPagesTest, AdminServiceMethodSecurityTest, OrderStatePolicyTest, cùng các suite MariaDB DonHangCOD, Momo, PhieuGiamGia, BienTheSanPham, CatalogDonHang và AdminOrderList. Có ca hai khách tranh SKU/voucher, hủy lặp, IPN lặp, từ chối chuyển đơn chưa trả, hết hạn và đến muộn. WebKit đối chiếu buyer/other/admin. Chưa chạy một lịch xen kẽ cưỡng bức GET–IPN–POST. Kết luận về lịch đó dựa trên thứ tự khóa và điều kiện SQL. OAuth, MoMo và AI dùng fixture/mock trong các phép kiểm tương ứng.

## Sáu lỗi root và cách tái kiểm

Root có `target/test-classes/com/beautyshop/security/OAuth2NguoiDungTest.class` và `target/test-classes/com/beautyshop/service/EmailVerificationTest.class`. Cả hai source đều vắng trong git tree nền và src/test hiện tại. Surefire vẫn phát hiện 3 + 5 ca từ các class này khi chạy incremental. Đối chiếu báo cáo Surefire ở root:

| Class | Triệu chứng và bằng chứng |
|---|---|
| OAuth2NguoiDungTest | 3 NoSuchMethodError gọi constructor cũ (RestClient.Builder, RestTemplate). Source hiện hành dùng constructor không đối số. |
| EmailVerificationTest | 1 NoSuchMethodError gọi Email_Service(JavaMailSender, String, String), source hiện dùng hai đối số. 2 IncompatibleClassChangeError do metadata InnerClasses của EmailVerification_Service và GuiOTP_Exception bất đồng. Class lồng cũ còn trong target/classes. Hai ca còn lại đạt. |

Root đếm 332 ca với 6 error. Checkout mới đếm 324 ca, giảm đúng 8 ca dư. Maven compile/test incremental có thể giữ output của source đã biến mất. Dùng clone mới với thư mục target ban đầu trống, hoặc sau khi owner commit dùng clone mới đúng SHA ứng viên. Trước commit, chép riêng hai file trong manifest vào clone cùng parent rồi so SHA-256. Chạy từ clone ở JDK 21:

```powershell
mvn -B test
if ($LASTEXITCODE) { throw 'Maven test failed' }
mvn -B -DskipTests package
if ($LASTEXITCODE) { throw 'Maven package failed' }
```

Kiểm offline dùng thêm `-o -ntp` và `"-Dmaven.repo.local=$repo"` với cache đầy đủ. Trên Windows, chạy runner từ clone đã package, truyền thư mục evidence mới và đường dẫn cache. Đặt JAVA_HOME tới JDK 21, BASE_PLAYWRIGHT tới module playwright-core đã cài kèm WebKit. Runner MariaDB khởi tạo datadir GUID mới trên loopback, ba schema test mới và tắt fixture khi xong. Runner trình duyệt dùng fixture MariaDB/dev riêng, tài khoản tổng hợp và JAR vừa package.

```powershell
$out = (New-Item -Type Directory ("target/quality-results-" + [guid]::NewGuid())).FullName
powershell -NoProfile -ExecutionPolicy Bypass -File docs/qa/QUALITY/final-review/run-mariadb.ps1 -Checkout . -MavenRepo $repo -EvidenceDirectory $out
if ($LASTEXITCODE) { throw 'Integration failed' }
powershell -NoProfile -ExecutionPolicy Bypass -File docs/qa/QUALITY/run-order-detail.ps1
if ($LASTEXITCODE) { throw 'WebKit failed' }
```

## Điểm chuyển tiếp

OAuth2LoginSuccess_Handler trống và overload voucher chưa thấy caller tĩnh là ứng viên bảo trì còn giữ. Hai overload danh sách đơn/đổi trả có test gọi. Chi phí màn catalog chờ phép đo PERF. AUTH-LIVE chờ provider thật. Workflow Maven CI ở revision nền chạy test và package trên JDK 21 cho push/pull_request, không bật fixture MariaDB hoặc WebKit. Owner đối chiếu SHA commit, tree và CI trước nghiệm thu.


## Kết quả chạy lại trong clone bàn giao

JDK 21.0.13 và Maven 3.9.11. Suite mặc định 236 PASS, 88 SKIP, 0 fail/error. Suite 14 lớp trên MariaDB riêng 94/94 PASS. Package thành công. WebKit trên JAR dev 3/3 PASS cho chủ đơn, Customer khác và Admin. Log cùng thư mục gồm [test](test.txt), [tích hợp](integration.txt), [package](package.txt) và [WebKit](browser.txt).
