# P20C · Dashboard quản trị v2

## Phạm vi

Dashboard tại `/admin/dashboard` có tám KPI, cụm biểu đồ ưu tiên, phần **Phân tích thêm** và các bảng cần kiểm tra. Điều hướng quản trị dùng sidebar cố định từ 1100 px và nút **Menu quản trị** dưới mốc đó. Bộ lọc và trang hiện tại của **Đánh giá** được gửi lại qua form duyệt/ẩn và giữ qua redirect sau POST.

Nguồn nghiệp vụ là định nghĩa ở §2 của P20 và kho đánh giá P23. Dashboard đọc dữ liệu qua JDBC và `DanhGia_Store`. Các truy vấn bổ sung chỉ đọc và dùng schema Flyway V1–V5. Biểu đồ tiếp tục dùng Chart.js 4.6.1 cục bộ. [P20](P20-admin-dashboard.md) · [P23](P23-danh-gia-mongodb.md)

## Đối chiếu với P20

| Hạng mục | P20 | P20C |
|---|---|---|
| Bộ lọc | 7/30/90 ngày hoặc kỳ tự chọn, ngày lịch Việt Nam | Giữ kỳ, ngưỡng SKU và cách xử lý bộ lọc sai. Thêm ghi chú về ngày thu tiền và ngày đặt. |
| KPI | Sáu thẻ, tỷ lệ hủy là một thẻ riêng | Hàng đầu gồm doanh thu, số đơn, đơn chờ xử lý và SKU sắp hết. Hàng hai gồm khách mới, giá trị đơn trung bình, đơn đã giao và đơn bị hủy kèm tỷ lệ. |
| Doanh thu thực thu | Đơn ngoài `CANCELLED`/`EXPIRED` có lần `PAID` khớp tổng đơn, gán theo lần thu sớm nhất | Giữ tập đơn và kỳ thu của P20. Tách tiền hàng sau giảm giá và phí vận chuyển từ snapshot trên cùng tập đơn. |
| Số đơn, tỷ lệ hủy | Theo ngày tạo, loại `PENDING_PAYMENT` và `EXPIRED`. Tỷ lệ hủy so theo điểm phần trăm. | Hiện thêm tử số `CANCELLED` của tỷ lệ hiện có. |
| Biểu đồ ưu tiên | Doanh thu và số đơn theo ngày, trạng thái đơn, top 5 sản phẩm | Giữ hai biểu đồ xếp chồng theo ngày và thứ tự top 5. Bảng top 5 hiện thương hiệu và liên kết sản phẩm. |
| Phân tích thêm | Doanh thu dòng theo danh mục, số đơn đã thu theo phương thức và phân bố sao | Giữ ba biểu đồ, thêm doanh thu dòng theo thương hiệu và biểu đồ cột chồng trạng thái thanh toán của đơn tạo trong kỳ. Số sao tiếp tục lấy từ P23. |
| Danh sách nhanh | Đơn mới, SKU sắp hết và số đếm việc cần xem | Giữ các danh sách và số đếm. Thêm chi tiết trả hàng, cảnh báo MoMo và số lượt voucher còn dùng được. |
| Điều hướng | Link quản trị trong menu header | Sidebar có chín mục theo route có thật. Trang con kế thừa mục hiện tại, ví dụ chi tiết đơn thuộc **Đơn hàng**. Header Admin giữ logo bản nền sáng, nút đăng xuất có CSRF và link về cửa hàng. |

## Định nghĩa bổ sung

