# BeautyShop · Một tài khoản, nhiều phương thức đăng nhập

Bản trình duyệt thiết kế, ngày 26/11/2026. Quyết định cần chốt nhất là đường nâng cấp dữ liệu: **demo đang ở V6, trong khi source main dừng ở V5**. Đề xuất giữ lịch sử V6 nguyên bản, đưa ba bảng mới vào V7, duy trì mã Customer hiện hữu. Mọi endpoint, cột và sự kiện ghi “đề xuất” bên dưới đều là hợp đồng tương lai.

## 1. Phiên bản và mức độ xác nhận

| Nguồn | Kết quả kiểm tra |
|---|---|
| Git | Branch main, HEAD và main trên GitHub cùng ở `72a280507d0494dc8a36492365347bf2fb068aab` qua `git ls-remote`. So với origin/main, ahead/behind 0/0. |
| Working tree đầu lượt | 16 file tracked đã sửa: .env.example, README, DECISIONS, sổ tiến độ triển khai, manifest 00, Bootstrap_Controller, OAuth2NguoiDung_Service, Security_Config, TrangThaiTaiKhoan_Filter, hai file cấu hình properties, login/register, GiaoDienPagesTest, DonHangCOD_MariaDb_Test, XacThuc_MariaDb_Test. Bốn file untracked là báo cáo AUTH-BASELINE, OAuth2Client_Config, DangKyOtp_TestSupport và OAuth2BaselineTest. Phân tích bao gồm các thay đổi này. |
| Nhánh đối chiếu | `feat/email-otp-registration@d769eaf` còn ở máy. Có PhienOAuth2 và V6 lưu một cặp provider/ID trực tiếp trên người dùng. Main dùng PhienNguoiDung và tìm bằng email. Không thể lấy nguyên thiết kế một cặp để hỗ trợ hai provider trên cùng tài khoản. |
| Schema chạy thật | Đọc metadata và số đếm trên MariaDB local, schema beautyshop_demo, 23 bảng cơ sở gồm flyway_schema_history. V1–V6 success. V6__them_danh_tinh_oauth.sql có checksum -413680991, trùng CRC32 theo dòng của file trên feature. Có 8 tài khoản, 0 provider khác NULL, 0 ID provider khác NULL. Không thể phân loại 8 tài khoản theo cách tạo từ hai số đếm này. |
| Đã kiểm thử ở baseline trước | JDK 21, Maven test 274 ca, 226 đạt, 48 skip. Package đạt. MariaDB cô lập V1–V5 có 12/12 ca xác thực + COD đạt, OTP giả lập. JAR khởi động với OAuth tắt, /login trả 200. OAuth2BaselineTest kiểm tra cấu hình thiếu, từng provider riêng và Customer-only với email Admin. Đây là bằng chứng trước đợt thiết kế. [Báo cáo và vị trí log](AUTH-BASELINE-on-dinh-xac-thuc.md) |
| Chưa xác minh | Callback Google/GitHub thật, SMTP thật, refresh/session của thiết kế mới, DB ngoài demo và bất kỳ migration mới nào. Lượt này chỉ đọc, đối chiếu và thiết kế. |

Các quyết định nền là D25, D33 trong [DECISIONS](../DECISIONS.md), cùng [quy trình cấp Admin](../deployment/03-cap-quyen-admin.md). Phiếu 73 ca và kết quả của owner vẫn là bằng chứng của phiên bản được ghi trên phiếu.

## 2. Người dùng hiện đi qua những bước nào?

| Luồng | Hành vi của working tree và điểm chịu trách nhiệm |
|---|---|
| 1. Đăng ký mật khẩu | GET/POST /register ở TaiKhoan_Controller.dangKy. DangKy_Form kiểm email ≤190, tên ≤160, mật khẩu 12–72 ký tự và ≤72 byte UTF-8. NguoiDung_Service.chuanHoaEmail strip/lowercase, kiểm trùng, băm BCrypt. Sau khi gửi OTP thành công, session giữ email, tên, hash. Chưa có bản ghi SQL. |
| 2. Xác minh | EmailVerification_Service.guiOTP gửi mã 6 số qua Email_Service.guiMaOTP, lưu mã ở Redis 5 phút, gửi lại sau 60 giây, tối đa 5 lần thử. POST /register/verify-otp kiểm session, định dạng, xacThucOTP rồi NguoiDung_Service.dangKyDaXacThuc saveAndFlush trong transaction. Trùng email bị unique chặn. Thành công dọn session và về /login?registered. Resend/cancel đi qua /register/resend-otp, /register/cancel. Method dangKy trong service cũng có đường save trực tiếp, controller công khai hiện gọi dangKyDaXacThuc. |
| 3. Đăng nhập mật khẩu | Spring formLogin xử lý POST /login. NguoiDungDetails_Service.loadUserByUsername chuẩn hóa email, lấy người dùng qua findByEmail. PhienNguoiDung mang ROLE từ SQL, active, mã nội bộ và phiên bản xác thực. Security_Config.passwordEncoder kiểm BCrypt và giới hạn byte. |
| 4–5. Google lần đầu và lần sau | Bootstrap_Controller và auth/login chỉ hiện Google nếu đăng ký client. /oauth2/authorization/google → provider → /login/oauth2/code/google. Security_Config dùng OAuth2 UserInfo, scopes profile/email, chặn nhánh OIDC hiện tại. OAuth2NguoiDung_Service.loadUser yêu cầu email_verified=true, lấy email/name. Lần đầu với email mới, taoNguoiDung băm UUID và tạo Customer. Lần sau vẫn gọi findByEmail. |
| 6–7. GitHub lần đầu và lần sau | Hai URL tương ứng kết thúc bằng github. loadUser lấy tên hoặc login, layEmailGitHub gọi /user/emails bằng token, chọn primary và verified. Cùng quy tắc tạo/đăng nhập như Google. Khi thiếu tên, service dùng phần email trước @. |
| 8. Trùng email | phienChoEmailDaXacMinh nhận Customer đang hoạt động tìm được bằng email ở cả hai provider. Khi tạo đồng thời gặp DataIntegrityViolationException, code đọc lại theo email và kiểm trạng thái/role. Không có bước chứng minh quyền trên tài khoản nội bộ trước đó. |
| 9. Provider đổi email | Lần sau có thể tạo một Customer mới hoặc mở một Customer khác có email mới trùng khớp. Đây là hệ quả của khóa tra cứu trong code, chưa quan sát một sự cố thực tế. Đơn cũ vẫn thuộc ID cũ. |
| 10. Bị khóa | Form principal disabled, OAuth kiểm kichHoat. TrangThaiTaiKhoan_Filter.doFilterInternal đối chiếu findTrangThai theo ID trên request nghiệp vụ, bỏ qua tài nguyên tĩnh. Khi sai trạng thái, phiên bản hoặc quyền, logout và về /login?inactive. |
| 11. Admin | Vận hành đăng ký tài khoản chuyên dùng, thử mật khẩu, logout rồi nâng vai trò trên đúng hàng đang active. Form Login dẫn vào Dashboard hoặc URL /admin/ hợp lệ đã lưu. OAuth từ chối Admin cả ở service và bộ lọc phiên. AdminNguoiDung_Controller.danhSach gồm Customer và Admin, có POST /admin/users/{id}/status, chưa có endpoint chi tiết/cấp lại mật khẩu. |
| 12. Session và giỏ | Spring Security thiết lập phiên đăng nhập với bảo vệ session fixation. LoginSuccess_Handler.onAuthenticationSuccess cùng dùng cho Form/OAuth. Với Customer, khóa session mutex, gọi GioHang_Service.gopSauDangNhap theo ID và snapshot, chỉ dọn GioKhach sau khi service trả về. Lỗi DB gỡ xác thực, giữ session Guest và về /login?cartError. Thành công đi theo saved request (ví dụ checkout) hoặc /. Admin dùng nhánh điều hướng riêng. OAuth2LoginSuccess_Handler là class rỗng. |
| 13. Thu hồi | NguoiDung.datKichHoat tăng phienBanXacThuc khi active chuyển sang khóa. Bộ lọc so role và version nên mở khóa lại vẫn đòi đăng nhập mới. Logout hủy session, về /?logout. Hiện chưa có luồng đổi/reset mật khẩu để tăng version. |

