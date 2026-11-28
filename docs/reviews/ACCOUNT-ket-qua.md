# 05-ACCOUNT · Khóa/mở tài khoản có lý do và thông báo

**DONE trong phạm vi fixture.** Ngày 28/11/2026, owner nghiệm thu ACCOUNT-1–5 trên source `610c816` và hồ sơ closeout `d7bc6e0`. Tiến độ dự án **9/19 DONE**. [Quyết định nghiệm thu](../DECISIONS.md#nghiệm-thu-account-ngày-28112026)

**Mốc triển khai ngày 28/11/2026.** Baseline `5aa4932cc663de1115ff06ab7a8d7b837a673d93`. Owner duyệt ACCOUNT-1–5 để triển khai trên fixture. Tiến độ dự án 8/19 DONE. [Quyết định](../DECISIONS.md#account--quyết-định-owner-ngày-28112026) · [Hai mẫu thư đã duyệt](ACCOUNT-owner-decisions-proposal.md)

## Hành vi đã triển khai

| Quyết định | Đường chạy và kết quả cần quan sát |
|---|---|
| ACCOUNT-1 | Form SSR có CSRF và một mã thao tác do server cấp, gắn với phiên, Admin, đích, chiều chuyển và phiên bản lúc đọc. Service khóa actor/đích theo ID, kiểm lại quyền, phiên và lý do. Hai Admin dùng hai form trên cùng trạng thái chỉ có một thay đổi. Gửi lại đúng thao tác đã commit trả biên nhận. Đổi lý do trên biên nhận hoặc gửi form cũ yêu cầu tải lại. Form hết phiên hoặc bị đẩy khỏi bộ đệm 256 form gần nhất cũng phải tải lại. |
| ACCOUNT-2 | SUCCESS cho ACCOUNT_LOCK/ACCOUNT_UNLOCK cùng giao dịch với user và outbox. Snapshot chỉ có boolean. Audit nhận mã lý do, actor/target và correlation. Rollback làm mất SUCCESS/outbox và ghi FAILED ở giao dịch riêng theo D7. POST bị chặn trước khi đổi và replay không tạo thêm SUCCESS. |
| ACCOUNT-3 | Danh sách Admin có lý do và kết quả mail mới nhất. Chi tiết hiển thị tối đa 50 lần gần nhất, gồm actor, thời gian UTC, lý do, số lần gửi và correlation. Projection outbox cho Admin không chọn địa chỉ nhận. Hai thư dùng nội dung owner duyệt và thời gian Việt Nam. UNVERIFIED thể hiện chưa gửi. |
| ACCOUNT-4 | V10 bổ sung một bảng, unique mã thao tác, FK actor/đích và index cho hàng đợi. Event giữ nguyên địa chỉ đã xác minh khi tạo. Worker chỉ claim event đã commit, theo thứ tự từng tài khoản. Cập nhật kết quả phải khớp lease. Message-ID cố định theo event để tra cứu relay. SMTP 4xx xác định chưa nhận hoặc kết nối bị từ chối cho phép retry, tối đa bốn lần, giãn 1/5/30 phút. Hết lượt hoặc bị từ chối vĩnh viễn chuyển FAILED. Timeout chưa rõ hoặc hết lease chuyển UNKNOWN. UNKNOWN chặn thư kế tiếp của cùng tài khoản, không chặn nghiệp vụ khóa/mở. |
| ACCOUNT-5 | Tận dụng active/version của AUTH. Password, Google và GitHub fixture đều phải đăng nhập mới sau mở. Phiên cũ bị loại ở request động tiếp theo. Service đổi trạng thái kiểm phiên Admin dưới khóa ngay trong transaction. |

Migration không tạo thư hồi tố và không thay đổi V1–V9. Tài khoản cũ chưa có event hiển thị “Chưa ghi nhận thao tác”. Các dữ liệu người nhận trong bảng outbox chỉ dùng cho vận chuyển thư; bảo vệ quyền truy cập DB/backup như với email của tài khoản. Log và bằng chứng công bố chỉ chứa kết quả tổng hợp.

## Vận hành và giới hạn nghiệm thu

- Worker tắt mặc định bằng `app.account.mail-enabled=false`. Nghiệp vụ vẫn ghi PENDING cho email verified. Gửi thật chờ owner kiểm kênh liên hệ footer từ phiên Guest/người bị khóa và xác nhận người trực hỗ trợ. Trước khi mở gửi cần rà các thư đang chờ. SENT chỉ là relay đã tiếp nhận.
- Đối soát thủ công **chưa mở**, chờ owner phân công người vận hành. Với UNKNOWN, giữ nguyên hàng và đối chiếu Message-ID dạng `<correlation@account.beautyshop.invalid>`, thời điểm UTC và biên nhận relay. Không tự chuyển thành PENDING, gửi lại hoặc đảo trạng thái tài khoản. Chưa xác minh phát đến inbox, tải production hoặc độ trễ hàng đợi production.
- Nếu cần dừng vận chuyển thư, tắt worker và giữ nguyên outbox. Khi quay lại tiếp tục từ hàng đã commit, coi lease hết hạn là UNKNOWN. Nếu lùi binary về trước ACCOUNT thì đường khóa/mở cũ sẽ thiếu audit và outbox, cần tạm dừng thao tác Admin này trong thời gian lùi. Không sửa checksum hoặc xóa lịch sử.
- AUTH-LIVE, PAY-LIVE và AUDIT PURGE tiếp tục BLOCKED_EXTERNAL. Không có thao tác vận hành NOTIFY hoặc marketing trong phạm vi ACCOUNT.

## Kiểm chứng trước ACCOUNT-FINAL

JDK 21, Maven offline, hai bản sao source trong workspace. Hash tổng hợp của 20 tệp source, cấu hình, migration và test thay đổi là `0e4016177085a141ab10f5b8a91b4a11c6fde72a62062cda5f96105e8d5e079c`. Toàn bộ cây `src` ở hai bản build/fixture được đối chiếu với working tree. [Manifest từng tệp](../qa/ACCOUNT/source.json)

| Kiểm chứng | Kết quả |
|---|---|
| Maven mặc định | 257 PASS, 113 SKIP, 0 lỗi. Gồm năm ca SMTP loopback với relay kiểm soát được phản hồi. Hai thư được so nội dung đã giải mã và Message-ID. Kiểm 450, 550 và mất kết nối sau DATA. [Maven](../qa/ACCOUNT/maven.txt) |
| Package | PASS, `mvn -o -DskipTests package`. [Package](../qa/ACCOUNT/package.txt) |
| MariaDB · ACCOUNT, AUTH, AUDIT | 41/41 PASS, gồm hai lượt WebKit. Đối chiếu SQL cho replay đồng thời, hai Admin, rollback rồi FAILED, email chưa xác minh, địa chỉ gốc, lease hết hạn, hai worker, giới hạn retry và UNKNOWN. [Integration](../qa/ACCOUNT/integration.txt) |
| WebKit trong suite integration | PASS qua UI Admin, mật khẩu, callback Google OIDC và GitHub mô phỏng, chặn khi khóa, phiên im lặng vẫn bị thu hồi sau mở, login mới thành công. Regression AUTH WebKit cũng PASS. |
| Flyway/MariaDB | 11/11 PASS trên fixture upgrade và bảo vệ bootstrap, gồm V9→V10 với user cũ, không tự tạo event, chạy lại migration. Spring integration khởi tạo V1→V10 trên schema trống với JPA validate. [Migration](../qa/ACCOUNT/migration.txt) |
| Kiểm tra diff | PASS. Các suite có giao nhau; giữ riêng kết quả mặc định, fixture và browser. |

**Gate tại thời điểm trước FINAL.** SMTP thật, inbox, manual UAT và OAuth provider thật chưa chạy. Đối soát vận hành thực tế chờ người được phân công. Chuẩn bị ACCOUNT-FINAL để review source, migration, bằng chứng và phạm vi trước khi tạo candidate. CI, merge và quyết định nghiệm thu ACCOUNT chưa có.

### Tái lập trên fixture

Dùng JDK 21, MariaDB fixture, Maven cache đã có dependency và playwright-core tương thích WebKit đã cài. Sao chép `pom.xml` và `src/` đúng manifest sang hai thư mục mới dưới `target/`. Chạy `mvn test` và `mvn -DskipTests package` ở bản build. Ở PowerShell từ root, chạy harness trong process riêng với `-Checkout` trỏ bản sao thứ hai, `-MavenRepo` trỏ cache và `-PlaywrightCore` trỏ package local. Harness tạo datadir mới và schema riêng trên loopback, tắt worker thực, dùng OAuth fixture và dừng đúng tiến trình MariaDB của lượt chạy. [Harness](../qa/ACCOUNT/run-fixture.ps1) · [WebKit](../qa/ACCOUNT/browser.cjs)

## ACCOUNT-FINAL · 28/11/2026

**IN_PROGRESS · Sẵn sàng trình owner review candidate local.** Hai ca fixture tái hiện worker ghi SENT sau hạn lease và service gộp no-op với form cũ. Bản sửa kiểm hạn lease trong UPDATE và trả riêng kết quả KHONG_DOI. Ba điểm rollback, replay, cạnh tranh giữa Admin, callback đến muộn, worker restart và mất kết nối DB sau SMTP đều đã được kiểm trên fixture.

JDK 21 Maven 257 PASS/121 SKIP, integration 81/81 PASS (gồm hai WebKit), bootstrap 11/11 PASS, package PASS. Có 41 phương thức ngoài tập fixture FINAL chưa chạy. Source FINAL `8756d38832cd5a6da4e5332a199d89ea965ff7200ea7b0e00eead183b3c8dd84`. SMTP production và đối soát thủ công chưa mở. GitHub CI, merge và nghiệm thu đúng revision vẫn còn chờ. [Báo cáo FINAL và phân loại SKIP](../qa/ACCOUNT/final-review/review.md)

## Closeout sau PR #17

**Tại thời điểm closeout**, ACCOUNT ở trạng thái MERGED / READY_FOR_OWNER_ACCEPTANCE và dự án đạt 8/19 DONE. Merge `610c816b5bd7f7bf924f80a62cec4eba0f7c59bb` có hai parent là baseline `5aa4932cc663de1115ff06ab7a8d7b837a673d93` và candidate `a656e6e7f4480117873d89d6fab0fb1a2be9f435`. HEAD và tracking ref origin/main cùng trỏ đến merge. Owner đã xác minh PR merged và run push/main completed/success trên SHA này. Truy vấn GitHub trực tiếp từ môi trường closeout bị chặn. Workflow trên tree này chạy Maven test và package với JDK 21. [PR #17](https://github.com/tuanank5/cosmetics_shop/pull/17) · [CI main](https://github.com/tuanank5/cosmetics_shop/actions/runs/46152490413)

Candidate và merge có cùng tree `dfeca3f664dcd32bf8b53f8068fe6120ab987f32`. Đã đối chiếu tên, Git blob và SHA-256 của đủ 45 file thay đổi với manifest FINAL. Hash tổng hợp tính lại từ 20 blob source/test/migration/config theo Git LF là `8756d38832cd5a6da4e5332a199d89ea965ff7200ea7b0e00eead183b3c8dd84`. V1–V9 giữ nguyên từng blob. V10 tạo bảng outbox với hai FK, unique mã thao tác và năm CHECK. Fixture nâng V9 lên V10 giữ user cũ và không tạo thư hồi tố. Cả 45 file cũng khớp stash ACCOUNT tìm theo tên. Index gốc trống và 12 stash được bảo toàn. [Đối chiếu từng file](../qa/ACCOUNT/closeout/verification.json)

| Quyết định | Kết quả trên tree đã merge |
|---|---|
| ACCOUNT-1 | POST cần CSRF và ROLE_ADMIN. UUID do server cấp gắn với phiên, actor, target, chiều thao tác và version. Service kiểm lại quyền Admin dưới khóa, chặn tự khóa và kiểm mã lý do đúng chiều. Hai request cùng UUID chỉ ghi một lần. Finding về hai Admin đã sửa: đích đã ở trạng thái yêu cầu trả KHONG_DOI, form cũ sau một vòng khóa/mở trả FORM_CU, replay đã commit trả DA_GHI. |
| ACCOUNT-2 | SQL ghi 14 LOCK và 3 UNLOCK thành công, mỗi thao tác có một event. Cả 4 FAILED đều không có event cùng correlation. Ba điểm ép rollback khôi phục active/version về 1/0 và loại bỏ SUCCESS cùng outbox. FAILED ghi riêng sau rollback. Audit chỉ giữ định danh, action, UTC, mã lý do, kết quả và hai cờ trạng thái. [SQL fixture](../qa/ACCOUNT/final-review/sql.txt) |
| ACCOUNT-3 | Đối chiếu lại SHA-256 của hai thân thư đã duyệt với assertion MIME UTF-8 ở giờ mẫu Việt Nam. Fixture xác nhận đúng tiêu đề, người nhận và nội dung. Địa chỉ verified được chụp lúc tạo event và giữ qua retry. Địa chỉ chưa xác minh cho trạng thái UNVERIFIED. SENT xác nhận relay đã tiếp nhận. |
| ACCOUNT-4 | Worker claim sau commit và giữ thứ tự theo tài khoản. Finding về worker trả muộn đã sửa bằng điều kiện SENDING, lease token và hạn UTC trong UPDATE. Worker hết lease không ghi được SENT. Nếu mất kết nối DB sau SMTP, poller chuyển lease hết hạn sang UNKNOWN và chặn gửi tiếp cho tài khoản đó. Chỉ retry lỗi tạm thời có bằng chứng relay chưa tiếp nhận, sau 1, 5 và 30 phút, tối đa bốn lần gửi. |
| ACCOUNT-5 | Password, Google OIDC và GitHub OAuth fixture kiểm phiên đang hoạt động, phiên im lặng xuyên khóa/mở, callback đến muộn và đăng nhập mới sau mở khóa. Giao dịch Admin kiểm lại active/version. Lỗi SMTP giữ nguyên trạng thái tài khoản. |

Kế thừa lượt FINAL trên tree trùng khớp. Maven **257 PASS / 121 SKIP**, gồm **5/5 SMTP loopback**. MariaDB **81/81 PASS**, gồm WebKit ACCOUNT và AUTH. Flyway/bootstrap **11/11 PASS**. Package và kiểm tra diff đạt. Trong 121 phương thức SKIP mặc định, 80 có invocation PASS trên fixture và 41 còn NOT RUN. Closeout đối chiếu log và hash, không chạy lại các suite. [Log và phân loại từng phương thức](../qa/ACCOUNT/final-review/review.md)

**Giới hạn nghiệm thu.** SMTP production tiếp tục tắt. Gửi thật chờ owner xác nhận kênh hỗ trợ ở footer hoạt động và người bị khóa truy cập được. Đối soát email thủ công **BLOCKED_OWNER** cho đến khi owner phân công người vận hành. OAuth provider thật, manual UAT và tải production **NOT RUN**. AUTH-LIVE, PAY-LIVE và AUDIT PURGE giữ **BLOCKED_EXTERNAL**. Owner giữ các giới hạn này khi nghiệm thu ACCOUNT.
