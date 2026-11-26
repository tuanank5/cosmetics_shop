# Sổ triển khai BeautyShop

## Cập nhật sau AUTH-02

Cả 13 gói đang ở trạng thái NOT_STARTED. SEC-01 chờ owner giao triển khai riêng. AUTH-02 nằm trên working tree của main tại HEAD 72a2805. Maven verify có 226/284 ca đạt, 58 skip. Bộ MariaDB riêng đạt 26/26. Callback Google/GitHub thật, Redis thật, SMTP thật và kết quả kiểm thử thủ công của owner còn chờ ghi nhận. [Nghiệm thu auth](AUTH-02-nghiem-thu-xac-thuc.md) · [Manifest hiện hành](implementation-prompts/00-EXECUTION_ORDER.md)

Tất cả gói đang chờ giao việc. Mốc soạn tài liệu 9a812ca không thay cho baseline khi bắt đầu. Dùng [execution order](implementation-prompts/00-EXECUTION_ORDER.md) và [Audit](BEAUTYSHOP_UPGRADE_AUDIT.md). Chỉ một phiên ghi sổ tại một thời điểm.

| ID | Trạng thái | Commit/baseline | File đã thay đổi | Test đã chạy | Kết quả nghiệm thu | Lỗi phát sinh | Thay đổi so với kế hoạch | Phụ thuộc | Prompt tiếp theo theo lịch |
|---|---|---|---|---|---|---|---|---|---|
| [SEC-01](implementation-prompts/01-SEC-01.md) | NOT_STARTED | — | — | — | — | — | — | Baseline | DATA-01 |
| [DATA-01](implementation-prompts/02-DATA-01.md) | NOT_STARTED | — | — | — | — | — | — | Baseline | ARCH-01 |
| [ARCH-01](implementation-prompts/03-ARCH-01.md) | NOT_STARTED | — | — | — | — | — | — | G1+G2 tích hợp | NAV-01 |
| [NAV-01](implementation-prompts/04-NAV-01.md) | NOT_STARTED | — | — | — | — | — | — | G1,G3 | UI-01C |
| [UI-01C](implementation-prompts/05-UI-01C.md) | NOT_STARTED | — | — | — | — | — | — | G4 | UI-01P |
| [UI-01P](implementation-prompts/06-UI-01P.md) | NOT_STARTED | — | — | — | — | — | — | G4,G5 | CART-01 |
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
