# BIZ-UX-03 · Mẫu ghi kết quả manual QA

Revision và snapshot source:

Ngày, người kiểm và Chrome/Edge version:

Fixture và trạng thái dữ liệu:

**PASS** là hành vi mong đợi đã xác nhận trực tiếp. **FAIL** là khác biệt tái hiện được. **BLOCKED** là thiếu điều kiện để chạy. **NOT RUN** là chưa thử.

| Màn, route và vai | Rộng × cao CSS px | Zoom và input | Kết quả | Mong đợi, thực tế và bằng chứng |
|---|---|---|---|---|
| Home, `/`, Guest | 390 × 844 | 100%, touch | NOT RUN | Native scroll, đủ sáu chương. |
| Home, `/`, Guest | 1440 × 844 | 100%, chuột | NOT RUN | Swiper khi đủ điều kiện, cuộn qua chương cuối tới footer. |
| Giỏ, `/cart`, Customer | 390 × 844 | 100%, bàn phím | NOT RUN | Escape ở xác nhận xóa khôi phục focus và số lượng. |
| Checkout, `/checkout`, Customer | | 200%, bàn phím | NOT RUN | CTA nhìn thấy, voucher lỗi được giữ lại. |
| Admin Dashboard, `/admin/dashboard`, Admin | | 200%, bàn phím | NOT RUN | Viền focus kỳ đang chọn hiện rõ. |
| Màn bổ sung | | | NOT RUN | |

## Phiếu lỗi sao chép cho mỗi FAIL

- ID và mức ảnh hưởng P0/P1/P2/P3:
- Route, vai và trạng thái dữ liệu:
- Browser, viewport CSS, zoom, motion và input:
- Các bước tái hiện:
- Kết quả mong đợi:
- Kết quả thực tế:
- Tần suất:
- Ảnh hoặc log đã che dữ liệu nhạy cảm:
- Kết quả kiểm lại:
