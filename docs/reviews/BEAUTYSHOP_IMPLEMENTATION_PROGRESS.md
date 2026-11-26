# Sổ triển khai BeautyShop

## Trạng thái hiện tại · 26/11/2026

SEC-01, DATA-01, ARCH-01, NAV-01, UI-01C và UI-01P **DONE** tại checkpoint `6368b67 + working diff`. G1–G6 đã đạt trên fixture riêng. CART-01 là gói tiếp theo khi được giao. Chưa commit/push. Phần “Cập nhật sau AUTH-02” bên dưới là baseline lịch sử. [SEC-01](SEC-01-ket-qua.md) · [DATA-01](DATA-01-ket-qua.md) · [ARCH-01](ARCH-01-ket-qua.md) · [NAV-01](NAV-01-ket-qua.md) · [UI-01C](UI-01C-ket-qua.md) · [UI-01P](UI-01P-ket-qua.md)

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
| [CART-01](implementation-prompts/07-CART-01.md) | NOT_STARTED | — | — | — | — | — | — | G3–G5 | CAT-01 |
| [CAT-01](implementation-prompts/08-CAT-01.md) | NOT_STARTED | — | — | — | — | — | — | G3 | WISH-01 |
| [WISH-01](implementation-prompts/09-WISH-01.md) | NOT_STARTED | — | — | — | — | — | — | G2,G4,G5,G8 | ORDER-01 |
| [ORDER-01](implementation-prompts/10-ORDER-01.md) | NOT_STARTED | — | — | — | — | — | — | G1,G4,G6 | REVIEW-01 |
| [REVIEW-01](implementation-prompts/11-REVIEW-01.md) | NOT_STARTED | — | — | — | — | — | — | G2,G5,G6,G9 tích hợp | DTO-01 |
| [DTO-01](implementation-prompts/12-DTO-01.md) | NOT_STARTED | — | — | — | — | — | — | G3 và các màn liên quan đã tích hợp | QA-01 |
| [QA-01](implementation-prompts/13-QA-01.md) | NOT_STARTED | — | — | — | — | — | — | G1–G12 | Owner xem xét phát hành |

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
