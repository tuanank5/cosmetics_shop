# 05-ACCOUNT · Review cuối ngày 28/11/2026

**Phạm vi**. Baseline và tracking ref local origin/main cùng ở `5aa4932cc663de1115ff06ab7a8d7b837a673d93`. Năm quyết định owner áp dụng cho fixture. Tiến độ 8/19 DONE, ACCOUNT IN_PROGRESS. Index gốc ban đầu trống, có 11 stash. Tài liệu AUTH/AUDIT/PERF/QUALITY đang sửa, backup, báo cáo đồ án, dữ liệu runtime và bằng chứng các WP khác nằm ngoài phạm vi stage.

### Hai finding đã tái hiện

| Finding | Bằng chứng trước sửa | Thay đổi, kết quả cần giữ |
|---|---|---|
| Worker hoàn tất sau hạn lease nhưng trước poller kế tiếp | MariaDB trả SENT dù fixture đã đẩy hạn lease về thời điểm hiện tại. | Câu UPDATE cuối đối chiếu token, SENDING và hạn UTC ngay tại DB. Nếu còn đúng token nhưng đã hết hạn thì chuyển UNKNOWN. Khi poller đã thu hồi token, worker cũ cập nhật 0 hàng. |
| Hai ý định nghiệp vụ trả chung FORM_CU | Admin B yêu cầu khóa một tài khoản đã được A khóa. Service trả FORM_CU giống trường hợp tài khoản đã đi hết một vòng khóa/mở. | Trả KHONG_DOI khi đích đã ở trạng thái yêu cầu. Trả FORM_CU khi áp lệnh cũ có thể đảo một thay đổi mới. Cả hai trả thông báo và kết thúc trước khi ghi. Replay khớp biên nhận vẫn trả DA_GHI. |

Lượt đỏ chạy riêng 7 ca với production source trước sửa. Hai assertion trên thất bại, 5 ca còn lại đạt. [Lượt đỏ](before.txt) · [Integration cuối](integration.txt)

### Đối chiếu các quyết định

| Quyết định | Đường kiểm và bằng chứng |
|---|---|
| ACCOUNT-1 | Form ràng buộc phiên, actor, target, chiều thao tác và version qua UUID do server cấp. Enum có đúng bốn mã owner duyệt, tất cả vừa VARCHAR(40). Service chỉ nhận enum đúng chiều. Test POST xác nhận thiếu quyền/CSRF, mã rỗng, trắng, ngoài danh mục, dài 41 ký tự và sai chiều. Hai correlation giữa hai Admin chỉ đổi một lần. Hai request cùng correlation cho một DA_DOI, một DA_GHI. Form mở trước khi người khác khóa rồi mở gặp conflict. Admin mất quyền và version bị kiểm dưới khóa. |
| ACCOUNT-2 | Trước INSERT audit SUCCESS, sau INSERT audit và trước INSERT outbox, trigger fixture lần lượt ép lỗi. SQL từng ca xác nhận target active/version trở về 1/0, outbox cho target bằng 0, chỉ còn FAILED của actor, target và correlation tương ứng, reason ROLLBACK, UTC, snapshot null. Retry UUID đã FAILED bị từ chối. UUID mới tạo đúng một SUCCESS. Replay thành công giữ một SUCCESS với mã lý do và hai cờ active. V7 unique theo correlation/action, đường ghi chỉ INSERT. SMTP diễn ra sau commit. [Kết quả SQL tổng hợp](sql.txt) |
| ACCOUNT-3 | Hai SHA-256 của thân thư trong test khớp độc lập với hai mẫu owner duyệt tại thời điểm mẫu 29/11/2026 00:05 giờ Việt Nam. SMTP loopback kiểm đúng To, subject, MIME UTF-8, Message-ID và nội dung đã giải mã. Dữ liệu legacy không tự sinh event khi migrate. OAuth chưa verified cho hai UNVERIFIED, dù callback có email. Khi địa chỉ verified thay đổi sau commit, worker gửi tới bản chụp ban đầu. Projection Admin chỉ đọc metadata thư. Email tài khoản đã có trên màn quản trị hiện hành; địa chỉ snapshot, raw SMTP, OTP và token không được thêm vào DTO, audit hoặc nhãn lỗi. |
| ACCOUNT-4 | V10 chỉ CREATE TABLE. V1–V9 khớp blob baseline. Hai FK user, unique operation, index theo trạng thái/lịch và target/ID. CHECK giới hạn số lần, trạng thái, địa chỉ nullable và lease. JPA validate khi Spring khởi động fixture fresh. V9 có user cũ nâng V10 và migrate lần hai giữ dữ liệu, không backfill thư. Worker riêng không thấy event chưa commit. Sau commit worker mới nhặt LOCK rồi UNLOCK theo thứ tự. Hai worker chỉ gọi gửi một lần. Đóng kết nối DB chuyên biệt ngay sau send khiến ghi SENT lỗi, hàng còn SENDING. Sau hết lease, worker restart chuyển UNKNOWN và không gửi lại. UNKNOWN chặn event sau cùng tài khoản. Bốn lần tối đa, các mốc retry 1/5/30 phút theo UTC. Relay 450, 550 và ngắt sau DATA lần lượt được kiểm là PENDING, FAILED và UNKNOWN. SENT chỉ xác nhận relay nhận. |
| ACCOUNT-5 | WebKit fixture kiểm mật khẩu, Google OIDC, GitHub OAuth, tab im lặng trong lúc khóa, phiên cũ sau mở và đăng nhập mới. Hai ca bổ sung bắt đầu authorization trước thay đổi và trả callback sau khi khóa/mở, kiểm hai endpoint bắt đầu liên kết/reauth với phiên đã thu hồi. Regression AUTH gồm đổi/reset mật khẩu và thu hồi version. Security chain tiếp tục không bật remember-me. SMTP lỗi không sửa active/version. WebKit AUTH, WebKit ACCOUNT đều là invocation bên trong suite OAuth2Callback, không cộng thêm hai lượt. |

