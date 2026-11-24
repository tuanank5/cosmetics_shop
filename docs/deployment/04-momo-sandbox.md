# MoMo P07

**Sandbox thật: BLOCKED_EXTERNAL.** Cần merchant Sandbox, credentials thử nghiệm và endpoint HTTPS công khai nhận được IPN. COD hoạt động với cấu hình mặc định `MOMO_ENABLED=false`.

## Mô phỏng dev/test

Chạy với profile `dev` hoặc `test` và cấu hình database thử nghiệm. Profile có `prod` sẽ vô hiệu hóa mock.

```powershell
$env:MOMO_ENABLED = 'true'
$env:MOMO_MODE = 'mock'
.\scripts\p00.ps1 -Task run
```

Đăng nhập Customer có giỏ và địa chỉ. Tại `/checkout` chọn **MoMo · MÔ PHỎNG - KHÔNG THU TIỀN**. Trang mô phỏng cho phép chọn thành công, từ chối hoặc cổng hết hạn. Người quản trị có thao tác **Đối soát MoMo** trong chi tiết đơn. Chế độ mock tạo khóa thử nghiệm và kết quả provider trong bộ nhớ tiến trình. Chạy hết kịch bản trong cùng lần khởi động.

## Điều kiện chạy Sandbox

- Cấu hình `MOMO_ENABLED=true`, `MOMO_MODE=sandbox`, `MOMO_PARTNER_CODE`, `MOMO_ACCESS_KEY`, `MOMO_SECRET_KEY` từ môi trường hoặc secret store.
- Cấu hình `MOMO_REDIRECT_URL` trỏ đến `/payment/momo/return` và `MOMO_IPN_URL` trỏ đến `/payment/momo/ipn`. Cả hai dùng HTTPS. Xác nhận MoMo truy cập được IPN từ Internet trước khi nghiệm thu.
- Adapter gọi host cố định `test-payment.momo.vn` với `captureWallet`, `autoCapture=true`. Request tạo/truy vấn có HMAC-SHA256. Kết quả query đi qua HTTPS và được đối chiếu merchant, request, order và số tiền. IPN có kiểm HMAC và trả HTTP 204 khi ghi nhận hoặc nhận lại kết quả hợp lệ.
- IPN giới hạn 60 yêu cầu/giây mỗi tiến trình và body tối đa 8 KiB. Chỉ `POST /payment/momo/ipn` được miễn CSRF khi adapter đang bật. Browser return chỉ hiển thị hướng dẫn xem đơn.

## Retry, đối soát và hoàn tiền

1. Khi tạo đơn, giữ tồn và lượt voucher cùng transaction trong cửa sổ thử lại 15 phút. Gọi gateway sau commit.
2. Retry khi chưa biết kết quả dùng cùng mã yêu cầu và mã đơn provider. Sau khi lần trước bị từ chối xác minh và vẫn còn thời gian giữ, lần mới có mã riêng.
3. Sau 15 phút, tác vụ định kỳ đối soát theo lô nhỏ. Admin cũng có thể đối soát từ chi tiết đơn. Thời gian hết hạn tại Shop, timeout, lỗi mạng và result code chưa xác định không tự giải phóng giữ hàng. Nếu cổng xác nhận hết hạn hoặc thất bại cuối cùng sau cửa sổ giữ, giải phóng tồn, lượt voucher rồi đánh dấu đơn `EXPIRED`.
4. Khi phát sinh thành công muộn cho giao dịch đã hết hiệu lực, hệ thống ghi `REFUND_PENDING` và hiển thị cảnh báo trên đơn. Người vận hành đối chiếu giao dịch và thực hiện hoàn tiền qua quy trình merchant. Luồng P07 không gọi API hoàn tiền tự động.

[MoMo One-Time Payments](https://developers.momo.vn/v3/docs/payment/api/wallet/onetime/) · [Query](https://developers.momo.vn/v3/docs/payment/api/payment-api/query/) · [IPN](https://developers.momo.vn/v3/vi/docs/payment/api/result-handling/notification/) · [Result codes](https://developers.momo.vn/v3/docs/payment/api/result-handling/resultcode/)
