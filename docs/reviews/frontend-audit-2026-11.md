# BeautyShop — Frontend Audit 11/2026

Ngày kiểm tra: 22/11/2026 (Asia/Saigon). Source nền: commit `837e550`, working tree sạch trước audit. Mục tiêu: đánh giá frontend phục vụ demo 03/12/2026 và lộ trình Premium/Luxury Beauty E-commerce.

**Phạm vi thay đổi: chỉ tạo báo cáo này. Không sửa Java/HTML/CSS/JS/cấu hình, không cài dependency, không commit/push, không seed dữ liệu, không gửi thao tác ghi nghiệp vụ.**

## A. Executive Summary

### Kết luận

Source có 29 template trang, 2 fragment, 18 controller và các luồng mua hàng/quản trị chủ yếu. Đây là frontend MVC có cấu trúc chức năng tương đối đầy đủ, nhưng chưa đủ bằng chứng để nghiệm thu toàn bộ trải nghiệm hoặc công bố đạt phong cách Premium/Luxury. Không đưa tỷ lệ hoàn thiện tổng thể vì thiếu render bằng trình duyệt, phiên Customer/Admin và dữ liệu demo trên endpoint đang chạy.

HTTP chỉ đọc tại `http://localhost:8080` xác nhận 7 trang công khai render HTML 200; **catalog không có thẻ sản phẩm, dropdown thương hiệu/danh mục chỉ có “Tất cả”**. Đây là blocker cho demo mua hàng trên endpoint này, không chứng minh database toàn hệ thống trống. Có thể đang dùng schema/profile khác, dữ liệu chưa nạp hoặc dữ liệu chưa được công khai; cần owner môi trường xác nhận, không tự seed.

Có **24 phát hiện: 0 Critical, 4 High, 13 Medium, 7 Low**. Phân loại bao gồm lỗi, thiếu sót UX và rủi ro có căn cứ; không phải 24 lỗi đều đã tái hiện động. Chi tiết và độ tin cậy tại D.

### Điểm mạnh có bằng chứng source/HTTP

- Tất cả 29 trang có `lang="vi"`, viewport, một h1 trong source, main và cùng header/footer. Không phát hiện view mồ côi hoặc view trả về bị thiếu sau kiểm tra thủ công các hàm helper.
- CSS/JS nhỏ, lưu local; không có font hoặc thư viện frontend tải từ CDN. JavaScript duy nhất được nạp bằng `defer`.
- Có nhãn form, thông báo lỗi/success, nhiều empty state; giá phần lớn định dạng phân nhóm VND; catalog bảo toàn filter trong link phân trang.
- Có focus-visible, min-height 44px cho nhiều nút/link, grid linh hoạt, aspect-ratio ảnh, object-fit contain và reduced-motion cho smooth scroll.
- Phân quyền backend, kiểm sở hữu, DTO validation, escaping Thymeleaf và upload ảnh có phòng vệ trong code. Ba form POST công khai kiểm tra được qua HTTP có hidden CSRF.
- Không dùng redirect trình duyệt để công nhận thanh toán; UI mô phỏng MoMo có nhãn không thu tiền.

### Rủi ro trước demo

1. Catalog trên endpoint kiểm tra không có sản phẩm bán được (F01).
2. URL sản phẩm sai hoặc query sai kiểu rơi vào trang lỗi Tomcat tiếng Anh, không có lối quay về shop; còn lộ phiên bản máy chủ (F02).
3. Chưa kiểm chứng responsive tại năm viewport yêu cầu; chưa có kiểm thử Customer/Admin trong audit này.
4. Chưa xác nhận frontend xử lý ổn các tình huống hết tồn, voucher sai/hết lượt, mất phiên, file quá dung lượng và đổi trạng thái đồng thời.
5. Tài liệu cũ ghi test/Chrome E2E đã thành công; không dùng các kết quả đó thay bằng chứng hiện tại. Không khẳng định MoMo Sandbox thật, Ollama thật hoặc host công khai đã sẵn sàng.

### Quy ước bằng chứng và giới hạn

- **S:** đọc source, đối chiếu file/dòng và luồng dữ liệu; chưa tương đương chạy thành công.
- **H:** HTTP thật và phân tích HTML/headers; không chạy JavaScript, không đo layout.
- **B:** trình duyệt thật. Audit này **không có B** và không có ảnh mới.
- **NV / Not Verified:** chưa đủ bằng chứng. Các bước tái hiện ghi trong D là hướng dẫn cho lần QA sau, trừ khi nêu H.
- Browser runtime khởi tạo được nhưng chọn browser báo `No browser is available`; discovery trả `[]`. Không có trình duyệt để chạy viewport, axe hoặc console. Không cài bổ sung và không thay bằng hình minh họa.
- Liệt kê listener ban đầu không thấy cổng, nhưng HEAD/GET tiếp theo xác nhận localhost:8080 có ứng dụng. Kết luận môi trường dựa trên HTTP, không dựa riêng kết quả liệt kê cổng.
- Không có biến `DB_*`, `P01_MARIADB_TEST` trong shell audit; database đằng sau tiến trình đang chạy và database test độc lập chưa được xác nhận. Không đọc secret của tiến trình, không thử đăng nhập bằng mật khẩu suy đoán.
- `java -version`: Temurin 21.0.13; **`mvn -version`: Java 17.0.17**, Maven 3.9.11. Không chạy Maven vì Java Maven chưa đúng và yêu cầu chỉ tạo báo cáo (Maven tạo target). Khi được phép xác minh tiếp, đặt JAVA_HOME cho riêng tiến trình sang JDK 21 rồi xác nhận lại; không sửa cấu hình hệ thống.

## B. Frontend Inventory

### B1. Thống kê thực tế

| Nhóm | Số lượng | Phạm vi/ghi chú |
|---|---:|---|
| HTML Thymeleaf | 31 | Tổng 65.326 byte tại templates, gồm fragment |
| Template trang | 29 | 20 ngoài admin, 9 trong admin; order/list, order/detail và return/list, return/detail còn được dùng chung hai vai trò |
| Fragment | 2 | siteHeader và siteFooter; mỗi fragment được gọi bởi cả 29 trang |
| CSS | 3 | theme.css 6.470 B; storefront.css 3.397 B; admin-catalog.css 969 B |
| JavaScript | 1 | storefront.js 140 B; tổng CSS+JS 10.976 B chưa nén |
| Ảnh trong static | 0 | img chỉ có .gitkeep; hero và placeholder được vẽ bằng CSS |
| Font trong static | 0 | System UI/Segoe UI và Cambria/Times New Roman; không có @font-face |
| Ảnh runtime trong uploads của workspace | 0 | Không suy ra ảnh trong thư mục external được cấu hình ở tiến trình khác |
| Ảnh trong docs | 11 | Bằng chứng lịch sử, không phải assets cửa hàng; không xem là screenshot của audit này |
| File placeholder static | 2 | img/.gitkeep và js/.gitkeep, không tính là ảnh hoặc JS |
| Controller | 18 | 11 ngoài admin, 7 admin |
| Method–URL pattern khai báo | 73 | 39 GET, 34 POST; tách các lựa chọn trong mapping array; chưa nhân tham số section thành 3 giá trị |
| DTO form | 10 | Validation cho các thao tác có form |
| Java test file | 16 | Có unit, MVC mock và MariaDB integration; không chạy lại |

73 pattern không bao gồm POST /login, POST /logout do Security xử lý, /error do Boot xử lý, tài nguyên static và HEAD ngầm định cho GET. Không coi mỗi pattern là một trang riêng.

### B2. Route → controller → template

Tên controller dưới đây bỏ hậu tố `_Controller` cho gọn. Mỗi dòng nhóm các route cùng hành vi; new/edit trong cùng ô là hai URL pattern riêng. `S/NV` nghĩa là đã đối chiếu source nhưng chưa kiểm thử endpoint có phiên phù hợp. `R` là redirect sau POST thành công; lỗi validation có thể render lại form. Tham số `{section}` chỉ nhận `categories`, `brands`, `products` theo domain enum. Mọi route POST nghiệp vụ trong bảng đều chưa được gửi trong audit.

