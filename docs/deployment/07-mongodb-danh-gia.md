# MongoDB cho đánh giá sản phẩm (P23)

Từ P23, nội dung đánh giá (số sao, bình luận, trạng thái duyệt) lưu ở MongoDB. MariaDB vẫn kiểm điều kiện được đánh giá: đúng chủ đơn, đơn DELIVERED, mỗi dòng đơn một đánh giá. Chi tiết thiết kế: [báo cáo P23](../reviews/P23-danh-gia-mongodb.md), quyết định D32 trong [DECISIONS](../DECISIONS.md).

## Cài đặt local (Windows)

1. Cài MongoDB Community Server **6.0.x** (nhóm đang dùng 6.0.8) dạng Windows service `MongoDB`. Không cần Docker.
2. Sửa `C:\Program Files\MongoDB\Server\6.0\bin\mongod.cfg`:

   ```yaml
   net:
     port: 27017
     bindIp: 127.0.0.1        # chỉ nghe trên máy local
   storage:
     wiredTiger:
       engineConfig:
         cacheSizeGB: 0.25    # máy 8 GB RAM chạy cùng MariaDB, JVM và Chrome
   ```

3. Khởi động lại service. Cần cửa sổ PowerShell **Run as Administrator**:

   ```powershell
   Restart-Service MongoDB
   Get-Service MongoDB          # Status = Running
   mongosh --quiet --eval "db.runCommand({ ping: 1 })"
   ```

> Lưu ý về bộ nhớ: ngày 24/11/2026, mongod tự dừng với lỗi `out of memory` (log mức `"s":"F"`) khi chạy cùng lúc bộ test Maven, app và Chrome trên máy 8 GB. Khi thấy trang báo "Tạm thời chưa tải được đánh giá", kiểm tra `Get-Service MongoDB` và cuối file `log\mongod.log`, rồi khởi động lại service bằng quyền Administrator. Trong QA, app chạy với `-Xmx512m`.

## Cấu hình ứng dụng

| Biến môi trường | Mặc định | Ý nghĩa |
|---|---|---|
| `MONGODB_URI` | `mongodb://127.0.0.1:27017/beautyshop_demo` | Kết nối và database đánh giá. |

- Local không bật xác thực vì chỉ bind `127.0.0.1`.
- Production dùng URI có user/password và TLS, lấy từ secret store, ví dụ `mongodb+srv://<user>:<password>@<host>/beautyshop?retryWrites=true`. User chỉ cần quyền `readWrite` trên database của app.
- Không commit URI có mật khẩu. `.env.example` chỉ ghi giá trị local.
- Timeout ngắn (`review/MongoDanhGia_Config.java`): chọn server 2 s, kết nối 2 s, đọc 5 s.
- Ứng dụng kiểm index khi khởi động, khi kết nối phục hồi và trước khi ghi. Index `uq_ma_chi_tiet_don_hang` phải có key `{maChiTietDonHang: 1}`, unique, không sparse hay partial. Sai chỉ mục, trùng document hoặc thiếu quyền sẽ làm kho đánh giá tạm ngưng và thử lại sau 15 giây. Kiểm tra bằng tài khoản vận hành trước khi xử lý thủ công.

Kiểm tra index:

```powershell
mongosh beautyshop_demo --quiet --eval "db.danh_gia.getIndexes().map(i => i.name)"
# [ '_id_', 'uq_ma_chi_tiet_don_hang', 'idx_nguoi_dung', 'idx_san_pham_hien_thi_ngay', 'idx_hien_thi_ngay' ]
```

## Quy trình dữ liệu demo

Chạy `scripts/seed-demo-orders.ps1` để tạo đơn, tồn minh họa và lượt yêu thích demo. Để tạo đánh giá demo, đăng nhập bằng Customer của fixture có đơn đã giao, gửi đánh giá từ dòng đơn và duyệt bằng Admin. Bảng SQL `danh_gia` chỉ lưu lịch sử; giữ nguyên các dòng đã có.

## Kiểm thử

```powershell
mvn -o test                                   # không cần MongoDB
$env:P23_MONGO_TEST = 'true'; mvn -o test     # thêm test MongoDB thật trên database beautyshop_test_mongo
```

- Mọi test tải Spring context đều dùng `beautyshop_test_mongo`, theo `src/test/resources/config/application.properties`. Database này bị xóa dữ liệu trước mỗi test.
- `SauMua_MongoDanhGia_Test` cần cả `P01_MARIADB_TEST=true` và `P23_MONGO_TEST=true`.

## Sao lưu và diễn tập phục hồi

1. Sao lưu SQL đầy đủ gồm bảng `danh_gia` lịch sử. Ghi số dòng, checksum mẫu và mốc migration.
2. Dùng `mongodump --archive` cho collection `danh_gia` để giữ document và metadata index. Sao lưu thư mục ảnh và danh sách UUID tham chiếu.
3. Khôi phục SQL vào schema mới. Khôi phục Mongo vào namespace mới bằng `mongorestore --archive --nsFrom --nsTo --stopOnError`. Đối chiếu số dòng, mẫu, revision và index unique trước khi cho ghi. Khôi phục ảnh vào thư mục riêng và đối chiếu hash. Giữ backup ngoài Git.
4. Khi cần quay lui, tạm đóng tính năng review hoặc dùng bản tương thích cấu hình Mongo và tắt runner ở bản cũ. Giữ nguyên cả hai kho và dữ liệu đã tạo sau mốc backup.

Diễn tập với dữ liệu tổng hợp trong repository dùng `docs/qa/DATA-01/run-fixture.ps1`. Dữ liệu demo/owner cần bản sao riêng do người vận hành quản lý.
