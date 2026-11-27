# Hiện trạng và đối chiếu phạm vi tại a9870e1

## Revision thực tế

Trước lần sửa tài liệu này, `main`, ref local `origin/main` và remote `refs/heads/main` đều ở `a9870e1cc25d378a34675481d29ea18d8bff4a4b`. Index và working tree sạch, 0 staged, 0 unstaged, 0 deleted, 0 untracked. Sau b03e0bf có đúng một commit. So theo đường dẫn, tắt nhận diện rename, có 80 sửa, 59 thêm, 9 xóa. Trong 59 tệp thêm có bốn báo cáo được viết sau snapshot 55 tệp. Cả DTO bộ lọc Admin và template `chat/index.html` đã tracked. Chín đường dẫn xóa là DTO, hai service/phụ trợ, controller, hai view và ba test luồng cũ D41. Theo dõi đủ pom, src (Java, tài nguyên, migration, template, ảnh, test), workflow và scripts vận hành bằng Git. Build không lấy lớp cũ từ target. [Manifest đầy đủ và lệnh kiểm](../qa/NEXT-PLAN/baseline.json)

.env, uploads thực, ai-data/vector-store, target, log và IDE được ignore. Trong uploads chỉ track .gitkeep. Giữ .env.example, ảnh công khai có nguồn và QA tổng hợp đã làm sạch. Không đưa dump SQL/BSON thật, hộp thư, cookie, token, log thô hoặc dữ liệu khách vào commit. Đây là kiểm tra theo đường dẫn và quy tắc ignore, chưa chứng nhận toàn bộ lịch sử Git sạch secret.

