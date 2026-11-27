# 13 · AI-HISTORY · Lịch sử riêng của Customer

**Phase 5 · P1/Lớn**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Cho Customer xem lại hội thoại riêng được lưu trong MongoDB; bảo toàn trải nghiệm khách vãng lai và guard hiện tại.

**Điều kiện tiên quyết.** MongoDB đã chốt. Cần BASE, AUTH, guard AI-OPS, quyết định 8–9 về thời hạn/xóa trước lưu dữ liệu. Mục 10 chỉ chặn việc thêm ngữ cảnh nhiều lượt. Trạng thái tại lúc soạn prompt: **BLOCKED_OWNER**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

MongoDB hiện đang lưu đánh giá. Bổ sung document cuộc hội thoại và tin/lượt riêng, repository theo owner, DTO đọc/ghi, controller/service và view /chat. MariaDB tiếp tục xác thực maNguoiDung, không có FK xuyên store. Không sửa collection danh_gia. Quyền xem của Customer, không cấp Admin đọc nội dung hay tự gửi ngữ cảnh.

Điểm vào để đối chiếu mã hoặc bằng chứng: [TuVan_Controller.java](../../../../src/main/java/com/beautyshop/controller/TuVan_Controller.java) · [model](../../../../src/main/java/com/beautyshop/model) · [MongoDanhGia_Store.java](../../../../src/main/java/com/beautyshop/repository/MongoDanhGia_Store.java) · [index.html](../../../../src/main/resources/templates/chat/index.html) · [07-mongodb-danh-gia.md](../../../../docs/deployment/07-mongodb-danh-gia.md)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Dùng MongoDB hiện có. Đề xuất collection hội thoại và collection tin/lượt trao đổi có kích thước giới hạn; quyết định tên theo repo. Gắn maNguoiDung ổn định từ principal đã được MariaDB xác thực. Không tạo bảng/FK Flyway cho chat, không dùng email làm khóa và không nhúng transcript tăng vô hạn. Lập index cho (owner, thời gian, ID) và (owner, cuộc, thứ tự/ID), unique khóa ghi lặp trong phạm vi cuộc.
2. Tách repository lưu/xem của Customer khỏi DTO gửi model. Lưu câu hợp lệ và kết quả văn bản sau guard; mọi đọc/ghi/xóa/list lọc đồng thời theo owner và ID cuộc. Hiển thị cuộc mới, lịch sử phân trang/cursor, quyền khi có hai tài khoản. Guest tiếp tục chat, Admin chỉ thấy số liệu tổng. Không lưu input bị từ chối hoặc tự đưa nội dung cũ vào Ollama.
3. Kiểm topology Mongo hiện tại. Nếu standalone, thiết kế ghi một lượt idempotent, xử lý lỗi giữa metadata và tin, sửa metadata dẫn xuất hoặc tiếp tục ghi an toàn. Tránh phụ thuộc mặc định vào transaction đa document/đa store. Mongo lỗi báo rõ chưa lưu/không tải được lịch sử; giữ trả lời hiện tại và luồng Guest. Kiểm Redis/SQL không thành kho lịch sử dự phòng.
4. Triển khai retention/xóa theo quyết định 8–9. Chặn đọc/ghi vào cuộc đang xóa; dọn cả tin và metadata theo batch có thể retry, tránh hồi sinh tin sau xóa. Đồng bộ manifest BACKUP, index, quyền và thử khôi phục trên namespace mới. Hộp thoại nhiều lượt chỉ trong phạm vi owner phê duyệt riêng.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Ánh xạ principal→owner, quyết định cho lưu sau guard, khóa ghi lặp, DTO hiển thị và chính sách xóa đã duyệt. |
| Integration Test | MongoDB thật trên fixture: hai tài khoản đọc/ghi/xóa chéo bị từ chối, index và cursor, retry sau ghi dở không trùng tin, Mongo tắt/khôi phục, xóa đồng thời và không có bản ghi Guest/PII bị chặn. |
| Browser Test / E2E | tạo cuộc, tải thêm, Back, empty/error |
| Regression Test | Guest chat và số call không đổi khi xem lại. |

**Gate của AI-HISTORY.** Chỉ chủ đọc được lịch sử riêng, câu bị từ chối không lưu, Admin chỉ có số liệu tổng. Manifest BSON bao phủ collection và index mới; kiểm được Mongo lỗi mà /chat vẫn phục vụ theo thiết kế. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/customer-chat-history` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Schema/quyền → lưu → list/detail. Dừng ghi/ẩn màn, giữ quy trình dọn đúng hạn.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/AI-HISTORY-ket-qua.md`, bằng chứng đã che ở `docs/qa/AI-HISTORY/`, ảnh chọn lọc ở `docs/ui/screenshots/AI-HISTORY/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
