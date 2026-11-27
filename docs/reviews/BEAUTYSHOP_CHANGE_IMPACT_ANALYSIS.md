# Ảnh hưởng từ b03e0bf đến working tree

Đọc cùng [baseline và kiểm kê tệp](BEAUTYSHOP_CURRENT_STATE_REVIEW.md). Các thay đổi mới vẫn nằm trong Spring MVC → service → repository → MariaDB/MongoDB. Không thấy migration, Entity hay dependency mới. Phần thay đổi nhiều nhất là hợp đồng giữa URL, dữ liệu view và template.

| Nhóm | Thay đổi, bằng chứng | Kế thừa và hồi quy cần kiểm |
|---|---|---|
| Danh mục, thương hiệu, sản phẩm | Tab và đích riêng, chung controller và service. AdminDanhMucSanPhamThuongHieu_Controller:44–53 phân trang backend và tạo link theo bộ lọc. Danh mục và thương hiệu đã tách trong điều hướng. | Giữ handler chung và BR21. Trang sản phẩm vẫn tải toàn bộ lựa chọn danh mục/thương hiệu và đếm đơn đang xử lý. Đo SQL với nhiều dữ liệu trước khi quyết định tìm kiếm từ xa hay giới hạn truy vấn đếm. |
| PDP, CSRF | CuaHang_Controller:90–111 lấy deferred token trước khi xuất HTML chứa form, có phân trang đánh giá. Thành phần đóng/mở bằng bàn phím theo browser-addresses.json. | Giữ sửa lỗi khách mới. Kiểm session mới, thêm giỏ hợp lệ, POST thiếu token bị từ chối, hết hàng và đổi biến thể. |
| Đơn và giỏ | Dòng đơn và dòng giỏ nhận focus, cartVariant giữ biến thể ban đầu. Chi tiết đơn có nhiều nguồn vào từ checkout, đánh giá, dashboard. | Dùng SCROLL-RESTORE kiểm trang 2 và dòng mất. Back phục hồi giỏ cache có thể mở cảnh báo cần tải lại. Bảo toàn checkout, snapshot giá/địa chỉ và gộp Guest; tái chạy COD, MoMo, hủy, hoàn tồn khi thay kho. |
| Địa chỉ | D26/D42. DiaChiNguoiDung_Service:46–70 khóa tài khoản trước khi đếm, chỉ chặn tạo ở 10, địa chỉ đầu mặc định, chọn mặc định mới xóa cờ cũ, xóa mặc định chuyển sang ID nhỏ nhất còn lại. NguoiDung_Repository:19 khóa ghi. addresses.html ẩn nút thêm, Backend kiểm cả POST và sở hữu. | Đã duyệt và triển khai. XacThuc_MariaDb_Test:440,468 có ca đồng thời và giới hạn. Khóa và kiểm đếm cùng transaction bảo vệ tình huống 9→10; nên có ca đúng biên hai POST tại 9. Sửa/xóa không sửa snapshot giao hàng trên đơn. Kiểm thêm một đơn đã đặt trước khi sửa địa chỉ. |
| Tài khoản và hậu mãi | Tài khoản có link đơn lên đầu, breadcrumb security/returns, địa chỉ xếp ngang. Wishlist/đổi trả có pager; review liên kết mua hàng và focus lại cả thẻ. Admin users lọc role/trạng thái. | Dùng lại fragment ui, nav-fields, admin-table. Tách nguồn checkout khi lưu hoặc quay lại địa chỉ. Đối chứng truy cập thẳng, mở từ đơn, lọc làm mất dòng, hai tab và lỗi validation. |
| AI và quyền | D41 gỡ form tư vấn cũ, controller/service/DTO và test riêng, chuyển view /chat. Security_Config có diff theo route. | Đây là thay đổi có chủ đích. Kiểm liên kết tài liệu cũ, GET/POST route cũ theo vai trò, PII/giới hạn gửi và bộ đếm provider trong /chat; test lịch sử luồng đã bỏ không còn là gate phát hành. |
| Auth/OAuth/OTP | Thay đổi CSS, lỗi form, logo và đường quay lại. Không thấy diff trong provider OAuth/Redis/mail hay migration danh tính. | Giữ bằng chứng mock về state, ownership, CSRF. Kiểm qua callback và mailbox thật. |
| UI chung | BUTTON-WIDTH, FIELD-SPACING, ADDRESS-FOCUS, WORDMARK-FIX chứng minh sửa nút, khoảng trống lỗi, viền thẻ, SVG cắt nét. Logo đã đo ở 320/390/1366. | Không thiết kế lại các màn đã đạt. Kiểm zoom native, keyboard, lỗi server và dữ liệu dài trên cùng revision. |
| Hạ tầng | Scripts, Compose và migration V1–V7 không có diff. | Kế thừa phục hồi SQL/Mongo/ảnh và checksum QA-01; diễn tập lại khi có migration kho. V5 legacy cần preflight theo README:18. |

## Danh sách và bảng dữ liệu

