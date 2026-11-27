# 04 · AUDIT · Nhật ký Admin chỉ đọc

**Phase 2 · P0/Lớn**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Mở rộng nhật ký xác thực sẵn có thành nhật ký nghiệp vụ Admin chỉ đọc, truy vết được cả kết quả thất bại.

**Điều kiện tiên quyết.** Sau BASE cần duyệt quyết định 6–7 về danh mục sự kiện, dữ liệu, quyền truy cập và retention. Chỉ chuẩn bị ma trận khi còn mở. Trạng thái tại lúc soạn prompt: **BLOCKED_OWNER**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

V7 ghi audit auth trong transaction. NhatKyQuanTri_Service/Repository, V7 mở rộng, controller.admin, ui/admin-table, NguonDieuHuong, app.js. Rủi ro lộ dữ liệu hoặc ghi SUCCESS cho việc rollback. Chỉ thao tác Admin quan trọng và màn nhật ký mới, không ghi mọi request.

Điểm vào để đối chiếu mã hoặc bằng chứng: [NhatKyQuanTri.java](../../../../src/main/java/com/beautyshop/model/NhatKyQuanTri.java) · [NhatKyQuanTri_Service.java](../../../../src/main/java/com/beautyshop/service/NhatKyQuanTri_Service.java) · [NguonDieuHuong.java](../../../../src/main/java/com/beautyshop/controller/NguonDieuHuong.java) · [V7__xac_thuc_da_phuong_thuc.sql](../../../../src/main/resources/db/migration/V7__xac_thuc_da_phuong_thuc.sql)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Lập danh mục được duyệt theo nghiệp vụ. Đối chiếu V7 nhat_ky_quan_tri, NhatKyQuanTri_Service.ghi và chứng từ đơn/attempt/tồn/lượt mã. Chỉ thêm actor, thời gian, action, target, lý do, kết quả, correlation, trước/sau theo allowlist nơi còn thiếu. Gắn ID chứng từ thay vì sao chép lịch sử nghiệp vụ.
2. Ghi SUCCESS cùng transaction dữ liệu; rollback không còn SUCCESS. Ghi FAILED sau rollback ở transaction ngắn riêng cho hành động đã duyệt, chống lặp. Không serialize request/entity/PII/secret. Xử lý retention theo quyết định có bằng chứng và bảo lưu hồ sơ đang đối soát.
3. Tạo list/detail SSR chỉ ROLE_ADMIN, tìm kiếm, lọc, sort có tie-break ID, phân trang. Tái dùng NguonDieuHuong, fragment bảng/pager và data-scroll-entry/row/list-start. Back giữ q, bộ lọc, sort, page, scroll và focus; dòng bị dọn đưa focus về bộ đếm. Không có endpoint chỉnh sửa nhật ký.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Allowlist snapshot, sort |
| Integration Test | commit, rollback, FAILED riêng, unique correlation, purge |
| Browser Test / E2E | tìm-lọc-sort-page→detail→Back đúng scroll/focus, hai tab/no-JS, dòng bị dọn |
| Regression Test | audit auth cũ, quyền chỉ Admin. |

**Gate của AUDIT.** Actor, giờ, target, trước/sau, lý do/kết quả đúng; không có thao tác sửa; khôi phục trạng thái chỉ cho list mới. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/admin-audit` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Schema additive → service → list/detail. Tắt ghi mới/ẩn màn, giữ chứng từ.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/AUDIT-ket-qua.md`, bằng chứng đã che ở `docs/qa/AUDIT/`, ảnh chọn lọc ở `docs/ui/screenshots/AUDIT/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