| Route | Controller | Template | Chức năng | Trạng thái | Vấn đề |
|---|---|---|---|---|---|
| GET / | Bootstrap | home/index | Trang chủ | H 200 | F20 |
| GET /login | Bootstrap | auth/login | Form đăng nhập | H 200 | F04, F06 |
| POST /login; POST /logout | Spring Security | auth/login hoặc R | Xác thực/đăng xuất | S/NV | Chưa test đăng nhập/merge |
| GET /register | TaiKhoan | auth/register | Đăng ký | H 200 | F05 |
| POST /register | TaiKhoan | auth/register hoặc R /login?registered | Lưu đăng ký | S/NV | Chưa ghi tài khoản |
| GET /products | CuaHang | product/list | Search/filter/sort/page | H 200, empty | F01, F04 |
| GET /products/{slug} | CuaHang | product/detail | SKU qua query variant | S; slug giả H 404 | F02, F03, F14 |
| GET /cart | GioHang | cart/index | Giỏ Guest/Customer | H 200, Guest empty | F08, F23 |
| POST /cart/items | GioHang | R /cart | Thêm 1 SKU | S/NV | Hết tồn chưa chạy |
| POST /cart/items/{variant}/quantity | GioHang | R /cart | Đổi lượng, 0 là xóa | S/NV | F10, F23 |
| GET /checkout | Checkout | order/checkout | Địa chỉ/báo giá | H Guest 302 /login | F07, F19 |
| POST /checkout/quote | Checkout | order/checkout | Preview voucher | S/NV | F07 |
| POST /checkout | Checkout | order/checkout hoặc R đơn/gateway | COD/MoMo | S/NV | F11, F19 |
| GET /orders | DonHang | order/list | Lịch sử của chủ tài khoản | H Guest 302 | F13 |
| GET /orders/{id} | DonHang | order/detail | Chi tiết đơn | S/NV | F02, F10 |
| POST /orders/{id}/cancel | DonHang | R /orders/{id} | Hủy theo policy | S/NV | F10 |
| GET /account | TaiKhoan | auth/account | Hồ sơ | H Guest 302 | F05 |
| POST /account | TaiKhoan | auth/account hoặc R | Sửa hồ sơ | S/NV | F05 |
| GET /account/addresses | DiaChiNguoiDung | auth/addresses | Địa chỉ | H Guest 302 | F10 |
| GET /account/addresses/new; GET /account/addresses/{id}/edit | DiaChiNguoiDung | auth/address-form | Form địa chỉ | S/NV | F05, F23 |
| POST /account/addresses/new; POST /account/addresses/{id}/edit | DiaChiNguoiDung | auth/address-form hoặc R | Lưu địa chỉ | S/NV | Ownership trong service, chưa test động |
| POST /account/addresses/{id}/delete | DiaChiNguoiDung | R /account/addresses | Xóa địa chỉ | S/NV | F10 |
| GET /account/wishlist | SauMua | auth/wishlist | Yêu thích | H Guest 302 | Không ảnh/giá, cơ hội P2 |
| POST /account/wishlist/{id}; POST /account/wishlist/{id}/delete | SauMua | R /account/wishlist | Thêm/bỏ thích | S/NV | Không success message riêng |
| GET /orders/items/{id}/review | SauMua | order/review | Đánh giá sau giao | S/NV | F05 |
| POST /orders/items/{id}/review; POST /orders/items/{id}/review/{action:edit} | SauMua | order/review hoặc R /orders/{id} | Tạo/sửa đánh giá, action chỉ edit | S/NV | F02 khi conflict |
| GET /orders/{id}/returns/new | SauMua | return/form | Yêu cầu đổi trả | S/NV | Giới hạn 7 ngày có source |
| POST /orders/{id}/returns/new | SauMua | return/form hoặc R chi tiết | Gửi yêu cầu | S/NV | F02 khi quá hạn giữa GET/POST |
| GET /account/returns | SauMua | return/list | Danh sách yêu cầu | H Guest 302 | F13 |
| GET /account/returns/{id} | SauMua | return/detail | Theo dõi yêu cầu | S/NV | Có ownership trong service |
| GET /chat | TuVan | chat/index | Form tư vấn | H 200 | F12 |
| POST /chat | TuVan | chat/index | FAQ/RAG, rate limit | S/NV | F11, F12 |
| GET /payment/momo/return | Momo | order/momo-return | Thông báo chờ xác minh | H 200 | Không có trạng thái đơn cụ thể |
| GET /payment/momo/mock/{id} | Momo | order/momo-mock | Trang mock | H Guest 302 | Chỉ khả dụng nếu mock bật + sở hữu đơn |
| POST /payment/momo/mock/{id} | Momo | R chi tiết đơn | Mô phỏng kết quả | S/NV | Không gửi vì có ghi thanh toán |
| POST /payment/momo/ipn | Momo | Không có, HTTP response | IPN JSON | S/NV | HMAC, ngoại lệ CSRF có điều kiện |
| POST /orders/{id}/momo/retry | Momo | R gateway/đơn | Retry | S/NV | Không gọi cổng trong audit |
| POST /admin/orders/{id}/momo/reconcile | Momo | R đơn Admin | Đối soát | S/NV | Không gọi vì có thể ghi kết quả |
| GET /uploads/sku/{filename} | AnhBienTheSanPham | Không có, stream ảnh | Ảnh đã kiểm tra | H tên giả 404 | F14, F21 |
| GET /admin/catalog | AdminDanhMucSanPhamThuongHieu | R /admin/catalog/categories | Điểm vào catalog | H Guest 302 | Không phải dashboard độc lập |
| GET /admin/catalog/{section} | AdminDanhMucSanPhamThuongHieu | admin/catalog/list | Danh mục/brand/product | S/NV | F13 |
| GET /admin/catalog/{section}/new; GET /admin/catalog/{section}/{id}/edit | AdminDanhMucSanPhamThuongHieu | admin/catalog/form | Form catalog | S/NV | F05 |
| POST /admin/catalog/{section}/new; POST /admin/catalog/{section}/{id}/edit | AdminDanhMucSanPhamThuongHieu | Form hoặc R danh sách | Lưu | S/NV | F11 |
| POST /admin/catalog/{section}/{id}/delete | AdminDanhMucSanPhamThuongHieu | R danh sách | Xóa mục/ngừng sản phẩm | S/NV | F10 |
| GET /admin/catalog/products/{productId}/variants | AdminBienTheSanPham | admin/variant/list | SKU/tồn/giá/ảnh | S/NV | F18 |
| GET /admin/catalog/products/{productId}/variants/new; GET /admin/catalog/products/{productId}/variants/{variantId}/edit | AdminBienTheSanPham | admin/variant/form | Form SKU | S/NV | F24 |
| POST /admin/catalog/products/{productId}/variants/new; POST /admin/catalog/products/{productId}/variants/{variantId}/edit | AdminBienTheSanPham | Form hoặc R danh sách | Lưu SKU và ảnh | S/NV | F24 |
| POST /admin/catalog/products/{productId}/variants/{variantId}/delete | AdminBienTheSanPham | R danh sách SKU | Ngừng bán | S/NV | F10 |
| GET /admin/users | AdminNguoiDung | admin/user/list | Search/page/khóa | H Guest 302 | Có phân trang 15 mục |
| POST /admin/users/{id}/status | AdminNguoiDung | R /admin/users | Khóa/mở tài khoản | S/NV | F10 |
| GET /admin/orders | AdminDonHang | order/list | Dashboard cơ bản và đơn | H Guest 302 | F13, không có dashboard template riêng |
| GET /admin/orders/{id} | AdminDonHang | order/detail | Chi tiết Admin | S/NV | F10 |
| POST /admin/orders/{id}/status | AdminDonHang | R chi tiết | Xác nhận/đóng/giao/hủy | S/NV | F10, F11 |
| GET /admin/vouchers | AdminPhieuGiamGia | admin/voucher/list | Quản trị voucher | H Guest 302 | F13 |
| GET /admin/vouchers/new; GET /admin/vouchers/{id}/edit | AdminPhieuGiamGia | admin/voucher/form | Form voucher | S/NV | UTC có nhãn rõ, chưa test timezone |
| POST /admin/vouchers/new; POST /admin/vouchers/{id}/edit | AdminPhieuGiamGia | Form hoặc R danh sách | Lưu voucher | S/NV | F05 |
| POST /admin/vouchers/{id}/delete | AdminPhieuGiamGia | R danh sách | Xóa có điều kiện | S/NV | F10 |
| GET /admin/reviews | AdminSauMua | admin/user/reviews | Duyệt đánh giá | H Guest 302 | F13 |
| POST /admin/reviews/{id}/visibility | AdminSauMua | R /admin/reviews | Duyệt/ẩn theo revision | S/NV | Có kiểm revision |
| GET /admin/returns | AdminSauMua | return/list | Đổi trả Admin | H Guest 302 | F13 |
| GET /admin/returns/{id} | AdminSauMua | return/detail | Chi tiết Admin | S/NV | Dùng chung template có base |
| POST /admin/returns/{id}/status | AdminSauMua | R chi tiết | Chuyển trạng thái | S/NV | Không tự nhập kho theo source |
| GET /admin/ai | AdminTuVan | admin/chat/index | Trạng thái chỉ mục | H Guest 302 | F15 |
| POST /admin/ai/reindex | AdminTuVan | R /admin/ai | Nạp chỉ mục | S/NV | F11 |
| /error (Boot) | BasicErrorController của framework | Không có error template riêng | Lỗi HTML/JSON theo Accept | H 400/404 ở URL thử | F02 |

### B3. Đối chiếu cấu trúc, tái sử dụng và mã thừa

- Đã đối chiếu tất cả 29 view với return/helper trong controller: không có trang HTML mồ côi; không có controller trả view thiếu. Hàm `duongDan()` trả chuỗi `/admin/catalog/products/…/variants` là URL helper, **không phải view thiếu**. Controller stream ảnh và IPN không cần template.
- Hai fragment đều được tái sử dụng. Không có base layout/head/form-error/pagination fragment chung; phần head lặp trên 29 trang. Đây là cơ hội refactor, không đồng nghĩa lỗi rendering.
- MoMo mock là trang có điều kiện runtime, không phải mã chết. Admin/Customer chỉ mở được với quyền đúng. Không có màn quản trị FAQ CRUD dù use case nhắc đến quản lý FAQ; `/admin/ai` chỉ hiển thị trạng thái/nạp lại.
- Trang order/return dùng chung Admin/Customer có điều kiện `base`; URL do controller đặt, không lấy trực tiếp từ người dùng. Cần test hai vai trò để tránh hồi quy khi sửa layout.
- `theme.css` chứa cả style nền, login, hero và quy tắc catalog/order. `admin-catalog.css` được dùng cho checkout, chat, wishlist, địa chỉ, đơn và đổi trả; tên file phản ánh kém phạm vi thực.
- Không có inline style/script hoặc inline event handler trong 31 file template source. Trang lỗi runtime do container tạo có inline CSS riêng; không đánh đồng với mã ứng dụng.
- Không có JS trùng lặp: file duy nhất có 2 dòng, selector có điểm dùng thật tại product/detail.
- Có CSS override/lặp đã xác định tại F17. Token `--accent` chưa thấy nơi dùng. Không tuyên bố toàn bộ selector khác chết nếu thiếu DOM của mọi nhánh dữ liệu/quyền.
- Tất cả tham chiếu CSS/JS literal trong template trỏ file tồn tại; HTTP 4/4 trả 200, SHA-256 nội dung khớp source. URL ảnh theo dữ liệu chưa kiểm tra được; workspace không có ảnh SKU.
- Không tìm thấy liên kết literal nội bộ sai trong các view đã đọc. Link dữ liệu slug/id, query phân trang, action động được đối chiếu source; chưa crawl được với dữ liệu/phiên. Favicon bị browser mặc định yêu cầu có thể 404 dù template không khai báo link favicon.

