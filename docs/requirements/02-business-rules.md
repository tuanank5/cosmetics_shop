# Business Rules (BR) v1.0

| ID | Quy tắc / bất biến |
|---|---|
| BR01 | Guest đọc catalog và giỏ Session; muốn checkout phải login |
| BR02 | CartItem nhận diện bởi variantId; merge Session->DB đúng một lần, kiểm tra lại giá & available. Khi mua lại, mỗi thao tác cộng tối đa lượng đặt cũ còn mua được sau khi trừ lượng trong giỏ. Báo kết quả từng SKU, bỏ qua hàng ngừng bán. Giá theo SKU hiện tại. Retry cùng thao tác chỉ ghi một lần. Mua lại giữ nguyên đơn cũ, attempt, lượt voucher và kho. Muốn đặt đơn mới, Customer phải xác nhận checkout riêng. |
| BR03 | Product là cha; variant SKU có giá/ảnh/on_hand/reserved riêng, `0 <= reserved <= on_hand` |
| BR04 | Available = on_hand - reserved; không cho bán vượt available, phải dùng khóa/version/SQL update có điều kiện |
| BR05 | Giá/order total do server tính, chốt snapshot item/address/discount/ship; không lấy giá/thành tiền từ request |
| BR06 | Khi module MoMo được bật (P1), checkout online giữ SKU 15 phút nguyên tử; **có thể cần kéo dài khi gateway xử lý**; không hủy/giải phóng chỉ dựa browser timeout |
| BR07 | COD chốt xuất kho trong transaction khi tạo Order; online chuyển giữ hàng thành xuất kho sau xác thực PAID |
| BR08 | IPN xác minh chữ ký, merchant, attempt, order, currency, amount; idempotent theo transaction reference và state |
| BR09 | PaymentAttempt có thể nhiều lần/Order; một Order chỉ ghi nhận số tiền thanh toán thành công hợp lệ một lần (trừ nghiệp vụ refund riêng) |
| BR10 | Không chấp nhận callback thành công muộn nếu reservation đã hết hạn/giải phóng; đưa trạng thái cần đối soát/hoàn tiền phù hợp, không oversell |
| BR11 | Tối đa một mã HÀNG và một mã VẬN_CHUYỂN. Giao các chiều hàng đã cấu hình, hợp trong chiều, kiểm Customer riêng. HÀNG xét minSpend trước giảm, SHIP xét hàng hợp lệ sau phân bổ. Quota tổng/khách gồm HELD + USED. Miễn ship tự bỏ mã SHIP. Trả lượt một lần khi hủy hợp lệ trước giao và đã giải quyết nghĩa vụ tiền. Hoàn sau giao giữ USED. [VOUCHER-1–3](../reviews/VOUCHER-quyet-dinh-1-3.md) |
| BR12 | Phí ship 25.000đ/đơn; miễn từ subtotal **sau voucher** đạt 500.000đ (quy tắc đề xuất được ghi trong Decisions) |
| BR13 | Order state và Payment state độc lập; Customer tự hủy khi chưa PACKING và thoả điều kiện payment/fulfillment |
| BR14 | Canceled Order trả tồn kho hoặc reserved/voucher đúng một lần; nếu đã thu tiền phải theo quy trình refund |
| BR15 | Admin cập nhật giao hàng thủ công; DELIVERED là xác nhận thủ công cho mục đích demo |
| BR16 | Review chỉ qua OrderItem thuộc Customer trong Order DELIVERED, tối đa 1 review/order item |
| BR17 | Return Request trong 7 ngày kể từ deliveredAt, admin duyệt, kiểm tra hàng thực tế; không tự động nhập hàng |
| BR18 | Bất kỳ khách hàng nào chỉ được xem/sửa bản ghi thuộc sở hữu chính mình; Backend kiểm tra IDOR |
| BR19 | Chatbot RAG không truy cập dữ liệu cá nhân, không ghi dữ liệu giao dịch; giá/tồn đọc từ DB, ngoài nguồn nói chưa có thông tin |
| BR20 | Giữ lịch sử audit cho nghiệp vụ quan trọng; xóa mềm product khi đã từng bán |
| BR21 | Từ ADMIN-UX-BIZ-01, chỉ tắt SKU, sản phẩm, danh mục hoặc thương hiệu khi các đơn liên quan đã kết thúc và đã xử lý nghĩa vụ liên quan. Áp dụng cả form sửa, thao tác tắt trực tiếp và checkout đồng thời. |

## Thứ tự tính tiền

1. `subtotal = Σ(unitPriceSnapshot × quantity)`.
2. `discount = min(calculatedVoucherDiscount, maxDiscountIfAny, subtotal)` và có ngưỡng/tình trạng hợp lệ.
3. `afterDiscount = subtotal - discount`.
4. `shipping = 0` nếu `afterDiscount >= 500000`, ngược lại `25000`.
5. `total = afterDiscount + shipping`.

VND sử dụng BigDecimal/DECIMAL(15,0). Không suy đoán fee do gateway hoặc merchant quyết định. Có log đối soát nếu IPN trạng thái không rõ.

## Quy tắc release và mock gateway

- **COD là luồng P0 phải chạy thật** trên MariaDB; MoMo P1 chỉ khi đủ thời gian và tài khoản Sandbox.
- Nếu chưa có merchant, mock gateway CHỈ mô phỏng kết quả theo môi trường dev/test, phải gắn nhãn `MOCK / KHÔNG THANH TOÁN THẬT`; không mở công khai như một cổng thu tiền.
- Không được đánh dấu đã thanh toán từ tham số `GET /payment/momo/return`, kể cả khi chữ ký hợp lệ; kiểm chứng IPN, truy vấn giao dịch và trạng thái quyết toán theo đặc tả API đang dùng.
- Trễ IPN sau khi giữ hàng hết hạn phải qua đối soát; reservation chưa xác minh không được tự release chỉ do timer.
