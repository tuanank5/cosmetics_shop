# Kịch bản demo BeautyShop 5–7 phút

Chuẩn bị JAR, database demo đã nạp, tài khoản Customer và Admin riêng. Mở sẵn hai phiên trình duyệt để đổi vai trò. [Hướng dẫn chuẩn bị và khôi phục](../deployment/06-p10-demo.md).

1. **Guest · 45 giây.** Lọc Vanicream trên `/products`, mở Moisturizing Cream, đổi từ 4 oz sang 16 oz để thấy giá 410.000 ₫ và thêm giỏ.
2. **Customer · 75 giây.** Đăng nhập, kiểm tra giỏ, chọn địa chỉ minh họa, nhập `DEMO10`, tính lại và đặt COD. Cho xem 410.000 ₫ tạm tính, 41.000 ₫ giảm, 25.000 ₫ phí giao và 394.000 ₫ tổng thanh toán.
3. **Admin · 90 giây.** Mở đơn tại `/admin/orders`. Xác nhận, đóng gói, gửi hàng và xác nhận đã giao, thu tiền. Dashboard tăng một đơn đã thu và 394.000 ₫. Mở lại chi tiết bằng phiên Customer để xem trạng thái đã giao.
4. **Tư vấn · 45 giây.** Hỏi “Đổi trả trong bao lâu?” tại `/chat` và chỉ nguồn FAQ. Sau đó hỏi về một sản phẩm ngoài catalog. Chỉ trình diễn Ollama thật nếu đã chạy kiểm tra model.
5. **Kiểm thử · 60 giây.** Mở báo cáo TC01–TC20. Giải thích tranh SKU cuối, chặn truy cập đơn người khác và kiểm tra callback trùng với adapter MoMo mô phỏng. MoMo Sandbox thật cần merchant và IPN công khai.

Dùng [video COD 70 giây](evidence-p10/demo-cod.mp4) và [slide trình bày](beautyshop-demo-p10.pptx) cho phương án offline. Video gồm ảnh chụp các bước đã chạy trên Chrome và phụ đề.

## Q&A

| Câu hỏi | Trả lời ngắn |
|---|---|
| Vì sao hai người mua SKU cuối không thể cùng thành công? | Checkout lấy và kiểm tra tồn hiện hành dưới khóa, rồi ghi đơn và trừ tồn trong transaction. Test TC03 chạy hai checkout đồng thời. |
| Vì sao dashboard chưa tính đơn COD vừa đặt? | Giao dịch COD bắt đầu `UNPAID`. Thao tác xác nhận giao và thu tiền mới chuyển thành `PAID`. Dashboard tính mỗi đơn một lần. |
| MoMo nhận lại cùng callback thì sao? | Backend xác thực IPN, kiểm tra attempt và trạng thái trước khi chốt giữ hàng. Bài kiểm thử mô phỏng gửi lại IPN và đối chiếu kho, voucher. |
| Chatbot lấy giá từ đâu? | RAG đọc giá và tồn từ MariaDB khi trả lời. Khi Ollama chưa sẵn sàng, ứng dụng tìm câu trả lời từ FAQ và ghi rõ chế độ. |
| Nếu mất mạng lúc demo? | Khởi chạy JAR và MariaDB local từ bản sao đã kiểm tra, hoặc dùng video COD. |
