# P15 · Phân tích bộ sưu tập tham chiếu Byredo

**B0 · 22/11/2026 · US English.** Trang bộ sưu tập tổ chức hành trình mua sắm quanh việc tìm, đánh giá và duyệt tiếp sản phẩm. Khối biên tập đặt giữa các nhóm hàng. Thông tin dịch vụ đặt sau danh sách. Đây là các quyết định tổ chức thông tin có thể áp dụng cho BeautyShop. [Bộ sưu tập tham chiếu](https://www.byredo.com/us_en/c/holiday-selection)

## Điều kiện quan sát

Công cụ đọc web truy cập được bản HTML công khai và theo được liên kết sang trang 2. Browser tích hợp không có phiên. Chrome 158 trên Windows thử trực tiếp ở 1366×768 và 390×844, cả hai lần trả `net::ERR_NETWORK_ACCESS_DENIED`. Vì vậy không có ảnh Byredo chụp trong B0. Ảnh trong thư mục B0 là ảnh BeautyShop. Hai tài liệu HTML do công cụ đọc web trả về có thể khác thời điểm thu thập. [Trang đầu](https://www.byredo.com/us_en/c/holiday-selection) · [Trang 2](https://www.byredo.com/us_en/c/holiday-selection?page=2)

| Vùng | Thấy trong tài liệu công khai | Hành vi đã kiểm |
|---|---|---|
| Đầu trang | Skip link, menu phân nhóm, tài khoản, danh sách yêu thích, ô tìm kiếm và số lượng giỏ. | Chưa kiểm mở menu, nhập từ khóa, Escape, focus, sticky hoặc bố cục mobile. |
| Đầu bộ sưu tập | Tiêu đề, tổng số kết quả, bộ lọc có bộ đếm và hai biểu tượng lưới. | Chưa kiểm mở bảng lọc, thay số cột, lưu lựa chọn hoặc lịch sử duyệt. |
| Card | Tên, ảnh, giá, nhãn và điều khiển có dấu hiệu thêm vào giỏ hoặc lưu yêu thích. | Chưa thực hiện lưu, mua hoặc chọn biến thể. Hai mục ảnh trong tài liệu chưa đủ xác nhận hành vi hover. |
| Khối giữa danh sách | Có nội dung biên tập, đích đọc tiếp và điều khiển media. | Chưa phát video, kiểm phụ đề hoặc trạng thái âm thanh thực tế. |
| Cuối danh sách | Có tiến độ, đường dẫn xem tiếp, nội dung dịch vụ, breadcrumb và nhóm liên kết hỗ trợ. | Công cụ đọc web theo được liên kết xem tiếp. Trang đầu hiển thị 18/58, đích `?page=2` hiển thị 36/58 và nhóm sản phẩm khác. Chưa kiểm việc nối DOM hay giữ vị trí cuộn bằng JavaScript. |

Các quan sát trên dựa vào thứ tự nội dung, nhãn và đích liên kết công khai. [Trang bộ sưu tập](https://www.byredo.com/us_en/c/holiday-selection) · [Đích xem tiếp](https://www.byredo.com/us_en/c/holiday-selection?page=2)

## Chuyển sang thiết kế BeautyShop

| Quyết định | Áp dụng cho BeautyShop |
|---|---|
| Điều hướng theo nhiệm vụ | Nhóm danh mục, thương hiệu và loại da quanh bộ lọc thật. Duy trì tìm kiếm bằng Enter. Đặt liên kết cá nhân theo quyền. |
| Nhịp đọc bộ sưu tập | Đặt tiêu đề trước số kết quả và bộ lọc. Dùng hệ khoảng cách U1 để tách phần công cụ khỏi card. Ghi tiến độ theo dữ liệu `Page`. Giữ Previous/Next dùng được khi tắt JS. |
| Phân cấp trên card | Cho ảnh, thương hiệu, tên, giá VND và tồn kho vị trí ổn định. Giữ vùng ảnh 4:5, vừa khung và có fallback khi tệp lỗi. Sản phẩm nhiều SKU dẫn tới bước chọn biến thể. |
| Nội dung biên tập | Tận dụng hero đồ họa CSS của BeautyShop và thông tin hỗ trợ đã duyệt. Nếu xen nội dung vào catalog, giữ vị trí tương đối của các sản phẩm và tổng kết quả. |
| Hình thức và chất liệu | Giữ nền ngà, chữ đậm sẫm, màu burgundy, viền và khoảng cách U1. Dùng heading serif và chữ nội dung hệ thống với fallback tiếng Việt đã khai báo. Đây là lựa chọn riêng cho BeautyShop. |

**Giới hạn thị giác.** Không có ảnh render tham chiếu để đo số cột theo viewport, lề, khoảng cách, font thực tế, độ dày chữ, mã màu, tỷ lệ ảnh, crop hay thời lượng chuyển động. B0 không đặt số đo Byredo giả định hoặc suy luận framework, thư viện và API nội bộ. Các tỷ lệ và token đề xuất ở trên lấy từ BeautyShop. [Design System U1](../ui/design-system.md)

**Tài sản và dịch vụ.** Logo, kiểu chữ độc quyền, ảnh chiến dịch, video, ảnh sản phẩm và nội dung quảng cáo của Byredo không dùng làm tài sản triển khai. BeautyShop chỉ gắn nhãn theo dữ liệu của mình. Các chương trình mẫu thử, gói quà hay giao hàng của thương hiệu tham chiếu không phải chính sách BeautyShop. Video chờ tư liệu có quyền và quyết định của owner về C21. [Ma trận B0](byredo-component-gap-analysis.md)
