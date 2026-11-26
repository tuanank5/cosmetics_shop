# REVIEW-01 · UI kiểm duyệt

## Cập nhật sau AUTH-02

Quyền mua/review theo ID nội bộ, giữ revision Mongo và scope Moderator hiện thực bằng ROLE_ADMIN. Không ghi kép audit SQL làm nguồn nội dung review. Kiểm Customer OAuth bị từ chối duyệt và Admin gặp xung đột revision. [Nghiệm thu AUTH-02](../AUTH-02-nghiem-thu-xac-thuc.md)

Khi owner giao prompt này, hãy thực hiện duy nhất REVIEW-01, Phase 5, và dừng sau checkpoint G11. BeautyShop là monolith Java 21, Spring MVC, Thymeleaf, JPA/JDBC, MariaDB và MongoDB cho nội dung review. Controller nhận HTTP/form, service giữ nghiệp vụ/transaction, repository đọc và ghi dữ liệu.

## Mục tiêu và quyết định

Sửa focus/flash/clamp trên PRG, thêm partial vùng kết quả nếu cần, reuse pager Không giữ dòng sai filter; giữ revision/CSRF/Admin, có UI degraded, không duyệt tự động sau conflict.

Giữ các quyết định owner mới nhất. Bảng review MariaDB cũ chỉ lưu lịch sử; không xóa, đồng bộ, ghi kép hoặc chuyển sang Mongo. Giữ quy tắc giá/ảnh SKU hiện tại. Phạm vi thanh toán Admin là trạng thái hiệu lực như chi tiết. Nhãn màn quản lý cả Customer và Admin là “Quản lý tài khoản” (UI-01C phụ trách); giữ các vai trò hiện tại.

## Trước khi sửa

1. Đọc AGENTS.md và các nguồn bắt buộc trong đó (README, SRS, Business Rules, ERD, DECISIONS). Đọc [thứ tự và checklist](00-EXECUTION_ORDER.md), các hàng REVIEW-01 ở [Audit §6–9](../BEAUTYSHOP_UPGRADE_AUDIT.md), mục liên quan của [kế hoạch](../BEAUTYSHOP_UPGRADE_PLAN.md), [báo cáo chức năng](../BEAUTYSHOP_FUNCTIONAL_REVIEW.md) và [nhật ký rà soát](../BEAUTYSHOP_REVIEW_PROGRESS.md). Giới hạn việc đọc mã ở caller, dependency và test dưới đây.
2. Kiểm tra `git status --short --branch --untracked-files=all`, `git rev-parse HEAD`, `git diff --stat`, `git diff --cached --stat`. Đối chiếu [tiến độ triển khai](../BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md), thay đổi chưa commit, diff/checkpoint của gói trước. Bảo toàn file owner. Không reset/clean/stash/restore/checkout. Một phiên sửa duy nhất trong working tree.
3. Điều kiện vào: Trace rect/flash, fixture cuối trang và revision thay. Phụ thuộc kỹ thuật: **G2,G5,G6,G9 tích hợp**. Theo lịch tuần tự, xác nhận kết quả gói ngay trước đã tích hợp; chỉ dùng ngoại lệ song song đã được điều phối ở file 00. Nếu contract trên HEAD khác Audit, lần theo caller/test mới và ghi tác động trước khi thực hiện. Nếu thiếu điều kiện trọng yếu, ghi BLOCKED và nêu đúng bằng chứng còn thiếu.

## Phạm vi và trình tự thực hiện

File/class được xem xét: AdminSauMua_Controller, SauMua_Service phần admin, admin/user/reviews, CSS/JS review; dùng pager/notice có sẵn. Kiểm tra đường dẫn hiện hành bằng rg; đặc biệt sau ARCH-01 dùng package trên HEAD. Biên dùng chung: Ràng buộc revision, focus/count/filter, lỗi Mongo; tránh sửa store/DTO đã khóa. Chỉ sửa caller, test và tài liệu trực tiếp cần cho gói; ghi lý do đối với mỗi file ngoài danh mục dự kiến.

