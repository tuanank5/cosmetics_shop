UPDATE nguoi_dung SET ngay_tao = UTC_TIMESTAMP() - INTERVAL 1 DAY WHERE ma_nguoi_dung = 2;
INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_khi_dat,
    tam_tinh, phi_van_chuyen, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan, ngay_tao, ngay_giao_thanh_cong) VALUES
('BS-QA-PAID', 2, 'Fixture', '000', 'Fixture address', 100000, 25000, 125000, 'DELIVERED', 'COD', UTC_TIMESTAMP() - INTERVAL 2 DAY, UTC_TIMESTAMP() - INTERVAL 1 DAY),
('BS-QA-CANCELLED', 2, 'Fixture', '000', 'Fixture address', 100000, 25000, 125000, 'CANCELLED', 'COD', UTC_TIMESTAMP() - INTERVAL 12 DAY, NULL);
INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat, ten_bien_the_khi_dat, don_gia, so_luong)
SELECT ma_don_hang, 1, 'FIXTURE-A', 'Fixture A', 'Fixture A', 100000, 1 FROM don_hang WHERE ma_don_hang_hien_thi = 'BS-QA-PAID';
INSERT INTO lan_thanh_toan (ma_don_hang, ma_yeu_cau, cong_thanh_toan, so_tien, trang_thai, ngay_thanh_toan)
SELECT ma_don_hang, CONCAT('COD-', ma_don_hang), 'COD', tong_thanh_toan, 'PAID', ngay_giao_thanh_cong
FROM don_hang WHERE ma_don_hang_hien_thi = 'BS-QA-PAID';
INSERT INTO yeu_cau_doi_tra (ma_don_hang, ma_nguoi_dung, trang_thai, ly_do)
SELECT ma_don_hang, 2, 'PENDING', 'Fixture return' FROM don_hang WHERE ma_don_hang_hien_thi = 'BS-QA-PAID';
