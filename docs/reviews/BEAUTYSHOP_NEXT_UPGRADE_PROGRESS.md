# Sổ thực thi nâng cấp · 10 phase, 19 Work Package

**5/19 DONE (BASE, AUTH, QUALITY, PERF, AUDIT).**

## Thứ tự thực thi và trạng thái

Số thứ tự là số tệp. “Chờ BASE/phụ thuộc” xác định gate kỹ thuật. BLOCKED_OWNER xác định quyết định còn thiếu cho toàn gói hoặc phần được nêu. BLOCKED_EXTERNAL xác định môi trường thật chưa được xác nhận. Bảng dưới ghi trạng thái lúc bàn giao prompt; chỉ chuyển IN_PROGRESS khi thực sự được giao và bắt đầu. Với gói có phần độc lập, ghi rõ phạm vi đang làm và giữ phần bị chặn ở BLOCKED.

| STT | Phase | Prompt | Trạng thái | Bước mở khóa / đầu vào | Owner · SHA · PR/CI · kết quả |
|---|---|---|---|---|---|
| 1 | 0 | [BASE](implementation-prompts/next-upgrade/01-BASE.md) | DONE | Owner nghiệm thu 28/11. | `faec999` → `f390c3a` · [PR #6](https://github.com/tuanank5/cosmetics_shop/pull/6) · [CI main PASS](https://github.com/tuanank5/cosmetics_shop/actions/runs/46106902807) |
| 2 | 1 | [AUTH](implementation-prompts/next-upgrade/02-AUTH.md) | DONE | Owner nghiệm thu sau merge. | `0ed547b` · [PR #7](https://github.com/tuanank5/cosmetics_shop/pull/7) · [CI main PASS](https://github.com/tuanank5/cosmetics_shop/actions/runs/46110439984) · [Báo cáo](AUTH-ket-qua.md) |
| 3 | 1 | [AUTH-LIVE](implementation-prompts/next-upgrade/03-AUTH-LIVE.md) | BLOCKED_EXTERNAL | BASE và AUTH đã đạt. Owner cần xác nhận môi trường test, client Google/GitHub, callback, hai Customer, Redis và Mailpit/SMTP thử. | Revision đầu vào `0ed547b` · chưa chạy |
| 4 | 2 | [AUDIT](implementation-prompts/next-upgrade/04-AUDIT.md) | DONE | Owner chọn AUDIT-A. AUDIT PURGE — BLOCKED_EXTERNAL. | `5bdada6` · [Quyết định AUDIT-A](../DECISIONS.md#nghiệm-thu-audit-ngày-28112026) |
| 5 | 2 | [ACCOUNT](implementation-prompts/next-upgrade/05-ACCOUNT.md) | BLOCKED_OWNER / phụ thuộc | Chốt nội dung mail, AUTH và AUDIT sẵn sàng. | — |
| 6 | 3 | [VOUCHER](implementation-prompts/next-upgrade/06-VOUCHER.md) | MERGED / READY_FOR_OWNER_ACCEPTANCE | Chờ owner nghiệm thu. | PR #12 · `e4c00c8` · [Closeout](VOUCHER-ket-qua.md) |
| 7 | 3 | [PAYMENT](implementation-prompts/next-upgrade/07-PAYMENT.md) | PARTIAL_ACCEPTED · READY_FOR_OWNER_ACCEPTANCE (mock) | Bốn gate sau VOUCHER đã chạy trên merge. Chờ quyết định owner. | `e4c00c8` · [Revalidation](PAYMENT-ket-qua.md#revalidation-pr-12) |
| 8 | 3 | [PAY-LIVE](implementation-prompts/next-upgrade/08-PAY-LIVE.md) | Chờ phụ thuộc · BLOCKED_EXTERNAL | Xác nhận môi trường thật, revision và số đầu kỳ. | — |
| 9 | 4 | [ORDER](implementation-prompts/next-upgrade/09-ORDER.md) | Chờ phụ thuộc | Chốt đầu vào AUTH; đồng bộ hai view chi tiết. | — |
| 10 | 4 | [RETURN](implementation-prompts/next-upgrade/10-RETURN.md) | BLOCKED_OWNER | Duyệt 12 và hợp đồng audit. | — |
| 11 | 4 | [REFUND](implementation-prompts/next-upgrade/11-REFUND.md) | BLOCKED_OWNER | Duyệt 3, 13–14 và nguồn bằng chứng chi tiền. | — |
| 12 | 5 | [AI-OPS](implementation-prompts/next-upgrade/12-AI-OPS.md) | Chờ BASE | BASE cho guard/counter; AUDIT cho reindex. | — |
| 13 | 5 | [AI-HISTORY](implementation-prompts/next-upgrade/13-AI-HISTORY.md) | BLOCKED_OWNER | Duyệt 8–9; AUTH và guard đạt; kiểm Mongo fixture. | — |
| 14 | 6 | [NOTIFY](implementation-prompts/next-upgrade/14-NOTIFY.md) | BLOCKED_OWNER | Duyệt 11; sẵn hợp đồng phân quyền và sự kiện gửi. | — |
| 15 | 6 | [SHOP-12](implementation-prompts/next-upgrade/15-SHOP-12.md) | BLOCKED_OWNER | Owner duyệt nội dung từng trang; phối hợp AI-OPS về reindex. | — |
| 16 | 7 | [PERF](implementation-prompts/next-upgrade/16-PERF.md) | DONE | Tree merge trùng tree đã review, regression đạt. Owner xác nhận CI sau merge PASS. | `98d2fa3` · [PR #9](https://github.com/tuanank5/cosmetics_shop/pull/9) · [CI sau merge](https://github.com/tuanank5/cosmetics_shop/actions/runs/46117800147) · [Báo cáo](PERF-ket-qua.md) |
| 17 | 7 | [QUALITY](implementation-prompts/next-upgrade/17-QUALITY.md) | DONE | Review local, tree merge khớp, CI sau merge PASS theo xác nhận owner. | `e33e4e3` · [PR #8](https://github.com/tuanank5/cosmetics_shop/pull/8) · [Nghiệm thu](QUALITY-ket-qua.md) |
| 18 | 8 | [BACKUP](implementation-prompts/next-upgrade/18-BACKUP.md) | BLOCKED_OWNER / phụ thuộc | Duyệt quyền/quota, có fixture và hợp đồng audit. | — |
| 19 | 9 | [UI-UAT](implementation-prompts/next-upgrade/19-UI-UAT.md) | Chờ phụ thuộc | Ghi coverage và revision bàn giao cho từng màn. | — |

## Nhánh công việc có thể làm song song

| Sau mốc | Có thể tiến hành song song khi được giao | Điểm đồng bộ |
|---|---|---|
| Trước khi chốt BASE | Chuẩn bị danh mục quyết định, nội dung SHOP, danh sách fixture và môi trường AUTH-LIVE/PAY-LIVE, checklist UI. BASE kiểm kê và trình SHA. | Owner chọn revision; build sạch, so gate và ghi kết luận BASE. Chưa mở code ứng dụng. |
| BASE đạt | AUTH kiểm mật khẩu/phiên Google, PAYMENT đọc dữ liệu attempt, AI-OPS kiểm guard/counter, PERF đo baseline và QUALITY rà module ổn định. AUDIT bắt đầu khi 6–7 được duyệt; SHOP-12 theo văn bản đã duyệt. | QA/CI của riêng từng branch. Phần PAYMENT bị ảnh hưởng hai mã chờ VOUCHER; AI reindex chờ audit. PERF/QUALITY chỉ sửa nơi có phép đo hoặc finding. |
| AUTH và/hoặc AUDIT đạt | AUTH-LIVE kiểm thật khi có test client; ORDER làm giỏ theo hợp đồng auth. ACCOUNT/NOTIFY cần cả auth và audit. VOUCHER, RETURN, runner BACKUP thực hiện độc lập theo quyết định/quyền tương ứng. | Đồng bộ migration/version và chủ sửa module chung. Serial hóa thay đổi cùng đường khóa/transaction, header, chat, order detail và sổ progress; chuẩn bị fixture riêng. |
| AUTH và guard AI-OPS đạt | AI-HISTORY triển khai trên MongoDB sau duyệt retention/xóa; Nguồn FAQ phối hợp reindex ở SHOP-12. | BACKUP/PERF và UI-UAT kiểm collection/index, chủ sở hữu và luồng khi Mongo lỗi trên revision có history. |
| VOUCHER và PAYMENT đạt | PAY-LIVE chạy Sandbox. REFUND có thể chuẩn bị xử lý attempt sau duyệt quy tắc; hồ sơ trả hàng chờ RETURN. | Đồng bộ đối soát amount, lượt, kho và KPI. Nghiệm thu provider thật riêng với mock. |
| Có bản tích hợp từng nhóm | UI-UAT chạy theo nhóm kể cả khi cổng dịch vụ khác đang chờ. BACKUP diễn tập trên bộ store/schema thật sự của release. | Ghi kết quả/ngoại lệ theo SHA và phán quyết owner. Maps, tệp 91 trang, AI-LIVE, FEFO không là gate toàn hệ thống. |

## Cách cập nhật khi triển khai

Trên mỗi dòng, điền owner, revision gốc/tích hợp, PR/CI và kết quả; nối biên bản theo mẫu `ngày | WP | trạng thái trước → sau | quyết định/phụ thuộc | môi trường/fixture | PASS/FAIL/SKIP/NOT RUN | log/ảnh | bước kế tiếp`. Khi bị chặn, ghi người có thể mở khóa, quyết định cụ thể và phạm vi còn làm độc lập. Chỉ ghi DONE khi checkpoint, regression ảnh hưởng và review/CI đúng revision đã được xác nhận. Giữ lịch sử các lần thử, không cộng gộp suite khác môi trường.

Mục 15 (restore web) và 17 (tự sinh mã SKU) tiếp tục ngoài phạm vi. D48 đã chốt MongoDB cho lịch sử Customer. D49 giao AUTH/AUTH-LIVE phân biệt logout BeautyShop với phiên Google, chỉ sửa lỗi ứng dụng tái hiện được.

## Nhật ký VOUCHER

28/11/2026 · Owner giao VOUCHER-1–3 trên `5bdada6`. V9, hai slot HÀNG/VẬN_CHUYỂN, quota tổng và theo khách, checkout COD/MoMo, audit Admin và migration legacy đang ở gate review local. Chờ VOUCHER-FINAL, PR, CI và nghiệm thu đúng revision. [Báo cáo, regression và manifest](VOUCHER-ket-qua.md)

28/11/2026 · VOUCHER-FINAL · IN_PROGRESS. Đã review SQL, quota, số tiền gateway mock và migration V9. Maven 244 PASS/102 SKIP, MariaDB/MongoDB 127/127, bootstrap 10/10, WebKit 4/4, package PASS. Chờ PR, CI đúng revision và owner nghiệm thu. [Review cuối](../qa/VOUCHER/final-review/review.md)

## Nhật ký AUDIT

28/11/2026 · Owner duyệt AUDIT-D6 và AUDIT-D7. Bắt đầu triển khai trên `4fa226210c95a4539ccb4279a2a78df54af7577f`. Local JDK 21 đạt 240 PASS/93 SKIP, MariaDB 102/102, WebKit 3/3 nhóm, package và diff check PASS. Đợi review, CI và nghiệm thu đúng revision. [Báo cáo và bằng chứng](AUDIT-ket-qua.md)

28/11/2026 · AUDIT-FINAL vẫn IN_PROGRESS. Maven 240 PASS/95 SKIP, MariaDB 104/104, WebKit 3/3 nhóm, package PASS. Purge thật BLOCKED_EXTERNAL chờ nguồn xác nhận. Nhánh qa/audit chờ review, CI và owner. [Review cuối](../qa/AUDIT/final-review/review.md)

## Nhật ký PAYMENT

| Ngày | WP | Kiểm chứng | Gate tiếp theo |
|---|---|---|---|
| 28/11/2026 | PAYMENT-FINAL · PARTIAL_ACCEPTED / BLOCKED_DEPENDENCY | Lịch sử độc lập đã review local trên `98d2fa3`. Maven 238 PASS/89 SKIP, MariaDB 95/95, WebKit 8/8 và package PASS. [Review](../qa/PAYMENT/final-review/review.md) | Chờ review PR, CI, nghiệm thu revision và tích hợp VOUCHER. |

## Nhật ký AUTH

| Ngày | WP và trạng thái | Môi trường, SHA và kết quả | Bước tiếp theo |
|---|---|---|---|
| 28/11/2026 | AUTH · IN_PROGRESS · checkpoint local | Working diff trên `f390c3a`. Đã chặn dùng lại mật khẩu cho cả reset và đổi mật khẩu. WebKit xác nhận phiên cũ mất quyền và cần callback mới sau logout. [Maven](../qa/AUTH/test.txt) · [Tích hợp và E2E](../qa/AUTH/integration.txt) · [Package](../qa/AUTH/package.txt) · [Manifest](../qa/AUTH/source.json) | Owner review kết quả và quyết định các mục 4, 5 và UX bộ chọn Google. Chưa có PR/CI AUTH. AUTH-LIVE chờ nghiệm thu. |
| 28/11/2026 | AUTH · owner duyệt D1–D3 · IN_PROGRESS | Working diff trên `f390c3a`. Đã triển khai bộ chọn khi bấm đăng nhập Google, chốt quyền khôi phục và kiểm đủ 14 mục trên fixture. Maven 233 PASS, 87 SKIP. Suite riêng 39 PASS. Package PASS. [Quyết định](../DECISIONS.md) · [Báo cáo](AUTH-ket-qua.md) · [Bằng chứng](../qa/AUTH/owner-d1-d3/) | Owner review bản sau D1–D3. Chờ phép xuất bản và CI đúng revision. Provider thật chưa chạy. |
| 28/11/2026 | AUTH-FINAL · IN_PROGRESS | Review đã sửa luân chuyển ID phiên khi nhận link reset và tiêu thụ state dưới khóa phiên. Hai POST gỡ đồng thời chỉ ghi một lần. Maven 234 PASS, 88 SKIP và suite riêng 40/40 PASS. Package đạt. [Bằng chứng](../qa/AUTH/final-review/) | Nhánh local `qa/auth` từ `f390c3a`, chưa có commit ứng viên. Owner duyệt commit/push, kiểm CI push/PR rồi nghiệm thu. AUTH-LIVE chưa chạy. |
| 28/11/2026 | QUALITY · IN_PROGRESS, checkpoint local | Owner giao phần rà độc lập trên `0ed547b5d4c18eab46fb3a194fbbdd327c37d827` cộng diff hai tệp. Đã kiểm một lần đọc dư ở chi tiết đơn MoMo. Maven 236 đạt, 88 skip. Regression 94/94, WebKit ba vai trò/kịch bản 3/3, package PASS. [Báo cáo](QUALITY-ket-qua.md) · [QA](../qa/QUALITY/verification/) | Chờ review, CI và nghiệm thu trên SHA ứng viên. AUTH-LIVE vẫn BLOCKED_EXTERNAL. |
| 28/11/2026 | QUALITY-FINAL · IN_PROGRESS | Clone độc lập `target/qa-quality`, nhánh `qa/quality` từ `0ed547b`. Đã review các hunk, khóa đơn, kết quả PAID/PENDING và sáu lỗi bytecode tại root. [Review](../qa/QUALITY/final-review/review.md) | Chờ owner cho phép commit và push, review PR, CI và nghiệm thu đúng revision. |
| 28/11/2026 | PERF · IN_PROGRESS | Owner giao phần độc lập trên e33e4e3. Hai lượt 1.200 request, 0% lỗi. Giỏ/checkout/quote giảm một SELECT. Maven 237 PASS/88 SKIP, regression 94/94, package và WebKit đạt. [Báo cáo PERF](PERF-ket-qua.md) | Chờ review, CI và nghiệm thu đúng revision. |
| 28/11/2026 | PERF-FINAL · IN_PROGRESS | Clone qa/perf từ e33e4e3, source khớp bản đo. Kiểm lại benchmark và regression badge, hai tab ghi đồng thời, voucher và hết hàng. [Review](../qa/PERF/final-review/review.md) | Chờ owner cho phép commit/push, review PR, CI và nghiệm thu đúng revision. AUTH-LIVE vẫn BLOCKED_EXTERNAL. |

## Backlog theo quyết định mới

| Mục | Trạng thái | Điều kiện xem xét lại |
|---|---|---|
| AI-LIVE | Hoãn nghiệm thu provider thật | Owner mở đợt thật, có đúng hai model, fixture nguồn và môi trường. Đợt này AI chỉ dùng mock. |
| LOT-DESIGN | Lưu thiết kế đã có | Owner mở lại phạm vi và chốt điều kiện kho, trách nhiệm, dữ liệu đầu kỳ. |
| LOT-DATA, LOT-SELL, RELEASE theo lô | Hoãn toàn bộ | Duyệt riêng phạm vi lô, kiểm kê/đối soát, migration/rollback và kiểm chứng giao dịch tương ứng. |
| SKU tự sinh, restore từ Admin | Chờ quyết định riêng | Đánh giá phương án và ảnh hưởng trước giao việc. |
| Pin Maps, báo cáo 91 trang | Chờ thông tin để xác minh/bàn giao | Địa chỉ/pin chính thức hoặc bản nộp có phiên bản xác định. |

## Sổ lịch sử trước khi hợp nhất phạm vi

Các trạng thái phía dưới thuộc kế hoạch cũ ngày 27/11.

# Theo dõi kế hoạch tiếp theo

Ngày 27/11/2026 đã đối chiếu working tree với b03e0bf và hoàn thành bộ báo cáo rà soát. [Hiện trạng](BEAUTYSHOP_CURRENT_STATE_REVIEW.md) · [Ảnh hưởng](BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) · [Kế hoạch và gate](BEAUTYSHOP_NEXT_UPGRADE_PLAN.md).

| Phase | WP | Trạng thái triển khai | Điều kiện tiếp theo |
|---|---|---|---|
| 1 | BASE | Chờ owner | Duyệt phạm vi diff và chiến lược revision/CI |
| 2 | AUTH-LIVE | Chờ owner | Test users, callback, Redis, mail thử |
| 2 | PAY-LIVE | Chờ owner | Merchant Sandbox, URL HTTPS nhận IPN |
| 2 | AI-LIVE | Chờ owner | Endpoint và hai model có thể chạy |
| 2 | UI-UAT | Chờ owner | Thiết bị, pin, liên hệ, bản báo cáo nộp |
| 3 | LOT-DESIGN | Chờ owner | Quy tắc hạn dùng, hàng trả, tồn cũ và thanh toán trễ |
| 3 | LOT-DATA | Chờ phụ thuộc và owner | Kiểm kê, thiết kế đã ký, fixture migration |
| 4 | LOT-SELL | Chờ phụ thuộc và owner | Nguồn tồn khả dụng và gate thanh toán |
| 4 | RELEASE | Chờ phụ thuộc và owner | Chạy thử, đối soát và phương án khôi phục |

Khi bắt đầu mỗi WP, ghi người duyệt, ngày, revision, môi trường, test thực chạy, pass/fail/skip và liên kết bằng chứng. Trạng thái 13 WP đời trước tiếp tục theo BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md.

## Mốc nghiệm thu khôi phục từ stash VOUCHER

| Ngày | Mốc | Bằng chứng | Phạm vi tại thời điểm ghi |
|---|---|---|---|
| 28/11/2026 | BASE · Chờ giao → IN_PROGRESS, gate FAIL | Owner chọn `3ea897b4fe9905d4007d0ead38278f38e8bf806b`. Checkout sạch, JDK 21, CI/package PASS. Suite mặc định 231/313, 82 skip. Suite MariaDB/Mongo 329/337, 3 fail, 0 error, 5 skip. Flyway local 8/8, auth migration 2/2, BSON 1/1, P21 1/1. WebKit checkout và FAQ/D35 8/8 đạt. [PR nghiệm thu BASE](https://github.com/tuanank5/cosmetics_shop/pull/6) | Trên schema riêng, catalog CRUD đạt 3/3, G12 và assertion điều hướng vẫn đỏ. Owner chốt xử lý và xác nhận revision sau retest. QA-01/release chờ quyết định riêng. |
| 28/11/2026 | BASE · gate tích hợp FAIL → PASS local · IN_PROGRESS | Revision kiểm `faec99999cd4646ebc64f6f9fbe126aa4cc8bb39`, nhánh local `qa/base-retest` trong clone `target/base-retest`. 7/7 tập trung, 231/313 mặc định, 332/337 tích hợp, 0 fail/error. WebKit checkout và 8/8 FAQ/D35 đạt. [PR nghiệm thu BASE](https://github.com/tuanank5/cosmetics_shop/pull/6) | Git HTTPS bị chặn; GitHub connector yêu cầu approval trong phiên có policy never. Chưa có PR/CI cho SHA mới. Lệnh push và mở draft PR ở checkpoint; owner duyệt ma trận sau CI/review. QA-01/release có quyết định riêng. |
| 28/11/2026 11:06 UTC+7 | BASE · BLOCKED_EXTERNAL tại bước xuất bản | Local `faec999` đã kiểm; remote main `3ea897b`, nhánh QA trả 404, 0 PR và 0 run trên SHA mới. Git CLI bị chặn HTTPS; connector từ chối tạo tree. [PR nghiệm thu BASE](https://github.com/tuanank5/cosmetics_shop/pull/6) | Push từ phiên có quyền ghi, mở draft PR, đối chiếu CI theo SHA rồi trình owner duyệt ma trận. |
| 28/11/2026 | BASE → DONE, AUTH → IN_PROGRESS | Owner nghiệm thu merge `f390c3a`. Ba CI đạt; tree trùng ứng viên đã kiểm. | AUTH thực hiện phần độc lập. |
| 28/11/2026 | AUTH → DONE | PR #7, CI push/PR/main PASS, tree merge khớp bản đã kiểm. | AUTH-LIVE chờ owner cấp môi trường thử. |
| 28/11/2026 | QUALITY → DONE | [PR #8](https://github.com/tuanank5/cosmetics_shop/pull/8), ứng viên `efcd5d5`, merge `e33e4e3`, cùng tree đã review. Owner xác nhận CI sau merge PASS. | Chuẩn bị PERF. AUTH-LIVE tiếp tục BLOCKED_EXTERNAL. |
| 28/11/2026 | PERF → DONE | [PR #9](https://github.com/tuanank5/cosmetics_shop/pull/9), merge `98d2fa3`, 49 blob khớp manifest. Owner xác nhận [CI sau merge](https://github.com/tuanank5/cosmetics_shop/actions/runs/46117800147) PASS. | Đề xuất owner giao phần độc lập của PAYMENT. |
| 28/11/2026 | PAYMENT · IN_PROGRESS | Owner giao phần lịch sử V1 sau BASE tại `98d2fa3`. Maven 238 PASS/89 SKIP, MariaDB 95/95, WebKit 8/8, package PASS. [Báo cáo và manifest](PAYMENT-ket-qua.md) | Chờ VOUCHER cho hai slot, review, CI và nghiệm thu đúng revision. |
| 28/11/2026 | PAYMENT-CLOSEOUT · lịch sử MERGED / PARTIAL_ACCEPTED | Merge `4fa226210c95a4539ccb4279a2a78df54af7577f`, tree khớp ứng viên. [PR #10](https://github.com/tuanank5/cosmetics_shop/pull/10) · [CI main](https://github.com/tuanank5/cosmetics_shop/actions/runs/46120901806) PASS theo owner. | Tổng tiền tích hợp VOUCHER, snapshot hai slot, đối soát voucher/payment và regression sau VOUCHER còn BLOCKED_DEPENDENCY. PAY-LIVE nghiệm thu MoMo Sandbox thật riêng. |
| 28/11/2026 | AUDIT → DONE (AUDIT-A) | Owner nghiệm thu D7 và guard D6 trên merge `5bdada6`. [PR #11](https://github.com/tuanank5/cosmetics_shop/pull/11) | Purge thực tế BLOCKED_EXTERNAL cho đến khi nguồn clearance đạt nghiệm thu. |

28/11/2026 · VOUCHER — MERGED / READY_FOR_OWNER_ACCEPTANCE. PAYMENT — PARTIAL_ACCEPTED, bốn gate tích hợp PASS local trên merge với MoMo mock. Chờ owner nghiệm thu VOUCHER và xác nhận phạm vi PAYMENT mock. Tổng 5/19 DONE. AUTH-LIVE và AUDIT PURGE tiếp tục BLOCKED_EXTERNAL. PAY-LIVE chưa nghiệm thu.
