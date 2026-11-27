# BIZ-UX-01 — Rà soát nghiệp vụ và tối ưu UI/UX BeautyShop

## Cách sử dụng

Đặt file này tại `docs/prompts/PROMPT-BIZ-UX-01-BeautyShop.md` trong dự án. Mở Codex ở thư mục gốc có `pom.xml`, rồi giao:

> Đọc và thực hiện `docs/prompts/PROMPT-BIZ-UX-01-BeautyShop.md`. Rà soát, tái hiện, sửa các lỗi có căn cứ, tối ưu UI/UX và kiểm chứng theo phạm vi trong file. Tiếp tục qua các chặng đã được giao; không dừng chỉ để trình bày kế hoạch. Giữ working tree, không commit/push/deploy. Khi gần hết phiên, cập nhật checkpoint để phiên khác tiếp tục được.

Prompt được soạn từ việc đọc repository `tuanank5/cosmetics_shop`, nhánh `main`, commit `c79983a58071d85cb9345c609576ef5b7d7cd61f` ngày 26/11/2026. Đây là mốc tham chiếu để hiểu cấu trúc và những thay đổi G7–G13; khi thực hiện phải đọc HEAD và working tree thực tế. Không checkout/reset về mốc này để ghi đè thay đổi mới.

Việc soạn prompt chỉ gồm đọc mã nguồn, tài liệu và trạng thái GitHub Actions; chưa chạy ứng dụng, Maven, cơ sở dữ liệu hoặc kiểm thử trình duyệt mới. Các mục “cần tái hiện” bên dưới không phải lỗi runtime đã được xác nhận.

---

## 1. Nhiệm vụ và kết quả cần đạt

Bạn phụ trách rà soát toàn bộ hành trình mua hàng và quản trị của BeautyShop, sửa lỗi có bằng chứng, rồi cải thiện tính dễ dùng, tính nhất quán và hiệu năng giao diện.

Ưu tiên theo thứ tự:

1. Đúng quyền truy cập, tiền, tồn kho, giao dịch và dữ liệu.
2. Người dùng hoàn thành được mua hàng, quản lý tài khoản và thao tác quản trị, kể cả khi có lỗi.
3. Giao diện rõ, nhất quán, responsive, dùng được bằng bàn phím và không phụ thuộc hoàn toàn vào JavaScript.
4. Hiệu năng và cách tổ chức code được cải thiện bằng số đo hoặc vấn đề cụ thể.

Thực hiện chuỗi **đọc → đối chiếu yêu cầu → tái hiện → sửa → kiểm thử → báo cáo**. Được sửa controller, service, repository, DTO, template, CSS, JavaScript và test liên quan để khắc phục lỗi trong phạm vi đã thống nhất. Chỉ sửa phần cần thiết, giữ hợp đồng công khai và hành vi đúng đang có.

Không tự triển khai thêm chính sách thanh toán/hoàn tiền, thay đổi cách tính tiền, mở quyền vai trò, đổi kiến trúc xác thực, hoặc xây lại website bằng framework khác. Nếu phát hiện yêu cầu nghiệp vụ mâu thuẫn hoặc chưa được quyết định, ghi hai phương án và tác động, tiếp tục các việc độc lập; chỉ hỏi phần thật sự cần owner quyết định sau khi đã có bằng chứng cụ thể.

Không dừng sau một báo cáo audit nếu còn các sửa lỗi và cải thiện đã được giao, đủ căn cứ và có thể kiểm chứng. Không coi “không có lỗi JavaScript” hoặc “không tràn ngang” là đủ để nghiệm thu toàn bộ UI/UX.

## 2. Tiếp nhận dự án và bảo toàn công việc hiện có

### 2.1. Kiểm tra môi trường trước khi chạy

Chạy các lệnh riêng, phù hợp shell hiện tại:

```text
git status --short
git rev-parse HEAD
git log -1 --oneline
java -version
mvn -version
```

- Ghi nhận HEAD, branch, diff và file chưa được theo dõi trước khi sửa. Giữ nguyên thay đổi của người dùng, không tự stash/reset/checkout đè hoặc `git clean`.
- JDK của `java` và Maven phải cùng dùng Java 21 theo dự án. Tìm đường dẫn công cụ trên máy, không chép cứng đường dẫn JDK/MariaDB từ báo cáo hay script của máy khác.
- Đọc cấu hình, `.env.example`, biến môi trường và các script trước khi chạy. Chỉ ghi tên biến, trạng thái có/thiếu và đích test đã che thông tin nhạy cảm; không in mật khẩu, OAuth secret, token, OTP hoặc nội dung `.env`.
- Kiểm biến gate integration đang kế thừa trong shell trước cả `mvn test`. Không suy rằng lệnh test mặc định chắc chắn không ghi DB.
- Không commit, push, merge, deploy, gửi thư thật hoặc thực hiện thanh toán thật trong task này. Không khởi chạy dịch vụ cổng đã bị tiến trình khác sử dụng và không dừng tiến trình không do task tạo.

### 2.2. Tài liệu cần đọc

Đọc trước: `AGENTS.md`, `README.md`, `pom.xml`, `docs/requirements/01-srs.md`, `docs/requirements/02-business-rules.md`, `docs/DECISIONS.md`, `docs/design/01-architecture.md`, `docs/design/02-erd.md`, `docs/design/03-order-payment-inventory.md`.

Đọc báo cáo mới để hiểu hành vi có chủ ý và phạm vi QA đã có:

- `docs/reviews/BEAUTYSHOP_IMPLEMENTATION_PROGRESS.md`.
- `docs/reviews/AUTH-02-nghiem-thu-xac-thuc.md`.
- Các báo cáo `NAV-01`, `UI-01C`, `UI-01P`, `CART-01`, `CAT-01`, `WISH-01`, `ORDER-01`, `REVIEW-01`, `DTO-01`, `QA-01` trong `docs/reviews/`.
- `docs/reviews/LOCAL-SETUP-01-khoi-tao-va-nang-cap.md`.
- `docs/reviews/UI-COPY-01-ket-qua.md` và `docs/reviews/SKIN-UI-01-chuan-hoa-tu-van-da.md`.
- `docs/deployment/01-local-db-and-flyway.md` và các hướng dẫn dịch vụ ngoài khi cần.

Nếu đường dẫn đã đổi, tìm bằng `rg --files`/`rg`, không đoán nội dung. Đọc theo từng cụm nghiệp vụ, không đổ toàn bộ repository và mọi log vào một lượt. Báo cáo cũ dùng để hiểu lịch sử; kết luận hiện tại phải đối chiếu code và môi trường đang kiểm.

### 2.3. Kiến trúc cần giữ

- Java 21, Spring Boot 3.5.16, Spring MVC/Thymeleaf, Spring Data JPA và các truy vấn JDBC, MariaDB/Flyway; một monolith Maven.
- Spring Security Form Login + Session + BCrypt; Google/GitHub OAuth tùy cấu hình. Guest là anonymous; Customer và Admin có quyền riêng.
- MongoDB lưu nội dung đánh giá theo quyết định DATA-01/D32. Redis phục vụ các chức năng xác thực/giới hạn tần suất đã có.
- Giao diện SSR, JavaScript bổ sung khả năng tương tác. GSAP, Swiper, Chart.js đã có artifact cục bộ; giữ bản pin và provenance. Không thêm React/Vue/Tailwind/htmx hoặc nâng dependency hàng loạt chỉ để “hiện đại hóa”.
- Storefront giữ Dark Luxury đen/charcoal, chữ sáng, điểm nhấn gold muted đã được duyệt; không đưa nền be trở lại. Admin giữ hệ U1 hiện tại, cải thiện tính rõ ràng và nhất quán trong hệ đó. Không thay toàn bộ token U1 chỉ vì tìm thấy mã màu be trong `tokens.css`.
- Giữ URL, tên tham số, POST contract, CSRF, PRG, khóa trạng thái và dữ liệu server. Khi cần thay hợp đồng, chứng minh lý do và kiểm toàn bộ nơi gọi.

## 3. Lập bản đồ và hồ sơ vấn đề trước khi sửa

Lập bản đồ ngắn: vai trò → route → controller → service → repository/store → template/JS → test. Mỗi luồng ghi dữ liệu đầu vào, nguồn giá/tồn, điều kiện quyền, trạng thái thành công/lỗi, dịch vụ phụ thuộc và nơi dữ liệu được ghi.

Mỗi vấn đề có:

| Trường | Nội dung phải ghi |
|---|---|
| ID và ưu tiên | Ví dụ `BIZ-001`, `UX-001`, `PERF-001`; P0/P1/P2/P3 kèm tác động |
| Bối cảnh | HEAD + diff, vai trò, route, dữ liệu và trạng thái tiền điều kiện |
| Bằng chứng | File và dòng, test hoặc các bước tái hiện, expected/actual; ảnh/trace nếu cần |
| Phân loại | `CONFIRMED_STATIC`, `REPRODUCED`, `NEEDS_REPRO`, `NOT_REPRODUCED`, `BLOCKED` |
| Cách xử lý | Nguyên nhân, sửa tối thiểu, phạm vi ảnh hưởng, test và kết quả hồi quy |
| Trạng thái sau sửa | `FIXED_UNVERIFIED`, `VERIFIED_FIX`, hoặc lý do còn mở |

P0 là sai quyền, mất dữ liệu, sai tiền/kho hoặc giao dịch bị xử lý sai nghiêm trọng. P1 là chặn hoàn thành luồng chính hoặc cho trạng thái dễ dẫn tới thao tác sai. P2 là khó dùng/không nhất quán/hiệu năng có tác động đã đo. P3 là tinh chỉnh nhỏ. Không gán mức độ chỉ để làm danh sách có vẻ nghiêm trọng.

Giữ riêng **lỗi hành vi**, **khoảng trống yêu cầu** và **đề xuất thiết kế**. Không ghi “đã phát hiện oversell/IDOR/double charge” nếu mới có một giả thuyết đọc code.

## 4. Những điểm cần kiểm tra ngay từ mã tham chiếu

Các điểm dưới giúp bắt đầu audit; vẫn đọc lại code mới nhất trước khi sửa.

