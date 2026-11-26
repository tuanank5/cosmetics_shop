# DATA-01 · Kho đánh giá MongoDB

Checkpoint ngày 26/11/2026 trên main, HEAD `6368b6723d425c23d8901a5ebc69bbaea3ac09b4` cộng diff SEC-01 đã qua G1 và diff DATA-01. Index Git ban đầu rỗng. SEC-01 được giữ trên cùng working tree, phần đụng chung ở GiaoDienPagesTest chỉ đổi fixture thẻ sản phẩm. Không sửa kết quả kiểm thử của owner.

## Thay đổi

- Gỡ `MariaDbDanhGia_Store`, `ChuyenDanhGiaSangMongo_Runner`, helper đọc nội dung cũ, cấu hình chọn/chuyển store và phần seed nội dung trong `scripts/seed-demo-orders.ps1`. Giữ các truy vấn `mucMua`, `tenSanPham`, `sanPhamLoc`, seed đơn/wishlist và V1 nguyên vẹn. Bảng review MariaDB vẫn nằm trong backup lịch sử.
- `MongoDanhGia_Store` kiểm index khi startup, khi hết khoảng nghỉ sau lỗi và trước khi ghi. Kiểm tên, key, unique, sparse và partial theo metadata. Một luồng xử lý kiểm/tạo index tại một thời điểm. Khi thiếu quyền, index xung đột hoặc không thể dựng, store ngừng phục vụ và thử lại sau 15 giây. Ứng dụng không sửa document cũ để dựng index.
- `SauMua_Service`, `Bootstrap_Controller` và `fragments/product-card.html` truyền/hiện trạng thái tóm tắt đánh giá. PDP và Admin review giữ cơ chế trạng thái có sẵn. `ThongKe_Service` và `admin/dashboard.html` hiện thông báo khi không đọc được KPI review và ẩn bảng/biểu đồ review khi đó. Các KPI SQL còn hoạt động. Ranking dùng hành vi hiện có, bỏ điểm sao khi Mongo lỗi và giữ số bán/lượt thích.
- Fixture ở `GiaoDienPagesTest`, `ThongKeServiceTest`, `SauMuaDanhGiaServiceTest`, `MongoDanhGiaStoreNghiTest`, `MongoDanhGia_Store_Test`, `MongoTat_MariaDb_Test`, `GoiYSanPham_MariaDb_Test` và hai lớp `SauMua_*` tách nội dung review khỏi SQL. Một hàng lịch sử SQL tổng hợp chỉ tồn tại trong test kiểm xếp hạng và backup.
- Cập nhật D32, ERD, deployment/07, roadmap, sổ triển khai và ghi chú ARCH-01. Không đổi URL, role, CSRF, schema, migration, BSON key hay rule review đã giao.

Contract nội bộ DATA-01: model key `tomTatDanhGia` vẫn giữ tên, giá trị chuyển từ Map sang `TomTatTheSanPham(theoSanPham, khongTaiDuoc)`. Caller là Bootstrap, catalog và fragment product card. `TongQuan.danhGiaChoDuyet` dùng null khi unavailable và `taiDuocDanhGia()` điều khiển chart/table/KPI. NAV-01, UI-01C, DTO-01 và QA-01 cần giữ contract fragment này khi chỉnh trang thẻ/dashboard. REVIEW-01 dùng trạng thái lỗi hiện tại của store. ARCH-01 cần chạy lại ca recovery và giữ index/BSON mapping sau đổi package.

## Kiểm chứng

Temurin 21.0.13.1, Maven 3.9.15, profile dev, MariaDB 12.2.2 và MongoDB 6.0 trên loopback. Mọi DB dùng thư mục UUID dưới target, schema/namespace riêng và fixture tổng hợp. MoMo chỉ dùng adapter mock, OTP dùng fixture test. Thao tác restore đích khác nguồn và không cần dữ liệu owner.

| Lệnh | Kết quả |
|---|---|
| Baseline hai suite MariaDB/Mongo trước khi sửa | 6/6, 0 fail/error/skip. Restore thử 3 dòng SQL và 1 Mongo document |
| run-fixture.ps1 | 22/22, 0 fail/error/skip. Bao gồm SauMua_MongoDanhGia, MongoTat, store Mongo, COD, MoMo mock, ranking và thống kê SQL |
| run-fixture.ps1 -MongoOff | 8/8, 0 fail/error/skip. COD có đăng nhập và đọc đơn, MoMo mock và trang khi Mongo tắt |
| run-fixture.ps1 -Backup | 2/2, 0 fail/error/skip. SQL có 1 dòng lịch sử với checksum trùng, Mongo 1 BSON document có SHA-256 trùng và khôi phục đúng index unique. Ảnh tổng hợp giữ tên UUID và SHA-256 |
| run-fixture.ps1 -MongoOff -Ui | 2/2 cho MongoTat và ca HTTP/Tomcat SEC-01 trên cùng checkpoint. JAR trả HTTP 200, có thẻ, thông báo unavailable và liên kết sản phẩm |
| run-fixture.ps1 -Browser | 2/2 ca fixture, sau đó Chrome headless đạt cả 3 viewport 390, 768 và 1366px khi MongoDB tắt |
| mvn -o test và mvn -o -DskipTests package | 287 tổng, 229 đạt, 58 skip, 0 fail/error. Package exit 0 |

G2 dữ liệu: Mongo vắng lúc khởi động, SQL vẫn đáp ứng. Mở Mongo sau đó mà không dựng lại store, qua backoff bằng clock test rồi ghi thành công khi đã dựng unique index. Hai luồng tạo cùng một dòng đơn cho đúng một thành công và một lần trùng. Index không unique bị từ chối và dữ liệu cũ giữ nguyên. Bộ sau mua kiểm stale approve. Trang test kiểm trạng thái unavailable của PDP, thẻ và dashboard, không xuất bảng sao 0 giả. Nghiệp vụ đã kiểm trong môi trường thử, gồm đơn/checkout, COD và MoMo mock.

## Giao diện và nghiệm thu

Đã đóng G2 và cập nhật DATA-01 thành **DONE**. Do kết nối cửa sổ trình duyệt lỗi native pipe, lượt kiểm dùng Chrome headless 158 với hồ sơ riêng trên JAR loopback. Tại cả ba chiều rộng, khi tắt JavaScript của trang, thẻ hiện trạng thái tạm ngưng thay cho sao, Tab đến liên kết sản phẩm có viền focus, và Enter mở trang chi tiết có thông báo đánh giá chưa tải được. Không có cuộn ngang ở danh sách và chi tiết. Dashboard Admin được kiểm bằng MockMvc về trạng thái KPI, bảng và biểu đồ khi MongoDB tắt.

Lệnh chạy lại là `powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/DATA-01/run-fixture.ps1 -Browser`, sau khi đóng gói JAR bằng JDK 21. Bằng chứng nằm ở `docs/qa/DATA-01/browser-results.json`, `docs/qa/DATA-01/ui-http.json` và hai ảnh đã rà tại `docs/ui/screenshots/DATA-01/`.

Dữ liệu owner và phục hồi trên môi trường triển khai nằm ngoài fixture này.

Nếu lỗi khi triển khai, tạm đóng review hoặc quay về binary tương thích với cấu hình Mongo và runner tắt theo runbook. Bảo toàn bảng lịch sử, document Mongo và dữ liệu mới.

Bằng chứng tại `docs/qa/DATA-01/`. Gói kế tiếp theo lịch là `03-ARCH-01.md` khi được owner giao riêng. Chưa chạy ARCH-01, chưa commit hoặc push.
