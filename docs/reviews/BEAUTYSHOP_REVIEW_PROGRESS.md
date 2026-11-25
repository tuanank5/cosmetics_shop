# Tiến độ rà soát chức năng BeautyShop · Lượt 1

## Điểm bắt đầu

- Nhánh `main`, HEAD `9a812ca433be3875dd423ff69aacaa31cc00ed1c`. `git status --short --branch` hiển thị `main...origin/main`. Diff tracked và staged trống. Tài liệu kiểm thử thủ công của owner là file untracked duy nhất lúc bắt đầu.
- Đã đọc quy ước `AGENTS.md`, README, SRS, Business Rules, ERD, thiết kế giao dịch và nhật ký quyết định. Đã lấy đủ 73 dòng ca trong tài liệu thủ công, gồm 44 ca ghi đúng “Đạt”, 2 ca ghi “Đạt” kèm nhận xét, 14 “Chưa chạy”, 1 để trống và 12 chỉ có nhận xét/câu hỏi/yêu cầu. Không có ô kết quả chỉ định “Chặn”. Các nhãn có thể chồng lấp về ý nghĩa, ví dụ một ca Đạt đồng thời có sai lệch về HTTP.
- SHA-256 của [tài liệu kết quả gốc](../testing/03-huong-dan-kiem-thu-thu-cong.md) lúc bắt đầu là `C8702602F88A85452E67F56987C8B20CC7505AFFDB6060FE0364E0C7790DC4A5`.
- Đã kiểm kê nguồn Java, test, tài nguyên giao diện và báo cáo QA. Bước kế tiếp là truy vết SHOP, sau đó TK, DH, SM, AD, MO, TV, QA. Chưa chạy app, gọi DB hoặc chạy lại test.

## SHOP · 13/13

- Đối chiếu SHOP-01–13 với `Bootstrap_Controller`, `CuaHang_Controller`, `CuaHang_Service`, `CuaHang_Repository`, template home/product/header/footer, `home-carousel.js`, `app.js` và QA P18/P19/P21. Đã ghi mục 4.1 của báo cáo.
- Xác định lưu chương trong `history.state`, lọc/sắp xếp trong DB trước khi lấy 12, điều kiện đóng panel, phân trang và đích thật của footer. Nháy logo còn cần ghi nhận đúng entry/scroll trong trình duyệt. Phân trang review và ảnh/giảm chuyển động là các ca owner chưa chạy.
- Tiếp tục ở TK-01–10, tập trung merge giỏ, layout nút mật khẩu và wishlist.

## TK · 10/10

- Đã đọc `GioKhach`, `GioHang_Controller/Service/Repository`, `LoginSuccess_Handler`, `Security_Config`, `TaiKhoan_Controller`, `NguoiDung_Service`, `DiaChiNguoiDung_Service`, `YeuThich_Repository`, các form auth/cart/wishlist, `components.css`, `app.js` và contract V3. Đã ghi mục 4.2.
- Gộp có mutex, transaction, khóa giỏ/SKU và UUID. Thông báo cắt số chỉ ở trang giỏ. Wishlist chưa có trạng thái trên thẻ/PDP và redirect về list là hành vi backend. Nút mật khẩu thay nhãn trong flex wrap, cần đo lại kích thước trình duyệt.
- Điểm tiếp tục sau nhóm này: checkout, COD, giao diện 403/404.

## DH · 8/8

- Đã đọc `Checkout_Controller`, `DonHang_Controller`, `DonHang_Service/Repository`, `QuyenDonHang_Service`, `PhieuGiamGia_Service/Repository`, hợp đồng tồn/voucher, template lỗi và test MariaDB.
- Đã mô tả khóa giỏ/SKU/voucher, snapshot giá/địa chỉ, trạng thái giao/COD, làm rõ 404 theo ownership và ghi nguyên văn 403 owner thấy. Cần xác minh request/status của 403. DH-03/07/08 còn thiếu lần thử tay của owner.
- Tiếp tục ở SM-01–06, Admin review và đổi trả.

## SM · 6/6

