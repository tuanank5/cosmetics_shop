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

Xem [README](../README.md#chạy-local) để chạy lại test tích hợp.

## Giao trách nhiệm 30–50 mỹ phẩm

- **A (lead dữ liệu):** 15–20 SKU thông tin thật có nguồn chính thức; kiểm tra brand/category, SKU, giá demo và license hình.
- **D (UI/seed + kiểm nguồn):** 15–20 SKU, ảnh tự chụp hoặc được phép; kiểm logo, alt text, nguồn sản phẩm, bảng audit dữ liệu.
- **B (QA nguồn & import):** 5–10 SKU, rà soát nhãn loại da/thành phần, chuẩn hóa file seed/import, đảm bảo không trích khẳng định chưa chứng thực.
- **C (QA vận hành):** 5 SKU, kiểm tình huống SKU còn 1, hết hàng, hủy COD, giá snapshot, kiểm dữ liệu seed với Order.
- **Deadline dữ liệu:** tối thiểu 15 hàng đã kiểm ngày 20/11; đủ 30–50 hàng có nguồn ngày **27/11**. File `docs/data/product-research.csv` là template, chưa giả vờ dữ liệu được nghiên cứu.

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
