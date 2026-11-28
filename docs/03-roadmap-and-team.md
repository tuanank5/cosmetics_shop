# Roadmap v4 — 21 ngày (12/11 → 03/12/2026), 4 người

**Ưu tiên P0 = COD E2E hoàn chỉnh, an toàn và có website demo công khai.** MoMo Sandbox (P1) và RAG demo (P1 ưu tiên cao) không được ngăn tiến độ COD. Feature freeze **29/11**, 30/11–02/12 QA/deploy/dự phòng.

| Ngày | Milestone | Owner | Điều kiện nghiệm thu |
|---|---|---|---|
| 12–13/11 | V4 docs/ERD, Flyway V1, boot, UI shell | D và cả nhóm | Flyway migrate sạch trên MariaDB thực, smoke web |
| 14–17/11 | Catalog category/brand/product/SKU + Security/register/login | A/B | CRUD, validate, role, Flyway schema | 
| 16–20/11 | Tìm kiếm/lọc, voucher, giao diện Shop, nhập liệu demo lô đầu | A/B/D | Search, voucher calculation, 15 sản phẩm kiểm nguồn |
| 19–24/11 | Cart Guest/customer, COD, Order, Inventory, Admin shipping | C (A/B hỗ trợ) | **P0 COD E2E PASS ngày 24/11** |
| 24–27/11 | Review + Wishlist, return request, dashboard cơ bản | D/B | Quyền review sau DELIVERED, luồng đổi trả |
| 24–29/11 | Chatbot RAG catalog/FAQ (ưu tiên cao, timebox), 30–50 sản phẩm | D/A | Chatbot có nguồn hoặc fallback rõ, danh sách nguồn |
| 25–29/11 | MoMo Sandbox **P1 chỉ nếu có merchant + IPN HTTPS**, mock adapter nếu thiếu | C | Không hứa payment production, không ghi PAID giả |
| **29/11** | Feature freeze | Cả nhóm | Tag RC, backup dữ liệu demo |
| 30/11–02/12 | Full QA, fix, deploy, SRS/report/slides/video | Cả nhóm | COD E2E, regression, screenshots, trình bày |
| **03/12** | **Nộp & demo** | Cả nhóm | Website + source + tài liệu + hướng dẫn |

## Kết quả kiểm chứng