OTP hiện gửi mail trước khi ghi Redis, key gắn với email, mã nằm dạng đọc được, các thao tác đọc/đếm/xóa tách rời. Hai session cùng email và sự cố giữa Redis/SQL cần fixture riêng. Đề xuất ràng buộc challenge ngẫu nhiên với phiên đăng ký và mục đích, lưu HMAC mã với khóa máy chủ, kiểm/đếm/tiêu thụ nguyên tử trong Redis, resend thay thế challenge và xử lý lỗi gửi thư. SQL lỗi sau khi dùng mã thì xin mã mới. Loại email và stack trace thô khỏi log lỗi đăng ký; duy trì thông tin chẩn đoán theo mã lỗi. Cấu hình Redis/SMTP nằm ở application.properties và README. OAuth2Client_Config.daCauHinh chỉ kiểm hasText hai biến, vì vậy literal local-placeholder kèm secret có chữ vẫn có thể bật nút. Đề xuất từ chối sentinel này cho cả ID/secret theo từng provider.

## 3. Entity đối chiếu với MariaDB thật

Nguồn: entity/NguoiDung.java, repository/NguoiDung_Repository.java, SQL V1/V4, SHOW CREATE TABLE trên demo. Entity có 8 thuộc tính, không khai báo quan hệ JPA hay Bean Validation cấp field. Form và service kiểm đầu vào. SQL có thêm các cột dưới bảng. Default trong bảng là default **SQL**.

| Java | Kiểu Java | Cột SQL | Cho NULL | Default | Ràng buộc và kiểu SQL | Công dụng |
|---|---|---|---|---|---|---|
| maNguoiDung | Long | ma_nguoi_dung | Không | tự tăng | BIGINT PK, JPA IDENTITY | Chủ sở hữu bền vững |
| email | String | email | Không | — | VARCHAR(190), UNIQUE uq_nguoi_dung_01 | Email chính, login nội bộ |
| matKhauBam | String | mat_khau_bam | Không | — | VARCHAR(255) | BCrypt của mật khẩu hoặc UUID ngẫu nhiên |
| hoTen | String | ho_ten | Không | — | VARCHAR(160) | Tên hiển thị |
| soDienThoai | String | so_dien_thoai | Có | NULL | VARCHAR(30) | Liên hệ hồ sơ |
| vaiTro | NguoiDung.VaiTro | vai_tro | Không | CUSTOMER | VARCHAR(20), @Enumerated(STRING), CUSTOMER/ADMIN trong Java | Quyền backend |
| kichHoat | boolean | kich_hoat | Không | 1 | BOOLEAN, demo TINYINT(1) | Trạng thái đăng nhập |
| phienBanXacThuc | long | phien_ban_xac_thuc | Không | 0 | BIGINT, V4 | Thu hồi session, không phải JPA @Version |
| không ánh xạ | — | ngay_tao | Không | CURRENT_TIMESTAMP(6) | DATETIME(6), V1 | DB ghi thời điểm tạo |
| không có trên main | — | nha_cung_cap_oauth, ma_danh_tinh_oauth | Có | NULL | VARCHAR(20/255) ascii_bin, unique cặp, CHECK cùng trống hoặc provider google/github + ID có giá trị, V6 demo | Trên feature là nhaCungCapOauth, maDanhTinhOauth |

FK **trỏ tới** nguoi_dung từ dia_chi_nguoi_dung, gio_hang, don_hang, danh_gia, san_pham_yeu_thich, yeu_cau_doi_tra và ghi_nhan_gop_gio_hang. Entity NguoiDung không có FK đi ra. Không có thời điểm xác minh email/đổi mật khẩu trong bảng đã kiểm.

| Thuộc tính | Sau OTP | Google mới | GitHub mới | Khi cấp quyền Admin theo runbook |
|---|---|---|---|---|
| maNguoiDung | DB sinh | DB sinh | DB sinh | giữ cùng hàng |
| email | form đã chuẩn hóa | email Google đã xác minh, chuẩn hóa | email primary verified, chuẩn hóa | giữ email chuyên dùng |
| matKhauBam | BCrypt từ form trong session | BCrypt(UUID) | BCrypt(UUID) | giữ BCrypt đã tự đặt |
| hoTen | form strip | name hoặc phần đầu email | name, login hoặc phần đầu email | giữ tên |
| soDienThoai | NULL | NULL | NULL | giữ hồ sơ, có thể NULL |
| vaiTro | CUSTOMER | CUSTOMER | CUSTOMER | đổi CUSTOMER → ADMIN |
| kichHoat | true | true | true | phải đang true |
| phienBanXacThuc | 0 | 0 | 0 | giữ version, bộ lọc so cả role |
| ngay_tao | DB sinh | DB sinh | DB sinh | giữ giờ tạo ban đầu |
| hai cột V6 trên demo | NULL | NULL qua main | NULL qua main | giữ nguyên |

