# SKIN-UI-01 · Chuẩn hóa giao diện và mã tư vấn da

Phạm vi owner giao tiếp từ [rà soát lịch sử](BEAUTYSHOP_MAIN_HISTORY_REVIEW.md). Baseline là `38fb1ca` cộng working diff đã có. Tiêu chí chỉnh sửa dựa vào việc tái sử dụng thành phần chung, tham chiếu thực tế và quy ước BeautyShop.

## Thay đổi

- `templates/chat/TuVanDa.html` dùng head, bố cục panel, form, lỗi field, thông báo và nút có sẵn. Link bỏ qua trỏ đúng `main`. Trình duyệt áp dụng cùng mức 1.000 ký tự với backend. JS dùng chung của storefront xử lý loading và Back. Kết quả giữ dấu xuống dòng và được escape như trước. Ba tập option và tên trường POST được giữ.
- `static/css/storefront.css` bổ sung quy tắc cục bộ để giữ xuống dòng và breakpoint trang da. Bỏ `static/css/tuvanda.css` cùng các quy tắc màu/body riêng, dọn các comment mô tả lại markup.
- `templates/fragments/footer.html` dọn comment lặp và khoảng trắng. Giữ nguyên link, nhãn, phân vai và dialog xác nhận dùng chung.
- Bỏ `templates/fragments/ai_tu_van.html`, lớp `ai/controller/TuVanDaController` và `ai/dto/TuVanDaResponse` rỗng sau khi tìm tham chiếu. Dọn overload không phân trang trong `BienTheSanPham_Repository`, query `findByLoaiDaPhuHop` trong `SanPham_Repository` và import thừa trong `TuVanDaService`. Cả hai truy vấn SKU đang được gọi và projection Admin được giữ.
- `TuVanDaControllerTest` bổ sung kiểm đích skip link, giới hạn chiều dài và mối liên kết giữa field sai và thông báo. Bản sửa QA-01 ở `/chat`, chính sách gửi dữ liệu tới Ollama, schema và thuật toán tư vấn giữ theo baseline. SHOP-12 vẫn thuộc backlog.

## Kiểm chứng

- JDK 21 và Maven 3.9.15 offline trên bản sao nguồn trong `target/skin-ui-01-verify` để kiểm JAR sạch. `mvn -Dtest=TuVanDaControllerTest,TuVanDaServiceTest,TuVanDaRepositoryTest,TuVan_ControllerTest,GiaoDienPagesTest test` đạt 83/83. `mvn test` có 241 đạt, 71 skip và 0 lỗi. `mvn -DskipTests package` đạt. Kiểm danh sách entry của JAR xác nhận tài nguyên/lớp đã bỏ không được đóng gói. [Tập trung](../qa/SKIN-UI-01/focused.txt) · [Test](../qa/SKIN-UI-01/test.txt) · [Package](../qa/SKIN-UI-01/package.txt)
- Chrome headless dùng JAR mới và MariaDB fixture riêng, Ollama tắt theo chế độ kiểm thử. Form gửi bằng bàn phím ở 390/768/1366/1920 px với JS bật và tắt. Cả 8 tổ hợp đạt, gồm loading khi JS bật, kết quả thiếu SKU phù hợp và bố cục vừa viewport. Back ở 390 px khôi phục nút gửi. Khi có SKU và provider tắt, thông báo dùng `role=alert`. [Chrome](../qa/SKIN-UI-01/browser-results.json) · [Mobile](../ui/screenshots/SKIN-UI-01/form-390.webp) · [Desktop](../ui/screenshots/SKIN-UI-01/service-error-1366.webp)
- `run-data.ps1` chạy trên MariaDB/MongoDB riêng, 312 đạt, 2 skip, 0 lỗi. FAQ đạt 2/2 và RAG mock đạt 3/3. BSON chạy riêng 1/1 và phục hồi khớp checksum SQL, BSON, unique index và ảnh fixture. `git diff --check` đạt. Kiểm tra model Ollama thật vẫn chưa có bằng chứng. [Tích hợp](../qa/SKIN-UI-01/data/integration.txt) · [BSON](../qa/SKIN-UI-01/data/bson.txt) · [Phục hồi](../qa/SKIN-UI-01/data/restore.txt)
