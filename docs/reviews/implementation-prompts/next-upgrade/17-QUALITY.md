# 17 · QUALITY · Rà mã thừa có chứng cứ

**Phase 7 · P2/Nhỏ–vừa**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Giữ hợp đồng đang chạy, chỉ dọn mã và trách nhiệm dư thừa có bằng chứng xuyên tầng.

**Điều kiện tiên quyết.** Sau BASE có thể rà module đã ổn định. Cần chứng minh ứng viên và test toàn bộ caller trước khi xóa/sửa. Trạng thái tại lúc soạn prompt: **Chờ BASE**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Các tệp cũ D41 đã xóa có chủ đích. Controller/service/repo/DTO, template/CSS/JS, mapping, comment. Rủi ro xóa nhầm bean/selector chỉ dùng runtime. Chỉ xử lý có căn cứ, không phục hồi thành phần D41.

Điểm vào để đối chiếu mã hoặc bằng chứng: [DonHang_Service.java](../../../../src/main/java/com/beautyshop/service/DonHang_Service.java) · [DonHangCOD_MariaDb_Test.java](../../../../src/test/java/com/beautyshop/integration/DonHangCOD_MariaDb_Test.java) · [templates](../../../../src/main/resources/templates) · [SKIN-UI-01-chuan-hoa-tu-van-da.md](../../../../docs/reviews/SKIN-UI-01-chuan-hoa-tu-van-da.md)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Lập danh sách ứng viên import/method/class/query/DTO/template/CSS/JS/comment hoặc trách nhiệm trùng. Ghi caller sản phẩm và test, route, DI, JPQL/reflection, Thymeleaf/fragment/selector và cấu hình liên quan. Overload danhSachCustomer không phân trang vẫn có integration test gọi.
2. Chỉ sửa ứng viên có bằng chứng và test đặc trưng cho hành vi. Đối chiếu HTML/HTTP, mapping, quyền, state và caller trước/sau. Giữ comment về bảo mật, khóa/transaction/quyết định, quy ước tên và mã D41/SKIN đã xóa hợp lệ.
3. Chia diff nhỏ theo finding. Tách các module có branch khác đang ghi. Ghi rõ ứng viên giữ lại và lý do; không refactor rộng để đủ số thay đổi.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Test caller/quy tắc liên quan |
| Integration Test | boot context, JPQL, render template |
| Browser Test / E2E | mở đúng role và trạng thái thực dùng |
| Regression Test | so contract, build/package và kiểm nghiệp vụ vùng sửa. |

**Gate của QUALITY.** Mỗi ứng viên có bằng chứng bỏ/giữ; không có finding thì kết thúc ở kiểm kê. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `chore/verified-cleanup` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Mỗi finding và test một commit. Lùi riêng thay đổi nhỏ bị lỗi.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/QUALITY-ket-qua.md`, bằng chứng đã che ở `docs/qa/QUALITY/`, ảnh chọn lọc ở `docs/ui/screenshots/QUALITY/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
