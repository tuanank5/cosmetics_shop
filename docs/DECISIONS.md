# Quyết định dự án — source of truth v4 (12/11/2026)

| ID | Đã thống nhất | Ghi chú |
|---|---|---|
| D01 | Dự án MỚI, single-store B2C, đa thương hiệu; tên `BeautyShop` **tạm thời** | Không phải dự án Roséa Beauty |
| D02 | 4 thành viên, hạn nộp & demo **03/12/2026** | Còn 21 ngày từ 12/11 |
| D03 | Java 21, Spring Boot 3.5.16, Spring MVC, Thymeleaf, Spring Data JPA, MariaDB | Spring AI 1.1.x tương thích Boot 3.5.x |
| D04 | Ba actor Guest / Customer / Admin | GUEST anonymous, không nhất thiết có record User/Role |
| D05 | Guest xem/catalog, thêm giỏ; **đăng nhập mới checkout** | Guest session cart; sau login merge idempotent |
| D06 | 30–50 sản phẩm dựa thông tin mỹ phẩm thực tế, nhiều thương hiệu | Dữ liệu/ảnh có nguồn và quyền sử dụng phù hợp |
| D07 | Sản phẩm có SKU riêng: màu, dung tích, giá, onHand/reserved | Loại da có thể chỉ là thuộc tính, không bắt buộc SKU |
| D08 | **COD P0**; **MoMo Sandbox P1 có điều kiện** khi merchant/IPN sẵn, có adapter mock dev/test; sẵn thiết kế chuyển Production khi đủ điều kiện | Chưa có merchant production |
| D09 | Khi MoMo được triển khai, giữ SKU **15 phút** khi thanh toán online, đối soát trước khi giải phóng | Cần idempotency/IPN, không bỏ giữ hàng mù quáng |
| D10 | Giao thủ công; phí 25.000đ, miễn phí khi subtotal **sau voucher** >= 500.000đ | Quy tắc tính ngưỡng sau voucher là đề xuất kỹ thuật để thống nhất; xác nhận khi review SRS |
| D11 | Một voucher mỗi đơn, giảm FIXED/PERCENT, ngày hết hạn, lượt dùng | Đồng bộ usage reservations khi payment pending |
| D12 | Customer tự hủy trước PACKING; đổi trả trong 7 ngày, admin duyệt theo điều kiện | Đơn đã PAID có quy trình hoàn tiền riêng |
| D13 | Chỉ review khi mua và giao thành công; wishlist P1 | Trường batch/expiry *demo* tùy chọn, không FEFO |
| D14 | Spring AI + RAG đọc catalog/FAQ, không dùng lịch sử cá nhân ở MVP; demo timebox P1 ưu tiên cao | Không chẩn đoán y khoa; giá/tồn truy vấn SQL |
| D15 | UX Luxury/Premium responsive; ngân sách hosting/API AI ưu tiên miễn phí | Không giả vờ API/hosting free vận hành production |
| D16 | Trọn bộ tài liệu SDLC | SRS, UML, ERD, test, deploy, slide và demo |
| D17 | Tổ chức source theo tầng MVC dưới `com.beautyshop`, giữ 1 Spring Boot monolith và Form Login + Session | Quy tắc domain độc lập giữ package theo trách nhiệm |

