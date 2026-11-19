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

## Giao trách nhiệm 30–50 mỹ phẩm (bắt đầu sớm, không dồn cuối)

- **A (lead dữ liệu):** 15–20 SKU thông tin thật có nguồn chính thức; kiểm tra brand/category, SKU, giá demo và license hình.
- **D (UI/seed + kiểm nguồn):** 15–20 SKU, ảnh tự chụp hoặc được phép; kiểm logo, alt text, nguồn sản phẩm, bảng audit dữ liệu.
**Xác minh P01:** CRUD danh mục, thương hiệu và sản phẩm tại `/admin/catalog` đã chạy qua test tích hợp với profile `dev`, JDK 21 và MariaDB 12.2.2. Flyway áp dụng V1 trên database trống. Maven chạy 15 test thành công và đóng gói JAR. Test dùng phiên Admin do Spring Security Test cấp. Đăng nhập bằng tài khoản thật thuộc P04.

- Chạy lại test tích hợp bằng cách trỏ `DB_URL`, `DB_USER`, `DB_PASSWORD` vào một database MariaDB trống dành riêng cho kiểm thử, đặt `P01_MARIADB_TEST=true`, rồi chạy `mvn test`.

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
