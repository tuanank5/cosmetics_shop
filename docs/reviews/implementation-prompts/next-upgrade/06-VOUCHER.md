# 06 · VOUCHER · Phạm vi mã và giảm phí ship

**Phase 3 · P0/Rất lớn**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Áp dụng đúng đối tượng và đồng thời tối đa một mã hàng/đơn cùng một mã phí ship, khớp tiền và lượt qua toàn giao dịch.

**Điều kiện tiên quyết.** Cần BASE, quy tắc 1–3 và hợp đồng AUDIT cho Admin. Chốt ví dụ tính tiền, lượt và phí trước khi viết policy/schema. Trạng thái tại lúc soạn prompt: **BLOCKED_OWNER**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Giảm toàn đơn một mã, Admin có lọc/đếm/pager. PhieuGiamGia_Service/Repository, DonHang_Service, ShippingFee_Policy, ThanhToanMomo_Service, checkout, admin/voucher, migration quan hệ/slot/quota. Rủi ro vượt lượt hoặc sai tổng. Giữ tồn theo biến thể và D27 đến khi có duyệt, không Excel.

Điểm vào để đối chiếu mã hoặc bằng chứng: [PhieuGiamGia_Service.java](../../../../src/main/java/com/beautyshop/service/PhieuGiamGia_Service.java) · [ShippingFee_Policy.java](../../../../src/main/java/com/beautyshop/shipping/ShippingFee_Policy.java) · [ThanhToanMomo_Service.java](../../../../src/main/java/com/beautyshop/service/ThanhToanMomo_Service.java) · [voucher](../../../../src/main/resources/templates/admin/voucher)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Chuyển quyết định 1–3 thành bảng ví dụ gồm nhiều chiều sản phẩm/danh mục/hãng/Customer, minSpend, cap, tổng quota/quota khách, thời gian và phí miễn. Chưa ký ví dụ thì dừng ở thiết kế. Giữ Admin search/filter/pager, đã dùng/giữ/còn và nhãn Mã giảm giá.
2. Thiết kế migration additive cho đích áp dụng, slot HÀNG/VẬN_CHUYỂN, giữ/dùng/hoàn và snapshot phí/giảm từng dòng. Ánh xạ mã lịch sử một lần, giữ nguyên tổng đơn cũ. Mỗi đơn tối đa một mã mỗi loại. Đặt khóa/unique/điều kiện quota cả lượt đang giữ.
3. Tính server-side từ giá SKU hiện hành, tách tập dòng đủ điều kiện, phân bổ đồng dư ổn định theo quy tắc được duyệt. Giữ D27 phí 25.000 ₫/miễn 500.000 ₫ sau giảm hàng đến khi owner duyệt phối hợp mã ship. Guest xem dự tính, sau merge và checkout tính lại. Không ghi lượt ở bước báo giá.
4. COD ghi hai slot cùng xuất kho trong transaction; MoMo giữ cùng SKU 15 phút, consume/release theo kết quả đã xác minh. Cố định thứ tự khóa qua các đường hủy/IPN/đối soát. Đối chiếu hủy, fail, hết hạn, refund, KPI và lượt từng khách. Test hai yêu cầu tranh lượt cuối và rollback giữa hai slot.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | FIXED/%, trần, ngưỡng, phân bổ phần dư, phí 0 |
| Integration Test | hai đơn giành lượt cuối, rollback giữa hai slot, giữ/consume/release, legacy |
| Browser Test / E2E | Guest→merge→quote→đặt JS/no-JS, duyệt mã |
| Regression Test | COD, MoMo, hủy, BR21, snapshot, KPI. |

**Gate của VOUCHER.** Đối soát từng đồng và mỗi lượt trên đơn, kho/giữ và quota đúng cả khi đồng thời. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/voucher-scope-shipping` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Schema/legacy → policy → khóa/checkout → Admin. Dừng nhận đơn hai mã, đối soát holds, giữ khả năng đọc đơn mới khi lùi.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/VOUCHER-ket-qua.md`, bằng chứng đã che ở `docs/qa/VOUCHER/`, ảnh chọn lọc ở `docs/ui/screenshots/VOUCHER/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