| D18 | Flyway V1 toàn bộ schema, bật migrate ở dev/prod, JPA validate; DB local 3306 | Không dùng ddl-auto=update |
| D19 | Class Diagram mô tả mô hình nghiệp vụ v4. ERD và tên vật lý MariaDB theo v5. | Xem `docs/design/07-group-diagram-mapping.md` cho sơ đồ nhóm và `docs/design/02-erd.md` cho schema v5 |
| D20 | Ollama qwen2.5:3b / bge-m3 dùng cho P09 khi có tài nguyên, AI disabled mặc định | Không cam kết chạy AI trên free host |
| D21 | P01 thực hiện thao tác xóa sản phẩm bằng cách chuyển sang ngừng hoạt động để giữ tham chiếu và lịch sử đơn. | Danh mục và thương hiệu chỉ xóa vật lý khi chưa có sản phẩm tham chiếu. Tính duy nhất của đường dẫn do khóa UNIQUE trong V1 bảo đảm. |
| D22 | Tên nghiệp vụ và biến nội bộ dùng tiếng Việt không dấu. Thuộc tính JPA dùng camelCase theo cột. Giữ thuật ngữ tiếng Anh và dùng dấu `_` trước hậu tố kỹ thuật như `_Controller`, `_Repository` và `_Policy`. | Giữ tên kỹ thuật như `path`, `label`, URL và tham số form hiện hành, cùng schema V1. |
| D23 | P02 dùng khóa hàng theo thứ tự đơn → biến thể. Giữ tồn dùng bảng V1. V2 thêm dấu ghi nhận COD theo cặp đơn–SKU để chống trừ lặp. | SKU được ngừng bán bằng trạng thái. Một SKU có tối đa một bộ mã lô/hạn dùng minh họa. |
| D24 | P03 chỉ công khai sản phẩm và SKU đang hoạt động, thuộc danh mục và thương hiệu đang hoạt động. Giá từ là giá thấp nhất trong các SKU hoạt động khớp truy vấn. | SKU hết hàng vẫn xem được và ghi rõ trạng thái. Chi tiết mặc định chọn SKU khả dụng có giá thấp nhất, rồi đến SKU giá thấp nhất nếu tất cả hết hàng. |
| D25 | P04 đăng ký CUSTOMER bằng email và BCrypt. Admin được cấp quyền theo quy trình vận hành. V3 ghi mã snapshot giỏ Guest trong cùng transaction hợp nhất giỏ. | Khi hợp nhất, giới hạn số lượng theo tồn hiện hành. Giữ CSRF cho IPN đến khi P07 triển khai HMAC. |
| D26 | P05 dùng khóa hàng tài khoản để giữ một địa chỉ mặc định nếu còn địa chỉ. Khi xóa địa chỉ mặc định, chọn địa chỉ còn lại có mã nhỏ nhất. | Admin chỉ đổi trạng thái kích hoạt và không thể tự khóa. V4 tăng phiên bản xác thực khi khóa để mở khóa không khôi phục phiên cũ. |
| D27 | P06 chốt COD trong transaction, lấy giá từ SKU, làm tròn voucher phần trăm xuống đồng và miễn phí giao từ 500.000 ₫ sau giảm. | Đặt đơn tạo thanh toán COD `UNPAID`. Admin dùng thao tác “Xác nhận đã giao và thu tiền” để chuyển vận chuyển/đơn sang `DELIVERED` và thanh toán sang `PAID` trong cùng transaction. |
| D28 | P07 dùng MoMo One-Time Wallet `captureWallet` với `autoCapture=true`, API v2 create/query và IPN HMAC-SHA256. | Giữ tồn và voucher trước khi gọi gateway. Kết quả không rõ giữ nguyên tài nguyên đến đối soát. MoMo Sandbox thật đang `BLOCKED_EXTERNAL` vì thiếu merchant và HTTPS public. |
| D29 | P08 dùng các bảng V1 cho đánh giá, yêu thích và đổi trả. Đánh giá mới hoặc vừa sửa chỉ công khai sau duyệt. | Khóa hàng đơn khi gửi yêu cầu đổi trả. Yêu cầu chờ duyệt, đã duyệt, đã nhận và hoàn tất đều tính vào số lượng đã yêu cầu. Từ chối trả lại hạn mức. |
| D30 | P09 dùng Spring AI 1.1.8, Ollama và vector store trong RAM. Model chọn đoạn nguồn hoặc từ chối. MariaDB cung cấp giá/tồn SKU tại thời điểm trả lời. | Mặc định tìm FAQ, Admin nạp lại chỉ mục khi bật AI. V5 nạp FAQ chính sách. Ollama thật là `BLOCKED_AI` trong môi trường kiểm thử. |
| D31 | P11 dùng bảng voucher V1 và khóa dòng voucher khi Admin lưu hoặc xóa. Giờ quản trị dùng UTC. Các bộ đếm lượt do checkout và đối soát cập nhật. | Chỉ xóa voucher chưa có lượt sử dụng hoặc tham chiếu. Voucher có lịch sử có thể ngừng hoạt động. Các lượt MoMo đã giữ vẫn được chốt hoặc trả theo kết quả đối soát. |
| D32 | P23 lưu nội dung đánh giá (sao + bình luận) ở MongoDB, collection `danh_gia`. MariaDB vẫn là nguồn sự thật cho điều kiện BR16: đúng chủ đơn, đơn DELIVERED, mỗi `ma_chi_tiet_don_hang` một đánh giá. Store chọn bằng `app.reviews.store=mongo\|mariadb`; mặc định `mongo`. | Thay phần lưu đánh giá của D29. Không đổi schema MariaDB. Bảng `danh_gia` giữ nguyên làm nguồn chuyển một lần (`app.reviews.migrate-from-mariadb=true`, idempotent, không đè) và làm store dự phòng. Khi store là `mongo`, app không ghi vào bảng này. Duyệt dùng `phienBan` (`@Version`, khóa lạc quan). Phiên bản tăng ở mọi thay đổi: khách sửa, Admin duyệt hoặc ẩn. Admin chỉ duyệt được đúng phiên bản đã xem. Trạng thái chờ duyệt và đã ẩn phân biệt bằng `ngayDuyet`; bản `mariadb` không phân biệt được. Nội dung tối đa 1.000 ký tự. MongoDB tắt thì trang vẫn chạy, phần đánh giá báo tạm thời; store nghỉ 15 s sau lỗi kết nối. Owner duyệt qua prompt P23. |
| D33 | Ổn định baseline xác thực: Google/GitHub là tùy chọn theo từng cặp biến môi trường. OAuth chỉ tạo phiên CUSTOMER đang hoạt động. ADMIN dùng Form Login. | Form Login và OAuth dùng chung xử lý sau đăng nhập. Xác minh OTP hoàn tất đăng ký rồi chuyển về trang đăng nhập. Hoàn thiện liên kết danh tính OAuth theo ID nhà cung cấp thuộc phạm vi riêng. |
| D34 | AUTH-02. Nhận diện OAuth theo provider và subject. Một Customer có tối đa một danh tính mỗi provider. Giữ lịch sử V6, bổ sung V7. | Email trùng yêu cầu chứng minh quyền tài khoản. Reset lưu hash trong MariaDB, Admin chỉ yêu cầu gửi link, sự kiện bảo mật ghi cùng transaction với thao tác thành công. Giữ ID, thu hồi phiên khi thay đổi credential. Chủ dự án kiểm thử trước khi đồng bộ kế hoạch triển khai. |

