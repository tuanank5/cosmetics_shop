# 05-ACCOUNT · Khóa/mở tài khoản có lý do và thông báo

**IN_PROGRESS · 28/11/2026.** Baseline `5aa4932cc663de1115ff06ab7a8d7b837a673d93`. Owner duyệt ACCOUNT-1–5 để triển khai trên fixture. Tiến độ dự án 8/19 DONE. [Quyết định](../DECISIONS.md#account--quyết-định-owner-ngày-28112026) · [Hai mẫu thư đã duyệt](ACCOUNT-owner-decisions-proposal.md)

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

## Kiểm chứng

JDK 21, Maven offline, hai bản sao source trong workspace. Hash tổng hợp của 20 tệp source, cấu hình, migration và test thay đổi là `0e4016177085a141ab10f5b8a91b4a11c6fde72a62062cda5f96105e8d5e079c`. Toàn bộ cây `src` ở hai bản build/fixture được đối chiếu với working tree. [Manifest từng tệp](../qa/ACCOUNT/source.json)

| Kiểm chứng | Kết quả |
|---|---|
| Maven mặc định | 257 PASS, 113 SKIP, 0 lỗi. Gồm năm ca SMTP loopback với relay kiểm soát được phản hồi. Hai thư được so nội dung đã giải mã và Message-ID. Kiểm 450, 550 và mất kết nối sau DATA. [Maven](../qa/ACCOUNT/maven.txt) |
| Package | PASS, `mvn -o -DskipTests package`. [Package](../qa/ACCOUNT/package.txt) |
| MariaDB · ACCOUNT, AUTH, AUDIT | 41/41 PASS, gồm hai lượt WebKit. Đối chiếu SQL cho replay đồng thời, hai Admin, rollback rồi FAILED, email chưa xác minh, địa chỉ gốc, lease hết hạn, hai worker, giới hạn retry và UNKNOWN. [Integration](../qa/ACCOUNT/integration.txt) |
| WebKit trong suite integration | PASS qua UI Admin, mật khẩu, callback Google OIDC và GitHub mô phỏng, chặn khi khóa, phiên im lặng vẫn bị thu hồi sau mở, login mới thành công. Regression AUTH WebKit cũng PASS. |
| Flyway/MariaDB | 11/11 PASS trên fixture upgrade và bảo vệ bootstrap, gồm V9→V10 với user cũ, không tự tạo event, chạy lại migration. Spring integration khởi tạo V1→V10 trên schema trống với JPA validate. [Migration](../qa/ACCOUNT/migration.txt) |
| Kiểm tra diff | PASS. Các suite có giao nhau; giữ riêng kết quả mặc định, fixture và browser. |

**Gate còn mở.** SMTP thật, inbox, manual UAT và OAuth provider thật chưa chạy. Đối soát vận hành thực tế chờ người được phân công. Chuẩn bị ACCOUNT-FINAL để review source, migration, bằng chứng và phạm vi trước khi tạo candidate. CI, merge và quyết định nghiệm thu ACCOUNT chưa có.

### Tái lập trên fixture

Dùng JDK 21, MariaDB fixture, Maven cache đã có dependency và playwright-core tương thích WebKit đã cài. Sao chép `pom.xml` và `src/` đúng manifest sang hai thư mục mới dưới `target/`. Chạy `mvn test` và `mvn -DskipTests package` ở bản build. Ở PowerShell từ root, chạy harness trong process riêng với `-Checkout` trỏ bản sao thứ hai, `-MavenRepo` trỏ cache và `-PlaywrightCore` trỏ package local. Harness tạo datadir mới và schema riêng trên loopback, tắt worker thực, dùng OAuth fixture và dừng đúng tiến trình MariaDB của lượt chạy. [Harness](../qa/ACCOUNT/run-fixture.ps1) · [WebKit](../qa/ACCOUNT/browser.cjs)

## ACCOUNT-FINAL · 28/11/2026

**IN_PROGRESS · Sẵn sàng trình owner review candidate local.** Hai ca fixture tái hiện worker ghi SENT sau hạn lease và service gộp no-op với form cũ. Bản sửa kiểm hạn lease trong UPDATE và trả riêng kết quả KHONG_DOI. Ba điểm rollback, replay, cạnh tranh giữa Admin, callback đến muộn, worker restart và mất kết nối DB sau SMTP đều đã được kiểm trên fixture.

JDK 21 Maven 257 PASS/121 SKIP, integration 81/81 PASS (gồm hai WebKit), bootstrap 11/11 PASS, package PASS. Có 41 phương thức ngoài tập fixture FINAL chưa chạy. Source FINAL `8756d38832cd5a6da4e5332a199d89ea965ff7200ea7b0e00eead183b3c8dd84`. SMTP production và đối soát thủ công chưa mở. GitHub CI, merge và nghiệm thu đúng revision vẫn còn chờ. [Báo cáo FINAL và phân loại SKIP](../qa/ACCOUNT/final-review/review.md)
