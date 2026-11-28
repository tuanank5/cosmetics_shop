# AUDIT · Nhật ký quản trị

IN_PROGRESS trên main `4fa226210c95a4539ccb4279a2a78df54af7577f`, tiến độ 4/19 DONE. Owner duyệt AUDIT-D6 và AUDIT-D7. Ma trận dưới được lập trước khi sửa mã.

| Nguồn | Sự kiện và allowlist | Giới hạn |
|---|---|---|
| V7 auth | Giữ các sự kiện xác thực và metadata hiện có | Giữ audit auth cũ. Gắn khóa/mở với lý do bắt buộc chờ hợp đồng ACCOUNT (BLOCKED_OWNER cho danh mục lý do); chưa sửa luồng tài khoản |
| Đơn Admin | ORDER_STATUS, ID đơn, hai trạng thái OrderStatus | Hủy hoặc đổi trạng thái thực tế, rollback ghi FAILED |
| SKU | SKU_STOCK, ID SKU, hai số tồn khi Admin sửa | Xuất COD tự động thuộc chứng từ kho |
| Catalog | CATALOG_CREATE/UPDATE/ACTIVE, ID danh mục/brand/SP/SKU, cờ active | Loại bỏ văn bản mô tả, tên và ảnh khỏi snapshot |
| Mã giảm hiện hành | VOUCHER_CREATE/UPDATE/DELETE, ID và cờ active | Không mở quy tắc hai slot |
| Sau mua | Kiểm định, tái nhập, hoàn thủ công BLOCKED_OWNER cho tới khi có hợp đồng chứng từ và failure cụ thể | RETURN/REFUND riêng |
| Khuyến mãi, backup, AI | Xác nhận gửi, tạo/kiểm/tải bản sao, đổi nguồn/reindex BLOCKED_OWNER về contract tác vụ/I/O và failure | Chưa ghi SUCCESS cho I/O chưa xác nhận |
| D6 | AUDIT_PREVIEW/PURGE, ID Admin vận hành, tổng đủ điều kiện và số dọn | Không lưu nội dung dòng bị dọn |

Metadata chung gồm ID người thao tác, vai trò, thời điểm UTC, action/target, correlation, kết quả và mã lý do cố định. Không serialize request, entity hoặc exception. Quyền vận hành mặc định rỗng. Review PR, CI và nghiệm thu owner còn mở.

## Vận hành D6

Owner cấu hình danh sách ID Admin trong `app.audit.operator-ids` trên môi trường triển khai (danh sách phân cách dấu phẩy). Mặc định không có người vận hành. List/detail ở `/admin/audit` chỉ đọc. Phiên Admin được phân công gửi POST có CSRF tới `/admin/operations/audit/retention` với UUID `correlation`. Mặc định dryRun=true, days=180, batch=100. Thời điểm bằng mốc cutoff UTC vẫn được giữ, bản ghi cũ hơn mới đủ điều kiện. Mỗi lần thực thi xử lý tối đa 500 dòng. Kết quả gồm số đủ điều kiện tại thời điểm đếm và số thực xóa. Gửi lại cùng UUID và cùng chế độ trả kết quả tác vụ đã ghi. Batch tiếp theo dùng UUID mới.

V8 giữ toàn bộ dòng V7 và thêm snapshot nullable, chỉ mục thời gian/ID và bảng `bao_luu_nhat_ky`. Trước khi dọn, nguồn độc lập phải xác nhận từng ID cho chính batch này theo điều kiện AUDIT-FINAL bên dưới. Đầu mối vận hành ghi từng cặp ID nhật ký và lý do DOI_SOAT, TRANH_CHAP hoặc NGHIA_VU cho các hồ sơ bảo lưu ngoài hệ thống. Từng lý do tồn tại độc lập, khóa ngoại RESTRICT chặn xóa dòng cha. Chỉ gỡ lý do tương ứng khi đầu mối xác nhận hết nghĩa vụ. Với target ORDER, pending/refund pending và yêu cầu đổi trả chưa kết thúc cũng được kiểm trực tiếp khi xóa. Tranh chấp bên ngoài cần được đầu mối thông báo và đánh dấu; ứng dụng không có nguồn tự phát hiện trường hợp đó.

Dry-run và thực thi ghi số liệu bằng hai action riêng. Kết quả xóa và SUCCESS commit cùng nhau. Lỗi rollback hoặc mất kết nối trước commit giữ nguyên cả batch. Với correlation đã ghi FAILED, vận hành kiểm tra trước khi yêu cầu batch mới. Dòng chưa tạo thành công không có ID chứng từ bền vững, FAILED khi rollback tạo mới tham chiếu Admin thực hiện. Trường snapshot thiếu trên dòng V7 hiển thị “Chưa ghi nhận”.

## Kiểm chứng local

Hai checkout cô lập trong `target/audit-run` và `target/audit-build` dùng cùng source. Temurin JDK 21, Maven offline, MariaDB 12.2.2 trên loopback. Runner tạo datadir mới mỗi lượt. WebKit dùng tài khoản fixture. [Manifest SHA-256](../qa/AUDIT/source.json)

Từ checkout có mã ứng viên, đặt JAVA_HOME tới JDK 21, BASE_PLAYWRIGHT tới module playwright-core có WebKit. Biến PowerShell `$repo` trỏ đến cache Maven, `$qa` tới thư mục bằng chứng AUDIT. Các lệnh đã dùng:

```powershell
mvn.cmd -o -B -ntp "-Dmaven.repo.local=$repo" test
mvn.cmd -o -B -ntp "-Dmaven.repo.local=$repo" -DskipTests package
powershell -NoProfile -ExecutionPolicy Bypass -File docs/qa/AUDIT/run-mariadb.ps1 -Checkout . -MavenRepo "$repo" -EvidenceDirectory "$qa"
powershell -NoProfile -ExecutionPolicy Bypass -File docs/qa/AUDIT/run-browser.ps1
git diff --check
```

Lượt integration đầu có 2 failure, 35 error. Fixture mới thiếu dấu kết thúc statement cho Spring SQL và một số test ghi bằng principal giả chỉ có role. Đã dùng principal có ID tài khoản fixture và sửa seed. Lượt thứ hai còn 1 failure do ca Guest của voucher thừa hưởng phiên Admin từ fixture lớp. Đã chỉ định anonymous cho request Guest và giữ các assertion quyền/CSRF. [Lượt đầu](../qa/AUDIT/integration-first.txt) · [Lượt hai](../qa/AUDIT/integration-second.txt)

| Gate trên source trước AUDIT-FINAL | Kết quả |
|---|---|
| Maven test | 240 PASS, 93 SKIP, 0 failure/error. Các lớp yêu cầu fixture dùng gate môi trường. [Test](../qa/AUDIT/test.txt) |
| MariaDB riêng | 102/102 PASS, 0 SKIP. Kiểm SUCCESS và FAILED sau rollback thực, snapshot theo loại, unique correlation/action, ngưỡng UTC trước/đúng/sau 180 ngày, ba lý do bảo lưu, refund pending, replay, rollback giữa hai batch, từ chối Admin chưa được phân công. Hai fixture V5/V6 qua V7 có dòng auth cũ rồi lên V8; migrate lại 0 migration. [Integration](../qa/AUDIT/integration.txt) · [Các testcase](../qa/AUDIT/cases.json) |
| WebKit | 3/3 nhóm PASS. Guest chuyển tới login, Customer bị từ chối cả list và detail. Admin lọc-tìm-sort-page sang detail và Back giữ query/anchor ở hai tab, chạy cả khi tắt JS. Khi bật JS kiểm focus hàng còn tồn tại, POST dọn thiếu CSRF bị chặn, sau khi dọn dòng trong fixture thì Back đưa focus về bộ đếm. [WebKit](../qa/AUDIT/webkit.txt) |
| Package và Git | JDK 21 package PASS, git diff --check PASS. [Package](../qa/AUDIT/package.txt) |

SHA-256 của tập source/test và runner AUDIT là `938173d57171d2dc0a24a0e905656c1c2cdea89f130b08b60db2fd167a6853ad`. **NOT RUN** cho review PR, CI của revision ứng viên và UAT của owner. Chưa chạy purge trên môi trường thật. AUDIT giữ IN_PROGRESS.

## AUDIT-FINAL · Review bảo mật

**AUDIT IN_PROGRESS. Purge thực tế BLOCKED_EXTERNAL** cho đến khi owner xác nhận nguồn phân loại bao phủ đối soát, tranh chấp và nghĩa vụ bảo lưu. Quyền đọc nhật ký độc lập với việc vận hành dọn. Bản đầu có thể chọn nhầm dòng chưa đánh dấu; ứng viên cuối yêu cầu xác nhận dương tính từng ID. Sửa thêm SUCCESS cho lần lưu catalog/SKU/mã giảm không thay đổi và việc bỏ sót thay đổi metadata SKU cùng lượt chỉnh tồn. [Review và ma trận gate](../qa/AUDIT/final-review/review.md) · [Manifest ứng viên cuối](../qa/AUDIT/final-review/source.json)

Công tắc phía server `app.audit.clearance-source-enabled` mặc định tắt. Khi tắt, kể cả Admin được phân công cũng nhận 409 trước khi preview hoặc purge. Chỉ mở sau khi có nguồn tin cậy cung cấp quan hệ chỉ đọc `audit_purge_clearance(batch_id, audit_id, expires_utc)` trong MariaDB. Nguồn này xác nhận hồ sơ **được phép dọn** sau khi kiểm mọi nghĩa vụ. Danh sách theo UUID phải cố định hoặc chỉ thu hẹp, hết hiệu lực theo UTC, được thu hồi khi có tranh chấp mới. Ứng dụng không có API hoặc luồng ghi cấp các xác nhận đó. Khi source bật, cùng correlation dùng cho preview và execute của một batch. Truy vấn xóa tự kiểm lại danh sách, hạn xác nhận, tuổi và các hold hiện hành. Thiếu quan hệ hoặc lỗi nguồn làm transaction thất bại. Chỉ fixture cung cấp nguồn giả lập; V8 không tạo nguồn hoặc tự phê duyệt dòng nào.

Kiểm lại trên ứng viên cuối: Maven 240 PASS/95 SKIP, MariaDB 104/104 PASS, WebKit 3/3 nhóm, package PASS. Phạm vi chưa chạy được liệt kê theo testcase. [Kết quả và giới hạn](../qa/AUDIT/final-review/review.md)