- Đã xem `SauMua_Controller`, `SauMua_Service`, `DanhGia_Repository/Store`, `MongoDanhGia_Store`, `DoiTra_Repository`, form review/đổi trả và bằng chứng P23. Đã ghi mục 4.4, đủ 6 mã owner ghi Đạt.
- Xác nhận MariaDB kiểm quyền và đơn đã giao; store review chọn một nơi để ghi; đổi trả khóa đơn và kiểm lượng còn lại. Cần phân biệt bằng chứng P23 có các bước review với phân trang review của SHOP-11.
- Tiếp tục qua các route Admin và chốt hiện trạng tùy chọn lọc đơn.

## AD · 15/15

- Đã xem các Controller dưới `controller/admin`, `ThongKe_Service/Repository`, `DonHang_Service/Repository`, service/repo catalog, tài khoản và SKU, template sidebar/dashboard/orders/reviews/catalog, CSS/JS chung, test liên quan và bằng chứng P20C.
- Đã ghi mục 4.5. Lọc đơn có query status từ dashboard và in-memory, thiếu form lọc và giữ ngữ cảnh trên link Quay lại. Hard delete danh mục/thương hiệu có ở cả route và service; đã ghi phạm vi cần xem xét. Duyệt/ẩn giữ tham số nhưng chưa có đo layout của hiện tượng owner ghi và còn nhánh trang rỗng sau lọc. AD-14 chưa chạy tay.
- Tiếp tục rà MoMo, TV và QA, sau đó hoàn thiện bảng và đối chiếu nguyên văn cột kết quả.

## MO · 7/7

- Đã đọc `Momo_Controller`, `Momo_Facade`, `ThanhToanMomo_Service/Repository`, `Momo_Settings`, `MomoReconciliation_Job`, hợp đồng gateway/mock/Sandbox và log P10/P20C. Đã ghi mục 4.6.
- Đã phân biệt đối soát thủ công với lịch job, mock với Sandbox và trạng thái cần hoàn với hoàn thật. Sáu ca MO-02–07 chưa chạy theo kết quả owner, MO-01 Đạt.
- Tiếp tục kiểm hai route tư vấn, phạm vi provider mô phỏng và câu hỏi owner mới.

## TV · 7/7

- Đã đọc `TuVan_Controller`, `TuVanDa_Controller`, `AdminTuVan_Controller`, `TuVan_Service`, `TuVanDaService`, `KhoTriThuc_Service`, `TriThuc_Repository`, DTO và test FAQ/RAG. Đã ghi mục 4.7.
- Đã tách form tư vấn da, FAQ và nạp lại RAG. TV-03 Đạt nhưng không ghi rõ nhánh AI thật, TV-07 chưa chạy. Chỉ mô tả hiện trạng AI.
- Tiếp tục QA cross-cutting và tổng hợp bằng chứng chạy đã lưu.

## QA · 7/7

- Đã rà Spring Security, trạng thái phiên, route riêng tư, trang lỗi, tài nguyên responsive, bàn phím/no-JS, upload và MongoDB tạm ngừng. Đã ghi mục 4.8.
- QA-03 để trống, QA-06 chưa chạy tay. P23 có bằng chứng độc lập MongoDB off và đổi kho. Các ca CSRF/read ownership có test riêng.
- Tiếp tục chốt bảng 73 dòng nguyên văn, bảng chờ xác minh, các đề xuất và kiểm diff.

## Kết thúc lượt 1

- [Báo cáo chức năng](BEAUTYSHOP_FUNCTIONAL_REVIEW.md) có bảng 73 mã duy nhất theo đúng thứ tự gốc. Đã đối chiếu từng ô kết quả với nguồn, 73/73 trùng nguyên văn kể cả ô QA-03 để trống, và mỗi ca có mục phân tích dẫn tới luồng tương ứng.
- Đã đối chiếu tên lớp gắn với log MariaDB P20C, tách log lịch sử, fixture, dữ liệu mock và kết quả owner ghi. Lần này không chạy lại ứng dụng hay test. Bảng ca chưa chạy có 14 mã và một mã ô trống.
- Trạng thái Git khi kết thúc vẫn ở `main` và cùng HEAD. Hai báo cáo Markdown là hai file mới; file kết quả gốc có SHA-256 như trước. Diff tracked và staged trống.
- Điểm còn để kiểm chứng gồm request 403 của DH-05, nháy logo và hai hiệu ứng bố cục, ma trận QA còn trống, lượt Admin harness với dữ liệu demo, điều kiện Sandbox/Ollama thật và kiểm vận hành ngoài giao diện. Đã ghi điểm tiếp tục ở mục 9–10 của báo cáo. Lượt 1 hoàn tất.

