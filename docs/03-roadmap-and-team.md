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