1. **Nhãn footer và đích liên kết.** `templates/fragments/footer.html` gọi `/account/returns` là “Chính sách đổi trả và hoàn tiền”; các nhãn hướng dẫn/chính sách khác dẫn catalog hoặc Home. Kiểm theo từng vai trò. Đây là khác biệt xác nhận từ template và đã có lịch sử backlog, chưa chứng minh là hồi quy mới. Đổi nhãn cho đúng chức năng hoặc dùng nội dung đã được owner xác nhận. Không tạo trang chính sách rỗng, bịa thông tin pháp lý, địa chỉ, hotline hay mạng xã hội để lấp chỗ trống.
2. **Ý định tư vấn trên Home.** `home/index.html` có ví dụ câu hỏi đổi trả/chính sách nhưng nút “Hỏi BeautyShop” dẫn `/tu-van-da`. Làm rõ ánh xạ hỏi sản phẩm/chính sách → `/chat`, gợi ý theo loại da → `/tu-van-da`; kiểm nhãn Home/header/footer và giữ riêng hai endpoint. Không bắt buộc nhân đôi CTA ở mọi vị trí.
3. **Giỏ hàng khi mạng bất thường.** `static/js/cart.js` ẩn nút cập nhật khi enhancement bật; request chưa có timeout riêng; trạng thái request theo dòng nhưng mỗi phản hồi cập nhật cả tạm tính/badge. Tái hiện request treo, cập nhật hai SKU, đảo thứ tự phản hồi, mất phản hồi sau khi server đã ghi và hai tab. Chỉ sửa lỗi đã chứng minh. Không giải quyết bằng tự tính tiền trên JS hoặc POST lại mù quáng khi chưa rõ kết quả lần trước.
4. **Phạm vi URL và CSS.** Một số selector/check trong `home-carousel.js`, `cart.js`, `home-story.js`, `catalog-preview.js` giả định URL bắt đầu tại `/`. Nếu deployment có context path, kiểm cả SSR lẫn enhancement với prefix; nếu không thuộc hợp đồng, ghi giới hạn thay vì mở rộng triển khai ngoài phạm vi. `fragments/head.html` tải CSS storefront/admin chung: đo lượng tải và CSS dùng thực tế trước khi tách, bảo toàn shared components và tránh nháy giao diện.
5. **Flyway V5/D37.** V5 FAQ đổi checksum là quyết định đã duyệt cho database mới, không phải thay đổi vô tình để tự revert. Phân loại DB trống, DB có history khớp và DB từng áp dụng V5 cũ. Không `repair`, sửa `flyway_schema_history`, bật baseline, drop/reset DB để làm startup xanh. Nếu cần đường nâng cấp giữ dữ liệu cũ, phân tích trên bản sao và ghi là quyết định riêng cần giải quyết.
6. **Bằng chứng CI mới hơn tài liệu.** Tại lúc soạn prompt, Maven CI của `c79983a` đã thành công, trong khi vài báo cáo còn mô tả working diff của `38fb1ca`. Kiểm Actions theo SHA hiện tại; không sao chép trạng thái BLOCKED hoặc PASS cũ. CI hiện chỉ chạy Maven test/package, chưa tự chứng minh MariaDB/Mongo/nhà cung cấp thật.

## 5. Rà soát nghiệp vụ theo hành trình

### 5.1. Tài khoản, xác thực và quyền sở hữu

Đọc `security/Security_Config.java`, `LoginSuccess_Handler.java`, `OAuth2NguoiDung_Service.java`, `TrangThaiTaiKhoan_Filter.java`, `service/NguoiDung_Service.java`, `DanhTinhOAuth_Service.java`, `EmailVerification_Service.java`, `MatKhau_Service.java` cùng controller/template liên quan.

- Đăng ký bằng địa chỉ email hợp lệ, không giới hạn tùy tiện ở `gmail.com`; phân biệt đăng ký bằng email với đăng nhập Google.
- Kiểm email trùng, xác minh chưa hoàn tất, OTP sai/hết hạn/vượt số lần/gửi lại, SMTP/Redis gián đoạn; reset token sai/hết hạn/đã dùng; không lộ sự tồn tại của tài khoản ngoài hợp đồng đã thống nhất.
- OAuth nhận diện theo provider + subject. Kiểm email trùng, thiếu email đã xác minh, user bị khóa, link/unlink/reauth và đường đăng nhập còn lại. Không tự gộp tài khoản theo email để giảm số bước.
- Khóa tài khoản/đổi credential phải làm phiên cũ mất hiệu lực đúng contract. Admin không tự khóa mình; Customer không nhận quyền admin qua request.
- Guest login để tiếp tục checkout phải quay đúng đích và gộp giỏ đúng một lần. LINK/REAUTH không được vô tình chạy lại nghiệp vụ login/merge.
- Kiểm IDOR bằng request trực tiếp cho địa chỉ, đơn, order item đánh giá, đổi trả; ẩn nút không thay kiểm quyền backend. Kiểm redirect nội bộ, CSRF và logout.
- Khóa HMAC OTP hiện sinh theo process: nếu audit restart/resend hoặc nhiều instance, tái hiện tác động rồi ghi giới hạn đúng môi trường. Không tự xây hệ phân tán khi mục tiêu chỉ là demo một instance.

### 5.2. Catalog, SKU, giá, ảnh và tìm kiếm

Đọc `CuaHang_Controller`, `CuaHang_Service`, `CuaHang_Repository`, `product/list.html`, `product/detail.html`, `fragments/product-card.html`.

- Visibility phải xét trạng thái danh mục, thương hiệu, sản phẩm và SKU. Sản phẩm ngừng bán không lọt qua search, Home, wishlist, chi tiết hoặc gợi ý.
- Bộ lọc/tìm kiếm/sort/pager dùng cùng điều kiện với tổng kết quả. Kiểm chuỗi rỗng, Unicode tiếng Việt, `%`, `_`, tham số sai, trang vượt, giá bằng nhau và thứ tự ổn định.
- Giữ `q`, `brand`, `category`, `skinType`, `sort`, `page`, `size`, `filterPanel`; khi mở PDP và quay lại phải giữ ngữ cảnh hợp lệ.
- Giá “từ” và SKU mặc định phải khớp policy đang có. Chọn SKU bằng keyboard không gửi GET chỉ do nhận focus. `variant` sai hoặc không thuộc sản phẩm không được dùng để mua SKU khác.
- SKU hết hàng vẫn có thông tin rõ nhưng không cho mua vượt tồn. Phân biệt trong kho, đang giữ và còn bán được.
- Kiểm ảnh thiếu/404/lỗi giải mã, ảnh upload hợp lệ, kích thước layout ổn định; không đánh đồng placeholder với ảnh sản phẩm thật đã nghiệm thu.