**CI đã xác minh.** Maven CI sự kiện push, completed/success, cập nhật 16:44:06 UTC ngày 27/11. Workflow Ubuntu/JDK 21 chạy Maven test và package skipTests. Shell local dùng Maven 3.9.15 với Java 25 mặc định, chọn JAVA_HOME trong tiến trình riêng xác nhận Temurin 21.0.13.1. BASE vẫn chờ owner chọn revision và nghiệm thu. [Run trên đúng SHA](https://github.com/tuanank5/cosmetics_shop/actions/runs/46035983484) · [Workflow](../../.github/workflows/ci.yml)

**Mức kế thừa.** So tập đường dẫn và SHA-256 của 321 tệp src cộng pom với target/wordmark-fix-20261127, khớp 322/322. WORDMARK-FIX ghi 313 tổng, 231 đạt, 82 skip, package đạt. XML Surefire phân nhóm 67 thiếu P01_MARIADB_TEST, 9 thiếu P23_MONGO_TEST, 3 thiếu LOCAL_BOOTSTRAP_MIGRATION_FIXTURE, mỗi nhóm AUTH_02_MIGRATION_FIXTURE, ARCH_01_BSON_PHASE, P21_DEMO_REPORT thiếu 1. Nhóm đầu tiên chặn thực thi là lý do được JUnit ghi, cần điều kiện lồng Mongo/backup tương ứng khi chạy lại. SCROLL-RESTORE là lượt khác gồm Admin order 3, COD 4, MoMo mock 3, hậu mãi Mongo 5, xác thực 12, đạt 27/27. Hash đã so cho bản WORDMARK, chưa chứng minh toàn bộ fixture/trình duyệt trước đó chạy đúng SHA này. G13 local ở revision lịch sử, QA-01 còn BLOCKED theo sổ và quyết định release. [Maven](../qa/WORDMARK-FIX/maven.txt) · [Integration](../qa/SCROLL-RESTORE/integration.txt) · [G13](QA-01-ket-qua.md) · [13 WP](BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md)

## Bổ sung sau khi lập kế hoạch

**Google sau logout (mục 2–3).** POST /logout qua form có CSRF, Security_Config hủy HttpSession và chuyển về /?logout. Trang login dùng link /oauth2/authorization/google; chưa thấy cấu hình bắt buộc chọn tài khoản. Xác minh GET /account sau logout, trước khi bấm Google. Nếu vẫn được cấp quyền hoặc login tự khởi động OAuth, AUTH tái hiện và sửa lỗi cục bộ. Nếu quyền đã bị chặn và chỉ đăng nhập sau khi bấm nút qua callback mới, AUTH-LIVE đối chiếu SSO ở profile có một/nhiều tài khoản và profile riêng tư. Chưa có nghiệm thu trên trình duyệt owner. Google hỗ trợ prompt=select_account nếu owner muốn luôn hiện bộ chọn. [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect)

**Lịch sử Customer (mục 15).** D48 chốt MongoDB. Ở revision ứng dụng đã rà, chức năng lịch sử vẫn chưa triển khai.

## Bảng đối chiếu 64 mục

Prompt cung cấp 29 nhóm IV cùng các phần II, III, V và VI, chưa kèm bảng số thứ tự gốc 1–64. Mã 01–64 dưới đây do báo cáo gán để truy vết các lựa chọn đã cung cấp. Cột nguồn giữ số mục của prompt. Mỗi trạng thái áp dụng cho phạm vi cụ thể trong dòng. “Có bằng chứng” chỉ phép kiểm đã nêu, không thay chữ ký nghiệm thu toàn hệ thống. Các đề xuất, rủi ro và test tương ứng ở [phân tích tác động](BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [kế hoạch](BEAUTYSHOP_NEXT_UPGRADE_PLAN.md).

**B** ĐÃ TRIỂN KHAI VÀ CÓ BẰNG CHỨNG · **I** ĐÃ TRIỂN KHAI, CHƯA NGHIỆM THU ĐẦY ĐỦ · **P** HOÀN THÀNH MỘT PHẦN · **N** CHƯA TRIỂN KHAI · **U** CHƯA XÁC MINH · **G** GIỮ NGUYÊN · **H** TIẾP TỤC HOÃN · **Q** CẦN OWNER QUYẾT ĐỊNH

| Mã | Mục prompt | WP | Trạng thái | Lựa chọn | Đối chứng và phần còn lại |
|---|---|---|---|---|---|
| 01 | II | BASE | B | Git và CI | main sạch trước rà soát, git ls-remote và run Maven CI cùng SHA. Gate phát hành theo dòng sau. |
| 02 | II | BASE | P | 13 WP và kiểm từ checkout sạch | Sổ triển khai ghi G1–G12 DONE, G13 local đạt, QA-01 BLOCKED. BASE phải liên kết suite, skip và quyết định release. |
| 03 | III | UI-UAT | B | Breadcrumb, đường về, cuộn và focus | NguonDieuHuong, app.js, nav-fields, row/variant anchor. SCROLL-RESTORE, ADMIN-RESTORE, ADDRESS-FOCUS và WISH-RESTORE có ca riêng. |
| 04 | III | UI-UAT | B | Toolbar, pager và tách danh mục/thương hiệu | BoLocDanhSachAdmin, PhanTrang, ui::pagination và AdminDanhMucSanPhamThuongHieu_Controller. LIST-PAGING, NAV-CATALOG kiểm trang cuối và đích riêng. |
| 05 | III,19 | UI-UAT | P | Địa chỉ và PDP | DiaChiNguoiDung_Service khóa người dùng, giới hạn 10, mặc định theo D26/D42. CuaHang_Controller resolve CSRF trước render PDP. Còn race đúng biên 9 và snapshot sau sửa/xóa. |
| 06 | III,18 | UI-UAT | G | Footer, SVG, nút, form, thống kê khách | WORDMARK-FIX, BUTTON-WIDTH, FIELD-SPACING, ADMIN-UX-BIZ-01 và UI-COPY-01. Giữ fragment, chữ và thống kê hiện hành. |
| 07 | 1,26 | AUTH | I | Yêu cầu reset và gửi lại | DatLaiMatKhau_Controller GET/POST /forgot-password trả thông báo chung. MatKhau_Service giới hạn nguồn/email qua Redis, gửi link qua Email_Service. AUTH-02 có fixture. |
| 08 | 1 | AUTH | B | Bảo vệ và tiêu thụ token | taoYeuCau sinh 32 byte ngẫu nhiên, V7 lưu SHA-256, hủy link trước, khóa user, kiểm phiên bản rồi tieuThu. Hai lần POST chỉ một lần đổi. XacThucDaPhuongThuc_MariaDb_Test. |
| 09 | 1 | AUTH | B | Xác nhận và chính sách mật khẩu | auth/reset-password, MatKhauMoi_Form.isHopLe kiểm nhập lại, tối thiểu 12 ký tự theo length Java, tối đa 72 byte UTF-8, @Valid trên reset/đổi. |
| 10 | 1 | AUTH | P | Chặn dùng lại mật khẩu hiện tại | MatKhau_Service.datLai và doi còn encode mật khẩu mới trực tiếp. matches hiện kiểm bằng chứng mật khẩu cũ ở luồng đổi. Bổ sung cả hai đường và test hash BCrypt có salt. |
| 11 | 1,2 | AUTH | I | Đặt mật khẩu lần đầu và thu hồi phiên | doi phân nhánh hash null, cần OAuth proof và email đã xác minh. NguoiDung tăng phiên bản và TrangThaiTaiKhoan_Filter đối chiếu lại. Reset chỉ cho Customer có hash. |
| 12 | 1 | AUTH | Q | Thời hạn link | YeuCauDatLaiMatKhau đặt 15 phút, controller giữ token trong session 900 giây. Đề nghị giữ cùng thời hạn ở hai nơi. |
| 13 | 2 | AUTH | I | Một Customer, ba phương thức | V6 lịch sử, V7 danh_tinh_oauth unique provider/subject và user/provider. dangNhap từ chối email trùng chưa liên kết. /account/security xác nhận trên user đang đăng nhập. |
| 14 | 2 | AUTH | I | Gỡ liên kết và bảo toàn dữ liệu | goLienKet khóa user, yêu cầu đã xác thực bằng đường còn dùng được, tăng version. Kiểm race và cùng ID trên giỏ, đơn, địa chỉ, wishlist. AUTH-02. |
| 15 | 3 | AUTH-LIVE | U | Callback Google và GitHub thật | OAuth2Callback_MariaDb_Test mô phỏng. Hai callback /login/oauth2/code/{registrationId}, từ chối consent, email xung đột và liên kết thành công cần test user thật. |
| 16 | 3,26 | AUTH-LIVE | U | OTP, Redis và SMTP thật | EmailVerification_Service dùng Lua, TTL, giới hạn thử, resend; HttpSession giữ challenge/ngữ cảnh. AUTH-02 chưa chứng minh chuỗi nhận và tiêu thụ OTP thật. |
| 17 | 4 | AUDIT | P | Nhật ký Admin theo nghiệp vụ | V7 nhat_ky_quan_tri, NhatKyQuanTri_Service.ghi MANDATORY có action/target/result/reason/correlation. Thiếu snapshot an toàn và bao phủ thao tác kinh doanh. |
| 18 | 4,18 | AUDIT | N | Trang tra cứu Audit Log và giữ trạng thái | Admin có bảng/pager và NguonDieuHuong dùng lại. Chưa có danh sách/chi tiết nhật ký chuyên dụng có lọc, sort, paging và focus khi Back. |
| 19 | 4 | AUDIT | Q | Danh mục sự kiện và thời hạn lưu | Audit hiện nằm trong transaction. Chốt trường trước/sau, sự kiện thất bại và thời hạn dọn theo mục quyết định. |
| 20 | 5,26 | ACCOUNT | P | Lý do khóa/mở, người thực hiện và thư | AdminNguoiDung_Controller POST /{id}/status gọi datTrangThai với enabled. Thiếu lý do hai chiều, lịch sử gắn thao tác và kết quả gửi mail. |
| 21 | 5 | ACCOUNT | I | Khóa xuyên phương thức và session | datTrangThai khóa có thứ tự, cấm tự khóa. TrangThaiTaiKhoan_Filter kiểm active/version/role mỗi request nghiệp vụ. Kiểm lại form/Google/GitHub và phiên trước khi mở khóa. |
| 22 | 6 | VOUCHER | B | Mã giảm toàn đơn và Admin | PhieuGiamGia_Service.tinh, luuAdmin, V1 phieu_giam_gia. FIXED/PERCENT, ngưỡng, trần, ngày, lượt. admin/voucher/list có q, active, pager, đã dùng/giữ/còn. |
| 23 | 6 | VOUCHER | N | Phạm vi sản phẩm, danh mục, thương hiệu, Customer | tinh chỉ nhận mã và tạm tính. Thiếu điều kiện trên dòng đủ chuẩn và hạn mức theo người mua. |
| 24 | 6 | VOUCHER | N | Mã giảm phí vận chuyển và phối hợp hai loại | don_hang hiện có một mã, ShippingFee_Policy áp phí sau giảm. Cần hai vị trí phân biệt và snapshot phí gốc/giảm. |
| 25 | 6 | VOUCHER | Q | Phạm vi kết hợp, ngưỡng miễn, trả lượt | D10/D27, D11/BR11 và tam_giu_phieu_giam_gia. Chốt ví dụ nhiều dòng/hủy/refund trước đổi hợp đồng. |
| 26 | 6,7 | VOUCHER | P | Giữ lượt và tính lại checkout | apDungCOD, giuOnline, ghiTamGiu, ketThucTamGiu đang cho một mã. Mở rộng cả khóa theo Customer và phân bổ, giữ nguyên giữ SKU, gộp giỏ V3. |
| 27 | 7 | PAYMENT | B | Các lần thử đã lưu | V1 lan_thanh_toan có ma_yeu_cau unique, mã cổng unique theo gateway, tiền, trạng thái và hai mốc thời gian. chuanBi dùng lại PENDING, FAILED phù hợp mới tạo lần khác. |
| 28 | 7 | PAYMENT | P | Hiển thị lịch sử và kết quả đối soát | DonHang_Service.chiTietCustomer/Admin, ThanhToanMomo_Repository.moiNhat và hai view order. Cần timeline theo attempt, mã lỗi đã lọc và nguồn kết quả khi có bằng chứng. |
| 29 | 8 | PAY-LIVE | U | MoMo Sandbox thật | Momo_MariaDb_Test và SCROLL-RESTORE có gateway mock. Merchant, HTTPS IPN và đối sổ cuối trên fixture riêng chưa được chứng minh. |
| 30 | 9 | ORDER | P | Danh sách đơn theo vai trò | AdminDonHang_Controller và order/list xử lý q, status, ngày, sort. DonHang_Controller chỉ phân trang của chủ. Xem bảng từng tiêu chí bên dưới. |
| 31 | 9 | ORDER | G | Chi tiết đơn và thao tác theo trạng thái | order/customer-detail có timeline, snapshot, tiền và retry. order/detail có tóm tắt, giao hàng/thu COD, đối soát/hủy có điều kiện. ORDER-01 và UI-REDESIGN-01/02. |
| 32 | 10 | ORDER | N | Mua lại | GioHang_Service.themGioCustomer/datSoLuongCustomer/gopSauDangNhap đã khóa giỏ và kiểm SKU. Chưa có thao tác mua lại từ đơn thuộc chủ. |
| 33 | 11 | RETURN | B | Yêu cầu, duyệt và nhận hàng | SauMua_Service.yeuCau/chuyenDoiTra, ReturnStatus tách APPROVED và RECEIVED. BR17 giữ 7 ngày và số lượng đã yêu cầu. |
| 34 | 11 | RETURN | N | Kiểm định và nhập lại SKU đúng một lần | chuyenDoiTra chỉ chuyển trạng thái hợp lệ. Thiếu kết luận từng dòng, người/giờ, lượng bán được và bút toán nhập có khóa chống lặp. |
| 35 | 12 | REFUND | P | Hoàn toàn phần COD/MoMo | ThanhToanMomo_Service.apDungThanhCong có REFUND_PENDING, COD thu khi giao. Chưa thấy sổ hoàn theo giao dịch và xác nhận thực trả. |
| 36 | 12 | REFUND | Q | Trả vài dòng so với toàn tiền giao dịch | D12/D29, chi_tiet_doi_tra theo mục. Chốt điều kiện bao phủ đơn và hình thức trả COD trước ghi sổ. |
| 37 | 13,17 | AI-OPS | G | Điểm vào, thuật toán và văn bản | D41 gỡ route/form cũ. TuVan_Service.tuVan dùng topK(6), tối đa 4 đoạn và số thứ tự chọn nguồn. 50 SKU/8/3 của thiết kế cũ không phải hợp đồng hiện hành. |
| 38 | 14 | AI-OPS | B | Validation, PII, rate, concurrency, timeout | TuVan_Form 500, TuVan_Controller 6/session và 30/process/phút, hoi chặn trước embedding/chat, worker 2, timeout hỏi 12s. D35 và CHAT-ROUTE-01. |
| 39 | 14,17 | AI-OPS | P | Guard nguồn tri thức và HTTP mock | KhoTriThuc_Service lọc URL/catalog, guard câu hỏi ở hoi khác với kiểm nội dung trước reindex. Bổ sung ca nguồn bẩn, mock provider lỗi, đếm embedding/chat. |
| 40 | 15 | AI-HISTORY | N | Hội thoại và tin nhắn thuộc Customer | GET/POST /chat dựng model câu hiện tại. Không thấy repository lưu cuộc trò chuyện có owner. Guest tiếp tục dùng như cũ. |
| 41 | 15 | AI-HISTORY | Q | Retention, xóa, ngữ cảnh | D14 áp dụng cho truy cập dữ liệu của AI. Owner chọn lưu để xem. Việc gửi lượt cũ cho model và quyền xóa cần quyết định riêng. |
| 42 | 16 | AI-OPS | P | AI Admin, trạng thái và thống kê | AdminTuVan_Controller.trangThai/napLai, GET /admin/ai và POST /admin/ai/reindex. Có bật/tắt theo bean, số đoạn, lúc nạp. Cần tình trạng gọi cuối và bộ đếm phân loại. |
| 43 | 17 | AI-OPS | G | Kiến trúc và kiểm mock | RagProvider, OllamaRag_Config, TriThuc_Repository, KhoTriThuc_Service, TuVan_Service.TraLoi và TuVan_Controller. Giữ tách hiện có, chỉ tách parser/guard nếu test chứng minh lợi ích. |
| 44 | 17,V | Backlog | H | Ollama thật | TuVanRag_MariaDb_Test dùng mock. Chuyển AI-LIVE ra khỏi gate hiện hành, cần duyệt riêng và model/máy thử để mở lại. |
| 45 | 18,19 | UI-UAT | I | Responsive, keyboard, zoom và điều hướng | BIZ-UX-01/02/03, UI-REDESIGN-01/02, ADMIN-UX-BIZ-01 và browser.json theo màn. Thiếu ma trận vai trò, zoom native và các biên còn lại. |
| 46 | 19 | UI-UAT | U | Pin Maps và hồ sơ 91 trang | fragments/footer có iframe/link ngoài. Chưa có xác nhận vị trí thật hoặc bản nộp xác định 91 trang. Hồ sơ nộp tách khỏi UAT kỹ thuật. |
| 47 | 20 | UI-UAT | G | Fetch chọn lọc và fallback SSR | cart.js/checkout.js dùng AbortController và pageshow, wishlist.js đồng bộ server. Dùng lại form, service và CSRF. Kiểm phản hồi cũ, lỗi và no-JS theo màn. |
| 48 | 21 | PERF | P | Ảnh local, cache, lazy và CLS | product-card chọn eager/lazy có kích thước, Home poster fetchpriority high, PDP 600×600. AnhBienTheSanPham_Controller trả stream. Cần đo header/revalidation và ảnh thay mới. |
| 49 | 22 | BACKUP | B | Công cụ và bằng chứng phục hồi | QA-01/run-data.ps1 -RestoreOnly và DATA-01/run-fixture.ps1 kiểm count/checksum SQL, BSON, unique index và UUID ảnh trên fixture. |
| 50 | 22 | BACKUP | N | Admin quản lý backup | Chưa có route tạo/list/trạng thái/verify/download. Tái dùng công cụ dump với runner cố định, tách hẳn khỏi script QA dựng fixture. |
| 51 | 22 | BACKUP | Q | Restore trên web | Runbook deployment/07 khôi phục sang namespace/schema mới. Phê duyệt riêng cho restore từ web, hiện chỉ đề xuất vận hành ngoại tuyến. |
| 52 | 23 | SHOP-12 | N | Tám trang thông tin cửa hàng | Footer mới có liên hệ/Maps/điều hướng. Chưa thấy tám đích SSR đầy đủ, cần nội dung thật và nối đúng link. |
| 53 | 23,24 | SHOP-12 | Q | Nội dung chính sách và liên hệ | D10/D12/D27/D29, BR, FAQ seed V5 và D37 làm căn cứ. Owner duyệt văn bản, địa chỉ và kênh trước công bố. |
| 54 | 24 | SHOP-12 | P | FAQ công khai có nhóm, tìm và đồng bộ /chat | TriThuc_Repository.faqCongKhai đọc bai_viet_faq. KhoTriThuc_Service.faq và nạp lại chỉ mục có sẵn. Thiếu trang FAQ riêng và nhóm. |
| 55 | 25 | NOTIFY | N | Admin soạn, chọn, xem trước, xác nhận gửi | Luồng lưu mã ở PhieuGiamGia_Service.luuAdmin chưa gửi chiến dịch. Cần thao tác riêng được audit, khóa chống gửi lại. |
| 56 | 25 | NOTIFY | N | Thông báo trong web theo chủ | Thiếu bản ghi người nhận/readAt, badge chưa đọc và đánh dấu. Cần chọn toàn Customer hoặc tập chọn, link đích hợp lệ. |
| 57 | 26 | ACCOUNT | P | Email giới hạn phạm vi | Email_Service đang gửi OTP/link reset và có thư báo đã đổi mật khẩu. Bổ sung khóa/mở và trạng thái thư gắn event. Không phát sinh email đơn hàng. |
| 58 | 27 | PERF | Q | Redis cache nghiệp vụ | StringRedisTemplate phục vụ OTP/rate; session xác thực dùng HttpSession, chưa thấy Spring Session Redis. Cache công khai chỉ sau benchmark và thiết kế invalidation. |
| 59 | 28 | PERF | U | SQL, index và N+1 theo quy mô | DTO-01 có số đo 1/7 dòng và BIZ-UX-01 có giỏ Guest theo batch. Chưa có EXPLAIN/latency cùng fixture lớn cho selector, count, lịch sử mới. |
| 60 | 29 | QUALITY | U | Mã/DTO/query/asset/comment dư | D41 đã xóa hợp lệ. Các ứng viên còn lại phải kiểm route/DI/JPQL/Thymeleaf/CSS/JS/runtime. Chưa kết luận xóa class nào. |
| 61 | VI.17 | Backlog | Q | Tự sinh mã SKU | BienTheSanPham_Form yêu cầu nhập mã. Chỉ đánh giá mã hiển thị unique, giữ PK/FK tự tăng và mã đã tồn tại. |
| 62 | V | Backlog | H | LOT-DESIGN/DATA/SELL, FEFO, HSD/cảnh báo/kiểm kê | D23/D40, TonKho_Service và tài liệu ADMIN-UX-BIZ-01-tiep-tuc. Giữ thiết kế, tồn theo SKU trong mọi WP. |
| 63 | V | Backlog | H | Mở rộng thương mại và email | Hoãn Excel, VIP/thống kê nâng cao, quote có TTL, hoàn một phần, email đơn/trạng thái, lịch sử toàn bộ email/thông báo và tìm gần đúng. |
| 64 | V | Backlog | G | Ranh giới công nghệ và trải nghiệm | Giữ local, Thumbnail/WebP, MVC/Thymeleaf, role, logo, Footer, các trang cũ, thuật toán/fallback chat. Gác CDN/Cloudinary, pipeline/dọn ảnh, SPA, thẻ gợi ý và các mục backlog bổ sung ở báo cáo ảnh hưởng. |

## Đối chiếu danh sách và chi tiết đơn

| Tiêu chí | Customer /orders | Admin /admin/orders |
|---|---|---|
| Tìm mã | Chưa có input/predicate | Tìm tiền tố q |
| Trạng thái | Chưa có bộ lọc | Đa trạng thái đơn, phương thức và trạng thái thanh toán hiệu lực |
| Ngày | Chưa có từ/đến | Có cả hai mốc |
| Sắp xếp | Thứ tự repository, chưa có bộ chọn | Có sort trong BoLocDonHangAdmin |
| Pager | 10 đơn/trang, chỉ theo chủ | Mặc định 20, hỗ trợ 10/50 |
| Kết quả và rỗng | pager.moTa và “Chưa có đơn hàng” | Count của bộ lọc, tổng hệ thống riêng, thông báo rỗng |
| Đường về | Giữ trang và focus dòng | Query allowlist, sort, size, page và focus dòng |

Giữ cỡ trang. Đề xuất bổ sung bốn điều khiển còn thiếu cho Customer nếu đưa vào ORDER, cùng predicate chủ sở hữu cho query dữ liệu và count. Hai trang chi tiết đã có các vùng giao hàng, địa chỉ snapshot, dòng hàng, tổng tiền, giảm giá và hành động theo quyền. Timeline Customer thể hiện bước tiến trình, chỉ ghi giờ sự kiện thật khi có dữ liệu. Ảnh SKU có thể là ảnh hiện tại; bảo toàn tên, giá, số lượng và địa chỉ đã chốt. Chỉ tích hợp lịch sử thanh toán, kiểm định/hoàn tiền và mua lại ở vị trí liên quan. [ORDER-01](ORDER-01-ket-qua.md) · [LIST-PAGING](LIST-PAGING-ket-qua.md) · [BIZ-UX-03](BIZ-UX-03-nghiem-thu-giao-dien.md)

## Điểm vào để kiểm tra lại kết luận

| Phạm vi | Mã nguồn và hợp đồng | Phép kiểm hoặc giới hạn bằng chứng |
|---|---|---|
| Nền, 13 WP | [SRS](../requirements/01-srs.md), [ERD](../design/02-erd.md), [đối chiếu chức năng](BEAUTYSHOP_FUNCTIONAL_REVIEW.md), [manifest](implementation-prompts/00-EXECUTION_ORDER.md) | [Audit cũ](BEAUTYSHOP_UPGRADE_AUDIT.md), [sổ rà soát](BEAUTYSHOP_REVIEW_PROGRESS.md), [sổ triển khai](BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md). Các ca cũ đánh theo gate và revision riêng. |
| Reset, danh tính | [Token, consume và đổi](../../src/main/java/com/beautyshop/service/MatKhau_Service.java), [link/unlink](../../src/main/java/com/beautyshop/service/DanhTinhOAuth_Service.java), [V7](../../src/main/resources/db/migration/V7__xac_thuc_da_phuong_thuc.sql) | [XacThucDaPhuongThuc_MariaDb_Test](../../src/test/java/com/beautyshop/integration/XacThucDaPhuongThuc_MariaDb_Test.java), [AUTH-02](AUTH-02-nghiem-thu-xac-thuc.md). Callback thật còn mở. |
| Audit, khóa và phiên | [ghi trong transaction](../../src/main/java/com/beautyshop/service/NhatKyQuanTri_Service.java), [kiểm phiên mỗi request nghiệp vụ](../../src/main/java/com/beautyshop/security/TrangThaiTaiKhoan_Filter.java), [đổi trạng thái](../../src/main/java/com/beautyshop/service/NguoiDung_Service.java) | [XacThuc_MariaDb_Test](../../src/test/java/com/beautyshop/integration/XacThuc_MariaDb_Test.java), [SEC-01](SEC-01-ket-qua.md). UI audit và thư khóa/mở là phạm vi mới. |
| Mã, kho, thanh toán | [tính/giữ lượt](../../src/main/java/com/beautyshop/service/PhieuGiamGia_Service.java), [retry/IPN/đối soát](../../src/main/java/com/beautyshop/service/ThanhToanMomo_Service.java), [SQL V1](../../src/main/resources/db/migration/V1__khoi_tao_co_so_du_lieu.sql) | [COD](../../src/test/java/com/beautyshop/integration/DonHangCOD_MariaDb_Test.java), [MoMo mock](../../src/test/java/com/beautyshop/integration/Momo_MariaDb_Test.java), [mã](../../src/test/java/com/beautyshop/integration/PhieuGiamGia_MariaDb_Test.java), [BR21](ADMIN-UX-BIZ-01-ket-qua.md). Chưa có ca cho hai mã khác loại. |
| Đơn, mua lại, trả và hoàn | [chi tiết/hủy/thu COD](../../src/main/java/com/beautyshop/service/DonHang_Service.java), [khóa giỏ](../../src/main/java/com/beautyshop/service/GioHang_Service.java), [hậu mãi](../../src/main/java/com/beautyshop/service/SauMua_Service.java) | [ORDER-01](ORDER-01-ket-qua.md), [SauMua_MariaDb_Test](../../src/test/java/com/beautyshop/integration/SauMua_MariaDb_Test.java). REFUND_PENDING là dấu cần xử lý. |
| AI, FAQ | [handler /chat](../../src/main/java/com/beautyshop/controller/TuVan_Controller.java), [nguồn, lựa chọn và timeout](../../src/main/java/com/beautyshop/service/TuVan_Service.java), [FAQ SQL](../../src/main/java/com/beautyshop/repository/TriThuc_Repository.java) | [SKIN-UI-01](SKIN-UI-01-chuan-hoa-tu-van-da.md) đã kiểm component của luồng trước D41; [CHAT-ROUTE-01](CHAT-ROUTE-01-ket-qua.md) kiểm route đã gỡ và /chat hiện hành. Chỉ kế thừa component còn dùng. |
| Giao diện, Fetch, ảnh và nội dung | [giỏ](../../src/main/resources/static/js/cart.js), [checkout](../../src/main/resources/static/js/checkout.js), [thẻ sản phẩm](../../src/main/resources/templates/fragments/product-card.html), [Footer](../../src/main/resources/templates/fragments/footer.html) | [BIZ-UX-01](BIZ-UX-01-ra-soat-nghiep-vu-uiux.md), [BIZ-UX-02](BIZ-UX-02-visual-qa-uiux.md), [UI-REDESIGN-02](UI-REDESIGN-02-ket-qua.md), [ADDRESS-FOCUS](ADDRESS-FOCUS-ket-qua.md), [UI-COPY-01](UI-COPY-01-ket-qua.md). Báo cáo runtime có phạm vi từng màn. |
| Dữ liệu, hiệu năng, backup | [hướng dẫn Flyway](../deployment/01-local-db-and-flyway.md), [harness fixture](../qa/QA-01/run-data.ps1), [nguồn KPI](../../src/main/java/com/beautyshop/repository/ThongKe_Repository.java) | [DTO-01](DTO-01-ket-qua.md), [restore](../qa/QA-01/rag-fix-data/restore.txt), [test plan](../testing/01-test-plan.md), [kiểm thủ công](../testing/03-huong-dan-kiem-thu-thu-cong.md). Không suy thời gian phục hồi production từ dữ liệu fixture. |

## Snapshot lịch sử trước commit tích hợp

Những số liệu, điều kiện FEFO và trạng thái remote trong phần lưu bên dưới thuộc lần rà trước khi có a9870e1.

# Hiện trạng BeautyShop ngày 27/11/2026

## Mốc đánh giá

Branch `main`, HEAD và ref local `origin/main` cùng ở `b03e0bf0533889ca8ae3ff75db7cca6b657efc35`. Lịch sử gần nhất đi từ `38fb1ca → c79983a → daea756 → b03e0bf`. Git xác nhận 195 đường dẫn giữa hai commit cuối. Phạm vi `b03e0bf..HEAD` rỗng. Đây là kết luận về checkout local, chưa truy vấn GitHub trong lượt này.

Trước khi viết bốn báo cáo, index trống, có 80 tệp tracked sửa, 9 xóa và 55 untracked. Hai tệp nguồn mới là `BoLocDanhSachAdmin.java` và `templates/chat/index.html`. Phần còn lại chưa theo dõi gồm báo cáo, QA và ảnh. Khi bàn giao phải lấy đủ tệp mới để build lại từ checkout sạch. Danh mục cuối tài liệu là snapshot trước bốn báo cáo. Diff theo đường dẫn cho thấy Entity, Flyway, scripts, Compose, pom, cấu hình ứng dụng và workflow không đổi so với HEAD.

## Đối chiếu các mốc

| Hạng mục | Đánh giá hiện tại | Bằng chứng và việc còn thiếu |
|---|---|---|
| BIZ-UX-01/02/03 | Đã triển khai, có hồi quy theo luồng | Báo cáo BIZ-UX-01/02/03 và bộ COD, giỏ, tồn, voucher tại checkpoint. Giao diện và đường về đã thay đổi tiếp. |
| UI-REDESIGN-01/02 | Có triển khai, nghiệm thu trực quan một phần | Các template checkout, order, header, wishlist đang tồn tại. UI-REDESIGN-02-ket-qua.md dòng 35–36 thiếu ảnh so sánh 1672×941 và zoom. Các ca Chrome mới chỉ bổ sung màn đã chọn. |
| ADMIN-UX-BIZ-01 | Chặn ngừng hoạt động, footer và bảng đã triển khai và kiểm theo fixture | D40, BR21, NgungHoatDongCatalog_Service, ADMIN-UX-BIZ-01-ket-qua.md. Cần giữ gate giao dịch khi thay cách tính tồn. |
| Kho nhiều lô | Chưa triển khai | V1–V7 và TonKho_Service dùng biến thể làm đơn vị tồn. Mã lô/HSD minh họa trên PDP chưa biểu diễn nhiều lô. |
| FEFO | Hoàn thành một phần thiết kế | ADMIN-UX-BIZ-01-tiep-tuc.md đã có khóa, phân bổ, legacy và rollback. Còn quyết định nghiệp vụ, kiểm kê và diễn tập. |
| Google/GitHub OAuth | Đã tích hợp, thiếu nghiệm thu thật | AUTH-02-nghiem-thu-xac-thuc.md dòng 11, 29–30. Google dùng token ký bởi server mô phỏng, GitHub dùng fixture. |
| Redis/SMTP/OTP | Đã triển khai, thiếu xác nhận toàn chuỗi thật | AUTH-02, docker-compose.yml. Kết nối TCP không chứng minh OTP được nhận, hết hạn, dùng một lần hay giao tới hộp thư ngoài. |
| MoMo | Gateway mock và xử lý giao dịch đã kiểm, Sandbox chưa xác minh | Momo_MariaDb_Test dùng mode mock. SCROLL-RESTORE/integration.txt có 3 ca MoMo. |
| Ollama/RAG | FAQ và RAG mock đã kiểm, provider thật còn mở | CHAT-ROUTE-01 có 6/6 FAQ/RAG. D41 chỉ giữ /chat. Nghiệm thu lại form /tu-van-da không còn phù hợp. |
| Google Maps | Hoàn thành một phần | fragments/footer.html:68–72 có iframe, fallback. ADMIN-UX-BIZ-01-ket-qua.md:90 chưa xác nhận pin và liên hệ chính thức. |
| Zoom native 200% | Thiếu nghiệm thu | ADMIN-UX-BIZ-01:73,91 có reflow mô phỏng và 66 phép đo, thiếu thao tác zoom trực tiếp. |
| Toàn bộ storefront/Admin | Đã kiểm một phần, chịu ảnh hưởng diff | NAV-CATALOG, LIST-PAGING, ADMIN-RESTORE, SCROLL-RESTORE, ADDRESS-FOCUS, WORDMARK-FIX có bằng chứng riêng. Còn thiếu ma trận chung theo vai trò và trạng thái. |
| Báo cáo 91 trang | Chưa đủ bằng chứng | Có docs/report/README.md và tài liệu nguồn. Kiểm kê Git hiện tại không tìm thấy PDF/DOCX xác định bản 91 trang. Cần owner cung cấp bản nộp để đếm trang và đối chiếu. |

## Môi trường và cách chuẩn bị

Maven CLI 3.9.15 mặc định nhận Java 25 trong shell được kiểm. Bản build kiểm thử dùng JDK 21. Chọn JDK 21 qua JAVA_HOME và xác nhận bằng `mvn -version`. Docker CLI chưa có trong PATH của shell. Compose khai báo MariaDB 11.4, MongoDB 7, Redis 7, Mailpit 1.27. Kiểm TCP loopback thấy 8080, 3306, 27017, 6379, 1025 và 8025 mở; 3307 và 11434 đóng. Đây là tín hiệu cổng tại thời điểm rà, chưa định danh tiến trình hoặc xác minh môi trường từ xa.

| Chuỗi | Chuẩn bị cho lần chạy được duyệt | Bằng chứng cần lấy |
|---|---|---|
| SQL/Mongo/Compose | Schema và DB test độc lập, account giới hạn, thư mục ảnh thử. Chọn cổng theo DB_URL. Nếu dùng Docker, chuẩn bị CLI/daemon và kiểm health. README:18 lưu ý lịch sử checksum V5 cũ. | Flyway validate, khởi tạo trống, nâng bản có dữ liệu rồi phục hồi và đối chiếu |
| Redis/Mailpit/Gmail | Chọn đúng endpoint test, cấu hình REDIS_* và MAIL_*, địa chỉ gửi, TLS và hộp thư do owner kiểm soát. Mailpit cho phép nghiệm thu local, Gmail cần tài khoản gửi được phép. | Đăng ký, nhận thư, sai/hết hạn/gửi lại/dùng lại OTP, lỗi gửi, xác minh chỉ một lần |
| Google/GitHub | Client test riêng, AUTH_PUBLIC_URL, callback khớp /login/oauth2/code/google và /github theo cùng tiền tố, test user đã cấp quyền | Callback thật, từ chối cấp quyền, email đã có, liên kết đúng tài khoản |
| MoMo Sandbox | Bộ MOMO_PARTNER_CODE, ACCESS_KEY, SECRET_KEY, REDIRECT_URL, IPN_URL với tiền tố MOMO_; endpoint HTTPS nhận được callback; bật sandbox theo cấu hình dự án | Đối chiếu amount, trạng thái cuối, IPN lặp, voucher, giữ/trả tồn |
| Ollama | Endpoint truy cập được từ ứng dụng, đủ tài nguyên, tải đúng chat model và embedding model, bật AI trên fixture catalog/FAQ | Nạp nguồn, truy xuất, câu trả lời có căn cứ, timeout, câu bị từ chối không gọi provider |
| Maps | Owner xác nhận địa chỉ, pin và liên hệ chính thức, mạng truy cập Google | Mở iframe và liên kết trên mobile/desktop, so pin với vị trí owner duyệt |

.env được Git ignore. Chỉ kiểm tên khóa có khai báo, không kiểm giá trị hoặc hiệu lực. Có khai báo OAuth, Redis, mail và model AI. Năm khóa MoMo nêu trên không xuất hiện dưới dạng có giá trị trong .env đã kiểm; máy chủ có thể cấp từ môi trường. Chỉ lưu kết quả đã ẩn danh của các phép thử.

## Giá trị của bằng chứng

Đối chiếu SHA-256 tất cả 321 tệp src và pom với bản build target/wordmark-fix-20261127 cho kết quả khớp. [Maven cuối](../qa/WORDMARK-FIX/maven.txt) ghi 231 đạt, 82 skip, package đạt. [Tích hợp chọn lọc](../qa/SCROLL-RESTORE/integration.txt) đạt 27/27 trên DB độc lập. Đây là tập chạy riêng, không cộng hai tổng. Chrome headless điều khiển tự động trên fixture khác với thao tác tay của owner. [Chrome logo](../qa/WORDMARK-FIX/browser.json) chỉ đo logo. [Tiến độ 13 WP](BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md) vẫn ghi QA-01 BLOCKED dù G13 local từng đạt; CI của revision chứa diff chưa có. Lượt này chỉ đọc và so bằng chứng.

## Danh mục Git trước khi viết báo cáo

```text
 M docs/03-roadmap-and-team.md
 M docs/DECISIONS.md
 M docs/testing/03-huong-dan-kiem-thu-thu-cong.md
 D src/main/java/com/beautyshop/ai/dto/TuVanDaRequest.java
 D src/main/java/com/beautyshop/ai/service/LoaiSanPham_TimKiem.java
 D src/main/java/com/beautyshop/ai/service/TuVanDaService.java
 M src/main/java/com/beautyshop/controller/AnhTaiLen_Advice.java
 M src/main/java/com/beautyshop/controller/CuaHang_Controller.java
 M src/main/java/com/beautyshop/controller/DiaChiNguoiDung_Controller.java
 M src/main/java/com/beautyshop/controller/DonHang_Controller.java
 M src/main/java/com/beautyshop/controller/NguonDieuHuong.java
 M src/main/java/com/beautyshop/controller/SauMua_Controller.java
 D src/main/java/com/beautyshop/controller/TuVanDa_Controller.java
 M src/main/java/com/beautyshop/controller/TuVan_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminBaoMatTaiKhoan_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminBienTheSanPham_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminDanhMucSanPhamThuongHieu_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminDonHang_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminNguoiDung_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminPhieuGiamGia_Controller.java
 M src/main/java/com/beautyshop/controller/admin/AdminSauMua_Controller.java
 M src/main/java/com/beautyshop/dto/PhanTrang.java
 M src/main/java/com/beautyshop/repository/BienTheSanPham_Repository.java
 M src/main/java/com/beautyshop/repository/DanhMuc_Repository.java
 M src/main/java/com/beautyshop/repository/DoiTra_Repository.java
 M src/main/java/com/beautyshop/repository/DonHang_Repository.java
 M src/main/java/com/beautyshop/repository/NguoiDung_Repository.java
 M src/main/java/com/beautyshop/repository/PhieuGiamGia_Repository.java
 M src/main/java/com/beautyshop/repository/SanPham_Repository.java
 M src/main/java/com/beautyshop/repository/ThuongHieu_Repository.java
 M src/main/java/com/beautyshop/repository/YeuThich_Repository.java
 M src/main/java/com/beautyshop/security/Security_Config.java
 M src/main/java/com/beautyshop/service/BienTheSanPham_Service.java
 M src/main/java/com/beautyshop/service/DanhMucSanPhamThuongHieu_Service.java
 M src/main/java/com/beautyshop/service/DiaChiNguoiDung_Service.java
 M src/main/java/com/beautyshop/service/DonHang_Service.java
 M src/main/java/com/beautyshop/service/NguoiDung_Service.java
 M src/main/java/com/beautyshop/service/PhieuGiamGia_Service.java
 M src/main/java/com/beautyshop/service/SauMua_Service.java
 M src/main/resources/static/css/admin.css
 M src/main/resources/static/css/components.css
 M src/main/resources/static/css/reference-pages.css
 M src/main/resources/static/css/storefront.css
 M src/main/resources/static/img/brand/beautyshop-wordmark.svg
 M src/main/resources/static/js/app.js
 M src/main/resources/static/js/wishlist.js
 D src/main/resources/templates/TuVanDa.html
 M src/main/resources/templates/admin/catalog/form.html
 M src/main/resources/templates/admin/catalog/list.html
 M src/main/resources/templates/admin/user/detail.html
 M src/main/resources/templates/admin/user/list.html
 M src/main/resources/templates/admin/user/reviews.html
 M src/main/resources/templates/admin/variant/form.html
 M src/main/resources/templates/admin/variant/list.html
 M src/main/resources/templates/admin/voucher/form.html
 M src/main/resources/templates/admin/voucher/list.html
 M src/main/resources/templates/auth/account.html
 M src/main/resources/templates/auth/address-form.html
 M src/main/resources/templates/auth/addresses.html
 M src/main/resources/templates/auth/login.html
 M src/main/resources/templates/auth/register.html
 M src/main/resources/templates/auth/security.html
 M src/main/resources/templates/auth/wishlist.html
 M src/main/resources/templates/cart/index.html
 D src/main/resources/templates/chat/TuVanDa.html
 M src/main/resources/templates/fragments/admin-nav.html
 M src/main/resources/templates/fragments/admin-table.html
 M src/main/resources/templates/fragments/footer.html
 M src/main/resources/templates/fragments/nav-fields.html
 M src/main/resources/templates/fragments/ui.html
 M src/main/resources/templates/order/checkout.html
 M src/main/resources/templates/order/customer-detail.html
 M src/main/resources/templates/order/detail.html
 M src/main/resources/templates/order/list.html
 M src/main/resources/templates/order/review.html
 M src/main/resources/templates/product/detail.html
 M src/main/resources/templates/product/list.html
 M src/main/resources/templates/return/detail.html
 M src/main/resources/templates/return/list.html
 D src/test/java/com/beautyshop/ai/service/TuVanDaServiceTest.java
 M src/test/java/com/beautyshop/controller/AnhTaiLenAdviceTest.java
 M src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java
 M src/test/java/com/beautyshop/controller/GioHangPagesTest.java
 D src/test/java/com/beautyshop/controller/TuVanDaControllerTest.java
 M src/test/java/com/beautyshop/controller/TuVan_ControllerTest.java
 M src/test/java/com/beautyshop/controller/admin/AdminDanhGiaPagesTest.java
 M src/test/java/com/beautyshop/integration/XacThuc_MariaDb_Test.java
 D src/test/java/com/beautyshop/repository/TuVanDaRepositoryTest.java
 M src/test/java/com/beautyshop/service/AdminServiceMethodSecurityTest.java
?? docs/qa/ADDRESS-FOCUS/browser.json
?? docs/qa/ADDRESS-FOCUS/maven.txt
?? docs/qa/ADMIN-RESTORE/browser.json
?? docs/qa/ADMIN-RESTORE/integration.txt
?? docs/qa/ADMIN-RESTORE/maven.txt
?? docs/qa/BUTTON-WIDTH/browser-states.json
?? docs/qa/BUTTON-WIDTH/browser.json
?? docs/qa/CHAT-ROUTE-01/http.json
?? docs/qa/CHAT-ROUTE-01/integration.txt
?? docs/qa/CHAT-ROUTE-01/package.txt
?? docs/qa/CHAT-ROUTE-01/test.txt
?? docs/qa/FIELD-SPACING/browser.json
?? docs/qa/FIELD-SPACING/maven.txt
?? docs/qa/LIST-PAGING/browser-count-alignment.json
?? docs/qa/LIST-PAGING/browser-voucher-return-nav.json
?? docs/qa/LIST-PAGING/browser.json
?? docs/qa/LIST-PAGING/integration.txt
?? docs/qa/LIST-PAGING/maven-count-alignment.txt
?? docs/qa/LIST-PAGING/maven-voucher-return-nav.txt
?? docs/qa/LIST-PAGING/maven.txt
?? docs/qa/NAV-CATALOG/admin-integration.txt
?? docs/qa/NAV-CATALOG/browser-addresses.json
?? docs/qa/NAV-CATALOG/browser-admin.json
?? docs/qa/NAV-CATALOG/browser-followup.json
?? docs/qa/NAV-CATALOG/browser-footer.json
?? docs/qa/NAV-CATALOG/browser-tabs.json
?? docs/qa/NAV-CATALOG/browser.json
?? docs/qa/NAV-CATALOG/maven-tabs.txt
?? docs/qa/SCROLL-RESTORE/browser.json
?? docs/qa/SCROLL-RESTORE/integration.txt
?? docs/qa/SCROLL-RESTORE/maven-package.txt
?? docs/qa/SCROLL-RESTORE/maven-test.txt
?? docs/qa/WISH-RESTORE/browser.json
?? docs/qa/WISH-RESTORE/maven.txt
?? docs/qa/WORDMARK-FIX/before.json
?? docs/qa/WORDMARK-FIX/browser.json
?? docs/qa/WORDMARK-FIX/maven.txt
?? docs/reviews/ADDRESS-FOCUS-ket-qua.md
?? docs/reviews/ADMIN-RESTORE-ket-qua.md
?? docs/reviews/BUTTON-WIDTH-ket-qua.md
?? docs/reviews/CHAT-ROUTE-01-ket-qua.md
?? docs/reviews/FIELD-SPACING-ket-qua.md
?? docs/reviews/FUNCTIONAL-REVIEW-doi-chieu-sau-nang-cap.md
?? docs/reviews/LIST-PAGING-ket-qua.md
?? docs/reviews/NAV-CATALOG-ket-qua.md
?? docs/reviews/SCROLL-RESTORE-ket-qua.md
?? docs/reviews/WISH-RESTORE-ket-qua.md
?? docs/reviews/WORDMARK-FIX-ket-qua.md
?? docs/ui/screenshots/WORDMARK-FIX/footer-after.webp
?? docs/ui/screenshots/WORDMARK-FIX/login-after.webp
?? docs/ui/screenshots/WORDMARK-FIX/login-before.webp
?? docs/ui/screenshots/WORDMARK-FIX/opening-reference.webp
?? docs/ui/screenshots/WORDMARK-FIX/register-mobile.webp
?? src/main/java/com/beautyshop/dto/BoLocDanhSachAdmin.java
?? src/main/resources/templates/chat/index.html
```
