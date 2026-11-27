# 05 · ACCOUNT · Khóa/mở có lý do và mail

**Phase 2 · P0/Vừa**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Buộc giải trình khóa/mở, truy được Admin và kết quả gửi thư, chặn mọi phương thức khi tài khoản bị khóa.

**Điều kiện tiên quyết.** Cần BASE, AUTH, AUDIT, hợp đồng lý do/sự kiện và nội dung email khóa/mở đã duyệt. Trạng thái tại lúc soạn prompt: **BLOCKED_OWNER / phụ thuộc**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Có khóa, kiểm active/version và danh sách Admin. NguoiDung_Service, AdminNguoiDung_Controller, TrangThaiTaiKhoan_Filter, Email_Service, admin/user. Metadata gửi theo event. Rủi ro mail lỗi làm đảo trạng thái hoặc gửi lặp. Giữ thống kê khách và email đúng loại.

Điểm vào để đối chiếu mã hoặc bằng chứng: [AdminNguoiDung_Controller.java](../../../../src/main/java/com/beautyshop/controller/admin/AdminNguoiDung_Controller.java) · [NguoiDung_Service.java](../../../../src/main/java/com/beautyshop/service/NguoiDung_Service.java) · [TrangThaiTaiKhoan_Filter.java](../../../../src/main/java/com/beautyshop/security/TrangThaiTaiKhoan_Filter.java) · [user](../../../../src/main/resources/templates/admin/user)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Ở form và service khóa/mở buộc nhập lý do cho cả hai chiều; trong datTrangThai kiểm quyền, version, tự khóa và trạng thái hiện hành dưới khóa. POST lặp cùng trạng thái không sinh hành động mới.
2. Ghi actor, thời điểm, lý do và thay đổi active vào audit chung. Cùng transaction tạo ý định gửi thư riêng cho thao tác (unique event). Sau commit gửi qua Email_Service, lưu pending/sent/failed, phân biệt SMTP nhận với đã giao inbox. Có retry giới hạn, kết quả không chắc phải đối soát. Mail lỗi không đảo trạng thái khóa.
3. Trong Admin user list/detail hiển thị lý do và kết quả thư theo quyền. Kiểm form login, Google, GitHub cùng chặn user bị khóa; mở lại không hồi sinh phiên cũ. Giữ số liệu khách, role và đường quay lại hiện hành.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Lý do hai chiều, no-op |
| Integration Test | user + audit + tác vụ gửi, retry, hai Admin |
| Browser Test / E2E | khóa rồi mở với form/Google/GitHub, phiên cũ |
| Regression Test | cấm tự khóa và giữ thống kê. |

**Gate của ACCOUNT.** Khóa có hiệu lực dù SMTP lỗi, xem được ai, khi nào, lý do và kết quả gửi. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/account-status` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Transaction + audit → gửi sau commit → form. Dừng worker và đối soát thư treo.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/ACCOUNT-ket-qua.md`, bằng chứng đã che ở `docs/qa/ACCOUNT/`, ảnh chọn lọc ở `docs/ui/screenshots/ACCOUNT/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
