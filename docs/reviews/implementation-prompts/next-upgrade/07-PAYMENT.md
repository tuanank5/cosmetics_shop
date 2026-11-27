# 07 · PAYMENT · Lịch sử từng lần thanh toán

**Phase 3 · P0/Lớn**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Đưa lịch sử từng attempt đã lưu lên chi tiết đơn, theo quyền, bảo toàn idempotency và trạng thái thanh toán hiệu lực.

**Điều kiện tiên quyết.** Sau BASE có thể làm projection lịch sử V1 độc lập. Gate tổng tiền, snapshot và hồi quy hai slot chờ VOUCHER. Trạng thái tại lúc soạn prompt: **Chờ BASE**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Có nhiều lần thử/đơn, chi tiết dùng trạng thái hiệu lực. ThanhToanMomo_Repository/Service, Momo_Facade, DonHang_Service, order/detail và customer-detail. Tái dùng lan_thanh_toan, thêm nullable metadata khi cần. Rủi ro lộ mã nội bộ hoặc đổi trạng thái hiệu lực. Tái dùng bảng attempt, không làm quote có TTL.

Điểm vào để đối chiếu mã hoặc bằng chứng: [ThanhToanMomo_Repository.java](../../../../src/main/java/com/beautyshop/repository/ThanhToanMomo_Repository.java) · [ThanhToanMomo_Service.java](../../../../src/main/java/com/beautyshop/service/ThanhToanMomo_Service.java) · [order](../../../../src/main/resources/templates/order) · [V1__khoi_tao_co_so_du_lieu.sql](../../../../src/main/resources/db/migration/V1__khoi_tao_co_so_du_lieu.sql)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Lập bảng field theo attempt của lan_thanh_toan (V1), phương thức từ đơn/cổng, request ID, mã cổng, tiền, trạng thái, tạo/thu, kết quả và nguồn đối soát. Dữ liệu cũ thiếu mốc ghi rõ chưa ghi nhận. Chỉ thêm metadata lỗi đã lọc nếu có nguồn xác thực.
2. Giữ chuanBi dùng lại PENDING, lần FAILED chỉ tạo attempt mới khi đúng trạng thái. Đọc theo đơn, thứ tự ngày/ID ổn định. Customer chỉ xem giao dịch của mình, Admin thấy thêm ref nghiệp vụ. Tích hợp vào hai view detail hiện tại, giữ trạng thái hiệu lực của bộ lọc Admin.
3. Kiểm nhanIPN/nhanDoiSoat với signature, order, amount/VND, gateway, duplicate, kết quả muộn và request đồng thời. Browser return chỉ thông báo. Đối chiếu kho/lượt cả hai slot sau VOUCHER trước đóng gate. Giữ COD delivery/thu cùng transaction.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Map trạng thái/legacy |
| Integration Test | retry song song, IPN lặp/trễ, mismatch, query, pending reuse |
| Browser Test / E2E | hai vai trò xem số tiền/giờ/trạng thái đúng, chặn truy cập chéo |
| Regression Test | COD thu khi giao, hai slot mã và tồn chỉ chốt một lần. |

**Gate của PAYMENT.** Attempt hiển thị khớp DB, lỗi đã lọc, thiếu mốc hiển thị chưa ghi nhận. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/payment-attempt-history` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Projection/metadata → view → regression. Ẩn phần trình bày, giữ mọi ref/attempt.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/PAYMENT-ket-qua.md`, bằng chứng đã che ở `docs/qa/PAYMENT/`, ảnh chọn lọc ở `docs/ui/screenshots/PAYMENT/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
