# UI-REDESIGN-02 · BeautyShop theo bốn ảnh tham chiếu

HEAD `daea756053021310d9fd1c6ada9475564711fac6` trên `main`. Index sạch khi bắt đầu. Working tree có 40 tệp tracked thay đổi cùng các tệp chưa theo dõi của BIZ-UX và UI-REDESIGN-01. Bản trước dùng JAR UI-REDESIGN-01. Bản sau dựng từ working tree hiện tại. Fixture chạy trên Windows, JDK 21, Maven 3.9.11, MariaDB 12.2.2 và MongoDB 6.0.8 tách riêng. [Checkpoint](UI-REDESIGN-02-tiep-tuc.md)

## Đối chiếu bốn ảnh

Đã mở từng ảnh trong `docs/ui/references/` và xác nhận kích thước **1672 × 941 px**. Các kích thước quan sát dưới đây được làm tròn. Cột sau mô tả cấu trúc và CSS đã áp dụng. Chưa xác nhận sai số pixel bằng ảnh chụp trình duyệt. [Kích thước và SHA-256 ảnh gốc](../qa/UI-REDESIGN-02/reference-evidence.json)

| Ảnh | Bố cục và thứ bậc quan sát | Bản thực tế sau sửa | Khác biệt do dữ liệu và hành vi |
| --- | --- | --- | --- |
| [01 Checkout](../ui/references/01-checkout.png) | Container khoảng 1.380 px, card trái khoảng 823 px và card phải 535 px. Hero trái có eyebrow và H1 khoảng 48 px. Ba bước nằm bên phải. Ba card trái có icon đầu mục, control viền nhẹ. Summary có ảnh khoảng 125 px, viền gold, alert và nút đặt đơn rộng hết card. | CSS 1.380 px, 60/40, gap 22 px. Có địa chỉ lưu và bản xem thông tin, mã giảm giá song song với trợ giúp, COD kiểu radio-card. Ảnh và tên cạnh số lượng, đơn giá. Bảng tổng có hàng cuối dùng serif và gold, nút Đặt đơn hàng 56 px. | Sổ địa chỉ thật thay các ô hành chính trong mẫu. Số lượng dẫn về giỏ. Tổng hợp lệ hiện trạng thái đã xác nhận. Áp dụng là thao tác tùy chọn khi JavaScript chạy và là phương án gửi form khi tắt JS. |
| [02 Chi tiết đơn](../ui/references/02-order-detail.png) | Container khoảng 1.488 px. Dải trạng thái khoảng 144 px gồm mã, badge và timeline. Phần dưới khoảng 65/35. Tên và địa chỉ ở các ô ngang. Bảng dòng hàng có ảnh khoảng 106 px. Card thanh toán có tổng gold và khối phương thức riêng. | CSS 1.490 px, cột dưới 1,9/1, dải trạng thái ba vùng, nút sao chép và số sản phẩm thật. Timeline có icon, đường nối và bước hiện tại. Thông tin giao hàng ba cột, bảng dòng hàng có các cột riêng, payment card và xác nhận hủy. | Dùng toàn bộ bước thật từ state machine, không tạo giờ chuyển trạng thái. Đơn hủy/hết hạn hiện trạng thái riêng. Đổi trả giữ dữ liệu và điều kiện thực tế. Ảnh là ảnh SKU hiện hành. Không dựng nút liên hệ người hỗ trợ khi dự án chưa có đích đó. |
| [03 AI Chat](../ui/references/03-ai-chat.png) | H1 serif căn giữa. Hai card rộng khoảng 410 và 976 px, cao khoảng 665 px, gap khoảng 18 px. Sidebar có tuyên ngôn và icon lợi ích. Panel có avatar, bubble trái charcoal và bubble phải gold, chip cuộn xuống composer. | CSS 1.408 px, 30/70, panel desktop tối đa 665 px. Có sidebar, avatar, vùng hội thoại cuộn riêng, hai màu bubble, sáu chip và nút gửi icon. Trên màn nhỏ ưu tiên hội thoại. | Dùng minh họa editorial sẵn có. Chỉ hiển thị lượt trao đổi thật và nhãn nguồn thật. Khi AI lỗi/tắt, phục vụ FAQ theo nghiệp vụ. Không có upload ảnh, giờ giả, bài tư vấn mẫu hay trạng thái online không xác minh. |
| [04 Wishlist và Khám phá](../ui/references/04-wishlist-mega-menu.png) | Menu mở rộng khoảng 1.443 px, có cột đóng, ba nhóm liên kết và promo. Hero Wishlist khoảng 1.357 px với breadcrumb, H1 và gạch gold. Card xếp dọc khoảng 106–110 px, bốn vùng ảnh, tên, giá/tồn và CTA. Footer là dải ngắn. | CSS Wishlist 1.360 px, row-card bốn vùng, ảnh tối đa 145 px, màu tồn kho và đếm mục. Footer logo/link rút gọn. Menu mặc định đóng. Khi mở ở desktop trên Wishlist, phần đầu trang nằm dưới panel gồm cột trái đóng, ba nhóm thật và promo danh mục. | Không có xóa tất cả, số lượng thấp, social hoặc contact minh họa. SKU duy nhất có tồn dùng POST giỏ. Nhiều SKU dẫn đến điểm chọn biến thể trên PDP. Hết hàng vô hiệu hóa CTA. Promo dùng tài nguyên editorial hiện có. |

Header storefront có khung một hàng ở desktop, logo và bốn mục chính ở trái, tìm kiếm, wishlist, giỏ, tài khoản và hành động phiên ở phải. Header thu gọn theo chiều rộng và giữ nguyên POST logout với CSRF. Màu dùng charcoal, ivory và token gold. Trang mẫu dùng tài nguyên hiện có và ảnh SKU do backend cấp.