| Thuộc tính | Hiện tại | Đề xuất | Thay đổi | Lý do |
|---|---|---|---|---|
| maNguoiDung, email, hoTen, soDienThoai, vaiTro, kichHoat | như trên | như cũ | giữ | Bảo toàn ownership gồm cả ID tham chiếu review Mongo |
| matKhauBam | bắt buộc | NULL khi tài khoản OAuth mới chưa thiết lập mật khẩu | sửa NULL ở JPA và SQL | Phân biệt khả năng đăng nhập bằng mật khẩu cho dữ liệu mới |
| phienBanXacThuc | tăng khi khóa | tăng thêm khi thay đổi credential và gỡ/liên kết theo chính sách bên dưới | sửa hành vi | Dùng lại cơ chế thu hồi |
| ngayXacMinhEmail | vắng | DATETIME(6) NULL, UTC | thêm | Chứng minh email BeautyShop đủ điều kiện khôi phục. Ghi khi chứng minh quyền nhận thư, không suy từ ngày tạo. |
| ngày tạo, hai cột V6 | chỉ ở SQL trên demo | giữ lịch sử V6, cột cũ ngừng dùng sau chuyển tiếp | giữ | Tránh mất dữ liệu và sửa checksum |
| ngayDoiMatKhau, googleId, githubId, isGoogle/isGithub | vắng | bỏ đề xuất | — | Sự kiện ghi thời gian, danh tính có bảng riêng |

## 4. Schema đề nghị và đường nâng cấp

| Bảng | Hiện tại | Sau khi duyệt | Loại | Ảnh hưởng |
|---|---|---|---|---|
| nguoi_dung | V5 trên main hoặc V6 trên demo | mat_khau_bam VARCHAR(255) NULL, thêm ngay_xac_minh_email DATETIME(6) NULL | sửa một bảng | Giữ mọi hash, PK, email, role, FK và lịch sử |
| danh_tinh_oauth | vắng | ma_danh_tinh BIGINT PK tự tăng, ma_nguoi_dung BIGINT FK, nha_cung_cap VARCHAR(20) allowlist, ma_dinh_danh_nha_cung_cap VARCHAR(255) ascii_bin, ngay_lien_ket UTC | mới | UNIQUE(provider, subject), UNIQUE(user, provider); các khóa cũng phục vụ lookup hai chiều |
| yeu_cau_dat_lai_mat_khau | vắng | ma_yeu_cau BIGINT PK, FK người dùng, token_bam BINARY(32) UNIQUE, ngay_tao, het_han_luc, da_su_dung_luc NULL, nguon_yeu_cau, version xác thực lúc cấp | mới | Index (user, thời điểm tạo), hết hạn cho dọn định kỳ khi cần. Mục đích cố định RESET. Người khởi tạo nằm ở audit, correlation nối qua mã yêu cầu. |
| nhat_ky_quan_tri | vắng | hợp đồng chung bên dưới cho cả sự kiện bảo mật | mới | Index (loại đối tượng, ID, thời gian) và correlation phục vụ truy vết |
| 21 bảng ứng dụng khác | V1–V5 | cùng schema | giữ | Không ghi lại đơn, giỏ, voucher, thanh toán, địa chỉ, sản phẩm, review hay collection Mongo |

Tổng hiện tại **6 JPA @Entity**, một DanhGia_Document riêng của Mongo. Có 22 bảng ứng dụng vì nhiều repository dùng SQL trực tiếp. Dự kiến **9 JPA entity, 25 bảng ứng dụng, 26 nếu tính Flyway**. Từ V6 demo: thêm 3 bảng, sửa 1 bảng, không xóa cột, không đổi kiểu dữ liệu hiện có. Từ V5 sạch: V6 bổ sung thêm hai cột legacy cùng các constraint của feature rồi V7 hội tụ. Là chi phí tương thích có chủ đích.

1. Trước triển khai, kiểm lịch sử, checksum, cột và cặp provider thực ở **từng DB**. Backup MariaDB, thử restore sang DB riêng, tắt ghi auth khi chuyển phiên bản. Dùng đúng file V6 từ feature đã đối chiếu, giữ V1–V5. Nếu gặp V6 khác checksum hoặc quan hệ mâu thuẫn, dừng để lập phương án cho DB ấy. Không dùng repair để ép khớp.
2. V7 tạo schema trên. Chuyển **chỉ liên kết đã có bằng chứng** từ cặp V6 hợp lệ của Customer sang bảng mới, giữ nguyên user ID. Đối chiếu chính xác tập cặp và chủ sở hữu trước/sau; flag Admin, dữ liệu bất thường để xét riêng. Với liên kết cũ thiếu mốc thời gian, để ngay_lien_ket NULL và ghi nguồn import, không gán ngày đăng nhập giả. Với bản ghi mới, service bắt buộc timestamp. Demo hiện có 0 cặp cần chuyển. DDL MariaDB có thể commit từng bước: diễn tập lỗi giữa các DDL, đối chiếu schema thực và restore bản sao trước khi thử lại. Kiểm cả DB trắng, V5 có dữ liệu, V6 có liên kết.
3. **Khoảng trống dữ liệu lịch sử.** Hash ngẫu nhiên và hash tự đặt cùng dạng BCrypt. Email có trên DB chưa chứng minh hộp thư hoặc loại tài khoản. Không suy đoán provider, tự NULL hash, ghi mốc verified hàng loạt hoặc tự liên kết. Trước chuyển đổi mời Customer đang có mật khẩu chứng minh lại và xác minh email. Với Customer legacy chỉ dùng OAuth, mở quy trình chuyển tiếp có thời hạn: chỉ tạo phiên giới hạn khôi phục sau bước chứng minh quyền tài khoản độc lập đã được duyệt (ví dụ kiểm tra hỗ trợ độc lập đối với chủ cũ), challenge hộp thư BeautyShop mới và bằng chứng provider mới; hiển thị xác nhận liên kết rõ ràng. Khi thiếu bằng chứng thì chuyển hỗ trợ, giữ ID và dữ liệu. Email trùng và việc bấm lại cùng provider chưa đủ để tự gắn. Không mở đường reset công khai đối với legacy chưa xác minh.
4. Khi chỉ một phần provider bật, kiểm trước khi gỡ rằng còn phương thức thực sự dùng được, nhất là tài khoản legacy có hash chưa rõ và provider ngừng cấu hình. Legacy không được tính một hash cũ làm đường dự phòng khi gỡ nếu chưa chứng minh được mật khẩu đó. Trước tắt một provider vận hành, thống kê tài khoản phụ thuộc, thông báo và cho thiết lập đường thay thế.
5. Rollback: trước dữ liệu mới có thể quay về checkpoint sau restore thử. Sau khi đã phát sinh password NULL/liên kết mới, binary cũ có thể lỗi hoặc đăng nhập nhầm theo email. Ưu tiên khóa riêng tính năng nhạy cảm, sửa tiến tới và bảo toàn ghi mới. Muốn khôi phục bản cũ cần cửa sổ ngừng ghi, kế hoạch đối soát đơn và tài khoản phát sinh, bản backup đã thử. Không chạy song song hai binary auth khác quy tắc.

