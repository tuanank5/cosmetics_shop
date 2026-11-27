# Sổ thực thi nâng cấp · 10 phase, 19 Work Package

Đã soạn đủ prompt theo thứ tự dưới đây. **Chưa có WP mới đang triển khai hoặc DONE.** SHA ứng viên của lượt rà là `a9870e1`; BASE cần owner chọn revision, đối chiếu build sạch và gate trước khi cho phép code. CI đã đạt trên SHA ứng viên. Sổ [13 WP cũ](BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md), gồm QA-01 BLOCKED, tiếp tục theo bằng chứng đã ghi. [Hiện trạng](BEAUTYSHOP_CURRENT_STATE_REVIEW.md) · [Hồ sơ từng WP](BEAUTYSHOP_NEXT_UPGRADE_PLAN.md) · [Quyết định cần owner duyệt, mục 1–17](BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md)

## Thứ tự thực thi và trạng thái

Số thứ tự là số tệp. “Chờ BASE/phụ thuộc” xác định gate kỹ thuật. BLOCKED_OWNER xác định quyết định còn thiếu cho toàn gói hoặc phần được nêu. BLOCKED_EXTERNAL xác định môi trường thật chưa được xác nhận. Bảng dưới ghi trạng thái lúc bàn giao prompt; chỉ chuyển IN_PROGRESS khi thực sự được giao và bắt đầu. Với gói có phần độc lập, ghi rõ phạm vi đang làm và giữ phần bị chặn ở BLOCKED.

| STT | Phase | Prompt | Trạng thái | Bước mở khóa / đầu vào | Owner · SHA · PR/CI · kết quả |
|---|---|---|---|---|---|
| 1 | 0 | [BASE](implementation-prompts/next-upgrade/01-BASE.md) | Chờ giao BASE | Giao BASE và ghi SHA do owner chọn. | — |
| 2 | 1 | [AUTH](implementation-prompts/next-upgrade/02-AUTH.md) | Chờ BASE · BLOCKED_OWNER một phần | BASE đạt; quyết định 4–5 chỉ chặn thay đổi chính sách tương ứng. | — |
| 3 | 1 | [AUTH-LIVE](implementation-prompts/next-upgrade/03-AUTH-LIVE.md) | Chờ phụ thuộc · BLOCKED_EXTERNAL | Cấp môi trường thật, ghi SHA được nghiệm thu. | — |
| 4 | 2 | [AUDIT](implementation-prompts/next-upgrade/04-AUDIT.md) | BLOCKED_OWNER | Owner duyệt 6–7 và BASE đạt. | — |
| 5 | 2 | [ACCOUNT](implementation-prompts/next-upgrade/05-ACCOUNT.md) | BLOCKED_OWNER / phụ thuộc | Chốt nội dung mail, AUTH và AUDIT sẵn sàng. | — |
| 6 | 3 | [VOUCHER](implementation-prompts/next-upgrade/06-VOUCHER.md) | BLOCKED_OWNER | Owner duyệt 1–3; phối hợp AUDIT. | — |
| 7 | 3 | [PAYMENT](implementation-prompts/next-upgrade/07-PAYMENT.md) | Chờ BASE | BASE cho phần lịch sử; VOUCHER cho tích hợp cuối. | — |
| 8 | 3 | [PAY-LIVE](implementation-prompts/next-upgrade/08-PAY-LIVE.md) | Chờ phụ thuộc · BLOCKED_EXTERNAL | Xác nhận môi trường thật, revision và số đầu kỳ. | — |
| 9 | 4 | [ORDER](implementation-prompts/next-upgrade/09-ORDER.md) | Chờ phụ thuộc | Chốt đầu vào AUTH; đồng bộ hai view chi tiết. | — |
| 10 | 4 | [RETURN](implementation-prompts/next-upgrade/10-RETURN.md) | BLOCKED_OWNER | Duyệt 12 và hợp đồng audit. | — |
| 11 | 4 | [REFUND](implementation-prompts/next-upgrade/11-REFUND.md) | BLOCKED_OWNER | Duyệt 3, 13–14 và nguồn bằng chứng chi tiền. | — |
| 12 | 5 | [AI-OPS](implementation-prompts/next-upgrade/12-AI-OPS.md) | Chờ BASE | BASE cho guard/counter; AUDIT cho reindex. | — |
| 13 | 5 | [AI-HISTORY](implementation-prompts/next-upgrade/13-AI-HISTORY.md) | BLOCKED_OWNER | Duyệt 8–9; AUTH và guard đạt; kiểm Mongo fixture. | — |
| 14 | 6 | [NOTIFY](implementation-prompts/next-upgrade/14-NOTIFY.md) | BLOCKED_OWNER | Duyệt 11; sẵn hợp đồng phân quyền và sự kiện gửi. | — |
| 15 | 6 | [SHOP-12](implementation-prompts/next-upgrade/15-SHOP-12.md) | BLOCKED_OWNER | Owner duyệt nội dung từng trang; phối hợp AI-OPS về reindex. | — |
| 16 | 7 | [PERF](implementation-prompts/next-upgrade/16-PERF.md) | Chờ BASE | BASE và fixture cho benchmark; số đo cho phép sửa. | — |
| 17 | 7 | [QUALITY](implementation-prompts/next-upgrade/17-QUALITY.md) | Chờ BASE | BASE cho rà soát; finding có đối chứng cho code. | — |
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