- **Đơn đã giao** đếm mốc `don_hang.ngay_giao_thanh_cong` trong kỳ hiện tại và kỳ trước. Server đổi 00:00 giờ Việt Nam sang UTC và dùng khoảng nửa mở. Một đơn đặt trước kỳ vẫn tính nếu giao trong kỳ.
- **SKU sắp hết** đếm mọi SKU và sản phẩm đang hoạt động có tồn khả dụng `trong kho − tạm giữ` không vượt ngưỡng đang chọn. Danh sách dùng cùng điều kiện, sắp theo tồn khả dụng rồi SKU và hiển thị tối đa 20 dòng. KPI là số hiện tại. Bấm thẻ đưa tiêu điểm tới danh sách.
- **Tách doanh thu** cộng `tam_tinh − so_tien_giam` và `phi_van_chuyen` trên từng đơn đã thu tiền. Chỉ hiện dòng phụ khi từng đơn có tiền giảm không vượt tạm tính và hai thành phần cộng lại khớp `tong_thanh_toan`. Nếu dữ liệu cũ lệch, tổng doanh thu P20 vẫn dùng `tong_thanh_toan` và dòng phụ được ẩn để tránh trình bày số tách sai.
- **Doanh thu theo thương hiệu** cộng `chi_tiet_don_hang.don_gia × so_luong` trong tập đơn đã thu tiền, trước voucher và phí giao hàng. Lấy tối đa 10 thương hiệu theo doanh thu giảm dần, dùng mã thương hiệu để phân thứ tự khi bằng doanh thu. Top 5 dùng thứ tự P20 và mở trang công khai của sản phẩm khi sản phẩm, danh mục và thương hiệu còn hiển thị. Với mục đã ngừng hiển thị, link mở trang sửa sản phẩm hiện có.
- **Trạng thái theo phương thức** đếm một lần mỗi đơn tạo trong kỳ. COD phân theo đã thu hợp lệ, chưa thu và hủy. MoMo phân theo thành công đã ghi nhận, đang chờ, và thất bại hoặc hết hạn. Lần thử mới nhất quyết định giữa hai nhóm sau khi đơn chưa có thanh toán thành công. Đơn `EXPIRED`/`CANCELLED` hoặc còn giữ hàng `HELD` quá hạn khi đơn đang `PENDING_PAYMENT` thuộc nhóm kết thúc. Khoản đã hoàn/chờ hoàn có xác nhận tiền và dấu vết kho `CONSUMED` được xem là từng thanh toán thành công cho biểu đồ này nếu đơn còn trong tập có thể hoàn tất. Số đơn và doanh thu đã thu theo P20 vẫn nằm ở biểu đồ phương thức riêng.
- **Cảnh báo MoMo** lấy từng lần thanh toán `MOMO`/`MOMO_MOCK` thất bại tạo trong kỳ, các lần đang chờ thuộc đơn `PENDING_PAYMENT` còn ít nhất một giữ hàng `HELD` với hạn không lớn hơn thời điểm hiện tại, và các lần của gateway đang bật thuộc đơn đủ điều kiện chạy đối soát. Điều kiện giữ hàng dùng chung với truy vấn của `MomoReconciliation_Job`. Con trỏ và giới hạn ba đơn chỉ phục vụ lịch chạy job. Cột thời điểm lấy ngày thanh toán nếu có, nếu không lấy ngày tạo lần thanh toán. Schema hiện tại chỉ có ngày tạo và ngày thanh toán nên không thể xác định thời điểm chuyển sang `FAILED` nếu nó khác ngày tạo.
- **Trả hàng và voucher** lấy tối đa năm yêu cầu `PENDING` mới nhất và rút gọn lý do còn tối đa 100 ký tự. Voucher dùng cửa sổ P20 `now < ket_thuc_luc ≤ now + 7 ngày`. Lượt dùng còn lại bằng giới hạn trừ lượt đã dùng và lượt đang giữ. Schema hiện tại lưu giới hạn hữu hạn cho mọi voucher, kể cả giới hạn bằng 0.
- **Duyệt và ẩn đánh giá** chỉ đưa các bộ lọc hợp lệ `status`, `stars`, `product` và `page` vào redirect. Trạng thái dùng enum P23, sao trong 1–5, mã sản phẩm là số nguyên dương và trang trong 0–10.000.

Các bảng biểu đồ mở khi JavaScript tắt. Chart.js đọc số từ `data-value`, có tên và mô tả cho canvas. Biểu đồ cột chồng dùng hai màu đã kiểm định ở P20 và một mẫu sọc, kèm chú giải, tooltip và bảng. [Kiểm định màu P20](../qa/P20/palette-validation.txt)

## Kiểm thử

Maven 3.9.15 và Microsoft JDK 21.0.13.0.2. Maven dùng cache hiện có qua `-Dmaven.repo.local`. Cache được bổ sung từ mirror Maven Central trước khi chạy offline.

| Lệnh | Kết quả | Bằng chứng |
|---|---|---|
| `mvn -o test` | 253 test, 0 lỗi, 48 bỏ qua | [Log](../qa/P20C/mvn-test.txt) |
| `P01_MARIADB_TEST=true mvn -o test` | 254 test, 0 lỗi, 10 bỏ qua. Riêng `ThongKe_MariaDb_Test` 2/2. | [Log](../qa/P20C/mvn-test-mariadb.txt) |
| `mvn -o -DskipTests package` | BUILD SUCCESS, JAR tại `target/beautyshop-0.0.1-SNAPSHOT.jar` | [Log](../qa/P20C/mvn-package.txt) |
| `node --check`, quét template và `git diff --check` | Cú pháp JS hợp lệ. Không có script nội tuyến, style nội tuyến, handler nội tuyến, `th:utext` hoặc CDN trong template sửa. | [Log](../qa/P20C/template-check.txt) |

MariaDB 12.2.2 chạy ở một tiến trình riêng chỉ nghe localhost. Database có 0 bảng trước Flyway và được khởi tạo đến V5 trong lần chạy. Tiến trình test đã dừng. Bộ tích hợp giữ `P23_MONGO_TEST` tắt. Các test bỏ qua là test MongoDB bật riêng và báo cáo demo. [Kiểm tra DB trống](../qa/P20C/mariadb-bootstrap.txt)