### 5.3. Giỏ Guest/Customer và gộp giỏ

Đọc `GioKhach`, `ThongBaoGopGio`, `GopGioNotice_Config`, `GioHang_Service`, `GioHang_Repository`, `cart/index.html`, `cart.js`.

- Giỏ nhận diện theo SKU; kiểm số lượng âm/0/quá lớn/không phải số, SKU ẩn/xóa/hết, tồn giảm hoặc giá thay đổi giữa hai lần mở trang. Giữ contract `quantity=0` và bước xác nhận xóa tương ứng.
- Gộp snapshot UUID một lần, khóa theo thứ tự thống nhất, ghi nhận V3 và cập nhật giỏ trong cùng transaction. Kiểm hai phiên đăng nhập đồng thời, retry sau rollback, lỗi ở SKU sau, refresh/callback lặp và đổi tài khoản.
- Thông báo điều chỉnh sau gộp thuộc đúng phiên/đích, không bị tab khác lấy mất, không phát lại sau Back/refresh/logout. Giữ contract CART-01 về dòng còn số lượng dương và thông báo chung cho SKU không còn mua được.
- Enhancement phải giữ tạm tính/badge/giá dòng đúng server, phản hồi lỗi rõ, phục hồi focus, xử lý response cũ và trạng thái không rõ. Có cách thao tác tiếp khi mạng treo/JS lỗi; no-JS vẫn dùng form server.

### 5.4. Quote, đặt đơn, voucher và tồn kho

Đọc `Checkout_Controller`, `DonHang_Service`, `TonKho_Service`, `PhieuGiamGia_Service`, các repository tương ứng, `ShippingFee_Policy`, `OrderState_Policy`, `order/checkout.html`, `checkout.js`.

- Tiền dùng BigDecimal/DECIMAL do server tính. Kiểm chèn giá/discount/total giả trong request và địa chỉ thuộc người khác.
- Chốt snapshot giá, SKU, địa chỉ, giảm giá, ship. Quote cũ không thay thế việc đọc và xác thực lại lúc đặt đơn. Nếu quote lỗi, không hiển thị tổng chưa xác nhận như số tiền cuối cùng; giữ dữ liệu hợp lệ để người dùng sửa.
- Kiểm voucher FIXED/PERCENT, thời gian/UTC, minSpend, maxDiscount, số lượt used/reserved, hai khách dùng lượt cuối, voucher bị ngừng khi đang có lượt giữ. Giữ quy tắc làm tròn và thứ tự tính tiền hiện hành.
- Ship 25.000 đồng, miễn từ 500.000 đồng sau voucher theo policy hiện tại; kiểm các biên 499.999/500.000/500.001. Không đổi ngưỡng để làm UI thuận mắt.
- COD xuất kho trong transaction khi tạo đơn; online giữ tồn/lượt voucher. Mọi kết quả duy trì `0 <= reserved <= on_hand` và không bán vượt available.
- Tái hiện hai người mua SKU cuối, nhiều SKU có lỗi ở dòng cuối, submit kép/refresh/retry khi mất phản hồi. Đánh giá chống trùng phía server; nút bị disable chỉ là bảo vệ UX.
- Địa chỉ mới/sửa từ checkout phải quay lại đúng luồng; không mất mã voucher hoặc lựa chọn đang còn hợp lệ mà không giải thích.

### 5.5. MoMo, trạng thái đơn, hủy và đổi trả

- Đọc `ThanhToanMomo_Service`, `ThanhToanMomo_Repository`, `Momo_Controller`, `MomoReconciliation_Job`, các adapter/settings trước khi sửa giao diện thanh toán.
- Browser return không được đánh dấu PAID. Kiểm chữ ký/merchant/attempt/order/amount/currency theo adapter hiện có, callback lặp, transId trùng, nhiều attempt, thành công muộn, lỗi gateway và đối soát.
- Không giải phóng giữ hàng chỉ vì đồng hồ vượt 15 phút nếu kết quả giao dịch còn chưa rõ. Không xuất kho/consume/release voucher hai lần.
- Mock phải có nhãn rõ; không tuyên bố MoMo sandbox thật hoặc production đã đạt từ mock.
- Hủy hiện thực ở `DonHang_Service` chỉ cho COD UNPAID ở PENDING_CONFIRMATION/CONFIRMED. Kiểm UI/FAQ hiển thị đúng điều kiện. Không tự mở hủy MoMo/PAID hoặc hoàn tiền khi chưa có nghiệp vụ được duyệt.
- Đổi trả kiểm chủ đơn, DELIVERED, thời hạn 7 ngày và tổng số lượng qua các yêu cầu; concurrent request không vượt số đã mua. Giữ trạng thái đơn, thanh toán và đổi trả tách biệt.
- Trạng thái hoàn tất đổi trả không tự đồng nghĩa hoàn tiền hay nhập kho. Dùng câu chữ đúng hành vi server, tránh thông báo hứa việc chưa thực hiện.

### 5.6. Wishlist và đánh giá

- Wishlist đặt trạng thái mong muốn, không đổi thành toggle dễ đảo sai khi retry. Kiểm cùng sản phẩm xuất hiện ở nhiều vùng, hai tab, request lặp, 403/session hết hạn, 409 khi sản phẩm không còn hiển thị, bỏ thẻ cuối và fallback no-JS.
- Review chỉ từ order item thuộc Customer trong đơn DELIVERED, một review/item; kiểm sửa/duyệt với revision cũ và không công khai nội dung chưa duyệt.
- SQL kiểm điều kiện mua hàng, Mongo lưu nội dung. Giữ unique index/revision; Mongo lỗi phải báo kho đánh giá tạm không khả dụng, không giả thành 0 review và không ghi SQL làm đường dự phòng.
- Dashboard/card/PDP dùng cùng định nghĩa rating được duyệt. Không để bài bị ẩn tiếp tục được tính do cache hoặc projection khác nhau.

