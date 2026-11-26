# ORDER-01 · Danh sách đơn Admin

## Cập nhật sau AUTH-02

Ownership dựa trên cùng mã user, tài khoản khóa và phiên cũ không đọc đơn. Audit trạng thái đơn cần phê duyệt riêng. Kiểm ownership sau đổi phương thức đăng nhập và trạng thái thanh toán hiệu lực. [Nghiệm thu AUTH-02](../AUTH-02-nghiem-thu-xac-thuc.md)

Khi owner giao prompt này, hãy thực hiện duy nhất ORDER-01, Phase 5, và dừng sau checkpoint G10. BeautyShop là monolith Java 21, Spring MVC, Thymeleaf, JPA/JDBC, MariaDB và MongoDB cho nội dung review. Controller nhận HTTP/form, service giữ nghiệp vụ/transaction, repository đọc và ghi dữ liệu.

## Mục tiêu và quyết định

SQL cho filter/status/keyword/page/sort, effective attempt rõ, tổng toàn hệ thống tách count lọc Giữ PENDING_PAYMENT/EXPIRED và sáu trạng thái giao/hủy theo enum, UNPAID/PENDING/FAILED/PAID/REFUND_PENDING theo phương thức; giữ POST state machine.

Giữ các quyết định owner mới nhất. Bảng review MariaDB cũ chỉ lưu lịch sử; không xóa, đồng bộ, ghi kép hoặc chuyển sang Mongo. Giữ quy tắc giá/ảnh SKU hiện tại. Phạm vi thanh toán Admin là trạng thái hiệu lực như chi tiết. Nhãn màn quản lý cả Customer và Admin là “Quản lý tài khoản” (UI-01C phụ trách); giữ các vai trò hiện tại.

## Trước khi sửa

1. Đọc AGENTS.md và các nguồn bắt buộc trong đó (README, SRS, Business Rules, ERD, DECISIONS). Đọc [thứ tự và checklist](00-EXECUTION_ORDER.md), các hàng ORDER-01 ở [Audit §6–9](../BEAUTYSHOP_UPGRADE_AUDIT.md), mục liên quan của [kế hoạch](../BEAUTYSHOP_UPGRADE_PLAN.md), [báo cáo chức năng](../BEAUTYSHOP_FUNCTIONAL_REVIEW.md) và [nhật ký rà soát](../BEAUTYSHOP_REVIEW_PROGRESS.md). Giới hạn việc đọc mã ở caller, dependency và test dưới đây.
2. Kiểm tra `git status --short --branch --untracked-files=all`, `git rev-parse HEAD`, `git diff --stat`, `git diff --cached --stat`. Đối chiếu [tiến độ triển khai](../BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md), thay đổi chưa commit, diff/checkpoint của gói trước. Bảo toàn file owner. Không reset/clean/stash/restore/checkout. Một phiên sửa duy nhất trong working tree.
3. Điều kiện vào: Fixture nhiều attempt, status hiệu lực khớp detail. Phụ thuộc kỹ thuật: **G1,G4,G6**. Theo lịch tuần tự, xác nhận kết quả gói ngay trước đã tích hợp; chỉ dùng ngoại lệ song song đã được điều phối ở file 00. Nếu contract trên HEAD khác Audit, lần theo caller/test mới và ghi tác động trước khi thực hiện. Nếu thiếu điều kiện trọng yếu, ghi BLOCKED và nêu đúng bằng chứng còn thiếu.

## Phạm vi và trình tự thực hiện

File/class được xem xét: AdminDonHang_Controller, DonHang_Service phần đọc, DonHang_Repository, ThanhToanMomo_Repository phần semantics, order/list/detail, link dashboard; filter/dòng đọc. Kiểm tra đường dẫn hiện hành bằng rg; đặc biệt sau ARCH-01 dùng package trên HEAD. Biên dùng chung: SQL + count, effective status, shared template Customer/Admin và status lặp P20. Chỉ sửa caller, test và tài liệu trực tiếp cần cho gói; ghi lý do đối với mỗi file ngoài danh mục dự kiến.

1. Đọc danhSachAdmin/danhSach, enum OrderStatus, next/chuyenAdmin và ThanhToanMomo_Repository.trangThaiHienThi. Lập fixture có nhiều attempt của một đơn, COD chưa thu/đã thu, MoMo pending/failed/paid/refund_pending. EXPIRED là trạng thái đơn.
2. Chuyển read path sang SQL tham số hóa, projection một dòng/đơn, count cùng predicate; trạng thái hiệu lực đúng quy tắc chi tiết (ưu tiên PAID rồi ID lần giảm). Giữ status lặp từ dashboard. Không thêm filter tìm trong lịch sử từng attempt.
3. Bổ sung tất cả trạng thái có thật, method/paymentStatus, tìm mã đơn, sort whitelist với khóa phụ ID, page mặc định 20 và size giới hạn, Xem tất cả/Xóa lọc. Tách tổng toàn hệ thống với số kết quả lọc, dùng pager G6, return/scroll G4. Template dùng chung với Customer phải giữ scope.
4. So count và danh sách với fixture, khớp detail khi vừa PAID vừa có attempt khác, GET không đổi trạng thái. Dùng EXPLAIN trên DB test nếu cần; không thêm index/migration khi chưa có quyết định riêng. Kiểm P20 links/KPI và POST chuyển trạng thái cũ.

## Bất biến và giới hạn

Giữ form session, quyền backend theo vai trò/chủ sở hữu, CSRF, validation, transaction/locking, snapshot, idempotency, State Machine và định nghĩa P20. Với dữ liệu giao dịch đọc hợp đồng docs/design/03-order-payment-inventory.md. Chỉ TK-04 thêm chi tiết khi gộp vượt tồn dương. Không thêm dependency, migration, sửa migration cũ, thay schema hay mở rộng tính năng ngoài gói. Không hard-delete danh mục/hãng như một giải pháp. Gác SHOP-12, AI/RAG/Ollama, phân quyền con, cache/index mới và redesign diện rộng. Không đưa secrets/PII vào URL, log, ảnh QA hoặc storage.

## Kiểm thử, nghiệm thu và quay lui

- Thiết lập fixture mô tả ở các bước trên. Tạo/cập nhật test cho nhánh thành công, lỗi và biên vừa đổi; giữ test nghiệp vụ cũ còn hiệu lực. Regression gate: **G10 MariaDB query/count, dashboard link lặp status, P20 KPI, regression COD/MoMo**.
- Dùng JDK 21, chạy `mvn -o test` và `mvn -o -DskipTests package` khi đổi code. Theo profile liên quan chạy tích hợp MariaDB/Mongo bằng database trống riêng, ghi môi trường và số chạy/skip/lỗi. Không chạy seed hoặc restore lên DB owner. Thiếu môi trường phải ghi rõ gate nào còn hở. Với UI kiểm keyboard, no-JS và responsive. Owner tự nhập tài khoản nếu cần.
- Chỉ nghiệm thu khi: **Lọc kết hợp, xóa lọc/Xem tất cả, sort ổn định, count đúng 1 dòng/đơn, về đúng scroll/page**. Báo kết quả quan sát riêng với nhận định từ mã.
- Rủi ro và phương án quay lui: Join nhân dòng/đếm sai; rollback read path giữ route và quyền cũ. Index là quyết định sau EXPLAIN. Đây là phương án trình owner khi cần; bảo toàn dữ liệu, không dùng lệnh phá hủy hoặc tự đảo thay đổi của gói khác.

## Bàn giao và dừng

Đối chiếu status, diff tracked/staged và danh sách untracked với baseline. Cập nhật đúng dòng ORDER-01 trong [sổ triển khai](../BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md): baseline/HEAD hoặc commit nếu được phép, file, test/lệnh và số liệu, kết quả gate G10, lỗi, sai khác và phụ thuộc. Lưu báo cáo docs/reviews/ORDER-01-ket-qua.md; log đã lọc trong docs/qa/ORDER-01/ và ảnh cần thiết trong docs/ui/screenshots/ORDER-01/. Nếu thay đổi một contract, ghi rõ phiên bản, caller và prompt chưa chạy bị ảnh hưởng; cập nhật nội dung prompt đó hoặc đánh dấu cần hiệu chỉnh trước khi mở gói. Không sửa nhận xét kiểm thử của owner.

Trả lời bằng tiếng Việt với kết quả, bằng chứng, điểm chưa xác minh và đề xuất checkpoint HEAD + diff. Commit/push chỉ khi owner cho phép riêng. Dừng ở ORDER-01; không tự chạy prompt kế tiếp.