## Nghiệp vụ và lỗi phát hiện khi kiểm thử

- Checkout giữ `POST /checkout/quote`, báo giá từ server, debounce, loại phản hồi cũ, timeout và form đặt hàng độc lập. Ở fixture, cả hai bản trả 175.000 ₫ trước voucher và 165.000 ₫ sau voucher. Hai POST đồng thời ghi một đơn và một dòng. Stock từ 20 xuống 19 và voucher tăng đúng một lượt. [HTTP trước](../qa/UI-REDESIGN-02/before-http.json) · [HTTP sau](../qa/UI-REDESIGN-02/after-http.json) · [Đối chiếu DB](../qa/UI-REDESIGN-02/after-cod-db.json)
- HTML Chat dài hơn làm lộ lỗi CSRF lazy trên GET của guest không có session. Controller lấy token trước khi Thymeleaf render. Nguồn FAQ giữ nguyên nhãn dùng trong regression. POST cũ `/tu-van-da` vẫn chuyển hướng và bảo vệ CSRF. Escaping, bộ lọc nội dung và giới hạn câu hỏi giữ hợp đồng hiện có.
- Wishlist lấy ảnh, giá, tên danh mục và SKU duy nhất trong cùng truy vấn nhóm theo chủ tài khoản. POST thêm giỏ dùng endpoint sẵn có để kiểm lại SKU và tồn. Chi tiết đơn giữ kiểm quyền ở service, giá và giao hàng snapshot, lịch sử đổi trả thật.
- Header giữ native disclosure để dùng được khi tắt JS. Có JS thì đóng bằng Escape hoặc nút bên trái, đồng bộ `aria-expanded` và trả focus. JS giỏ và điều kiện Swiper của Home dùng mã hiện có.

## Kiểm thử và những mục chưa nghiệm thu

| Mục | Kết quả |
| --- | --- |
| Maven test | **PASS**. 317 khai báo, 241 đạt, 76 **SKIPPED** theo cấu hình, 0 lỗi. [Nhật ký](../qa/UI-REDESIGN-02/maven-test.txt) |
| Maven package | **PASS**. [Nhật ký](../qa/UI-REDESIGN-02/maven-package.txt) |
| Integration MariaDB/MongoDB | **PASS 47/47**. COD đồng thời, phân quyền, snapshot, voucher, lỗi SKU, đổi trả, FAQ/RAG và MoMo fixture. [Nhật ký](../qa/UI-REDESIGN-02/maven-integration.txt) |
| HTTP trước/sau | **PASS**. HTML từ hai ứng dụng thật với cùng seed, kiểm cấu trúc, vai trò, ảnh và thêm giỏ SKU duy nhất với CSRF. [Trước](../qa/UI-REDESIGN-02/before-http.json) · [Sau](../qa/UI-REDESIGN-02/after-http.json) |
| VM Checkout | **PASS 5/5**. Tự cập nhật theo địa chỉ, debounce voucher, loại phản hồi cũ, lỗi trường và timeout. [Kết quả](../qa/UI-REDESIGN-02/checkout-vm.txt) |
| Kiểm diff | **PASS**. [Kết quả](../qa/UI-REDESIGN-02/git-check.txt) |
| Ảnh chạy thật 1672 × 941 và so sánh cạnh nhau | **BLOCKED**. Browser tích hợp báo không có phiên. Không có ảnh chạy thật cho lượt này. |
| Responsive ở 320, 360, 390, 768, 1024, 1280, 1440, 1920 px và zoom 200% | **BLOCKED** trên browser. Layout khai báo collapse các cột nhỏ, timeline dọc, bảng sản phẩm xếp lại và menu thu gọn. Kiểm bằng máy có browser là **NOT RUN**. |
| Accessibility | Kiểm tĩnh cặp màu, DOM render và nhãn trong bằng chứng QA. Kiểm thứ tự Tab, Enter/Escape, focus nhìn thấy, reduced motion và không-JS bằng browser là **NOT RUN**. [Cặp màu](../qa/UI-REDESIGN-02/contrast-tokens.json) |
| Dịch vụ ngoài | Ollama thật, MoMo Sandbox, OAuth, SMTP và Redis thật **NOT RUN** trong lượt này. RAG và MoMo trong suite tích hợp dùng fixture. |

[Checklist trực quan 1672 × 941 và responsive](../qa/UI-REDESIGN-02/manual-visual-qa.md)

## Tệp thay đổi trong task

- Template `src/main/resources/templates/order/{checkout,customer-detail}.html`, `TuVanDa.html`, `auth/wishlist.html`, `product/detail.html` và `fragments/{commerce,footer,head,header}.html`.
- Assets `src/main/resources/static/css/reference-pages.css` và `src/main/resources/static/js/{app,chat,checkout,order-detail,wishlist}.js`.
- Backend `DonHang_Controller.java`, `TuVan_Controller.java`, `YeuThich_Repository.java`. Regression giao diện `GiaoDienPagesTest.java`.
- Hai báo cáo UI-REDESIGN-02 và bằng chứng dưới `docs/qa/UI-REDESIGN-02/`. Các ảnh tham chiếu gốc được dùng để đối chiếu trực tiếp.

Bản dựng và datadir của task nằm dưới `target/ui-redesign-02/`. HEAD, index và dữ liệu runtime của người dùng giữ trạng thái ban đầu. [Chạy lại và tiếp tục](UI-REDESIGN-02-tiep-tuc.md)
