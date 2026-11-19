# Dữ liệu demo — 30–50 mỹ phẩm từ thông tin công khai thực tế

**Chưa có bộ seed đã kiểm chứng** trong scaffold. Để tránh bịa thành phần/giá/hình và rủi ro bản quyền, nhóm cần chuẩn bị dữ liệu từ website chính thức của thương hiệu/nhà bán có quyền phù hợp, ghi nguồn và ngày kiểm tra từng sản phẩm.

## Tiêu chí dữ liệu

- Mục tiêu 40 sản phẩm: 8–10 thương hiệu, 4–6 danh mục; mỗi sản phẩm 1–3 SKU khi thực tế có màu/dung tích.
- Bắt buộc mỗi sản phẩm có: tên, brand, category, tên SKU/size, nguồn, ngày kiểm tra, mô tả, URL ảnh nếu được phép, giá VND **demo** (giá chỉ mang tính mô phỏng, không được ghi là giá chính thức nếu không kiểm chứng), thành phần/loại da có căn cứ.
- Không scrape ảnh từ website không cho phép. Ảnh do nhóm tạo phải có tỷ lệ kích thước thống nhất, không làm giả nhãn/khẳng định sản phẩm thật.
- Tránh tuyên bố điều trị/y tế không được xác minh. Không đưa dữ liệu người mua thật vào seed.

## Quy trình

1. Thu thập vào `product-research.csv` (template chưa xác minh).
2. Điền `source_url`, `reviewed_at`, `image_license`, reviewer.
3. Chủ sản phẩm module A kiểm tra; D kiểm tra ảnh và metadata RAG.
4. Sau khi xác minh, xuất `db/migration/Vxxx__seed_demo.sql` hoặc code seeder dev theo commit riêng; production không tự seed dữ liệu demo.

CSV `verified=no` nghĩa là **không được coi là sản phẩm thực tế đã kiểm chứng**.

## Kế hoạch dữ liệu thực tế (v4)
- 4 thành viên chia 10–15 SKU/người theo `docs/03-roadmap-and-team.md`. Owner A/D lead nguồn và ảnh; B/C kiểm theo nghiệp vụ.
- Không trộn `giá tham khảo` với giá bán tại Shop: price trong seed là **giá demo**, cần ghi rõ.
- Ghi URL nguồn hãng cho tên/INCI/công dụng; quyền dùng ảnh phải rõ. Nếu không có quyền, dùng ảnh tự chụp hoặc placeholder do nhóm tạo. Không tải lại ảnh có watermark.
- Mẫu CSV hiện mới có header và chưa xác minh sản phẩm; không coi là seed hoàn thành.