### 5.7. Quản trị và thống kê

Đọc controller/service/repository quản trị; `BoLocDonHangAdmin`, `NguonDieuHuong`, `ThongKe_Service`, `ThongKe_Repository`, `admin/dashboard.html`, `dashboard.js`.

- Bảo toàn D21: danh mục, thương hiệu, sản phẩm ngừng/kích hoạt bằng trạng thái theo contract mới; lịch sử đơn và SKU phải còn nguyên. Kiểm visibility của toàn bộ cây quan hệ.
- Upload kiểm MIME/nội dung/phần mở rộng/dung lượng/path traversal và đường phục vụ; thông báo lỗi dễ hiểu, không lộ path máy chủ.
- Admin order list phải có một dòng/đơn dù nhiều lần thanh toán; count/pager/filter cùng predicate. Giữ `status` lặp, method, paymentStatus, q, sort, page, size và ngữ cảnh dashboard khi xem chi tiết rồi quay lại.
- Kiểm thao tác làm đơn/review rời tập lọc hoặc xóa dòng cuối trang: chuyển về trang hợp lệ và đưa focus tới vị trí có ý nghĩa.
- Dashboard phân biệt doanh thu đã thu, giá trị dòng hàng, số đơn, AOV, tỉ lệ hủy, tồn hiện hành; không gọi doanh thu là lợi nhuận. Đối chiếu bảng server với dataset biểu đồ và bộ lọc ngày/timezone.
- Dữ liệu rỗng/lỗi khác dữ liệu 0. Bảng số liệu vẫn đọc được khi Chart.js lỗi hoặc JavaScript tắt.

### 5.8. FAQ, RAG và tư vấn theo da

- `/chat` dùng `templates/TuVanDa.html`; `/tu-van-da` dùng `templates/chat/TuVanDa.html`. Kiểm riêng hai luồng và giải thích đúng chức năng trên UI.
- Bảo toàn D35 cho `/chat`: câu hỏi bị từ chối/validation/429 không bị điền lại qua form hoặc BindingResult; kiểm bằng dữ liệu tổng hợp và xác nhận không có request embedding/chat ngoài phạm vi cho phép.
- Luồng tư vấn da nhận mô tả tự do và có hành vi lỗi khác. Audit dữ liệu gửi provider, giới hạn đầu vào, timeout/rate limit và sự minh bạch với người dùng. Không tự coi D35 đã áp dụng cho cả hai hoặc gộp route để che khác biệt chính sách.
- Provider lỗi/tắt/chậm, không có SKU phù hợp, model trả ID ngoài tập, nội dung ngoài nguồn, HTML không an toàn: phản hồi phải đúng khả năng hiện có. Không biến nội dung model thành lệnh ghi, không chẩn đoán y khoa hay tạo giá/tồn giả.
- RAG chọn nguồn và đọc lại giá/tồn; tư vấn da chọn trong tập sản phẩm hợp lệ; gợi ý phổ biến theo rule. Không mô tả tất cả là một hệ AI cá nhân hóa đã được kiểm chứng.

## 6. Tối ưu UI/UX có mục tiêu và ảnh đối chiếu

### 6.1. Bộ khung chung

- Rà `fragments/{head,header,footer,ui,nav-fields,product-card,wishlist}.html`, CSS `tokens/base/components/storefront/admin` và `app.js` trước khi vá từng màn.
- Giữ nhịp spacing, phân cấp chữ, tỷ lệ ảnh, trạng thái button/input, màu thông báo và focus nhất quán. Chỉ tạo/thay token khi có nhu cầu chung rõ; không nối thêm nhiều lớp override `!important` cuối file để che nguyên nhân cascade.
- Nhãn tiếng Việt rõ, UTF-8; giữ chuẩn hóa UI-COPY-01. Tách nội dung người dùng cần biết khỏi chi tiết kỹ thuật. Lỗi ghi hành động có thể làm tiếp, không chỉ “có lỗi xảy ra”.
- Storefront sang trọng nhưng giá, SKU, tồn, lựa chọn và CTA vẫn rõ. Admin ưu tiên scan bảng, tìm kiếm, lọc, thao tác và trạng thái, không thêm animation trang trí cản công việc.
- Kiểm contrast trạng thái thường/focus/error theo tiêu chí WCAG 2.2 áp dụng; disabled phải dễ nhận biết và được đánh giá theo đúng ngoại lệ, không áp cùng ngưỡng tùy tiện. Mục tiêu dự án cho nút icon quan trọng trên mobile là 44×44 CSS px; CTA mua 56 px khi bố cục phù hợp. Không gọi 44 px là ngưỡng tối thiểu AA của WCAG và không coi một số đo nút là chứng nhận accessibility.
- Label thật cho field, lỗi gắn trường, error summary có link/focus; thông báo động dùng live region phù hợp. Modal có focus management và đóng được bằng bàn phím; nội dung ẩn không còn trong Tab order.

### 6.2. Home và chuyển động

Giữ sáu chương, các vùng sản phẩm và số đếm lấy từ server. Cải thiện thứ tự thông tin, khả năng hiểu CTA và khả năng đến catalog; không rút thành landing page demo thiếu chức năng.

