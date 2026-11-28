# AUTH · Mật khẩu và liên kết danh tính · 28/11/2026

**IN_PROGRESS · AUTH-FINAL đạt review và regression local.** Nhánh local `qa/auth` trong clone riêng xuất phát từ `f390c3adb9ff2180638a425225007e871d41b6f0`. SHA này là BASE đã nghiệm thu. Bản AUTH hiện nằm ở index, chưa có SHA commit riêng. Gate còn lại là CI và owner nghiệm thu đúng commit. [Phạm vi staged](../qa/AUTH/final-review/scope.txt) · [Manifest mã đã chạy](../qa/AUTH/final-review/source.json)

## Security review cuối

| Phạm vi | Bằng chứng và xử lý |
|---|---|
| Phiên mang quyền reset | Khi nhận link hợp lệ, controller trước đây lưu quyền reset vào phiên đang có mà chưa đổi ID. Đã đổi ID trước khi lưu token. Đã che giá trị khi chuyển đối tượng token trong phiên thành chuỗi. WebKit so sánh ID trước/sau bằng giá trị boolean, giữ được luồng nhập lại rồi reset thành công. |
| Tiêu thụ OAuth state | Đã đọc bytecode phiên bản Spring Security trong cache build. Repository của Spring đọc request rồi xóa thuộc tính session qua hai lời gọi riêng. Đã dùng chung mutex của phiên cho ghi và lấy/xóa request. Unit test với hai consumer của cùng request xác nhận chỉ một lần lấy được state đã lưu. Callback tuần tự kiểm thêm state sai và state đã dùng. |
| Reset, log và rate limit | `SecureRandom` sinh 32 byte. DB lưu SHA-256. TTL 900 giây, khóa hàng Customer, kiểm lại token và version, đổi hash, tiêu thụ và thu hồi phiên trong một giao dịch. Redis giới hạn nguồn và email. SQL áp khoảng chờ cấp lại. Endpoint công khai trả cùng thông báo và xử lý gửi bất đồng bộ. Service mail gửi link trong thư, các nhánh log lỗi của ứng dụng chỉ ghi loại lỗi hoặc thông báo cố định. Token thô tồn tại tạm trong thư và phiên để hoàn tất form. Access log của hạ tầng ngoài repo chưa được kiểm. |
| Quyền sở hữu và xác thực lại | Endpoint bảo mật lấy ID từ principal. Grant gắn với ID, version và hạn 5 phút. Proof kiểm thêm provider và mục đích liên kết hoặc reauth. Google kiểm OIDC, issuer Google và email đã xác minh. GitHub đọc ID tại user-info và email primary/verified. V7 ràng buộc duy nhất theo provider/subject và Customer/provider. Không gộp qua email trùng. Các POST theo session dùng CSRF. Admin tiếp tục dùng form login và route theo vai trò. |
| Hai yêu cầu gỡ cùng lúc | Hai phiên của cùng Customer OAuth-only xác thực lại qua hai provider khác nhau và OTP, sau đó gửi đồng thời hai POST gỡ đối nghịch. Chỉ một POST thành công. DB còn đúng danh tính đã chứng minh, version tăng một và audit có một lần gỡ. |
| Google và logout | Hai lần bấm đăng nhập gửi `select_account`. Liên kết và reauth giữ tham số mặc định. WebKit xác nhận ID phiên đổi sau login, cả hai tab mất quyền sau logout và chỉ đăng nhập lại qua callback mới. Handler logout chỉ kết thúc phiên BeautyShop. |
| Dữ liệu | Diff so với BASE không thay migration hoặc schema. Suite dùng datadir mới trên loopback và kiểm lại hai ca V5/V6 có dữ liệu lên V7, giữ ID và danh tính legacy đủ bằng chứng. |

### Regression và số SKIP

| Lượt | Maven mặc định | Suite có fixture |
|---|---|---|
| BASE đã nghiệm thu | 231 PASS, 82 SKIP | Ma trận BASE được nghiệm thu riêng tại PR #6 |
| AUTH sau D1–D3 | 233 PASS, 87 SKIP | 39/39 PASS |
| AUTH-FINAL | **234 PASS, 88 SKIP**, 0 fail/error | **40/40 PASS**, 0 skip, có WebKit 26.6 |

