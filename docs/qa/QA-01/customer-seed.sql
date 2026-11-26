INSERT INTO don_hang (ma_don_hang_hien_thi, ma_nguoi_dung, ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_khi_dat,
    tam_tinh, tong_thanh_toan, trang_thai, phuong_thuc_thanh_toan, ngay_giao_thanh_cong) VALUES
('BS-QA-OWN', 1, 'Fixture Buyer', '000', 'Fixture order snapshot', 100000, 100000, 'DELIVERED', 'COD', UTC_TIMESTAMP()),
('BS-QA-OTHER', 2, 'Fixture', '000', 'Other fixture snapshot', 100000, 100000, 'DELIVERED', 'COD', UTC_TIMESTAMP());
INSERT INTO chi_tiet_don_hang (ma_don_hang, ma_bien_the, sku_khi_dat, ten_san_pham_khi_dat, ten_bien_the_khi_dat, don_gia, so_luong)
VALUES (1, 1, 'FIXTURE-1', 'Fixture Serum', '30 ml', 100000, 1);
INSERT INTO yeu_cau_doi_tra (ma_don_hang, ma_nguoi_dung, trang_thai, ly_do)
VALUES (1, 1, 'PENDING', 'Fixture return'), (2, 2, 'PENDING', 'Other fixture return');
INSERT INTO chi_tiet_doi_tra (ma_yeu_cau_doi_tra, ma_chi_tiet_don_hang, so_luong) VALUES (1, 1, 1);
