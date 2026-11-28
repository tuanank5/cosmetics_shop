# 10-RETURN · Hồ sơ trình owner ngày 28/11/2026

**Hồ sơ đề xuất trước khi duyệt.** Ngày 28/11/2026 owner duyệt RETURN-1–5 và giao triển khai trên fixture. Các nhãn PROPOSED bên dưới ghi lại phương án đã trình. [Quyết định chính thức](../DECISIONS.md#return--quyết-định-owner-ngày-28112026) Baseline đối chiếu là `e100a1e1edd32a26b36877572aa2eeac06801c41`, trùng HEAD và tracking ref origin/main. Owner cần quyết định tiêu chuẩn kiểm định và xử lý ngoại lệ ở mục 12 của ma trận tác động. Năm mục dưới đây là các phần của quyết định đó. Quyết định 13–14 về trả một phần và chi tiền vẫn thuộc REFUND. BASE và AUDIT đã đạt. [Prompt RETURN](implementation-prompts/next-upgrade/10-RETURN.md) · [Kế hoạch](BEAUTYSHOP_NEXT_UPGRADE_PLAN.md) · [Ma trận tác động](BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) · [Tiến độ](BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md)

## Hiện trạng có thể đối chiếu trên baseline

| Điểm vào | Hành vi đang có | Khoảng trống ảnh hưởng quyết định |
|---|---|---|
| Customer tạo yêu cầu | `POST /orders/{id}/returns/new` lấy chủ từ principal. Service khóa đơn bằng `FOR UPDATE` trong transaction READ_COMMITTED. SQL yêu cầu DELIVERED, có giờ giao và giờ DB UTC nằm trong khoảng từ lúc giao đến đúng lúc giao + 7 ngày, gồm hai đầu. Chỉ nhận dòng của đơn, số nguyên dương cho ít nhất một dòng. | Đây là hạn **gửi yêu cầu**. Baseline không đặt hạn vận chuyển kiện về. [Controller](../../src/main/java/com/beautyshop/controller/SauMua_Controller.java) · [Service](../../src/main/java/com/beautyshop/service/SauMua_Service.java) |
| Lượng và quyền sở hữu | Lượng còn được yêu cầu bằng lượng mua trừ tổng lượng của mọi hồ sơ cùng dòng, trừ hồ sơ REJECTED (D29). Customer đọc danh sách/chi tiết theo ID người dùng trong phiên, truy cập chéo trả 404. | Lượng trên dòng là **lượng yêu cầu**. Duyệt đang ở cấp hồ sơ, chưa có cột duyệt một phần hay đếm kiện thực nhận. [Repository](../../src/main/java/com/beautyshop/repository/DoiTra_Repository.java) · [Quyết định D12/D29/D45](../DECISIONS.md) |
| Trạng thái và UI | PENDING → APPROVED hoặc REJECTED. APPROVED → RECEIVED → COMPLETED. Service yêu cầu ROLE_ADMIN, kiểm bước hợp lệ và UPDATE với điều kiện trạng thái cũ. Trang chi tiết chung liệt kê tên snapshot, lượng yêu cầu và nút POST theo bước tiếp. Admin dùng PRG, danh sách 15/trang, Customer 10/trang. | Nút nhận/hoàn tất chỉ cập nhật trạng thái. Chưa có bằng chứng nhận vật lý, checklist, actor kiểm định, xác nhận theo dòng hay biên nhận cho retry. [Enum](../../src/main/java/com/beautyshop/order/ReturnStatus.java) · [Admin controller](../../src/main/java/com/beautyshop/controller/admin/AdminSauMua_Controller.java) · [Template](../../src/main/resources/templates/return/detail.html) |
| Dữ liệu và kho | V1 có PK từng dòng trả, FK về yêu cầu và dòng đơn, unique (yêu cầu, dòng đơn), CHECK lượng > 0. Đơn/dòng đơn/trả hàng được đọc qua SQL và projection. SKU có entity JPA, lượng trong kho và tạm giữ. Kho hiện khóa đơn rồi SKU, có chứng từ trừ COD V2 và giữ tồn MoMo V1. | Chưa có chứng từ nhập do kiểm định. Hoàn kho khi hủy COD dựa trên lần xuất của **đơn COD**; một đơn đã giao có thể trả nhiều đợt, kể cả MoMo. [V1](../../src/main/resources/db/migration/V1__khoi_tao_co_so_du_lieu.sql) · [SKU](../../src/main/java/com/beautyshop/model/BienTheSanPham.java) · [Kho](../../src/main/java/com/beautyshop/service/TonKho_Service.java) |
| Audit và BR21 | V7 unique (correlation, action), V8 có snapshot trước/sau tối đa 80 ký tự. Audit service có hook sau rollback và transaction riêng cho FAILED. BR21 xét hồ sơ PENDING/APPROVED/RECEIVED theo dòng, cùng các điều kiện payment, đơn và lượt giữ. | Enum audit chưa có action RETURN; serializer chưa nhận ReturnStatus hoặc kết quả kiểm định. COMPLETED gỡ nhánh chặn do hồ sơ trả của BR21. Nếu đóng sớm có thể mất chặn dù thiếu hàng hoặc nghĩa vụ chưa được biểu diễn ở payment. [Audit service](../../src/main/java/com/beautyshop/service/SuKienQuanTri_Service.java) · [Query BR21](../../src/main/java/com/beautyshop/repository/DonHangCatalog_Repository.java) · [Hồ sơ AUDIT](AUDIT-ket-qua.md) |

## RETURN-1 · PROPOSED · Tiêu chuẩn kiểm theo dòng và SKU

**Đề nghị chỉ đưa vào tồn bán được khi xác minh đủ các điều kiện.** Mỹ phẩm có thể nhiễm bẩn do tiếp xúc với tay/dụng cụ hoặc điều kiện bảo quản. Mùi, màu và hình thức bình thường không đủ chứng minh sản phẩm đã mở còn an toàn. Đây là căn cứ cho lựa chọn vận hành của shop. [FDA, Shelf Life and Expiration Dating of Cosmetics](https://www.fda.gov/cosmetics/cosmetics-labeling/shelf-life-and-expiration-dating-cosmetics)

| Kết luận đề xuất | Căn cứ ghi trên phiếu | Xử lý |
|---|---|---|
| Đủ bán | Khớp SKU/biến thể và quy cách, dấu bảo vệ nguyên bản đúng loại, bao bì trực tiếp nguyên vẹn, nhãn và thông tin truy xuất/hạn dùng kiểm được theo sản phẩm, còn hạn, không có dấu hiệu sử dụng, hư hỏng, thu hồi hoặc bảo quản sai. SKU còn được phép bán tại lúc ghi kho. | Chỉ số lượng được Admin xác nhận đạt mới được lập bút toán. |
| Đã mở/mất niêm phong | Mở nắp hoặc seal bảo vệ, đã dùng thử, dụng cụ đã tiếp xúc sản phẩm. Phân biệt hộp vận chuyển rách với bao bì bảo vệ sản phẩm. | Ghi số không đủ bán và mã lý do. Tách vật lý khỏi hàng bán được. |
| Hỏng | Vỡ, rò, biến dạng ảnh hưởng bảo vệ hoặc sản phẩm biến chất. | Ghi lượng và tình trạng quan sát được. Không cộng tồn bán. |
| Khác/chưa xác minh | Hết hạn, nhãn không đọc được, SKU ngừng kinh doanh hoặc thiếu căn cứ về tính nguyên vẹn. Với loại không có seal từ nhà sản xuất, cần checklist riêng về cơ chế bảo vệ tương đương. | Chờ làm rõ ở khu cách ly. Nếu không xác minh được, xác nhận không đủ bán kèm lý do. Không tự tái kích hoạt SKU. |

Mỗi dòng liên kết **ID dòng trả → dòng đơn → SKU**. Ghi lượng duyệt, lượng nhận theo đợt, lượng đủ bán, lượng không đủ bán theo mã lý do, người nhận, người kiểm, người xác nhận, thời điểm UTC từng bước và mã chứng từ. Đề nghị duyệt nguyên lượng từng dòng khi duyệt cả hồ sơ như hiện hành. Không cho sửa lượng đã duyệt trong bước nhận. Với hàng nhầm hoặc thừa, ghi biên bản sai lệch ngoài lượng nhận hợp lệ của dòng. Ảnh không bắt buộc cho ca chuẩn. Với tranh chấp/hỏng/sai kiện, đề nghị lưu tham chiếu bằng chứng giới hạn quyền, tránh ảnh nhãn vận chuyển có thông tin khách; chờ owner duyệt trước khi thêm lưu trữ ảnh.

**Bất biến cộng dồn cho từng dòng:** `0 ≤ đủ bán ≤ thực nhận khớp dòng ≤ đã duyệt`. Khi kiểm dở, `thực nhận = đủ bán + không đủ bán + chờ kiểm`. Khi chốt đợt, chờ kiểm bằng 0 cho lượng được chốt. `Chưa nhận = đã duyệt − thực nhận`; thiếu chưa nhận và hỏng đã nhận là hai số khác nhau. Tổng đã nhập bằng tổng đủ bán của các đợt **đã xác nhận**. Lựa chọn chặt hơn là loại toàn bộ mỹ phẩm trả khỏi tồn bán; dễ vận hành nhưng mất hàng còn nguyên và đủ chứng cứ.

## RETURN-2 · PROPOSED · Nhận nhiều đợt và sai lệch

**Đề nghị giữ năm trạng thái hiện có.** Ghi một phiếu cho mỗi lần giao nhận vật lý. Chuyển APPROVED sang RECEIVED khi lần đầu có ít nhất một đơn vị khớp dòng được nhận. Hiển thị “Đã nhận một phần” và số nhận/duyệt trên từng dòng khi còn thiếu. Đây là nhãn suy ra từ chứng từ. Các đợt tiếp theo giữ RECEIVED. Lựa chọn khác là giữ APPROVED đến khi đủ tất cả dòng, nhưng màn hình sẽ che khuất hàng đã nhận và chờ kiểm nếu không bổ sung cùng loại chi tiết.

| Sự việc thực tế | Cách ghi và trạng thái đề xuất |
|---|---|
| Nhận đủ | Ghi phiếu nhận, chuyển RECEIVED. Tồn chỉ thay đổi sau xác nhận kiểm định. |
| Nhận thiếu hoặc chỉ có vài SKU | Ghi đúng từng lượng dương, dòng vắng vẫn bằng 0. Hồ sơ còn mở, đợt sau chỉ nhận trong phần dư. |
| Gửi nhầm SKU/kiện, gửi quá lượng | Ghi sai lệch và quản lý hàng ngoài lượng được duyệt. Không mượn dòng khác để khớp. Nếu toàn kiện sai, giữ APPROVED khi trước đó chưa nhận đúng hàng. |
| Không nhận, thất lạc, khách gửi trễ | Ghi lý do chờ và chứng từ liên hệ/giao vận nếu có. Không dùng mốc 7 ngày mở yêu cầu để hủy một hồ sơ đã duyệt. Đề nghị không tự hết hạn. Kiện muộn còn hồ sơ mở được tra soát rồi nhận thực tế. |
| Tranh chấp mất hàng, cả hồ sơ nhận 0 | Giữ APPROVED và ghi chờ đối soát. Nếu owner muốn đóng vĩnh viễn trường hợp này cần duyệt nhánh kết thúc riêng, điều kiện chứng minh và ảnh hưởng D29/BR21. REJECTED hiện chỉ đi từ PENDING và trả hạn mức, dùng lại sẽ đổi nghiệp vụ. |

**Ví dụ để duyệt.** Duyệt A×3, B×2. Đợt 1 nhận A×2, trong đó một nguyên seal đủ bán và một đã mở. Sau xác nhận, tồn A tăng 1, B tăng 0. Đợt 2 nhận A×1 đủ bán và B×1 hỏng. Sau hai đợt A nhận 3/3, đủ bán 2, không đủ 1; B nhận 1/2, đủ bán 0, hỏng 1, thiếu 1. Tồn cộng dồn A +2, B +0. Vẫn còn nghĩa vụ làm rõ B×1. Gửi lại xác nhận đợt 2 giữ nguyên các số này.

## RETURN-3 · PROPOSED · Quyền và audit

| Ai | Quyền đề xuất |
|---|---|
| Customer | Gửi yêu cầu và xem tiến độ, kết luận dành cho khách trong hồ sơ của mình. ID người dùng từ principal. Giữ 404 cho tài nguyên của khách khác. Không sửa số nhận, kết luận, người xử lý hay chứng từ nội bộ. |
| Admin tiếp nhận | Duyệt/từ chối theo PENDING, ghi nhận từng kiện, lượng khớp dòng, sai lệch và thời điểm thực nhận. |
| Admin kiểm định | Ghi checklist và lượng theo từng kết luận cho đúng dòng của đợt đã nhận. Bản chờ xác nhận chưa tác động kho. |
| Admin xác nhận | Đọc lại version, checklist và lượng còn được xác nhận, ký xác nhận để ghi kho, xử lý ngoại lệ có căn cứ và đóng khi đủ điều kiện. |

Ba công đoạn Admin dùng ROLE_ADMIN hiện hành và lưu riêng actor theo công đoạn. Đề nghị cho cùng một Admin thực hiện ca thường. Phương án bắt buộc hai Admin độc lập tăng kiểm soát sai sót nhưng có thể treo hồ sơ ca trực một người. Nếu chọn cho tranh chấp/ghi điều chỉnh, owner cần xác định loại ngoại lệ, người thứ hai và điều kiện bàn giao; chưa trao quyền ghi đè checklist an toàn. Mọi POST cần CSRF, PRG và kiểm role, trạng thái phiên/quyền Admin trong transaction ghi. Server xác định SKU, actor và thời gian.

Đề nghị action cho duyệt/từ chối, ghi nhận đợt, xác nhận kiểm định kèm nhập kho, giải quyết thiếu và đóng hồ sơ. Mỗi hành động làm đổi dữ liệu có correlation, actor/role, target bền vững, UTC, trước/sau, kết luận, mã lý do và kết quả. Chứng từ chi tiết giữ lượng trước/sau từng dòng và liên kết phiếu nhập. Audit giữ ID chứng từ, số tổng/thay đổi trạng thái theo allowlist vừa cột V8; không serialize danh sách dòng, email, địa chỉ, ảnh hay lý do tự do của khách. SUCCESS cùng transaction nghiệp vụ. Khi rollback sau khi bắt đầu hành động, FAILED ở transaction riêng tham chiếu hồ sơ gốc còn tồn tại. POST bị từ chối ở cổng quyền/CSRF, no-op và replay không sinh SUCCESS mới. UUID đã có FAILED dùng kết quả cũ, lần thực hiện mới dùng correlation mới cho cùng chứng từ nghiệp vụ. [Audit D7](AUDIT-ket-qua.md) · [V8](../../src/main/resources/db/migration/V8__nhat_ky_nghiep_vu.sql)

## RETURN-4 · PROPOSED · Nhập phần đủ bán đúng một lần

1. Server cấp mã cho đợt nhận và cho lần xác nhận. Ràng buộc vào hồ sơ, actor, loại thao tác, phiên bản và nội dung. Trước khi ghi, kiểm phiên Admin, khóa hàng hồ sơ trả, kiểm dòng thuộc hồ sơ và đúng đợt, đọc lại tổng lượng dưới khóa. Nếu cần khóa đơn gốc, lấy khóa đơn **trước** hồ sơ. Giữ mọi khóa SKU theo ID tăng dần và đọc lại trạng thái bán tại lúc ghi. Đối chiếu thứ tự khóa thực của đường tắt catalog/checkout trước khi tích hợp.
2. Tìm biên nhận dưới khóa. Replay đúng lệnh đã commit trả kết quả cũ kể cả mất response. Cùng mã khác nội dung bị từ chối. Hai mã khác nhau cùng xác nhận một dòng nhận vật lý cũng chỉ được tiêu thụ dòng đó một lần. Chỉ ghi đợt mới khi có căn cứ là lần giao thực khác; ràng buộc mã phiếu nhận để chống tạo lại cùng kiện khi retry.
3. Kiểm bất biến từng dòng và tràn số trước khi cộng. SKU đã tắt hoặc bị chặn bán thì giữ hàng cách ly, yêu cầu xử lý lại, chưa nhập. Trong **một transaction cho toàn phiếu xác nhận**, đóng băng kết quả, ghi bút toán dương cho từng dòng đủ bán, tăng `so_luong_trong_kho` đúng số đó, giữ `so_luong_tam_giu`, ghi audit SUCCESS và commit. Lượng đủ bán bằng 0 vẫn lưu kết luận có căn cứ, không tạo bút toán số 0. Sai bất kỳ dòng nào rollback toàn phiếu xác nhận; các đợt đã commit trước vẫn nguyên vẹn.
4. Dùng unique nghiệp vụ trên **ID dòng nhận đã kiểm** cho kết quả xác nhận cuối và cho bút toán nhập; unique mã thao tác riêng cho replay. Dòng nhận đã cố định thuộc đúng một đợt và một dòng trả. Mỗi đợt mới có dòng nhận khác, kể cả cùng SKU. Chỉ unique correlation sẽ lọt hai lần bấm bằng hai mã; chỉ unique dòng trả sẽ chặn nhầm đợt thứ hai. Lượng cộng dồn qua nhiều hàng phải kiểm dưới khóa hồ sơ trong mọi đường ghi. CHECK từng hàng không bảo vệ được phép cộng liên đợt.

Phương án nhập một lần khi đóng cả hồ sơ giảm số bút toán nhưng trì hoãn hàng đã đạt và ghép quyền nhập kho với ngoại lệ thiếu/tiền. Đề nghị ghi theo đợt đã kiểm. Sau timeout/crash chỉ đọc biên nhận theo khóa cũ để quyết định retry. Nếu phát hiện sai sau commit, bảo toàn phiếu gốc, ngừng nhập thêm phần tranh chấp và chuyển xử lý có thẩm quyền; quyết định hiện tại chưa cấp quyền sửa lịch sử hoặc tự phát sinh bút toán đối ứng.

## RETURN-5 · PROPOSED · Điều kiện đóng và hồ sơ legacy

| Trước khi đóng | Quyết định đề xuất |
|---|---|
| Đã nhận đủ, gồm cả phần hỏng/đã mở | Cho đóng từ RECEIVED khi mọi đơn vị đã phân loại, có chứng từ cho toàn bộ phần đủ bán, phần loại có lý do, tranh chấp đã giải quyết và nghĩa vụ liên quan đã rõ. Hồ sơ toàn hàng hỏng có thể kết thúc với nhập kho 0. |
| Đã nhận một phần và phần thiếu đã giải quyết | Admin ghi lượng thiếu cuối, mã lý do, tham chiếu đối soát và người quyết định. Chỉ đóng khi tổng thực nhận + lượng thiếu đã chốt = lượng duyệt trên **từng dòng** và các điều kiện trên đạt. D29 tiếp tục tính lượng yêu cầu của hồ sơ COMPLETED; không tự trả hạn mức cho phần thiếu. |
| Còn chờ kiện, kiểm định, sai lệch, tranh chấp hoặc nghĩa vụ tiền chưa rõ | Giữ hồ sơ mở dù các đợt đủ bán đã nhập. Chờ kết quả nghiệp vụ có thể đối chiếu cho nghĩa vụ tiền. Không dùng nút hoàn tất để đánh dấu đã hoàn tiền. Nếu chưa có nguồn xác minh, giữ chờ và BR21. |
| Hồ sơ cũ | Đọc trạng thái, ngày, lý do và lượng yêu cầu như đã lưu. Thiếu dữ liệu nhận/kiểm thì hiển thị “Chưa ghi nhận”. COMPLETED/REJECTED cũ chỉ đọc, không backfill chứng từ hay nhập kho. APPROVED/RECEIVED cũ chưa có chứng cứ cần đối chiếu thủ công lượng đã xử lý và số đã nhập; khi chưa xác định được thì khóa xác nhận nhập. |

Đề nghị cho RETURN ghi nhận kết quả vật lý nhưng không ước tính tiền hoàn, không tạo refund, không cập nhật payment attempt, snapshot đơn hoặc trả quota HÀNG/SHIP. Một hồ sơ có hàng đủ bán chưa chứng minh đủ điều kiện hoàn toàn bộ giao dịch. Nếu owner muốn đóng phần vật lý ngay khi còn nợ tiền, cần nguồn nghĩa vụ độc lập, có quyền đọc và được BR21 kiểm tới khi giải quyết. Chưa chọn phương án đó trong đề xuất. [PAYMENT](PAYMENT-ket-qua.md) · [VOUCHER](VOUCHER-ket-qua.md) · [ORDER](ORDER-ket-qua.md)

## Ma trận tác động nếu owner duyệt

| Phần | Thay đổi dự kiến | Ràng buộc khi tích hợp |
|---|---|---|
| Dữ liệu | Migration additive kế tiếp sau V10 (dự kiến V11 nếu số còn trống). Phiếu nhận có FK hồ sơ, mã nhận unique, actor/UTC. Dòng nhận có FK dòng trả, unique (phiếu nhận, dòng trả). Kết quả kiểm lưu phân loại, lượng theo lý do, người/giờ kiểm và xác nhận. Phiếu hoặc dòng nhập có FK nguồn kiểm, SKU, delta dương, khóa unique RETURN-4. Chứng từ giải quyết phần thiếu liên kết dòng và actor. | FK ghép hoặc kiểm dưới khóa ngăn trỏ dòng của hồ sơ khác. CHECK lượng không âm, lượng phân loại không vượt nhận. Index theo hồ sơ/dòng và SKU để đối chiếu. V1–V10 giữ nguyên checksum. Không gán mặc định “đã kiểm” cho dữ liệu cũ. |
| Service/repository/kho | Mở rộng `DoiTra_Repository`, `SauMua_Service` và entry point kho dành cho nhập RETURN. Chặn cả đường đổi trạng thái cũ khi thiếu phiếu. Khóa SKU từ dòng đơn và dùng một transaction MariaDB cho phiếu, kho, audit. | Giữ CAS trạng thái, kiểm quyền, D29 và chứng từ COD/MoMo riêng. Không mở LOT/FEFO. |
| UI và đọc đơn | Bổ sung form nhận, kiểm và xác nhận trên màn quản trị hiện có. Detail Customer hiển thị lượng theo dòng, đợt đã nhận, phần hỏng/thiếu và việc đang chờ. | Giữ SSR/PRG, pager, Back/focus, thông báo lỗi và liên kết từ chi tiết đơn. Không lộ chứng từ nội bộ hoặc actor riêng tư cho khách. |
| Audit, BR21 và sau mua | Mở allowlist action/snapshot cụ thể, nối ID chứng từ. Kiểm các query tắt SKU/sản phẩm/hãng/danh mục và các đường xem thống kê dùng trạng thái cũ. | Giữ nghĩa vụ hàng và tiền đang mở. Không mở REFUND, đổi quy tắc KPI hoặc ký thay xác nhận đã chi. |

## Ma trận kiểm thử sau khi duyệt

Tất cả ca dưới đây **NOT RUN trong lượt phân tích**. Test baseline `doiTraNhieuMucCoHanVaTrangThaiThuCongKhongDoiTonHayThanhToan` hiện mô tả đúng hành vi chuyển trạng thái cũ; khi triển khai phải tách assertion “nhận chưa đổi tồn” và “xác nhận chỉ cộng phần đạt”. Test hai yêu cầu Customer đồng thời kiểm hạn mức mua, chưa chứng minh hai Admin xác nhận chỉ nhập một lần. [SauMua_MariaDb_Test](../../src/test/java/com/beautyshop/integration/SauMua_MariaDb_Test.java) · [CatalogDonHang_MariaDb_Test](../../src/test/java/com/beautyshop/integration/CatalogDonHang_MariaDb_Test.java)

| Nhóm | Fixture / phép kiểm | Điều kiện đạt |
|---|---|---|
| Unit | Bốn kết luận, thiếu seal, sai SKU, số âm, quá duyệt, nhận 0, overflow, nhiều đợt, đóng khi chờ tiền | Đúng bất biến, thứ tự bước và lý do chặn từng dòng. |
| MariaDB · nhận/kiểm | Chạy ví dụ A/B ở trên, thêm kiện nhầm, muộn, hồ sơ không nhận gì, SKU bị tắt ngay trước xác nhận | Đối chiếu SQL trước/sau từng đợt, không RECEIVED giả, không nhập cho lượng hỏng/thiếu, không đóng sớm. |
| MariaDB · idempotency | Hai Admin cùng/khác correlation cho cùng dòng nhận, hai phiếu có SKU trùng theo thứ tự ngược, replay sau mất response | Một kết quả và tối đa một bút toán mỗi dòng nhận; giao dịch nhiều SKU hoặc commit đủ hoặc rollback. Đợt độc lập hợp lệ tiếp tục được ghi. |
| MariaDB · rollback/AUDIT | Ép lỗi trước INSERT bút toán, sau INSERT/tăng tồn và sau SUCCESS trước commit, thử lại; gửi form cũ/đổi nội dung | Đợt lỗi không để lại tăng kho, bút toán hoặc SUCCESS. FAILED sau rollback gắn hồ sơ/correlation; retry hợp lệ khớp số dòng SQL, replay đã commit không thêm log. |
| Flyway | Schema rỗng từ V1 và schema V10 có hồ sơ đủ năm trạng thái, nâng cấp rồi migrate lần hai, JPA validate | Checksum V1–V10 nguyên vẹn, FK/CHECK/unique có hiệu lực, snapshot và tồn đầu kỳ nguyên vẹn; dữ liệu cũ thiếu bằng chứng hiển thị đúng. |
| Quyền, thời hạn, D29 | Guest, chủ, khách khác, Admin và Admin bị khóa; thiếu CSRF; lúc giao, đúng +7 ngày UTC, muộn một đơn vị thời gian DB; hai yêu cầu cùng lượng cuối | Chỉ chủ được tạo/xem; chỉ Admin hợp lệ được nhận/kiểm/đóng. REJECTED trả hạn mức, các trạng thái còn lại giữ lượng đã yêu cầu. |
| WebKit desktop/mobile | Customer gửi và xem nhiều dòng. Admin nhận hai đợt, phân loại hỏng/thiếu, double-click, Back/refresh với JS và no-JS | Đúng quyền, hiện rõ nhận/duyệt/đủ bán, còn thiếu và lý do. Reload không ghi lại. |
| Snapshot/tiền/BR21 | Đơn COD đã thu và MoMo mock đã thanh toán có hai voucher, lịch sử attempt, hàng giữ trên SKU, ca có REFUND_PENDING; thử tắt bốn cấp catalog khi còn nghĩa vụ | So SQL đơn/dòng/địa chỉ/giá/tiền, hai snapshot, lượt HELD/CONSUMED/RELEASED, attempt, reservation và KPI trước/sau. Chỉ delta tồn vật lý được xác nhận thay đổi. BR21 vẫn chặn khi còn bất kỳ nghĩa vụ liên quan. |

## Phiếu quyết định và thứ tự triển khai

| Mục owner cần duyệt | Lựa chọn đề nghị |
|---|---|
| RETURN-1 · PROPOSED | Duyệt checklist và bốn kết luận, cách ly hàng mở/không xác minh được; tham chiếu bằng chứng cho ngoại lệ. |
| RETURN-2 · PROPOSED | Cho nhận nhiều đợt, RECEIVED từ đơn vị khớp đầu tiên, hiển thị tiến độ từng dòng. Hồ sơ nhận 0 hoặc hàng trễ chờ xử lý có chứng cứ. |
| RETURN-3 · PROPOSED | Dùng ROLE_ADMIN, tách bước và ghi actor từng bước; cho phép cùng người ở ca thường; duyệt phạm vi audit và quyền xem. |
| RETURN-4 · PROPOSED | Nhập kho theo đợt kiểm đã xác nhận, unique theo dòng nhận vật lý, replay trả biên nhận, rollback toàn phiếu. |
| RETURN-5 · PROPOSED | Đóng khi mọi dòng và nghĩa vụ đã giải quyết, phần thiếu phải có quyết định cuối, giữ D29 và BR21; hồ sơ cũ thiếu chứng cứ không được nhập bù. |

Sau khi owner chốt từng mục, ghi quyết định và ví dụ A/B vào sổ, sau đó mới chuyển RETURN sang IN_PROGRESS. Triển khai theo thứ tự migration/đọc legacy → transaction và audit → form Admin/chi tiết Customer → fixture JDK 21, MariaDB, Flyway, WebKit, Maven test/package và diff check. Chỉ trình nghiệm thu sau review, CI và đối chiếu đúng revision. Nếu cần dừng rollout, khóa thao tác nhập mới, giữ chứng từ đã commit và đối chiếu chênh lệch theo SKU.