Từ 82 lên 87 có đúng năm phương thức mới dưới điều kiện `P01_MARIADB_TEST`. Một ca kiểm WebKit hai tab. Bốn ca ở lớp xác thực đa phương thức kiểm hai Customer tranh cùng identity, đổi mật khẩu trùng rồi nhập lại, cấp link thay thế/hết hạn, và từ chối reset qua Admin cho OAuth-only. Lượt mặc định thiếu biến này nên JUnit đánh dấu SKIP ở cấp lớp. Phương thức liên kết nay có hai tham số Google/GitHub vẫn tính một mục khi lớp bị bỏ qua, hai lượt khi fixture bật. Lượt cuối bổ sung một ca gỡ đồng thời nên thành 88 SKIP và một unit test state nên có 234 PASS. Không có ca cũ mất khỏi danh sách SKIP khi đối chiếu theo tên phương thức. Cả sáu ca bổ sung theo điều kiện đều PASS với fixture. Hai suite chồng lặp ở unit test mật khẩu. [Đối chiếu lý do SKIP](../qa/AUTH/final-review/skips.json) · [Maven](../qa/AUTH/final-review/test.txt) · [40 ca](../qa/AUTH/final-review/cases.json)

JDK 21 chạy `mvn -o -B -ntp test` và `mvn -o -B -ntp -DskipTests package` trong clone QA. Runner chạy suite chọn lọc trên bản sao khớp mã nguồn với MariaDB 12.2 mới và WebKit. Package và diff check đạt. Google OIDC và GitHub dùng fixture, SMTP/OTP/Redis dùng mock trong các ca liên quan. Provider thật, cấu hình log proxy triển khai và AUTH-LIVE là **NOT RUN**. Xuất bản/CI **chờ quyền owner**. Workflow GitHub hiện chạy Maven mặc định và package, nên ma trận MariaDB/WebKit local vẫn là bằng chứng riêng. [Log tích hợp](../qa/AUTH/final-review/integration.txt) · [SHA-256 JAR](../qa/AUTH/final-review/package.txt)

### Nhánh QA và lệnh cho owner

Clone QA nằm ở `target/qa-auth` trong workspace. Đã chọn mã AUTH, fixture và bằng chứng rút gọn, các quyết định và phần sổ tiến độ liên quan. Bản sổ trong clone chỉ mang các thay đổi cần cho AUTH cùng xác nhận BASE làm đầu vào. Các báo cáo, backup và tài liệu khác còn ở workspace chính. [Danh sách file](../qa/AUTH/final-review/scope.txt) · [Quét index](../qa/AUTH/final-review/scan.json)

Sau khi duyệt staged diff, chạy PowerShell từ root workspace. Lệnh tạo draft PR dùng GitHub CLI đã đăng nhập. Nếu push bị từ chối, cần đối chiếu nhánh remote trước khi tiếp tục.

```powershell
Set-Location -LiteralPath '.\target\qa-auth'
if ((git branch --show-current) -ne 'qa/auth') { throw 'Sai nhanh QA' }
if ((git rev-parse HEAD) -ne 'f390c3adb9ff2180638a425225007e871d41b6f0') { throw 'Sai revision goc' }
git diff --cached --check
if ($LASTEXITCODE -ne 0) { throw 'Staged diff check failed' }
python docs/qa/AUTH/final-review/verify-index.py
if ($LASTEXITCODE -ne 0) { throw 'Index scan failed' }
git diff --cached --stat
git commit -m "feat(auth): secure password reset and OAuth account management"
if ($LASTEXITCODE -ne 0) { throw 'Commit failed' }
git rev-parse HEAD
git push --set-upstream origin qa/auth
if ($LASTEXITCODE -ne 0) { throw 'Push failed' }
gh pr create --repo tuanank5/cosmetics_shop --base main --head qa/auth --draft --title "AUTH: Secure password reset and OAuth account management" --body-file docs/qa/AUTH/final-review/pull-request.md
```

## Kết quả sau quyết định của owner

Với JDK 21, `mvn -o -B -ntp test` ghi nhận **233 PASS, 87 SKIP, 0 lỗi**. Suite riêng dùng MariaDB mới trên loopback và WebKit 26.6 đạt **39/39**, gồm cả hai unit test mật khẩu. `mvn -o -B -ntp -DskipTests package` và `git diff --check` đều đạt. Hai suite có phạm vi giao nhau. [Log Maven](../qa/AUTH/owner-d1-d3/test.txt) · [Tích hợp và WebKit](../qa/AUTH/owner-d1-d3/integration.txt) · [JAR SHA-256](../qa/AUTH/owner-d1-d3/package.txt) · [39 kết quả Surefire theo tên ca](../qa/AUTH/owner-d1-d3/cases.json)

