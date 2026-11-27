# UI-REDESIGN-01 · Kết quả triển khai BeautyShop

Ngày 27/11/2026. Bản kiểm tra là `main` tại `daea756053021310d9fd1c6ada9475564711fac6` cộng working tree BIZ-UX-01/02/03, rồi cộng phần redesign. Lúc bắt đầu có 21 tệp tracked thay đổi, index sạch và các hồ sơ QA chưa theo dõi. Bản dựng trước dùng JAR lưu từ BIZ-UX-03. Bản dựng sau lấy nguồn từ working tree hiện tại. Hai lượt chạy HTTP dùng cùng seed, profile dev và database mới tách biệt. [Revision, phiên bản công cụ và mã SHA-256 của hai JAR](../qa/UI-REDESIGN-01/environment.json)

Môi trường kiểm tra gồm Windows NT build 26300, Java Temurin 21.0.13, Spring Boot 3.5.16, Maven 3.9.11, MariaDB 12.2.2, MongoDB 6.0.8 và Node 26.13.0. Maven mặc định trên máy trỏ Java 17. Script đặt Java 21 cho tiến trình kiểm thử. Tài liệu tham chiếu có ảnh QA của các đợt cũ. Bốn ảnh thiết kế của yêu cầu này không xuất hiện trong workspace, nên triển khai theo đặc tả bằng văn bản.

## Thay đổi và bằng chứng trước/sau

| Hạng mục | Trước | Sau |
| --- | --- | --- |
| Checkout | GET có địa chỉ mặc định vẫn chưa xác nhận tổng. Phải gửi form bằng nút “Tính lại tổng tiền”. Dòng hàng thiếu ảnh. | GET có tổng từ server. Bố cục 60/40 trên desktop, một cột từ 880 px. Mỗi dòng có ảnh, tên, SKU, đơn giá, số lượng và thành tiền. Form riêng đặt đơn dùng địa chỉ và mã đã xác nhận. |
| Báo giá tương tác | Thay đổi địa chỉ/voucher chỉ vô hiệu hóa nút đặt đơn. | Đổi địa chỉ gửi báo giá ngay. Voucher có debounce 350 ms. Sau 8 giây hoặc lỗi mạng, tổng mang trạng thái chưa xác nhận và có thao tác thử lại. Hủy request cũ và so phiên bản trước khi áp dụng phản hồi. |
| Chi tiết đơn | Layout chung với Admin, thông tin thanh toán phẳng, thiếu ảnh và bước xác nhận hủy. | Trang dành cho Customer có mã, ngày đặt (giờ Việt Nam), tiến trình theo enum, địa chỉ giao, dòng hàng, tổng snapshot, trạng thái thanh toán và yêu cầu đổi trả thật. Hủy dùng disclosure để xác nhận ngay cả khi tắt JavaScript. Yêu cầu đổi trả chỉ mở khi còn hạn và còn số lượng đủ điều kiện. |
| Chat | Header, Home và footer điều hướng tới hai dạng tư vấn. GET `/tu-van-da` render biểu mẫu. | Header, Home và footer dẫn về `/chat`. Link cũ chuyển hướng 302, POST cũ có CSRF rồi chuyển 303. Chat hiển thị hội thoại của lượt hiện tại, gợi ý, trạng thái xử lý, trạng thái AI không khả dụng và nguồn xác minh khi có. |
| Header và Khám phá | Guest có hai liên kết đăng nhập, Customer có hai POST đăng xuất. Khám phá dùng summary để đóng. | Guest và Customer mỗi vai trò có một hành động phiên ở nhóm phải. Admin có lối vào quản trị và POST logout. Nút đóng Khám phá đứng đầu panel, căn trái, trả focus về summary. Escape tiếp tục dùng hành vi đóng chi tiết theo cấp đang mở. |
| Wishlist | Chỉ hiển thị tên sản phẩm và nút bỏ thích. | Thẻ dùng tên, ảnh, thương hiệu, danh mục, số biến thể, giá từ catalog và trạng thái còn hàng, hết hàng hoặc tạm ngừng bán. Truy vấn nhóm một lượt theo chủ tài khoản. Chi tiết sản phẩm giữ bước chọn SKU. |

