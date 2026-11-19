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
| D17 | Chia source theo module, giữ 1 Spring Boot monolith, Form Login + Session | Không yêu cầu JWT cho web MVP |

| D18 | Flyway V1 toàn bộ schema, bật migrate ở dev/prod, JPA validate; DB local 3306 | Không dùng ddl-auto=update |
| D19 | Class Diagram & ERD nộp theo v4; sơ đồ gốc nhóm là tài liệu tham chiếu lịch sử | `docs/design/07-group-diagram-mapping.md` |
| D20 | Ollama qwen2.5:3b / bge-m3 dùng cho P09 khi có tài nguyên, AI disabled mặc định | Không cam kết chạy AI trên free host |
| D21 | P01 thực hiện thao tác xóa sản phẩm bằng cách chuyển sang ngừng hoạt động để giữ tham chiếu và lịch sử đơn. | Danh mục và thương hiệu chỉ xóa vật lý khi chưa có sản phẩm tham chiếu. Tính duy nhất của đường dẫn do khóa UNIQUE trong V1 bảo đảm. |

## Điểm còn cần xác minh/định cấu hình

- Tên, logo và thương hiệu chính thức của nhóm; domain thực tế và tài khoản Sandbox.
- Nguồn hợp lệ và quyền sử dụng hình ảnh, tuyên bố thành phần mỹ phẩm.
- Chính sách cụ thể về hoàn tiền/cửa hàng nhận lại hàng lỗi và tiến độ duyệt Sandbox.
- Địa chỉ deploy/host và khả năng hỗ trợ inbound HTTPS/IPN công khai trên free tier; MoMo là P1 conditional, không làm block COD.

## Nguyên tắc quản lý thay đổi

Khi một quyết định thay đổi: cập nhật bảng này, SRS, ERD, prompt liên quan trong **cùng PR**, và ghi ai duyệt vào mô tả PR. Không coi ý tưởng P2 là yêu cầu P0.
