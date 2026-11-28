# SRS v1.1 — baseline v4 — Website bán mỹ phẩm đa thương hiệu

**Ngày:** 12/11/2026. **Deadline:** 03/12/2026. **Team:** 4. **Tên:** BeautyShop (tạm). **Ngân sách:** ưu tiên miễn phí.

## 1. Mục tiêu và đối tượng

Website thương mại điện tử **single-store B2C** hỗ trợ khách tìm mỹ phẩm, lựa chọn SKU, giỏ hàng, đặt và theo dõi đơn; admin quản lý catalog, tồn kho, đơn, voucher; chatbot Spring AI + RAG tư vấn trên catalog/FAQ (sau core). Không hỗ trợ nhiều seller/multi-tenant, tự động hóa vận chuyển, thanh toán production, quản lý **nhiều lô/FEFO** trong MVP (chỉ hiển thị thông tin một lô minh họa nếu có).

## 2. Actor / quyền

| Tác nhân | Quyền chính |
|---|---|
| Guest | Xem catalog, filter/search, giỏ Session, FAQ, đăng ký/đăng nhập, hỏi chatbot khi bật |
| Customer | Guest + checkout, địa chỉ, lịch sử/chi tiết/hủy đơn của mình, đánh giá sau giao, wishlist |
| Admin | Catalog/variants/stock, voucher, xử lý đơn/shipments, đổi trả, quản trị FAQ, dashboard |
| Payment Provider (external) | Gửi callback/IPN có chữ ký; không được tin redirect client |

## 3. Functional requirements

| ID | Chức năng | Độ ưu tiên | Nghiệm thu |
|---|---|---|---|
| FR-01 | Trang chủ/catalog, 30–50 sản phẩm demo đa thương hiệu | P0 | Hiển thị được SKU còn bán |
| FR-02 | CRUD category/brand/product, ảnh và thông tin thành phần/loại da | P0 | Validate dữ liệu và quyền Admin. Kiểm BR21 khi tắt SKU, sản phẩm, danh mục hoặc thương hiệu. |
| FR-03 | ProductVariant SKU, hình/giá/tồn riêng | P0 | Chọn biến thể làm thay đổi giá/tồn hiển thị |
| FR-04 | Search/filter/sort/pagination | P0 | Kết quả tìm nhất quán |
| FR-05 | Đăng ký/đăng nhập/đăng xuất, CUSTOMER/ADMIN | P0 | Mật khẩu BCrypt, phân quyền backend. Customer có thể liên kết Google/GitHub sau xác thực lại. Bấm đăng nhập Google yêu cầu chọn tài khoản. Reset dùng link 15 phút một lần và thu hồi phiên khi thành công. [Quyết định AUTH-D1–D3](../DECISIONS.md) |
| FR-06 | Giỏ Guest Session + giỏ Customer DB, merge khi login | P0 | Không gộp 2 lần, không lấy giá client |
| FR-07 | Checkout chỉ Customer, snapshot theo dòng và tối đa một mã HÀNG cùng một mã VẬN_CHUYỂN mỗi đơn | P0 | Tính lại toàn bộ tiền trên server |
| FR-08 | COD, tạo Order, bảo toàn tồn kho | P0 | Rollback nếu thiếu tồn / lỗi giao dịch |
| FR-09 | MoMo Sandbox, PaymentAttempt, IPN + idempotency | P1 có điều kiện | Duplicate callback không thanh toán/trừ kho 2 lần |
| FR-10 | Giữ SKU 15 phút online, hết hạn & đối soát | P1 gắn MoMo | Không oversell khi đồng thời |
| FR-11 | Admin cập nhật đơn và giao thủ công | P0 | Chặn chuyển trạng thái sai |
| FR-12 | Phí ship 25k, miễn >=500k; quản lý voucher FIXED/PERCENT | P0 | Phí và voucher tính đúng trên server |
| FR-13 | Danh sách và chi tiết đơn của chính Customer | P0 | Chống IDOR. Mở rộng P1 theo 09-ORDER: lọc mã, trạng thái, ngày Việt Nam, sắp xếp, giữ trang khi quay lại và mua lại vào giỏ theo BR02. |
| FR-14 | Dashboard cơ bản (đơn/doanh thu đã xác nhận thu tiền) | P0 | Không trộn đơn chưa thanh toán vào doanh thu |
| FR-15 | Customer tự hủy trước PACKING; hoàn kho/lượt voucher một lần | P0 | Giao dịch PAID đưa refund pending nếu cần |
| FR-16 | Wishlist, review với order_item DELIVERED | P1 | Không đánh giá hàng chưa nhận |
| FR-17 | Yêu cầu đổi trả trong 7 ngày, admin duyệt thủ công | P1 | Không auto nhập kho trả |
| FR-18 | Chatbot Spring AI + RAG catalog/FAQ, liên kết sản phẩm | P1 | Không bịa thành phần, giá/tồn lấy DB hiện hành |
| FR-19 | Cá nhân hóa, sentiment, forecast | P2 | Không đưa vào nghĩa vụ MVP |