- **P00:** JAR chạy với profile `dev`. `/`, `/products` và `/login` trả HTTP 200. Xác nhận hiển thị ở 375, 768 và 1440 px còn mở.
- **P01:** test tích hợp xác minh CRUD danh mục, thương hiệu và sản phẩm bằng phiên Admin do Spring Security Test cấp. Đăng nhập bằng tài khoản thật thuộc P04.
- **Sau refactor MVC:** JDK 21 chạy compile, 15/15 test và đóng gói JAR thành công. Flyway áp dụng V1 trên MariaDB 12.2.2 trống và JPA xác thực schema. Flyway cảnh báo phiên bản MariaDB này mới hơn phạm vi kiểm chứng của nó.
- **P02:** JDK 21 chạy compile, 22/22 test và đóng gói JAR thành công. Flyway áp dụng V1 và V2 trên MariaDB 12.2.2 trống. Test tích hợp kiểm chứng CRUD/ảnh SKU bằng phiên Admin kiểm thử, hai đơn COD hoặc hai đơn MOMO tranh một SKU, retry, rollback và đọc lại tồn khi checkout đã nạp SKU. Test MOMO chỉ sử dụng dữ liệu đơn nội bộ.
- **P03:** JDK 21 chạy compile, 23/23 test và đóng gói JAR thành công với MariaDB 12.2.2. Test kiểm chứng tìm tiếng Việt và theo SKU, bộ lọc, sắp xếp, phân trang, giá/tồn theo SKU và HTTP 404. JAR trả HTTP 200 cho danh sách và chi tiết có dữ liệu kiểm thử. Kiểm tra trực quan các viewport còn mở do công cụ không truy cập được cửa sổ Chrome.
- **P04:** JDK 21 chạy compile, 28/28 test và đóng gói JAR thành công. Flyway áp dụng V1–V3 trên MariaDB 12.2.2 trống. Test xác minh đăng ký BCrypt, email duy nhất, login/logout, đổi session ID, quyền Guest/Customer/Admin, CSRF, kiểm sở hữu đơn và hai yêu cầu gộp cùng snapshot giỏ.
- **P05:** JDK 21 chạy compile, 31/31 test và đóng gói JAR thành công. Flyway áp dụng V1–V4 trên MariaDB 12.2.2 trống. Test xác minh sửa hồ sơ, địa chỉ mặc định khi ghi song song, từ chối sửa/xóa địa chỉ của người khác, phân trang quản trị, khóa/mở và thu hồi phiên cũ.
- **P06:** JDK 21 chạy compile, 34/34 test và đóng gói JAR thành công với MariaDB 12.2.2. Luồng Guest → đăng nhập → đặt COD → Admin giao và thu tiền đã qua test tích hợp. Đã kiểm tra hủy và hoàn đúng một lần, snapshot giá/địa chỉ, quyền chủ đơn, rollback, tranh SKU và tranh lượt voucher. P06 dùng schema V1–V4.
- **P07:** JDK 21 chạy compile, 40/40 test và đóng gói JAR thành công với MariaDB 12.2.2. Đã kiểm chứng luồng mock dev/test, HMAC, IPN sai đơn/số tiền, replay, retry, đối soát, hết hạn và kết quả thành công muộn. COD tiếp tục qua test hồi quy. **Sandbox end-to-end: BLOCKED_EXTERNAL** vì chưa có merchant và HTTPS công khai được xác minh. [Hướng dẫn P07](deployment/04-momo-sandbox.md).
- **P08:** JDK 21 chạy compile, 44/44 test và đóng gói JAR thành công với MariaDB 12.2.2. Test kiểm chứng đánh giá từ đơn đã giao, duyệt đúng phiên bản nội dung, yêu thích chống trùng, quyền sở hữu, từ chối đổi trả ngày thứ tám và hai yêu cầu cạnh tranh số lượng. Trạng thái đổi trả không sửa đơn, tồn kho hay thanh toán.

