# 02 · AUTH · Reset và liên kết danh tính

**Phase 1 · P0/Vừa**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Chặn tái sử dụng mật khẩu hiện tại và xác minh lỗi đăng nhập lại Google; duy trì ba phương thức trên cùng Customer.

**Điều kiện tiên quyết.** Sau BASE có thể sửa kiểm mật khẩu mới và kiểm đăng xuất Google. Các thay đổi TTL, quy trình gỡ/liên kết hoặc luôn hiện bộ chọn chờ quyết định riêng. Giữ chính sách đang chạy cho phần độc lập. Trạng thái tại lúc soạn prompt: **Chờ BASE · BLOCKED_OWNER một phần**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Ngày 28/11/2026 owner chốt các mục 4–5 và UX bộ chọn Google. Dùng AUTH-D1–D3 cho phần triển khai tiếp theo và đối chiếu kết quả provider thật ở WP riêng khi được giao. [Biên bản quyết định](../../../DECISIONS.md)

Có form, reset link, OTP, unlink và tăng phiên bản xác thực. MatKhau_Service, MatKhauMoi_Form, DanhTinhOAuth_Service, hai auth controller và view. V7 đủ theo rà soát. Chặn tái dùng mật khẩu, kiểm xung đột link. Rủi ro mất đường đăng nhập. Giữ V6/V7, SMTP và mô hình danh tính, không thêm role/lịch sử mật khẩu.

Điểm vào để đối chiếu mã hoặc bằng chứng: [MatKhau_Service.java](../../../../src/main/java/com/beautyshop/service/MatKhau_Service.java) · [Security_Config.java](../../../../src/main/java/com/beautyshop/security/Security_Config.java) · [OAuth2ThaoTac.java](../../../../src/main/java/com/beautyshop/security/OAuth2ThaoTac.java) · [auth](../../../../src/main/resources/templates/auth) · [OAuth2Callback_MariaDb_Test.java](../../../../src/test/java/com/beautyshop/integration/OAuth2Callback_MariaDb_Test.java)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Lần theo /forgot-password, /reset-password, /account/security, MatKhauMoi_Form, MatKhau_Service.datLai/doi và NguoiDung. Dưới khóa user, kiểm khớp hai ô, chính sách 12 ký tự/tối đa 72 byte và encoder.matches(rawNew, hashHiệnTại) trước encode/consume token/đổi version. Lỗi chọn mật khẩu phải cho nhập lại khi link còn hiệu lực. OAuth-only dùng proof đã xác minh để đặt lần đầu.
2. Giữ hash token, TTL hiện hành, resend, rate Redis, phản hồi chung, thu hồi phiên cũ, unique provider/subject và user/provider ở V7. Test đăng nhập ba phương thức vào cùng Customer, email trùng chưa liên kết, liên kết đua và chặn gỡ phương thức cuối. Giữ các FK đơn/giỏ/wishlist/địa chỉ. Không tự đổi chính sách 4–5.
3. Tái hiện phản ánh Google trên fixture trước khi sửa. POST /logout có CSRF phải đưa về /?logout. Trước khi bấm Google, mở trực tiếp /account và /orders, refresh hoặc Back rồi tải từ server, kiểm cả phiên cũ trên tab thứ hai. Nếu còn quyền hoặc /login tự kích hoạt OAuth, sửa đúng handler/redirect/session đã tái hiện và thêm regression. Phân biệt tài liệu cũ trong bộ nhớ trình duyệt với request được backend cho phép.
4. Nếu truy cập riêng tư đã bị chặn và chỉ sau khi **bấm Google** mới quay về cùng tài khoản qua callback mới, ghi nhận khả năng Google còn SSO/consent. Đối chiếu cửa sổ riêng tư và tài khoản thứ hai trong AUTH-LIVE. Giữ nguyên hành vi mặc định nếu đúng SSO. Chỉ khi owner chọn UX luôn hỏi tài khoản mới xét prompt=select_account cho login Google. Không ép nhập lại mật khẩu, logout Google toàn trình duyệt hoặc đổi GitHub/link/reauth. Giữ state/nonce/CSRF.

Tham chiếu hành vi của Google và tùy chọn bộ chọn tài khoản. [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect)

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | matches/mismatch/UTF-8/hash null |
| Integration Test | hai reset và link tranh chấp, rollback |
| Browser Test / E2E | reset, reauth, gỡ cuối, OAuth-only; POST /logout → /account bị chặn trước khi bấm Google; đối chứng sau callback mới, không ghi cookie/token. |
| Regression Test | session version, CSRF, cùng ID và giỏ merge một lần. |

**Gate của AUTH.** Mật khẩu cũ bị từ chối ở cả hai đường, token hợp lệ chỉ đổi một lần, dữ liệu cùng owner. Phản ánh Google có kết luận hành vi SSO, lỗi ứng dụng đã retest, hoặc ca thật còn chờ AUTH-LIVE. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/auth-account-methods` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Quy tắc → form → regression. Lùi binary tương thích V7, giữ token đã tiêu.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/AUTH-ket-qua.md`, bằng chứng đã che ở `docs/qa/AUTH/`, ảnh chọn lọc ở `docs/ui/screenshots/AUTH/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