## 4. Non-functional requirements

- **Bảo mật:** HTTPS khi triển khai; CSRF trên form session; mật khẩu BCrypt; role + ownership check; kiểm tra upload; không commit secret; xác minh IPN + idempotency.
- **Dữ liệu:** giá tiền DECIMAL/BigDecimal; tồn không âm; snapshot đơn; tránh hard delete dữ liệu có FK/hóa đơn; migration và backup.
- **Hiệu năng đề xuất:** 95% request catalog <2 giây ở bài kiểm thử có điều kiện xác định (không khẳng định đã đạt).
- **UI:** Premium/Luxury, tối ưu mobile, font và kích thước nút thống nhất, thông báo tiếng Việt UTF-8.
- **Khả năng chạy:** Java 21 + MariaDB, không phụ thuộc Docker để đóng gói; hồ sơ profile dev/prod.
- **Demo:** có 30–50 dữ liệu sản phẩm hợp lệ, 3 actor (Guest/CUSTOMER/Admin) và quy trình từ catalog tới đơn hoàn tất.

## 5. Out of scope

Multi-seller, vận chuyển tự động, quản lý nhiều lô/FEFO (chỉ metadata batch/expiry hiển thị), thanh toán production khi chưa có merchant, đồng bộ ERP, AI chẩn đoán bệnh, dự báo bán hàng độ chính xác thương mại.

## 6. Điều kiện chấp nhận

- E2E COD: từ xem SKU -> giỏ -> login -> checkout -> Order -> admin xác nhận/giao -> khách xem trạng thái.
- E2E MoMo Sandbox là mốc nghiệm thu có điều kiện (P1): giữ SKU -> IPN ký hợp lệ -> PaymentAttempt.PAID và Order.PENDING_CONFIRMATION -> chốt stock; nếu thiếu merchant/IPN công khai, demo adapter mock có nhãn rõ, không nói đã xử lý giao dịch thật.
- Voucher hạn dùng & giới hạn lượt được thực thi khi checkout đồng thời.
- Customer khác không đọc được đơn của nhau, Admin được truy cập khu quản trị.
- Đánh giá chỉ với SKU từ order item `DELIVERED`.

## 7. Phụ thuộc và giả định

Merchant Sandbox, endpoint HTTPS công khai phù hợp IPN, host free-tier thực tế và API AI free-tier **chưa được xác minh**. Không mô tả tính năng chưa có code là đã hoàn thành.

**05-ACCOUNT (P0).** Tại danh sách và chi tiết tài khoản, Admin theo dõi lý do khóa/mở và kết quả SMTP. Khóa có hiệu lực với password, Google và GitHub kể cả khi gửi thư lỗi. Thư thật chờ owner xác nhận kênh hỗ trợ công khai. [Quyết định ACCOUNT](../DECISIONS.md#account--quyết-định-owner-ngày-28112026)
