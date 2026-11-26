# Sổ triển khai BeautyShop

## Trạng thái hiện tại · 26/11/2026

SEC-01, DATA-01, ARCH-01, NAV-01, UI-01C và UI-01P **DONE** tại commit `38fb1ca` trên `origin/main`. CART-01, CAT-01, WISH-01, ORDER-01, REVIEW-01 và DTO-01 **DONE** tại `38fb1ca + working diff`, G7–G12 đạt trên fixture riêng. QA-01 đã đạt G13 local sau bản sửa HTML RAG và còn **BLOCKED** ở CI của revision tích hợp và quyết định phát hành. Phần “Cập nhật sau AUTH-02” bên dưới là baseline lịch sử. [QA-01](QA-01-ket-qua.md)

## Cập nhật sau AUTH-02

Cả 13 gói đang ở trạng thái NOT_STARTED. SEC-01 chờ owner giao triển khai riêng. AUTH-02 nằm trên working tree của main tại HEAD 72a2805. Maven verify có 226/284 ca đạt, 58 skip. Bộ MariaDB riêng đạt 26/26. Callback Google/GitHub thật, Redis thật, SMTP thật và kết quả kiểm thử thủ công của owner còn chờ ghi nhận. [Nghiệm thu auth](AUTH-02-nghiem-thu-xac-thuc.md) · [Manifest hiện hành](implementation-prompts/00-EXECUTION_ORDER.md)

Tất cả gói đang chờ giao việc. Mốc soạn tài liệu 9a812ca không thay cho baseline khi bắt đầu. Dùng [execution order](implementation-prompts/00-EXECUTION_ORDER.md) và [Audit](BEAUTYSHOP_UPGRADE_AUDIT.md). Chỉ một phiên ghi sổ tại một thời điểm.

