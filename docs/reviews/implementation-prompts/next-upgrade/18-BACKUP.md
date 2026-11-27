# 18 · BACKUP · Admin quản lý bản sao

**Phase 8 · P1/Lớn**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Cho Admin quản lý tác vụ sao lưu an toàn và chứng minh có thể phục hồi bản READY ở đích độc lập.

**Điều kiện tiên quyết.** Cần BASE, AUDIT, công cụ/đích lưu trữ, quyền tải và chính sách dung lượng/giữ bản do owner xác nhận. Phủ các store thực có trong release. Quyết định 15 về restore web ở ngoài WP. Trạng thái tại lúc soạn prompt: **BLOCKED_OWNER / phụ thuộc**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Fixture đã phục hồi SQL/BSON/ảnh, chưa có Admin quản lý. Runner cho công cụ dump cố định, manifest/job, controller.admin và view, SQL/BSON/uploads. Học từ run-data.ps1, không chạy harness QA từ web. Rủi ro lệch mốc, partial, PII trong archive. Chỉ tạo/quản lý/tải bản sao theo quyền, restore vận hành riêng.

Điểm vào để đối chiếu mã hoặc bằng chứng: [run-data.ps1](../../../../docs/qa/QA-01/run-data.ps1) · [run-fixture.ps1](../../../../docs/qa/DATA-01/run-fixture.ps1) · [07-mongodb-danh-gia.md](../../../../docs/deployment/07-mongodb-danh-gia.md) · [restore.txt](../../../../docs/qa/QA-01/rag-fix-data/restore.txt)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Rà harness QA-01/run-data.ps1, DATA-01 và runbook Mongo. Dùng lại công cụ dump/verify với executable và đối số cố định do server quản lý. Admin tạo job qua POST/CSRF, trả ID, danh sách thể hiện thời gian, byte, trạng thái và lỗi thành phần. Giới hạn tác vụ chạy lâu/quota/timeout; không nhận chuỗi shell hay đường dẫn từ form.
2. Chốt mốc nhất quán giữa MariaDB, MongoDB và ảnh local. Manifest gồm app/Flyway, schema, Mongo namespace/collection/index, SHA-256, byte và ref ảnh. Khi AI-HISTORY có trong release, bao phủ cả hội thoại/tin và kiểm owner liên kho; giữ bao phủ review. Dừng ghi phối hợp và xử lý IPN đến muộn theo runbook. PARTIAL/FAILED không đủ điều kiện tải như bản phục hồi.
3. Lưu archive ngoài web root/Git, kiểm quyền tải và verify theo từng request, audit tạo/verify/download. Kiểm archive chứa dữ liệu riêng theo retention và quyền đã duyệt. Diễn tập restore ngoại tuyến sang đích mới, so count/hash/index, quyền chủ, đơn–attempt–tồn–mã và ảnh; đối soát trước chuyển hoặc lùi đích. Không có nút restore web.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Job ID, manifest, state |
| Integration Test | disk đầy, thiếu một store, hash sai, restart, restore đích mới |
| Browser Test / E2E | tạo-list-verify-tải có quyền, trạng thái và lỗi |
| Regression Test | checksum, unique index, snapshot, ảnh/owner. Khi có AI-HISTORY, khôi phục BSON hội thoại/tin, index và kiểm quyền theo ID MariaDB sau restore. |

**Gate của BACKUP.** Bản READY phục hồi được theo runbook, audit truy được người tạo/tải. Restore web chờ quyết định riêng. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/admin-backups` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Runner/manifest → quyền/UI → diễn tập. Dừng job, giữ archive tốt, chuyển/lùi đích khi đã đối soát.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/BACKUP-ket-qua.md`, bằng chứng đã che ở `docs/qa/BACKUP/`, ảnh chọn lọc ở `docs/ui/screenshots/BACKUP/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