## 5. Hợp đồng đăng nhập và liên kết

**DanhTinhOAuth.** Một Customer có tối đa một Google và một GitHub. Quan hệ con → NguoiDung dùng ManyToOne LAZY bắt buộc, FK RESTRICT, không cascade persist/remove tới user. Truy vấn con theo unique key, fetch người dùng khi xác thực. Không cần collection eager trên NguoiDung. Gỡ chỉ xóa một bản ghi con trong transaction và ghi audit. Khóa thay thế maDanhTinh đơn giản hóa JPA; ngayLienKet phục vụ Customer kiểm tra thay đổi. Đề xuất bỏ emailNhaCungCap và lanDangNhapCuoi: UI hiện tên phương thức là đủ, tránh lưu thêm email và ghi SQL mỗi lần login. Không lưu access/refresh token lâu dài. Đánh giá cấu hình OAuth2AuthorizedClientService của Spring, dọn authorized client sau khi dùng xong, chỉ giữ claims tối thiểu trong principal.

**Định danh đầu vào.** Google đề xuất bật OpenID Connect, giao Spring kiểm chữ ký, issuer, audience, expiry, state và nonce; lấy sub từ issuer Google được allowlist. Thay nhánh OIDC bị chặn ở baseline bằng adapter đã test. GitHub giữ Authorization Code, đọc id tài khoản từ user endpoint, chuẩn hóa thành chuỗi thập phân ổn định, không lấy login làm ID; kiểm primary/verified qua danh sách email có phân trang. Dữ liệu đến từ client đăng ký đúng provider. Thiếu ID hoặc lỗi provider thì dừng trước ghi SQL. Kiểm giới hạn độ dài email/tên trước insert. Tách việc gọi mạng khỏi transaction giữ khóa. [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect) · [GitHub email API](https://docs.github.com/en/rest/users/emails)

| Tình huống | Giao dịch và kết quả đề xuất |
|---|---|
| Đăng ký truyền thống | OTP đúng → tạo user bằng hash và ghi mốc xác minh. Không tạo danh tính OAuth. Hết phiên hoặc mã không hợp lệ thì ở lại luồng đăng ký. |
| LOGIN, cặp provider/ID đã biết | Đọc user qua FK, kiểm active và CUSTOMER. Giữ ID và email BeautyShop dù email provider đổi. Nếu provider không trả được bằng chứng email theo chính sách đã chọn, báo lỗi provider, không chuyển sang lookup email. |
| LOGIN, cặp mới và email mới đã xác minh | Một transaction tạo Customer, hash NULL và bản ghi danh tính. Xác minh hộp thư **BeautyShop** bằng challenge riêng trước khi cho phép recovery, đặc biệt với email thuộc miền bên thứ ba. Có thể mua hàng bằng OAuth trước bước này. |
| LOGIN, email trùng nhưng chưa liên kết | Không tạo phiên nội bộ bằng email. Mời đăng nhập BeautyShop bằng phương thức đang có rồi khởi tạo LINK mới. Thông báo chung, không tiết lộ role hay chi tiết tài khoản. Nếu cặp đã thuộc user khác, từ chối. |
| Hai callback đồng thời | Hai unique key và email unique là hàng rào cuối. Để transaction lỗi rollback hoàn toàn trước khi đọc lại trong transaction mới. Chỉ chấp nhận đúng cùng provider/subject và user hợp lệ. Hai provider khác nhau cùng email đi vào luồng trùng email. |
| LINK có chủ đích | POST có CSRF từ /account/security. Yêu cầu Customer active xác thực lại bằng **phương thức đang gắn**. Gắn intent một lần với session, user ID, version, provider, purpose, state ngẫu nhiên và TTL ngắn; lưu server-side. Callback kiểm cả state của Spring lẫn intent. Hai tab có intent riêng hoặc từ chối rõ lần thứ hai. Hết phiên, đổi user, replay, provider lệch, logout → hủy. Xác thực provider mới rồi màn xác nhận POST (nhất là khi email khác). Khóa NguoiDung_Repository.khoa trước khi kiểm lại quyền và ghi liên kết. Không gọi login-success, không chuyển principal/merge giỏ của tài khoản khác. |
| UNLINK | POST + CSRF + chứng minh gần đây bằng **phương thức vẫn còn sau khi gỡ**. Khóa cùng hàng user trước khi đếm phương thức khả dụng, xóa và ghi audit. Hai yêu cầu gỡ đồng thời được tuần tự hóa. Nếu chỉ còn provider đang tắt hoặc password legacy chưa chứng minh, yêu cầu thiết lập dự phòng trước. |

Reauth bằng OAuth phải ràng buộc **đúng subject đã liên kết**, có bằng chứng xác thực đủ mới. Chọn lại tài khoản hoặc có access token còn hiệu lực chưa đủ chứng minh người dùng vừa xác thực lại. Google kiểm khả năng nhận/kiểm auth_time theo cấu hình thực, GitHub cần xác minh khả năng step-up thực tế; nếu provider không chứng minh độ mới, yêu cầu bước độc lập đã duyệt (ví dụ OTP gửi tới email khôi phục đã xác minh **kèm** OAuth đúng subject), hoặc khóa thao tác. Không để callback LINK/REAUTH đi qua OAuth login mặc định rồi mới kiểm ở success handler khi Spring đã thay principal. Dùng endpoint callback/handler phân biệt mục đích, cùng redirect URI đã đăng ký hoặc URI riêng nếu provider hỗ trợ; không dùng query do trình duyệt tự khai để quyết định chủ tài khoản. Kiểm session rotation, clear pending intents và credentials sau hoàn tất. Với thay đổi credential, chọn đăng xuất và yêu cầu đăng nhập lại mọi phiên, kể cả phiên vừa thao tác; chỉ login thật mới chạy LoginSuccess_Handler.

**Password NULL là thay đổi hợp đồng Java.** PhienNguoiDung hiện gọi constructor Spring User với getMatKhauBam. Phải sửa consumer trước khi phát sinh NULL. Đề xuất một principal nhận diện nội bộ dùng được ở các @AuthenticationPrincipal và bộ lọc hiện tại, hỗ trợ OAuth/OIDC qua adapter tối thiểu; chỉ mật khẩu mới cần UserDetails có hash khi xác thực. Với hash NULL, loadUserByUsername đi theo nhánh lỗi đăng nhập chung, có xử lý chi phí băm tương đương; OAuth không giữ hash, ID token hay toàn bộ attributes trong view/session lâu hơn giao thức cần. Test cả filter, controller, request cache, logout và login bằng mật khẩu sai. Không dùng chuỗi mật khẩu giả trong DB để lách constructor.

## 6. Thiết lập, đổi, đặt lại mật khẩu

| Thao tác | Ai được khởi tạo | Bằng chứng bắt buộc | Khi commit |
|---|---|---|---|
| Thiết lập lần đầu | Customer active, hash NULL | phiên đúng user, OAuth đã gắn được xác thực lại, chứng minh hộp thư BeautyShop | Khóa user, kiểm lại NULL, băm mật khẩu, tăng version, ghi PASSWORD_CREATED, thu hồi phiên |
| Đổi khi biết mật khẩu | Chủ tài khoản đã có mật khẩu (trang Customer trong phạm vi) | mật khẩu hiện tại đúng, CSRF, kiểm version và active | Cập nhật cùng ID, tăng version, audit PASSWORD_CHANGED, thu hồi token/phiên cũ |
| Quên mật khẩu | Guest qua form email | Customer active, đã có mật khẩu và đã xác minh email chính, token đúng mục đích | Khóa user, tiêu thụ token, ghi hash mới và version, audit, đăng nhập lại |
| Admin hỗ trợ | Admin active qua POST có lý do và xác nhận | kiểm quyền cả route lẫn service, version Admin, Customer đích đủ điều kiện như trên | Dùng cùng service cấp token. Chỉ Customer hoàn tất mới đổi mật khẩu; không đổi active hoặc mở khóa |

Áp dụng cùng validation 12–72 ký tự/≤72 byte UTF-8, xác nhận hai lần cho mật khẩu mới. Mật khẩu, OTP và hash không đi qua flash, query hay DTO đầu ra. Không gửi mật khẩu qua thư. Nhánh quên mật khẩu và Admin hỗ trợ đều loại trừ tài khoản Admin, bị khóa, OAuth-only và legacy chưa đủ bằng chứng. Chính sách giúp Admin tự khôi phục nằm ngoài cơ chế hỗ trợ Customer này.

| Kho token | Lợi ích | Chi phí và lựa chọn |
|---|---|---|
| Redis | TTL tự hết hạn, Lua kiểm và tiêu thụ nguyên tử trong Redis; phù hợp bộ đếm theo email băm/IP | Khi MariaDB rollback sau khi consume, link bị mất; nếu ghi SQL trước, hai request có thể cùng thấy token. Cần ràng buộc version và cơ chế phục hồi. |
| MariaDB | Unique token_bam và khóa cùng transaction với user, consume, version và audit | Cần bảng, index và dọn theo hạn lưu. **Đề xuất chọn**. Redis chỉ giới hạn tần suất/OTP, không lưu thêm bản sao token. |

**Cấp và gửi.** POST quên mật khẩu trả cùng status, nội dung và đường điều hướng cho mọi email. Rate limit cả nguồn gửi và đích, kiểm chênh lệch thời gian phản hồi. Tách gửi thư khỏi response bằng executor giới hạn nếu cần; lỗi Redis limit phải có chế độ từ chối an toàn. Sinh ngẫu nhiên 32 byte từ SecureRandom; lưu SHA-256 token, TTL đề xuất 15 phút. Trong transaction khóa user, giới hạn yêu cầu đang hoạt động, vô hiệu link trước theo chính sách cấp mới, ghi yêu cầu và audit nguồn. Admin chọn lý do chuẩn. Dùng correlation sinh server. Sau commit gửi link HTTPS qua Email_Service đến đúng địa chỉ đã xác minh; lấy base URL cấu hình tin cậy. SMTP lỗi thì ghi thất bại và hủy **riêng token ấy** nếu còn hiệu lực. Nếu crash giữa commit và gửi, link hết hạn, người dùng được xin lại sau cooldown; trạng thái gửi chưa biết không ghi SENT. Không cần outbox chứa raw token. SMTP chấp nhận thư chỉ chứng minh giao cho mail server. Không hủy mật khẩu hoặc phiên lúc xin link. [OWASP Forgot Password](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html)

**Sử dụng.** GET mở link chỉ trình bày form hoặc đổi sang challenge session ngắn và redirect URL sạch, không tiêu thụ token vì trình quét email có thể mở link. Dùng no-store, Referrer-Policy no-referrer, tránh tài nguyên ngoài, lọc query khỏi access logs. POST có CSRF, kiểm hash, hạn, mục đích, active/CUSTOMER, password hiện có, version, unused. Tra user ID rồi khóa **user trước, request sau**, đọc lại điều kiện trong transaction; mọi nhánh cấp/consume dùng cùng thứ tự. Admin và target khi cùng cần khóa thì theo ID tăng dần như datTrangThai. Cập nhật mật khẩu, tiêu thụ link và tăng version trong cùng commit. Link khác hết hiệu lực theo version, hai submit chỉ một thành công. Token sai/hết hạn/đã dùng hiển thị cùng lỗi có đường xin lại. Sau commit gửi thông báo thay đổi và về /login; lỗi gửi thư xác nhận không đảo ngược thay đổi thành công. Bộ lọc vô hiệu session ở request tiếp theo; thao tác nhạy cảm đang chạy phải kiểm version dưới khóa trước ghi.

## 7. Nhật ký quản trị và sự kiện bảo mật

Đã đối chiếu kế hoạch, Audit, manifest và bộ prompt. Upgrade Plan phần quản trị (dòng 159) nêu audit bền vững cần duyệt. Audit phần nghiệp vụ tùy chọn (dòng 84) chỉ duyệt log chọn lọc. Manifest 00 dòng 29 ghi rõ “audit DB cần duyệt thêm”. **Chưa thuộc Phase hay Work Package triển khai chính thức.** Không tìm thấy bảng audit trong 22 bảng, hoặc Entity, Repository, Service nhật ký chung trong mã nguồn. Có SLF4J xử lý lỗi ở TaiKhoan_Controller, LoginSuccess_Handler và có bảng nghiệp vụ ghi nhận trừ kho/gộp giỏ. Chúng không cung cấp lịch sử Admin yêu cầu reset. Từ “Audit” trong tên báo cáo là hoạt động rà soát kế hoạch. [Kế hoạch](BEAUTYSHOP_UPGRADE_PLAN.md) · [Audit](BEAUTYSHOP_UPGRADE_AUDIT.md) · [Manifest](implementation-prompts/00-EXECUTION_ORDER.md)

**Đề xuất duyệt phần nền tảng trong đợt auth, trước tính năng Admin cấp lại**, sử dụng chung một bảng và một dịch vụ ghi. Chưa mở màn hình duyệt nhật ký. Không ghi toàn bộ login, tài khoản không tồn tại hay mọi request vào SQL. Tách Security/Admin bằng action và quyền đọc, tách kho vật lý chỉ khi nhu cầu lưu giữ/quyền thực sự khác.

| Trường đề xuất | Cách dùng |
|---|---|
| maNhatKy | PK tự tăng, định vị sự kiện |
| maNguoiThucHien, vaiTroNguoiThucHien | ID nội bộ khi xác định được, snapshot loại ADMIN/CUSTOMER/SYSTEM/ANONYMOUS. Request quên mật khẩu trước xác minh chưa chứng minh ai là Customer. |
| hanhDong, loaiDoiTuong, maDoiTuong | Mã sự kiện, ACCOUNT/RESET_REQUEST hoặc loại về sau, ID nội bộ. Dùng ID logic với nhiều loại đối tượng, không FK đa hình. |
| thoiGian, ketQua | UTC DATETIME(6), trạng thái kết quả của **đúng bước** |
| lyDo | Mã lý do cho hỗ trợ/từ chối, hạn chế free text và độ dài |
| correlationId | ID ngẫu nhiên chung cho request, gửi thư và hoàn tất. Không dùng token. |
| metadata | Chưa cần cột JSON tự do. Bổ sung allowlist field cụ thể khi có caller, ví dụ tên provider được chuẩn hóa. |

| Sự kiện | Actor → đối tượng | Thời điểm ghi |
|---|---|---|
| ADMIN_PASSWORD_RESET_REQUESTED | Admin → Customer, correlation đến yêu cầu | cùng commit tạo yêu cầu |
| PASSWORD_RESET_REQUESTED | anonymous → yêu cầu đủ điều kiện | cùng commit tạo yêu cầu tự phục vụ |
| PASSWORD_RESET_EMAIL_SENT / FAILED | system → yêu cầu | sau kết quả SMTP, transaction riêng |
| PASSWORD_RESET_COMPLETED | người chứng minh token của Customer → tài khoản | cùng commit đổi hash, không gán công cho Admin |
| PASSWORD_CREATED / PASSWORD_CHANGED | Customer đã xác thực lại → chính mình | cùng commit ghi credential |
| OAUTH_LINKED / OAUTH_UNLINKED | Customer → chính mình, tạo mới tự động ghi đúng ngữ cảnh signup | cùng commit liên kết/gỡ |
| SESSION_REVOKED | system → tài khoản | ghi một lần khi tăng version, diễn đạt là thu hồi theo version, không khẳng định mọi thiết bị đã gửi request |

Audit thành công của thao tác SQL cùng transaction, lỗi ghi audit thì rollback ghi nhạy cảm. Sự kiện từ chối/lỗi cần điều tra ghi sau rollback bằng transaction ngắn riêng hoặc structured application log mã hóa theo allowlist khi DB lỗi. Chống ghi trùng tại điểm chuyển trạng thái, dùng khóa sự kiện duy nhất cho bước một lần (request ID + action, hoặc ID thao tác cho nhiều lần thử). Không ghi OTP, mật khẩu/hash, token/hash token, URL reset, client secret, header, claims, request body, địa chỉ email nguyên văn. Customer không có quyền đọc, tài khoản ứng dụng cấp INSERT và quyền đọc tối thiểu với bảng này, tách vận hành retention khỏi tài khoản web; không cung cấp route sửa/xóa. Đề nghị giữ audit 90 ngày rồi dọn có kiểm soát, token hết hạn dọn sớm hơn; owner duyệt thời hạn, quyền đọc và backup. Tương lai khóa user, catalog/SKU/tồn, đơn, return, review, voucher có thể dùng cùng từ vựng actor/action/target. Riêng review Mongo không thể cam kết atomic với SQL audit; cần thiết kế riêng nếu sau này duyệt, tránh ghi kép nội dung review.

## 8. MVC và giao diện

Cây rút gọn đủ để xác định phạm vi auth. Những tên mang NEW là tên đề xuất. Package entity vẫn dùng cho tới ARCH-01.

```text
Hiện tại
com/beautyshop/
  controller/         Bootstrap_Controller, TaiKhoan_Controller, admin/AdminNguoiDung_Controller
  dto/                DangKy_Form, HoSo_Form
  entity/             NguoiDung
  repository/         NguoiDung_Repository
  service/            NguoiDung_Service, EmailVerification_Service, Email_Service, GioHang_Service
  security/           Security_Config, OAuth2Client_Config, OAuth2NguoiDung_Service,
                      NguoiDungDetails_Service, PhienNguoiDung, TrangThaiTaiKhoan_Filter,
                      LoginSuccess_Handler, OAuth2LoginSuccess_Handler (rỗng)
resources/
  templates/          auth/{login,register,account}, admin/user/list
  db/migration/       V1–V5

Sau khi duyệt
com/beautyshop/
  entity/       MODIFIED NguoiDung
                NEW DanhTinhOAuth, YeuCauDatLaiMatKhau, NhatKyQuanTri
  repository/   NEW DanhTinhOAuth_Repository, YeuCauDatLaiMatKhau_Repository, NhatKyQuanTri_Repository
                UNCHANGED NguoiDung_Repository.khoa và findTrangThai tái sử dụng
  security/     MODIFIED OAuth2Client_Config, OAuth2NguoiDung_Service, Security_Config,
                         PhienNguoiDung, NguoiDungDetails_Service
                NEW adapter OIDC và xử lý intent LINK/REAUTH tách khỏi LOGIN
                UNCHANGED contract TrangThaiTaiKhoan_Filter, LoginSuccess_Handler
  service/      NEW DanhTinhOAuth_Service (liên kết và transaction),
                    MatKhau_Service (cấp/tiêu thụ, đổi/thiết lập),
                    NhatKyQuanTri_Service (ghi sự kiện)
                MODIFIED NguoiDung_Service, Email_Service, EmailVerification_Service
                UNCHANGED GioHang_Service
  controller/   NEW BaoMatTaiKhoan_Controller, DatLaiMatKhau_Controller
                MODIFIED TaiKhoan_Controller, admin/AdminNguoiDung_Controller
  dto/          NEW form email khôi phục, mật khẩu mới/đổi, lý do hỗ trợ,
                    record trạng thái bảo mật chỉ gồm nhãn, khả năng thao tác, trạng thái xác minh
resources/
  templates/    MODIFIED auth/login, auth/account, admin/user/list
                NEW auth/security, auth/forgot-password, auth/reset-password,
                    fragment mật khẩu/xác nhận, admin/user/detail
                UNCHANGED hợp đồng auth/register và các form CSRF
  db/migration/ UNCHANGED V1–V5, NEW trong main bản lịch sử V6 nguyên văn và V7 nâng cấp
  config        MODIFIED application.properties và tài liệu biến cấu hình URL gửi thư
REMOVED         Chưa cần xóa class hoặc package. Bỏ đường tự nhận tài khoản theo email trong service hiện hữu.
```

Controller chỉ nhận form @Valid, BindingResult, principal và điều hướng PRG. Repository cấp projection tối thiểu cho view. Service giữ khóa, kiểm quyền và transaction. Adapter security xác minh provider và trích bằng chứng. Email_Service gửi thư ngoài transaction. Xử lý lỗi form tại form, sai role/CSRF giữ HTTP status của Security. Không dùng endpoint GET thay đổi credential.

| Màn hình / route đề xuất | Thao tác và kết quả |
|---|---|
| /login, /register | Từng nút OAuth chỉ hiện khi provider sẵn sàng. Link “Quên mật khẩu” ở login. Đăng ký OTP thành công quay về login như cũ. |
| /forgot-password, /reset-password | GET form, POST gửi/đổi. Chỉ form reset hợp lệ hiện ô mật khẩu. Không lưu token/bản nhập trong storage, analytics, URL quay lại hoặc flash. Thông báo chung khi xin thư. |
| /account → /account/security | Hiện email chính, trạng thái xác minh và từng phương thức. POST link/unlink có CSRF, xác nhận gần đây và thông báo cụ thể về cách đăng nhập còn lại. Provider tắt vẫn có thể hiện nhãn trạng thái liên kết và hướng dẫn dùng đường khác, ẩn nút bắt đầu OAuth. |
| /account/security/password | Hiện form thiết lập hoặc đổi tùy hash NULL. Bảo toàn focus, lỗi từng trường, accessible name cho nút hiện mật khẩu, mật khẩu nhập lại sau lỗi. |
| /admin/users → /admin/users/{id} | Đề xuất trang chi tiết tối thiểu dùng projection. Nút “Gửi yêu cầu đặt lại mật khẩu” chỉ cho Customer đủ điều kiện. POST /admin/users/{id}/password-reset sau xác nhận và lý do, backend kiểm lại. Hiện “đã gửi yêu cầu”, “máy chủ thư đã nhận” hoặc “chưa gửi được” theo bằng chứng. Quay lại danh sách giữ q/page. Không xuất hash, ID provider đầy đủ hay link reset. |

Ưu tiên form native/PRG, xác nhận có trang fallback khi JS tắt. NAV-01 nhận hợp đồng quay lại URL nội bộ allowlist, giữ bộ lọc; sau thu hồi phiên bắt buộc đi login. UI-01C tiếp nhận fragment, thông báo, focus, zoom/mobile. UI-01P chỉ áp dụng pager cho danh sách hiện có. Danh sách audit cho Admin ngoài phạm vi đề nghị lần này.

## 9. Ma trận nghiệm thu sau khi được duyệt

| Nhóm / mức kiểm | Fixture, kết quả phải quan sát |
|---|---|
| Cấu hình / context + MockMvc | Không có client, chỉ Google, chỉ GitHub, cả hai, thiếu từng nửa, khoảng trắng, local-placeholder → đúng bean, nút, route, form login và ứng dụng vẫn khởi động khi provider tắt. |
| OAuth / provider mô phỏng HTTP | Code exchange, claims/OIDC và GitHub email endpoint. Mỗi provider lần đầu và lần sau cùng user ID, provider đổi email, email không verified, email trùng chưa link, thiếu subject, provider timeout, Admin/locked. Chạy qua filter/callback; chỉ tiêm principal giả chưa kiểm được giao thức. |
| Identity / MariaDB | Hai callback cùng subject, hai provider cùng email, một subject muốn gắn hai Customer, hai subject cùng provider/user → unique, rollback không có user mồ côi. Giữ nguyên FK đơn/giỏ/địa chỉ/wishlist/review. |
| Link và unlink / hai session | Email khác nhưng chứng minh đủ, nhầm user/provider/session, state sai/replay, timeout, logout, callback cũ sau đổi version; phiên đăng nhập ban đầu giữ đúng user. Hai request gỡ đồng thời luôn còn đường login dùng được. Link/reauth không kích hoạt gộp giỏ. |
| Mật khẩu / service + MVC | Hash NULL thất bại form login bình thường, vẫn OAuth được. Tạo lần đầu cần reauth + email, đổi cần mật khẩu hiện tại, sai byte-length/confirmation có lỗi form. Thu hồi cả phiên form/OAuth và link cũ sau commit. |
| Recovery / MariaDB + mail capture | Email tồn tại/không có/bị khóa cho phản hồi công khai tương đương, giới hạn tần suất. Token sai, hết hạn, đã dùng, version sai, GET do scanner, hai POST đồng thời, rollback giữa consume/đổi. Chỉ một hash đổi, không mở khóa, không tự đăng nhập. SMTP fail/timeout, crash trước gửi, xin lại và thông báo sau đổi. |
| Admin và audit / filter thật | Guest, Customer, Admin hết phiên, Admin nhắm Admin khác, tài khoản OAuth-only/bị khóa, thiếu CSRF bị từ chối. Admin hợp lệ gửi link, Customer tự đặt. Kiểm actor/target/correlation, email sent khác completed, transaction rollback và sự kiện lỗi. Quét mail/log/DTO/flash để phát hiện secret bằng marker test; không cấp quyền xem/sửa nhật ký cho Customer. |
| OTP / Redis + mail test riêng | Verify đúng/sai/hết hạn, hai session cùng email, resend/cancel, hai verify đồng thời, SMTP/Redis/SQL lỗi, bảo toàn hash session và unique email. |
| Migration / restore | DB trắng, V5 có dữ liệu, V6 có cặp hợp lệ, legacy chưa rõ nguồn, cặp Admin cần xét. Validate checksum, row/ID counts, thử restore, chạy Flyway lần hai không nhân bản. Kiểm JPA validate và seed cũ. |
| Regression hệ thống | Mật khẩu và từng provider qua LoginSuccess_Handler, session fixation, checkout saved request, merge Guest đúng một lần, COD/MoMo đúng phạm vi sẵn có, catalog, Dashboard, yêu thích, đơn hàng, review, logout, quyền GET/POST tư vấn da, CSRF, HTML 403/404 và API/IPN. |

Gate đề xuất: JDK 21 chạy `mvn test`, `mvn verify` và `mvn -DskipTests package` theo quy ước dự án. Bật fixture MariaDB/MongoDB/Redis cô lập cho suite phụ thuộc, so số pass/fail/error/skip. Kiểm JAR chứa đúng tập migration. Ghi riêng mock provider, SMTP test local và callback Google/GitHub thật với credential test, không lưu secret/log callback URL. Thiếu credential thì ghi chưa xác minh E2E. Chạy CI trên revision được phép sau này. Không lấy kết quả lịch sử làm kết quả nghiệm thu cho các hàng mới.

## 10. Đồng bộ bảy Phase sau khi auth hoàn tất

Baseline auth là điều kiện đầu vào trước SEC-01. Giữ lịch dưới đây; cập nhật manifest, prompt và tiến độ **sau** nghiệm thu auth trên revision cụ thể. Không chuyển thiết kế hôm nay thành công việc đã hoàn thành.

| Phase | WP theo thứ tự | Delta cần ghi vào prompt và gate |
|---|---|---|
| 1 | SEC-01 | Dùng Security_Config và principal mới, NULL password, reset không dò email, CSRF, quyền Admin, version, OAuth-only, trang lỗi. Không xây hệ thống auth lần hai. |
| 1 | DATA-01 | Fixture người mua theo cùng ID, tài khoản mới có/không có hash. Review Mongo tắt không cản auth SQL; auth không fallback hay ghi audit nội dung review. |
| 2 | ARCH-01 | Di chuyển cả ba JPA entity mới khi đổi entity → model. Giữ tên bảng, JPQL, principal mapping và fixture V5/V6/V7. |
| 3 | NAV-01 | Kiểm saved request của login thật, quay về /account/security sau link và ngữ cảnh danh sách Admin. Loại token, state, callback và URL ngoài site khỏi nguồn Back. |
| 3 | UI-01C | Tái dùng form mật khẩu, vùng lỗi, xác nhận Admin, focus, nhãn quản lý tài khoản. Test cả no-JS, zoom và mobile. |
| 3 | UI-01P | Duy trì q/page ở /admin/users. Chỉ phân trang audit nếu owner duyệt riêng màn hình đọc sau này. |
| 4 | CART-01 | Ba phương thức login cùng ID, gộp đúng một lần. LINK/REAUTH/reset không gộp. Giữ scope điều chỉnh tồn dương và notice sau commit. |
| 4 | CAT-01 | Bảo toàn chính sách ngừng hoạt động. Chỉ tái dùng hợp đồng audit nếu được duyệt thêm sự kiện catalog. |
| 5 | WISH-01 | Đổi phương thức đăng nhập vẫn cùng wishlist. Kiểm phiên bị thu hồi khi thao tác từ tab cũ. |
| 5 | ORDER-01 | Ownership dựa trên cùng mã user, tài khoản khóa và phiên cũ không đọc đơn. Audit trạng thái đơn cần phê duyệt riêng. |
| 5 | REVIEW-01 | Quyền mua/review theo ID nội bộ, giữ revision Mongo và scope Moderator hiện thực bằng ROLE_ADMIN. Không ghi kép audit SQL làm nguồn nội dung review. |
| 6 | DTO-01 | Projection tài khoản chỉ gồm trường cần hiển thị; loại hash, token, provider subject và thuộc tính nội bộ khỏi JSON/view. |
| 7 | QA-01 | Chạy lại ma trận mục 9, các suite cũ và CI với baseline mới, phân biệt môi trường mô phỏng/thật. |

Khi auth đạt gate: ghi quyết định và runbook ở DECISIONS, README, ERD, hướng dẫn cấp Admin (tài khoản chuyên dùng có mật khẩu, rà liên kết trước khi nâng quyền), backup/restore và quản lý secret. Ghi phụ lục có phiên bản cho BEAUTYSHOP_FUNCTIONAL_REVIEW, UPGRADE_PLAN, UPGRADE_AUDIT và REVIEW_PROGRESS, giữ nguyên số liệu lịch sử. Cập nhật BEAUTYSHOP_IMPLEMENTATION_PROGRESS, 00-EXECUTION_ORDER, 01–13 theo bảng trên; chỉ bắt đầu SEC-01 khi có phê duyệt triển khai. [Bộ prompt hiện hành](implementation-prompts/00-EXECUTION_ORDER.md) · [Tiến độ](BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md)

## 11. Quyết định trình owner

1. Duyệt V6 lịch sử + V7, ba bảng mới, password nullable và mốc xác minh email, một danh tính mỗi provider trên mỗi Customer. Duyệt chính sách bảo toàn legacy và quy trình hỗ trợ chứng minh quyền sở hữu trước cutover.
2. Duyệt quy tắc step-up, gồm phương án riêng cho GitHub khi không chứng minh được xác thực gần đây; xác minh hộp thư BeautyShop trước recovery. Gỡ phương thức cần chứng minh đường còn lại.
3. Duyệt token MariaDB 15 phút, Redis dùng rate limit/OTP, thu hồi mọi phiên sau thay đổi credential, Admin chỉ gửi yêu cầu đến Customer đủ điều kiện. Duyệt củng cố challenge OTP đăng ký trong cùng đợt hoặc tách phần này có gate riêng.
4. Duyệt nhật ký chung append-only cho auth/Admin reset, quyền đọc vận hành và thời hạn đề xuất 90 ngày. Mở rộng audit sang catalog, kho, đơn, return, review, voucher và UI tra cứu cần quyết định sau.

Thứ tự triển khai sau phê duyệt: diễn tập dữ liệu và restore → schema tương thích + principal chấp nhận tài khoản không mật khẩu → audit chung → login/link/reauth → mật khẩu/recovery/Admin → regression và kiểm provider thật → đóng baseline và cập nhật 13 prompt. Đây là thứ tự phụ thuộc của đợt xác thực; 7 Phase hiện hữu giữ nguyên.
