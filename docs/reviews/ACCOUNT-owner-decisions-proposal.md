# 05-ACCOUNT · Đề xuất quyết định nghiệp vụ

**Hồ sơ đề xuất ngày 28/11/2026.** Owner đã duyệt ACCOUNT-1–5 kèm điều kiện gửi thật và phân công vận hành. [Quyết định chính thức](../DECISIONS.md#account--quyết-định-owner-ngày-28112026)

**Hiện trạng lúc trình duyệt.** Baseline `5aa4932cc663de1115ff06ab7a8d7b837a673d93`. Tiến độ 8/19 DONE. BASE, AUTH và AUDIT đã đạt. ACCOUNT đang **BLOCKED_OWNER** ở hợp đồng lý do, sự kiện, thư và vận hành. Đây là phân tích source và các test hiện có. Ma trận kiểm thử bên dưới chưa chạy cho ACCOUNT.

## Hiện trạng tại baseline

| Đường nghiệp vụ | Đã có trong source | Phần còn thiếu cho ACCOUNT |
|---|---|---|
| Admin đổi trạng thái | `POST /admin/users/{id}/status` nhận trạng thái đích, lấy ID và phiên bản Admin từ principal. Có CSRF, quyền ADMIN ở route và service, PRG giữ bộ lọc/trang/mốc dòng. Form danh sách ẩn thao tác với chính mình. | Form thiếu lý do và trạng thái/phiên bản đích đã đọc. Service trả `true` cả khi trạng thái đã như yêu cầu nên UI có thể báo vừa đổi thành công. [Controller](../../src/main/java/com/beautyshop/controller/admin/AdminNguoiDung_Controller.java) · [Danh sách](../../src/main/resources/templates/admin/user/list.html) |
| Giao dịch và khóa | `datTrangThai` chặn tự khóa, khóa ghi actor/target theo ID tăng dần, kiểm lại role, active và phiên bản **của Admin**. `datKichHoat` tăng phiên bản xác thực khi chuyển từ hoạt động sang khóa. | Khóa hàng tuần tự hóa ghi nhưng chưa phát hiện màn hình cũ. Entity không có JPA `@Version`. Chưa gắn sự kiện audit hoặc email với lần đổi. [Service](../../src/main/java/com/beautyshop/service/NguoiDung_Service.java) · [Repository](../../src/main/java/com/beautyshop/repository/NguoiDung_Repository.java) · [Entity](../../src/main/java/com/beautyshop/model/NguoiDung.java) |
| Audit | V7 có actor, role, target, action, UTC, result, `ly_do VARCHAR(40)`, unique `(correlation_id, hanh_dong)`. V8 có snapshot trước/sau và bảo lưu. `SuKienQuanTri_Service` hỗ trợ boolean, ghi SUCCESS trong giao dịch và FAILED bằng REQUIRES_NEW sau rollback. Tra cứu ROLE_ADMIN đã có. | Enum chưa có LOCK/UNLOCK. Hàm ghi hiện để lý do SUCCESS là null và FAILED là ROLLBACK. Luồng audit xác thực hiện hữu của `NhatKyQuanTri_Service` phục vụ reset/liên kết. [Dịch vụ sự kiện](../../src/main/java/com/beautyshop/service/SuKienQuanTri_Service.java) · [Tra cứu](../../src/main/java/com/beautyshop/repository/TraCuuNhatKy_Repository.java) |
| Email và màn chi tiết | `Email_Service` dùng `JavaMailSender.send` đồng bộ cho OTP, reset và báo đổi mật khẩu. Luồng reset đã tách bước gửi và ghi kết quả. Trang chi tiết hiển thị phương thức đăng nhập, trạng thái xác minh email và form reset có lý do riêng. | Chưa có mẫu khóa/mở, outbox bền vững cho ACCOUNT, trạng thái SMTP chưa rõ hoặc hiển thị kết quả thư khóa/mở. [Email](../../src/main/java/com/beautyshop/service/Email_Service.java) · [Reset](../../src/main/java/com/beautyshop/service/MatKhau_Service.java) · [Chi tiết Admin](../../src/main/resources/templates/admin/user/detail.html) |
| Xác thực | Password principal dùng `kichHoat` cho enabled. Callback Google/GitHub đưa danh tính đã liên kết qua kiểm Customer còn active. Filter trước AuthorizationFilter đọc lại active, role và phiên bản ở request động, hủy phiên sai rồi chuyển về login. Không thấy cấu hình remember-me trong security chain. | Cần regression qua chính thao tác Admin mới cho cả ba phương thức, kể cả khi SMTP lỗi. [Security](../../src/main/java/com/beautyshop/security/Security_Config.java) · [Danh tính OAuth](../../src/main/java/com/beautyshop/service/DanhTinhOAuth_Service.java) · [Filter](../../src/main/java/com/beautyshop/security/TrangThaiTaiKhoan_Filter.java) |

Phạm vi kế thừa D26, D44, D47, AUDIT-D6 và AUDIT-D7. Giữ các luồng OTP/reset hiện hành, thống kê khách và phân quyền hiện tại. AUTH-LIVE, PAY-LIVE và AUDIT PURGE tiếp tục BLOCKED_EXTERNAL. [Quyết định hiện hành](../DECISIONS.md) · [Prompt ACCOUNT](implementation-prompts/next-upgrade/05-ACCOUNT.md) · [Ma trận tác động](BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) · [Hồ sơ AUTH](AUTH-ket-qua.md) · [Hồ sơ AUDIT](AUDIT-ket-qua.md)

## ACCOUNT-1 · Một trạng thái khóa, lý do bắt buộc cho cả hai chiều

**Đề nghị owner chọn lý do chuẩn có diễn giải tiếng Việt.** Khóa gồm `KHACH_YEU_CAU_KHOA` (chủ tài khoản yêu cầu khóa) hoặc `BAO_VE_TAI_KHOAN` (cần bảo vệ tài khoản trong lúc xác minh). Mở gồm `KHACH_XAC_NHAN_MO` (đã xác minh yêu cầu mở của chủ tài khoản) hoặc `DA_XU_LY_RUI_RO` (đã xử lý lý do bảo mật). Đây là danh mục đề xuất cần owner xác nhận đủ cho vận hành. Khóa bảo mật và khóa quản trị dùng chung cờ active và quyền mở hiện tại. Nếu cần hai lệnh giữ độc lập hoặc người duyệt khác nhau, một cờ sẽ không biểu diễn được điều kiện giải tỏa từng lệnh. Khi đó cần chốt hợp đồng riêng trước triển khai.

Mỗi chiều phải chọn đúng một mã, dài 1–40 ký tự ASCII sau trim, khớp chính xác danh mục backend. Từ chối thiếu, trắng, sai chiều, ngoài danh mục hoặc quá dài. Danh mục kín vừa đủ cột V7 và tránh đưa PII vào audit. Nếu owner cần tường trình tự do, phương án thay thế là mã bắt buộc kèm phần giải trình nội bộ tối đa 200 ký tự Unicode, với quyền đọc, lưu giữ và cột riêng cần duyệt. Không cắt ngắn âm thầm vào cột 40.

Giữ quyền Admin hiện tại, cấm tự khóa cả khi POST trực tiếp. Kiểm phiên và quyền trước khi trả kết quả thao tác. Form mang trạng thái đích mong muốn, cặp **active và phiên bản xác thực của target lúc đọc**, cùng mã thao tác do server cấp. Dưới khóa actor/target theo thứ tự hiện hành, đối chiếu với DB. Cặp này là tiền điều kiện ghi kiểu optimistic, đủ phát hiện vòng khóa rồi mở vì mỗi lần khóa tăng version. Thay đổi mật khẩu cũng có thể làm form hết hạn, khi đó Admin tải lại. Chưa cần thêm cột version.

| Tình huống | Kết quả đề xuất |
|---|---|
| Cùng thao tác được gửi lại sau commit | Trả kết quả đã ghi sau kiểm quyền và đối chiếu actor/target/action. Không tăng version, không thêm audit hoặc thư. |
| Hai Admin cùng khóa | Chỉ người đầu tạo thay đổi. Người sau thấy “Tài khoản đã ở trạng thái yêu cầu”, không sinh SUCCESS hay thư mới. |
| Form cũ muốn thay đổi trạng thái sau một vòng khóa/mở | Nếu đích khác trạng thái hiện hành và cặp mong đợi lệch, từ chối xung đột, yêu cầu đọc lại. Không tự áp ý định cũ. |
| Khóa/mở mới có chủ đích | Form mới, lý do hợp lệ và tiền điều kiện khớp thì cho phép. Giữ role, danh tính OAuth và dữ liệu Customer. |

## ACCOUNT-2 · Hai sự kiện nghiệp vụ trong Audit V8

**Đề nghị ghi `ACCOUNT_LOCK` và `ACCOUNT_UNLOCK`.** Actor là ID Admin từ principal đã kiểm lại trong DB, role ADMIN, target là loại ACCOUNT và ID đích. Thời điểm lấy UTC từ server/DB. SUCCESS lưu mã lý do được duyệt, snapshot boolean `true → false` hoặc `false → true`, UUID correlation và kết quả. Audit và outbox dùng cùng định danh thao tác. Chỉ ROLE_ADMIN đọc list/detail. Ở danh sách user hiển thị lý do của lần đổi mới nhất và trạng thái thư. Ở chi tiết cho xem thời điểm, actor, lý do và kết quả theo từng lần, giữ điều hướng và số liệu thống kê hiện có.

Ghi SUCCESS trong transaction đổi trạng thái. Với thao tác hợp lệ đã vào xử lý nhưng bị rollback, ghi FAILED sau rollback bằng transaction riêng theo D7, cùng correlation/action, mã lỗi giới hạn như `ROLLBACK`, snapshot sau để trống. Không biểu diễn trạng thái dự định là trạng thái đã commit. No-op, validation ở form, Guest, CSRF sai và request thiếu quyền không sinh sự kiện thay đổi thành công hay outbox. Giữ audit reset/liên kết hiện hữu. Khi gặp unique đã có, đọc đối chiếu actor, target, action và kết quả trước khi trả replay. FAILED không ghi đè SUCCESS. Muốn thử lại lệnh nghiệp vụ đã FAILED thì tải form lấy correlation mới. Retry email vẫn dùng event cũ.

Snapshot chỉ chứa cờ active. Không ghi email, họ tên, nội dung thư, mật khẩu, OTP, token, cookie, raw SMTP/exception hoặc toàn bộ request/entity. Giữ D6 với mức mặc định 180 ngày, bảo lưu đối soát/tranh chấp/nghĩa vụ và purge production bị khóa. Tránh để vòng đời outbox tự xóa audit hoặc kích hoạt purge. [V7](../../src/main/resources/db/migration/V7__xac_thuc_da_phuong_thuc.sql) · [V8](../../src/main/resources/db/migration/V8__nhat_ky_nghiep_vu.sql)

## ACCOUNT-3 · Hai thư giao dịch để owner duyệt nguyên văn

Thời điểm trong thư là lúc đổi trạng thái đã commit, hiển thị `dd/MM/yyyy HH:mm` theo Asia/Ho_Chi_Minh. Cả hai mẫu dẫn tới kênh liên hệ chính thức ở chân trang công khai, người bị khóa vẫn truy cập được. Owner cần xác nhận có ít nhất một kênh được trực trước khi bật gửi thật. Footer hiện đọc cấu hình liên hệ của cửa hàng. [Footer](../../src/main/resources/templates/fragments/footer.html)

**Tiêu đề: BeautyShop đã khóa tài khoản của bạn**

> Xin chào bạn,
>
> BeautyShop đã khóa tài khoản của bạn lúc {thời điểm} (giờ Việt Nam). Trong thời gian tài khoản bị khóa, bạn sẽ không thể đăng nhập vào BeautyShop.
>
> Nếu bạn không nhận ra thay đổi này hoặc muốn yêu cầu xem xét mở khóa, vui lòng vào trang chủ BeautyShop và liên hệ bộ phận hỗ trợ qua thông tin ở cuối trang. Bạn có thể cung cấp thời điểm ghi trong thư để chúng tôi tra cứu. Vui lòng giữ kín mật khẩu và mã xác minh của bạn.
>
> Trân trọng,
> BeautyShop

**Tiêu đề: BeautyShop đã mở khóa tài khoản của bạn**

> Xin chào bạn,
>
> BeautyShop đã mở khóa tài khoản của bạn lúc {thời điểm} (giờ Việt Nam). Bạn có thể đăng nhập lại trên BeautyShop bằng phương thức đã thiết lập cho tài khoản.
>
> Nếu bạn vẫn gặp khó khăn hoặc không nhận ra thay đổi này, vui lòng vào trang chủ BeautyShop và liên hệ bộ phận hỗ trợ qua thông tin ở cuối trang. Bạn có thể cung cấp thời điểm ghi trong thư để chúng tôi tra cứu. Vui lòng giữ kín mật khẩu và mã xác minh của bạn.
>
> Trân trọng,
> BeautyShop

Đề nghị gửi đến địa chỉ đã xác minh của tài khoản đích. Với dữ liệu legacy hoặc OAuth chưa có dấu xác minh nội bộ, vẫn commit trạng thái và ghi nhận event, hiển thị “Chưa gửi · chưa xác minh địa chỉ”, chờ xử lý có quyền. Không dùng email do Admin gõ hoặc địa chỉ mới xuất hiện khi retry làm người nhận thay thế. Quyết định này cần owner duyệt vì một số tài khoản sẽ cần hỗ trợ qua kênh khác. UI dùng nhãn “Máy chủ thư đã tiếp nhận” cho SENT. Mẫu thư không chứa lý do điều tra nội bộ, link mở khóa hoặc thông tin đăng nhập.

## ACCOUNT-4 · Outbox riêng cho email khóa/mở

1. **Ghi nguyên tử.** Tạo correlation trên server, ràng buộc với actor, target, action và trạng thái/version đã đọc. Trong cùng transaction MariaDB, xác thực Admin, khóa và kiểm target, đổi trạng thái, ghi SUCCESS, chèn một event thư. Unique DB theo sự kiện nghiệp vụ, ví dụ unique correlation/action, bảo đảm một ý định gửi cho một lần đổi. Lỗi ghi audit hoặc event làm rollback toàn bộ. Với request hợp lệ đã rollback, D7 ghi FAILED riêng. SMTP không chạy trong transaction này.
2. **Schema đề nghị.** Một migration additive sau V9 tạo outbox ACCOUNT, lưu event ID, correlation, action, FK user, phiên bản/địa chỉ nhận đã xác minh tại thời điểm thao tác theo chính sách bảo vệ PII, thời điểm sự kiện UTC, phiên bản mẫu, trạng thái, số lượt thử, thời điểm thử kế tiếp, mã lease và hạn lease, mã kết quả allowlist. Địa chỉ nhận chỉ dành cho worker, không xuất ở audit/log. Có thể dùng fingerprint và đối chiếu lại địa chỉ verified khi gửi nếu owner muốn tránh lưu thêm email. Index theo trạng thái và thời điểm đến hạn, unique sự kiện ở DB. Dữ liệu cũ không có audit/thư hiển thị “Chưa ghi nhận”. Không backfill sự kiện hoặc gửi thư cho thay đổi quá khứ.
3. **Gửi sau commit.** Poller chỉ đọc hàng đã commit, claim nguyên tử một event đến hạn và tăng số lần thử trong transaction ngắn. Gửi ngoài transaction giữ khóa, rồi ghi kết quả theo đúng lease. Hai worker tranh cùng event chỉ một worker được gửi. Có thể đánh thức worker sau commit, poller vẫn nhặt được hàng nếu tiến trình chết ngay sau commit. Dùng event ID cho tham chiếu SMTP/Message-ID ổn định khi adapter hỗ trợ. SMTP không bảo đảm exactly-once delivery.
4. **Thứ tự.** Theo từng tài khoản xử lý theo thứ tự sự kiện đã commit. Khi khóa rồi mở nhanh, tránh worker gửi một thông báo khóa trễ như thể đó là trạng thái hiện tại. Nếu sự kiện cũ còn UNKNOWN, đưa chuỗi của tài khoản đó vào hàng đối soát và báo Admin ngay. Trạng thái đăng nhập thật vẫn do DB quyết định. Thời gian trong thư giúp người nhận phân biệt các mốc kể cả khi nhà cung cấp giao thư khác thứ tự.

| Trạng thái / bằng chứng | Xử lý và nhãn cho Admin |
|---|---|
| PENDING | Đã ghi ý định. Worker gửi lần đầu ngay khi nhận việc sau commit. |
| SENDING | Một worker có lease. Timeout SMTP hữu hạn, hạn lease dài hơn timeout; worker khác chỉ ghi kết quả khi còn đúng lease. Lease hết khi đang gửi đưa sang UNKNOWN để đối soát. |
| SENT | SMTP xác nhận tiếp nhận. Lưu thời điểm, không gửi lại tự động. Không suy ra đã vào inbox. |
| Lỗi tạm thời, chắc chắn SMTP chưa nhận | Quay lại PENDING có lịch. Đề nghị tối đa **4 lần gửi tổng cộng**, gồm lần đầu và 3 retry sau **1, 5, 30 phút** tính từ lần thất bại trước. Lần ngắn xử lý gián đoạn thoáng qua, các khoảng dài giảm tải khi SMTP lỗi kéo dài. Đây là thông số vận hành đề xuất, chưa có số đo production. |
| FAILED | Từ chối vĩnh viễn, thiếu địa chỉ verified hoặc hết lượt retry xác định. Lưu mã phân loại và số lần thực tế, dừng tự động. Không gửi thử nếu thiếu địa chỉ. |
| UNKNOWN | Timeout khi chờ xác nhận cuối, mất kết nối sau khi có thể đã nhận, hoặc tiến trình chết giữa SMTP và cập nhật DB. Dừng retry tự động. Không xếp chung với lỗi chắc chắn chưa nhận. |

**Vận hành thư treo.** Đề nghị Admin được owner phân công theo dõi PENDING quá hạn, FAILED và UNKNOWN trên màn quản trị có quyền. Đối chiếu event ID/Message-ID, mốc UTC và biên nhận relay trong kênh vận hành hạn chế. Xác nhận relay đã nhận → SENT; chứng minh chưa nhận và đã khắc phục → cho phép gửi lại chính event bằng lệnh có CSRF, ghi actor, thời gian, lý do và số lần; chưa đủ bằng chứng → giữ UNKNOWN và liên hệ hỗ trợ. Thao tác thủ công cần quyết định riêng cho từng event sau giới hạn tự động. Không mở role mới hay màn sửa trực tiếp audit. Dừng worker khi rollback bản triển khai, giữ dữ liệu để tiếp tục đối soát. Không lấy lỗi email làm lý do mở lại tài khoản.

## ACCOUNT-5 · Phiên và các phương thức đăng nhập

**Đề nghị giữ hợp đồng xác thực hiện hành.** Sau commit khóa, đăng nhập mật khẩu mới bị từ chối theo enabled. Google OIDC và GitHub OAuth với danh tính đã liên kết phải bị từ chối khi ánh xạ về Customer inactive. Subject mới có email trùng không tự nhập vào tài khoản cũ. Khóa giữa lúc bắt đầu OAuth và callback phải xét trạng thái tại ứng dụng. Cả liên kết và reauth cũng phải qua kiểm phiên/phiên bản hiện tại.

Với phiên đã mở, điểm cưỡng chế là request động tiếp theo qua filter. Khóa tăng `phien_ban_xac_thuc` một lần, mở giữ giá trị mới. Phiên cũ kể cả tab chưa từng gửi request trong lúc khóa sẽ sai version sau mở. Người dùng phải đăng nhập lại bằng mật khẩu hoặc callback mới của phương thức đã liên kết. Phiên Google/GitHub bên ngoài do provider quản lý. Một request đã vượt filter trước commit có thể đang chạy; với đường ghi nhạy cảm kiểm lại active/version dưới khóa giao dịch tương ứng. Không phụ thuộc worker hoặc kết quả gửi mail. Remember-me hiện không bật, đề nghị giữ cấu hình này và kiểm không phát hành cookie remember-me.

## Ma trận ảnh hưởng và kiểm thử dự kiến

Tất cả ca bổ sung dưới đây là **NOT RUN cho ACCOUNT**. Các test đã tồn tại là điểm bắt đầu để bổ sung assertion. Fixture email và OAuth chỉ chứng minh hành vi ứng dụng với dịch vụ mô phỏng.

| Quyết định và yêu cầu | Nguồn hiện có → thay đổi dự kiến | Migration | Phép kiểm nghiệm thu và rủi ro | Phụ thuộc |
|---|---|---|---|---|
| 1 · Lý do, no-op, hai Admin | Controller/service, projection `TomTat`, form list → thêm lý do, phiên bản đích, kết quả changed/no-op/conflict. Giữ khóa theo ID. | Dùng lại V4 và cột lý do V7 với danh mục chuẩn. | Unit cả hai chiều, rỗng/trắng/sai chiều/40–41 ký tự, tự khóa. MariaDB hai Admin cùng chiều, form cũ sau khóa/mở, Admin bị thu hồi phiên, retry cùng form sau mất response. Đếm đúng một chuyển đổi/audit/event. | D26, AUTH, duyệt danh mục và mô hình khóa. |
| 2 · Audit và phân quyền | Enum, API ghi lý do ở `SuKienQuanTri_Service`, projection Admin. Tận dụng list/detail audit V8. | V7/V8 đủ với mã lý do và hai boolean. | MariaDB ép lỗi sau ghi user, sau SUCCESS và khi chèn outbox. Đối chiếu user/version cũ, 0 SUCCESS/0 event, 1 FAILED sau rollback. Test replay và phân biệt lỗi SMTP với lỗi đổi trạng thái. Guest, Customer, CSRF sai bị chặn. | D6/D7, bảo lưu và quyền đọc. |
| 3 · Thư đúng người, Admin thấy kết quả | `Email_Service`, `AdminBaoMatTaiKhoan_Controller`, admin/user list/detail → thêm hai mẫu, lý do và trạng thái SMTP. | Dùng metadata outbox của mục 4. | Mailpit/SMTP fixture kiểm subject, người nhận, UTF-8 và thời gian Việt Nam cả hai chiều. Thử email chưa verified, dữ liệu cũ và kênh hỗ trợ chưa cấu hình. WebKit giữ filter/page/Back và thống kê khách; detail phân biệt reset với khóa/mở. | D44/D47, owner xác nhận kênh hỗ trợ, nguyên văn và người nhận. |
| 4 · Gửi bền vững và retry | Chưa có outbox ACCOUNT → thêm repository, worker, phân loại SMTP và lệnh đối soát. | Migration mới additive, kiểm fresh và V9 nâng cấp, JPA validate, checksum cũ. | Hai worker, crash sau commit trước gửi, timeout trước/sau tiếp nhận, crash sau SMTP, lease quá hạn, lỗi vĩnh viễn, đủ 4 lần tự động, replay lệnh thủ công, khóa rồi mở nhanh. Đọc DB trực tiếp, kiểm số lần gọi SMTP, trạng thái user và audit. | MariaDB, SMTP fixture có điều khiển kết quả, người vận hành được phân công. |
| 5 · Phiên password/Google/GitHub | Security chain, principal, callback, filter → giữ cơ chế và bổ sung regression qua POST Admin. | Không cần schema xác thực mới. | Fixture cho từng phương thức: khóa lúc đang login, callback khi đã khóa, hai phiên cũ kể cả phiên im lặng, mở và đăng nhập mới, trường hợp SMTP hỏng. Kiểm Guest/Customer không ghi được, không có remember-me. Provider thật **BLOCKED_EXTERNAL** theo AUTH-LIVE. | AUTH, Google/GitHub fixture. |

Test nền để tái sử dụng gồm `adminTimPhanTrangKhoaMoVaThuHoiPhienCu` (đã có hai phiên, password, tự khóa, quyền và CSRF), các callback dùng fixture, kiểm quyền ở tầng service và kịch bản transaction audit. Ca OAuth hiện có cập nhật trạng thái hoặc version trực tiếp bằng SQL ở một số bước; gate ACCOUNT cần đi qua POST Admin để kiểm cả reason/audit/outbox. [XacThuc_MariaDb_Test](../../src/test/java/com/beautyshop/integration/XacThuc_MariaDb_Test.java) · [OAuth2Callback_MariaDb_Test](../../src/test/java/com/beautyshop/integration/OAuth2Callback_MariaDb_Test.java) · [Audit_MariaDb_Test](../../src/test/java/com/beautyshop/integration/Audit_MariaDb_Test.java) · [AdminServiceMethodSecurityTest](../../src/test/java/com/beautyshop/service/AdminServiceMethodSecurityTest.java)

## Quyết định cần nhận và thứ tự triển khai

1. **ACCOUNT-1** — Duyệt danh mục bốn lý do, dùng một cờ khóa và cách xử lý form lặp/cũ. Nếu cần tường trình riêng hoặc khóa nhiều cấp, nêu trường hợp nghiệp vụ cụ thể.
2. **ACCOUNT-2** — Duyệt tên sự kiện, trường allowlist, xử lý SUCCESS/FAILED/no-op và quyền xem lý do.
3. **ACCOUNT-3** — Duyệt hai mẫu, nguồn kênh liên hệ công khai và quy tắc địa chỉ đã xác minh cho tài khoản legacy/OAuth.
4. **ACCOUNT-4** — Duyệt outbox riêng, phạm vi lưu địa chỉ nhận, 4 lần gửi tối đa tự động, việc dừng khi kết quả chưa rõ và quyền đối soát thủ công.
5. **ACCOUNT-5** — Duyệt ranh giới hiệu lực tại request kế tiếp, yêu cầu đăng nhập mới sau mở và phạm vi nghiệm thu OAuth trên fixture.

Sau khi owner chốt và giao triển khai, ghi quyết định vào sổ, kiểm lại HEAD và phiên bản migration còn trống rồi mới chuyển ACCOUNT sang IN_PROGRESS. Thực hiện theo thứ tự hợp đồng và test → transaction user/audit/outbox → worker và đối soát → form, mail và UI → regression AUTH/AUDIT. Chạy JDK 21 Maven test/package, migration fresh/upgrade và integration trên MariaDB riêng, WebKit và SMTP fixture. Gắn kết quả với đúng revision để review, CI và owner nghiệm thu. Không triển khai NOTIFY hoặc email marketing trong ACCOUNT.
