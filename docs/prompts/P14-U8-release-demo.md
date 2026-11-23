# P14 / U8 – Release readiness, demo rehearsal và triển khai có kiểm soát

> **PHẦN MỞ RỘNG VẬN HÀNH, KHÔNG THUỘC P12 UI GỐC.** U8 không được hiểu là cho phép tự public, tạo tài khoản cloud hoặc tiêu tiền. Chỉ deploy khi owner cung cấp môi trường và phê duyệt rõ.
>
> **Cách dùng:** lưu `docs/prompts/P14-U8-release-demo.md`, bắt đầu `Chỉ làm U8.1 – Kiểm tra release readiness.`
>
> **Mốc lập kế hoạch:** feature freeze dự kiến 29/11/2026, demo/nộp 03/12/2026. Điều chỉnh khi owner xác nhận lịch khác; không bỏ qua kiểm thử để theo deadline.

## 0. Vai trò và mục tiêu

Bạn là Release Manager + QA Lead + DevOps Engineer. Mục tiêu: từ repo BeautyShop đưa ra **bằng chứng đủ điều kiện demo**, gói chạy lại trên máy khác, môi trường trình diễn an toàn, và rollback/backup plan; không nhầm demo local với production-ready.

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

## 1. Điều kiện đầu vào

- U1–U4: chức năng UI cốt lõi đã được nghiệm thu hoặc blocker được ghi.
- U5: các flow quan trọng có test/QA bằng chứng hoặc giải thích chưa thể verify.
- U6: baseline/performance/SEO không tạo blocker checkout hoặc security.
- U7: audit an ninh và P0; không tuyên bố 20/20 nếu vẫn Not Verified.
- F01 catalog có dữ liệu công khai **trong môi trường demo đã xác minh**; không seed database cũ để chữa triệu chứng.
- Test DB riêng cho test ghi; không gắn chung với DB demo đang được sử dụng cho rehearsal.

## 2. U8.1 – Release checklist và source provenance (read-only)

1. Ghi repo, branch, commit hash, dirty/clean, `git status`, tag nếu có; không tự checkout/reset.
2. Kiểm JDK 21, Maven, package JAR, test count; kiểm version Boot/MariaDB/Flyway, profile và các biến môi trường **chỉ tên, không lộ giá trị secret**.
3. Kiểm điều kiện F01, trang lỗi, dữ liệu demo, upload ảnh hợp quyền, tài khoản test role, MoMo mock/sandbox rõ nhãn, chatbot FAQ fallback.
4. Kiểm tình trạng 24 frontend findings, 20 security controls, dependency scan và blocker; phân loại `GO / CONDITIONAL / NO-GO` kèm lý do.
5. Soạn `docs/release/release-readiness.md`, `docs/release/release-manifest.md` (source hash/artifact hash), chưa triển khai.

**Nghiệm thu:** biết chính xác phiên bản đang demo, có blocker list, không dùng nhầm profile hoặc DB.

## 3. U8.2 – Chuẩn hóa build và clone trên máy khác

1. Kiểm `README.md`, `pom.xml`, `.gitignore`, scripts khởi động, hướng dẫn Java 21, MariaDB, secrets qua env, Flyway, mẫu cấu hình (không chứa password thật).
2. Hướng dẫn fresh clone → JDK → cấu hình env → tạo schema demo sạch → migrate → seed **chỉ DB demo mới đã xác nhận** → run JAR/IntelliJ. Không chạy scripts tự động trên máy hiện tại nếu chưa duyệt.
3. Build reproducible: `mvn.cmd -B clean verify`, `mvn.cmd -B -DskipTests package` nếu phù hợp và không ghi DB ngoài ý muốn; hash JAR. Không tính skipped là PASS.
4. Test fresh-clone bằng môi trường sạch nếu có, ghi lỗi dependency/Java/path Windows có khoảng trắng; không sửa global PATH/JAVA_HOME.
5. Tạo `docs/release/fresh-clone-runbook.md` với troubleshooting và cách xác minh health/catalog/DB mà không tiết lộ secrets.

