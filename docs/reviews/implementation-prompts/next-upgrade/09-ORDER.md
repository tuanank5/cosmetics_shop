# 09 · ORDER · Danh sách và mua lại vào giỏ

**Phase 4 · P1/Vừa**. Khi owner giao gói này, hãy dùng [sổ thực thi](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) để kiểm tra trạng thái mới nhất. Đọc [quy ước dự án](../../../../AGENTS.md), [kế hoạch](../../BEAUTYSHOP_NEXT_UPGRADE_PLAN.md), [phân tích và các quyết định còn mở](../../BEAUTYSHOP_CHANGE_IMPACT_ANALYSIS.md) và [DECISIONS](../../../DECISIONS.md) trước khi sửa.

## Mục tiêu và điều kiện bắt đầu

Hoàn thiện control còn thiếu trên danh sách Customer và mua lại bằng giỏ hiện tại, giữ nguyên snapshot.

**Điều kiện tiên quyết.** Cần BASE và hợp đồng AUTH theo chủ. Có thể làm lọc/mua lại độc lập RETURN; kiểm tích hợp detail và checkout sau PAYMENT/VOUCHER. Trạng thái tại lúc soạn prompt: **Chờ phụ thuộc**. Ghi ID hoặc biên bản owner duyệt vào sổ trước khi dùng quy tắc còn mở. Chỉ thực hiện phần độc lập đã được giao nếu một gate của gói còn BLOCKED.

## Phần đang chạy và phạm vi ảnh hưởng

Admin đủ tìm/lọc/ngày/sort; Customer có pager; chưa có mua lại. DonHang_Controller/Repository, QuyenDonHang_Service, GioHang_Service, order/list/customer-detail. Giữ pager; thêm control thiếu của Customer và POST mua lại. Rủi ro mua nhầm giá/xuyên chủ. Giữ số mục/trang và layout đã nâng cấp, mua lại không checkout.

Điểm vào để đối chiếu mã hoặc bằng chứng: [GioHang_Service.java](../../../../src/main/java/com/beautyshop/service/GioHang_Service.java) · [DonHang_Controller.java](../../../../src/main/java/com/beautyshop/controller/DonHang_Controller.java) · [QuyenDonHang_Service.java](../../../../src/main/java/com/beautyshop/service/QuyenDonHang_Service.java) · [order](../../../../src/main/resources/templates/order)

## Các bước triển khai và yêu cầu nghiệp vụ

1. Dùng bảng tám tiêu chí hiện trạng. Giữ q/trạng thái/ngày/sort/pager Admin; bổ sung vào truy vấn theo chủ và count của Customer đúng control còn thiếu. Giữ cỡ trang, bộ đếm, empty, breadcrumb và nguồn detail.
2. Thêm thao tác mua lại có chủ đích qua POST/CSRF trên đơn thuộc Customer. Dùng GioHang_Service và khóa giỏ/SKU theo thứ tự. Kiểm SKU, cha, hãng, danh mục còn hoạt động, giá hiện tại và số lượng đã có. Cộng tối đa lượng còn được mua, phân biệt đủ/giảm/bỏ qua theo dòng, có lý do. Chống gửi lặp cùng thao tác.
3. Giữ snapshot đơn cũ, địa chỉ, hàng, giá và attempt. Chỉ cập nhật giỏ, không tạo đơn/thu tiền hay áp lại mã cũ. Hợp nhất thay đổi order detail với PAYMENT ở vùng cần hiển thị và test quay lại đúng dòng.

## Bảo toàn và ngoại lệ phạm vi

Giữ D41/D42, CSRF, quyền theo vai trò và chủ sở hữu, snapshot và tính tiền server-side ở luồng có liên quan. Dùng SSR/PRG và component hiện có cho màn bị tác động. Kho tiếp tục theo biến thể; FEFO, kho nhiều lô, AI-LIVE và các mục backlog chỉ được mở bằng quyết định riêng. Giữ các migration đã áp dụng và bằng chứng của 13 WP. Mọi contract thay đổi phải nêu consumer và cách đọc dữ liệu cũ.

## Kiểm thử và tiêu chí nghiệm thu

| Loại | Ca cần kiểm và bằng chứng |
|---|---|
| Unit Test | SKU còn hợp lệ, lượng có thể thêm |
| Integration Test | khóa giỏ/SKU, hai POST, thay giá và ngừng hãng/danh mục |
| Browser Test / E2E | Back đúng dòng, mua lại đơn hỗn hợp và thấy lý do |
| Regression Test | snapshot, merge và timeline. |

**Gate của ORDER.** Đơn cũ, tiền và attempt không đổi; giỏ dùng giá/khả dụng hiện tại, báo theo dòng. Ghi môi trường, revision, fixture, phép kiểm và kết quả thực tế. Phân biệt mock, E2E thật, manual UAT, skip và chưa chạy. Với thay đổi code, chạy JDK 21 `mvn test`, `mvn -DskipTests package` và integration/dev MariaDB hoặc Mongo fixture theo vùng ảnh hưởng.

## Git, rollback và bàn giao

- Dùng branch đề xuất `feat/order-reorder` từ baseline đã xác nhận (với BASE dùng SHA ứng viên được duyệt), tích hợp các dependency đã review. Rà working tree trước khi sửa để giữ phần việc đang có.
- Chia commit và rollback theo phạm vi gói. Bộ lọc → service giỏ → chi tiết. Ẩn hành động mới, giữ dữ liệu giỏ hợp lệ.
- Kiểm `git diff --check`, phạm vi tệp và dữ liệu nhạy cảm; push branch/PR theo quyền owner giao. Gắn CI đúng SHA, review rồi merge khi được duyệt. Gặp lỗi gate thì giữ kết quả và retest trên revision sửa.
- Cập nhật [sổ tiến độ](../../BEAUTYSHOP_NEXT_UPGRADE_PROGRESS.md) với owner, ngày, SHA, gate, test, PR/CI và blocker. Ghi báo cáo ở `docs/reviews/ORDER-ket-qua.md`, bằng chứng đã che ở `docs/qa/ORDER/`, ảnh chọn lọc ở `docs/ui/screenshots/ORDER/`. Cập nhật tài liệu thiết kế, kiểm thử, vận hành và quyết định đã duyệt nếu contract thay đổi.
