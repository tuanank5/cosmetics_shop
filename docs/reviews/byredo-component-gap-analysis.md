# P15 · Ma trận 30 thành phần BeautyShop

**Hiện tại sau B4 · 24/30 PASS (80%), 2 PARTIAL, 4 FAIL, 0 N/A. Core 12/12 PASS trong phạm vi UI/MVC/fixture.** C27 đạt sau kiểm chứng F07/COD/MoMo theo cấu hình. F01/MariaDB và production còn cổng riêng. [Kiểm chứng B4](#b4). Các mục B0–B2 giữ lịch sử; B3 chưa triển khai.

**13/30 PASS (43,33%) · 13 PARTIAL · 4 FAIL · 0 N/A.** Trong 12 thành phần cốt lõi, 8 PASS và 4 PARTIAL. C01, C02, C15 và C27 là bốn điều kiện bắt buộc còn thiếu. Để đạt 80% với mẫu số hiện tại cần thêm ít nhất 11 PASS. Ngày chốt 22/11/2026, nhánh `main`, HEAD `837e550`, chấm trên working tree gồm cả thay đổi chưa commit.

PASS cần bằng chứng thực thi đúng ở lớp được nêu. Với component giao diện, MVC hoặc trình duyệt trên fixture có thể xác nhận hiển thị và tương tác. Nghiệm thu dữ liệu/giao dịch trên MariaDB là cổng riêng. PARTIAL bao gồm trường hợp thiếu bằng chứng cho một yêu cầu. FAIL là chức năng còn thiếu. C21 đang bị chặn về quyền sử dụng media, vẫn tính vào mẫu số vì chưa được owner duyệt N/A. Trước và sau B0 có cùng trạng thái ở từng hàng.

## Cách đọc bằng chứng

- **S** là mã nguồn và contract. **M** là MockMvc chạy lại bằng JDK 21. BS, GD và GH lần lượt chỉ các lớp kiểm trang công khai, giao diện và giỏ. [BootstrapPagesTest](../../src/test/java/com/beautyshop/controller/BootstrapPagesTest.java) · [GiaoDienPagesTest](../../src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java) · [GioHangPagesTest](../../src/test/java/com/beautyshop/controller/GioHangPagesTest.java)
- **B** là Chrome trong B0 trên Spring MVC/Thymeleaf ở loopback, repository dùng bộ nhớ. Tên ảnh trong bảng tương ứng tệp PNG trong bộ baseline. **H2** là hai SELECT thực của giỏ chạy với dữ liệu test trong bộ nhớ. [Ảnh B0](../ui/screenshots/byredo-inspired/B0/) · [Nhật ký và điều kiện kiểm](byredo-inspired-ui-implementation.md) · [GioHangRepositoryTest](../../src/test/java/com/beautyshop/repository/GioHangRepositoryTest.java)
- **U21/U22/U23** là bằng chứng lịch sử đã đối chiếu lại với source, không phải tất cả thao tác được chạy lại trong B0. **F07** đến từ audit frontend. **V5** là nội dung FAQ của dự án. [U2.1–U2.3](ui-upgrade-U2-2026-11.md) · [F07](frontend-audit-2026-11.md) · [Nguồn FAQ](../../src/main/resources/db/migration/V5__nap_faq_beautyshop.sql)

## Ma trận tại working tree sau B4

| ID | Thành phần | Trạng thái | File | URL | Bằng chứng | Thiếu sót | Test |
|---|---|---|---|---|---|---|---|
| C01 | Header tối giản | PASS | [header.html](../../src/main/resources/templates/fragments/header.html) | / | Chiều cao B1.2 khớp 90 số đo B1.1. | MariaDB vẫn có cổng riêng. | B1.1 + B1.2 fixture |
| C02 | Menu nhiều cấp | PASS | [header.html](../../src/main/resources/templates/fragments/header.html#L20) | /products#categories · /products#brands · /products#skin-type | Panel và bốn nhóm native, GET tới control thật, keyboard/touch/no-JS, role-aware. | Hiện dùng anchor fallback, chưa có dữ liệu demo MariaDB. | B1.2, 90 tổ hợp + kiểm đích |
| C03 | Tìm kiếm | PASS | [header.html L9](../../src/main/resources/templates/fragments/header.html#L9) · [CuaHang_Controller.java L25](../../src/main/java/com/beautyshop/controller/CuaHang_Controller.java#L25) | `/products?q=Hydrating` | Enter tại ô tìm kiếm từ header chuyển sang GET và trả 1 kết quả fixture. Có label và nút submit HTML. | Không có overlay cần xử lý Escape. Kết quả dữ liệu demo chờ F01. | B · tìm kiếm · M · GD |
| C04 | Lối vào tài khoản theo vai trò | PASS | [header.html L23](../../src/main/resources/templates/fragments/header.html#L23) · [login.html L16](../../src/main/resources/templates/auth/login.html#L16) · [Security_Config.java L35](../../src/main/java/com/beautyshop/security/Security_Config.java#L35) | `/login` · `/register` · `/account` · `/orders` · `/admin/catalog` | Guest có đăng nhập/đăng ký. Customer có hồ sơ và đơn. Admin dùng Enter mở danh mục quản trị và không thấy giỏ trong header. | Lỗi phân quyền của liên kết footer được ghi riêng ở C28. | B · login, chuyển vai, admin-1366 · M · BS |
| C05 | Lối vào wishlist | PASS | [header.html](../../src/main/resources/templates/fragments/header.html) | /account/wishlist | Icon Customer có accessible name, mở bằng keyboard/touch/no-JS và AX tree. Guest/Admin ẩn entry. | Lưu wishlist thật trên MariaDB chưa xác minh. | B1.3 source/MVC/Chrome |
| C06 | Chỉ báo giỏ | PASS | [header.html](../../src/main/resources/templates/fragments/header.html) · [GiaoDien_Advice](../../src/main/java/com/beautyshop/controller/GiaoDien_Advice.java) | /cart | Icon túi và badge server 0/1/5/12 đơn vị, giữ sau login trên fixture, no-JS, Admin ẩn. | Gộp nguyên tử MariaDB chưa xác minh. | B1.3 MVC/Chrome |
| C07 | Trạng thái điều hướng | PASS | [header.html](../../src/main/resources/templates/fragments/header.html) | /login · /register · /products · /account/wishlist · /admin/catalog/categories | Exact path và canonical đã kiểm. Namespace con dùng data-active-group. Focus 3 px. | Screen reader thực chưa chạy. | B1.3 MVC/Chrome |
| C08 | Header responsive và reflow | PASS | [storefront.css](../../src/main/resources/static/css/storefront.css#L76) | Home/Catalog/Login/Register/PDP/Cart/Account/Checkout/Wishlist/Admin/404 | 110/110 tổ hợp không tràn ngang. Header giữ 109 px mobile, desktop 113–116 px ở 100%. Panel/menu/action giữ kết quả B1.3. | PASS trên fixture. Thiết bị thật và screen reader chưa kiểm. | B1.4 Chrome, trước/sau + regression |
| C09 | Tiêu đề bộ sưu tập | PASS | [list.html L8](../../src/main/resources/templates/product/list.html#L8) · [storefront.css L301](../../src/main/resources/static/css/storefront.css#L301) | `/products` | Một h1, có mô tả ngắn, tách khỏi toolbar và vùng kết quả ở hai kích thước. | Có thể chỉnh câu chữ theo nội dung đã duyệt khi cần. | B · catalog-1366, catalog-390 |
| C10 | Số kết quả | PASS | [list.html L55](../../src/main/resources/templates/product/list.html#L55) · [CuaHang_Repository.java L38](../../src/main/java/com/beautyshop/repository/CuaHang_Repository.java#L38) | `/products` · `/products?q=Hydrating` | Page.totalElements. Fixture cho 3 → 1 khi tìm, MVC hiển thị tổng 27 ở trang hai. Query đếm DISTINCT sản phẩm. | Truy vấn đếm trên dataset MariaDB chưa chạy trong B0. | B · catalog · M · GD · S |
| C11 | Điều khiển bộ lọc | PASS | [list.html L14](../../src/main/resources/templates/product/list.html#L14) · [app.js L58](../../src/main/resources/static/js/app.js#L58) · [CuaHang_Repository.java L14](../../src/main/java/com/beautyshop/repository/CuaHang_Repository.java#L14) | `/products#categories` · `/products#brands` · `/products#skin-type` | Form GET danh mục, brand, loại da, sort. Mobile mở panel từ anchor, gửi loại da và nhận chip đúng. Các option đến từ server. | Fixture B0 không xác nhận tập sản phẩm lọc đúng trên MariaDB. U2.1 có thêm kiểm tra bàn phím và JS tắt. | B · mobile lọc · M · GD · U21 |
| C12 | Chip và số bộ lọc | PASS | [list.html](../../src/main/resources/templates/product/list.html#L16) | /products?brand=cerave&category=duong-am | SSR đếm brand/category/skinType từ state đã strip; q/sort/page/size không cộng. 8 tổ hợp MVC, chip giữ query và reset page, clear-all giữ hành vi cũ. Chrome keyboard/no-JS/history/form/touch trên 5 viewport × 2 zoom. | PASS UI/fixture; truy vấn MariaDB còn cổng riêng. | B2 · 160 lượt catalog +20 tương tác · MVC |
| C13 | Chuyển mật độ lưới | FAIL | [list.html L98](../../src/main/resources/templates/product/list.html#L98) · [storefront.css L340](../../src/main/resources/static/css/storefront.css#L340) | `/products` | Số cột đổi theo breakpoint. | Không có control chọn mật độ desktop và chưa có trạng thái truy cập cho lựa chọn. | S · B · catalog-1366 |
| C14 | Lưới sản phẩm | PASS | [list.html L98](../../src/main/resources/templates/product/list.html#L98) · [storefront.css L340](../../src/main/resources/static/css/storefront.css#L340) · [storefront.css L381](../../src/main/resources/static/css/storefront.css#L381) | `/products` | Desktop 3 cột, mobile 2 cột. Ba card tên dài/ngắn cùng hàng desktop có cùng đáy. Hai card hàng đầu mobile cũng cùng đáy. | F01 giới hạn kiểm với catalog thật. | B · cards-1366, catalog-390 · U21 |
| C15 | Ảnh sản phẩm trong catalog | PASS | [product-card.html](../../src/main/resources/templates/fragments/product-card.html#L4) · [app.js](../../src/main/resources/static/js/app.js#L35) | /products · /uploads/sku/{filename} | Chrome xác nhận URL NULL/rỗng/trắng, PNG OK, HTTP404 và decode-fail 200. JS lỗi ẩn img/naturalWidth=0 và hiện monogram; lỗi trước listener cũng xử lý. Khung4:5, contain, eager đầu/lazy sau; AX không lặp thông điệp. | No-JS: URL thiếu có SSR fallback; URL404/decode có thể còn icon native, link vẫn hoạt động như prompt cho phép. PNG OK là tài nguyên QA, chưa chứng minh ảnh thương phẩm MariaDB. | B2 · 20 lượt ảnh theo viewport/zoom/JS +cached-failure +MVC |
| C16 | Nhãn tồn kho | PASS | [product-card.html L13](../../src/main/resources/templates/fragments/product-card.html#L13) · [CuaHang_Repository.java L32](../../src/main/java/com/beautyshop/repository/CuaHang_Repository.java#L32) | `/products` | Ba card fixture lần lượt hiện còn/hết/còn. Truy vấn dùng tồn lớn nhất của các SKU phù hợp. | Không có nguồn dữ liệu cho nhãn giới hạn/chiến dịch. Giữ nhãn bán hàng bám theo dữ liệu. | B · cards-1366 · S |
| C17 | Wishlist / quick add trên card | FAIL | [product-card.html L4](../../src/main/resources/templates/fragments/product-card.html#L4) · [CuaHang_Repository.java L50](../../src/main/java/com/beautyshop/repository/CuaHang_Repository.java#L50) · [SauMua_Controller.java L105](../../src/main/java/com/beautyshop/controller/SauMua_Controller.java#L105) | `/products` · `/cart/items` · `/account/wishlist/{id}` | Cả card là một liên kết tới PDP. Mua và lưu trên PDP đã có contract riêng. | Card thiếu mã sản phẩm, mã SKU duy nhất mua được và thông tin số biến thể. Không suy SKU từ giá thấp nhất. Không lồng form trong link. | S · B · cards-1366 · M · GD (PDP) |
| C18 | Duyệt nhiều trang | PASS | [list.html](../../src/main/resources/templates/product/list.html#L58) · [Controller chỉ đọc](../../src/main/java/com/beautyshop/controller/CuaHang_Controller.java#L38) | /products?page=1&size=12 | Range từ Page: 0,1,12,27; 1–12/13–24/25–27, page99 đọc lại trang cuối theo controller cũ. MVC xử lý Page rỗng/tổng dương. Chrome Next/Previous, no-JS, reload, Back/Forward và query preservation. | Không đổi pagination SSR hoặc thêm AJAX. MariaDB thực chưa xác minh. | B2 · MVC +160 lượt catalog +20 tương tác |
| C19 | Hero biên tập | PASS | [index.html L7](../../src/main/resources/templates/home/index.html#L7) · [storefront.css L221](../../src/main/resources/static/css/storefront.css#L221) | `/` | Đồ họa CSS riêng, heading tiếng Việt, một CTA chính tới catalog và một lối phụ tới danh mục. | Giữ tài sản riêng khi cải tiến header. | B · home-1366, home-390 · M · BS |
| C20 | Nội dung xen trong catalog | FAIL | [list.html L98](../../src/main/resources/templates/product/list.html#L98) · [index.html L41](../../src/main/resources/templates/home/index.html#L41) | `/products` | Danh sách chỉ lặp sản phẩm. Có nội dung dịch vụ đã có ở home để cân nhắc tái dùng. | Chưa có khối biên tập xen danh sách. Cần giữ vị trí và tổng khi phân trang. | S · B · catalog-1366 |
| C21 | Video / media điện ảnh | FAIL | [index.html L17](../../src/main/resources/templates/home/index.html#L17) · [storefront.css L232](../../src/main/resources/static/css/storefront.css#L232) | `/` · `/products` | Có minh họa tĩnh. Không tìm thấy video được cấp quyền trong assets của dự án. | BLOCKED về tư liệu. Có thể trình owner duyệt N/A. Hiện vẫn tính vào mẫu số 30. | S · kiểm kê assets · B · home-1366 |
| C22 | Thẻ dịch vụ / tư vấn | PASS | [index.html L41](../../src/main/resources/templates/home/index.html#L41) · [TuVan_Controller.java L25](../../src/main/java/com/beautyshop/controller/TuVan_Controller.java#L25) | `/` · `/chat` | Các khối hỗ trợ, COD, đổi trả có nội dung cụ thể. Mở được trang tư vấn từ đích CTA trên fixture. | B0 chỉ mở biểu mẫu tư vấn. Chất lượng trả lời FAQ/RAG chưa kiểm ở đây. | B · GET /chat · U21 |
| C23 | PDP cao cấp | PASS | [detail.html L9](../../src/main/resources/templates/product/detail.html#L9) · [detail.html L61](../../src/main/resources/templates/product/detail.html#L61) · [storefront.css L115](../../src/main/resources/static/css/storefront.css#L115) | `/products/{slug}?variant=22` | Ảnh/placeholder, tên, brand, giá VND, tồn và nội dung có điều kiện. Hai cột desktop, một cột mobile. | Ảnh thương phẩm đã upload của người bán còn chờ demo có SKU công khai. | B · pdp-1366, pdp-390 · M · GD · U22 |
| C24 | Chọn biến thể | PASS | [detail.html L27](../../src/main/resources/templates/product/detail.html#L27) · [CuaHang_Service.java L49](../../src/main/java/com/beautyshop/service/CuaHang_Service.java#L49) | `/products/{slug}?variant={id}` | Nút GET liệt kê thuộc tính có sẵn, giá/tồn từng SKU. Tab không đổi trang. Enter chọn đúng khi tắt JS. | U2.2 đã kiểm SKU sai và hết hàng trong fixture. DB chạy thật còn mở. | M · GD · U22 · U23 |
| C25 | Thêm vào giỏ | PASS | [detail.html L44](../../src/main/resources/templates/product/detail.html#L44) · [GioHang_Controller.java L38](../../src/main/java/com/beautyshop/controller/GioHang_Controller.java#L38) · [app.js L101](../../src/main/resources/static/js/app.js#L101) | `POST /cart/items` | Guest thêm đúng SKU bằng Enter. U2.2 đã kiểm Customer, thành công/lỗi tồn, JS tắt và hai lần Enter chỉ gửi một POST có CSRF. | Chưa xác minh khóa tồn khi nhiều người mua trên MariaDB độc lập. | B · guest-cart-390 · M · GD · U22 |
| C26 | Giỏ hàng | PASS | [index.html L25](../../src/main/resources/templates/cart/index.html#L25) · [GioHang_Repository.java L34](../../src/main/java/com/beautyshop/repository/GioHang_Repository.java#L34) · [GioHang_Service.java L76](../../src/main/java/com/beautyshop/service/GioHang_Service.java#L76) | `/cart` · `POST /cart/items/{variant}/quantity` | Ảnh SKU/fallback, brand, link đúng variant, lượng, thành tiền, tạm tính, tồn và empty state. Guest → Customer ở B0 giữ 1 SKU giá 245.000 ₫. | PASS ở lớp UI/fixture. Ảnh thương phẩm thật, lưu giỏ bền và tính nguyên tử khi gộp chưa được MariaDB xác nhận. | B · empty-cart-390, guest-cart-390, checkout · M · GH · H2 · U23 |
| C27 | Checkout | PASS | [checkout.html](../../src/main/resources/templates/order/checkout.html) · [Checkout_Controller.java](../../src/main/java/com/beautyshop/controller/Checkout_Controller.java) · [CheckoutPagesTest](../../src/test/java/com/beautyshop/controller/CheckoutPagesTest.java) | GET /checkout · POST /checkout/quote · POST /checkout | F07 tái hiện trước/sau qua controller: lỗi giữ voucher nhưng bỏ toàn bộ khoản chưa xác nhận. 29 MVC +8 service tests; 320 bản ghi Chrome JS on/off/5 viewport/2 zoom; COD, MoMo OFF/ON/mock/pending, CSRF/submitter và duplicate Enter. | PASS UI/MVC/fixture; không chứng minh lưu đơn/khóa tồn/voucher nguyên tử MariaDB, merchant/IPN thật hoặc CSP production. | B4 · Chrome +MVC +service doubles · regression B2/B1/GSAP |
| C28 | Footer nhiều cột | PASS | [footer.html](../../src/main/resources/templates/fragments/footer.html#L5) | Các đích public/Customer/Admin hiện có | Server render đúng role. 23 link, 46 lượt GET + keyboard/no-JS trả 200. Touch 200% theo ba vai trò. Footer reflow không clip nội dung. | Kiểm DB thật vẫn thuộc cổng demo. | B1.4 MVC/Chrome/HTTP fixture |
| C29 | Thông tin hỗ trợ | PARTIAL | [footer.html L8](../../src/main/resources/templates/fragments/footer.html#L8) · [index.html L6](../../src/main/resources/templates/chat/index.html#L6) · [index.html L41](../../src/main/resources/templates/home/index.html#L41) | `/chat` · `/orders` · `/account/returns` | Có tư vấn, theo dõi đơn, đổi trả. V5 có nguồn FAQ về thời hạn và phí. Trang home đã có lời dẫn. | Chưa chứng minh người dùng đọc được đầy đủ FAQ phí/đổi trả với dữ liệu thực. Có thể liên kết nội dung đã duyệt từ vùng hỗ trợ. | B · GET /chat · S · V5 |
| C30 | Breadcrumb và trạng thái cuối | PARTIAL | [detail.html L7](../../src/main/resources/templates/product/detail.html#L7) · [list.html L89](../../src/main/resources/templates/product/list.html#L89) · [momo-return.html L6](../../src/main/resources/templates/order/momo-return.html#L6) · [404.html L6](../../src/main/resources/templates/error/404.html#L6) | `/products` · `/products/{slug}` · `/cart` · `/payment/momo/return` | Có lối về shop, giỏ trống, kết quả rỗng, SKU hết, thông điệp chờ xác minh MoMo và trang lỗi tiếng Việt. | Catalog thiếu breadcrumb. PDP có liên kết quay lại. Kết quả sau thanh toán chưa E2E ở B0. | B · no-match-390, empty-cart-390 · M · GD · U22 |

## Lộ trình ít thay đổi nhất để đạt ngưỡng

Các số dưới đây là mục tiêu nếu từng thay đổi vượt qua kiểm thử và 13 PASS hiện tại tiếp tục đạt. Tổng tối thiểu 24/30 vẫn tính cả C21.

| Đợt đề xuất | Thay đổi nhỏ | Điều kiện để ghi nhận thêm PASS | Tổng dự kiến |
|---|---|---|---:|
| B1 · header và footer | Dùng lại `header.html`, `footer.html`, `<details>` và token U1. Gộp danh mục, thương hiệu, loại da vào menu có nhóm. Trỏ đến các filter hiện hữu. Thêm lối vào wishlist và biểu tượng giỏ kèm nhãn. Thống nhất trạng thái menu mobile ở checkout, sửa active ở đăng ký và giới hạn link footer theo vai trò. | **C01, C02, C05, C06, C07, C08, C28**. Thử Tab/Shift+Tab, Enter, cảm ứng, no-JS. Kiểm Guest/Customer/Admin, badge sau POST và các đích Admin. Kiểm lại header ở 390×844 và zoom 200%. | 20/30 |
| B2 · công cụ catalog | Đếm tiêu chí từ model sẵn có, thêm tiến độ “đang hiển thị X–Y trong tổng Z” từ Page và thêm fallback bản địa cho card khi URL ảnh hỏng. Tận dụng mẫu giỏ, giữ khung 4:5 và ảnh lazy. | **C12, C15, C18**. Thử chọn nhiều tiêu chí, bỏ hết tiêu chí, duyệt trang đầu/cuối và tìm kiếm cho 0 kết quả. Kiểm ảnh có quyền, URL NULL, 404 và lỗi giải mã. Bảo toàn liên kết GET khi tắt JS. | 23/30 |
| B4 · checkout | Giữ hai POST hiện có. Nhóm địa chỉ–voucher–tóm tắt, tách CTA báo giá/đặt đơn, hiện trạng thái báo giá rõ sau lỗi. Dùng trạng thái lỗi trong model để yêu cầu báo giá lại nếu số tiền không còn xác nhận. | **C27**. Kiểm bỏ/sai voucher, lỗi sau báo giá, COD, MoMo tắt/bật hoặc mock có nhãn. Chạy Guest → SKU → giỏ → login → COD trên MariaDB test khi có quyền. | **24/30 = 80%, core 12/12** |

**Phần còn lại của P15.** C13 chỉ cần control `aria-pressed` và CSS trên lưới hiện có. C30 có thể bổ sung breadcrumb HTML ngay trong B1. C29 dùng lại FAQ và đường dẫn hỗ trợ đã có. C20 có thể dùng nội dung dịch vụ đã duyệt làm khối riêng, bỏ qua khi không có kết quả. B3 có thể giữ hero/thẻ dịch vụ, chỉ bổ sung nội dung khi đủ điều kiện. C21 chờ media có quyền hoặc owner xác nhận N/A. Những mục này vẫn phải được theo dõi kể cả khi đạt ngưỡng.

**Backend cần cân nhắc.** Các thay đổi tối thiểu trên dùng dữ liệu sẵn có. Nếu C02 cần từng nhãn category/brand trong header ở mọi trang, dùng lại `CuaHang_Service.danhMuc()/thuongHieu()` và giới hạn phạm vi đọc vào storefront. Bản đầu có thể điều hướng theo nhóm đến control thật. Với C17, `TheSanPham` chỉ có slug, tên, brand, giá từ, URL ảnh và tồn lớn nhất. Phương án ít thay đổi nhất hiện nay là đưa người dùng đến PDP để chọn SKU. Để làm quick add ngay trong card và lưu wishlist ở đó, cần đề xuất phép chiếu chỉ đọc cho ID sản phẩm, số biến thể và ID biến thể duy nhất nếu đủ điều kiện. Sử dụng POST hiện hữu và CSRF, kiểm lại tồn ở server. Một sản phẩm có nhiều SKU luôn đi qua chọn biến thể. Backend wishlist hiện chuyển hướng tới danh sách. Chỉ thông báo thành công khi có bằng chứng lưu. Thay đổi nghiệp vụ, DB hoặc security cần phê duyệt độc lập.

**Cổng demo.** F01 vẫn BLOCKED. B0 không có bộ dữ liệu MariaDB công khai để đối chiếu ảnh SKU và giá/tồn. Cần endpoint demo đọc được, SKU công khai có nguồn ảnh hợp lệ và DB test riêng trước khi xác nhận ghi đơn, gộp giỏ hoặc thanh toán thật. [Trạng thái nghiệm thu B0](byredo-inspired-ui-implementation.md)

## B1.1 · Compact header · 22/11/2026

Header mobile đóng mặc định trên toàn bộ trang. Desktop từ 1100 CSS px mở điều hướng thành một hàng. Ngưỡng này dựa theo khoảng trống cần cho các liên kết Admin. Bản HTML mặc định đóng và dùng native details/summary nên vẫn mở được khi tắt JavaScript. Nhãn chuyển từ “Menu cửa hàng” sang “Đóng menu”, Escape đóng menu và đưa focus về summary. Resize xuống mobile đưa focus về summary trước khi đóng nếu đang ở trong menu.

File sửa gồm header.html (bỏ topnote và trạng thái open, thêm nhãn), storefront.css (grid, vùng bấm, khung search và reflow hẹp), app.js (thay điều kiện riêng PDP/giỏ bằng một disclosure dùng chung). Các thay đổi mã khác đã có trong working tree được bảo toàn. Head, backend, security, footer, token và các template nội dung giữ nguyên trong lát cắt này.

### Chiều cao mặc định sau thay đổi

Các giá trị dưới đây tính bằng CSS px. Mỗi nhóm đo lại trên tất cả trang đã liệt kê, ở cả năm viewport và hai mức zoom. Tổng cộng 90 tổ hợp. [Dữ liệu từng lượt đo](../ui/screenshots/byredo-inspired/B1.1/measurements.json)

| Viewport vật lý | Guest 100% | Customer 100% | Admin 100% | Mọi vai trò 200% |
|---|---:|---:|---:|---:|
| 360×800 | 109 | 109 | 109 | 226 |
| 390×844 | 109 | 109 | 109 | 226 |
| 768×1024 | 109 | 109 | 109 | 109 |
| 1366×768 | 113 | 116 | 116 | 109 |
| 1920×1080 | 113 | 116 | 116 | 109 |

So với B0, Guest desktop giảm 240 → 113 px, Guest mobile giảm 296 → 109 px và Customer Checkout mobile giảm 494 → 109 px. Ở zoom 200% trên 360/390, viewport CSS chỉ còn khoảng 172/187 px. Header xếp logo, menu và search thành nhiều hàng để giữ vùng thao tác và chữ đọc được.

### Bằng chứng và giới hạn

Chrome 158, Windows, Spring MVC/Thymeleaf với repository fixture trong bộ nhớ, tắt DataSource/JPA/Flyway. Guest được kiểm Home, Catalog, PDP, Cart, Login và lỗi sản phẩm 404. Customer kiểm Checkout và Account. Admin kiểm Catalog. Cả 90 lượt đều kiểm Enter/Space mở/đóng, Tab/Shift+Tab qua menu, Escape, và mở/đóng với JavaScript disabled. Quyền trong header đúng vai trò, Admin không thấy liên kết giỏ hoặc tài khoản Customer. Không ghi nhận Runtime.exceptionThrown. Những lượt này dùng fixture, chưa nghiệm thu MariaDB.

[Ảnh trước B0](../ui/screenshots/byredo-inspired/B0/home-390.png) · [Ảnh sau và các trang/role](../ui/screenshots/byredo-inspired/B1.1/) · [Checkout trước B0](../ui/screenshots/byredo-inspired/B0/checkout-390.png)

Header vẫn nằm trong luồng tài liệu và menu mở đẩy nội dung xuống. Chưa chạy screen reader, thiết bị cảm ứng vật lý hoặc chứng nhận WCAG toàn trang. B0 đã ghi hai link footer Admin dẫn đến 403. Vấn đề này vẫn thuộc B1.4.

**C01 PASS trên fixture. C08 PARTIAL.** Header đạt mục tiêu chiều cao ở zoom 100% và thao tác native/keyboard. Khi zoom 200% ở 360/390, toàn trang vẫn tràn ngang đến 208 CSS px. Probe chỉ ra chữ BeautyShop ở footer rộng 192 px trong cột 141 px, ngoài phạm vi header. Header được reflow, nhưng tiêu chí toàn trang không cuộn ngang chưa đạt. C08 tiếp tục PARTIAL để phản ánh giới hạn này. Coverage hiện tại 14/30 = 46,67%, core 9/12. Các thành phần khác theo ma trận B0.

Maven 3.9.11 chạy Temurin 21.0.13, P01_MARIADB_TEST=false. mvn test đạt 43 PASS, 0 FAIL, 0 ERROR, 34 SKIPPED (MariaDB). mvn -DskipTests package thành công và node --check app.js đạt. F01 vẫn BLOCKED. Chưa xác nhận DB test riêng, ảnh thương phẩm hay giao dịch checkout thật.

Dừng ở B1.1. Các mục menu nhiều cấp, biểu tượng wishlist/giỏ và footer tiếp tục theo các lát cắt riêng.

<a id="b12"></a>

## B1.2 · Luxury Multi-level Navigation · 22/11/2026

**C02 PASS trên MVC/Chrome fixture. C01 giữ PASS. C08 giữ PARTIAL.** Coverage hiện tại **15/30 = 50%**, 10/12 core PASS. C15 và C27 còn PARTIAL. Các trạng thái B0/B1.1 trong phần lịch sử thể hiện thời điểm tương ứng.

### Thay đổi và cách chọn dữ liệu

Panel “Khám phá” có bốn disclosure con Danh mục, Thương hiệu, Loại da và Đồng hành. Ba nhóm mua sắm dẫn tới GET `/products#categories`, `/products#brands` và `/products#skin-type`. Đích là select/input đang có trên catalog. Nhóm Đồng hành dẫn về `/` và `/chat`. Giữ liên kết Sản phẩm tới `/products` và các entry tài khoản, đơn hàng, giỏ theo vai trò.

Đã chọn fallback anchor được prompt B1.2 cho phép vì F01 chưa được xác minh. Không liệt kê nhãn danh mục/thương hiệu tự tạo. Người dùng chọn giá trị công khai tại filter hiện có. Bước này không cần bổ sung view-model hoặc truy vấn backend.

Desktop giữ thanh điều hướng B1.1. Panel Khám phá và các nhóm con luôn đóng khi tải. Khi mở, panel có bốn cột, đường chia màu U1 và giới hạn chiều cao 65vh, cuộn bên trong. Mobile dùng disclosure xếp dọc, nằm trong luồng tài liệu. Các nhóm có thể mở độc lập. Native details/summary tiếp tục hoạt động khi tắt JS. JS chỉ tăng cường Escape đóng cấp đang mở và đưa focus về summary, cùng xử lý resize qua breakpoint 1100 px ở cả hai chiều.

### Inventory và diff trong B1.2

Đã lưu Git inventory và hash source trước lần sửa trong thư mục build `target/p15-b12/`. Working tree đầu kỳ gồm nhiều thay đổi U1–U2 và B1.1 chưa commit.

| File | Thay đổi B1.2 |
|---|---|
| [header.html](../../src/main/resources/templates/fragments/header.html#L20) | Thêm panel Khám phá, bốn nhóm con và năm đích GET. Panel chỉ hiển thị cho Guest/Customer. |
| [storefront.css](../../src/main/resources/static/css/storefront.css#L451) | Quy tắc panel, disclosure, mục tiêu bấm ít nhất 44×44 px, cuộn và breakpoint dùng token U1. |
| [app.js](../../src/main/resources/static/js/app.js#L7) | Escape xử lý cấp gần nhất đang mở. Resize phục hồi focus trước khi đóng các disclosure con. |
| [GiaoDienPagesTest.java](../../src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java#L63) | Ba lượt MVC mới kiểm liên kết bộ lọc, form search GET và điều kiện hiển thị theo role. |
| Hai báo cáo P15 | Cập nhật C02, coverage, bằng chứng và những lỗi tiếp tục mở. |
| [Bộ ảnh B1.2](../ui/screenshots/byredo-inspired/B1.2/) | Ảnh trước được sao nguyên từ B1.1, ảnh sau chụp mới. JSON lưu số đo từng lượt và các kiểm tra đích. |

Hash cuối lần chạy chỉ thay đổi bốn file source kể trên. Các controller, repository, security, DB/Flyway, token, footer và nội dung PDP/cart/checkout nằm ngoài diff B1.2.

### Số đo và visual regression

Chrome Headless 158.0.8127.36 trên Windows. Fixture Tomcat dùng MVC, Thymeleaf, Spring Security và service thật. Repository mô phỏng trong bộ nhớ, DataSource/JPA/Flyway bị loại. Phiên Customer/Admin chỉ tồn tại trong fixture. Ảnh sản phẩm và số tồn không được dùng để nghiệm thu dữ liệu demo. [Số đo 90 tổ hợp](../ui/screenshots/byredo-inspired/B1.2/measurements.json) · [Nhật ký kiểm tra đích](../ui/screenshots/byredo-inspired/B1.2/checks.json)

Tất cả 90 số đo chiều cao khớp B1.1 khi panel Khám phá đóng. Các số dưới đây là CSS px. Thanh điều hướng desktop vẫn hiện mặc định theo B1.1.

| Viewport | Guest 100% | Customer 100% | Admin 100% | Mọi vai trò 200% |
|---|---:|---:|---:|---:|
| 360×800 | 109 | 109 | 109 | 226 |
| 390×844 | 109 | 109 | 109 | 226 |
| 768×1024 | 109 | 109 | 109 | 109 |
| 1366×768 | 113 | 116 | 116 | 109 |
| 1920×1080 | 113 | 116 | 116 | 109 |

Khung panel khi mở luôn có cạnh trái ≥0 và cạnh phải ≤clientWidth trong 80 tổ hợp Guest/Customer. Summary nhóm đo ít nhất 44×44 px. Tại zoom 200% và viewport 360/390, header tăng số hàng để giữ chữ và control. Cả 14 lượt tràn ngang toàn tài liệu thuộc các viewport hẹp này và còn nguyên mức lỗi đã biết của B1.1. C08 tiếp tục PARTIAL.

| Bằng chứng 100% | Trước B1.1 | Sau B1.2 |
|---|---|---|
| Guest desktop 1366×768 | [Trước](../ui/screenshots/byredo-inspired/B1.2/before-guest-1366.png) | [Đóng](../ui/screenshots/byredo-inspired/B1.2/guest-1366-100-closed.png) · [Panel](../ui/screenshots/byredo-inspired/B1.2/guest-1366-100-panel.png) · [Nhóm mở](../ui/screenshots/byredo-inspired/B1.2/guest-1366-100-group.png) |
| Guest mobile 390×844 | [Trước](../ui/screenshots/byredo-inspired/B1.2/before-guest-390.png) | [Đóng](../ui/screenshots/byredo-inspired/B1.2/guest-390-100-closed.png) · [Panel](../ui/screenshots/byredo-inspired/B1.2/guest-390-100-panel.png) · [Nhóm mở](../ui/screenshots/byredo-inspired/B1.2/guest-390-100-group.png) |
| Checkout Customer mobile | [Trước](../ui/screenshots/byredo-inspired/B1.2/before-checkout-390.png) | [Đóng](../ui/screenshots/byredo-inspired/B1.2/customer-390-100-closed.png) · [Nhóm mở](../ui/screenshots/byredo-inspired/B1.2/customer-390-100-group.png) |
| Admin catalog desktop | [Trước](../ui/screenshots/byredo-inspired/B1.2/before-admin-1366.png) | [Sau](../ui/screenshots/byredo-inspired/B1.2/admin-1366-100-closed.png) |
| JavaScript disabled | | [Mở cả bốn nhóm](../ui/screenshots/byredo-inspired/B1.2/guest-1366-100-nojs-all-groups.png) |
| Zoom 200% | | [Guest 360×800, nhóm mở](../ui/screenshots/byredo-inspired/B1.2/guest-360-200-group.png) · [Customer 1920×1080](../ui/screenshots/byredo-inspired/B1.2/customer-1920-200-group.png) |

Các ảnh mang tên `role-width-zoom-state` ghi đúng độ rộng vật lý, zoom và trạng thái. Mỗi bộ Home Guest/Checkout Customer có cả năm viewport, hai mức zoom và ba trạng thái. Admin có mười ảnh để đối chiếu menu đúng quyền. Ảnh trước giữ nguyên dữ liệu và thời điểm B1.1, không phải lượt render lại bản cũ.

### Kiểm thử thực tế

- **90 tổ hợp.** Guest Home, Catalog, PDP, Cart, Login, 404; Customer Account và Checkout; Admin Catalog. Mỗi trang chạy năm viewport × hai zoom. Mở/đóng menu ngoài, mở/đóng panel, Space/Enter trên nhóm, Tab/Shift+Tab và Escape có assertion. Guest/Customer có thêm click chuột và touch event để mở/đóng panel ở mỗi tổ hợp. No-JS chạy lại trên toàn bộ 90 tổ hợp, mở bằng Enter qua các cấp để tới link con.
- **Đích GET.** Guest và Customer, desktop/mobile, JS bật/tắt: 24 lượt kích hoạt link con để mở đúng ba ID trên catalog và xác nhận filter panel mở. Tám lượt tới /chat xác nhận textarea câu hỏi. Kiểm bổ sung kích hoạt link về Home và header search GET với từ khóa Hydrating, nhận một kết quả fixture. Đây là kiểm điều hướng tới control, chưa phải kiểm query MariaDB theo giá trị danh mục/thương hiệu.
- **Focus và resize.** Escape từ summary nhóm đóng nhóm và trả focus tại summary. Escape tiếp đóng panel và focus summary Khám phá. Desktop → mobile khi đang ở submenu và mobile → desktop khi focus ở link con đều phục hồi về summary ngoài, đóng nhóm, giữ navigation sử dụng được.
- **Vai trò và giỏ.** Customer sau login, duyệt menu và thay viewport vẫn có một SKU, badge 1, tạm tính 245.000 ₫. Logout trở lại Guest đúng entry. Admin không có panel mua sắm, link /account hoặc giỏ trong header và vẫn có liên kết quản trị. Hai link Customer trong footer Admin là known issue B1.4 đã ghi từ B0.
- **JavaScript.** 0 Runtime.exceptionThrown trong lượt QA chính. Ảnh URL hỏng có thể tiếp tục trả 404 theo fixture và card catalog vẫn thuộc vấn đề C15. Không có thay đổi resource từ bên thứ ba.

### Maven và build

Maven 3.9.11 dùng Temurin JDK 21.0.13. `P01_MARIADB_TEST=false`.

| Phạm vi | PASS | FAIL | ERROR | SKIPPED |
|---|---:|---:|---:|---:|
| Suite trước B1.2 theo B1.1 | 43 | 0 | 0 | 34 |
| Suite sau B1.2, 80 test | **46** | **0** | **0** | **34** |

Đã chạy `mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test`, `mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package`, `node --check src/main/resources/static/js/app.js` và `git diff --check`. JAR tạo tại `target/beautyshop-0.0.1-SNAPSHOT.jar`. 34 SKIPPED là tích hợp MariaDB, chưa có DB test độc lập được xác nhận.

### Những việc còn mở

**KNOWN ISSUE B1.4** là footer tràn ngang ở zoom 200% và liên kết Customer hiện cho Admin. **F01 BLOCKED**, catalog demo chưa có bằng chứng mới trên MariaDB. Ảnh/giá/tồn và gộp giỏ nguyên tử trên MariaDB chưa được kiểm trong fixture. **C21** còn chờ quyền media hoặc quyết định N/A. Chưa kiểm screen reader, thiết bị cảm ứng vật lý, Lighthouse hoặc toàn bộ tiêu chí WCAG. Không có bằng chứng browser mới về Byredo trong B1.2.

B1.2 hoàn tất ở lớp UI/MVC/fixture với các đích fallback đã được prompt phê duyệt. Các giai đoạn B1.3/B1.4/B2/B3/B4 tiếp tục theo phạm vi riêng.

<a id="b13"></a>

## B1.3 · Header actions và trạng thái điều hướng · 22/11/2026

**C05, C06, C07 PASS trên source/MVC/Chrome fixture. C01, C02 giữ PASS.** Coverage hiện tại **18/30 = 60%**, core **10/12 PASS**. C08 tiếp tục PARTIAL vì footer gây tràn ngang ở zoom 200%. Các mục lịch sử B0–B1.2 ghi trạng thái ở thời điểm kiểm tương ứng.

### Quyết định và diff

- **Wishlist.** Chỉ Customer thấy action trái tim tới GET `/account/wishlist`, có nhãn “Sản phẩm yêu thích”. Guest tiếp tục dùng đăng nhập hiện có. Admin không có entry Customer. B1.3 bổ sung lối vào danh sách, không bổ sung thao tác lưu.
- **Giỏ.** SVG túi mua sắm tự vẽ, dùng stroke currentColor và badge gắn bên cạnh. Nhãn link đọc “Giỏ hàng · N sản phẩm”. N lấy trực tiếp từ `uiCartCount` của advice. Khi thiếu dữ liệu badge, link chỉ đọc “Giỏ hàng”. SVG trang trí có `aria-hidden=true` và `focusable=false`.
- **Trạng thái.** Link chỉ có `aria-current=page` khi khớp đúng path. Các namespace con dùng `data-active-group` và độ đậm chữ để định vị nhóm. Link Admin Catalog vẫn trỏ tới /admin/catalog, là canonical redirect đã kiểm tới /admin/catalog/categories. Trang danh mục brands/products và form con không được đánh current cho link categories. Các link Admin khác chỉ nhận current khi đúng đích danh sách.

| File sửa | Nội dung |
|---|---|
| [header.html](../../src/main/resources/templates/fragments/header.html#L45) | Action giỏ, wishlist Customer và phép so khớp path chính xác. |
| [storefront.css](../../src/main/resources/static/css/storefront.css#L435) | Icon 22 px, vùng bấm ≥44 px, badge không co và nền/viền chỉ trạng thái current cho action. Giữ focus U1. |
| [GiaoDienPagesTest.java](../../src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java#L64) | Thêm 9 lượt MVC kiểm role, badge 0/1/5/12 và current ở login/register/catalog/PDP. |
| Báo cáo này và ma trận | Cập nhật C05–C08, coverage và bằng chứng. |
| [Ảnh và JSON B1.3](../ui/screenshots/byredo-inspired/B1.3/) | 78 ảnh PNG, gồm 4 ảnh trước sao nguyên từ B1.2, 74 ảnh mới. Ba JSON lưu kích thước, current, tên action và đích đã kiểm. |

Inventory Git và hash của source đầu kỳ được lưu trong `target/p15-b13/`. So sánh cuối kỳ có đúng ba file source thay đổi. Các thay đổi chưa commit trước B1.3 được giữ. app.js, GiaoDien_Advice, controller, repository, Security_Config, dữ liệu, migration và nội dung trang ngoài header không thuộc diff B1.3.

### Trạng thái điều hướng đã kiểm

| URL | Link header nhận aria-current=page |
|---|---|
| / | / |
| /login | /login |
| /register | Không có |
| /products | /products |
| /products/demo-r026?variant=22 | Không có. Link Sản phẩm chỉ mang data-active-group. |
| /cart | /cart |
| /checkout | Không có |
| /account | /account |
| /account/wishlist | /account/wishlist. Tài khoản chỉ mang data-active-group. |
| /admin/catalog/categories | /admin/catalog, canonical đã xác minh |
| /admin/catalog/products | Không có. Quản trị chỉ mang data-active-group. |

### Chrome và regression C01/C02

Chrome Headless 158.0.8127.36, Windows, Tomcat MVC/Thymeleaf và Spring Security thực trên loopback. Repository fixture trong bộ nhớ, loại DataSource/JPA/Flyway. Tên/giá sản phẩm theo dữ liệu nghiên cứu hiện có, stock fixture được đặt đủ cho kịch bản 12 đơn vị. Các POST chỉ tới fixture. Chưa xác minh lưu wishlist, khóa tồn hoặc giao dịch trên MariaDB.

**100 tổ hợp** gồm Guest Home/Catalog/Login/Register/PDP/Cart (60), Customer Account/Wishlist/Checkout (30) và Admin Catalog (10). Mỗi trang có năm viewport × zoom 100%/200%. Các lượt kiểm trạng thái menu và current theo vai trò, rồi mở lại khi tắt JS. 90 lượt Guest/Customer có thêm cấp con, Enter/Space/Tab/Shift+Tab/Escape, click và touch mô phỏng. Panel mua sắm ẩn cho Admin. 80 tổ hợp trùng trang với B1.2 đều khớp đúng chiều cao cũ. Các trang mới có cùng chiều cao theo vai trò.

| Viewport vật lý | Guest 100% | Customer 100% | Admin 100% | Mọi vai trò 200% |
|---|---:|---:|---:|---:|
| 360×800 | 109 | 109 | 109 | 226 |
| 390×844 | 109 | 109 | 109 | 226 |
| 768×1024 | 109 | 109 | 109 | 109 |
| 1366×768 | 113 | 116 | 116 | 109 |
| 1920×1080 | 113 | 116 | 116 | 109 |

Khung header khi mở menu và panel nằm trong chiều rộng CSS khả dụng ở cả 90 lượt Guest/Customer. Từng icon có bounding box ít nhất 44×44 px và nằm trong viewport. Không che lỗi bằng overflow-x:hidden. Toàn trang còn tràn ngang trong 16 lượt tại 360/390 và zoom 200%, tập trung vào nội dung/footer đã ghi từ B1.1. C08 vẫn PARTIAL, nguyên nhân hiện tại được cập nhật ngay trong hàng ma trận. [Chi tiết từng lượt](../ui/screenshots/byredo-inspired/B1.3/measurements.json)

**C05.** Trên cả mobile/desktop, Tab từ giỏ sang wishlist, Shift+Tab quay lại giỏ, focus có outline 3 px. Enter mở được danh sách bằng controller thật. Touch mô phỏng vẫn mở wishlist khi tắt JS. Cây accessibility Chrome xác nhận hai tên link “Sản phẩm yêu thích” và “Giỏ hàng · 12 sản phẩm”. Guest truy cập thẳng route bảo vệ chuyển tới login. Admin truy cập thẳng nhận 403 và không có action trong header. [Kiểm tra action](../ui/screenshots/byredo-inspired/B1.3/action-checks.json)

**C06.** Guest bắt đầu 0, thêm một SKU thành 1, đổi lượng và thêm SKU thứ hai để tổng 5 đơn vị. Sau đó cập nhật SKU thứ nhất lên 9 và giữ SKU thứ hai 3, badge là 12. Tải giỏ khi tắt JS vẫn giữ đúng 12. Login qua form thật hợp nhất về fixture Customer, tiếp tục duyệt menu và wishlist giữ hai dòng, tổng 12 và tạm tính 4.425.000 ₫. Đây là minh chứng đếm đơn vị server, không xác nhận tính nguyên tử MariaDB. [Kiểm tra luồng giỏ và GET menu](../ui/screenshots/byredo-inspired/B1.3/checks.json)

**C02.** Đã kiểm lại 24 lượt tới ba bộ lọc theo Guest/Customer, desktop/mobile và JS bật/tắt. Có thêm các lượt tới tư vấn và resize desktop → mobile. Panel và nhóm con giữ hành vi native. Không có Runtime.exceptionThrown trong lượt chính. Các ảnh SKU 404 theo fixture tiếp tục là C15, chưa xử lý ở B1.3.

### Ảnh trước và sau

| Trạng thái | Trước B1.2 | Sau B1.3 |
|---|---|---|
| Guest desktop 1366×768 | [Trước](../ui/screenshots/byredo-inspired/B1.3/before-guest-1366.png) | [Sau](../ui/screenshots/byredo-inspired/B1.3/guest-1366-100-closed.png) |
| Guest mobile 390×844, nhóm mở | [Trước](../ui/screenshots/byredo-inspired/B1.3/before-guest-390-group.png) | [Sau](../ui/screenshots/byredo-inspired/B1.3/guest-390-100-group.png) |
| Customer desktop, header checkout | [Trước](../ui/screenshots/byredo-inspired/B1.3/before-customer-1366.png) | [Sau](../ui/screenshots/byredo-inspired/B1.3/customer-1366-100-closed.png) |
| Customer mobile, nhóm mở | [Trước](../ui/screenshots/byredo-inspired/B1.3/before-customer-390-group.png) | [Sau](../ui/screenshots/byredo-inspired/B1.3/customer-390-100-group.png) |
| Wishlist current | | [390×844](../ui/screenshots/byredo-inspired/B1.3/customer-wishlist-390.png) · [1366×768](../ui/screenshots/byredo-inspired/B1.3/customer-wishlist-1366.png) |
| Badge hai SKU | | [5 đơn vị](../ui/screenshots/byredo-inspired/B1.3/guest-two-sku-five-units.png) · [12 đơn vị](../ui/screenshots/byredo-inspired/B1.3/guest-two-sku-twelve-units.png) |
| Zoom 200% | | [Guest 360](../ui/screenshots/byredo-inspired/B1.3/guest-360-200-group.png) · [Customer 390](../ui/screenshots/byredo-inspired/B1.3/customer-390-200-group.png) · [Admin 1920](../ui/screenshots/byredo-inspired/B1.3/admin-1920-200-closed.png) |

### Maven JDK 21 và build

Maven 3.9.11 dùng Temurin 21.0.13. P01_MARIADB_TEST=false.

| Lượt | PASS | FAIL | ERROR | SKIPPED |
|---|---:|---:|---:|---:|
| Baseline B1.2 | 46 | 0 | 0 | 34 |
| Lượt đầu B1.3 | 46 | 0 | 9 | 34 |
| Suite cuối B1.3 (89 test) | **55** | **0** | **0** | **34** |

9 ERROR của lượt đầu do dùng bộ phân tích XML XPath cho HTML5 có thuộc tính defer. Đã sửa kiểm tra để đọc phần header HTML, không đổi markup hợp lệ của ứng dụng. Các kiểm tra DOM và cây accessibility thực được chạy trên Chrome.

Đã chạy Maven test, Maven -DskipTests package với repository cục bộ `target/maven-repository`, node --check app.js và git diff --check. JAR ở `target/beautyshop-0.0.1-SNAPSHOT.jar`. 34 SKIPPED thuộc MariaDB integration vì chưa xác nhận database test riêng. Các test cũ về SKU, quyền, CSRF và tiền tiếp tục đạt.

### Những mục còn mở

- **C08 PARTIAL, B1.4** còn lỗi footer tràn ngang tại 360/390 khi zoom 200%. Header đã gọn từ B1.1 và không phải các giá trị 296/494 px của B0.
- **C28 PARTIAL, B1.4** còn hai liên kết Customer trong footer Admin dẫn 403.
- **F01 BLOCKED**, dữ liệu catalog/ảnh thật và giỏ bền vững trên MariaDB chưa có bằng chứng mới. C15, C27 và C21 giữ trạng thái trước đó.
- Cây accessibility xác nhận tên link, chưa có lượt dùng screen reader hoặc thiết bị cảm ứng vật lý. Không tuyên bố WCAG toàn trang hay điểm Lighthouse.

B1.3 dừng sau header actions và navigation state. Chưa commit/push.

<a id="b14"></a>

## B1.4 · Luxury Footer, quyền và reflow · 22/11/2026

**C08 và C28 PASS trên source/MVC/Chrome fixture.** C01/C02/C05/C06/C07 tiếp tục đạt. Coverage hiện tại **20/30 = 66,67%**, core **10/12**. C15 và C27 là hai core còn PARTIAL. Các mục B0–B1.3 bên trên giữ nguyên lịch sử, gồm lượt test B1.3 có 9 ERROR đã được sửa.

### Nguyên nhân và phép đo trước/sau

Đã chụp và đo trước khi sửa, trên cùng fixture và dữ liệu với lượt sau. Bộ kiểm footer có **110 tổ hợp**, gồm bộ 100 của B1.3 và thêm Guest 404 tại năm viewport × hai zoom. Trước sửa có 18 lượt toàn trang tràn ngang. Hai lượt tăng so với B1.3 thuộc trang 404 mới được thêm. Sau sửa, **110/110 có scrollWidth == clientWidth**.

Ở các trang tràn, footer-brand có font 36 px và chuỗi BeautyShop không có điểm ngắt. Cột rộng 141 hoặc 156 px nhưng phần chữ có scrollWidth 192 px, đẩy scrollWidth của footer/tài liệu thành 208 px. Grid desktop trước đó dùng track có min-size tự động. B1.4 dùng minmax(0, …), min-width:0 và overflow-wrap:anywhere trong các cột footer. Thêm điểm ngắt tự nhiên Beauty<wbr>Shop. Font, màu và các token U1 giữ nguyên.

| Nhóm đo tại zoom 200% | clientWidth | Brand rect left/right | Brand clientWidth | Brand scroll trước → sau | Trang scroll trước → sau |
|---|---:|---|---:|---:|---:|
| Guest Home/Catalog/Login/Register/404, 360×800 | 172 | 16 / 156,5 | 141 | 192 → 141 | 208 → 172 |
| Customer Account/Checkout/Wishlist, 360×800 | 172 | 16 / 156,5 | 141 | 192 → 141 | 208 → 172 |
| Admin Catalog, 360×800 | 172 | 16 / 156,5 | 141 | 192 → 141 | 208 → 172 |
| Cùng các nhóm tại 390×844 | 187 | 16 / 171,5 | 156 | 192 → 156 | 208 → 187 |

Rect là hộp bố cục. Phần chữ trước sửa tràn ra ngoài hộp, được thể hiện bằng scrollWidth. Guest PDP/Cart vốn dùng footer-brand 24 px theo CSS U2 nên không nằm trong 18 lượt lỗi. Quy tắc đó được giữ nguyên.

Probe cũng ghi nhận vài phần tử khác có nội dung rộng hơn ô chứa, như button-group ở hero, form lọc và login tại viewport CSS hẹp. Chúng vẫn nằm trong mép trang. Label visually-hidden là phần tử được ẩn cho mục đích accessibility. Sau sửa footer, không thành phần nào trong bộ trang kiểm làm toàn tài liệu tràn ngang. B1.4 không thêm clip hoặc overflow:hidden để che lỗi. [JSON trước](../ui/screenshots/byredo-inspired/B1.4/before-measurements.json) · [JSON sau](../ui/screenshots/byredo-inspired/B1.4/after-measurements.json)

### Inventory và file sửa

Đầu kỳ đã lưu Git inventory, diff stat và hash source trong target/p15-b14. Hash cuối chỉ đổi ba file source dưới đây. Header, app.js, tokens, Security_Config, controller, service, repository, entity, pom.xml và Flyway giữ nguyên trong B1.4.

| File | Thay đổi |
|---|---|
| [footer.html](../../src/main/resources/templates/fragments/footer.html#L5) | Bốn cột có tên navigation phân biệt. Cột tài khoản render riêng Guest/Customer; Admin có cột quản trị. Wordmark có wbr để ngắt tại Beauty/Shop. |
| [storefront.css](../../src/main/resources/static/css/storefront.css#L76) | Chỉ sửa quy tắc grid/cột/brand footer. Track có min-size 0, cột cho phép ngắt chữ. |
| [GiaoDienPagesTest.java](../../src/test/java/com/beautyshop/controller/GiaoDienPagesTest.java#L64) | Ba test MVC đọc đúng node footer, kiểm các link phải hiện và phải ẩn theo role. |
| Hai báo cáo P15 và [bằng chứng B1.4](../ui/screenshots/byredo-inspired/B1.4/) | Cập nhật trạng thái hiện tại và lưu 96 PNG cùng JSON. Có 24 ảnh trước/sau footer và 72 ảnh regression header/menu/giỏ. |

### Liên kết theo vai trò

Tất cả link hiển thị đều được lấy từ đúng node footer, gửi GET với session của vai trò tương ứng và kích hoạt bằng Enter. Mỗi link được kiểm cả JS bật và tắt. 23 link theo ba vai trò tạo **46 lượt GET + keyboard/no-JS**, toàn bộ trả HTTP 200.

| Vai trò | Các link footer hiển thị | HTTP / hành vi |
|---|---|---|
| Guest | /products, /products#categories, /products#brands, /login, /register, /cart, /chat | Cả 7 đích trả 200. Link tài khoản ghi rõ Đăng nhập/Tạo tài khoản. Không có link Customer hoặc Admin. |
| Customer | /products, /products#categories, /products#brands, /account, /orders, /account/wishlist, /cart, /chat, /account/returns | Cả 9 đích trả 200. Hồ sơ/đơn/wishlist/đổi trả truy cập đúng session. |
| Admin | /products, /products#categories, /products#brands, /admin/catalog/categories, /admin/catalog/brands, /admin/catalog/products, /chat | Cả 7 đích trả 200. Không có /account, /orders, /cart hoặc wishlist trong DOM footer. |

Hai đích /account và /orders của footer Admin cũ vẫn trả 403 khi kiểm trực tiếp với phiên Admin. Việc sửa nằm ở điều kiện render server, không nới quyền backend. [Kết quả từng link](../ui/screenshots/byredo-inspired/B1.4/footer-links.json)

Tab/Shift+Tab theo thứ tự nhìn và Enter mở đúng URL. Focus footer giữ outline 3 px màu nền ngà. Các nav có accessible name phân biệt, link có tên hiển thị. Touch mô phỏng ở 390×844, zoom 200% và JS tắt mở đúng /products#brands cho cả ba role; vùng bấm rộng và cao ít nhất 44 CSS px. Không khóa cuộn body. [Kiểm tra touch](../ui/screenshots/byredo-inspired/B1.4/footer-touch.json)

### Bằng chứng trước/sau cùng viewport và vai trò

Các ảnh dưới đây chụp mới trước và sau khi chỉnh footer trên cùng fixture. Role, zoom, viewport và dữ liệu sản phẩm/giỏ giống nhau. Tại desktop, footer ngắn hơn viewport nên một phần nội dung trước footer xuất hiện. Tại mobile/zoom 200%, ảnh là vùng đầu footer và phần còn lại truy cập bằng cuộn. Các link cuối footer đã được kiểm thực tế bằng bàn phím.

| Vai trò / viewport / zoom | Trước | Sau |
|---|---|---|
| Guest 1366×768 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-guest-1366-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-guest-1366-100.png) |
| Guest 390×844 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-guest-390-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-guest-390-100.png) |
| Guest 360×800 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-guest-360-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-guest-360-200.png) |
| Guest 390×844 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-guest-390-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-guest-390-200.png) |
| Customer 1366×768 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-customer-1366-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-customer-1366-100.png) |
| Customer 390×844 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-customer-390-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-customer-390-100.png) |
| Customer 360×800 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-customer-360-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-customer-360-200.png) |
| Customer 390×844 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-customer-390-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-customer-390-200.png) |
| Admin 1366×768 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-admin-1366-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-admin-1366-100.png) |
| Admin 390×844 · 100% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-admin-390-100.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-admin-390-100.png) |
| Admin 360×800 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-admin-360-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-admin-360-200.png) |
| Admin 390×844 · 200% | [Trước](../ui/screenshots/byredo-inspired/B1.4/before-admin-390-200.png) | [Sau](../ui/screenshots/byredo-inspired/B1.4/after-admin-390-200.png) |

### Regression và chiều cao header

Đã chạy lại bộ **100 tổ hợp B1.3** sau sửa footer với menu đóng/mở, nhóm con, Guest/Customer/Admin, action giỏ/wishlist, active state, Enter/Space/Escape, Tab/Shift+Tab, click/touch, resize và no-JS. Cả 100 tài liệu đều không tràn ngang. Cả 110 số đo header trong cặp trước/sau footer đều khớp nhau.

| Viewport | Guest 100% | Customer 100% | Admin 100% | Mọi role 200% |
|---|---:|---:|---:|---:|
| 360×800 | 109 | 109 | 109 | 226 |
| 390×844 | 109 | 109 | 109 | 226 |
| 768×1024 | 109 | 109 | 109 | 109 |
| 1366×768 | 113 | 116 | 116 | 109 |
| 1920×1080 | 113 | 116 | 116 | 109 |

Menu Khám phá vẫn mở tới đúng categories/brands/skin-type. Badge server được kiểm 0 → 1 → 5 → 12 đơn vị. Sau login và duyệt menu, Customer giữ hai SKU, tổng 12 đơn vị và tạm tính 4.425.000 ₫. 0 Runtime.exceptionThrown trong lượt regression. [Số đo regression](../ui/screenshots/byredo-inspired/B1.4/measurements.json) · [Kiểm tra luồng](../ui/screenshots/byredo-inspired/B1.4/checks.json)

### Maven, JAR và giới hạn kiểm chứng

Maven 3.9.11, Temurin JDK 21.0.13. Lượt cuối ghi Tests run 92, Failures 0, Errors 0, Skipped 34, tức **58 PASS / 0 FAIL / 0 ERROR / 34 SKIPPED**. Ba test thêm kiểm footer Guest/Customer/Admin. Baseline B1.3 là 55 PASS và 34 SKIPPED.

Đã chạy các lệnh sau với P01_MARIADB_TEST=false.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.13.9-hotspot'
$env:Path = "$env:JAVA_HOME/bin;$env:Path"
$env:P01_MARIADB_TEST = 'false'
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' test
mvn.cmd -o -B '-Dmaven.repo.local=target/maven-repository' -DskipTests package
git diff --check
```

JAR tạo thành công tại target/beautyshop-0.0.1-SNAPSHOT.jar. App.js không thay đổi. Chrome Headless 158.0.8127.36 trên Windows dùng zoom thật của browser qua settingsPrivate.setDefaultZoom(1/2), viewport qua DevTools; không dùng transform hoặc CSS zoom.

Các kết quả UI/HTTP/browser dùng Spring MVC, Thymeleaf và Spring Security thật trên Tomcat loopback với repository trong bộ nhớ. Fixture loại DataSource/JPA/Flyway. Tăng số trang kiểm bằng controller Đơn hàng thật trong fixture để xác minh /orders, không thêm controller hoặc route vào ứng dụng. Không gửi POST nghiệp vụ tới database thật.

**F01 vẫn BLOCKED.** 34 MariaDB integration test tiếp tục SKIPPED vì chưa có database test riêng được xác nhận. C15 ảnh 404, C27 báo giá voucher và C21 quyền media tiếp tục mở. Screen reader, cảm ứng trên thiết bị vật lý và đánh giá WCAG 2.2 AA toàn trang là NOT VERIFIED. Không nâng C29/C30.

C08/C28 đạt trong phạm vi bộ trang, vai trò và viewport đã kiểm trên fixture. B1.4 hoàn thành và dừng tại đây. B2 tiếp tục cần xử lý các khoảng trống catalog theo ma trận sau khi được yêu cầu.

<a id="b2"></a>

## B2 · Luxury Catalog · 23/11/2026

**C15, C12, C18: PARTIAL → PASS** theo đúng thứ tự triển khai, có MVC/Chrome assertion độc lập. Tổng **23 PASS +3 PARTIAL +4 FAIL =30**, coverage **76,67%**; core **11/12** (C27 còn PARTIAL). Chưa đạt80%, không triển khai B3/B4 để cộng thêm điểm.

| Component | Bằng chứng quyết định | Giới hạn giữ lại |
|---|---|---|
| C15 | Sáu nhánh URL tại20 tổ hợp viewport/zoom/JS: NULL/rỗng/trắng, PNG200/naturalWidth1280,404/naturalWidth0 và decode200/naturalWidth0. JS ẩn ảnh hỏng/hiện monogram; cached failure trước listener cũng đạt. Ratio4:5/contain/eager đầu/lazy sau, tên AX không lặp. | No-JS URL thiếu vẫn SSR fallback; URL404/decode có thể còn icon native, link PDP vẫn dùng được. PNG tốt là tài nguyên QA có nguồn, không phải ảnh thương phẩm DB. |
| C12 | Tám tổ hợp MVC từ state GET chuẩn hóa; q/sort/page/size không cộng. Xóa5 loại chip giữ trường còn lại và reset page; clear-all vẫn/products. Chrome count nhìn được khi panel đóng, Space/Enter/touch/Áp dụng/refresh/Back/Forward/no-JS. | Chỉ UI/fixture, không xác nhận query lọc MariaDB. “Tất cả” mặc định value rỗng; không tự bỏ slug lạ đang thật sự được backend áp dụng. |
| C18 | Page0/1/12/27 và27 phần tử chia1–12,13–24,25–27; page99 dùng hành vi đọc lại cuối của controller cũ. MVC thêm empty page/tổng dương. Browser Next/Previous, không link vượt biên, giữ q/filter/sort/size và history/no-JS. | Không thêm API/AJAX hoặc thay luồng pagination. Dataset MariaDB còn F01. |

Source diff chỉ `product/list.html`, `fragments/product-card.html`, catalog CSS trong `storefront.css`, error handler catalog trong `app.js`, `GiaoDienPagesTest.java`. Không sửa controller/service/repository, backend nghiệp vụ, Security, Flyway, pom.xml, checkout hoặc GSAP C19. Fragment productCard nhận thêm cờ ưu tiên ảnh, nơi gọi catalog duy nhất được cập nhật cùng lúc.

**Maven JDK21.0.13 cuối: 115 =81 PASS +0 FAIL +0 ERROR +34 SKIPPED.** Package JAR, node --check và diff check đạt. Sau từng phần MVC đạt34→42→50; baseline sau GSAP là61 PASS/34 SKIPPED, không dùng mốc58 cũ làm kết quả mới. 34 SKIPPED là MariaDB chưa có DB test riêng. [Maven summary](../ui/screenshots/byredo-inspired/B2/maven-summary.json) · [Log](../ui/screenshots/byredo-inspired/B2/tests.log)

Chrome158.0.8127.36/Windows, zoom browser thật: **160/160 lượt catalog**, **20/20 tổ hợp tương tác**, **100/100 tổ hợp regression Guest/Customer/Admin**,0 Runtime.exceptionThrown hoặc tràn ngang. Có178 PNG và JSON/log, không dùng screenshot fixture để khẳng định MariaDB. Bản trước chụp từ snapshot template/assets trên cùng dữ liệu test. C01/C02/C05/C06/C07/C08/C28 và U2 tiếp tục đạt trong phạm vi regression.

GSAP vendor/adapter/hash và animation Home giữ nguyên; Catalog chỉ tải app.js. Owner đã nghiệm thu kỹ thuật C19 có điều kiện trước B2. **F01 BLOCKED, F14 pipeline ảnh PARTIAL; C13/C17/C20/C21 vẫn FAIL, C27/C29/C30 vẫn PARTIAL.** Screen reader, thiết bị thật, CLS/LCP/INP, Lighthouse, CSP production và MariaDB chưa được nghiệm thu mới trong B2.

[Báo cáo B2 chi tiết, công thức/contract, lỗi QA đã sửa và ảnh trước/sau](byredo-inspired-ui-implementation.md#b2) · [Bằng chứng B2](../ui/screenshots/byredo-inspired/B2/) · [Checks HTTP/GET](../ui/screenshots/byredo-inspired/B2/checks.json) · [Tương tác/AX](../ui/screenshots/byredo-inspired/B2/interaction-checks.json) · [Regression](../ui/screenshots/byredo-inspired/B2/regression/checks.json).

**Dừng sau B2.** Không commit/push, không sửa F01 bằng seed hoặc mở rộng checkout/B3/B4.

<a id="b4"></a>

## B4 · Luxury Checkout / C27 / F07 · 23/11/2026

**C27 PARTIAL → PASS ở UI/MVC/fixture**, tổng **24/30 =80%, core12/12**. Chỉ nâng sau khi có before/after F07 qua controller,29 MVC +8 service cases và Chrome đầy đủ. F01/MariaDB/gateway/IPN/production là các cổng riêng chưa được nghiệm thu.

Trước B4, SAVE50 vẫn ở input sau lỗi COD nhưng giảm50.000→0, tổng465.000→515.000. Sau B4, GET/lỗi trả baoGiaHopLe=false và không render giảm/phí/tổng chưa xác nhận. Quote thành công mới hiện số từ server, label snapshot mã/địa chỉ; giảm thật0 được phân biệt với lỗi. Giữ input để sửa, nút cập nhật quote rõ ràng; JS chỉ invalidation hiển thị/focus, no-JS vẫn native quote/submit. Service đặt đơn hiện có tiếp tục revalidate giá/tồn/voucher/quyền địa chỉ, không tin cờ UI hoặc tiền client.

File production sửa: Checkout_Controller (chỉ model/view-state/read validation/messages), order/checkout.html, CSS có prefix checkout; thêm checkout.js riêng checkout. Test mới CheckoutPagesTest và CheckoutIntegrityTest. Không sửa service/repository/security/Flyway/DTO/pom/DB, B2 hoặc GSAP.187→190 source files,184 file cũ giữ hash,3 file cũ đổi/3 file mới. [Preservation](../ui/screenshots/byredo-inspired/B4/preservation.json)

| Gate C27 | Bằng chứng cuối |
|---|---|
| F07 và quote | Before3 tổ hợp, mã còn/tổng sai; after320 bản ghi có đủ valid/no voucher/zero ở MVC/invalid/minimum/read-error/F07/requote/address/empty và no-JS edited-code submit. |
| Contract/security | GET Guest→login, Admin403; CSRF giữ nguyên; addressId/voucher/payment, POST quote/view200 và POST place/redirect không đổi. Không thêm quote token/client total. |
| Giá/tồn/voucher |8 ca service thật với repository doubles kiểm giá SKU hiện hành thay giá giỏ cũ, preview không consume, quyền địa chỉ, tồn/voucher thay đổi và reserve MoMo. Không suy diễn transaction thật. |
| COD/MoMo | Browser COD về đơn PENDING_CONFIRMATION/UNPAID; MoMo OFF ẩn option, ON mock tới trang mock có nhãn, gateway không rõ về đơn pending. MVC thêm ON non-mock label/redirect-only; không gọi merchant/IPN. |
| Responsive/A11y |5 viewport ×2 zoom ×JS on/off,320 records,0 tràn/JS exception, CTA≥56px, keyboard/focus/error links/touch. Hai Enter/80ms cho đúng1 POST COD và1 POST MOMO, giữ submitter/CSRF/loading dimensions. |
| Hồi quy | B1/U2:100/100 tổ hợp; B2:160/160 lượt; Home/GSAP:10/10. Các PASS cũ giữ, ảnh B2 không bị ghi đè. |
| Build | Baseline chạy lại81/0/0/34; suite cuối152=**118 PASS/0 FAIL/0 ERROR/34 SKIPPED**, JDK21.0.13. Package JAR/diff/node-check đạt. |

[Báo cáo B4 đầy đủ và ảnh trước/sau](byredo-inspired-ui-implementation.md#b4) · [320 measurements](../ui/screenshots/byredo-inspired/B4/measurements.json) · [Checks](../ui/screenshots/byredo-inspired/B4/checks.json) · [Rapid Enter](../ui/screenshots/byredo-inspired/B4/rapid-checks.json) · [Maven](../ui/screenshots/byredo-inspired/B4/maven-summary.json) · [262 PNG/log/JSON](../ui/screenshots/byredo-inspired/B4/).

**F07 đạt ở lớp view-state. F01 vẫn BLOCKED.** MariaDB thật, độ nguyên tử/concurrency/idempotency, MoMo merchant/IPN/Sandbox thật, CSP/performance production, screen reader và thiết bị vật lý còn NOT VERIFIED. Không nâng C29/C30, C13/C17/C20/C21. Không coi80% fixture là production-ready. **Dừng B4, không B3, không commit/push.**
