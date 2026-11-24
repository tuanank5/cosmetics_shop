# Báo cáo kiểm thử P10

Chạy ngày 19/11/2026 bởi Codex trên working tree P08–P10, từ commit nền `448225d`. Môi trường Windows, Java 21, Spring Boot 3.5.16 và MariaDB 12.2.2 với database riêng. Browser E2E chạy JAR profile `dev` và Chrome headless qua Playwright.

`mvn compile`, `mvn test` và `mvn -DskipTests package` thành công. Surefire chạy 49 test, 0 lỗi, 0 bỏ qua. Flyway áp dụng V1–V5 trên database mới và JPA validate. JAR trả HTTP 200 cho `/`, `/products`, `/login` và redirect khách chưa đăng nhập từ `/admin/catalog`. Flyway cảnh báo MariaDB 12.2 mới hơn phiên bản đã được Flyway kiểm chứng.

## TC01–TC20

Các tên dưới đây chỉ đến lớp kiểm thử trong [src/test/java/com/beautyshop](../../src/test/java/com/beautyshop/). Kết quả `PASS (mock)` áp dụng cho môi trường mô phỏng đã chạy.

| TC | Dữ liệu và kỳ vọng | Kết quả quan sát | Trạng thái và bằng chứng |
|---|---|---|---|
| TC01 | Guest thêm SKU vào giỏ rồi đăng nhập. Merge đúng một lần và kiểm lại tồn. | SKU vẫn ở giỏ sau login. Hai yêu cầu dùng chung snapshot chỉ gộp một lần. | PASS · `XacThuc_MariaDb_Test`, [giỏ sau login](../report/evidence-p10/cod-03-cart.png) |
| TC02 | Giỏ có hai SKU, một SKU hết trước checkout. Từ chối và giữ tồn SKU khác. | Form báo “Không đủ tồn”, không tạo đơn và tồn SKU còn lại vẫn là 2. | PASS · `DonHangCOD_MariaDb_Test.checkoutHetHangGiuNguyenMucKhacVaAdminHuyHoanTonMotLan` |
| TC03 | Hai Customer checkout đồng thời một SKU còn 1. | Một đơn thành công, tồn còn 0 và giỏ bên thua còn SKU. | PASS · `DonHangCOD_MariaDb_Test.haiCustomerTranhSkuVaRollbackKhoVoucherVaGio` |
| TC04 | Admin hủy đơn COD hợp lệ và gửi lại cùng thao tác. | SKU giảm từ 2 xuống 1 khi đặt, về 2 khi hủy và vẫn là 2 sau lần lặp. | PASS · `DonHangCOD_MariaDb_Test.checkoutHetHangGiuNguyenMucKhacVaAdminHuyHoanTonMotLan` |
| TC05 | Checkout online với tồn 2, mua 1 và dùng voucher. | Đơn `PENDING_PAYMENT`. On-hand 2, tạm giữ 1, còn khả dụng 1. Voucher giữ 1 lượt. | PASS (mock) · `Momo_MariaDb_Test.ipnKySaiSaiSoTienVaReplayKhongTruTonHaiLan` |
| TC06 | IPN ký hợp lệ và báo thành công. | Attempt `PAID`, đơn `PENDING_CONFIRMATION`, chốt một SKU và một lượt voucher. | PASS (mock) · `Momo_MariaDb_Test.ipnKySaiSaiSoTienVaReplayKhongTruTonHaiLan` |
| TC07 | Gửi lại IPN thành công. | HTTP 204 cho cả hai lần. Tồn còn 1, lượt dùng voucher là 1. | PASS (mock) · `Momo_MariaDb_Test.ipnKySaiSaiSoTienVaReplayKhongTruTonHaiLan` |
| TC08 | IPN sai chữ ký hoặc sai số tiền. | HTTP 401 hoặc 400. Đơn vẫn `PENDING_PAYMENT`. | PASS (mock) · `Momo_MariaDb_Test.ipnKySaiSaiSoTienVaReplayKhongTruTonHaiLan` |
| TC09 | Reservation quá hạn, cổng chưa xác nhận kết quả. | Đối soát giữ trạng thái chờ và giữ 1 SKU. Chỉ giải phóng sau khi cổng báo hết hạn. | PASS (mock) · `Momo_MariaDb_Test.chiGiaiPhongKhiCongXacNhanHetHanVaDanhDauThanhCongMuon` |
| TC10 | IPN thành công đến sau khi reservation đã giải phóng. | Đơn vẫn `EXPIRED`, tồn 2, voucher chưa tiêu thụ và đúng một attempt cần hoàn/đối soát. | PASS (mock) · `Momo_MariaDb_Test.chiGiaiPhongKhiCongXacNhanHetHanVaDanhDauThanhCongMuon` |
| TC11 | Hai Customer mua hai SKU khác nhau và tranh voucher còn 1 lượt. | Một đơn được tạo, một SKU được trừ và voucher dùng đúng 1 lượt. | PASS · `DonHangCOD_MariaDb_Test.voucherConMotLuotChiChoMotDonKhiHaiSkuKhacNhau` |
| TC12 | Giỏ 510.000 ₫, voucher 10% trần 20.000 ₫. Phí giao tính sau giảm. | Tổng 515.000 ₫. Unit test ngưỡng 499.999/500.000 ₫ trả phí 25.000/0 ₫. | PASS · `DonHangCOD_MariaDb_Test`, `ShippingFeePolicyTest` |
| TC13 | Customer khác mở chi tiết đơn hoặc POST hủy. | Cả hai trả HTTP 404. | PASS · `DonHangCOD_MariaDb_Test.checkoutHuyVaGiaoCodEndToEnd` |
| TC14 | Customer gửi đánh giá cho đơn chưa giao. | Backend trả HTTP 409. | PASS · `SauMua_MariaDb_Test.danhGiaDaGiaoChiChuMucVaChiCongKhaiSauDuyet` |
| TC15 | Customer gửi lại đánh giá cùng mục của đơn đã giao. | Gửi tạo lần hai trả HTTP 409. Sửa qua form riêng thành công và cần duyệt lại. | PASS · `SauMua_MariaDb_Test.danhGiaDaGiaoChiChuMucVaChiCongKhaiSauDuyet` |
| TC16 | Gửi yêu cầu đổi trả vào ngày thứ tám. | Từ chối và hiển thị giới hạn 7 ngày. | PASS · `SauMua_MariaDb_Test.doiTraNhieuMucCoHanVaTrangThaiThuCongKhongDoiTonHayThanhToan` |
| TC17 | Admin gửi yêu cầu chuyển từ `PENDING_PAYMENT` sang `DELIVERED`. | Backend báo lỗi và giữ `PENDING_PAYMENT`. | PASS (mock) · `Momo_MariaDb_Test.ipnKySaiSaiSoTienVaReplayKhongTruTonHaiLan` |
| TC18 | Hỏi ngoài catalog/FAQ trong RAG. | Trả “Chưa đủ thông tin”. Câu hỏi có nguồn dùng giá và tồn mới từ MariaDB. | PASS (mock Ollama) · `TuVanRag_MariaDb_Test.ragCoNguonGiaTonMoiVaFallbackKhiModelSaiHoacLoi` |
| TC19 | File HTML đặt tên `sku.png` và file PNG đặt tên `sku.html`. | Cả hai báo lỗi ảnh. Upload ảnh hợp lệ vẫn đọc được với header `nosniff`. | PASS · `BienTheSanPham_MariaDb_Test.crudSkuVaAnhKiemTraQuyenSoHuuBienTheVaTonDangGiu` |
| TC20 | 375, 768 và 1440 px ở trang chủ, catalog, chi tiết, login, giỏ, tư vấn và đơn admin. | 21 lượt kiểm tra có HTTP 200, không tràn ngang, nút chính và link điều hướng đạt 44 px. | PASS · [375 px](../report/evidence-p10/catalog-375.png), [768 px](../report/evidence-p10/catalog-768.png), [1440 px](../report/evidence-p10/catalog-1440.png) |