| Phạm vi | Hiện trạng và giới hạn kiểm chứng |
|---|---|
| Catalog công khai, PDP | /products mặc định 12, PageRequest và lọc/sort trong CuaHang_Controller:33–47. Đánh giá PDP có pager riêng giữ variant và nguồn. Danh sách biến thể để chọn mua vẫn tải theo sản phẩm (CuaHang_Service:56–65). Kiểm với SKU nhiều biến thể trước khi thay UX. |
| Đơn khách, đổi trả khách, wishlist | 10, 10, 12 mục/trang. Backend dùng PhanTrang; chi tiết mở từ danh sách mang số trang. Khi nguồn là giao dịch mới hoặc URL trực tiếp thì về danh sách mặc định. |
| Catalog Admin, biến thể, tài khoản, voucher, đổi trả | 15 mục/trang. PhanTrang.doc xử lý trang vượt cuối ở các caller áp dụng. Users giữ q/role/active, sản phẩm giữ category/brand/active/q, voucher giữ q/active. Kiểm trang cuối sau thao tác làm giảm tập kết quả. |
| Đơn Admin, đánh giá Admin | Đơn mặc định 20, có 10/50, giữ nhiều trạng thái, ngày và sort. Đánh giá 20 giữ sản phẩm, sao, trạng thái. Đã có test chọn lọc, chưa chứng minh mọi tích tổ hợp lọc. |
| Danh sách có phạm vi riêng | Địa chỉ tối đa 10, giữ một danh sách. Giỏ, mục đơn, dòng hoàn trả thuộc một giao dịch; cần xem đủ để đối chiếu tổng. Dashboard là số tổng hợp, trang /admin/ai là trạng thái và thao tác reindex (AdminTuVan_Controller). Trang chủ có giới hạn teaser. Ưu tiên đo giỏ lớn, bộ chọn và lịch sử theo đơn trước khi thêm pager. |
| Khung bảng và toolbar | order/list, return/list:17–30, admin/user/list dùng tiêu đề cột, scope và vùng cuộn ngang. Bộ đếm đúng đơn vị, pager phía dưới. Voucher/review dùng thẻ. ui::pagination, admin-table, CSS bảng/btn dùng chung; bộ lọc đơn chuyên biệt và nhãn theo nghiệp vụ. |
| Trạng thái cần nghiệm thu bổ sung | Có thông báo theo điều kiện, danh sách rỗng và kho review không phản hồi; checkout/cart có pending và recovery. Trang GET render server chủ yếu chuyển tài liệu. Không yêu cầu spinner mọi bảng. Kiểm 0/1/nhiều trang, dữ liệu dài, keyboard cuộn ngang, thông báo sau POST, zoom thật, focus sau lọc. |

Breadcrumb có chủ đích khác theo ngữ cảnh. Danh sách catalog Admin dùng tab; form thêm/sửa dùng breadcrumb ngắn. Khu tài khoản thêm cấp Tài khoản; chi tiết review có đường về đơn. NguonDieuHuong gom whitelist query và mốc dòng, app.js xử lý focus. Một số link và tên tham số còn trong template, cần test xuyên trang khi đổi hợp đồng. Nguồn giỏ và wishlist có logic phục hồi riêng do trạng thái giỏ có thể cũ. Không có bằng chứng cần hợp nhất toàn bộ JavaScript.

## Điều kiện chuyển sang FEFO

Kế thừa [thiết kế lô](ADMIN-UX-BIZ-01-tiep-tuc.md) và D40. TonKho_Service hiện khóa/cập nhật theo maBienThe trong reserveAtomically, consumeReservation, releaseReservation, deductCOD, hoanCOD. Không thể lấy lô minh họa trên PDP để suy ra lô đã giao.

1. Chốt ngày hết hạn theo giờ Việt Nam, lượng bán được, ngưỡng cảnh báo, quy tắc kiểm định hàng trả và pin trách nhiệm phê duyệt điều chỉnh kho.
2. Kiểm kê từng SKU, tách vật lý/giữ/bán được/hết hạn/cách ly. Đối chiếu tồn tổng, reservation, đơn COD còn hủy được và voucher trước bật theo lô. Đơn cũ giữ lịch sử snapshot.
3. Khóa giỏ hoặc đơn theo hợp đồng hiện tại, rồi SKU theo ID và lô theo HSD/ngày nhập/ID. Một dòng được chia lô, thiếu một phần phải rollback toàn transaction. Ghi phân bổ và biến động có định danh chống lặp.
4. COD xuất và hoàn đúng lô. MoMo giữ đúng lô, xử lý IPN lặp, hết giữ, thanh toán muộn khi lô hết hạn và đối soát/hoàn tiền. Đổi trả chỉ nhập kho sau kiểm định. Duy trì BR21 khi có đơn đang xử lý.
5. Catalog, PDP, checkout, AI và báo cáo dùng cùng định nghĩa tồn bán được. Diễn tập Flyway từ trống và legacy, checksum, sao lưu/khôi phục, chênh lệch, rollback và dừng ghi. Owner duyệt gate trước chuyển từng SKU.

**Kết luận rủi ro.** Chưa xác nhận regression nghiệp vụ nghiêm trọng mới từ các ca đã chạy. Working tree chưa có CI theo revision; báo cáo cũ, ảnh cũ, test đã xóa và chuỗi tích hợp thật có phạm vi khác nhau. Riêng nguy cơ chi phí truy vấn theo kích thước catalog, điều hướng khi dòng biến mất, cờ quyền và snapshot đơn cần giữ trong hồi quy. Phần thiết kế lô có thể làm song song với kiểm dịch vụ, triển khai ghi kho cần qua các gate giao dịch và dữ liệu.
