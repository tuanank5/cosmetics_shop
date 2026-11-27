# 10 · RETURN · Kiểm định và nhập kho SKU

**Phase 4 · P0/Lớn**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Ghi nhận kiểm định từng dòng sau khi nhận hàng và chỉ nhập lại lượng đủ bán đúng một lần.

**Điều kiện tiên quyết.** Cần BASE, AUDIT và quyết định 12 về tiêu chuẩn đủ bán, số không nhận/hỏng và người xác nhận. Trạng thái tại lúc soạn prompt: **BLOCKED_OWNER**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

APPROVED và RECEIVED riêng, chưa kiểm định từng dòng. SauMua_Service, DoiTra_Repository, ReturnStatus, TonKho_Service, return/detail, migration kiểm định theo dòng + unique bút toán. Rủi ro nhập hàng hỏng/hai lần. Không lô/FEFO, không tự hoàn tiền.

Điểm vào để đối chiếu mã hoặc bằng chứng: [ReturnStatus.java](../../../../src/main/java/com/beautyshop/order/ReturnStatus.java) · [SauMua_Service.java](../../../../src/main/java/com/beautyshop/service/SauMua_Service.java) · [DoiTra_Repository.java](../../../../src/main/java/com/beautyshop/repository/DoiTra_Repository.java) · [return](../../../../src/main/resources/templates/return)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Giữ PENDING→APPROVED/REJECTED và APPROVED→RECEIVED→COMPLETED, thời hạn 7 ngày và số lượng đã yêu cầu. Với quy tắc 12 đã duyệt, ghi nhận số nhận thực, đủ bán, hỏng/mở/chưa nhận, lý do, người và giờ trên từng dòng.
2. Chỉ sau xác nhận kiểm định mới tạo bút toán nhập SKU đủ điều kiện. Khóa hồ sơ rồi biến thể, unique theo dòng/lần kiểm, kiểm tổng không vượt số duyệt. Audit trong transaction; lặp/rollback không phát sinh bút toán thứ hai. Hồ sơ COMPLETED cũ không nhập bù.
3. Admin nhận/kiểm/đóng đúng trạng thái, Customer chỉ xem hồ sơ của mình. Xử lý hàng mất/chưa nhận theo quyết định, không ghi RECEIVED giả. Nhập kho và quyền nhận tiền hoàn được xét riêng.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Số đủ bán ≤ thực nhận ≤ duyệt, transitions |
| Integration Test | hai Admin, rollback giữa nhập/audit, đọc hồ sơ cũ |
| Browser Test / E2E | duyệt→nhận→kiểm định, thiếu/hỏng |
| Regression Test | 7 ngày, lượng cộng dồn, BR21, owner. |

**Gate của RETURN.** Đúng người/giờ/kết luận; chỉ tăng đúng lượng đủ điều kiện một lần sau xác nhận. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/return-inspection` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Schema/kết quả → transaction → UI. Dừng nhập mới, đối ứng sai lệch đã xác nhận, không xóa lịch sử.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/RETURN-ket-qua.md`, bằng chứng đã che ở `docs/qa/RETURN/`, ảnh chọn lọc ở `docs/ui/screenshots/RETURN/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