- **P09:** JDK 21 chạy compile, 48/48 test và đóng gói JAR thành công với MariaDB 12.2.2. Flyway áp dụng V1–V5. Test qua Spring AI và vector store với HTTP mock Ollama kiểm tra FAQ fallback, trích nguồn, giá/tồn hiện hành, CSRF và câu hỏi ngoài nguồn. **Ollama thật: BLOCKED_AI**. [Hướng dẫn P09](deployment/05-chatbot-rag.md).
- **P10:** JDK 21 chạy compile, 49/49 test và đóng gói JAR thành công. Chrome hoàn tất COD E2E và kiểm tra 375, 768, 1440 px. Đã nạp 31 sản phẩm, 32 SKU và kiểm tra backup/restore trên MariaDB 12.2.2. **Host công khai: BLOCKED_HOST.** [Báo cáo QA](testing/02-p10-results.md) · [Runbook](deployment/06-p10-demo.md) · [Hồ sơ demo](report/README.md).
- **P11:** quản trị tại `/admin/vouchers`. JDK 21 chạy compile, 51/51 test và đóng gói JAR thành công với MariaDB 12.2.2. Test kiểm chứng quyền Admin, CSRF, mã trùng, điều kiện giảm giá, tranh lượt cuối khi đặt COD, giữ/chốt/trả lượt với MoMo mock và bảo toàn voucher đã có lịch sử. Chrome kiểm chứng Admin tạo, Customer áp mã và đặt COD, bộ đếm từ 0 lên 1, Admin ngừng hoạt động và trang quản trị ở 375, 768, 1440 px. Customer truy cập trang quản trị nhận HTTP 403, log ghi thiếu template lỗi dùng chung. [Prompt P11](prompts/P11-promotion.md) · [Contract](design/03-order-payment-inventory.md#contract-p11-cho-checkout-và-momo).

- **P12/U1 (22/11)** đã triển khai token, control/nút, head/header/footer, lỗi tiếng Việt và mẫu đăng nhập/đăng ký. JDK 21 chạy 29 test thành công, bỏ qua 34 test MariaDB và đóng gói JAR thành công. Nghiệm thu browser và dev với DB demo riêng còn mở. [Báo cáo U1](reviews/ui-upgrade-2026-11.md)
- **P12/U2.1 (22/11)** đã nâng cấp trang chủ và catalog. JDK 21 chạy 31 test thành công, bỏ qua 34 test MariaDB và đóng gói JAR thành công. Chrome kiểm tra năm viewport trên HTML Thymeleaf với dữ liệu kiểm thử. F01 vẫn bị chặn do chưa xác nhận môi trường demo có catalog hoạt động. [Báo cáo U2.1](reviews/ui-upgrade-U2-2026-11.md)
- **P12/U2.2 (22/11)** đã nâng cấp trang chi tiết sản phẩm và xử lý F03. Chrome kiểm chọn SKU, thêm giỏ Guest/Customer, năm viewport và zoom 200% trên fixture. JDK 21 chạy 35 test đạt, 34 test MariaDB bỏ qua, đóng gói JAR thành công. F01 vẫn chặn nghiệm thu dữ liệu demo. [Báo cáo U2.2](reviews/ui-upgrade-U2-2026-11.md#u22)
- **P12/U2.3 (22/11)** Giỏ đọc ảnh SKU, thương hiệu và slug bằng truy vấn hiện có. Hai SKU cùng sản phẩm mở đúng biến thể và giữ đúng tạm tính sau đăng nhập trên fixture. Chrome đạt ở 5 viewport, zoom 100% và 200%. JDK 21 có 43 PASS, 0 FAIL, 0 ERROR và 34 SKIPPED. Còn chờ ảnh thương phẩm và đối chiếu giao dịch trên MariaDB test độc lập. F01 vẫn BLOCKED. [Báo cáo U2.3](reviews/ui-upgrade-U2-2026-11.md#u23-du-lieu)

Xem [README](../README.md#chạy-local) để chạy lại test tích hợp.

## Giao trách nhiệm 30–50 mỹ phẩm

- **A (lead dữ liệu):** 15–20 SKU thông tin thật có nguồn chính thức; kiểm tra brand/category, SKU, giá demo và license hình.
- **D (UI/seed + kiểm nguồn):** 15–20 SKU, ảnh tự chụp hoặc được phép; kiểm logo, alt text, nguồn sản phẩm, bảng audit dữ liệu.
- **B (QA nguồn & import):** 5–10 SKU, rà soát nhãn loại da/thành phần, chuẩn hóa file seed/import, đảm bảo không trích khẳng định chưa chứng thực.
- **C (QA vận hành):** 5 SKU, kiểm tình huống SKU còn 1, hết hàng, hủy COD, giá snapshot, kiểm dữ liệu seed với Order.
- **Deadline dữ liệu:** tối thiểu 15 hàng đã kiểm ngày 20/11; đủ 30–50 hàng có nguồn ngày **27/11**. [Catalog P10](data/product-research.csv) đã có 31 sản phẩm đối chiếu từ trang thương hiệu và minh họa CSS của dự án.

## Contract & tích hợp

| Thành viên | Primary | Mốc phụ thuộc |
|---|---|---|
| A | Catalog, SKU, Inventory | Chốt contract InventoryService với C trước 18/11 |
| B | Auth, Address, Voucher, dashboard | Chốt VoucherService và UserId trước 19/11 |
| C | Cart, COD, Order, MoMo (conditional), Shipping | COD chạy trên MariaDB thật ngày 24/11 |
| D | Layout, Reviews, Returns, AI demo, tài liệu | RAG song song theo dữ liệu A cung cấp, không block COD |

**Migration:** chỉ một reviewer phụ trách mỗi migration. V1 có schema gốc; thêm trường/bảng mới qua V2+, không sửa V1 sau khi được áp dụng.

## Fallback khi trễ

1. Bất biến không được cắt: Auth + ownership, Catalog/SKU, Cart, COD, checkout, kho, dashboard đơn hàng, tài liệu cốt lõi.
2. RAG giữ **demo timebox** nếu khả thi; fallback FAQ search/knowledge base có nhãn `không phải AI` nếu thiếu model/API.
3. MoMo mock chỉ test nội bộ, không thay thế bằng chứng giao dịch Sandbox thật.
4. Wishlist/returns/report nâng cao cắt trước; không được tuyên bố đã làm khi còn stub.

## P15-V · Dark Luxury storefront · 23/11/2026

Owner phê duyệt palette tối và ScrollTrigger local cho Home. Đã triển khai Home3
chapter, Catalog editorial4/2/1 cột, optional hover-video hook; giữ Backend/B2/B4.
JDK21:120 PASS/0 FAIL/0 ERROR/34 SKIPPED, JAR đóng gói. Nội dung video thương phẩm,
F01/MariaDB và nghiệm thu thị giác của owner còn mở. Không mở rộng scope/commit/push.
[Báo cáo và bằng chứng](reviews/P15-V-dark-luxury-redesign.md).

## P20C · Dashboard quản trị v2 · 25/11/2026

Đã bổ sung KPI, biểu đồ, bảng cảnh báo và sidebar quản trị. Test offline và tích hợp MariaDB trên database trống đã đạt. Chrome đã kiểm bố cục và biểu đồ bằng fixture MockMvc. Phiên QA Chrome với tài khoản Admin của owner và dữ liệu demo đang chờ cấu hình kết nối. [Báo cáo và bằng chứng](reviews/P20C-dashboard-v2.md)

## SEC-01 · HTTP/error · 26/11/2026

G1 đạt trên fixture riêng: ownership 404 qua REQUEST/ERROR dispatch, role/CSRF 403, upload/form và IPN giữ hợp đồng; dùng fragment lỗi chung, bổ sung nội dung 429. JDK21: 229 đạt/288 tổng, 59 skip; MariaDB 21/21; package thành công. DH-05 phiên owner chưa tái hiện. [Báo cáo và bằng chứng](reviews/SEC-01-ket-qua.md). DATA-01 chờ giao riêng; chưa commit/push.

## DATA-01 · Kho đánh giá MongoDB · 26/11/2026

Đã nghiệm thu G2 cho MongoDB duy nhất và giữ bảng đánh giá SQL lịch sử. Hồi quy, phục hồi fixture và kiểm trạng thái tạm ngưng trên Chrome headless đã đạt. [Báo cáo](reviews/DATA-01-ket-qua.md).

## ARCH-01 · Package model · 26/11/2026

Đã chuyển 11 lớp ánh xạ sang `com.beautyshop.model`. G3 đạt trên MariaDB và MongoDB riêng, gồm Flyway V1–V7, JPA validate và đọc/ghi BSON hai chiều. [Báo cáo](reviews/ARCH-01-ket-qua.md).

## NAV-01 · Điều hướng và đường quay lại · 26/11/2026

G4 đạt trên fixture MariaDB và Chrome headless. Logo mở chương 1, PDP quay lại đúng thẻ popular, checkout lấy lại giỏ và địa chỉ hiện hành sau khi quay về từ form. [Báo cáo](reviews/NAV-01-ket-qua.md).

## UI-01C · Disclosure, mật khẩu và thông báo · 26/11/2026

G5 đạt trên fixture MariaDB và Chrome headless. Ô mật khẩu ổn định qua init và hiện/ẩn. Bộ lọc, PDP, menu, bảng dashboard và xác nhận Admin đã kiểm bằng bàn phím và chế độ tắt JavaScript. Maven đạt 234/293, tích hợp 34/34 và đóng gói thành công. UI-01P chờ giao riêng. [Báo cáo](reviews/UI-01C-ket-qua.md)

## UI-01P · Phân trang GET · 26/11/2026

G6 đạt trên fixture MariaDB, MongoDB và Chrome. Catalog, tài khoản và hai màn đánh giá dùng chung pager. Đã kiểm bộ lọc, SKU, giới hạn trang, thứ tự khi trùng timestamp, bàn phím và Back khi tắt JavaScript. CART-01 chờ giao riêng. [Báo cáo](reviews/UI-01P-ket-qua.md)

Bổ sung phân trang cho đơn của khách, yêu thích, đổi trả, các danh sách catalog và mã giảm giá quản trị. P05 áp dụng trần 10 địa chỉ giao hàng theo D42. [Kết quả và cấu hình từng màn](reviews/LIST-PAGING-ket-qua.md)

## CART-01 · Thông báo gộp giỏ · 26/11/2026

G7 đạt trên MariaDB và Chrome. Checkout hiển thị số lượng điều chỉnh theo từng SKU còn tồn dương sau gộp. Test xác nhận 12→10 giảm 2, mỗi snapshot gộp một lần, rollback giữ snapshot và refresh/Back không phát lại thông báo. CAT-01 chờ giao riêng. [Báo cáo](reviews/CART-01-ket-qua.md)

## CAT-01 · Vòng đời catalog · 26/11/2026

G8 đạt trên MariaDB và Chrome. Admin dùng Ngừng hoạt động và Kích hoạt lại; URL xóa cũ của danh mục và thương hiệu trả 404. Đã kiểm visibility, SKU, voucher, COD và snapshot đơn. WISH-01 chờ giao riêng. [Báo cáo](reviews/CAT-01-ket-qua.md)

## WISH-01 · Yêu thích tại chỗ · 26/11/2026

G9 đạt trên MariaDB, MongoDB và Chrome. Home, catalog, PDP và danh sách yêu thích dùng cùng trạng thái theo sản phẩm. Đã kiểm luồng Guest và no-JS, hai tab, mất phản hồi, phiên thu hồi và sản phẩm vừa tắt. ORDER-01 chờ giao riêng. [Báo cáo](reviews/WISH-01-ket-qua.md)

## ORDER-01 · Danh sách đơn Admin · 26/11/2026

G10 đạt trên fixture MariaDB và Chrome headless. Bộ lọc và phân trang chạy trong SQL theo trạng thái thanh toán hiệu lực, giữ tổng toàn hệ thống riêng. Đã kiểm link dashboard, quyền sở hữu, COD/MoMo mock và quay lại đúng trang, đúng dòng với JavaScript tắt. REVIEW-01 chờ giao riêng. [Báo cáo](reviews/ORDER-01-ket-qua.md)

## REVIEW-01 · Kiểm duyệt đánh giá · 26/11/2026

G11 đạt trên MariaDB, MongoDB và Chrome headless. Duyệt và ẩn giữ bộ lọc, cập nhật count và focus, đồng thời đưa URL về trang hợp lệ nếu dòng cuối rời lọc. Đã kiểm revision cũ, mất phản hồi, MongoDB ngừng hoạt động và JavaScript tắt. DTO-01 chờ giao riêng. [Báo cáo](reviews/REVIEW-01-ket-qua.md)

## DTO-01 · Projection theo màn · 26/11/2026

G12 đạt. Thu hẹp cột đọc cho địa chỉ, catalog Admin và trang bảo mật. 13 mẫu HTML khớp sau chuẩn hóa CSRF, số SELECT JPA giữ nguyên khi tăng từ 1 lên 7 dòng. Tích hợp 144/144 và Chrome 2/2. QA-01 chờ giao riêng. [Báo cáo](reviews/DTO-01-ket-qua.md)

## QA-01 · Bản tích hợp · 26/11/2026

**G13 local đạt, QA-01 còn BLOCKED ở CI của revision tích hợp và quyết định phát hành.** Bản sửa HTML RAG đạt 3/3 ca tích hợp và 8/8 tổ hợp Chrome. JDK 21 compile/test/package đạt, hồi quy MariaDB/MongoDB có 312 đạt, 2 skip và 0 fail/error. BSON, phục hồi SQL/MongoDB/ảnh và REVIEW-01 đã đạt. OAuth Google/GitHub, Redis và SMTP thật còn chờ xác minh. GitHub Actions đạt cho `38fb1ca`. [Báo cáo](reviews/QA-01-ket-qua.md)

## LOCAL-SETUP-01 · Thiết lập local và nâng cấp · 26/11/2026

MariaDB fixture xác nhận schema trống và lịch sử chuẩn V1–V6 lên V7, seed chạy lặp giữ dữ liệu và một lệnh demo mở được web. Giữ V1–V7 để đối chiếu checkout cũ. Cấu hình Compose đã kiểm tĩnh và còn chờ chạy trên máy có Docker. [Báo cáo](reviews/LOCAL-SETUP-01-khoi-tao-va-nang-cap.md)

## UI-COPY-01 · Thuật ngữ giao diện

Đã đồng bộ câu chữ cho Guest, Customer và Admin. V5 nạp FAQ đã sửa khi khởi tạo MariaDB trống theo D37. Chrome kiểm ba nhóm tương tác. Hồi quy MariaDB/MongoDB có 313 đạt, 5 bỏ qua và 0 lỗi. [Báo cáo](reviews/UI-COPY-01-ket-qua.md)

## BIZ-UX-01 · Nghiệp vụ và giao diện · 27/11/2026

Đã sửa báo giá khi SKU thiếu tồn, phục hồi giỏ khi phản hồi đến chậm hoặc thất lạc, và đích điều hướng tư vấn/đổi trả. Giỏ Guest 7 SKU giảm từ 7 SELECT xuống 1 SELECT. COD tới Admin xác nhận đạt qua HTTP trên database cô lập. Nghiệm thu trực quan và ma trận tương tác còn bị chặn do trình duyệt. [Báo cáo](reviews/BIZ-UX-01-ra-soat-nghiep-vu-uiux.md) · [Checkpoint](reviews/BIZ-UX-01-tiep-tuc.md)

## BIZ-UX-02 · Responsive và accessibility · 27/11/2026

Đã sửa điều kiện bật Swiper, bổ sung đường dẫn từ gợi ý tư vấn tới đúng SKU, nối lỗi nhập với trường SKU và cải thiện viền focus của bộ chọn kỳ Admin. HTTP trước/sau đạt 35 lượt mỗi bên, integration 22/22 và Maven package đạt. Chrome, Edge và Firefox chưa render được trong môi trường QA của phiên này, nên ảnh trước/sau và nghiệm thu responsive thực vẫn mở. [Báo cáo](reviews/BIZ-UX-02-visual-qa-uiux.md) · [Checkpoint](reviews/BIZ-UX-02-tiep-tuc.md)

## BIZ-UX-03 · Hồi quy và nghiệm thu giao diện · 27/11/2026

HTTP đạt 35 phép kiểm route, MariaDB/MongoDB đạt 38/38. Truy vấn DB sau hai POST đồng thời xác nhận một đơn, một lần trừ tồn và một lượt voucher. Chrome/Edge vẫn chặn ở renderer với profile/cache mới. Đã bàn giao fixture và checklist để nghiệm thu thủ công trước release. [Báo cáo](reviews/BIZ-UX-03-nghiem-thu-giao-dien.md) · [Checkpoint](reviews/BIZ-UX-03-tiep-tuc.md)

## ADMIN-UX-BIZ-01 · Đơn hàng, catalog và Footer · 27/11/2026

Đã triển khai Footer liên hệ, bảng Admin Orders/Catalog/SKU và điều kiện ngừng hoạt động theo đơn liên quan. Tồn theo lô được bàn giao thiết kế chuyển đổi, kho hiện hành tiếp tục theo SKU. [Báo cáo](reviews/ADMIN-UX-BIZ-01-ket-qua.md) · [Checkpoint](reviews/ADMIN-UX-BIZ-01-tiep-tuc.md)

## CHAT-ROUTE-01 · Gỡ form tư vấn cũ

Đã gỡ route và mã riêng của form tư vấn theo D41. Khách mở chức năng tư vấn tại `/chat`. [Kết quả](reviews/CHAT-ROUTE-01-ket-qua.md)

## Kế hoạch tiếp theo chờ duyệt · 28/11/2026

Revision ứng viên là `a9870e1`, trùng remote main và Maven CI đã đạt. Owner yêu cầu rà soát tài liệu trước khi giao việc. Đề xuất 10 phase, 19 WP, BASE đứng đầu. AUTH-LIVE, PAY-LIVE và UI-UAT giữ phạm vi nghiệm thu riêng. AI đợt này chỉ dùng mock. Toàn bộ hạng mục lô/FEFO và AI-LIVE ở backlog. [Sổ trạng thái mới](reviews/BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) · [Kế hoạch và tiêu chí](reviews/BEAUTYSHOP_NEXT_UPGRADE_PLAN.md)

### Nghiệm thu các WP nâng cấp · 28/11/2026

BASE đã nghiệm thu tại `f390c3a` (PR #6), AUTH tại `0ed547b` (PR #7), QUALITY tại `e33e4e3` (PR #8) và PERF tại `98d2fa3` (PR #9). Ứng viên BASE đạt 332 ca tích hợp, 5 ca bỏ qua. AUTH-FINAL đạt 40/40 trên fixture MariaDB/WebKit. Tiến độ tại mốc PERF là 4/19 DONE. [Sổ 19 WP](reviews/BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md)

PAYMENT giữ PARTIAL_ACCEPTED / BLOCKED_DEPENDENCY. Lịch sử từng lần thanh toán đã merge tại `4fa2262`, CI main PASS theo owner. Tổng tiền, snapshot hai slot, đối soát và regression sau VOUCHER còn mở. PAY-LIVE nghiệm thu riêng trên MoMo Sandbox. [PR #10](https://github.com/tuanank5/cosmetics_shop/pull/10) · [Báo cáo PAYMENT](reviews/PAYMENT-ket-qua.md)

28/11/2026 · Owner chọn AUDIT-A. AUDIT DONE trên merge `5bdada6bb370b281319ff671fd8a9b62b5673560` (PR #11, CI main PASS theo owner). D7 và guard D6 đạt. AUDIT PURGE — BLOCKED_EXTERNAL. Giữ công tắc tắt cho đến khi nghiệm thu độ phủ, quyền cấp, thu hồi, thời hạn và retry của nguồn clearance. Tiến độ 5/19 DONE. Owner giao VOUCHER, đang IN_PROGRESS.

28/11/2026 · Sau merge PR #12 tại `e4c00c8`. VOUCHER — MERGED / READY_FOR_OWNER_ACCEPTANCE. PAYMENT — PARTIAL_ACCEPTED, bốn gate tích hợp PASS local trên merge với MoMo mock. Chờ owner nghiệm thu VOUCHER và xác nhận phạm vi PAYMENT mock. Tổng 5/19 DONE. AUTH-LIVE và AUDIT PURGE tiếp tục BLOCKED_EXTERNAL. PAY-LIVE chưa nghiệm thu. [Kiểm chứng PAYMENT](reviews/PAYMENT-ket-qua.md#revalidation-pr-12)

## Nghiệm thu ngày 28/11/2026

Owner chốt **8/19 DONE** gồm BASE, AUTH, QUALITY, PERF, AUDIT, VOUCHER, PAYMENT và ORDER. VOUCHER và PAYMENT được nghiệm thu với COD và MoMo mock tại `e4c00c8`. ORDER được nghiệm thu tại `13c586a`. AUTH-LIVE, PAY-LIVE và AUDIT PURGE tiếp tục BLOCKED_EXTERNAL. [Quyết định owner](DECISIONS.md#nghiệm-thu-voucher-payment-và-order-ngày-28112026)