| ID | Trạng thái | Commit/baseline | File đã thay đổi | Test đã chạy | Kết quả nghiệm thu | Lỗi phát sinh | Thay đổi so với kế hoạch | Phụ thuộc | Prompt tiếp theo theo lịch |
|---|---|---|---|---|---|---|---|---|---|
| [SEC-01](implementation-prompts/01-SEC-01.md) | DONE | 6368b67 + diff | Error fragment/templates, 3 file test, QA; [báo cáo](SEC-01-ket-qua.md) | 229/288 đạt, 59 skip; MariaDB 21/21; package OK | G1 đạt trong fixture: 404 ownership, 403 role/CSRF, HTTP dispatch/upload/IPN/UI | DH-05 phiên owner chưa tái hiện | Không đổi contract backend; thêm nội dung 429 | Baseline | DATA-01 (chờ giao riêng) |
| [DATA-01](implementation-prompts/02-DATA-01.md) | DONE | 6368b67 + SEC-01/DATA-01 diff | Store/seed, KPI/thẻ, fixture, runbook và QA trình duyệt. [Báo cáo](DATA-01-ket-qua.md) | Maven 229/287, 58 skip, package OK. MariaDB/Mongo 22/22, Mongo-off 8/8, restore OK | G2 đạt. Chrome headless đạt keyboard/no-JS/responsive ở 390, 768 và 1366px | Native pipe lỗi, đã dùng Chrome headless riêng | Store Mongo duy nhất, trạng thái unavailable cho thẻ và KPI | Baseline | ARCH-01 |
| [ARCH-01](implementation-prompts/03-ARCH-01.md) | DONE | 6368b67 + diff | 11 lớp model, tham chiếu Java, QA và tài liệu. [Báo cáo](ARCH-01-ket-qua.md) | Maven 229/288, 59 skip, package OK. Tích hợp 48/49, BSON 3/3 | G3 đạt: JPA/JPQL, V1–V7, đọc/ghi BSON hai chiều | Lớp build cũ trong target, đã tách và chạy lại đạt | Package model, giữ schema và hợp đồng dữ liệu | G1+G2 tích hợp | NAV-01 |
| [NAV-01](implementation-prompts/04-NAV-01.md) | DONE | 6368b67 + diff | Home carousel, PDP/card, checkout/địa chỉ, Admin và QA. [Báo cáo](NAV-01-ket-qua.md) | Maven 230/289, 59 skip, package OK. Fixture 30/30 và Chrome baseline/sau sửa | G4 đạt: logo reset, Back/Forward, anchor, tab mới, no-JS | Home BFCache chưa quan sát, đã quan sát Forward PDP từ BFCache | Nguồn GET allowlist và anchor vùng/thẻ | G1,G3 | UI-01C |
| [UI-01C](implementation-prompts/05-UI-01C.md) | DONE | 6368b67 + diff | Fragment password/notice/disclosure, catalog/PDP, account Admin, dashboard, CSS/JS, QA. [Báo cáo](UI-01C-ket-qua.md) | Maven 234/293, 59 skip, package OK. Tích hợp 34/34. Chrome 12/12 tổ hợp rect và 8 luồng | G5 đạt trên fixture với bàn phím, mobile, zoom mô phỏng và JS tắt | Maven cần quyền đọc cache khi biên dịch. Chưa dùng tài khoản hoặc dịch vụ xác thực thật | Giữ ô nút 48px, dành hàng menu desktop, thêm trạng thái hiển thị catalog và bảng | G4 | UI-01P |
| [UI-01P](implementation-prompts/06-UI-01P.md) | DONE | 6368b67 + diff | Record + fragment pager, bốn màn, thứ tự Mongo và QA. [Báo cáo](UI-01P-ket-qua.md) | Maven 235/295, 60 skip, package OK. Tích hợp 90/92, 2 skip. Chrome 5/5 | G6 đạt: điểm đầu/giữa/cuối, giới hạn truy vấn, filter và SKU, tie Mongo, mobile/no-JS | Chưa đo tải trang sâu trên dữ liệu vận hành | Một lần đọc lại trang hợp lệ, giữ trạng thái lỗi Mongo và hợp đồng G4/G5 | G4,G5 | CART-01 |
| [CART-01](implementation-prompts/07-CART-01.md) | DONE | 38fb1ca + diff | Service gộp, handler login, notice theo phiên/đích, UI, test và QA. [Báo cáo](CART-01-ket-qua.md) | Maven 236/300, 64 skip, package OK. Tích hợp 69/69. Chrome 3 nhóm luồng đạt | G7 đạt: 12→10 giảm 2, nhiều SKU, hai tab/phiên, checkout, rollback và retry V3 | OAuth nhà cung cấp thật và hai thiết bị vật lý chưa chạy | Notice một lần qua GET đích; có thể mất khi dừng tiến trình sau commit | G3–G5 | CAT-01 |
| [CAT-01](implementation-prompts/08-CAT-01.md) | DONE | 38fb1ca + CART-01/CAT-01 diff | Route trạng thái, service/repository, list Admin, D21 và QA. [Báo cáo](CAT-01-ket-qua.md) | Maven 238/304, 66 skip, package OK. Tích hợp 23/23. Chrome 3/3 | G8 đạt, route xóa cha 404 và visibility/FK/snapshot đúng | Kiểm bằng dữ liệu fixture và Chrome headless | POST gán trạng thái, giữ đường ngừng sản phẩm cũ | G3 | WISH-01 |
| [WISH-01](implementation-prompts/09-WISH-01.md) | DONE | 38fb1ca + CART-01/CAT-01/WISH-01 diff | Batch wishlist, POST/GET controller, tim SSR/JS, nguồn NAV, test và QA. [Báo cáo](WISH-01-ket-qua.md) | Maven 238/305, 67 skip, package OK. Tích hợp 97/97. Chrome 5/5 | G9 đạt trên fixture: unique, hai chủ/tab, Back, no-JS, timeout và phiên thu hồi | OAuth thật và thiết bị vật lý chưa kiểm | Cặp POST cũ nhận JSON theo yêu cầu, PRG về nguồn; GET đối soát dùng dữ liệu chủ | G2,G4,G5,G8 | ORDER-01 |
| [ORDER-01](implementation-prompts/10-ORDER-01.md) | DONE | 38fb1ca + CART-01/CAT-01/WISH-01/ORDER-01 diff | Truy vấn, bộ lọc, điều hướng, test và QA. [Báo cáo](ORDER-01-ket-qua.md) | Maven 238/307, 69 skip, package OK. Tích hợp 37/37, Chrome 3/3 | G10 đạt trên fixture với count và thanh toán hiệu lực, link P20, COD/MoMo, Back và phạm vi Customer | OAuth/MoMo thật, thiết bị vật lý và tải DB vận hành chưa kiểm | `status` lặp, `q` mã prefix, 10/20/50, Back theo `list*` và anchor | G1,G4,G6 | REVIEW-01 |
| [REVIEW-01](implementation-prompts/11-REVIEW-01.md) | DONE | 38fb1ca + diff G7–G11 | Controller, service Admin, template/CSS, test và QA. [Báo cáo](REVIEW-01-ket-qua.md) | Maven 239/308, 69 skip, package OK. Tích hợp 33/34, 1 skip. Chrome 6/6 | G11 đạt. Dòng rời lọc, count/focus và URL đúng. Từ chối revision cũ. Outage không báo rỗng | Ca backup tùy chọn của DATA-01 bỏ qua. Chưa kiểm OAuth thật hoặc thiết bị vật lý | PRG với autofocus HTML, dành chỗ flash, redirect trang hợp lệ | G2,G5,G6,G9 tích hợp | DTO-01 |
| [DTO-01](implementation-prompts/12-DTO-01.md) | DONE | 38fb1ca + diff G7–G12 | Projection địa chỉ, catalog Admin và bảo mật. Test và QA. [Báo cáo](DTO-01-ket-qua.md) | Maven 239/309, 70 skip, package OK. Tích hợp 144/144. Đối chiếu 13/13 HTML, Chrome 2/2 | G12 đạt. Số SELECT JPA giữ nguyên khi tăng từ 1 lên 7 dòng, ít cột hơn, HTML và form khớp | Chưa đo catalog thật, OAuth thật hoặc thiết bị vật lý | Giữ model keys, `lienKetCho` trở thành chuỗi email. Giữ PDP và SKU Admin SSR | G3 và các màn liên quan đã tích hợp | QA-01 |
| [QA-01](implementation-prompts/13-QA-01.md) | BLOCKED | 38fb1ca + diff G7–G12, bản sửa HTML RAG và test QA | Bản sửa `/chat`, test đối soát, test RAG, harness BSON/REVIEW-01 và bằng chứng G13. [Báo cáo](QA-01-ket-qua.md) | Compile/package đạt. Maven 241/312, 71 skip. Tích hợp 312/314, 2 skip, 0 fail/error. BSON 1/1 và restore đạt. Chrome RAG-HTML 8/8, sáu nhóm trước dùng lại bằng chứng | G13 local đạt | CI từ xa đạt cho HEAD. Working diff chưa có CI. OAuth/Redis/SMTP thật chưa xác minh | D35 chỉ hiển thị lại câu hỏi hợp lệ. Giữ assertion HTML và đối chứng embedding/chat | G1–G12 | Xác minh CI của revision tích hợp được phép đưa lên, owner quyết định release |

