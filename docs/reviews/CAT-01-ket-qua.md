# CAT-01 · Vòng đời catalog

Baseline `main` `38fb1ca300ce9903db033da13ee4105588ea394a` trên `origin/main`, cộng working diff CART-01 đã đạt G7. G3 đã tích hợp. Giữ nguyên thay đổi CART-01 đang chờ commit.

## Thay đổi

- `AdminDanhMucSanPhamThuongHieu_Controller.java` nhận `POST /admin/catalog/{section}/{id}/status` với `active=true|false`, dùng allowlist section và ROLE_ADMIN/CSRF hiện có. Hai URL POST `categories/{id}/delete` và `brands/{id}/delete` được gỡ khỏi mapping. URL `products/{id}/delete` giữ tương thích bằng mapping tường minh với trạng thái false.
- `DanhMucSanPhamThuongHieu_Service.java` và `MucDanhMucSanPhamThuongHieu.java` gán cờ mong muốn cho bản ghi đã khóa trong transaction. Edit và thao tác nhanh cùng sử dụng khóa hàng để tránh ghi đè từ hai lần đọc đồng thời. Không gọi delete hoặc đổi cờ của bản ghi con.
- `DanhMucSanPhamThuongHieu_Repository.java` chỉ cung cấp thao tác đọc, lưu và khóa đang dùng, bỏ API hard delete của JpaRepository. Giữ schema V1, FK, UNIQUE slug, giá/ảnh SKU và snapshot đơn. Xóa voucher và ngừng SKU theo route hiện có.
- `admin/catalog/list.html` thay form xóa bằng hai hành động **Ngừng hoạt động** và **Kích hoạt lại** với giá trị đích rõ ràng. Form sửa giữ checkbox hiện có. D21, README, sổ tiến độ và roadmap ghi chính sách hiện hành. Báo cáo và phiếu QA lịch sử được giữ.
- `AdminCatalogSecurityTest.java` và `DanhMucSanPhamThuongHieu_MariaDb_Test.java` kiểm quyền, CSRF, URL cũ, gán lặp, ID thiếu, ngừng/kích hoạt lại, FK và snapshot. Thêm fixture MariaDB và Chrome.

## Hợp đồng bàn giao

WISH-01 và DTO-01 giữ tiêu chí công khai hiện tại gồm danh mục, thương hiệu, sản phẩm và SKU cùng hoạt động. Bật lại bản ghi cha chỉ làm những bản ghi con vẫn đang hoạt động và đủ điều kiện xuất hiện lại. QA-01 có thể dùng fixture G8 để kiểm URL cũ cho cả hai loại cha. Các thống kê theo quan hệ catalog hiện hành tiếp tục dùng tên và nhóm hiện hành.

Nếu cần quay lui phần UI, dùng checkbox trạng thái trong form sửa và giữ hai URL xóa cha bị vô hiệu hóa. Không có thay đổi schema cần quay lui.

## Nghiệm thu G8

- Temurin 21.0.13.1, Maven 3.9.15. `mvn -o test` đạt **238/304**, 66 skip theo profile, 0 lỗi. `mvn -o -DskipTests package` thành công. [Log Maven](../qa/CAT-01/maven-results.txt)
- `run-fixture.ps1 -Integration` đạt **23/23** trên schema MariaDB trống. Request cũ trả 404 cho cả danh mục/thương hiệu có sản phẩm và chưa dùng. Gán lặp cùng giá trị giữ đúng trạng thái. Tắt mỗi loại cha ẩn PDP và kết quả catalog. Bật lại cho hiện sản phẩm đủ điều kiện, với SKU tắt và sản phẩm tắt tiếp tục ẩn. Đơn fixture đọc được tên/SKU snapshot trong khi cha ngừng hoạt động và FK giữ đúng ID. SKU, voucher và COD đều đạt hồi quy. [Log tích hợp](../qa/CAT-01/integration-results.txt)
- `run-fixture.ps1` chạy Chrome 158 ở 390 CSS px, tắt JavaScript. Ba nhóm luồng đạt, gồm Tab/Enter cho ngừng/kích hoạt lại của hai loại cha và gửi trực tiếp URL cũ với CSRF của Admin fixture. Kiểm trang vừa chiều ngang và thao tác hiển thị đúng trạng thái. [Kết quả Chrome](../qa/CAT-01/browser-results.json) · [Ảnh 390px](../ui/screenshots/CAT-01/categories-390-reactivate-nojs.webp)
- `git diff --check` đạt, staged diff trống. Kiểm Git xác nhận migration và lớp voucher không đổi. **G8 đạt trên fixture.** Chưa chạy trên dữ liệu vận hành hoặc thiết bị vật lý.
