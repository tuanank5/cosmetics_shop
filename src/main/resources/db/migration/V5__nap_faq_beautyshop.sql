INSERT INTO bai_viet_faq (cau_hoi, cau_tra_loi, url_nguon, ngay_cap_nhat, dang_hoat_dong)
SELECT seed.cau_hoi, seed.cau_tra_loi, seed.url_nguon, UTC_TIMESTAMP(6), TRUE
FROM (
    SELECT 'Thanh toán khi nhận hàng (COD) và phí giao hàng thế nào?' AS cau_hoi,
           'Bạn thanh toán khi nhận hàng. Phí giao hàng là 25.000 ₫. Đơn được miễn phí giao hàng khi tiền hàng sau giảm giá từ 500.000 ₫.' AS cau_tra_loi,
           '/checkout' AS url_nguon
    UNION ALL SELECT 'Đổi trả trong bao lâu?', 'Bạn có thể gửi yêu cầu đổi trả trong 7 ngày kể từ lúc giao thành công. Cửa hàng kiểm tra và duyệt yêu cầu.', '/account/returns'
    UNION ALL SELECT 'Xem giá và lượng còn hàng của sản phẩm ở đâu?', 'Mở trang chi tiết và chọn biến thể sản phẩm để xem giá, ảnh và số lượng còn mua được.', '/products'
    UNION ALL SELECT 'Thêm địa chỉ giao hàng ở đâu?', 'Mở mục Tài khoản, chọn Quản lý địa chỉ giao hàng và thêm địa chỉ người nhận.', '/account/addresses'
) seed
WHERE NOT EXISTS (SELECT 1 FROM bai_viet_faq f WHERE f.cau_hoi = seed.cau_hoi);
