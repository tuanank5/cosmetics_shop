# ADMIN-UX-BIZ-01 · Checkpoint

## Chạy lại trên fixture

Các lệnh dùng source copy, Maven cache, ứng dụng và datadir mới dưới `target/admin-ux-biz-01/`. Bản trước đã được chụp từ working tree ban đầu vào `target/admin-ux-biz-01/before/`.

```powershell
$qa = 'docs/qa/ADMIN-UX-BIZ-01'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$qa/build.ps1"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$qa/run-fixture.ps1" -Phase integration
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$qa/run-fixture.ps1" -Phase http -Revision before -Browser
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$qa/run-fixture.ps1" -Phase http -Revision after -Browser
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$qa/run-fixture.ps1" -Phase http -Revision after -ContactFixture
git diff --check
```

Với Chrome có mạng, kiểm tra pin tại địa chỉ hiển thị, điều hướng link dự phòng, thẻ liên hệ chính thức và đọc tên cột/trạng thái bằng screen reader. Test Chrome headless dùng fixture riêng và chặn truy cập mạng ngoài. Bằng chứng accessibility tự động lấy từ cây accessibility của Chrome và tương tác phím thật qua DevTools.

## Thiết kế đề xuất cho tồn theo lô

| Bảng | Dữ liệu và bất biến |
|---|---|
| `lo_hang` | PK `ma_lo_hang`, FK `ma_bien_the`, `ma_lo_nha_san_xuat`, `ngay_san_xuat` tùy chọn, `han_su_dung`, `ngay_nhap`, `so_luong_nhap`, `so_luong_trong_kho`, `so_luong_tam_giu`, `trang_thai`. Unique theo SKU/mã lô, kiểm dương/không âm và giữ không quá tồn, NSX trước hoặc bằng HSD. Index FEFO theo SKU, trạng thái, HSD, ngày nhập và ID. |
| `phan_bo_lo_hang` | FK bất biến tới dòng đơn và lô. Một cặp dòng đơn–lô có một lượng phân bổ dương, trạng thái `HELD`, `CONSUMED`, `RELEASED` hoặc `RESTORED` với mốc thời gian. Ràng buộc cặp SKU theo khóa ngoại ghép hoặc kiểm transaction trước insert. Giữ lịch sử sau khi hủy và giao. |
| `bien_dong_lo_hang` | Nhật ký nhập và điều chỉnh, số lượng thay đổi, lý do, nhân viên, ngày ghi, tham chiếu phân bổ hoặc yêu cầu điều chỉnh có khóa idempotency. |

Admin dùng một trang từ chi tiết SKU để xem và thêm lô, kiểm ngày nhập/NSX/HSD và phân biệt trạng thái đang dùng, sắp hết hạn và đã hết hạn. Chốt ngưỡng sắp hết hạn và thời điểm đơn vị bán được tính là hết hạn theo ngày Việt Nam. Tồn khả dụng khi bán lấy từ lô đủ điều kiện; hiển thị số đang giữ và số không khả dụng riêng. Mã lô, HSD và dòng phân bổ đã dùng được lưu lịch sử. Điều chỉnh chỉ qua nhật ký dưới khóa SKU/lô, kiểm số còn sau thay đổi vẫn đủ cho lượng giữ, và duy trì lô đã có giao dịch.

### Luồng giao dịch

1. Giữ thứ tự khóa đã xác minh: checkout khóa giỏ rồi SKU tăng dần; cập nhật đơn hiện hữu khóa đơn trước SKU. Sau SKU mới khóa các lô theo thứ tự FEFO `han_su_dung, ngay_nhap, ma_lo_hang`. Nhập/điều chỉnh/đổi chế độ kho cũng khóa SKU trước lô.
2. Chọn lô còn hạn và đủ điều kiện theo ngày Việt Nam. Chia một dòng đơn trên nhiều lô trong cùng transaction và rollback khi tổng khả dụng thiếu. Ghi dòng đơn trước phân bổ vì FK. Một giao dịch tạo nhiều phân bổ chỉ trừ/giữ đúng một lần. Giữ hợp đồng voucher và tiền hiện hành.
3. COD ghi xuất lô cùng lần trừ SKU, chỉ hoàn chính xác lượng đã xuất về đúng lô khi hủy hợp lệ. Lô đã hết hạn khi hàng quay lại vẫn giữ dấu vết và cách ly khỏi lượng bán được. Đã giao chuyển trạng thái fulfillment, không cộng tồn. Đổi trả chỉ nhập kho khi có kiểm định và biên nhận được phê duyệt.
4. MoMo giữ song song ở SKU và phân bổ lô trong transaction. Callback hợp lệ chốt lượng ở đúng các lô đã giữ. Giá trị thất bại/hết hạn chỉ trả giữ sau đối soát theo quy trình hiện hành. Nếu HSD vượt ngưỡng trong thời gian MoMo còn bất định, phải chốt cách xử lý thanh toán muộn và cách ly tồn. Khi không đủ điều kiện xuất, dùng luồng hoàn tiền qua đối soát, không phát hành sang lô khác một cách âm thầm.
5. Dashboard, catalog công khai, PDP, giỏ, wishlist và AI phải dùng cùng định nghĩa tồn bán được. Tách lượng vật lý, đang giữ, hết hạn/cách ly và bán được. Đơn legacy hiện không thể suy ra mã lô đã xuất.

### Chuyển dữ liệu và phát hành

- Tạo migration kế tiếp (dự kiến V8 nếu lịch sử vẫn dừng ở V7) thêm bảng và chế độ tồn theo từng SKU. Giữ checksum V1–V7. Test cả schema trống và fixture nâng cấp có đơn, voucher và reservation đã tồn tại. Kiểm tra FK, unique, check, index và khả năng đọc ứng dụng trước khi áp dụng trên dữ liệu thật.
- SKU legacy tiếp tục dùng tồn tổng. Khi kiểm kê đủ nguồn gốc và số lượng lô, khóa SKU khi ghi nhận số đầu kỳ. Đối chiếu tổng lô với tồn SKU, hoàn tất các đơn COD còn có thể hủy và reservation MoMo legacy trước khi chuyển, hoặc xây dựng quy trình xử lý chênh lệch và hoàn tồn legacy được nghiệp vụ duyệt. Metadata minh họa không được sử dụng làm nguồn kê khai.
- Rollback tạm dừng ghi mới theo SKU. Giữ bảng và lịch sử phân bổ. Đối soát hết giữ hàng/đơn có thể hủy, xuất báo cáo vật lý và cách ly hàng hết hạn trước khi chuyển nguồn tồn. Tránh đổi cờ chế độ khi còn đơn đang ghi theo lô.
- Gate test ngoài nghiệp vụ hiện có gồm nhiều lô, trùng mã lô, hết hạn theo giờ Việt Nam, FEFO và hòa thứ tự, chia lô, nhiều checkout tranh cùng lô, điều chỉnh underflow, rollback toàn transaction, hoàn COD đúng lô, MoMo pending/success/failed/late/replay, giữ voucher, truy vết đơn cũ và bảo toàn số liệu legacy.

[Kết quả triển khai và bằng chứng](ADMIN-UX-BIZ-01-ket-qua.md)
