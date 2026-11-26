# ARCH-01 · Đổi package ánh xạ dữ liệu

Checkpoint ngày 26/11/2026 trên main, HEAD `6368b6723d425c23d8901a5ebc69bbaea3ac09b4` cộng working diff SEC-01/DATA-01 đã đạt G1/G2 và diff ARCH-01. Index Git ban đầu rỗng. Bản so sánh trước rename giữ đúng source đang chạy của hai gói trước trong `target/arch-01-before/`.

## Thay đổi

- Chuyển 11 lớp từ `src/main/java/com/beautyshop/entity/` sang `src/main/java/com/beautyshop/model/`. Tám lớp trong kế hoạch gồm BienTheSanPham, DanhGia_Document, DanhMuc, DiaChiNguoiDung, MucDanhMucSanPhamThuongHieu, NguoiDung, SanPham, ThuongHieu. Ba lớp từ AUTH-02 là DanhTinhOAuth, NhatKyQuanTri và YeuCauDatLaiMatKhau.
- Sửa package/import/tham chiếu Java trong 59 file gồm 11 lớp đã chuyển. Đối chiếu 305 file trong src với ảnh chụp trước rename. Toàn bộ khác biệt Java chỉ là tên package/FQCN và đường dẫn. Phần còn lại trong src giống từng byte. Migration, annotation, JPQL, tên bảng/cột/collection, ID, revision và index giữ nguyên. Scan tự động dưới `com.beautyshop` vẫn bao cả hai lớp mapping JPA/Mongo theo package mới.
- Thêm `ReviewBsonCompatibilityTest.java`, script fixture và bằng chứng trong `docs/qa/ARCH-01/`. Cập nhật AGENTS, kiến trúc, ERD, tiến độ và roadmap. Danh sách file Java đầy đủ nằm trong `package-inventory.json`.
- Tham chiếu package cũ còn lại trong src là chuỗi fixture để kiểm discriminator đọc ngược. Không thêm mapper, alias hoặc đường bulk rewrite BSON.

## Kiểm tương thích hai chiều

Temurin JDK 21.0.13.1, Maven 3.9.15, profile dev. MariaDB 12.2.2 và MongoDB 6.0 đều chạy loopback trên thư mục UUID trong target. Fixture MariaDB dùng schema rỗng, Flyway V1–V7 và JPA validate. Mongo dùng BSON tổng hợp, namespace nguồn và namespace đích khác nhau.

1. Build cũ ghi bằng repository một document có `_class=com.beautyshop.entity.DanhGia_Document` và tạo một document không có discriminator. Cả hai đọc được qua repository lẫn MongoTemplate. Xuất mẫu BSON dưới dạng Extended JSON và restore sang `arch_01_copy`.
2. Build mới đọc cả hai mẫu trên bản copy qua repository và MongoTemplate, sửa sao và xác nhận giữ ID/nội dung/ngày tạo, revision tăng từ 0 lên 1. Ghi document mới có `_class=com.beautyshop.model.DanhGia_Document` và xác nhận revision 0.
3. Chạy lại build cũ. Cả hai document đã sửa và document mới đều đọc được bằng repository/MongoTemplate. Build cũ sửa được document mới và revision tăng lên 1. Mẫu roundtrip xuất sau lượt ghi cuối, nên các discriminator có thể phản ánh binary ghi sau cùng.

Cả ba bước pass khi chạy Spring Data mapping có kiểu. Kết quả tương thích áp dụng cho các đường đọc typed hiện có của review, không suy rộng ra đường đọc kiểu Object hoặc một client ngoài ứng dụng chưa kiểm. Phương án rollback trong cửa sổ thử nghiệm là dùng binary trước ARCH-01 và tiếp tục dùng MongoDB. Giữ dữ liệu SQL lịch sử và Mongo nguyên vẹn.

## G3 và kiểm thử

- Lượt JPA đầu phát hiện ba class AUTH-02 cũ tồn tại trong target/classes dưới package entity, gây NoClassDefFoundError khi Hibernate scan. Đã di chuyển đúng thư mục class sinh ra sang thư mục ARCH-01 riêng dưới target rồi chạy lại. Không cần thay đổi source hoặc cấu hình scan để chữa lỗi build cũ.
- Các lệnh chạy: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File docs/qa/ARCH-01/run-fixture.ps1 -Baseline`, cùng lệnh bỏ `-Baseline` cho G3, `mvn -o test`, `mvn -o -DskipTests package`, `git diff --check`.
- Script G3 chạy bằng bản snapshot `src` và `pom.xml` cũ đặt trong `target/arch-01-before/` gồm cùng lớp test tương thích nhưng import cũ. Dùng snapshot trước rename này khi lặp lại kiểm hai binary. Bản ghi đích riêng và không dựa vào dữ liệu owner.

| Kiểm chứng | Kết quả |
|---|---|
| Ba lượt Mongo old-write / new-roundtrip / old-read | **3/3 đạt**, không lỗi hay skip |
| Tích hợp G3 | **48/49 đạt**, 0 lỗi. Một ca tạo mẫu backup DATA-01 chỉ chạy khi có cờ riêng nên skip |
| Flyway và JPA | V1–V7 trên schema rỗng, JPA validate, hai ca nâng V5/V6 lên V7 và hồi quy JPQL/context qua các suite MariaDB đều đạt |
| Toàn bộ Maven test JDK 21 | **229/288 đạt**, 59 skip theo điều kiện môi trường, 0 lỗi |
| Package và diff | Package exit 0. JAR chứa đủ 11 lớp model, không có class dưới package entity. Diff không lỗi whitespace |

**G3 đạt. ARCH-01 DONE** trên checkpoint HEAD + diff cùng hai gói trước.

Các dịch vụ ngoài DB vẫn theo fixture của suite, gồm OAuth callback trên provider mô phỏng, OTP/SMTP/Redis mock nơi suite yêu cầu và MoMo mock. Không kiểm trên dữ liệu thật. Gói chỉ đổi package, không thay giao diện. Bước kế tiếp theo lịch là `04-NAV-01.md` khi được giao riêng. Chưa commit/push.

Bằng chứng trong `docs/qa/ARCH-01/`: package-inventory.json, source-compare.txt, legacy-fixture.jsonl, roundtrip-fixture.jsonl và log đã lọc theo lượt.
