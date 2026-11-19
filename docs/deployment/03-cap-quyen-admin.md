# Cấp quyền Admin

1. Người quản trị đăng ký tài khoản chuyên dùng qua `/register` và tự đặt mật khẩu riêng. Đăng nhập thử để xác nhận tài khoản và đăng xuất trước khi đổi vai trò.
2. Người vận hành kết nối MariaDB bằng tài khoản được cấp quyền cập nhật. Xác minh email và mã người dùng với người nhận quyền. Dùng email đã chuẩn hóa chữ thường.
3. Trong cùng kết nối SQL, mở transaction và xem hàng cần cấp quyền.

```sql
START TRANSACTION;
SELECT ma_nguoi_dung, email, vai_tro, kich_hoat
FROM nguoi_dung
WHERE email = '<EMAIL_DA_XAC_MINH>'
FOR UPDATE;
```

4. Sau khi xác nhận kết quả đúng tài khoản CUSTOMER đang kích hoạt, thay `<MA_NGUOI_DUNG>` bằng mã vừa đọc và chạy phần sau. Thay cả placeholder email trước khi chạy.

```sql
UPDATE nguoi_dung
SET vai_tro = 'ADMIN'
WHERE ma_nguoi_dung = <MA_NGUOI_DUNG>
  AND email = '<EMAIL_DA_XAC_MINH>'
  AND vai_tro = 'CUSTOMER'
  AND kich_hoat = TRUE;
SELECT ROW_COUNT() AS so_tai_khoan_da_cap_quyen;
```

5. Chạy `COMMIT;` nếu có đúng một hàng được cập nhật. Nếu không, chạy `ROLLBACK;`. Đăng nhập lại để phiên mới nhận quyền và mở `/admin/catalog`.

Phiên mới nhận vai trò sau khi xác thực. Khi vai trò đã đổi, phiên cũ bị kết thúc ở yêu cầu nghiệp vụ tiếp theo. Để khóa hoặc mở khóa thông thường, dùng `/admin/users`. Khóa qua trang này thu hồi các phiên cũ kể cả khi tài khoản được mở lại.
