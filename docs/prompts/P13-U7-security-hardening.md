# P13 / U7 – Security audit và hardening trước khi public (20 tiêu chí)

> **PHẦN MỞ RỘNG BẢO MẬT, KHÔNG NẰM TRONG P12 UI GỐC.** Đây là công việc security riêng; **mặc định CHỈ AUDIT**. Không tự sửa Spring Security/config/DB/Cloudflare/gateway khi chưa duyệt.
>
> **Cách dùng:** lưu `docs/prompts/P13-U7-security-hardening.md`, giao `Chỉ làm U7.1 – Security audit read-only.`

## 0. Vai trò và định nghĩa đạt

Bạn là Senior Application Security Engineer + Java Spring Security Reviewer + Threat Modeler. Mục tiêu: xác minh 20 yêu cầu bảo mật trước go-live bằng source/test/HTTP evidence. Mỗi mục có `PASS / FAIL / NOT VERIFIED / BLOCKED`. **PASS phải có bằng chứng**, không suy ra từ thư viện tồn tại.

## Quy tắc chung bắt buộc

1. **Nguồn sự thật:** đọc `AGENTS.md`, `README.md`, `docs/prompts/P12-luxury-ui-upgrade.md`, `docs/prompts/P12-U2-luxury-storefront.md`, `docs/reviews/frontend-audit-2026-11.md`, báo cáo U1/U2 nếu tồn tại, rồi **đối chiếu với mã nguồn hiện tại**. Báo cáo audit chụp tại 22/11/2026, không mặc định vẫn đúng sau các commit mới.
2. **Môi trường:** `E:\Nam 4 HK 1\WWW\cosmetics_shop`; Java 21; Spring Boot 3.5.16; Thymeleaf + Spring Security; MariaDB + Flyway; CSS/JS thuần. `mvn.cmd -version` phải chỉ Java 21; chỉ chỉnh `JAVA_HOME`/`Path` trong terminal hiện tại khi cần, không sửa máy toàn cục.
3. **Thay đổi nhỏ, có kiểm soát:** đọc `git status --short` trước tiên; không reset/stash/ghi đè sửa đổi không thuộc phiên; mỗi lát cắt là một đơn vị có thể rollback. **Không tự commit/push**.
4. **Giữ contract:** không đổi URL, HTTP method, `name=`, `th:field`, `th:action`, CSRF, Spring Security role/ownership, giá, tồn, vận chuyển, voucher, thanh toán, MoMo IPN/HMAC, quy tắc hủy/đổi trả, schema/entity/migration. Nếu buộc sửa Java, service, repository hoặc thêm dependency, **đưa đề xuất file/method, tác động và regression plan, rồi chờ duyệt**.
5. **Không tạo thông tin giả:** không bịa sản phẩm, ảnh mỹ phẩm/logo thương hiệu, review, số lượng bán, địa chỉ liên hệ, pháp lý, chính sách hoặc tình trạng thanh toán. Không dùng CDN, framework frontend mới, `th:utext`, `innerHTML`, inline script/style/event, trừ ngoại lệ có căn cứ và được phê duyệt.
6. **Database an toàn:** không tự seed, đặt đơn, sửa tồn, tạo tài khoản, upload hoặc thử thanh toán trên database đang dùng `beautyshop`. Trước bất kỳ test ghi nào phải xác minh *từ cùng tiến trình test* rằng JDBC URL trỏ `beautyshop_test`, schema dữ liệu đã được xác nhận, tài khoản có quyền phù hợp và thư mục upload riêng. `P01_MARIADB_TEST=true` **không được bật** khi chưa có những điều kiện đó.
7. **Bằng chứng:** phân biệt `S` = code/source, `H` = HTTP render/headers, `B` = browser, `T` = test thực chạy. Không có browser thì ghi `NOT VERIFIED`, không tự kết luận responsive/WCAG/Lighthouse PASS. Không có đủ dữ liệu hoặc môi trường thì ghi `BLOCKED`, không ngụy tạo số đo.
8. **Test:** chạy kiểm tra an toàn với JDK 21, thống kê `Tests run / Failures / Errors / Skipped` từ log thật. Ở baseline cũ `50 run, 34 skipped`; số này **không tự trở thành PASS** sau thay đổi. Không để test tùy tiện kết nối DB demo.
9. **Ngôn ngữ thị giác:** kế thừa U1 Quiet Luxury (kem, rosewood, champagne chỉ trang trí), type serif cho heading và sans cho nội dung; nút thông thường ≥48px, CTA ≥56px, không control nhấn nào thấp hơn 44px theo tiêu chuẩn thiết kế P12; chữ dễ đọc, tương phản đo thật.
10. **Dừng có chủ đích:** chỉ làm lát cắt được giao, tạo báo cáo tiến độ, chờ nghiệm thu; không tự chuyển sang lát cắt/giai đoạn tiếp theo.

## 1. U7.1 – Audit 20 tiêu chí (chỉ đọc)

