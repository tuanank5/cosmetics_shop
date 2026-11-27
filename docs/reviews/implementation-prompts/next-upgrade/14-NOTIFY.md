# 14 · NOTIFY · Thông báo khuyến mãi trong web

**Phase 6 · P1/Vừa**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Cho Admin chủ động phát thông báo trong website và Customer quản lý trạng thái đã đọc của chính mình.

**Điều kiện tiên quyết.** Cần AUTH, AUDIT và quyết định 11 về tập Customer nhận và tài khoản khóa. Trạng thái tại lúc soạn prompt: **BLOCKED_OWNER**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Chưa có campaign/inbox/readAt. Controller/service mới, header/badge, Admin preview, inbox Customer, campaign và recipient/readAt. Rủi ro fan-out trùng hoặc nhầm tập người nhận. Chỉ gửi chủ động trong website, không lịch sử gửi tổng quát.

Điểm vào để đối chiếu mã hoặc bằng chứng: [NguoiDung.java](../../../../src/main/java/com/beautyshop/model/NguoiDung.java) · [NhatKyQuanTri_Service.java](../../../../src/main/java/com/beautyshop/service/NhatKyQuanTri_Service.java) · [header.html](../../../../src/main/resources/templates/fragments/header.html)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Xác nhận quyết định 11 rồi làm luồng Admin soạn nội dung, chọn tập Customer, xem trước số người và đích nội bộ, xác nhận gửi. Chốt tập tại thời điểm xác nhận; nêu hành vi nếu audience đổi kể từ preview.
2. Lưu campaign và bản ghi recipient/readAt tối thiểu. Khóa thao tác gửi, unique chiến dịch–Customer, fan-out theo batch có thể retry. Audit ai xác nhận, lúc nào, số lượng và kết quả. Việc tạo/sửa mã không tự gửi.
3. Thêm hộp thông báo và badge chưa đọc; mark read bằng POST/CSRF theo principal. Kiểm quyền cả khi mở liên kết, đồng bộ lại khi có hai tab. Không gửi email khuyến mãi hay thêm lịch sử gửi chung.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Chọn người, URL nội bộ, read idempotent |
| Integration Test | repeat send, batch retry, read đồng thời, IDOR |
| Browser Test / E2E | preview→xác nhận→đọc, hai tab/no-JS |
| Regression Test | tạo/sửa mã không gửi gì, chỉ Admin được gửi. |

**Gate của NOTIFY.** Unique theo người nhận/chiến dịch, số chưa đọc khớp DB và có audit. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/promotion-inbox` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Schema/recipient → gửi/audit → inbox. Dừng gửi mới, giữ bản đã phát.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/NOTIFY-ket-qua.md`, bằng chứng đã che ở `docs/qa/NOTIFY/`, ảnh chọn lọc ở `docs/ui/screenshots/NOTIFY/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
