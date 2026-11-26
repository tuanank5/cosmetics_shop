# Tiến độ rà soát lịch sử BeautyShop · Lượt 1

Ngày 26/11/2026. Chỉ đọc mã, lịch sử và bằng chứng sẵn có. Đầu ra là tài liệu tiến độ và [báo cáo Lượt 1](BEAUTYSHOP_MAIN_HISTORY_REVIEW.md).

| Hạng mục | Trạng thái | Checkpoint |
|---|---|---|
| Baseline và tích hợp Git | Đã xác minh | HEAD, origin/main và SHA GitHub đều là `38fb1ca300ce9903db033da13ee4105588ea394a`. 70 file tracked sửa, 127 file untracked, 0 staged lúc bắt đầu. Bốn SHA là tổ tiên của main qua nhánh merge PR #5. |
| Commit `075e77f` | Đã đọc | Tư vấn da, Ollama, link Header và khoảng trống view `/chat`. |
| Commit `dd62596` | Đã đọc | Giới hạn ứng viên, validation, trạng thái gửi và cấu hình opt-in. |
| Commit `f15e3ce` | Đã đọc | Chỉ sửa ba kỳ vọng vai trò trong một file test. |
| Commit `b97ec75` | Đã đọc | JSON ID, lọc thương hiệu, khôi phục view `/chat`, link Footer theo vai trò và CTA Home. |
| AI tư vấn da và đối chiếu QA-01 | Đã đối chiếu | Hai controller, hai view và hai đường gửi dữ liệu. Xem test và bằng chứng QA sẵn có, không chạy ứng dụng trong lượt này. |
| Footer và trang thông tin | Đã đối chiếu | Đọc thêm commit cha `409c6da`, nhánh logo `8fe4615`, merge và template/CSS/mapping hiện nay. |
| So sánh cũ và hiện tại | Đã đối chiếu | Quy nguyên những gì bị thay, phục hồi và còn ngoài Backlog. |
| Chất lượng mã và comment | Đã đối chiếu | Phân biệt mã dư, comment mô tả hợp đồng và rủi ro cần test. |
| Đối chiếu 13 Work Package | Đã đối chiếu | Sổ tiến độ, manifest, diff G1–G6 và diff chưa commit. |
| Đề xuất Lượt 2 | Đã ghi | 12 đề xuất có phạm vi, rủi ro, ưu tiên, tiêu chí, test và Work Package liên quan. Tất cả ở trạng thái PENDING. |
| Hoàn thành báo cáo | Hoàn tất Lượt 1 | Đã kiểm đủ 5 bảng, 13 Work Package, liên kết nội bộ và `git diff --check`. HEAD và index giữ nguyên. Diff tracked khớp trước/sau với Git blob hash `88eb904f5bf7f4e5165addc13f908f5f6afcba80`. Số file untracked tăng từ 127 lên 129 do hai tài liệu mới. |

Chờ owner đọc, bổ sung yêu cầu và phê duyệt phạm vi Lượt 2. Bản sửa QA-01 và trạng thái các Work Package được bảo toàn.
