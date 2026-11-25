# Thứ tự thực hiện BeautyShop

Bộ này sẵn sàng để owner đọc và duyệt **từng lần triển khai**. Khi giao việc, gửi nguyên file có số thứ tự. Mỗi phiên chỉ làm một gói. Mốc tác giả dùng để soạn là main, 9a812ca433be3875dd423ff69aacaa31cc00ed1c. Khi chạy phải lấy HEAD và working tree lúc đó. [Audit §6–9](../BEAUTYSHOP_UPGRADE_AUDIT.md) quyết định thứ tự và hợp đồng, [kế hoạch](../BEAUTYSHOP_UPGRADE_PLAN.md) giải thích thiết kế, [rà soát chức năng](../BEAUTYSHOP_FUNCTIONAL_REVIEW.md) và [tiến độ ba lượt](../BEAUTYSHOP_REVIEW_PROGRESS.md) cung cấp bằng chứng lịch sử. Cập nhật [sổ triển khai](../BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md) sau từng gói.

## Quyết định mới và đối chiếu độ bao phủ

Bốn quyết định mới thay các câu hỏi còn mở trong Audit. DATA-01 giữ nguyên dữ liệu lịch sử ở bảng review SQL và bỏ đường dùng bảng làm store. UI-01C xác minh phạm vi màn tài khoản, đổi nhãn thành **Quản lý tài khoản** nếu gồm cả Customer/Admin, giữ nguyên quyền và truy vấn. Mọi gói giữ quy tắc giá/ảnh SKU hiện tại. ORDER-01 chỉ lọc thanh toán hiệu lực như chi tiết đơn.

| Yêu cầu | Phân loại và nơi chịu trách nhiệm |
|---|---|
| Trang chi tiết Customer/Admin | **Bao phủ một phần trong manifest cũ.** NAV-01 phụ trách đường quay lại; UI-01C kiểm kê PDP, đơn, trả, địa chỉ/checkout và màn Admin hiện hữu, áp các component tiêu đề, hành động, trạng thái, focus/responsive khi có sai khác cụ thể. DTO-01 lo dữ liệu. QA-01 kiểm theo màn. Redesign mỹ thuật toàn trang cần duyệt brief và phạm vi riêng. |
| Điều hướng toàn hệ thống | NAV-01 lập bảng nguồn → đích → quay lại cho Guest/Customer/Admin; QA-01 kiểm Back, link trực tiếp, nhiều cấp và mục biến mất. |
| Checkout → thêm/sửa địa chỉ → checkout | NAV-01 chịu cả lưu/hủy/validation và mở form trực tiếp; kiểm quyền và tính lại báo giá. Trước đây đã có thiết kế nhưng gate G4 còn thiếu ca cụ thể. |
| Cập nhật từng phần | WISH-01 bắt buộc tim tại chỗ; REVIEW-01 theo kết quả đo PRG; giữ cart.js. Fetch cho search/pager/quote/dashboard và tác vụ quản trị khác chỉ khi có bằng chứng UX, cần duyệt thêm. |
| Design System | UI-01C sở hữu fragment/tokens, form, focus, notice và layout chi tiết; UI-01P sở hữu pager, SEC-01 sở hữu nội dung lỗi. Giữ font và phong cách. |
| Details/Summary | UI-01C phân loại native, SSR lọc, Back, menu/dialog; NAV-01 cung cấp entry nguồn. |
| Pagination | UI-01P cho màn đã có phân trang, ORDER-01 cho list đơn Admin, REVIEW-01 xử lý trang cuối sau ghi. Danh sách nhỏ giữ nguyên. |
| DTO/Projection/ViewModel | DTO-01 chọn theo lợi ích thực, giữ input form tách đường đọc. |
| N+1 | DTO-01 đo/select field; WISH-01 đọc theo batch; ORDER-01 đẩy query xuống DB. Tối ưu riêng badge/giỏ Guest/ranking cần số đo. |
| Index | DATA-01 bảo vệ unique index review. Index SQL mới cần EXPLAIN, đánh giá dữ liệu và phê duyệt riêng. |
| Cache | Chưa cần thêm Caffeine/Redis; tránh stale giá/tồn, quyền và dữ liệu Customer. |
| Thumbnail/WebP/ảnh | Giữ ảnh/giá SKU, preview, lazy load, placeholder; backlog riêng khi có số liệu payload/LCP. |
| Bảo mật tài khoản | SEC-01 giữ quyền/status, QA kiểm session/CSRF/logout; UI-01C đổi nhãn nếu đúng. Reset password, email, 2FA và thay quyền ngoài gói hiện tại. |
| Background job | Giữ lịch và batch hiện tại. QA-01 kiểm restart và sự cô lập dịch vụ khi liên quan. Không tạo scheduler ranking. |
| MoMo reconciliation | Giữ job kiểm gateway, trạng thái, hold và idempotency; QA regression mock, ORDER-01 chỉ đọc. Không giải phóng chỉ vì hết thời gian. |
| Backup/restore | Baseline, DATA-01, ARCH-01, QA-01 kiểm kế hoạch phục hồi SQL/Mongo/ảnh trên bản sao độc lập. |
| Tồn kho | Giữ khóa, available, V2, hold và kiểm lại checkout; nhiều lô/FEFO, kiểm kê mở rộng cần duyệt riêng. |
| Đổi trả/hoàn tiền | Giữ state machine; regression snapshot, giới hạn số lượng và trạng thái. Tái nhập có kiểm định mỹ phẩm, hoàn một phần và sổ hoàn là backlog nghiệp vụ. |
| Thông báo | CART-01 chỉ chi tiết khi giảm còn tồn dương; UI-01C vùng alert, REVIEW-01 focus. Inbox, realtime, audit DB cần duyệt thêm. |
| Các việc khác | SHOP-12 gồm toàn bộ footer/nhận diện/chính sách chỉ backlog. Giữ ranking, tìm SKU và D24; tìm gần đúng, quote có thời hạn và lịch sử từng attempt chưa triển khai. AI/RAG/Ollama và vai trò Admin chi tiết ngoài phạm vi. |

