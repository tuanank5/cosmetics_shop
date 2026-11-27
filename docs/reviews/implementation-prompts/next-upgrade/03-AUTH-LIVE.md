# 03 · AUTH-LIVE · Nghiệm thu OAuth, Redis và mail

**Phase 1 · P0/Lớn**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Chứng minh từng luồng auth đã chọn hoạt động với dịch vụ thử thật, kể cả phiên trước và sau Google logout.

**Điều kiện tiên quyết.** Cần BASE, bản AUTH ở các luồng đã sửa, client Google/GitHub thử, callback và Redis/SMTP thử đã cấp. Có thể chuẩn bị kịch bản riêng. Trạng thái tại lúc soạn prompt: **Chờ phụ thuộc · BLOCKED_EXTERNAL**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Callback có fixture, mail/Redis chưa nghiệm thu đủ chuỗi. OAuth handlers, EmailVerification_Service, Email_Service, MatKhau_Service, Redis, Mailpit và SMTP. Rủi ro liên kết nhầm người hoặc kết luận từ kết nối TCP. Không ghi credential, OTP hay callback URL chứa code vào chứng cứ.

Điểm vào để đối chiếu mã hoặc bằng chứng: [EmailVerification_Service.java](../../../../src/main/java/com/beautyshop/service/EmailVerification_Service.java) · [OAuth2Client_Config.java](../../../../src/main/java/com/beautyshop/security/OAuth2Client_Config.java) · [AUTH-02-nghiem-thu-xac-thuc.md](../../../../docs/reviews/AUTH-02-nghiem-thu-xac-thuc.md)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Lập ma trận Google, GitHub, OAuth-only và email/password trên hai Customer thử, đúng callback /login/oauth2/code/{provider}. Dùng Redis test, Mailpit local rồi mailbox owner kiểm soát nếu nghiệm thu SMTP ngoài. Cấu hình secret ngoài Git.
2. Chạy consent thành công/từ chối, email trùng, link đúng ID, tài khoản khóa và phiên đã thu hồi. Với Google ghi chuỗi đích chỉ gồm path/host: POST logout → trang công khai → GET /account bị chặn → bấm Google → callback mới → /account. So một Google đang đăng nhập, nhiều tài khoản và profile riêng tư chưa có phiên Google. Back/refresh trước bấm phải còn anonymous. Không ghi query callback, cookie hoặc mã OTP.
3. Nhận OTP thật, thử đúng, sai, hết hạn, resend, dùng lại và hai lần verify đồng thời. Phân biệt session web và Redis OTP/rate. Test reset link hết hạn/đã dùng, email lỗi và phiên cũ. Nếu provider thiếu thì đánh dấu từng ca BLOCKED_EXTERNAL; nếu phát hiện lỗi logout, chuyển bằng chứng cho AUTH và retest bản sửa.

Tham chiếu hành vi của Google và tùy chọn bộ chọn tài khoản. [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect)

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | Kế thừa proof/OTP |
| Integration Test | Redis thật TTL, sai/đúng/resend/replay/race, Mailpit nhận local |
| Browser Test / E2E | callback thật hai provider, consent/deny/trùng email, nhận link và đăng nhập; logout, /account trước click, so các profile Google và callback mới sau click. |
| Regression Test | phiên cũ, khóa và gộp giỏ. |

**Gate của AUTH-LIVE.** Biên bản theo provider/môi trường, đúng ID. Bên ngoài cần mailbox thử do owner kiểm soát, chỉ lưu kết quả đã che. Tách phiên local bị thu hồi khỏi phiên tại Google. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `qa/auth-live` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Fixture và kịch bản → biên bản đã che. Thu hồi grant/session thử.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/AUTH-LIVE-ket-qua.md`, bằng chứng đã che ở `docs/qa/AUTH-LIVE/`, ảnh chọn lọc ở `docs/ui/screenshots/AUTH-LIVE/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
