# Chạy và sao lưu demo P10

## Demo local

1. Tạo database trống `beautyshop_demo` và user riêng theo [hướng dẫn MariaDB](01-local-db-and-flyway.md). Cấp quyền cho user trên database demo. Dùng thông tin nhân vật giả cho phần đăng ký và giao hàng.
2. Trong PowerShell, đặt `DB_URL=jdbc:mariadb://127.0.0.1:3306/beautyshop_demo`, `DB_USER`, `DB_PASSWORD`. Giữ các biến này trong môi trường local.
3. Chạy `mvn -DskipTests package`, rồi chạy JAR với profile dev và bind loopback để Flyway khởi tạo database.

```powershell
java -jar target/beautyshop-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev --server.address=127.0.0.1
```

4. Từ một PowerShell có cùng `DB_USER` và `DB_PASSWORD`, nạp catalog. Truyền `-Client` với đường dẫn đầy đủ tới `mariadb.exe` nếu client chưa có trong PATH. Nếu MariaDB chạy qua Compose, dùng cổng host tương ứng.

```powershell
.\scripts\seed-demo.ps1 -Database beautyshop_demo -User $env:DB_USER -Port 3306
```

5. Mở `http://127.0.0.1:8080/products`. `/products/demo-r026` có hai lựa chọn SKU, `DEMO-004` có tồn 1 và `DEMO-013` hết hàng lúc mới nạp. `DEMO10` giảm 10%, tối đa 50.000 ₫, cho đơn từ 200.000 ₫ trong 30 ngày kể từ lần nạp đầu.
6. Đăng ký một Customer để mua. Đăng ký tài khoản khác và [cấp quyền Admin](03-cap-quyen-admin.md) để xử lý đơn demo. Dashboard ở `/admin/orders` ghi tổng số đơn, số đơn đã thu tiền và số tiền của thanh toán `PAID` hợp lệ theo từng đơn.

Seeder chỉ kết nối loopback và nhận database có tên dành riêng cho demo. Chạy lại giữ dữ liệu, giá, tồn và số lượt hiện có của các bản ghi đã nạp. Muốn bắt đầu một kịch bản mới, dùng database demo trống riêng với hậu tố phân biệt.

## Backup trước demo

Dùng client MariaDB và nhập mật khẩu khi được nhắc. Chọn đường dẫn file ngoài Git.

```powershell
mariadb-dump -h 127.0.0.1 -P 3306 -u $env:DB_USER -p --single-transaction --result-file=target/beautyshop-demo-backup.sql beautyshop_demo
```

Để diễn tập khôi phục, tạo `beautyshop_demo_restore` trống, cấp quyền cho user trên đó và nhập bản dump. Dùng `source` để giữ nguyên dữ liệu UTF-8, hoặc nhập từ client SQL có hỗ trợ file.

```powershell
mariadb -h 127.0.0.1 -P 3306 -u $env:DB_USER -p beautyshop_demo_restore -e "source target/beautyshop-demo-backup.sql"
```

Đối chiếu số sản phẩm, SKU, thương hiệu, danh mục và voucher sau khi khôi phục. Nếu demo đã tải ảnh, sao lưu và khôi phục thêm thư mục upload. Giữ JAR và video demo cùng bản sao dự phòng.

## Môi trường công khai

**BLOCKED_HOST.** Chưa có host, domain hoặc MariaDB công khai đã cấp cho dự án. Cần một runtime Java 21 chạy JAR, TLS ở reverse proxy, MariaDB bền vững và volume cho ảnh SKU. Dùng profile `prod`, khai báo `DB_URL`, `DB_USER`, `DB_PASSWORD` từ secret store của host và kiểm tra HTTPS, Flyway, auth/COD cùng khả năng khôi phục trước khi chia sẻ URL.

Demo COD local dùng profile dev với giá/tồn mô phỏng. MoMo Sandbox thật cần merchant và IPN HTTPS công khai. Ollama thật cần runtime và model theo [hướng dẫn P09](05-chatbot-rag.md). Chế độ FAQ chạy độc lập với Ollama.