Bằng chứng cho bảng lấy từ phản hồi HTML của hai ứng dụng chạy thật với cùng fixture. Trên fixture, checkout ban đầu báo 175.000 ₫. Sau khi nhập voucher hợp lệ, tổng là 165.000 ₫. Cả hai bản ghi đúng một đơn và một dòng đơn khi nhận hai POST đồng thời, tồn SKU đi từ 20 xuống 19 và voucher dùng đúng một lượt. [HTTP trước](../qa/UI-REDESIGN-01/before-http.json) · [HTTP sau](../qa/UI-REDESIGN-01/after-http.json) · [DB trước](../qa/UI-REDESIGN-01/before-cod-db.json) · [DB sau](../qa/UI-REDESIGN-01/after-cod-db.json)

## Hợp đồng và dữ liệu

- Checkout tiếp tục dùng `POST /checkout/quote` với CSRF cho HTML render phía server và giao diện tự động. Khi JavaScript tắt hoặc không tải, “Xem báo giá” gửi form này. Lúc render, báo giá hợp lệ hiển thị địa chỉ và mã dùng để đặt. POST đặt hàng kiểm tra lại giá, voucher, tồn và địa chỉ trong transaction hiện hành. Thông báo thử lại chỉ kích hoạt báo giá. Phí giao hàng tiếp tục tính theo tiền hàng sau giảm. Hai địa chỉ cùng đơn trong fixture vì thế có cùng phí.
- Voucher hết hạn, chưa đến kỳ, hết lượt và không đủ giá trị đơn được báo trong luồng xem trước. Tra cứu này không ghi nhận lượt hoặc giữ kho. SQL khóa và bộ đếm của lúc đặt đơn giữ cơ chế hiện hành.
- Ảnh trên chi tiết đơn là ảnh SKU hiện có, với fallback khi thiếu. Tên mặt hàng, biến thể, đơn giá, số lượng, địa chỉ và tổng lấy từ snapshot đơn. Thông tin đổi trả và thanh toán có trạng thái riêng. Tiến trình dùng ngày đặt ghi trong database và không hiển thị thời điểm các bước nếu nguồn chưa lưu.
- Chat dùng FAQ/RAG, giới hạn tần suất, giới hạn 500 ký tự và lọc dữ liệu của D35. Thymeleaf escape câu hỏi và câu trả lời. Liên kết dùng nguồn nội bộ được kiểm trong backend. Một lượt FAQ khi AI tắt hiển thị trạng thái thật và không tự sinh câu trả lời.
- Storefront tiếp tục dùng token charcoal, ivory, muted gold, serif và các lớp card, input, badge, alert, focus hiện có. Sidebar, template chi tiết đơn và điều khiển nghiệp vụ Admin giữ hệ hiện hành. Không có migration, framework frontend mới hoặc thay đổi cấu hình triển khai. [Quyết định D39](../DECISIONS.md)

## Kiểm thử

| Kiểm tra | Kết quả | Bằng chứng |
| --- | --- | --- |
| `mvn test` trong build cô lập | **PASS**. 317 khai báo, 241 chạy đạt, 76 **SKIPPED** theo điều kiện môi trường, 0 lỗi. | [Tổng hợp Maven](../qa/UI-REDESIGN-01/maven-test.txt) |
| `mvn -DskipTests package` | **PASS**. Spring Boot JAR dựng trong `target/ui-redesign-01/current`. | [Log package](../qa/UI-REDESIGN-01/maven-package.txt) |
| Integration MariaDB/MongoDB cô lập | **PASS**. 47/47. Bao gồm COD đồng thời, tồn/voucher, chủ đơn, snapshot, hủy và đổi trả, Wishlist, lỗi SKU, phân quyền, FAQ/RAG và MoMo mock. | [Danh sách suite và số lượng](../qa/UI-REDESIGN-01/maven-integration.txt) |
| HTTP ứng dụng thật trước/sau | **PASS**. Cùng seed, có Checkout, ảnh phục vụ HTTP, Wishlist chủ tài khoản, order, quyền và logout CSRF. | [Trước](../qa/UI-REDESIGN-01/before-http.json) · [Sau](../qa/UI-REDESIGN-01/after-http.json) |
| JavaScript checkout trong Node VM | **PASS**. 5/5 gồm tự báo giá, debounce, phản hồi đến sai thứ tự, lỗi field, timeout và không tự POST đặt đơn. | [Kết quả](../qa/UI-REDESIGN-01/checkout-vm.txt) · [Script](../qa/UI-REDESIGN-01/checkout.test.mjs) |
| `git diff --check` | **PASS**. | [Kết quả Git](../qa/UI-REDESIGN-01/git-check.txt) |
| Browser E2E và screenshot | **BLOCKED**. Browser tích hợp báo không có phiên. BIZ-UX-03 đã khoanh vùng renderer headless trên môi trường Windows này. | [Môi trường](../qa/UI-REDESIGN-01/environment.json) · [Chẩn đoán BIZ-UX-03](BIZ-UX-03-nghiem-thu-giao-dien.md) |