1. Ghi rect/focus/loading khi duyệt hoặc ẩn trên filter tất cả và chờ duyệt. Đọc AdminSauMua_Controller.hienThi, SauMua_Service.datHienThi, Mongo store revision và kết quả unavailable G2.
2. Giữ PRG canonical với filter/status/stars/product/page hợp lệ. Dùng G6 clamp trang cuối và G5 alert/nút/badge ổn định. Dòng không còn khớp phải biến mất; tính lại tổng, focus dòng kế/heading, announce.
3. Nếu PRG còn gây nhảy không đạt gate, enhance fetch thay vùng list + count + pager + alert cùng snapshot server. Giữ CSRF/Admin và no-JS. Đừng tự retry mutation khi mất response; lấy lại dữ liệu. Duyệt sai revision → trình bản mới; ẩn giữ precondition hiện hành.
4. Kiểm last row/last page, Mongo-down lúc đọc/ghi, hai tab có người sửa nội dung trước khi duyệt, timeout và quay lại. Không tạo minh chứng giả từ row biến mất hợp lệ; ghi đo trước/sau.

## Bất biến và giới hạn

Giữ form session, quyền backend theo vai trò/chủ sở hữu, CSRF, validation, transaction/locking, snapshot, idempotency, State Machine và định nghĩa P20. Với dữ liệu giao dịch đọc hợp đồng docs/design/03-order-payment-inventory.md. Chỉ TK-04 thêm chi tiết khi gộp vượt tồn dương. Không thêm dependency, migration, sửa migration cũ, thay schema hay mở rộng tính năng ngoài gói. Không hard-delete danh mục/hãng như một giải pháp. Gác SHOP-12, AI/RAG/Ollama, phân quyền con, cache/index mới và redesign diện rộng. Không đưa secrets/PII vào URL, log, ảnh QA hoặc storage.

## Kiểm thử, nghiệm thu và quay lui

- Thiết lập fixture mô tả ở các bước trên. Tạo/cập nhật test cho nhánh thành công, lỗi và biên vừa đổi; giữ test nghiệp vụ cũ còn hiệu lực. Regression gate: **G11 AdminDanhGiaPagesTest, MongoDanhGia_Store_Test, browser PRG/partial/JS-off**.
- Dùng JDK 21, chạy `mvn -o test` và `mvn -o -DskipTests package` khi đổi code. Theo profile liên quan chạy tích hợp MariaDB/Mongo bằng database trống riêng, ghi môi trường và số chạy/skip/lỗi. Không chạy seed hoặc restore lên DB owner. Thiếu môi trường phải ghi rõ gate nào còn hở. Với UI kiểm keyboard, no-JS và responsive. Owner tự nhập tài khoản nếu cần.
- Chỉ nghiệm thu khi: **Rời filter đúng, tổng và focus đúng, clamp khi còn dữ liệu, outage không báo rỗng; stale approve bị từ chối**. Báo kết quả quan sát riêng với nhận định từ mã.
- Rủi ro và phương án quay lui: Response chậm/không rõ commit; refetch, rollback về PRG đã clamp. Đây là phương án trình owner khi cần; bảo toàn dữ liệu, không dùng lệnh phá hủy hoặc tự đảo thay đổi của gói khác.

## Bàn giao và dừng

Đối chiếu status, diff tracked/staged và danh sách untracked với baseline. Cập nhật đúng dòng REVIEW-01 trong [sổ triển khai](../BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md): baseline/HEAD hoặc commit nếu được phép, file, test/lệnh và số liệu, kết quả gate G11, lỗi, sai khác và phụ thuộc. Lưu báo cáo docs/reviews/REVIEW-01-ket-qua.md; log đã lọc trong docs/qa/REVIEW-01/ và ảnh cần thiết trong docs/ui/screenshots/REVIEW-01/. Nếu thay đổi một contract, ghi rõ phiên bản, caller và prompt chưa chạy bị ảnh hưởng; cập nhật nội dung prompt đó hoặc đánh dấu cần hiệu chỉnh trước khi mở gói. Không sửa nhận xét kiểm thử của owner.

Trả lời bằng tiếng Việt với kết quả, bằng chứng, điểm chưa xác minh và đề xuất checkpoint HEAD + diff. Commit/push chỉ khi owner cho phép riêng. Dừng ở REVIEW-01; không tự chạy prompt kế tiếp.
