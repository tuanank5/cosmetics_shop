# VOUCHER-FINAL · Biên bản review local

Ngày 28/11/2026, nền `5bdada6bb370b281319ff671fd8a9b62b5673560`. 24/24 SHA-256 của bản triển khai khớp manifest gốc và hai checkout regression ban đầu. Candidate cuối có 25 tệp source/migration/test. SHA-256 tổng hợp theo byte working tree là `a19f783050d4ea073b2f1aedfab234e65de1bb330c5dcbdd85dd57ed9cc24c51`. [Manifest cuối](source.json)

## Kết quả review

| Gate | Đối chiếu |
|---|---|
| Scope và preview | Mỗi SKU xuất hiện một lần trong giỏ. Tập ID hợp trong chiều, lọc giao giữa các chiều đã cấu hình. minSpend HÀNG trên hàng đủ điều kiện, SHIP trên phần còn lại của đúng tập sau phân bổ. Guest có principal null bị loại khỏi mã giới hạn Customer. Đọc giá SKU và giờ UTC từ DB. Quote chỉ đọc. |
| Quota và Admin | CHECK V1 bảo vệ tổng USED + HELD. Mọi luồng đặt đơn giữ khóa ghi trên hàng voucher theo ID. Quota mỗi khách được kiểm từ các chứng từ HELD/CONSUMED dưới cùng khóa. Review phát hiện bước INSERT trước đây dựa hoàn toàn vào precheck. Candidate thêm điều kiện SQL ngay tại INSERT, lấy khách qua FK của đơn. Hai unique của chứng từ bảo vệ từng đơn/slot và đơn/mã. Một khách có thể dùng cùng mã trên nhiều đơn đến đúng quota. Admin khóa cùng phiếu, so giới hạn mới với tổng đã dùng/giữ và nhóm theo từng khách trước UPDATE. |
| Tiền | Tổng dòng 520.000, phân bổ HÀNG 30.000, tiền hàng còn 490.000, phí chuẩn 25.000, giảm SHIP 10.000, phí thực 15.000, đơn 505.000 VND. COD đối chiếu snapshot và attempt bằng SQL sau WebKit. MoMo so tổng từng dòng, tổng phân bổ, snapshot, attempt và đối số thực tế vào gateway mock bằng spy. Sai số cho phép 0 VND. |
| Làm tròn và miễn phí | Phần trăm làm tròn xuống, FIXED và PERCENT chịu cap và không vượt cơ sở giảm. Tỷ trọng chia nguyên VND, phần dư theo phần lẻ giảm dần rồi ID SKU tăng. Test 10.000 chia ba dòng ra 3.334/3.333/3.333. Từ ngưỡng 500.000 sau giảm HÀNG, phí và giảm SHIP bằng 0. SQL sau WebKit xác nhận đơn 860.000 chỉ có một chứng từ. |
| Transaction | COD chốt CONSUMED (USED) và kho cùng transaction. Hủy đủ điều kiện chỉ chuyển CONSUMED sang RELEASED một lần. MoMo HELD cùng giữ SKU, thành công xác minh chuyển CONSUMED, thất bại cuối của đơn đủ điều kiện chuyển RELEASED. FAILED của một attempt còn retry có thể vẫn giữ lượt cho đơn. REFUND_PENDING giữ nghĩa vụ tiền, chặn retry không an toàn. Hoàn sau giao không có đường tự tăng quota. |
| Concurrency | Checkout khóa giỏ, SKU theo ID, rồi phiếu theo ID. Hủy/IPN/query bắt đầu bằng khóa đơn, xử lý SKU rồi phiếu theo ID. Counter và chứng từ cùng rollback khi ghi slot hai lỗi. Race lượt cuối, retry song song, callback trùng, callback muộn và hủy tại gateway chạy đua với thành công đều có fixture. Controller trả lỗi có thể thử lại khi xung đột khóa. Chưa đo deadlock dưới tải production. |
| Quyền và audit | Guest cần đăng nhập để đặt đơn, ownership lấy từ principal. Admin-only và CSRF cho cấu hình mã. Client không quyết định giá, quota tiêu thụ hoặc kết quả gateway. Lỗi điều kiện dùng thông báo chung. Audit V8 dùng allowlist trạng thái, SUCCESS cùng transaction, FAILED sau rollback ở transaction riêng và unique correlation/action. Test no-op giữ nguyên số event. |
| V9 và legacy | V1–V8 giữ blob theo parent. V9 giữ lịch sử 1 mã ở HÀNG, không tính lại tiền cũ hoặc nhân đôi lượt. Fixture V8 có COD và MoMo, tổng 130.000 VND trước/sau. Chạy migrate, validate, migrate lại 0 thay đổi. Kiểm metadata FK và hai unique, đồng thời thử ghi vi phạm từng unique riêng và FK trên fixture. JPA dev validate khi khởi động. |
| Chỉ mục | Tra phiếu theo unique mã áp dụng rồi khóa PK theo ID. Tra receipt theo đơn dùng prefix của hai unique, theo voucher có index FK. Không khẳng định execution plan hay latency production cho phép đếm lượt theo khách. |