Fixture H2 kiểm biên giờ Việt Nam, SKU khả dụng, doanh thu thương hiệu loại đơn hủy, một lần đếm mỗi đơn MoMo có retry, cảnh báo ngay tại ngưỡng quá hạn, gateway đối soát và dữ liệu snapshot tiền không khớp. MockMvc kiểm thứ tự tám KPI, ghi chú kỳ, các bảng và link mới, mục hiện tại ở Dashboard, Đánh giá, Thương hiệu và chi tiết Đơn hàng, redirect duyệt/ẩn có tham số hợp lệ và Customer nhận 403.

## QA trình duyệt và ảnh

Harness P20 đã được cập nhật cho các điều hướng và biểu đồ P20C. Chrome Headless đạt **51/51**, 0 lỗi JavaScript, trên HTML do MockMvc render và dữ liệu repository giả lập. Tài nguyên CSS, Chart.js và JavaScript là mã của ứng dụng. Các phép đo kiểm 9 canvas trên 8 bảng, kỳ 7/30/90 ngày, sidebar và cuộn ngang ở 1366, 1920, 768 và 390 px, bảng khi tắt JavaScript và menu mobile bằng bàn phím. [Harness](../qa/P20C/admin-dashboard.qa.mjs) · [Log](../qa/P20C/browser-fixture.txt) · [Kết quả JSON](../qa/P20C/results-layout-fixture.json)

| Ảnh fixture | Phạm vi |
|---|---|
| [Dashboard 1366](../ui/screenshots/P20C/fixture-dashboard-1366.webp) · [Cụm biểu đồ ưu tiên](../ui/screenshots/P20C/fixture-primary-charts-1366.webp) | Màn hình đầu, bộ lọc, hai hàng KPI và cụm doanh thu/ngày, trạng thái đơn, top sản phẩm |
| [Phân tích 1366](../ui/screenshots/P20C/fixture-analysis-1366.webp) · [Cột chồng](../ui/screenshots/P20C/fixture-payment-status-1366.webp) | Doanh thu theo thương hiệu/danh mục và trạng thái theo phương thức |
| [Dashboard 390](../ui/screenshots/P20C/fixture-dashboard-mobile-390.webp) | Màn hình đầu trên mobile |
| [Sidebar desktop](../ui/screenshots/P20C/fixture-sidebar-desktop-1366.webp) · [Menu mobile](../ui/screenshots/P20C/fixture-sidebar-mobile-390.webp) | Hai cách mở điều hướng |
| [Trả hàng](../ui/screenshots/P20C/fixture-return-pending-title.webp) · [MoMo](../ui/screenshots/P20C/fixture-payment-alert-title.webp) · [Voucher](../ui/screenshots/P20C/fixture-voucher-expiring-title.webp) | Bảng dùng dữ liệu tổng hợp cho QA |

**Còn mở:** QA chế độ `admin` với phiên đăng nhập của owner và dữ liệu demo. Phiên chạy hiện tại thiếu cấu hình kết nối database demo. Trình duyệt fixture đã xác minh cách hiển thị và dữ liệu truyền từ bảng sang biểu đồ. Xác thực phiên thật, điều hướng giữa các trang quản trị trên phiên đó, và đối chiếu số liệu với dữ liệu demo vẫn chờ. Không có tài khoản Admin demo nào được tạo hoặc nâng quyền cho QA.

Khi có app demo chạy bằng JAR mới, chế độ `admin` mở Chrome có giao diện để owner tự đăng nhập. Harness không đọc hay ghi thông tin đăng nhập, chỉ chụp ảnh sau khi Dashboard đã tải.

```powershell
node docs/qa/P20C/admin-dashboard.qa.mjs admin 'C:/Program Files/Google/Chrome/Application/chrome.exe' 'http://127.0.0.1:8081' (Get-Location).Path
```

## File thay đổi

- Java và SQL đọc: `ThongKe_Service.java`, `ThongKe_Repository.java`, `ThanhToanMomo_Repository.java`, `AdminSauMua_Controller.java`.
- Giao diện: `admin/dashboard.html`, `admin/dashboard-kpi.html`, `admin/user/reviews.html`, `fragments/header.html`, `fragments/admin-nav.html`, `admin.css`, `dashboard.js`.
- Test: `ThongKeServiceTest.java`, `ThongKe_MariaDb_Test.java`, `AdminDashboardPagesTest.java`, `AdminDanhGiaPagesTest.java`.
- Bằng chứng trong `docs/qa/P20C/` và `docs/ui/screenshots/P20C/`. Trạng thái chờ owner xem.
