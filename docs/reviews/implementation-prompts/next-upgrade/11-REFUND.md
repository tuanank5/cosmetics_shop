# 11 · REFUND · Hoàn toàn phần theo giao dịch

**Phase 4 · P0/Rất lớn**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Hoàn toàn phần đúng giao dịch thực thu, xác minh để không chi trùng và giữ số liệu tài chính đối soát được.

**Điều kiện tiên quyết.** Cần PAYMENT, AUDIT, quyết định 13–14; quyết định 3 cho xử lý lượt. Hồ sơ hàng trả cần RETURN; late payment độc lập kiểm định. Trạng thái tại lúc soạn prompt: **BLOCKED_OWNER**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

COD có thu tiền, MoMo có REFUND_PENDING. Sổ hoàn gắn attempt, Momo_Facade, SauMua_Service, ThongKe_Repository và order/return detail. Rủi ro chi hai lần hoặc hoàn cả đơn từ một mục. Chỉ hoàn toàn phần, chỉ ghi thành công khi có chứng cứ.

Điểm vào để đối chiếu mã hoặc bằng chứng: [Momo_Facade.java](../../../../src/main/java/com/beautyshop/service/Momo_Facade.java) · [DonHang_Service.java](../../../../src/main/java/com/beautyshop/service/DonHang_Service.java) · [ThongKe_Repository.java](../../../../src/main/java/com/beautyshop/repository/ThongKe_Repository.java)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Gắn yêu cầu hoàn toàn phần với đúng attempt thực thu, gồm late paid cần hoàn, phân biệt COD chưa thu với đã thu. Áp dụng quyết định 13–14 về trả vài dòng, phí/bao phủ và cách chứng minh đã chi COD. Không suy ra hoàn cả đơn chỉ từ yêu cầu trả một phần.
2. Lưu unique attempt gốc, request idempotent, amount, phương thức/provider hoặc thủ công, ref, người, lúc và kết quả. Khóa chống vượt thực thu/chi hai lần. PENDING khi chưa rõ, query trước retry; SUCCEEDED cần chứng từ xác nhận, FAILED chỉ khi có lỗi cuối. Không giữ dữ liệu ngân hàng nhạy cảm.
3. Đối chiếu khả năng refund/query của Sandbox trước bật đường provider. Ghi audit và hiển thị trạng thái theo quyền, đối soát thực thu, đã hoàn, nghĩa vụ BR21 và quy tắc hoàn lượt. Không tự động nhập kho hoặc trả lượt lần hai khi đánh dấu thành công.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Số thực thu, điều kiện bao phủ |
| Integration Test | unique, retry/query sau timeout, hai lệnh cùng lúc, COD chưa thu và MoMo muộn |
| Browser Test / E2E | Admin xác nhận có chứng từ, Customer xem kết quả; Sandbox khi được cấp |
| Regression Test | doanh thu, kho, lượt không đảo hai lần. |

**Gate của REFUND.** Tối đa số thực trả, ref/actor/thời điểm và bằng chứng cuối; pending khi chưa rõ. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/full-refund` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Ledger/policy → provider hoặc thủ công/đối soát → UI/KPI. Dừng lệnh mới, query lệnh treo, không rollback tiền bằng xóa DB.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/REFUND-ket-qua.md`, bằng chứng đã che ở `docs/qa/REFUND/`, ảnh chọn lọc ở `docs/ui/screenshots/REFUND/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