- Carousel chỉ bật khi đủ điều kiện. Nội dung cao, mobile phù hợp fallback, reduced-motion, vendor CSS/JS thiếu phải dùng cuộn thường với đủ nội dung; không ép `height:100vh` và `overflow:hidden` để đạt số đo.
- Kiểm wheel 1→6→footer→6→1, keyboard, pagination, menu mở, touch, resize, anchor/direct link và Back/Forward. Logo mở đầu khác phục hồi vị trí quay lại; giữ `home=opening` và anchor sản phẩm của NAV-01.
- Không chặn wheel toàn trang; không để focus lọt vào chương đang ẩn hoặc CTA bị keyboard che.
- Video hover/scroll giữ allowlist và nguồn có quyền, chỉ phát khi thích hợp; pause ngoài viewport/đổi tab, tôn trọng reduced-motion/saveData, xử lý autoplay reject/404/decode lỗi. Không dùng clip QA làm media thương phẩm và không thêm video giả để tăng điểm giao diện.

### 6.3. Các màn mua hàng và tài khoản

- Catalog: filter mở/đóng dễ hiểu, chip bỏ từng tiêu chí, count đúng server, reset/pager giữ trạng thái; card gọn nhưng đủ thông tin để quyết định.
- PDP: chọn SKU rõ, giá/tồn cập nhật đúng, ảnh ổn định, số lượng hợp lệ, CTA dễ tìm; thông báo thêm giỏ không làm mất vị trí hoặc dẫn sang biến thể khác.
- Cart: tên/ảnh/biến thể/link đúng; tăng giảm, cập nhật, xóa và thông báo điều chỉnh tồn dễ hiểu; tạm tính phản ánh phản hồi cuối hợp lệ của server.
- Checkout: nhóm địa chỉ/phương thức/voucher/tổng tiền có thứ tự; lỗi giữ dữ liệu hợp lệ; không có CTA giả, loading vô hạn hoặc tổng tiền chưa xác nhận trông như đã chốt. Cảnh báo rõ khi phiên hết hạn/mạng lỗi.
- Auth/account: đường đăng ký/đăng nhập/reset/liên kết OAuth dễ phân biệt; password toggle có tên truy cập; autocomplete phù hợp; Back khôi phục nút gửi; sửa địa chỉ từ checkout có đường quay lại.
- Đơn/đổi trả: hiển thị trạng thái kèm hành động thực sự được phép, timeline dựa dữ liệu có thật, empty state có đường tiếp tục phù hợp.

### 6.4. Đánh giá trực quan độc lập với functional QA

Chụp trước/sau cùng dữ liệu, vai trò, viewport và trạng thái. Nhận xét cụ thể về hierarchy, typography, spacing, mật độ thông tin, vị trí CTA, tỷ lệ ảnh và tính nhất quán. Phải mở và xem ảnh, không chỉ kiểm chiều rộng DOM.

Nếu dùng ảnh Byredo đã được cung cấp làm tham chiếu, ghi rõ vùng được tham khảo và khác biệt chủ ý. Không tính “giống 80%” bằng số component PASS; không khẳng định đã xem reference nếu mạng chặn hoặc thiếu ảnh.

## 7. Hiệu năng và chất lượng code

- Đo trước khi tối ưu: số query theo request, thời gian response, số bản ghi đọc, request/transfer CSS–JS–ảnh, lỗi asset, long task, layout shift và kích thước DOM ở màn chậm thực tế.
- DTO-01 đã có projection và giảm dữ liệu đọc. Tái kiểm ở vùng sửa với dữ liệu đủ để thấy N+1; không gọi mọi truy vấn JPA là N+1 hoặc thêm cache/index khi chưa có query plan/căn cứ.
- Bounded pagination; count đúng; tránh tải cả danh sách rồi lọc trong Java/template. Giữ query parameter bằng bind, escape ký tự wildcard theo contract.
- Lazy-load ảnh dưới màn hình, giữ kích thước ảnh/poster; không lazy-load vô điều kiện ảnh LCP. Không tải trước toàn bộ video hoặc library chỉ dùng ở màn khác.
- Nếu tách CSS/JS theo trang, kiểm shared shell, no-JS, tải lỗi và layout ban đầu. Không thay dependency hoặc thêm bundler nếu lợi ích không đo được.
- Ghi cùng thiết bị, phiên bản browser, viewport, dữ liệu, cache và throttling khi so sánh trước/sau. LCP/CLS có thể đo lab; INP cần tương tác và nguồn đo phù hợp. Không lấy TBT hoặc một điểm Lighthouse để kết luận INP/field Core Web Vitals đã đạt.
- Dọn code chết/trùng chỉ trong vùng tác động sau tìm caller và kiểm hồi quy. Không thực hiện refactor diện rộng để đổi phong cách đặt tên.

## 8. Kiểm thử an toàn và vừa đủ theo rủi ro

### 8.1. Database, migration và provider

- Tách MariaDB schema, Mongo database/URI, dữ liệu Redis và uploads của QA khỏi dữ liệu đang sử dụng. Chỉ đổi `DB_URL` là chưa đủ vì Mongo có thể vẫn trỏ `beautyshop_demo`.
- Đọc script tạo/seed/cleanup và schema đích trước khi chạy. Không chạy test ghi trên `beautyshop`, `beautyshop_demo` hoặc database khác có dữ liệu của owner chỉ vì tên giống fixture.
- Gate thực có gồm `P01_MARIADB_TEST`, `P23_MONGO_TEST`, `AUTH_02_MIGRATION_FIXTURE`, `LOCAL_BOOTSTRAP_MIGRATION_FIXTURE`; mỗi suite có điều kiện và DB đích riêng. Chỉ bật cờ cần dùng sau xác minh isolation, không bật tất cả trong shell rồi chạy bừa toàn bộ suite.
- Kiểm `DB_URL/DB_USER/DB_PASSWORD`, `MONGODB_URI/MONGODB_TEST_URI` và các cổng theo harness. Tránh chép cứng JDK/MariaDB/Chrome paths. Có thể sửa portability tối thiểu của harness và ghi rõ.
- Theo D37, kiểm startup DB mới và thông báo khi DB V5 cũ mismatch bằng bản sao phù hợp. Giữ `ddl-auto=validate`, không dùng `update`, `clean`, `repair` hoặc sửa lịch sử để vượt gate.
- Provider thiếu cấu hình: ghi BLOCKED riêng cho Google/GitHub thật, SMTP/Redis thật, MoMo merchant/HTTPS, Ollama thật; tiếp tục unit/MVC/mock và công việc không phụ thuộc. Không yêu cầu owner dán secret vào chat/log.
- Mailpit hoặc mock cục bộ được dùng để test gửi email; không gửi tới hộp thư thật khi chưa được giao rõ. Không tạo OAuth app/tài khoản hoặc gọi giao dịch thật tự động.