| Mục owner | Kết quả đã quan sát |
|---|---|
| 1–2 · Thời hạn, cấp lại | PASS. Bản ghi có TTL 900 giây và chứa SHA-256 của token. Trong khoảng chờ gửi lại, yêu cầu thứ hai không gửi thư. Sau khoảng chờ, link mới còn hiệu lực và link trước bị từ chối. Link quá hạn bị từ chối ở cả bước mở và ghi. Chỉ cập nhật đồng hồ dữ liệu của tài khoản fixture. |
| 3–5 · Replay, mật khẩu hiện tại, phiên cũ | PASS. Mật khẩu sai chính sách hoặc trùng mật khẩu hiện tại cho thử lại bằng cùng link. Hai lần reset đồng thời chỉ một lần ghi thành công. Replay link đã dùng bị từ chối. Phiên đăng nhập từ trước reset mất quyền trên server. |
| 6–8 · Liên kết và va chạm | PASS. Cả Google và GitHub hoàn tất liên kết qua reauth, callback và POST xác nhận, ghi đúng Customer kể cả khi hai email khác nhau. Email trùng của identity chưa liên kết không cấp quyền vào Customer hiện có. Hai Customer tranh cùng identity chỉ một bên được ghi và tăng phiên bản. |
| 9–10 · Gỡ, thay đổi khi thiếu bằng chứng | PASS. Hệ thống từ chối liên kết/gỡ thiếu reauth và từ chối gỡ phương thức khả dụng cuối cùng. OAuth-only cần callback đã liên kết và OTP để đặt mật khẩu đầu tiên. Admin yêu cầu reset cho OAuth-only có email đã xác minh cũng không tạo link. |
| 11 · Bộ chọn Google | PASS trên fixture. WebKit thực hiện hai lượt bấm đăng nhập Google và một lượt reauth. Fixture nhận lần lượt `select_account`, `select_account`, rồi không có `prompt`. Callback đăng nhập có state và nonce. Yêu cầu liên kết Google và yêu cầu GitHub giữ tham số mặc định. |
| 12 · Logout đa tab | PASS. Cùng browser context, tab Orders tải lại sau logout phải đăng nhập. Back rồi tải từ server và GET trực tiếp account/orders cũng mất quyền. Lượt bấm mới và callback mới cho truy cập trở lại. |
| 13–14 · CSRF, phân quyền, đồng thời, regression | PASS. POST thiếu CSRF, sai vai trò, callback sai state hoặc dùng lại state đều bị từ chối trong các ca tương ứng. Kiểm ba phương thức cùng ID, ownership của đơn, gộp giỏ một lần, phiên bản xác thực, SMTP lỗi, migration tài khoản V5/V6 lên V7 và các ca COD/MoMo liên quan. |

**Phạm vi provider.** WebKit và Spring Boot chạy thật trên loopback. Google dùng OIDC fixture ký JWT, GitHub dùng token endpoint fixture và user-info mô phỏng. Email, OTP và Redis trong luồng này dùng mock. Trải nghiệm chọn tài khoản, consent và callback trên Google/GitHub thật chưa chạy. AUTH-LIVE chờ owner giao riêng.

**Khôi phục khi mất OAuth.** Dùng phương thức còn lại để đăng nhập, xác thực lại rồi gỡ phương thức đã mất. Customer đã có mật khẩu và email BeautyShop đã xác minh có thể yêu cầu reset. Nếu chỉ còn một phương thức OAuth và đã mất quyền tại đó, cần khôi phục quyền tại chính nhà cung cấp. Admin chỉ yêu cầu gửi link cho Customer đủ điều kiện. Cấu trúc V7 đáp ứng D1–D3. [Hợp đồng AUTH-D2](../DECISIONS.md)

## Hành vi đã triển khai

- Tại `POST /reset-password` và `POST /account/security/password`, service khóa tài khoản và kiểm quyền hoặc hiệu lực token trước khi so mật khẩu mới với hash hiện tại bằng BCrypt. Hai ô phải khớp, đạt ít nhất 12 ký tự Unicode và tối đa 72 byte UTF-8. Một emoji ngoài BMP tính là một ký tự.
- Khi trùng mật khẩu, người dùng thấy thông báo yêu cầu chọn mật khẩu khác. Form reset cho nhập lại trong thời hạn còn lại của cùng link. Việc từ chối diễn ra trước khi băm mới, tiêu thụ token, tăng phiên bản phiên hoặc ghi audit thành công. Token chỉ được tiêu thụ cùng transaction đổi mật khẩu thành công.
- Với Customer OAuth chưa có hash, lần đặt đầu tiên dùng proof của danh tính đã liên kết và email đã xác minh. Hai controller truyền cả phần xác nhận xuống service; hợp đồng HTTP, schema và các FK giữ nguyên. Các tệp ứng dụng gồm `MatKhau_Service`, `MatKhauMoi_Form`, `DatLaiMatKhau_Controller` và `BaoMatTaiKhoan_Controller`.