Điều chỉnh đề xuất trước triển khai là ghi rõ người chịu trách nhiệm và gate cho UI chi tiết/checkout ở G4, G5, G13 và nhãn tài khoản ở G5. Không thêm gói, không thay ID. Nếu owner muốn thiết kế lại toàn bộ trang chi tiết thay vì chuẩn hóa theo component, cần duyệt riêng trước phần mở rộng.

## Checklist baseline trước SEC-01 (chưa thực hiện)

- [ ] Owner giao SEC-01, một người ghi duy nhất trong working tree. Đọc AGENTS.md. Ghi branch, HEAD, status bao gồm untracked, diff tracked/staged; lưu danh sách thay đổi sẵn có, không ghi đè. Đặc biệt bảo toàn tài liệu untracked và phiếu test của owner.
- [ ] Ghi phiên bản Java 21, Maven, profile và revision thực tế. Chạy compile trước, ví dụ `mvn -o compile`; ghi exit code. Với cùng revision chạy `mvn -o test` và `mvn -o -DskipTests package`. Đối chiếu Surefire: tổng, chạy đạt, failure, error, skip và tên ca bị chặn; tách từng profile. Ghi test đỏ có sẵn và lý do skip. Không dùng số 270/48 của lần P20C làm baseline.
- [ ] Kiểm tra đích hiệu lực của từng suite qua cấu hình/test trước khi chạy: MariaDB test trống riêng + P01_MARIADB_TEST=true khi dùng, Mongo test riêng, uploads fixture riêng; phân biệt với dev/demo/prod. Không dùng dữ liệu owner để seed/test. Ghi môi trường thiếu (ví dụ Mongo, merchant, Ollama, đăng nhập Admin) và tác động lên gate; không biến chưa chạy thành đạt.
- [ ] Xác định dữ liệu cần bảo toàn: dump SQL gồm cả bảng review cũ, Mongo collection cùng index/metadata, thư mục ảnh và tham chiếu UUID. Với gói liên quan dữ liệu, kiểm khả năng khôi phục trên đích cô lập, kiểm số lượng/index/mẫu/ảnh trước khi thao tác; không restore đè. Không đưa dump thật, secrets, token hoặc PII vào báo cáo/Git.
- [ ] Lưu bằng chứng baseline có ngày/revision trong docs/qa/SEC-01/ và dòng SEC-01 trong sổ. Nếu baseline lỗi, tách lỗi cũ với mới và xác định có cản nghiệm thu không; thiếu gate trọng yếu thì ghi BLOCKED, không tự bỏ gate.

## Execution Order