## C. Page-by-Page Audit

### C1. Tiêu chí áp dụng cho từng trang

Mỗi dòng dưới đây áp dụng cả 10 tiêu chí người dùng yêu cầu: (1) bố cục/phân cấp, (2) chữ, (3) màu/contrast, (4) khoảng cách/kích thước, (5) header/nav/footer, (6) hover/focus/active/disabled, (7) validation/lỗi, (8) loading/empty/success, (9) tiếng Việt/VND, (10) nhận diện Premium/Luxury.

**Kết quả chung về 1–6 và 10:** có thể xác định cấu trúc, CSS và semantic từ source, nhưng vị trí thực, khoảng trắng, độ cân đối, clipping, target size thực và thẩm mỹ đều **Not Verified bằng trình duyệt cho tất cả trang**. Palette kem–rose và serif heading chỉ thể hiện ý đồ thiết kế. Không kết luận giao diện đẹp/xấu qua source.

Tất cả trang dùng header/footer chung; nền và chữ có token, h1 dùng clamp, body dùng system-ui. Focus-visible có cho link/button/input/select/textarea/summary; hover có ở link/nút/card, chưa có hệ thống active/disabled và aria-current. Error text có màu nhưng lỗi field chưa liên kết accessible với input. Các nhãn bọc input là nhãn hợp lệ, không cần máy móc coi thiếu for/id là lỗi. Bảng sau tập trung khác biệt từng trang; cột NV bổ sung phần phải chạy, không thay đánh giá chung.

| Trang/template | 1–6: cấu trúc và hệ thống từ S/H | 7–9: dữ liệu, form và trạng thái | 10: hướng nâng cấp; phần NV |
|---|---|---|---|
| home/index | Hero h1/CTA và intro h2; artwork CSS ẩn với AT; max-width hero 1280 | H 200, không có dữ liệu mua sắm hoặc error form | Bỏ copy phát triển, thêm brand/category/product từ catalog và nguồn tin cậy; cân đối hero NV |
| product/list | Grid min 230px, ảnh 4:3; form GET 5 trường, có nhãn search | H 200 empty; reset có điều kiện; giữ filter khi phân trang; không có số kết quả/price range | Nhóm filter dễ quét, hiển thị số lượng; grid mobile và nội dung thật NV |
| product/detail | Hai cột, xuống 1 cột <=640; 1 ảnh/SKU; chọn biến thể bằng select + nút | Có sold-out, review empty, giá VND; thiếu chọn lượng tại PDP; JS tự submit khi change | F03 trước; gallery/ảnh thật/CTA theo SKU sau; tất cả nhánh có SKU NV |
| cart/index | ul/li cơ bản, không có lớp card riêng; chỉ theme.css | H giỏ Guest trống; flash success/error, cảnh báo tồn; cập nhật/xóa; không subtotal/tổng; input lượng không max | F08/F23; cần thumbnail, line total, nhãn đăng nhập rồi checkout; giỏ có dữ liệu NV |
| order/checkout | Form tuyến tính, select địa chỉ và dl tiền; dùng CSS catalog chung | COD/MoMo có nhãn, voucher quote; empty địa chỉ/giỏ; giá preview có thể reset sau lỗi; không nút bỏ mã riêng | F07/F19; tóm tắt đơn rõ, thông tin ship trước xác nhận; responsive/submit NV |
| order/list — Customer | Danh sách code/status/total; không ngày đặt trong HTML dù DTO có | Empty state; không search/page; link chi tiết | Đổi thành card đơn có ngày/nhóm trạng thái; dữ liệu thật NV |
| order/detail — Customer | h2 giao hàng/sản phẩm; dl tổng; CTA theo quyền/trạng thái | Flash, mock, thông báo cần hoàn tiền, retry/cancel/review/return; không timeline | Bảo vệ thao tác hủy, làm rõ payment/fulfillment riêng; transition NV |
| auth/login | Label explicit, password/email autocomplete, input 44px | H 200, CSRF; error generic, cartError/inactive; không có toggle hiện mật khẩu | F04, nhãn lỗi liên kết; keyboard/focus/validation NV |
| auth/register | login-card nhất quán với login; label/field | H 200, CSRF; min 12 ký tự, max 72, th:errors; thiếu hướng dẫn giới hạn byte BCrypt | Hướng dẫn mật khẩu trước submit; server trả lỗi/đọc lỗi NV |
| auth/account | Form hồ sơ và link addresses/orders/wishlist/returns | Success, field error, email chỉ đọc bằng text; tel/name autocomplete | Tổ chức navigation tài khoản rõ; phiên Customer NV |
| auth/addresses | Danh sách có nhãn mặc định, thêm/sửa/xóa | Empty và success; xóa không xác nhận | F10; tách primary/secondary/destructive; nhiều địa chỉ dài NV |
| auth/address-form | login-card; địa chỉ dài dùng input một dòng; checkbox cũng chịu style input height | Field errors, autocomplete; không address lookup (không bắt buộc MVP) | F23; xem xét textarea địa chỉ, tổng hợp lỗi; checkbox thực tế NV |
| auth/wishlist | ul/catalog-list, tên/link/bỏ thích; không thumbnail/giá | Empty và sản phẩm ngừng bán; không success cụ thể | Thẻ beauty product có giá/tồn đọc hiện hành; danh sách thực NV |
| order/review | Select 1–5, textarea có label | Giới hạn nội dung, lỗi từng field; chờ duyệt/ẩn; chỉ vào từ đơn giao theo source | Không tự thêm social proof; xung đột/trùng review NV |
| return/list — Customer | List link yêu cầu/trạng thái, không thời gian trong view | Empty; không phân trang | Có thể thêm ngày và lối trợ giúp; ownership NV |
| return/form | Form lượng theo item, max conLai, lý do; nhãn bọc input | Hết 7 ngày ẩn form, allErrors role alert; hết lượng có thông báo; khi tất cả conLai=0 vẫn có nút gửi | Vô hiệu hóa trường hợp không có item hợp lệ, giữ kiểm tra server; ngày 7/8 NV |
| return/detail — Customer | Tên yêu cầu, đơn, trạng thái, lý do và item | Flash success/error; không CTA Admin khi base Customer | Hiển thị tiến trình/xử lý thực tế; render nhánh NV |
| chat/index | Một form textarea và một vùng kết quả, không phải chat nhiều lượt | H 200 + CSRF; aria-live ở answer; không bind lại câu hỏi; không loading; rate limit trong controller | F11/F12; giữ tư vấn có nguồn, không bắt buộc streaming/history; FAQ POST NV |
| order/momo-return | Thông báo trung lập và link danh sách đơn | H 200; không tin browser redirect; không trạng thái giao dịch cụ thể | Giữ thông điệp an toàn; UX chờ/refresh và giao dịch thật NV |
| order/momo-mock | Nêu rõ mô phỏng, giá và 3 nút kết quả | Có requestId, CSRF theo pipeline; GET yêu cầu sở hữu/mock mode | Duy trì nhãn; không nghiệm thu như Sandbox thật; tất cả thao tác NV |
| order/list — Admin/dashboard | 3 ô tổng số đơn/đã thu/doanh thu; nav sang reviews/returns/AI | Source SUM chỉ nhận thanh toán PAID đúng số tiền; không date filter/page | F13; ưu tiên điều phối công việc, không ép dashboard BI vào P0; số liệu NV |
| admin/catalog/list — sản phẩm | Dùng chung category/brand/product, link SKU/sửa/ngừng | Empty, success/error; không search/page | F13; tìm nhanh, trạng thái rõ; tránh làm thay đổi soft-delete |
| admin/catalog/list — danh mục/brand | Nav 3 mục; list tên/slug/status | Xóa có xử lý FK và flash; không confirm | F10; dùng hierarchy nhất quán, pending/action rõ; FK conflict NV |
| admin/catalog/form | Fieldset product; dropdown inactive được chú thích | Global + field errors, giới hạn tên/slug/text, checkbox hoạt động | F05; hướng dẫn slug, preview trước lưu là P2; lỗi thực NV |
| admin/variant/list | Hiển thị ảnh 80×80, giá/tồn on-hand/giữ/available, metadata lô | Empty/success; giá chưa phân nhóm VND, ngừng bán | F18; không suy diễn cần FEFO; ảnh và thông tin dài NV |
| admin/variant/form | Fieldset lô minh họa; file accept PNG/JPEG, preview 80×80 | Có hướng dẫn 2MB/2048px và field errors; multipart oversize có thể bị chặn trước controller | F24, không tự nới giới hạn; upload thực NV |
| admin/user/list | Search name/email, pagination, vai trò/status; không tự khóa | Empty, flash; giữ query/page sau POST | Xác nhận khóa theo user, accessible name cụ thể hơn; thao tác NV |
| order/detail — Admin | Chung cấu trúc Customer, CTA theo next state; nhãn “giao và thu tiền” rõ | Cancel/status/reconcile; flash error; không confirm thu tiền | F10/F11; backend state policy là bắt buộc giữ; chuyển trạng thái NV |
| admin/voucher/list | Mã/loại/giá trị/thời gian UTC/lượt giữ/đã dùng/còn | Empty/flash; xóa có aria-label cụ thể, không confirm; không page | F10/F13; trạng thái hết hạn và hoạt động nên phân biệt; dữ liệu NV |
| admin/voucher/form | Fieldset UTC, datetime-local, fixed/percent có chữ | Global/field errors, giới hạn số; không tự đổi max khi chọn percent | Giữ UTC theo D31; thêm giải thích giờ VN chỉ khi được duyệt, không tự đổi contract |
| admin/user/reviews | Nội dung, sao, link đơn, trạng thái và nút duyệt | Empty/flash; revision chống duyệt nội dung cũ | F13; thêm filter chờ duyệt, không cần table desktop bắt buộc; race NV |
| return/list — Admin | Chung view Customer, base URL khác | Empty, trạng thái; không filter/page | F13; phân biệt tác vụ đang chờ và đã đóng; dữ liệu NV |
| return/detail — Admin | Hiện nút bước tiếp theo theo ReturnStatus | Flash; không tự nhập kho/đổi payment theo thiết kế | Xác nhận các bước có tác động, hướng dẫn kiểm hàng; transition NV |
| admin/chat/index | Trạng thái AI, số đoạn và thời gian UTC; nút reindex | Error/success, AI tắt có fallback copy; disabled khi đang nạp nhưng không có polling/loading UI | F15/F11; hiện không có CRUD FAQ; provider thật NV |