## Lượt 2 · Thiết kế nâng cấp

- Mốc ban đầu `main` tại `9a812ca433be3875dd423ff69aacaa31cc00ed1c`. Ba tài liệu lượt 1 chưa được Git theo dõi. Đã đối chiếu nguồn yêu cầu, D21/D24/D32, ba đầu vào, truy vấn catalog, trạng thái đơn và cách Swiper lưu/khôi phục. Kế tiếp kiểm tra hợp đồng transversal, transaction, Mongo và vận hành.
- Đã ghi nhóm MVC và UI vào kế hoạch: 8 lớp entity, JPQL, projection, cả 4 nhóm template có details, dialog, pagination, component, điều hướng, trace SHOP-01 và TK-06. Đã phân biệt fallback server với trạng thái history/tab. Kế tiếp ghi hợp đồng dữ liệu, security và đối chiếu vận hành.
- Đã ghi hợp đồng catalog, TK-04/TK-09, checkout, DH-05, AD-05/06/10/15 và Mongo duy nhất. Đối chiếu trạng thái hiển thị MoMo ưu tiên PAID, seed SQL review, index chỉ tạo lúc ApplicationReadyEvent và sự khác nhau giữa kết quả rỗng với Mongo-off. Còn ma trận, đánh giá ý tưởng, nghiệm thu và rollback.
- Đã hoàn tất đánh giá tồn theo lô, hoàn tiền, vận chuyển, xếp hạng, cache, ảnh, job, backup, tài khoản và audit. Đã kiểm tra seed-demo-orders, SQL ranking, callback file ảnh, filter phiên và query danh sách. Kế tiếp chốt phụ thuộc, vùng sửa và tiêu chí đóng gói.
- Hoàn thành lượt 2. Mỗi mã trong 12 Work Package xuất hiện ở cả Dependency Matrix, File Impact Matrix và phiếu nghiệm thu/rollback. Đã đối chiếu status/diff và SHA-256 của phiếu test/báo cáo lượt 1 với mốc ban đầu. Chỉ thêm kế hoạch và nối tiếp nhật ký này. Điểm tiếp tục của lượt 3 là §7–10 của BEAUTYSHOP_UPGRADE_PLAN.md, sau đó đối chứng trace logo/403, metadata BSON, index và semantics thanh toán. Các phép đo, test và Phase triển khai vẫn ở trạng thái dự kiến.

## Lượt 3 · Kiểm định độc lập

- Đã xác định main/9a812ca, bốn Markdown untracked, tracked/staged sạch. So từng mã theo thứ tự và toàn bộ chuỗi Kết quả: 73/73 trùng. Kiểm log 270/48 skip, phân biệt service record ChiTietDon với repository record. Kế tiếp đối chiếu hợp đồng UI, review và transaction.
- Đã ghi bảng kiểm 14 quyết định, 13 phản biện và kiểm transaction/ownership/Mongo. Phát hiện constructor SQL trong ThongKeServiceTest, kết quả wishlist no-op và ưu tiên ảnh preview; đã phân biệt với triệu chứng còn chờ runtime. Điểm tiếp tục: hợp đồng UI, vận hành và các ma trận điều phối.
- Đã chốt contract về đầu home so với quay lại home-popular, no-JS/Back, error status và chính sách từng details. Đã rà rủi ro kho/hoàn tiền, partial, cache và phục hồi dịch vụ; giữ các ý tưởng mở rộng ngoài các gói bắt buộc. Còn manifest, regression gates và kiểm chứng tài liệu.
- Đã hoàn tất Audit: 7 Phase, 13 mã trong Execution Manifest, 18 liên kết nội bộ tồn tại. UI-01 cũ tách UI-01C/UI-01P; ma trận file, gate, rollback và checkpoint ở §6–9. Hash phiếu tay, báo cáo chức năng và kế hoạch cũ giữ nguyên; chỉ thêm Audit và nối tiếp progress. Kết luận có thể trình owner phê duyệt. Dừng ở lượt 3, chưa triển khai gói nào.