## Kiểm chứng bản demo

- **Catalog PASS.** Nạp hai lần vào database demo riêng. Số bản ghi là 31 sản phẩm, 32 SKU, 8 thương hiệu, 6 danh mục, 1 voucher. Sao lưu và khôi phục vào schema trống khác cho cùng số bản ghi.
- **COD E2E PASS.** Chrome lọc Vanicream, chọn SKU 16 oz, thêm giỏ với tư cách Guest, đăng nhập, thêm địa chỉ, áp mã DEMO10 và đặt COD. Admin xác nhận, đóng gói, giao và xác nhận thu tiền. Customer thấy đã giao và dashboard hiện 394.000 ₫. [Báo giá](../report/evidence-p10/cod-04-quote.png) · [Dashboard](../report/evidence-p10/cod-06-admin.png) · [Đơn đã giao](../report/evidence-p10/cod-07-delivered.png).
- **MoMo Sandbox thật — BLOCKED_EXTERNAL.** Chưa có merchant và IPN HTTPS công khai. TC05–TC10 dùng adapter mock.
- **Ollama thật — BLOCKED_AI.** Môi trường chưa có Ollama/model. Chạy qua Spring AI và HTTP mock cho TC18. [FAQ trên trình duyệt](../report/evidence-p10/faq.png) trả lời từ MariaDB ở chế độ mặc định.
- **Host công khai — BLOCKED_HOST.** Chưa có host/domain đã cấp. [Runbook local, backup và yêu cầu triển khai](../deployment/06-p10-demo.md).