### C2. Responsive và accessibility

| Viewport/kiểm tra | Kết quả | Căn cứ và việc phải làm tiếp |
|---|---|---|
| 360×800 | Not Verified | Browser không khả dụng; cần Guest/Customer/Admin, filter, checkout và thao tác cuối danh sách |
| 390×844 | Not Verified | Đo scrollWidth/clientWidth, ảnh, nút và form |
| 768×1024 | Not Verified | Kiểm header ở breakpoint 760/800, layout 2 cột detail |
| 1366×768 | Not Verified | Kiểm first viewport, hero, nav Admin nhiều mục |
| 1920×1080 | Not Verified | Kiểm max-width, khoảng trắng và grid thật |
| Keyboard/screen reader | Not Verified | Source có native controls/focus, nhưng select auto-submit và lỗi field cần sửa |
| Zoom 200%, text spacing/reflow | Not Verified | Meta không cấm zoom; không xem đây là PASS |
| Axe-core/console JS | Not Verified | Không có browser/DOM thực thi hoặc axe được dùng; chỉ kiểm cú pháp JS |

Không có table HTML trong 29 trang; Admin dùng list flex-wrap, vì vậy “bảng Admin tràn” chưa phải lỗi quan sát được. Tuy nhiên tên dài, danh sách nhiều thao tác, ảnh 80px và header Admin cần test thật. Không suy từ flex-wrap/aspect-ratio rằng trang không overflow.