## Checkpoint trước khi owner chốt D1–D3

Maven mặc định ghi nhận 318 ca, gồm 233 PASS, 85 SKIP và 0 lỗi. Lượt chạy riêng với MariaDB và WebKit 26.6 đạt 36/36, gồm cả hai unit test mật khẩu chạy lại. Đóng gói với `-DskipTests package` đạt. [Log Maven](../qa/AUTH/test.txt) · [Log MariaDB và WebKit](../qa/AUTH/integration.txt) · [Kết quả đóng gói và SHA-256 của JAR](../qa/AUTH/package.txt)

| Phép kiểm | Điều kiện và kết quả cần đối chiếu |
|---|---|
| Unit | BCrypt với hash null, khớp và khác mật khẩu hiện tại; hai ô khác nhau; giới hạn code point và byte UTF-8, gồm đúng 72 byte. |
| MariaDB | Hai lượt reset đồng thời chỉ một lượt thành công. Link vẫn sử dụng được sau khi từ chối mật khẩu cũ. Đổi trong tài khoản từ chối rồi cho thử lại ở cùng phiên. Hai Customer tranh chấp cùng subject chỉ một chủ được gắn; giao dịch thua giữ version và không có audit liên kết thành công. |
| Regression | Đăng nhập form, GitHub và Google cùng ID; gộp giỏ một lần; phiên cũ bị thu hồi; POST thiếu CSRF; email trùng chưa liên kết; lỗi gửi mail; di trú V5/V6 lên V7. Chạy thêm các ca COD và MoMo mock trong suite liên quan. |
| WebKit | Chạy form thật trên Spring Boot với cổng loopback, MariaDB riêng và Google OIDC fixture ký JWT. Reset trùng → chọn lại → dùng lại link đã tiêu; đăng nhập Google → logout → tab Orders reload → Back/reload → GET account/orders đều bị chặn. Sau lượt bấm Google mới, callback cho truy cập lại. Thử reauth Google + OTP, gỡ phương thức cuối bị chặn, đặt mật khẩu đầu tiên thành công. Test kiểm đúng ba lần tới endpoint ủy quyền mô phỏng (login, login lại, reauth) và đọc lại chủ sở hữu trong DB. [Kịch bản WebKit](../qa/AUTH/browser.cjs) |

**Kết luận về Google.** Trong fixture, phiên BeautyShop mất quyền sau logout. Người dùng có thể quay lại cùng Customer sau một callback mới. Kết quả phù hợp với khả năng phiên SSO của Google vẫn còn. Hành vi consent hoặc chọn tài khoản trên Google thật, cửa sổ riêng tư và nhiều tài khoản cần môi trường AUTH-LIVE. Mail, OTP/Redis và các provider trong suite này dùng fixture hoặc mock. [Bằng chứng tích hợp](../qa/AUTH/integration.txt)

## Chạy lại và bàn giao

Dùng Temurin 21.0.13 và Maven 3.9.11 với cache offline. Từ checkout chứa working diff đã đối chiếu, chạy `mvn -o -B -ntp test` và `mvn -o -B -ntp -DskipTests package` với `-Dmaven.repo.local` chỉ đến cache. Truyền checkout và cache vào `run-mariadb.ps1 -Checkout … -MavenRepo …`; biến `BASE_PLAYWRIGHT` trỏ tới playwright-core có WebKit. Runner tạo một data directory mới, tự dừng MariaDB sau suite và ghi các dòng tổng hợp. [Runner](../qa/AUTH/run-mariadb.ps1) · [Maven mặc định](../qa/AUTH/test.txt) · [Package](../qa/AUTH/package.txt)

Để ghi một lượt chạy riêng, dùng thêm `-EvidenceDirectory` trỏ tới thư mục bằng chứng đã tạo. Mã xử lý bộ chọn nằm ở `OAuth2ThaoTac.YeuCauDangNhap` và được đăng ký tại `Security_Config`. Khi được phép xuất bản, đối chiếu manifest với revision gửi review và CI trước khi ghi DONE. [Bằng chứng D1–D3](../qa/AUTH/owner-d1-d3/)