| Thứ tự | Prompt | Phase | Phụ thuộc | File dùng chung | Nguy cơ xung đột | Model | Reasoning | Regression Gate |
|---|---|---|---|---|---|---|---|---|
| 1 | [SEC-01](01-SEC-01.md) | 1 | Baseline | Security_Config, error views | Handler HTML vs IPN/API, không đổi mã gốc. Chỉ sửa security chain nếu trace đòi hỏi | gpt-6.2-astra | high | G1 MockMvc + DonHangCOD_MariaDb_Test, lỗi form/upload và API/IPN |
| 2 | [DATA-01](02-DATA-01.md) | 1 | Baseline | SauMua_Service, ThongKe_Service, review fixtures | Types RONG vs unavailable, bean constructor, seed, collection/index. V1 SQL chỉ đọc | gpt-6.2-astra | high | G2 SauMua_MongoDanhGia_Test, MongoTat_MariaDb_Test, ThongKeServiceTest, GoiYPhoBienServiceTest, 2 create cùng item |
| 3 | [ARCH-01](03-ARCH-01.md) | 2 | G1+G2 tích hợp | model/import main và test | FQCN và BSON, JPQL/mapping; không đổi tên table/collection/DTO | gpt-6.2-astra | high | G3 compile cả test, MariaDB validate và Mongo integration trên bản sao |
| 4 | [NAV-01](04-NAV-01.md) | 3 | G1,G3 | header, product-card, home-carousel.js | GET/query/context path, dùng cùng helper cho entry/exit. Không nhận redirect ngoài site | gpt-6.2-astra | high | G4 P18/P21 browser, LoginRedirectTest, no-JS/Back/Forward/BFCache/tab mới |
| 5 | [UI-01C](05-UI-01C.md) | 3 | G4 | app.js, components.css, admin-nav | Hợp đồng stable id, native open, CSS password, vùng notice, Chart.js và breakpoint | gpt-6.2-sol | medium | G5 keyboard, zoom, no-JS, login/register và Admin dashboard |
| 6 | [UI-01P](06-UI-01P.md) | 3 | G4,G5 | pager, AdminSauMua_Controller | Page/size/sort, reviewPage/variant, bảo toàn filter và trạng thái lỗi store; một owner pager | gpt-6.2-sol | high | G6 MockMvc catalog/user/reviews + Mongo timestamp trùng + mobile |
| 7 | [CART-01](07-CART-01.md) | 4 | G3–G5 | LoginSuccess_Handler, layout notice | Thay boolean ở caller + test cùng gói. Alive session + transactional return, không đổi order/stock | gpt-6.2-astra | high | G7 XacThuc_MariaDb_Test, GioHangPagesTest, hai tab/thiết bị và checkout |
| 8 | [CAT-01](08-CAT-01.md) | 4 | G3 | Admin catalog, D21 | URL delete cũ, route sản phẩm dùng chung, interface JpaRepository; giữ voucher | gpt-6.2-sol | medium | G8 AdminCatalogSecurityTest, DanhMucSanPhamThuongHieu_MariaDb_Test, regression SKU và voucher |
| 9 | [WISH-01](09-WISH-01.md) | 5 | G2,G4,G5,G8 | product-card, SauMua_Service | Membership theo product ID/principal, GET origin, PRG/partial/CSRF; dùng selector NAV | gpt-6.2-sol | high | G9 MockMvc/customer scope, MariaDB unique, no-JS, lỗi mạng/Back/2 tab |
| 10 | [ORDER-01](10-ORDER-01.md) | 5 | G1,G4,G6 | order/list, DonHang_Repository | SQL + count, effective status, shared template Customer/Admin và status lặp P20 | gpt-6.2-astra | high | G10 MariaDB query/count, dashboard link lặp status, P20 KPI, regression COD/MoMo |
| 11 | [REVIEW-01](11-REVIEW-01.md) | 5 | G2,G5,G6,G9 tích hợp | SauMua_Service, admin/user/reviews | Ràng buộc revision, focus/count/filter, lỗi Mongo; tránh sửa store/DTO đã khóa | gpt-6.2-sol | high | G11 AdminDanhGiaPagesTest, MongoDanhGia_Store_Test, browser PRG/partial/JS-off |
| 12 | [DTO-01](12-DTO-01.md) | 6 | G3 và các màn liên quan đã tích hợp | model keys, controller/view | Không đổi form write/model keys riêng lẻ. Không đưa toàn entity lên JSON | gpt-6.2-sol | high | G12 catalog/SKU/checkout/address MockMvc + CheckoutIntegrityTest |
| 13 | [QA-01](13-QA-01.md) | 7 | G1–G12 | fixture DB và sổ QA | Không ghi đè phiếu 73 ca, log cũ, fixture user hay DB vận hành | gpt-6.2-sol | high | G13 JDK21 test/package, MariaDB/Mongo riêng, mock MoMo, 390/768/1366/1920px, JS on/off |

Model và reasoning là gợi ý cho từng phiên. Astra/high dành cho ownership, cutover dữ liệu, rename BSON, điều hướng nhiều tầng, concurrency và SQL thanh toán. Sol phù hợp thay đổi component, truy vấn/DTO đã khoanh vùng và QA theo manifest.

**Điều phối.** Thứ tự 01–13 là mặc định. Hoàn tất gate G tương ứng, ghi HEAD/diff, file, test và bằng chứng rồi tích hợp trước khi gói phụ thuộc bắt đầu. Checkpoint có thể là HEAD + diff đã rà; chỉ commit khi owner cho phép. G1/G2 có thể chuẩn bị fixture độc lập. G7/G8 và G9/G10 chỉ có thể triển khai song song nếu dùng checkout/worktree và DB test riêng, hợp đồng dùng chung đã đóng, thay đổi file/test độc lập và có một phiên tích hợp chạy lại cả hai gate. Tránh song song với G3 hoặc giữa WISH và REVIEW. Trong cùng working tree chỉ có một phiên sửa tại một thời điểm, kể cả sổ tiến độ.

**Dừng và xin quyết định** nếu phải đổi schema/migration, thêm dependency, mở quyền, sửa nghiệp vụ ngoài gói, đụng file owner đang sửa mà không thể bảo toàn, hoặc cần dữ liệu thật chưa được phép dùng. Ở G2/G3 cần đường phục hồi dữ liệu; G3 cần bằng chứng tương thích Mongo hai chiều; G13 cần owner quyết định phát hành. Mỗi lần giao prompt là quyền cho đúng gói đó. Không chạy dây chuyền chỉ vì gói trước đã đạt.