## Cách ghi sau mỗi lần thực hiện

Đổi trạng thái từ NOT_STARTED sang IN_PROGRESS khi thực sự bắt đầu; dùng BLOCKED nếu thiếu điều kiện thiết yếu, hoặc DONE khi gate đạt và diff đã được kiểm. Dấu — biểu thị chưa có ghi nhận. Ghi lệnh/profile, số tổng/đạt/lỗi/skip, đường dẫn log/ảnh, hành vi đã quan sát, phần chưa xác minh và checkpoint HEAD + diff hoặc commit khi được phép. Nếu phải dừng giữa chừng, ghi thao tác cuối, file đang mở và điều kiện tiếp tục.

Nếu thay đổi URL, model key, kiểu DTO, selector, chính sách store, fixture hay gate, ghi mục nhật ký dưới đây, kèm các ID downstream và chỉnh prompt chưa chạy hoặc đánh dấu cần chỉnh trước khi mở. Giữ phiếu test và báo cáo cũ nguyên vẹn. Hoàn tất gói không tự cấp quyền chạy prompt kế tiếp.

## Nhật ký điều chỉnh khi triển khai

Baseline xác thực được ổn định trên mã nguồn `main` tại `72a2805` cộng với thay đổi cục bộ. Xem kết quả Maven và các ca phải giữ trong [AUTH-BASELINE](AUTH-BASELINE-on-dinh-xac-thuc.md). Thứ tự và checkpoint của 13 gói theo Execution Order.

Chưa bắt đầu triển khai.


SEC-01 ngày 26/11/2026: bắt đầu trên HEAD 6368b67 sạch. Đã chuẩn hóa fragment lỗi; chưa nghiệm thu G1. Xem [checkpoint](SEC-01-ket-qua.md). Các đoạn NOT_STARTED phía trên là ghi nhận lịch sử sau AUTH-02.

SEC-01 hoàn tất ngày 26/11/2026: G1 đạt trên fixture MariaDB/Tomcat riêng, ma trận HTTP và UI keyboard/no-JS/responsive. Các đoạn IN_PROGRESS/NOT_STARTED phía trên là lịch sử; trạng thái hiện hành theo bảng. Không commit/push. DATA-01 chưa bắt đầu.

DATA-01 ngày 26/11/2026: tomTatDanhGia chuyển từ Map sang TomTatTheSanPham và KPI review có trạng thái unavailable. NAV-01/UI-01C/DTO-01/QA-01 giữ model key và trạng thái. ARCH-01 chạy lại ca recovery. Xem [checkpoint](DATA-01-ket-qua.md).