| ID | Nội dung cần xác minh | Bằng chứng tối thiểu |
|---|---|---|
| S01 | Password BCrypt/Argon2id; không lưu plaintext | PasswordEncoder + DB test hash |
| S02 | Login rate limit chống brute-force | config + test trên test env |
| S03 | Session timeout, fixation, logout invalidation | security/session config + test |
| S04 | Log không tiết lộ secrets/PII | code/log production đã redact |
| S05 | Secrets không ở JS/template/Git/build | scan an toàn, không in secret |
| S06 | Lỗi không lộ stack trace/server version | response 400/403/404/500 |
| S07 | Upload kiểm MIME/ext/nội dung ảnh | validator + test giả ảnh |
| S08 | Upload có size/request/dimension limit | cấu hình + HTTP oversize test |
| S09 | Validate ở backend | DTO/service + invalid request |
| S10 | Chặn IDOR orders/addresses/returns | test Customer A/B riêng |
| S11 | Chặn Customer/Guest vào Admin | controller/security + HTTP 403/redirect |
| S12 | SQL parameter binding, sort allowlist | repo/JdbcClient/JPA review |
| S13 | HTTPS ở public origin và ingress | đo domain/chứng chỉ đúng deploy |
| S14 | Header bảo vệ/CSP phù hợp | HTTP headers + browser functionality |
| S15 | Cookie HttpOnly/Secure/SameSite | DevTools HTTPS session |
| S16 | CORS chỉ origin cần thiết | server config + cross-origin test |
| S17 | MariaDB không public không cần thiết | network/hosting evidence, không scan trái phép |
| S18 | Runtime DB quyền tối thiểu, khác migration | `SHOW GRANTS` đã redact |
| S19 | Edge/WAF/Cloudflare nếu chọn triển khai | proxy/TLS/origin rule; tùy chọn, không bắt buộc là Cloudflare |
| S20 | Backup, restore test, uptime/error alert | restore rehearsal + cảnh báo thử |

**Ngoài checklist, kiểm riêng:** CSRF form Thymeleaf; MoMo IPN HMAC, idempotency/replay; concurrency SKU/voucher/đơn; dependency/vulnerability scan; role/ownership sau login. Đừng xem đây là 20 mục trong video nếu chúng là bổ sung riêng cho BeautyShop.

**Nghiệm thu U7.1:** mỗi mục có trạng thái và evidence/source file/line hoặc blocker; không log password/token.

## 2. U7.2 – Threat model và ưu tiên P0

1. Sơ đồ trust boundaries: trình duyệt → Spring MVC → DB; upload → filesystem; MoMo IPN (nếu bật); Ollama/AI và external provider; admin vs customer.
2. Assets: tài khoản, hash, session, địa chỉ, đơn, voucher, SKU/stock, payment signature, logs/backups.
3. Rủi ro cao: IDOR, CSRF, role bypass, replay payment, arbitrary upload, SQL injection, session hijack, exposed credentials, database public.
4. Chốt file/route chịu ảnh hưởng, khả năng khai thác **đã chứng minh hay mới suy đoán**, severity và action; không công bố exploit payload ra production.

**Nghiệm thu:** backlog P0/P1/P2 không gộp cosmetic UI vào security severity.

## 3. U7.3 – Kế hoạch hardening có phê duyệt

- Ưu tiên fix auth/role/ownership, transaction/payment safety, secrets/upload/database permissions trước security headers/Cloudflare cosmetics.
- Đề xuất patch theo mô-đun, nêu thay đổi SecurityFilterChain, controller advice, upload filters, headers, DB privileges hoặc deployment; **chỉ thực thi khi user giao rõ gói tương ứng**.
- Những thay đổi làm ảnh hưởng login/session/cookie, MoMo IPN CSRF exemption, CSP với Thymeleaf JS, reverse proxy hoặc migrations cần regression matrix trước khi sửa.
- Sử dụng `application-prod.properties` đúng production config, **không đưa mật khẩu thực vào file Git**.
- Không quét, brute-force, fuzz hay thử tấn công hệ thống bên ngoài khi không có ủy quyền.

**Nghiệm thu:** mỗi patch có test và rollback; không disable CSRF/authorization để cho test xanh.

## 4. U7.4 – Verification & release gate

- Chạy tests riêng từ fixture/DB test độc lập; test IDOR với hai tài khoản; CSRF với/không token; rate 429; cookie sau HTTPS; upload invalid/oversize; payment mock repeat.
- Lưu test report `PASS/FAIL/NOT VERIFIED/BLOCKED`, file/line, command không có secret, screenshots/header đã redact.
- **Security gate:** Không khuyến nghị public nếu còn P0 chưa được xử lý hoặc chưa cô lập. Nhiều tiêu chí hạ tầng (HTTPS/edge/backup) chỉ có thể xác minh tại deployment thật.

## 5. Báo cáo và DoD

- `docs/security/security-audit-20-checklist.md`
- `docs/security/threat-model.md`
- `docs/reviews/security-U7-2026-11.md`

**Definition of Done:** đủ 20 mục với trạng thái trung thực và bằng chứng; các hardening được phê duyệt và test; bí mật không lộ; không lẫn audit với deploy/sửa tự động.

**Lệnh cuối prompt:** `Chỉ làm U7.1 – audit read-only, tạo báo cáo và DỪNG.`
