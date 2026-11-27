# Hướng dẫn kiểm thử thủ công BeautyShop

Phạm vi đối chiếu là mã nguồn trên `main` tại commit `9a812ca`. Mỗi dòng dưới đây mô tả **kết quả dự kiến**. Bấm mã ca để đọc luồng xử lý, kiểu dữ liệu của từng tầng, lỗi và đích quay lại. Đánh dấu kết quả của bạn sau khi thực hiện từng ca.

**Khi kiểm bản hiện hành:** TV-01 đến TV-03 là ca lịch sử của form tư vấn đã gỡ. Mở **Tư vấn** từ header, trang chủ và footer để kiểm cả ba lối vào đều dẫn đến `/chat`. Tiếp tục với TV-04 đến TV-07. Khi chạy QA-03, kiểm giao diện `/chat`. [D41](../DECISIONS.md)

## 1. Chuẩn bị

- Chạy BeautyShop theo [hướng dẫn local](../deployment/01-local-db-and-flyway.md). Các đường dẫn trong tài liệu bắt đầu từ `http://127.0.0.1:8080`. Nếu app dùng cổng khác, giữ nguyên phần đường dẫn sau cổng.
- Dùng dữ liệu demo để các thao tác đặt hàng, hủy đơn, khóa tài khoản và xóa mục kiểm thử không ảnh hưởng dữ liệu đang sử dụng. Chuẩn bị một SKU còn hàng, một SKU hết hàng, một sản phẩm có nhiều biến thể và một voucher còn hiệu lực. Dữ liệu thiếu thì ghi **Chặn** cùng điều kiện còn thiếu. [Chuẩn bị dữ liệu demo](../deployment/06-p10-demo.md)
- Mở các phiên trình duyệt riêng cho **Khách** chưa đăng nhập, **Customer A**, **Customer B** và **Admin**. Hai Customer dùng dữ liệu giả lập. Dùng tài khoản Admin hiện có. Giữ mã đơn, mã yêu cầu đổi trả và SKU của lượt test để đối chiếu giữa các phiên.
- Mặc định, MariaDB giữ dữ liệu cửa hàng và MongoDB giữ đánh giá. Khi test đánh giá demo, dùng MongoDB mà `MONGODB_URI` của app đang trỏ tới. `beautyshop_test_mongo` dành cho bộ test tự động. [Cấu hình đánh giá](../deployment/07-mongodb-danh-gia.md)
- Chạy **COD** trước. MoMo mock, MoMo Sandbox và AI Ollama có điều kiện riêng ở [mục 7](#7-thanh-toán-momo) và ca **TV-03**, **TV-07**.

**Thứ tự thực hành** dùng một đơn để từ Customer mua đến Admin giao, rồi quay lại Customer đánh giá và yêu cầu đổi trả. Dùng đơn COD thứ hai để kiểm tra hủy. Hai yêu cầu đổi trả riêng giúp kiểm tra cả nhánh duyệt và nhánh từ chối.

**Cách đọc đường dẫn**: `{id}`, `{slug}`, `{skuId}` là giá trị lấy từ link trên trang đang test. `#...` chỉ cuộn tới một khu trên cùng trang. Với bảng dưới đây, điền **Đạt**, **Lỗi**, **Chặn**, hoặc **Chưa chạy** vào cột cuối.

## 2. Khách chưa đăng nhập · Trang chủ và catalog

| Mã | Thao tác | Đi đâu và kiểm tra gì | Kết quả |
|---|---|---|---|
| [SHOP-01](#shop-01) | Mở `/`. Bấm logo **BeautyShop** hoặc **Trang chủ** từ trang khác. | Về `/`. Trang có phần giới thiệu, khu sản phẩm nổi bật, **Được yêu thích nhất** và **Tất cả sản phẩm** khi có dữ liệu tương ứng. | BeautyShop khi bấm ở `/` sẽ có hiện tượng mở ở 01/BEAUTYSHOP (Chương 1) và ngay lập tức chuyển sang ô hiện tại (Chương lúc bấm BeautyShop): Nó là bình thường hay bất thường |
| [SHOP-02](#shop-02) | Ở trang chủ, dùng điều hướng chương hiện trên màn hình. Cuộn qua các chương rồi kiểm tra lại ở mobile. | Vẫn ở `/`. Có thể đi qua nội dung về loại da, nghi thức, hoạt chất, sản phẩm và tư vấn. Nếu carousel không hoạt động, nội dung và link vẫn đọc được khi cuộn. | Đạt |
| [SHOP-03](#shop-03) | Bấm một ô loại da, link của bước nghi thức có danh mục hoặc một ô hoạt chất. | Đến `/products` với bộ lọc tương ứng `skinType`, `category` hoặc `q`. Ô loại da hoặc hoạt chất chỉ có khi dữ liệu phù hợp. Link **Tìm theo loại da** mở `/products#skin-type`. | Đạt |
| [SHOP-04](#shop-04) | Trong **Tất cả sản phẩm** ở trang chủ, bấm **Mới nhất**, **Giá tăng**, **Giá giảm**, rồi **Xem tất cả sản phẩm**. | Mỗi lựa chọn ở lại `/?sort=...#home-all-products`. Link cuối mở `/products` và giữ lựa chọn giá tăng/giảm khi có. | 12 sản phẩm ở Tất cả sản phẩm hiển thị theo tiêu chí nào, bấm thay đổi ở Mới nhất, giá tăng, giá giảm thì hiển thị tiêu chí thế nào trong toàn bộ sản phẩm |
| [SHOP-05](#shop-05) | Bấm **Khám phá sản phẩm**, **Sản phẩm** hoặc gửi ô tìm kiếm ở header. Tìm bằng tên và sau đó bằng mã SKU. | Mở `/products`. Khi tìm kiếm, URL chứa `q`, ô tìm kiếm giữ từ khóa, danh sách chỉ hiện kết quả khớp. Từ khóa vô nghĩa cho trạng thái không có kết quả. | mã SKU là gì sao người dùng biết mà nhập |
| [SHOP-06](#shop-06) | Mở **Menu cửa hàng → Khám phá**, lần lượt chọn **Danh mục**, **Thương hiệu** và **Loại da**. | Các link mở khu tương ứng của `/products` với mốc `#categories`, `#brands`, `#skin-type`. Bấm **Đóng menu** hoặc quay lại để kiểm tra điều hướng. | Đạt |
| [SHOP-07](#shop-07) | Mở **Bộ lọc và sắp xếp**, kết hợp từ khóa, danh mục, thương hiệu, loại da và thứ tự giá. Bấm **Áp dụng**. | Ở `/products` với các tham số đã chọn. Nhãn bộ lọc, số kết quả, thứ tự giá và thẻ sản phẩm khớp lựa chọn. | Đạt. Nhưng hãy sửa lại sao cho việc đóng mở Bộ lọc (1) đang áp dụng và sắp xếp sẽ không bị đóng tùy tiền mỗi lần áp dụng (hãy nghĩ ra phương pháp tối ưu, có thể bằng cookie hay sao đó tốt cho hệ thống) |
| [SHOP-08](#shop-08) | Xóa một nhãn bộ lọc, đổi **Trang sau/Trang trước** nếu có, rồi bấm **Xóa tất cả**. | Xóa một nhãn giữ các lựa chọn còn lại. Phân trang giữ lọc và thứ tự. **Xóa tất cả** về `/products` với mặc định. | Làm thêm Trang đầu/Trang cuối(Trang đầu có thêm Trang cuối, Trang giữa có thêm Trang đầu/Trang cuối, Trang cuối có thêm Trang đầu. xử lý theo tôi nó đã tối ưu chưa ) |
| [SHOP-09](#shop-09) | Bấm một thẻ sản phẩm từ trang chủ/catalog. Nếu dùng link hình tim trên thẻ, kiểm tra đích riêng. | Thẻ mở `/products/{slug}`. Link tim trên thẻ cuộn tới `#pdp-actions` của trang chi tiết. Đọc tên, thương hiệu, giá, ảnh hoặc ảnh thay thế khi thiếu. | Đạt |
| [SHOP-10](#shop-10) | Trên chi tiết, bấm từng biến thể rồi mở **Hướng dẫn sử dụng**, **Thành phần**, **Loại da phù hợp**. | Chọn SKU tải lại `/products/{slug}?variant={skuId}`. Kiểm tra SKU, tên biến thể, giá, ảnh và tồn cập nhật đúng. Ba phần thông tin mở tại chỗ. SKU hết hàng có nút vô hiệu hóa. | Đạt |
| [SHOP-11](#shop-11) | Cuộn tới **Đánh giá sản phẩm**, dùng phân trang nếu có đủ đánh giá. | Thấy sao trung bình, phân bố sao và đánh giá được duyệt hoặc thông báo chưa có đánh giá. Phân trang giữ SKU và cuộn về `#danh-gia`. | Chưa chạy: vì không đủ dữ liệu test |
| [SHOP-12](#shop-12) | Kiểm tra link footer: **Giới thiệu cửa hàng**, **Thương hiệu**, **Hướng dẫn mua hàng**, **Chính sách thanh toán**, **Chính sách bảo mật thông tin**, các link mạng xã hội. | Hiện tại các link lần lượt dẫn tới `/`, `/products#brands`, `/products`, `/products`, `/`. Link Facebook, Instagram, Zalo mở trang ngoài trong tab mới. Email và hotline đang hiển thị dưới dạng văn bản. | HƯỚNG DẪN MUA HÀNG Hướng dẫn mua hàng Chính sách thanh toán Chính sách bảo mật thông tin ĐỒNG HÀNH CÙNG BẠN Khám phá sản phẩm mới ĐỊA CHỈ CỬA HÀNG Khám phá cửa hàng Facebook Instagram Zalo chưa xử lý các trang chuyển. Tư vấn sản phẩm(trùng với Tư vấn làm đẹp) |
| [SHOP-13](#shop-13) | Kiểm tra ảnh thay thế trên thẻ sản phẩm thiếu ảnh và nội dung trang chủ khi bật chế độ giảm chuyển động của hệ điều hành hoặc trình duyệt. | Thẻ vẫn có tên, giá, trạng thái tồn và link. Nội dung trang chủ vẫn đọc được với chuyển động giảm. Nút video chỉ hiện ở chỗ có media phù hợp; giao diện mặc định có thể chỉ hiện ảnh hoặc minh họa tĩnh. | Chưa chạy |

## 3. Giỏ hàng, đăng ký và hồ sơ Customer

| Mã | Thao tác | Đi đâu và kiểm tra gì | Kết quả |
|---|---|---|---|
| [TK-01](#tk-01) | Khi là Khách, chọn SKU còn hàng rồi bấm **Thêm vào giỏ**. Thêm lần nữa cùng SKU, sau đó thêm một biến thể khác. | Mỗi lần về `/cart`. Cùng SKU tăng số lượng trên một dòng. SKU khác có dòng riêng. Kiểm tra thông báo, số lượng trên biểu tượng giỏ, đơn giá, thành tiền và tạm tính. | Đạt |
| [TK-02](#tk-02) | Bấm **+/−**, nhập số lượng và bấm **Cập nhật** nếu nút có trên trang. Thử số vượt khả dụng. | Giữ ở `/cart`. Tăng/giảm hợp lệ cập nhật dòng và tổng. Yêu cầu vượt tồn bị chặn hoặc báo lỗi. Sau khi tải lại, kiểm tra số lượng đã lưu. | Đạt |
| [TK-03](#tk-03) | Bấm **Xóa** ở một dòng, thử **Quay lại** rồi thử **Xác nhận** trong hộp thoại khi JavaScript bật. | Quay lại giữ dòng. Xác nhận xóa dòng và cập nhật tổng. Khi giỏ rỗng, **Khám phá sản phẩm** dẫn tới `/products`. Link tên sản phẩm trong giỏ trở về đúng biến thể. | Đạt |
| [TK-04](#tk-04) | Giữ lại một SKU trong giỏ Khách. Bấm **Đăng nhập để thanh toán**. | Link vào `/checkout` và chuyển Khách sang `/login`. Sau khi đăng nhập Customer, quay lại checkout đã yêu cầu. Giỏ Khách được gộp đúng một lần vào giỏ Customer và số lượng được kiểm lại theo tồn. Tải lại hoặc đăng nhập lại không cộng thêm dòng đã gộp. | Ở đây khi đăng nhập đã xử lý race condition chưa (nếu người dùng đã có 1 số lượng sản phẩm trong giỏ hàng khi gộp lại nếu vượt quá thì xử lý sao, có mô tả cho người dùng chưa). Khi đăng nhập nó gộp lại thì khi đăng xuất ra nó mất hết giỏ hàng rồi đúng không |
| [TK-05](#tk-05) | Bấm **Tạo tài khoản**, thử form thiếu trường, email sai hoặc email đã dùng, rồi đăng ký với email test mới. | Form lỗi giữ ở `/register` và báo cạnh trường liên quan. Đăng ký đúng chuyển sang `/login?registered`. Tài khoản mới đăng nhập theo vai trò Customer. | Đạt |
| [TK-06](#tk-06) | Trên `/login`, bấm **Hiện mật khẩu** rồi ẩn lại. Thử sai mật khẩu, sau đó đăng nhập đúng bằng link đăng nhập trực tiếp. | Nút đổi chế độ hiển thị trong ô. Sai mật khẩu ở `/login?error`. Customer vào trang đã yêu cầu trước đó hoặc trang mặc định khi đăng nhập trực tiếp. Header đổi theo phiên. | Ẩn/Hiện mật khẩu ở cả 2 /login và /register đang bị Layout Shift (dịch chuyển bố cục), sửa lại |
| [TK-07](#tk-07) | Bấm **Tài khoản → Lưu hồ sơ**. Sửa tên/số điện thoại bằng dữ liệu giả lập, lưu rồi mở lại. | Ở `/account`. Hiện thông báo đã cập nhật và giá trị mới. Email hiển thị để nhận diện tài khoản. Form có dữ liệu không hợp lệ báo lỗi. | Đạt |
| [TK-08](#tk-08) | Từ hồ sơ bấm **Quản lý địa chỉ giao hàng → Thêm địa chỉ**. Tạo hai địa chỉ, chọn một địa chỉ mặc định, rồi **Sửa** và **Xóa** một địa chỉ test. | Form tạo `/account/addresses/new`, form sửa `/account/addresses/{id}/edit`. Lưu và xóa quay về `/account/addresses`. Khi còn địa chỉ, có một địa chỉ mặc định. Xóa mặc định thì một địa chỉ còn lại được chọn. | Đạt |
| [TK-09](#tk-09) | Ở trang chi tiết sản phẩm, bấm **Lưu yêu thích**. Bấm tên sản phẩm rồi quay lại và **Bỏ yêu thích**. | Lưu mở `/account/wishlist`. Tên sản phẩm đang bán mở trang chi tiết. Bỏ yêu thích ở lại danh sách và mục biến mất. Khách chỉ thấy **Đăng nhập để lưu yêu thích**. | Lưu yêu thích khi quay lại chi tiết sản phẩm vẫn Lưu yêu thích, hãy sửa nếu đã yêu thích thì sẽ thành Bỏ yêu thích, khi bấm yêu thích cũng không còn nhảy vào danh sách yêu thích. Khi đã đăng nhập bấm vào nút Yêu thích (Mở sản phẩm để lưu yêu thích) phải được sửa thành nút Yêu thích (Yêu thích sản phẩm) và bấm vào sẽ chuyển sang tim màu đỏ(tương ứng với đã yêu thích, bấm lần nữa sẽ bỏ yêu thích) |
| [TK-10](#tk-10) | Từ **Tài khoản**, bấm **Xem đơn hàng của bạn**, **Sản phẩm yêu thích**, **Yêu cầu đổi trả**. Sau đó đăng xuất bằng biểu tượng tài khoản hoặc nút trong menu nếu hiện. | Các link mở `/orders`, `/account/wishlist`, `/account/returns`. Đăng xuất về `/?logout`. Mở lại một trang riêng tư bằng URL thì chuyển vào đăng nhập. | Đạt |

## 4. Customer · Checkout, COD và lịch sử đơn

| Mã | Thao tác | Đi đâu và kiểm tra gì | Kết quả |
|---|---|---|---|
| [DH-01](#dh-01) | Bấm **Tiến hành thanh toán** trong giỏ. Kiểm tra khi có giỏ và địa chỉ, rồi thử riêng với một Customer chưa có địa chỉ. | Vào `/checkout`. Có SKU, số lượng, tạm tính và danh sách địa chỉ của chính Customer. Nếu thiếu địa chỉ, **Thêm địa chỉ giao hàng** mở `/account/addresses/new`. Giỏ trống có link về `/cart`. | Đạt |
| [DH-02](#dh-02) | Chọn địa chỉ. Bấm **Cập nhật báo giá** với ô voucher trống, rồi thử mã hợp lệ. | Trình duyệt gửi tới `/checkout/quote` và hiện lại màn hình checkout. Kiểm tra địa chỉ, mã được dùng, giảm giá, phí và tổng dự kiến. Phí là **25.000 ₫** nếu tiền hàng **sau voucher** dưới **500.000 ₫**, miễn phí từ ngưỡng đó. Báo giá chưa tạo đơn. | Đạt |
| [DH-03](#dh-03) | Thử mã sai, chưa tới giờ, hết hạn, hết lượt hoặc chưa đủ tạm tính nếu có mẫu. Xóa ô voucher và báo giá lại. Sau khi có báo giá hợp lệ, đổi địa chỉ hoặc nội dung voucher khi bật JavaScript. | Mã không hợp lệ báo lỗi và bỏ số tiền chưa xác nhận. Xóa mã rồi báo giá lại cho kết quả không dùng voucher. Khi đổi thông tin đã báo giá, trang yêu cầu cập nhật lại trước khi bấm đặt đơn. | Chưa chạy: do chưa có các mã voucher chưa tới giờ, hết hạn, hết lượt hoặc chưa đủ tạm tính |
| [DH-04](#dh-04) | Với báo giá đúng, bấm **Đặt đơn · Thanh toán khi nhận hàng** một lần. Sau khi trang chuyển, tải lại chi tiết. | Vào `/orders/{id}`, báo đã đặt COD. Đơn **Chờ xác nhận**, **Chưa thu tiền COD**. Tên người nhận, địa chỉ, SKU, giá snapshot, giảm giá, phí và tổng đúng lúc đặt. Tải lại không tạo đơn mới. Giỏ đã đặt được xóa. | Đạt |
| [DH-05](#dh-05) | Bấm **Quay lại đơn hàng**, rồi bấm mã đơn vừa tạo. Mở cùng URL bằng Customer B. | Customer A về `/orders`, thấy đơn và mở lại được chi tiết. Customer B không xem được đơn của A, dự kiến trả 404 thay vì hiện thông tin đơn. | Đạt. Nhưng đang hiện 403, người dùng sẽ biết có cái đơn mà không được quyền xem, ngoài 404, 403 ra còn các trang nào(nếu để 403 có thể lộ thông tin là đã có cái đơn đó). Tôi muốn biết ngoài 403,404 còn lỗi trang nào nữa |
| [DH-06](#dh-06) | Trên đơn COD **Chờ xác nhận** hoặc **Đã xác nhận** chưa thu tiền, bấm **Hủy đơn**. Dùng một đơn khác để kiểm tra sau khi đã đóng gói. | Ở `/orders/{id}`, đơn hợp lệ chuyển **Đã hủy**, tồn khả dụng và lượt voucher hoàn một lần. Đơn đã đóng gói không còn nút hủy. | Đạt |
| [DH-07](#dh-07) | Tạo một đơn COD mới và xử lý theo **AD-05**. Sau mỗi bước, quay lại Customer A và tải lại chi tiết. | Trạng thái theo thứ tự **Chờ xác nhận → Đã xác nhận → Đang đóng gói → Đang giao hàng → Đã giao**. Chỉ bước giao thành công mới hiện **Đã thu tiền COD** và mở các link sau mua. | Chưa chạy |
| [DH-08](#dh-08) | Dùng hai phiên test cho một SKU còn đúng một đơn vị. Chuẩn bị checkout ở cả hai, sau đó đặt COD gần cùng lúc. Nếu muốn test lượt voucher cuối, dùng hai SKU khác nhau và một mã còn một lượt. | Chỉ một đơn có thể lấy đơn vị tồn hoặc lượt voucher cuối. Phiên bị chặn hiện lỗi để thử lại, không có đơn dư, tồn âm hay voucher bị dùng vượt lượt. | Chưa chạy |

## 5. Customer · Đánh giá và đổi trả

| Mã | Thao tác | Đi đâu và kiểm tra gì | Kết quả |
|---|---|---|---|
| [SM-01](#sm-01) | Mở một đơn chưa giao, rồi mở đơn **Đã giao** do Customer A mua. Bấm **Đánh giá hoặc sửa đánh giá** ở đúng dòng sản phẩm. | Link đánh giá chỉ hiện sau giao. Trang `/orders/items/{id}/review` hiện tên mục đã mua, 1–5 sao, ô bình luận và bộ đếm tối đa 1.000 ký tự. | Đạt |
| [SM-02](#sm-02) | Chọn sao, nhập bình luận test và bấm **Gửi đánh giá**. Mở lại trang sản phẩm bằng phiên Khách. | Quay về `/orders/{id}`, báo gửi để duyệt. Bài chờ duyệt chưa hiện công khai. Dùng **AD-10** duyệt, sau đó kiểm tra bài, nhãn **Đã mua hàng**, số lượt đánh giá, sao trung bình và phân bố sao. | Đạt |
| [SM-03](#sm-03) | Từ đúng mục đó, mở form đánh giá lần nữa, sửa sao hoặc bình luận rồi bấm **Lưu và gửi duyệt lại**. | Form hiện bản cũ để sửa. Sau khi lưu, quay về đơn. Chỉ có một đánh giá cho dòng đã mua. Bản sửa chờ duyệt và tạm biến mất ở trang công khai cho đến khi Admin duyệt lại. | Đạt |
| [SM-04](#sm-04) | Trên đơn đã giao trong 7 ngày, bấm **Yêu cầu đổi trả**. Nhập số lượng cho ít nhất một dòng và lý do, rồi bấm **Gửi yêu cầu**. | Form `/orders/{id}/returns/new` hiển thị số lượng tối đa từng dòng. Gửi đúng chuyển tới `/account/returns/{id}` với trạng thái **Chờ duyệt**. Link quay lại mở `/account/returns`. | Đạt |
| [SM-05](#sm-05) | Thử yêu cầu quá số lượng còn lại, không chọn dòng hàng hoặc mở yêu cầu trên đơn đã giao quá 7 ngày. Dùng Customer B mở chi tiết yêu cầu của A. | Form chặn đầu vào sai. Đơn quá 7 ngày báo hết thời hạn và không hiện form gửi. Customer B không đọc được yêu cầu của A. | Đạt |
| [SM-06](#sm-06) | Sau khi Admin xử lý ở **AD-11**, mở lại **Tài khoản → Yêu cầu đổi trả**. | Danh sách và chi tiết hiển thị trạng thái mới. Nếu yêu cầu bị từ chối, kiểm tra số lượng có thể yêu cầu lại khi đơn vẫn còn trong hạn. | Đạt |

## 6. Admin · Điều hướng và vận hành

| Mã | Thao tác | Đi đâu và kiểm tra gì | Kết quả |
|---|---|---|---|
| [AD-01](#ad-01) | Đăng nhập trực tiếp bằng Admin, rồi mở `/admin`. Mở một URL Admin hợp lệ từ cửa sổ chưa đăng nhập và đăng nhập lại. | Đăng nhập trực tiếp và `/admin` tới `/admin/dashboard`. Nếu bắt đầu từ một trang Admin hợp lệ đã yêu cầu, đăng nhập có thể đưa lại đúng trang đó. | Đạt |
| [AD-02](#ad-02) | Dùng sidebar. Ở màn hình dưới 1100 px, mở **Menu quản trị**. Bấm **Về cửa hàng** và thử đăng xuất. | Menu có **Tổng quan**, **Sản phẩm & biến thể**, **Danh mục & thương hiệu**, **Đơn hàng**, **Khách hàng**, **Khuyến mãi**, **Đánh giá**, **Trả hàng**, **Tri thức tư vấn**. Mục đang mở được đánh dấu, kể cả ở trang con. **Về cửa hàng** mở `/`. | Đạt |
| [AD-03](#ad-03) | Bấm **Tổng quan** → **7 ngày**, **30 ngày**, **90 ngày**. Chọn kỳ riêng bằng **Xem kỳ này** và thử ngày bắt đầu sau ngày kết thúc. | Ở `/admin/dashboard`. Kỳ nhanh đổi tham số `range`, kỳ riêng dùng `from` và `to`. Kỳ sai hiện thông báo và kỳ mặc định. Kiểm tra đủ 8 KPI, so sánh kỳ trước và ghi chú doanh thu theo ngày thu, số đơn theo ngày đặt. | Đạt |
| [AD-04](#ad-04) | Mở **Xem dạng bảng** dưới mỗi biểu đồ. Bấm **Xem đơn chờ xử lý**, KPI **SKU sắp hết**, thay ngưỡng và bấm **Áp dụng**. Thử các link trong bảng. | Giá trị biểu đồ khớp bảng. **Xem đơn chờ xử lý** mở danh sách đã lọc. KPI tồn cuộn tới danh sách cùng ngưỡng, **Sửa SKU** mở form. Mã đơn và cảnh báo MoMo mở chi tiết đơn. **Xử lý** mở yêu cầu trả hàng. Mã voucher mở form sửa. Tên sản phẩm top 5 mở trang sản phẩm nếu đang hiển thị, hoặc trang sửa nếu đã ẩn. Khu trống hiện thông báo thay cho dòng giả. | Đạt |
| [AD-05](#ad-05) | Bấm **Đơn hàng** → mã đơn vừa tạo. Bấm các nút tiếp theo, lần lượt **Đã xác nhận**, **Đang đóng gói**, **Đang giao hàng**, **Xác nhận đã giao và thu tiền**. | Danh sách ở `/admin/orders`, mã đơn mở `/admin/orders/{id}`. Sau mỗi lần gửi, về chi tiết và chỉ còn nút chuyển trạng thái hợp lệ. Đơn COD cuối cùng **Đã giao** và **Đã thu tiền COD**. Kiểm tra doanh thu dashboard sau giao. | chưa có các bộ lọc Đã xác nhận, Đang đóng gói, Đang giao hàng, Xác nhận đã giao và thu tiền |
| [AD-06](#ad-06) | Từ danh sách đơn đã lọc, bấm **Xem tất cả đơn**. Dùng một đơn COD test chưa đóng gói để bấm **Hủy đơn**. | **Xem tất cả đơn** bỏ lọc. Hủy hợp lệ quay về chi tiết với **Đã hủy**. Khách thấy cùng kết quả. Kiểm tra tồn/lượt voucher hoàn, không lấy đơn vừa hủy để tiếp tục giao. | chưa có Xem tất cả đơn vì hiện tại lúc nào cũng hiện tất cả đơn |
| [AD-07](#ad-07) | Bấm **Danh mục & thương hiệu**, đổi tab giữa **Danh mục**, **Thương hiệu**, **Sản phẩm**. Bấm **Thêm mới**, **Sửa**, **Lưu**. Thử đường dẫn trùng và xóa một mục trống, rồi thử một mục đang được sản phẩm sử dụng. | Các danh sách là `/admin/catalog/categories`, `/admin/catalog/brands`, `/admin/catalog/products`. Form thêm/sửa nằm dưới list tương ứng. Lưu đúng về danh sách, dữ liệu sai báo lỗi. Xóa được mục chưa có sản phẩm tham chiếu. Mục đang dùng bị chặn xóa. | Đạt |
| [AD-08](#ad-08) | Bấm **Sản phẩm & biến thể**. Tạo một sản phẩm test có danh mục và thương hiệu hợp lệ. Bấm **SKU → Thêm SKU**, nhập mã, tên biến thể, giá, tồn, ảnh và thông tin lô nếu cần. Sau đó thử **Sửa**, **Xóa ảnh hiện có**, **Ngừng bán**, và **Ngừng hoạt động** sản phẩm test. | Danh sách SKU ở `/admin/catalog/products/{id}/variants`. Lưu về danh sách, SKU trùng hoặc dữ liệu sai bị chặn. Ảnh PNG/JPEG hợp lệ tối đa 2 MB và 2048 × 2048 px hiển thị ở nơi dùng SKU đó. Để trống ảnh khi sửa giữ ảnh cũ, tích xóa sẽ bỏ ảnh. SKU ngừng bán không còn chọn công khai, sản phẩm ngừng hoạt động không còn hiện ở catalog. | Đạt |
| [AD-09](#ad-09) | Bấm **Khách hàng**, tìm Customer test, chuyển trang nếu có, rồi **Khóa** và **Mở khóa** tài khoản đó. Giữ một phiên đã đăng nhập của Customer test để kiểm tra. | Ở `/admin/users`. Tìm và chuyển trang giữ bộ lọc. Khóa/mở khóa giữ màn đang xem. Tài khoản bị khóa không đăng nhập được và phiên cũ mất quyền. Khi mở khóa, Customer phải đăng nhập lại. Admin đang dùng không thể tự khóa. | Đạt |
| [AD-10](#ad-10) | Bấm **Đánh giá**. Lọc trạng thái, số sao và sản phẩm. Bấm **Xem đơn mua**, quay lại, rồi bấm **Duyệt đánh giá** hoặc **Ẩn đánh giá**. | `/admin/reviews` hiển thị đúng bộ lọc. **Xem đơn mua** mở chi tiết đơn Admin. Duyệt/ẩn về danh sách, giữ bộ lọc và số trang đã gửi. Mục có thể rời danh sách nếu trạng thái mới khác bộ lọc. Kiểm tra lại hiển thị công khai trên trang sản phẩm. Với MongoDB, thử giữ form cũ ở một tab, sửa bài ở phiên Customer rồi duyệt từ tab cũ. App yêu cầu kiểm tra lại bản mới. | Nút Duyệt đánh giá/Ẩn đánh giá lỗi Layout Shift (dịch chuyển bố cục), sửa  |
| [AD-11](#ad-11) | Bấm **Trả hàng**, mở yêu cầu chờ duyệt. Dùng một yêu cầu để chọn **Đã duyệt → Đã nhận hàng → Đã hoàn tất**. Dùng yêu cầu khác thử **Đã từ chối**. | List `/admin/returns`, chi tiết `/admin/returns/{id}`. Bấm trạng thái quay về chi tiết. Nút tiếp theo phụ thuộc trạng thái. Đã hoàn tất hoặc bị từ chối thì không còn nút chuyển tiếp. Xử lý yêu cầu không tự nhập lại tồn hoặc tự hoàn tiền. | Đạt |
| [AD-12](#ad-12) | Bấm **Khuyến mãi → Tạo voucher**. Tạo một mã số tiền cố định và một mã phần trăm có trần. Nhập thời gian theo **UTC**, giới hạn lượt và tạm tính tối thiểu. Dùng Customer báo giá rồi đặt một đơn với mã test. | Mở form `/admin/vouchers/new`. **Lưu voucher** về `/admin/vouchers`. Báo giá tuân thủ giá trị giảm, trần, ngưỡng, thời gian, lượt dùng. Đặt COD tăng lượt dùng, chỉ báo giá thì chưa dùng lượt. | Đạt |
| [AD-13](#ad-13) | Ở danh sách voucher, bấm **Sửa**, thử trùng mã, thời gian kết thúc trước bắt đầu và hạ giới hạn dưới số đã dùng/đang giữ. Bỏ chọn **Đang bật**. Thử **Xóa nếu chưa sử dụng** trên mã test mới và mã đã dùng. | Sửa ở `/admin/vouchers/{id}/edit`. Dữ liệu không hợp lệ báo lỗi trên form. Mã tắt không dùng được khi báo giá. Chỉ xóa được mã chưa có lịch sử hoặc tham chiếu. | Đạt |
| [AD-14](#ad-14) | Bấm **Tri thức tư vấn**. Thử một lượt khi AI tắt và, nếu có Ollama, làm thêm **TV-07**. | Vào `/admin/ai`. Khi AI tắt, trang báo provider đang tắt. Khi bật đúng cấu hình, trang có số đoạn, thời điểm nạp khi có và nút **Nạp lại FAQ và catalog**. | Chưa chạy |
| [AD-15](#ad-15) | Với danh mục và thương hiệu chỉ dùng cho test, dùng **Sửa** để bỏ **Đang hoạt động** trên từng mục rồi bật lại. Sau mỗi lần lưu, tìm sản phẩm liên quan bằng phiên Khách. | Sản phẩm đang hoạt động vẫn bị ẩn khỏi catalog nếu danh mục hoặc thương hiệu của nó ngừng hoạt động. Sản phẩm có SKU hoạt động xuất hiện lại sau khi đủ điều kiện công khai. | Danh mục và Thương hiệu sửa Xóa thành Ngừng hoạt động tương tự Sản phẩm (xóa luôn cả dòng code xóa Danh mục và Thương hiệu, chỉ cho Ngừng hoạt động) |

### Đối chiếu số liệu ở Tổng quan

1. Trước và sau **AD-05**, ghi lại số đơn, doanh thu thực thu và số đơn đã giao trong cùng kỳ. Đơn COD mới làm tăng **Số đơn** theo ngày đặt, rồi mới tăng **Doanh thu thực thu** khi Admin thu tiền. Đơn đã hủy vẫn nằm trong tập **Số đơn** và được tính vào tỷ lệ hủy.
2. Khi dữ liệu có đủ, mở bảng của **Doanh thu và số đơn theo ngày**, **Đơn hàng theo trạng thái**, **Top 5 sản phẩm bán chạy**, **Doanh thu theo thương hiệu**, **Doanh thu theo danh mục**, **Phương thức thanh toán**, **Trạng thái thanh toán theo phương thức**, **Phân bố số sao đánh giá**. So sánh cột, đường và số trong bảng. Top 5, danh mục và thương hiệu tính doanh thu dòng trước voucher và phí giao hàng. **Chọn MoMo** phản ánh cách đặt đơn, còn nhóm thành công thể hiện kết quả thanh toán.
3. Kiểm tra danh sách 8 đơn mới nhất, SKU sắp hết, tối đa 5 yêu cầu trả hàng chờ duyệt, cảnh báo MoMo và voucher sắp hết hạn. Với voucher còn lượt, đối chiếu giới hạn trừ lượt đã dùng và lượt đang giữ. [Định nghĩa Dashboard và trường hợp biên](../reviews/P20C-dashboard-v2.md)

## 7. Thanh toán MoMo

Test **MO-01** với cấu hình mặc định. Với **MO-02 đến MO-06**, bật `MOMO_ENABLED=true`, `MOMO_MODE=mock` trên profile `dev`, rồi khởi động lại app trên dữ liệu demo. Chạy cùng một lần khởi động vì kết quả của cổng mock nằm trong bộ nhớ. Thực hiện mỗi nhánh trên một đơn riêng, trừ bước thử lại đã ghi rõ. [Hướng dẫn MoMo mock và Sandbox](../deployment/04-momo-sandbox.md)

| Mã | Thao tác | Đi đâu và kiểm tra gì | Kết quả |
|---|---|---|---|
| [MO-01](#mo-01) | Mở checkout khi `MOMO_ENABLED=false`. | Chỉ có COD. Vẫn báo giá và đặt COD bình thường. | Đạt |
| [MO-02](#mo-02) | Trong chế độ mock, cho một SKU vào giỏ, chọn địa chỉ, báo giá rồi bấm **Đặt đơn MoMo · MÔ PHỎNG - KHÔNG THU TIỀN**. | Mở `/payment/momo/mock/{id}`, có nhãn mô phỏng. **Quay lại đơn hàng** mở `/orders/{id}` và đơn **Chờ thanh toán**. Tồn và lượt voucher được tạm giữ, chưa ghi thu tiền. | Chưa chạy |
| [MO-03](#mo-03) | Trên trang mock, bấm **Mô phỏng thành công**. Tải lại chi tiết và xem **Tổng quan** bên Admin. | Về `/orders/{id}`, đơn **Chờ xác nhận** và hiện **Đã xác minh thanh toán MoMo**. Kiểm tra giữ hàng đã được chốt và doanh thu trong kỳ. Admin có thể xử lý giao đơn. | Chưa chạy |
| [MO-04](#mo-04) | Với đơn mock mới, bấm **Mô phỏng từ chối**, rồi **Mở lại thanh toán MoMo** trong thời gian còn giữ hàng. Trên lần mới bấm thành công. | Từ chối về chi tiết, đơn còn **Chờ thanh toán** nếu giữ hàng còn hiệu lực. Nút mở lại trở về trang mock của cùng đơn. Thành công xác minh thanh toán trên đơn đó. | Chưa chạy |
| [MO-05](#mo-05) | Với đơn khác, bấm **Mô phỏng cổng hết hạn**. | Về chi tiết với **Đã hết hạn**, không còn nút thử lại. Tồn khả dụng và lượt voucher đã giữ được trả. Đơn hết hạn không làm tăng doanh thu thực thu. | Chưa chạy |
| [MO-06](#mo-06) | Giữ một đơn **Chờ thanh toán** mà chưa chọn kết quả mock. Admin vào chi tiết và bấm **Đối soát MoMo**. | Quay lại `/admin/orders/{id}`. Khi cổng chưa có kết quả xác minh, trang thông báo chưa có kết quả và đơn vẫn chờ. Mốc giờ giữ hàng tự nó không xác nhận giao dịch đã thất bại. | Chưa chạy |
| [MO-07](#mo-07) | Chỉ chạy khi có merchant **Sandbox** và IPN HTTPS public theo hướng dẫn. Từ checkout bấm **Đặt đơn · MoMo Sandbox**, hoàn thành một lần thử nghiệm, rồi bấm **Xem đơn hàng** khi quay về. | Checkout mở cổng Sandbox. `/payment/momo/return` là trang thông báo, nút mở `/orders`. Xác nhận kết quả trong chi tiết đơn sau IPN hoặc đối soát. Thiếu điều kiện Sandbox thì ghi **Chặn**. | Chưa chạy |

## 8. Tư vấn sản phẩm, FAQ và AI

| Mã | Thao tác | Đi đâu và kiểm tra gì | Kết quả |
|---|---|---|---|
| [TV-01](#tv-01) | Bấm **Tư vấn** trên header hoặc **Hỏi BeautyShop** ở trang chủ. Mở `/chat` riêng bằng link **Tư vấn làm đẹp** hoặc **Tư vấn sản phẩm** ở footer. | Hai link đầu tới `/tu-van-da`, là form chọn loại da, độ nhạy cảm, nhu cầu và mô tả. Link footer tới `/chat`, là form hỏi về sản phẩm và chính sách. **Mở tư vấn chăm sóc da** trên `/chat` đưa sang `/tu-van-da`. | Đạt |
| [TV-02](#tv-02) | Trên `/tu-van-da`, bỏ trống một trường rồi bấm **Gửi thông tin tư vấn**. | Vẫn ở `/tu-van-da`. Trình duyệt hoặc form báo dữ liệu thiếu. Những lựa chọn đã nhập còn trên form. | Đạt |
| [TV-03](#tv-03) | Trên `/tu-van-da`, điền dữ liệu giả lập với nhu cầu về loại sản phẩm có SKU còn hàng. Bấm **Gửi thông tin tư vấn** khi AI tắt, rồi thử lại khi Ollama sẵn sàng. | Phản hồi nằm trên cùng trang. AI tắt và có sản phẩm ứng viên thì app báo chưa sẵn sàng. Khi AI hoạt động và chọn được ứng viên, so sánh tối đa ba gợi ý, giá, tồn và thông tin catalog. Nếu không có sản phẩm khớp loại yêu cầu, trang báo không tìm thấy. | Đạt |
| [TV-04](#tv-04) | Trên `/chat` khi AI tắt, hỏi **“Đổi trả trong bao lâu?”** rồi bấm **Gửi câu hỏi**. Bấm link **Nguồn** nếu hiện. | Ở `/chat`, thấy câu trả lời dựa trên FAQ và nhãn **Tìm FAQ · không phải AI**. Nguồn mở trang được ghi trong câu trả lời. | Đạt |
| [TV-05](#tv-05) | Trên `/chat`, hỏi một câu ngoài kho thông tin rồi thử form trống hoặc vượt giới hạn 500 ký tự. | Câu không có nguồn nhận phản hồi thiếu thông tin. Form sai báo lỗi và giữ trên trang. | Đạt |
| [TV-06](#tv-06) | Trong một phiên test riêng, gửi hơn sáu câu hỏi hợp lệ trong một phút ở `/chat`. | Lần vượt giới hạn báo thử lại sau một phút và trả HTTP 429. | Đạt |
| [TV-07](#tv-07) | Nếu đã có Ollama cùng model chat và embedding, bật AI theo hướng dẫn. Admin bấm **Nạp lại FAQ và catalog** trong `/admin/ai`, chờ có số đoạn. Thử câu hỏi có nguồn và câu hỏi ngoài nguồn ở `/chat`. | Sau khi bấm nạp, quay lại `/admin/ai` và thấy kết quả hoặc thông báo lỗi. Trang `/chat` trả kết quả có nguồn khi có đoạn phù hợp. Nếu có giá/tồn, đối chiếu catalog. Thiếu Ollama/model thì ghi **Chặn** cho nhánh AI thật và vẫn chạy **TV-04**. | Chưa chạy |

[Cấu hình Ollama, model và kho tri thức](../deployment/05-chatbot-rag.md)

## 9. Phân quyền, hiển thị và trạng thái lỗi

| Mã | Thao tác | Đi đâu và kiểm tra gì | Kết quả |
|---|---|---|---|
| [QA-01](#qa-01) | Dùng Khách mở `/account`, `/orders`, `/checkout`, `/admin/dashboard`. Sau đó thử với Customer đã đăng nhập. | Khách được đưa tới `/login`. Customer vào được các trang mua sắm cá nhân và bị 403 tại Admin. Admin đăng nhập có thể đọc catalog công khai, song trang riêng của Customer vẫn yêu cầu vai trò Customer. | Đạt |
| [QA-02](#qa-02) | Với hai tài khoản test, kiểm tra link đơn, yêu cầu đổi trả và trang sửa địa chỉ thuộc tài khoản khác. Giữ lại một URL sản phẩm test rồi ngừng hoạt động sản phẩm đó qua Admin. | Dữ liệu cá nhân không hiển thị cho tài khoản khác. URL sản phẩm đã ngừng hoạt động hoặc slug không tồn tại trả 404. Link quay về cửa hàng hoạt động. | Đạt |
| [QA-03](#qa-03) | Kiểm tra ở 390, 768, 1366 và 1920 px. Mở header, form checkout, chi tiết sản phẩm, footer, `/tu-van-da` và Admin dashboard. Thử phóng to 200%. | Ghi mọi vị trí bị cắt chữ, chồng nút, cuộn ngang toàn trang hoặc menu khó mở. Kiểm tra điều hướng mobile, nhãn form, thông báo lỗi và link footer theo vai trò. | |
| [QA-04](#qa-04) | Dùng Tab, Shift+Tab và Enter. Thử link **Đến nội dung chính**, mở menu, mở bộ lọc, chọn sao ở form đánh giá và hủy/xác nhận xóa dòng giỏ bằng bàn phím. | Tiêu điểm nhìn thấy được. Menu, form, nút và hộp thoại dùng được bằng bàn phím. Nếu link bỏ qua điều hướng không đưa tới nội dung chính ở trang nào, ghi rõ URL. | Đạt |
| [QA-05](#qa-05) | Tắt JavaScript trong một phiên test riêng rồi tải lại. Thử lọc catalog, chọn SKU, dùng **Cập nhật** giỏ, báo giá và đặt COD với đơn test, mở **Menu quản trị** và đọc bảng dashboard. | Các link và form cơ bản vẫn hoạt động. Nút bước nhanh của giỏ hoặc chuyển động trang chủ có thể biến mất. Dashboard mở bảng dữ liệu thay cho biểu đồ. Kiểm tra kỳ và tổng tiền theo cùng điều kiện như khi bật JavaScript. | Đạt |
| [QA-06](#qa-06) | Chỉ ở app test riêng, thử cấu hình MongoDB không kết nối được và mở trang chủ, catalog, chi tiết và Admin đánh giá. Nếu cần đối chiếu kho dự phòng, dùng profile test cấu hình `REVIEWS_STORE=mariadb`. | Phần đánh giá báo chưa tải được khi MongoDB lỗi, còn mua sắm cơ bản vẫn dùng được. Kho dự phòng đọc/ghi bảng đánh giá MariaDB, nên dữ liệu có thể khác kho MongoDB và bộ lọc chờ duyệt/đã ẩn có giới hạn. | Chưa chạy |
| [QA-07](#qa-07) | Trong form SKU test, thử ảnh định dạng ngoài danh sách hoặc quá 2 MB. Sau đó lưu bằng ảnh hợp lệ và mở ảnh từ trang sản phẩm. | Upload sai báo lỗi trên form hoặc trang lỗi dung lượng phù hợp. Upload đúng hiển thị ảnh. SKU test chưa lưu thành công không xuất hiện công khai. | Đạt |

## 10. Kiểm thử tự động bổ sung

Thao tác trình duyệt xác nhận luồng nghiệp vụ và màn hình. Các trường hợp như yêu cầu đồng thời, xử lý lặp và lỗi giao dịch có test tự động để đối chiếu.

| Muốn kiểm tra | Lệnh hoặc nhóm test |
|---|---|
| Unit test, MVC và tài nguyên trang | `mvn -o test` với JDK 21 khi đã có cache Maven. Nếu cache chưa đủ, dùng `mvn test`. |
| Flyway từ database trống, JPA, khóa SKU, gộp giỏ, COD, voucher, số liệu dashboard | Trỏ `DB_URL` tới **MariaDB test riêng**, đặt `P01_MARIADB_TEST=true` rồi chạy test. Xem `DonHangCOD_MariaDb_Test`, `XacThuc_MariaDb_Test`, `PhieuGiamGia_MariaDb_Test`, `ThongKe_MariaDb_Test`. |
| MongoDB và tương tác MariaDB → đánh giá → duyệt | Đặt cả `P01_MARIADB_TEST=true` và `P23_MONGO_TEST=true` với hai database test riêng. `MONGODB_TEST_URI` mặc định dùng `beautyshop_test_mongo`. Bộ test MongoDB có ca xóa collection trước khi chạy, nên giữ URI test tách khỏi dữ liệu demo. |
| MoMo IPN, giữ hàng, kết quả lặp và thành công muộn | `Momo_MariaDb_Test` trên MariaDB test với cổng mock. Dùng các test sẵn có để kiểm chữ ký sai, đối chiếu số tiền và ghi nhận đúng một lần. |
| Biểu đồ và sidebar trên Chrome có phiên Admin thật | Dùng mục chạy chế độ `admin` trong [báo cáo P20C](../reviews/P20C-dashboard-v2.md). Tài khoản Admin do người test tự đăng nhập trong Chrome. |

[Test plan và các ca TC01–TC20](01-test-plan.md) · [Bằng chứng chạy P10](02-p10-results.md) · [Checklist giao diện](../ui/QA-checklist.md)

## 11. Ghi nhận để báo lỗi hoặc đề xuất nâng cấp

Với mỗi mục cần báo lại, sao chép mẫu này và điền kết quả thực tế. Bạn có thể ghi **Đề xuất nâng cấp** dù thao tác hiện tại vẫn đạt.

```text
Mã ca
Kết quả (Đạt / Lỗi / Chặn / Chưa chạy)
Vai trò, trình duyệt và độ rộng màn hình
Trang bắt đầu và URL đích thực tế
Các nút đã bấm và thứ tự
Kết quả dự kiến
Kết quả thực tế
Mong muốn nâng cấp (nếu có)
Ảnh hoặc log (che thông tin đăng nhập và dữ liệu cá nhân)
```

## 12. Luồng kỹ thuật, dữ liệu, lỗi và đường quay lại theo từng ca

### Cách đọc phần kỹ thuật

- **DTO form** nhận dữ liệu từ ô nhập hoặc tham số. Một số form cũng được điền sẵn khi mở trang sửa, ví dụ `HoSo_Form`. **Projection** là giao diện chỉ lấy một số cột từ truy vấn. **Record đọc / view model** là dữ liệu đã được gom hoặc ánh xạ để trang hiển thị. Các record có thể nằm ngay trong Service hoặc Repository.
- **Entity vào Model** nghĩa là Controller đưa nguyên đối tượng JPA vào `Model` cho Thymeleaf đọc ở phía server. Trình duyệt nhận HTML đã dựng từ các trường mà template dùng. Ví dụ `CuaHang_Service.ChiTiet` là record nhưng bên trong vẫn chứa entity. Ngược lại, các bản ghi đơn hàng và voucher được ánh xạ từ SQL nên tên `Don` hoặc `Phieu` tự nó không làm record trở thành entity.
- **Quay lại** trong từng ca ưu tiên link của ứng dụng. Khi dùng nút **Back** của trình duyệt, trang đích phụ thuộc lịch sử của tab. Với màn hình được render trực tiếp sau POST, trình duyệt có thể hỏi gửi lại. Kiểm tra dữ liệu bằng cách mở lại URL GET khi muốn tránh lặp thao tác ghi. Thông báo chuyển hướng một lần có thể biến mất sau khi tải lại.
- Các trang lỗi dùng chung gồm **400** cho yêu cầu không hợp lệ, **403** “Bạn chưa có quyền truy cập”, **404** “Không tìm thấy trang”, **409** “Thông tin vừa thay đổi”, **413** cho tệp quá lớn và **500** “Chưa thể hoàn tất yêu cầu”. Khi app dựng các trang lỗi mặc định này, dùng **Xem sản phẩm** để về `/products` hoặc **Về trang chủ** để về `/`. Nội dung trang lỗi 409 là thông báo chung, có thể khác lý do chi tiết trong Service. [Các template lỗi](../../src/main/resources/templates/error)

**Tóm tắt các màn hình còn truyền entity vào Model**

| Màn hình | Entity trong Model | Phần đã ánh xạ |
|---|---|---|
| Chi tiết sản phẩm công khai | `SanPham`, danh sách `BienTheSanPham` và SKU đang chọn trong record `ChiTiet` | Đánh giá dùng `TrangDanhGiaSanPham` và `DanhGiaCongKhai` |
| Danh sách địa chỉ và checkout | `List<DiaChiNguoiDung>` | Form sửa dùng `DiaChiNguoiDung_Form`. Checkout dùng `Checkout_Form` và các dòng giỏ `MucGio` |
| Admin danh mục, thương hiệu, sản phẩm | Danh sách `DanhMuc`, `ThuongHieu` hoặc `SanPham`. Form sản phẩm nhận thêm danh sách danh mục và thương hiệu cho ô chọn | Dữ liệu chỉnh sửa bằng `DanhMucSanPhamThuongHieu_Form` |
| Admin danh sách và form SKU | `SanPham` trong list và form, `List<BienTheSanPham>` ở list | `BienTheSanPham_Form` trên form và URL ảnh hiện tại riêng |
| Tư vấn da | Repository đưa `BienTheSanPham` đã nạp quan hệ tới Service | Controller nhận `TuVanDaRequest` và đưa kết quả dạng chuỗi lên Model, không chuyển entity tới view |

### 12.1. Trang chủ và catalog

#### SHOP-01

**Luồng và dữ liệu.** `Bootstrap_Controller.home()` dựng trang chủ với `TrangChu_Service` cho nội dung nhóm, `CuaHang_Service` cho sản phẩm và `GoiYSanPham_Service` cho danh sách phổ biến. Thẻ lấy projection `CuaHang_Repository.TheSanPham` từ MariaDB gồm mã, slug, tên, thương hiệu, giá từ, URL ảnh và tồn khả dụng lớn nhất. `SauMua_Service` gom `TomTatDanhGia` theo mã sản phẩm cho các thẻ.

**Lỗi và quay lại.** Mỗi phần đọc dữ liệu ở trang chủ có fallback riêng nếu không có dữ liệu hoặc gặp `DataAccessException`. Lỗi đánh giá có thể chỉ làm thẻ mất số sao. Logo và link **Trang chủ** luôn dẫn về `/`, không giữ lọc từ catalog.

#### SHOP-02

**Luồng và dữ liệu.** `Bootstrap_Controller` đưa dữ liệu lên `home/index.html` một lần. Swiper, `home-carousel.js` và `home-story.js` chỉ điều khiển các chương, chuyển động và media ở trình duyệt. Các nút đổi chương không gửi form, không thêm DTO hay truy vấn DB.

**Lỗi và quay lại.** Khi carousel không chạy hoặc không được dùng ở bố cục hiện tại, xem nội dung bằng cách cuộn trang. Điều hướng chương thường giữ pathname `/`. Nếu đã bấm CTA sang trang khác, **Back** của trình duyệt trở về mục lịch sử trước đó, còn logo mở lại trang chủ.

#### SHOP-03

**Luồng và dữ liệu.** `TrangChu_Service` dựa vào dữ liệu catalog từ `CuaHang_Repository` để tạo record `LoaiDa(ten, giaTriLoc, soSanPham, moTa)`, `BuocNghiThuc(so, ten, moTa, duongDanDanhMuc)` và `HoatChat(ten, tuKhoa, moTa, soSanPham)`. Thymeleaf dùng giá trị lọc để tạo link GET, không chuyển entity hoặc form giữa hai trang.

**Lỗi và quay lại.** Bước nghi thức thiếu danh mục vẫn có nội dung nhưng không có link danh mục. Bộ lọc không khớp cho trạng thái trống ở catalog. Dùng **Back** về trang chủ hoặc mở menu để chọn bộ lọc khác.

#### SHOP-04

**Luồng và dữ liệu.** `Bootstrap_Controller` nhận `sort` và chỉ công nhận `newest`, `priceAsc`, `priceDesc`. `CuaHang_Service` đưa thứ tự xuống `CuaHang_Repository`, lấy trang đầu tối đa 12 projection `TheSanPham`. Khu phổ biến và nổi bật có nguồn riêng, không sắp lại theo nút của khu **Tất cả sản phẩm**.

**Lỗi và quay lại.** Giá trị `sort` lạ dùng `newest`. Mốc `#home-all-products` cuộn tới danh sách. Bấm **Xem tất cả sản phẩm** tạo đường dẫn mới tới catalog và chỉ giữ thứ tự nếu là giá tăng/giảm.

#### SHOP-05

**Luồng và dữ liệu.** Form tìm kiếm ở header gửi GET với `q`. `CuaHang_Controller` cắt khoảng trắng và giới hạn từ khóa 160 ký tự theo code point. `CuaHang_Service` chuẩn bị mẫu tìm kiếm và `CuaHang_Repository` tìm trên catalog công khai, gồm tên và SKU. View nhận `Page<TheSanPham>` cùng chuỗi lọc.

**Lỗi và quay lại.** Từ khóa không khớp trả danh sách trống, không phải trang 404. Ô lọc trong catalog giữ từ khóa; ô tìm kiếm dùng chung ở header là ô nhập riêng. **Xem tất cả sản phẩm** trong trạng thái đã lọc mở `/products` sạch. Logo đưa về `/`.

#### SHOP-06

**Luồng và dữ liệu.** `GiaoDien_Advice` gắn `uiPath`, Thymeleaf dùng nó để đánh dấu menu hiện tại. Các mục **Khám phá** là `details`/link HTML với mốc trong catalog. Mở hoặc đóng menu không qua Service và không có DTO.

**Lỗi và quay lại.** Mở mốc không đổi bộ lọc hay tự bấm **Áp dụng**. Đóng menu chỉ thu giao diện tại chỗ. **Back** phụ thuộc mốc và URL vừa mở. Từ trang khác có thể dùng **Trang chủ** hoặc **Sản phẩm** để đi theo link rõ ràng.

#### SHOP-07

**Luồng và dữ liệu.** `CuaHang_Controller` nhận `q`, `brand`, `category`, `skinType`, `sort`, `page` và `size` bằng `@RequestParam`, không có form DTO riêng. `CuaHang_Repository` lọc SKU, sản phẩm, danh mục và thương hiệu đang hoạt động, gom theo sản phẩm và tính giá từ trên các SKU khớp. Model nhận `Page<TheSanPham>`, danh mục và thương hiệu dạng `CuaHang_Service.TuyChon`, cùng bản đồ tóm tắt sao.

**Lỗi và quay lại.** Tổ hợp không khớp hiện trạng thái trống. Thứ tự lạ rơi về **Mới nhất**. `page` hoặc `size` không đọc được thành số có thể trả 400 trước Controller. Mở `/products` để bỏ các tham số test.

#### SHOP-08

**Luồng và dữ liệu.** Thymeleaf dựng link xóa từng nhãn và phân trang từ các tham số trong Model và số trang của `Page<TheSanPham>`. Phân trang bắt đầu từ `0`, mặc định 12 phần tử, kích thước hợp lệ bị giới hạn 1–48. Trang vượt số trang sẽ được đọc lại ở trang cuối phù hợp ngay trong Controller.

**Lỗi và quay lại.** Đọc lại trang cuối không nhất thiết đổi URL đã gõ. **Xóa tất cả** đi thẳng `/products`. **Back** có thể phục hồi bộ lọc trước đó từ URL lịch sử.

#### SHOP-09

**Luồng và dữ liệu.** Thẻ hiển thị từ projection `TheSanPham`, còn `CuaHang_Controller.chiTiet()` gọi Service lấy `ChiTiet`. Bên trong `ChiTiet` có nguyên entity `SanPham` và `BienTheSanPham`. `CuaHang_Repository` nạp sẵn danh mục, thương hiệu của sản phẩm. Link hình tim trên thẻ chỉ điều hướng, chưa ghi yêu thích.

**Lỗi và quay lại.** Slug sai, sản phẩm không còn công khai hoặc không còn SKU hoạt động trả 404. Link **Tất cả sản phẩm** trên chi tiết về `/products` và không khôi phục bộ lọc cũ. Dùng **Back** nếu cần thử quay về đúng URL catalog trước đó.

#### SHOP-10

**Luồng và dữ liệu.** Nút biến thể gửi GET `variant`. `CuaHang_Service` chỉ chọn từ entity SKU đang hoạt động thuộc sản phẩm đó. Nếu không truyền ID thì chọn SKU khả dụng rẻ nhất, rồi chọn SKU hoạt động rẻ nhất khi tất cả hết hàng. Template đọc giá, tồn khả dụng, mô tả, thành phần và hướng dẫn từ entity ở server.

**Lỗi và quay lại.** SKU sai hoặc thuộc sản phẩm khác trả 404. SKU hoạt động nhưng hết tồn vẫn mở được trang với nút vô hiệu hóa. Các mục mô tả chỉ mở/đóng tại chỗ. **Back** có thể về biến thể trước nếu vừa bấm đổi SKU.

#### SHOP-11

**Luồng và dữ liệu.** `SauMua_Service.danhGiaSanPham()` đọc `TomTatDanhGia` và `Page<DanhGiaCongKhai>` qua `DanhGia_Store`. Record công khai chứa tên hiển thị, sao, bình luận và ngày tạo. `MongoDanhGia_Store` dùng `DanhGia_Document` trong kho, sau đó ánh xạ ra record. Kho MariaDB dự phòng đọc `danh_gia` bằng SQL.

**Lỗi và quay lại.** Rỗng hợp lệ hiện **Chưa có đánh giá được duyệt.** Lỗi kho hiện **Tạm thời chưa tải được đánh giá. Vui lòng thử lại sau.** và giữ trang sản phẩm. Link phân trang giữ `variant` và về `#danh-gia`. Link **Tất cả sản phẩm** về catalog.

#### SHOP-12

**Luồng và dữ liệu.** `fragments/footer.html` tạo link tĩnh và chọn nhóm link bằng vai trò trong session. Không có DTO, entity hay thao tác ghi DB khi bấm link thông thường. Link tên chính sách hiện điều hướng như bảng test, vì nhãn link và route đích là hai thứ cần đối chiếu riêng.

**Lỗi và quay lại.** Lỗi trang mạng xã hội thuộc tab ngoài. Đóng tab đó để quay lại BeautyShop. Link trong cửa hàng dùng lịch sử trình duyệt hoặc menu. Với link tài khoản, backend vẫn kiểm vai trò khi mở URL.

#### SHOP-13

**Luồng và dữ liệu.** Template `product-card.html` ưu tiên dữ liệu media nếu Model cung cấp, rồi tới URL ảnh trong projection `TheSanPham`. `catalog-preview.js` xử lý lỗi ảnh và điều khiển xem trước khi có video. Trang chủ dùng media và CSS/JS có chế độ giảm chuyển động. Không phải luồng lưu dữ liệu.

**Lỗi và quay lại.** Ảnh thiếu hoặc lỗi chuyển sang placeholder theo cơ chế của trang. Bấm thẻ vẫn tới trang chi tiết. Nút phát, dừng hoặc chế độ giảm chuyển động không làm đổi URL. Không ghi video là lỗi nếu bản test không có media được cấp cho thẻ.

### 12.2. Giỏ hàng, xác thực và tài khoản

#### TK-01

**Luồng và dữ liệu.** Form chỉ gửi `variant` tới `GioHang_Controller.them()`. `GioHang_Service` đọc giá và tồn thật từ `GioHang_Repository`. Khách lưu `GioKhach` trong session, gồm mã SKU và số lượng. Customer lưu vào `gio_hang` và `chi_tiet_gio_hang` bằng SQL. Model giỏ dùng `MucGio` gồm mã SKU, tên sản phẩm/biến thể, giá, tồn, số lượng, slug, SKU và ảnh. Header chỉ nhận tổng số lượng `uiCartCount`.

**Lỗi và quay lại.** Hết tồn hoặc thêm vượt khả dụng hiện **SKU không đủ tồn khả dụng.** và vẫn chuyển về `/cart`. Link tên sản phẩm mở lại đúng SKU. **Tiếp tục mua sắm** tới `/products`.

#### TK-02

**Luồng và dữ liệu.** `cart.js` tăng/giảm nhanh và cập nhật đoạn HTML của giỏ. Form gốc gửi `variant` trong URL và `quantity` trong POST, không có DTO riêng. `GioHang_Service` kiểm số lượng. Với Customer, service khóa giỏ và đọc SKU có khóa trong giao dịch trước khi `GioHang_Repository` lưu. View vẫn dùng `MucGio`, không mở entity SKU để sửa ở trình duyệt.

**Lỗi và quay lại.** Lỗi có thể hiện cạnh dòng khi JS xử lý, hoặc thành thông báo trên `/cart` khi gửi form thường. Nội dung điển hình là **Số lượng vượt tồn khả dụng hoặc không hợp lệ.** hoặc **SKU không có trong giỏ.** Khi tồn thay đổi từ lúc mở trang, kiểm tra lại dòng và tổng sau tải lại.

#### TK-03

**Luồng và dữ liệu.** `app.js` dùng hộp thoại chung. **Quay lại** chỉ đóng hộp thoại. **Xác nhận** gửi `quantity=0` qua cùng route với cập nhật số lượng. `GioKhach` bỏ khóa SKU trong session hoặc `GioHang_Repository` xóa dòng SQL của Customer. Không thay đổi tồn vật lý. Danh sách sau đó vẫn là record `MucGio`.

**Lỗi và quay lại.** Nếu dòng đã biến mất ở tab khác, service có thể báo SKU không có trong giỏ. Khi tắt JS, nút **Xóa** gửi form trực tiếp. Giỏ rỗng có **Khám phá sản phẩm → /products**.

#### TK-04

**Luồng và dữ liệu.** `Security_Config` đưa Khách từ checkout sang login. `LoginSuccess_Handler` lấy `PhienNguoiDung` của Customer và `GioKhach` trong session, rồi gọi `GioHang_Service.gopSauDangNhap()`. `GioHang_Repository` ghi mã lần gộp duy nhất vào `ghi_nhan_gop_gio_hang`, sau đó khóa giỏ và cập nhật số lượng theo tồn hiện hành. Giỏ Khách được xóa khỏi session sau khi gộp thành công. Stateful session và dữ liệu giỏ `MucGio` thay cho DTO JSON.

**Lỗi và quay lại.** Nếu số lượng phải giảm, lần mở `/cart` tiếp theo có thể báo **Một số SKU đã được điều chỉnh theo tồn hiện tại.** Nếu DB lỗi khi gộp, login quay về `/login?cartError` với **Lỗi: Chưa thể đồng bộ giỏ. Vui lòng đăng nhập lại.** Giỏ chưa gộp vẫn còn trong session. Trên checkout, bấm **Giỏ hàng** để về `/cart`.

#### TK-05

**Luồng và dữ liệu.** `TaiKhoan_Controller` nhận `DangKy_Form` gồm email, họ tên và mật khẩu. `NguoiDung_Service` chuẩn hóa email, băm mật khẩu rồi tạo entity `NguoiDung` vai trò Customer qua `NguoiDung_Repository`. Entity và mật khẩu băm không được đặt vào Model của trang đăng ký.

**Lỗi và quay lại.** Validation giữ form ở `/register`. Email trùng báo **Email đã được sử dụng.** Thành công chuyển `/login?registered` với thông báo đăng ký thành công. Link **Đăng nhập** dẫn `/login`, **Khám phá cửa hàng** dẫn `/products`.

#### TK-06

**Luồng và dữ liệu.** Form login truyền `username` và `password` cho Spring Security. `NguoiDungDetails_Service` tìm entity `NguoiDung` theo email và chuyển thành `PhienNguoiDung` dùng nội bộ để xác thực. Header nhận vai trò của phiên. Nút hiện/ẩn chỉ thay đổi cách hiển thị ô mật khẩu trên trình duyệt.

**Lỗi và quay lại.** Sai thông tin hoặc tài khoản bị khóa đi tới `/login?error`, với lỗi đăng nhập chung và dòng **Lỗi: Vui lòng kiểm tra thông tin đăng nhập.** Phiên mất hiệu lực chuyển `/login?inactive`. Link **Tạo tài khoản** mở `/register`, **Khám phá cửa hàng** mở `/products`.

#### TK-07

**Luồng và dữ liệu.** `TaiKhoan_Controller` đọc email từ principal và `NguoiDung_Service.hoSo()` ánh xạ entity `NguoiDung` sang `HoSo_Form` gồm họ tên và số điện thoại. Khi bấm **Lưu hồ sơ**, service khóa hàng tài khoản, kiểm Customer đang hoạt động rồi cập nhật. Email trong Model là chuỗi riêng, không nằm trong form cập nhật.

**Lỗi và quay lại.** Sai dữ liệu quay lại template hồ sơ ở cùng URL với lỗi cạnh trường. Lưu đúng redirect `/account` và hiện **Đã cập nhật hồ sơ.** Tài khoản không còn quyền có thể bị chuyển về login hoặc 403 tùy phiên. Từ trang địa chỉ có link **Quay lại hồ sơ**.

#### TK-08

**Luồng và dữ liệu.** `DiaChiNguoiDung_Controller` đưa nguyên `List<DiaChiNguoiDung>` vào Model của danh sách. Form thêm/sửa dùng `DiaChiNguoiDung_Form` gồm tên người nhận, điện thoại, địa chỉ chi tiết và cờ mặc định. `DiaChiNguoiDung_Service` khóa Customer khi ghi, tìm đúng cặp ID địa chỉ và chủ sở hữu qua `DiaChiNguoiDung_Repository`. Nếu xóa địa chỉ mặc định, địa chỉ còn lại có ID nhỏ nhất được chọn.

**Lỗi và quay lại.** Form sai hiển thị lỗi cạnh trường. Sửa/xóa ID không thuộc mình trả 404. Lưu/xóa đúng về danh sách và báo **Đã lưu địa chỉ.** hoặc **Đã xóa địa chỉ.** Link **Quay lại địa chỉ** về danh sách, **Quay lại hồ sơ** về `/account`. Nếu tới từ checkout, quay lại checkout bằng giỏ hoặc lịch sử trình duyệt.

#### TK-09

**Luồng và dữ liệu.** `SauMua_Controller` nhận mã sản phẩm trong URL và lấy Customer từ phiên. `SauMua_Service` gọi `YeuThich_Repository` đọc hoặc ghi `san_pham_yeu_thich`. Danh sách dùng record `Muc` gồm mã sản phẩm, tên, slug và cờ đang bán. Không đưa entity sản phẩm hay entity tài khoản vào Model.

**Lỗi và quay lại.** Lưu hoặc bỏ yêu thích ở `/account/wishlist`, danh sách thay đổi theo thao tác. Mục không còn bán có thể vẫn có tên nhưng không có link sản phẩm. Link **Xem sản phẩm** đưa tới `/products`. Link đăng nhập của Khách trên chi tiết không tự thực hiện thao tác lưu yêu thích.

#### TK-10

**Luồng và dữ liệu.** Các link hồ sơ đọc dữ liệu riêng qua `DonHang_Controller` hoặc `SauMua_Controller`. Danh sách nhận record đơn, yêu thích hoặc đổi trả tương ứng. Đăng xuất là POST do Spring Security xử lý, hủy session và redirect `/?logout`.

**Lỗi và quay lại.** **Back** sau logout có thể hiện bản HTML cũ do trình duyệt lưu. Tải lại hoặc mở GET của trang bảo vệ phải quay về login. Đăng xuất không xóa giỏ đã lưu DB của Customer.

### 12.3. Checkout, COD và đơn hàng

#### DH-01

**Luồng và dữ liệu.** `Checkout_Controller.hienCheckout()` lấy giỏ từ `GioHang_Service` và địa chỉ từ `DiaChiNguoiDung_Service`. Model là `Checkout_Form` gồm `addressId`, `voucher`; danh sách dòng `MucGio`; nguyên `List<DiaChiNguoiDung>`; cờ MoMo và `tamTinh` kiểu `BigDecimal`. Khi mở GET lần đầu, địa chỉ mặc định có thể được chọn sẵn.

**Lỗi và quay lại.** Giỏ trống hiện **Giỏ hàng đang trống.** và link `/cart`. Thiếu địa chỉ hiện **Cần thêm địa chỉ giao hàng** và link tới form tạo. **Quản lý địa chỉ** mở danh sách địa chỉ. Bấm **Giỏ hàng** về `/cart`.

#### DH-02

**Luồng và dữ liệu.** `Checkout_Controller.baoGia()` nhận cùng `Checkout_Form`, đọc giỏ và địa chỉ hiện tại rồi kiểm địa chỉ thuộc Customer. `PhieuGiamGia_Service.xemTruoc()` tìm voucher hợp lệ bằng `PhieuGiamGia_Repository`, trả record `GiamGia(maVoucher, soTien)`. `ShippingFee_Policy` tính lại phí. Model có tạm tính, giảm, phí, tổng dạng `BigDecimal` và cờ cho biết báo giá hợp lệ. Báo giá chỉ đọc và không giữ lượt.

**Lỗi và quay lại.** Sai địa chỉ hiện lỗi cạnh ô chọn và trong khối **Chưa thể hoàn tất**. Nếu giỏ rỗng, có lỗi và link quay về giỏ. Khi truy vấn voucher đang tranh chấp hoặc không xác nhận được, trang báo **Chưa thể xác nhận báo giá. Vui lòng cập nhật lại báo giá.** Form ở template checkout, URL sau POST có thể là `/checkout/quote`.

#### DH-03

**Luồng và dữ liệu.** `PhieuGiamGia_Service` chuẩn hóa mã và kiểm điều kiện hoạt động, thời hạn, lượt đã dùng/đang giữ và tạm tính. Tính phần trăm làm tròn xuống đồng rồi áp trần nếu có. `checkout.js` khi phát hiện thay đổi sau báo giá sẽ ẩn phần tiền đã xác nhận và vô hiệu nút đặt đơn cho tới báo giá mới. Cơ chế này chỉ hỗ trợ giao diện, service tính lại khi đặt.

**Lỗi và quay lại.** Lỗi điển hình là **Voucher không hợp lệ, đã hết hạn hoặc hết lượt.** hoặc **Đơn chưa đạt mức tối thiểu của voucher.** Ô voucher giữ giá trị đang nhập để sửa. Mở lại GET `/checkout` là lượt xem mới, nên bấm **Cập nhật báo giá** lại trước khi đặt.

#### DH-04

**Luồng và dữ liệu.** `Checkout_Controller.datDon()` nhận `Checkout_Form` và tham số `payment`, mặc định `COD`. `DonHang_Service.datCOD()` chạy trong một transaction: khóa giỏ, xác minh địa chỉ, khóa/đọc SKU, ghi lại tên, mã SKU, giá và số lượng vào record `DonHang_Repository.ChiTiet`, tính tổng, chốt tồn và voucher, ghi thanh toán `UNPAID` rồi xóa mục trong giỏ. Repo dùng SQL cho `don_hang`, `chi_tiet_don_hang`, `lan_thanh_toan`. Entity địa chỉ được đọc nội bộ để chép thông tin lúc đặt. View sau đó nhận `ChiTietDon`, không nhận entity địa chỉ hay entity đơn.

**Lỗi và quay lại.** Lỗi tồn hoặc voucher render lại checkout với **Chưa thể hoàn tất**, ví dụ **Không đủ tồn cho …**. Lỗi đồng thời hiện **Giỏ hàng hoặc tồn kho vừa thay đổi. Vui lòng thử lại.** Transaction thất bại không để lại đơn/kho/voucher ghi nửa chừng. Thành công redirect tới GET chi tiết nên tải lại chỉ đọc đơn. Link **Quay lại đơn hàng** về `/orders`.

#### DH-05

**Luồng và dữ liệu.** `DonHang_Controller` dùng Customer trong session. `DonHang_Service` gọi `QuyenDonHang_Service` để kiểm chủ đơn rồi đọc qua `DonHang_Repository`. List nhận `TomTat`, có mã, trạng thái, tổng và ngày tạo. Chi tiết nhận `ChiTietDon` gồm record `Don` với thông tin người nhận và tổng tiền, `ChiTiet` từng SKU, trạng thái thanh toán và cờ hành động.

**Lỗi và quay lại.** ID không tồn tại hoặc thuộc Customer khác trả 404 với trang **Không tìm thấy trang**. Link **Quay lại đơn hàng** trên chi tiết Customer luôn về `/orders`, không dẫn tới nơi đã mở đơn trước đó.

#### DH-06

**Luồng và dữ liệu.** `DonHang_Controller` nhận ID đơn trong URL. `DonHang_Service.huyCustomer()` kiểm sở hữu, khóa đơn và chỉ chấp nhận COD `UNPAID` ở `PENDING_CONFIRMATION` hoặc `CONFIRMED`. `TonKho_Service` và `PhieuGiamGia_Service` hoàn phần đã ghi nhận rồi `DonHang_Repository` đổi trạng thái. View đọc lại `ChiTietDon` sau redirect.

**Lỗi và quay lại.** Hủy từ màn cũ sau khi đơn đổi trạng thái có thể báo **Đơn hiện không thể hủy.** ngay trong chi tiết. Hủy thành công hoặc gửi lại hủy cùng đơn đã hủy không hoàn tồn thêm. Link quay lại về `/orders`.

#### DH-07

**Luồng và dữ liệu.** Customer đọc cùng record `ChiTietDon` mỗi lần mở lại. Việc đổi trạng thái do Admin qua **AD-05**. `OrderStatus` là enum trạng thái đơn. Trạng thái thanh toán đọc riêng từ `lan_thanh_toan`. Giao thành công mới bật link sau mua trong template Customer.

**Lỗi và quay lại.** Nếu thấy trạng thái cũ, tải lại URL chi tiết đúng mã. Khi Admin chuyển sai bước, bước đó bị chặn và trạng thái còn như cũ. **Quay lại đơn hàng** về danh sách Customer kể cả khi chưa giao xong.

#### DH-08

**Luồng và dữ liệu.** Hai form chỉ đem ID địa chỉ, voucher và phương thức. Báo giá cũ không dành riêng tồn hay lượt. Khi đặt thực tế, `DonHang_Service` khóa giỏ, khóa SKU rồi kiểm/chốt voucher trong transaction. `TonKho_Repository` và `PhieuGiamGia_Repository` dùng điều kiện tồn/lượt để bảo toàn số liệu. Giao diện nhận báo giá hoặc lỗi, không nhận đối tượng khóa hay entity của người mua còn lại.

**Lỗi và quay lại.** Phiên thắng về chi tiết đơn. Phiên còn lại ở checkout với lỗi tồn, voucher hoặc thông báo thử lại do dữ liệu vừa đổi. Dùng link giỏ để sửa số lượng, rồi mở lại checkout. Đối chiếu bằng mã đơn và giá trị tồn/lượt, vì hai lần bấm bằng tay không nhất thiết đồng thời thật.

### 12.4. Đánh giá và đổi trả của Customer

#### SM-01

**Luồng và dữ liệu.** `SauMua_Controller` gọi `SauMua_Service.xemDanhGia()`. `DanhGia_Repository` đọc `MucMua` từ MariaDB theo ID dòng đơn và chủ sở hữu. Service chỉ cho đánh giá đơn `DELIVERED`. Model có `TrangDanhGia` gồm `MucMua`, `DanhGiaCuaKhach` nếu đã đánh giá, và `DanhGia_Form` gồm sao/bình luận.

**Lỗi và quay lại.** Dòng của người khác hoặc không tồn tại trả 404. Truy cập trang đánh giá của đơn chưa giao trực tiếp trả 409. MongoDB lỗi có thể vẫn mở form với cảnh báo kho chưa tải được. Link **Quay lại đơn** về đúng `/orders/{id}`.

#### SM-02

**Luồng và dữ liệu.** `SauMua_Controller.luuDanhGia()` nhận `DanhGia_Form`. Service kiểm lại chủ đơn, trạng thái, số sao 1–5 và giới hạn nội dung, rồi tạo record `DanhGiaMoi` với ID dòng đơn, đơn, người mua, sản phẩm, SKU và nội dung. `DanhGia_Store` lưu bằng MongoDB `DanhGia_Document` hoặc bảng MariaDB dự phòng. Record công khai chỉ được đọc khi đã hiển thị.

**Lỗi và quay lại.** Bài trùng dòng đơn trả 409. Validation giữ form và hiện lỗi. Kho lỗi giữ nội dung và báo **Tạm thời chưa lưu được đánh giá. Vui lòng thử lại sau ít phút.** Thành công redirect về chi tiết đơn với **Đã gửi đánh giá để duyệt.** Link quay lại đơn giống **SM-01**.

#### SM-03

**Luồng và dữ liệu.** GET dùng `DanhGiaCuaKhach` để điền lại `DanhGia_Form`. Form sửa POST tới `/orders/items/{id}/review/edit`. Service gọi `DanhGia_Store.sua()` theo dòng đơn và Customer. Với MongoDB, lúc sửa bài bị ẩn, về trạng thái chờ duyệt và phiên bản tăng để Admin không duyệt nhầm bản cũ.

**Lỗi và quay lại.** Mục đánh giá không còn hoặc không thuộc mình trả 404. Sai sao/nội dung hoặc mất kết nối ở lại form. Lưu đúng quay về đơn. Nếu trở lại trang đã được cache, tải lại trang công khai để đối chiếu bài đã ẩn.

#### SM-04

**Luồng và dữ liệu.** `SauMua_Controller` nhận `DoiTra_Form` gồm `lyDo` và bản đồ `soLuong` theo ID dòng đơn. GET trả `TrangDoiTra` gồm record `Don` và các `Muc` còn có thể yêu cầu. POST gọi Service khóa đơn, đối chiếu trạng thái, thời hạn và từng số lượng, rồi ghi `yeu_cau_doi_tra` và `chi_tiet_doi_tra` bằng `DoiTra_Repository`. Chi tiết trả `ChiTietDoiTra` gồm lý do, trạng thái và các `MucTra`, không đưa entity JPA.

**Lỗi và quay lại.** Gửi đúng vào `/account/returns/{id}`. Từ form, **Quay lại đơn** về `/orders/{id}`. Từ chi tiết yêu cầu, **Quay lại yêu cầu đổi trả** về `/account/returns`.

#### SM-05

**Luồng và dữ liệu.** `DoiTra_Repository` tính hạn dựa vào ngày giao và tổng số đã yêu cầu ở các yêu cầu chưa bị từ chối. Khi lưu, `SauMua_Service` kiểm lại trong transaction, kể cả form đã mở ở tab cũ. GET/POST kiểm chủ sở hữu bằng ID từ phiên.

**Lỗi và quay lại.** Thiếu mặt hàng báo **Chọn ít nhất một mặt hàng để đổi trả.** Số lượng hoặc ID dòng không hợp lệ báo lỗi trên form. GET hết hạn hiện **Chỉ nhận yêu cầu trong 7 ngày kể từ khi giao thành công.** và giấu form. Nếu POST sau khi vừa hết hạn, trả 409. Người khác mở chi tiết trả 404. Từ form dùng **Quay lại đơn**.

#### SM-06

**Luồng và dữ liệu.** `SauMua_Controller` đọc danh sách theo Customer và trả `List<DoiTra_Repository.YeuCau>`. Chi tiết dùng `ChiTietDoiTra`. Khi Admin chuyển trạng thái, Customer thấy dữ liệu mới ở lần GET tiếp theo. Yêu cầu bị từ chối không còn được tính vào số đã yêu cầu.

**Lỗi và quay lại.** Danh sách trống hiện trạng thái rỗng của màn hình. ID không còn truy cập được trả 404. Từ chi tiết quay lại `/account/returns`. Có thể mở lại đúng đơn từ mục **Đơn hàng** để test hạn mức sau từ chối.

### 12.5. Quản trị

#### AD-01

**Luồng và dữ liệu.** Spring Security xác thực `NguoiDung` rồi tạo principal `PhienNguoiDung` như **TK-06**. `LoginSuccess_Handler` đọc URL đã yêu cầu, chỉ dùng lại đường dẫn Admin hợp lệ của cùng site. Nếu không có đích hợp lệ, vào `/admin/dashboard`. Route `/admin` và `/admin/` cũng redirect về đó. Không có DTO form Admin riêng.

**Lỗi và quay lại.** Sai thông tin về `/login?error`. Đăng nhập bằng Customer rồi mở URL Admin trả 403. Mở trực tiếp `/admin/dashboard` để kiểm lại trang mặc định sau khi đã đăng nhập.

#### AD-02

**Luồng và dữ liệu.** `GiaoDien_Advice` cung cấp chuỗi `uiPath`. Fragment `header` và `admin-nav` dùng path và vai trò để dựng các link, với `aria-current` cho trang hoặc nhóm trang đang mở. CSS chọn sidebar từ 1100 px, menu mobile dùng HTML `details`. Không có entity hay DTO ở bước mở menu.

**Lỗi và quay lại.** Đóng menu mobile ở tại chỗ. **Về cửa hàng** mở `/` và giữ phiên Admin. Nếu muốn về lại nhanh, mở `/admin/dashboard` hoặc chọn **Tổng quan** từ trang Admin. Đăng xuất về `/?logout`.

#### AD-03

**Luồng và dữ liệu.** `AdminTongQuan_Controller` nhận `BoLocThongKe_Form(range, from, to)` và chuỗi `nguong`. `ThongKe_Service` xác định kỳ, kỳ trước và các mốc theo ngày Việt Nam rồi đổi sang thời gian phù hợp cho truy vấn. `ThongKe_Repository` đọc MariaDB bằng SQL. Kết quả gồm các record như `DoanhThu`, `DemDon`, `DemGiao`, `TachDoanhThu` và được gom trong `ThongKe_Service.TongQuan` với `Ky`, `Kpi`. `DanhGia_Store` cung cấp số sao và số chờ duyệt. Dashboard không truyền entity JPA vào Model.

**Lỗi và quay lại.** Ngày sai định dạng, thiếu một đầu, ngày bắt đầu sau ngày kết thúc, kỳ tương lai, quá 366 ngày hoặc `range` lạ đều báo lỗi trên dashboard và hiển thị 30 ngày gần nhất. Ngưỡng tồn sai báo chỉ nhận số nguyên 0–1000 và dùng ngưỡng mặc định 5. Mở `/admin/dashboard` để về lựa chọn mặc định.

#### AD-04

**Luồng và dữ liệu.** `dashboard.js` đọc các ô `data-value` từ chính bảng HTML rồi vẽ Chart.js. `TongQuan` chứa `NgayThongKe`, `Nhom`, `SanPhamBanChay`, `NhomThanhToan`, `DonGanDay` và các record cảnh báo `SkuSapHet`, `DoiTraCho`, `CanhBaoThanhToan`, `VoucherSapHetHan`. Những link trong bảng được tạo từ mã hoặc slug trong record. Danh sách và bộ đếm SKU dùng chung điều kiện tồn khả dụng.

**Lỗi và quay lại.** Tắt JS hoặc không tải được Chart.js thì bảng vẫn dùng được. Không có cảnh báo MoMo hiện **Không có giao dịch cần kiểm tra**. Kho đánh giá lỗi có thể làm khu sao rỗng trong khi dashboard vẫn hiển thị. Link **Quay lại đơn hàng**, **Quay lại yêu cầu đổi trả**, **Quay lại voucher** và **Quay lại SKU** ở trang đích về danh sách tương ứng. Dùng **Back** để giữ URL dashboard vừa xem.

#### AD-05

**Luồng và dữ liệu.** `AdminDonHang_Controller` nhận ID và `OrderStatus` từ nút POST. Danh sách dùng record `TomTat` và thống kê `DonHang_Repository.TongQuan`. Chi tiết dùng `DonHang_Service.ChiTietDon` như phía Customer, nhưng kèm bước chuyển Admin. Service khóa đơn, kiểm `OrderState_Policy`, tạo thông tin vận chuyển khi gửi hàng. Với COD, giao thành công ghi trạng thái vận chuyển, đơn và thanh toán `PAID` trong cùng giao dịch.

**Lỗi và quay lại.** Chuyển từ màn đã cũ hoặc bước sai báo **Không thể chuyển đơn sang trạng thái đã chọn.** Nếu không xác nhận được giao/thu tiền, báo **Không thể xác nhận hoàn tất giao hàng.** Controller redirect lại chi tiết để đọc trạng thái thực. **Quay lại đơn hàng** về `/admin/orders`, không giữ lọc cũ qua link này.

#### AD-06

**Luồng và dữ liệu.** Danh sách nhận các giá trị `status` lặp dưới dạng `List<OrderStatus>` và lọc các record `TomTat`. **Xem tất cả đơn** bỏ tham số. Hủy gửi `CANCELLED` tới controller Admin, dùng cùng logic COD chưa thu tiền, hoàn tồn và voucher như **DH-06**. Màn hình đọc lại `ChiTietDon`.

**Lỗi và quay lại.** Lọc với trạng thái không chuyển được thành enum có thể trả 400. Đơn không đủ điều kiện hủy vẫn ở chi tiết và báo lỗi. Link quay lại về danh sách toàn bộ đơn.

#### AD-07

**Luồng và dữ liệu.** `AdminDanhMucSanPhamThuongHieu_Controller` chọn nhóm theo route. `DanhMucSanPhamThuongHieu_Service` dùng `DanhMuc_Repository`, `ThuongHieu_Repository` hoặc `SanPham_Repository`. List đưa nguyên entity `DanhMuc`, `ThuongHieu` hoặc `SanPham` vào Model. Form chỉnh sửa chuyển entity sang `DanhMucSanPhamThuongHieu_Form` gồm tên, slug, hoạt động và các trường sản phẩm như danh mục, thương hiệu, mô tả, thành phần, hướng dẫn, loại da. Select danh mục và thương hiệu vẫn nhận danh sách entity.

**Lỗi và quay lại.** Lỗi dữ liệu hoặc slug trùng giữ form, ví dụ **Không thể lưu. Kiểm tra đường dẫn trùng hoặc danh mục và thương hiệu đã thay đổi.** Danh mục/thương hiệu đang được dùng bị chặn xóa và báo có thể ngừng hoạt động. Mã không tồn tại trả 404. **Quay lại danh sách** mở đúng nhóm. Lưu hoặc xóa quay về danh sách nhóm tương ứng.

#### AD-08

**Luồng và dữ liệu.** `AdminBienTheSanPham_Controller` cùng `BienTheSanPham_Service` và `BienTheSanPham_Repository` xử lý list, thêm, sửa, ngừng bán. List nhận nguyên `SanPham` và `List<BienTheSanPham>`. Form nhận `SanPham` làm ngữ cảnh và `BienTheSanPham_Form` gồm mã, tên, giá, tồn kho, lô/hạn, trạng thái, tệp ảnh và cờ xóa ảnh. Service khóa SKU theo cả sản phẩm và SKU, giữ tồn kho tối thiểu bằng lượng tạm giữ và chuẩn hóa mã. `AnhBienTheSanPham_Service` kiểm và lưu ảnh, dọn ảnh cũ khi DB commit hoặc dọn ảnh mới khi DB rollback. Ngừng sản phẩm gọi nhánh `SanPham.deactivate()` của **AD-07**.

**Lỗi và quay lại.** Mã trùng hoặc ảnh không hợp lệ báo cạnh trường. Tồn thấp hơn lượng giữ báo **Tồn kho phải đủ cho số lượng đang tạm giữ.** Chọn thay ảnh và xóa cùng lúc báo chỉ được chọn một thao tác. SKU không thuộc sản phẩm trả 404. **Quay lại SKU** về danh sách SKU, **Quay lại sản phẩm** về `/admin/catalog/products`. Xem thêm **QA-07** cho lỗi tệp quá lớn.

#### AD-09

**Luồng và dữ liệu.** `AdminNguoiDung_Controller` nhận `q` và `page`, mặc định 15 hàng. `NguoiDung_Repository` trả projection `TomTat` gồm mã, tên, email, vai trò và trạng thái kích hoạt cho Model. Khi khóa/mở khóa, form gửi ID, `enabled` và bộ lọc. `NguoiDung_Service` dùng entity `NguoiDung` nội bộ, khóa các hàng liên quan và kiểm phiên Admin. Khóa cập nhật phiên bản xác thực để filter từ chối phiên cũ.

**Lỗi và quay lại.** Tự khóa báo **Bạn không thể khóa tài khoản đang sử dụng.** Kết quả thao tác giữ `q` và `page` khi quay về `/admin/users`. Phiên bị khóa bị đưa tới `/login?inactive` khi gửi yêu cầu tiếp. Tìm không khớp cho danh sách rỗng. Trang quá cao được đọc lại ở trang cuối có thể dùng.

#### AD-10

**Luồng và dữ liệu.** `AdminSauMua_Controller` đổi query thành `DanhGia_Store.BoLocDanhGia`. `SauMua_Service` đọc `Page<DanhGiaAdmin>` trong store và bổ sung tên sản phẩm bằng `DanhGia_Repository` ở MariaDB. Model dùng `TrangDanhGiaAdmin` gồm page và lựa chọn `SanPhamLoc`. `DanhGia_Document` không được truyền nguyên vào view. Form duyệt/ẩn gửi `visible` và `revision`, kèm tham số giữ bộ lọc. Duyệt cần đúng phiên bản đã xem. Ẩn không có yêu cầu so khớp phiên bản như duyệt.

**Lỗi và quay lại.** Duyệt từ tab cũ báo **Đánh giá đã thay đổi. Vui lòng kiểm tra lại.** Kho lỗi báo tạm thời không kết nối được. Ẩn một mục đã biến mất có thể trả 404. Sau thao tác hợp lệ hoặc lỗi được xử lý bằng thông báo, redirect về `/admin/reviews` với tham số được chấp nhận. Khi bấm **Xem đơn mua**, nút quay lại của chi tiết đơn dẫn về danh sách đơn. Dùng **Back** để quay về bộ lọc đánh giá trước đó.

#### AD-11

**Luồng và dữ liệu.** `AdminSauMua_Controller` dùng record `YeuCau`, `ChiTietDoiTra` và `MucTra` như phía Customer nhưng truy vấn Admin không giới hạn theo một chủ mua. Form gửi `ReturnStatus`. `SauMua_Service` kiểm bước tiếp và `DoiTra_Repository` cập nhật khi trạng thái hiện tại vẫn khớp. Chỉ ghi trạng thái yêu cầu, không gọi service nhập kho hay hoàn tiền.

**Lỗi và quay lại.** Thao tác từ tab cũ hoặc chuyển không hợp lệ báo **Không thể chuyển yêu cầu sang trạng thái đã chọn.** rồi về lại chi tiết. ID sai trả 404. **Quay lại yêu cầu đổi trả** về `/admin/returns`.

#### AD-12

**Luồng và dữ liệu.** `AdminPhieuGiamGia_Controller` nhận `PhieuGiamGia_Form` gồm mã, loại, giá trị, trần, đơn tối thiểu, giới hạn, giờ UTC và cờ bật. `PhieuGiamGia_Service` chuẩn hóa mã và chuyển thành record `CauHinh`. `PhieuGiamGia_Repository` ghi SQL vào `phieu_giam_gia`. List nhận `ThongTin` bọc cấu hình và bộ đếm đã dùng/đang giữ. Đây là dữ liệu đọc từ SQL. Luồng tính giảm ở checkout tạo record `GiamGia` như **DH-02**.

**Lỗi và quay lại.** Mã trùng báo **Mã voucher đã tồn tại.** Phần trăm vượt 100 hoặc thời gian sai báo cạnh trường. **Quay lại voucher** mở `/admin/vouchers`. Sau khi lưu thành công, về list với **Đã lưu voucher.**

#### AD-13

**Luồng và dữ liệu.** GET sửa chuyển `CauHinh` từ SQL thành `PhieuGiamGia_Form`. Khi lưu hoặc xóa, service khóa voucher. Sửa không cho giới hạn thấp hơn tổng lượt đã dùng và đang giữ. SQL xóa kiểm lịch sử, tham chiếu và bộ đếm. Checkbox **Đang bật** chỉ cập nhật trạng thái cấu hình, không xóa lịch sử.

**Lỗi và quay lại.** Hạ giới hạn sai báo **Giới hạn phải đủ cho lượt đã dùng và đang giữ.** Xóa mã có lịch sử quay về list và báo **Voucher có lịch sử đơn hoặc lượt sử dụng. Hãy chuyển sang ngừng hoạt động.** ID sai trả 404. Khi checkout, mã tắt được xử lý như voucher không hợp lệ.

#### AD-14

**Luồng và dữ liệu.** `AdminTuVan_Controller` đưa `TuVan_Service.TrangThai` vào Model. Record gồm `aiEnabled`, số đoạn, thời điểm cập nhật và cờ đang nạp. POST `/admin/ai/reindex` không có DTO form nghiệp vụ, chỉ kích hoạt tác vụ có quyền Admin. Kho nguồn đọc SQL qua `KhoTriThuc_Service` và `TriThuc_Repository`, chỉ mục AI nằm trong RAM.

**Lỗi và quay lại.** Khi AI tắt, nút nạp không hiện và trang cho biết FAQ vẫn dùng được. Nạp lỗi thì về `/admin/ai` và báo kiểm tra Ollama hoặc model. Thành công hiện **Đã nạp kho tri thức.** Sidebar cho phép rời trang bằng link khác.

#### AD-15

**Luồng và dữ liệu.** Lưu cờ hoạt động qua `DanhMucSanPhamThuongHieu_Form` cập nhật entity `DanhMuc` hoặc `ThuongHieu`. `CuaHang_Repository` kiểm trạng thái cả hai quan hệ ở truy vấn công khai. Sau khi lưu, list Admin vẫn đọc các entity của nhóm. Catalog công khai đọc projection `TheSanPham` mới.

**Lỗi và quay lại.** Lỗi lưu ở lại form. Khi một quan hệ bị tắt, mở URL chi tiết cũ trả 404 và tìm kiếm trên catalog không còn sản phẩm đó. Từ trang lỗi, **Xem sản phẩm** về catalog. Bật lại trong cùng form Admin để kiểm danh sách xuất hiện lại.

### 12.6. Thanh toán MoMo

#### MO-01

**Luồng và dữ liệu.** `Momo_Settings` chọn adapter theo profile và cấu hình. `Checkout_Controller` đưa hai cờ `momoEnabled` và `momoMock` vào Model bên cạnh `Checkout_Form`, record `MucGio` và entity địa chỉ. Khi MoMo tắt, HTML không có nút đặt MoMo, còn `DonHang_Service.datCOD()` hoạt động độc lập.

**Lỗi và quay lại.** Nếu một tab checkout cũ gửi lựa chọn MoMo sau khi cấu hình đã tắt, service báo **MoMo chưa khả dụng.** và render lại checkout. **Giỏ hàng** về `/cart`.

#### MO-02

**Luồng và dữ liệu.** `Checkout_Controller` nhận `Checkout_Form` và `payment=MOMO`. `DonHang_Service.datMomo()` tạo đơn chờ, ghi snapshot và giữ tồn/lượt voucher trong transaction. `TonKho_Service` và `PhieuGiamGia_Service` ghi các bảng tạm giữ. `ThanhToanMomo_Service` chuẩn bị attempt trong `lan_thanh_toan` rồi `Momo_Facade` mở gateway sau commit. Template mock nhận record `LanThanhToan` chứa `MomoGateway.Request`, trạng thái và gateway, không nhận entity JPA.

**Lỗi và quay lại.** Nếu không lấy được URL gateway sau khi đã tạo đơn, có thể vào `/orders/{id}` với thông báo đơn đang chờ và có thể mở lại. Số tiền ngoài giới hạn MoMo báo lỗi tại checkout. **Quay lại đơn hàng** trên mock về chi tiết của chính Customer. Mở mock của đơn người khác hoặc khi mock tắt trả 404.

#### MO-03

**Luồng và dữ liệu.** `Momo_Controller` nhận lựa chọn mô phỏng và mã lần thanh toán. `Momo_Facade` đưa kết quả mock vào cùng nhánh kiểm tra IPN. `ThanhToanMomo_Service` kiểm lần thanh toán còn hợp lệ, chốt giữ tồn và voucher, cập nhật `PAID` và đưa đơn sang `PENDING_CONFIRMATION`. Repo ghi bằng SQL. Trang chi tiết hiển thị `ChiTietDon`, Dashboard đọc record tổng hợp độc lập.

**Lỗi và quay lại.** Quay về `/orders/{id}`. Trang mock cũ có thể báo **Trang mô phỏng đã cũ. Mở lại lần thanh toán hiện tại.** Tải lại chi tiết không gửi lại thanh toán. Kết quả hợp lệ đã được ghi không chốt tồn lần nữa. Nếu tiền tới muộn khi lần thanh toán không còn hiệu lực, service có đường ghi cần hoàn/đối soát, chi tiết hiển thị cảnh báo thay vì chốt như đơn mới.

#### MO-04

**Luồng và dữ liệu.** Từ chối cập nhật attempt thành `FAILED`. Nếu đơn vẫn trong thời gian giữ, `ThanhToanMomo_Service.chuanBi()` có thể tạo attempt mới. Nếu attempt vẫn `PENDING`, service dùng lại request hiện tại. `Momo_Facade` kiểm chủ đơn trước khi mở lại cổng. Các đối tượng nội bộ là record `LanThanhToan` và `Request`, còn chi tiết hiện `ChiTietDon`.

**Lỗi và quay lại.** Từ chối còn hạn hiện hướng dẫn thử lại trong chi tiết. Nếu hết thời gian thử lại, báo **Đơn đã hết thời gian thử lại. Vui lòng chờ đối soát.** Khi gateway chưa trả URL, ở lại chi tiết và hiện thông báo chưa nhận được trang thanh toán. Link từ mock luôn về đúng đơn đó.

#### MO-05

**Luồng và dữ liệu.** Với kết quả hết hạn đã xác minh, `ThanhToanMomo_Service` dùng `TonKho_Service` và `PhieuGiamGia_Service` để giải phóng phần giữ, rồi dùng `DonHang_Repository` đổi đơn sang `EXPIRED`. Dữ liệu hiển thị vẫn là `ChiTietDon`. `ThongKe_Repository` loại đơn hết hạn khỏi doanh thu và số đơn theo định nghĩa KPI.

**Lỗi và quay lại.** Chi tiết hiển thị **Đã hết hạn**, mất nút thử lại. **Quay lại đơn hàng** về `/orders`. Muốn test lại nhánh thanh toán thì tạo đơn mới từ sản phẩm và giỏ.

#### MO-06

**Luồng và dữ liệu.** Admin POST `/admin/orders/{id}/momo/reconcile`, dùng ID đơn. `Momo_Facade` hỏi gateway về request hiện tại. Chỉ khi nhận kết quả thì gọi `ThanhToanMomo_Service.nhanDoiSoat()`. `MomoReconciliation_Job` cũng quét các đơn chờ có lượt giữ quá hạn theo lô. Các record của gateway là `Request` và `KetQua`, chi tiết Admin là `ChiTietDon`.

**Lỗi và quay lại.** Nút đối soát redirect tới `/admin/orders/{id}`. Chưa có dữ liệu từ cổng hiện **Chưa có kết quả đối soát xác minh được.** Có kết quả đã xử lý thì kiểm trạng thái mới trong chi tiết. **Quay lại đơn hàng** về danh sách Admin.

#### MO-07

**Luồng và dữ liệu.** `Checkout_Controller` redirect tới Sandbox khi nhận URL. Browser return do `Momo_Controller` dựng trang thông báo chung, không cập nhật đơn. Kết quả từ gateway đi qua IPN JSON ánh xạ thành `MomoSignature.Ipn`, hoặc qua đối soát. Service xác minh kết quả trước khi ghi, và trang Customer đọc `ChiTietDon` của chính mình.

**Lỗi và quay lại.** Nếu quay về sớm, đơn có thể còn **Chờ thanh toán**. Bấm **Xem đơn hàng** về `/orders`, rồi mở mã đơn và tải lại để kiểm. Thiếu merchant hoặc IPN công khai thì ghi **Chặn**. Các trường hợp chữ ký, số tiền và IPN lặp kiểm bằng bộ test sẵn có ở mục 10.

### 12.7. Tư vấn và kho tri thức

#### TV-01

**Luồng và dữ liệu.** `TuVanDa_Controller` mở `chat/TuVanDa.html` với `TuVanDaRequest` rỗng. `TuVan_Controller` mở view `TuVanDa.html` với `TuVan_Form` chỉ có câu hỏi. Hai link đi tới hai form khác nhau. Mở form chưa gọi Ollama hay truy vấn gợi ý sản phẩm.

**Lỗi và quay lại.** Từ `/chat`, link **Mở tư vấn chăm sóc da** mở `/tu-van-da`. Từ trang tư vấn da, link footer **Tư vấn sản phẩm** dẫn `/chat` hoặc logo về `/`. Form có thể hiện dù AI chưa được cấu hình.

#### TV-02

**Luồng và dữ liệu.** POST `/tu-van-da` bind `TuVanDaRequest(loaiDa, vanDeDa, daNhayCam, nhuCau)` và kiểm `@Valid`. Các ô chọn dùng tập giá trị cho phép. Mô tả giới hạn tối đa 1.000 ký tự. Khi `BindingResult` có lỗi, controller trả form mà chưa gọi `TuVanDaService`.

**Lỗi và quay lại.** Trình duyệt có thể chặn trước, hoặc form báo cạnh trường như **Vui lòng chọn loại da** hay **Vui lòng mô tả vấn đề da**. Form vẫn ở `/tu-van-da`. Mở mới GET nếu muốn xóa dữ liệu nhập.

#### TV-03

**Luồng và dữ liệu.** `TuVanDaService` nhận DTO `TuVanDaRequest`, dùng `LoaiSanPham_TimKiem` nhận diện nhóm rồi truy vấn bằng `BienTheSanPham_Repository`. Repo trả nguyên `BienTheSanPham` vào Service, nạp sẵn `SanPham`, `DanhMuc` và `ThuongHieu`. Service giới hạn ứng viên, cho model chọn ID trong danh sách và ánh xạ các ID hợp lệ về dữ liệu đã đọc từ catalog. Controller đặt `traLoi` dạng chuỗi vào Model. `TuVanDaResponse` ở repo hiện rỗng, chưa tham gia luồng. Thymeleaf hiển thị bằng `th:text`.

**Lỗi và quay lại.** Thiếu ứng viên thì báo không tìm thấy loại phù hợp. Nếu có ứng viên mà AI tắt hoặc lỗi, trang hiện **AI tư vấn hiện chưa sẵn sàng. Vui lòng thử lại sau** và giữ nội dung đã nhập. Sau POST ở cùng trang, có thể mở lại GET `/tu-van-da` thay vì tải lại POST. Link footer mở `/chat` nếu muốn thử FAQ.

#### TV-04

**Luồng và dữ liệu.** `TuVan_Controller` nhận `TuVan_Form`. Khi không có provider hoặc chưa có chỉ mục, `TuVan_Service` tìm FAQ qua `KhoTriThuc_Service` và `TriThuc_Repository`. SQL đọc record `Faq` từ `bai_viet_faq`, service tạo `Nguon` nội bộ. Model nhận `TraLoi(nhan, noiDung, tieuDeNguon, url, gia, khaDung)`. Trong chế độ FAQ, các trường giá/tồn có thể rỗng.

**Lỗi và quay lại.** Câu hỏi hợp lệ render lại `/chat` với câu trả lời hoặc thiếu thông tin. Link nguồn mở URL trong record nếu đủ dữ liệu. Quay lại theo lịch sử hoặc mở `/chat` để bắt đầu câu mới.

#### TV-05

**Luồng và dữ liệu.** Giới hạn 500 ký tự thuộc `TuVan_Form`. `TuVan_Service` kiểm nội dung ngoài phạm vi và mẫu thông tin cá nhân trước khi tìm nguồn. Kết quả thiếu nguồn vẫn là `TraLoi` với thông báo phù hợp. Không chuyển entity tài khoản hay lịch sử đơn vào kết quả.

**Lỗi và quay lại.** Form trống hoặc quá dài hiện lỗi trường. Câu ngoài nguồn hiện phản hồi thiếu thông tin, còn nội dung không phù hợp có thể nhận nhãn **Hướng dẫn** và yêu cầu bỏ thông tin riêng tư. Sửa câu ngay trên `/chat` để thử lại.

#### TV-06

**Luồng và dữ liệu.** `TuVan_Controller` kiểm giới hạn sau validation và trước khi gọi service. Bộ đếm `CuaSo` nằm trong session và có thêm giới hạn chung của tiến trình. Request vượt mức không gọi `KhoTriThuc_Service`, repo FAQ hay Ollama. Form vào vẫn là `TuVan_Form`.

**Lỗi và quay lại.** Câu vượt hạn mức giữ template `/chat`, trả 429 với `Retry-After: 60` và báo **Bạn vừa gửi nhiều câu hỏi. Vui lòng thử lại sau một phút.** Mở lại GET có thể hiện form nhưng không xóa bộ đếm của phiên.

#### TV-07

**Luồng và dữ liệu.** `OllamaRag_Config` tạo `RagProvider` khi AI được bật và provider là Ollama. `TuVan_Service.napLai()` đọc FAQ/catalog thành record nguồn rồi tạo chỉ mục vector trong RAM. Khi hỏi, chọn đoạn phù hợp từ nguồn đã kiểm, đọc lại giá/tồn của SKU nếu cần và trả `TraLoi`. Trang Admin dùng `TrangThai` như **AD-14**. Trang `/tu-van-da` dùng chat model cho xếp hạng sản phẩm và không phụ thuộc việc nạp chỉ mục `/chat`.

**Lỗi và quay lại.** Thiếu model hoặc Ollama lỗi thì trang Admin báo không thể nạp kho. `/chat` có thể dùng FAQ nếu chưa có chỉ mục hoặc khi provider lỗi. Rời trang nạp bằng sidebar. Sau khởi động lại, nạp lại chỉ mục trước khi test nhánh RAG.

### 12.8. Phân quyền, giao diện và lỗi

#### QA-01

**Luồng và dữ liệu.** `Security_Config` kiểm route theo vai trò trước controller. Principal `PhienNguoiDung` giữ thông tin phiên từ entity `NguoiDung`. `TrangThaiTaiKhoan_Filter` dùng projection `TrangThaiPhien` để kiểm còn hoạt động, còn đúng phiên bản và đúng vai trò. Template dùng quyền để chọn link. Link bị ẩn hay hiện không thay thế kiểm tra ở backend.

**Lỗi và quay lại.** Khách mở route bảo vệ bị đưa sang login. Phiên sai vai trò nhận 403. Phiên hết hiệu lực bị chuyển `/login?inactive` với **Lỗi: Phiên đã hết hiệu lực. Vui lòng đăng nhập lại.** Từ trang 403 bấm **Về trang chủ** hoặc đăng nhập bằng tài khoản test phù hợp.

#### QA-02

**Luồng và dữ liệu.** `QuyenDonHang_Service`, `DoiTra_Repository` và `DiaChiNguoiDung_Repository` kiểm chủ sở hữu khi mở đơn, yêu cầu và địa chỉ. Chi tiết đơn/đổi trả dùng record. Form địa chỉ dùng DTO, còn danh sách địa chỉ truyền entity của đúng người. Public catalog kiểm hoạt động của SKU, sản phẩm, danh mục và thương hiệu.

**Lỗi và quay lại.** URL của tài khoản test khác trả 404, không hiện dữ liệu của người đó. Slug không còn công khai cũng trả 404. **Xem sản phẩm** đi tới catalog, còn **Back** quay về lịch sử của đúng phiên test.

#### QA-03

**Luồng và dữ liệu.** Cùng controller, DTO, entity và HTML với ca tương ứng. Breakpoint CSS thay cách sắp xếp. Menu Admin mobile dùng `details`, Dashboard dùng bảng và canvas. Font lấy token và fallback theo hệ điều hành.

**Lỗi và quay lại.** Kẹt nút, ẩn thông báo, cuộn ngang toàn trang hoặc bố cục tràn ở zoom 200% là kết quả cần ghi. Một bảng có cuộn trong khung cần phân biệt với toàn trang bị tràn. Thay kích thước hoặc zoom không đổi route.

#### QA-04

**Luồng và dữ liệu.** Header có skip link tới `#main`. Menu và bộ lọc dùng `details`, form đánh giá dùng radio, hộp thoại giỏ dùng fragment `confirmDialog`. Nút **Quay lại** trong hộp thoại chỉ hủy việc gửi ở trình duyệt. `DanhGia_Form` hoặc tham số giỏ chỉ được gửi khi xác nhận thao tác tương ứng.

**Lỗi và quay lại.** Nếu Enter kích hoạt nhầm form hoặc focus biến mất, ghi URL và nút cuối cùng. Skip link phải tới nội dung chính; nếu trang thiếu đích thì ghi đúng trang đó. Phím đóng/hủy hộp thoại không được thay đổi giỏ.

#### QA-05

**Luồng và dữ liệu.** Khi tắt JS, form Thymeleaf vẫn gửi cùng query hoặc DTO. `GioHang_Controller` nhận form truyền thống, checkout dùng `Checkout_Form`, menu Admin mở bằng `details`. Dashboard đọc cùng record `TongQuan` để render bảng HTML mở sẵn.

**Lỗi và quay lại.** Nút cộng/trừ giỏ tự động và canvas có thể không hiện. Dùng **Cập nhật** giỏ, **Cập nhật báo giá** và link điều hướng như bình thường. Xóa giỏ qua form có thể gửi ngay vì không có hộp thoại JS. Lỗi từ server vẫn hiện trên form hoặc trang đích redirect.

#### QA-06

**Luồng và dữ liệu.** `DanhGia_Store` chọn `MongoDanhGia_Store` hoặc `MariaDbDanhGia_Store` theo cấu hình. MongoDB dùng `DanhGia_Document` nội bộ. Các trang đều đọc cùng record công khai, Admin và tóm tắt từ giao diện store. Nếu mất kết nối MongoDB, service đặt cờ lỗi cho view đánh giá hoặc bỏ số sao trên các thẻ catalog.

**Lỗi và quay lại.** Chi tiết sản phẩm và Admin đánh giá báo tạm thời chưa tải được. Form Customer giữ nội dung khi lưu gặp sự cố. Mở lại trang sau khi khôi phục kết nối để kiểm. Chuyển sang kho MariaDB không đồng bộ tự động dữ liệu từ MongoDB; đánh dấu rõ kho đang dùng khi ghi kết quả.

#### QA-07

**Luồng và dữ liệu.** Form multipart bind `BienTheSanPham_Form.hinhAnh` kiểu `MultipartFile`. `AnhBienTheSanPham_Service` kiểm ảnh và giới hạn. Nếu parser từ chối trước controller, `AnhTaiLen_Advice` xử lý 413 và có thể dựng lại form SKU hợp lệ cho Admin. Khi xem ảnh đã lưu, `AnhBienTheSanPham_Controller` trả nội dung tệp nếu đường dẫn còn được SKU tham chiếu. Trình duyệt nhận ảnh, còn entity chỉ được dùng ở server.

**Lỗi và quay lại.** Ảnh sai có lỗi cạnh ô tệp. Với lỗi quá dung lượng trước khi bind, form dựng lại có cảnh báo và dữ liệu vừa nhập có thể phải nhập lại. Nếu không dựng được form, trang 413 hiện **Tệp tải lên quá lớn**. Ảnh không tồn tại hoặc không còn tham chiếu trả 404. Từ form có thể bấm **Quay lại SKU** rồi mở lại mục cần sửa.