UI-01C ngày 26/11/2026: G5 đạt. Trước khi mở UI-01P, đối chiếu allowlist `filterPanel=open|closed` và selector `[data-catalog-filter-pages]` với pager mới. WISH-01 và QA-01 giữ trạng thái bộ lọc qua nguồn PDP và selector thẻ catalog. CART-01 và REVIEW-01 dùng vùng `[data-page-notices]` trong phạm vi đã giao. [Hợp đồng và bằng chứng](UI-01C-ket-qua.md)

UI-01P ngày 26/11/2026: G6 đạt. Trước khi mở ORDER-01 và REVIEW-01, dùng hợp đồng `PhanTrang.tu` và fragment `pagination(pager, label)`. REVIEW-01 giữ lỗi kho phân biệt với trang rỗng và giữ bộ lọc qua PRG. QA-01 có thêm fixture Mongo trùng timestamp và kiểm Back khi tắt JavaScript. [Hợp đồng và bằng chứng](UI-01P-ket-qua.md)

CART-01 ngày 26/11/2026: G7 đạt. `gopSauDangNhap` trả `KetQuaGop`. WISH-01 giữ saved request và loại tham số UI `cartMerge` khi dựng nguồn. QA-01 dùng fixture ba phương thức login cùng ID, hai phiên, rollback và notice checkout. [Hợp đồng và bằng chứng](CART-01-ket-qua.md)

CAT-01 ngày 26/11/2026: G8 đạt. D21 dùng POST trạng thái catalog, hai route xóa danh mục/thương hiệu trả 404. WISH-01 và DTO-01 giữ điều kiện active ở cả bốn cấp và QA-01 giữ fixture hai URL cũ. [Hợp đồng và bằng chứng](CAT-01-ket-qua.md)

WISH-01 ngày 26/11/2026: G9 đạt. DTO-01 giữ ID sản phẩm và tập membership theo principal tại các caller. QA-01 kiểm hai tab, mất phản hồi sau ghi, Guest quay đúng nguồn và hai phiên bị thu hồi. REVIEW-01 giữ hợp đồng PRG của review khi dùng chung controller/service. [Hợp đồng và bằng chứng](WISH-01-ket-qua.md)

ORDER-01 ngày 26/11/2026: G10 đạt. Caller dashboard P20 giữ `status` lặp. Trang Admin dùng `DongDonAdmin`, `BoLocDonHangAdmin` và `PhanTrang`. DTO-01 giữ trạng thái thanh toán hiệu lực, count và projection một dòng mỗi đơn. QA-01 cần cập nhật fixture theo bộ lọc mới để kiểm dashboard, Back theo `list*` và anchor, cùng ownership qua ba phương thức đăng nhập. REVIEW-01 dùng lại fragment pager hiện hành. [Hợp đồng và bằng chứng](ORDER-01-ket-qua.md)

REVIEW-01 ngày 26/11/2026: G11 đạt. `GET /admin/reviews` có thể chuyển hướng theo bộ lọc và trang hợp lệ khi MongoDB đọc được. `#review-results` là tiêu đề nhận focus khi dòng không còn trong trang. QA-01 cần giữ fixture dòng cuối, hai tab, mất phản hồi và outage. DTO-01 giữ model trạng thái kho, revision và projection review hiện hành. [Hợp đồng và bằng chứng](REVIEW-01-ket-qua.md)

DTO-01 ngày 26/11/2026: G12 đạt. `diaChi` và `items` giữ model key cùng trường Thymeleaf, producer dùng các record đọc. `auth/security.html` nhận `lienKetCho` là chuỗi email, POST vẫn đọc bằng chứng từ session. QA-01 cần giữ ca xác nhận OAuth fixture, checkout/address và so HTML/cột SQL theo nhóm màn. [Hợp đồng và bằng chứng](DTO-01-ket-qua.md)

QA-01 ngày 26/11/2026: Lượt hồi quy mới kiểm Flyway tới V7, BSON đúng namespace và khôi phục SQL/Mongo/ảnh trong cùng lượt. Chrome REVIEW-01 đạt lại focus tab cũ và bề rộng nút. RAG mock xác nhận email bị trả lại trong HTML và không phát sinh request tới hai endpoint AI của fixture. AUTH-02 đạt 12/12 trong ba suite chỉ định. CI GitHub đạt cho HEAD, working diff chưa có run tương ứng. [Báo cáo](QA-01-ket-qua.md)

QA-01 sau D35 ngày 26/11/2026: HTML hết phản chiếu dữ liệu bị từ chối, RAG tích hợp 3/3 và Chrome trang chat 8/8. Hồi quy MariaDB/MongoDB, migration và phục hồi đạt. Chờ CI cho revision tích hợp và quyết định phát hành. [Báo cáo](QA-01-ket-qua.md)