### Nguồn và cách tái lập

Đối chiếu từng byte cả 20 file với hai checkout cũ và manifest trước FINAL. Maven cũ 257 PASS/113 SKIP. Fixture cũ 41 integration và 11 bootstrap PASS. Lượt FINAL dùng JDK 21 Temurin, Maven 3.9.11 offline, hai checkout riêng với source theo byte Git LF. Manifest cuối giữ hash từng file và hash vật lý của workspace Windows để phân biệt xuống dòng. Gửi thật và công cụ đối soát thủ công đều tắt. [Manifest trước](../source.json) · [Manifest cuối](source.json)

Chép source đúng manifest và pom vào hai checkout mới trong target. Chạy Maven test và package ở checkout build. Chạy harness ở PowerShell với checkout integration, Maven cache và playwright-core của WebKit local, thư mục evidence mới, tùy chọn `-VerifyAccountSql`. Suite FINAL gồm ThuTaiKhoan_MariaDb_Test, XacThuc_MariaDb_Test, OAuth2Callback_MariaDb_Test, Audit_MariaDb_Test, XacThucDaPhuongThuc_MariaDb_Test, PhieuGiamGia_MariaDb_Test, DonHangCOD_MariaDb_Test, Momo_MariaDb_Test, AdminOrderList_MariaDb_Test, DocView_MariaDb_Test và ThongKe_MariaDb_Test. Harness tạo datadir mới, chỉ dùng loopback, chạy bootstrap riêng và chỉ công bố log tổng hợp. [Harness](../run-fixture.ps1)

### Kết quả regression và các phương thức SKIP

Maven JDK 21 đạt **257 PASS / 121 SKIP**, không có failure/error. Integration **81/81 PASS** (gồm hai WebKit), bootstrap **11/11 PASS**, SMTP loopback **5/5 PASS** nằm trong Maven mặc định, package và diff check đạt. Đã kiểm COD, MoMo mock, quota, snapshot, mua lại, quyền truy cập theo phiên và thống kê khách. [Maven](maven.txt) · [Integration](integration.txt) · [Flyway](migration.txt) · [Package](package.txt)

Trong 113 phương thức SKIP trước FINAL, 43 có invocation PASS ở fixture ban đầu. Ở FINAL, 72 trong 113 phương thức cũ có invocation PASS. Tám phương thức fixture mới nâng số SKIP mặc định lên 121. Trong 121 phương thức, 80 có invocation PASS, 41 còn NOT RUN trong lượt FINAL. Tham số hóa có thể tạo nhiều invocation trên cùng một phương thức. [Danh sách theo từng phương thức, lý do và invocation](skip.json)

| Cờ fixture vắng ở Maven mặc định | Phương thức đã chạy ở fixture FINAL | NOT RUN |
|---|---:|---:|
| ACCOUNT_OUTBOX_DB_URL | 12 | 0 |
| ARCH_01_BSON_PHASE | 0 | 1 |
| AUTH_02_MIGRATION_FIXTURE | 0 | 1 |
| LOCAL_BOOTSTRAP_MIGRATION_FIXTURE | 3 | 0 |
| P01_MARIADB_TEST | 65 | 29 |
| P21_DEMO_REPORT | 0 | 1 |
| P23_MONGO_TEST | 0 | 9 |

Tại điểm cuối của fixture outbox, 17 SUCCESS có đủ 17 event, 4 FAILED không có event cùng correlation. Schema thực có 2 FK, 1 unique ngoài PK và 5 CHECK. Hash SHA-256 source/test/config/migration của bản FINAL là `8756d38832cd5a6da4e5332a199d89ea965ff7200ea7b0e00eead183b3c8dd84`.

### Gate tiếp theo

Owner review candidate trước commit và push. GitHub CI của ACCOUNT cần chạy trên commit thích hợp, sau đó cần merge và quyết định nghiệm thu owner. Phiên này chỉ đọc được tracking ref local. Lệnh kiểm remote live bị chặn kết nối tới github.com:443. Owner cần đối chiếu main lần nữa ngay trước khi xuất bản.

SMTP production và inbox thật NOT RUN. Gửi thật chờ owner xác minh kênh liên hệ công khai cho người bị khóa. UNKNOWN cần bằng chứng relay và người vận hành do owner phân công. Chưa mở đối soát thủ công hoặc dọn dữ liệu outbox. Manual UAT, tải production và provider thật NOT RUN. AUTH-LIVE, PAY-LIVE và AUDIT PURGE tiếp tục BLOCKED_EXTERNAL.
