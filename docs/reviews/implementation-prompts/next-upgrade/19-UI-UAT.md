# 19 · UI-UAT · Nghiệm thu giao diện và bàn giao

**Phase 9 · P1/Lớn**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Nghiệm thu các luồng cũ/mới thực sự có trong release trên ma trận vai trò và thiết bị.

**Điều kiện tiên quyết.** Chuẩn bị checklist sau BASE; chạy từng luồng khi có revision tích hợp và role/fixture. Maps và hồ sơ 91 trang theo điều kiện riêng. Trạng thái tại lúc soạn prompt: **Chờ phụ thuộc**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Đã có bằng chứng theo màn, thiếu zoom native và ma trận tổng. Design system, fragments/ui, app.js, CSS, docs/testing và QA. Rủi ro coi screenshot từng màn là UAT toàn bộ. Chỉ kiểm chứng, không redesign hoặc làm lại scroll toàn hệ thống.

Điểm vào để đối chiếu mã hoặc bằng chứng: [03-huong-dan-kiem-thu-thu-cong.md](../../../../docs/testing/03-huong-dan-kiem-thu-thu-cong.md) · [SCROLL-RESTORE-ket-qua.md](../../../../docs/reviews/SCROLL-RESTORE-ket-qua.md) · [LIST-PAGING-ket-qua.md](../../../../docs/reviews/LIST-PAGING-ket-qua.md) · [UI-REDESIGN-02-ket-qua.md](../../../../docs/reviews/UI-REDESIGN-02-ket-qua.md)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Lấy SHA tích hợp, role và fixture. Lập ma trận Guest/Customer/Admin tại desktop, tablet, mobile, keyboard, native zoom 200%, JS-off khi có form, dữ liệu rỗng/dài/lỗi. Kế thừa chứng cứ đúng SHA và ghi ca thiếu. Bổ sung 1672×941 theo UI-REDESIGN-02.
2. Kiểm PDP session mới và CSRF, Guest merge đúng một lần, Back/Forward giỏ và báo giá từ server, hai POST địa chỉ khi đã có 9, tối đa 10 và đúng một mặc định, snapshot đơn sau sửa/xóa. Kiểm tìm/lọc/sort/trang cuối/hai tab/dòng bị mất, Audit Log giữ keyword, sort, page, vị trí và focus.
3. Bao phủ màn mới theo WP đã tích hợp, gồm trước/sau logout Google, Mongo history chỉ thuộc Customer, trạng thái mail/backup lỗi và không tự gửi khuyến mãi. Ghi bug có bước tái hiện/kỳ vọng/thực tế, ảnh đã che và retest. Maps chỉ khi có pin thật; tệp 91 trang là bàn giao riêng, Ollama thật và FEFO ở backlog.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Kế thừa rule/DTO liên quan |
| Integration Test | hai POST khi có 9 địa chỉ, snapshot sau sửa/xóa, CSRF PDP session mới |
| Browser Test / E2E | Chạy ma trận vai trò, thiết bị, native zoom, JS/no-JS và Back/Forward đã mô tả ở bước 1–3; đính kèm kết quả từng tổ hợp. |
| Regression Test | breadcrumb, footer, logo, focus và đặc quyền. |

**Gate của UI-UAT.** Có kết quả theo tổ hợp, bug có bước tái hiện, retest trên SHA. Biên bản bàn giao tách báo cáo 91 trang. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `qa/ui-uat` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Ma trận → bằng chứng → retest. Giữ phát hành phần lỗi đến khi xác nhận lại.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/UI-UAT-ket-qua.md`, bằng chứng đã che ở `docs/qa/UI-UAT/`, ảnh chọn lọc ở `docs/ui/screenshots/UI-UAT/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