Chuẩn tham chiếu: [W3C WCAG 2.2](https://www.w3.org/TR/WCAG22/). Rà soát 1.1.1, 1.3.1/1.3.5, 1.4.3/1.4.4/1.4.10/1.4.11/1.4.12, 2.1.1, 2.4.1/2.4.3/2.4.6/2.4.7/2.4.11, 2.5.8, 3.1.1, 3.2.2, 3.3.1/3.3.2/3.3.3/3.3.4/3.3.7/3.3.8 và 4.1.2/4.1.3. Đây là checklist đánh giá, không phải chứng nhận conformant.

- Alt ảnh sản phẩm có tên sản phẩm/SKU; hero và placeholder trang trí aria-hidden. Chưa có dữ liệu ảnh để xác minh chất lượng alt thực tế.
- H1/main/lang được xác nhận trong source và 7 HTML công khai. Nhãn input bọc trong label hợp lệ; login dùng for/id explicit. Không thấy tabindex dương hoặc interactive div.
- Có focus outline rose 3px; chưa đo focus bị che/độ tương phản từng nền. Login password cho phép dán/autocomplete, không thấy CAPTCHA hoặc bài toán nhận thức trong source.
- Có reduced-motion cho scroll; không thấy animation carousel/video tự chạy trên web. Không yêu cầu thêm animation để đạt Luxury.
- Nút thường đặt min-height 44px nhưng không bảo đảm chiều ngang, khoảng cách hoặc checkbox target. **WCAG 2.2 AA 2.5.8 dùng 24×24 CSS px hoặc điều kiện ngoại lệ/khoảng cách; 44×44 là mục tiêu thiết kế và tiêu chí enhanced AAA, không nhầm thành ngưỡng AA bắt buộc.**
- Thiếu skip link, aria-current và quan hệ lỗi field. Không tự coi mọi thông báo sau full-page navigation là lỗi aria-live; cần test với AT.
- Tính contrast bằng relative luminance sRGB từ màu source: muted #7a6762 trên bg #faf7f3 = 4,99:1, trên panel #fffdfb = 5,25:1; rose #894b5d/bg = 6,09:1; trắng trên nút #7b4a55 = 7,11:1; lỗi #8c2637/bg = 8,03:1. Các cặp này đạt ngưỡng normal text trong phạm vi màu đã tính, không chứng minh cả trang đạt AA.
- Viền input #c9ada7/trắng = 2,10:1; #ccb6b0/trắng = 1,93:1: thấp hơn 3:1 cho đường biên xác định control; xem F04. Màu rendered/native controls khác cần kiểm lại trong browser.

### C3. Luồng và trạng thái biên

| Luồng | Đã xác minh | Chưa xác minh/cách kiểm an toàn tiếp theo |
|---|---|---|
| Guest duyệt → SKU → thêm giỏ | H catalog empty; S có route/source | Cần DB demo/test đã xác nhận; SKU active/inactive, hết tồn, stale URL |
| Login và hợp nhất giỏ | S handler gộp và xóa snapshot sau thành công, notice khi giới hạn; exception về login?cartError | Hai request cùng snapshot, merge lỗi DB, login từ checkout; không đăng nhập trên DB hiện dùng |
| Search/filter/sort/page | H empty, query lạ, số âm/lớn và size 0 đều xử lý; sai kiểu page trả 400 | Cần dataset để chứng minh kết quả/sắp xếp/phân trang đúng, không chỉ HTTP 200 |
| Chọn SKU/số lượng | S GET variant và JS; cart quantity 0 là xóa | Keyboard select, back button, stock thay đổi; không submit cart |
| Áp/bỏ voucher | S quote dùng server; bỏ bằng xóa input rồi tính lại, chưa có nút riêng | Voucher sai/hết hạn/tranh lượt cuối; code valid nhưng quote reset khi lỗi F07 |
| COD checkout | S transaction/lock/giá server/ownership address và PRG | Cần DB độc lập để đặt đơn, rollback SKU thiếu, double click và stale tab |
| Theo dõi/hủy đơn | H Guest redirect; S ownership/state và template condition | Customer khác, PACKING, PAID, hủy lặp; không dùng dữ liệu đang chạy |
| Hồ sơ/địa chỉ | H Guest redirect; S @Valid, ownership và default address | Sai phone, địa chỉ dài, xóa mặc định, phiên bị khóa; database test riêng |
| Wishlist/review | H Guest redirect; S review delivered/unique/duyệt revision | Trùng item, sửa chờ duyệt, customer khác, sản phẩm ngừng bán |
| Đổi trả | S giới hạn thời gian/số lượng/khóa đơn | Ngày 7/8, hết số lượng, cạnh tranh; timeout quá hạn giữa mở và gửi form |
| Admin CRUD/SKU/ảnh | H Guest redirect; S backend role, form, upload kiểm MIME/ext/content | Cần Admin test; sai MIME/ext, >2MB, >2048px, xóa khi tham chiếu; không upload |
| Admin đơn/tồn/voucher | S transition/tồn và bộ đếm bảo vệ server | Test DB riêng cho khóa SKU, giá trị voucher, thu tiền và cấm tự khóa |
| FAQ khi AI tắt | H GET form + CSRF; S có FAQ fallback và nhãn không phải AI | Chưa POST câu hỏi; cần kiểm provider runtime, FAQ thật, ngoài nguồn, rate 429, giữ input |
| MoMo | H trang return trung lập, Guest mock redirect; S host allowlist/HMAC | Không gọi gateway, IPN/reconcile/mock submit; cần môi trường mock/test độc lập |

### C4. Hiệu năng và bảo mật frontend

**Hiệu năng:** CSS nạp trong head là render-blocking thông thường, tổng dung lượng rất nhỏ; chưa có bằng chứng cần critical CSS. JS defer 140B, không có bundle/framework lớn. Ảnh catalog lazy/async, detail không lazy, vùng ảnh có aspect-ratio giúp dự trữ không gian; chưa có width/height ở ảnh public nhưng không được suy ra CLS cao khi CSS đã đặt tỷ lệ. Không có srcset/picture/derivatives; upload được giới hạn 2MB/2048px nhưng file ghi lại chưa có budget nén/thumbnail. Không có font download nên font loading/preload không phải blocker; không phát hiện external font request trong source.

HTTP bốn static assets có `Cache-Control: no-store`; stream ảnh không có ETag/Last-Modified/CacheControl riêng trong controller. Chưa biết reverse proxy/prod cache, Content-Encoding và CDN; không kết luận cấu hình production hỏng. **Lighthouse, LCP, INP, CLS, TTFB distribution và 95% catalog <2s: Not Verified**. Không dùng thời gian một script probe để suy ra Core Web Vitals. Không có browser nên console/runtime JS errors chưa đo.

**Bảo mật:** không tìm thấy th:utext, innerHTML, inline event hoặc dynamic script interpolation trong phạm vi frontend. Probe search với chuỗi `<script>alert(1)</script>` trả 200, giá trị input được escape, không có script raw trong HTML. Đây là một test reflected escaping, không đủ kết luận không có XSS toàn hệ thống.

Form ghi dùng method post và th:action; HTTP form login/register/chat có hidden `_csrf` (không ghi token vào báo cáo). CSRF chỉ bỏ qua IPN khi MoMo enabled. `/admin/**` yêu cầu ADMIN; checkout/orders/account yêu cầu CUSTOMER. H xác nhận Guest redirect, **chưa xác minh Customer bị 403 hoặc Admin thành công trong runtime hiện tại**.

Ownership được kiểm trong QuyenDonHang_Service, DiaChiNguoiDung_Service, SauMua_Service và Momo_Facade; template hide button chỉ là phụ trợ. Không thấy đường rò IDOR qua các path đã đọc, nhưng chưa thực hiện thử hai tài khoản. Upload kiểm MIME/ext/content/size/dimensions, UUID, pattern filename và NOFOLLOW_LINKS; có re-encode ảnh. Không thực hiện upload/path traversal động.

Redirect gateway Sandbox kiểm HTTPS, host test-payment.momo.vn, không user-info và port 443 hoặc mặc định; các action/base URL khác được controller ghép từ path nội bộ. KhoTriThuc lọc link nguồn HTTPS hoặc danh sách nội bộ; không phát hiện javascript URL trực tiếp từ model. Không phát hiện secret trong template/JS; không in giá trị biến secret. H lỗi 400/404 có banner Tomcat/version (F02), không thấy stack trace trong hai response đó. Không tuyên bố toàn bộ lỗi server không tiết lộ thông tin.

## D. Issues

### Cách đọc và mức độ

Critical: lỗi bảo mật/giao dịch nghiêm trọng đã có bằng chứng đủ mạnh hoặc hệ thống sập diện rộng. High: chặn demo/luồng chính hoặc rào cản accessibility lớn. Medium: ảnh hưởng UX rõ hoặc rủi ro cần xử lý trước nghiệm thu. Low: chất lượng, nhất quán, tối ưu. Mức độ không đồng nghĩa đã tái hiện bằng browser.

Đường dẫn `T/` = `src/main/resources/templates/`; `J/` = `src/main/java/com/beautyshop/`; `R/` = `src/main/resources/`. Dòng code theo source nền audit. Không có ảnh mới cho bất kỳ issue nào; H là response/HTML/headers quan sát trong phiên này.

### F01 — High — Endpoint demo không có catalog công khai

- **Vị trí:** T/product/list.html:40; J/controller/CuaHang_Controller.java:39. Loại: môi trường/dữ liệu hiển thị, không khẳng định lỗi code.
- **Bằng chứng/tái hiện:** H GET /products không query → 200, không product link/img, brand/category chỉ “Tất cả”, empty text. Không có dữ liệu bán được để vào PDP.
- **Ảnh hưởng:** không demo được chọn SKU/thêm giỏ/COD trên endpoint này.
- **Đề xuất:** owner xác nhận profile/schema và dataset active; chuẩn bị DB demo độc lập có sản phẩm/SKU hợp lệ rồi smoke lại, không tự seed DB đang dùng.
- **Tin cậy:** cao cho biểu hiện; nguyên nhân chưa xác minh. Không có screenshot.

### F02 — High — Trang lỗi container tiếng Anh, mất navigation và lộ server version

- **Vị trí:** R/application.properties:20; không có T/error.html hoặc T/error/*.html. J/controller/CuaHang_Controller.java:28 nhận page int; J/service/CuaHang_Service.java:72 tạo 404.
- **Bằng chứng/tái hiện:** H GET /products/audit-khong-ton-tai-202611 → 404; /products?page=abc → 400; /favicon.ico → 404. Response HTML lang=en, title HTTP Status…, nội dung Tomcat và Apache Tomcat/10.1.55; không link về shop.
- **Ảnh hưởng:** dead end khi link stale/query sai; mất tiếng Việt/nhận diện, lộ fingerprint. Không khẳng định đã xảy ra 500 hoặc leak stack trace.
- **Đề xuất:** error view 400/403/404/409/413/500 bằng tiếng Việt, quay về shop/đơn; xử lý lỗi binding và session expired; tắt banner container; giữ status HTTP gốc. Test riêng lỗi Security và multipart.
- **Tin cậy:** cao S+H; 403/409/413/500 chưa kiểm động. Không có screenshot.

### F03 — High — Đổi select SKU tự điều hướng không báo trước

- **Vị trí:** R/static/js/storefront.js:2; T/product/detail.html:28.
- **Bằng chứng/tái hiện:** S `change` gọi `variant.form.requestSubmit()` dù có nút “Xem biến thể”; chưa có hướng dẫn rằng đổi lựa chọn sẽ tải trang. Dùng keyboard chọn option khi có nhiều SKU để tái hiện trong browser test.
- **Ảnh hưởng:** đổi context bất ngờ, có thể ngắt thao tác/focus khi duyệt lựa chọn; không phù hợp WCAG 3.2.2 nếu không báo trước.
- **Đề xuất:** dùng nút submit đã có hoặc cập nhật nội dung tại chỗ có thông báo phù hợp; giữ fallback không JS.
- **Tin cậy:** cao về hành vi code; ảnh hưởng keyboard cụ thể NV. Không có screenshot.

### F04 — High — Viền nhận diện input có contrast thấp

- **Vị trí:** R/static/css/storefront.css:16; R/static/css/theme.css:5 (login-card input).
- **Bằng chứng/tái hiện:** S tính sRGB #c9ada7/trắng 2,10:1; #ccb6b0/trắng 1,93:1. Nền input trắng trên nền gần trắng, viền là dấu hiệu control chính. Kiểm computed colors và control boundary khi render ở trạng thái thường.
- **Ảnh hưởng:** người thị lực kém khó nhận diện trường; rủi ro WCAG 1.4.11, đặc biệt login/đăng ký/bộ lọc.
- **Đề xuất:** token control-border >=3:1 với nền kề; kiểm cả nền panel/bg/focus/error, không tăng contrast text vốn đã đủ chỉ để đồng bộ.
- **Tin cậy:** cao phép tính source; kết luận conformance cả trang cần browser. Không có screenshot.

### F05 — Medium — Lỗi field không có liên kết accessible

- **Vị trí:** T/auth/register.html:11; T/auth/account.html:13; T/admin/catalog/form.html:20; T/order/checkout.html:29; tương tự address/SKU/voucher/review.
- **Bằng chứng/tái hiện:** S có th:errors nhưng không aria-describedby/aria-invalid hoặc error id tương ứng trong 31 template. Submit invalid trong DB test, Tab tới field rồi kiểm accessibility tree/AT.
- **Ảnh hưởng:** lỗi có thể thấy bằng mắt nhưng thiếu quan hệ máy đọc; khó xác định field cần sửa. Không khẳng định mọi screen reader đều bỏ qua toàn bộ lỗi.
- **Đề xuất:** id lỗi và liên kết field, error summary có link/focus hợp lý, giữ label và dữ liệu hợp lệ; messages tiếng Việt cho binding typeMismatch/date/enum.
- **Tin cậy:** cao S; AT NV. Không có screenshot.

### F06 — Medium — Không có skip-to-content

- **Vị trí:** T/fragments/header.html:4; các main không có target id chung.
- **Bằng chứng/tái hiện:** S không có skip link; 29 trang lặp menu, đặc biệt Admin. Tái hiện bằng Tab từ đầu trang sau mỗi navigation.
- **Ảnh hưởng:** phải đi qua menu lặp lại; main landmark hỗ trợ AT nhưng chưa đủ chứng minh trải nghiệm bàn phím thuận tiện.
- **Đề xuất:** link “Đến nội dung chính” đầu trang, hiện khi focus, target main chung; kiểm 2.4.1 với AT/browser.
- **Tin cậy:** cao thiếu mechanism trong source, keyboard NV. Không có screenshot.

### F07 — Medium — Báo giá voucher bị đặt lại khi checkout trả lỗi

- **Vị trí:** J/controller/Checkout_Controller.java:83 và :112; T/order/checkout.html:30.
- **Bằng chứng/tái hiện:** S hienCheckout luôn gọi themBaoGia(tamTinh, ZERO). Quote thành công hiển thị giảm giá; POST đặt đơn lỗi tồn/validation quay về hienCheckout, input voucher vẫn còn nhưng giam=0, phi/tong tính không giảm. Chưa gửi đơn để tái hiện động.
- **Ảnh hưởng:** khách không biết mã còn hiệu lực hay đã bị bỏ, tổng thay đổi thiếu giải thích. Backend vẫn tính lại server-side; không khẳng định tính sai tiền đơn đã tạo.
- **Đề xuất:** tính lại preview hợp lệ hoặc hiển thị rõ báo giá đã hết hiệu lực và yêu cầu tính lại; có thao tác “Bỏ mã” riêng cập nhật toàn bộ tổng.
- **Tin cậy:** cao S; lỗi cụ thể với dataset NV. Không có screenshot.

### F08 — Medium — Giỏ thiếu tổng tiền và hỗ trợ kiểm tra đơn trước checkout

- **Vị trí:** T/cart/index.html:12 và :26.
- **Bằng chứng/tái hiện:** S chỉ có đơn giá × lượng, tên; không line total, subtotal, thumbnail/link sản phẩm hoặc chú thích phí ship. Mở giỏ nhiều SKU trên DB test.
- **Ảnh hưởng:** khó đối chiếu giá trị giỏ và phát hiện thêm nhầm trước checkout; trải nghiệm mua mỹ phẩm thiếu thông tin trực quan.
- **Đề xuất:** tổng từng dòng/tạm tính server-side, ảnh/link SKU, ghi rõ phí xác nhận tại checkout và điều kiện miễn ship; không lấy giá từ client.
- **Tin cậy:** cao S; bố cục có dữ liệu NV. Không có screenshot.

### F09 — Medium — Navigation không đánh dấu trang hiện tại

- **Vị trí:** T/fragments/header.html:8; T/admin/catalog/list.html:13; R/static/css/theme.css:3.
- **Bằng chứng/tái hiện:** S/H không aria-current/class active theo trang; nav Admin ghép chung nav shop. Mở lần lượt category/product/voucher rồi kiểm vị trí hiện tại.
- **Ảnh hưởng:** khó định hướng khi nhiều màn dùng layout tương tự; header Admin có nhiều mục cần test wrap thực tế.
- **Đề xuất:** active/current cho menu chính/subnav, nhóm Admin riêng và breadcrumb theo ngữ cảnh; không mặc định cần hamburger nếu wrap đang dùng được.
- **Tin cậy:** cao S/H cho menu Guest; Admin visual NV. Không có screenshot.

### F10 — Medium — Thao tác phá hủy/thu tiền không có bước kiểm tra bổ sung

- **Vị trí:** T/auth/addresses.html:22; T/admin/catalog/list.html:33; T/order/detail.html:57 và :65; T/admin/user/list.html:24; T/admin/voucher/list.html:35.
- **Bằng chứng/tái hiện:** S form một nút POST, không confirm/undo trong frontend. Chỉ thử xóa/hủy/thu tiền trên DB test.
- **Ảnh hưởng:** dễ nhấp nhầm hoặc chọn sai đối tượng, đặc biệt xác nhận đã giao và thu tiền. Không chứng minh backend thiếu policy hoặc vi phạm toàn bộ WCAG 3.3.4.
- **Đề xuất:** xác nhận có code đơn/tên đối tượng và hệ quả; phân biệt primary/secondary/destructive; tránh modal thiếu focus management; giữ backend authorization/state.
- **Tin cậy:** cao S về thiếu bước UI; lỗi người dùng là rủi ro. Không có screenshot.

### F11 — Medium — Thiếu trạng thái đang gửi cho thao tác lâu/quan trọng

- **Vị trí:** T/order/checkout.html:40; T/chat/index.html:16; T/admin/chat/index.html:16; R/static/js/storefront.js:1.
- **Bằng chứng/tái hiện:** S không submit pending/busy/disable client hoặc progress message; reindex có th:disabled lúc render nhưng không phản ánh ngay thời gian request đang chạy. Mô phỏng mạng chậm trên DB test.
- **Ảnh hưởng:** khách dễ bấm lại, không biết đã nhận yêu cầu; tăng request FAQ và rủi ro phản hồi lỗi lần gửi tiếp theo. Không khẳng định tạo đơn trùng: service khóa giỏ/xóa item trong transaction.
- **Đề xuất:** pending feedback accessible, chống submit lặp hợp lý, giữ giá trị payment của nút submit và PRG; chỉ thêm idempotency server nếu test chứng minh cần.
- **Tin cậy:** cao S thiếu UI, hệ quả dữ liệu NV. Không có screenshot.

### F12 — Medium — Câu hỏi tư vấn không được giữ sau POST/lỗi

- **Vị trí:** T/chat/index.html:13; J/controller/TuVan_Controller.java:43.
- **Bằng chứng/tái hiện:** S textarea chỉ name, không th:field/th:text; controller trả cùng template với form đã bind. Khi render lại, textarea trống dù có answer/lỗi/rate limit. POST câu hỏi trên môi trường test để xác minh.
- **Ảnh hưởng:** mất ngữ cảnh hỏi–đáp, phải gõ lại khi cần sửa, khó hiểu câu trả lời thuộc câu nào.
- **Đề xuất:** bind lại cauHoi, hiển thị câu hỏi cùng đáp án, focus/hướng dẫn retry khi 429; không cần bổ sung lịch sử cá nhân vào RAG.
- **Tin cậy:** cao S; POST NV. Không có screenshot.

### F13 — Medium — Nhiều danh sách quản trị không có search/filter/pagination

- **Vị trí:** T/admin/catalog/list.html:23; T/order/list.html:21; T/admin/user/reviews.html:12; T/admin/voucher/list.html:19; T/return/list.html:10. J/repository/DonHang_Repository.java:47; DanhGia_Repository.java:57; DoiTra_Repository.java:50; PhieuGiamGia_Repository.java:28.
- **Bằng chứng/tái hiện:** S query list không LIMIT/page, view render toàn bộ. Ngoại lệ: Admin users và storefront products đã phân trang. Cần dataset lớn độc lập để đo.
- **Ảnh hưởng:** khó tìm đơn/chờ duyệt, DOM và request tăng theo dữ liệu; không công bố đã chậm thực tế.
- **Đề xuất:** ưu tiên order filter theo trạng thái/code/ngày và paging server; catalog search; reviews/returns/voucher sau; cập nhật contract DTO/query/test.
- **Tin cậy:** cao S thiếu chức năng, độ chậm NV. Không có screenshot.

### F14 — Medium — Chưa có ảnh nhiều kích thước/budget tối ưu theo ngữ cảnh

- **Vị trí:** T/product/list.html:48; T/product/detail.html:18; J/service/AnhBienTheSanPham_Service.java:28 và :52.
- **Bằng chứng/tái hiện:** S dùng cùng URL, không srcset/picture/thumbnail; upload tối đa 2MB và 2048px, re-encode nhưng không resize/nén theo kích thước card/detail. Không có ảnh live để đo byte.
- **Ảnh hưởng:** nguy cơ tải ảnh lớn cho card mobile; placeholder CSS không thay thế được nhận diện sản phẩm đa thương hiệu.
- **Đề xuất:** tạo derivative và budget sau khi chốt quyền ảnh, phục vụ đúng kích thước; giữ giới hạn/kiểm tra an toàn. Đo trước khi quyết định WebP/AVIF hoặc thay đổi pipeline.
- **Tin cậy:** cao S, tác động mạng/CLS NV. Không có screenshot.

### F15 — Medium — Quản trị FAQ được mô tả nhưng UI chỉ có reindex

- **Vị trí:** docs/design/04-use-cases.md:16; T/admin/chat/index.html:9; J/controller/admin/AdminTuVan_Controller.java:16.
- **Bằng chứng/tái hiện:** S có UC quản lý FAQ, nhưng không có route/form tạo/sửa/ẩn FAQ; chỉ trạng thái AI và POST reindex. FAQ hiện được nạp bằng migration V5.
- **Ảnh hưởng:** Admin không tự sửa nội dung tư vấn qua UI; khoảng cách giữa use case và thực hiện.
- **Đề xuất:** owner chốt rõ MVP chỉ FAQ seed/reindex hay cần CRUD; nếu thêm phải kiểm URL nguồn/nội dung, quyền, CSRF và invalidation index. Không tự mở scope trong audit.
- **Tin cậy:** cao S cho chênh lệch phạm vi. Không có screenshot.

### F16 — Medium — Thiếu lối truy cập chính sách/liên hệ từ luồng mua

- **Vị trí:** T/fragments/footer.html:4; T/order/checkout.html:39; T/home/index.html:22.
- **Bằng chứng/tái hiện:** S/H footer chỉ hai dòng nhận diện đồ án; không link vận chuyển/đổi trả/hỗ trợ. FAQ tư vấn không thay thế đường truy cập ổn định cho mọi khách.
- **Ảnh hưởng:** khách khó kiểm tra điều kiện mua/đổi trả trước đặt hàng, giảm độ tin cậy.
- **Đề xuất:** trình bày chính sách đã duyệt từ BR/FAQ và kênh hỗ trợ thật; không tự đặt cam kết hoàn tiền, địa chỉ hoặc giấy phép chưa xác nhận.
- **Tin cậy:** cao S/H. Không có screenshot.

### F17 — Low — CSS override phân tán và token chưa dùng

- **Vị trí:** R/static/css/theme.css:1, :2, :6, :17, :19, :23, :48; R/static/css/admin-catalog.css:25.
- **Bằng chứng/tái hiện:** S body font bị thay từ serif sang system-ui; nav-shell height/min-height, focus, mobile nav có nhiều khai báo override; --accent chưa tham chiếu. `.catalog-admin .btn` khai báo ở hai CSS.
- **Ảnh hưởng:** khó dự đoán cascade và duy trì scale; không tự kết luận toàn bộ khai báo đầu chết vì selector/độ ưu tiên khác.
- **Đề xuất:** hợp nhất token, base/component/responsive theo phạm vi, giữ visual regression; đổi tên CSS dùng chung khi refactor được duyệt.
- **Tin cậy:** cao S. Không có screenshot.

### F18 — Low — Giá SKU Admin chưa theo format VND chung

- **Vị trí:** T/admin/variant/list.html:24.
- **Bằng chứng/tái hiện:** S `${sku.giaBan} ₫` trực tiếp, trong khi public/order/voucher dùng formatInteger(...,'POINT'). SKU giá 1234567 để so sánh.
- **Ảnh hưởng:** khó quét giá trị lớn, format không thống nhất.
- **Đề xuất:** formatter VND chung server/template, không đổi kiểu tiền BigDecimal.
- **Tin cậy:** cao S; dữ liệu runtime NV. Không có screenshot.

### F19 — Low — Tiêu đề COD chưa phản ánh lựa chọn MoMo có điều kiện

- **Vị trí:** T/cart/index.html:26; T/order/checkout.html:10 và :41.
- **Bằng chứng/tái hiện:** S link/heading nói COD dù khi momoEnabled=true còn có nút MoMo. Mặc định MoMo tắt nên không phải blocker COD.
- **Ảnh hưởng:** nội dung không nhất quán khi demo P1.
- **Đề xuất:** dùng tên bước trung lập “Thanh toán/Đặt hàng”, giải thích từng phương thức và nhãn mock rõ.
- **Tin cậy:** cao S; nhánh bật MoMo NV. Không có screenshot.

### F20 — Low — Trang chủ còn copy phát triển, thiếu điểm vào catalog theo nhu cầu

- **Vị trí:** T/home/index.html:23; T/fragments/footer.html:5.
- **Bằng chứng/tái hiện:** S/H copy “DỰ ÁN ĐANG PHÁT TRIỂN”, “Tên thương hiệu tạm thời”; hero/intro không có brand/category/featured products.
- **Ảnh hưởng:** chưa thể hiện nhận diện shop đa thương hiệu, thêm bước khám phá sản phẩm.
- **Đề xuất:** chốt nhận diện/giọng điệu trước; thêm navigation danh mục/brand và sản phẩm chọn lọc từ dữ liệu active; không tự bịa testimonial/chứng nhận.
- **Tin cậy:** cao nội dung S/H, cảm nhận visual NV. Không có screenshot.

### F21 — Low — Static assets không tận dụng cache trên endpoint hiện tại

- **Vị trí:** R/application-prod.properties:8 chỉ bật cache template; J/controller/AnhBienTheSanPham_Controller.java:34 không có validator/cache ảnh; chưa có cấu hình cache static riêng trong source.
- **Bằng chứng/tái hiện:** H GET 3 CSS + 1 JS trả Cache-Control: no-store; byte tải khớp repo. Không biết profile/proxy hiện tại, không khẳng định cấu hình prod cũng vậy.
- **Ảnh hưởng:** tải lại assets khi điều hướng; tác động hiện nhỏ vì tổng ~11KB, ảnh tương lai lớn hơn.
- **Đề xuất:** xác minh prod; fingerprint/version asset rồi cache có kiểm soát, ETag/Last-Modified ảnh. Không cache HTML tài khoản hoặc dữ liệu nhạy cảm.
- **Tin cậy:** cao H tại localhost, prod NV. Không có screenshot.

### F22 — Low — Thiếu favicon

- **Vị trí:** T/home/index.html:3 (head); không có favicon.ico trong R/static/ hoặc favicon link trong các head. R/static/img/ chỉ có .gitkeep rỗng.
- **Bằng chứng/tái hiện:** H GET /favicon.ico → 404 và trang lỗi container.
- **Ảnh hưởng:** thiếu nhận diện tab, có request lỗi nhỏ.
- **Đề xuất:** tạo favicon từ logo đã được duyệt và khai báo đúng; không tự lấy logo thương hiệu khác.
- **Tin cậy:** cao S/H. Không có screenshot.

### F23 — Low — Style control chưa bao phủ đúng cart và checkbox

- **Vị trí:** T/cart/index.html:18; T/auth/address-form.html:19; R/static/css/theme.css:5.
- **Bằng chứng/tái hiện:** S cart không load catalog CSS, không thuộc login-card/storefront nên input quantity dùng phần lớn style trình duyệt. Ngược lại `.login-card input` áp height 44px/margin/border cho checkbox địa chỉ.
- **Ảnh hưởng:** nguy cơ control không nhất quán; chưa khẳng định checkbox render sai kích thước khi native appearance còn hiệu lực.
- **Đề xuất:** control styles theo type, field group dùng chung; kiểm target và spacing thật ở mobile/zoom.
- **Tin cậy:** cao phạm vi selector, ảnh hưởng visual NV. Không có screenshot.

### F24 — Medium — Upload vượt giới hạn multipart chưa có phản hồi form rõ ràng

- **Vị trí:** R/application.properties:15; J/controller/admin/AdminBienTheSanPham_Controller.java:35; T/admin/variant/form.html:24.
- **Bằng chứng/tái hiện:** S giới hạn multipart 2MB; kiểm/catch lỗi ảnh trong service chỉ chạy khi request đã bind được. Không có ControllerAdvice/ExceptionHandler cho MaxUploadSizeExceededException trong source. File >2MB có thể bị từ chối trước xử lý form.
- **Ảnh hưởng:** Admin có thể rơi ra trang lỗi, mất nội dung đang nhập thay vì lỗi cạnh ô ảnh. HTTP status/HTML cụ thể chưa kiểm chứng, không khẳng định đã tái hiện.
- **Đề xuất:** xử lý multipart quá lớn thành lỗi tiếng Việt và lối khôi phục; kiểm type/dimensions/size client để hỗ trợ, vẫn giữ server validation. Test file giả chỉ trên DB/upload dir test độc lập.
- **Tin cậy:** cao thiếu handler S; kết quả động trung bình/NV. Không có screenshot.

## E. UX/UI Scorecard

Thang 0–10 chỉ dùng cho **checklist source**, mỗi mục chấm từ 5 kiểm tra nhị phân, 2 điểm/kiểm tra, mô tả bên dưới. Không dùng điểm source thay chất lượng hình ảnh, trải nghiệm thực, chứng nhận WCAG hoặc hiệu năng. Không tính điểm tổng vì nhiều hạng mục NV.

| Hạng mục | Điểm source theo rubric | Điểm trải nghiệm thực | Căn cứ |
|---|---:|---|---|
| Visual design | Not Verified | Not Verified | Chưa có render browser; không chấm thẩm mỹ từ CSS |
| Design consistency | 6/10 | Not Verified | Đạt: header/footer chung, palette token, focus chung; thiếu: VND mọi trang, hệ thống control theo type/phạm vi |
| Navigation | 6/10 | Not Verified | Đạt: logo về home, nhãn/link native, link theo role; thiếu: current page, policy/support navigation |
| Responsive | Not Verified | Not Verified | Có CSS thích ứng; không có viewport measurement |
| Accessibility | Not Verified | Not Verified | Có semantic/focus nhưng tồn tại F03/F04/F05; không đủ evidence để gán điểm conformant |
| Forms | 6/10 | Not Verified | Đạt: label, server validation cho form DTO, nhiều thành công PRG; thiếu: liên kết field error, pending UI |
| Product discovery | 6/10 | Not Verified | Đạt: keyword, brand/category/skin filter, sort+page; thiếu: số kết quả, lọc khoảng giá. Không tính dataset hiện tại là đạt |
| Checkout UX | 6/10 | Not Verified | Đạt: địa chỉ, breakdown tiền, nhãn thanh toán; thiếu: preview voucher ổn định sau lỗi, pending UI |
| Admin usability | 6/10 | Not Verified | Đạt: route phân quyền, feedback, next-state actions; thiếu: search/page ở phần lớn list, xác nhận thao tác nhạy cảm |
| Performance | Not Verified | Not Verified | ~11KB CSS/JS chỉ là inventory; không có Lighthouse/CWV/thực thi browser |

## F. Remediation Roadmap

Không có thay đổi nào trong roadmap được thực hiện trong audit. Ước lượng theo ngày công kỹ thuật + QA, giả định một người làm tập trung và contract không thay đổi; không phải cam kết lịch. S: <=0,5 ngày; M: 0,5–1,5 ngày; L: 2–4 ngày. Có thể ghép việc chung, không cộng cơ học từng issue.

### P0 — Trước buổi rehearsal tiếp theo

| Thứ tự | Việc | Độ phức tạp/ước lượng | Phụ thuộc | Điều kiện nghiệm thu |
|---|---|---|---|---|
| 1 | Xác nhận endpoint/profile/DB demo và dataset (F01) | S–M, 0,5–1 ngày | Owner dữ liệu/vận hành; DB demo độc lập được xác nhận | Có SKU active và ít nhất một tuyến COD demo kiểm soát được, không dùng production |
| 2 | Trang lỗi/validation fallback (F02, F24) | M, 1–1,5 ngày | Security + controller + Thymeleaf | 400/403/404/409/413/500 giữ status, tiếng Việt, không lộ stack/version, có recovery |
| 3 | Chạy QA có browser, tài khoản test, DB test riêng | M, 1 ngày | Browser kết nối, JDK 21 cho Maven, schema test trống, upload dir test | 5 viewport; Guest→merge→COD→Admin→Customer; test quyền/CSRF/stock/voucher; ảnh và log có nguồn |

P0 gồm cả việc mở khóa môi trường xác minh, không mặc nhiên coi mọi vấn đề chưa test là lỗi P0. Không có thay đổi nghiệp vụ nào được đề nghị để né kiểm thử.

### P1 — Ổn định trải nghiệm cốt lõi trước feature freeze 29/11

| Thứ tự | Việc | Độ phức tạp/ước lượng | Phụ thuộc | Điều kiện nghiệm thu |
|---|---|---|---|---|
| 4 | SKU explicit submit, contrast, field errors, skip link (F03–F06) | M, 1–1,5 ngày | Component/form pattern dùng chung | Keyboard chọn SKU không context change bất ngờ; lỗi được đọc; contrast đủ; bypass nav |
| 5 | Giỏ/checkout/voucher preview và control (F07/F08/F19/F23) | M–L, 1,5–3 ngày | DTO/rendering/tổng tiền server; không đổi pricing policy | Tổng luôn rõ, bỏ mã có hồi đáp, lỗi không hiển thị preview sai trạng thái, quantity/mobile dùng được |
| 6 | Pending và xác nhận nhạy cảm (F10/F11) | M, 1–1,5 ngày | JS enhancement giữ fallback/CSRF/submitter; modal accessible nếu dùng | Mạng chậm có phản hồi; thao tác đúng đối tượng; regression double click trên DB test |
| 7 | Navigation/current state và chat giữ input (F09/F12) | S–M, 0,5–1 ngày | Header model/current path, Thymeleaf binding | Current nav đúng role; hỏi/lỗi/429 giữ câu hỏi và focus hợp lý |
| 8 | Admin order search/filter/page trước, list khác sau (F13) | L, 2–4 ngày | Query/DTO/controller/template và test contract | Tìm đúng đơn, giữ filter, phân trang server, không mất thao tác mobile |
| 9 | Policy/support đã duyệt và scope FAQ (F15/F16) | M, 0,5–1,5 ngày nếu chỉ nội dung; CRUD riêng L | Owner nghiệp vụ duyệt nội dung/phạm vi | Khách mở chính sách từ checkout/footer; tài liệu khớp chức năng thật |

### P2 — Premium/Luxury có bằng chứng, sau core ổn định

| Thứ tự | Việc | Độ phức tạp/ước lượng | Phụ thuộc | Điều kiện nghiệm thu |
|---|---|---|---|---|
| 10 | Chốt brand direction, nội dung home/footer, styleguide (F20/F17) | M–L, 2–3 ngày | Tên/logo, typography/ảnh hợp quyền, review wireframe | Palette/spacing/type/button/card thống nhất được owner duyệt qua render thật |
| 11 | Ảnh sản phẩm thật + derivatives/budget (F14) | L, 2–4 ngày | Quyền ảnh, pipeline upload/storage, metadata kích thước | Card/detail sắc nét, tải đúng cỡ; đo LCP/CLS trên dataset chuẩn |
| 12 | VND formatter, favicon, cache prod (F18/F21/F22) | S–M, 0,5–1 ngày | Logo đã duyệt, version asset, reverse proxy | VND thống nhất, favicon 200, cache có kiểm soát không áp cho trang riêng tư |
| 13 | Nâng polish sau mua/wishlist/filter UX | M–L, 2–3 ngày | Component nền đã ổn, dữ liệu và browser QA | Thumbnail, timeline, count/chips, empty state có CTA; không bịa review/testimonial |

Hướng thiết kế: neutral kem/rose hiện tại có thể giữ sau khi render và duyệt; serif cho display và sans cho body/control; scale spacing/type nhất quán; ảnh sản phẩm là trung tâm, khoảng trắng có chủ đích, CTA mua rõ. Trang Admin ưu tiên mật độ thông tin và thao tác chính xác. Không đổi sang SPA, thêm framework, carousel, animation hoặc webfont chỉ để tạo cảm giác cao cấp. Không đổi phí ship, voucher, điều kiện hủy/đổi trả hoặc payment states trong đợt polish.

## G. Nhật ký kiểm chứng thực tế và blocker

### Đã thực hiện

- `git status --short` đầu phiên: sạch. `git rev-parse --short HEAD`: 837e550.
- `rg --files`, `rg -n`, Get-Content và Python in-memory để đọc đủ template, CSS/JS, controller, security, DTO; đối chiếu service/repository phục vụ render và tài liệu README/SRS/BR/ERD/Decisions/use case. Đếm lại thực tế, không lấy số từ báo cáo trước.
- `java -version`, `mvn -version`: xác nhận lệch JVM như A; không chạy build/test/package.
- Browser: bootstrap thành công, getForUrl localhost báo không có browser, sau troubleshooting list trả []; không chạy browser khác, không cài Playwright/Chromium/axe.
- `curl.exe --max-time 3 --head http://localhost:8080/`: HTTP 200, UTF-8, nosniff, frame DENY, cache không lưu.
- Python urllib GET không theo redirect: **35 URL duy nhất** trong ma trận dưới; không lưu cookie/credentials/token ra file. Request có thể tạo session Guest ở server theo hành vi framework; không tạo/sửa bản ghi giao dịch, không login, không submit POST.
- Parse 7 HTML công khai: lang=vi, không còn thuộc tính th:text/href/field/each chưa xử lý; các form POST login/register/chat có CSRF. Không ghi giá trị token.
- Đối chiếu SHA-256 bốn asset HTTP với file repo: 4/4 khớp. Điều này xác nhận asset version, **không chứng minh toàn bộ Java của tiến trình là commit HEAD**.
- `Get-Content ...storefront.js | node --check --input-type=commonjs`: exit 0. Đây là syntax check, không phải thực thi DOM hoặc kiểm console.
- Tính contrast sRGB offline trên các cặp đã liệt kê; không tạo artifact khác.
- Một lần script probe đầu bị lỗi output CP1252 khi in tiếng Việt sau GET /; đã sửa encoding trong tiến trình Python và chạy lại thành công. Không quy lỗi encoding công cụ thành lỗi UTF-8 website.

### Ma trận 35 URL HTTP

| Nhóm | URL | Kết quả quan sát |
|---|---|---|
| Trang công khai (7) | /; /products; /login; /register; /cart; /chat; /payment/momo/return | 7×200 HTML UTF-8; products/cart empty; không kết luận FAQ POST đã hoạt động |
| Protected Guest (14) | /checkout; /orders; /account; /account/addresses; /account/wishlist; /account/returns; /admin/catalog; /admin/orders; /admin/users; /admin/vouchers; /admin/reviews; /admin/returns; /admin/ai; /payment/momo/mock/1 | 14×302 đến http://localhost:8080/login; chưa chứng minh role/ownership sau login |
| Static (4) | /css/theme.css; /css/storefront.css; /css/admin-catalog.css; /js/storefront.js | 4×200, bytes khớp source, Cache-Control no-store |
| Lỗi (3) | /products/audit-khong-ton-tai-202611; /products?page=abc; /favicon.ico | 404/400/404; trang Tomcat tiếng Anh |
| Path không công khai (1) | /audit-khong-ton-tai-202611 | 302 /login do anyRequest authenticated; không được gọi đây là 404 PASS |
| Query normalization (1) | /products?sort=invalid&page=-1&size=1000 | 200 empty; không kiểm được thứ tự sản phẩm |
| Edge GET (5) | /products?q=%3Cscript%3Ealert(1)%3C%2Fscript%3E; /products?brand=no-such-brand&category=no-such-category&skinType=test; /products?page=2147483647&size=48; /products?size=0; /uploads/sku/audit-missing.png | 4×200, ảnh giả 404; search payload escape trong value, không raw script |

### Chưa kiểm chứng động

**Tất cả trang đều chưa được kiểm chứng bằng trình duyệt.** Chỉ 7 trang công khai nêu trên đã nhận/parse HTML thật. Product detail có dữ liệu, cart có item, mọi trang Customer sau xác thực và mọi trang Admin sau xác thực đều chưa được render trong audit. Trang mock MoMo chưa được render với đơn thuộc sở hữu. Không có screenshot mới.

Không chạy `mvn test` hoặc `mvn -DskipTests package`, không chạy test tích hợp MariaDB vì chưa có DB test độc lập xác nhận và giới hạn chỉ tạo báo cáo. Các test hiện có dùng P01_MARIADB_TEST=true để bật integration; tuyệt đối không bật cờ đó với DB đang dùng. Không báo PASS 51 test từ tài liệu cũ cho lần audit này.

Không có Lighthouse/CWV, axe report, accessibility tree, console errors, navigation keyboard, 200% zoom hoặc kiểm screenshot viewport. Không kiểm transport/cache production/host/domain. Không truy cập DB trực tiếp để đếm dữ liệu hoặc thay đổi dữ liệu.

### Phương án xác minh tiếp theo

1. Owner xác nhận đúng endpoint + dataset và cung cấp phiên/tài khoản test theo vai trò; xác nhận schema test độc lập có thể ghi, tuyệt đối không dùng production.
2. Kết nối browser hiện có hoặc được phép chuẩn bị công cụ; thống nhất nơi lưu ảnh ngoài đợt chỉ tạo một file này.
3. Chạy viewport/keyboard/axe/zoom trên các trang có dataset chuẩn, kiểm scrollWidth và bounding box, ảnh contain, nav/focus/CTA, filters, form, list Admin; chụp có URL/role/viewport/timestamp.
4. Chạy flows ghi chỉ trên DB test: cart merge, COD, voucher, sở hữu đơn, hủy/thu tiền, SKU/upload, review/return và FAQ. MoMo mock và Ollama mock phải có nhãn bằng chứng riêng.
5. Sau khi được phép tạo build artifacts, dùng Maven với Java 21 và chạy suite phù hợp, báo số chạy/bỏ qua thật; chỉ mở rộng khi còn lỗi/chưa rõ. Cuối cùng đo hiệu năng với cấu hình, dataset và mạng đã ghi nhận.

Không có PR hoặc commit trong lần kiểm tra này. Báo cáo không phê duyệt thay đổi contract/nghiệp vụ; các điểm phạm vi FAQ, nhận diện và policy cần owner quyết định khi bước sang triển khai.
