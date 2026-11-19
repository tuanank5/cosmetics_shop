> **BeautyShop v5:** tên bảng và cột MariaDB đã đổi sang tiếng Việt không dấu. Migration chuẩn là `db/migration/V1__khoi_tao_co_so_du_lieu.sql`. Bảng ánh xạ: `docs/data/bang_doi_chieu_ten.csv`. Nếu prompt cũ nhắc tên bảng tiếng Anh, phải áp dụng tên v5; không tạo trùng bảng tiếng Anh. Chỉ chạy migration mới trên database trống, không sửa V1 đã được áp dụng.

# AGENTS.md — Quy ước làm việc cho Codex và nhóm (v4 · 12/11/2026)

Đọc `README.md`, `docs/requirements/01-srs.md`, `docs/requirements/02-business-rules.md`, `docs/design/02-erd.md` và `docs/DECISIONS.md` trước khi sửa. Đặc tả **đã duyệt** trong tài liệu ưu tiên hơn các gợi ý trước đây. Nếu có xung đột, báo rõ và cập nhật nhật ký quyết định; tuyệt đối không âm thầm đổi nghiệp vụ.

## Dự án / phạm vi

- Website mỹ phẩm đa thương hiệu, **single-store**, nhóm **4 người**, nộp **03/12/2026**; `BeautyShop` là tên tạm.
- Java **21**, Spring Boot **3.5.16**, Spring MVC + Thymeleaf + Spring Data JPA + MariaDB; Maven. Không microservices, Next.js, React SPA, hoặc JWT cho web MVP.
- **Bắt buộc P0**: phân quyền Guest/Customer/Admin; danh mục, thương hiệu, sản phẩm, SKU giá/tồn; tìm kiếm/lọc; giỏ guest/customer, đăng nhập trước checkout; COD hoàn chỉnh P0, MoMo Sandbox P1 có điều kiện (không block COD), order state, tồn kho, voucher một mã/đơn, quản lý Admin, dashboard cơ bản.
- **P1**: chatbot Spring AI + RAG catalog/FAQ là mục tiêu demo có timebox và nguồn; đánh giá sau giao hàng, yêu thích, yêu cầu trả hàng (admin duyệt); MoMo Sandbox nếu merchant và HTTPS sẵn. Không nói đã có tích hợp thật khi chỉ mock.
- **P2**: cá nhân hóa, phân tích review, dự báo bán hàng, quản lý nhiều batch/FEFO, nhiều cổng thanh toán, REST API public và Production payment. Có `demoBatchCode/demoExpiryDate` hiển thị tùy chọn trên SKU nhưng không hỗ trợ đa lô.

## Kiến trúc và package

`com.beautyshop/{config,common,security,user,catalog,inventory,cart,order,shipping,payment,promotion,review,ai,admin,web}`. Mỗi feature có thể chia `entity`, `repository`, `service`, `web`, `dto` **bên trong feature** nếu cần. Không tạo lớp rỗng chỉ để đủ thư mục. Thymeleaf template ở `resources/templates/...`.

- `@Controller` cho Thymeleaf, `@Service` nghiệp vụ, repository JPA; constructor injection.
- DTO + `@Valid` + `BindingResult` cho input; PRG (`redirect:`) sau POST; thông báo và nội dung UI tiếng Việt chuẩn UTF-8.
- Tiền VNĐ dùng **BigDecimal/DECIMAL**, tuyệt đối không `double`, không tin giá gửi từ client.
- Spring Security **formLogin + Session**, BCrypt. Guest không phải record trong bảng users; khách đã đăng nhập là `ROLE_CUSTOMER`; quản trị `ROLE_ADMIN`. CSRF giữ bật với form/endpoint ghi theo session. Cấp quyền tại backend & kiểm tra sở hữu từng đơn (chống IDOR).
- Ảnh: thư mục upload riêng, validate MIME/extension/dung lượng, tên UUID, tránh path traversal; ảnh mẫu chỉ lấy từ nguồn có quyền phù hợp.
- Flyway V1 có schema thật; **Flyway bật ở cả dev/prod**, `ddl-auto=validate` (trừ khi có lý do được duyệt). **Không** dùng `ddl-auto=update` hoặc tự ý sửa V1 khi đã migrate. MariaDB local 3306 mặc định; Compose tùy chọn dùng 3307.

## Bắt buộc về giao dịch