### 8.2. Lệnh và mức kiểm chứng

Sau khi xác minh môi trường và cờ test, baseline và cuối task chạy:

```text
mvn -B test
mvn -B -DskipTests package
git diff --check
```

Chạy focused tests sau từng sửa lỗi nghiệp vụ hoặc hành vi rủi ro. Bổ sung test cho lỗi có ý nghĩa: tiền/kho/quyền, transaction/idempotency, state, race, recovery hoặc contract bị ảnh hưởng. Không viết test chỉ kiểm một class CSS xuất hiện để làm đẹp số lượng.

Tái sử dụng test hiện có ở `src/test/java/com/beautyshop/` và harness trong `docs/qa/` sau đọc cấu hình. `docs/qa/QA-01/run-browser.ps1` có chọn nhóm; các nhóm NAV-01/UI-01P/CART-01/WISH-01/ORDER-01/REVIEW-01, SKIN-UI-01, UI-COPY-01 và P18 có script chuyên biệt. Chạy nhóm liên quan và smoke hành trình chính; không nhân toàn bộ mọi tổ hợp vô hạn.

Phân biệt unit/MVC, H2, MariaDB/Mongo cô lập, provider mock, provider thật và browser. Ghi tổng/đạt/lỗi/skip từ lần chạy thực tế, lệnh, exit code, commit + diff, log. Không cộng các bộ test giao nhau, không chuyển SKIPPED thành PASS, không chép số QA cũ sang kết quả mới. Nếu test lỗi do harness, sửa harness có căn cứ và giữ assertion bảo vệ đúng.

### 8.3. Ma trận browser

- Smoke Guest/Customer/Admin trên các hành trình thuộc quyền ở 390×844 và 1366×768.
- Component/màn thay layout kiểm thêm 360×800, 768×1024, 1920×1080.
- Kiểm zoom thật 200%, keyboard Tab/Shift+Tab/Enter/Space/Escape, focus, touch giả lập cho màn liên quan. Ghi rõ mô phỏng; không gọi `deviceScaleFactor`/DPR hoặc `transform:scale` là browser zoom.
- Kiểm JS tắt, reduced-motion, library/media bị chặn, mạng chậm, request lỗi/treo, double submit và Back/Forward cho enhancement bị sửa.
- Không tràn ngang toàn trang; bảng Admin rộng có thể cuộn trong vùng có nhãn và dùng được bằng keyboard. Không dùng `overflow-x:hidden` để che nội dung lỗi.
- Dữ liệu gồm rỗng, một dòng, nhiều trang, tên dài, Unicode, thiếu ảnh, hết hàng, giá/tồn đổi, phiên hết hạn, dịch vụ lỗi.
- Kiểm form server thật và request/response cho luồng ghi; HTML tĩnh không đủ nghiệm thu giỏ/checkout/auth.
- Nếu browser không dùng được, thử harness/headless sẵn có phù hợp. Vẫn không chạy được thì đánh dấu `NOT_RUN/BLOCKED_BROWSER`, hoàn thành phần độc lập và để hướng dẫn chạy lại; không viết “đã kiểm UI”.

## 9. Thứ tự triển khai và checkpoint

1. **Baseline:** xác nhận revision/môi trường, bản đồ luồng, đọc quyết định mới, test baseline và ảnh chọn lọc.
2. **Nghiệp vụ quan trọng:** tái hiện và sửa lỗi quyền/tiền/kho/transaction, auth, giỏ/checkout/đơn; xử lý các blocker trước.
3. **Hành trình và giao diện chung:** sửa nhãn/đích điều hướng, lỗi và recovery, tokens/components đúng scope, các state dùng chung.
4. **Màn cụ thể:** Home/catalog/PDP/cart/checkout/account/admin/tư vấn; làm theo mức ảnh hưởng đã tìm thấy.
5. **Hiệu năng và hồi quy:** tối ưu có số đo, kiểm đường mua hàng và vận hành, full Maven gate cuối khi code ổn định.
6. **Bàn giao:** cập nhật báo cáo, evidence và checkpoint, liệt kê phần đã sửa, phần kiểm chứng, phần thật sự còn mở.

Mỗi chặng có thể chia thành nhóm nhỏ để diff dễ xem. Không cần xin duyệt lại cho sửa lỗi thông thường đã nằm trong phạm vi. Nếu dùng sub-agent, có thể chia đọc/nghiệp vụ/UI/QA độc lập; không cho nhiều agent cùng sửa CSS chung hoặc cùng chạy trên một DB fixture.

Sau mỗi nhóm đã kiểm chứng, cập nhật `docs/reviews/BIZ-UX-01-tiep-tuc.md` gồm HEAD + diff, file đã sửa, issue đã đóng/mở, lệnh cuối và exit code, app/process do task tạo, fixture đang dùng đã che secret, bước tiếp theo cụ thể. Khi quota sắp hết, lưu checkpoint trung thực; lần tiếp tục kiểm working tree và tiếp bước dang dở, không làm lại baseline đã đủ bằng chứng nếu source/môi trường không đổi.

