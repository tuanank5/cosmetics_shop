# 08 · PAY-LIVE · Nghiệm thu MoMo Sandbox

**Phase 3 · P1/Lớn**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Đối soát giao dịch Sandbox từ trình duyệt qua IPN/query tới tiền, tồn và lượt mã.

**Điều kiện tiên quyết.** Cần PAYMENT, VOUCHER, merchant Sandbox và callback/IPN HTTPS đã cấp. Chuẩn bị fixture/mock độc lập. Trạng thái tại lúc soạn prompt: **Chờ phụ thuộc · BLOCKED_EXTERNAL**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Đã kiểm MoMo mock, thiếu bằng chứng Sandbox. Gateway, IPN, reconciliation job, deployment/04 và SQL fixture. Rủi ro callback đến muộn, lẫn mock với merchant. Sandbox riêng, không merchant production hoặc FEFO.

Điểm vào để đối chiếu mã hoặc bằng chứng: [04-momo-sandbox.md](../../../../docs/deployment/04-momo-sandbox.md) · [MomoReconciliation_Job.java](../../../../src/main/java/com/beautyshop/payment/MomoReconciliation_Job.java) · [Momo_MariaDb_Test.java](../../../../src/test/java/com/beautyshop/integration/Momo_MariaDb_Test.java)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Chuẩn bị merchant Sandbox, callback/IPN HTTPS, fixture đơn/SKU/mã thử và số dư đầu kỳ. Tách hoàn toàn khỏi cơ sở dữ liệu khách thật. Chốt SHA, mode sandbox và nguồn đọc kết quả merchant.
2. Chạy tạo/đăng nhập ví thử, thành công, lỗi, hủy, retry, callback lặp, kết quả đến muộn và query kết thúc. Ghi riêng sự kiện redirect và IPN. Tình huống Sandbox không điều khiển được phải ghi NOT RUN hoặc đối chứng mock có nhãn.
3. Đối soát số tiền/attempt/ref thử, order status, payment, SKU vật lý/giữ và từng lượt mã trước/sau. Kiểm không chốt/giải phóng hai lần. Sự kiện chưa rõ giữ để đối soát. Gỡ endpoint thử sau khi xử lý các giao dịch treo.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Kế thừa chữ ký/validator |
| Integration Test | mock lỗi khó kích hoạt + đối sổ DB |
| Browser Test / E2E | E2E Sandbox success/fail/cancel/replay/late/query, đối chiếu merchant |
| Regression Test | tồn SKU và hai slot trước/sau. |

**Gate của PAY-LIVE.** Mỗi ca có đơn, attempt, amount VND, kết quả cổng và số dư; thiếu môi trường thì BLOCKED_EXTERNAL riêng MoMo. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `qa/payment-sandbox` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Đầu kỳ → kịch bản → đối soát. Xử lý xong lệnh đang chờ trước khi tắt endpoint.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/PAY-LIVE-ket-qua.md`, bằng chứng đã che ở `docs/qa/PAY-LIVE/`, ảnh chọn lọc ở `docs/ui/screenshots/PAY-LIVE/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
