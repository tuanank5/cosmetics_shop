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
| `REVIEWS_STORE` | `mongo` | `mongo` hoặc `mariadb` (dự phòng: đọc/ghi bảng `danh_gia` cũ). |
| `REVIEWS_MIGRATE_FROM_MARIADB` | `false` | `true`: khi khởi động, chép bảng `danh_gia` sang MongoDB một lần (idempotent). |

- Local không bật xác thực vì chỉ bind `127.0.0.1`.
- Production dùng URI có user/password và TLS, lấy từ secret store, ví dụ `mongodb+srv://<user>:<password>@<host>/beautyshop?retryWrites=true`. User chỉ cần quyền `readWrite` trên database của app.
- Không commit URI có mật khẩu. `.env.example` chỉ ghi giá trị local.
- Timeout ngắn (`review/MongoDanhGia_Config.java`): chọn server 2 s, kết nối 2 s, đọc 5 s.
- Index được tạo khi app khởi động, từ annotation của `DanhGia_Document`, bằng `IndexOperations`. Nếu MongoDB tắt, app chỉ log WARN và vẫn chạy.

Kiểm tra index:

```powershell
mongosh beautyshop_demo --quiet --eval "db.danh_gia.getIndexes().map(i => i.name)"
# [ '_id_', 'uq_ma_chi_tiet_don_hang', 'idx_nguoi_dung', 'idx_san_pham_hien_thi_ngay', 'idx_hien_thi_ngay' ]
```

## Quy trình dữ liệu demo

`scripts/seed-demo-orders.ps1` vẫn ghi đánh giá demo vào bảng MariaDB `danh_gia`, như từ P20. Script không ghi trực tiếp vào MongoDB. Từ P23, script còn thêm 30 lượt yêu thích demo; script idempotent. Sau khi seed:

```powershell
# 1. Seed MariaDB (đơn, đánh giá demo, yêu thích demo); DB_PASSWORD lấy từ môi trường.
.\scripts\seed-demo-orders.ps1 -Database beautyshop_demo -User $env:DB_USER -Client 'C:\Program Files\MariaDB 12.2\bin\mariadb.exe'

# 2. Chạy app MỘT lần với cờ chuyển dữ liệu, xem log rồi dừng app.
$env:REVIEWS_MIGRATE_FROM_MARIADB = 'true'
java -jar target\beautyshop-0.0.1-SNAPSHOT.jar --server.port=8081
#   log: "P23 migrate danh_gia MariaDB -> MongoDB: doc=N, ghi=N, bo qua=0 (...)"

# 3. Tắt cờ và chạy bình thường.
Remove-Item Env:REVIEWS_MIGRATE_FROM_MARIADB
```

- Chạy lại bước 2 không nhân đôi document: upsert theo `maChiTietDonHang`, chỉ dùng `$setOnInsert`. Document đã có, kể cả bản khách đã sửa hoặc Admin đã duyệt, không bị đè.
- Đánh giá đang hiển thị trong MariaDB được coi là đã duyệt.
- Khi store là `mongo`, bảng MariaDB `danh_gia` không còn được ghi.

## Kiểm thử

```powershell
mvn -o test                                   # không cần MongoDB
$env:P23_MONGO_TEST = 'true'; mvn -o test     # thêm test MongoDB thật trên database beautyshop_test_mongo
```

- Mọi test tải Spring context đều dùng `beautyshop_test_mongo`, theo `src/test/resources/config/application.properties`. Database này bị xóa dữ liệu trước mỗi test.
- `SauMua_MongoDanhGia_Test` cần cả `P01_MARIADB_TEST=true` và `P23_MONGO_TEST=true`.

## Sao lưu demo

```powershell
mongodump --db beautyshop_demo --collection danh_gia --out backup\mongo-<ngày>
mongorestore --db beautyshop_demo --collection danh_gia backup\mongo-<ngày>\beautyshop_demo\danh_gia.bson
```

Lưu bản sao lưu ngoài Git, cùng bản backup MariaDB.