1. Catalog có `Product` (thông tin chung) và `ProductVariant` (SKU, giá, on_hand, reserved). `available = on_hand - reserved`.
2. Guest thêm giỏ trong HttpSession. Khi login, hợp nhất vào giỏ lưu DB với kiểm tra lại giá/tồn và idempotency (không gộp 2 lần).
3. Checkout chỉ Customer, giá/discount/ship/stock tính **server-side**. COD cam kết xuất kho trong transaction. MoMo (P1) nếu được bật sẽ tạo đơn `PENDING_PAYMENT` và **giữ hàng 15 phút** nguyên tử trước khi chuyển hướng; COD không phụ thuộc MoMo.
4. IPN MoMo phải xác minh HMAC-SHA256 **đúng spec API đã chọn**, đối chiếu order ID, attempt ID, số tiền, currency, result; callback lặp không được trừ kho/ghi PAID/hoàn voucher lần hai. Browser redirect chỉ để thông báo UX; trạng thái chưa rõ phải đối soát, không giải phóng giữ hàng mù quáng.
5. Status order/payment/return tách rời; không nhảy qua trạng thái. Chỉ cho Customer tự hủy trước `PACKING` (điều kiện bổ sung theo tình trạng giao dịch); hủy sau khi đã trả tiền cần hoàn tiền hợp lệ.
6. Một voucher trên đơn, hỗ trợ PERCENT/FIXED, min subtotal, max discount, thời gian và lượt giới hạn. Với thanh toán chờ, **giữ lượt voucher** và đảm bảo không vượt hạn mức khi nhiều checkout đồng thời; ghi nhận/release đúng một lần.
7. Đánh giá phải có order item sở hữu bởi customer, đơn trạng thái `DELIVERED`; mỗi order item tối đa một review.
8. Chatbot chỉ tư vấn, không ghi dữ liệu kinh doanh, không tiết lộ dữ liệu cá nhân; RAG cho dữ liệu tĩnh, giá/tồn đọc lại DB hiện hành; không khẳng định chẩn đoán/điều trị da liễu.

## Cấu hình & an toàn

- Chỉ cho phép bypass CSRF của **POST /payment/momo/ipn**; route phải kiểm HMAC nghiêm ngặt; `/uploads/**` chỉ cho ảnh public được upload an toàn, không lộ file tùy ý.
- Không hardcode token, secret, mật khẩu tài khoản. Chỉ đọc từ env/secret store; `.env` và file runtime không commit. `application-prod.properties` KHÔNG có giá trị mặc định cho DB credentials.
- Không tạo tài khoản admin với mật khẩu demo cố định. Scaffold `BootstrapUserDetailsService` tạm từ chối login; P04 thay bằng repository-backed auth.
- Giao diện **Luxury/Premium**, font thống nhất, nút cân đối, không lỗi tiếng Việt, responsive. Tránh phụ thuộc CDN cho giao diện tối thiểu; dùng CSS tự lưu trong repo.
- Chạy **`mvn test`** và **`mvn -DskipTests package`** khi có Maven; không tuyên bố PASS nếu chưa chạy được. Đảm bảo mã nguồn chạy với DB thật ở các bước tích hợp.
- Dự án không yêu cầu Docker để xuất JAR hoặc deploy; Compose chỉ dùng để tạo DB cục bộ.

## Công việc từng task

- Chỉ 1 task/lần, có commit/PR nhỏ; mọi thay đổi từ Codex phải có danh sách file sửa, kết quả thực thi, blocker chưa xác minh.
- Thêm test tối thiểu cho các quy tắc: trạng thái đơn, tính tiền, khóa SKU, hết hạn, IPN trùng, voucher, sở hữu đơn.
- Sau mỗi task cập nhật `docs/DECISIONS.md` nếu có quyết định mới, `docs/03-roadmap-and-team.md` nếu thay đổi tiến độ; cập nhật AGENTS.md khi cấu trúc đã thay đổi.
- Không ghi 'đã triển khai' vào tài liệu chỉ vì có prompt hoặc class stub.

## Definition of Done

- [ ] Code compile; chạy với profile dev + MariaDB; build không lỗi.
- [ ] Nghiệp vụ và quyền đúng; test có chứng cứ.
- [ ] Không có dữ liệu nhạy cảm trong Git/log; giá luôn tính từ DB.
- [ ] UI responsive, thông báo lỗi tiếng Việt, trạng thái trống hợp lý.
- [ ] Có mô tả thay đổi, link PR, test, lỗi tồn đọng.