Hai lỗi phát hiện khi mở rộng test. Bootstrap còn kỳ vọng 7 migration nên sáu nhánh nâng cấp đỏ dù DB đã tới V9. Đã cập nhật đích và thêm nguồn V7/V8. Lượt kiểm đầu của guard mới cho thấy Spring dịch IllegalArgumentException phát ra từ repository thành DataAccessException. Repository nay trả số dòng thành kết quả boolean, service phát lỗi nghiệp vụ bên trong transaction để rollback. [Bootstrap trước sửa](bootstrap-before.txt) · [Integration trước sửa](integration-before.txt)

## Khôi phục khi nâng cấp lỗi

MariaDB có DDL tự commit. Trước triển khai cần lưu backup nhất quán và ghi revision, lịch sử Flyway, số chứng từ/tiền/counter theo slot. Nếu V9 dừng giữa chừng, dừng writer và job đối soát, giữ bản lỗi để điều tra. Dựng schema khác từ backup V8, xác nhận checksum, diễn tập migrate/validate V9 rồi đối chiếu tổng, kho, lượt và attempt. Owner duyệt chuyển kết nối khi bộ dữ liệu khớp. Trường hợp đã nhận đơn hai slot cần đối soát giao dịch phát sinh trước khi chuyển. Khôi phục backup production chưa chạy trong WP này.

## Gate tiếp theo

VOUCHER IN_PROGRESS đến review PR, CI đúng revision và owner nghiệm thu. Sau merge, PAYMENT cần chạy lại tổng quote/đơn/attempt/gateway, lịch sử retry/IPN, snapshot giảm theo dòng, lượt hai slot, kho và KPI trên chính revision tích hợp. PAYMENT PARTIAL_ACCEPTED / BLOCKED_DEPENDENCY. AUTH-LIVE và AUDIT PURGE BLOCKED_EXTERNAL. MoMo Sandbox và manual UAT chưa chạy.

## Regression và đối chiếu SKIP

JDK 21 Maven 244 PASS/102 SKIP, 0 FAIL. MariaDB/MongoDB cô lập 127/127 PASS. Bootstrap MariaDB riêng 10/10 PASS. Package PASS. Cổng thật và manual UAT chưa chạy. [Maven](maven.txt) · [Fixture chính](integration.txt) · [Bootstrap](bootstrap.txt) · [Package](package.txt)

101 SKIP ban đầu chia theo điều kiện JUnit như sau. Số lượng là phương thức. XML fixture xác nhận 80 phương thức với 105 lượt PASS do tham số hóa. 21 phương thức chưa chạy ban đầu được đối chiếu bên dưới. Bản cuối thêm một test đối chiếu amount và SQL nên có 102 SKIP mặc định, 89 phương thức đã có ít nhất một lần chạy đạt trên fixture. [Danh sách 101 SKIP gốc và invocation](skip-initial.json) · [Danh sách 102 SKIP cuối và invocation](skip-final.json)

| Điều kiện chưa bật mặc định | Số phương thức ban đầu |
|---|---:|
| P01_MARIADB_TEST | 86 |
| P23_MONGO_TEST | 9 |
| LOCAL_BOOTSTRAP_MIGRATION_FIXTURE | 3 |
| P21_DEMO_REPORT | 1 |
| AUTH_02_MIGRATION_FIXTURE | 1 |
| ARCH_01_BSON_PHASE | 1 |