**Nghiệm thu:** người thứ hai trong nhóm có thể chạy lại theo runbook, không cần chia sẻ password qua Git.

## 4. U8.3 – Kịch bản demo 7–10 phút và phương án dự phòng

1. Kịch bản có phân vai người thuyết trình, role Guest/Customer/Admin, từng URL/màn hình và lời nói, target thời gian theo nhu cầu người dùng; **chỉ mô tả chức năng đã hoạt động**.
2. Luồng chính (DB demo riêng): Home → Catalog có dữ liệu → PDP/SKU → Guest cart → login → COD → chi tiết đơn → Admin xác nhận đơn → Customer theo dõi; FAQ fallback → Admin quản lý SKU/voucher; MoMo **mock** nếu muốn minh họa và ghi rõ không phải thanh toán thật.
3. Dự phòng: mất internet thì local app + local MariaDB + ảnh local được cấp quyền; AI provider down → FAQ fallback; MoMo gateway down → COD hoặc mock; domain down → local demo.
4. Smoke rehearsal một lần có checklist/screenshot/số liệu test; không tạo đơn trên DB thật nếu user chưa cho phép.
5. Soạn `docs/release/demo-script-7-10min.md`, `docs/release/demo-fallback.md`, `docs/release/demo-checklist.md`.

**Nghiệm thu:** có kịch bản chạy thực tế, vai trò và dữ liệu chuẩn, diễn giải rõ phần mô phỏng.

## 5. U8.4 – Go-live / deploy plan (phải duyệt riêng)

1. **Chỉ lập phương án** deploy: chọn domain, reverse proxy/HTTPS, database private, backups, rotate credentials, monitoring, log redaction, rate limits, rollback, Cloudflare nếu chọn. Không tự trả phí/mua domain/cấp tài khoản cloud.
2. Tách config `dev`, `test`, `prod` thật; credentials theo Secret Manager/env, không đẩy vào Git; DB migration cần sao lưu và chạy kiểm soát trên staging trước.
3. Smoke domain HTTPS, headers/cookies/CORS/CSRF, origin isolation, path upload, Health, email/payment dependencies khi có domain và được phép test.
4. Backup + **restore dry run** sang DB riêng; lập người chịu trách nhiệm (4 thành viên), sơ đồ xử lý sự cố, rollback/redeploy. Không rollback database bằng drop/force repair.
5. Nếu chưa có host/domain thật, ghi `BLOCKED – external deployment`, không ghi "đã deploy".

**Nghiệm thu:** quy trình vận hành/rollback có người thực hiện, các P0 security đã được xử lý; deploy chỉ khi user cho phép.

## 6. Cổng bàn giao cuối

| Hạng mục | Đủ điều kiện khi | Nếu chưa đạt |
|---|---|---|
| Frontend | U1–U4 + F01 xử lý + browser QA | NO-GO demo chức năng nếu catalog trống |
| Test | CI/build và suite cần thiết không FAIL; skipped minh bạch | BLOCKED hoặc CONDITIONAL theo loại test |
| Database | Demo/test tách, restore/backup test an toàn | NO-GO public khi mất dữ liệu khó phục hồi |
| Auth/Security | P0 quyền, XSS/CSRF, secret, payment safety được kiểm | NO-GO public nếu P0 chưa giải quyết |
| Deployment | HTTPS, session secure, DB network private, monitoring | NO-GO public, vẫn có thể demo local kiểm soát |
| Demo | Kịch bản + tài khoản test + fallback chạy thật | CONDITIONAL, cần rehearsal |

Tạo `docs/reviews/release-U8-2026-11.md`, đường dẫn manifest/runbooks, kết luận cuối **LOCAL DEMO READY / PUBLIC READY / BLOCKED / NOT VERIFIED** độc lập; không đồng nhất local demo và public readiness.

**Lệnh cuối prompt:** `Chỉ làm U8.1 – report read-only, không deploy và DỪNG.`