## Responsive và accessibility

| Độ rộng CSS hoặc zoom | Thiết kế | Kiểm trên browser thật |
| --- | --- | --- |
| 320, 360, 390 px | Header có menu thu gọn và tìm kiếm theo hàng. Checkout và Chat một cột. Dòng hàng chuyển giá xuống hàng riêng khi cần. | **BLOCKED** |
| 768 px | Menu thu gọn, checkout và Chat một cột, Wishlist có grid tự chia. | **BLOCKED** |
| 1024 px | Checkout và chi tiết đơn hai cột. Header còn menu thu gọn. | **BLOCKED** |
| 1280, 1440 và 1920 px | Header desktop và checkout hai cột với giới hạn chiều rộng. | **BLOCKED** |
| Zoom trình duyệt 200% | CSS đổi theo chiều rộng khả dụng. | **BLOCKED** |

Trong HTML sau sửa, sáu màn hình có skip-link đến đúng đích, một H1 và không trùng ID hay thiếu đích của label/ARIA đã quét. Kiểm tương phản tĩnh tính trên các cặp màu chữ và control CSS. Trạng thái focus thật, thứ tự Tab, Enter/Escape, clipping, nội dung dài, reduced motion, Swiper và cuộn đến footer cần chạy trên trình duyệt. Trạng thái của lần kiểm thủ công là **NOT RUN**. [HTML render](../qa/UI-REDESIGN-01/after-http.json) · [Tương phản token](../qa/UI-REDESIGN-01/contrast-tokens.json) · [Checklist thủ công](../qa/UI-REDESIGN-01/manual-visual-qa.md)

Ollama thật, MoMo Sandbox/thật, OAuth, SMTP và Redis thật chưa được kiểm trong task này. Integration RAG và thanh toán sử dụng provider/mạng mô phỏng theo suite. Chưa đo thời gian render, Core Web Vitals, tác động ảnh thương phẩm và chưa có ảnh trước/sau trực quan. Ưu tiên kiểm thủ công Checkout/COD, menu ở 200%, Home đến footer và Wishlist trên mobile trước khi release.

## Các tệp thay đổi trong task

| Nhóm | Đường dẫn |
| --- | --- |
| Controller | `src/main/java/com/beautyshop/controller/{Checkout,DonHang,TuVanDa}_Controller.java` |
| Repository | `src/main/java/com/beautyshop/repository/AnhBienTheHienThi.java`, `{DoiTra,DonHang,GioHang,PhieuGiamGia,YeuThich}_Repository.java` |
| Service | `src/main/java/com/beautyshop/service/{PhieuGiamGia,SauMua,TuVan}_Service.java` |
| Template | `src/main/resources/templates/{TuVanDa.html,auth/wishlist.html,fragments/commerce.html,fragments/footer.html,fragments/header.html,home/index.html,order/checkout.html,order/customer-detail.html}` |
| CSS và JS | `src/main/resources/static/css/storefront.css`, `src/main/resources/static/js/{app,chat,checkout,home-carousel}.js` |
| Test | `CheckoutPagesTest`, `GiaoDienPagesTest`, `TrangChuPagesTest`, `TuVanDaControllerTest`, `TuVan_ControllerTest`, `AdminDashboardPagesTest`, `DonHangCOD_MariaDb_Test`, `SauMua_MariaDb_Test` |
| Tài liệu và QA | `docs/DECISIONS.md`, hai báo cáo UI-REDESIGN-01 và `docs/qa/UI-REDESIGN-01/` |

Hướng dẫn dựng lại, khởi tạo fixture, chạy kiểm thử và chụp ảnh nằm trong checkpoint. Dữ liệu MariaDB/MongoDB thật của người dùng, datadir cũ, index và lịch sử Git được bảo toàn. [Checkpoint](UI-REDESIGN-01-tiep-tuc.md)