## Điểm còn cần xác minh/định cấu hình

**P15-V · 23/11/2026 — quyết định đã được owner duyệt:** storefront chuyển sang
Dark Luxury (đen/gold muted), giữ U1 cho Admin. Chỉ sửa frontend, giữ các contract,
CSRF và nghiệp vụ. Bổ sung ScrollTrigger3.15.1 từ artifact local khớp SHA-256 owner
cung cấp; GSAP Core/pilot giữ nguyên. Video chỉ dùng khi có nguồn hợp lệ; không
thêm field database, không dùng fixture để khẳng định có media thương phẩm.
[Báo cáo triển khai](reviews/P15-V-dark-luxury-redesign.md).

- Tên, logo và thương hiệu chính thức của nhóm; domain thực tế và tài khoản Sandbox.
- Nguồn hợp lệ và quyền sử dụng hình ảnh, tuyên bố thành phần mỹ phẩm.
- Chính sách cụ thể về hoàn tiền/cửa hàng nhận lại hàng lỗi và tiến độ duyệt Sandbox.
- Địa chỉ deploy/host và khả năng hỗ trợ inbound HTTPS/IPN công khai trên free tier; MoMo là P1 conditional, không làm block COD.

## Nguyên tắc quản lý thay đổi

Khi một quyết định thay đổi: cập nhật bảng này, SRS, ERD, prompt liên quan trong **cùng PR**, và ghi ai duyệt vào mô tả PR. Không coi ý tưởng P2 là yêu cầu P0.