| Lớp và phương thức | Phạm vi | Kết quả cuối |
|---|---|---|
| LocalDatabaseMigration_MariaDb_Test · nangCapVaGiuNguoiDungTuLichSuChinhThuc(int) | Nâng cấp V9 và kiểm tra từ chối checksum sai | PASS (8 lượt) |
| LocalDatabaseMigration_MariaDb_Test · tuChoiChecksumKhongKhopTruocKhiChayBanMoi | Nâng cấp V9 và kiểm tra từ chối checksum sai | PASS (1 lượt) |
| LocalDatabaseMigration_MariaDb_Test · tuChoiSchemaCuKhongCoLichSu | Nâng cấp V9 và kiểm tra từ chối checksum sai | PASS (1 lượt) |
| CuaHang_MariaDb_Test · locTiengVietSapXepTheoSkuVaPhanTrang | SKU và giá hiện hành | PASS (1 lượt) |
| CuaHang_MariaDb_Test · chonSkuDocGiaTonVaAnhHienHanh | SKU và giá hiện hành | PASS (1 lượt) |
| DocView_MariaDb_Test · doCotSoTruyVanVaHtmlCuaNhungDuongDoc | Đọc nhiều SKU và giỏ Guest | PASS (1 lượt) |
| DocView_MariaDb_Test · gioKhachDocNhieuSkuTrongMotTruyVan | Đọc nhiều SKU và giỏ Guest | PASS (1 lượt) |
| GoiYPhoBienDemoReport_Test · inBangDiemTop5 | Báo cáo gợi ý demo | NOT RUN · ngoài gate VOUCHER |
| GoiYSanPham_MariaDb_Test · demBanDanhGiaYeuThichDungDieuKien | Xếp hạng gợi ý sản phẩm | NOT RUN · ngoài gate VOUCHER |
| HttpLoi_MariaDb_Test · httpThucGiuStatusSoHuuRoleCsrfVaErrorDispatch | Phân quyền HTTP, CSRF và ownership | PASS (1 lượt) |
| MongoTat_MariaDb_Test · trangChinhVanChayKhiMongoTat | Degraded mode khi Mongo dừng | NOT RUN · ngoài gate VOUCHER |
| MongoDanhGia_Store_Test · phanTrangAdminVaCongKhaiMoiNhatTruoc | Store đánh giá Mongo, index và snapshot riêng | NOT RUN · ngoài gate VOUCHER |
| MongoDanhGia_Store_Test · taoMauSaoLuu | Store đánh giá Mongo, index và snapshot riêng | NOT RUN · ngoài gate VOUCHER |
| MongoDanhGia_Store_Test · findAndModifyTheoPhienBanVaTrangThaiLocAdmin | Store đánh giá Mongo, index và snapshot riêng | NOT RUN · ngoài gate VOUCHER |
| MongoDanhGia_Store_Test · indexSaiChanGhiVaKhongSuaDuLieu | Store đánh giá Mongo, index và snapshot riêng | NOT RUN · ngoài gate VOUCHER |
| MongoDanhGia_Store_Test · khiTrungThoiDiemThiSapTheoIdVaDemDungBoLocQuaCacTrang | Store đánh giá Mongo, index và snapshot riêng | NOT RUN · ngoài gate VOUCHER |
| MongoDanhGia_Store_Test · aggregationTrungBinhPhanBoNhieuSanPhamVaToanCuaHang | Store đánh giá Mongo, index và snapshot riêng | NOT RUN · ngoài gate VOUCHER |
| MongoDanhGia_Store_Test · haiNguoiGhiCungMucChiMotLanThanhCong | Store đánh giá Mongo, index và snapshot riêng | NOT RUN · ngoài gate VOUCHER |
| MongoDanhGia_Store_Test · indexDuocTaoVaKhoaDuyNhatChanTrungDongDon | Store đánh giá Mongo, index và snapshot riêng | NOT RUN · ngoài gate VOUCHER |
| MongoDanhGia_Store_Test · khoiDongKhiMongoTatRoiTaoIndexTruocGhiSauPhucHoi | Store đánh giá Mongo, index và snapshot riêng | NOT RUN · ngoài gate VOUCHER |
| ReviewBsonCompatibilityTest · documentRoundTripAcrossPackageVersions | Tương thích BSON của review | NOT RUN · ngoài gate VOUCHER |

WebKit đã chạy trên JAR của candidate. Cả bốn nhóm đạt, gồm Guest→login→merge, checkout JS/no-JS, quyền Customer và Admin cấu hình phạm vi. Ba đơn COD đối chiếu SQL có tổng đơn bằng attempt 505.000/505.000/860.000 VND và số slot 2/2/1. [Log WebKit](webkit.txt)

Tái lập từ checkout candidate với JDK 21 và cache Maven như báo cáo triển khai. Chạy thêm `docs/qa/VOUCHER/final-review/run-bootstrap.ps1` với cùng ba tham số Checkout, MavenRepo, EvidenceDirectory như runner MariaDB. Chọn một thư mục evidence mới cho mỗi lượt. Các manifest và log ở thư mục cha mô tả lượt trước FINAL.