## 10. Đầu ra và điều kiện kết thúc

Lưu trong repository theo AGENTS:

- `docs/reviews/BIZ-UX-01-ra-soat-nghiep-vu-uiux.md`: bản đồ ngắn, danh sách issue, nguyên nhân/sửa, kết quả, giới hạn, bảng trước/sau.
- `docs/reviews/BIZ-UX-01-tiep-tuc.md`: checkpoint và hướng dẫn tiếp tục nếu còn việc.
- `docs/qa/BIZ-UX-01/`: script/JSON/log cần để tái lập, đã che dữ liệu nhạy cảm.
- `docs/ui/screenshots/BIZ-UX-01/`: tối đa khoảng 10 ảnh trước/sau tiêu biểu đã xem và nén; dữ liệu QA tổng hợp, không chứa PII/secret. Không xuất hàng trăm ảnh thay cho phân tích.

Không xóa/sửa bằng chứng lịch sử để che lần chạy đỏ. Cập nhật trạng thái hiện tại có SHA/lệnh mới; lịch sử đã qua giữ nguyên ý nghĩa.

Điều kiện kết thúc:

1. Mọi nhóm ở mục 5 có trạng thái audit; issue P0/P1 đã xác nhận được sửa và kiểm chứng hoặc ghi blocker cụ thể cùng tác động. Không giấu phần chưa làm.
2. Hành trình Guest → catalog → SKU → cart → login/merge → địa chỉ → quote → COD → xem đơn và thao tác quản trị liên quan có bằng chứng theo môi trường chạy được.
3. Giá/tồn/quyền/state đúng server; no-JS và recovery của vùng sửa hoạt động; không tạo lỗi accessibility/layout mới ở ma trận đã chạy.
4. Có ảnh trước/sau và giải thích UX cụ thể; hiệu năng có số đo hoặc ghi chưa đủ điều kiện đo. Không tự chấm điểm “đẹp”, % tương đồng hay production-ready từ số PASS.
5. Test/package/diff được kiểm; file thay đổi và giới hạn rõ, dữ liệu owner được bảo toàn; không commit/push/deploy.

Tin nhắn kết thúc phải nêu: vấn đề chính đã xử lý và lợi ích, nhóm file sửa, kết quả kiểm thử thực tế, cải thiện UI/UX quan sát được, các phần chưa xác minh và đường dẫn báo cáo/bằng chứng. Không chỉ trả “đã hoàn thành” kèm tổng số test.

---

## Nguồn đối chiếu khi soạn prompt

Các liên kết dưới khóa theo commit để không lẫn với cập nhật sau này:

- [Repository tại c79983a](https://github.com/tuanank5/cosmetics_shop/tree/c79983a58071d85cb9345c609576ef5b7d7cd61f).
- [Quy ước AGENTS](https://github.com/tuanank5/cosmetics_shop/blob/c79983a58071d85cb9345c609576ef5b7d7cd61f/AGENTS.md), [Business Rules](https://github.com/tuanank5/cosmetics_shop/blob/c79983a58071d85cb9345c609576ef5b7d7cd61f/docs/requirements/02-business-rules.md), [Decisions, gồm D35–D37](https://github.com/tuanank5/cosmetics_shop/blob/c79983a58071d85cb9345c609576ef5b7d7cd61f/docs/DECISIONS.md).
- [Footer](https://github.com/tuanank5/cosmetics_shop/blob/c79983a58071d85cb9345c609576ef5b7d7cd61f/src/main/resources/templates/fragments/footer.html), [Home](https://github.com/tuanank5/cosmetics_shop/blob/c79983a58071d85cb9345c609576ef5b7d7cd61f/src/main/resources/templates/home/index.html), [cart.js](https://github.com/tuanank5/cosmetics_shop/blob/c79983a58071d85cb9345c609576ef5b7d7cd61f/src/main/resources/static/js/cart.js).
- [Điều hướng nguồn](https://github.com/tuanank5/cosmetics_shop/blob/c79983a58071d85cb9345c609576ef5b7d7cd61f/src/main/java/com/beautyshop/controller/NguonDieuHuong.java), [xử lý đơn hàng](https://github.com/tuanank5/cosmetics_shop/blob/c79983a58071d85cb9345c609576ef5b7d7cd61f/src/main/java/com/beautyshop/service/DonHang_Service.java).
- [Hướng dẫn local/migration](https://github.com/tuanank5/cosmetics_shop/blob/c79983a58071d85cb9345c609576ef5b7d7cd61f/docs/deployment/01-local-db-and-flyway.md), [QA-01](https://github.com/tuanank5/cosmetics_shop/blob/c79983a58071d85cb9345c609576ef5b7d7cd61f/docs/reviews/QA-01-ket-qua.md), [workflow CI](https://github.com/tuanank5/cosmetics_shop/blob/c79983a58071d85cb9345c609576ef5b7d7cd61f/.github/workflows/ci.yml).
- [Maven CI của c79983a đã completed/success khi kiểm tra](https://github.com/tuanank5/cosmetics_shop/actions/runs/45818577904).
- [W3C — Target Size Minimum](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html): AA quy định tối thiểu 24×24 CSS px với các ngoại lệ; 44×44 ở đây là mục tiêu tiện dụng riêng cho các nút quan trọng.
- [web.dev — Web Vitals](https://web.dev/articles/vitals): phân biệt nguồn đo và các chỉ số LCP/INP/CLS; dữ liệu lab không tự thay thế trải nghiệm field.
